set(KERNEL_HEAP_SOURCES
    Heap/SlabAllocator.cpp
    Heap/kmalloc.cpp
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS_STATIC}")

set(KERNEL_SOURCES
    ACPI/DynamicParser.cpp
    ACPI/Initialize.cpp
    ACPI/MultiProcessorParser.cpp
    ACPI/Parser.cpp
    Arch/i386/CPU.cpp
    Arch/i386/ProcessorInfo.cpp
    Arch/i386/SafeMem.cpp
    Arch/PC/BIOS.cpp
    CMOS.cpp
    CommandLine.cpp
    Console.cpp
    CoreDump.cpp
    DMI.cpp
    Devices/AsyncDeviceRequest.cpp
    Devices/BXVGADevice.cpp
    Devices/BlockDevice.cpp
    Devices/CharacterDevice.cpp
    Devices/Device.cpp
    Devices/FullDevice.cpp
    Devices/I8042Controller.cpp
    Devices/KeyboardDevice.cpp
    Devices/MBVGADevice.cpp
    Devices/MemoryDevice.cpp
    Devices/NullDevice.cpp
    Devices/PCSpeaker.cpp
    Devices/PS2MouseDevice.cpp
    Devices/RandomDevice.cpp
    Devices/SB16.cpp
    Devices/SerialDevice.cpp
    Devices/USB/UHCIController.cpp
    Devices/VMWareBackdoor.cpp
    Devices/ZeroDevice.cpp
    Storage/Partition/DiskPartition.cpp
    Storage/Partition/DiskPartitionMetadata.cpp
    Storage/Partition/EBRPartitionTable.cpp
    Storage/Partition/GUIDPartitionTable.cpp
    Storage/Partition/MBRPartitionTable.cpp
    Storage/Partition/PartitionTable.cpp
    Storage/StorageDevice.cpp
    Storage/IDEController.cpp
    Storage/IDEChannel.cpp
    Storage/PATADiskDevice.cpp
    Storage/RamdiskController.cpp
    Storage/RamdiskDevice.cpp
    Storage/StorageManagement.cpp
    DoubleBuffer.cpp
    FileSystem/AnonymousFile.cpp
    FileSystem/BlockBasedFileSystem.cpp
    FileSystem/Custody.cpp
    FileSystem/DevFS.cpp
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
    FileSystem/Plan9FileSystem.cpp
    FileSystem/ProcFS.cpp
    FileSystem/TmpFS.cpp
    FileSystem/VirtualFileSystem.cpp
    FutexQueue.cpp
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
    Net/NE2000NetworkAdapter.cpp
    Net/NetworkAdapter.cpp
    Net/NetworkTask.cpp
    Net/RTL8139NetworkAdapter.cpp
    Net/Routing.cpp
    Net/Socket.cpp
    Net/TCPSocket.cpp
    Net/UDPSocket.cpp
    PCI/Access.cpp
    PCI/Device.cpp
    PCI/DeviceController.cpp
    PCI/IOAccess.cpp
    PCI/Initializer.cpp
    PCI/MMIOAccess.cpp
    PerformanceEventBuffer.cpp
    Process.cpp
    ProcessGroup.cpp
    RTC.cpp
    Random.cpp
    Scheduler.cpp
    StdLib.cpp
    Syscall.cpp
    Syscalls/anon_create.cpp
    Syscalls/abort.cpp
    Syscalls/access.cpp
    Syscalls/alarm.cpp
    Syscalls/beep.cpp
    Syscalls/chdir.cpp
    Syscalls/chmod.cpp
    Syscalls/chown.cpp
    Syscalls/chroot.cpp
    Syscalls/clock.cpp
    Syscalls/debug.cpp
    Syscalls/disown.cpp
    Syscalls/dup2.cpp
    Syscalls/execve.cpp
    Syscalls/exit.cpp
    Syscalls/fcntl.cpp
    Syscalls/fork.cpp
    Syscalls/ftruncate.cpp
    Syscalls/futex.cpp
    Syscalls/get_dir_entries.cpp
    Syscalls/get_stack_bounds.cpp
    Syscalls/getrandom.cpp
    Syscalls/getuid.cpp
    Syscalls/hostname.cpp
    Syscalls/ioctl.cpp
    Syscalls/keymap.cpp
    Syscalls/kill.cpp
    Syscalls/link.cpp
    Syscalls/lseek.cpp
    Syscalls/mkdir.cpp
    Syscalls/mknod.cpp
    Syscalls/mmap.cpp
    Syscalls/module.cpp
    Syscalls/mount.cpp
    Syscalls/open.cpp
    Syscalls/perf_event.cpp
    Syscalls/pipe.cpp
    Syscalls/pledge.cpp
    Syscalls/prctl.cpp
    Syscalls/process.cpp
    Syscalls/profiling.cpp
    Syscalls/ptrace.cpp
    Syscalls/purge.cpp
    Syscalls/read.cpp
    Syscalls/readlink.cpp
    Syscalls/realpath.cpp
    Syscalls/rename.cpp
    Syscalls/rmdir.cpp
    Syscalls/sched.cpp
    Syscalls/select.cpp
    Syscalls/sendfd.cpp
    Syscalls/setpgid.cpp
    Syscalls/setuid.cpp
    Syscalls/shutdown.cpp
    Syscalls/sigaction.cpp
    Syscalls/socket.cpp
    Syscalls/stat.cpp
    Syscalls/sync.cpp
    Syscalls/sysconf.cpp
    Syscalls/thread.cpp
    Syscalls/times.cpp
    Syscalls/ttyname.cpp
    Syscalls/umask.cpp
    Syscalls/uname.cpp
    Syscalls/unlink.cpp
    Syscalls/unveil.cpp
    Syscalls/utime.cpp
    Syscalls/waitid.cpp
    Syscalls/watch_file.cpp
    Syscalls/write.cpp
    TTY/MasterPTY.cpp
    TTY/PTYMultiplexer.cpp
    TTY/SlavePTY.cpp
    TTY/TTY.cpp
    TTY/VirtualConsole.cpp
    Tasks/FinalizerTask.cpp
    Tasks/SyncTask.cpp
    Thread.cpp
    ThreadBlockers.cpp
    ThreadTracer.cpp
    Time/APICTimer.cpp
    Time/HPET.cpp
    Time/HPETComparator.cpp
    Time/PIT.cpp
    Time/RTC.cpp
    Time/TimeManagement.cpp
    TimerQueue.cpp
    UBSanitizer.cpp
    UserOrKernelBuffer.cpp
    VM/AnonymousVMObject.cpp
    VM/ContiguousVMObject.cpp
    VM/InodeVMObject.cpp
    VM/MemoryManager.cpp
    VM/PageDirectory.cpp
    VM/PhysicalPage.cpp
    VM/PhysicalRegion.cpp
    VM/PrivateInodeVMObject.cpp
    VM/ProcessPagingScope.cpp
    VM/PurgeablePageRanges.cpp
    VM/RangeAllocator.cpp
    VM/Region.cpp
    VM/SharedInodeVMObject.cpp
    VM/Space.cpp
    VM/VMObject.cpp
    WaitQueue.cpp
    init.cpp
    kprintf.cpp
)

