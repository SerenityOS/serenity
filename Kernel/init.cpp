/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/ACPI/DynamicParser.h>
#include <Kernel/ACPI/Initialize.h>
#include <Kernel/ACPI/MultiProcessorParser.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/CMOS.h>
#include <Kernel/CommandLine.h>
#include <Kernel/DMI.h>
#include <Kernel/Devices/FullDevice.h>
#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/Devices/MemoryDevice.h>
#include <Kernel/Devices/NullDevice.h>
#include <Kernel/Devices/PCISerialDevice.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Devices/SB16.h>
#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/Devices/USB/UHCIController.h>
#include <Kernel/Devices/VMWareBackdoor.h>
#include <Kernel/Devices/ZeroDevice.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/Interrupts/PIC.h>
#include <Kernel/KSyms.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Net/E1000NetworkAdapter.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/NE2000NetworkAdapter.h>
#include <Kernel/Net/NetworkTask.h>
#include <Kernel/Net/RTL8139NetworkAdapter.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/Initializer.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>
#include <Kernel/RTC.h>
#include <Kernel/Random.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Storage/StorageManagement.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/TTY/PTYMultiplexer.h>
#include <Kernel/TTY/VirtualConsole.h>
#include <Kernel/Tasks/FinalizerTask.h>
#include <Kernel/Tasks/SyncTask.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VirtIO/VirtIO.h>
#include <Kernel/WorkQueue.h>
#include <Kernel/kstdio.h>

// Defined in the linker script
typedef void (*ctor_func_t)();
extern ctor_func_t start_heap_ctors;
extern ctor_func_t end_heap_ctors;
extern ctor_func_t start_ctors;
extern ctor_func_t end_ctors;

extern u32 __stack_chk_guard;
u32 __stack_chk_guard;

extern "C" u8* start_of_safemem_text;
extern "C" u8* end_of_safemem_text;
extern "C" u8* start_of_safemem_atomic_text;
extern "C" u8* end_of_safemem_atomic_text;

extern "C" u8* end_of_kernel_image;

multiboot_module_entry_t multiboot_copy_boot_modules_array[16];
size_t multiboot_copy_boot_modules_count;

extern "C" const char kernel_cmdline[4096];
bool g_in_early_boot;

