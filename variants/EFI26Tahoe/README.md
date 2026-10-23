# EFI26Tahoe - OpenCore Configuration for macOS 26 Tahoe (Intel M720q/M920q)

Bộ cấu hình OpenCore này được xây dựng chuyên biệt để đưa chiếc **Lenovo ThinkCentre M720q / M920q Tiny** (Coffee Lake Gen 8) lên **macOS 26 Tahoe** (phiên bản macOS cuối cùng còn hỗ trợ vi xử lý Intel x86_64).

---

## 1. Các thay đổi cốt lõi cho macOS 26 Tahoe

| Hạng mục | macOS 15 Sequoia (`EFI15Base14`) | macOS 26 Tahoe (`EFI26Tahoe`) | Lý do kỹ thuật |
| :--- | :--- | :--- | :--- |
| **SMBIOS** | `iMac19,1` | **`iMac20,1`** | macOS 26 Tahoe đã ngừng hỗ trợ `iMac19,1`. Cần chuyển sang `iMac20,1` (Comet Lake) để vượt qua bộ lọc tương thích. |
| **Âm thanh Analog (Jack 3.5mm)** | Chỉ cần `AppleALC.kext` | **`AppleALC.kext` + `SimpleHDA.kext`** | Apple đã xóa bỏ hoàn toàn `AppleHDA.kext` trong Tahoe. `SimpleHDA` khôi phục lại audio stack cho chip ALC233 mà không cần tắt SIP hay patch root. |
| **Bypass kiểm tra phần cứng** | Không có | **`RestrictEvents.kext`** | Hỗ trợ OTA update ổn định và hiển thị đúng thông số vi xử lý trên SMBIOS mới. |
| **Bluetooth Arguments** | `-btlfxallowanyaddr` | **`-btlfxallowanyaddr -ibtcompatbeta`** | Bổ sung cờ `-ibtcompatbeta` để driver Intel Bluetooth tương thích với daemon CoreBluetooth trên Tahoe. |
| **Thứ tự Kext Bluetooth** | Xáo trộn | **`Firmware` ➡️ `BlueToolFixup` ➡️ `Patcher`** | Chuẩn hóa quy trình inject firmware cho Bluetooth 5.2 (AX200 / AX210). |

---

## 2. Kext nạp bổ sung trong `EFI26Tahoe/EFI/OC/Kexts/`
- **`SimpleHDA.kext`**: Khắc phục triệt để lỗi mất âm thanh jack cắm tai nghe / loa trong do Apple gỡ bỏ driver AppleHDA.
- **`RestrictEvents.kext`**: Tối ưu hóa OTA updates và tương thích SMBIOS iMac20,1.

---

## 3. Các bước nâng cấp lên macOS 26 Tahoe

1. **Chuẩn bị sao lưu:**
   - Sao lưu toàn bộ dữ liệu quan trọng trước khi nâng cấp.
   - Đăng xuất tài khoản iCloud / tắt FileVault trước khi chạy trình cài đặt Tahoe để tránh lỗi mã hóa ổ đĩa khi reboot.
2. **Cài đặt EFI:**
   - Mount phân vùng EFI và copy toàn bộ nội dung `EFI26Tahoe/EFI` vào phân vùng EFI.
3. **Reset NVRAM:**
   - Khởi động lại máy, chọn **Reset NVRAM** tại màn hình boot OpenCore để hệ thống cập nhật SMBIOS `iMac20,1` mới.
4. **Cập nhật hệ điều hành:**
   - Tải bộ cài macOS 26 Tahoe và tiến hành trích xuất:
     ```bash
     sudo installer -pkg /path/to/InstallAssistant.pkg -target /
     ```
   - Mở ứng dụng **Install macOS Tahoe.app** trong thư mục Applications để bắt đầu nâng cấp.
