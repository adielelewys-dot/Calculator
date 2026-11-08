package com.example.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat

/** Safe recursive-descent evaluator: + - * / unary minus, postfix %. */
object Evaluator {
    fun evaluate(s: String): Double {
        var i = 0
        fun peek(): Char? = s.getOrNull(i)

        fun num(): Double {
            val st = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            require(st != i)
            var v = s.substring(st, i).toDouble()
            while (peek() == '%') { i++; v /= 100 }
            return v
        }

        fun unary(): Double = when (peek()) {
            '-' -> { i++; -unary() }
            '+' -> { i++; unary() }
            else -> num()
        }

        fun term(): Double {
            var v = unary()
            while (peek() == '*' || peek() == '/') {
                val o = s[i++]
                val r = unary()
                v = if (o == '*') v * r else v / r
            }
            return v
        }

        fun sum(): Double {
            var v = term()
            while (peek() == '+' || peek() == '-') {
                val o = s[i++]
                val r = term()
                v = if (o == '+') v + r else v - r
            }
            return v
        }

        val out = sum()
        require(i == s.length)
        return out
    }

    fun format(n: Double): String {
        if (n.isNaN() || n.isInfinite()) return "Can't divide by 0"
        val rounded = BigDecimal(n).round(MathContext(12))
        return DecimalFormat("#,##0.############").format(rounded)
    }

    fun raw(n: Double): String =
        BigDecimal(n).round(MathContext(12)).stripTrailingZeros().toPlainString()
}

class CalcState {
    var expr by mutableStateOf("")
        private set
    var history by mutableStateOf("")
        private set
    private var justEvaluated = false

    val display: String
        get() = pretty(expr)

    val result: String
        get() {
            if (expr.isEmpty()) return "0"
            val t = expr.trimEnd('+', '-', '*', '/', '.')
            if (t.isEmpty()) return "0"
            return try { Evaluator.format(Evaluator.evaluate(t)) } catch (e: Exception) { "0" }
        }

    private fun pretty(s: String): String =
        s.replace("*", " × ").replace("/", " ÷ ")
            .replace(Regex("(?<=[0-9%.])\\+"), " + ")
            .replace(Regex("(?<=[0-9%.])-"), " − ")
            .replace("-", "−").trim()

    private fun lastNumber() = Regex("[0-9.]*%*$").find(expr)!!.value

    fun press(k: String) {
        when (k) {
            "AC" -> { expr = ""; history = ""; justEvaluated = false }
            "DEL" -> { expr = expr.dropLast(1); history = ""; justEvaluated = false }
            "=" -> {
                if (expr.isEmpty()) return
                val t = expr.trimEnd('+', '-', '*', '/', '.')
                try {
                    val v = Evaluator.evaluate(t)
                    if (v.isNaN() || v.isInfinite()) { history = pretty(t); expr = ""; return }
                    history = pretty(t) + " ="
                    expr = Evaluator.raw(v)
                    justEvaluated = true
                } catch (_: Exception) { }
            }
            "NEG" -> {
                val ln = lastNumber()
                if (ln.isEmpty()) {
                    if (expr.isEmpty() || expr.last() in "+*/") expr += "-"
                    else if (expr.endsWith("-")) expr = expr.dropLast(1) + "+"
                } else {
                    val head = expr.dropLast(ln.length)
                    expr = when {
                        head == "-" -> ln
                        head.endsWith("-") && (head.length == 1 || head[head.length - 2] in "+*/") ->
                            head.dropLast(1) + ln
                        else -> "$head-$ln"
                    }
                }
                justEvaluated = false
            }
            "+", "-", "*", "/" -> {
                justEvaluated = false
                when {
                    expr.isEmpty() -> if (k == "-") expr = "-"
                    expr == "-" -> {}
                    expr.last() in "+-*/" ->
                        expr = if (k == "-" && expr.last() in "*/") expr + "-"
                        else expr.trimEnd('+', '-', '*', '/') + k
                    else -> expr += k
                }
            }
            "%" -> if (expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == '.' || expr.last() == '%')) {
                expr += "%"; justEvaluated = false
            }
            "." -> {
                if (justEvaluated) { expr = "0."; justEvaluated = false; return }
                val ln = lastNumber()
                if ('.' in ln || ln.endsWith("%")) return
                expr += if (ln.isEmpty()) "0." else "."
            }
            else -> { // digit
                if (justEvaluated) { expr = ""; justEvaluated = false; history = "" }
                if (expr.endsWith("%")) return
                val cur = lastNumber()
                if (cur == "0" && k == "0") return
                if (cur == "0") expr = expr.dropLast(1)
                if (cur.replace(".", "").length >= 15) return
                expr += k
            }
        }
    }
}
