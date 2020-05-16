set(KERNEL_SOURCES
    ACPI/DMIDecoder.cpp
    ACPI/DynamicParser.cpp
    ACPI/Initialize.cpp
    ACPI/MultiProcessorParser.cpp
    ACPI/Parser.cpp
    Arch/i386/CPU.cpp
    CMOS.cpp
    CommandLine.cpp
    Console.cpp
    Devices/BXVGADevice.cpp
    Devices/BlockDevice.cpp
    Devices/CharacterDevice.cpp
    Devices/Device.cpp
    Devices/DiskPartition.cpp
    Devices/EBRPartitionTable.cpp
    Devices/FullDevice.cpp
    Devices/GPTPartitionTable.cpp
    Devices/KeyboardDevice.cpp
    Devices/MBRPartitionTable.cpp
    Devices/MBVGADevice.cpp
    Devices/NullDevice.cpp
    Devices/PATAChannel.cpp
    Devices/PATADiskDevice.cpp
    Devices/PCSpeaker.cpp
    Devices/PS2MouseDevice.cpp
    Devices/RandomDevice.cpp
    Devices/SB16.cpp
    Devices/SerialDevice.cpp
    Devices/VMWareBackdoor.cpp
    Devices/ZeroDevice.cpp
    DoubleBuffer.cpp
    FileSystem/Custody.cpp
    FileSystem/DevPtsFS.cpp
    FileSystem/Ext2FileSystem.cpp
    FileSystem/FIFO.cpp
    FileSystem/File.cpp
    FileSystem/FileBackedFileSystem.cpp
    FileSystem/FileDescription.cpp
    FileSystem/FileSystem.cpp
    FileSystem/Inode.cpp
    FileSystem/InodeFile.cpp
    FileSystem/InodeWatcher.cpp
    FileSystem/ProcFS.cpp
    FileSystem/TmpFS.cpp
    FileSystem/VirtualFileSystem.cpp
    Heap/SlabAllocator.cpp
    Heap/kmalloc.cpp
    Interrupts/APIC.cpp
    Interrupts/GenericInterruptHandler.cpp
    Interrupts/IOAPIC.cpp
    Interrupts/IRQHandler.cpp
    Interrupts/InterruptManagement.cpp
    Interrupts/PIC.cpp
    Interrupts/SharedIRQHandler.cpp
    Interrupts/SpuriousInterruptHandler.cpp
    Interrupts/UnhandledInterruptHandler.cpp
    KBufferBuilder.cpp
    KSyms.cpp
    Lock.cpp
    Net/E1000NetworkAdapter.cpp
    Net/IPv4Socket.cpp
    Net/LocalSocket.cpp
    Net/LoopbackAdapter.cpp
    Net/NetworkAdapter.cpp
    Net/NetworkTask.cpp
    Net/RTL8139NetworkAdapter.cpp
    Net/Routing.cpp
    Net/Socket.cpp
    Net/TCPSocket.cpp
    Net/UDPSocket.cpp
    PCI/Access.cpp
    PCI/Device.cpp
    PCI/IOAccess.cpp
    PCI/Initializer.cpp
    PCI/MMIOAccess.cpp
    PerformanceEventBuffer.cpp
    Process.cpp
    Profiling.cpp
    Ptrace.cpp
    RTC.cpp
    Random.cpp
    Scheduler.cpp
    SharedBuffer.cpp
    StdLib.cpp
    Syscall.cpp
    TTY/MasterPTY.cpp
    TTY/PTYMultiplexer.cpp
    TTY/SlavePTY.cpp
    TTY/TTY.cpp
    TTY/VirtualConsole.cpp
    Tasks/FinalizerTask.cpp
    Tasks/SyncTask.cpp
    Thread.cpp
    ThreadTracer.cpp
    Time/HPET.cpp
    Time/HPETComparator.cpp
    Time/HardwareTimer.cpp
    Time/PIT.cpp
    Time/RTC.cpp
    Time/TimeManagement.cpp
    TimerQueue.cpp
    VM/AnonymousVMObject.cpp
    VM/ContiguousVMObject.cpp
    VM/InodeVMObject.cpp
    VM/MemoryManager.cpp
    VM/PageDirectory.cpp
    VM/PhysicalPage.cpp
    VM/PhysicalRegion.cpp
    VM/PrivateInodeVMObject.cpp
    VM/ProcessPagingScope.cpp
    VM/PurgeableVMObject.cpp
    VM/RangeAllocator.cpp
    VM/Region.cpp
    VM/SharedInodeVMObject.cpp
    VM/VMObject.cpp
    WaitQueue.cpp
    init.cpp
    kprintf.cpp
)

set(AK_SOURCES
    ../AK/FileSystemPath.cpp
    ../AK/FlyString.cpp
    ../AK/JsonParser.cpp
    ../AK/JsonValue.cpp
    ../AK/LogStream.cpp
    ../AK/String.cpp
    ../AK/StringBuilder.cpp
    ../AK/StringImpl.cpp
    ../AK/StringUtils.cpp
    ../AK/StringView.cpp
)

set(ELF_SOURCES
    ../Libraries/LibELF/Image.cpp
    ../Libraries/LibELF/Loader.cpp
    ../Libraries/LibELF/Validation.cpp
)

set(SOURCES
    ${KERNEL_SOURCES}
    ${AK_SOURCES}
    ${ELF_SOURCES}
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DKERNEL")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pie -fPIE -ffreestanding -fbuiltin")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mno-80387 -mno-mmx -mno-sse -mno-sse2")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-asynchronous-unwind-tables")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -nostdinc -nostdinc++")

add_link_options(LINKER:-T ${CMAKE_CURRENT_BINARY_DIR}/linker.ld -nostdlib)

add_library(boot OBJECT Arch/i386/Boot/boot.S)
file(GENERATE OUTPUT linker.ld INPUT linker.ld)

if (${CMAKE_HOST_SYSTEM_NAME} MATCHES SerenityOS)
    include_directories(/usr/local/include/c++/10.1.0/)
    include_directories(/usr/local/include/c++/10.1.0/i686-pc-serenity/)
else()
    include_directories(../Toolchain/Local/i686-pc-serenity/include/c++/10.1.0/)
    include_directories(../Toolchain/Local/i686-pc-serenity/include/c++/10.1.0/i686-pc-serenity/)
endif()

add_executable(Kernel ${SOURCES})
target_link_libraries(Kernel gcc)
add_dependencies(Kernel boot)
install(TARGETS Kernel RUNTIME DESTINATION boot)

add_custom_command(
    TARGET Kernel
    COMMAND sh ${CMAKE_CURRENT_SOURCE_DIR}/mkmap.sh
)
install(FILES ${CMAKE_CURRENT_BINARY_DIR}/kernel.map DESTINATION res)

add_subdirectory(Modules)
