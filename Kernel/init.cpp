#include "Devices/PATADiskDevice.h"
#include "KSyms.h"
#include "Process.h"
#include "RTC.h"
#include "Scheduler.h"
#include "kmalloc.h"
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/PIC.h>
#include <Kernel/Arch/i386/PIT.h>
#include <Kernel/CMOS.h>
#include <Kernel/Devices/BXVGADevice.h>
#include <Kernel/Devices/DebugLogDevice.h>
#include <Kernel/Devices/DiskPartition.h>
#include <Kernel/Devices/FloppyDiskDevice.h>
#include <Kernel/Devices/FullDevice.h>
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/Devices/MBRPartitionTable.h>
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
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KParams.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Net/E1000NetworkAdapter.h>
#include <Kernel/Net/NetworkTask.h>
#include <Kernel/TTY/PTYMultiplexer.h>
#include <Kernel/TTY/VirtualConsole.h>
#include <Kernel/VM/MemoryManager.h>

VirtualConsole* tty0;
VirtualConsole* tty1;
VirtualConsole* tty2;
VirtualConsole* tty3;
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

    auto root = KParams::the().get("root");
    if (root.is_empty()) {
        root = "/dev/hda";
    }

    if (!root.starts_with("/dev/hda")) {
        kprintf("init_stage2: root filesystem must be on the first IDE hard drive (/dev/hda)\n");
        hang();
    }

    auto pata0 = PATAChannel::create(PATAChannel::ChannelType::Primary);
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

        auto partition = mbr.partition(partition_number);
        if (!partition) {
            kprintf("init_stage2: couldn't get partition %d\n", partition_number);
            hang();
        }

        root_dev = *partition;
    }

    auto e2fs = Ext2FS::create(root_dev);
    if (!e2fs->initialize()) {
        kprintf("init_stage2: couldn't open root filesystem\n");
        hang();
    }

    vfs->mount_root(e2fs);

    dbgprintf("Load ksyms\n");
    load_ksyms();
    dbgprintf("Loaded ksyms\n");

    vfs->mount(ProcFS::the(), "/proc");
    vfs->mount(DevPtsFS::the(), "/dev/pts");

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

    auto* system_server_process = Process::create_user_process("/bin/SystemServer", (uid_t)100, (gid_t)100, (pid_t)0, error, {}, {}, tty0);
    if (error != 0) {
        dbgprintf("init_stage2: error spawning SystemServer: %d\n", error);
        hang();
    }
    system_server_process->set_priority(Process::HighPriority);

    current->process().sys$exit(0);
    ASSERT_NOT_REACHED();
}

extern "C" {
multiboot_info_t* multiboot_info_ptr;
}

extern "C" [[noreturn]] void init()
{
    sse_init();

    kmalloc_init();
    init_ksyms();

    // must come after kmalloc_init because we use AK_MAKE_ETERNAL in KParams
    new KParams(String(reinterpret_cast<const char*>(multiboot_info_ptr->cmdline)));

    vfs = new VFS;
    dev_debuglog = new DebugLogDevice;

    auto console = make<Console>();

    RTC::initialize();
    PIC::initialize();
    gdt_init();
    idt_init();

    keyboard = new KeyboardDevice;
    ps2mouse = new PS2MouseDevice;
    sb16 = new SB16;
    dev_null = new NullDevice;
    ttyS0 = new SerialDevice(SERIAL_COM1_ADDR, 64);
    ttyS1 = new SerialDevice(SERIAL_COM2_ADDR, 65);
    ttyS2 = new SerialDevice(SERIAL_COM3_ADDR, 66);
    ttyS3 = new SerialDevice(SERIAL_COM4_ADDR, 67);

    VirtualConsole::initialize();
    tty0 = new VirtualConsole(0, VirtualConsole::AdoptCurrentVGABuffer);
    tty1 = new VirtualConsole(1);
    tty2 = new VirtualConsole(2);
    tty3 = new VirtualConsole(3);
    VirtualConsole::switch_to(0);

    kprintf("Starting Serenity Operating System...\n");

    MemoryManager::initialize();
    PIT::initialize();

    new BXVGADevice;

    auto e1000 = E1000NetworkAdapter::autodetect();

    NonnullRefPtr<ProcFS> new_procfs = ProcFS::create();
    new_procfs->initialize();

    auto devptsfs = DevPtsFS::create();
    devptsfs->initialize();

    Process::initialize();
    Thread::initialize();
    Process::create_kernel_process("init_stage2", init_stage2);
    Process::create_kernel_process("syncd", [] {
        for (;;) {
            Syscall::sync();
            current->sleep(1 * TICKS_PER_SECOND);
        }
    });
    Process::create_kernel_process("Finalizer", [] {
        g_finalizer = current;
        current->process().set_priority(Process::LowPriority);
        for (;;) {
            Thread::finalize_dying_threads();
            (void)current->block<Thread::SemiPermanentBlocker>(Thread::SemiPermanentBlocker::Reason::Lurking);
        }
    });
    Process::create_kernel_process("NetworkTask", NetworkTask_main);

    Scheduler::pick_next();

    sti();

    // This now becomes the idle process :^)
    for (;;) {
        asm("hlt");
    }
}
