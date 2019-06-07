#include "i386.h"
#include "Assertions.h"
#include "IRQHandler.h"
#include "PIC.h"
#include "Process.h"
#include "Scheduler.h"
#include <AK/Types.h>
#include <Kernel/KSyms.h>
#include <Kernel/VM/MemoryManager.h>

//#define PAGE_FAULT_DEBUG

struct [[gnu::packed]] DescriptorTablePointer
{
    word limit;
    void* address;
};

static DescriptorTablePointer s_idtr;
static DescriptorTablePointer s_gdtr;
static Descriptor s_idt[256];
static Descriptor s_gdt[256];

static IRQHandler* s_irq_handler[16];

static Vector<word>* s_gdt_freelist;

static word s_gdt_length;

word gdt_alloc_entry()
{
    ASSERT(s_gdt_freelist);
    ASSERT(!s_gdt_freelist->is_empty());
    return s_gdt_freelist->take_last();
}

void gdt_free_entry(word entry)
{
    s_gdt_freelist->append(entry);
}

extern "C" void handle_irq();
extern "C" void asm_irq_entry();

asm(
    ".globl asm_irq_entry\n"
    "asm_irq_entry: \n"
    "    pusha\n"
    "    pushw %ds\n"
    "    pushw %es\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    popw %ds\n"
    "    popw %es\n"
    "    call handle_irq\n"
    "    popw %es\n"
    "    popw %ds\n"
    "    popa\n"
    "    iret\n");

#define EH_ENTRY(ec)                                                          \
    extern "C" void exception_##ec##_handler(RegisterDumpWithExceptionCode&); \
    extern "C" void exception_##ec##_entry();                                 \
    asm(                                                                      \
        ".globl exception_" #ec "_entry\n"                                    \
        "exception_" #ec "_entry: \n"                                         \
        "    pusha\n"                                                         \
        "    pushw %ds\n"                                                     \
        "    pushw %es\n"                                                     \
        "    pushw %fs\n"                                                     \
        "    pushw %gs\n"                                                     \
        "    pushw %ss\n"                                                     \
        "    pushw %ss\n"                                                     \
        "    pushw %ss\n"                                                     \
        "    pushw %ss\n"                                                     \
        "    pushw %ss\n"                                                     \
        "    popw %ds\n"                                                      \
        "    popw %es\n"                                                      \
        "    popw %fs\n"                                                      \
        "    popw %gs\n"                                                      \
        "    mov %esp, %eax\n"                                                \
        "    call exception_" #ec "_handler\n"                                \
        "    popw %gs\n"                                                      \
        "    popw %gs\n"                                                      \
        "    popw %fs\n"                                                      \
        "    popw %es\n"                                                      \
        "    popw %ds\n"                                                      \
        "    popa\n"                                                          \
        "    add $0x4, %esp\n"                                                \
        "    iret\n");

#define EH_ENTRY_NO_CODE(ec)                                 \
    extern "C" void exception_##ec##_handler(RegisterDump&); \
    extern "C" void exception_##ec##_entry();                \
    asm(                                                     \
        ".globl exception_" #ec "_entry\n"                   \
        "exception_" #ec "_entry: \n"                        \
        "    pusha\n"                                        \
        "    pushw %ds\n"                                    \
        "    pushw %es\n"                                    \
        "    pushw %fs\n"                                    \
        "    pushw %gs\n"                                    \
        "    pushw %ss\n"                                    \
        "    pushw %ss\n"                                    \
        "    pushw %ss\n"                                    \
        "    pushw %ss\n"                                    \
        "    pushw %ss\n"                                    \
        "    popw %ds\n"                                     \
        "    popw %es\n"                                     \
        "    popw %fs\n"                                     \
        "    popw %gs\n"                                     \
        "    mov %esp, %eax\n"                               \
        "    call exception_" #ec "_handler\n"               \
        "    popw %gs\n"                                     \
        "    popw %gs\n"                                     \
        "    popw %fs\n"                                     \
        "    popw %es\n"                                     \
        "    popw %ds\n"                                     \
        "    popa\n"                                         \
        "    iret\n");

