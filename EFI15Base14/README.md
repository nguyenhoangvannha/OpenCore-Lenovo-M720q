# M920Q Sequoia EFI candidate

This folder is a Sequoia 15 candidate based on the EFI that previously booted on this ThinkCentre M920q with Sonoma 14.4. It has not yet been boot-tested with Sequoia.

## Changes in this candidate

- Preserves the M920q-specific ACPI, graphics, audio, SMBIOS, and device properties from the Sonoma EFI.
- Updates OpenCore to 1.0.8 and refreshes the core kexts.
- Enables `itlwm.kext` 2.3.0 and disables the Sonoma-targeted `AirportItlwm.kext`.
- Replaces broken `USBInjectAll.kext` + `XHCI-unsupported.kext` with `USBToolBox.kext` + `UTBMap.kext` (macOS 11.3+ / Sequoia compatible port map).
- Sets `XhciPortLimit` to `false`.
- Configures `BlueToolFixup.kext` with `MinKernel=21.0.0` and adds `-btlfxallowanyaddr` to boot-args for macOS 15 Sequoia Bluetooth compatibility.

## Hardware Confirmed (This Device)

- **System:** Lenovo ThinkCentre M920q Tiny (Q370 Chipset)
- **CPU:** Intel Core Gen 8 Coffee Lake (ES `0000 @ 1.60GHz`)
- **iGPU:** Intel UHD Graphics 630 (`[8086:3e92]`)
- **Audio:** Realtek ALC233 (`alcid=13`)
- **LAN:** Intel Ethernet Connection I219-LM (`[8086:15bb]`)
- **Wi-Fi / BT:** Intel Dual Band Wireless-AC 7260 (`[8086:08b2]`, BT `8087:07dc`)
- **Storage:** WD PC SN520 512GB NVMe

## Wi-Fi & Bluetooth

- **Wi-Fi:** Install HeliPort in macOS to connect with `itlwm`. Intel AC 7260 has community reports of intermittent disconnects on Sequoia, so keep Ethernet available during installation and initial testing.
- **Bluetooth:** Handled by `IntelBluetoothFirmware.kext`, `IntelBTPatcher.kext`, and `BlueToolFixup.kext` with the `-btlfxallowanyaddr` boot-arg.

## USB Architecture & Official Lenovo Hardware Documentation

### Official Lenovo ThinkCentre M920q Tiny USB Port Layout

According to the official **Lenovo PSREF (Product Specifications Reference)** and **Hardware Maintenance Manual**, the ThinkCentre M920q Tiny **does not possess any physical legacy USB 2.0 ports**. All exterior ports are modern USB 3.1:

#### Front Panel:
```text
+-----------------------------------------------------------+
| [Power Button]  [Storage LED]  [Mic]  [Headphone Combo]   |
| [USB 3.1 Gen 1 Type-A (Always On)]   [USB-C 3.1 Gen 1]    |
+-----------------------------------------------------------+
```
- **1x USB 3.1 Gen 1 Type-A** (5 Gbps, supports Yellow/Always-On rapid charging)
- **1x USB-C 3.1 Gen 1** (5 Gbps)
- **2x Audio Jacks** (1x 3.5mm microphone, 1x 3.5mm combo headphone/mic)

#### Rear Panel:
```text
+---------------------------------------------------------------------------------+
| [Power Input]     [USB 3.1 Gen 2 (10G)]   [DisplayPort]   [Ethernet RJ-45]      |
| [Opt. Punch-out*] [USB 3.1 Gen 1 (5G) ]   [HDMI Out   ]   [USB 3.1 Gen 1 (5G)]  |
+---------------------------------------------------------------------------------+
*(Note: An additional 2nd USB 3.1 Gen 2 port is located on the rear depending on model layout).*
```
- **2x USB 3.1 Gen 1 Type-A** (5 Gbps)
- **2x USB 3.1 Gen 2 Type-A** (10 Gbps)
- **1x DisplayPort Out** & **1x HDMI Out**
- **1x Gigabit Ethernet (RJ-45)**: Intel I219-LM
- **1x Optional Punch-out Slot**: May house optional serial port, 2nd DP, 2nd HDMI, USB-C DP, or PCIe card.

### How USB 2.0 (HS) vs USB 3.0 (SS) Operates on These Ports

Even though all physical ports are USB 3.1 (5G/10G), **every USB 3.x Type-A and Type-C port incorporates two separate logical lines inside the Intel Q370 xHCI controller**:
1. **HighSpeed (HS - USB 2.0 logic):** Uses standard 4-pin connections (`VBUS`, `D-`, `D+`, `GND`). When you plug in a mouse, keyboard, or a **USB 2.0 flash drive** (black/white connector with 4 pins) into *any* port, only the HS logic is activated.
2. **SuperSpeed (SS - USB 3.0/3.1 logic):** Uses 5 additional deep pins (`SSTX+`, `SSTX-`, `SSRX+`, `SSRX-`, `GND_DRAIN`). When plugging in a USB 3.0/3.1 drive (blue/teal connector), the controller negotiates over the SS line.

