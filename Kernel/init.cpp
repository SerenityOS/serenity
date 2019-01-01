#include "types.h"
#include "kmalloc.h"
#include "i386.h"
#include "i8253.h"
#include "Keyboard.h"
#include "Process.h"
#include "system.h"
#include "PIC.h"
#include "CMOS.h"
#include "IDEDiskDevice.h"
#include "KSyms.h"
#include <VirtualFileSystem/NullDevice.h>
#include <VirtualFileSystem/ZeroDevice.h>
#include <VirtualFileSystem/FullDevice.h>
#include <VirtualFileSystem/RandomDevice.h>
#include <VirtualFileSystem/Ext2FileSystem.h>
#include <VirtualFileSystem/VirtualFileSystem.h>
#include "MemoryManager.h"
#include "ProcFileSystem.h"
#include "RTC.h"
#include "VirtualConsole.h"
#include "Scheduler.h"

#define SPAWN_MULTIPLE_SHELLS
//#define STRESS_TEST_SPAWNING

system_t system;

VirtualConsole* tty0;
VirtualConsole* tty1;
VirtualConsole* tty2;
VirtualConsole* tty3;
Keyboard* keyboard;

#ifdef STRESS_TEST_SPAWNING
static void spawn_stress() NORETURN;
static void spawn_stress()
{
    dword last_sum_alloc = sum_alloc;

    for (unsigned i = 0; i < 10000; ++i) {
        int error;
        Process::create_user_process("/bin/true", (uid_t)100, (gid_t)100, (pid_t)0, error, Vector<String>(), Vector<String>(), tty0);
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

    auto vfs = make<VFS>();

    auto dev_zero = make<ZeroDevice>();
    vfs->register_character_device(*dev_zero);

    auto dev_null = make<NullDevice>();
    vfs->register_character_device(*dev_null);

    auto dev_full = make<FullDevice>();
    vfs->register_character_device(*dev_full);

    auto dev_random = make<RandomDevice>();
    vfs->register_character_device(*dev_random);

    vfs->register_character_device(*keyboard);

    vfs->register_character_device(*tty0);
    vfs->register_character_device(*tty1);
    vfs->register_character_device(*tty2);
    vfs->register_character_device(*tty3);

    auto dev_hd0 = IDEDiskDevice::create();
    auto e2fs = Ext2FS::create(dev_hd0.copyRef());
    e2fs->initialize();

    vfs->mount_root(e2fs.copyRef());

    load_ksyms();

    vfs->mount(ProcFS::the(), "/proc");

    Vector<String> environment;
    environment.append("TERM=ansi");

    int error;
    Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, Vector<String>(), move(environment), tty0);
#ifdef SPAWN_MULTIPLE_SHELLS
    Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, Vector<String>(), Vector<String>(), tty1);
    Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, Vector<String>(), Vector<String>(), tty2);
    Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, Vector<String>(), Vector<String>(), tty3);
#endif

#ifdef STRESS_TEST_SPAWNING
    Process::create_kernel_process("spawn_stress", spawn_stress);
#endif

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

    keyboard = new Keyboard;

    VirtualConsole::initialize();
    tty0 = new VirtualConsole(0, VirtualConsole::AdoptCurrentVGABuffer);
    tty1 = new VirtualConsole(1);
    tty2 = new VirtualConsole(2);
    tty3 = new VirtualConsole(3);
    VirtualConsole::switch_to(0);

    kprintf("Starting Serenity Operating System...\n");

    MemoryManager::initialize();

    VFS::initialize_globals();
    StringImpl::initialize_globals();

    PIT::initialize();

    memset(&system, 0, sizeof(system));

    word base_memory = (CMOS::read(0x16) << 8) | CMOS::read(0x15);
    word ext_memory = (CMOS::read(0x18) << 8) | CMOS::read(0x17);

    kprintf("%u kB base memory\n", base_memory);
    kprintf("%u kB extended memory\n", ext_memory);

    auto procfs = ProcFS::create();
    procfs->initialize();

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
