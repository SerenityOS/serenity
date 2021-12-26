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

#include "Devices/PATADiskDevice.h"
#include "KSyms.h"
#include "Process.h"
#include "RTC.h"
#include "Scheduler.h"
#include "kstdio.h"
#include <AK/Types.h>
#include <Kernel/ACPI/ACPIDynamicParser.h>
#include <Kernel/ACPI/ACPIStaticParser.h>
#include <Kernel/ACPI/DMIDecoder.h>
#include <Kernel/Arch/i386/APIC.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/PIC.h>
#include <Kernel/Arch/i386/PIT.h>
#include <Kernel/CMOS.h>
#include <Kernel/Devices/BXVGADevice.h>
#include <Kernel/Devices/DebugLogDevice.h>
#include <Kernel/Devices/DiskPartition.h>
#include <Kernel/Devices/FloppyDiskDevice.h>
#include <Kernel/Devices/FullDevice.h>
#include <Kernel/Devices/GPTPartitionTable.h>
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/Devices/MBRPartitionTable.h>
#include <Kernel/Devices/MBVGADevice.h>
#include <Kernel/Devices/NullDevice.h>
#include <Kernel/Devices/PATAChannel.h>
#include <Kernel/Devices/PS2MouseDevice.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Devices/SB16.h>
#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/Devices/ZeroDevice.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/KParams.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Net/E1000NetworkAdapter.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/NetworkTask.h>
#include <Kernel/Net/RTL8139NetworkAdapter.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/Initializer.h>
#include <Kernel/Random.h>
#include <Kernel/TTY/PTYMultiplexer.h>
#include <Kernel/TTY/VirtualConsole.h>
#include <Kernel/VM/MemoryManager.h>

VirtualConsole* tty0;
VirtualConsole* tty1;
KeyboardDevice* keyboard;
PS2MouseDevice* ps2mouse;
SB16* sb16;
DebugLogDevice* dev_debuglog;
NullDevice* dev_null;
SerialDevice* ttyS0;
SerialDevice* ttyS1;
SerialDevice* ttyS2;
SerialDevice* ttyS3;
VFS* vfs;

