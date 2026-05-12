package com.mypassword.app.ui.generator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.ln
import kotlin.math.log2

@Composable
fun PasswordGeneratorDialog(
    onDismiss: () -> Unit,
    onPasswordSelected: (String) -> Unit
) {
    var length by remember { mutableIntStateOf(16) }
    var includeUpper by remember { mutableStateOf(true) }
    var includeLower by remember { mutableStateOf(true) }
    var includeDigits by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    // 初始生成
    LaunchedEffect(Unit) {
        password = generatePassword(length, includeUpper, includeLower, includeDigits, includeSymbols)
    }

    fun regenerate() {
        password = generatePassword(length, includeUpper, includeLower, includeDigits, includeSymbols)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "密码生成器",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 密码预览
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = password,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            IconButton(
                                onClick = { regenerate() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = "重新生成",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(password))
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = "复制",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 密码强度
                Spacer(modifier = Modifier.height(8.dp))
                PasswordStrengthIndicator(password)

                Spacer(modifier = Modifier.height(20.dp))

                // 长度滑块
                Text(
                    "长度：$length",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = length.toFloat(),
                    onValueChange = {
                        length = it.toInt()
                        regenerate()
                    },
                    valueRange = 8f..64f,
                    steps = 55,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("8", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("64", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 字符类型选项
                Text(
                    "包含字符类型",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = includeUpper,
                        onClick = { includeUpper = !includeUpper; regenerate() },
                        label = { Text("A-Z", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = includeLower,
                        onClick = { includeLower = !includeLower; regenerate() },
                        label = { Text("a-z", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = includeDigits,
                        onClick = { includeDigits = !includeDigits; regenerate() },
                        label = { Text("0-9", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = includeSymbols,
                        onClick = { includeSymbols = !includeSymbols; regenerate() },
                        label = { Text("!@#$%", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 使用此密码按钮
                Button(
                    onClick = {
                        onPasswordSelected(password)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("使用此密码", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    }
}

@Composable
private fun PasswordStrengthIndicator(password: String) {
    val entropy = calculateEntropy(password)
    val (label, color, progress) = when {
        entropy < 40 -> Triple("弱", MaterialTheme.colorScheme.error, 0.25f)
        entropy < 60 -> Triple("中等", MaterialTheme.colorScheme.tertiary, 0.5f)
        entropy < 80 -> Triple("强", MaterialTheme.colorScheme.primary, 0.75f)
        else -> Triple("非常强", MaterialTheme.colorScheme.primary, 1f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun calculateEntropy(password: String): Int {
    if (password.isEmpty()) return 0

    var poolSize = 0
    if (password.any { it.isUpperCase() }) poolSize += 26
    if (password.any { it.isLowerCase() }) poolSize += 26
    if (password.any { it.isDigit() }) poolSize += 10
    if (password.any { !it.isLetterOrDigit() }) poolSize += 32

    if (poolSize == 0) poolSize = 26
    return (password.length * log2(poolSize.toDouble())).toInt()
}

private fun generatePassword(
    length: Int,
    includeUpper: Boolean,
    includeLower: Boolean,
    includeDigits: Boolean,
    includeSymbols: Boolean
): String {
    val chars = buildString {
        if (includeUpper) append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        if (includeLower) append("abcdefghijklmnopqrstuvwxyz")
        if (includeDigits) append("0123456789")
        if (includeSymbols) append("!@#$%^&*()-_=+<>?")
    }
    if (chars.isEmpty()) return ""

    val secureRandom = java.security.SecureRandom()
    return (1..length).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
}
