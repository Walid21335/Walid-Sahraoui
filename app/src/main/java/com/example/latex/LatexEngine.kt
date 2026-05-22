package com.example.latex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

// Represents parsed fragments of a textbook paragraph (mixing Arabic text and LaTeX formula blocks)
sealed class LatexElement {
    data class TextBlock(val text: String) : LatexElement()
    data class EquationBlock(val rawFormula: String, val isDisplayMode: Boolean) : LatexElement()
}

object LatexEngine {

    // Prepopulated list of LaTeX examples for teachers/students
    val latexExamples = listOf(
        Pair("دالة تربيعية", "f(x) = x^2 - 5x + 6 = 0"),
        Pair("تكامل محدد", "\\int_{a}^{b} f(x) \\, dx = F(b) - F(a)"),
        Pair("جذر وقوى", "E = \\sqrt{m^2 c^4 + p^2 c^2}"),
        Pair("نسبية أينشتاين", "E = m c^2"),
        Pair("كسر اعتيادي", "\\frac{\\Delta y}{\\Delta x} = \\lim_{\\Delta x \\to 0} \\frac{f(x + \\Delta x) - f(x)}{\\Delta x}"),
        Pair("مصفوفة ثنائية", "A = \\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}"),
        Pair("علاقة كهرومغناطيسية", "B = \\mu_0 \\frac{N I}{L}")
    )

    // Substitute standard LaTeX symbol commands with beautiful unicode math symbols
    fun substituteMathSymbols(raw: String): String {
        var text = raw
            .replace("\\Delta", "Δ")
            .replace("\\alpha", "α")
            .replace("\\beta", "β")
            .replace("\\gamma", "γ")
            .replace("\\theta", "θ")
            .replace("\\lambda", "λ")
            .replace("\\pi", "π")
            .replace("\\mu_0", "μ₀")
            .replace("\\mu", "μ")
            .replace("\\sigma", "σ")
            .replace("\\phi", "φ")
            .replace("\\omega", "ω")
            .replace("\\to", " → ")
            .replace("\\infty", "∞")
            .replace("\\sin", "sin")
            .replace("\\cos", "cos")
            .replace("\\tan", "tan")
            .replace("\\det", "det")
            .replace("\\approx", "≈")
            .replace("\\cdot", "·")
            .replace("\\, ", " ")
            .replace("\\\\", "\n")
            .trim()
        
        return text
    }

    // Parses a line of text that might contain LaTeX into clean fragments
    fun parseParagraph(input: String): List<LatexElement> {
        val elements = mutableListOf<LatexElement>()
        if (input.isEmpty()) return elements

        // Patterns to extract display equations \[ ... \] or $$ ... $$
        // and inline equations $ ... $
        val pattern = Pattern.compile("(?s)\\$\\$(.*?)\\$\\$|\\\\\\[(.*?)\\\\\\]|\\$(.*?)\\$")
        val matcher = pattern.matcher(input)

        var lastIdx = 0
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            // Preceding standard text block
            if (start > lastIdx) {
                elements.add(LatexElement.TextBlock(input.substring(lastIdx, start)))
            }

            // Extract the matching groups
            val displayDoubleDollar = matcher.group(1)
            val displayBracket = matcher.group(2)
            val inlineDollar = matcher.group(3)

            when {
                displayDoubleDollar != null -> {
                    elements.add(LatexElement.EquationBlock(displayDoubleDollar, isDisplayMode = true))
                }
                displayBracket != null -> {
                    elements.add(LatexElement.EquationBlock(displayBracket, isDisplayMode = true))
                }
                inlineDollar != null -> {
                    elements.add(LatexElement.EquationBlock(inlineDollar, isDisplayMode = false))
                }
            }
            lastIdx = end
        }

        if (lastIdx < input.length) {
            elements.add(LatexElement.TextBlock(input.substring(lastIdx)))
        }

        return elements
    }
}

