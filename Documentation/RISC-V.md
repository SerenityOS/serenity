# Building and running SerenityOS on RISC-V

## NOTE

SerenityOS on RISC-V 64 bits (RV64G / RV64IMAFDZicsr_Zifencei, named riscv64) is in very early development. Use this guide if you want to setup a development environment.

## Building

SerenityOS has RV64 Clang and GCC toolchains. Since only one Clang build is required for all architectures, using this toolchain may be faster since it doesn't need to be built separately.

Currently, booting in QEMU via U-Boot is recommended. For this you need to build U-Boot via the `Toolchain/BuildUBoot.sh` script. This requires any riscv64 cross compiler; if you do not have a SerenityOS toolchain it can also use gcc-riscv64-linux-gnu.

## Running in QEMU

run.py does not support running riscv64 in QEMU via U-Boot. The following command line should be adopted instead:

```sh
# Assuming you built QEMU with BuildQEMU.sh
Toolchain/Local/qemu/bin/qemu-system-riscv64 -M virt -kernel Toolchain/Local/u-boot-riscv64/u-boot -drive id=disk,file=Build/riscv64clang/_disk_image,format=raw,if=none -device nvme,drive=disk,serial=deadbeef -m 2G -device VGA -serial mon:stdio
```

Change the `_disk_image` path depending on which toolchain you're using, and adopt the RAM amount as needed.
