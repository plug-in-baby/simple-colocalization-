package simplecolocalization.commands.batch

import de.siegmar.fastcsv.writer.CsvWriter
import ij.IJ
import ij.ImagePlus
import ij.gui.GenericDialog
import ij.gui.MessageDialog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.scijava.Context
import org.w3c.dom.Document
import org.w3c.dom.Element
import simplecolocalization.commands.ChannelDoesNotExistException
import simplecolocalization.commands.SimpleColocalization
import simplecolocalization.commands.batch.SimpleBatch.OutputFormat
import simplecolocalization.preprocessing.PreprocessingParameters

class BatchableColocalizer(
    private val targetChannel: Int,
    private val transducedChannel: Int,
    private val allChannel: Int,
    private val context: Context
) : Batchable {
    override fun process(
        inputImages: List<ImagePlus>,
        outputFormat: String,
        outputFile: File,
        preprocessingParameters: PreprocessingParameters
    ) {
        val simpleColocalization = SimpleColocalization()

        // TODO(sonjoonho): I hate this
        simpleColocalization.targetChannel = targetChannel
        simpleColocalization.transducedChannel = transducedChannel
        simpleColocalization.allCellsChannel = allChannel
        context.inject(simpleColocalization)

        val analyses = inputImages.mapNotNull {
            try {
                simpleColocalization.process(it, preprocessingParameters)
            } catch (e: ChannelDoesNotExistException) {
                MessageDialog(IJ.getInstance(), "Error", e.message)
                null
            }
        }

        val fileNameAndAnalysis = inputImages.map { it.title }.zip(analyses)

        try {
            when (outputFormat) {
                OutputFormat.CSV -> outputToCSV(fileNameAndAnalysis, outputFile)
                OutputFormat.XML -> outputToXML(fileNameAndAnalysis, outputFile)
                else -> throw IllegalArgumentException("Invalid output type provided")
            }
        } catch (ioe: IOException) {
            displayErrorDialog()
        }

        MessageDialog(
            IJ.getInstance(),
            "Saved",
            "The colocalization results have successfully been saved to the specified file."
        )
    }

    private fun outputToCSV(
        fileNameAndAnalysis: List<Pair<String, SimpleColocalization.TransductionResult>>,
        outputFile: File
    ) {
        val csvWriter = CsvWriter()
        val outputData = mutableListOf(
            arrayOf(
                "File Name",
                "Total Target Cells",
                "Total Transduced Target Cells",
                "Cells Overlapping All Three Channels"
            )
        )
        outputData.addAll(fileNameAndAnalysis.map {
            val totalTargetCells = it.second.targetCellCount.toString()
            val totalTransducedTargetCells = it.second.overlappingTwoChannelCells.size.toString()
            val threeChannelCells =
                if (it.second.overlappingThreeChannelCells != null) it.second.overlappingThreeChannelCells!!.size.toString() else "N/A"
            arrayOf(it.first, totalTargetCells, totalTransducedTargetCells, threeChannelCells)
        })
        csvWriter.write(outputFile, StandardCharsets.UTF_8, outputData)
    }

    private fun outputToXML(
        fileNameAndAnalysis: List<Pair<String, SimpleColocalization.TransductionResult>>,
        outputFile: File
    ) {
        // Create XML factory, builder and document.
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.newDocument()

        val rootElement = doc.createElement("colocalizationresult")
        doc.appendChild(rootElement)
        fileNameAndAnalysis.forEach { addSummaryForFile(it.second, it.first, doc, rootElement) }

        try {
            // Create transformer and set output properties.
            val tr = TransformerFactory.newInstance().newTransformer()
            tr.setOutputProperty(OutputKeys.INDENT, "yes")
            tr.setOutputProperty(OutputKeys.METHOD, "xml")
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "roles.dtd")
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            // Send output file to file output stream.
            tr.transform(DOMSource(doc), StreamResult(FileOutputStream(outputFile)))
        } catch (te: TransformerException) {
            displayErrorDialog(fileType = "XML")
        }
    }

    // TODO(tiger-cross): Reduce duplication in the below 3 functions.
    private fun displayErrorDialog(fileType: String = "") {
        GenericDialog("Error").apply {
            addMessage("Unable to save results to $fileType file. Ensure the output file is not currently in use by other programs and try again.")
            hideCancelButton()
            showDialog()
        }
    }

    /**
     * Create a <attrName> element with the value as a Text Node for a given parent element.
     * Used for creating the summary attributes.
     */
    private fun addAttribute(attrName: String, attrVal: String, parent: Element, doc: Document) {
        val elem = doc.createElement(attrName)
        parent.appendChild(elem)
        elem.appendChild(doc.createTextNode(attrVal))
    }

    /**
     * Adds a summary section to the XML output.
     */
    private fun addSummaryForFile(
        result: SimpleColocalization.TransductionResult,
        fileName: String,
        doc: Document,
        root: Element
    ) {
        val summary = doc.createElement("Summary")
        root.appendChild(summary)
        val fileAttr = doc.createAttribute("File")
        fileAttr.setValue(fileName)
        summary.setAttributeNode(fileAttr)
        addAttribute("TotalTargetCellCount", result.targetCellCount.toString(), summary, doc)
        addAttribute(
            "NumTransducedCellsOverlappingTarget",
            result.overlappingTwoChannelCells.size.toString(),
            summary,
            doc
        )
        if (result.overlappingThreeChannelCells != null) {
            addAttribute(
                "NumCellsOverlappingThreeChannels",
                result.overlappingThreeChannelCells.size.toString(),
                summary,
                doc
            )
        }
    }
}
