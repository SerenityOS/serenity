if (ENABLE_EXTRA_KERNEL_DEBUG_SYMBOLS)
    add_compile_options(-Og)
    add_compile_options(-ggdb3)
else()
    add_compile_options(-O2)
endif()

if ("${SERENITY_ARCH}" STREQUAL "aarch64")
    set(KERNEL_ARCH aarch64)
elseif ("${SERENITY_ARCH}" STREQUAL "i686")
    set(KERNEL_ARCH i386)
elseif("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(KERNEL_ARCH x86_64)
endif()

set(KERNEL_HEAP_SOURCES
    Heap/kmalloc.cpp
)

set(KERNEL_SOURCES
    AddressSanitizer.cpp
    Bus/PCI/Controller/HostBridge.cpp
    Bus/PCI/Controller/MemoryBackedHostBridge.cpp
    Bus/PCI/Controller/VolumeManagementDevice.cpp
    Bus/PCI/Access.cpp
    Bus/PCI/API.cpp
    Bus/PCI/Device.cpp
    Bus/PCI/Initializer.cpp
    Bus/PCI/SysFSPCI.cpp
    Bus/USB/SysFSUSB.cpp
    Bus/USB/UHCI/UHCIController.cpp
    Bus/USB/UHCI/UHCIRootHub.cpp
    Bus/USB/USBController.cpp
    Bus/USB/USBDevice.cpp
    Bus/USB/USBHub.cpp
    Bus/USB/USBManagement.cpp
    Bus/USB/USBPipe.cpp
    Bus/USB/USBTransfer.cpp
    Bus/VirtIO/Console.cpp
    Bus/VirtIO/ConsolePort.cpp
    Bus/VirtIO/Device.cpp
    Bus/VirtIO/Queue.cpp
    Bus/VirtIO/RNG.cpp
    CMOS.cpp
    CommandLine.cpp
    Coredump.cpp
    Devices/AsyncDeviceRequest.cpp
    Devices/Audio/AC97.cpp
    Devices/Audio/SB16.cpp
    Devices/BlockDevice.cpp
    Devices/CharacterDevice.cpp
    Devices/ConsoleDevice.cpp
    Devices/Device.cpp
    Devices/DeviceControlDevice.cpp
    Devices/DeviceManagement.cpp
    Devices/FullDevice.cpp
    Devices/KCOVDevice.cpp
    Devices/KCOVInstance.cpp
    Devices/MemoryDevice.cpp
    Devices/NullDevice.cpp
    Devices/PCISerialDevice.cpp
    Devices/PCSpeaker.cpp
    Devices/RandomDevice.cpp
    Devices/SerialDevice.cpp
    Devices/VMWareBackdoor.cpp
    Devices/ZeroDevice.cpp
    Devices/HID/I8042Controller.cpp
    Devices/HID/HIDManagement.cpp
    Devices/HID/KeyboardDevice.cpp
    Devices/HID/MouseDevice.cpp
    Devices/HID/PS2KeyboardDevice.cpp
    Devices/HID/PS2MouseDevice.cpp
    Devices/HID/VMWareMouseDevice.cpp
    GlobalProcessExposed.cpp
    Graphics/Bochs/GraphicsAdapter.cpp
    Graphics/Console/BootFramebufferConsole.cpp
    Graphics/Console/GenericFramebufferConsole.cpp
    Graphics/Console/ContiguousFramebufferConsole.cpp
    Graphics/Console/TextModeConsole.cpp
    Graphics/Console/VGAConsole.cpp
    Graphics/FramebufferDevice.cpp
    Graphics/GraphicsManagement.cpp
    Graphics/Intel/NativeGraphicsAdapter.cpp
    Graphics/VirtIOGPU/FramebufferDevice.cpp
    Graphics/VirtIOGPU/Console.cpp
    Graphics/VirtIOGPU/GraphicsAdapter.cpp
    Graphics/VGACompatibleAdapter.cpp
    Graphics/GenericFramebufferDevice.cpp
    SanCov.cpp
    Storage/ATA/AHCIController.cpp
    Storage/ATA/AHCIPort.cpp
    Storage/ATA/AHCIPortHandler.cpp
    Storage/ATA/ATADevice.cpp
    Storage/ATA/ATADiskDevice.cpp
    Storage/ATA/ATAPIDiscDevice.cpp
    Storage/ATA/BMIDEChannel.cpp
    Storage/ATA/IDEController.cpp
    Storage/ATA/IDEChannel.cpp
    Storage/Partition/DiskPartition.cpp
    Storage/Partition/DiskPartitionMetadata.cpp
    Storage/Partition/EBRPartitionTable.cpp
    Storage/Partition/GUIDPartitionTable.cpp
    Storage/Partition/MBRPartitionTable.cpp
    Storage/Partition/PartitionTable.cpp
    Storage/NVMe/NVMeController.cpp
    Storage/NVMe/NVMeNameSpace.cpp
    Storage/NVMe/NVMeInterruptQueue.cpp
    Storage/NVMe/NVMePollQueue.cpp
    Storage/NVMe/NVMeQueue.cpp
    Storage/StorageDevice.cpp
    Storage/RamdiskController.cpp
    Storage/RamdiskDevice.cpp
    Storage/StorageManagement.cpp
    DoubleBuffer.cpp
    FileSystem/AnonymousFile.cpp
    FileSystem/BlockBasedFileSystem.cpp
    FileSystem/Custody.cpp
    FileSystem/DevPtsFS.cpp
    FileSystem/DevTmpFS.cpp
    FileSystem/Ext2FileSystem.cpp
    FileSystem/FIFO.cpp
    FileSystem/File.cpp
    FileSystem/FileBackedFileSystem.cpp
    FileSystem/FileSystem.cpp
    FileSystem/Inode.cpp
    FileSystem/InodeFile.cpp
    FileSystem/InodeWatcher.cpp
    FileSystem/ISO9660FileSystem.cpp
    FileSystem/Mount.cpp
    FileSystem/OpenFileDescription.cpp
    FileSystem/Plan9FileSystem.cpp
    FileSystem/ProcFS.cpp
    FileSystem/SysFS.cpp
    FileSystem/SysFSComponent.cpp
    FileSystem/TmpFS.cpp
    FileSystem/VirtualFileSystem.cpp
    Firmware/ACPI/Initialize.cpp
    Firmware/ACPI/Parser.cpp
    Firmware/MultiProcessor/Parser.cpp
    Firmware/BIOS.cpp
    Firmware/PowerStateSwitch.cpp
    Firmware/SysFSFirmware.cpp
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
    KLexicalPath.cpp
    KString.cpp
    KSyms.cpp
    Memory/AddressSpace.cpp
    Memory/AnonymousVMObject.cpp
    Memory/InodeVMObject.cpp
    Memory/MemoryManager.cpp
    Memory/PageDirectory.cpp
    Memory/PhysicalPage.cpp
    Memory/PhysicalRegion.cpp
    Memory/PhysicalZone.cpp
    Memory/PrivateInodeVMObject.cpp
    Memory/Region.cpp
    Memory/RingBuffer.cpp
    Memory/ScatterGatherList.cpp
    Memory/ScopedAddressSpaceSwitcher.cpp
    Memory/SharedInodeVMObject.cpp
    Memory/VMObject.cpp
    Memory/VirtualRange.cpp
    Memory/VirtualRangeAllocator.cpp
    MiniStdLib.cpp
    Locking/LockRank.cpp
    Locking/Mutex.cpp
    Net/Intel/E1000ENetworkAdapter.cpp
    Net/Intel/E1000NetworkAdapter.cpp
    Net/NE2000/NetworkAdapter.cpp
    Net/Realtek/RTL8139NetworkAdapter.cpp
    Net/Realtek/RTL8168NetworkAdapter.cpp
    Net/IPv4Socket.cpp
    Net/LocalSocket.cpp
    Net/LoopbackAdapter.cpp
    Net/NetworkAdapter.cpp
    Net/NetworkTask.cpp
    Net/NetworkingManagement.cpp
    Net/Routing.cpp
    Net/Socket.cpp
    Net/TCPSocket.cpp
    Net/UDPSocket.cpp
    Panic.cpp
    PerformanceEventBuffer.cpp
    Process.cpp
    ProcessExposed.cpp
    ProcessSpecificExposed.cpp
    ProcessGroup.cpp
    ProcessProcFSTraits.cpp
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
    Syscalls/clock.cpp
    Syscalls/debug.cpp
    Syscalls/disown.cpp
    Syscalls/dup2.cpp
    Syscalls/emuctl.cpp
    Syscalls/execve.cpp
    Syscalls/exit.cpp
    Syscalls/fcntl.cpp
    Syscalls/fork.cpp
    Syscalls/fsync.cpp
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
    Syscalls/mount.cpp
    Syscalls/open.cpp
    Syscalls/perf_event.cpp
    Syscalls/pipe.cpp
    Syscalls/pledge.cpp
    Syscalls/poll.cpp
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
    Syscalls/sendfd.cpp
    Syscalls/setpgid.cpp
    Syscalls/setuid.cpp
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
    WaitQueue.cpp
    WorkQueue.cpp
    init.cpp
    kprintf.cpp
)

if ("${SERENITY_ARCH}" STREQUAL "i686" OR "${SERENITY_ARCH}" STREQUAL "x86_64")
    set(KERNEL_SOURCES
        ${KERNEL_SOURCES}
        Arch/x86/common/ScopedCritical.cpp
        Arch/x86/common/SmapDisabler.cpp
        Arch/x86/common/Spinlock.cpp
    )

    set(KERNEL_SOURCES
        ${KERNEL_SOURCES}
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/${KERNEL_ARCH}/ASM_wrapper.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/${KERNEL_ARCH}/Boot/ap_setup.S
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/${KERNEL_ARCH}/InterruptEntry.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/${KERNEL_ARCH}/Processor.cpp
    )

    set(KERNEL_SOURCES
        ${KERNEL_SOURCES}
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/common/ASM_wrapper.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/common/CPU.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/common/Interrupts.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/common/Processor.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/common/ProcessorInfo.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/common/SafeMem.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/common/TrapFrame.cpp
    )

    if("${SERENITY_ARCH}" STREQUAL "x86_64")
        set(KERNEL_SOURCES
            ${KERNEL_SOURCES}
            ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/${KERNEL_ARCH}/SyscallEntry.cpp
        )
    endif()
endif()

set(AK_SOURCES
    ../AK/FlyString.cpp
    ../AK/GenericLexer.cpp
    ../AK/Hex.cpp
    ../AK/String.cpp
    ../AK/StringBuilder.cpp
    ../AK/StringImpl.cpp
    ../AK/StringUtils.cpp
    ../AK/StringView.cpp
    ../AK/Time.cpp
    ../AK/Format.cpp
    ../AK/UUID.cpp
    ../AK/Utf8View.cpp
    ../AK/Utf16View.cpp
)

set(EDID_SOURCES
    ../Userland/Libraries/LibEDID/DMT.cpp
    ../Userland/Libraries/LibEDID/EDID.cpp
    ../Userland/Libraries/LibEDID/VIC.cpp
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

set(CRYPTO_SOURCES
    ../Userland/Libraries/LibCrypto/Cipher/AES.cpp
    ../Userland/Libraries/LibCrypto/Hash/SHA2.cpp
)

set(SOURCES
    ${AK_SOURCES}
    )
if (NOT "${SERENITY_ARCH}" STREQUAL "aarch64")
    set(SOURCES
        ${KERNEL_SOURCES}
        ${SOURCES}
        ${EDID_SOURCES}
        ${ELF_SOURCES}
        ${VT_SOURCES}
        ${CRYPTO_SOURCES}
    )
else()
    set(SOURCES
        ${SOURCES}
        ${AK_SOURCES}
        
        Arch/aarch64/dummy.cpp
        Arch/aarch64/SmapDisabler.cpp
        Arch/aarch64/ScopedCritical.cpp
        StdLib.cpp
        MiniStdLib.cpp
        UBSanitizer.cpp
    )

    # Otherwise linker errors e.g undefined reference to `__aarch64_cas8_acq_rel'
    add_compile_options(-mno-outline-atomics -latomic)
endif()

add_compile_options(-fsigned-char)
add_compile_options(-Wno-unknown-warning-option -Wvla -Wnull-dereference)
add_compile_options(-fno-rtti -ffreestanding -fbuiltin)
if ("${SERENITY_ARCH}" STREQUAL "i686" OR "${SERENITY_ARCH}" STREQUAL "x86_64")
    add_compile_options(-mno-80387 -mno-mmx -mno-sse -mno-sse2)
endif()
add_compile_options(-fno-asynchronous-unwind-tables)
add_compile_options(-fstack-protector-strong)
add_compile_options(-fno-exceptions)
# FIXME: remove -nodefaultlibs after the next toolchain update
add_compile_options(-nodefaultlibs -nostdlib)

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    # Apply any flags that are only available on >= GCC 11.1
    if (CMAKE_CXX_COMPILER_VERSION VERSION_GREATER_EQUAL "11.1")
        # Zero any registers used within a function on return (to reduce data lifetime and ROP gadgets).
        add_compile_options(-fzero-call-used-regs=used-gpr)
    endif()
    link_directories(${TOOLCHAIN_ROOT}/${SERENITY_ARCH}-pc-serenity/lib)
    link_directories(${TOOLCHAIN_ROOT}/lib/gcc/${SERENITY_ARCH}-pc-serenity/${GCC_VERSION}/)

    set(TARGET_STRING "")
else() # Assume Clang
    add_compile_options(-Waddress-of-packed-member)
    add_compile_options(-faligned-allocation)

    # We need this in order to pick up the #define __serenity__, otherwise we end up including unistd.h into the linker script
    set(TARGET_STRING "--target=${CMAKE_CXX_COMPILER_TARGET}")
    
    add_link_options(LINKER:--build-id=none)
endif()

macro (set_new_alignment alignment)
    if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
        add_compile_options(-faligned-new=${alignment})
    elseif (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
        add_compile_options(-fnew-alignment=${alignment})
    endif()
endmacro()

if ("${SERENITY_ARCH}" STREQUAL "x86_64")
    add_compile_options(-mcmodel=large -mno-red-zone)
    set_new_alignment(8)
else()
    set_new_alignment(4)
endif()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -static-pie")

# Kernel Coverage (KCOV) is an API to collect and expose program counters of
# kernel code that has been run to user space. It's rather slow and likely not
# secure to run in production builds. Useful for coverage guided fuzzing.
if (ENABLE_KERNEL_COVERAGE_COLLECTION)
    add_definitions(-DENABLE_KERNEL_COVERAGE_COLLECTION)
    add_compile_options(-fsanitize-coverage=trace-pc)
    set(KCOV_EXCLUDED_SOURCES
        # Make sure we don't instrument any code called from __sanitizer_cov_trace_pc
        # otherwise we'll end up with recursive calls to that function.
        ../AK/Format.cpp
        ../AK/StringBuilder.cpp
        ../Kernel/Arch/x86/${KERNEL_ARCH}/Processor.cpp
        ../Kernel/Devices/KCOVDevice.cpp
        ../Kernel/Devices/KCOVInstance.cpp
        ../Kernel/FileSystem/File.cpp
        ../Kernel/FileSystem/OpenFileDescription.cpp
        ../Kernel/init.cpp
        ../Kernel/SanCov.cpp
        # GCC assumes that the caller saves registers for functions according
        # to the System V ABI and happily inserts coverage calls into the
        # function prologue for all functions. This assumption is not true for
        # interrupt handlers because their calling convention is not compatible
        # with the System V ABI.
        ../Kernel/Arch/x86/common/Interrupts.cpp
        ../Kernel/Syscall.cpp
    )
    set_source_files_properties(${KCOV_EXCLUDED_SOURCES} PROPERTIES COMPILE_FLAGS "-fno-sanitize-coverage=trace-pc")
endif()

# Kernel Undefined Behavior Sanitizer (KUBSAN)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fsanitize=undefined")

# Kernel Address Sanitize (KASAN) implementation is still a work in progress, this option
# is not currently meant to be used, besides when developing Kernel ASAN support.
#
if (ENABLE_KERNEL_ADDRESS_SANITIZER)
    add_compile_options(-fsanitize=kernel-address)
    add_link_options(-fsanitize=kernel-address)
endif()

add_compile_definitions(KERNEL)
add_link_options(LINKER:-z,notext)

if (NOT "${SERENITY_ARCH}" STREQUAL "aarch64")
    add_library(kernel_heap STATIC ${KERNEL_HEAP_SOURCES})
endif()
add_executable(Kernel ${SOURCES})
add_dependencies(Kernel generate_EscapeSequenceStateMachine.h)

add_custom_command(
    OUTPUT linker.ld
    COMMAND "${CMAKE_CXX_COMPILER}" ${TARGET_STRING} -E -P -x c -I${CMAKE_CURRENT_SOURCE_DIR}/.. "${CMAKE_CURRENT_SOURCE_DIR}/linker.ld" -o "${CMAKE_CURRENT_BINARY_DIR}/linker.ld"
    MAIN_DEPENDENCY "${CMAKE_CURRENT_SOURCE_DIR}/linker.ld"
    COMMENT "Preprocessing linker.ld"
    VERBATIM
)

add_custom_target(generate_kernel_linker_script DEPENDS linker.ld)
target_link_options(Kernel PRIVATE LINKER:-T ${CMAKE_CURRENT_BINARY_DIR}/linker.ld -nostdlib -nodefaultlibs)
set_target_properties(Kernel PROPERTIES LINK_DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/linker.ld")

if (ENABLE_KERNEL_LTO)
    include(CheckIPOSupported)
    check_ipo_supported()
    add_definitions(-DENABLE_KERNEL_LTO)
    set_property(TARGET Kernel PROPERTY INTERPROCEDURAL_OPTIMIZATION TRUE)
    if (NOT "${SERENITY_ARCH}" STREQUAL "aarch64")
        set_property(TARGET kernel_heap PROPERTY INTERPROCEDURAL_OPTIMIZATION TRUE)
    endif()
endif()

if (NOT "${SERENITY_ARCH}" STREQUAL "aarch64")
    if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
        target_link_libraries(Kernel PRIVATE kernel_heap gcc)
    elseif(CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
        target_link_libraries(Kernel PRIVATE kernel_heap clang_rt.builtins)
    endif()
endif()

add_custom_command(
    TARGET Kernel POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E env NM=${CMAKE_NM} sh ${CMAKE_CURRENT_SOURCE_DIR}/mkmap.sh
    COMMAND ${CMAKE_COMMAND} -E env OBJCOPY=${CMAKE_OBJCOPY} sh ${CMAKE_CURRENT_SOURCE_DIR}/embedmap.sh
    COMMAND ${CMAKE_OBJCOPY} --only-keep-debug Kernel Kernel.debug
    COMMAND ${CMAKE_OBJCOPY} --strip-debug Kernel
    COMMAND ${CMAKE_OBJCOPY} --add-gnu-debuglink=Kernel.debug Kernel
    BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/kernel.map
)

install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Kernel" DESTINATION boot)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Kernel.debug" DESTINATION boot)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/kernel.map" DESTINATION res)

serenity_install_headers(Kernel)
serenity_install_sources(Kernel)

add_subdirectory(Prekernel)
