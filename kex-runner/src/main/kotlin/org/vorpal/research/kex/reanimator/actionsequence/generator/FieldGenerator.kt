package org.vorpal.research.kex.reanimator.actionsequence.generator

import org.vorpal.research.kex.descriptor.Descriptor
import org.vorpal.research.kex.descriptor.ObjectDescriptor
import org.vorpal.research.kex.descriptor.descriptor
import org.vorpal.research.kex.ktype.*
import org.vorpal.research.kex.reanimator.actionsequence.ActionList
import org.vorpal.research.kex.reanimator.actionsequence.ActionSequence
import org.vorpal.research.kex.reanimator.actionsequence.ExternalMethodCall
import org.vorpal.research.kex.reanimator.actionsequence.MethodCall
import org.vorpal.research.kex.util.field
import org.vorpal.research.kfg.Package
import org.vorpal.research.kfg.ir.Class
import org.vorpal.research.kfg.type.SystemTypeNames

class FieldGenerator(val fallback: Generator) : Generator {
    override val context: GeneratorContext
        get() = fallback.context

    val kfgJavaClass = KexJavaClass().kfgClass(context.types)
    val kfgFieldClass = context.cm[SystemTypeNames.field]
    val kexFieldClass = kfgFieldClass.kexType

    override fun supports(descriptor: Descriptor): Boolean = descriptor.type == kexFieldClass

    override fun generate(descriptor: Descriptor, generationDepth: Int): ActionSequence = with(context) {
        descriptor as ObjectDescriptor
        val name = "${descriptor.term}"
        val actionSequence = ActionList(name)

        var klass: Class? = null

        val klassDescriptor = (descriptor["clazz" to KexJavaClass()] as? ObjectDescriptor)?.also { klassDesc ->
            val klassName = klassDesc["name" to KexString()]?.asStringValue
            val asmKlassName = klassName?.replace(Package.CANONICAL_SEPARATOR, Package.SEPARATOR)
            klass = asmKlassName?.let { cm[it] } ?: context.cm.concreteClasses.random(context.random).also {
                klassDesc["name" to KexString()] = descriptor { string(it.canonicalDesc) }
            }
        } ?: descriptor {
            val klassDesc = `object`(KexJavaClass())
            klass = context.cm.concreteClasses.filter { it.fields.isNotEmpty() }.random(context.random)
            klassDesc["name" to KexString()] = string(klass!!.canonicalDesc)
            klassDesc
        }

        val generatedKlass = fallback.generate(klassDescriptor, generationDepth)
        val nameField = descriptor["name" to KexString()]
        val fixedNameField = when (val descriptorName = nameField?.asStringValue) {
            null -> descriptor { string(klass!!.fields.random(context.random).name) }
            else -> when {
                klass!!.fields.any { it.name == descriptorName } -> nameField
                else -> descriptor { string(klass!!.fields.random(context.random).name) }
            }
        }
        val generatedName = fallback.generate(fixedNameField, generationDepth)

        val getDeclField = kfgJavaClass.getMethod("getDeclaredField", kfgFieldClass.type, types.stringType)

        actionSequence += ExternalMethodCall(getDeclField, generatedKlass, listOf(generatedName))

        val setAccessible = descriptor["override" to KexBool()] ?: descriptor { const(false) }
        val setAccessibleAS = fallback.generate(setAccessible, generationDepth)
        val setAccessibleMethod = kfgFieldClass.getMethod("setAccessible", types.voidType, types.boolType)
        actionSequence += MethodCall(setAccessibleMethod, listOf(setAccessibleAS))

        actionSequence
    }
}