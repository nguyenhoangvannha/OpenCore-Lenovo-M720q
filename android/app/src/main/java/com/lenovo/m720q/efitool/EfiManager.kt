package com.lenovo.m720q.efitool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UsbPartition(
    val devPath: String,
    val fsType: String,
    val label: String,
    val isProbableEfi: Boolean
)

object EfiManager {

    const val DEFAULT_MOUNT_POINT = "/mnt/runtime/default/efi_usb"

    suspend fun scanUsbPartitions(onLog: (String) -> Unit): List<UsbPartition> = withContext(Dispatchers.IO) {
        onLog("[*] Bắt đầu quét phân vùng USB OTG...")
        val partitions = mutableListOf<UsbPartition>()

        val blkidResult = RootHelper.runSuCommand("blkid", onLog)
        val lines = blkidResult.stdout.lines().filter { it.isNotBlank() }

        for (line in lines) {
            // Line format e.g.: /dev/block/sda1: LABEL="EFI" UUID="1234-ABCD" TYPE="vfat"
            if (line.contains("/dev/block/sd") || line.contains("/dev/block/vd")) {
                val dev = line.substringBefore(":").trim()
                val fsType = Regex("""TYPE="([^"]+)"""").find(line)?.groupValues?.get(1) ?: "unknown"
                val label = Regex("""LABEL="([^"]+)"""").find(line)?.groupValues?.get(1) ?: ""

                val isProbableEfi = fsType.contains("vfat", ignoreCase = true) ||
                        fsType.contains("fat", ignoreCase = true) ||
                        label.contains("efi", ignoreCase = true) ||
                        label.contains("boot", ignoreCase = true)

                partitions.add(UsbPartition(dev, fsType, label, isProbableEfi))
            }
        }

        if (partitions.isEmpty()) {
            onLog("[!] Không thấy phân vùng qua blkid, đang quét trực tiếp /dev/block/sd*...")
            val devListResult = RootHelper.runSuCommand("ls -1 /dev/block/sd* 2>/dev/null")
            val devLines = devListResult.stdout.lines().filter { it.matches(Regex(""".*/sd[a-z][0-9]+$""")) }
            for (dev in devLines) {
                partitions.add(UsbPartition(dev, "vfat (dự đoán)", "USB", true))
            }
        }

        onLog("[+] Tìm thấy ${partitions.size} phân vùng USB.")
        partitions
    }

    suspend fun mountPartition(
        devPath: String,
        mountPoint: String = DEFAULT_MOUNT_POINT,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onLog("[*] Tạo thư mục mount: $mountPoint")
        RootHelper.runSuCommand("mkdir -p \"$mountPoint\"", onLog)

        onLog("[*] Đang mount $devPath vào $mountPoint...")
        val mountRes = RootHelper.runSuCommand("mount -t vfat -o rw,umask=000 \"$devPath\" \"$mountPoint\"", onLog)

        if (!mountRes.isSuccess) {
            onLog("[!] Thử mount phương án 2 (không cờ bổ sung)...")
            val fallbackRes = RootHelper.runSuCommand("mount \"$devPath\" \"$mountPoint\"", onLog)
            if (!fallbackRes.isSuccess) {
                onLog("[ERROR] Không thể mount phân vùng $devPath")
                return@withContext false
            }
        }

        onLog("[✓] Mount thành công!")
        true
    }

    suspend fun copyEfi(
        sourcePath: String,
        mountPoint: String = DEFAULT_MOUNT_POINT,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onLog("[*] Kiểm tra thư mục nguồn: $sourcePath")
        val checkSrc = RootHelper.runSuCommand("[ -d \"$sourcePath\" ] && echo EXISTS", onLog)
        if (!checkSrc.stdout.contains("EXISTS")) {
            onLog("[ERROR] Thư mục nguồn không tồn tại: $sourcePath")
            return@withContext false
        }

        // Backup existing EFI if present on USB
        val checkExisting = RootHelper.runSuCommand("[ -d \"$mountPoint/EFI\" ] && echo EXISTS")
        if (checkExisting.stdout.contains("EXISTS")) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupName = "EFI_backup_$timestamp"
            onLog("[*] Phát hiện EFI cũ trên USB. Đang sao lưu thành: $backupName")
            RootHelper.runSuCommand("mv \"$mountPoint/EFI\" \"$mountPoint/$backupName\"", onLog)
        }

        onLog("[*] Bắt đầu copy EFI từ $sourcePath vào USB...")
        val cpRes = if (sourcePath.endsWith("/EFI") || sourcePath.endsWith("/EFI/")) {
            RootHelper.runSuCommand("cp -rf \"$sourcePath\" \"$mountPoint/\"", onLog)
        } else {
            RootHelper.runSuCommand("mkdir -p \"$mountPoint/EFI\" && cp -rf \"$sourcePath\"/* \"$mountPoint/EFI/\"", onLog)
        }

        if (!cpRes.isSuccess) {
            onLog("[ERROR] Lỗi trong quá trình copy file!")
            return@withContext false
        }

        onLog("[*] Đang đồng bộ bộ nhớ đệm (sync)...")
        RootHelper.runSuCommand("sync", onLog)
        onLog("[✓] Đã copy thành công thư mục EFI vào USB!")
        true
    }

    suspend fun unmountPartition(
        mountPoint: String = DEFAULT_MOUNT_POINT,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onLog("[*] Đang đồng bộ bộ đệm trước khi unmount...")
        RootHelper.runSuCommand("sync", onLog)

        onLog("[*] Đang gỡ mount $mountPoint...")
        val umountRes = RootHelper.runSuCommand("umount \"$mountPoint\"", onLog)
        RootHelper.runSuCommand("rmdir \"$mountPoint\" 2>/dev/null")

        if (umountRes.isSuccess) {
            onLog("[✓] Gỡ mount an toàn thành công! Bây giờ bạn có thể rút USB.")
            true
        } else {
            onLog("[!] Lỗi gỡ mount: ${umountRes.stderr}")
            false
        }
    }
}
