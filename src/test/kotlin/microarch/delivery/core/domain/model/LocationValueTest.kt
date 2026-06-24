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

    @TestFactory
    fun `create accepts valid coordinates within range 1 to 10`(): List<DynamicTest> {
        // Given — допустимые координаты поля 10x10 (границы 1 и 10 включительно)
        data class Case(
            val x: Int,
            val y: Int,
        )

        val cases =
            listOf(
                Case(1, 1),
                Case(10, 10),
                Case(1, 10),
                Case(10, 1),
                Case(5, 5),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("create(${case.x}, ${case.y}) succeeds") {
                // When
                val result = LocationValue.create(case.x, case.y)

                // Then
                assertAll(
                    {
                        assertThat(result.isRight())
                            .describedAs("create(${case.x}, ${case.y}) is right")
                            .isTrue()
                    },
                    {
                        val value = result.getOrNull()
                        assertThat(value).describedAs("created value").isNotNull
                        assertThat(value?.x).describedAs("x").isEqualTo(case.x)
                        assertThat(value?.y).describedAs("y").isEqualTo(case.y)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `create rejects coordinates outside range 1 to 10`(): List<DynamicTest> {
        // Given — недопустимые координаты за пределами диапазона 1..10
        data class Case(
            val x: Int,
            val y: Int,
            val expectedMessagePart: String,
        )

        val cases =
            listOf(
                Case(0, 5, "x (0) must be greater than 1"),
                Case(11, 5, "x (11) must be less than 10"),
                Case(5, 0, "y (0) must be greater than 1"),
                Case(5, 11, "y (11) must be less than 10"),
                Case(0, 0, "must be greater than 1"),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("create(${case.x}, ${case.y}) is rejected") {
                // When
                val result = LocationValue.create(case.x, case.y)

                // Then
                assertAll(
                    {
                        assertThat(result.isLeft())
                            .describedAs("create(${case.x}, ${case.y}) is left")
                            .isTrue()
                    },
                    {
                        assertThat(result.leftOrNull()?.fullMessage())
                            .describedAs("error message")
                            .contains(case.expectedMessagePart)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `equals returns true when coordinates match`(): List<DynamicTest> {
        // Given — пары координат, которые должны быть равны (равенство по x и y)
        data class Case(
            val a: Pair<Int, Int>,
            val b: Pair<Int, Int>,
        )

        val cases =
            listOf(
                Case(3 to 7, 3 to 7),
                Case(1 to 1, 1 to 1),
                Case(10 to 10, 10 to 10),
                Case(1 to 10, 1 to 10),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("${case.a} == ${case.b}") {
                // Given
                val first = LocationValue.create(case.a.first, case.a.second).getValueOrThrow()
                val second = LocationValue.create(case.b.first, case.b.second).getValueOrThrow()

                // Then
                assertAll(
                    {
                        assertThat(first == second)
                            .describedAs("first == second")
                            .isTrue()
                    },
                    {
                        assertThat(second == first)
                            .describedAs("second == first (symmetry)")
                            .isTrue()
                    },
                    {
                        assertThat(first.hashCode())
                            .describedAs("hashCode equality for equal objects")
                            .isEqualTo(second.hashCode())
                    },
                )
            }
        }
    }

    @TestFactory
    fun `equals returns false when coordinates differ`(): List<DynamicTest> {
        // Given — пары координат, которые НЕ должны быть равны
        data class Case(
            val a: Pair<Int, Int>,
            val b: Pair<Int, Int>,
        )

        val cases =
            listOf(
                Case(4 to 7, 3 to 7),
                Case(3 to 7, 3 to 8),
                Case(3 to 7, 8 to 2),
                Case(1 to 5, 1 to 6),
                Case(6 to 5, 5 to 5),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("${case.a} != ${case.b}") {
                // Given
                val first = LocationValue.create(case.a.first, case.a.second).getValueOrThrow()
                val second = LocationValue.create(case.b.first, case.b.second).getValueOrThrow()

                // Then
                assertAll(
                    {
                        assertThat(first != second)
                            .describedAs("first != second")
                            .isTrue()
                    },
                    {
                        assertThat(second != first)
                            .describedAs("second != first (symmetry)")
                            .isTrue()
                    },
                )
            }
        }
    }
}
