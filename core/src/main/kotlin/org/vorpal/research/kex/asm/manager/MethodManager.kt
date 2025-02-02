package org.vorpal.research.kex.asm.manager

import org.vorpal.research.kex.config.kexConfig
import org.vorpal.research.kex.ktype.type
import org.vorpal.research.kex.util.asArray
import org.vorpal.research.kfg.ClassManager
import org.vorpal.research.kfg.Package
import org.vorpal.research.kfg.ir.Method
import org.vorpal.research.kfg.ir.value.instruction.ReturnInst
import org.vorpal.research.kthelper.assert.ktassert
import org.vorpal.research.kthelper.logging.log

object MethodManager {
    object InlineManager {
        val inliningEnabled = kexConfig.getBooleanValue("inliner", "enabled", true)
        private val ignorePackages = hashSetOf<Package>()
        private val ignoreClasses = hashSetOf<String>()

        init {
            ignorePackages.addAll(
                kexConfig.getMultipleStringValue("inliner", "ignorePackage", ",").map {
                    Package.parse(it)
                }
            )
            ignoreClasses.addAll(
                kexConfig.getMultipleStringValue("inliner", "ignoreClass", ",").map {
                    it.replace(Package.CANONICAL_SEPARATOR, Package.SEPARATOR)
                }
            )
            ignorePackages += Package.parse("org.vorpal.research.kex.intrinsics.*")
        }

        fun isIgnored(method: Method) = when {
            ignorePackages.any { it.isParent(method.klass.pkg) } -> true
            ignoreClasses.any { method.cm[it] == method.klass } -> true
            else -> false
        }

        fun isInlinable(method: Method): Boolean = when {
            !inliningEnabled -> false
            isIgnored(method) -> false
            method.isStatic -> true
            method.isConstructor -> true
            !method.isFinal -> false
            method.body.flatten().all { it !is ReturnInst } -> false
            else -> true
        }
    }

    object IntrinsicManager {
        private const val intrinsicsClass = "kotlin/jvm/internal/Intrinsics"

        fun checkParameterIsNotNull(cm: ClassManager) = cm[intrinsicsClass].getMethod(
                "checkParameterIsNotNull",
                cm.type.voidType, cm.type.objectType, cm.type.stringType
        )

        fun checkNotNullParameter(cm: ClassManager) = cm[intrinsicsClass].getMethod(
                "checkNotNullParameter",
                cm.type.voidType, cm.type.objectType, cm.type.stringType
        )

        fun areEqual(cm: ClassManager) = cm[intrinsicsClass].getMethod(
                "areEqual",
                cm.type.boolType, cm.type.objectType, cm.type.objectType
        )

    }

    object KexIntrinsicManager {
        private val supportedVersions = mutableSetOf("0.1.0")
        private const val assertIntrinsics = "org/vorpal/research/kex/intrinsics/AssertIntrinsics"
        private const val collectionIntrinsics = "org/vorpal/research/kex/intrinsics/CollectionIntrinsics"
        private const val unknownIntrinsics = "org/vorpal/research/kex/intrinsics/UnknownIntrinsics"
        private const val objectIntrinsics = "org/vorpal/research/kex/intrinsics/ObjectIntrinsics"

        init {
            val currentVersion = kexConfig.getStringValue("kex", "intrinsicsVersion")
            ktassert(currentVersion in supportedVersions) {
                log.error("Unsupported version of kex-intrinsics: $currentVersion")
                log.error("Supported versions: ${supportedVersions.joinToString(", ")}")
            }
        }

        fun assertionsIntrinsics(cm: ClassManager) = cm[assertIntrinsics]
        fun collectionIntrinsics(cm: ClassManager) = cm[collectionIntrinsics]
        fun unknownIntrinsics(cm: ClassManager) = cm[unknownIntrinsics]
        fun objectIntrinsics(cm: ClassManager) = cm[objectIntrinsics]
        private fun getGenerator(cm: ClassManager, name: String) = cm["org/vorpal/research/kex/intrinsics/internal/${name}Generator"]

