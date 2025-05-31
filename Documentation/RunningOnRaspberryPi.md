# Running Serenity on Raspberry Pi

Serenity currently boots to the desktop on Raspberry Pi 4 and 5 when using a USB drive.
USB mice and keyboards are supported.

On the Raspberry Pi 4, only the USB-C port is supported at this time. This means you’ll need to either:

-   Use a docking station that can both power the Pi and function as a USB hub, or
-   Provide power through another method while connecting your USB drive.

To use this USB-C port, you need to set [`otg_mode=1`](https://www.raspberrypi.com/documentation/computers/config_txt.html#otg_mode-raspberry-pi-4-only) in config.txt.
The Raspberry Pi 4 firmware doesn't seem to support booting from this port, so you need to put the kernel and boot files
on another USB drive or SD card (or just the entire `Build/aarch64/raspberry_pi_disk_image` image).

The SD host controller driver currently does not work on any of the supported Raspberry Pi models when running on real hardware.

We also lack a USB host controller driver for the Raspberry Pi 3, so it currently fails to boot and displays a "Couldn't find a suitable device to boot from" panic message.

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

You can build a disk image for the Raspberry Pi 3, 4, and 5 with:

```console
ninja -C Build/aarch64 raspberry-pi-image
```

Note that this command doesn't (re)build the system, so you need to run `Meta/serenity.sh build aarch64` first.

The generated disk image will be written to `Build/aarch64/raspberry_pi_disk_image`.
Write this image to the USB drive you want to use to boot Serenity using a command such as:

```console
sudo cp Build/aarch64/raspberry_pi_disk_image /dev/sdx && sync
```

(Replace `/dev/sdx` with your USB drive device file)

### Step 1: Connect your Raspberry Pi to your PC using a UART cable

Please follow one of the existing guides (for example [here](https://scribles.net/setting-up-serial-communication-between-raspberry-pi-and-pc)) and make sure UART is working on Raspberry Pi OS before proceeding.
On the Pi 5 you need to use a [Raspberry Pi Debug Probe](https://www.raspberrypi.com/documentation/microcontrollers/debug-probe.html) and attach it to the debug UART connector.

### Step 2: Plug the USB drive in the Raspberry Pi and power on

You should start seeing some messages in your UART terminal window. The default configuration is 115200-8-N-1 (115200 baud, one start bit, 8 data bits, no parity).
