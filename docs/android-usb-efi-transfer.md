# Hướng Dẫn Copy Thư Mục EFI Bằng Điện Thoại Android Qua Cổng OTG

Tài liệu này hướng dẫn cách sử dụng điện thoại/máy tính bảng Android kết nối qua USB OTG để copy hoặc cập nhật thư mục `EFI` (OpenCore) vào phân vùng khởi động của USB mà không cần máy tính.

---

## Cách 1: Sử Dụng App Không Cần Root (Non-Root)

Đối với các thiết bị Android chưa Root, các app quản lý file mặc định sẽ không thấy hoặc không cho phép ghi vào phân vùng EFI (ESP) ẩn. Sử dụng các công cụ chuyên dụng sau:

### 1. Dùng MiXplorer (Khuyên dùng)
- **Tải về**: [MiXplorer trên APKMirror / XDA Labs](https://www.apkmirror.com/apk/pootkill/mixplorer-hootanparsa/)
- **Các bước thực hiện**:
  1. Cắm USB thông qua đầu chuyển OTG vào điện thoại Android.
  2. Mở app **MiXplorer**, chọn cấp quyền truy cập thiết bị lưu trữ USB khi hộp thoại hệ thống xuất hiện.
  3. Mở menu bên trái (vuốt từ mép trái màn hình sang phải).
  4. Chọn thiết bị USB hoặc phân vùng FAT32 của USB. MiXplorer có driver OTG riêng cho phép đọc/ghi trực tiếp vào phân vùng FAT32/EFI mà không bị giới hạn bởi hệ điều hành Android.
  5. Copy thư mục `EFI` từ bộ nhớ máy (`/sdcard/Download/...`) và dán vào thư mục gốc của phân vùng USB.

### 2. Dùng Total Commander + Paragon USB Plugin
- **Tải về từ Google Play**:
  - [Total Commander](https://play.google.com/store/apps/details?id=com.ghisler.android.TotalCommander)
  - [Microsoft exFAT/NTFS for USB by Paragon Software](https://play.google.com/store/apps/details?id=com.paragon.tcplugins_ntfs)
- **Các bước thực hiện**:
  1. Mở Total Commander -> chọn plugin Paragon USB.
  2. Plugin sẽ liệt kê tất cả các phân vùng trên USB (kể cả phân vùng EFI / FAT32 ẩn).
  3. Chọn phân vùng EFI -> Mount -> Copy file `EFI` vào.

---

## Cách 2: Sử Dụng Script Tự Động Với Root (Termux / Root Shell)

Nếu máy Android của bạn đã **Root** (Magisk / KernelSU / APatch), bạn có thể dùng script tự động đi kèm trong repo này.

### Script: `tools/android-efi-transfer.sh`

1. Copy script hoặc clone repo về Termux trên Android:
   ```bash
   pkg install git tsu
   ```
2. Chạy script với quyền Root:
   ```bash
   su
   sh tools/android-efi-transfer.sh /sdcard/Download/EFI
   ```
3. Script sẽ tự động:
   - Quét các phân vùng USB OTG được cắm (`blkid` / `/dev/block/sd*`).
   - Yêu cầu bạn xác nhận phân vùng EFI (ví dụ: `/dev/block/sda1`).
   - Mount phân vùng vào thư mục tạm thời.
   - Copy toàn bộ thư mục `EFI` vào USB.
   - Chạy lệnh `sync` đồng bộ dữ liệu và `umount` an toàn trước khi rút USB.

---

## Cấu Trúc Phân Vùng EFI Tiêu Chuẩn

Sau khi copy xong, đảm bảo cấu trúc thư mục trên USB như sau:

```text
<USB_ROOT>/
└── EFI/
    ├── BOOT/
    │   └── BOOTx64.efi
    └── OC/
        ├── config.plist
        ├── OpenCore.efi
        ├── ACPI/
        ├── Drivers/
        └── Kexts/
```
> [!WARNING]
> Không để lồng 2 lớp thư mục thành `EFI/EFI/OC/...`. Phải đảm bảo thư mục `EFI` nằm ngay tại thư mục gốc (root) của phân vùng boot USB.
