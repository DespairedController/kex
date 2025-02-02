package org.vorpal.research.kex.state.transformer

import org.vorpal.research.kex.ktype.*
import org.vorpal.research.kex.state.PredicateState
import org.vorpal.research.kex.state.StateBuilder
import org.vorpal.research.kex.state.basic
import org.vorpal.research.kex.state.predicate.*
import org.vorpal.research.kex.state.term.*
import org.vorpal.research.kfg.type.TypeFactory
import org.vorpal.research.kthelper.collection.dequeOf

class ConstStringAdapter(
    val tf: TypeFactory
) : RecollectingTransformer<ConstStringAdapter> {
    override val builders = dequeOf(StateBuilder())
    private val strings = mutableMapOf<String, Term>()

    override fun apply(ps: PredicateState): PredicateState {
        val strings = collectStringTerms(ps).toMutableSet()
        for (str in strings) {
            currentBuilder += buildStr(str.value)
        }
        return super.apply(ps)
    }

    private fun buildStr(string: String): PredicateState = basic {
        val strTerm = generate(KexString())
        state { strTerm.new() }

        val charArray = KexArray(KexChar())
        val valueArray = generate(charArray)
        state { valueArray.new(string.length) }
        for ((index, char) in string.withIndex()) {
            state { valueArray[index].store(const(char)) }
        }

        state { strTerm.field(charArray, "value").store(valueArray) }
        strings[string] = strTerm
    }

    private fun replaceString(constStringTerm: ConstStringTerm) =
        strings.getOrDefault(constStringTerm.value, constStringTerm)

    private val Term.map
        get() = when (this) {
            is ConstStringTerm -> replaceString(this)
            else -> this
        }

    override fun transformArrayInitializerPredicate(predicate: ArrayInitializerPredicate): Predicate =
        predicate(predicate.type, predicate.location) { predicate.arrayRef.initialize(predicate.value.map) }


    override fun transformArrayStorePredicate(predicate: ArrayStorePredicate): Predicate =
        predicate(predicate.type, predicate.location) { predicate.arrayRef.store(predicate.value.map) }

    override fun transformCallPredicate(predicate: CallPredicate): Predicate {
        return when {
            predicate.hasLhv -> predicate(
                predicate.type,
                predicate.location
            ) { predicate.lhv.map.call(predicate.callTerm) }
            else -> predicate(predicate.type, predicate.location) { call(predicate.callTerm) }
        }
    }

    override fun transformEqualityPredicate(predicate: EqualityPredicate): Predicate {
        val newLhv = predicate.lhv.map
        val newRhv = predicate.rhv.map
        return predicate(predicate.type, predicate.location) { newLhv equality newRhv }
    }

    override fun transformFieldInitializerPredicate(predicate: FieldInitializerPredicate): Predicate =
        predicate(predicate.type, predicate.location) { predicate.field.initialize(predicate.value.map) }


    override fun transformFieldStorePredicate(predicate: FieldStorePredicate): Predicate =
        predicate(predicate.type, predicate.location) { predicate.field.store(predicate.value.map) }

    override fun transformInequalityPredicate(predicate: InequalityPredicate): Predicate {
        val newLhv = predicate.lhv.map
        val newRhv = predicate.rhv.map
        return predicate(predicate.type, predicate.location) { newLhv inequality newRhv }
    }

    override fun transformBinaryTerm(term: BinaryTerm): Term =
        term { term.lhv.map.apply(term.type, term.opcode, term.rhv.map) }

    override fun transformCallTerm(term: CallTerm): Term {
        val args = term.arguments.map { it.map }
        val owner = term.owner.map
        return term { owner.call(term.method, args) }
    }

    override fun transformCastTerm(term: CastTerm): Term = term { term.operand.map `as` term.type }

    override fun transformCmpTerm(term: CmpTerm): Term =
        term { term.lhv.map.apply(term.opcode, term.rhv.map) }

    override fun transformFieldTerm(term: FieldTerm): Term {
        val owner = term.owner.map
        return term { owner.field(term.type as KexReference, term.fieldName) }
    }

    override fun transformInstanceOf(term: InstanceOfTerm): Term {
        return term { term.operand.map `is` term.checkedType }
    }

    override fun transformEquals(term: EqualsTerm): Term {
        return term { term.lhv.map equls term.rhv.map }
    }

    override fun transformLambdaTerm(term: LambdaTerm): Term {
        return term { lambda(term.type, term.parameters, transform(term.body)) }
    }
}

class TypeNameAdapter(
    val tf: TypeFactory
) : RecollectingTransformer<TypeNameAdapter> {
    override val builders = dequeOf(StateBuilder())

    override fun apply(ps: PredicateState): PredicateState {
        if (!hasClassAccesses(ps)) return ps

        val constStrings = getConstStringMap(ps)
        val strings = collectTypes(tf, ps)
            .map { it.unreferenced() }
            .map { term { const(it.javaName) } as ConstStringTerm }
            .toMutableSet()
        if (strings.isNotEmpty()) {
            strings += term { const(KexString().javaName) } as ConstStringTerm
            strings += term { const(KexChar().asArray().javaName) } as ConstStringTerm
            strings += term { const(KexChar().javaName) } as ConstStringTerm
            strings += term { const(KexInt().javaName) } as ConstStringTerm
        }

        for (str in strings.filter { it.value !in constStrings }) {
            currentBuilder += buildStr(str.value)
        }
        return super.apply(ps)
    }

    private fun buildStr(string: String): PredicateState = basic {
        val strTerm = generate(KexString())
        state { strTerm.new() }

        val charArray = KexArray(KexChar())
        val valueArray = generate(charArray)
        state { valueArray.new(string.length) }
        for ((index, char) in string.withIndex()) {
            state { valueArray[index].store(const(char)) }
        }

        state { strTerm.field(charArray, "value").store(valueArray) }
    }
}