[[noreturn]] static void init_stage2()
{
    Syscall::initialize();

    auto dev_zero = make<ZeroDevice>();
    auto dev_full = make<FullDevice>();
    auto dev_random = make<RandomDevice>();
    auto dev_ptmx = make<PTYMultiplexer>();

    bool text_debug = KParams::the().has("text_debug");
    bool force_pio = KParams::the().has("force_pio");

    auto root = KParams::the().get("root");
    if (root.is_empty()) {
        root = "/dev/hda";
    }

    bool dmi_unreliable = KParams::the().has("dmi_unreliable");
    if (dmi_unreliable) {
        DMIDecoder::initialize_untrusted();
    } else {
        DMIDecoder::initialize();
    }

    if (!root.starts_with("/dev/hda")) {
        kprintf("init_stage2: root filesystem must be on the first IDE hard drive (/dev/hda)\n");
        hang();
    }

    auto pata0 = PATAChannel::create(PATAChannel::ChannelType::Primary, force_pio);
    NonnullRefPtr<DiskDevice> root_dev = *pata0->master_device();

    root = root.substring(strlen("/dev/hda"), root.length() - strlen("/dev/hda"));

    if (root.length()) {
        bool ok;
        unsigned partition_number = root.to_uint(ok);

        if (!ok) {
            kprintf("init_stage2: couldn't parse partition number from root kernel parameter\n");
            hang();
        }

        if (partition_number < 1 || partition_number > 4) {
            kprintf("init_stage2: invalid partition number %d; expected 1 to 4\n", partition_number);
            hang();
        }

        MBRPartitionTable mbr(root_dev);

        if (!mbr.initialize()) {
            kprintf("init_stage2: couldn't read MBR from disk\n");
            hang();
        }

        if (mbr.is_protective_mbr()) {
            dbgprintf("GPT Partitioned Storage Detected!\n");
            GPTPartitionTable gpt(root_dev);
            if (!gpt.initialize()) {
                kprintf("init_stage2: couldn't read GPT from disk\n");
                hang();
            }
            auto partition = gpt.partition(partition_number);
            if (!partition) {
                kprintf("init_stage2: couldn't get partition %d\n", partition_number);
                hang();
            }
            root_dev = *partition;
        } else {
            dbgprintf("MBR Partitioned Storage Detected!\n");
            auto partition = mbr.partition(partition_number);
            if (!partition) {
                kprintf("init_stage2: couldn't get partition %d\n", partition_number);
                hang();
            }
            root_dev = *partition;
        }
    }

    auto e2fs = Ext2FS::create(root_dev);
    if (!e2fs->initialize()) {
        kprintf("init_stage2: couldn't open root filesystem\n");
        hang();
    }

    if (!vfs->mount_root(e2fs)) {
        kprintf("VFS::mount_root failed\n");
        hang();
    }

    current->process().set_root_directory(vfs->root_custody());

    dbgprintf("Load ksyms\n");
    load_ksyms();
    dbgprintf("Loaded ksyms\n");

    // Now, detect whether or not there are actually any floppy disks attached to the system
    u8 detect = CMOS::read(0x10);
    RefPtr<FloppyDiskDevice> fd0;
    RefPtr<FloppyDiskDevice> fd1;
    if ((detect >> 4) & 0x4) {
        fd0 = FloppyDiskDevice::create(FloppyDiskDevice::DriveType::Master);
        kprintf("fd0 is 1.44MB floppy drive\n");
    } else {
        kprintf("fd0 type unsupported! Type == 0x%x\n", detect >> 4);
    }

    if (detect & 0x0f) {
        fd1 = FloppyDiskDevice::create(FloppyDiskDevice::DriveType::Slave);
        kprintf("fd1 is 1.44MB floppy drive");
    } else {
        kprintf("fd1 type unsupported! Type == 0x%x\n", detect & 0x0f);
    }

    int error;

    // SystemServer will start WindowServer, which will be doing graphics.
    // From this point on we don't want to touch the VGA text terminal or
    // accept keyboard input.
    if (text_debug) {
        tty0->set_graphical(false);
        Thread* thread = nullptr;
        Process::create_user_process(thread, "/bin/Shell", (uid_t)0, (gid_t)0, (pid_t)0, error, {}, {}, tty0);
        if (error != 0) {
            kprintf("init_stage2: error spawning Shell: %d\n", error);
            hang();
        }
        thread->set_priority(THREAD_PRIORITY_HIGH);
    } else {
        tty0->set_graphical(true);
        Thread* thread = nullptr;
        Process::create_user_process(thread, "/bin/SystemServer", (uid_t)0, (gid_t)0, (pid_t)0, error, {}, {}, tty0);
        if (error != 0) {
            kprintf("init_stage2: error spawning SystemServer: %d\n", error);
            hang();
        }
        thread->set_priority(THREAD_PRIORITY_HIGH);
    }
    {
        Thread* thread = nullptr;
        Process::create_kernel_process(thread, "NetworkTask", NetworkTask_main);
    }

    current->process().sys$exit(0);
    ASSERT_NOT_REACHED();
}

extern "C" {
multiboot_info_t* multiboot_info_ptr;
}

typedef void (*ctor_func_t)();

// Defined in the linker script
extern ctor_func_t start_ctors;
extern ctor_func_t end_ctors;

// Define some Itanium C++ ABI methods to stop the linker from complaining
// If we actually call these something has gone horribly wrong
void* __dso_handle __attribute__((visibility("hidden")));

extern "C" int __cxa_atexit(void (*)(void*), void*, void*)
{
    ASSERT_NOT_REACHED();
    return 0;
}

extern u32 __stack_chk_guard;
u32 __stack_chk_guard;

