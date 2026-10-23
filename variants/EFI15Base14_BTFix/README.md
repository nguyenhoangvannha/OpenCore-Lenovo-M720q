# EFI15Base14_BTFix - OpenCore Configuration for macOS 15 Sequoia (Bluetooth Audio Fix)

Bản EFI này được tạo từ `EFI15Base14` nhằm khắc phục sự cố **Bluetooth chớp tắt / tự bật-tắt khi kết nối loa/tai nghe Bluetooth (A2DP Audio)** trên card mạng **Intel Dual Band Wireless-AC 7260** (`8087:07dc`).

---

## 1. Hiện tượng lỗi trước khi sửa (Root Cause)
- Khi bấm **Connect** vào thiết bị âm thanh Bluetooth (ví dụ: loa *XDOBO X8 III*), giao diện Bluetooth nháy tắt rồi tự bật lại ngay, thiết bị không kết nối được.
- **Nguyên nhân:**
  - Card Intel AC 7260 sử dụng Bluetooth 4.0 thế hệ cũ (năm 2013).
  - Trên macOS 15 Sequoia, Apple thay đổi hoàn toàn Bluetooth stack. Khi bắt tay kết nối profile âm thanh A2DP, kext `IntelBTPatcher.kext` gây lỗi không tương thích với bảng patch của firmware 7260 khiến daemon `bluetoothd` bị crash và khởi động lại liên tục.

---

## 2. Các thay đổi đã thực hiện trong `config.plist`

| Thành phần | Trước khi sửa (`EFI15Base14`) | Sau khi sửa (`EFI15Base14_BTFix`) | Mục đích |
| :--- | :--- | :--- | :--- |
| **`IntelBTPatcher.kext`** | `Enabled: true` | **`Enabled: false`** | Tránh crash daemon `bluetoothd` khi kết nối thiết bị Audio |
| **Thứ tự nạp Kext** | `IntelBTPatcher` ➡️ `IntelBluetoothFirmware` ➡️ `BlueToolFixup` | **`IntelBluetoothFirmware` ➡️ `BlueToolFixup` ➡️ `IntelBTPatcher (Off)`** | Đảm bảo firmware được inject trước khi `BlueToolFixup` patch |
| **NVRAM Delete** | Chỉ xóa `boot-args` | Bổ sung: `bluetoothExternalDongleFailed`, `bluetoothInternalControllerInfo` | Dọn sạch cache controller lỗi cũ giữa các lần khởi động |
| **boot-args** | `-btlfxallowanyaddr` | Giữ nguyên `-btlfxallowanyaddr` | Cần thiết cho `BlueToolFixup` trên macOS 15 |
| **ReleaseUsbOwnership** | `true` | `true` | Đảm bảo BIOS bàn giao sạch controller USB cho macOS |

---

## 3. Hướng dẫn sử dụng & Kiểm tra

1. **Mount phân vùng EFI** của ổ cứng hoặc USB boot:
   - Dùng *OpenCore Configurator*, *MountEFI*, hoặc lệnh terminal:
     ```bash
     sudo diskutil mount /dev/disk0s1
     ```
2. **Sao chép thư mục `EFI`**:
   - Copy toàn bộ nội dung bên trong thư mục `EFI15Base14_BTFix/EFI` vào phân vùng EFI vừa mount (nhớ backup EFI cũ).
3. **Reset NVRAM (Bắt buộc):**
   - Khởi động lại máy.
   - Tại màn hình chọn boot của OpenCore (Boot Picker), chọn mục **Reset NVRAM** để xóa sạch cache Bluetooth cũ.
4. **Kiểm tra kết nối:**
   - Vào macOS Sequoia -> *System Settings* -> *Bluetooth*.
   - Đưa loa/tai nghe về chế độ Pairing.
   - Bấm **Connect** để kiểm tra âm thanh.

---

## 4. Lưu ý phần cứng lâu dài
Card Intel AC 7260 là dòng đời cũ. Nếu sau bản vá này loa kết nối được nhưng vẫn gặp hiện tượng méo tiếng hoặc drop tín hiệu do giới hạn băng thông Bluetooth 4.0:
- Khuyến nghị nâng cấp lên card **Intel AX210 M.2 2230** (Bluetooth 5.2, chuẩn giống cấu hình tác giả repo gốc).
