#include "types.h"
#include "kmalloc.h"
#include "i386.h"
#include "i8253.h"
#include "KeyboardDevice.h"
#include "Process.h"
#include "system.h"
#include "PIC.h"
#include "IDEDiskDevice.h"
#include "KSyms.h"
#include <Kernel/NullDevice.h>
#include <Kernel/ZeroDevice.h>
#include <Kernel/FullDevice.h>
#include <Kernel/RandomDevice.h>
#include <Kernel/Ext2FileSystem.h>
#include <Kernel/VirtualFileSystem.h>
#include "MemoryManager.h"
#include "ProcFS.h"
#include "RTC.h"
#include "VirtualConsole.h"
#include "Scheduler.h"
#include "PS2MouseDevice.h"
#include "PTYMultiplexer.h"
#include "DevPtsFS.h"
#include "BXVGADevice.h"

#define SPAWN_LAUNCHER
//#define SPAWN_GUITEST2
//#define SPAWN_FILE_MANAGER
#define SPAWN_PROCESS_MANAGER
//#define SPAWN_FONTEDITOR
//#define SPAWN_MULTIPLE_SHELLS
//#define STRESS_TEST_SPAWNING

system_t system;

VirtualConsole* tty0;
VirtualConsole* tty1;
VirtualConsole* tty2;
VirtualConsole* tty3;
KeyboardDevice* keyboard;
PS2MouseDevice* ps2mouse;
NullDevice* dev_null;
VFS* vfs;

#ifdef STRESS_TEST_SPAWNING
[[noreturn]] static void spawn_stress()
{
    dword last_sum_alloc = sum_alloc;

    for (unsigned i = 0; i < 10000; ++i) {
        int error;
        Process::create_user_process("/bin/true", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty0);
        dbgprintf("malloc stats: alloc:%u free:%u eternal:%u !delta:%u\n", sum_alloc, sum_free, kmalloc_sum_eternal, sum_alloc - last_sum_alloc);
        last_sum_alloc = sum_alloc;
        sleep(60);
    }
    for (;;) {
        asm volatile("hlt");
    }
}
#endif

[[noreturn]] static void init_stage2()
{
    Syscall::initialize();

    auto dev_zero = make<ZeroDevice>();
    auto dev_full = make<FullDevice>();
    auto dev_random = make<RandomDevice>();
    auto dev_ptmx = make<PTYMultiplexer>();
    auto dev_hd0 = IDEDiskDevice::create();
    auto e2fs = Ext2FS::create(dev_hd0.copy_ref());
    e2fs->initialize();

    vfs->mount_root(e2fs.copy_ref());

    load_ksyms();

    vfs->mount(ProcFS::the(), "/proc");
    vfs->mount(DevPtsFS::the(), "/dev/pts");

    int error;

    auto* window_server_process = Process::create_user_process("/bin/WindowServer", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty0);
    if (error != 0) {
        dbgprintf("error: %d\n", error);
        hang();
    }
    window_server_process->set_priority(Process::HighPriority);
    //Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, move(environment), tty0);
    Process::create_user_process("/bin/Terminal", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty0);
#ifdef SPAWN_GUITEST2
    Process::create_user_process("/bin/guitest2", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty0);
#endif
#ifdef SPAWN_LAUNCHER
    Process::create_user_process("/bin/Launcher", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty0);
#endif
#ifdef SPAWN_FILE_MANAGER
    Process::create_user_process("/bin/FileManager", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty0);
#endif
#ifdef SPAWN_PROCESS_MANAGER
    Process::create_user_process("/bin/ProcessManager", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty0);
#endif
#ifdef SPAWN_FONTEDITOR
    Process::create_user_process("/bin/FontEditor", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, move(environment), tty0);
#endif
#ifdef SPAWN_MULTIPLE_SHELLS
    Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty1);
    Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty2);
    Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty3);
#endif

#ifdef STRESS_TEST_SPAWNING
    Process::create_kernel_process("spawn_stress", spawn_stress);
#endif

    current->sys$exit(0);
    ASSERT_NOT_REACHED();
}

[[noreturn]] void init()
{
    cli();

    kmalloc_init();
    init_ksyms();

    vfs = new VFS;

    auto console = make<Console>();

    RTC::initialize();
    PIC::initialize();
    gdt_init();
    idt_init();

    keyboard = new KeyboardDevice;
    ps2mouse = new PS2MouseDevice;
    dev_null = new NullDevice;

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

    Retained<ProcFS> new_procfs = ProcFS::create();
    new_procfs->initialize();

    auto devptsfs = DevPtsFS::create();
    devptsfs->initialize();

    Process::initialize();
    Process::create_kernel_process("init_stage2", init_stage2);
    Process::create_kernel_process("syncd", [] {
        for (;;) {
            Syscall::sync();
            sleep(1 * TICKS_PER_SECOND);
        }
    });
    Process::create_kernel_process("Finalizer", [] {
        g_finalizer = current;
        current->set_priority(Process::LowPriority);
        for (;;) {
            Process::finalize_dying_processes();
            current->block(Process::BlockedLurking);
            Scheduler::yield();
        }
    });

    Scheduler::pick_next();

    sti();

    // This now becomes the idle process :^)
    for (;;) {
        asm("hlt");
    }
}