@Composable
fun LatexText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Float = 16f,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val parsedElements = LatexEngine.parseParagraph(text)

    // Using Flow Row or a beautiful Column to present Arabic prose and math blocks side-by-side or stacked
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        parsedElements.forEach { element ->
            when (element) {
                is LatexElement.TextBlock -> {
                    // Standard Arabic text
                    Text(
                        text = element.text,
                        fontSize = fontSize.sp,
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        fontFamily = FontFamily.SansSerif
                    )
                }
                is LatexElement.EquationBlock -> {
                    // LaTeX Formula Block
                    EquationRenderer(
                        formula = element.rawFormula,
                        isDisplay = element.isDisplayMode,
                        fontSize = fontSize,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun EquationRenderer(
    formula: String,
    isDisplay: Boolean,
    fontSize: Float,
    accentColor: Color
) {
    // Elegant container for display and inline equations
    Box(
        modifier = Modifier
            .run {
                if (isDisplay) {
                    fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                } else {
                    padding(horizontal = 4.dp)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Typesetting formula
        ExtendedMathLayout(formula = formula, fontSize = fontSize, accentColor = accentColor)
    }
}

// Custom typesetter for common elements
@Composable
fun ExtendedMathLayout(
    formula: String,
    fontSize: Float,
    accentColor: Color
) {
    // Detect structured mathematical objects like \frac, \sqrt, \begin{pmatrix}
    val cleanFormula = formula.trim()

    when {
        cleanFormula.contains("\\frac") -> {
            RenderFraction(cleanFormula, fontSize, accentColor)
        }
        cleanFormula.contains("\\sqrt") -> {
            RenderSquareRoot(cleanFormula, fontSize, accentColor)
        }
        cleanFormula.contains("\\begin{pmatrix}") -> {
            RenderMatrix(cleanFormula, fontSize, accentColor)
        }
        cleanFormula.contains("\\int") -> {
            RenderIntegral(cleanFormula, fontSize, accentColor)
        }
        else -> {
            // Standard equation style with exponents and subscripts
            RenderSimpleEquation(cleanFormula, fontSize, accentColor)
        }
    }
}

// Renders a vertically stacked fraction \frac{Numerator}{Denominator}
@Composable
fun RenderFraction(formula: String, baseFontSize: Float, accentColor: Color) {
    // Simple regex parser for \frac{num}{den}
    // E.g. \frac{\Delta y}{\Delta x} or \frac{1}{2}
    val parts = extractCurlyBracketsContent(formula, "\\frac")
    if (parts.size >= 2) {
        val numerator = parts[0]
        val denominator = parts[1]

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Numerator
                ExtendedMathLayout(formula = numerator, fontSize = baseFontSize * 0.85f, accentColor = accentColor)
                
                // Fraction bar
                Canvas(modifier = Modifier
                    .width(70.dp)
                    .height(2.dp)
                    .padding(vertical = 1.dp)) {
                    drawLine(
                        color = accentColor.copy(alpha = 0.8f),
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                
                // Denominator
                ExtendedMathLayout(formula = denominator, fontSize = baseFontSize * 0.85f, accentColor = accentColor)
            }
        }
    } else {
        // Fallback
        RenderSimpleEquation(formula, baseFontSize, accentColor)
    }
}

// Renders \sqrt{expression} with a custom radical symbol and a horizontal overline
@Composable
fun RenderSquareRoot(formula: String, baseFontSize: Float, accentColor: Color) {
    val parts = extractCurlyBracketsContent(formula, "\\sqrt")
    val inner = if (parts.isNotEmpty()) parts[0] else formula.replace("\\sqrt", "")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        // Radical symbol √
        Text(
            text = "√",
            fontSize = (baseFontSize * 1.3f).sp,
            color = accentColor,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Serif
        )
        // Expression inside square root with a top border to simulate radical bar
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .background(Color.Transparent)
        ) {
            Column {
                // Radical Bar simulated
                Canvas(modifier = Modifier
                    .fillMaxWidth(0.12f)
                    .height(1.dp)) {
                    drawLine(
                        color = accentColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
                ExtendedMathLayout(formula = inner, fontSize = baseFontSize, accentColor = accentColor)
            }
        }
    }
}

// Renders an integral with limit values: \int_{lower}^{upper} f(x)
@Composable
fun RenderIntegral(formula: String, baseFontSize: Float, accentColor: Color) {
    // Estimate bounds \int_{lower}^{upper} or \int_a^b
    var lowerBound = ""
    var upperBound = ""
    
    // Simplistic integration parsing for demo parameters
    val lowerMatcher = Pattern.compile("_\\{(.*?)\\}|_([a-zA-Z0-9])").matcher(formula)
    if (lowerMatcher.find()) {
        lowerBound = lowerMatcher.group(1) ?: lowerMatcher.group(2) ?: ""
    }
    
    val upperMatcher = Pattern.compile("\\^\\{(.*?)\\}|\\^([a-zA-Z0-9])").matcher(formula)
    if (upperMatcher.find()) {
        upperBound = upperMatcher.group(1) ?: upperMatcher.group(2) ?: ""
    }

    // Clean equation by removing limits and symbol to render the body of integrand
    var body = formula.replace("\\int", "")
    if (lowerBound.isNotEmpty()) body = body.replace("_\\{$lowerBound\\}", "").replace("_$lowerBound", "")
    if (upperBound.isNotEmpty()) body = body.replace("\\^{$upperBound\\}", "").replace("^$upperBound", "")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        // Build the ∫ symbol column with sub/superscripts stacked
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Large ∫ symbol
                Text(
                    text = "∫",
                    fontSize = (baseFontSize * 1.5f).sp,
                    color = accentColor,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Serif
                )
                
                // Stack of bounds
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxHeight(0.05f)
                ) {
                    if (upperBound.isNotEmpty()) {
                        Text(
                            text = LatexEngine.substituteMathSymbols(upperBound),
                            fontSize = (baseFontSize * 0.65f).sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.offset(y = (-4).dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (lowerBound.isNotEmpty()) {
                        Text(
                            text = LatexEngine.substituteMathSymbols(lowerBound),
                            fontSize = (baseFontSize * 0.65f).sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.offset(y = 4.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        RenderSimpleEquation(body, baseFontSize, accentColor)
    }
}

// Renders matrix elements like \begin{pmatrix} a & b \\ c & d \end{pmatrix}
@Composable
fun RenderMatrix(formula: String, baseFontSize: Float, accentColor: Color) {
    // Extract everything between \begin{pmatrix} and \end{pmatrix}
    val contentStart = formula.indexOf("\\begin{pmatrix}") + "\\begin{pmatrix}".length
    val contentEnd = formula.indexOf("\\end{pmatrix}")
    if (contentStart in 0 until contentEnd) {
        val matrixData = formula.substring(contentStart, contentEnd).trim()
        val rows = matrixData.split("\\\\", "\n")

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .background(Color.Transparent)
        ) {
            // Left Bracket Parenthesis
            Text(
                text = "(",
                fontSize = (baseFontSize * (rows.size + 1)).sp,
                fontWeight = FontWeight.ExtraLight,
                color = accentColor,
                fontFamily = FontFamily.Serif
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                rows.forEach { row ->
                    val columns = row.split("&")
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        columns.forEach { cell ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                RenderSimpleEquation(cell.trim(), baseFontSize, accentColor)
                            }
                        }
                    }
                }
            }

            // Right Bracket Parenthesis
            Text(
                text = ")",
                fontSize = (baseFontSize * (rows.size + 1)).sp,
                fontWeight = FontWeight.ExtraLight,
                color = accentColor,
                fontFamily = FontFamily.Serif
            )
        }
    } else {
        RenderSimpleEquation(formula, baseFontSize, accentColor)
    }
}

// Low level rendering engine with sub/superscripts on simple equations
@Composable
fun RenderSimpleEquation(formula: String, baseFontSize: Float, accentColor: Color) {
    val cleanText = LatexEngine.substituteMathSymbols(formula)

    // Parse ^ (power) and _ (subscript)
    val annotatedString = buildAnnotatedString {
        var i = 0
        while (i < cleanText.length) {
            val char = cleanText[i]
            when {
                char == '^' -> {
                    // Check if it has curly braces ^{...}
                    if (i + 1 < cleanText.length && cleanText[i + 1] == '{') {
                        val endCurly = cleanText.indexOf('}', i + 2)
                        if (endCurly != -1) {
                            val powerVal = cleanText.substring(i + 2, endCurly)
                            withStyle(SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript, fontSize = (baseFontSize * 0.7f).sp, fontStyle = FontStyle.Normal, fontWeight = FontWeight.SemiBold)) {
                                append(powerVal)
                            }
                            i = endCurly + 1
                        } else {
                            i++
                        }
                    } else if (i + 1 < cleanText.length) {
                        // Single char power
                        val powerVal = cleanText[i + 1].toString()
                        withStyle(SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript, fontSize = (baseFontSize * 0.7f).sp, fontStyle = FontStyle.Normal, fontWeight = FontWeight.SemiBold)) {
                            append(powerVal)
                        }
                        i += 2
                    } else {
                        i++
                    }
                }
                char == '_' -> {
                    // Check if it has curly braces _{...}
                    if (i + 1 < cleanText.length && cleanText[i + 1] == '{') {
                        val endCurly = cleanText.indexOf('}', i + 2)
                        if (endCurly != -1) {
                            val subVal = cleanText.substring(i + 2, endCurly)
                            withStyle(SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift.Subscript, fontSize = (baseFontSize * 0.7f).sp)) {
                                append(subVal)
                            }
                            i = endCurly + 1
                        } else {
                            i++
                        }
                    } else if (i + 1 < cleanText.length) {
                        // Single char subscript
                        val subVal = cleanText[i + 1].toString()
                        withStyle(SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift.Subscript, fontSize = (baseFontSize * 0.7f).sp)) {
                            append(subVal)
                        }
                        i += 2
                    } else {
                        i++
                    }
                }
                else -> {
                    // Standard equation characters in nice Serif/Math formatting
                    val isItalic = char.isLetter() && !cleanText.substring(maxOf(0, i-3), minOf(cleanText.length, i+4)).contains("sin") && !cleanText.substring(maxOf(0, i-3), minOf(cleanText.length, i+4)).contains("cos") && !cleanText.substring(maxOf(0, i-3), minOf(cleanText.length, i+4)).contains("tan") && !cleanText.substring(maxOf(0, i-3), minOf(cleanText.length, i+4)).contains("det")
                    
                    withStyle(
                        SpanStyle(
                            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                            fontFamily = FontFamily.Serif,
                            fontSize = baseFontSize.sp,
                            fontWeight = if (isItalic) FontWeight.Medium else FontWeight.Normal
                        )
                    ) {
                        append(char)
                    }
                    i++
                }
            }
        }
    }

    Text(
        text = annotatedString,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 1.dp)
    )
}

// Regex utility to grab curly brackets contents
private fun extractCurlyBracketsContent(text: String, prefix: String): List<String> {
    val results = mutableListOf<String>()
    val idx = text.indexOf(prefix)
    if (idx == -1) return results

    var searchIdx = idx + prefix.length
    var openBrackets = 0
    var accumulator = StringBuilder()

    while (searchIdx < text.length) {
        val char = text[searchIdx]
        if (char == '{') {
            if (openBrackets > 0) {
                accumulator.append(char)
            }
            openBrackets++
        } else if (char == '}') {
            openBrackets--
            if (openBrackets == 0) {
                results.add(accumulator.toString())
                accumulator = StringBuilder()
                // See if another brace follows immediately
                val nextCharIdx = searchIdx + 1
                if (nextCharIdx < text.length && text[nextCharIdx] == '{') {
                    // continue parsing next block
                } else {
                    break
                }
            } else {
                accumulator.append(char)
            }
        } else {
            if (openBrackets > 0) {
                accumulator.append(char)
            }
        }
        searchIdx++
    }
    return results
}
