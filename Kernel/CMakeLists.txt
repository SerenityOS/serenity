if (ENABLE_EXTRA_KERNEL_DEBUG_SYMBOLS)
    add_compile_options(-Og)
    add_compile_options(-ggdb3)
else()
    add_compile_options(-Os)
endif()

if ("${SERENITY_ARCH}" STREQUAL "i686")
    set(KERNEL_ARCH i386)
elseif("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(KERNEL_ARCH x86_64)
endif()

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
    AddressSanitizer.cpp
    Arch/PC/BIOS.cpp
    Arch/x86/SmapDisabler.h
    CMOS.cpp
    CommandLine.cpp
    ConsoleDevice.cpp
    CoreDump.cpp
    DMI.cpp
    Devices/AsyncDeviceRequest.cpp
    Devices/BlockDevice.cpp
    Devices/CharacterDevice.cpp
    Devices/Device.cpp
    Devices/FullDevice.cpp
    Devices/MemoryDevice.cpp
    Devices/NullDevice.cpp
    Devices/PCISerialDevice.cpp
    Devices/PCSpeaker.cpp
    Devices/RandomDevice.cpp
    Devices/SB16.cpp
    Devices/SerialDevice.cpp
    Devices/USB/UHCIController.cpp
    VirtIO/VirtIO.cpp
    VirtIO/VirtIOQueue.cpp
    VirtIO/VirtIOConsole.cpp
    Devices/VMWareBackdoor.cpp
    Devices/ZeroDevice.cpp
    Devices/HID/I8042Controller.cpp
    Devices/HID/HIDManagement.cpp
    Devices/HID/KeyboardDevice.cpp
    Devices/HID/MouseDevice.cpp
    Devices/HID/PS2KeyboardDevice.cpp
    Devices/HID/PS2MouseDevice.cpp
    Devices/HID/VMWareMouseDevice.cpp
    Graphics/Console/FramebufferConsole.cpp
    Graphics/Console/TextModeConsole.cpp
    Graphics/Console/VGAConsole.cpp
    Graphics/BochsGraphicsAdapter.cpp
    Graphics/FramebufferDevice.cpp
    Graphics/GraphicsManagement.cpp
    Graphics/IntelNativeGraphicsAdapter.cpp
    Graphics/VGACompatibleAdapter.cpp
    Storage/Partition/DiskPartition.cpp
    Storage/Partition/DiskPartitionMetadata.cpp
    Storage/Partition/EBRPartitionTable.cpp
    Storage/Partition/GUIDPartitionTable.cpp
    Storage/Partition/MBRPartitionTable.cpp
    Storage/Partition/PartitionTable.cpp
    Storage/StorageDevice.cpp
    Storage/AHCIController.cpp
    Storage/AHCIPort.cpp
    Storage/AHCIPortHandler.cpp
    Storage/SATADiskDevice.cpp
    Storage/BMIDEChannel.cpp
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
    KString.cpp
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
    PCI/MMIOAccess.cpp
    PCI/Initializer.cpp
    PCI/WindowedMMIOAccess.cpp
    Panic.cpp
    PerformanceEventBuffer.cpp
    Process.cpp
    ProcessGroup.cpp
    RTC.cpp
    Random.cpp
    Scheduler.cpp
    StdLib.cpp
    Syscall.cpp
    Syscalls/anon_create.cpp
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
    Syscalls/emuctl.cpp
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
    Syscalls/statvfs.cpp
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
    Syscalls/inode_watcher.cpp
    Syscalls/write.cpp
    TTY/ConsoleManagement.cpp
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
    VirtIO/VirtIO.cpp
    VirtIO/VirtIOConsole.cpp
    VirtIO/VirtIOQueue.cpp
    VirtIO/VirtIORNG.cpp
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
    VM/Range.cpp
    VM/RangeAllocator.cpp
    VM/Region.cpp
    VM/RingBuffer.cpp
    VM/ScatterGatherList.cpp
    VM/SharedInodeVMObject.cpp
    VM/Space.cpp
    VM/VMObject.cpp
    WaitQueue.cpp
    WorkQueue.cpp
    init.cpp
    kprintf.cpp
)

