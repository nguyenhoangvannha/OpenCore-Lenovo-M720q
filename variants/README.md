# EFI Variants & Archive

Thư mục này lưu trữ các biến thể cấu hình EFI OpenCore khác nhau cho dòng máy **Lenovo ThinkCentre M720q / M920q Tiny**:

| Thư mục | Mô tả cấu hình | Trạng thái |
| :--- | :--- | :--- |
| **`EFI_Upstream_Fork/`** | Cấu hình EFI nguyên bản từ repo gốc được fork (Macmini8,1, SSDT chuẩn, AudioDxe). | Lưu trữ tham chiếu |
| **`EFI15Base14/`** | Bản Sequoia ban đầu (chuyển sang USBToolBox + UTBMap thay cho USBInjectAll). | Thử nghiệm ban đầu |
| **`EFI15Base14_AX200_AX210/`** | Bản Sequoia tinh chỉnh cho card Wi-Fi Intel AX200 / AX210 thế hệ mới. | Hoạt động tốt với AX200/AX210 |
| **`EFI15Base14_BTFix/`** | Bản Sequoia đã fix triệt để lỗi Bluetooth chớp tắt trên card Intel AC 7260. *(Đã được đồng bộ thành bản chính tại `/EFI`)* | **Hoạt động hoàn hảo (Active)** |
| **`EFI26Tahoe/`** | Bản cấu hình thử nghiệm tương lai cho macOS 26 Tahoe (SimpleHDA, RestrictEvents). | Nghiên cứu phát triển |

> [!TIP]
> Bản EFI hoàn chỉnh, sẵn sàng sử dụng nhất nằm trực tiếp tại thư mục gốc: **[`/EFI`](../EFI)**.
