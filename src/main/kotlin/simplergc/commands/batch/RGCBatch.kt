package simplergc.commands.batch

import java.util.prefs.Preferences
import javax.swing.JFrame
import javax.swing.JTabbedPane
import net.imagej.ImageJ
import org.scijava.Context
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import simplergc.commands.batch.views.rgcCounterPanel
import simplergc.commands.batch.views.rgcTransductionPanel
import simplergc.services.CellColocalizationService
import simplergc.services.CellSegmentationService

@Plugin(type = Command::class, menuPath = "Plugins > Simple RGC > RGC Batch")
class RGCBatch : Command {

    @Parameter
    private lateinit var logService: LogService

    @Parameter
    private lateinit var context: Context

    object OutputFormat {
        const val CSV = "Save as CSV file"
        const val XML = "Save as XML file"
    }

    private val prefs = Preferences.userRoot().node(this.javaClass.name)

    private fun gui() {
        val frame = JFrame()
        val simpleCellCounterPanel = rgcCounterPanel(context, prefs)
        val simpleColocalizerPanel = rgcTransductionPanel(context, prefs)
        val tp = JTabbedPane()
        tp.setBounds(10, 10, 500, 550)
        tp.add("RGCCounter", simpleCellCounterPanel)
        tp.add("RGCTransduction", simpleColocalizerPanel)
        frame.add(tp)
        frame.setSize(525, 600)
        frame.isResizable = false

        frame.layout = null
        frame.isVisible = true
    }

    override fun run() {
        gui()
    }

    companion object {
        /**
         * Entry point to directly open the plugin, used for debugging purposes.
         *
         * @throws Exception
         */
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val ij = ImageJ()

            ij.context().inject(CellSegmentationService())
            ij.context().inject(CellColocalizationService())

            ij.launch()

            ij.command().run(RGCBatch::class.java, true)
        }
    }
}