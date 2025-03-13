/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <AK/SetOnce.h>
#include <AK/Types.h>
#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/InterruptManagement.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Boot/BootInfo.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Boot/Multiboot.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/Bus/USB/Drivers/USBDriver.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/Detect.h>
#include <Kernel/Devices/Audio/Management.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/FUSEDevice.h>
#include <Kernel/Devices/GPU/Console/BootDummyConsole.h>
#include <Kernel/Devices/GPU/Console/BootFramebufferConsole.h>
#include <Kernel/Devices/GPU/Management.h>
#include <Kernel/Devices/Generic/DeviceControlDevice.h>
#include <Kernel/Devices/Generic/FullDevice.h>
#include <Kernel/Devices/Generic/MemoryDevice.h>
#include <Kernel/Devices/Generic/NullDevice.h>
#include <Kernel/Devices/Generic/PCSpeakerDevice.h>
#include <Kernel/Devices/Generic/RandomDevice.h>
#include <Kernel/Devices/Generic/SelfTTYDevice.h>
#include <Kernel/Devices/Generic/ZeroDevice.h>
#include <Kernel/Devices/Input/Management.h>
#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
#    include <Kernel/Devices/KCOVDevice.h>
#endif
#include <Kernel/Devices/PCISerialDevice.h>
#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Devices/TTY/PTYMultiplexer.h>
#include <Kernel/Devices/TTY/VirtualConsole.h>
#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/Directory.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Firmware/ACPI/Initialize.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/KSyms.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Net/NetworkTask.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/FinalizerTask.h>
#include <Kernel/Tasks/HostnameContext.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/SyncTask.h>
#include <Kernel/Tasks/WorkQueue.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/kstdio.h>

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Hypervisor/VMWareBackdoor.h>
#    include <Kernel/Arch/x86_64/Interrupts/APIC.h>
#    include <Kernel/Arch/x86_64/Interrupts/PIC.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/RPi/Framebuffer.h>
#    include <Kernel/Arch/aarch64/RPi/Mailbox.h>
#    include <Kernel/Arch/aarch64/RPi/MiniUART.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/Delay.h>
#endif

#if ARCH(AARCH64) || ARCH(RISCV64)
#    include <Kernel/Firmware/DeviceTree/Management.h>
#    include <Kernel/Firmware/DeviceTree/PlatformInit.h>
#endif

// Defined in the linker script
typedef void (*ctor_func_t)();
extern ctor_func_t start_heap_ctors[];
extern ctor_func_t end_heap_ctors[];
extern ctor_func_t start_ctors[];
extern ctor_func_t end_ctors[];

extern uintptr_t __stack_chk_guard;
READONLY_AFTER_INIT uintptr_t __stack_chk_guard __attribute__((used));

extern "C" u8 start_of_safemem_text[];
extern "C" u8 end_of_safemem_text[];
extern "C" u8 start_of_safemem_atomic_text[];
extern "C" u8 end_of_safemem_atomic_text[];

extern "C" USB::DriverInitFunction driver_init_table_start[];
extern "C" USB::DriverInitFunction driver_init_table_end[];

extern "C" u8 end_of_kernel_image[];

READONLY_AFTER_INIT SetOnce g_not_in_early_boot;

