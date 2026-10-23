---
name: openwrt-configuration-assistant
description: >-
  Guides OpenWrt router configuration, network, wireless (Wi-Fi), firewall, DHCP,
  UCI CLI operations, package management, and firmware build configuration.
  Use when managing, diagnosing, or configuring OpenWrt/ImmortalWrt routers or firmware builds.
---

# OpenWrt Configuration Assistant

Provides structured runbooks and best practices for configuring, diagnosing, and maintaining devices running OpenWrt, ImmortalWrt, or QSDK-based firmwares.

## Core Principles

1. **Safety First**: Never apply network or firewall changes blindly that could sever SSH or web access. Always verify syntax before committing.
2. **Atomic UCI Updates**: Group changes into logical UCI transactions, verify with `uci changes`, then `uci commit`, followed by reloading only the impacted daemon.
3. **Rollback Strategy**: When making high-risk network/firewall changes, use a safety timer (e.g. `(sleep 60 && reboot) &` or reload with a fallback script).

---

## 1. UCI (Unified Configuration Interface) Workflow

All persistent system configurations reside in `/etc/config/`.

### Common Commands
```sh
# View current configuration
uci show <package>                  # e.g. uci show network, uci show wireless
uci show <package>.<section>        # e.g. uci show network.lan

# Check pending changes before writing to disk
uci changes

# Commit changes to /etc/config/
uci commit <package>

# Revert uncommitted changes
uci revert <package>
```

### Reloading Services
* **Network & Interfaces**: `/etc/init.d/network reload` or `service network restart`
* **Wi-Fi**: `wifi reload` or `wifi up` / `wifi down`
* **Firewall**: `/etc/init.d/firewall reload`
* **DHCP / DNS**: `/etc/init.d/dnsmasq reload`

---

## 2. Wireless (Wi-Fi) Configuration (`/etc/config/wireless`)

### Băng tần & Chuẩn phát (Wi-Fi Device)
```sh
# Set Country code (e.g., VN, US, CN)
uci set wireless.radio0.country='VN'

# Set Channel & Channel Width
uci set wireless.radio0.channel='auto'       # or specific channel e.g., 36
uci set wireless.radio0.htmode='HT40'        # 2.4GHz: HT20 / HT40; 5GHz: VHT80 / HE160
uci commit wireless && wifi reload
```

### Chế độ Access Point (Phát Wi-Fi)
```sh
uci set wireless.@wifi-iface[0].ssid='MyWiFi'
uci set wireless.@wifi-iface[0].key='MyPassword123'
uci set wireless.@wifi-iface[0].encryption='psk2+ccmp'   # WPA2-PSK (AES)
uci commit wireless && wifi reload
```

### Chế độ Repeater / Client (Thu Wi-Fi - WWAN)
```sh
# Tạo interface STA để kết nối wifi nguồn
uci set wireless.wwan=wifi-iface
uci set wireless.wwan.device='radio0'        # hoặc wifi1
uci set wireless.wwan.network='wwan'
uci set wireless.wwan.mode='sta'
uci set wireless.wwan.ssid='Source_WiFi_SSID'
uci set wireless.wwan.encryption='psk2'
uci set wireless.wwan.key='Source_Password'

# Khai báo interface mạng tương ứng trong /etc/config/network
uci set network.wwan=interface
uci set network.wwan.proto='dhcp'

# Đưa interface wwan vào firewall zone 'wan'
uci add_list firewall.@zone[1].network='wwan'

uci commit wireless
uci commit network
uci commit firewall
/etc/init.d/network reload
/etc/init.d/firewall reload
wifi reload
```

---

## 3. Network & Interface Configuration (`/etc/config/network`)

### Đổi IP LAN Gateway
```sh
uci set network.lan.ipaddr='192.168.30.1'
uci set network.lan.netmask='255.255.255.0'
uci commit network && /etc/init.d/network restart
```

### Cấu hình Static WAN
```sh
uci set network.wan.proto='static'
uci set network.wan.ipaddr='192.168.1.100'
uci set network.wan.netmask='255.255.255.0'
uci set network.wan.gateway='192.168.1.1'
uci set network.wan.dns='8.8.8.8 1.1.1.1'
uci commit network && /etc/init.d/network reload
```

---

## 4. Firewall & Port Forwarding (`/etc/config/firewall`)

### Mở Port (Port Forwarding / NAT)
```sh
uci add firewall redirect
uci set firewall.@redirect[-1].name='PortForward-Web'
uci set firewall.@redirect[-1].src='wan'
uci set firewall.@redirect[-1].src_dport='8080'
uci set firewall.@redirect[-1].dest='lan'
uci set firewall.@redirect[-1].dest_ip='192.168.30.100'
uci set firewall.@redirect[-1].dest_port='80'
uci set firewall.@redirect[-1].proto='tcp'
uci commit firewall && /etc/init.d/firewall reload
```

---

## 5. DHCP & DNS (`/etc/config/dhcp`)

### Gán IP tĩnh theo MAC (Static Lease)
```sh
uci add dhcp host
uci set dhcp.@host[-1].name='MyServer'
uci set dhcp.@host[-1].mac='aa:bb:cc:dd:ee:ff'
uci set dhcp.@host[-1].ip='192.168.30.50'
uci commit dhcp && /etc/init.d/dnsmasq reload
```

### Đổi DNS Upstream
```sh
uci set network.wan.peerdns='0'
uci set network.wan.dns='8.8.8.8 1.1.1.1'
uci commit network && /etc/init.d/network reload
```

---

## 6. Gói phần mềm & Quản trị (`opkg`)

```sh
opkg update
opkg list-installed
opkg install <package-name>   # e.g., curl, htop, tcpdump, luci-app-wireguard
```

---

## 7. Chẩn đoán & Giám sát Hệ thống

* **Kiểm tra trạng thái interface qua Ubus**:
  ```sh
  ubus call network.interface.wan status
  ubus call network.interface.lan status
  ```
* **Danh sách thiết bị kết nối Wi-Fi**:
  * Chuẩn mac80211: `iwinfo <interface> assoclist` hoặc `iw dev <interface> station dump`
  * Chuẩn Qualcomm QCA: `wlanconfig <interface> list sta`
* **Kiểm tra nhật ký hệ thống**:
  ```sh
  logread | tail -n 50
  dmesg | tail -n 50
  ```
