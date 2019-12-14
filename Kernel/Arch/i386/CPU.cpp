#include "APIC.h"
#include "Assertions.h"
#include "IRQHandler.h"
#include "PIC.h"
#include "Process.h"
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/KSyms.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/mallocdefs.h>

//#define PAGE_FAULT_DEBUG

struct [[gnu::packed]] DescriptorTablePointer
{
    u16 limit;
    void* address;
};

static DescriptorTablePointer s_idtr;
static DescriptorTablePointer s_gdtr;
static Descriptor s_idt[256];
static Descriptor s_gdt[256];

static IRQHandler* s_irq_handler[16];

static Vector<u16>* s_gdt_freelist;

static u16 s_gdt_length;

u16 gdt_alloc_entry()
{
    ASSERT(s_gdt_freelist);
    ASSERT(!s_gdt_freelist->is_empty());
    return s_gdt_freelist->take_last();
}

void gdt_free_entry(u16 entry)
{
    s_gdt_freelist->append(entry);
}

extern "C" void handle_irq();
extern "C" void asm_irq_entry();

asm(
    ".globl asm_irq_entry\n"
    "asm_irq_entry: \n"
    "    pushl $0x0\n"
    "    pusha\n"
    "    pushw %ds\n"
    "    pushw %es\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    popw %ds\n"
    "    popw %es\n"
    "    cld\n"
    "    call handle_irq\n"
    "    popw %es\n"
    "    popw %ds\n"
    "    popa\n"
    "    add $0x4, %esp\n"
    "    iret\n");

#define EH_ENTRY(ec)                                        \
    extern "C" void exception_##ec##_handler(RegisterDump); \
    extern "C" void exception_##ec##_entry();               \
    asm(                                                    \
        ".globl exception_" #ec "_entry\n"                  \
        "exception_" #ec "_entry: \n"                       \
        "    pusha\n"                                       \
        "    pushw %ds\n"                                   \
        "    pushw %es\n"                                   \
        "    pushw %fs\n"                                   \
        "    pushw %gs\n"                                   \
        "    pushw %ss\n"                                   \
        "    pushw %ss\n"                                   \
        "    pushw %ss\n"                                   \
        "    pushw %ss\n"                                   \
        "    pushw %ss\n"                                   \
        "    popw %ds\n"                                    \
        "    popw %es\n"                                    \
        "    popw %fs\n"                                    \
        "    popw %gs\n"                                    \
        "    cld\n"                                         \
        "    call exception_" #ec "_handler\n"              \
        "    popw %gs\n"                                    \
        "    popw %gs\n"                                    \
        "    popw %fs\n"                                    \
        "    popw %es\n"                                    \
        "    popw %ds\n"                                    \
        "    popa\n"                                        \
        "    add $0x4, %esp\n"                              \
        "    iret\n");

#define EH_ENTRY_NO_CODE(ec)                                \
    extern "C" void exception_##ec##_handler(RegisterDump); \
    extern "C" void exception_##ec##_entry();               \
    asm(                                                    \
        ".globl exception_" #ec "_entry\n"                  \
        "exception_" #ec "_entry: \n"                       \
        "    pushl $0x0\n"                                  \
        "    pusha\n"                                       \
        "    pushw %ds\n"                                   \
        "    pushw %es\n"                                   \
        "    pushw %fs\n"                                   \
        "    pushw %gs\n"                                   \
        "    pushw %ss\n"                                   \
        "    pushw %ss\n"                                   \
        "    pushw %ss\n"                                   \
        "    pushw %ss\n"                                   \
        "    pushw %ss\n"                                   \
        "    popw %ds\n"                                    \
        "    popw %es\n"                                    \
        "    popw %fs\n"                                    \
        "    popw %gs\n"                                    \
        "    cld\n"                                         \
        "    call exception_" #ec "_handler\n"              \
        "    popw %gs\n"                                    \
        "    popw %gs\n"                                    \
        "    popw %fs\n"                                    \
        "    popw %es\n"                                    \
        "    popw %ds\n"                                    \
        "    popa\n"                                        \
        "    add $0x4, %esp\n"                              \
        "    iret\n");

