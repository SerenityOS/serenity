# Building and running SerenityOS on RISC-V

## NOTE

SerenityOS on [RISC-V](https://riscv.org) (RV64G / RV64IMAFDZicsr_Zifencei, named riscv64) is in very early development. Use this guide if you want to setup a development environment, and ask in the #risc-v channel if you need assistance.

## Building

SerenityOS has RV64 Clang and GCC toolchains. The GCC toolchain is required for building U-Boot, but both toolchains can be used for SerenityOS.

Currently, booting in QEMU via U-Boot is recommended. For this you need to build U-Boot via the `Toolchain/BuildUBoot.sh` script. This currently requires the GCC toolchain due to missing upstream support for Clang on RISC-V; if you do not have a SerenityOS toolchain you can also install and use the Linux cross-compiler gcc-riscv64-linux-gnu.

## Running in QEMU

run.py does not support running riscv64 in QEMU via U-Boot. The following command line should be adopted instead:

```sh
# Assuming you built QEMU with BuildQEMU.sh
Toolchain/Local/qemu/bin/qemu-system-riscv64 -M virt -kernel Toolchain/Local/u-boot-riscv64/u-boot -drive id=disk,file=Build/riscv64clang/_disk_image,format=raw,if=none -device nvme,drive=disk,serial=deadbeef -m 2G -device VGA -serial mon:stdio
```

Change the `_disk_image` path depending on which toolchain you're using, and adopt the RAM amount as needed.