---

## Detailed Root Cause Analysis: Sonoma vs Sequoia Boot Failure

### The Problem
During macOS Sequoia installation from a USB installer, the verbose log reached `DSMOS has arrived`, followed by:
- A prohibitory circle-with-slash symbol `🚫` (`support.apple.com/mac/startup`)
- Screen corruption and the kernel halt message: **`Still waiting for root device`**

### Why Did the Previous Sonoma Config Boot While Sequoia Failed?

Reviewing the working reference backup (`reference/Working-But-Not-Sure-Best-Config-Sonoma-14.4-M920Q`) revealed the exact technical discrepancies:

1. **`XhciPortLimit` Difference (`True` vs `False`):**
   - In the **Sonoma reference config**, `Kernel -> Quirks -> XhciPortLimit` was set to **`True`** along with `USBInjectAll.kext` (v0.8.0).
   - In the **initial Sequoia candidate**, `XhciPortLimit` was toggled to **`False`** (following OpenCore general guidelines).
   - **The Conflict:** Intel Coffee Lake Q370 has **26 logical ports** (HS01–HS14, SS01–SS10, USR1–USR2). When using `USBInjectAll` with `XhciPortLimit = False`, macOS enforces its strict **15-port maximum per controller**. Because macOS enumerates HS ports first, ports 1 through 14 were filled exclusively by USB 2.0 (HS) lines. **All SuperSpeed USB 3.0 lines (SS01–SS10, starting at index 16+) were completely dropped.** The moment the macOS kernel initialized its USB driver (`AppleUSBXHCIPCI`), the USB 3.0 installer drive lost its connection, producing `Still waiting for root device`.

2. **Booting From Internal NVMe vs Booting From USB Installer:**
   - The Sonoma reference EFI was used on an **already installed system booting directly from internal NVMe SSD (`WD PC SN520`)** via PCIe.
   - An internal NVMe drive communicates via `IONVMeFamily` and does not depend on USB controller handoff. Even with broken or unmapped USB ports, macOS still boots directly to Desktop.
   - A **macOS USB Installer**, however, relies 100% on the USB flash drive being readable throughout the entire boot sequence. If the USB port drops during controller handoff, the installer crashes immediately.

3. **Apple USB Stack Tightening in macOS 15 Sequoia:**
   - Apple significantly overhauled `AppleUSBXHCIPCI` starting with macOS 11.3 Big Sur, and continuing aggressively through Monterey, Ventura, Sonoma, and Sequoia.
   - Legacy shims and RehabMan's old `USBInjectAll.kext` can no longer reliably inject port definitions or bypass controller boundaries without native port mapping.

---

## The Solution: Native Mapping with USBToolBox + UTBMap

To permanently eliminate USB dropping and kernel stalls, `USBInjectAll.kext` and `XHCI-unsupported.kext` have been disabled, replaced by **`USBToolBox.kext` (1.2.0)** and **`UTBMap.kext` (1.1)** tailored specifically for the M720q / M920q topology:

| Port | Type / Connector | Description |
|---|---|---|
| **HS01 / SS01** | USB 3.0 Type-A (`UsbConnector=3`) | Front USB Type-A port |
| **HS02 / SS02** | Type-C (`UsbConnector=9`) | Front USB Type-C port |
| **HS03 / SS03** | USB 3.0 Type-A (`UsbConnector=3`) | Rear USB port 1 |
| **HS04 / SS04** | USB 3.0 Type-A (`UsbConnector=3`) | Rear USB port 2 |
| **HS05 / SS05** | USB 3.0 Type-A (`UsbConnector=3`) | Rear USB port 3 |
| **HS06 / SS06** | USB 3.0 Type-A (`UsbConnector=3`) | Rear USB port 4 |
| **HS07** | Internal (`UsbConnector=255`) | Internal Bluetooth header (`8087:07dc`) |

