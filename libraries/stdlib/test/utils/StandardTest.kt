/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.*

class StandardTest {

    @Test fun takeIfIsInstanceMatch() {
        val string: CharSequence = "a string"
        val applied = string.takeIfInstance<String>()
        assertEquals(applied, string)
    }

    @Test fun takeIfIsInstanceNoMatch() {
        val stringBuilder: CharSequence = StringBuilder("a string builder")
        val applied = stringBuilder.takeIfInstance<String>()
        assertEquals(applied, null)
    }

    @Test fun takeIfIsInstanceNoMatchUnrelatedType() {
        val string: CharSequence = "a string"
        val applied = string.takeIfInstance<Int>()
        assertEquals(applied, null)
    }
}