extern "C" [[noreturn]] void init()
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

    // serial_debug will output all the kprintf and dbgprintf data to COM1 at
    // 8-N-1 57600 baud. this is particularly useful for debugging the boot
    // process on live hardware.
    //
    // note: it must be the first option in the boot cmdline.
    u32 cmdline = low_physical_to_virtual(multiboot_info_ptr->cmdline);
    if (cmdline && bad_prefix_check(reinterpret_cast<const char*>(cmdline), "serial_debug"))
        set_serial_debug(true);

    detect_cpu_features();

    kmalloc_init();
    slab_alloc_init();

    // must come after kmalloc_init because we use AK_MAKE_ETERNAL in KParams
    new KParams(String(reinterpret_cast<const char*>(cmdline)));

    bool text_debug = KParams::the().has("text_debug");
    bool complete_acpi_disable = KParams::the().has("noacpi");
    bool dynamic_acpi_disable = KParams::the().has("noacpi_aml");
    bool pci_mmio_disable = KParams::the().has("nopci_mmio");

    complete_acpi_disable = true;

    MemoryManager::initialize();

    if (complete_acpi_disable) {
        ACPIParser::initialize_limited();
    } else {
        if (!dynamic_acpi_disable) {
            ACPIDynamicParser::initialize_without_rsdp();
        } else {
            ACPIStaticParser::initialize_without_rsdp();
        }
    }

    vfs = new VFS;
    dev_debuglog = new DebugLogDevice;

    auto console = make<Console>();

    kprintf("Starting SerenityOS...\n");

    if (g_cpu_supports_sse) {
        sse_init();
        kprintf("x86: SSE support enabled\n");
    }

    if (g_cpu_supports_umip) {
        asm volatile(
            "mov %cr4, %eax\n"
            "orl $0x800, %eax\n"
            "mov %eax, %cr4\n");
        kprintf("x86: UMIP support enabled\n");
    }

    if (g_cpu_supports_tsc) {
        asm volatile(
            "mov %cr4, %eax\n"
            "orl $0x4, %eax\n"
            "mov %eax, %cr4\n");
        kprintf("x86: RDTSC support restricted\n");
    }

    if (g_cpu_supports_rdrand) {
        kprintf("x86: Using RDRAND for good randomness\n");
    } else {
        kprintf("x86: No RDRAND support detected. Randomness will be shitty\n");
    }

    __stack_chk_guard = get_good_random<u32>();

    RTC::initialize();
    PIC::initialize();
    gdt_init();
    idt_init();

    // call global constructors after gtd and itd init
    for (ctor_func_t* ctor = &start_ctors; ctor < &end_ctors; ctor++)
        (*ctor)();

    keyboard = new KeyboardDevice;
    ps2mouse = new PS2MouseDevice;
    sb16 = new SB16;
    dev_null = new NullDevice;
    if (!get_serial_debug())
        ttyS0 = new SerialDevice(SERIAL_COM1_ADDR, 64);
    ttyS1 = new SerialDevice(SERIAL_COM2_ADDR, 65);
    ttyS2 = new SerialDevice(SERIAL_COM3_ADDR, 66);
    ttyS3 = new SerialDevice(SERIAL_COM4_ADDR, 67);

    VirtualConsole::initialize();
    tty0 = new VirtualConsole(0, VirtualConsole::AdoptCurrentVGABuffer);
    tty1 = new VirtualConsole(1);
    VirtualConsole::switch_to(0);

    // Sample test to see if the ACPI parser is working...
    kprintf("ACPI: HPET table @ P 0x%x\n", ACPIParser::the().find_table("HPET"));

    PCI::Initializer::the().test_and_initialize(pci_mmio_disable);
    PCI::Initializer::the().dismiss();

    if (APIC::init())
        APIC::enable(0);

    PIT::initialize();

    PCI::enumerate_all([](const PCI::Address& address, PCI::ID id) {
        kprintf("PCI: device @ %w:%b:%b.%d [%w:%w]\n",
            address.seg(),
            address.bus(),
            address.slot(),
            address.function(),
            id.vendor_id,
            id.device_id);
    });

    if (text_debug) {
        dbgprintf("Text mode enabled\n");
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
    auto e1000 = E1000NetworkAdapter::autodetect();
    auto rtl8139 = RTL8139NetworkAdapter::autodetect();

    Process::initialize();
    Thread::initialize();

    Thread* init_stage2_thread = nullptr;
    Process::create_kernel_process(init_stage2_thread, "init_stage2", init_stage2);

    Thread* syncd_thread = nullptr;
    Process::create_kernel_process(syncd_thread, "syncd", [] {
        for (;;) {
            VFS::the().sync();
            current->sleep(1 * TICKS_PER_SECOND);
        }
    });

    Process::create_kernel_process(g_finalizer, "Finalizer", [] {
        current->set_priority(THREAD_PRIORITY_LOW);
        for (;;) {
            current->wait_on(*g_finalizer_wait_queue);
            Thread::finalize_dying_threads();
        }
    });

    Scheduler::pick_next();

    sti();

    Scheduler::idle_loop();
    ASSERT_NOT_REACHED();
}
