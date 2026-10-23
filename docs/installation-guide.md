# Hướng dẫn cài đặt Hackintosh (macOS) cho Lenovo M720q / M920q

Tài liệu này được tổng hợp và chuẩn hóa từ ghi chú cài đặt gốc (`docs/Install Hackintosh.rtf`), hướng dẫn chi tiết từng bước cài đặt macOS lên Lenovo ThinkCentre M720q / M920q Tiny.

---

## Các bước thực hiện tổng quan

```text
[1. Tạo USB Boot macOS] ➔ [2. Cài EFI vào USB] ➔ [3. Cấu hình BIOS] ➔ [4. Cài đặt macOS] ➔ [5. Hoàn thiện Wi-Fi/Bluetooth]
```

---

## 1. Tạo USB Boot macOS

Sử dụng bộ cài chính thức tải từ Apple hoặc file cài đặt dạng `.app` (ví dụ `Install macOS Sequoia.app` hoặc `Install macOS Sonoma.app`):

1. Chuẩn bị USB có dung lượng tối thiểu 16GB.
2. Mở **Disk Utility**, định dạng USB với thông số:
   - **Name**: `MyVolume`
   - **Format**: `Mac OS Extended (Journaled)`
   - **Scheme**: `GUID Partition Map`
3. Mở Terminal và chạy lệnh `createinstallmedia`:
   ```bash
   sudo /Applications/Install\ macOS\ Sequoia.app/Contents/Resources/createinstallmedia --volume /Volumes/MyVolume
   ```
   *(Thay đổi đường dẫn file cài đặt tương ứng với phiên bản macOS bạn dùng)*

---

## 2. Gắn (Mount) và sao chép EFI vào phân vùng EFI của USB

### Cách 1: Sử dụng MountEFI trên macOS (Khuyến nghị)
Bộ công cụ đã được tích hợp sẵn tại [`tools/MountEFI/`](../tools/MountEFI/):
1. Chạy file [`tools/MountEFI/MountEFI.command`](../tools/MountEFI/MountEFI.command) (hoặc cài đặt Finder Quick Action từ `Mount EFI.workflow`).
2. Chọn số thứ tự phân vùng EFI của USB Boot vừa tạo để mount.
3. Chạy script tải kext trong thư mục gốc repo:
   ```bash
   ./download-kexts.sh
   ```
4. Copy toàn bộ thư mục `EFI` (ví dụ `EFI15Base14/EFI` hoặc cấu hình tương ứng) vào thư mục gốc của phân vùng EFI vừa mount:
   ```text
   /Volumes/EFI/
   └── EFI/
       ├── BOOT/
       │   └── BOOTx64.efi
       └── OC/
           ├── config.plist
           ├── Kexts/
           └── ...
   ```

### Cách 2: Sử dụng thiết bị Android (Khi không có máy Mac)
Tham khảo hướng dẫn chi tiết tại: [Android USB EFI Transfer Guide](android-usb-efi-transfer.md) hoặc ứng dụng tại [`android/`](../android/).

---

## 3. Cấu hình BIOS máy Lenovo M720q / M920q

Khởi động máy và nhấn liên tục **F1** để vào BIOS Setup. Thiết lập các thông số sau:

* **Security**:
  - `Secure Boot` ➔ **Disabled**
* **Devices**:
  - `Serial Port` ➔ **Disabled**
  - `USB Support` ➔ **Enabled**
  - `USB Legacy Support` ➔ **Disabled**
  - `USB Enumeration Delay` ➔ **Disabled**
  - `Front USB Ports, Rear USB Ports` ➔ **Enabled**
* **Devices -> ATA Drive Setup**:
  - `Configure SATA as` ➔ **AHCI**
  - `Hard-disk Pre Delay` ➔ **Disabled**
* Nhấn **F10** để lưu và khởi động lại.

---

## 4. Khởi động và cài đặt macOS

1. Cắm USB Boot vào cổng USB trên máy Lenovo M720q / M920q.
2. Bật máy và nhấn liên tục **F12** để mở Boot Menu.
3. Chọn USB Boot (UEFI).
4. Tại giao diện OpenCore Picker, chọn **Install macOS...**.
5. Trong bộ cài:
   - Vào **Disk Utility**, chọn *Show All Devices*.
   - Format ổ cứng đích (SSD NVMe):
     - **Format**: `APFS`
     - **Scheme**: `GUID Partition Map`
   - Thoát Disk Utility và tiến hành cài đặt macOS vào phân vùng vừa format.
   - Máy sẽ tự khởi động lại vài lần trong quá trình cài đặt (ở mỗi lần khởi động lại, tiếp tục boot từ USB và chọn phân vùng macOS Installer cho đến khi hoàn tất thiết lập tài khoản).

---

## 5. Hoàn thiện sau cài đặt (Wi-Fi, Bluetooth & Chuyển EFI sang SSD)

### Sao chép EFI vào ổ cứng trong (SSD)
1. Dùng [`tools/MountEFI/MountEFI.command`](../tools/MountEFI/MountEFI.command) để mount cả phân vùng EFI của USB và phân vùng EFI của SSD NVMe nội bộ máy.
2. Copy toàn bộ thư mục `EFI` từ USB sang phân vùng EFI của SSD. Từ lúc này, máy có thể tự khởi động thẳng vào macOS mà không cần cắm USB.

### Cấu hình Wi-Fi & Bluetooth
* Do card Wi-Fi Intel (AX200 / AX210) sử dụng driver mã nguồn mở:
  - Wi-Fi được quản lý qua giao diện Ethernet ảo.
  - Tải và cài đặt ứng dụng [HeliPort](https://github.com/OpenIntelWireless/HeliPort) để kết nối mạng Wi-Fi và nhập mật khẩu qua khay menu bar.
  - Bluetooth hoạt động tự động thông qua các kext `IntelBluetoothFirmware` và `BlueToolFixup`.
