package microarch.delivery.core.domain.model

import libs.errs.getValueOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertAll

class LocationValueTest {
    @TestFactory
    fun `minus computes manhattan distance for field 10x10`(): List<DynamicTest> {
        // Given — контрольные точки поля 10x10 и ожидаемое манхэттенское расстояние
        data class Case(
            val a: Pair<Int, Int>,
            val b: Pair<Int, Int>,
            val expected: Int,
        )

        val cases =
            listOf(
                Case(4 to 9, 2 to 6, 5),
                Case(5 to 5, 5 to 5, 0),
                Case(3 to 3, 4 to 3, 1),
                Case(1 to 1, 1 to 10, 9),
                Case(1 to 5, 10 to 5, 9),
                Case(1 to 1, 10 to 10, 18),
                Case(1 to 10, 10 to 1, 18),
                Case(2 to 8, 7 to 3, 10),
                Case(3 to 3, 4 to 4, 2),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("distance between ${case.a} and ${case.b} == ${case.expected}") {
                // Given
                val first = LocationValue.create(case.a.first, case.a.second).getValueOrThrow()
                val second = LocationValue.create(case.b.first, case.b.second).getValueOrThrow()

                // When
                val resultFirstMinusSecond = first - second
                val resultSecondMinusFirst = second - first

                // Then
                assertAll(
                    {
                        assertThat(resultFirstMinusSecond)
                            .describedAs("distance ${case.a} - ${case.b}")
                            .isEqualTo(case.expected)
                    },
                    {
                        assertThat(resultSecondMinusFirst)
                            .describedAs("distance ${case.b} - ${case.a}")
                            .isEqualTo(case.expected)
                    },
                )
            }
        }
    }
}