*Total mapped ports: **13 logical ports** (safely below Apple's 15-port limit).*

### Practical Advice When Booting Installer:
- **Plug into Rear Ports:** Always use a rear USB port (preferably the bottom port adjacent to the Intel RJ-45 Ethernet port) for the installer.
- **USB 2.0 Flash Drive / Hub Tip:** If using an unmapped EFI or encountering issues, plugging through a USB 2.0 extension cable, USB 2.0 hub, or using a USB 2.0 flash drive forces the port to connect via the HS line, bypassing any USB 3.0 SuperSpeed link negotiation drops.

## BIOS Settings (Lenovo ThinkCentre M920q)

Press `F1` on boot to enter BIOS Setup. Configure the following:

### Disable:
- **Security -> Secure Boot:** `Disabled` *(Critical: prevents OpenCore boot)*
- **Devices -> Serial Port:** `Disabled`
- **Devices -> USB Setup -> USB Legacy Support:** `Disabled`
- **Startup -> CSM (Compatibility Support Module):** `Disabled` *(UEFI only)*
- **Security -> Intel SGX:** `Disabled`
- **Power -> Hard-disk Pre Delay:** `Disabled`

### Enable:
- **Devices -> ATA Drive Setup -> Configure SATA as:** `AHCI`
- **Devices -> Video Setup:**
  - **Active Video:** `IGD` (Intel Onboard)
  - **Pre-Allocated Memory (DVMT):** `64MB` (or 32MB minimum)
  - **Total Graphics Memory:** `MAX`
- **Devices -> USB Setup:**
  - **USB Support:** `Enabled`
  - **Front USB Ports / Rear USB Ports:** `Enabled`
- **Advanced / CPU Setup:**
  - **Intel (VMX) Virtualization:** `Enabled`
  - **VT-d:** `Enabled` *(handled safely by OpenCore)*
  - **Hyper-Threading:** `Enabled`
  - **CFG Lock (if visible):** `Disabled` *(Note: `AppleXcpmCfgLock=True` is enabled in OpenCore as fallback)*

## Before use

- Keep the known-working Sonoma EFI unchanged and use a separate USB EFI for testing.
- Confirm the system's CPU and BIOS settings before changing ACPI or CFG Lock settings.
- Do not share this config publicly without replacing its SMBIOS serial, MLB, and UUID with your own values.

## Step-by-Step macOS Sequoia Installation Guide

Follow these steps once you plug the installer USB into a **rear USB port** of the ThinkCentre M920q:

### 1. Booting the USB Installer
1. Plug your USB into a **rear USB 3.0 port** (avoid front Type-C or front USB ports for initial boot).
2. Connect your display via **DisplayPort** (recommended) or HDMI.
3. Plug in an Ethernet cable (Intel LAN) for internet access during installation.
4. Power on the machine and immediately press **`F12`** repeatedly to bring up the Lenovo Boot Menu.
5. Select your USB drive (marked as `UEFI: SanDisk...`).
6. At the OpenCore boot picker menu:
   - Select **`Install macOS Sequoia (external)`** and press `Enter`.
   - The screen will show verbose scrolling text (`-v`). Wait 1–3 minutes until the macOS Recovery / Installer screen appears.

### 2. Formatting the Drive (Disk Utility)
1. On the macOS Recovery screen, select **Disk Utility** -> Click **Continue**.
2. In the top-left menu bar, click **View** -> select **Show All Devices** (`Cmd + 2`).
3. Under the *Internal* section on the left sidebar:
   - Select the main NVMe drive (e.g. `WDC PC SN520...` parent disk, or select the specific partition you wish to install on).
4. Click **Erase** at the top:
   - **Name:** `Macintosh HD` (or any name you prefer)
   - **Format:** `APFS`
   - **Scheme:** `GUID Partition Map`
5. Click **Erase** and wait for it to complete, then close Disk Utility (`Cmd + Q`).

### 3. Running the Installer
1. Back on the Recovery menu, select **Install macOS Sequoia** -> Click **Continue**.
2. Agree to the Apple software license terms.
3. Choose the target disk you just formatted (`Macintosh HD`) -> Click **Continue**.
4. The initial installer copy phase will take about 15–25 minutes.

### 4. Automatic Reboots (Crucial Step!)
The machine will restart 2–4 times during the installation.
- Leave the USB plugged in.
- Each time the machine reboots, OpenCore should automatically pick the next boot stage. If the menu appears:
  - **Reboot 1 & 2:** Select **`macOS Installer`** (from Macintosh HD).
  - **Final Reboot:** Select **`Macintosh HD`**.
- *Note:* Do not select `Install macOS Sequoia (external)` again after the first phase, otherwise it will restart the setup from the beginning!

### 5. First Boot & Setup Assistant
1. When setup finishes, you will see the macOS Welcome / Country selection screen.
2. Follow the on-screen prompts to create your local user account.
3. *Recommendation during setup:* Select "My computer does not connect to the Internet" or skip Apple ID sign-in initially until you're at the Desktop.

### 6. Post-Installation (Mounting EFI to Internal NVMe)
Once on the macOS Desktop:
1. Copy the `EFI` folder from your USB to the internal NVMe EFI partition using **MountEFI** or **OCAT** (located in `Hackintosh_M920Q/04_EFI_Backups/Working/`).
2. Install **HeliPort.app** (from the same folder) into `/Applications` to connect to Wi-Fi networks using `itlwm.kext`.
3. Unplug the USB and reboot to confirm the machine boots independently from the internal SSD.

## Component versions

- OpenCore: 1.0.8
- Lilu: 1.7.2
- VirtualSMC, SMCProcessor, SMCSuperIO: 1.3.8
- AppleALC: 1.9.8
- WhateverGreen: 1.7.1
- IntelMausi: 1.0.8
- NVMeFix: 1.1.3
- FeatureUnlock: 1.1.8
- IntelBluetoothFirmware, IntelBTPatcher: 2.4.0
- BlueToolFixup: 2.7.2
- itlwm: 2.3.0
- USBToolBox: 1.2.0
- UTBMap: 1.1 (M720q / M920q tailored map)

