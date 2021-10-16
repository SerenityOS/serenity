# Running Serenity on Raspberry Pi

## NOTE

This is for development purposes only - Serenity doesn't currently boot on Rasperry Pi! Use this guide if you want to set up a development environment.

Currently only UART output is supported, no display.

64-bit only, so you need a Rasperry Pi 3 or newer.

## Running in QEMU

### Step 1: Set Up Serenity

Please follow [build instructions](BuildInstructions.md) to download and build Serenity. Make sure everything builds successfully for x86.

### Step 2: Build Aarch64 toolchain

Use following the command to build the toolchain for Aarch64:

```console
Meta/serenity.sh rebuild-toolchain aarch64
```

### Step 3: Build and run in emulator

Use the following command to build and run the Aarch64 kernel:

```console
Meta/serenity.sh run aarch64
```

It should build Serenity and open a QEMU window, similar to the x86 version. You should see some messages in the terminal.

You can also run it under gdb with:

```console
Meta/serenity.sh gdb aarch64
```

## Running on real hardware using an SD Card

### Step 0: Download and run Raspberry Pi OS from an SD Card

This step is needed because the original firmware files need to be present on the SD Card when booting Serenity. It will also help with the UART setup.

### Step 1: Connect your Raspberry Pi to your PC using a UART cable

Please follow one of the existing guides (for example [here](https://scribles.net/setting-up-serial-communication-between-raspberry-pi-and-pc)) and make sure UART is working on Raspberry Pi OS before proceeding.

### Step 2: Mount SD Card

If you use a Raspberry Pi 4, and your serenity kernel is called `kernel8.img`
(the default), and you don't have any other `kernel*.img` files on your SD
card, make sure `config.txt` is empty.

If you want to use filename that isn't `kernel8.img` or if you want to keep
other `kernel*.img` files on your SD card, put this in config.txt:

```
arm_64bit=1
kernel=myfilename.img
```

If you use a Raspberry Pi 3, put this in config.txt:

```
enable_uart=1
```

### Step 3: Copy Serenity kernel to SD Card

`kernel8.img` can be found in `Build/aarch64/Kernel/Prekernel/`. Copy it to the main directory on the `Boot/` partition, next to `config.txt`. You can either replace the original file or use another name (see above).

### Step 4: Put the SD Card in the Raspberry Pi and power on

You should start seeing some messages in your UART terminal window.

## Running on real hardware using network (Raspberry Pi 3)

### Prerequisites

There are multiple ways to set up your network. The easiest way is a direct connection between the Raspberry Pi and your PC. To achieve this your PC has to have an Ethernet port.

Here's the [Raspberry Pi Documentation](https://www.raspberrypi.com/documentation/computers/raspberry-pi.html#debugging-network-boot-mode) on booting from the network.

### Step 1: Make sure OTP mode is enabled on the board 

This is enabled by default on Raspberry Pi 3+. For the previous boards please see the section [Debugging Network Boot Mode](https://www.raspberrypi.com/documentation/computers/raspberry-pi.html#debugging-network-boot-mode) of the Raspberry Pi documentation.

### Step 2: Copy all files from the original SD Card to your PC

This directory will serve as a TFTP server, sending files to the Raspberry Pi when requested.

### Step 2: Set up the network interface

Switch the network interface to static mode (static IP) and disable the firewall.

### Step 3: Set up network services

Booting Raspberry Pi requires DHCP and TFTP servers.

On Windows, you can use the [Tftpd32](https://bitbucket.org/phjounin/tftpd64/src/master/) program.

Example configuration for DHCP:

![](Tftpd32_Dhcp.png)

Make sure you **disable** the `Ping address before assignment` option.

Example configuration for TFTP:

![](Tftpd32_Tftp.png)

The only option worth noting is `Base Directory` which should contain the files from the SD Card.

### Step 4: Power up the Raspberry Pi

Remove the SD Card, connect an Ethernet cable between the Raspberry Pi and your PC and power on the board.

After 5-10 seconds you should see files being served by the TFTP server:

![](Tftpd32_Serving.png)

The system should boot normally as it would from the SD Card.

### Step 5: Modify config.txt and copy Serenity kernel

Similarly to booting from SD Card (see above), modify `config.txt` and copy the Serenity kernel to the TFTP directory.

### Step 6: Reset Raspberry Pi

You should start seeing some Serenity messages in your UART terminal window.
