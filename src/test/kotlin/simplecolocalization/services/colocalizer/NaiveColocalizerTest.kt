package simplecolocalization.services.colocalizer

import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row

class NaiveColocalizerTest : FreeSpec({
    "Analyse Targeting" - {
        listOf(
            row(
                "zero threshold matches all cells",
                0f,
                listOf(PositionedCell(hashSetOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)))),
                listOf(PositionedCell(hashSetOf(Pair(0, 0)))),
                TransductionAnalysis(listOf(PositionedCell(hashSetOf(Pair(0, 0)))), listOf())
            ),
            row(
                "one threshold matches no cells",
                1f,
                listOf(PositionedCell(hashSetOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)))),
                listOf(PositionedCell(hashSetOf(Pair(0, 0)))),
                TransductionAnalysis(listOf(), listOf(PositionedCell(hashSetOf(Pair(0, 0)))))
            ),
            row(
                "transduced cell overlaps multiple target cells",
                0.2f,
                listOf(PositionedCell(hashSetOf(Pair(0, 0))), PositionedCell(hashSetOf(Pair(0, 1))), PositionedCell(hashSetOf(Pair(1, 0))), PositionedCell(hashSetOf(Pair(1, 1)))),
                listOf(PositionedCell(hashSetOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)))),
                TransductionAnalysis(listOf(PositionedCell(hashSetOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)))), listOf())
            ),
            row(
                "transduced cell does not overlap any target cells",
                0f,
                listOf(PositionedCell(hashSetOf(Pair(0, 0))), PositionedCell(hashSetOf(Pair(0, 1))), PositionedCell(hashSetOf(Pair(1, 0))), PositionedCell(hashSetOf(Pair(1, 1)))),
                listOf(PositionedCell(hashSetOf(Pair(2, 2)))),
                TransductionAnalysis(listOf(), listOf(PositionedCell(hashSetOf(Pair(2, 2)))))
            )
        ).map { (description: String, threshold: Float, target: List<PositionedCell>, transduced: List<PositionedCell>, expected: TransductionAnalysis) ->
            description {
                NaiveColocalizer(threshold).analyseTransduction(target, transduced) shouldBe expected
            }
        }
    }
})