set(AK_SOURCES
    ../AK/ByteBuffer.cpp
    ../AK/FlyString.cpp
    ../AK/GenericLexer.cpp
    ../AK/Hex.cpp
    ../AK/JsonParser.cpp
    ../AK/JsonValue.cpp
    ../AK/LexicalPath.cpp
    ../AK/LogStream.cpp
    ../AK/String.cpp
    ../AK/StringBuilder.cpp
    ../AK/StringImpl.cpp
    ../AK/StringUtils.cpp
    ../AK/StringView.cpp
    ../AK/Time.cpp
    ../AK/Format.cpp
    ../AK/UUID.cpp
)

set(ELF_SOURCES
    ../Userland/Libraries/LibELF/Image.cpp
    ../Userland/Libraries/LibELF/Validation.cpp
)

set(VT_SOURCES
    ../Userland/Libraries/LibVT/Terminal.cpp
    ../Userland/Libraries/LibVT/Line.cpp
)

set(KEYBOARD_SOURCES
    ../Userland/Libraries/LibKeyboard/CharacterMap.cpp
)

set(CRYPTO_SOURCES
    ../Userland/Libraries/LibCrypto/Cipher/AES.cpp
    ../Userland/Libraries/LibCrypto/Hash/SHA2.cpp
)

set(C_SOURCES
    ../Userland/Libraries/LibC/ctype.cpp
)

set(SOURCES
    ${KERNEL_SOURCES}
    ${AK_SOURCES}
    ${ELF_SOURCES}
    ${VT_SOURCES}
    ${KEYBOARD_SOURCES}
    ${CRYPTO_SOURCES}
    ${C_SOURCES}
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fsanitize=nonnull-attribute,returns-nonnull-attribute,bool,vla-bound,signed-integer-overflow,shift,shift-exponent,shift-base,integer-divide-by-zero,return,bounds,bounds-strict,object-size,enum,vptr")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-unknown-warning-option -DKERNEL")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pie -fPIE -fno-rtti -ffreestanding -fbuiltin")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mno-80387 -mno-mmx -mno-sse -mno-sse2")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-asynchronous-unwind-tables")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fstack-protector-strong")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -nostdinc -nostdinc++")

add_link_options(LINKER:-T ${CMAKE_CURRENT_BINARY_DIR}/linker.ld -nostdlib)

add_library(boot OBJECT Arch/i386/Boot/boot.S)
add_library(kernel_heap STATIC ${KERNEL_HEAP_SOURCES})
file(GENERATE OUTPUT linker.ld INPUT linker.ld)

if (${CMAKE_HOST_SYSTEM_NAME} MATCHES SerenityOS)
    include_directories(/usr/local/include/c++/10.2.0/)
    include_directories(/usr/local/include/c++/10.2.0/${SERENITY_ARCH}-pc-serenity/)
else()
    include_directories(../Toolchain/Local/${SERENITY_ARCH}/${SERENITY_ARCH}-pc-serenity/include/c++/10.2.0/)
    include_directories(../Toolchain/Local/${SERENITY_ARCH}/${SERENITY_ARCH}-pc-serenity/include/c++/10.2.0/${SERENITY_ARCH}-pc-serenity/)
endif()

add_executable(Kernel ${SOURCES})
target_link_libraries(Kernel kernel_heap gcc stdc++)
add_dependencies(Kernel boot kernel_heap)
install(TARGETS Kernel RUNTIME DESTINATION boot)

add_custom_command(
    TARGET Kernel
    COMMAND sh ${CMAKE_CURRENT_SOURCE_DIR}/mkmap.sh
)
install(FILES ${CMAKE_CURRENT_BINARY_DIR}/kernel.map DESTINATION res)

serenity_install_headers(Kernel)
serenity_install_sources(Kernel)

add_subdirectory(Modules)
