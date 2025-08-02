# Running Serenity on Raspberry Pi

Serenity currently boots to the desktop on Raspberry Pi 4 and 5 when using a USB drive.
USB mice and keyboards are supported.

The SD host controller driver currently does not work on any of the supported Raspberry Pi models when running on real hardware.

We lack a USB host controller driver for the Raspberry Pi 3, so it currently fails to boot and displays a "Couldn't find a suitable device to boot from" panic message.

32-bit Raspberry Pi models are not supported.

## Running in QEMU

### Step 1: Set Up Serenity

Please follow [build instructions](BuildInstructions.md) to download and build Serenity.

### Step 2: Build and run in emulator

Use the following command to build and run the AArch64 version of the system in an emulated Pi 3:

```console
SERENITY_RUN=raspi3b Meta/serenity.sh run aarch64
```

Replace `raspi3b` with `raspi4b` if you want a Pi 4.

It should build Serenity and open a QEMU window. It should reach the desktop after some time.

You can also run it under gdb with:

```console
SERENITY_RUN=raspi3b Meta/serenity.sh gdb aarch64
```

## Running on real hardware using a USB drive

### Step 0: Build a SerenityOS disk image for Raspberry Pis

In order to create a disk image, you first need to build SerenityOS by running

```console
Meta/serenity.sh build aarch64
```

To create a disk image for the Raspberry Pi 3 and 5, run

```console
ninja -C Build/aarch64 raspberry-pi-image
```

For the Pi 4, booting with EDK II is currently required to enable USB support.
Start by building EDK II for the Pi 4 with

```console
Toolchain/BuildEDK2.sh rpi4
```

After you built EDK II, you can create a SerenityOS disk image with EDK II by running

```console
ninja -C Build/aarch64 raspberry-pi-4-edk2-image
```

The generated disk image will be written to `Build/aarch64/raspberry_pi_disk_image` or `Build/aarch64/raspberry_pi_4_uefi_disk_image`.
Write this image to the USB drive you want to use to boot Serenity using a command such as:

```console
sudo cp Build/aarch64/<disk image> /dev/<usb drive> && sync
```

### Step 1: Connect your Raspberry Pi to your PC using a UART cable

Please follow one of the existing guides (for example [here](https://scribles.net/setting-up-serial-communication-between-raspberry-pi-and-pc)) and make sure UART is working on Raspberry Pi OS before proceeding.
On the Pi 5 you need to use a [Raspberry Pi Debug Probe](https://www.raspberrypi.com/documentation/microcontrollers/debug-probe.html) and attach it to the debug UART connector.

### Step 2: Plug the USB drive in the Raspberry Pi and power on

You should start seeing some messages in your UART terminal window. The default configuration is 115200-8-N-1 (115200 baud, one start bit, 8 data bits, no parity).
