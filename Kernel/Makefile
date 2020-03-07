OBJS = \
    ../AK/FileSystemPath.o \
    ../AK/JsonParser.o \
    ../AK/JsonValue.o \
    ../AK/LogStream.o \
    ../AK/String.o \
    ../AK/StringBuilder.o \
    ../AK/StringImpl.o \
    ../AK/StringView.o \
    ../AK/StringUtils.o \
    ../Libraries/LibELF/ELFImage.o \
    ../Libraries/LibELF/ELFLoader.o \
    ../Libraries/LibBareMetal/Output/Console.o \
    ../Libraries/LibBareMetal/Output/kprintf.o \
    ../Libraries/LibBareMetal/StdLib.o \
    Arch/i386/CPU.o \
    Interrupts/InterruptManagement.o \
    Interrupts/APIC.o \
    Interrupts/IOAPIC.o \
    Interrupts/PIC.o \
    Interrupts/GenericInterruptHandler.o \
    Interrupts/UnhandledInterruptHandler.o \
    Interrupts/SpuriousInterruptHandler.o \
    Interrupts/IRQHandler.o \
    Interrupts/SharedIRQHandler.o \
    CMOS.o \
    Devices/PIT.o \
    Devices/BXVGADevice.o \
    Devices/BlockDevice.o \
    Devices/CharacterDevice.o \
    Devices/DebugLogDevice.o \
    Devices/Device.o \
    Devices/DiskPartition.o \
    Devices/FloppyDiskDevice.o \
    Devices/FullDevice.o \
    Devices/GPTPartitionTable.o \
    Devices/EBRPartitionTable.o \
    Devices/KeyboardDevice.o \
    Devices/MBRPartitionTable.o \
    Devices/MBVGADevice.o \
    Devices/NullDevice.o \
    Devices/PATAChannel.o \
    Devices/PATADiskDevice.o \
    Devices/PCSpeaker.o \
    Devices/PS2MouseDevice.o \
    Devices/RandomDevice.o \
    Devices/SB16.o \
    Devices/SerialDevice.o \
    Devices/ZeroDevice.o \
    Devices/VMWareBackdoor.o \
    Devices/HardwareTimer.o \
    DoubleBuffer.o \
    FileSystem/Custody.o \
    FileSystem/DevPtsFS.o \
    FileSystem/DiskBackedFileSystem.o \
    FileSystem/Ext2FileSystem.o \
    FileSystem/FIFO.o \
    FileSystem/File.o \
    FileSystem/FileDescription.o \
    FileSystem/FileSystem.o \
    FileSystem/Inode.o \
    FileSystem/InodeFile.o \
    FileSystem/InodeWatcher.o \
    FileSystem/ProcFS.o \
    FileSystem/TmpFS.o \
    FileSystem/VirtualFileSystem.o \
    Heap/SlabAllocator.o \
    Heap/kmalloc.o \
    KBufferBuilder.o \
    KParams.o \
    KSyms.o \
    Lock.o \
    Net/E1000NetworkAdapter.o \
    Net/IPv4Socket.o \
    Net/LocalSocket.o \
    Net/LoopbackAdapter.o \
    Net/NetworkAdapter.o \
    Net/NetworkTask.o \
    Net/RTL8139NetworkAdapter.o \
    Net/Routing.o \
    Net/Socket.o \
    Net/TCPSocket.o \
    Net/UDPSocket.o \
    PCI/Access.o \
    PCI/IOAccess.o \
    PCI/MMIOAccess.o \
    PCI/Initializer.o \
    PCI/Device.o \
    PerformanceEventBuffer.o \
    Process.o \
    ProcessTracer.o \
    Profiling.o \
    RTC.o \
    Random.o \
    Scheduler.o \
    SharedBuffer.o \
    Syscall.o \
    TimerQueue.o \
    TTY/MasterPTY.o \
    TTY/PTYMultiplexer.o \
    TTY/SlavePTY.o \
    TTY/TTY.o \
    TTY/VirtualConsole.o \
    Thread.o \
    VM/AnonymousVMObject.o \
    VM/ContiguousVMObject.o \
    VM/InodeVMObject.o \
    VM/MemoryManager.o \
    VM/PageDirectory.o \
    VM/PhysicalPage.o \
    VM/PhysicalRegion.o \
    VM/PurgeableVMObject.o \
    VM/PrivateInodeVMObject.o \
    VM/ProcessPagingScope.o \
    VM/RangeAllocator.o \
    VM/Region.o \
    VM/SharedInodeVMObject.o \
    VM/VMObject.o \
    ACPI/ACPIParser.o \
    ACPI/ACPIStaticParser.o \
    ACPI/ACPIDynamicParser.o \
    ACPI/DMIDecoder.o \
    ACPI/MultiProcessorParser.o \
    WaitQueue.o \
    init.o

OBJ_SUFFIX = .kernel

MODULE_OBJS = TestModule$(OBJ_SUFFIX).o

EXTRA_OBJS = Arch/i386/Boot/boot.ao

KERNEL = 1

PROGRAM = kernel

SUBPROJECT_CXXFLAGS += -pie -fPIE -ffreestanding -fbuiltin -mno-80387 -mno-mmx -mno-sse -mno-sse2 -fno-asynchronous-unwind-tables
SUBPROJECT_CXXFLAGS += -nostdlib -nostdinc -nostdinc++
SUBPROJECT_CXXFLAGS += -I../Toolchain/Local/i686-pc-serenity/include/c++/9.2.0/
SUBPROJECT_CXXFLAGS += -I../Toolchain/Local/i686-pc-serenity/include/c++/9.2.0/i686-pc-serenity/

LDFLAGS += -Wl,-T linker.ld -nostdlib -lgcc -lstdc++

all: $(PROGRAM) $(MODULE_OBJS) kernel.map

kernel.map: kernel
	@echo "MKMAP $@"
	$(QUIET) sh mkmap.sh

EXTRA_CLEAN += kernel.map

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/Kernel/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/Kernel/

include ../Makefile.common
