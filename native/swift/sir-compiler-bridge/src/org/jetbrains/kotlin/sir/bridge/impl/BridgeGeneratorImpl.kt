/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge.impl

import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.bridge.*
import org.jetbrains.kotlin.sir.util.SirSwiftModule

private const val exportAnnotationFqName = "kotlin.native.internal.ExportForCppRuntime"
private const val stdintHeader = "stdint.h"

internal class BridgeGeneratorImpl : BridgeGenerator {
    override fun generate(request: BridgeRequest): FunctionBridge {
        val (kotlinReturnType, cReturnType) = bridgeType(request.function.returnType)
        val parameterBridges = request.function.parameters.map { bridgeParameter(it) }

        val cDeclaration = createCDeclaration(request.bridgeName, cReturnType, parameterBridges.map { it.c })
        val kotlinBridge = createKotlinBridge(request.bridgeName, request.fqName, kotlinReturnType, parameterBridges.map { it.kotlin })
        return FunctionBridge(
            KotlinFunctionBridge(kotlinBridge, listOf(exportAnnotationFqName)),
            CFunctionBridge(cDeclaration, listOf(stdintHeader))
        )
    }
}

private fun createKotlinBridge(
    bridgeName: String,
    functionFqName: List<String>,
    returnType: KotlinType,
    parameterBridges: List<KotlinBridgeParameter>,
): List<String> {
    val declaration = createKotlinDeclarationSignature(bridgeName, returnType, parameterBridges)
    val annotation = "@${exportAnnotationFqName.substringAfterLast('.')}(\"${bridgeName}\")"
    val resultName = "result"
    val callSite = createCallSite(functionFqName, parameterBridges.map { it.name }, resultName)
    return """
        $annotation
        $declaration {
            $callSite
            return $resultName
        }
    """.trimIndent().lines()
}

private fun createCallSite(functionFqName: List<String>, parameterNames: List<String>, resultName: String): String {
    val functionCall = "${functionFqName.joinToString(separator = ".")}(${parameterNames.joinToString(", ")})"
    return "val $resultName = $functionCall"
}

private fun createKotlinDeclarationSignature(bridgeName: String, returnType: KotlinType, parameters: List<KotlinBridgeParameter>): String {
    return "public fun $bridgeName(${
        parameters.joinToString(
            separator = ", ",
            transform = { "${it.name}: ${it.type.repr}" }
        )
    }): ${returnType.repr}"
}

private fun createCDeclaration(bridgeName: String, returnType: CType, parameters: List<CBridgeParameter>): List<String> {
    val cParameters = parameters.joinToString(separator = ", ", transform = { "${it.type.repr} ${it.name}" })
    val declaration = "${returnType.repr} $bridgeName($cParameters);"
    return listOf(declaration)
}


private fun bridgeType(type: SirType): Pair<KotlinType, CType> {
    require(type is SirNominalType)
    return when (type.type) {
        SirSwiftModule.bool -> (KotlinType.Boolean to CType.Bool)

        SirSwiftModule.int8 -> (KotlinType.Byte to CType.Int8)
        SirSwiftModule.int32 -> (KotlinType.Int to CType.Int32)
        SirSwiftModule.int64 -> (KotlinType.Long to CType.Int64)
        SirSwiftModule.int16 -> (KotlinType.Short to CType.Int16)

        SirSwiftModule.uint8 -> (KotlinType.UByte to CType.UInt8)
        SirSwiftModule.uint32 -> (KotlinType.UInt to CType.UInt32)
        SirSwiftModule.uint64 -> (KotlinType.ULong to CType.UInt64)
        SirSwiftModule.uint16 -> (KotlinType.UShort to CType.UInt16)

        else -> error("Unsupported type: ${type.type}")
    }
}

private fun bridgeParameter(parameter: SirParameter): BridgeParameter {
    val bridgeParameterName = parameter.argumentName?.let(::createBridgeParameterName) ?: ""
    val (kotlinType, cType) = bridgeType(parameter.type)
    return BridgeParameter(
        KotlinBridgeParameter(bridgeParameterName, kotlinType),
        CBridgeParameter(bridgeParameterName, cType)
    )
}

fun createBridgeParameterName(kotlinName: String): String {
    // TODO: Post-process because C has stricter naming conventions.
    return kotlinName
}

internal data class BridgeParameter(
    val kotlin: KotlinBridgeParameter,
    val c: CBridgeParameter,
)

internal data class CBridgeParameter(
    val name: String,
    val type: CType,
)

enum class CType(val repr: String) {
    Bool("_Bool"),

    Int8("int8_t"),
    Int16("int16_t"),
    Int32("int32_t"),
    Int64("int64_t"),

    UInt8("uint8_t"),
    UInt16("uint16_t"),
    UInt32("uint32_t"),
    UInt64("uint64_t"),
}

internal data class KotlinBridgeParameter(
    val name: String,
    val type: KotlinType,
)

internal enum class KotlinType(val repr: String) {
    Boolean("Boolean"),

    Byte("Byte"),
    Short("Short"),
    Int("Int"),
    Long("Long"),

    UByte("UByte"),
    UShort("UShort"),
    UInt("UInt"),
    ULong("ULong"),
}