template<typename DumpType>
static void dump(const DumpType& regs)
{
    word ss;
    dword esp;
    if (!current || current->process().is_ring0()) {
        ss = regs.ds;
        esp = regs.esp;
    } else {
        ss = regs.ss_if_crossRing;
        esp = regs.esp_if_crossRing;
    }

    if constexpr (IsSame<DumpType, RegisterDumpWithExceptionCode>::value) {
        kprintf("exception code: %w\n", regs.exception_code);
    }
    kprintf("  pc=%w:%x ds=%w es=%w fs=%w gs=%w\n", regs.cs, regs.eip, regs.ds, regs.es, regs.fs, regs.gs);
    kprintf(" stk=%w:%x\n", ss, esp);
    if (current)
        kprintf("kstk=%w:%x, base=%x, sigbase=%x\n", current->tss().ss0, current->tss().esp0, current->kernel_stack_base(), current->kernel_stack_for_signal_handler_base());
    kprintf("eax=%x ebx=%x ecx=%x edx=%x\n", regs.eax, regs.ebx, regs.ecx, regs.edx);
    kprintf("ebp=%x esp=%x esi=%x edi=%x\n", regs.ebp, esp, regs.esi, regs.edi);

    if (current && current->process().validate_read((void*)regs.eip, 8)) {
        byte* codeptr = (byte*)regs.eip;
        kprintf("code: %b %b %b %b %b %b %b %b\n",
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

// 6: Invalid Opcode
EH_ENTRY_NO_CODE(6);
void exception_6_handler(RegisterDump& regs)
{
    if (!current) {
        kprintf("#UD with !current\n");
        hang();
    }

    kprintf("%s Illegal instruction: %s(%u)\n",
        current->process().is_ring0() ? "Kernel" : "Process",
        current->process().name().characters(),
        current->pid());

    dump(regs);
    dump_backtrace();

    if (current->process().is_ring0()) {
        kprintf("Oh shit, we've crashed in ring 0 :(\n");
        hang();
    }

    current->process().crash(SIGILL);
}

// 7: FPU not available exception
EH_ENTRY_NO_CODE(7);
void exception_7_handler(RegisterDump& regs)
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
        current->set_has_used_fpu(true);
    }

#ifdef FPU_EXCEPTION_DEBUG
    kprintf("%s FPU not available exception: %u(%s)\n", current->process().is_ring0() ? "Kernel" : "Process", current->pid(), current->process().name().characters());
    dump(regs);
#endif
}

// 0: Divide error
EH_ENTRY_NO_CODE(0);
void exception_0_handler(RegisterDump& regs)
{
    kprintf("%s Division by zero: %s(%u)\n",
        current->process().is_ring0() ? "Kernel" : "User",
        current->process().name().characters(),
        current->pid());

    dump(regs);

    if (current->process().is_ring0()) {
        kprintf("Oh shit, we've crashed in ring 0 :(\n");
        hang();
    }

    current->process().crash(SIGFPE);
}

// 13: General Protection Fault
EH_ENTRY(13);
void exception_13_handler(RegisterDumpWithExceptionCode& regs)
{
    kprintf("%s GPF: %u(%s)\n", current->process().is_ring0() ? "Kernel" : "User", current->pid(), current->process().name().characters());

    dump(regs);

    if (current->process().is_ring0()) {
        kprintf("Oh shit, we've crashed in ring 0 :(\n");
        hang();
    }

    current->process().crash();
}

// 14: Page Fault
EH_ENTRY(14);
void exception_14_handler(RegisterDumpWithExceptionCode& regs)
{
    ASSERT(current);

    dword faultAddress;
    asm("movl %%cr2, %%eax"
        : "=a"(faultAddress));

    dword fault_page_directory;
    asm("movl %%cr3, %%eax"
        : "=a"(fault_page_directory));

#ifdef PAGE_FAULT_DEBUG
    dbgprintf("%s(%u): ring%u %s page fault in PD=%x, %s L%x\n",
        current->process().name().characters(),
        current->pid(),
        regs.cs & 3,
        regs.exception_code & 1 ? "PV" : "NP",
        fault_page_directory,
        regs.exception_code & 2 ? "write" : "read",
        faultAddress);
#endif

#ifdef PAGE_FAULT_DEBUG
    dump(regs);
#endif

    auto response = MM.handle_page_fault(PageFault(regs.exception_code, VirtualAddress(faultAddress)));

    if (response == PageFaultResponse::ShouldCrash) {
        kprintf("%s(%u:%u) unrecoverable page fault, %s vaddr=%p\n",
            current->process().name().characters(),
            current->pid(),
            current->tid(),
            regs.exception_code & 2 ? "write" : "read",
            faultAddress);
        dump(regs);
        current->process().crash();
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
        dword cr0, cr2, cr3, cr4;                                     \
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

static void write_raw_gdt_entry(word selector, dword low, dword high)
{
    word i = (selector & 0xfffc) >> 3;
    s_gdt[i].low = low;
    s_gdt[i].high = high;

    if (i > s_gdt_length)
        s_gdtr.limit = (s_gdt_length + 1) * 8 - 1;
}

void write_gdt_entry(word selector, Descriptor& descriptor)
{
    write_raw_gdt_entry(selector, descriptor.low, descriptor.high);
}

Descriptor& get_gdt_entry(word selector)
{
    word i = (selector & 0xfffc) >> 3;
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

    s_gdt_freelist = new Vector<word>();
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

void register_irq_handler(byte irq, IRQHandler& handler)
{
    ASSERT(!s_irq_handler[irq]);
    s_irq_handler[irq] = &handler;
    register_interrupt_handler(IRQ_VECTOR_BASE + irq, asm_irq_entry);
}

void unregister_irq_handler(byte irq, IRQHandler& handler)
{
    ASSERT(s_irq_handler[irq] == &handler);
    s_irq_handler[irq] = nullptr;
}

void register_interrupt_handler(byte index, void (*f)())
{
    s_idt[index].low = 0x00080000 | LSW((f));
    s_idt[index].high = ((dword)(f)&0xffff0000) | 0x8e00;
    flush_idt();
}

void register_user_callable_interrupt_handler(byte index, void (*f)())
{
    s_idt[index].low = 0x00080000 | LSW((f));
    s_idt[index].high = ((dword)(f)&0xffff0000) | 0xef00;
    flush_idt();
}

void flush_idt()
{
    asm("lidt %0" ::"m"(s_idtr));
}

/* If an 8259 gets cranky, it'll generate a spurious IRQ7.
 * ATM I don't have a clear grasp on when/why this happens,
 * so I ignore them and assume it makes no difference.
 */

extern "C" void irq7_handler();
asm(
    ".globl irq7_handler \n"
    "irq7_handler: \n"
    "   iret\n");

void idt_init()
{
    s_idtr.address = s_idt;
    s_idtr.limit = 0x100 * 8 - 1;

    for (byte i = 0xff; i > 0x10; --i)
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

    for (byte i = 0; i < 16; ++i) {
        s_irq_handler[i] = nullptr;
    }

    flush_idt();
}

void load_task_register(word selector)
{
    asm("ltr %0" ::"r"(selector));
}

void handle_irq()
{
    word isr = PIC::get_isr();
    if (!isr) {
        kprintf("Spurious IRQ\n");
        return;
    }

    byte irq = 0;
    for (byte i = 0; i < 16; ++i) {
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
