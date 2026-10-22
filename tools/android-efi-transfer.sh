#!/system/bin/sh
# ==============================================================================
# Script: android-efi-transfer.sh
# Purpose: Copy OpenCore EFI to USB drive directly on Android (via OTG)
# Requirements: Android with Root (Termux / Root Shell / ADB shell)
# ==============================================================================

set -e

# Check Root permissions
if [ "$(id -u)" -ne 0 ]; then
    echo "[!] Script requires root privileges. Attempting to run with su..."
    exec su -c "sh \"$0\" \"$@\""
fi

echo "============================================================"
echo "    Android USB OTG - OpenCore EFI Transfer Tool"
echo "============================================================"

SRC_EFI="${1:-}"

# If not provided via argument, search in current directory or prompt
if [ -z "$SRC_EFI" ]; then
    if [ -d "./EFI" ]; then
        SRC_EFI="./EFI"
    elif [ -d "../EFI" ]; then
        SRC_EFI="../EFI"
    else
        echo "[?] Nhập đường dẫn thư mục EFI cần copy (ví dụ: /sdcard/Download/EFI):"
        read -r SRC_EFI
    fi
fi

if [ ! -d "$SRC_EFI" ]; then
    echo "[ERROR] Không tìm thấy thư mục EFI tại: $SRC_EFI"
    exit 1
fi

echo "[+] Thư mục nguồn EFI: $SRC_EFI"

# Scan for connected USB block devices
echo ""
echo "[*] Đang quét các phân vùng USB OTG..."

USB_PARTITIONS=$(blkid | grep -iE 'vfat|fat32|msdos' || true)

if [ -z "$USB_PARTITIONS" ]; then
    echo "[!] Không tìm thấy phân vùng FAT32/vfat nào qua blkid."
    echo "[*] Thử quét trực tiếp qua /dev/block/sd*..."
    USB_PARTITIONS=$(ls -1 /dev/block/sd* 2>/dev/null | grep -E '[0-9]$' || true)
fi

if [ -z "$USB_PARTITIONS" ]; then
    echo "[ERROR] Không phát hiện USB OTG nào được cắm. Vui lòng kiểm tra cáp OTG và thử lại."
    exit 1
fi

echo "------------------------------------------------------------"
echo "Danh sách phân vùng tìm thấy:"
echo "$USB_PARTITIONS"
echo "------------------------------------------------------------"

echo "[?] Nhập đường dẫn phân vùng EFI của USB (ví dụ: /dev/block/sda1 hoặc /dev/block/sdb1):"
read -r TARGET_DEV

if [ ! -b "$TARGET_DEV" ]; then
    echo "[ERROR] Thiết bị $TARGET_DEV không hợp lệ hoặc không tồn tại!"
    exit 1
fi

MOUNT_POINT="/mnt/runtime/default/efi_temp_$$"
[ ! -d "$MOUNT_POINT" ] && mkdir -p "$MOUNT_POINT"

echo "[*] Đang mount $TARGET_DEV vào $MOUNT_POINT..."
mount -t vfat -o rw,umask=000 "$TARGET_DEV" "$MOUNT_POINT" || {
    # Fallback mount without extra options
    mount "$TARGET_DEV" "$MOUNT_POINT"
}

cleanup() {
    echo ""
    echo "[*] Đang đồng bộ dữ liệu (sync)..."
    sync
    echo "[*] Đang unmount phân vùng..."
    umount "$MOUNT_POINT" 2>/dev/null || true
    rmdir "$MOUNT_POINT" 2>/dev/null || true
    echo "[✓] Hoàn tất và an toàn để rút USB!"
}
trap cleanup EXIT INT TERM

echo "[*] Nội dung hiện tại trên phân vùng USB:"
ls -la "$MOUNT_POINT"

# Copy EFI
echo ""
echo "[*] Bắt đầu copy EFI..."
if [ -d "$MOUNT_POINT/EFI" ]; then
    echo "[!] Đã có sẵn thư mục EFI trên USB. Tiến hành ghi đè/cập nhật..."
fi

# Copy entire EFI directory
cp -rf "$SRC_EFI" "$MOUNT_POINT/"

echo "[✓] Copy hoàn tất. Kiểm tra thư mục trên USB:"
ls -la "$MOUNT_POINT/EFI" 2>/dev/null || ls -la "$MOUNT_POINT"

echo "============================================================"
echo "    THÀNH CÔNG! Đang chuẩn bị ngắt kết nối an toàn..."
echo "============================================================"
