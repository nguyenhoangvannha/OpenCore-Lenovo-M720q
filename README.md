# OpenCore - Lenovo M720Q Tiny

<!-- <p align="center"> -->
<!--     <img height="auto" width="auto" src="images/screenshot.png" /> -->
<!-- </p> -->

## Configuration for macOS 15 Sequoia

> [!CAUTION]
> I don't take any responsibility for you voilating the Apple ToS and/or damaging your device.

## Installation

For a step-by-step walkthrough, see the [Hackintosh Installation Guide](docs/installation-guide.md).

> [!IMPORTANT]  
> This repository ships without kexts except for `UTBMap`. Run `./download-kexts.sh` to download them before using the EFI.

```
PlatformInfo -> Generic -> Add Serial, SystemUUID and MLB for SMBIOS Macmini8,1
```

## Updating Kexts

Run the update script to download the latest kexts from their respective GitHub releases:

```bash
./download-kexts.sh
```

This will update the following kexts:

| Kext | Source |
|------|--------|
| Lilu | [acidanthera/Lilu](https://github.com/acidanthera/Lilu) |
| VirtualSMC | [acidanthera/VirtualSMC](https://github.com/acidanthera/VirtualSMC) |
| AppleALC | [acidanthera/AppleALC](https://github.com/acidanthera/AppleALC) |
| WhateverGreen | [acidanthera/WhateverGreen](https://github.com/acidanthera/WhateverGreen) |
| IntelMausi | [acidanthera/IntelMausi](https://github.com/acidanthera/IntelMausi) |
| NVMeFix | [acidanthera/NVMeFix](https://github.com/acidanthera/NVMeFix) |
| IntelBluetoothFirmware | [OpenIntelWireless/IntelBluetoothFirmware](https://github.com/OpenIntelWireless/IntelBluetoothFirmware) |
| IntelBTPatcher | [OpenIntelWireless/IntelBluetoothFirmware](https://github.com/OpenIntelWireless/IntelBluetoothFirmware) |
| BlueToolFixup | [OpenIntelWireless/IntelBluetoothFirmware](https://github.com/OpenIntelWireless/IntelBluetoothFirmware) |
| itlwm | [OpenIntelWireless/itlwm](https://github.com/OpenIntelWireless/itlwm) |
| USBToolBox | [USBToolBox/kext](https://github.com/USBToolBox/kext) |

> [!NOTE]
> `UTBMap.kext` is machine-specific and will not be updated by the script.

## Base Specs

- CPU: i3-8100T
- GPU: Intel HD630 (DP, HDMI)
- Motherboard: Intel B360
- Audio Codec: ALC235
- WiFi & Bluetooth: Intel AX210

## BIOS

```md
Secure Boot - Disabed
Serial Port - Disabled
USB Support - Enabled
USB Legacy Support - Disabled
USB Enumeration Delay - Disabled
Front USB Ports , Rear USB Ports - Enabled
SATA Controller - Enabled
Configure SATA as - AHCI
Hard-disk Pre Delay - Disabled
```

## Limitations 

- No AirDrop support due to Intel WiFi (can be fixed by using _BCM_ card and _OCLP_).
- No compatibility for software that requires T2 chip. Such as Apple Inteligence, iPhone Mirroring etc.

## Notes

- WiFi will be shown as Ethernet Interface. Joining networks requires [HeliPort](https://github.com/OpenIntelWireless/HeliPort).

## Tools & Utilities

- **MountEFI**: [`tools/MountEFI/`](tools/MountEFI/) - Python & Automator Quick Action tool by CorpNewt to mount EFI partitions on macOS.
- **Android OTG Tools**:
  - Android App (Jetpack Compose + Root): [`android/`](android/) - GUI tool with Root permissions to scan USB partitions, mount, copy EFI and unmount safely.
  - Detailed guide: [Android USB EFI Transfer Guide](docs/android-usb-efi-transfer.md)
  - Root/Termux script: [`tools/android-efi-transfer.sh`](tools/android-efi-transfer.sh)
