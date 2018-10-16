OBJS = \
       _start.o \
       init.o \
       VGA.o \
       kmalloc.o \
       StdLib.o \
       i386.o \
       Task.o \
       i8253.o \
       Keyboard.o \
       IPC.o \
       CMOS.o \
       IO.o \
       PIC.o \
       Syscall.o \
       DataBuffer.o \
       String.o \
       panel.o \
       Disk.o \
       fs.o \
       Userspace.o
NASM = nasm
KERNEL = kernel
BOOTLOADER = Boot/boot.bin
IMAGE = .floppy-image
ARCH_FLAGS =
STANDARD_FLAGS = -std=c++17 -nostdinc++ -nostdlib
KERNEL_FLAGS = -ffreestanding -fno-stack-protector -fno-ident
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings
FLAVOR_FLAGS = -fomit-frame-pointer -mregparm=3 -march=i386 -m32 -fno-exceptions -fno-rtti -ffunction-sections -fdata-sections -fmerge-all-constants -fno-unroll-loops -falign-functions=1 -falign-jumps=1 -falign-loops=1
#FLAVOR_FLAGS = -fomit-frame-pointer -mregparm=3 -march=i386 -m32 -fno-exceptions -fno-rtti -ffunction-sections -fdata-sections
OPTIMIZATION_FLAGS = -Os -fno-asynchronous-unwind-tables
INCLUDE_FLAGS = -Iinclude/ -I.
CXXFLAGS = $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(KERNEL_FLAGS) $(FLAVOR_FLAGS) $(ARCH_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS)
#CXX = /usr/local/gcc-4.8.1-for-linux64/bin/x86_64-pc-linux-g++
#LD = /usr/local/gcc-4.8.1-for-linux64/bin/x86_64-pc-linux-ld
CXX = g++
LD = ld
LDFLAGS = -T linker.ld --strip-debug -melf_i386 --gc-sections --build-id=none -z norelro -z now


all: $(KERNEL) $(IMAGE)

$(KERNEL): $(OBJS)
	@echo "LD $@"; $(LD) $(LDFLAGS) -o $@ -Ttext 0x10000 $(OBJS)

$(IMAGE): $(KERNEL) $(BOOTLOADER)
	@echo "CREATE $@"; cat $(BOOTLOADER) $(KERNEL) > $(IMAGE)

$(BOOTLOADER): Boot/boot.asm
	@echo "NASM $<"; $(NASM) -f bin -o $@ $<

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

clean:
	@echo "CLEAN"; rm -f $(KERNEL) $(OBJS) $(BOOTLOADER) $(IMAGE)

