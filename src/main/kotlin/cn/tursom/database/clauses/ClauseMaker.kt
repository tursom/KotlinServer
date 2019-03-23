package cn.tursom.database.clauses

import cn.tursom.regex.RegexMaker
import cn.tursom.regex.RegexUnit
import java.lang.reflect.Field
import kotlin.reflect.KProperty

object ClauseMaker {
	infix fun Clause.and(clause: Clause) = AndClause(this, clause)
	
	infix fun String.equal(value: String) = EqualClause(this, value)
	infix fun Field.equal(value: String) = EqualClause(this, value)
	infix fun KProperty<*>.equal(value: String) = EqualClause(this, value)
	
	infix fun String.greateEqual(value: String) = GreaterEqualClause(this, value)
	infix fun Field.greateEqual(value: String) = GreaterEqualClause(this, value)
	infix fun KProperty<*>.greateEqual(value: String) = GreaterEqualClause(this, value)
	
	infix fun String.greaterThan(value: String) = GreaterThanClause(this, value)
	infix fun Field.greaterThan(value: String) = GreaterThanClause(this, value)
	infix fun KProperty<*>.greaterThan(value: String) = GreaterThanClause(this, value)
	
	infix fun String.lessEqual(value: String) = LessEqualClause(this, value)
	infix fun Field.lessEqual(value: String) = LessEqualClause(this, value)
	infix fun KProperty<*>.lessEqual(value: String) = LessEqualClause(this, value)
	
	infix fun String.lessThan(value: String) = LessThanClause(this, value)
	infix fun Field.lessThan(value: String) = LessThanClause(this, value)
	infix fun KProperty<*>.lessThan(value: String) = LessThanClause(this, value)
	
	infix fun String.like(value: String) = LikeClause(this, value)
	infix fun Field.like(value: String) = LikeClause(this, value)
	infix fun KProperty<*>.like(value: String) = LikeClause(this, value)
	
	infix fun String.like(value: LikeWildcard.() -> String) = LikeClause(this, value)
	infix fun Field.like(value: LikeWildcard.() -> String) = LikeClause(this, value)
	infix fun KProperty<*>.like(value: LikeWildcard.() -> String) = LikeClause(this, value)
	
	operator fun Clause.not() = NotClause(this)
	
	infix fun String.notEqual(value: String) = NotEqualClause(this, value)
	infix fun Field.notEqual(value: String) = NotEqualClause(this, value)
	infix fun KProperty<*>.notEqual(value: String) = NotEqualClause(this, value)
	
	infix fun Clause.or(value: Clause) = OrClause(this, value)
	
	infix fun String.regexp(value: String) = RegexpClause(this, value)
	infix fun Field.regexp(value: String) = RegexpClause(this, value)
	infix fun KProperty<*>.regexp(value: String) = RegexpClause(this, value)
	
	infix fun String.regexp(value: Regex) = RegexpClause(this, value)
	infix fun Field.regexp(value: Regex) = RegexpClause(this, value)
	infix fun KProperty<*>.regexp(value: Regex) = RegexpClause(this, value)
	
	infix fun String.regexp(value: RegexUnit) = RegexpClause(this, value)
	infix fun Field.regexp(value: RegexUnit) = RegexpClause(this, value)
	infix fun KProperty<*>.regexp(value: RegexUnit) = RegexpClause(this, value)
	
	infix fun String.regexp(value: RegexMaker.() -> RegexUnit) = RegexpClause(this, value)
	infix fun Field.regexp(value: RegexMaker.() -> RegexUnit) = RegexpClause(this, value)
	infix fun KProperty<*>.regexp(value: RegexMaker.() -> RegexUnit) = RegexpClause(this, value)
	
	@Suppress("UNUSED_EXPRESSION")
	fun make(maker: ClauseMaker.() -> Clause) = maker()
}