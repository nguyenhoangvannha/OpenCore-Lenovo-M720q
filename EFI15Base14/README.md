# M920Q Sequoia EFI candidate

This folder is a Sequoia 15 candidate based on the EFI that previously booted on this ThinkCentre M920q with Sonoma 14.4. It has not yet been boot-tested with Sequoia.

## Changes in this candidate

- Preserves the M920q-specific ACPI, graphics, audio, SMBIOS, and device properties from the Sonoma EFI.
- Updates OpenCore to 1.0.8 and refreshes the core kexts.
- Enables `itlwm.kext` 2.3.0 and disables the Sonoma-targeted `AirportItlwm.kext`.
- Sets `XhciPortLimit` to `false`.

## Wi-Fi

Install HeliPort in macOS to connect with `itlwm`. HeliPort is an app and is not part of this EFI. Intel AC 7260 has community reports of intermittent disconnects on Sequoia, so keep Ethernet available during installation and initial testing.

## USB status

This EFI still contains the previous `USBInjectAll.kext` setup. A machine-specific USB port map has not been confirmed for this M920q. Validate all ports, sleep/wake, and Bluetooth before relying on this EFI as a daily configuration.

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
