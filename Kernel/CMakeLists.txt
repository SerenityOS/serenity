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
    Bus/PCI/Controller/HostController.cpp
    Bus/PCI/Controller/MemoryBackedHostBridge.cpp
    Bus/PCI/Controller/VolumeManagementDevice.cpp
    Bus/PCI/Access.cpp
    Bus/PCI/API.cpp
    Bus/PCI/Device.cpp
    Bus/USB/UHCI/UHCIController.cpp
    Bus/USB/UHCI/UHCIRootHub.cpp
    Bus/USB/USBConfiguration.cpp
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
    CommandLine.cpp
    Coredump.cpp
    Credentials.cpp
    Devices/AsyncDeviceRequest.cpp
    Devices/Audio/AC97.cpp
    Devices/Audio/Channel.cpp
    Devices/Audio/Management.cpp
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
    Devices/RandomDevice.cpp
    Devices/SelfTTYDevice.cpp
    Devices/SerialDevice.cpp
    Devices/ZeroDevice.cpp
    Devices/HID/HIDManagement.cpp
    Devices/HID/KeyboardDevice.cpp
    Devices/HID/MouseDevice.cpp
    GlobalProcessExposed.cpp
    Graphics/Bochs/GraphicsAdapter.cpp
    Graphics/Bochs/QEMUDisplayConnector.cpp
    Graphics/Console/BootFramebufferConsole.cpp
    Graphics/Console/GenericFramebufferConsole.cpp
    Graphics/Console/ContiguousFramebufferConsole.cpp
    Graphics/Console/VGATextModeConsole.cpp
    Graphics/DisplayConnector.cpp
    Graphics/Generic/DisplayConnector.cpp
    Graphics/GraphicsManagement.cpp
    Graphics/Intel/NativeDisplayConnector.cpp
    Graphics/Intel/NativeGraphicsAdapter.cpp
    Graphics/VMWare/Console.cpp
    Graphics/VMWare/GraphicsAdapter.cpp
    Graphics/VMWare/DisplayConnector.cpp
    Graphics/VirtIOGPU/DisplayConnector.cpp
    Graphics/VirtIOGPU/Console.cpp
    Graphics/VirtIOGPU/GPU3DDevice.cpp
    Graphics/VirtIOGPU/GraphicsAdapter.cpp
    IOWindow.cpp
    SanCov.cpp
    Storage/ATA/AHCI/Controller.cpp
    Storage/ATA/AHCI/Port.cpp
    Storage/ATA/AHCI/InterruptHandler.cpp
    Storage/ATA/GenericIDE/Controller.cpp
    Storage/ATA/GenericIDE/Channel.cpp
    Storage/ATA/ATAController.cpp
    Storage/ATA/ATADevice.cpp
    Storage/ATA/ATADiskDevice.cpp
    Storage/ATA/ATAPort.cpp
    Storage/NVMe/NVMeController.cpp
    Storage/NVMe/NVMeNameSpace.cpp
    Storage/NVMe/NVMeInterruptQueue.cpp
    Storage/NVMe/NVMePollQueue.cpp
    Storage/NVMe/NVMeQueue.cpp
    Storage/DiskPartition.cpp
    Storage/StorageController.cpp
    Storage/StorageDevice.cpp
    Storage/StorageManagement.cpp
    DoubleBuffer.cpp
    FileSystem/AnonymousFile.cpp
    FileSystem/BlockBasedFileSystem.cpp
    FileSystem/Custody.cpp
    FileSystem/DevPtsFS.cpp
    FileSystem/DevTmpFS.cpp
    FileSystem/Ext2FileSystem.cpp
    FileSystem/FATFileSystem.cpp
    FileSystem/FIFO.cpp
    FileSystem/File.cpp
    FileSystem/FileBackedFileSystem.cpp
    FileSystem/FileSystem.cpp
    FileSystem/Inode.cpp
    FileSystem/InodeFile.cpp
    FileSystem/InodeMetadata.cpp
    FileSystem/InodeWatcher.cpp
    FileSystem/ISO9660FileSystem.cpp
    FileSystem/Mount.cpp
    FileSystem/OpenFileDescription.cpp
    FileSystem/Plan9FileSystem.cpp
    FileSystem/ProcFS.cpp
    FileSystem/SysFS.cpp
    FileSystem/SysFS/Component.cpp
    FileSystem/SysFS/Registry.cpp
    FileSystem/SysFS/RootDirectory.cpp
    FileSystem/SysFS/Subsystems/Bus/PCI/BusDirectory.cpp
    FileSystem/SysFS/Subsystems/Bus/PCI/DeviceAttribute.cpp
    FileSystem/SysFS/Subsystems/Bus/PCI/DeviceDirectory.cpp
    FileSystem/SysFS/Subsystems/Bus/USB/BusDirectory.cpp
    FileSystem/SysFS/Subsystems/Bus/USB/DeviceInformation.cpp
    FileSystem/SysFS/Subsystems/Bus/Directory.cpp
    FileSystem/SysFS/Subsystems/DeviceIdentifiers/BlockDevicesDirectory.cpp
    FileSystem/SysFS/Subsystems/DeviceIdentifiers/CharacterDevicesDirectory.cpp
    FileSystem/SysFS/Subsystems/DeviceIdentifiers/DeviceComponent.cpp
    FileSystem/SysFS/Subsystems/DeviceIdentifiers/Directory.cpp
    FileSystem/SysFS/Subsystems/DeviceIdentifiers/SymbolicLinkDeviceComponent.cpp
    FileSystem/SysFS/Subsystems/Devices/Storage/DeviceAttribute.cpp
    FileSystem/SysFS/Subsystems/Devices/Storage/DeviceDirectory.cpp
    FileSystem/SysFS/Subsystems/Devices/Storage/Directory.cpp
    FileSystem/SysFS/Subsystems/Devices/Graphics/Directory.cpp
    FileSystem/SysFS/Subsystems/Devices/Graphics/DisplayConnector/Directory.cpp
    FileSystem/SysFS/Subsystems/Devices/Graphics/DisplayConnector/DeviceDirectory.cpp
    FileSystem/SysFS/Subsystems/Devices/Graphics/DisplayConnector/DeviceAttribute.cpp
    FileSystem/SysFS/Subsystems/Devices/Directory.cpp
    FileSystem/SysFS/Subsystems/Firmware/BIOS/Component.cpp
    FileSystem/SysFS/Subsystems/Firmware/BIOS/Directory.cpp
    FileSystem/SysFS/Subsystems/Firmware/Directory.cpp
    FileSystem/SysFS/Subsystems/Firmware/PowerStateSwitch.cpp
    FileSystem/TmpFS.cpp
    FileSystem/VirtualFileSystem.cpp
    Firmware/BIOS.cpp
    Firmware/ACPI/Initialize.cpp
    Firmware/ACPI/Parser.cpp
    Firmware/MultiProcessor/Parser.cpp
    FutexQueue.cpp
    Interrupts/GenericInterruptHandler.cpp
    Interrupts/IRQHandler.cpp
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
    Memory/RegionTree.cpp
    Memory/RingBuffer.cpp
    Memory/ScatterGatherList.cpp
    Memory/ScopedAddressSpaceSwitcher.cpp
    Memory/SharedFramebufferVMObject.cpp
    Memory/SharedInodeVMObject.cpp
    Memory/VMObject.cpp
    Memory/VirtualRange.cpp
    MiniStdLib.cpp
    Locking/LockRank.cpp
    Locking/Mutex.cpp
    Locking/Spinlock.cpp
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
    Syscalls/fallocate.cpp
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
    Syscalls/resource.cpp
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
    Syscalls/umask.cpp
    Syscalls/uname.cpp
    Syscalls/unlink.cpp
    Syscalls/unveil.cpp
    Syscalls/utime.cpp
    Syscalls/utimensat.cpp
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
    Time/TimeManagement.cpp
    TimerQueue.cpp
    UBSanitizer.cpp
    UserOrKernelBuffer.cpp
    WaitQueue.cpp
    WorkQueue.cpp
    kprintf.cpp
)

