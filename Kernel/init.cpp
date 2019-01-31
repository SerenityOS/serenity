#include "types.h"
#include "kmalloc.h"
#include "i386.h"
#include "i8253.h"
#include "Keyboard.h"
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
#include "GUIEventDevice.h"
#include "MemoryManager.h"
#include "ProcFileSystem.h"
#include "RTC.h"
#include "VirtualConsole.h"
#include "Scheduler.h"
#include "PS2MouseDevice.h"
#include "PTYMultiplexer.h"
#include "DevPtsFS.h"

//#define SPAWN_GUITEST
#define SPAWN_GUITEST2
//#define SPAWN_MULTIPLE_SHELLS
//#define STRESS_TEST_SPAWNING

system_t system;

VirtualConsole* tty0;
VirtualConsole* tty1;
VirtualConsole* tty2;
VirtualConsole* tty3;
Keyboard* keyboard;
PS2MouseDevice* ps2mouse;
GUIEventDevice* gui_event_device;
VFS* vfs;

#ifdef STRESS_TEST_SPAWNING
static void spawn_stress() NORETURN;
static void spawn_stress()
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

static void init_stage2() NORETURN;
static void init_stage2()
{
    Syscall::initialize();

    auto dev_zero = make<ZeroDevice>();
    vfs->register_character_device(*dev_zero);

    auto dev_null = make<NullDevice>();
    vfs->register_character_device(*dev_null);

    auto dev_full = make<FullDevice>();
    vfs->register_character_device(*dev_full);

    auto dev_random = make<RandomDevice>();
    vfs->register_character_device(*dev_random);

    auto dev_ptmx = make<PTYMultiplexer>();
    vfs->register_character_device(*dev_ptmx);

    vfs->register_character_device(*keyboard);
    vfs->register_character_device(*ps2mouse);
    vfs->register_character_device(*gui_event_device);
    vfs->register_character_device(*tty0);
    vfs->register_character_device(*tty1);
    vfs->register_character_device(*tty2);
    vfs->register_character_device(*tty3);

    auto dev_hd0 = IDEDiskDevice::create();
    auto e2fs = Ext2FS::create(dev_hd0.copy_ref());
    e2fs->initialize();

    vfs->mount_root(e2fs.copy_ref());

    load_ksyms();

    vfs->mount(ProcFS::the(), "/proc");
    vfs->mount(DevPtsFS::the(), "/dev/pts");

    Vector<String> environment;
    environment.append("TERM=ansi");

    int error;
    //Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, move(environment), tty0);
    Process::create_user_process("/bin/Terminal", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, move(environment), tty0);
#ifdef SPAWN_GUITEST
    Process::create_user_process("/bin/guitest", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, move(environment), tty0);
#endif
#ifdef SPAWN_GUITEST2
    Process::create_user_process("/bin/guitest2", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, move(environment), tty0);
#endif
#ifdef SPAWN_MULTIPLE_SHELLS
    Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty1);
    Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty2);
    Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, { }, { }, tty3);
#endif

#ifdef STRESS_TEST_SPAWNING
    Process::create_kernel_process("spawn_stress", spawn_stress);
#endif

    extern void WindowServer_main();
    Process::create_kernel_process("WindowServer", WindowServer_main);

    current->sys$exit(0);
    ASSERT_NOT_REACHED();
}

void init() NORETURN;
void init()
{
    cli();

    kmalloc_init();
    init_ksyms();

    auto console = make<Console>();

    RTC::initialize();
    PIC::initialize();
    gdt_init();
    idt_init();

    PTYMultiplexer::initialize_statics();
    VFS::initialize_globals();
    vfs = new VFS;

    keyboard = new Keyboard;
    ps2mouse = new PS2MouseDevice;
    gui_event_device = new GUIEventDevice;

    VirtualConsole::initialize();
    tty0 = new VirtualConsole(0, VirtualConsole::AdoptCurrentVGABuffer);
    tty1 = new VirtualConsole(1);
    tty2 = new VirtualConsole(2);
    tty3 = new VirtualConsole(3);
    VirtualConsole::switch_to(0);

    kprintf("Starting Serenity Operating System...\n");

    MemoryManager::initialize();

    StringImpl::initialize_globals();

    PIT::initialize();

    memset(&system, 0, sizeof(system));

    auto procfs = ProcFS::create();
    procfs->initialize();

    auto devptsfs = DevPtsFS::create();
    devptsfs->initialize();

    Process::initialize();
    Process::create_kernel_process("init_stage2", init_stage2);
    Process::create_kernel_process("syncd", [] {
        for (;;) {
            Syscall::sync();
            sleep(10 * TICKS_PER_SECOND);
        }
    });

    Scheduler::pick_next();

    sti();

    // This now becomes the idle process :^)
    for (;;) {
        asm("hlt");
    }
}
