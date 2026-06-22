package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    CalculatorApp()
                }
            }
        }
    }
}

@Composable
fun CalculatorApp() {
    var num1 by remember { mutableStateOf("") }
    var num2 by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    var activeField by remember { mutableIntStateOf(1) }

    fun appendValue(value: String) {
        if (activeField == 1) num1 += value
        else if (activeField == 2) num2 += value
    }

    fun deleteLast() {
        if (activeField == 1 && num1.isNotEmpty()) num1 = num1.dropLast(1)
        else if (activeField == 2 && num2.isNotEmpty()) num2 = num2.dropLast(1)
    }

    fun clearAll() {
        num1 = ""
        num2 = ""
        result = ""
        activeField = 1
    }

    fun calculate(op: String) {
        val n1 = num1.toDoubleOrNull()
        val n2 = num2.toDoubleOrNull()

        if (n1 == null || n2 == null) {
            result = "Error"
            activeField = 3
            return
        }

        result = when (op) {
            "+" -> (n1 + n2).toString()
            "-" -> (n1 - n2).toString()
            "*" -> (n1 * n2).toString()
            "/" -> if (n2 == 0.0) "Div/0" else (n1 / n2).toString()
            else -> ""
        }
        activeField = 3
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "SIMPLE CALCULATOR",
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFF7A45),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        CustomInput(num1, "Angka 1", selected = (activeField == 1)) { activeField = 1 }
        Spacer(modifier = Modifier.height(16.dp))
        CustomInput(num2, "Angka 2", selected = (activeField == 2)) { activeField = 2 }
        Spacer(modifier = Modifier.height(16.dp))
        CustomInput(result, "Result", selected = (activeField == 3)) { activeField = 3 }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Operator("+") { calculate("+") }
            Operator("-") { calculate("-") }
            Operator("×") { calculate("*") }
            Operator("÷") { calculate("/") }
            Operator("Clear", true) { clearAll() }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KeyRow(listOf("1", "2", "3", "-"), { appendValue(it) }, { deleteLast() })
            KeyRow(listOf("4", "5", "6", "↵"), { appendValue(it) }, { activeField = 2 })
            KeyRow(listOf("7", "8", "9", "⌫"), { appendValue(it) }, { deleteLast() })
            KeyRow(listOf(",", "0", ".", "←"), {
                appendValue(if (it == ",") "." else it)
            }, { activeField = 1 })
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CustomInput(
    value: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = Color(0xFFF3F0F7)

    Surface(
        onClick = onClick,
        color = containerColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        TextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledContainerColor = Color.Transparent,
                disabledLabelColor = Color.Gray,
                disabledTextColor = Color.Black,
                disabledIndicatorColor = if (selected) Color(0xFFFF7A45) else Color.Transparent
            )
        )
    }
}

@Composable
fun Operator(text: String, wide: Boolean = false, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(if (wide) 80.dp else 50.dp, 50.dp)
            .background(Color(0xFFFF7A45), RoundedCornerShape(50))
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun KeyRow(
    buttons: List<String>,
    onNumber: (String) -> Unit,
    onSpecial: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        buttons.forEach { label ->
            val color = when (label) {
                "↵" -> Color(0xFFDCEEFF)
                "⌫" -> Color(0xFFF0E8FF)
                "←" -> Color(0xFF8ED1FF)
                "-" -> Color(0xFFDCEEFF)
                else -> Color.White
            }

            Button(
                onClick = {
                    if (label in listOf("↵", "⌫", "←")) onSpecial() else onNumber(label)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(50),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = color,
                    contentColor = Color.Black
                )
            ) {
                Text(label, fontSize = 22.sp)
            }
        }
    }
}