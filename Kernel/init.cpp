#include "types.h"
#include "kmalloc.h"
#include "i386.h"
#include "i8253.h"
#include "Keyboard.h"
#include "Process.h"
#include "system.h"
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
#include <VirtualFileSystem/FileDescriptor.h>
#include <AK/OwnPtr.h>
#include "MemoryManager.h"
#include <ELFLoader/ELFLoader.h>
#include "Console.h"
#include "ProcFileSystem.h"
#include "RTC.h"
#include "VirtualConsole.h"
#include "Scheduler.h"

#define KSYMS
#define SPAWN_MULTIPLE_SHELLS
//#define STRESS_TEST_SPAWNING

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

bool ksyms_ready()
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
    dword ksym_count = 0;

    for (unsigned i = 0; i < 8; ++i)
        ksym_count = (ksym_count << 4) | parseHexDigit(*(bufptr++));
    s_ksyms->ensureCapacity(ksym_count);
    ++bufptr; // skip newline

    kprintf("Loading ksyms: \033[s");

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

        if ((ksyms().size() % 10) == 0 || ksym_count == ksyms().size())
            kprintf("\033[u\033[s%u/%u", ksyms().size(), ksym_count);
        ++bufptr;
    }
    kprintf("\n");
    s_ksyms_ready = true;
}

void dump_backtrace(bool use_ksyms)
{
    if (!current) {
        HANG;
        return;
    }
    if (use_ksyms && !ksyms_ready()) {
        HANG;
        return;
    }
    struct RecognizedSymbol {
        dword address;
        const KSym* ksym;
    };
    Vector<RecognizedSymbol> recognizedSymbols;
    if (use_ksyms) {
        for (dword* stackPtr = (dword*)&use_ksyms; current->isValidAddressForKernel(LinearAddress((dword)stackPtr)); stackPtr = (dword*)*stackPtr) {
            dword retaddr = stackPtr[1];
            if (auto* ksym = ksymbolicate(retaddr))
                recognizedSymbols.append({ retaddr, ksym });
        }
    } else{
        for (dword* stackPtr = (dword*)&use_ksyms; current->isValidAddressForKernel(LinearAddress((dword)stackPtr)); stackPtr = (dword*)*stackPtr) {
            dword retaddr = stackPtr[1];
            kprintf("%x (next: %x)\n", retaddr, stackPtr ? (dword*)*stackPtr : 0);
        }
        return;
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

#ifdef STRESS_TEST_SPAWNING
static void spawn_stress() NORETURN;
static void spawn_stress()
{
    dword lastAlloc = sum_alloc;

    for (unsigned i = 0; i < 10000; ++i) {
        int error;
        Process::create_user_process("/bin/id", (uid_t)100, (gid_t)100, (pid_t)0, error, Vector<String>(), Vector<String>(), tty0);
        kprintf("malloc stats: alloc:%u free:%u page_aligned:%u eternal:%u\n", sum_alloc, sum_free, kmalloc_page_aligned, kmalloc_sum_eternal);
        kprintf("delta:%u\n", sum_alloc - lastAlloc);
        lastAlloc = sum_alloc;
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

#ifdef KSYMS
    {
        int error;
        auto descriptor = vfs->open("/kernel.map", error);
        if (!descriptor) {
            kprintf("Failed to open /kernel.map\n");
        } else {
            auto buffer = descriptor->readEntireFile();
            ASSERT(buffer);
            loadKsyms(buffer);
        }
    }
#endif

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
    Process::create_kernel_process(spawn_stress, "spawn_stress");
#endif

    current->sys$exit(0);
    ASSERT_NOT_REACHED();
}

void init() NORETURN;
void init()
{
    cli();

#ifdef KSYMS
    s_ksyms = nullptr;
    s_ksyms_ready = false;
#endif

    kmalloc_init();

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
    StringImpl::initializeGlobals();

    PIT::initialize();

    memset(&system, 0, sizeof(system));

    word base_memory = (CMOS::read(0x16) << 8) | CMOS::read(0x15);
    word ext_memory = (CMOS::read(0x18) << 8) | CMOS::read(0x17);

    kprintf("%u kB base memory\n", base_memory);
    kprintf("%u kB extended memory\n", ext_memory);

    auto procfs = ProcFS::create();
    procfs->initialize();

    Process::initialize();
    Process::create_kernel_process(init_stage2, "init_stage2");

    Scheduler::pick_next();

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
