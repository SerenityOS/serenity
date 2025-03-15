if (ENABLE_EXTRA_KERNEL_DEBUG_SYMBOLS)
    add_compile_options(-Og)
    add_compile_options(-ggdb3)
else()
    add_compile_options(-O2)
endif()

if ("${SERENITY_ARCH}" STREQUAL "aarch64")
    set(KERNEL_ARCH aarch64)
elseif("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(KERNEL_ARCH x86_64)
elseif("${SERENITY_ARCH}" STREQUAL "riscv64")
    set(KERNEL_ARCH riscv64)
endif()

set(KERNEL_HEAP_SOURCES
    Heap/kmalloc.cpp
)

set(KERNEL_SOURCES
    Arch/init.cpp
    Arch/DeferredCallPool.cpp
    Arch/PageFault.cpp
    Arch/Processor.cpp
    Arch/TrapFrame.cpp
    Boot/CommandLine.cpp
    Bus/PCI/Controller/HostController.cpp
    Bus/PCI/Controller/MemoryBackedHostBridge.cpp
    Bus/PCI/Controller/VolumeManagementDevice.cpp
    Bus/PCI/Access.cpp
    Bus/PCI/API.cpp
    Bus/PCI/Device.cpp
    Bus/PCI/DeviceIdentifier.cpp
    Bus/USB/EHCI/EHCIController.cpp
    Bus/USB/UHCI/UHCIController.cpp
    Bus/USB/UHCI/UHCIRootHub.cpp
    Bus/USB/xHCI/DeviceTreexHCIController.cpp
    Bus/USB/xHCI/PCIxHCIController.cpp
    Bus/USB/xHCI/xHCIController.cpp
    Bus/USB/xHCI/xHCIInterrupter.cpp
    Bus/USB/xHCI/xHCIRootHub.cpp
    Bus/USB/Drivers/HID.cpp
    Bus/USB/Drivers/MassStorage/MassStorageDriver.cpp
    Bus/USB/USBConfiguration.cpp
    Bus/USB/USBController.cpp
    Bus/USB/USBDevice.cpp
    Bus/USB/USBHub.cpp
    Bus/USB/USBManagement.cpp
    Bus/USB/USBPipe.cpp
    Bus/USB/USBTransfer.cpp
    Bus/VirtIO/Transport/PCIe/Detect.cpp
    Bus/VirtIO/Transport/PCIe/InterruptHandler.cpp
    Bus/VirtIO/Transport/PCIe/TransportLink.cpp
    Bus/VirtIO/Transport/Entity.cpp
    Bus/VirtIO/Transport/InterruptHandler.cpp
    Bus/VirtIO/Device.cpp
    Bus/VirtIO/Queue.cpp
    Devices/AsyncDeviceRequest.cpp
    Devices/Audio/AC97/AC97.cpp
    Devices/Audio/Channel.cpp
    Devices/Audio/IntelHDA/Codec.cpp
    Devices/Audio/IntelHDA/Controller.cpp
    Devices/Audio/IntelHDA/Format.cpp
    Devices/Audio/IntelHDA/InterruptHandler.cpp
    Devices/Audio/IntelHDA/Stream.cpp
    Devices/Audio/Management.cpp
    Devices/BlockDevice.cpp
    Devices/CharacterDevice.cpp
    Devices/Device.cpp
    Devices/FUSEDevice.cpp
    Devices/PCISerialDevice.cpp
    Devices/SerialDevice.cpp
    Devices/Generic/ConsoleDevice.cpp
    Devices/Generic/DeviceControlDevice.cpp
    Devices/Generic/FullDevice.cpp
    Devices/Generic/MemoryDevice.cpp
    Devices/Generic/NullDevice.cpp
    Devices/Generic/PCSpeakerDevice.cpp
    Devices/Generic/RandomDevice.cpp
    Devices/Generic/SelfTTYDevice.cpp
    Devices/Generic/ZeroDevice.cpp
    Devices/GPU/3dfx/GraphicsAdapter.cpp
    Devices/GPU/3dfx/VoodooDisplayConnector.cpp
    Devices/GPU/Bochs/GraphicsAdapter.cpp
    Devices/GPU/Bochs/QEMUDisplayConnector.cpp
    Devices/GPU/Console/BootFramebufferConsole.cpp
    Devices/GPU/Console/GenericFramebufferConsole.cpp
    Devices/GPU/Console/ContiguousFramebufferConsole.cpp
    Devices/GPU/DisplayConnector.cpp
    Devices/GPU/Generic/DisplayConnector.cpp
    Devices/GPU/Generic/SimpleFramebuffer.cpp
    Devices/GPU/Management.cpp
    Devices/GPU/Intel/Auxiliary/GMBusConnector.cpp
    Devices/GPU/Intel/Plane/DisplayPlane.cpp
    Devices/GPU/Intel/Plane/G33DisplayPlane.cpp
    Devices/GPU/Intel/Transcoder/AnalogDisplayTranscoder.cpp
    Devices/GPU/Intel/Transcoder/DisplayTranscoder.cpp
    Devices/GPU/Intel/Transcoder/PLL.cpp
    Devices/GPU/Intel/DisplayConnectorGroup.cpp
    Devices/GPU/Intel/NativeDisplayConnector.cpp
    Devices/GPU/Intel/NativeGraphicsAdapter.cpp
    Devices/GPU/VMWare/Console.cpp
    Devices/GPU/VMWare/GraphicsAdapter.cpp
    Devices/GPU/VMWare/DisplayConnector.cpp
    Devices/GPU/VirtIO/DisplayConnector.cpp
    Devices/GPU/VirtIO/Console.cpp
    Devices/GPU/VirtIO/GPU3DDevice.cpp
    Devices/GPU/VirtIO/GraphicsAdapter.cpp
    Devices/Input/HID/Device.cpp
    Devices/Input/HID/KeyboardDriver.cpp
    Devices/Input/HID/MouseDriver.cpp
    Devices/Input/KeyboardDevice.cpp
    Devices/Input/Management.cpp
    Devices/Input/MouseDevice.cpp
    Devices/Input/PS2/KeyboardDevice.cpp
    Devices/Input/PS2/MouseDevice.cpp
    Devices/Input/VirtIO/Input.cpp
    Devices/Loop/LoopDevice.cpp
    Devices/Serial/VirtIO/Console.cpp
    Devices/Serial/VirtIO/ConsolePort.cpp
    Devices/Storage/AHCI/ATADevice.cpp
    Devices/Storage/AHCI/ATADiskDevice.cpp
    Devices/Storage/AHCI/Controller.cpp
    Devices/Storage/AHCI/InterruptHandler.cpp
    Devices/Storage/AHCI/Port.cpp
    Devices/Storage/NVMe/NVMeController.cpp
    Devices/Storage/NVMe/NVMeNameSpace.cpp
    Devices/Storage/NVMe/NVMeInterruptQueue.cpp
    Devices/Storage/NVMe/NVMePollQueue.cpp
    Devices/Storage/NVMe/NVMeQueue.cpp
    Devices/Storage/SD/PCISDHostController.cpp
    Devices/Storage/SD/SDHostController.cpp
    Devices/Storage/SD/SDMemoryCard.cpp
    Devices/Storage/USB/BOT/BulkSCSIInterface.cpp
    Devices/Storage/USB/BOT/BulkSCSIStorageDevice.cpp
    Devices/Storage/USB/UAS/UASInterface.cpp
    Devices/Storage/USB/UAS/UASStorageDevice.cpp
    Devices/Storage/VirtIO/VirtIOBlockController.cpp
    Devices/Storage/VirtIO/VirtIOBlockDevice.cpp
    Devices/Storage/StorageController.cpp
    Devices/Storage/StorageDevice.cpp
    Devices/Storage/StorageManagement.cpp
    Devices/Storage/StorageDevicePartition.cpp
    FileSystem/AnonymousFile.cpp
    FileSystem/BlockBasedFileSystem.cpp
    FileSystem/Custody.cpp
    FileSystem/CustodyBase.cpp
    FileSystem/DevLoopFS/FileSystem.cpp
    FileSystem/DevLoopFS/Inode.cpp
    FileSystem/DevPtsFS/FileSystem.cpp
    FileSystem/DevPtsFS/Inode.cpp
    FileSystem/Ext2FS/BlockView.cpp
    FileSystem/Ext2FS/FileSystem.cpp
    FileSystem/Ext2FS/Inode.cpp
    FileSystem/FATFS/FileSystem.cpp
    FileSystem/FATFS/Inode.cpp
    FileSystem/FATFS/SFNUtilities.cpp
    FileSystem/FIFO.cpp
    FileSystem/File.cpp
    FileSystem/FileBackedFileSystem.cpp
    FileSystem/FileSystem.cpp
    FileSystem/FileSystemSpecificOption.cpp
    FileSystem/FUSE/FileSystem.cpp
    FileSystem/FUSE/Inode.cpp
    FileSystem/Inode.cpp
    FileSystem/InodeFile.cpp
    FileSystem/InodeMetadata.cpp
    FileSystem/InodeWatcher.cpp
    FileSystem/ISO9660FS/DirectoryIterator.cpp
    FileSystem/ISO9660FS/FileSystem.cpp
    FileSystem/ISO9660FS/Inode.cpp
    FileSystem/Mount.cpp
    FileSystem/MountFile.cpp
    FileSystem/OpenFileDescription.cpp
    FileSystem/Plan9FS/FileSystem.cpp
    FileSystem/Plan9FS/Inode.cpp
    FileSystem/Plan9FS/Message.cpp
    FileSystem/ProcFS/FileSystem.cpp
    FileSystem/ProcFS/Inode.cpp
    FileSystem/ProcFS/ProcessExposed.cpp
    FileSystem/RAMFS/FileSystem.cpp
    FileSystem/RAMFS/Inode.cpp
    FileSystem/SysFS/Component.cpp
    FileSystem/SysFS/DirectoryInode.cpp
    FileSystem/SysFS/FileSystem.cpp
    FileSystem/SysFS/Inode.cpp
    FileSystem/SysFS/LinkInode.cpp
    FileSystem/SysFS/Registry.cpp
    FileSystem/SysFS/RootDirectory.cpp
    FileSystem/SysFS/Subsystems/Bus/PCI/BusDirectory.cpp
    FileSystem/SysFS/Subsystems/Bus/PCI/DeviceAttribute.cpp
    FileSystem/SysFS/Subsystems/Bus/PCI/DeviceDirectory.cpp
    FileSystem/SysFS/Subsystems/Bus/PCI/DeviceExpansionROM.cpp
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
    FileSystem/SysFS/Subsystems/Firmware/Directory.cpp
    FileSystem/SysFS/Subsystems/Kernel/Interrupts.cpp
    FileSystem/SysFS/Subsystems/Kernel/Processes.cpp
    FileSystem/SysFS/Subsystems/Kernel/DeviceMajorNumberAllocations.cpp
    FileSystem/SysFS/Subsystems/Kernel/CPUInfo.cpp
    FileSystem/SysFS/Subsystems/Kernel/ConstantInformation.cpp
    FileSystem/SysFS/Subsystems/Kernel/Keymap.cpp
    FileSystem/SysFS/Subsystems/Kernel/Profile.cpp
    FileSystem/SysFS/Subsystems/Kernel/Directory.cpp
    FileSystem/SysFS/Subsystems/Kernel/DiskUsage.cpp
    FileSystem/SysFS/Subsystems/Kernel/Log.cpp
    FileSystem/SysFS/Subsystems/Kernel/RequestPanic.cpp
    FileSystem/SysFS/Subsystems/Kernel/SystemStatistics.cpp
    FileSystem/SysFS/Subsystems/Kernel/GlobalInformation.cpp
    FileSystem/SysFS/Subsystems/Kernel/MemoryStatus.cpp
    FileSystem/SysFS/Subsystems/Kernel/PowerStateSwitch.cpp
    FileSystem/SysFS/Subsystems/Kernel/Uptime.cpp
    FileSystem/SysFS/Subsystems/Kernel/Network/Adapters.cpp
    FileSystem/SysFS/Subsystems/Kernel/Network/ARP.cpp
    FileSystem/SysFS/Subsystems/Kernel/Network/Directory.cpp
    FileSystem/SysFS/Subsystems/Kernel/Network/Local.cpp
    FileSystem/SysFS/Subsystems/Kernel/Network/Route.cpp
    FileSystem/SysFS/Subsystems/Kernel/Network/TCP.cpp
    FileSystem/SysFS/Subsystems/Kernel/Network/UDP.cpp
    FileSystem/SysFS/Subsystems/Kernel/Configuration/BooleanVariable.cpp
    FileSystem/SysFS/Subsystems/Kernel/Configuration/CapsLockRemap.cpp
    FileSystem/SysFS/Subsystems/Kernel/Configuration/CoredumpDirectory.cpp
    FileSystem/SysFS/Subsystems/Kernel/Configuration/Directory.cpp
    FileSystem/SysFS/Subsystems/Kernel/Configuration/DumpKmallocStack.cpp
    FileSystem/SysFS/Subsystems/Kernel/Configuration/StringVariable.cpp
    FileSystem/SysFS/Subsystems/Kernel/Configuration/UBSANDeadly.cpp
    FileSystem/VFSRootContext.cpp
    FileSystem/VirtualFileSystem.cpp
    Firmware/ACPI/Initialize.cpp
    Firmware/ACPI/Parser.cpp
    Firmware/ACPI/StaticParsing.cpp
    Firmware/DeviceTree/Device.cpp
    Firmware/DeviceTree/DeviceTree.cpp
    Firmware/DeviceTree/Management.cpp
    Firmware/DeviceTree/PlatformInit.cpp
    Interrupts/GenericInterruptHandler.cpp
    Interrupts/IRQHandler.cpp
    Interrupts/PCIIRQHandler.cpp
    Interrupts/SharedIRQHandler.cpp
    Interrupts/UnhandledInterruptHandler.cpp
    KSyms.cpp
    Memory/AddressSpace.cpp
    Memory/AnonymousVMObject.cpp
    Memory/InodeVMObject.cpp
    Memory/MemoryManager.cpp
    Memory/MMIOVMObject.cpp
    Memory/PhysicalRAMPage.cpp
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
    Locking/LockRank.cpp
    Locking/Mutex.cpp
    Library/Assertions.cpp
    Library/DoubleBuffer.cpp
    Library/IOWindow.cpp
    Library/MiniStdLib.cpp
    Library/Panic.cpp
    Library/ScopedCritical.cpp
    Library/StdLib.cpp
    Library/KBufferBuilder.cpp
    Library/KLexicalPath.cpp
    Library/KString.cpp
    Library/UserOrKernelBuffer.cpp
    Net/Intel/E1000ENetworkAdapter.cpp
    Net/Intel/E1000NetworkAdapter.cpp
    Net/Realtek/RTL8168NetworkAdapter.cpp
    Net/VirtIO/VirtIONetworkAdapter.cpp
    Net/IP/Socket.cpp
    Net/LocalSocket.cpp
    Net/LoopbackAdapter.cpp
    Net/NetworkAdapter.cpp
    Net/NetworkTask.cpp
    Net/NetworkingManagement.cpp
    Net/Routing.cpp
    Net/Socket.cpp
    Net/TCPSocket.cpp
    Net/UDPSocket.cpp
    Security/Random/VirtIO/RNG.cpp
    Security/AddressSanitizer.cpp
    Security/Credentials.cpp
    Security/Random.cpp
    Security/UBSanitizer.cpp
    Syscalls/anon_create.cpp
    Syscalls/alarm.cpp
    Syscalls/chdir.cpp
    Syscalls/chmod.cpp
    Syscalls/chown.cpp
    Syscalls/clock.cpp
    Syscalls/debug.cpp
    Syscalls/disown.cpp
    Syscalls/dup2.cpp
    Syscalls/execve.cpp
    Syscalls/exit.cpp
    Syscalls/faccessat.cpp
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
    Syscalls/profiled_syscalls.cpp
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
    Syscalls/SyscallHandler.cpp
    Syscalls/sysconf.cpp
    Syscalls/thread.cpp
    Syscalls/times.cpp
    Syscalls/umask.cpp
    Syscalls/uname.cpp
    Syscalls/unlink.cpp
    Syscalls/unshare.cpp
    Syscalls/unveil.cpp
    Syscalls/utime.cpp
    Syscalls/utimensat.cpp
    Syscalls/waitid.cpp
    Syscalls/inode_watcher.cpp
    Syscalls/write.cpp
    Devices/TTY/MasterPTY.cpp
    Devices/TTY/PTYMultiplexer.cpp
    Devices/TTY/SlavePTY.cpp
    Devices/TTY/TTY.cpp
    Devices/TTY/VirtualConsole.cpp
    Tasks/Coredump.cpp
    Tasks/CrashHandler.cpp
    Tasks/FinalizerTask.cpp
    Tasks/FutexQueue.cpp
    Tasks/HostnameContext.cpp
    Tasks/PerformanceEventBuffer.cpp
    Tasks/PowerStateSwitchTask.cpp
    Tasks/Process.cpp
    Tasks/ProcessGroup.cpp
    Tasks/ScopedProcessList.cpp
    Tasks/Scheduler.cpp
    Tasks/SyncTask.cpp
    Tasks/Thread.cpp
    Tasks/ThreadBlockers.cpp
    Tasks/ThreadTracer.cpp
    Tasks/WaitQueue.cpp
    Tasks/WorkQueue.cpp
    Time/TimeManagement.cpp
    Time/TimerQueue.cpp
)

if ("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(KERNEL_SOURCES
        ${KERNEL_SOURCES}

        Arch/x86_64/archctl.cpp

        Arch/x86_64/CMOS.cpp
        Arch/x86_64/DebugOutput.cpp
        Arch/x86_64/Delay.cpp

        Arch/x86_64/Firmware/ACPI.cpp
        Arch/x86_64/Firmware/PCBIOS/Mapper.cpp
        Arch/x86_64/Firmware/PCBIOS/SysFSComponent.cpp
        Arch/x86_64/Firmware/PCBIOS/SysFSDirectory.cpp

        Arch/x86_64/Hypervisor/BochsDisplayConnector.cpp
        Arch/x86_64/Hypervisor/VMWareBackdoor.cpp

        Arch/x86_64/CurrentTime.cpp

        Arch/x86_64/I8042Reboot.cpp
        Arch/x86_64/Interrupts/APIC.cpp
        Arch/x86_64/Interrupts/IOAPIC.cpp
        Arch/x86_64/Interrupts/PIC.cpp
        Arch/x86_64/Time/APICTimer.cpp
        Arch/x86_64/Time/HPET.cpp
        Arch/x86_64/Time/HPETComparator.cpp
        Arch/x86_64/Time/PIT.cpp
        Arch/x86_64/Time/RTC.cpp
        Arch/x86_64/PCSpeaker.cpp

        Arch/x86_64/ISABus/I8042Controller.cpp
        Arch/x86_64/ISABus/Input/VMWareMouseDevice.cpp
        Arch/x86_64/ISABus/SerialDevice.cpp
        Arch/x86_64/PCI/Controller/PIIX4HostBridge.cpp
        Arch/x86_64/PCI/Initializer.cpp
        Arch/x86_64/PCI/MSI.cpp

        Arch/x86_64/PowerState.cpp
        Arch/x86_64/RTC.cpp
        Arch/x86_64/Shutdown.cpp
        Arch/x86_64/SmapDisabler.cpp

        # TODO: Share these with the aarch64 build
        Interrupts/SpuriousInterruptHandler.cpp
        kprintf.cpp        
    )

    set(KERNEL_SOURCES
        ${KERNEL_SOURCES}
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86_64/Boot/ap_setup.S
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86_64/InterruptEntry.cpp
    )

    set(KERNEL_SOURCES
        ${KERNEL_SOURCES}
        
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86_64/ASM_wrapper.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86_64/CPUID.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86_64/InterruptManagement.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86_64/Interrupts.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86_64/PageDirectory.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86_64/Processor.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86_64/ProcessorInfo.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86_64/SafeMem.cpp
    )

    if("${SERENITY_ARCH}" STREQUAL "x86_64")
        set(KERNEL_SOURCES
            ${KERNEL_SOURCES}
            ${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86_64/SyscallEntry.cpp
        )
    endif()
elseif("${SERENITY_ARCH}" STREQUAL "aarch64")
    set(RPI_SOURCES
        Arch/aarch64/RPi/AUXPeripherals.cpp
        Arch/aarch64/RPi/Framebuffer.cpp
        Arch/aarch64/RPi/GPIO.cpp
        Arch/aarch64/RPi/InterruptController.cpp
        Arch/aarch64/RPi/Mailbox.cpp
        Arch/aarch64/RPi/MiniUART.cpp
        Arch/aarch64/RPi/SDHostController.cpp
        Arch/aarch64/RPi/Timer.cpp
        Arch/aarch64/RPi/Watchdog.cpp
    )
    set(SOURCES_RUNNING_WITHOUT_MMU
        Arch/aarch64/Exceptions.cpp
        Arch/aarch64/MMU.cpp
        Arch/aarch64/pre_init.cpp
        Arch/aarch64/RPi/MMIO.cpp
        )
    set(KERNEL_SOURCES
        ${KERNEL_SOURCES}
        ${RPI_SOURCES}
        ${SOURCES_RUNNING_WITHOUT_MMU}

        kprintf.cpp

        Arch/aarch64/archctl.cpp
        Arch/aarch64/boot.S
        Arch/aarch64/CPUID.cpp
        Arch/aarch64/CurrentTime.cpp
        Arch/aarch64/DebugOutput.cpp
        Arch/aarch64/Delay.cpp
        Arch/aarch64/FPUState.S
        Arch/aarch64/InterruptManagement.cpp
        Arch/aarch64/Interrupts.cpp
        Arch/aarch64/Interrupts/GIC.cpp
        Arch/aarch64/MainIdRegister.cpp
        Arch/aarch64/PSCI.cpp
        Arch/aarch64/PageDirectory.cpp
        Arch/aarch64/Processor.cpp
        Arch/aarch64/PowerState.cpp
        Arch/aarch64/SafeMem.cpp
        Arch/aarch64/SmapDisabler.cpp
        Arch/aarch64/Time/ARMv8Timer.cpp
        Arch/aarch64/Time/PL031.cpp
        Arch/aarch64/vector_table.S

        Arch/aarch64/PlatformInit/RaspberryPi.cpp
        Arch/aarch64/PlatformInit/Virt.cpp

        Arch/aarch64/Serial/PL011.cpp

        Bus/PCI/DeviceTreeInitializer.cpp
    )

    # Otherwise linker errors e.g undefined reference to `__aarch64_cas8_acq_rel'
    add_compile_options(-mno-outline-atomics)

    # NOTE: These files cannot use a stack protector and sanitizers, as these will cause accesses to global variables to be inserted
    #       by the compiler. The CPU cannot access global variables without the MMU as the kernel is linked for a virtual address in high memory.
    set_source_files_properties(${SOURCES_RUNNING_WITHOUT_MMU} PROPERTIES COMPILE_FLAGS "-fno-stack-protector -fno-sanitize=all")
elseif("${SERENITY_ARCH}" STREQUAL "riscv64")
    set(SOURCES_RUNNING_WITHOUT_MMU
        Arch/riscv64/MMU.cpp
        Arch/riscv64/pre_init.cpp

        # FIXME: Don't disable stack protectors and sanitizers in SBI.cpp.
        #        Maybe implement SBI debug console printing directly in pre_init.cpp as well?
        Arch/riscv64/SBI.cpp
    )

    set(KERNEL_SOURCES
        ${KERNEL_SOURCES}
        ${SOURCES_RUNNING_WITHOUT_MMU}

        Arch/Processor.cpp
        kprintf.cpp

        Arch/riscv64/archctl.cpp
        Arch/riscv64/boot.S
        Arch/riscv64/CurrentTime.cpp
        Arch/riscv64/DebugOutput.cpp
        Arch/riscv64/Delay.cpp
        Arch/riscv64/InterruptManagement.cpp
        Arch/riscv64/Interrupts.cpp
        Arch/riscv64/Interrupts/PLIC.cpp
        Arch/riscv64/MMU.cpp
        Arch/riscv64/PageDirectory.cpp
        Arch/riscv64/PowerState.cpp
        Arch/riscv64/pre_init.cpp
        Arch/riscv64/Processor.cpp
        Arch/riscv64/SafeMem.cpp
        Arch/riscv64/SBI.cpp
        Arch/riscv64/SmapDisabler.cpp
        Arch/riscv64/Timer.cpp
        Arch/riscv64/trap_handler.S

        Bus/PCI/DeviceTreeInitializer.cpp
    )

    # NOTE: These files cannot use a stack protector and sanitizers, as these will cause accesses to global variables to be inserted
    #       by the compiler. The CPU cannot access global variables without the MMU as the kernel is linked for a virtual address in high memory.
    #       On GCC, also prevent loops from being optimized into functions like memset, as those might use stack protectors and sanitizers.
    #       We also have to disable our -ftrivial-auto-var-init=pattern mitigation, as that would cause memsets to be inserted.
    if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
        set_source_files_properties(${SOURCES_RUNNING_WITHOUT_MMU} PROPERTIES COMPILE_FLAGS "-fno-stack-protector -fno-sanitize=all -ftrivial-auto-var-init=uninitialized -fno-tree-loop-distribution -fno-tree-loop-distribute-patterns")
    else()
        set_source_files_properties(${SOURCES_RUNNING_WITHOUT_MMU} PROPERTIES COMPILE_FLAGS "-fno-stack-protector -fno-sanitize=all -ftrivial-auto-var-init=uninitialized")
    endif()
endif()

set(AK_SOURCES
    ../AK/DOSPackedTime.cpp
    ../AK/GenericLexer.cpp
    ../AK/Hex.cpp
    ../AK/MemoryStream.cpp
    ../AK/SipHash.cpp
    ../AK/Stream.cpp
    ../AK/StringBuilder.cpp
    ../AK/StringUtils.cpp
    ../AK/StringView.cpp
    ../AK/Time.cpp
    ../AK/Error.cpp
    ../AK/Format.cpp
    ../AK/UUID.cpp
)

set(DT_SOURCES
    ../Userland/Libraries/LibDeviceTree/DeviceTree.cpp
    ../Userland/Libraries/LibDeviceTree/FlattenedDeviceTree.cpp
    ../Userland/Libraries/LibDeviceTree/Validation.cpp
)

set(EDID_SOURCES
    ../Userland/Libraries/LibEDID/DMT.cpp
    ../Userland/Libraries/LibEDID/EDID.cpp
    ../Userland/Libraries/LibEDID/VIC.cpp
)

set(ELF_SOURCES
    ../Userland/Libraries/LibELF/Image.cpp
    ../Userland/Libraries/LibELF/Relocation.cpp
    ../Userland/Libraries/LibELF/Validation.cpp
)

set(HID_SOURCES
    ../Userland/Libraries/LibHID/ReportDescriptorParser.cpp
    ../Userland/Libraries/LibHID/ReportParser.cpp
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
    ../Userland/Libraries/LibPartition/PartitionableDevice.cpp
    ../Userland/Libraries/LibPartition/PartitionTable.cpp
)

add_compile_options(-fsigned-char)
add_compile_options(-Wno-unknown-warning-option -Wvla -Wnull-dereference)
add_compile_options(-fno-rtti -ffreestanding -fbuiltin)

# We use __builtin_offsetof() on Processor, which inherits from ProcessorBase and is therefore a non-standard-layout type.
# This is an issue on non-Itanium ABIs (and in irrelevant edge cases), so we can safely ignore it.
add_compile_options(-Wno-invalid-offsetof)

if ("${SERENITY_ARCH}" STREQUAL "x86_64")
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
    set_source_files_properties(Library/MiniStdLib.cpp
        PROPERTIES COMPILE_FLAGS "-fno-tree-loop-distribution -fno-tree-loop-distribute-patterns")

    # Prevent volatile bitfield accesses from being optimized into smaller load/stores
    set_source_files_properties(Bus/USB/xHCI/xHCIController.cpp PROPERTIES COMPILE_FLAGS "-fstrict-volatile-bitfields")

    if ("${SERENITY_ARCH}" STREQUAL "x86_64")
        # The BFD linker only supports RELR relocations on x86 and POWER.
        add_link_options(LINKER:-z,pack-relative-relocs)
    endif()
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
    add_compile_options(-mno-red-zone)
    set_new_alignment(8)
endif()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -static-pie")

# Kernel Coverage (KCOV) is an API to collect and expose program counters of
# kernel code that has been run to user space. It's rather slow and likely not
# secure to run in production builds. Useful for coverage guided fuzzing.
if (ENABLE_KERNEL_COVERAGE_COLLECTION)
    add_compile_definitions(ENABLE_KERNEL_COVERAGE_COLLECTION)
    if(ENABLE_KERNEL_COVERAGE_COLLECTION_DEBUG)
        add_compile_definitions(ENABLE_KERNEL_COVERAGE_COLLECTION_DEBUG)
    endif()

    set(KERNEL_SOURCES
        ${KERNEL_SOURCES}
        Devices/KCOVDevice.cpp
        Devices/KCOVInstance.cpp
        SanCov.cpp
    )

    add_compile_options(-fsanitize-coverage=trace-pc)
    # To avoid __sanitizer_cov_trace_pc from causing an infinite recursion,
    # don't instrument any even transitively used code. You can either skip
    # individual function via the NO_SANITIZE_COVERAGE macro or by excluding
    # the whole file by adding it to KCOV_EXCLUDED_SOURCES.
    # Note that you can only add .cpp and not .h files to KCOV_EXCLUDED_SOURCES.
    set(KCOV_EXCLUDED_SOURCES
        # Skip core KCOV code.
        ../Kernel/Devices/KCOVDevice.cpp
        ../Kernel/Devices/KCOVInstance.cpp
        ../Kernel/SanCov.cpp
        # GCC assumes that the caller saves registers for functions according
        # to the System V ABI and happily inserts coverage calls into the
        # function prologue for all functions. This assumption is not true for
        # interrupt handlers because their calling convention is not compatible
        # with the System V ABI.
        ../Kernel/Arch/x86_64/Interrupts.cpp
        ../Kernel/Syscall/SyscallHandler.cpp
    )
    set_source_files_properties(${KCOV_EXCLUDED_SOURCES} PROPERTIES COMPILE_FLAGS "-fno-sanitize-coverage=trace-pc")
endif()

if (ENABLE_USERSPACE_COVERAGE_COLLECTION)
    # Disable checking open() pledges and the veil for coverage data when building userspace with coverage
    # so that binaries can write out coverage data even with pledges/veil
    add_compile_definitions(SKIP_PATH_VALIDATION_FOR_COVERAGE_INSTRUMENTATION)
endif()

if (ENABLE_KERNEL_UNDEFINED_SANITIZER)
    # Kernel Undefined Behavior Sanitizer (KUBSAN)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fsanitize=undefined")
endif()

if (ENABLE_KERNEL_ADDRESS_SANITIZER)
    add_compile_options(-fsanitize=kernel-address)
    if(CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
        # TODO: Support inline KASAN for improved performance
        add_compile_options("SHELL:-mllvm -asan-instrumentation-with-call-threshold=0")
        # TODO: Support KASAN stack poisoning (inline) for use-after-return and use-after-scope detection
        add_compile_options("SHELL:-mllvm -asan-stack=0")
    endif()
    set_source_files_properties(Security/AddressSanitizer.cpp PROPERTIES COMPILE_FLAGS "-fno-sanitize=kernel-address")
    add_link_options(-fsanitize=kernel-address)
endif()

if ("${SERENITY_ARCH}" STREQUAL "aarch64")
    add_compile_options(-fno-threadsafe-statics)

    # Unaligned memory access will cause a trap, so to make sure the compiler doesn't generate
    # those unaligned accesses, this flag is added.
    add_compile_options(-mstrict-align -Wno-cast-align)
endif()

set(SOURCES
    ${KERNEL_SOURCES}
    ${GENERATED_SOURCES}
    ${AK_SOURCES}
    ${DT_SOURCES}
    ${EDID_SOURCES}
    ${ELF_SOURCES}
    ${HID_SOURCES}
    ${VT_SOURCES}
    ${CRYPTO_SOURCES}
    ${PARTITION_SOURCES}
)

add_compile_definitions(KERNEL)

add_library(kernel_heap STATIC ${KERNEL_HEAP_SOURCES})
target_link_libraries(kernel_heap PUBLIC GenericClangPlugin)

add_executable(Kernel ${SOURCES})
add_dependencies(Kernel generate_EscapeSequenceStateMachine.h generate_version_header)
target_link_libraries(Kernel PUBLIC GenericClangPlugin)

if("${SERENITY_ARCH}" STREQUAL "aarch64")
    target_link_options(Kernel PRIVATE LINKER:-T ${CMAKE_CURRENT_SOURCE_DIR}/Arch/aarch64/linker.ld -nostdlib LINKER:--no-pie)
    set_target_properties(Kernel PROPERTIES LINK_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/Arch/aarch64/linker.ld)
elseif("${SERENITY_ARCH}" STREQUAL "riscv64")
    add_custom_command(
        OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/linker.ld
        COMMAND "${CMAKE_CXX_COMPILER}" ${TARGET_STRING} -E -P -x c -I${CMAKE_CURRENT_SOURCE_DIR}/.. "${CMAKE_CURRENT_SOURCE_DIR}/Arch/riscv64/linker.ld" -o "${CMAKE_CURRENT_BINARY_DIR}/linker.ld"
        MAIN_DEPENDENCY "Arch/riscv64/linker.ld"
        COMMENT "Preprocessing linker.ld"
        VERBATIM
    )
    add_custom_target(generate_kernel_linker_script DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/linker.ld)

    # The final kernel binary for some reason includes temporary local symbols on riscv64 clang, which causes kernel.map to be too big to fit in its section in the kernel.
    # Explicitly pass -X to the linker to remove them.
    target_link_options(Kernel PRIVATE LINKER:-T ${CMAKE_CURRENT_BINARY_DIR}/linker.ld -nostdlib LINKER:-X)

    set_target_properties(Kernel PROPERTIES LINK_DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/linker.ld")
elseif ("${SERENITY_ARCH}" STREQUAL "x86_64")
    add_custom_command(
        OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/linker.ld
        COMMAND "${CMAKE_CXX_COMPILER}" ${TARGET_STRING} -E -P -x c -I${CMAKE_CURRENT_SOURCE_DIR}/.. "${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86_64/linker.ld" -o "${CMAKE_CURRENT_BINARY_DIR}/linker.ld"
        MAIN_DEPENDENCY "Arch/x86_64/linker.ld"
        COMMENT "Preprocessing linker.ld"
        VERBATIM
    )
    add_custom_target(generate_kernel_linker_script DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/linker.ld)
    target_link_options(Kernel PRIVATE LINKER:-T ${CMAKE_CURRENT_BINARY_DIR}/linker.ld -nostdlib -nodefaultlibs)
    set_target_properties(Kernel PROPERTIES LINK_DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/linker.ld")

    if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
        target_compile_options(Kernel PRIVATE -mpreferred-stack-boundary=3)
    elseif(CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
        target_compile_options(Kernel PRIVATE -mstack-alignment=8)
    endif()
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

if ("${SERENITY_ARCH}" STREQUAL "aarch64")
    set(KERNEL_ELF_OBJCOPY_TARGET "elf64-littleaarch64")
elseif ("${SERENITY_ARCH}" STREQUAL "riscv64")
    set(KERNEL_ELF_OBJCOPY_TARGET "elf64-littleriscv")
elseif ("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(KERNEL_ELF_OBJCOPY_TARGET "elf64-x86-64")
endif()

# The Kernel is linked to the Pre-kernel binary.
add_custom_command(
    TARGET Kernel POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E env NM=${CMAKE_NM} sh ${CMAKE_CURRENT_SOURCE_DIR}/mkmap.sh
    COMMAND ${CMAKE_COMMAND} -E env OBJCOPY=${CMAKE_OBJCOPY} sh ${CMAKE_CURRENT_SOURCE_DIR}/embedmap.sh
    COMMAND ${CMAKE_OBJCOPY} --only-keep-debug Kernel Kernel.debug
    COMMAND ${CMAKE_OBJCOPY} --strip-debug Kernel
    COMMAND ${CMAKE_OBJCOPY} --add-gnu-debuglink=Kernel.debug Kernel
    COMMAND ${CMAKE_OBJCOPY} --set-section-flags .heap=load Kernel Kernel_shared_object
    COMMAND ${CMAKE_OBJCOPY} -I binary -O ${KERNEL_ELF_OBJCOPY_TARGET} --rename-section .data=.Kernel_image --set-section-alignment .Kernel_image=4096 Kernel_shared_object Kernel.o
    BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/Kernel_shared_object ${CMAKE_CURRENT_BINARY_DIR}/Kernel.o ${CMAKE_CURRENT_BINARY_DIR}/kernel.map
)

# Architectures (x86_64, aarch64, riscv64) share the same location in their respective architecture build folder.
# The Kernel is linked to the Pre-kernel binary and then generates the result Kernel binary.
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Kernel" DESTINATION boot)

install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Kernel.debug" DESTINATION boot)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/kernel.map" DESTINATION res)

if (("${SERENITY_ARCH}" STREQUAL "aarch64") OR ("${SERENITY_ARCH}" STREQUAL "riscv64"))
    add_custom_command(
        TARGET Kernel POST_BUILD
        COMMAND ${CMAKE_OBJCOPY} -O binary Kernel Kernel.bin
        BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/Kernel.bin
    )
    install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Kernel.bin" DESTINATION boot)
endif()

serenity_install_headers(Kernel)
serenity_install_sources(Kernel)

# Only x86 uses the Multiboot Prekernel
if ("${SERENITY_ARCH}" STREQUAL "x86_64")
    add_subdirectory(Prekernel)
endif()

add_subdirectory(EFIPrekernel)