namespace Kernel {

[[noreturn]] static void init_stage2(void*);
static void setup_serial_debug();

// boot.S expects these functions to exactly have the following signatures.
// We declare them here to ensure their signatures don't accidentally change.
extern "C" void init_finished(u32 cpu) __attribute__((used));
extern "C" [[noreturn]] void init_ap(u32 cpu, Processor* processor_info);
extern "C" [[noreturn]] void init();

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

extern "C" UNMAP_AFTER_INIT [[noreturn]] void init()
{
    if ((FlatPtr)&end_of_kernel_image >= 0xc2000000u) {
        // The kernel has grown too large again!
        asm volatile("cli;hlt");
    }

    g_in_early_boot = true;
    setup_serial_debug();

    // We need to copy the command line before kmalloc is initialized,
    // as it may overwrite parts of multiboot!
    CommandLine::early_initialize(kernel_cmdline);
    memcpy(multiboot_copy_boot_modules_array, (u8*)low_physical_to_virtual(multiboot_info_ptr->mods_addr), multiboot_info_ptr->mods_count * sizeof(multiboot_module_entry_t));
    multiboot_copy_boot_modules_count = multiboot_info_ptr->mods_count;
    s_bsp_processor.early_initialize(0);

    // Invoke the constructors needed for the kernel heap
    for (ctor_func_t* ctor = &start_heap_ctors; ctor < &end_heap_ctors; ctor++)
        (*ctor)();
    kmalloc_init();
    slab_alloc_init();

    ConsoleDevice::initialize();
    s_bsp_processor.initialize(0);

    CommandLine::initialize();
    MemoryManager::initialize(0);

    // Ensure that the safemem sections are not empty. This could happen if the linker accidentally discards the sections.
    VERIFY(&start_of_safemem_text != &end_of_safemem_text);
    VERIFY(&start_of_safemem_atomic_text != &end_of_safemem_atomic_text);

    // Invoke all static global constructors in the kernel.
    // Note that we want to do this as early as possible.
    for (ctor_func_t* ctor = &start_ctors; ctor < &end_ctors; ctor++)
        (*ctor)();

    APIC::initialize();
    InterruptManagement::initialize();
    ACPI::initialize();

    // Initialize the PCI Bus as early as possible, for early boot (PCI based) serial logging
    PCI::initialize();
    PCISerialDevice::detect();

    VFS::initialize();

    dmesgln("Starting SerenityOS...");

    TimeManagement::initialize(0);

    __stack_chk_guard = get_fast_random<u32>();

    NullDevice::initialize();
    if (!get_serial_debug())
        new SerialDevice(IOAddress(SERIAL_COM1_ADDR), 64);
    new SerialDevice(IOAddress(SERIAL_COM2_ADDR), 65);
    new SerialDevice(IOAddress(SERIAL_COM3_ADDR), 66);
    new SerialDevice(IOAddress(SERIAL_COM4_ADDR), 67);

    VMWareBackdoor::the(); // don't wait until first mouse packet
    HIDManagement::initialize();

    GraphicsManagement::the().initialize();
    ConsoleManagement::the().initialize();

    Thread::initialize();
    Process::initialize();
    Scheduler::initialize();

    WorkQueue::initialize();

    {
        RefPtr<Thread> init_stage2_thread;
        Process::create_kernel_process(init_stage2_thread, "init_stage2", init_stage2, nullptr);
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
extern "C" UNMAP_AFTER_INIT [[noreturn]] void init_ap(u32 cpu, Processor* processor_info)
{
    processor_info->early_initialize(cpu);

    processor_info->initialize(cpu);
    MemoryManager::initialize(cpu);

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
    if (APIC::initialized() && APIC::the().enabled_processor_count() > 1) {
        // We can't start the APs until we have a scheduler up and running.
        // We need to be able to process ICI messages, otherwise another
        // core may send too many and end up deadlocking once the pool is
        // exhausted
        APIC::the().boot_aps();
    }

    SyncTask::spawn();
    FinalizerTask::spawn();

    auto boot_profiling = kernel_command_line().is_boot_profiling_enabled();

    USB::UHCIController::detect();

    DMIExpose::initialize();

    VirtIO::detect();

    E1000NetworkAdapter::detect();
    NE2000NetworkAdapter::detect();
    RTL8139NetworkAdapter::detect();

    LoopbackAdapter::the();

    Syscall::initialize();

    new MemoryDevice;
    new ZeroDevice;
    new FullDevice;
    new RandomDevice;
    PTYMultiplexer::initialize();
    SB16::detect();

    StorageManagement::initialize(kernel_command_line().root_device(), kernel_command_line().is_force_pio());
    if (!VFS::the().mount_root(StorageManagement::the().root_filesystem())) {
        PANIC("VFS::mount_root failed");
    }

    Process::current()->set_root_directory(VFS::the().root_custody());

    load_kernel_symbol_table();

    // NOTE: Everything marked READONLY_AFTER_INIT becomes non-writable after this point.
    MM.protect_readonly_after_init_memory();

    // NOTE: Everything marked UNMAP_AFTER_INIT becomes inaccessible after this point.
    MM.unmap_memory_after_init();

    // Switch out of early boot mode.
    g_in_early_boot = false;

    int error;

    // FIXME: It would be nicer to set the mode from userspace.
    // FIXME: It would be smarter to not hardcode that the first tty is the only graphical one
    ConsoleManagement::the().first_tty()->set_graphical(GraphicsManagement::the().framebuffer_devices_exist());
    RefPtr<Thread> thread;
    auto userspace_init = kernel_command_line().userspace_init();
    auto init_args = kernel_command_line().userspace_init_args();
    Process::create_user_process(thread, userspace_init, (uid_t)0, (gid_t)0, ProcessID(0), error, move(init_args), {}, tty0);
    if (error != 0) {
        PANIC("init_stage2: Error spawning SystemServer: {}", error);
    }
    thread->set_priority(THREAD_PRIORITY_HIGH);

    if (boot_profiling) {
        dbgln("Starting full system boot profiling");
        auto result = Process::current()->sys$profiling_enable(-1, ~0ull);
        VERIFY(!result.is_error());
    }

    NetworkTask::spawn();

    Process::current()->sys$exit(0);
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

extern "C" {
multiboot_info_t* multiboot_info_ptr;
}

// Define some Itanium C++ ABI methods to stop the linker from complaining.
// If we actually call these something has gone horribly wrong
void* __dso_handle __attribute__((visibility("hidden")));

}
