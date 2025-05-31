# Running Serenity on Raspberry Pi

## NOTE

This is for development purposes only - Serenity doesn't currently boot on Raspberry Pi! Use this guide if you want to set up a development environment.

64-bit only, so you need a Raspberry Pi 3 or newer.

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

## Running on real hardware using an SD card

### Step 0: Build a SerenityOS SD card image

You can build an SD card image for the Raspberry Pi 3 and 4 with:

```console
ninja -C Build/aarch64 raspberry-pi-image
```

The generated disk image will be written to `Build/aarch64/raspberry_pi_disk_image`.
Write this image to the SD card you want to use to boot Serenity using a command such as:

```console
sudo cp Build/aarch64/raspberry_pi_disk_image /dev/sdx && sync
```

(Replace `/dev/sdx` with your sd card device file)

### Step 1: Connect your Raspberry Pi to your PC using a UART cable

Please follow one of the existing guides (for example [here](https://scribles.net/setting-up-serial-communication-between-raspberry-pi-and-pc)) and make sure UART is working on Raspberry Pi OS before proceeding.

### Step 2: Put the SD Card in the Raspberry Pi and power on

You should start seeing some messages in your UART terminal window. The default configuration is 115200-8-N-1 (115200 baud, one start bit, 8 data bits, no parity).
