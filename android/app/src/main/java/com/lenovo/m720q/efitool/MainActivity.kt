package com.lenovo.m720q.efitool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EfiToolScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EfiToolScreen() {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val logScrollState = rememberScrollState()

    var isRootGranted by remember { mutableStateOf(false) }
    var isCheckingRoot by remember { mutableStateOf(false) }

    var detectedPartitions by remember { mutableStateOf<List<UsbPartition>>(emptyList()) }
    var selectedPartition by remember { mutableStateOf("/dev/block/sda1") }
    var sourceEfiPath by remember { mutableStateOf("/sdcard/Download/EFI") }

    var isProcessing by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf("=== Sẵn sàng. Nhấn 'Yêu cầu Root' để bắt đầu ===\n") }

    fun addLog(msg: String) {
        logs += "$msg\n"
    }

    LaunchedEffect(Unit) {
        isCheckingRoot = true
        val granted = RootHelper.requestRootPermission()
        isRootGranted = granted
        isCheckingRoot = false
        if (granted) {
            addLog("[✓] Đã phát hiện và cấp quyền Root thành công!")
        } else {
            addLog("[!] Chưa có quyền Root. Vui lòng bấm nút 'Yêu cầu Root'.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lenovo M720q EFI Tool", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Copy OpenCore EFI qua USB OTG", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Root Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isRootGranted) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRootGranted) "Quyền Root: ĐÃ CẤP" else "Quyền Root: CHƯA CÓ",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isRootGranted) "Có thể thao tác mount và ghi trực tiếp vào USB."
                            else "Cần quyền Root (Magisk/KernelSU/APatch) để truy cập phân vùng EFI ẩn.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    if (!isRootGranted) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isCheckingRoot = true
                                    addLog("[*] Đang gửi yêu cầu cấp quyền Root...")
                                    val ok = RootHelper.requestRootPermission()
                                    isRootGranted = ok
                                    isCheckingRoot = false
                                    if (ok) addLog("[✓] Cấp quyền Root thành công!")
                                    else addLog("[ERROR] Không nhận được quyền Root. Vui lòng kiểm tra Magisk / KernelSU.")
                                }
                            },
                            enabled = !isCheckingRoot,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Xin Root", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Step 1: Scan USB Partitions
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("1. Chọn Phân Vùng USB (EFI)", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isProcessing = true
                                val list = EfiManager.scanUsbPartitions { addLog(it) }
                                detectedPartitions = list
                                if (list.isNotEmpty()) {
                                    val probable = list.firstOrNull { it.isProbableEfi } ?: list.first()
                                    selectedPartition = probable.devPath
                                    addLog("[*] Đã tự động chọn phân vùng: ${probable.devPath}")
                                }
                                isProcessing = false
                            }
                        },
                        enabled = isRootGranted && !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Quét Các Phân Vùng USB OTG")
                    }

                    if (detectedPartitions.isNotEmpty()) {
                        Text("Các phân vùng phát hiện được:", fontSize = 13.sp, color = Color.Gray)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            detectedPartitions.forEach { part ->
                                val isSelected = selectedPartition == part.devPath
                                Surface(
                                    onClick = { selectedPartition = part.devPath },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${part.devPath} (${part.fsType})", fontWeight = FontWeight.Medium)
                                        if (part.label.isNotBlank()) {
                                            Text(part.label, fontSize = 12.sp, color = Color.Cyan)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = selectedPartition,
                        onValueChange = { selectedPartition = it },
                        label = { Text("Đường dẫn phân vùng USB (khuyên dùng /dev/block/sda1)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Step 2: Source EFI
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("2. Thư Mục EFI Nguồn", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    OutlinedTextField(
                        value = sourceEfiPath,
                        onValueChange = { sourceEfiPath = it },
                        label = { Text("Đường dẫn thư mục EFI trên máy") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { sourceEfiPath = "/sdcard/Download/EFI" },
                            label = { Text("/Download/EFI") }
                        )
                        SuggestionChip(
                            onClick = { sourceEfiPath = "/sdcard/EFI" },
                            label = { Text("/sdcard/EFI") }
                        )
                    }
                }
            }

            // Step 3: Actions
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("3. Thực Thi", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    // Auto One-Click Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isProcessing = true
                                addLog(">>> BẮT ĐẦU QUY TRÌNH TỰ ĐỘNG <<<")
                                val mounted = EfiManager.mountPartition(selectedPartition, onLog = { addLog(it) })
                                if (mounted) {
                                    val copied = EfiManager.copyEfi(sourceEfiPath, onLog = { addLog(it) })
                                    if (copied) {
                                        addLog("[*] Copy hoàn tất, tự động sync và gỡ mount...")
                                        EfiManager.unmountPartition(onLog = { addLog(it) })
                                    } else {
                                        addLog("[!] Copy thất bại, giữ nguyên mount để kiểm tra.")
                                    }
                                }
                                isProcessing = false
                            }
                        },
                        enabled = isRootGranted && !isProcessing && selectedPartition.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🚀 Tự Động: Mount ➔ Copy EFI ➔ Sync & Unmount", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isProcessing = true
                                    EfiManager.mountPartition(selectedPartition, onLog = { addLog(it) })
                                    isProcessing = false
                                }
                            },
                            enabled = isRootGranted && !isProcessing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Chỉ Mount")
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isProcessing = true
                                    EfiManager.unmountPartition(onLog = { addLog(it) })
                                    isProcessing = false
                                }
                            },
                            enabled = isRootGranted && !isProcessing,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Gỡ Mount")
                        }
                    }
                }
            }

            // Step 4: Real-time Terminal Log
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Nhật Ký Lệnh (Terminal Log)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.LightGray
                        )
                        TextButton(onClick = { logs = "" }) {
                            Text("Xoá log", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 220.dp)
                            .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(logScrollState)
                    ) {
                        Text(
                            text = logs,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF76FF03)
                        )
                    }
                }
            }
        }
    }
}
