package simplecolocalization.commands.batch.controllers

import java.io.File
import java.io.FileNotFoundException
import org.scijava.Context
import simplecolocalization.commands.batch.BatchableColocalizer
import simplecolocalization.services.CellDiameterRange

/** Runs BatchableColocalizer, called in action listener for "Ok" button. */
fun runSimpleColocalizer(
    inputFolder: File?,
    shouldProcessFilesInNestedFolders: Boolean,
    thresholdRadius: Int,
    gaussianBlurSigma: Double,
    outputFormat: String,
    targetChannel: Int,
    transducedChannel: Int,
    allCellsChannel: Int,
    outputFile: File?,
    context: Context
) {
    if (inputFolder == null) {
        throw FileNotFoundException("No output directory is selected")
    } else if (outputFile == null) {
        throw FileNotFoundException("No output file selected")
    } else if (!inputFolder.exists()) {
        throw FileNotFoundException("The input folder could not be opened. Please create it if it does not already exist")
    }

    val files = getAllFiles(inputFolder, shouldProcessFilesInNestedFolders)
    val colocalizer = BatchableColocalizer(targetChannel, transducedChannel, allCellsChannel, context)
    colocalizer.process(
        openFiles(files),
        CellDiameterRange(0.0, 100.0),
        thresholdRadius,
        gaussianBlurSigma,
        outputFormat,
        outputFile
    )
}
