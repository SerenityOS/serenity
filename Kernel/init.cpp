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

#if 0
/* Keyboard LED disco task ;^) */

static void led_disco() NORETURN;

static void led_disco()
{
    BYTE b = 0;
    for (;;) {
        sleep(0.5 * TICKS_PER_SECOND);
        Keyboard::unsetLED((Keyboard::LED)b++);
        b &= 7;
        Keyboard::setLED((Keyboard::LED)b);
    }
}
#endif

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

void init()
{
    disableInterrupts();

    kmalloc_init();
    vga_init();

    PIC::initialize();
    gdt_init();
    idt_init();

    MemoryManager::initialize();

    // Anything that registers interrupts goes *after* PIC and IDT for obvious reasons.
    Syscall::initialize();
    PIT::initialize();
    Keyboard::initialize();
    Task::initialize();

    memset(&system, 0, sizeof(system));

    WORD base_memory = (CMOS::read(0x16) << 8) | CMOS::read(0x15);
    WORD ext_memory = (CMOS::read(0x18) << 8) | CMOS::read(0x17);

    kprintf("%u kB base memory\n", base_memory);
    kprintf("%u kB extended memory\n", ext_memory);

    extern void panel_main();

    new Task(panel_main, "panel", IPC::Handle::PanelTask, Task::Ring0);

    //new Task(led_disco, "led-disco", IPC::Handle::Any, Task::Ring0);

    scheduleNewTask();
    enableInterrupts();

    banner();

    Disk::initialize();

    auto vfs = make<VirtualFileSystem>();

    auto dev_zero = make<ZeroDevice>();
    vfs->registerCharacterDevice(1, 3, *dev_zero);

    auto dev_null = make<NullDevice>();
    vfs->registerCharacterDevice(1, 5, *dev_zero);

    auto dev_full = make<FullDevice>();
    vfs->registerCharacterDevice(1, 7, *dev_full);

    auto dev_random = make<RandomDevice>();
    vfs->registerCharacterDevice(1, 8, *dev_random);

    auto dev_hd0 = IDEDiskDevice::create();
    auto e2fs = Ext2FileSystem::create(dev_hd0.copyRef());
    e2fs->initialize(); 

    vfs->mountRoot(e2fs.copyRef());

    //new Task(motd_main, "motd", IPC::Handle::MotdTask, Task::Ring0);
    new Task(user_main, "user", IPC::Handle::UserTask, Task::Ring3);

    //vfs->listDirectory("/");

#if 0
    {
        auto motdFile = vfs->open("/motd.txt");
        ASSERT(motdFile);
        auto motdData = motdFile->readEntireFile();

        for (unsigned i = 0; i < motdData.size(); ++i) {
            kprintf("%c", motdData[i]);
        }
    }
#endif

    // The idle task will spend its eternity here for now.
    for (;;) {
        asm("hlt");
    }
}

