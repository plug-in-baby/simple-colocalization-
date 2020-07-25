package simplergc.commands.batch.output

import simplergc.commands.RGCTransduction
import simplergc.services.BaseRow
import simplergc.services.CellColocalizationService.CellAnalysis
import simplergc.services.Field
import simplergc.services.IntField
import simplergc.services.SimpleOutput
import simplergc.services.StringField
import simplergc.services.Table
import simplergc.services.colocalizer.output.ColocalizationOutput
import simplergc.services.colocalizer.output.DocumentationRow

enum class Metric(val value: String, val compute: (CellAnalysis) -> Int) {
    Area("Morphology Area", CellAnalysis::area),
    Mean("Mean Int", CellAnalysis::mean),
    Median("Median Int", CellAnalysis::median),
    Min("Min Int", CellAnalysis::min),
    Max("Max Int", CellAnalysis::max),
    IntDen("Raw IntDen", CellAnalysis::rawIntDen)
}

abstract class BatchColocalizationOutput(val colocalizatonOutput: ColocalizationOutput) : SimpleOutput {

    val fileNameAndResultsList = mutableListOf<Pair<String, RGCTransduction.TransductionResult>>()

    val documentationRows = listOf(
        DocumentationRow("The article: ", "TODO: Insert citation"),
        DocumentationRow("", ""),
        DocumentationRow("Abbreviation", "Description"),
        DocumentationRow("Summary", "Key measurements per image"),
        DocumentationRow("Mean Int: ", "Mean fluorescence intensity for each transduced cell"),
        DocumentationRow("Median Int:", "Median fluorescence intensity for each transduced cell"),
        DocumentationRow("Min Int: ", "Min fluorescence intensity for each transduced cell"),
        DocumentationRow("Max Int: ", "Max fluorescence intensity for each transduced cell"),
        DocumentationRow("Raw IntDen:", "Raw Integrated Density for each transduced cell"),
        DocumentationRow("Parameters", "Parameters used to run the SimpleRGC plugin")
    )

    abstract fun writeMetricSheet(metric: Metric)

    fun addTransductionResultForFile(transductionResult: RGCTransduction.TransductionResult, file: String) {
        fileNameAndResultsList.add(Pair(file, transductionResult))
        colocalizatonOutput.addTransductionResultForFile(transductionResult, file)
    }

    // Returns a map from metric to a list of [filenames and a list of values].
    fun metricMappings(): Map<Metric, MutableList<Pair<String, List<Int>>>> {
        return Metric.values().toList().associateWith { metric: Metric ->
            // Create [filenames and a list of values] to associate with metric
            val fileValues = mutableListOf<Pair<String, List<Int>>>()
            for ((fileName, result) in fileNameAndResultsList) {
                val cellValues = result.overlappingTransducedIntensityAnalysis.map { cell ->
                    metric.compute(cell)
                }
                fileValues.add(Pair(fileName, cellValues))
            }
            fileValues
        }
    }

    fun maxRows(): Int {
        val results = fileNameAndResultsList.unzip().second
        val sizes = results.map { it.overlappingTwoChannelCells.size }
        return sizes.max() ?: 0
    }

    fun metricData() =
        Table(listOf("Transduced Cell") + fileNameAndResultsList.map { (filename, _) -> filename })
}

// A MetricRow is a row for a given cell in a given file. The parameter metrics is nullable because not all columns are
// of equal length so fields can be null.
data class MetricRow(val rowIdx: Int, val metrics: List<Int?>) : BaseRow {
    override fun toList(): List<Field<*>> {
        return metrics.map { StringField(it?.toString() ?: "") }
    }
}
