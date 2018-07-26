package org.jetbrains.research.kex.test

class BasicTests {
    fun testPlain(a: Int, b: Int): Int {
        val i = 10
        val j = 152
        val k = j + i - 15

        val max = maxOf(a, b, k)
        Intrinsics.assertReachable()
        return max
    }

    fun testIf(a: Int, b: Int): Int {
        val res = if (a > b) {
            Intrinsics.assertReachable(a > b)
            a - b
        } else {
            Intrinsics.assertReachable(a <= b)
            a + b
        }

        Intrinsics.assertReachable()
        println(res)
        return res
    }

    fun testLoop(a: Int, b: Int): Int {
        var x = a - b
        while (x < a) {
            Intrinsics.assertReachable(x < a)
            if (b > x) {
                Intrinsics.assertReachable(b > x, x < a)
                println("b bigger")
            }
            ++x
        }
        Intrinsics.assertReachable()
        return x
    }

    fun testUnreachableIf(x: Int): Int {
        val set = "asdasdal;djadslas;d".length
        val z = 10
        val y = if (x > z && x < 0) {
            Intrinsics.assertUnreachable()
            println("lol")
            142
        } else {
            Intrinsics.assertReachable(x <= z || x >= 0)
            println("lol2")
            x- 2 * x
        }
        Intrinsics.assertReachable()
        return y
    }

    fun testUnreachableLoop(): Int {
        var x = 10
        while (x > 5) {
            Intrinsics.assertReachable(x > 5)
            ++x
        }
        Intrinsics.assertUnreachable()
        println(x)
        return x
    }

    fun testArray(): Int {
        val array = arrayOf(
                arrayOf(0, 1, 2, 3, 4),
                arrayOf(5, 6, 7, 8, 9),
                arrayOf(10, 11, 12, 13, 14)
        )
        if (array[4][4] > 10) {
            Intrinsics.assertReachable()
        }
        if (array.size > 2) {
            Intrinsics.assertReachable()
        }
        if (array[0].size > 4) {
            Intrinsics.assertReachable()
        }
        return array.flatten().reduce { a, b -> a + b}
    }
}