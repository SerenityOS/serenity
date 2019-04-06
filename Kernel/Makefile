KERNEL_OBJS = \
       init.o \
       kmalloc.o \
       StdLib.o \
       i386.o \
       Process.o \
       Thread.o \
       i8253.o \
       Devices/KeyboardDevice.o \
       CMOS.o \
       PIC.o \
       Syscall.o \
       Devices/IDEDiskDevice.o \
       VM/MemoryManager.o \
       VM/Region.o \
       VM/VMObject.o \
       VM/PageDirectory.o \
       VM/PhysicalPage.o \
       Console.o \
       IRQHandler.o \
       kprintf.o \
       RTC.o \
       TTY/TTY.o \
       TTY/PTYMultiplexer.o \
       TTY/MasterPTY.o \
       TTY/SlavePTY.o \
       TTY/VirtualConsole.o \
       FileSystem/FIFO.o \
       Scheduler.o \
       DoubleBuffer.o \
       ELF/ELFImage.o \
       ELF/ELFLoader.o \
       KSyms.o \
       FileSystem/DevPtsFS.o \
       Devices/BXVGADevice.o \
       PCI.o \
       Devices/PS2MouseDevice.o \
       Net/Socket.o \
       Net/LocalSocket.o \
       Net/IPv4Socket.o \
       Net/TCPSocket.o \
       Net/UDPSocket.o \
       Net/NetworkAdapter.o \
       Net/E1000NetworkAdapter.o \
       Net/LoopbackAdapter.o \
       Net/Routing.o \
       Net/NetworkTask.o

VFS_OBJS = \
    FileSystem/ProcFS.o \
    Devices/DiskDevice.o \
    Devices/Device.o \
    Devices/CharacterDevice.o \
    Devices/BlockDevice.o \
    Devices/NullDevice.o \
    Devices/FullDevice.o \
    Devices/ZeroDevice.o \
    Devices/RandomDevice.o \
    FileSystem/FileSystem.o \
    FileSystem/DiskBackedFileSystem.o \
    FileSystem/Ext2FileSystem.o \
    FileSystem/VirtualFileSystem.o \
    FileSystem/FileDescriptor.o \
    FileSystem/SyntheticFileSystem.o

AK_OBJS = \
    ../AK/String.o \
    ../AK/StringImpl.o \
    ../AK/StringBuilder.o \
    ../AK/FileSystemPath.o \
    ../AK/StdLibExtras.o

CXX_OBJS = $(KERNEL_OBJS) $(VFS_OBJS) $(AK_OBJS)
OBJS = $(CXX_OBJS) Boot/boot.ao

KERNEL = kernel
ARCH_FLAGS =
STANDARD_FLAGS = -std=c++17 -nostdinc++ -nostdlib -nostdinc
KERNEL_FLAGS = -ffreestanding
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough
FLAVOR_FLAGS = -mregparm=3 -fno-exceptions -fno-rtti
OPTIMIZATION_FLAGS = -Os
INCLUDE_FLAGS = -I.. -I.
CLANG_FLAGS = -Wconsumed -m32 -ffreestanding -march=i686
#SUGGEST_FLAGS = -Wsuggest-final-types -Wsuggest-final-methods -Wsuggest-override #-Wsuggest-attribute=noreturn 

DEFINES = -DSERENITY -DKERNEL -DSANITIZE_PTRS

CXXFLAGS = -MMD -MP $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(KERNEL_FLAGS) $(FLAVOR_FLAGS) $(ARCH_FLAGS) $(STANDARD_FLAGS) $(SUGGEST_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
#CXX = clang $(CLANG_FLAGS)
CXX = i686-pc-serenity-g++
LD = i686-pc-serenity-ld
AS = i686-pc-serenity-as
LDFLAGS = -T linker.ld

all: $(KERNEL) kernel.map

kernel.map: kernel
	@echo "MKMAP $@"; sh mkmap.sh

$(KERNEL): $(OBJS)
	@echo "LD $@"; $(LD) $(LDFLAGS) -o $@ -Ttext 0x10000 $(OBJS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

%.ao: %.S
	@echo "AS $@"; $(AS) -o $@  $<

-include $(CXX_OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(KERNEL) $(OBJS) *.d

