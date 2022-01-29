/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/BootInfo.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/CMOS.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/Audio/AC97.h>
#include <Kernel/Devices/Audio/SB16.h>
#include <Kernel/Devices/DeviceControlDevice.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/FullDevice.h>
#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/Devices/KCOVDevice.h>
#include <Kernel/Devices/MemoryDevice.h>
#include <Kernel/Devices/NullDevice.h>
#include <Kernel/Devices/PCISerialDevice.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/Devices/VMWareBackdoor.h>
#include <Kernel/Devices/ZeroDevice.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Firmware/ACPI/Initialize.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Firmware/SysFSFirmware.h>
#include <Kernel/Graphics/Console/BootFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/Interrupts/PIC.h>
#include <Kernel/KSyms.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Net/NetworkTask.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Panic.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/RTC.h>
#include <Kernel/Random.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/StorageManagement.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/TTY/PTYMultiplexer.h>
#include <Kernel/TTY/VirtualConsole.h>
#include <Kernel/Tasks/FinalizerTask.h>
#include <Kernel/Tasks/SyncTask.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/WorkQueue.h>
#include <Kernel/kstdio.h>

// Defined in the linker script
typedef void (*ctor_func_t)();
extern ctor_func_t start_heap_ctors[];
extern ctor_func_t end_heap_ctors[];
extern ctor_func_t start_ctors[];
extern ctor_func_t end_ctors[];

extern size_t __stack_chk_guard;
READONLY_AFTER_INIT size_t __stack_chk_guard __attribute__((used));

extern "C" u8 start_of_safemem_text[];
extern "C" u8 end_of_safemem_text[];
extern "C" u8 start_of_safemem_atomic_text[];
extern "C" u8 end_of_safemem_atomic_text[];

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

static Processor s_bsp_processor; // global but let's keep it "private"

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
#if ARCH(X86_64)
READONLY_AFTER_INIT PhysicalAddress boot_pml4t;
#endif
READONLY_AFTER_INIT PhysicalAddress boot_pdpt;
READONLY_AFTER_INIT PhysicalAddress boot_pd0;
READONLY_AFTER_INIT PhysicalAddress boot_pd_kernel;
READONLY_AFTER_INIT PageTableEntry* boot_pd_kernel_pt1023;
READONLY_AFTER_INIT const char* kernel_cmdline;
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

Atomic<Graphics::BootFramebufferConsole*> boot_framebuffer_console;

extern "C" [[noreturn]] UNMAP_AFTER_INIT void init(BootInfo const& boot_info)
{
    g_in_early_boot = true;

    start_of_prekernel_image = PhysicalAddress { boot_info.start_of_prekernel_image };
    end_of_prekernel_image = PhysicalAddress { boot_info.end_of_prekernel_image };
    physical_to_virtual_offset = boot_info.physical_to_virtual_offset;
    kernel_mapping_base = boot_info.kernel_mapping_base;
    kernel_load_base = boot_info.kernel_load_base;
#if ARCH(X86_64)
    gdt64ptr = boot_info.gdt64ptr;
    code64_sel = boot_info.code64_sel;
    boot_pml4t = PhysicalAddress { boot_info.boot_pml4t };
#endif
    boot_pdpt = PhysicalAddress { boot_info.boot_pdpt };
    boot_pd0 = PhysicalAddress { boot_info.boot_pd0 };
    boot_pd_kernel = PhysicalAddress { boot_info.boot_pd_kernel };
    boot_pd_kernel_pt1023 = (PageTableEntry*)boot_info.boot_pd_kernel_pt1023;
    kernel_cmdline = (char const*)boot_info.kernel_cmdline;
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

    setup_serial_debug();

    // We need to copy the command line before kmalloc is initialized,
    // as it may overwrite parts of multiboot!
    CommandLine::early_initialize(kernel_cmdline);
    memcpy(multiboot_copy_boot_modules_array, multiboot_modules, multiboot_modules_count * sizeof(multiboot_module_entry_t));
    multiboot_copy_boot_modules_count = multiboot_modules_count;
    s_bsp_processor.early_initialize(0);

    // Invoke the constructors needed for the kernel heap
    for (ctor_func_t* ctor = start_heap_ctors; ctor < end_heap_ctors; ctor++)
        (*ctor)();
    kmalloc_init();

    load_kernel_symbol_table();

    s_bsp_processor.initialize(0);

    CommandLine::initialize();
    Memory::MemoryManager::initialize(0);

    if (!multiboot_framebuffer_addr.is_null()) {
        // NOTE: If the bootloader provided a framebuffer, then set up an initial console so we can see the output on the screen as soon as possible!
        boot_framebuffer_console = &try_make_ref_counted<Graphics::BootFramebufferConsole>(multiboot_framebuffer_addr, multiboot_framebuffer_width, multiboot_framebuffer_height, multiboot_framebuffer_pitch).value().leak_ref();
    }
    dmesgln("Starting SerenityOS...");

    DeviceManagement::initialize();
    SysFSComponentRegistry::initialize();
    DeviceManagement::the().attach_null_device(*NullDevice::must_initialize());
    DeviceManagement::the().attach_console_device(*ConsoleDevice::must_create());
    DeviceManagement::the().attach_device_control_device(*DeviceControlDevice::must_create());

    MM.unmap_prekernel();

    // Ensure that the safemem sections are not empty. This could happen if the linker accidentally discards the sections.
    VERIFY(+start_of_safemem_text != +end_of_safemem_text);
    VERIFY(+start_of_safemem_atomic_text != +end_of_safemem_atomic_text);

    // Invoke all static global constructors in the kernel.
    // Note that we want to do this as early as possible.
    for (ctor_func_t* ctor = start_ctors; ctor < end_ctors; ctor++)
        (*ctor)();

    InterruptManagement::initialize();
    ACPI::initialize();

    // Initialize TimeManagement before using randomness!
    TimeManagement::initialize(0);

    __stack_chk_guard = get_fast_random<size_t>();

    ProcFSComponentRegistry::initialize();
    Process::initialize();

    Scheduler::initialize();

    if (APIC::initialized() && APIC::the().enabled_processor_count() > 1) {
        // We must set up the AP boot environment before switching to a kernel process,
        // as pages below address USER_RANGE_BASE are only accesible through the kernel
        // page directory.
        APIC::the().setup_ap_boot_environment();
    }

    {
        RefPtr<Thread> init_stage2_thread;
        (void)Process::create_kernel_process(init_stage2_thread, KString::must_create("init_stage2"), init_stage2, nullptr, THREAD_AFFINITY_DEFAULT, Process::RegisterProcess::No);
        // We need to make sure we drop the reference for init_stage2_thread
        // before calling into Scheduler::start, otherwise we will have a
        // dangling Thread that never gets cleaned up
    }

    Scheduler::start();
    VERIFY_NOT_REACHED();
}

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

