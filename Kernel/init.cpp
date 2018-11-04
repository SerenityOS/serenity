#include "types.h"
#include "VGA.h"
#include "kmalloc.h"
#include "i386.h"
#include "i8253.h"
#include "Keyboard.h"
#include "Process.h"
#include "system.h"
#include "Disk.h"
#include "PIC.h"
#include "StdLib.h"
#include "Syscall.h"
#include "CMOS.h"
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
#include "ProcFileSystem.h"
#include "RTC.h"
#include "VirtualConsole.h"

#define TEST_VFS
#define KSYMS
#define SPAWN_MULTIPLE_SHELLS
//#define STRESS_TEST_SPAWNING
//#define TEST_ELF_LOADER

system_t system;

VirtualConsole* tty0;
VirtualConsole* tty1;
VirtualConsole* tty2;
VirtualConsole* tty3;
Keyboard* keyboard;

static byte parseHexDigit(char nibble)
{
    if (nibble >= '0' && nibble <= '9')
        return nibble - '0';
    ASSERT(nibble >= 'a' && nibble <= 'f');
    return 10 + (nibble - 'a');
}

#ifdef KSYMS
static Vector<KSym, KmallocEternalAllocator>* s_ksyms;
static bool s_ksyms_ready;

Vector<KSym, KmallocEternalAllocator>& ksyms()
{
    return *s_ksyms;
}

volatile bool ksyms_ready()
{
    return s_ksyms_ready;
}

const KSym* ksymbolicate(dword address)
{
    if (address < ksyms().first().address || address > ksyms().last().address)
        return nullptr;
    for (unsigned i = 0; i < ksyms().size(); ++i) {
        if (address < ksyms()[i + 1].address)
            return &ksyms()[i];
    }
    return nullptr;
}

static void loadKsyms(const ByteBuffer& buffer)
{
    // FIXME: It's gross that this vector grows dynamically rather than being sized-to-fit.
    //        We're wasting that eternal kmalloc memory.
    s_ksyms = new Vector<KSym, KmallocEternalAllocator>;
    auto* bufptr = (const char*)buffer.pointer();
    auto* startOfName = bufptr;
    dword address = 0;

    while (bufptr < buffer.endPointer()) {
        for (unsigned i = 0; i < 8; ++i)
            address = (address << 4) | parseHexDigit(*(bufptr++));
        bufptr += 3;
        startOfName = bufptr;
        while (*(++bufptr)) {
            if (*bufptr == '\n') {
                break;
            }
        }
        // FIXME: The Strings here should be eternally allocated too.
        ksyms().append({ address, String(startOfName, bufptr - startOfName) });
        ++bufptr;
    }
    s_ksyms_ready = true;
}

void dump_backtrace()
{
    if (!current)
        return;
    extern volatile bool ksyms_ready();
    if (!ksyms_ready())
        return;
    dword stack_variable;
    struct RecognizedSymbol {
        dword address;
        const KSym* ksym;
    };
    Vector<RecognizedSymbol> recognizedSymbols;
    for (dword* stackPtr = &stack_variable; current->isValidAddressForKernel(LinearAddress((dword)stackPtr)); stackPtr = (dword*)*stackPtr) {
        dword retaddr = stackPtr[1];
        if (auto* ksym = ksymbolicate(retaddr))
            recognizedSymbols.append({ retaddr, ksym });
    }
    size_t bytesNeeded = 0;
    for (auto& symbol : recognizedSymbols) {
        bytesNeeded += symbol.ksym->name.length() + 8 + 16;
    }
    for (auto& symbol : recognizedSymbols) {
        unsigned offset = symbol.address - symbol.ksym->address;
        dbgprintf("%p  %s +%u\n", symbol.address, symbol.ksym->name.characters(), offset);
    }
}
#endif

static void undertaker_main() NORETURN;
static void undertaker_main()
{
    for (;;) {
        Process::doHouseKeeping();
        sleep(300);
    }
}

static void spawn_stress() NORETURN;
static void spawn_stress()
{
    dword lastAlloc = sum_alloc;

    for (unsigned i = 0; i < 10000; ++i) {
        int error;
        Process::create_user_process("/bin/id", (uid_t)100, (gid_t)100, (pid_t)0, error, Vector<String>(), Vector<String>(), tty0);
//        kprintf("malloc stats: alloc:%u free:%u page_aligned:%u eternal:%u\n", sum_alloc, sum_free, kmalloc_page_aligned, kmalloc_sum_eternal);
//        kprintf("delta:%u\n", sum_alloc - lastAlloc);
        lastAlloc = sum_alloc;
        sleep(60);
    }
    for (;;) {
        asm volatile("hlt");
    }
}


