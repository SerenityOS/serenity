#include "types.h"
#include "kmalloc.h"
#include "VGA.h"
#include "i386.h"
#include "Assertions.h"
#include "Task.h"
#include "MemoryManager.h"

struct DescriptorTablePointer {
    WORD size;
    void* address;
} PACKED;

static DescriptorTablePointer s_idtr;
static DescriptorTablePointer s_gdtr;
static Descriptor* s_idt;
static Descriptor* s_gdt;

static WORD s_gdtLength;

WORD allocateGDTEntry()
{
    // FIXME: This should not grow indefinitely.
    ASSERT(s_gdtLength < 256);
    WORD newGDTEntry = s_gdtLength * 8;
    s_gdtLength++;
    return newGDTEntry;
}

extern volatile dword exception_state_dump;
extern volatile word exception_code;
asm(
    ".globl exception_state_dump\n"
    "exception_state_dump:\n"
    ".long 0\n"
    ".globl exception_code\n"
    "exception_code:\n"
    ".short 0\n"
);

#define EH_ENTRY(ec) \
extern "C" void exception_ ## ec ## _handler(); \
extern "C" void exception_ ## ec ## _entry(); \
asm( \
    ".globl exception_" # ec "_entry\n" \
    "exception_" # ec "_entry: \n" \
    "    pop exception_code\n" \
    "    pusha\n" \
    "    pushw %ds\n" \
    "    pushw %es\n" \
    "    pushw %fs\n" \
    "    pushw %gs\n" \
    "    pushw %ss\n" \
    "    pushw %ss\n" \
    "    pushw %ss\n" \
    "    pushw %ss\n" \
    "    popw %ds\n" \
    "    popw %es\n" \
    "    popw %fs\n" \
    "    popw %gs\n" \
    "    mov %esp, exception_state_dump\n" \
    "    call exception_" # ec "_handler\n" \
    "    popw %gs\n" \
    "    popw %fs\n" \
    "    popw %es\n" \
    "    popw %ds\n" \
    "    popa\n" \
    "    iret\n" \
);

#define EH_ENTRY_NO_CODE(ec) \
extern "C" void exception_ ## ec ## _handler(); \
extern "C" void exception_ ## ec ## _entry(); \
asm( \
    ".globl exception_" # ec "_entry\n" \
    "exception_" # ec "_entry: \n" \
    "    pusha\n" \
    "    pushw %ds\n" \
    "    pushw %es\n" \
    "    pushw %fs\n" \
    "    pushw %gs\n" \
    "    pushw %ss\n" \
    "    pushw %ss\n" \
    "    pushw %ss\n" \
    "    pushw %ss\n" \
    "    popw %ds\n" \
    "    popw %es\n" \
    "    popw %fs\n" \
    "    popw %gs\n" \
    "    mov %esp, exception_state_dump\n" \
    "    call exception_" # ec "_handler\n" \
    "    popw %gs\n" \
    "    popw %fs\n" \
    "    popw %es\n" \
    "    popw %ds\n" \
    "    popa\n" \
    "    iret\n" \
);

// 6: Invalid Opcode
EH_ENTRY_NO_CODE(6);
void exception_6_handler()
{
    auto& regs = *reinterpret_cast<RegisterDump*>(exception_state_dump);
    kprintf("%s invalid opcode: %u(%s)\n", current->isRing0() ? "Kernel" : "Process", current->pid(), current->name().characters());

    word ss;
    dword esp;
    if (current->isRing0()) {
        ss = regs.ds;
        esp = regs.esp;
    } else {
        ss = regs.ss_if_crossRing;
        esp = regs.esp_if_crossRing;
    }

    kprintf("exception code: %w\n", exception_code);
    kprintf("pc=%w:%x ds=%w es=%w fs=%w gs=%w\n", regs.cs, regs.eip, regs.ds, regs.es, regs.fs, regs.gs);
    kprintf("eax=%x ebx=%x ecx=%x edx=%x\n", regs.eax, regs.ebx, regs.ecx, regs.edx);
    kprintf("ebp=%x esp=%x esi=%x edi=%x\n", regs.ebp, esp, regs.esi, regs.edi);

    if (current->isRing0()) {
        kprintf("Oh shit, we've crashed in ring 0 :(\n");
        HANG;
    }
    HANG;

    // NOTE: This will schedule a new task.
    Task::taskDidCrash(current);
}

