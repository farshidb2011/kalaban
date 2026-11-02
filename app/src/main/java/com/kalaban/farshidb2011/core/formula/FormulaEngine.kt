package com.kalaban.farshidb2011.core.formula

import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.Numbers
import java.math.BigDecimal
import java.math.RoundingMode

enum class FormulaErrorCode {
    EMPTY,
    TOO_LONG,
    TOO_COMPLEX,
    UNEXPECTED_CHARACTER,
    UNEXPECTED_TOKEN,
    UNEXPECTED_END,
    UNBALANCED_PARENTHESES,
    UNKNOWN_VARIABLE,
    MISSING_VALUE,
    UNKNOWN_FUNCTION,
    WRONG_ARGUMENT_COUNT,
    EXPECTED_NUMBER,
    EXPECTED_CONDITION,
    DIVISION_BY_ZERO,
    VALUE_TOO_LARGE,
    INVALID_ARGUMENT,
}

data class FormulaError(val code: FormulaErrorCode, val position: Int, val detail: String? = null)

class FormulaException(val error: FormulaError) : RuntimeException("${error.code} at ${error.position}: ${error.detail}")

enum class ValueType { NUMBER, CONDITION }

sealed class Expr {
    abstract val pos: Int

    data class Num(val value: BigDecimal, override val pos: Int) : Expr()
    data class Var(val name: String, override val pos: Int) : Expr()
    data class Unary(val op: String, val operand: Expr, override val pos: Int) : Expr()
    data class Binary(val op: String, val left: Expr, val right: Expr, override val pos: Int) : Expr()
    data class Call(val name: String, val args: List<Expr>, override val pos: Int) : Expr()
}

/**
 * Sandboxed arithmetic language for pricing formulas: numbers, variables, + - * / ^, comparisons,
 * and/or/not, parentheses and a fixed set of functions. Expressions are parsed into an AST and
 * evaluated with [BigDecimal]; nothing user-supplied is ever executed as code.
 */
object FormulaEngine {
    const val MAX_LENGTH = 2000
    const val MAX_DEPTH = 64
    private const val MAX_EXPONENT = 10
    private val LIMIT = BigDecimal("1E18")

    private class Fn(val minArgs: Int, val maxArgs: Int, val conditionArg: Int = -1)

    private val functions = linkedMapOf(
        "min" to Fn(1, 20),
        "max" to Fn(1, 20),
        "abs" to Fn(1, 1),
        "round" to Fn(1, 2),
        "ceil" to Fn(1, 1),
        "floor" to Fn(1, 1),
        "roundto" to Fn(2, 2),
        "ceilto" to Fn(2, 2),
        "floorto" to Fn(2, 2),
        "clamp" to Fn(3, 3),
        "pct" to Fn(2, 2),
        "addpct" to Fn(2, 2),
        "subpct" to Fn(2, 2),
        "margin" to Fn(2, 2),
        "if" to Fn(3, 3, conditionArg = 0),
    )

    val functionNames: List<String> get() = functions.keys.toList()

    private val keywords = setOf("and", "or", "not")

    fun parse(source: String): Expr {
        if (source.isBlank()) throw FormulaException(FormulaError(FormulaErrorCode.EMPTY, 0))
        if (source.length > MAX_LENGTH) throw FormulaException(FormulaError(FormulaErrorCode.TOO_LONG, MAX_LENGTH))
        return Parser(lex(source)).parseAll()
    }

    /** Parses and type-checks; [variables] is the set of names a formula may reference. */
    fun compile(source: String, expected: ValueType, variables: Set<String>): Expr {
        val expr = parse(source)
        val actual = typeOf(expr, variables)
        if (actual != expected) {
            val code = if (expected == ValueType.NUMBER) FormulaErrorCode.EXPECTED_NUMBER else FormulaErrorCode.EXPECTED_CONDITION
            throw FormulaException(FormulaError(code, expr.pos))
        }
        return expr
    }