set(KERNEL_SOURCES
    ${KERNEL_SOURCES}
    ${CMAKE_CURRENT_SOURCE_DIR}/Arch/${KERNEL_ARCH}/CPU.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/Arch/${KERNEL_ARCH}/InterruptEntry.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/Arch/${KERNEL_ARCH}/ProcessorInfo.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/Arch/${KERNEL_ARCH}/SafeMem.cpp
)

set(AK_SOURCES
    ../AK/FlyString.cpp
    ../AK/GenericLexer.cpp
    ../AK/Hex.cpp
    ../AK/JsonParser.cpp
    ../AK/JsonValue.cpp
    ../AK/LexicalPath.cpp
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

generate_state_machine(../Userland/Libraries/LibVT/StateMachine.txt ../Userland/Libraries/LibVT/EscapeSequenceStateMachine.h)

set(VT_SOURCES
    ../Userland/Libraries/LibVT/Terminal.cpp
    ../Userland/Libraries/LibVT/Line.cpp
    ../Userland/Libraries/LibVT/EscapeSequenceParser.cpp
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

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-unknown-warning-option -Wvla -Wnull-dereference")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pie -fPIE -fno-rtti -ffreestanding -fbuiltin")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mno-80387 -mno-mmx -mno-sse -mno-sse2")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-asynchronous-unwind-tables")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fstack-protector-strong")
if (NOT ${CMAKE_HOST_SYSTEM_NAME} MATCHES SerenityOS)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -nostdinc -nostdinc++")
endif()

if ("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mcmodel=large -mno-red-zone")
endif()

# Kernel Undefined Behavior Sanitizer (KUBSAN)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fsanitize=undefined")

# Kernel Address Sanitize (KASAN) implementation is still a work in progress, this option
# is not currently meant to be used, besides when developing Kernel ASAN support.
#
if (ENABLE_KERNEL_ADDRESS_SANITIZER)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fsanitize=kernel-address")
endif()

add_compile_definitions(KERNEL)

# HACK: This is a workaround for CLion to grok the kernel sources.
#       It's needed because CLion doesn't understand the way we switch compilers mid-build.
add_compile_definitions(__serenity__)

add_link_options(LINKER:-T ${CMAKE_CURRENT_BINARY_DIR}/linker.ld -nostdlib)

# HACK: This is to work around a bug in CMake dependency resolution, the
#       kernel won't re-link when boot.S changes without this.
set_source_files_properties(init.cpp
    PROPERTIES
    OBJECT_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/Arch/${KERNEL_ARCH}/Boot/boot.S
)

add_library(boot OBJECT Arch/${KERNEL_ARCH}/Boot/boot.S)

add_library(kernel_heap STATIC ${KERNEL_HEAP_SOURCES})
file(GENERATE OUTPUT linker.ld INPUT linker.ld)

if (${CMAKE_HOST_SYSTEM_NAME} MATCHES SerenityOS)
    include_directories(/usr/local/include/c++/${GCC_VERSION}/)
else()
    if (NOT EXISTS ${TOOLCHAIN_ROOT}/Kernel/${SERENITY_ARCH}-pc-serenity/include/c++/${GCC_VERSION}/)
        message(FATAL_ERROR "Toolchain version ${GCC_VERSION} appears to be missing! Please run: Meta/serenity.sh rebuild-toolchain")
    endif()
    include_directories(${TOOLCHAIN_ROOT}/Kernel/${SERENITY_ARCH}-pc-serenity/include/c++/${GCC_VERSION}/)
    include_directories(${TOOLCHAIN_ROOT}/Kernel/${SERENITY_ARCH}-pc-serenity/include/c++/${GCC_VERSION}/${SERENITY_ARCH}-pc-serenity/)
    link_directories(${TOOLCHAIN_ROOT}/Kernel/${SERENITY_ARCH}-pc-serenity/lib)
    link_directories(${TOOLCHAIN_ROOT}/Kernel/lib/gcc/${SERENITY_ARCH}-pc-serenity/${GCC_VERSION}/)
endif()

add_executable(Kernel ${SOURCES})
add_dependencies(Kernel generate_EscapeSequenceStateMachine.h)

if (ENABLE_KERNEL_LTO)
    include(CheckIPOSupported)
    check_ipo_supported()
    set_property(TARGET Kernel PROPERTY INTERPROCEDURAL_OPTIMIZATION TRUE)
endif()
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