static void dump(const RegisterDump& regs)
{
    u16 ss;
    u32 esp;
    if (!current || current->process().is_ring0()) {
        ss = regs.ss;
        esp = regs.esp;
    } else {
        ss = regs.ss_if_crossRing;
        esp = regs.esp_if_crossRing;
    }

    kprintf("exception code: %04x\n", regs.exception_code);
    kprintf("  pc=%04x:%08x ds=%04x es=%04x fs=%04x gs=%04x\n", regs.cs, regs.eip, regs.ds, regs.es, regs.fs, regs.gs);
    kprintf(" stk=%04x:%08x\n", ss, esp);
    if (current)
        kprintf("kstk=%04x:%08x, base=%08x\n", current->tss().ss0, current->tss().esp0, current->kernel_stack_base());
    kprintf("eax=%08x ebx=%08x ecx=%08x edx=%08x\n", regs.eax, regs.ebx, regs.ecx, regs.edx);
    kprintf("ebp=%08x esp=%08x esi=%08x edi=%08x\n", regs.ebp, esp, regs.esi, regs.edi);

    if (current && current->process().validate_read((void*)regs.eip, 8)) {
        u8* codeptr = (u8*)regs.eip;
        kprintf("code: %02x %02x %02x %02x %02x %02x %02x %02x\n",
            codeptr[0],
            codeptr[1],
            codeptr[2],
            codeptr[3],
            codeptr[4],
            codeptr[5],
            codeptr[6],
            codeptr[7]);
    }
}

void handle_crash(RegisterDump& regs, const char* description, int signal)
{
    if (!current) {
        kprintf("%s with !current\n", description);
        hang();
    }

    kprintf("\033[31;1mCRASH: %s. %s: %s(%u)\033[0m\n",
        description,
        current->process().is_ring0() ? "Kernel" : "Process",
        current->process().name().characters(),
        current->pid());

    dump(regs);

    if (current->process().is_ring0()) {
        kprintf("Oh shit, we've crashed in ring 0 :(\n");
        dump_backtrace();
        hang();
    }

    cli();
    current->process().crash(signal, regs.eip);
}

EH_ENTRY_NO_CODE(6);
void exception_6_handler(RegisterDump regs)
{
    handle_crash(regs, "Illegal instruction", SIGILL);
}

EH_ENTRY_NO_CODE(0);
void exception_0_handler(RegisterDump regs)
{
    handle_crash(regs, "Division by zero", SIGFPE);
}

EH_ENTRY(13);
void exception_13_handler(RegisterDump regs)
{
    handle_crash(regs, "General protection fault", SIGSEGV);
}

// 7: FPU not available exception
EH_ENTRY_NO_CODE(7);
void exception_7_handler(RegisterDump regs)
{
    (void)regs;

    asm volatile("clts");
    if (g_last_fpu_thread == current)
        return;
    if (g_last_fpu_thread) {
        asm volatile("fxsave %0"
                     : "=m"(g_last_fpu_thread->fpu_state()));
    } else {
        asm volatile("fnclex");
    }
    g_last_fpu_thread = current;

    if (current->has_used_fpu()) {
        asm volatile("fxrstor %0" ::"m"(current->fpu_state()));
    } else {
        asm volatile("fninit");
        asm volatile("fxsave %0"
                     : "=m"(current->fpu_state()));
        current->set_has_used_fpu(true);
    }

#ifdef FPU_EXCEPTION_DEBUG
    kprintf("%s FPU not available exception: %u(%s)\n", current->process().is_ring0() ? "Kernel" : "Process", current->pid(), current->process().name().characters());
    dump(regs);
#endif
}