        /**
         * assert intrinsics
         */
        fun kexAssume(cm: ClassManager) = cm[assertIntrinsics].getMethod(
            "kexAssume",
            cm.type.voidType, cm.type.boolType
        )

        fun kexNotNull(cm: ClassManager) = cm[assertIntrinsics].getMethod(
            "kexNotNull",
            cm.type.objectType, cm.type.objectType
        )

        fun kexAssert(cm: ClassManager) = cm[assertIntrinsics].getMethod(
            "kexAssert",
            cm.type.voidType, cm.type.boolType
        )

        fun kexAssertWithId(cm: ClassManager) = cm[assertIntrinsics].getMethod(
            "kexAssert",
            cm.type.voidType, cm.type.stringType, cm.type.boolType
        )

        fun kexUnreachableEmpty(cm: ClassManager) = cm[assertIntrinsics].getMethod(
            "kexUnreachable",
            cm.type.voidType,
        )

        fun kexUnreachable(cm: ClassManager) = cm[assertIntrinsics].getMethod(
            "kexUnreachable",
            cm.type.voidType, cm.type.boolType
        )

        fun kexUnreachableWithId(cm: ClassManager) = cm[assertIntrinsics].getMethod(
            "kexUnreachable",
            cm.type.voidType, cm.type.stringType, cm.type.boolType
        )

        /**
         * unknown intrinsics
         */
        fun kexUnknownBoolean(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownBoolean",
            cm.type.boolType,
        )

        fun kexUnknownByte(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownByte",
            cm.type.byteType,
        )

        fun kexUnknownChar(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownChar",
            cm.type.charType,
        )

        fun kexUnknownShort(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownShort",
            cm.type.shortType,
        )

        fun kexUnknownInt(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownInt",
            cm.type.intType,
        )

        fun kexUnknownLong(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownLong",
            cm.type.longType,
        )

        fun kexUnknownFloat(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownFloat",
            cm.type.floatType,
        )

        fun kexUnknownDouble(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownDouble",
            cm.type.doubleType,
        )

        fun kexUnknown(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknown",
            cm.type.objectType,
        )


        fun kexUnknownBooleanArray(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownBooleanArray",
            cm.type.getArrayType(cm.type.boolType),
        )

        fun kexUnknownByteArray(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownByteArray",
            cm.type.getArrayType(cm.type.byteType),
        )

        fun kexUnknownCharArray(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownCharArray",
            cm.type.getArrayType(cm.type.charType),
        )

        fun kexUnknownShortArray(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownShortArray",
            cm.type.getArrayType(cm.type.shortType),
        )

        fun kexUnknownIntArray(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownIntArray",
            cm.type.getArrayType(cm.type.intType),
        )

        fun kexUnknownLongArray(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownLongArray",
            cm.type.getArrayType(cm.type.longType),
        )

        fun kexUnknownFloatArray(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownFloatArray",
            cm.type.getArrayType(cm.type.floatType),
        )

        fun kexUnknownDoubleArray(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownDoubleArray",
            cm.type.getArrayType(cm.type.doubleType),
        )

        fun kexUnknownArray(cm: ClassManager) = cm[unknownIntrinsics].getMethod(
            "kexUnknownArray",
            cm.type.getArrayType(cm.type.objectType),
        )

        /**
         * collection intrinsics
         */
        fun kexForAll(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "forAll",
            cm.type.boolType,
            cm.type.intType,
            cm.type.intType,
            cm["org/vorpal/research/kex/intrinsics/internal/IntConsumer"].type
        )

        fun kexContainsBool(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "containsBool",
            cm.type.boolType,
            cm.type.boolType.asArray(cm.type),
            cm.type.boolType
        )

        fun kexContainsByte(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "containsByte",
            cm.type.boolType,
            cm.type.byteType.asArray(cm.type),
            cm.type.byteType
        )

