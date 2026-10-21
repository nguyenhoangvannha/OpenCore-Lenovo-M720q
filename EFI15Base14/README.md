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

## USB Mapping & Status

Previously, this candidate relied on `USBInjectAll.kext` and `XHCI-unsupported.kext`. Since **macOS Big Sur 11.3+ through macOS 15 Sequoia**, `USBInjectAll.kext` and the `XhciPortLimit` quirk are completely deprecated/broken by Apple's updated USB stack, leading directly to the `Still waiting for root device` error (prohibitory sign `🚫`) during boot because macOS drops the USB installer drive.

This EFI now uses **`USBToolBox.kext` (1.2.0)** paired with **`UTBMap.kext`**, custom-mapped for the Lenovo ThinkCentre M720q / M920q:

| Port | Type / Connector | Description |
|---|---|---|
| **HS01 / SS01** | USB 3.0 Type-A (`UsbConnector=3`) | Front USB Type-A port |
| **HS02 / SS02** | Type-C (`UsbConnector=9`) | Front USB Type-C port |
| **HS03 / SS03** | USB 3.0 Type-A (`UsbConnector=3`) | Rear USB port 1 |
| **HS04 / SS04** | USB 3.0 Type-A (`UsbConnector=3`) | Rear USB port 2 |
| **HS05 / SS05** | USB 3.0 Type-A (`UsbConnector=3`) | Rear USB port 3 |
| **HS06 / SS06** | USB 3.0 Type-A (`UsbConnector=3`) | Rear USB port 4 |
| **HS07** | Internal (`UsbConnector=255`) | Internal Bluetooth header (`8087:07dc`) |

*Total mapped ports: 13 ports (comfortably under macOS's 15-port-per-controller limit).*

### Troubleshooting: "Still waiting for root device" (Prohibited Sign 🚫)
If you encounter a prohibitory sign `🚫` (`support.apple.com/mac/startup`) or the verbose message `Still waiting for root device`:
1. **Use Rear USB Ports:** Always plug the installer drive into a **rear USB 3.0 port** on the back of the machine (preferably near the Intel LAN port). Avoid front USB or Type-C ports during the installation phase.
2. **USB 2.0 Fallback:** If your USB drive controller continues to reset, insert it into a USB 2.0 port / USB 2.0 extension hub, or burn the installer to a USB 2.0 flash drive.
3. **Verify UTBMap:** Ensure `USBToolBox.kext` and `UTBMap.kext` are both enabled in `config.plist`, and `USBInjectAll.kext` / `XHCI-unsupported.kext` remain disabled.

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