// 14: Page Fault
EH_ENTRY(14);
void exception_14_handler(RegisterDump regs)
{
    ASSERT(current);

    u32 fault_address;
    asm("movl %%cr2, %%eax"
        : "=a"(fault_address));

    u32 fault_page_directory;
    asm("movl %%cr3, %%eax"
        : "=a"(fault_page_directory));

#ifdef PAGE_FAULT_DEBUG
    dbgprintf("%s(%u): ring%u %s page fault in PD=%x, %s V%08x\n",
        current->process().name().characters(),
        current->pid(),
        regs.cs & 3,
        regs.exception_code & 1 ? "PV" : "NP",
        fault_page_directory,
        regs.exception_code & 2 ? "write" : "read",
        fault_address);
#endif

#ifdef PAGE_FAULT_DEBUG
    dump(regs);
#endif

    bool faulted_in_userspace = (regs.cs & 3) == 3;
    if (faulted_in_userspace && !MM.validate_user_stack(current->process(), VirtualAddress(regs.esp_if_crossRing))) {
        dbgprintf("Invalid stack pointer: %p\n", regs.esp_if_crossRing);
        handle_crash(regs, "Bad stack on page fault", SIGSTKFLT);
        ASSERT_NOT_REACHED();
    }

    auto response = MM.handle_page_fault(PageFault(regs.exception_code, VirtualAddress(fault_address)));

    if (response == PageFaultResponse::ShouldCrash) {
        if (current->has_signal_handler(SIGSEGV)) {
            current->send_urgent_signal_to_self(SIGSEGV);
            return;
        }

        kprintf("\033[31;1m%s(%u:%u) Unrecoverable page fault, %s address %p\033[0m\n",
            current->process().name().characters(),
            current->pid(),
            current->tid(),
            regs.exception_code & 2 ? "write to" : "read from",
            fault_address);

        u32 malloc_scrub_pattern = explode_byte(MALLOC_SCRUB_BYTE);
        u32 free_scrub_pattern = explode_byte(FREE_SCRUB_BYTE);
        if ((fault_address & 0xffff0000) == (malloc_scrub_pattern & 0xffff0000)) {
            kprintf("\033[33;1mNote: Address %p looks like it may be uninitialized malloc() memory\033[0m\n", fault_address);
        } else if ((fault_address & 0xffff0000) == (free_scrub_pattern & 0xffff0000)) {
            kprintf("\033[33;1mNote: Address %p looks like it may be recently free()'d memory\033[0m\n", fault_address);
        } else if (fault_address < 4096) {
            kprintf("\033[33;1mNote: Address %p looks like a possible nullptr dereference\033[0m\n", fault_address);
        }

        handle_crash(regs, "Page Fault", SIGSEGV);
    } else if (response == PageFaultResponse::Continue) {
#ifdef PAGE_FAULT_DEBUG
        dbgprintf("Continuing after resolved page fault\n");
#endif
    } else {
        ASSERT_NOT_REACHED();
    }
}

#define EH(i, msg)                                                    \
    static void _exception##i()                                       \
    {                                                                 \
        kprintf(msg "\n");                                            \
        u32 cr0, cr2, cr3, cr4;                                       \
        asm("movl %%cr0, %%eax"                                       \
            : "=a"(cr0));                                             \
        asm("movl %%cr2, %%eax"                                       \
            : "=a"(cr2));                                             \
        asm("movl %%cr3, %%eax"                                       \
            : "=a"(cr3));                                             \
        asm("movl %%cr4, %%eax"                                       \
            : "=a"(cr4));                                             \
        kprintf("CR0=%x CR2=%x CR3=%x CR4=%x\n", cr0, cr2, cr3, cr4); \
        hang();                                                       \
    }

EH(1, "Debug exception")
EH(2, "Unknown error")
EH(3, "Breakpoint")
EH(4, "Overflow")
EH(5, "Bounds check")
EH(8, "Double fault")
EH(9, "Coprocessor segment overrun")
EH(10, "Invalid TSS")
EH(11, "Segment not present")
EH(12, "Stack exception")
EH(15, "Unknown error")
EH(16, "Coprocessor error")

static void write_raw_gdt_entry(u16 selector, u32 low, u32 high)
{
    u16 i = (selector & 0xfffc) >> 3;
    s_gdt[i].low = low;
    s_gdt[i].high = high;

    if (i > s_gdt_length)
        s_gdtr.limit = (s_gdt_length + 1) * 8 - 1;
}

void write_gdt_entry(u16 selector, Descriptor& descriptor)
{
    write_raw_gdt_entry(selector, descriptor.low, descriptor.high);
}

Descriptor& get_gdt_entry(u16 selector)
{
    u16 i = (selector & 0xfffc) >> 3;
    return *(Descriptor*)(&s_gdt[i]);
}

void flush_gdt()
{
    s_gdtr.address = s_gdt;
    s_gdtr.limit = (s_gdt_length * 8) - 1;
    asm("lgdt %0" ::"m"(s_gdtr)
        : "memory");
}

void gdt_init()
{
    s_gdt_length = 5;

    s_gdt_freelist = new Vector<u16>();
    s_gdt_freelist->ensure_capacity(256);
    for (size_t i = s_gdt_length; i < 256; ++i)
        s_gdt_freelist->append(i * 8);

    s_gdt_length = 256;
    s_gdtr.address = s_gdt;
    s_gdtr.limit = (s_gdt_length * 8) - 1;

    write_raw_gdt_entry(0x0000, 0x00000000, 0x00000000);
    write_raw_gdt_entry(0x0008, 0x0000ffff, 0x00cf9a00);
    write_raw_gdt_entry(0x0010, 0x0000ffff, 0x00cf9200);
    write_raw_gdt_entry(0x0018, 0x0000ffff, 0x00cffa00);
    write_raw_gdt_entry(0x0020, 0x0000ffff, 0x00cff200);

    flush_gdt();

    asm volatile(
        "mov %%ax, %%ds\n"
        "mov %%ax, %%es\n"
        "mov %%ax, %%fs\n"
        "mov %%ax, %%gs\n"
        "mov %%ax, %%ss\n" ::"a"(0x10)
        : "memory");

    // Make sure CS points to the kernel code descriptor.
    asm volatile(
        "ljmpl $0x8, $sanity\n"
        "sanity:\n");
}

