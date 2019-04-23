include ../Makefile.common

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
       SharedMemory.o \
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
       Net/NetworkTask.o \
       ProcessTracer.o

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
    Devices/DebugLogDevice.o \
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
    ../AK/StringView.o \
    ../AK/FileSystemPath.o \
    ../AK/StdLibExtras.o

CXX_OBJS = $(KERNEL_OBJS) $(VFS_OBJS) $(AK_OBJS)
OBJS = $(CXX_OBJS) Boot/boot.ao

KERNEL = kernel
CXXFLAGS += -ffreestanding -mregparm=3 -mno-80387 -mno-mmx -mno-sse -mno-sse2
DEFINES += -DKERNEL
LDFLAGS += -Ttext 0x10000 -Wl,-T linker.ld -nostdlib

all: $(KERNEL) kernel.map

kernel.map: kernel
	@echo "MKMAP $@"; sh mkmap.sh

$(KERNEL): $(OBJS)
	@echo "LD $@"; $(LD) $(LDFLAGS) -o $@ $(OBJS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

%.ao: %.S
	@echo "AS $@"; $(AS) -o $@  $<

-include $(CXX_OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(KERNEL) $(OBJS) *.d