// 13: General Protection Fault
EH_ENTRY(13);
void exception_13_handler()
{
    auto& regs = *reinterpret_cast<RegisterDump*>(exception_state_dump);
    kprintf("%s crash: %u(%s)\n", current->isRing0() ? "Kernel" : "Process", current->pid(), current->name().characters());

    word ss;
    dword esp;
    if (current->isRing0()) {
        ss = regs.ds;
        esp = regs.esp;
    } else {
        ss = regs.ss_if_crossRing;
        esp = regs.esp_if_crossRing;
    }

    kprintf("exception code: %w\n", exception_code);
    kprintf("pc=%w:%x ds=%w es=%w fs=%w gs=%w\n", regs.cs, regs.eip, regs.ds, regs.es, regs.fs, regs.gs);
    kprintf("eax=%x ebx=%x ecx=%x edx=%x\n", regs.eax, regs.ebx, regs.ecx, regs.edx);
    kprintf("ebp=%x esp=%x esi=%x edi=%x\n", regs.ebp, esp, regs.esi, regs.edi);

    if (current->isRing0()) {
        kprintf("Oh shit, we've crashed in ring 0 :(\n");
        HANG;
    }

    // NOTE: This will schedule a new task.
    Task::taskDidCrash(current);
}

// 14: Page Fault
EH_ENTRY(14);
void exception_14_handler()
{
    dword faultAddress;
    asm ("movl %%cr2, %%eax":"=a"(faultAddress));

    auto& regs = *reinterpret_cast<RegisterDump*>(exception_state_dump);
    kprintf("%s page fault: %u(%s), %s laddr=%p\n",
        current->isRing0() ? "Kernel" : "User",
        current->pid(),
        current->name().characters(),
        exception_code & 2 ? "write" : "read",
        faultAddress);

    word ss;
    dword esp;
    if (current->isRing0()) {
        ss = regs.ds;
        esp = regs.esp;
    } else {
        ss = regs.ss_if_crossRing;
        esp = regs.esp_if_crossRing;
    }

    kprintf("exception code: %w\n", exception_code);
    kprintf("pc=%w:%x ds=%w es=%w fs=%w gs=%w\n", regs.cs, regs.eip, regs.ds, regs.es, regs.fs, regs.gs);
    kprintf("eax=%x ebx=%x ecx=%x edx=%x\n", regs.eax, regs.ebx, regs.ecx, regs.edx);
    kprintf("ebp=%x esp=%x esi=%x edi=%x\n", regs.ebp, esp, regs.esi, regs.edi);

    if (current->isRing0())
        HANG;

    auto response = MemoryManager::the().handlePageFault(PageFault(exception_code, LinearAddress(faultAddress)));

    if (response == PageFaultResponse::ShouldCrash) {
        kprintf("Crashing after unresolved page fault\n");
        // NOTE: This will schedule a new task.
        Task::taskDidCrash(current);
    } else if (response == PageFaultResponse::Continue) {
        kprintf("Continuing after resolved page fault\n");
    } else {
        ASSERT_NOT_REACHED();
    }
}

#define EH(i, msg) \
    static void _exception ## i () \
    { \
        vga_set_attr(0x0a); \
        kprintf(msg"\n"); \
        DWORD cr0, cr2, cr3, cr4; \
        asm ("movl %%cr0, %%eax":"=a"(cr0)); \
        asm ("movl %%cr2, %%eax":"=a"(cr2)); \
        asm ("movl %%cr3, %%eax":"=a"(cr3)); \
        asm ("movl %%cr4, %%eax":"=a"(cr4)); \
        kprintf("CR0=%x CR2=%x CR3=%x CR4=%x\n", cr0, cr2, cr3, cr4); \
        HANG; \
    }

EH(0, "Divide error")
EH(1, "Debug exception")
EH(2, "Unknown error")
EH(3, "Breakpoint")
EH(4, "Overflow")
EH(5, "Bounds check")
EH(6, "Invalid opcode")
EH(7, "Coprocessor not available")
EH(8, "Double fault")
EH(9, "Coprocessor segment overrun")
EH(10, "Invalid TSS")
EH(11, "Segment not present")
EH(12, "Stack exception")
EH(13, "General protection fault")
EH(14, "Page fault")
EH(15, "Unknown error")
EH(16, "Coprocessor error")