        fun kexContainsChar(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "containsChar",
            cm.type.boolType,
            cm.type.charType.asArray(cm.type),
            cm.type.charType
        )

        fun kexContainsShort(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "containsShort",
            cm.type.boolType,
            cm.type.shortType.asArray(cm.type),
            cm.type.shortType
        )

        fun kexContainsInt(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "containsInt",
            cm.type.boolType,
            cm.type.intType.asArray(cm.type),
            cm.type.intType
        )

        fun kexContainsLong(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "containsLong",
            cm.type.boolType,
            cm.type.longType.asArray(cm.type),
            cm.type.longType
        )

        fun kexContainsFloat(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "containsFloat",
            cm.type.boolType,
            cm.type.floatType.asArray(cm.type),
            cm.type.floatType
        )

        fun kexContainsDouble(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "containsDouble",
            cm.type.boolType,
            cm.type.doubleType.asArray(cm.type),
            cm.type.doubleType
        )

        fun kexContainsRef(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "containsRef",
            cm.type.boolType,
            cm.type.objectType.asArray(cm.type),
            cm.type.objectType
        )

        fun kexContains(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "contains",
            cm.type.boolType,
            cm.type.objectType.asArray(cm.type),
            cm.type.objectType
        )

        fun kexContainsMethods(cm: ClassManager) = setOf(
            kexContainsBool(cm),
            kexContainsByte(cm),
            kexContainsChar(cm),
            kexContainsShort(cm),
            kexContainsInt(cm),
            kexContainsLong(cm),
            kexContainsFloat(cm),
            kexContainsDouble(cm),
            kexContainsRef(cm)
        )

        fun kexGenerateBoolArray(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "generateBoolArray",
            cm.type.boolType.asArray(cm.type),
            cm.type.intType,
            getGenerator(cm, "Boolean").toType()
        )

        fun kexGenerateByteArray(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "generateByteArray",
            cm.type.byteType.asArray(cm.type),
            cm.type.intType,
            getGenerator(cm, "Byte").toType()
        )

        fun kexGenerateCharArray(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "generateCharArray",
            cm.type.charType.asArray(cm.type),
            cm.type.intType,
            getGenerator(cm, "Char").toType()
        )

        fun kexGenerateShortArray(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "generateShortArray",
            cm.type.shortType.asArray(cm.type),
            cm.type.intType,
            getGenerator(cm, "Short").toType()
        )

        fun kexGenerateIntArray(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "generateIntArray",
            cm.type.intType.asArray(cm.type),
            cm.type.intType,
            getGenerator(cm, "Int").toType()
        )

        fun kexGenerateLongArray(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "generateLongArray",
            cm.type.longType.asArray(cm.type),
            cm.type.intType,
            getGenerator(cm, "Long").toType()
        )

        fun kexGenerateFloatArray(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "generateFloatArray",
            cm.type.floatType.asArray(cm.type),
            cm.type.intType,
            getGenerator(cm, "Float").toType()
        )

        fun kexGenerateDoubleArray(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "generateDoubleArray",
            cm.type.doubleType.asArray(cm.type),
            cm.type.intType,
            getGenerator(cm, "Double").toType()
        )

        fun kexGenerateObjectArray(cm: ClassManager) = cm[collectionIntrinsics].getMethod(
            "generateObjectArray",
            cm.type.objectType.asArray(cm.type),
            cm.type.intType,
            getGenerator(cm, "Object").toType()
        )

        fun kexGenerateArrayMethods(cm: ClassManager) = setOf(
            kexGenerateBoolArray(cm),
            kexGenerateByteArray(cm),
            kexGenerateCharArray(cm),
            kexGenerateShortArray(cm),
            kexGenerateIntArray(cm),
            kexGenerateLongArray(cm),
            kexGenerateFloatArray(cm),
            kexGenerateDoubleArray(cm),
            kexGenerateObjectArray(cm)
        )
    }
}