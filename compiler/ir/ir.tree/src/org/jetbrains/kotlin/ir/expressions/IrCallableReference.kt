/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.name.Name

abstract class IrCallableReference<S : IrSymbol>(typeArgumentsCount: Int) : IrMemberAccessExpression<S>(typeArgumentsCount) {
    abstract val referencedName: Name
}