static void init_stage2() NORETURN;
static void init_stage2()
{
    Syscall::initialize();

    Disk::initialize();

#ifdef TEST_VFS
    auto vfs = make<VirtualFileSystem>();

    auto dev_zero = make<ZeroDevice>();
    vfs->registerCharacterDevice(*dev_zero);

    auto dev_null = make<NullDevice>();
    vfs->registerCharacterDevice(*dev_null);

    auto dev_full = make<FullDevice>();
    vfs->registerCharacterDevice(*dev_full);

    auto dev_random = make<RandomDevice>();
    vfs->registerCharacterDevice(*dev_random);

    vfs->registerCharacterDevice(*keyboard);

    vfs->registerCharacterDevice(*tty0);
    vfs->registerCharacterDevice(*tty1);
    vfs->registerCharacterDevice(*tty2);
    vfs->registerCharacterDevice(*tty3);

    auto dev_hd0 = IDEDiskDevice::create();
    auto e2fs = Ext2FileSystem::create(dev_hd0.copyRef());
    e2fs->initialize();

    vfs->mountRoot(e2fs.copyRef());

#ifdef KSYMS
    {
        int error;
        auto handle = vfs->open("/kernel.map", error);
        if (!handle) {
            kprintf("Failed to open /kernel.map\n");
        } else {
            auto buffer = handle->readEntireFile();
            ASSERT(buffer);
            loadKsyms(buffer);
        }
    }
#endif

    vfs->mount(ProcFileSystem::the(), "/proc");

#endif

#ifdef TEST_ELF_LOADER
    {
        auto testExecutable = vfs->open("/bin/id");
        ASSERT(testExecutable);
        auto testExecutableData = testExecutable->readEntireFile();
        ASSERT(testExecutableData);

        ExecSpace space;
        space.loadELF(move(testExecutableData));
        auto* elf_entry = space.symbol_ptr("_start");
        ASSERT(elf_entry);

        typedef int (*MainFunctionPtr)(void);
        kprintf("elf_entry: %p\n", elf_entry);
        int rc = reinterpret_cast<MainFunctionPtr>(elf_entry)();
        kprintf("it returned %d\n", rc);
    }
#endif

    int error;
    auto* sh0 = Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, Vector<String>(), Vector<String>(), tty0);
#ifdef SPAWN_MULTIPLE_SHELLS
    auto* sh1 = Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, Vector<String>(), Vector<String>(), tty1);
    auto* sh2 = Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, Vector<String>(), Vector<String>(), tty2);
    auto* sh3 = Process::create_user_process("/bin/sh", (uid_t)100, (gid_t)100, (pid_t)0, error, Vector<String>(), Vector<String>(), tty3);
#endif

#ifdef STRESS_TEST_SPAWNING
    Process::createKernelProcess(spawn_stress, "spawn_stress");
#endif

#if 0
    // It would be nice to exit this process, but right now it instantiates all kinds of things.
    // At the very least it needs to be made sure those things stick around as appropriate.
    DO_SYSCALL_A1(Syscall::PosixExit, 413);

    kprintf("uh, we're still going after calling sys$exit...\n");
    HANG;
#endif

    for (;;) {
        //sleep(3600 * TICKS_PER_SECOND);
        asm("hlt");
    }
}

void init()
{
    cli();

#ifdef KSYMS
    s_ksyms = nullptr;
    s_ksyms_ready = false;
#endif

    kmalloc_init();
    vga_init();

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

    VirtualFileSystem::initializeGlobals();
    StringImpl::initializeGlobals();

    PIT::initialize();

    memset(&system, 0, sizeof(system));

    WORD base_memory = (CMOS::read(0x16) << 8) | CMOS::read(0x15);
    WORD ext_memory = (CMOS::read(0x18) << 8) | CMOS::read(0x17);

    kprintf("%u kB base memory\n", base_memory);
    kprintf("%u kB extended memory\n", ext_memory);

    auto procfs = ProcFileSystem::create();
    procfs->initialize();

    Process::initialize();

    Process::createKernelProcess(undertaker_main, "undertaker");
    Process::createKernelProcess(init_stage2, "init");

    scheduleNewProcess();

    sti();

    // This now becomes the idle process :^)
    for (;;) {
        asm("hlt");
    }
}

void log_try_lock(const char* where)
{
    kprintf("[%u] >>> locking... (%s)\n", current->pid(), where);
}

void log_locked(const char* where)
{
    kprintf("[%u] >>> locked() in %s\n", current->pid(), where);
}

void log_unlocked(const char* where)
{
    kprintf("[%u] <<< unlocked()\n", current->pid(), where);
}
