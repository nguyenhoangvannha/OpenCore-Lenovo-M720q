# OpenCore - Lenovo ThinkCentre M720q / M920q Tiny

Bộ cấu hình OpenCore hoàn chỉnh dành cho máy tính tí hon **Lenovo ThinkCentre M720q / M920q Tiny** (Intel Coffee Lake Gen 8 / Gen 9), tương thích tối ưu với **macOS 15 Sequoia** và hỗ trợ card mạng Intel Wi-Fi / Bluetooth.

---

## 📌 Tổng quan cấu hình Active (`EFI/`)

Bản EFI chính thức tại thư mục gốc **[`EFI/`](EFI/)** được kế thừa từ bản kiểm nghiệm thực tế `EFI15Base14_BTFix`:
* **OpenCore**: Phiên bản 1.0.8 (RELEASE)
* **Giao diện Boot (GUI)**: Theme đồ họa **OpenCanopy GoldenGate** chuẩn Apple, hiển thị icon trực quan.
* **Bluetooth Audio Fix**: Đã xử lý triệt để lỗi Bluetooth chớp tắt/tự ngắt khi kết nối tai nghe/loa Bluetooth trên các dòng card Intel Wi-Fi (như AC 7260, AX200, AX210) nhờ cờ boot-arg `-btlfxallowanyaddr` và tối ưu thứ tự nạp kext Bluetooth.
* **USB Mapping**: Đã map cổng chuẩn xác bằng `USBToolBox.kext` + `UTBMap.kext` chuyên biệt cho ThinkCentre Tiny.

---

## 💻 Cấu hình phần cứng (Hardware Specs)

| Linh kiện | Thông số chi tiết | Trạng thái macOS |
| :--- | :--- | :--- |
| **CPU** | Intel Core i3-8100T / i5-8500T / i7-8700T (Coffee Lake) | Hoạt động hoàn hảo |
| **iGPU** | Intel UHD Graphics 630 (DisplayPort / HDMI) | Nhận full qe/ci acceleration |
| **Audio** | Realtek ALC235 (alcid=13) qua jack tai nghe 3.5mm & loa tích hợp | Hoạt động hoàn hảo |
| **Ethernet** | Intel I219-V Gigabit Ethernet (`IntelMausi.kext`) | Hoạt động hoàn hảo |
| **Wi-Fi** | Intel Wireless (`itlwm.kext` + HeliPort) | Hoạt động (hiển thị như Ethernet) |
| **Bluetooth** | Intel Bluetooth (`IntelBluetoothFirmware` + `BlueToolFixup`) | Hoạt động ổn định |
| **NVMe** | M.2 NVMe SSD (`NVMeFix.kext`) | Quản lý nguồn & tốc độ chuẩn |

---

## ⚙️ Thiết lập BIOS khuyến nghị (Recommended BIOS Settings)

Trước khi khởi động bằng EFI này, hãy đảm bảo các mục BIOS sau đã được cài đặt:

```text
Security:
  - Secure Boot: Disabled
  
Devices:
  - Serial Port: Disabled
  - USB Setup:
      - USB Support: Enabled
      - USB Legacy Support: Disabled
      - USB Enumeration Delay: Disabled
      - Front / Rear USB Ports: Enabled
  - ATA Drive Setup:
      - SATA Controller: Enabled
      - Configure SATA as: AHCI
      - Hard-disk Pre Delay: Disabled
```

---

## 🚀 Hướng dẫn cài đặt & Sử dụng

1. **Mount phân vùng EFI trên ổ cứng hoặc USB:**
   * Sử dụng công cụ có sẵn trong repository:
     ```bash
     cd tools/MountEFI && python3 MountEFI.command
     ```
2. **Sao chép thư mục `EFI`:**
   * Copy toàn bộ thư mục **`EFI`** ở thư mục gốc của repo này vào phân vùng EFI vừa mount.
3. **Thêm thông tin SMBIOS (Serial / UUID):**
   * Mở file `EFI/OC/config.plist` bằng *OCAuxiliaryTools* hoặc *ProperTree*.
   * Đi tới mục `PlatformInfo` -> `Generic` và tạo mới các thông số `SystemSerialNumber`, `SystemUUID`, `MLB` (SMBIOS mẫu: `Macmini8,1` hoặc `iMac19,1`).
4. **Khởi động và đặt phân vùng mặc định:**
   * Khi màn hình menu boot OpenCore hiển thị (giao diện GoldenGate), dùng phím mũi tên chuyển đến icon **macOS** và bấm tổ hợp phím **`Ctrl + Enter`** một lần để lưu làm mặc định. Máy sẽ tự động đếm ngược 5 giây và vào thẳng hệ thống.

---

## 📂 Cấu trúc Repository

```text
OpenCore-Lenovo-M720q/
├── EFI/                  # Bản EFI chính thức đang hoạt động ổn định nhất
│   ├── BOOT/             # BOOTx64.efi
│   └── OC/               # OpenCore.efi, config.plist, ACPI, Drivers, Kexts, Resources
├── variants/             # Lưu trữ các biến thể & bản build tham khảo
│   ├── EFI_Upstream_Fork/# Bản gốc từ repository upstream fork
│   ├── EFI15Base14/      # Bản thử nghiệm Sequoia ban đầu
│   ├── EFI15Base14_AX200_AX210/ # Bản tinh chỉnh riêng cho AX200 / AX210
│   ├── EFI15Base14_BTFix/# Bản gốc lưu vết fix Bluetooth
│   └── EFI26Tahoe/       # Bản thử nghiệm macOS 26 Tahoe
├── docs/                 # Tài liệu hướng dẫn cài đặt & kinh nghiệm
├── tools/                # Công cụ MountEFI & script hỗ trợ
├── android/              # Ứng dụng Android OTG USB Transfer (dành cho cấp cứu EFI)
└── reference/            # Báo cáo phần cứng & cấu hình M920Q tham khảo
```

---

## 🛠 Công cụ hỗ trợ độc quyền (Tools & Utilities)

* **MountEFI**: [`tools/MountEFI/`](tools/MountEFI/) - Công cụ mount nhanh phân vùng EFI trên macOS.
* **Android OTG Tools**:
  * Ứng dụng Android (Jetpack Compose + Root): [`android/`](android/) - Ứng dụng sao chép trực tiếp EFI sang USB qua cổng OTG điện thoại khi gặp sự cố không vào được macOS.
  * Hướng dẫn chi tiết: [Android USB EFI Transfer Guide](docs/android-usb-efi-transfer.md).

---

## ⚠️ Lưu ý & Giới hạn (Limitations)

* **AirDrop / Continuity**: Không hỗ trợ trực tiếp qua Intel Wi-Fi (cần thay card Broadcom BCM94360NG nếu muốn dùng native AirDrop/Sidecar).
* **Quản lý Wi-Fi**: Cần cài ứng dụng **[HeliPort](https://github.com/OpenIntelWireless/HeliPort)** trên macOS để kết nối mạng Wi-Fi thông qua `itlwm.kext`.

---

## 📄 License & Disclaimer

Dự án này được chia sẻ cho mục đích nghiên cứu và học tập. Toàn bộ kext và driver thuộc bản quyền của các tác giả thuộc đội ngũ **Acidanthera**, **OpenIntelWireless** và cộng đồng Hackintosh thế giới.
