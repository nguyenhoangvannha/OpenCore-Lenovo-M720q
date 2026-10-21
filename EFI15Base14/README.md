# M920Q Sequoia EFI candidate

This folder is a Sequoia 15 candidate based on the EFI that previously booted on this ThinkCentre M920q with Sonoma 14.4. It has not yet been boot-tested with Sequoia.

## Changes in this candidate

- Preserves the M920q-specific ACPI, graphics, audio, SMBIOS, and device properties from the Sonoma EFI.
- Updates OpenCore to 1.0.8 and refreshes the core kexts.
- Enables `itlwm.kext` 2.3.0 and disables the Sonoma-targeted `AirportItlwm.kext`.
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

## USB status

This EFI still contains the previous `USBInjectAll.kext` setup. `reference/MyM920Q/DxDiag.txt` identifies the Intel USB controller (`8086:A36D`) and shows Windows locations for devices attached during that report, including ports 7 and 8. That is useful topology evidence, but it does not enumerate every physical port or establish the macOS HS/SS port mapping. A complete machine-specific map has therefore not been confirmed. Validate every port, sleep/wake, and Bluetooth before relying on this EFI as a daily configuration. When booting the installer, use the USB ports on the back of the machine.

## Before use

- Keep the known-working Sonoma EFI unchanged and use a separate USB EFI for testing.
- Confirm the system's CPU and BIOS settings before changing ACPI or CFG Lock settings.
- Do not share this config publicly without replacing its SMBIOS serial, MLB, and UUID with your own values.

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

