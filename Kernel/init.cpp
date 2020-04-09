/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Types.h>
#include <Kernel/ACPI/ACPIDynamicParser.h>
#include <Kernel/ACPI/DMIDecoder.h>
#include <Kernel/ACPI/Initialize.h>
#include <Kernel/ACPI/MultiProcessorParser.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/CMOS.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/BXVGADevice.h>
#include <Kernel/Devices/DiskPartition.h>
#include <Kernel/Devices/EBRPartitionTable.h>
#include <Kernel/Devices/FullDevice.h>
#include <Kernel/Devices/GPTPartitionTable.h>
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/Devices/MBRPartitionTable.h>
#include <Kernel/Devices/MBVGADevice.h>
#include <Kernel/Devices/NullDevice.h>
#include <Kernel/Devices/PATAChannel.h>
#include <Kernel/Devices/PATADiskDevice.h>
#include <Kernel/Devices/PS2MouseDevice.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Devices/SB16.h>
#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/Devices/VMWareBackdoor.h>
#include <Kernel/Devices/ZeroDevice.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/Interrupts/PIC.h>
#include <Kernel/KSyms.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/NetworkTask.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/Initializer.h>
#include <Kernel/Process.h>
#include <Kernel/RTC.h>
#include <Kernel/Random.h>
#include <Kernel/Scheduler.h>
#include <Kernel/TTY/PTYMultiplexer.h>
#include <Kernel/TTY/VirtualConsole.h>
#include <Kernel/Tasks/FinalizerTask.h>
#include <Kernel/Tasks/SyncTask.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/VM/MemoryManager.h>

// Defined in the linker script
typedef void (*ctor_func_t)();
extern ctor_func_t start_ctors;
extern ctor_func_t end_ctors;

extern u32 __stack_chk_guard;
u32 __stack_chk_guard;

