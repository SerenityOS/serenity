#include "Devices/PATADiskDevice.h"
#include "KSyms.h"
#include "Process.h"
#include "RTC.h"
#include "Scheduler.h"
#include "kstdio.h"
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/APIC.h>
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
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/TmpFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/KParams.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Net/E1000NetworkAdapter.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/NetworkTask.h>
#include <Kernel/Net/RTL8139NetworkAdapter.h>
#include <Kernel/PCI.h>
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
        thread->set_priority(ThreadPriority::High);
    } else {
        tty0->set_graphical(true);
        Thread* thread = nullptr;
        Process::create_user_process(thread, "/bin/SystemServer", (uid_t)0, (gid_t)0, (pid_t)0, error, {}, {}, tty0);
        if (error != 0) {
            kprintf("init_stage2: error spawning SystemServer: %d\n", error);
            hang();
        }
        thread->set_priority(ThreadPriority::High);
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
void* __dso_handle __attribute__((visibility ("hidden")));

extern "C" int __cxa_atexit ( void (*)(void *), void *, void *)
{
    ASSERT_NOT_REACHED();
    return 0;
}

extern "C" [[noreturn]] void init(u32 physical_address_for_kernel_page_tables)
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
    if (multiboot_info_ptr->cmdline && bad_prefix_check(reinterpret_cast<const char*>(multiboot_info_ptr->cmdline), "serial_debug"))
        set_serial_debug(true);

    sse_init();

    kmalloc_init();
    slab_alloc_init();

    // must come after kmalloc_init because we use AK_MAKE_ETERNAL in KParams
    new KParams(String(reinterpret_cast<const char*>(multiboot_info_ptr->cmdline)));

    bool text_debug = KParams::the().has("text_debug");

    vfs = new VFS;
    dev_debuglog = new DebugLogDevice;

    auto console = make<Console>();

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

    kprintf("Starting SerenityOS...\n");

    MemoryManager::initialize(physical_address_for_kernel_page_tables);

    if (APIC::init())
        APIC::enable(0);

    PIT::initialize();

    PCI::enumerate_all([](const PCI::Address& address, PCI::ID id) {
        kprintf("PCI device: bus=%d slot=%d function=%d id=%w:%w\n",
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
        current->set_priority(ThreadPriority::Low);
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
