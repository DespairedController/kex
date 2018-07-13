package org.jetbrains.research.kex.state.transformer

import org.jetbrains.research.kex.state.predicate.ArrayStorePredicate
import org.jetbrains.research.kex.state.predicate.FieldStorePredicate
import org.jetbrains.research.kex.state.predicate.Predicate
import org.jetbrains.research.kex.state.term.*

class PointerCollector : Transformer<PointerCollector> {
    val ptrs = mutableSetOf<Term>()

    override fun transformArrayStorePredicate(predicate: ArrayStorePredicate): Predicate {
        ptrs.add(predicate.getArrayRef())
        return predicate
    }

    override fun transformFieldStorePredicate(predicate: FieldStorePredicate): Predicate {
        ptrs.add(predicate.getOwner())
        return predicate
    }

    override fun transformArrayLoadTerm(term: ArrayLoadTerm): Term {
        ptrs.add(term.getArrayRef())
        return term
    }

    override fun transformArrayLengthTerm(term: ArrayLengthTerm): Term {
        ptrs.add(term.getArrayRef())
        return term
    }

    override fun transformFieldLoadTerm(term: FieldLoadTerm): Term {
        ptrs.add(term.getOwner())
        return term
    }

    override fun transformConstClassTerm(term: ConstClassTerm): Term {
        ptrs.add(term)
        return term
    }
}