static void unimp_trap()
{
    kprintf("Unhandled IRQ.");
    hang();
}

void register_irq_handler(u8 irq, IRQHandler& handler)
{
    ASSERT(!s_irq_handler[irq]);
    s_irq_handler[irq] = &handler;
    register_interrupt_handler(IRQ_VECTOR_BASE + irq, asm_irq_entry);
}

void unregister_irq_handler(u8 irq, IRQHandler& handler)
{
    ASSERT(s_irq_handler[irq] == &handler);
    s_irq_handler[irq] = nullptr;
}

void register_interrupt_handler(u8 index, void (*f)())
{
    s_idt[index].low = 0x00080000 | LSW((f));
    s_idt[index].high = ((u32)(f)&0xffff0000) | 0x8e00;
    flush_idt();
}

void register_user_callable_interrupt_handler(u8 index, void (*f)())
{
    s_idt[index].low = 0x00080000 | LSW((f));
    s_idt[index].high = ((u32)(f)&0xffff0000) | 0xef00;
    flush_idt();
}

void flush_idt()
{
    asm("lidt %0" ::"m"(s_idtr));
}

extern "C" void irq7_handler();
asm(
    ".globl irq7_handler\n"
    "irq7_handler:\n"
    "   iret\n");

extern "C" void irq15_handler();
asm(
    ".globl irq15_handler\n"
    "irq15_handler:\n"
    "   iret\n");

void idt_init()
{
    s_idtr.address = s_idt;
    s_idtr.limit = 0x100 * 8 - 1;

    for (u8 i = 0xff; i > 0x10; --i)
        register_interrupt_handler(i, unimp_trap);

    register_interrupt_handler(0x00, exception_0_entry);
    register_interrupt_handler(0x01, _exception1);
    register_interrupt_handler(0x02, _exception2);
    register_interrupt_handler(0x03, _exception3);
    register_interrupt_handler(0x04, _exception4);
    register_interrupt_handler(0x05, _exception5);
    register_interrupt_handler(0x06, exception_6_entry);
    register_interrupt_handler(0x07, exception_7_entry);
    register_interrupt_handler(0x08, _exception8);
    register_interrupt_handler(0x09, _exception9);
    register_interrupt_handler(0x0a, _exception10);
    register_interrupt_handler(0x0b, _exception11);
    register_interrupt_handler(0x0c, _exception12);
    register_interrupt_handler(0x0d, exception_13_entry);
    register_interrupt_handler(0x0e, exception_14_entry);
    register_interrupt_handler(0x0f, _exception15);
    register_interrupt_handler(0x10, _exception16);

    register_interrupt_handler(0x57, irq7_handler);
    register_interrupt_handler(0x5f, irq15_handler);

    for (u8 i = 0; i < 16; ++i) {
        s_irq_handler[i] = nullptr;
    }

    flush_idt();
}

void load_task_register(u16 selector)
{
    asm("ltr %0" ::"r"(selector));
}

void handle_irq()
{
    u16 isr = PIC::get_isr();
    if (!isr) {
        kprintf("Spurious IRQ\n");
        return;
    }

    u8 irq = 0;
    for (u8 i = 0; i < 16; ++i) {
        if (i == 2)
            continue;
        if (isr & (1 << i)) {
            irq = i;
            break;
        }
    }

    if (s_irq_handler[irq])
        s_irq_handler[irq]->handle_irq();
    PIC::eoi(irq);
}

#ifdef DEBUG
void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func)
{
    asm volatile("cli");
    kprintf("ASSERTION FAILED: %s\n%s:%u in %s\n", msg, file, line, func);
    dump_backtrace();
    asm volatile("hlt");
    for (;;)
        ;
}
#endif

void sse_init()
{
    asm volatile(
        "mov %cr0, %eax\n"
        "andl $0xfffffffb, %eax\n"
        "orl $0x2, %eax\n"
        "mov %eax, %cr0\n"
        "mov %cr4, %eax\n"
        "orl $0x600, %eax\n"
        "mov %eax, %cr4\n");
}
