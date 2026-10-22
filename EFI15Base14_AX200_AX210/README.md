# EFI15Base14_AX200_AX210 - OpenCore Configuration for Intel Wi-Fi 6 / 6E (macOS 15 Sequoia)

Thư mục này được cấu hình sẵn để khi bạn cắm card **Intel AX200** hoặc **Intel AX210** (M.2 Key A/E 2230) vào máy Lenovo M720q/M920q, hệ thống sẽ nhận diện và hoạt động trơn tru cả Wi-Fi và Bluetooth 5.2 trên macOS 15 Sequoia.

---

## 1. Giữ nguyên toàn bộ cấu hình tối ưu của máy bạn
Bộ EFI này **bảo lưu toàn bộ các tinh chỉnh phần cứng riêng của máy bạn** từ `EFI15Base14`:
- **Đồ họa:** Intel UHD Graphics 630 (`AAPL,ig-platform-id: 07009B3E`, `device-id: 9B3E0000`).
- **Âm thanh:** Realtek ALC233 với layout `alcid=13` (loa trong và jack 3.5mm hoạt động bình thường).
- **SMBIOS:** `iMac19,1` (chuẩn cho dòng CPU Coffee Lake Gen 8).
- **ACPI:** DSDT Patch chuyên biệt của Lenovo M920q/M720q + file vá nguồn `MaLd0n.aml`.
- **Cổng USB:** Ánh xạ cổng USB bằng `USBToolBox.kext` + `UTBMap.kext`.

---

## 2. Các tinh chỉnh cho card Intel AX200 / AX210 trên macOS 15 Sequoia

1. **Chuẩn hóa thứ tự nạp Kext Bluetooth:**
   - `IntelBluetoothFirmware.kext` (V2.4.0) ➡️ Nạp trước để inject firmware Bluetooth 5.2.
   - `BlueToolFixup.kext` (V2.6.8, `MinKernel=21.0.0`) ➡️ Tương thích với daemon Bluetooth của macOS Sequoia.
   - `IntelBTPatcher.kext` (V2.4.0, `Enabled=true`) ➡️ Bật đầy đủ để patch các tính năng mở rộng của AX200/AX210.
2. **Cờ boot-args:**
   - Giữ nguyên cờ `-btlfxallowanyaddr` bắt buộc cho `BlueToolFixup`.
3. **NVRAM Cleanup:**
   - Tự động xóa cache controller Bluetooth cũ (`bluetoothExternalDongleFailed`, `bluetoothInternalControllerInfo`) để tránh xung đột với địa chỉ MAC của card cũ AC 7260.
4. **Wi-Fi:**
   - Điều khiển bởi `itlwm.kext` (V2.3.0). Sau khi vào macOS, sử dụng app **HeliPort** để kết nối mạng Wi-Fi.

---

## 3. Hướng dẫn sử dụng khi nhận card AX200 / AX210

1. **Thay card phần cứng:**
   - Tắt máy, rút nguồn. Mở nắp máy Lenovo M720q Tiny.
   - Tháo ốc giữ card cũ (Intel AC 7260), gỡ 2 dây ăng-ten (Main và Aux).
   - Lắp card **Intel AX200 / AX210** mới vào khe M.2 2230, gắn lại 2 dây ăng-ten và vặn ốc.
2. **Áp dụng EFI:**
   - Copy toàn bộ nội dung thư mục `EFI15Base14_AX200_AX210/EFI` vào phân vùng EFI của ổ cứng.
3. **Reset NVRAM:**
   - Khởi động lại máy. Tại màn hình OpenCore Boot Picker, chọn mục **Reset NVRAM** (bắt buộc để hệ điều hành nhận diện controller Bluetooth mới).
4. **Tận hưởng kết quả:**
   - Vào macOS, Bluetooth 5.2 sẽ tự động nhận diện.
   - Bấm kết nối loa Bluetooth, tai nghe (A2DP / AAC) hoạt động mượt mà, không còn lỗi chớp tắt.
