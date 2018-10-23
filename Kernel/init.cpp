#include "types.h"
#include "VGA.h"
#include "kmalloc.h"
#include "i386.h"
#include "i8253.h"
#include "Keyboard.h"
#include "Task.h"
#include "IPC.h"
#include "system.h"
#include "Disk.h"
#include "PIC.h"
#include "StdLib.h"
#include "Syscall.h"
#include "CMOS.h"
#include "Userspace.h"
#include "IDEDiskDevice.h"
#include <VirtualFileSystem/NullDevice.h>
#include <VirtualFileSystem/ZeroDevice.h>
#include <VirtualFileSystem/FullDevice.h>
#include <VirtualFileSystem/RandomDevice.h>
#include <VirtualFileSystem/Ext2FileSystem.h>
#include <VirtualFileSystem/VirtualFileSystem.h>
#include <VirtualFileSystem/FileHandle.h>
#include <AK/OwnPtr.h>
#include "MemoryManager.h"
#include <ELFLoader/ELFLoader.h>
#include "Console.h"

#define TEST_VFS
//#define TEST_ELF_LOADER
//#define TEST_CRASHY_USER_PROCESSES

static void motd_main() NORETURN;
static void motd_main()
{
    kprintf("Hello in motd_main!\n");
    int fd = Userspace::open("/test.asm");
    kprintf("motd: fd=%d\n", fd);
    ASSERT(fd != -1);
    DO_SYSCALL_A3(0x2000, 1, 2, 3);
    kprintf("getuid(): %u\n", Userspace::getuid());
    auto buffer = DataBuffer::createUninitialized(33);
    memset(buffer->data(), 0, buffer->length());
    int nread = Userspace::read(fd, buffer->data(), buffer->length() - 1);
    kprintf("read(): %d\n", nread);
    buffer->data()[nread] = 0;
    kprintf("read(): '%s'\n", buffer->data());
    for (;;) {
        //kill(4, 5);
        sleep(1 * TICKS_PER_SECOND);
    }
}

static void syscall_test_main() NORETURN;
static void syscall_test_main()
{
    kprintf("Hello in syscall_test_main!\n");
    for (;;) {
        Userspace::getuid();
//        Userspace::yield();
        //kprintf("getuid(): %u\n", Userspace::getuid());
        sleep(1 * TICKS_PER_SECOND);
    }
}

static void user_main() NORETURN;
static void user_main()
{
    DO_SYSCALL_A3(0x3000, 2, 3, 4);
    // Crash ourselves!
    char* x = reinterpret_cast<char*>(0xbeefbabe);
    *x = 1;
    HANG;
    for (;;) {
        // nothing?
        Userspace::sleep(1 * TICKS_PER_SECOND);
    }
}

system_t system;

void banner()
{
    kprintf("\n");
    vga_set_attr(0x0a);
    kprintf(" _____         _           _   \n");
    vga_set_attr(0x0b);
    kprintf("|   __|___ ___| |_ ___ ___| |_ \n");
    vga_set_attr(0x0c);
    kprintf("|  |  | -_|  _| . | -_|  _|  _|\n");
    vga_set_attr(0x0d);
    kprintf("|_____|___|_| |___|___|_| |_|  \n");
    vga_set_attr(0x07);
    kprintf("\n");
}

static void init_stage2() NORETURN;
static void init_stage2()
{
    kprintf("init stage2...\n");

    Syscall::initialize();

    auto keyboard = make<Keyboard>();

    extern void panel_main();
    new Task(panel_main, "panel", IPC::Handle::PanelTask, Task::Ring0);
    //new Task(led_disco, "led-disco", IPC::Handle::Any, Task::Ring0);

    Disk::initialize();

#ifdef TEST_VFS
    auto vfs = make<VirtualFileSystem>();

    auto dev_zero = make<ZeroDevice>();
    vfs->registerCharacterDevice(1, 3, *dev_zero);

    auto dev_null = make<NullDevice>();
    vfs->registerCharacterDevice(1, 5, *dev_zero);

    auto dev_full = make<FullDevice>();
    vfs->registerCharacterDevice(1, 7, *dev_full);

    auto dev_random = make<RandomDevice>();
    vfs->registerCharacterDevice(1, 8, *dev_random);

    vfs->registerCharacterDevice(85, 1, *keyboard);

    auto dev_hd0 = IDEDiskDevice::create();
    auto e2fs = Ext2FileSystem::create(dev_hd0.copyRef());
    e2fs->initialize();

    vfs->mountRoot(e2fs.copyRef());

    //vfs->listDirectory("/");

    {
        auto motdFile = vfs->open("/motd.txt");
        ASSERT(motdFile);
        auto motdData = motdFile->readEntireFile();

        for (unsigned i = 0; i < motdData.size(); ++i) {
            kprintf("%c", motdData[i]);
        }
    }
#endif

#ifdef TEST_CRASHY_USER_PROCESSES
    new Task(user_main, "user", IPC::Handle::UserTask, Task::Ring3);
#endif

#ifdef TEST_ELF_LOADER
    {
        auto testExecutable = vfs->open("/bin/id");
        ASSERT(testExecutable);
        auto testExecutableData = testExecutable->readEntireFile();
        ASSERT(testExecutableData);

        ExecSpace space;
        space.loadELF(move(testExecutableData));
        auto* elf_entry = space.symbolPtr("_start");
        ASSERT(elf_entry);

        typedef int (*MainFunctionPtr)(void);
        kprintf("elf_entry: %p\n", elf_entry);
        int rc = reinterpret_cast<MainFunctionPtr>(elf_entry)();
        kprintf("it returned %d\n", rc);
    }
#endif

    //auto* idTask = Task::create("/bin/id", (uid_t)209, (gid_t)1985);

    auto* shTask = Task::create("/bin/sh", (uid_t)100, (gid_t)100);

    //new Task(motd_main, "motd", IPC::Handle::MotdTask, Task::Ring0);
    //new Task(syscall_test_main, "syscall_test", IPC::Handle::MotdTask, Task::Ring3);

    kprintf("init stage2 is done!\n");

#if 0
    // It would be nice to exit this process, but right now it instantiates all kinds of things.
    // At the very least it needs to be made sure those things stick around as appropriate.
    DO_SYSCALL_A1(Syscall::PosixExit, 413);

    kprintf("uh, we're still going after calling sys$exit...\n");
    HANG;
#endif

    for (;;) {
        asm("hlt");
    }
}

void init()
{
    cli();

    kmalloc_init();
    vga_init();

    auto console = make<Console>();

    PIC::initialize();
    gdt_init();
    idt_init();

    MemoryManager::initialize();

    VirtualFileSystem::initializeGlobals();
    StringImpl::initializeGlobals();

    PIT::initialize();

    memset(&system, 0, sizeof(system));
    WORD base_memory = (CMOS::read(0x16) << 8) | CMOS::read(0x15);
    WORD ext_memory = (CMOS::read(0x18) << 8) | CMOS::read(0x17);

    kprintf("%u kB base memory\n", base_memory);
    kprintf("%u kB extended memory\n", ext_memory);

    Task::initialize();

    auto* init2 = new Task(init_stage2, "init", IPC::Handle::InitTask, Task::Ring0);
    scheduleNewTask();

    sti();

    // This now becomes the idle task :^)
    for (;;) {
        asm("hlt");
    }
}