namespace Kernel {

[[noreturn]] static void init_stage2();
static void setup_serial_debug();
static void setup_interrupts();
static void setup_time_management();

VirtualConsole* tty0;

// SerenityOS Kernel C++ entry point :^)
//
// This is where C++ execution begins, after boot.S transfers control here.
//
// The purpose of init() is to start multi-tasking. It does the bare minimum
// amount of work needed to start the scheduler.
//
// Once multi-tasking is ready, we spawn a new thread that starts in the
// init_stage2() function. Initialization continues there.

extern "C" [[noreturn]] void init()
{
    setup_serial_debug();

    cpu_setup();

    kmalloc_init();
    slab_alloc_init();

    CommandLine::initialize(reinterpret_cast<const char*>(low_physical_to_virtual(multiboot_info_ptr->cmdline)));

    MemoryManager::initialize();

    gdt_init();
    idt_init();

    // Invoke all static global constructors in the kernel.
    // Note that we want to do this as early as possible.
    for (ctor_func_t* ctor = &start_ctors; ctor < &end_ctors; ctor++)
        (*ctor)();

    setup_interrupts();
    ACPI::initialize();

    new VFS;
    new KeyboardDevice;
    new PS2MouseDevice;
    new Console;

    klog() << "Starting SerenityOS...";

    __stack_chk_guard = get_good_random<u32>();

    setup_time_management();

    new NullDevice;
    if (!get_serial_debug())
        new SerialDevice(SERIAL_COM1_ADDR, 64);
    new SerialDevice(SERIAL_COM2_ADDR, 65);
    new SerialDevice(SERIAL_COM3_ADDR, 66);
    new SerialDevice(SERIAL_COM4_ADDR, 67);

    VirtualConsole::initialize();
    tty0 = new VirtualConsole(0, VirtualConsole::AdoptCurrentVGABuffer);
    new VirtualConsole(1);
    VirtualConsole::switch_to(0);

    Process::initialize();
    Thread::initialize();

    Thread* init_stage2_thread = nullptr;
    Process::create_kernel_process(init_stage2_thread, "init_stage2", init_stage2);

    Scheduler::pick_next();

    sti();

    Scheduler::idle_loop();
    ASSERT_NOT_REACHED();
}

void init_stage2()
{
    SyncTask::spawn();
    FinalizerTask::spawn();

    PCI::initialize();

    if (kernel_command_line().contains("text_debug")) {
        dbg() << "Text mode enabled";
    } else {
        if (multiboot_info_ptr->framebuffer_type == 1 || multiboot_info_ptr->framebuffer_type == 2) {
            new MBVGADevice(
                PhysicalAddress((u32)(multiboot_info_ptr->framebuffer_addr)),
                multiboot_info_ptr->framebuffer_pitch,
                multiboot_info_ptr->framebuffer_width,
                multiboot_info_ptr->framebuffer_height);
        } else {
            new BXVGADevice;
        }
    }

    LoopbackAdapter::the();

    Syscall::initialize();

    new ZeroDevice;
    new FullDevice;
    new RandomDevice;
    new PTYMultiplexer;
    new SB16;
    VMWareBackdoor::initialize();

    bool dmi_unreliable = kernel_command_line().contains("dmi_unreliable");
    if (dmi_unreliable) {
        DMIDecoder::initialize_untrusted();
    } else {
        DMIDecoder::initialize();
    }

    bool text_debug = kernel_command_line().contains("text_debug");
    bool force_pio = kernel_command_line().contains("force_pio");

    auto root = kernel_command_line().get("root");
    if (root.is_empty()) {
        root = "/dev/hda";
    }

    if (!root.starts_with("/dev/hda")) {
        klog() << "init_stage2: root filesystem must be on the first IDE hard drive (/dev/hda)";
        hang();
    }

    auto pata0 = PATAChannel::create(PATAChannel::ChannelType::Primary, force_pio);
    NonnullRefPtr<BlockDevice> root_dev = *pata0->master_device();

    root = root.substring(strlen("/dev/hda"), root.length() - strlen("/dev/hda"));

    if (root.length()) {
        bool ok;
        unsigned partition_number = root.to_uint(ok);

        if (!ok) {
            klog() << "init_stage2: couldn't parse partition number from root kernel parameter";
            hang();
        }

        MBRPartitionTable mbr(root_dev);

        if (!mbr.initialize()) {
            klog() << "init_stage2: couldn't read MBR from disk";
            hang();
        }

        if (mbr.is_protective_mbr()) {
            dbg() << "GPT Partitioned Storage Detected!";
            GPTPartitionTable gpt(root_dev);
            if (!gpt.initialize()) {
                klog() << "init_stage2: couldn't read GPT from disk";
                hang();
            }
            auto partition = gpt.partition(partition_number);
            if (!partition) {
                klog() << "init_stage2: couldn't get partition " << partition_number;
                hang();
            }
            root_dev = *partition;
        } else {
            dbg() << "MBR Partitioned Storage Detected!";
            if (mbr.contains_ebr()) {
                EBRPartitionTable ebr(root_dev);
                if (!ebr.initialize()) {
                    klog() << "init_stage2: couldn't read EBR from disk";
                    hang();
                }
                auto partition = ebr.partition(partition_number);
                if (!partition) {
                    klog() << "init_stage2: couldn't get partition " << partition_number;
                    hang();
                }
                root_dev = *partition;
            } else {
                if (partition_number < 1 || partition_number > 4) {
                    klog() << "init_stage2: invalid partition number " << partition_number << "; expected 1 to 4";
                    hang();
                }
                auto partition = mbr.partition(partition_number);
                if (!partition) {
                    klog() << "init_stage2: couldn't get partition " << partition_number;
                    hang();
                }
                root_dev = *partition;
            }
        }
    }
    auto e2fs = Ext2FS::create(*FileDescription::create(root_dev));
    if (!e2fs->initialize()) {
        klog() << "init_stage2: couldn't open root filesystem";
        hang();
    }

    if (!VFS::the().mount_root(e2fs)) {
        klog() << "VFS::mount_root failed";
        hang();
    }

    Process::current->set_root_directory(VFS::the().root_custody());

    load_kernel_symbol_table();

    int error;

    // SystemServer will start WindowServer, which will be doing graphics.
    // From this point on we don't want to touch the VGA text terminal or
    // accept keyboard input.
    if (text_debug) {
        tty0->set_graphical(false);
        Thread* thread = nullptr;
        Process::create_user_process(thread, "/bin/Shell", (uid_t)0, (gid_t)0, (pid_t)0, error, {}, {}, tty0);
        if (error != 0) {
            klog() << "init_stage2: error spawning Shell: " << error;
            hang();
        }
        thread->set_priority(THREAD_PRIORITY_HIGH);
    } else {
        tty0->set_graphical(true);
        Thread* thread = nullptr;
        Process::create_user_process(thread, "/bin/SystemServer", (uid_t)0, (gid_t)0, (pid_t)0, error, {}, {}, tty0);
        if (error != 0) {
            klog() << "init_stage2: error spawning SystemServer: " << error;
            hang();
        }
        thread->set_priority(THREAD_PRIORITY_HIGH);
    }

    NetworkTask::spawn();

    Process::current->sys$exit(0);
    ASSERT_NOT_REACHED();
}

void setup_serial_debug()
{
    // this is only used one time, directly below here. we can't use this part
    // of libc at this point in the boot process, or we'd just pull strstr in
    // from <string.h>.
    auto bad_prefix_check = [](const char* str, const char* search) -> bool {
        while (*search)
            if (*search++ != *str++)
                return false;

        return true;
    };

    // serial_debug will output all the klog() and dbg() data to COM1 at
    // 8-N-1 57600 baud. this is particularly useful for debugging the boot
    // process on live hardware.
    //
    // note: it must be the first option in the boot cmdline.
    u32 cmdline = low_physical_to_virtual(multiboot_info_ptr->cmdline);
    if (cmdline && bad_prefix_check(reinterpret_cast<const char*>(cmdline), "serial_debug"))
        set_serial_debug(true);
}

extern "C" {
multiboot_info_t* multiboot_info_ptr;
}

// Define some Itanium C++ ABI methods to stop the linker from complaining
// If we actually call these something has gone horribly wrong
void* __dso_handle __attribute__((visibility("hidden")));

extern "C" int __cxa_atexit(void (*)(void*), void*, void*)
{
    ASSERT_NOT_REACHED();
    return 0;
}

void setup_interrupts()
{
    InterruptManagement::initialize();

    if (!kernel_command_line().contains("smp")) {
        InterruptManagement::the().switch_to_pic_mode();
        return;
    }
    auto smp = kernel_command_line().get("smp");
    if (smp == "off") {
        InterruptManagement::the().switch_to_pic_mode();
        return;
    }
    if (smp == "on") {
        InterruptManagement::the().switch_to_ioapic_mode();
        return;
    }

    klog() << "smp boot argmuent has an invalid value.";
    hang();
}

void setup_time_management()
{
    if (!kernel_command_line().contains("time")) {
        TimeManagement::initialize(true);
        return;
    }
    auto time = kernel_command_line().get("time");
    if (time == "legacy") {
        TimeManagement::initialize(false);
        return;
    }
    if (time == "modern") {
        TimeManagement::initialize(true);
        return;
    }

    kprintf("time boot argmuent has an invalid value.\n");
    hang();
}
}