    fun validate(source: String, expected: ValueType, variables: Set<String>): FormulaError? =
        try {
            compile(source, expected, variables)
            null
        } catch (e: FormulaException) {
            e.error
        }

    fun evaluateNumber(expr: Expr, values: Map<String, BigDecimal>, known: Set<String> = values.keys): BigDecimal =
        Evaluator(values, known).number(expr)

    fun evaluateCondition(expr: Expr, values: Map<String, BigDecimal>, known: Set<String> = values.keys): Boolean =
        Evaluator(values, known).condition(expr)

    // ---- Lexer ----

    private enum class T { NUMBER, IDENT, OP, LPAREN, RPAREN, COMMA, EOF }

    private data class Token(val type: T, val text: String, val pos: Int)

    private fun lex(source: String): List<Token> {
        val s = Numbers.normalizeDigits(source)
        val tokens = ArrayList<Token>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c in '0'..'9' || (c == '.' && i + 1 < s.length && s[i + 1] in '0'..'9') -> {
                    val start = i
                    while (i < s.length && s[i] in '0'..'9') i++
                    if (i < s.length && s[i] == '.') {
                        i++
                        while (i < s.length && s[i] in '0'..'9') i++
                    }
                    tokens += Token(T.NUMBER, s.substring(start, i), start)
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < s.length && (s[i].isLetterOrDigit() || s[i] == '_')) i++
                    tokens += Token(T.IDENT, s.substring(start, i).lowercase(), start)
                }
                c == '(' -> tokens += Token(T.LPAREN, "(", i++)
                c == ')' -> tokens += Token(T.RPAREN, ")", i++)
                c == ',' || c == ';' -> tokens += Token(T.COMMA, ",", i++)
                else -> {
                    val two = if (i + 1 < s.length) s.substring(i, i + 2) else ""
                    when {
                        two == "<=" || two == ">=" || two == "==" || two == "!=" -> {
                            tokens += Token(T.OP, two, i)
                            i += 2
                        }
                        two == "&&" -> { tokens += Token(T.OP, "and", i); i += 2 }
                        two == "||" -> { tokens += Token(T.OP, "or", i); i += 2 }
                        c == '×' -> tokens += Token(T.OP, "*", i++)
                        c == '÷' -> tokens += Token(T.OP, "/", i++)
                        c == '=' -> tokens += Token(T.OP, "==", i++)
                        c == '!' -> tokens += Token(T.OP, "not", i++)
                        c in "+-*/^<>" -> tokens += Token(T.OP, c.toString(), i++)
                        else -> throw FormulaException(FormulaError(FormulaErrorCode.UNEXPECTED_CHARACTER, i, c.toString()))
                    }
                }
            }
            if (tokens.size > MAX_LENGTH) throw FormulaException(FormulaError(FormulaErrorCode.TOO_LONG, i))
        }
        tokens += Token(T.EOF, "", s.length)
        return tokens
    }

    // ---- Parser (recursive descent, precedence low → high: or, and, not, comparison, + -, * /, unary, ^) ----

    private class Parser(private val tokens: List<Token>) {
        private var p = 0
        private var depth = 0

        fun parseAll(): Expr {
            val e = parseOr()
            if (peek().type != T.EOF) unexpected(peek())
            return e
        }

        private fun peek() = tokens[p]
        private fun next() = tokens[p++]

        private fun <R> nested(block: () -> R): R {
            if (++depth > MAX_DEPTH) throw FormulaException(FormulaError(FormulaErrorCode.TOO_COMPLEX, peek().pos))
            try {
                return block()
            } finally {
                depth--
            }
        }

        private fun at(vararg ops: String): Boolean {
            val t = peek()
            return (t.type == T.OP || t.type == T.IDENT) && t.text in ops
        }

        private fun unexpected(t: Token): Nothing {
            val code = if (t.type == T.EOF) FormulaErrorCode.UNEXPECTED_END else FormulaErrorCode.UNEXPECTED_TOKEN
            throw FormulaException(FormulaError(code, t.pos, t.text))
        }

        private fun expectClose() {
            val t = peek()
            if (t.type == T.RPAREN) {
                next()
                return
            }
            if (t.type == T.EOF) throw FormulaException(FormulaError(FormulaErrorCode.UNBALANCED_PARENTHESES, t.pos))
            unexpected(t)
        }

        private fun parseOr(): Expr = nested {
            var left = parseAnd()
            while (at("or")) {
                val t = next()
                left = Expr.Binary("or", left, parseAnd(), t.pos)
            }
            left
        }

        private fun parseAnd(): Expr {
            var left = parseNot()
            while (at("and")) {
                val t = next()
                left = Expr.Binary("and", left, parseNot(), t.pos)
            }
            return left
        }

        private fun parseNot(): Expr = nested {
            if (at("not")) {
                val t = next()
                Expr.Unary("not", parseNot(), t.pos)
            } else {
                parseComparison()
            }
        }

        private fun parseComparison(): Expr {
            val left = parseAdditive()
            if (at("<", "<=", ">", ">=", "==", "!=")) {
                val t = next()
                return Expr.Binary(t.text, left, parseAdditive(), t.pos)
            }
            return left
        }

        private fun parseAdditive(): Expr {
            var left = parseTerm()
            while (peek().type == T.OP && peek().text in setOf("+", "-")) {
                val t = next()
                left = Expr.Binary(t.text, left, parseTerm(), t.pos)
            }
            return left
        }

        private fun parseTerm(): Expr {
            var left = parseUnary()
            while (peek().type == T.OP && peek().text in setOf("*", "/")) {
                val t = next()
                left = Expr.Binary(t.text, left, parseUnary(), t.pos)
            }
            return left
        }

        private fun parseUnary(): Expr = nested {
            val t = peek()
            if (t.type == T.OP && (t.text == "-" || t.text == "+")) {
                next()
                Expr.Unary(t.text, parseUnary(), t.pos)
            } else {
                parsePower()
            }
        }

        private fun parsePower(): Expr {
            val base = parsePrimary()
            if (peek().type == T.OP && peek().text == "^") {
                val t = next()
                return Expr.Binary("^", base, parseUnary(), t.pos)
            }
            return base
        }

        private fun parsePrimary(): Expr {
            val t = peek()
            return when (t.type) {
                T.NUMBER -> {
                    next()
                    Expr.Num(BigDecimal(t.text), t.pos)
                }
                T.IDENT -> {
                    if (t.text in keywords) unexpected(t)
                    next()
                    if (peek().type == T.LPAREN) {
                        next()
                        val args = ArrayList<Expr>()
                        if (peek().type != T.RPAREN) {
                            args += parseOr()
                            while (peek().type == T.COMMA) {
                                next()
                                args += parseOr()
                            }
                        }
                        expectClose()
                        Expr.Call(t.text, args, t.pos)
                    } else {
                        Expr.Var(t.text, t.pos)
                    }
                }
                T.LPAREN -> {
                    next()
                    val e = parseOr()
                    expectClose()
                    e
                }
                else -> unexpected(t)
            }
        }
    }

    // ---- Static type check ----

    private fun typeOf(e: Expr, variables: Set<String>): ValueType = when (e) {
        is Expr.Num -> ValueType.NUMBER
        is Expr.Var -> {
            if (e.name !in variables) throw FormulaException(FormulaError(FormulaErrorCode.UNKNOWN_VARIABLE, e.pos, e.name))
            ValueType.NUMBER
        }
        is Expr.Unary -> if (e.op == "not") {
            require(e.operand, ValueType.CONDITION, variables)
            ValueType.CONDITION
        } else {
            require(e.operand, ValueType.NUMBER, variables)
            ValueType.NUMBER
        }
        is Expr.Binary -> when (e.op) {
            "and", "or" -> {
                require(e.left, ValueType.CONDITION, variables)
                require(e.right, ValueType.CONDITION, variables)
                ValueType.CONDITION
            }
            "<", "<=", ">", ">=", "==", "!=" -> {
                require(e.left, ValueType.NUMBER, variables)
                require(e.right, ValueType.NUMBER, variables)
                ValueType.CONDITION
            }
            else -> {
                require(e.left, ValueType.NUMBER, variables)
                require(e.right, ValueType.NUMBER, variables)
                ValueType.NUMBER
            }
        }
        is Expr.Call -> {
            val fn = functions[e.name]
                ?: throw FormulaException(FormulaError(FormulaErrorCode.UNKNOWN_FUNCTION, e.pos, e.name))
            if (e.args.size !in fn.minArgs..fn.maxArgs) {
                throw FormulaException(FormulaError(FormulaErrorCode.WRONG_ARGUMENT_COUNT, e.pos, e.name))
            }
            e.args.forEachIndexed { index, arg ->
                require(arg, if (index == fn.conditionArg) ValueType.CONDITION else ValueType.NUMBER, variables)
            }
            ValueType.NUMBER
        }
    }

    private fun require(e: Expr, type: ValueType, variables: Set<String>) {
        if (typeOf(e, variables) != type) {
            val code = if (type == ValueType.NUMBER) FormulaErrorCode.EXPECTED_NUMBER else FormulaErrorCode.EXPECTED_CONDITION
            throw FormulaException(FormulaError(code, e.pos))
        }
    }

    // ---- Evaluation ----

    private class Evaluator(private val values: Map<String, BigDecimal>, private val known: Set<String>) {
        fun number(e: Expr): BigDecimal = when (e) {
            is Expr.Num -> e.value
            is Expr.Var -> values[e.name] ?: throw FormulaException(
                FormulaError(
                    if (e.name in known) FormulaErrorCode.MISSING_VALUE else FormulaErrorCode.UNKNOWN_VARIABLE,
                    e.pos,
                    e.name,
                ),
            )
            is Expr.Unary -> when (e.op) {
                "-" -> number(e.operand).negate()
                "+" -> number(e.operand)
                else -> throw FormulaException(FormulaError(FormulaErrorCode.EXPECTED_NUMBER, e.pos))
            }
            is Expr.Binary -> checked(binary(e), e.pos)
            is Expr.Call -> checked(call(e), e.pos)
        }

        fun condition(e: Expr): Boolean = when {
            e is Expr.Unary && e.op == "not" -> !condition(e.operand)
            e is Expr.Binary && e.op == "and" -> condition(e.left) && condition(e.right)
            e is Expr.Binary && e.op == "or" -> condition(e.left) || condition(e.right)
            e is Expr.Binary && e.op in setOf("<", "<=", ">", ">=", "==", "!=") -> {
                val c = number(e.left).compareTo(number(e.right))
                when (e.op) {
                    "<" -> c < 0
                    "<=" -> c <= 0
                    ">" -> c > 0
                    ">=" -> c >= 0
                    "==" -> c == 0
                    else -> c != 0
                }
            }
            else -> throw FormulaException(FormulaError(FormulaErrorCode.EXPECTED_CONDITION, e.pos))
        }

        private fun checked(v: BigDecimal, pos: Int): BigDecimal {
            if (v.abs() > LIMIT) throw FormulaException(FormulaError(FormulaErrorCode.VALUE_TOO_LARGE, pos))
            return v
        }

        private fun binary(e: Expr.Binary): BigDecimal {
            val l = number(e.left)
            val r = number(e.right)
            return when (e.op) {
                "+" -> l.add(r, Money.MC)
                "-" -> l.subtract(r, Money.MC)
                "*" -> l.multiply(r, Money.MC)
                "/" -> {
                    if (r.signum() == 0) throw FormulaException(FormulaError(FormulaErrorCode.DIVISION_BY_ZERO, e.pos))
                    l.divide(r, Money.MC)
                }
                "^" -> power(l, r, e.pos)
                else -> throw FormulaException(FormulaError(FormulaErrorCode.EXPECTED_NUMBER, e.pos))
            }
        }

        private fun power(base: BigDecimal, exponent: BigDecimal, pos: Int): BigDecimal {
            val n = runCatching { exponent.stripTrailingZeros().intValueExact() }.getOrNull()
            if (n == null || n !in -MAX_EXPONENT..MAX_EXPONENT) {
                throw FormulaException(FormulaError(FormulaErrorCode.INVALID_ARGUMENT, pos, "^"))
            }
            if (base.signum() == 0 && n < 0) throw FormulaException(FormulaError(FormulaErrorCode.DIVISION_BY_ZERO, pos))
            return base.pow(n, Money.MC)
        }

        private fun step(e: Expr.Call): BigDecimal {
            val step = number(e.args[1])
            if (step.signum() <= 0) throw FormulaException(FormulaError(FormulaErrorCode.INVALID_ARGUMENT, e.pos, e.name))
            return step
        }

        private fun percent(e: Expr): BigDecimal = number(e).divide(Money.HUNDRED, Money.MC)

        private fun call(e: Expr.Call): BigDecimal = when (e.name) {
            "if" -> if (condition(e.args[0])) number(e.args[1]) else number(e.args[2])
            "min" -> e.args.map { number(it) }.minOrNull()!!
            "max" -> e.args.map { number(it) }.maxOrNull()!!
            "abs" -> number(e.args[0]).abs()
            "round" -> {
                val digits = if (e.args.size > 1) {
                    runCatching { number(e.args[1]).stripTrailingZeros().intValueExact() }.getOrNull()
                        ?.takeIf { it in -12..10 }
                        ?: throw FormulaException(FormulaError(FormulaErrorCode.INVALID_ARGUMENT, e.pos, e.name))
                } else {
                    0
                }
                number(e.args[0]).setScale(digits, RoundingMode.HALF_UP)
            }
            "ceil" -> number(e.args[0]).setScale(0, RoundingMode.CEILING)
            "floor" -> number(e.args[0]).setScale(0, RoundingMode.FLOOR)
            "roundto" -> step(e).let { s -> number(e.args[0]).divide(s, 0, RoundingMode.HALF_UP).multiply(s) }
            "ceilto" -> step(e).let { s -> number(e.args[0]).divide(s, 0, RoundingMode.CEILING).multiply(s) }
            "floorto" -> step(e).let { s -> number(e.args[0]).divide(s, 0, RoundingMode.FLOOR).multiply(s) }
            "clamp" -> {
                val x = number(e.args[0])
                val lo = number(e.args[1])
                val hi = number(e.args[2])
                if (lo > hi) throw FormulaException(FormulaError(FormulaErrorCode.INVALID_ARGUMENT, e.pos, e.name))
                x.max(lo).min(hi)
            }
            "pct" -> number(e.args[0]).multiply(percent(e.args[1]), Money.MC)
            "addpct" -> number(e.args[0]).multiply(BigDecimal.ONE.add(percent(e.args[1])), Money.MC)
            "subpct" -> number(e.args[0]).multiply(BigDecimal.ONE.subtract(percent(e.args[1])), Money.MC)
            "margin" -> {
                val keep = BigDecimal.ONE.subtract(percent(e.args[1]))
                if (keep.signum() <= 0) throw FormulaException(FormulaError(FormulaErrorCode.INVALID_ARGUMENT, e.pos, e.name))
                number(e.args[0]).divide(keep, Money.MC)
            }
            else -> throw FormulaException(FormulaError(FormulaErrorCode.UNKNOWN_FUNCTION, e.pos, e.name))
        }
    }
}