if ("${SERENITY_ARCH}" STREQUAL "i686" OR "${SERENITY_ARCH}" STREQUAL "x86_64")
    set(KERNEL_SOURCES
        ${KERNEL_SOURCES}
        Arch/x86/init.cpp

        Arch/Processor.cpp

        Arch/x86/common/Interrupts/APIC.cpp
        Arch/x86/common/Interrupts/IOAPIC.cpp
        Arch/x86/common/Interrupts/PIC.cpp

        Arch/x86/common/CMOS.cpp
        Arch/x86/common/DebugOutput.cpp
        Arch/x86/common/Delay.cpp
        Arch/x86/common/I8042Reboot.cpp
        Arch/x86/common/PCSpeaker.cpp
        Arch/x86/common/RTC.cpp
        Arch/x86/common/ScopedCritical.cpp
        Arch/x86/common/SmapDisabler.cpp
        Arch/x86/common/Shutdown.cpp

        Arch/x86/CurrentTime.cpp

        Arch/x86/Hypervisor/BochsDisplayConnector.cpp
        Arch/x86/Hypervisor/VMWareBackdoor.cpp

        Arch/x86/ISABus/HID/PS2KeyboardDevice.cpp
        Arch/x86/ISABus/HID/PS2MouseDevice.cpp
        Arch/x86/ISABus/HID/VMWareMouseDevice.cpp
        Arch/x86/ISABus/I8042Controller.cpp
        Arch/x86/ISABus/IDEController.cpp
        Arch/x86/ISABus/SerialDevice.cpp
        Arch/x86/PCI/Controller/HostBridge.cpp
        Arch/x86/PCI/IDELegacyModeController.cpp
        Arch/x86/PCI/Initializer.cpp

        Arch/x86/Time/APICTimer.cpp
        Arch/x86/Time/HPET.cpp
        Arch/x86/Time/HPETComparator.cpp
        Arch/x86/Time/PIT.cpp
        Arch/x86/Time/RTC.cpp

        Arch/x86/VGA/IOArbiter.cpp
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
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/common/CPUID.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/common/CrashHandler.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/common/InterruptManagement.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/common/Interrupts.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/common/PageDirectory.cpp
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

    if ("${SERENITY_ARCH}" STREQUAL "i686")
        set(KERNEL_SOURCES
            ${KERNEL_SOURCES}
            ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/${KERNEL_ARCH}/Atomics.cpp
        )
    endif()
endif()

set(AK_SOURCES
    ../AK/GenericLexer.cpp
    ../AK/Hex.cpp
    ../AK/StringBuilder.cpp
    ../AK/StringUtils.cpp
    ../AK/StringView.cpp
    ../AK/Time.cpp
    ../AK/Format.cpp
    ../AK/UUID.cpp
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

add_custom_command(
    COMMAND "${SerenityOS_SOURCE_DIR}/Kernel/generate-version-file.sh" "${CMAKE_CURRENT_BINARY_DIR}/Version.h.tmp"
    COMMAND "${CMAKE_COMMAND}" -E copy_if_different "${CMAKE_CURRENT_BINARY_DIR}/Version.h.tmp" "${CMAKE_CURRENT_BINARY_DIR}/Version.h"
    COMMAND "${CMAKE_COMMAND}" -E remove "${CMAKE_CURRENT_BINARY_DIR}/Version.h.tmp"
    WORKING_DIRECTORY "${SerenityOS_SOURCE_DIR}"
    COMMENT "Generating SerenityOS version information"
    OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/Version.h"
    VERBATIM
)
add_custom_target(generate_version_header DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/Version.h")
set(GENERATED_SOURCES "${CMAKE_CURRENT_BINARY_DIR}/Version.h")

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

set(PARTITION_SOURCES
    ../Userland/Libraries/LibPartition/DiskPartitionMetadata.cpp
    ../Userland/Libraries/LibPartition/EBRPartitionTable.cpp
    ../Userland/Libraries/LibPartition/GUIDPartitionTable.cpp
    ../Userland/Libraries/LibPartition/MBRPartitionTable.cpp
    ../Userland/Libraries/LibPartition/PartitionTable.cpp
)

set(SOURCES
    ${AK_SOURCES}
    )
if (NOT "${SERENITY_ARCH}" STREQUAL "aarch64")
    set(SOURCES
        ${KERNEL_SOURCES}
        ${GENERATED_SOURCES}
        ${SOURCES}
        ${EDID_SOURCES}
        ${ELF_SOURCES}
        ${VT_SOURCES}
        ${CRYPTO_SOURCES}
        ${PARTITION_SOURCES}
    )
else()
    set(RPI_SOURCES
        Arch/aarch64/RPi/DebugOutput.cpp
        Arch/aarch64/RPi/Framebuffer.cpp
        Arch/aarch64/RPi/GPIO.cpp
        Arch/aarch64/RPi/InterruptController.cpp
        Arch/aarch64/RPi/Mailbox.cpp
        Arch/aarch64/RPi/MMIO.cpp
        Arch/aarch64/RPi/Timer.cpp
        Arch/aarch64/RPi/UART.cpp
    )
    set(SOURCES
        ${AK_SOURCES}
        ${RPI_SOURCES}

        Arch/Processor.cpp

        Arch/aarch64/boot.S
        Arch/aarch64/BootPPMParser.cpp
        Arch/aarch64/CrashHandler.cpp
        Arch/aarch64/CurrentTime.cpp
        Arch/aarch64/Dummy.cpp
        Arch/aarch64/Exceptions.cpp
        Arch/aarch64/init.cpp
        Arch/aarch64/InterruptManagement.cpp
        Arch/aarch64/Interrupts.cpp
        Arch/aarch64/kprintf.cpp
        Arch/aarch64/MainIdRegister.cpp
        Arch/aarch64/MMU.cpp
        Arch/aarch64/PageDirectory.cpp
        Arch/aarch64/Panic.cpp
        Arch/aarch64/Processor.cpp
        Arch/aarch64/SafeMem.cpp
        Arch/aarch64/ScopedCritical.cpp
        Arch/aarch64/SmapDisabler.cpp
        Arch/aarch64/vector_table.S

        # Files from base Kernel
        CommandLine.cpp
        KString.cpp
        KSyms.cpp
        MiniStdLib.cpp
        StdLib.cpp
        Time/TimeManagement.cpp
        TimerQueue.cpp
        UBSanitizer.cpp
        UserOrKernelBuffer.cpp

        Devices/DeviceManagement.cpp

        Graphics/Console/BootFramebufferConsole.cpp
        Graphics/Console/GenericFramebufferConsole.cpp

        Locking/Spinlock.cpp

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
        Memory/RegionTree.cpp
        Memory/RingBuffer.cpp
        Memory/ScatterGatherList.cpp
        Memory/SharedInodeVMObject.cpp
        Memory/VirtualRange.cpp
        Memory/VMObject.cpp

        Interrupts/GenericInterruptHandler.cpp
        Interrupts/IRQHandler.cpp
        Interrupts/SharedIRQHandler.cpp
        Interrupts/UnhandledInterruptHandler.cpp
    )

    # Otherwise linker errors e.g undefined reference to `__aarch64_cas8_acq_rel'
    add_compile_options(-mno-outline-atomics -latomic)

    # FIXME: Remove this once compiling MemoryManager.cpp doesn't give the nonnull error anymore.
    add_compile_options(-Wno-nonnull)
endif()

add_compile_options(-fsigned-char)
add_compile_options(-Wno-unknown-warning-option -Wvla -Wnull-dereference)
add_compile_options(-fno-rtti -ffreestanding -fbuiltin)

if ("${SERENITY_ARCH}" STREQUAL "i686" OR "${SERENITY_ARCH}" STREQUAL "x86_64")
    add_compile_options(-mno-80387 -mno-mmx -mno-sse -mno-sse2)
elseif("${SERENITY_ARCH}" STREQUAL "aarch64")
    add_compile_options(-mgeneral-regs-only)
endif()

add_compile_options(-fno-asynchronous-unwind-tables)
add_compile_options(-fstack-protector-strong)
add_compile_options(-fno-exceptions)
add_compile_options(-nostdlib)

# Auto initialize trivial types on the stack, we use "pattern" as 
# it's the only option portable across compilers going forward.
#
# This is designed to help avoid uninitialized variables bugs and
# information disclosures coming from the kernel stack. 
#
# FIXME: It appears to conflict with something during the boot of the
# aarch64 kernel, we should investigate and remove this special case.
if (NOT "${SERENITY_ARCH}" STREQUAL "aarch64")
    add_compile_options(-ftrivial-auto-var-init=pattern)
endif()

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    # Apply any flags that are only available on >= GCC 11.1
    if (CMAKE_CXX_COMPILER_VERSION VERSION_GREATER_EQUAL "11.1")
        # Zero any registers used within a function on return (to reduce data lifetime and ROP gadgets).
        add_compile_options(-fzero-call-used-regs=used-gpr)
    endif()
    link_directories(${TOOLCHAIN_ROOT}/${SERENITY_ARCH}-pc-serenity/lib)
    link_directories(${TOOLCHAIN_ROOT}/lib/gcc/${SERENITY_ARCH}-pc-serenity/${GCC_VERSION}/)

    set(TARGET_STRING "")


    # Prevent naively implemented string functions (like strlen) from being "optimized" into a call to themselves.
    set_source_files_properties(MiniStdLib.cpp
        PROPERTIES COMPILE_FLAGS "-fno-tree-loop-distribution -fno-tree-loop-distribute-patterns")

    add_link_options(LINKER:-z,pack-relative-relocs)
else() # Assume Clang
    add_compile_options(-Waddress-of-packed-member)
    add_compile_options(-faligned-allocation)

    # We need this in order to pick up the #define __serenity__, otherwise we end up including unistd.h into the linker script
    set(TARGET_STRING "--target=${CMAKE_CXX_COMPILER_TARGET}")

    add_link_options(LINKER:--build-id=none LINKER:--pack-dyn-relocs=relr)
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
elseif ("${SERENITY_ARCH}" STREQUAL "i686")
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
elseif (ENABLE_USERSPACE_COVERAGE_COLLECTION)
    # Disable checking open() pledges and the veil for coverage data when building userspace with coverage
    # so that binaries can write out coverage data even with pledges/veil
    add_compile_definitions(SKIP_PATH_VALIDATION_FOR_COVERAGE_INSTRUMENTATION)
endif()

if (ENABLE_KERNEL_UNDEFINED_SANITIZER)
    # Kernel Undefined Behavior Sanitizer (KUBSAN)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fsanitize=undefined")
endif()

# Kernel Address Sanitize (KASAN) implementation is still a work in progress, this option
# is not currently meant to be used, besides when developing Kernel ASAN support.
#
if (ENABLE_KERNEL_ADDRESS_SANITIZER)
    add_compile_options(-fsanitize=kernel-address)
    add_link_options(-fsanitize=kernel-address)
endif()

if ("${SERENITY_ARCH}" STREQUAL "aarch64")
    add_compile_options(-fno-threadsafe-statics)
endif()

add_compile_definitions(KERNEL)
add_link_options(LINKER:-z,notext)

add_library(kernel_heap STATIC ${KERNEL_HEAP_SOURCES})

add_executable(Kernel ${SOURCES})
add_dependencies(Kernel generate_EscapeSequenceStateMachine.h generate_version_header)

if (NOT "${SERENITY_ARCH}" STREQUAL "aarch64")
    add_custom_command(
        OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/linker.ld
        COMMAND "${CMAKE_CXX_COMPILER}" ${TARGET_STRING} -E -P -x c -I${CMAKE_CURRENT_SOURCE_DIR}/.. "${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/linker.ld" -o "${CMAKE_CURRENT_BINARY_DIR}/linker.ld"
        MAIN_DEPENDENCY "Arch/x86/linker.ld"
        COMMENT "Preprocessing linker.ld"
        VERBATIM
    )

    add_custom_target(generate_kernel_linker_script DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/linker.ld)
    target_link_options(Kernel PRIVATE LINKER:-T ${CMAKE_CURRENT_BINARY_DIR}/linker.ld -nostdlib -nodefaultlibs)
    set_target_properties(Kernel PROPERTIES LINK_DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/linker.ld")
else()
    target_link_options(Kernel PRIVATE LINKER:-T ${CMAKE_CURRENT_SOURCE_DIR}/Arch/aarch64/linker.ld -nostdlib LINKER:--no-pie)
    set_target_properties(Kernel PROPERTIES LINK_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/Arch/aarch64/linker.ld)
endif()

if (ENABLE_KERNEL_LTO)
    include(CheckIPOSupported)
    check_ipo_supported()
    add_definitions(-DENABLE_KERNEL_LTO)
    set_property(TARGET Kernel PROPERTY INTERPROCEDURAL_OPTIMIZATION TRUE)
    if (NOT "${SERENITY_ARCH}" STREQUAL "aarch64")
        set_property(TARGET kernel_heap PROPERTY INTERPROCEDURAL_OPTIMIZATION TRUE)
    endif()
endif()

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    target_link_libraries(Kernel PRIVATE kernel_heap gcc)
elseif(CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    target_link_libraries(Kernel PRIVATE kernel_heap clang_rt.builtins)
endif()

add_custom_command(
        TARGET Kernel POST_BUILD
        COMMAND "${CMAKE_COMMAND}" -E env NM=${CMAKE_NM} sh ${CMAKE_CURRENT_SOURCE_DIR}/mkmap.sh
        COMMAND "${CMAKE_COMMAND}" -E env OBJCOPY=${CMAKE_OBJCOPY} sh ${CMAKE_CURRENT_SOURCE_DIR}/embedmap.sh
        COMMAND ${CMAKE_OBJCOPY} --only-keep-debug Kernel Kernel.debug
        COMMAND ${CMAKE_OBJCOPY} --strip-debug Kernel
        COMMAND ${CMAKE_OBJCOPY} --add-gnu-debuglink=Kernel.debug Kernel
        BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/kernel.map
)

install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Kernel" DESTINATION boot)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Kernel.debug" DESTINATION boot)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/kernel.map" DESTINATION res)

if ("${SERENITY_ARCH}" STREQUAL "aarch64")
    embed_resource(Kernel serenity_boot_logo "Arch/aarch64/SerenityLogoRGB.ppm")
    add_custom_command(
            TARGET Kernel POST_BUILD
            COMMAND ${CMAKE_OBJCOPY} -O binary Kernel kernel8.img
            BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/kernel8.img
    )
endif()

serenity_install_headers(Kernel)
serenity_install_sources(Kernel)

# aarch64 does not need a Prekernel
if (NOT "${SERENITY_ARCH}" STREQUAL "aarch64")
    add_subdirectory(Prekernel)
endif()