static void writeRawGDTEntry(WORD selector, DWORD low, DWORD high)
{
    WORD i = (selector & 0xfffc) >> 3;
    s_gdt[i].low = low;
    s_gdt[i].high = high;

    if (i > s_gdtLength) {
        s_gdtr.size = (s_gdtLength + 1) * 8;
    }
}

void writeGDTEntry(WORD selector, Descriptor& descriptor)
{
    writeRawGDTEntry(selector, descriptor.low, descriptor.high);
}

Descriptor& getGDTEntry(WORD selector)
{
    WORD i = (selector & 0xfffc) >> 3;
    return *(Descriptor*)(&s_gdt[i]);
}

void flushGDT()
{
    s_gdtr.address = s_gdt;
    s_gdtr.size = (s_gdtLength * 8) - 1;
    asm("lgdt %0"::"m"(s_gdtr));
}

void gdt_init()
{
    s_gdt = new Descriptor[256];
    s_gdtLength = 5;

    s_gdtr.address = s_gdt;
    s_gdtr.size = (s_gdtLength * 8) - 1;

    writeRawGDTEntry(0x0000, 0x00000000, 0x00000000);
    writeRawGDTEntry(0x0008, 0x0000ffff, 0x00cf9a00);
    writeRawGDTEntry(0x0010, 0x0000ffff, 0x00cf9200);
    writeRawGDTEntry(0x0018, 0x0000ffff, 0x00cffa00);
    writeRawGDTEntry(0x0020, 0x0000ffff, 0x00cff200);

    flushGDT();
}

static void unimp_trap()
{
    kprintf("Unhandled IRQ.");
    HANG;
}

void registerInterruptHandler(BYTE index, void (*f)())
{
    s_idt[index].low = 0x00080000 | LSW((f));
    s_idt[index].high = ((DWORD)(f) & 0xffff0000) | 0x8e00;
    flushIDT();
}

void registerUserCallableInterruptHandler(BYTE index, void (*f)())
{
    s_idt[index].low = 0x00080000 | LSW((f));
    s_idt[index].high = ((DWORD)(f) & 0xffff0000) | 0xef00;
    flushIDT();
}

void flushIDT()
{
    asm("lidt %0"::"m"(s_idtr));
}

/* If an 8259 gets cranky, it'll generate a spurious IRQ7.
 * ATM I don't have a clear grasp on when/why this happens,
 * so I ignore them and assume it makes no difference.
 */

extern "C" void irq7_handler();
asm(
    ".globl irq7_handler \n"
    "irq7_handler: \n"
    "   iret\n"
);

void idt_init()
{
    s_idt = new Descriptor[256];

    s_idtr.address = s_idt;
    s_idtr.size = 0x100 * 8;

    for (BYTE i = 0xff; i > 0x10; --i)
        registerInterruptHandler(i, unimp_trap);

    registerInterruptHandler(0x00, _exception0);
    registerInterruptHandler(0x01, _exception1);
    registerInterruptHandler(0x02, _exception2);
    registerInterruptHandler(0x03, _exception3);
    registerInterruptHandler(0x04, _exception4);
    registerInterruptHandler(0x05, _exception5);
    registerInterruptHandler(0x06, exception_6_entry);
    registerInterruptHandler(0x07, _exception7);
    registerInterruptHandler(0x08, _exception8);
    registerInterruptHandler(0x09, _exception9);
    registerInterruptHandler(0x0a, _exception10);
    registerInterruptHandler(0x0b, _exception11);
    registerInterruptHandler(0x0c, _exception12);
    registerInterruptHandler(0x0d, exception_13_entry);
    registerInterruptHandler(0x0e, exception_14_entry);
    registerInterruptHandler(0x0f, _exception15);
    registerInterruptHandler(0x10, _exception16);

    registerInterruptHandler(0x57, irq7_handler);

    flushIDT();
}

void loadTaskRegister(WORD selector)
{
    asm("ltr %0"::"r"(selector));
}
