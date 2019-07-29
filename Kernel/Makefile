include ../Makefile.common

KERNEL_OBJS = \
       init.o \
       kmalloc.o \
       StdLib.o \
       Lock.o \
       Arch/i386/CPU.o \
       Process.o \
       SharedBuffer.o \
       Thread.o \
       Arch/i386/PIT.o \
       Devices/KeyboardDevice.o \
       CMOS.o \
       Arch/i386/PIC.o \
       Syscall.o \
       Devices/PATAChannel.o \
       Devices/PATADiskDevice.o \
       Devices/FloppyDiskDevice.o \
       VM/MemoryManager.o \
       VM/Region.o \
       VM/VMObject.o \
       VM/PageDirectory.o \
       VM/PhysicalPage.o \
       VM/PhysicalRegion.o \
       VM/RangeAllocator.o \
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
       KSyms.o \
       KParams.o \
       FileSystem/SharedMemory.o \
       FileSystem/DevPtsFS.o \
       Devices/BXVGADevice.o \
       PCI.o \
       Devices/PS2MouseDevice.o \
       Devices/SerialDevice.o \
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
       ProcessTracer.o \
       Devices/PCSpeaker.o \
       FileSystem/InodeFile.o \
       FileSystem/Custody.o \
       FileSystem/File.o

VFS_OBJS = \
    FileSystem/ProcFS.o \
    FileSystem/Inode.o \
    Devices/DiskDevice.o \
    Devices/Device.o \
    Devices/CharacterDevice.o \
    Devices/BlockDevice.o \
    Devices/NullDevice.o \
    Devices/FullDevice.o \
    Devices/ZeroDevice.o \
    Devices/RandomDevice.o \
    Devices/DebugLogDevice.o \
    Devices/DiskPartition.o \
    Devices/MBRPartitionTable.o \
    FileSystem/InodeWatcher.o \
    FileSystem/FileSystem.o \
    FileSystem/DiskBackedFileSystem.o \
    FileSystem/Ext2FileSystem.o \
    FileSystem/VirtualFileSystem.o \
    FileSystem/FileDescription.o \
    FileSystem/SyntheticFileSystem.o \
    Devices/SB16.o

AK_OBJS = \
    ../AK/String.o \
    ../AK/StringImpl.o \
    ../AK/StringBuilder.o \
    ../AK/StringView.o \
    ../AK/FileSystemPath.o \
    ../AK/JsonObject.o \
    ../AK/JsonValue.o \
    ../AK/JsonArray.o \
    ../AK/JsonParser.o \
    ../AK/LogStream.o \
    ../AK/ELF/ELFImage.o \
    ../AK/ELF/ELFLoader.o

CXX_OBJS = $(KERNEL_OBJS) $(VFS_OBJS) $(AK_OBJS)
OBJS = $(CXX_OBJS) Boot/boot.ao

KERNEL = kernel
CXXFLAGS += -ffreestanding -mregparm=3 -mno-80387 -mno-mmx -mno-sse -mno-sse2
CXXFLAGS += -nostdlib -nostdinc -nostdinc++
CXXFLAGS += -I../Toolchain/Local/i686-pc-serenity/include/c++/8.3.0/
CXXFLAGS += -I../Toolchain/Local/i686-pc-serenity/include/c++/8.3.0/i686-pc-serenity/
DEFINES += -DKERNEL
DEFINES += -DEXPENSIVE_USERSPACE_STACKS
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