namespace Kernel {

[[noreturn]] static void init_stage2(void*);
static void setup_serial_debug();

// boot.S expects these functions to exactly have the following signatures.
// We declare them here to ensure their signatures don't accidentally change.
extern "C" void init_finished(u32 cpu) __attribute__((used));
extern "C" [[noreturn]] void init_ap(FlatPtr cpu, Processor* processor_info);
extern "C" [[noreturn]] void init(BootInfo const&);

READONLY_AFTER_INIT VirtualConsole* tty0;

ProcessID g_init_pid { 0 };

ALWAYS_INLINE static Processor& bsp_processor()
{
    // This solves a problem where the bsp Processor instance
    // gets "re"-initialized in init() when we run all global constructors.
    alignas(Processor) static u8 bsp_processor_storage[sizeof(Processor)];
    return (Processor&)bsp_processor_storage;
}

// SerenityOS Kernel C++ entry point :^)
//
// This is where C++ execution begins, after boot.S transfers control here.
//
// The purpose of init() is to start multi-tasking. It does the bare minimum
// amount of work needed to start the scheduler.
//
// Once multi-tasking is ready, we spawn a new thread that starts in the
// init_stage2() function. Initialization continues there.

Atomic<Graphics::Console*> g_boot_console;

READONLY_AFTER_INIT static StringView s_kernel_cmdline;

READONLY_AFTER_INIT constinit BootInfo g_boot_info;

extern "C" [[noreturn]] UNMAP_AFTER_INIT NO_SANITIZE_COVERAGE void init(BootInfo const& boot_info)
{
#if ARCH(X86_64)
    g_boot_info = boot_info;
    s_kernel_cmdline = boot_info.cmdline;
#elif ARCH(AARCH64) || ARCH(RISCV64)
    if (boot_info.boot_method == BootMethod::EFI) {
        g_boot_info = boot_info;
        s_kernel_cmdline = boot_info.cmdline;
    } else {
        if (!DeviceTree::verify_fdt())
            // We are too early in the boot process to print anything, so just hang if the FDT is invalid.
            Processor::halt();

        auto maybe_command_line = DeviceTree::get_command_line_from_fdt();
        if (maybe_command_line.is_error())
            s_kernel_cmdline = "serial_debug"sv;
        else
            s_kernel_cmdline = maybe_command_line.value();
    }
#endif

    setup_serial_debug();

    // We need to copy the command line before kmalloc is initialized,
    // as it may overwrite parts of multiboot!
    CommandLine::early_initialize(s_kernel_cmdline);

    new (&bsp_processor()) Processor();
    bsp_processor().early_initialize(0);

    // Invoke the constructors needed for the kernel heap
    for (ctor_func_t* ctor = start_heap_ctors; ctor < end_heap_ctors; ctor++)
        (*ctor)();
    kmalloc_init();

    load_kernel_symbol_table();

    bsp_processor().initialize(0);

    CommandLine::initialize();
    Memory::MemoryManager::initialize(0);

#if ARCH(AARCH64) || ARCH(RISCV64)
    DeviceTree::map_flattened_devicetree();
    DeviceTree::run_platform_init();
#endif

    // NOTE: If the bootloader provided a framebuffer, then set up an initial console.
    // If the bootloader didn't provide a framebuffer, then set up an initial text console.
    // We do so we can see the output on the screen as soon as possible.
    if (!kernel_command_line().is_early_boot_console_disabled()) {
        if (!g_boot_info.boot_framebuffer.paddr.is_null() && g_boot_info.boot_framebuffer.type == BootFramebufferType::BGRx8888) {
            g_boot_console = &try_make_lock_ref_counted<Graphics::BootFramebufferConsole>(g_boot_info.boot_framebuffer.paddr, g_boot_info.boot_framebuffer.width, g_boot_info.boot_framebuffer.height, g_boot_info.boot_framebuffer.pitch).value().leak_ref();
        } else {
            dbgln("No early framebuffer console available, initializing dummy console");
            g_boot_console = &try_make_lock_ref_counted<Graphics::BootDummyConsole>().value().leak_ref();
        }
    }
    dmesgln("Starting SerenityOS...");

    dmesgln("Boot method: {}", boot_info.boot_method);

    MM.unmap_prekernel();

    // Ensure that the safemem sections are not empty. This could happen if the linker accidentally discards the sections.
    VERIFY(+start_of_safemem_text != +end_of_safemem_text);
    VERIFY(+start_of_safemem_atomic_text != +end_of_safemem_atomic_text);

    // Invoke all static global constructors in the kernel.
    // Note that we want to do this as early as possible.
    for (ctor_func_t* ctor = start_ctors; ctor < end_ctors; ctor++)
        (*ctor)();

    for (auto* init_function = driver_init_table_start; init_function != driver_init_table_end; init_function++)
        (*init_function)();

#if ARCH(AARCH64) || ARCH(RISCV64)
    MUST(DeviceTree::unflatten_fdt());

    if (kernel_command_line().contains("dump_fdt"sv))
        DeviceTree::dump_fdt();

    DeviceTree::Management::initialize();
#endif

#if ARCH(RISCV64)
    init_delay_loop();
#endif

    InterruptManagement::initialize();
    ACPI::initialize();

    // Initialize TimeManagement before using randomness!
    TimeManagement::initialize(0);

    SysFSComponentRegistry::initialize();

    Device::initialize_base_devices();

    __stack_chk_guard = get_fast_random<uintptr_t>();

    // NOTE: Initialize the empty VFS root context just before we need to create
    // kernel processes.
    VFSRootContext::initialize_empty_ramfs_root_context_for_kernel_processes();

    Process::initialize();

    Scheduler::initialize();

#if ARCH(X86_64)
    // FIXME: Add an abstraction for the smp related functions, instead of using ifdefs in this file.
    if (APIC::initialized() && APIC::the().enabled_processor_count() > 1) {
        // We must set up the AP boot environment before switching to a kernel process,
        // as pages below address USER_RANGE_BASE are only accessible through the kernel
        // page directory.
        APIC::the().setup_ap_boot_environment();
    }
#endif

    MUST(Process::create_kernel_process("init_stage2"sv, init_stage2, nullptr, THREAD_AFFINITY_DEFAULT, Process::RegisterProcess::No));

    Scheduler::start();
    VERIFY_NOT_REACHED();
}

#if ARCH(X86_64)
//
// This is where C++ execution begins for APs, after boot.S transfers control here.
//
// The purpose of init_ap() is to initialize APs for multi-tasking.
//
extern "C" [[noreturn]] UNMAP_AFTER_INIT void init_ap(FlatPtr cpu, Processor* processor_info)
{
    processor_info->early_initialize(cpu);

    processor_info->initialize(cpu);
    Memory::MemoryManager::initialize(cpu);

    Scheduler::set_idle_thread(APIC::the().get_idle_thread(cpu));

    Scheduler::start();
    VERIFY_NOT_REACHED();
}

//
// This method is called once a CPU enters the scheduler and its idle thread
// At this point the initial boot stack can be freed
//
extern "C" UNMAP_AFTER_INIT void init_finished(u32 cpu)
{
    if (cpu == 0) {
        // TODO: we can reuse the boot stack, maybe for kmalloc()?
    } else {
        APIC::the().init_finished(cpu);
        TimeManagement::initialize(cpu);
    }
}
#endif

void init_stage2(void*)
{
    // This is a little bit of a hack. We can't register our process at the time we're
    // creating it, but we need to be registered otherwise finalization won't be happy.
    // The colonel process gets away without having to do this because it never exits.
    Process::register_new(Process::current());

    WorkQueue::initialize();

#if ARCH(X86_64)
    if (kernel_command_line().is_smp_enabled() && APIC::initialized() && APIC::the().enabled_processor_count() > 1) {
        // We can't start the APs until we have a scheduler up and running.
        // We need to be able to process ICI messages, otherwise another
        // core may send too many and end up deadlocking once the pool is
        // exhausted
        APIC::the().boot_aps();
    }
#endif

    // Initialize the PCI Bus as early as possible, for early boot (PCI based) serial logging
    PCI::initialize();
    if (!PCI::Access::is_disabled()) {
        PCISerialDevice::detect();
    }

#if ARCH(X86_64)
    if (!is_serial_debug_enabled())
        (void)SerialDevice::must_create(0).leak_ref();
    (void)SerialDevice::must_create(1).leak_ref();
    (void)SerialDevice::must_create(2).leak_ref();
    (void)SerialDevice::must_create(3).leak_ref();
#elif ARCH(AARCH64)
    // FIXME: Make MiniUART a DeviceTree::Driver.
    if (DeviceTree::get().is_compatible_with("raspberrypi,3-model-b"sv) || DeviceTree::get().is_compatible_with("raspberrypi,4-model-b"sv))
        (void)MUST(RPi::MiniUART::create()).leak_ref();
#endif

    (void)PCSpeakerDevice::must_create().leak_ref();

#if ARCH(X86_64)
    VMWareBackdoor::the(); // don't wait until first mouse packet
#endif
    MUST(InputManagement::initialize());

    GraphicsManagement::the().initialize();
    VirtualConsole::initialize_consoles();

    SyncTask::spawn();
    FinalizerTask::spawn();

    auto boot_profiling = kernel_command_line().is_boot_profiling_enabled();

    USB::USBManagement::initialize();
    SysFSFirmwareDirectory::initialize();

    if (!PCI::Access::is_disabled()) {
        VirtIO::detect_pci_instances();
    }

    NetworkingManagement::the().initialize();

#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    (void)KCOVDevice::must_create().leak_ref();
#endif
    (void)MemoryDevice::must_create().leak_ref();
    (void)ZeroDevice::must_create().leak_ref();
    (void)FullDevice::must_create().leak_ref();
    (void)FUSEDevice::must_create().leak_ref();
    (void)RandomDevice::must_create().leak_ref();
    (void)SelfTTYDevice::must_create().leak_ref();
    PTYMultiplexer::initialize();

    AudioManagement::the().initialize();

    StorageManagement::the().initialize(kernel_command_line().is_nvme_polling_enabled());
    for (int i = 0; i < 5; ++i) {
        if (StorageManagement::the().determine_boot_device(kernel_command_line().root_device()))
            break;
        dbgln_if(STORAGE_DEVICE_DEBUG, "Boot device {} not found, sleeping 2 seconds", kernel_command_line().root_device());
        (void)Thread::current()->sleep(Duration::from_seconds(2));
    }

    auto first_process_vfs_context_or_error = StorageManagement::the().create_first_vfs_root_context();
    if (first_process_vfs_context_or_error.is_error()) {
        PANIC("StorageManagement::create_first_vfs_root_context failed");
    }

    auto first_process_vfs_context = first_process_vfs_context_or_error.release_value();

    // Switch out of early boot mode.
    g_not_in_early_boot.set();

    // NOTE: Everything marked READONLY_AFTER_INIT becomes non-writable after this point.
    MM.protect_readonly_after_init_memory();

    // NOTE: Everything in the .ksyms section becomes read-only after this point.
    MM.protect_ksyms_after_init();

    auto hostname_context_or_error = HostnameContext::create_initial();
    if (hostname_context_or_error.is_error())
        PANIC("init_stage2: Error creating initial hostname context: {}", hostname_context_or_error.error());
    auto hostname_context = hostname_context_or_error.release_value();

    // NOTE: Everything marked UNMAP_AFTER_INIT becomes inaccessible after this point.
    MM.unmap_text_after_init();

    auto userspace_init = kernel_command_line().userspace_init();
    auto init_args = kernel_command_line().userspace_init_args();

    dmesgln("Running first user process: {}", userspace_init);
    dmesgln("Init (first) process args: {}", init_args);

    auto init_or_error = Process::create_user_process(userspace_init, UserID(0), GroupID(0), move(init_args), {}, move(first_process_vfs_context), move(hostname_context), tty0);
    if (init_or_error.is_error())
        PANIC("init_stage2: Error spawning init process: {}", init_or_error.error());

    auto [init_process, init_thread] = init_or_error.release_value();

    g_init_pid = init_process->pid();
    init_thread->set_priority(THREAD_PRIORITY_HIGH);

    NetworkTask::spawn();

    // NOTE: All kernel processes must be created before enabling boot profiling.
    //       This is so profiling_enable() can emit process created performance events for them.
    if (boot_profiling) {
        dbgln("Starting full system boot profiling");
        MutexLocker mutex_locker(Process::current().big_lock());
        auto const enable_all = ~(u64)0;
        auto result = Process::current().profiling_enable(-1, enable_all);
        VERIFY(!result.is_error());
    }

    Process::current().sys$exit(0);
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT void setup_serial_debug()
{
    // serial_debug will output all the dbgln() data to COM1 at
    // 8-N-1 57600 baud. this is particularly useful for debugging the boot
    // process on live hardware.
    if (s_kernel_cmdline.contains("serial_debug"sv)) {
        set_serial_debug_enabled(true);
    }
}

// Define some Itanium C++ ABI methods to stop the linker from complaining.
// If we actually call these something has gone horribly wrong
void* __dso_handle __attribute__((visibility("hidden")));

}
