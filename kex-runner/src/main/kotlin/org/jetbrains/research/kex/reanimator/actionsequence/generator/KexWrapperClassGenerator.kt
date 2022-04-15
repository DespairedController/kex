package org.jetbrains.research.kex.reanimator.actionsequence.generator

import org.jetbrains.research.kex.descriptor.Descriptor
import org.jetbrains.research.kex.descriptor.ObjectDescriptor
import org.jetbrains.research.kex.descriptor.descriptor
import org.jetbrains.research.kex.ktype.KexRtManager.rtMapped
import org.jetbrains.research.kex.ktype.KexRtManager.rtUnmapped
import org.jetbrains.research.kex.ktype.kexType
import org.jetbrains.research.kex.reanimator.actionsequence.ActionList
import org.jetbrains.research.kex.reanimator.actionsequence.ActionSequence
import org.jetbrains.research.kex.reanimator.actionsequence.ConstructorCall
import org.jetbrains.research.kex.state.transformer.getCtor
import org.jetbrains.research.kex.util.getPrimitive
import org.jetbrains.research.kfg.type.ClassType

class KexWrapperClassGenerator(val fallback: Generator) : Generator {
    override val context: GeneratorContext
        get() = fallback.context
    private val supportedTypes = setOf(
        context.cm.boolWrapper.rtMapped.kexType,
        context.cm.byteWrapper.rtMapped.kexType,
        context.cm.charWrapper.rtMapped.kexType,
        context.cm.shortWrapper.rtMapped.kexType,
        context.cm.intWrapper.rtMapped.kexType,
        context.cm.longWrapper.rtMapped.kexType,
        context.cm.floatWrapper.rtMapped.kexType,
        context.cm.doubleWrapper.rtMapped.kexType
    )

    override fun supports(descriptor: Descriptor): Boolean = descriptor.type in supportedTypes

    override fun generate(descriptor: Descriptor, generationDepth: Int): ActionSequence = with(context) {
        descriptor as ObjectDescriptor

        val name = "${descriptor.term}"
        val actionSequence = ActionList(name)
        saveToCache(descriptor, actionSequence)

        val descriptorKfgType = descriptor.type.getKfgType(types) as ClassType
        val baseKfgType = types.getPrimitive(descriptorKfgType.rtUnmapped)
        val baseType = baseKfgType.kexType

        val valueDesc = when {
            "value" to baseType in descriptor.fields -> descriptor["value" to baseType]!!
            else -> descriptor { default(baseType) }
        }
        val constructor = descriptorKfgType.klass.getCtor(baseKfgType)
        val valueAS = fallback.generate(valueDesc, generationDepth)
        actionSequence += ConstructorCall(constructor, listOf(valueAS))

        actionSequence
    }
}