void init_stage2(void*)
{
    // This is a little bit of a hack. We can't register our process at the time we're
    // creating it, but we need to be registered otherwise finalization won't be happy.
    // The colonel process gets away without having to do this because it never exits.
    Process::register_new(Process::current());

    WorkQueue::initialize();

    if (kernel_command_line().is_smp_enabled() && APIC::initialized() && APIC::the().enabled_processor_count() > 1) {
        // We can't start the APs until we have a scheduler up and running.
        // We need to be able to process ICI messages, otherwise another
        // core may send too many and end up deadlocking once the pool is
        // exhausted
        APIC::the().boot_aps();
    }

    // Initialize the PCI Bus as early as possible, for early boot (PCI based) serial logging
    PCI::initialize();
    PCISerialDevice::detect();

    VirtualFileSystem::initialize();

    if (!get_serial_debug())
        (void)SerialDevice::must_create(0).leak_ref();
    (void)SerialDevice::must_create(1).leak_ref();
    (void)SerialDevice::must_create(2).leak_ref();
    (void)SerialDevice::must_create(3).leak_ref();

    VMWareBackdoor::the(); // don't wait until first mouse packet
    HIDManagement::initialize();

    GraphicsManagement::the().initialize();
    ConsoleManagement::the().initialize();

    SyncTask::spawn();
    FinalizerTask::spawn();

    auto boot_profiling = kernel_command_line().is_boot_profiling_enabled();

    USB::USBManagement::initialize();
    FirmwareSysFSDirectory::initialize();

    VirtIO::detect();

    NetworkingManagement::the().initialize();
    Syscall::initialize();

#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    (void)KCOVDevice::must_create().leak_ref();
#endif
    (void)MemoryDevice::must_create().leak_ref();
    (void)ZeroDevice::must_create().leak_ref();
    (void)FullDevice::must_create().leak_ref();
    (void)RandomDevice::must_create().leak_ref();
    PTYMultiplexer::initialize();

    (void)SB16::try_detect_and_create();
    AC97::detect();

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

    // FIXME: It would be nicer to set the mode from userspace.
    // FIXME: It would be smarter to not hardcode that the first tty is the only graphical one
    ConsoleManagement::the().first_tty()->set_graphical(GraphicsManagement::the().framebuffer_devices_exist());
    RefPtr<Thread> thread;
    auto userspace_init = kernel_command_line().userspace_init();
    auto init_args = kernel_command_line().userspace_init_args();

    auto init_or_error = Process::try_create_user_process(thread, userspace_init, UserID(0), GroupID(0), move(init_args), {}, tty0);
    if (init_or_error.is_error())
        PANIC("init_stage2: Error spawning init process: {}", init_or_error.error());

    thread->set_priority(THREAD_PRIORITY_HIGH);

    if (boot_profiling) {
        dbgln("Starting full system boot profiling");
        MutexLocker mutex_locker(Process::current().big_lock());
        auto result = Process::current().sys$profiling_enable(-1, ~0ull);
        VERIFY(!result.is_error());
    }

    NetworkTask::spawn();

    Process::current().sys$exit(0);
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT void setup_serial_debug()
{
    // serial_debug will output all the dbgln() data to COM1 at
    // 8-N-1 57600 baud. this is particularly useful for debugging the boot
    // process on live hardware.
    if (StringView(kernel_cmdline).contains("serial_debug")) {
        set_serial_debug(true);
    }
}

// Define some Itanium C++ ABI methods to stop the linker from complaining.
// If we actually call these something has gone horribly wrong
void* __dso_handle __attribute__((visibility("hidden")));

}
