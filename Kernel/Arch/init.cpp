/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Arch/InterruptManagement.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Boot/BootInfo.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Boot/Multiboot.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Devices/Audio/Management.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/GPU/Console/BootFramebufferConsole.h>
#include <Kernel/Devices/GPU/Console/VGATextModeConsole.h>
#include <Kernel/Devices/GPU/Management.h>
#include <Kernel/Devices/Generic/DeviceControlDevice.h>
#include <Kernel/Devices/Generic/FullDevice.h>
#include <Kernel/Devices/Generic/MemoryDevice.h>
#include <Kernel/Devices/Generic/NullDevice.h>
#include <Kernel/Devices/Generic/RandomDevice.h>
#include <Kernel/Devices/Generic/SelfTTYDevice.h>
#include <Kernel/Devices/Generic/ZeroDevice.h>
#include <Kernel/Devices/HID/Management.h>
#include <Kernel/Devices/KCOVDevice.h>
#include <Kernel/Devices/PCISerialDevice.h>
#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/Directory.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Firmware/ACPI/Initialize.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/KSyms.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Net/NetworkTask.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/TTY/PTYMultiplexer.h>
#include <Kernel/TTY/VirtualConsole.h>
#include <Kernel/Tasks/FinalizerTask.h>
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
#endif

// Defined in the linker script
typedef void (*ctor_func_t)();
extern ctor_func_t start_heap_ctors[];
extern ctor_func_t end_heap_ctors[];
extern ctor_func_t start_ctors[];
extern ctor_func_t end_ctors[];

extern uintptr_t __stack_chk_guard;
READONLY_AFTER_INIT uintptr_t __stack_chk_guard __attribute__((used));

#if ARCH(X86_64)
extern "C" u8 start_of_safemem_text[];
extern "C" u8 end_of_safemem_text[];
extern "C" u8 start_of_safemem_atomic_text[];
extern "C" u8 end_of_safemem_atomic_text[];
#endif

extern "C" u8 end_of_kernel_image[];

multiboot_module_entry_t multiboot_copy_boot_modules_array[16];
size_t multiboot_copy_boot_modules_count;

READONLY_AFTER_INIT bool g_in_early_boot;

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

extern "C" {
READONLY_AFTER_INIT PhysicalAddress start_of_prekernel_image;
READONLY_AFTER_INIT PhysicalAddress end_of_prekernel_image;
READONLY_AFTER_INIT size_t physical_to_virtual_offset;
READONLY_AFTER_INIT FlatPtr kernel_mapping_base;
READONLY_AFTER_INIT FlatPtr kernel_load_base;
READONLY_AFTER_INIT PhysicalAddress boot_pml4t;
READONLY_AFTER_INIT PhysicalAddress boot_pdpt;
READONLY_AFTER_INIT PhysicalAddress boot_pd0;
READONLY_AFTER_INIT PhysicalAddress boot_pd_kernel;
READONLY_AFTER_INIT Memory::PageTableEntry* boot_pd_kernel_pt1023;
READONLY_AFTER_INIT StringView kernel_cmdline;
READONLY_AFTER_INIT u32 multiboot_flags;
READONLY_AFTER_INIT multiboot_memory_map_t* multiboot_memory_map;
READONLY_AFTER_INIT size_t multiboot_memory_map_count;
READONLY_AFTER_INIT multiboot_module_entry_t* multiboot_modules;
READONLY_AFTER_INIT size_t multiboot_modules_count;
READONLY_AFTER_INIT PhysicalAddress multiboot_framebuffer_addr;
READONLY_AFTER_INIT u32 multiboot_framebuffer_pitch;
READONLY_AFTER_INIT u32 multiboot_framebuffer_width;
READONLY_AFTER_INIT u32 multiboot_framebuffer_height;
READONLY_AFTER_INIT u8 multiboot_framebuffer_bpp;
READONLY_AFTER_INIT u8 multiboot_framebuffer_type;
}

Atomic<Graphics::Console*> g_boot_console;

#if ARCH(AARCH64)
READONLY_AFTER_INIT static u8 s_command_line_buffer[512];
#endif

extern "C" [[noreturn]] UNMAP_AFTER_INIT void init([[maybe_unused]] BootInfo const& boot_info)
{
    g_in_early_boot = true;

#if ARCH(X86_64)
    start_of_prekernel_image = PhysicalAddress { boot_info.start_of_prekernel_image };
    end_of_prekernel_image = PhysicalAddress { boot_info.end_of_prekernel_image };
    physical_to_virtual_offset = boot_info.physical_to_virtual_offset;
    kernel_mapping_base = boot_info.kernel_mapping_base;
    kernel_load_base = boot_info.kernel_load_base;
    gdt64ptr = boot_info.gdt64ptr;
    code64_sel = boot_info.code64_sel;
    boot_pml4t = PhysicalAddress { boot_info.boot_pml4t };
    boot_pdpt = PhysicalAddress { boot_info.boot_pdpt };
    boot_pd0 = PhysicalAddress { boot_info.boot_pd0 };
    boot_pd_kernel = PhysicalAddress { boot_info.boot_pd_kernel };
    boot_pd_kernel_pt1023 = (Memory::PageTableEntry*)boot_info.boot_pd_kernel_pt1023;
    char const* cmdline = (char const*)boot_info.kernel_cmdline;
    kernel_cmdline = StringView { cmdline, strlen(cmdline) };
    multiboot_flags = boot_info.multiboot_flags;
    multiboot_memory_map = (multiboot_memory_map_t*)boot_info.multiboot_memory_map;
    multiboot_memory_map_count = boot_info.multiboot_memory_map_count;
    multiboot_modules = (multiboot_module_entry_t*)boot_info.multiboot_modules;
    multiboot_modules_count = boot_info.multiboot_modules_count;
    multiboot_framebuffer_addr = PhysicalAddress { boot_info.multiboot_framebuffer_addr };
    multiboot_framebuffer_pitch = boot_info.multiboot_framebuffer_pitch;
    multiboot_framebuffer_width = boot_info.multiboot_framebuffer_width;
    multiboot_framebuffer_height = boot_info.multiboot_framebuffer_height;
    multiboot_framebuffer_bpp = boot_info.multiboot_framebuffer_bpp;
    multiboot_framebuffer_type = boot_info.multiboot_framebuffer_type;
#elif ARCH(AARCH64)
    // FIXME: For the aarch64 platforms, we should get the information by parsing a device tree instead of using multiboot.
    auto [ram_base, ram_size] = RPi::Mailbox::the().query_lower_arm_memory_range();
    auto [vcmem_base, vcmem_size] = RPi::Mailbox::the().query_videocore_memory_range();
    multiboot_memory_map_t mmap[] = {
        {
            sizeof(struct multiboot_mmap_entry) - sizeof(u32),
            (u64)ram_base,
            (u64)ram_size,
            MULTIBOOT_MEMORY_AVAILABLE,
        },
        {
            sizeof(struct multiboot_mmap_entry) - sizeof(u32),
            (u64)vcmem_base,
            (u64)vcmem_size,
            MULTIBOOT_MEMORY_RESERVED,
        },
        // FIXME: VideoCore only reports the first 1GB of RAM, the rest only shows up in the device tree.
    };
    multiboot_memory_map = mmap;
    multiboot_memory_map_count = 2;

    multiboot_modules = nullptr;
    multiboot_modules_count = 0;
    // FIXME: Read the /chosen/bootargs property.
    kernel_cmdline = RPi::Mailbox::the().query_kernel_command_line(s_command_line_buffer);
#endif

    setup_serial_debug();

    // We need to copy the command line before kmalloc is initialized,
    // as it may overwrite parts of multiboot!
    CommandLine::early_initialize(kernel_cmdline);
    if (multiboot_modules_count > 0) {
        VERIFY(multiboot_modules);
        memcpy(multiboot_copy_boot_modules_array, multiboot_modules, multiboot_modules_count * sizeof(multiboot_module_entry_t));
    }
    multiboot_copy_boot_modules_count = multiboot_modules_count;

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

#if ARCH(AARCH64)
    auto firmware_version = RPi::Mailbox::the().query_firmware_version();
    dmesgln("RPi: Firmware version: {}", firmware_version);

    RPi::Framebuffer::initialize();
#endif

    // NOTE: If the bootloader provided a framebuffer, then set up an initial console.
    // If the bootloader didn't provide a framebuffer, then set up an initial text console.
    // We do so we can see the output on the screen as soon as possible.
    if (!kernel_command_line().is_early_boot_console_disabled()) {
        if (!multiboot_framebuffer_addr.is_null() && multiboot_framebuffer_type == MULTIBOOT_FRAMEBUFFER_TYPE_RGB) {
            g_boot_console = &try_make_lock_ref_counted<Graphics::BootFramebufferConsole>(multiboot_framebuffer_addr, multiboot_framebuffer_width, multiboot_framebuffer_height, multiboot_framebuffer_pitch).value().leak_ref();
        } else {
            g_boot_console = &Graphics::VGATextModeConsole::initialize().leak_ref();
        }
    }
    dmesgln("Starting SerenityOS...");

    DeviceManagement::initialize();
    SysFSComponentRegistry::initialize();
    DeviceManagement::the().attach_null_device(*NullDevice::must_initialize());
    DeviceManagement::the().attach_console_device(*ConsoleDevice::must_create());
    DeviceManagement::the().attach_device_control_device(*DeviceControlDevice::must_create());

    MM.unmap_prekernel();

#if ARCH(X86_64)
    // Ensure that the safemem sections are not empty. This could happen if the linker accidentally discards the sections.
    VERIFY(+start_of_safemem_text != +end_of_safemem_text);
    VERIFY(+start_of_safemem_atomic_text != +end_of_safemem_atomic_text);
#endif

    // Invoke all static global constructors in the kernel.
    // Note that we want to do this as early as possible.
    for (ctor_func_t* ctor = start_ctors; ctor < end_ctors; ctor++)
        (*ctor)();

    InterruptManagement::initialize();
    ACPI::initialize();

    // Initialize TimeManagement before using randomness!
    TimeManagement::initialize(0);

    __stack_chk_guard = get_fast_random<uintptr_t>();

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

    VirtualFileSystem::initialize();

#if ARCH(X86_64)
    if (!is_serial_debug_enabled())
        (void)SerialDevice::must_create(0).leak_ref();
    (void)SerialDevice::must_create(1).leak_ref();
    (void)SerialDevice::must_create(2).leak_ref();
    (void)SerialDevice::must_create(3).leak_ref();
#elif ARCH(AARCH64)
    (void)MUST(RPi::MiniUART::create()).leak_ref();
#endif

#if ARCH(X86_64)
    VMWareBackdoor::the(); // don't wait until first mouse packet
#endif
    MUST(HIDManagement::initialize());

    GraphicsManagement::the().initialize();
    ConsoleManagement::the().initialize();

    SyncTask::spawn();
    FinalizerTask::spawn();

    auto boot_profiling = kernel_command_line().is_boot_profiling_enabled();

    if (!PCI::Access::is_disabled()) {
        USB::USBManagement::initialize();
    }
    SysFSFirmwareDirectory::initialize();

    if (!PCI::Access::is_disabled()) {
        VirtIO::detect();
    }

    NetworkingManagement::the().initialize();

#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    (void)KCOVDevice::must_create().leak_ref();
#endif
    (void)MemoryDevice::must_create().leak_ref();
    (void)ZeroDevice::must_create().leak_ref();
    (void)FullDevice::must_create().leak_ref();
    (void)RandomDevice::must_create().leak_ref();
    (void)SelfTTYDevice::must_create().leak_ref();
    PTYMultiplexer::initialize();

    AudioManagement::the().initialize();

    StorageManagement::the().initialize(kernel_command_line().root_device(), kernel_command_line().is_force_pio(), kernel_command_line().is_nvme_polling_enabled());
    if (VirtualFileSystem::the().mount_root(StorageManagement::the().root_filesystem()).is_error()) {
        PANIC("VirtualFileSystem::mount_root failed");
    }

    // Switch out of early boot mode.
    g_in_early_boot = false;

    // NOTE: Everything marked READONLY_AFTER_INIT becomes non-writable after this point.
    MM.protect_readonly_after_init_memory();

    // NOTE: Everything in the .ksyms section becomes read-only after this point.
    MM.protect_ksyms_after_init();

    // NOTE: Everything marked UNMAP_AFTER_INIT becomes inaccessible after this point.
    MM.unmap_text_after_init();

    auto userspace_init = kernel_command_line().userspace_init();
    auto init_args = kernel_command_line().userspace_init_args();

    auto init_or_error = Process::create_user_process(userspace_init, UserID(0), GroupID(0), move(init_args), {}, tty0);
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
    if (kernel_cmdline.contains("serial_debug"sv)) {
        set_serial_debug_enabled(true);
    }
}

// Define some Itanium C++ ABI methods to stop the linker from complaining.
// If we actually call these something has gone horribly wrong
void* __dso_handle __attribute__((visibility("hidden")));

}
