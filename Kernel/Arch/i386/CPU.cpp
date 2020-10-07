/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/ISRStubs.h>
#include <Kernel/Arch/i386/ProcessorInfo.h>
#include <Kernel/IO.h>
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/Interrupts/SharedIRQHandler.h>
#include <Kernel/Interrupts/SpuriousInterruptHandler.h>
#include <Kernel/Interrupts/UnhandledInterruptHandler.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/SpinLock.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>
#include <LibC/mallocdefs.h>

//#define PAGE_FAULT_DEBUG
//#define CONTEXT_SWITCH_DEBUG
//#define SMP_DEBUG

namespace Kernel {

static DescriptorTablePointer s_idtr;
static Descriptor s_idt[256];

static GenericInterruptHandler* s_interrupt_handler[GENERIC_INTERRUPT_HANDLERS_COUNT];

// The compiler can't see the calls to these functions inside assembly.
// Declare them, to avoid dead code warnings.
extern "C" void enter_thread_context(Thread* from_thread, Thread* to_thread);
extern "C" void context_first_init(Thread* from_thread, Thread* to_thread, TrapFrame* trap);
extern "C" u32 do_init_context(Thread* thread, u32 flags);
extern "C" void pre_init_finished(void);
extern "C" void post_init_finished(void);
extern "C" void handle_interrupt(TrapFrame*);

#define EH_ENTRY(ec, title)                         \
    extern "C" void title##_asm_entry();            \
    extern "C" void title##_handler(TrapFrame*); \
    asm(                                            \
        ".globl " #title "_asm_entry\n"             \
        "" #title "_asm_entry: \n"                  \
        "    pusha\n"                               \
        "    pushl %ds\n"                           \
        "    pushl %es\n"                           \
        "    pushl %fs\n"                           \
        "    pushl %gs\n"                           \
        "    pushl %ss\n"                           \
        "    mov $" __STRINGIFY(GDT_SELECTOR_DATA0) ", %ax\n" \
        "    mov %ax, %ds\n"                        \
        "    mov %ax, %es\n"                        \
        "    mov $" __STRINGIFY(GDT_SELECTOR_PROC) ", %ax\n" \
        "    mov %ax, %fs\n"                        \
        "    pushl %esp \n" /* set TrapFrame::regs */ \
        "    subl $" __STRINGIFY(TRAP_FRAME_SIZE - 4) ", %esp \n" \
        "    pushl %esp \n"                         \
        "    cld\n"                                 \
        "    call enter_trap_no_irq \n"             \
        "    call " #title "_handler\n"             \
        "    jmp common_trap_exit \n");

#define EH_ENTRY_NO_CODE(ec, title)                 \
    extern "C" void title##_handler(TrapFrame*); \
    extern "C" void title##_asm_entry();            \
    asm(                                            \
        ".globl " #title "_asm_entry\n"             \
        "" #title "_asm_entry: \n"                  \
        "    pushl $0x0\n"                          \
        "    pusha\n"                               \
        "    pushl %ds\n"                           \
        "    pushl %es\n"                           \
        "    pushl %fs\n"                           \
        "    pushl %gs\n"                           \
        "    pushl %ss\n"                           \
        "    mov $" __STRINGIFY(GDT_SELECTOR_DATA0) ", %ax\n" \
        "    mov %ax, %ds\n"                        \
        "    mov %ax, %es\n"                        \
        "    mov $" __STRINGIFY(GDT_SELECTOR_PROC) ", %ax\n" \
        "    mov %ax, %fs\n"                        \
        "    pushl %esp \n" /* set TrapFrame::regs */ \
        "    subl $" __STRINGIFY(TRAP_FRAME_SIZE - 4) ", %esp \n" \
        "    pushl %esp \n"                         \
        "    cld\n"                                 \
        "    call enter_trap_no_irq \n"             \
        "    call " #title "_handler\n"             \
        "    jmp common_trap_exit \n");

static void dump(const RegisterState& regs)
{
    u16 ss;
    u32 esp;

    if (!(regs.cs & 3)) {
        ss = regs.ss;
        esp = regs.esp;
    } else {
        ss = regs.userspace_ss;
        esp = regs.userspace_esp;
    }

    klog() << "exception code: " << String::format("%04x", regs.exception_code) << " (isr: " << String::format("%04x", regs.isr_number);
    klog() << "  pc=" << String::format("%04x", (u16)regs.cs) << ":" << String::format("%08x", regs.eip) << " flags=" << String::format("%04x", (u16)regs.eflags);
    klog() << " stk=" << String::format("%04x", ss) << ":" << String::format("%08x", esp);
    klog() << "  ds=" << String::format("%04x", (u16)regs.ds) << " es=" << String::format("%04x", (u16)regs.es) << " fs=" << String::format("%04x", (u16)regs.fs) << " gs=" << String::format("%04x", (u16)regs.gs);
    klog() << "eax=" << String::format("%08x", regs.eax) << " ebx=" << String::format("%08x", regs.ebx) << " ecx=" << String::format("%08x", regs.ecx) << " edx=" << String::format("%08x", regs.edx);
    klog() << "ebp=" << String::format("%08x", regs.ebp) << " esp=" << String::format("%08x", regs.esp) << " esi=" << String::format("%08x", regs.esi) << " edi=" << String::format("%08x", regs.edi);
    u32 cr0;
    asm("movl %%cr0, %%eax"
        : "=a"(cr0));
    u32 cr2;
    asm("movl %%cr2, %%eax"
        : "=a"(cr2));
    u32 cr3 = read_cr3();
    u32 cr4;
    asm("movl %%cr4, %%eax"
        : "=a"(cr4));
    klog() << "cr0=" << String::format("%08x", cr0) << " cr2=" << String::format("%08x", cr2) << " cr3=" << String::format("%08x", cr3) << " cr4=" << String::format("%08x", cr4);

    auto process = Process::current();
    u8 code[8];
    void* fault_at;
    if (process && safe_memcpy(code, (void*)regs.eip, 8, fault_at)) {
        SmapDisabler disabler;
        klog() << "code: " << String::format("%02x", code[0]) << " " << String::format("%02x", code[1]) << " " << String::format("%02x", code[2]) << " " << String::format("%02x", code[3]) << " " << String::format("%02x", code[4]) << " " << String::format("%02x", code[5]) << " " << String::format("%02x", code[6]) << " " << String::format("%02x", code[7]);
    }
}

void handle_crash(RegisterState& regs, const char* description, int signal, bool out_of_memory)
{
    auto process = Process::current();
    if (!process) {
        klog() << description << " with !current";
        Processor::halt();
    }

    // If a process crashed while inspecting another process,
    // make sure we switch back to the right page tables.
    MM.enter_process_paging_scope(*process);

    klog() << "CRASH: CPU #" << Processor::current().id() << " " << description << ". Ring " << (regs.cs & 3) << ".";
    dump(regs);

    if (!(regs.cs & 3)) {
        klog() << "Crash in ring 0 :(";
        dump_backtrace();
        Processor::halt();
    }

    cli();
    process->crash(signal, regs.eip, out_of_memory);
}

EH_ENTRY_NO_CODE(6, illegal_instruction);
void illegal_instruction_handler(TrapFrame* trap)
{
    clac();
    handle_crash(*trap->regs, "Illegal instruction", SIGILL);
}

EH_ENTRY_NO_CODE(0, divide_error);
void divide_error_handler(TrapFrame* trap)
{
    clac();
    handle_crash(*trap->regs, "Divide error", SIGFPE);
}

EH_ENTRY(13, general_protection_fault);
void general_protection_fault_handler(TrapFrame* trap)
{
    clac();
    handle_crash(*trap->regs, "General protection fault", SIGSEGV);
}

// 7: FPU not available exception
EH_ENTRY_NO_CODE(7, fpu_exception);
void fpu_exception_handler(TrapFrame*)
{
    // Just clear the TS flag. We've already restored the FPU state eagerly.
    // FIXME: It would be nice if we didn't have to do this at all.
    asm volatile("clts");
}

extern "C" u8* safe_memcpy_ins_1;
extern "C" u8* safe_memcpy_1_faulted;
extern "C" u8* safe_memcpy_ins_2;
extern "C" u8* safe_memcpy_2_faulted;
extern "C" u8* safe_strnlen_ins;
extern "C" u8* safe_strnlen_faulted;
extern "C" u8* safe_memset_ins_1;
extern "C" u8* safe_memset_1_faulted;
extern "C" u8* safe_memset_ins_2;
extern "C" u8* safe_memset_2_faulted;

bool safe_memcpy(void* dest_ptr, const void* src_ptr, size_t n, void*& fault_at)
{
    fault_at = nullptr;
    size_t dest = (size_t)dest_ptr;
    size_t src = (size_t)src_ptr;
    size_t remainder;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && !(src & 0x3) && n >= 12) {
        size_t size_ts = n / sizeof(size_t);
        asm volatile(
            ".global safe_memcpy_ins_1 \n"
            "safe_memcpy_ins_1: \n"
            "rep movsl \n"
            ".global safe_memcpy_1_faulted \n"
            "safe_memcpy_1_faulted: \n" // handle_safe_access_fault() set edx to the fault address!
            : "=S" (src),
              "=D" (dest),
              "=c" (remainder),
              [fault_at] "=d" (fault_at)
            : "S" (src),
              "D" (dest),
              "c" (size_ts)
            : "memory");
        if (remainder != 0)
            return false; // fault_at is already set!
        n -= size_ts * sizeof(size_t);
        if (n == 0) {
            fault_at = nullptr;
            return true;
        }
    }
    asm volatile(
        ".global safe_memcpy_ins_2 \n"
        "safe_memcpy_ins_2: \n"
        "rep movsb \n"
        ".global safe_memcpy_2_faulted \n"
        "safe_memcpy_2_faulted: \n" // handle_safe_access_fault() set edx to the fault address!
        : "=c" (remainder),
          [fault_at] "=d" (fault_at)
        : "S" (src),
          "D" (dest),
          "c" (n)
        : "memory");
    if (remainder != 0)
        return false; // fault_at is already set!
    fault_at = nullptr;
    return true;
}

ssize_t safe_strnlen(const char* str, size_t max_n, void*& fault_at)
{
    ssize_t count = 0;
    fault_at = nullptr;
    asm volatile(
        "1: \n"
        "test %[max_n], %[max_n] \n"
        "je 2f \n"
        "dec %[max_n] \n"
        ".global safe_strnlen_ins \n"
        "safe_strnlen_ins: \n"
        "cmpb $0,(%[str], %[count], 1) \n"
        "je 2f \n"
        "inc %[count] \n"
        "jmp 1b \n"
        ".global safe_strnlen_faulted \n"
        "safe_strnlen_faulted: \n" // handle_safe_access_fault() set edx to the fault address!
        "xor %[count_on_error], %[count_on_error] \n"
        "dec %[count_on_error] \n" // return -1 on fault
        "2:"
        : [count_on_error] "=c" (count),
          [fault_at] "=d" (fault_at)
        : [str] "b" (str),
          [count] "c" (count),
          [max_n] "d" (max_n)
    );
    if (count >= 0)
        fault_at = nullptr;
    return count;
}

bool safe_memset(void* dest_ptr, int c, size_t n, void*& fault_at)
{
    fault_at = nullptr;
    size_t dest = (size_t)dest_ptr;
    size_t remainder;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && n >= 12) {
        size_t size_ts = n / sizeof(size_t);
        size_t expanded_c = (u8)c;
        expanded_c |= expanded_c << 8;
        expanded_c |= expanded_c << 16;
        asm volatile(
            ".global safe_memset_ins_1 \n"
            "safe_memset_ins_1: \n"
            "rep stosl \n"
            ".global safe_memset_1_faulted \n"
            "safe_memset_1_faulted: \n" // handle_safe_access_fault() set edx to the fault address!
            : "=D" (dest),
              "=c" (remainder),
              [fault_at] "=d" (fault_at)
            : "D" (dest),
              "a" (expanded_c),
              "c" (size_ts)
            : "memory");
        if (remainder != 0)
            return false; // fault_at is already set!
        n -= size_ts * sizeof(size_t);
        if (remainder == 0) {
            fault_at = nullptr;
            return true;
        }
    }
    asm volatile(
        ".global safe_memset_ins_2 \n"
        "safe_memset_ins_2: \n"
        "rep stosb \n"
        ".global safe_memset_2_faulted \n"
        "safe_memset_2_faulted: \n" // handle_safe_access_fault() set edx to the fault address!
        : "=D" (dest),
          "=c" (remainder),
          [fault_at] "=d" (fault_at)
        : "D" (dest),
          "c" (n),
          "a" (c)
        : "memory");
    if (remainder != 0)
        return false; // fault_at is already set!
    fault_at = nullptr;
    return true;
}

static bool handle_safe_access_fault(RegisterState& regs, u32 fault_address)
{
    // If we detect that the fault happened in safe_memcpy() safe_strnlen(),
    // or safe_memset() then resume at the appropriate _faulted label
    if (regs.eip == (FlatPtr)&safe_memcpy_ins_1)
        regs.eip = (FlatPtr)&safe_memcpy_1_faulted;
    else if (regs.eip == (FlatPtr)&safe_memcpy_ins_2)
        regs.eip = (FlatPtr)&safe_memcpy_2_faulted;
    else if (regs.eip == (FlatPtr)&safe_strnlen_ins)
        regs.eip = (FlatPtr)&safe_strnlen_faulted;
    else if (regs.eip == (FlatPtr)&safe_memset_ins_1)
        regs.eip = (FlatPtr)&safe_memset_1_faulted;
    else if (regs.eip == (FlatPtr)&safe_memset_ins_2)
        regs.eip = (FlatPtr)&safe_memset_2_faulted;
    else
        return false;

    regs.edx = fault_address;
    return true;
}

// 14: Page Fault
EH_ENTRY(14, page_fault);
void page_fault_handler(TrapFrame* trap)
{
    clac();

    auto& regs = *trap->regs;
    u32 fault_address;
    asm("movl %%cr2, %%eax"
        : "=a"(fault_address));

#ifdef PAGE_FAULT_DEBUG
    u32 fault_page_directory = read_cr3();
    dbg() << "CPU #" << (Processor::is_initialized() ? Processor::current().id() : 0) << " ring " << (regs.cs & 3)
          << " " << (regs.exception_code & 1 ? "PV" : "NP")
          << " page fault in PD=" << String::format("%x", fault_page_directory) << ", "
          << (regs.exception_code & 8 ? "reserved-bit " : "")
          << (regs.exception_code & 2 ? "write" : "read")
          << " " << VirtualAddress(fault_address);
#endif

#ifdef PAGE_FAULT_DEBUG
    dump(regs);
#endif

    bool faulted_in_kernel = !(regs.cs & 3);

    if (faulted_in_kernel && Processor::current().in_irq()) {
        // If we're faulting in an IRQ handler, first check if we failed
        // due to safe_memcpy, safe_strnlen, or safe_memset. If we did,
        // gracefully continue immediately. Because we're in an IRQ handler
        // we can't really try to resolve the page fault in a meaningful
        // way, so we need to do this before calling into
        // MemoryManager::handle_page_fault, which would just bail and
        // request a crash
        if (handle_safe_access_fault(regs, fault_address))
            return;
    }

    auto current_thread = Thread::current();
    if (!faulted_in_kernel && !MM.validate_user_stack(current_thread->process(), VirtualAddress(regs.userspace_esp))) {
        dbg() << "Invalid stack pointer: " << VirtualAddress(regs.userspace_esp);
        handle_crash(regs, "Bad stack on page fault", SIGSTKFLT);
        ASSERT_NOT_REACHED();
    }

    auto response = MM.handle_page_fault(PageFault(regs.exception_code, VirtualAddress(fault_address)));

    if (response == PageFaultResponse::ShouldCrash || response == PageFaultResponse::OutOfMemory) {
        if (faulted_in_kernel && handle_safe_access_fault(regs, fault_address)) {
            // If this would be a ring0 (kernel) fault and the fault was triggered by
            // safe_memcpy, safe_strnlen, or safe_memset then we resume execution at
            // the appropriate _fault label rather than crashing
            return;
        }

        if (response != PageFaultResponse::OutOfMemory) {
            if (current_thread->has_signal_handler(SIGSEGV)) {
                current_thread->send_urgent_signal_to_self(SIGSEGV);
                return;
            }
        }

        dbg() << "Unrecoverable page fault, "
              << (regs.exception_code & PageFaultFlags::ReservedBitViolation ? "reserved bit violation / " : "")
              << (regs.exception_code & PageFaultFlags::InstructionFetch ? "instruction fetch / " : "")
              << (regs.exception_code & PageFaultFlags::Write ? "write to" : "read from")
              << " address " << VirtualAddress(fault_address);
        u32 malloc_scrub_pattern = explode_byte(MALLOC_SCRUB_BYTE);
        u32 free_scrub_pattern = explode_byte(FREE_SCRUB_BYTE);
        u32 kmalloc_scrub_pattern = explode_byte(KMALLOC_SCRUB_BYTE);
        u32 kfree_scrub_pattern = explode_byte(KFREE_SCRUB_BYTE);
        u32 slab_alloc_scrub_pattern = explode_byte(SLAB_ALLOC_SCRUB_BYTE);
        u32 slab_dealloc_scrub_pattern = explode_byte(SLAB_DEALLOC_SCRUB_BYTE);
        if ((fault_address & 0xffff0000) == (malloc_scrub_pattern & 0xffff0000)) {
            dbg() << "Note: Address " << VirtualAddress(fault_address) << " looks like it may be uninitialized malloc() memory";
        } else if ((fault_address & 0xffff0000) == (free_scrub_pattern & 0xffff0000)) {
            dbg() << "Note: Address " << VirtualAddress(fault_address) << " looks like it may be recently free()'d memory";
        } else if ((fault_address & 0xffff0000) == (kmalloc_scrub_pattern & 0xffff0000)) {
            dbg() << "Note: Address " << VirtualAddress(fault_address) << " looks like it may be uninitialized kmalloc() memory";
        } else if ((fault_address & 0xffff0000) == (kfree_scrub_pattern & 0xffff0000)) {
            dbg() << "Note: Address " << VirtualAddress(fault_address) << " looks like it may be recently kfree()'d memory";
        } else if ((fault_address & 0xffff0000) == (slab_alloc_scrub_pattern & 0xffff0000)) {
            dbg() << "Note: Address " << VirtualAddress(fault_address) << " looks like it may be uninitialized slab_alloc() memory";
        } else if ((fault_address & 0xffff0000) == (slab_dealloc_scrub_pattern & 0xffff0000)) {
            dbg() << "Note: Address " << VirtualAddress(fault_address) << " looks like it may be recently slab_dealloc()'d memory";
        } else if (fault_address < 4096) {
            dbg() << "Note: Address " << VirtualAddress(fault_address) << " looks like a possible nullptr dereference";
        }

        handle_crash(regs, "Page Fault", SIGSEGV, response == PageFaultResponse::OutOfMemory);
    } else if (response == PageFaultResponse::Continue) {
#ifdef PAGE_FAULT_DEBUG
        dbg() << "Continuing after resolved page fault";
#endif
    } else {
        ASSERT_NOT_REACHED();
    }
}

EH_ENTRY_NO_CODE(1, debug);
void debug_handler(TrapFrame* trap)
{
    clac();
    auto& regs = *trap->regs;
    auto current_thread = Thread::current();
    if (&current_thread->process() == nullptr || (regs.cs & 3) == 0) {
        klog() << "Debug Exception in Ring0";
        Processor::halt();
        return;
    }
    constexpr u8 REASON_SINGLESTEP = 14;
    bool is_reason_singlestep = (read_dr6() & (1 << REASON_SINGLESTEP));
    if (!is_reason_singlestep)
        return;

    if (current_thread->tracer()) {
        current_thread->tracer()->set_regs(regs);
    }
    current_thread->send_urgent_signal_to_self(SIGTRAP);
}

EH_ENTRY_NO_CODE(3, breakpoint);
void breakpoint_handler(TrapFrame* trap)
{
    clac();
    auto& regs = *trap->regs;
    auto current_thread = Thread::current();
    if (&current_thread->process() == nullptr || (regs.cs & 3) == 0) {
        klog() << "Breakpoint Trap in Ring0";
        Processor::halt();
        return;
    }
    if (current_thread->tracer()) {
        current_thread->tracer()->set_regs(regs);
    }
    current_thread->send_urgent_signal_to_self(SIGTRAP);
}

#define EH(i, msg)                                                                                                                                                             \
    static void _exception##i()                                                                                                                                                \
    {                                                                                                                                                                          \
        klog() << msg;                                                                                                                                                         \
        u32 cr0, cr2, cr3, cr4;                                                                                                                                                \
        asm("movl %%cr0, %%eax"                                                                                                                                                \
            : "=a"(cr0));                                                                                                                                                      \
        asm("movl %%cr2, %%eax"                                                                                                                                                \
            : "=a"(cr2));                                                                                                                                                      \
        asm("movl %%cr3, %%eax"                                                                                                                                                \
            : "=a"(cr3));                                                                                                                                                      \
        asm("movl %%cr4, %%eax"                                                                                                                                                \
            : "=a"(cr4));                                                                                                                                                      \
        klog() << "CR0=" << String::format("%x", cr0) << " CR2=" << String::format("%x", cr2) << " CR3=" << String::format("%x", cr3) << " CR4=" << String::format("%x", cr4); \
        Processor::halt();                                                                                                                                                     \
    }

EH(2, "Unknown error")
EH(4, "Overflow")
EH(5, "Bounds check")
EH(8, "Double fault")
EH(9, "Coprocessor segment overrun")
EH(10, "Invalid TSS")
EH(11, "Segment not present")
EH(12, "Stack exception")
EH(15, "Unknown error")
EH(16, "Coprocessor error")

const DescriptorTablePointer& get_idtr()
{
    return s_idtr;
}

static void unimp_trap()
{
    klog() << "Unhandled IRQ.";
    Processor::Processor::halt();
}

GenericInterruptHandler& get_interrupt_handler(u8 interrupt_number)
{
    ASSERT(s_interrupt_handler[interrupt_number] != nullptr);
    return *s_interrupt_handler[interrupt_number];
}

static void revert_to_unused_handler(u8 interrupt_number)
{
    new UnhandledInterruptHandler(interrupt_number);
}

void register_generic_interrupt_handler(u8 interrupt_number, GenericInterruptHandler& handler)
{
    ASSERT(interrupt_number < GENERIC_INTERRUPT_HANDLERS_COUNT);
    if (s_interrupt_handler[interrupt_number] != nullptr) {
        if (s_interrupt_handler[interrupt_number]->type() == HandlerType::UnhandledInterruptHandler) {
            s_interrupt_handler[interrupt_number] = &handler;
            return;
        }
        if (s_interrupt_handler[interrupt_number]->is_shared_handler() && !s_interrupt_handler[interrupt_number]->is_sharing_with_others()) {
            ASSERT(s_interrupt_handler[interrupt_number]->type() == HandlerType::SharedIRQHandler);
            static_cast<SharedIRQHandler*>(s_interrupt_handler[interrupt_number])->register_handler(handler);
            return;
        }
        if (!s_interrupt_handler[interrupt_number]->is_shared_handler()) {
            ASSERT(s_interrupt_handler[interrupt_number]->type() == HandlerType::IRQHandler);
            auto& previous_handler = *s_interrupt_handler[interrupt_number];
            s_interrupt_handler[interrupt_number] = nullptr;
            SharedIRQHandler::initialize(interrupt_number);
            static_cast<SharedIRQHandler*>(s_interrupt_handler[interrupt_number])->register_handler(previous_handler);
            static_cast<SharedIRQHandler*>(s_interrupt_handler[interrupt_number])->register_handler(handler);
            return;
        }
        ASSERT_NOT_REACHED();
    } else {
        s_interrupt_handler[interrupt_number] = &handler;
    }
}

void unregister_generic_interrupt_handler(u8 interrupt_number, GenericInterruptHandler& handler)
{
    ASSERT(s_interrupt_handler[interrupt_number] != nullptr);
    if (s_interrupt_handler[interrupt_number]->type() == HandlerType::UnhandledInterruptHandler) {
        dbg() << "Trying to unregister unused handler (?)";
        return;
    }
    if (s_interrupt_handler[interrupt_number]->is_shared_handler() && !s_interrupt_handler[interrupt_number]->is_sharing_with_others()) {
        ASSERT(s_interrupt_handler[interrupt_number]->type() == HandlerType::SharedIRQHandler);
        static_cast<SharedIRQHandler*>(s_interrupt_handler[interrupt_number])->unregister_handler(handler);
        if (!static_cast<SharedIRQHandler*>(s_interrupt_handler[interrupt_number])->sharing_devices_count()) {
            revert_to_unused_handler(interrupt_number);
        }
        return;
    }
    if (!s_interrupt_handler[interrupt_number]->is_shared_handler()) {
        ASSERT(s_interrupt_handler[interrupt_number]->type() == HandlerType::IRQHandler);
        revert_to_unused_handler(interrupt_number);
        return;
    }
    ASSERT_NOT_REACHED();
}

void register_interrupt_handler(u8 index, void (*f)())
{
    s_idt[index].low = 0x00080000 | LSW((f));
    s_idt[index].high = ((u32)(f)&0xffff0000) | 0x8e00;
}

void register_user_callable_interrupt_handler(u8 index, void (*f)())
{
    s_idt[index].low = 0x00080000 | LSW((f));
    s_idt[index].high = ((u32)(f)&0xffff0000) | 0xef00;
}

void flush_idt()
{
    asm("lidt %0" ::"m"(s_idtr));
}

static void idt_init()
{
    s_idtr.address = s_idt;
    s_idtr.limit = 256 * 8 - 1;

    register_interrupt_handler(0x00, divide_error_asm_entry);
    register_user_callable_interrupt_handler(0x01, debug_asm_entry);
    register_interrupt_handler(0x02, _exception2);
    register_user_callable_interrupt_handler(0x03, breakpoint_asm_entry);
    register_interrupt_handler(0x04, _exception4);
    register_interrupt_handler(0x05, _exception5);
    register_interrupt_handler(0x06, illegal_instruction_asm_entry);
    register_interrupt_handler(0x07, fpu_exception_asm_entry);
    register_interrupt_handler(0x08, _exception8);
    register_interrupt_handler(0x09, _exception9);
    register_interrupt_handler(0x0a, _exception10);
    register_interrupt_handler(0x0b, _exception11);
    register_interrupt_handler(0x0c, _exception12);
    register_interrupt_handler(0x0d, general_protection_fault_asm_entry);
    register_interrupt_handler(0x0e, page_fault_asm_entry);
    register_interrupt_handler(0x0f, _exception15);
    register_interrupt_handler(0x10, _exception16);

    for (u8 i = 0x11; i < 0x50; i++)
        register_interrupt_handler(i, unimp_trap);

    register_interrupt_handler(0x50, interrupt_80_asm_entry);
    register_interrupt_handler(0x51, interrupt_81_asm_entry);
    register_interrupt_handler(0x52, interrupt_82_asm_entry);
    register_interrupt_handler(0x53, interrupt_83_asm_entry);
    register_interrupt_handler(0x54, interrupt_84_asm_entry);
    register_interrupt_handler(0x55, interrupt_85_asm_entry);
    register_interrupt_handler(0x56, interrupt_86_asm_entry);
    register_interrupt_handler(0x57, interrupt_87_asm_entry);
    register_interrupt_handler(0x58, interrupt_88_asm_entry);
    register_interrupt_handler(0x59, interrupt_89_asm_entry);
    register_interrupt_handler(0x5a, interrupt_90_asm_entry);
    register_interrupt_handler(0x5b, interrupt_91_asm_entry);
    register_interrupt_handler(0x5c, interrupt_92_asm_entry);
    register_interrupt_handler(0x5d, interrupt_93_asm_entry);
    register_interrupt_handler(0x5e, interrupt_94_asm_entry);
    register_interrupt_handler(0x5f, interrupt_95_asm_entry);
    register_interrupt_handler(0x60, interrupt_96_asm_entry);
    register_interrupt_handler(0x61, interrupt_97_asm_entry);
    register_interrupt_handler(0x62, interrupt_98_asm_entry);
    register_interrupt_handler(0x63, interrupt_99_asm_entry);
    register_interrupt_handler(0x64, interrupt_100_asm_entry);
    register_interrupt_handler(0x65, interrupt_101_asm_entry);
    register_interrupt_handler(0x66, interrupt_102_asm_entry);
    register_interrupt_handler(0x67, interrupt_103_asm_entry);
    register_interrupt_handler(0x68, interrupt_104_asm_entry);
    register_interrupt_handler(0x69, interrupt_105_asm_entry);
    register_interrupt_handler(0x6a, interrupt_106_asm_entry);
    register_interrupt_handler(0x6b, interrupt_107_asm_entry);
    register_interrupt_handler(0x6c, interrupt_108_asm_entry);
    register_interrupt_handler(0x6d, interrupt_109_asm_entry);
    register_interrupt_handler(0x6e, interrupt_110_asm_entry);
    register_interrupt_handler(0x6f, interrupt_111_asm_entry);
    register_interrupt_handler(0x70, interrupt_112_asm_entry);
    register_interrupt_handler(0x71, interrupt_113_asm_entry);
    register_interrupt_handler(0x72, interrupt_114_asm_entry);
    register_interrupt_handler(0x73, interrupt_115_asm_entry);
    register_interrupt_handler(0x74, interrupt_116_asm_entry);
    register_interrupt_handler(0x75, interrupt_117_asm_entry);
    register_interrupt_handler(0x76, interrupt_118_asm_entry);
    register_interrupt_handler(0x77, interrupt_119_asm_entry);
    register_interrupt_handler(0x78, interrupt_120_asm_entry);
    register_interrupt_handler(0x79, interrupt_121_asm_entry);
    register_interrupt_handler(0x7a, interrupt_122_asm_entry);
    register_interrupt_handler(0x7b, interrupt_123_asm_entry);
    register_interrupt_handler(0x7c, interrupt_124_asm_entry);
    register_interrupt_handler(0x7d, interrupt_125_asm_entry);
    register_interrupt_handler(0x7e, interrupt_126_asm_entry);
    register_interrupt_handler(0x7f, interrupt_127_asm_entry);
    register_interrupt_handler(0x80, interrupt_128_asm_entry);
    register_interrupt_handler(0x81, interrupt_129_asm_entry);
    register_interrupt_handler(0x82, interrupt_130_asm_entry);
    register_interrupt_handler(0x83, interrupt_131_asm_entry);
    register_interrupt_handler(0x84, interrupt_132_asm_entry);
    register_interrupt_handler(0x85, interrupt_133_asm_entry);
    register_interrupt_handler(0x86, interrupt_134_asm_entry);
    register_interrupt_handler(0x87, interrupt_135_asm_entry);
    register_interrupt_handler(0x88, interrupt_136_asm_entry);
    register_interrupt_handler(0x89, interrupt_137_asm_entry);
    register_interrupt_handler(0x8a, interrupt_138_asm_entry);
    register_interrupt_handler(0x8b, interrupt_139_asm_entry);
    register_interrupt_handler(0x8c, interrupt_140_asm_entry);
    register_interrupt_handler(0x8d, interrupt_141_asm_entry);
    register_interrupt_handler(0x8e, interrupt_142_asm_entry);
    register_interrupt_handler(0x8f, interrupt_143_asm_entry);
    register_interrupt_handler(0x90, interrupt_144_asm_entry);
    register_interrupt_handler(0x91, interrupt_145_asm_entry);
    register_interrupt_handler(0x92, interrupt_146_asm_entry);
    register_interrupt_handler(0x93, interrupt_147_asm_entry);
    register_interrupt_handler(0x94, interrupt_148_asm_entry);
    register_interrupt_handler(0x95, interrupt_149_asm_entry);
    register_interrupt_handler(0x96, interrupt_150_asm_entry);
    register_interrupt_handler(0x97, interrupt_151_asm_entry);
    register_interrupt_handler(0x98, interrupt_152_asm_entry);
    register_interrupt_handler(0x99, interrupt_153_asm_entry);
    register_interrupt_handler(0x9a, interrupt_154_asm_entry);
    register_interrupt_handler(0x9b, interrupt_155_asm_entry);
    register_interrupt_handler(0x9c, interrupt_156_asm_entry);
    register_interrupt_handler(0x9d, interrupt_157_asm_entry);
    register_interrupt_handler(0x9e, interrupt_158_asm_entry);
    register_interrupt_handler(0x9f, interrupt_159_asm_entry);
    register_interrupt_handler(0xa0, interrupt_160_asm_entry);
    register_interrupt_handler(0xa1, interrupt_161_asm_entry);
    register_interrupt_handler(0xa2, interrupt_162_asm_entry);
    register_interrupt_handler(0xa3, interrupt_163_asm_entry);
    register_interrupt_handler(0xa4, interrupt_164_asm_entry);
    register_interrupt_handler(0xa5, interrupt_165_asm_entry);
    register_interrupt_handler(0xa6, interrupt_166_asm_entry);
    register_interrupt_handler(0xa7, interrupt_167_asm_entry);
    register_interrupt_handler(0xa8, interrupt_168_asm_entry);
    register_interrupt_handler(0xa9, interrupt_169_asm_entry);
    register_interrupt_handler(0xaa, interrupt_170_asm_entry);
    register_interrupt_handler(0xab, interrupt_171_asm_entry);
    register_interrupt_handler(0xac, interrupt_172_asm_entry);
    register_interrupt_handler(0xad, interrupt_173_asm_entry);
    register_interrupt_handler(0xae, interrupt_174_asm_entry);
    register_interrupt_handler(0xaf, interrupt_175_asm_entry);
    register_interrupt_handler(0xb0, interrupt_176_asm_entry);
    register_interrupt_handler(0xb1, interrupt_177_asm_entry);
    register_interrupt_handler(0xb2, interrupt_178_asm_entry);
    register_interrupt_handler(0xb3, interrupt_179_asm_entry);
    register_interrupt_handler(0xb4, interrupt_180_asm_entry);
    register_interrupt_handler(0xb5, interrupt_181_asm_entry);
    register_interrupt_handler(0xb6, interrupt_182_asm_entry);
    register_interrupt_handler(0xb7, interrupt_183_asm_entry);
    register_interrupt_handler(0xb8, interrupt_184_asm_entry);
    register_interrupt_handler(0xb9, interrupt_185_asm_entry);
    register_interrupt_handler(0xba, interrupt_186_asm_entry);
    register_interrupt_handler(0xbb, interrupt_187_asm_entry);
    register_interrupt_handler(0xbc, interrupt_188_asm_entry);
    register_interrupt_handler(0xbd, interrupt_189_asm_entry);
    register_interrupt_handler(0xbe, interrupt_190_asm_entry);
    register_interrupt_handler(0xbf, interrupt_191_asm_entry);
    register_interrupt_handler(0xc0, interrupt_192_asm_entry);
    register_interrupt_handler(0xc1, interrupt_193_asm_entry);
    register_interrupt_handler(0xc2, interrupt_194_asm_entry);
    register_interrupt_handler(0xc3, interrupt_195_asm_entry);
    register_interrupt_handler(0xc4, interrupt_196_asm_entry);
    register_interrupt_handler(0xc5, interrupt_197_asm_entry);
    register_interrupt_handler(0xc6, interrupt_198_asm_entry);
    register_interrupt_handler(0xc7, interrupt_199_asm_entry);
    register_interrupt_handler(0xc8, interrupt_200_asm_entry);
    register_interrupt_handler(0xc9, interrupt_201_asm_entry);
    register_interrupt_handler(0xca, interrupt_202_asm_entry);
    register_interrupt_handler(0xcb, interrupt_203_asm_entry);
    register_interrupt_handler(0xcc, interrupt_204_asm_entry);
    register_interrupt_handler(0xcd, interrupt_205_asm_entry);
    register_interrupt_handler(0xce, interrupt_206_asm_entry);
    register_interrupt_handler(0xcf, interrupt_207_asm_entry);
    register_interrupt_handler(0xd0, interrupt_208_asm_entry);
    register_interrupt_handler(0xd1, interrupt_209_asm_entry);
    register_interrupt_handler(0xd2, interrupt_210_asm_entry);
    register_interrupt_handler(0xd3, interrupt_211_asm_entry);
    register_interrupt_handler(0xd4, interrupt_212_asm_entry);
    register_interrupt_handler(0xd5, interrupt_213_asm_entry);
    register_interrupt_handler(0xd6, interrupt_214_asm_entry);
    register_interrupt_handler(0xd7, interrupt_215_asm_entry);
    register_interrupt_handler(0xd8, interrupt_216_asm_entry);
    register_interrupt_handler(0xd9, interrupt_217_asm_entry);
    register_interrupt_handler(0xda, interrupt_218_asm_entry);
    register_interrupt_handler(0xdb, interrupt_219_asm_entry);
    register_interrupt_handler(0xdc, interrupt_220_asm_entry);
    register_interrupt_handler(0xdd, interrupt_221_asm_entry);
    register_interrupt_handler(0xde, interrupt_222_asm_entry);
    register_interrupt_handler(0xdf, interrupt_223_asm_entry);
    register_interrupt_handler(0xe0, interrupt_224_asm_entry);
    register_interrupt_handler(0xe1, interrupt_225_asm_entry);
    register_interrupt_handler(0xe2, interrupt_226_asm_entry);
    register_interrupt_handler(0xe3, interrupt_227_asm_entry);
    register_interrupt_handler(0xe4, interrupt_228_asm_entry);
    register_interrupt_handler(0xe5, interrupt_229_asm_entry);
    register_interrupt_handler(0xe6, interrupt_230_asm_entry);
    register_interrupt_handler(0xe7, interrupt_231_asm_entry);
    register_interrupt_handler(0xe8, interrupt_232_asm_entry);
    register_interrupt_handler(0xe9, interrupt_233_asm_entry);
    register_interrupt_handler(0xea, interrupt_234_asm_entry);
    register_interrupt_handler(0xeb, interrupt_235_asm_entry);
    register_interrupt_handler(0xec, interrupt_236_asm_entry);
    register_interrupt_handler(0xed, interrupt_237_asm_entry);
    register_interrupt_handler(0xee, interrupt_238_asm_entry);
    register_interrupt_handler(0xef, interrupt_239_asm_entry);
    register_interrupt_handler(0xf0, interrupt_240_asm_entry);
    register_interrupt_handler(0xf1, interrupt_241_asm_entry);
    register_interrupt_handler(0xf2, interrupt_242_asm_entry);
    register_interrupt_handler(0xf3, interrupt_243_asm_entry);
    register_interrupt_handler(0xf4, interrupt_244_asm_entry);
    register_interrupt_handler(0xf5, interrupt_245_asm_entry);
    register_interrupt_handler(0xf6, interrupt_246_asm_entry);
    register_interrupt_handler(0xf7, interrupt_247_asm_entry);
    register_interrupt_handler(0xf8, interrupt_248_asm_entry);
    register_interrupt_handler(0xf9, interrupt_249_asm_entry);
    register_interrupt_handler(0xfa, interrupt_250_asm_entry);
    register_interrupt_handler(0xfb, interrupt_251_asm_entry);
    register_interrupt_handler(0xfc, interrupt_252_asm_entry);
    register_interrupt_handler(0xfd, interrupt_253_asm_entry);
    register_interrupt_handler(0xfe, interrupt_254_asm_entry);
    register_interrupt_handler(0xff, interrupt_255_asm_entry);

    dbg() << "Installing Unhandled Handlers";

    for (u8 i = 0; i < GENERIC_INTERRUPT_HANDLERS_COUNT; ++i) {
        new UnhandledInterruptHandler(i);
    }

    flush_idt();
}

void load_task_register(u16 selector)
{
    asm("ltr %0" ::"r"(selector));
}

void handle_interrupt(TrapFrame* trap)
{
    clac();
    auto& regs = *trap->regs;
    ASSERT(regs.isr_number >= IRQ_VECTOR_BASE && regs.isr_number <= (IRQ_VECTOR_BASE + GENERIC_INTERRUPT_HANDLERS_COUNT));
    u8 irq = (u8)(regs.isr_number - 0x50);
    ASSERT(s_interrupt_handler[irq]);
    s_interrupt_handler[irq]->handle_interrupt(regs);
    s_interrupt_handler[irq]->eoi();
}

void enter_trap_no_irq(TrapFrame* trap)
{
    Processor::current().enter_trap(*trap, false);
}

void enter_trap(TrapFrame* trap)
{
    Processor::current().enter_trap(*trap, true);
}

void exit_trap(TrapFrame* trap)
{
    return Processor::current().exit_trap(*trap);
}

static void sse_init()
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

u32 read_cr0()
{
    u32 cr0;
    asm("movl %%cr0, %%eax"
        : "=a"(cr0));
    return cr0;
}

u32 read_cr3()
{
    u32 cr3;
    asm("movl %%cr3, %%eax"
        : "=a"(cr3));
    return cr3;
}

void write_cr3(u32 cr3)
{
    asm volatile("movl %%eax, %%cr3" ::"a"(cr3)
                 : "memory");
}

u32 read_cr4()
{
    u32 cr4;
    asm("movl %%cr4, %%eax"
        : "=a"(cr4));
    return cr4;
}

u32 read_dr6()
{
    u32 dr6;
    asm("movl %%dr6, %%eax"
        : "=a"(dr6));
    return dr6;
}

FPUState Processor::s_clean_fpu_state;

static Vector<Processor*>* s_processors;
static SpinLock s_processor_lock;
volatile u32 Processor::g_total_processors;
static volatile bool s_smp_enabled;

Vector<Processor*>& Processor::processors()
{
    ASSERT(s_processors);
    return *s_processors;
}

Processor& Processor::by_id(u32 cpu)
{
    // s_processors does not need to be protected by a lock of any kind.
    // It is populated early in the boot process, and the BSP is waiting
    // for all APs to finish, after which this array never gets modified
    // again, so it's safe to not protect access to it here
    auto& procs = processors();
    ASSERT(procs[cpu] != nullptr);
    ASSERT(procs.size() > cpu);
    return *procs[cpu];
}

[[noreturn]] static inline void halt_this()
{
    for (;;) {
        asm volatile("cli; hlt");
    }
}

void Processor::cpu_detect()
{
    // NOTE: This is called during Processor::early_initialize, we cannot
    //       safely log at this point because we don't have kmalloc
    //       initialized yet!
    auto set_feature =
        [&](CPUFeature f) {
            m_features = static_cast<CPUFeature>(static_cast<u32>(m_features) | static_cast<u32>(f));
        };
    m_features = static_cast<CPUFeature>(0);

    CPUID processor_info(0x1);
    if (processor_info.edx() & (1 << 4))
        set_feature(CPUFeature::TSC);
    if (processor_info.edx() & (1 << 6))
        set_feature(CPUFeature::PAE);
    if (processor_info.edx() & (1 << 13))
        set_feature(CPUFeature::PGE);
    if (processor_info.edx() & (1 << 23))
        set_feature(CPUFeature::MMX);
    if (processor_info.edx() & (1 << 25))
        set_feature(CPUFeature::SSE);
    if (processor_info.edx() & (1 << 26))
        set_feature(CPUFeature::SSE2);
    if (processor_info.ecx() & (1 << 0))
        set_feature(CPUFeature::SSE3);
    if (processor_info.ecx() & (1 << 9))
        set_feature(CPUFeature::SSSE3);
    if (processor_info.ecx() & (1 << 19))
        set_feature(CPUFeature::SSE4_1);
    if (processor_info.ecx() & (1 << 20))
        set_feature(CPUFeature::SSE4_2);
    if (processor_info.ecx() & (1 << 30))
        set_feature(CPUFeature::RDRAND);
    if (processor_info.edx() & (1 << 11)) {
        u32 stepping = processor_info.eax() & 0xf;
        u32 model = (processor_info.eax() >> 4) & 0xf;
        u32 family = (processor_info.eax() >> 8) & 0xf;
        if (!(family == 6 && model < 3 && stepping < 3))
            set_feature(CPUFeature::SEP);
        if ((family == 6 && model >= 3) || (family == 0xf && model >= 0xe))
            set_feature(CPUFeature::CONSTANT_TSC);
    }

    u32 max_extended_leaf = CPUID(0x80000000).eax();

    ASSERT(max_extended_leaf >= 0x80000001);
    CPUID extended_processor_info(0x80000001);
    if (extended_processor_info.edx() & (1 << 20))
        set_feature(CPUFeature::NX);
    if (extended_processor_info.edx() & (1 << 27))
        set_feature(CPUFeature::RDTSCP);
    if (extended_processor_info.edx() & (1 << 11)) {
        // Only available in 64 bit mode
        set_feature(CPUFeature::SYSCALL);
    }

    if (max_extended_leaf >= 0x80000007) {
        CPUID cpuid(0x80000007);
        if (cpuid.edx() & (1 << 8)) {
            set_feature(CPUFeature::CONSTANT_TSC);
            set_feature(CPUFeature::NONSTOP_TSC);
        }
    }

    CPUID extended_features(0x7);
    if (extended_features.ebx() & (1 << 20))
        set_feature(CPUFeature::SMAP);
    if (extended_features.ebx() & (1 << 7))
        set_feature(CPUFeature::SMEP);
    if (extended_features.ecx() & (1 << 2))
        set_feature(CPUFeature::UMIP);
    if (extended_features.ebx() & (1 << 18))
        set_feature(CPUFeature::RDSEED);
}

void Processor::cpu_setup()
{
    // NOTE: This is called during Processor::early_initialize, we cannot
    //       safely log at this point because we don't have kmalloc
    //       initialized yet!
    cpu_detect();

    if (has_feature(CPUFeature::SSE))
        sse_init();

    asm volatile(
        "movl %%cr0, %%eax\n"
        "orl $0x00010000, %%eax\n"
        "movl %%eax, %%cr0\n" ::
            : "%eax", "memory");

    if (has_feature(CPUFeature::PGE)) {
        // Turn on CR4.PGE so the CPU will respect the G bit in page tables.
        asm volatile(
            "mov %cr4, %eax\n"
            "orl $0x80, %eax\n"
            "mov %eax, %cr4\n");
    }

    if (has_feature(CPUFeature::NX)) {
        // Turn on IA32_EFER.NXE
        asm volatile(
            "movl $0xc0000080, %ecx\n"
            "rdmsr\n"
            "orl $0x800, %eax\n"
            "wrmsr\n");
    }

    if (has_feature(CPUFeature::SMEP)) {
        // Turn on CR4.SMEP
        asm volatile(
            "mov %cr4, %eax\n"
            "orl $0x100000, %eax\n"
            "mov %eax, %cr4\n");
    }

    if (has_feature(CPUFeature::SMAP)) {
        // Turn on CR4.SMAP
        asm volatile(
            "mov %cr4, %eax\n"
            "orl $0x200000, %eax\n"
            "mov %eax, %cr4\n");
    }

    if (has_feature(CPUFeature::UMIP)) {
        asm volatile(
            "mov %cr4, %eax\n"
            "orl $0x800, %eax\n"
            "mov %eax, %cr4\n");
    }

    if (has_feature(CPUFeature::TSC)) {
        asm volatile(
            "mov %cr4, %eax\n"
            "orl $0x4, %eax\n"
            "mov %eax, %cr4\n");
    }
}

String Processor::features_string() const
{
    StringBuilder builder;
    auto feature_to_str =
        [](CPUFeature f) -> const char*
        {
            switch (f) {
                case CPUFeature::NX:
                    return "nx";
                case CPUFeature::PAE:
                    return "pae";
                case CPUFeature::PGE:
                    return "pge";
                case CPUFeature::RDRAND:
                    return "rdrand";
                case CPUFeature::RDSEED:
                    return "rdseed";
                case CPUFeature::SMAP:
                    return "smap";
                case CPUFeature::SMEP:
                    return "smep";
                case CPUFeature::SSE:
                    return "sse";
                case CPUFeature::TSC:
                    return "tsc";
                case CPUFeature::RDTSCP:
                    return "rdtscp";
                case CPUFeature::CONSTANT_TSC:
                    return "constant_tsc";
                case CPUFeature::NONSTOP_TSC:
                    return "nonstop_tsc";
                case CPUFeature::UMIP:
                    return "umip";
                case CPUFeature::SEP:
                    return "sep";
                case CPUFeature::SYSCALL:
                    return "syscall";
                case CPUFeature::MMX:
                    return "mmx";
                case CPUFeature::SSE2:
                    return "sse2";
                case CPUFeature::SSE3:
                    return "sse3";
                case CPUFeature::SSSE3:
                    return "ssse3";
                case CPUFeature::SSE4_1:
                    return "sse4.1";
                case CPUFeature::SSE4_2:
                    return "sse4.2";
                // no default statement here intentionally so that we get
                // a warning if a new feature is forgotten to be added here
            }
            // Shouldn't ever happen
            return "???";
        };
    bool first = true;
    for (u32 flag = 1; flag != 0; flag <<= 1) {
        if ((static_cast<u32>(m_features) & flag) != 0) {
            if (first)
                first = false;
            else
                builder.append(' ');
            auto str = feature_to_str(static_cast<CPUFeature>(flag));
            builder.append(str, strlen(str));
        }
    }
    return builder.build();
}

void Processor::early_initialize(u32 cpu)
{
    m_self = this;

    m_cpu = cpu;
    m_in_irq = 0;
    m_in_critical = 0;

    m_invoke_scheduler_async = false;
    m_scheduler_initialized = false;

    m_message_queue = nullptr;
    m_idle_thread = nullptr;
    m_current_thread = nullptr;
    m_scheduler_data = nullptr;
    m_mm_data = nullptr;
    m_info = nullptr;

    m_halt_requested = false;
    if (cpu == 0) {
        s_smp_enabled = false;
        atomic_store(&g_total_processors, 1u, AK::MemoryOrder::memory_order_release);
    } else {
        atomic_fetch_add(&g_total_processors, 1u, AK::MemoryOrder::memory_order_acq_rel);
    }

    cpu_setup();
    gdt_init();
    ASSERT(&current() == this); // sanity check
}

void Processor::initialize(u32 cpu)
{
    ASSERT(m_self == this);
    ASSERT(&current() == this); // sanity check

    klog() << "CPU[" << id() << "]: Supported features: " << features_string();
    if (!has_feature(CPUFeature::RDRAND))
        klog() << "CPU[" << id() << "]: No RDRAND support detected, randomness will be poor";

    if (cpu == 0)
        idt_init();
    else
        flush_idt();

    if (cpu == 0) {
        ASSERT((FlatPtr(&s_clean_fpu_state) & 0xF) == 0);
        asm volatile("fninit");
        asm volatile("fxsave %0"
            : "=m"(s_clean_fpu_state));
    }

    m_info = new ProcessorInfo(*this);

    {
        ScopedSpinLock lock(s_processor_lock);
        // We need to prevent races between APs starting up at the same time
        if (!s_processors)
            s_processors = new Vector<Processor*>();
        if (cpu >= s_processors->size())
            s_processors->resize(cpu + 1);
        (*s_processors)[cpu] = this;
    }
}

void Processor::write_raw_gdt_entry(u16 selector, u32 low, u32 high)
{
    u16 i = (selector & 0xfffc) >> 3;
    u32 prev_gdt_length = m_gdt_length;

    if (i > m_gdt_length) {
        m_gdt_length = i + 1;
        ASSERT(m_gdt_length <= sizeof(m_gdt) / sizeof(m_gdt[0]));
        m_gdtr.limit = (m_gdt_length + 1) * 8 - 1;
    }
    m_gdt[i].low = low;
    m_gdt[i].high = high;

    // clear selectors we may have skipped
    while (i < prev_gdt_length) {
        m_gdt[i].low = 0;
        m_gdt[i].high = 0;
        i++;
    }
}

void Processor::write_gdt_entry(u16 selector, Descriptor& descriptor)
{
    write_raw_gdt_entry(selector, descriptor.low, descriptor.high);
}

Descriptor& Processor::get_gdt_entry(u16 selector)
{
    u16 i = (selector & 0xfffc) >> 3;
    return *(Descriptor*)(&m_gdt[i]);
}

void Processor::flush_gdt()
{
    m_gdtr.address = m_gdt;
    m_gdtr.limit = (m_gdt_length * 8) - 1;
    asm volatile("lgdt %0" ::"m"(m_gdtr)
        : "memory");
}

const DescriptorTablePointer& Processor::get_gdtr()
{
    return m_gdtr;
}

bool Processor::get_context_frame_ptr(Thread& thread, u32& frame_ptr, u32& eip)
{
    ScopedCritical critical;
    auto& proc = Processor::current();
    if (&thread == proc.current_thread()) {
        ASSERT(thread.state() == Thread::Running);
        asm volatile("movl %%ebp, %%eax"
                     : "=g"(frame_ptr));
    } else {
        // Since the thread may be running on another processor, there
        // is a chance a context switch may happen while we're trying
        // to get it. It also won't be entirely accurate and merely
        // reflect the status at the last context switch.
        ScopedSpinLock lock(g_scheduler_lock);
        if (thread.state() == Thread::Running) {
            ASSERT(thread.cpu() != proc.id());
            // TODO: If this is the case, the thread is currently running
            // on another processor. We can't trust the kernel stack as
            // it may be changing at any time. We need to probably send
            // an IPI to that processor, have it walk the stack and wait
            // until it returns the data back to us
            dbg() << "CPU[" << proc.id() << "] getting stack for "
                  << thread << " on other CPU# " << thread.cpu() << " not yet implemented!";
            frame_ptr = eip = 0; // TODO
            return false;
        } else {
            // We need to retrieve ebp from what was last pushed to the kernel
            // stack. Before switching out of that thread, it switch_context
            // pushed the callee-saved registers, and the last of them happens
            // to be ebp.
            auto& tss = thread.tss();
            u32* stack_top = reinterpret_cast<u32*>(tss.esp);
            frame_ptr = stack_top[0];
            eip = tss.eip;
        }
    }
    return true;
}

extern "C" void enter_thread_context(Thread* from_thread, Thread* to_thread)
{
    ASSERT(from_thread == to_thread || from_thread->state() != Thread::Running);
    ASSERT(to_thread->state() == Thread::Running);

    auto& processor = Processor::current();
    processor.set_current_thread(*to_thread);

    auto& from_tss = from_thread->tss();
    auto& to_tss = to_thread->tss();
    asm volatile("fxsave %0"
        : "=m"(from_thread->fpu_state()));

    from_tss.fs = get_fs();
    from_tss.gs = get_gs();
    set_fs(to_tss.fs);
    set_gs(to_tss.gs);

    auto& tls_descriptor = processor.get_gdt_entry(GDT_SELECTOR_TLS);
    tls_descriptor.set_base(to_thread->thread_specific_data().as_ptr());
    tls_descriptor.set_limit(to_thread->thread_specific_region_size());

    if (from_tss.cr3 != to_tss.cr3)
        write_cr3(to_tss.cr3);

    to_thread->set_cpu(processor.id());

    asm volatile("fxrstor %0"
        ::"m"(to_thread->fpu_state()));

    // TODO: debug registers
    // TODO: ioperm?
}

#define ENTER_THREAD_CONTEXT_ARGS_SIZE (2 * 4) //  to_thread, from_thread

void Processor::switch_context(Thread*& from_thread, Thread*& to_thread)
{
    ASSERT(!in_irq());
    ASSERT(m_in_critical == 1);
    ASSERT(is_kernel_mode());
#ifdef CONTEXT_SWITCH_DEBUG
    dbg() << "switch_context --> switching out of: " << VirtualAddress(from_thread) << " " << *from_thread;
#endif

    // Switch to new thread context, passing from_thread and to_thread
    // through to the new context using registers edx and eax
    asm volatile(
        // NOTE: changing how much we push to the stack affects
        //       SWITCH_CONTEXT_TO_STACK_SIZE and thread_context_first_enter()!
        "pushfl \n"
        "pushl %%ebx \n"
        "pushl %%esi \n"
        "pushl %%edi \n"
        "pushl %%ebp \n"
        "movl %%esp, %[from_esp] \n"
        "movl $1f, %[from_eip] \n"
        "movl %[to_esp0], %%ebx \n"
        "movl %%ebx, %[tss_esp0] \n"
        "movl %[to_esp], %%esp \n"
        "pushl %[to_thread] \n"
        "pushl %[from_thread] \n"
        "pushl %[to_eip] \n"
        "cld \n"
        "jmp enter_thread_context \n"
        "1: \n"
        "popl %%edx \n"
        "popl %%eax \n"
        "popl %%ebp \n"
        "popl %%edi \n"
        "popl %%esi \n"
        "popl %%ebx \n"
        "popfl \n"
        : [from_esp] "=m" (from_thread->tss().esp),
          [from_eip] "=m" (from_thread->tss().eip),
          [tss_esp0] "=m" (m_tss.esp0),
          "=d" (from_thread), // needed so that from_thread retains the correct value
          "=a" (to_thread) // needed so that to_thread retains the correct value
        : [to_esp] "g" (to_thread->tss().esp),
          [to_esp0] "g" (to_thread->tss().esp0),
          [to_eip] "c" (to_thread->tss().eip),
          [from_thread] "d" (from_thread),
          [to_thread] "a" (to_thread)
    );
#ifdef CONTEXT_SWITCH_DEBUG
    dbg() << "switch_context <-- from " << VirtualAddress(from_thread) << " " << *from_thread << " to " << VirtualAddress(to_thread) << " " << *to_thread;
#endif
}

extern "C" void context_first_init(Thread* from_thread, Thread* to_thread, TrapFrame* trap)
{
    ASSERT(!are_interrupts_enabled());
    ASSERT(is_kernel_mode());
    (void)from_thread;
    (void)to_thread;
    (void)trap;

#ifdef CONTEXT_SWITCH_DEBUG
    dbg() << "switch_context <-- from " << VirtualAddress(from_thread) << " " << *from_thread << " to " << VirtualAddress(to_thread) << " " << *to_thread << " (context_first_init)";
#endif

    ASSERT(to_thread == Thread::current());

    Scheduler::enter_current(*from_thread);

    // Since we got here and don't have Scheduler::context_switch in the
    // call stack (because this is the first time we switched into this
    // context), we need to notify the scheduler so that it can release
    // the scheduler lock.
    Scheduler::leave_on_first_switch(trap->regs->eflags);
}

extern "C" void thread_context_first_enter(void);
asm(
// enter_thread_context returns to here first time a thread is executing
".globl thread_context_first_enter \n"
"thread_context_first_enter: \n"
// switch_context will have pushed from_thread and to_thread to our new
// stack prior to thread_context_first_enter() being called, and the
// pointer to TrapFrame was the top of the stack before that
"    movl 8(%esp), %ebx \n" // save pointer to TrapFrame
"    cld \n"
"    call context_first_init \n"
"    addl $" __STRINGIFY(ENTER_THREAD_CONTEXT_ARGS_SIZE) ", %esp \n"
"    movl %ebx, 0(%esp) \n" // push pointer to TrapFrame
"    jmp common_trap_exit \n"
);

u32 Processor::init_context(Thread& thread, bool leave_crit)
{
    ASSERT(is_kernel_mode());
    ASSERT(g_scheduler_lock.is_locked());
    if (leave_crit) {
        // Leave the critical section we set up in in Process::exec,
        // but because we still have the scheduler lock we should end up with 1
        m_in_critical--; // leave it without triggering anything or restoring flags
        ASSERT(in_critical() == 1);
    }

    const u32 kernel_stack_top = thread.kernel_stack_top();
    u32 stack_top = kernel_stack_top;

    // TODO: handle NT?
    ASSERT((cpu_flags() & 0x24000) == 0); // Assume !(NT | VM)

    auto& tss = thread.tss();
    bool return_to_user = (tss.cs & 3) != 0;

    // make room for an interrupt frame
    if (!return_to_user) {
        // userspace_esp and userspace_ss are not popped off by iret
        // unless we're switching back to user mode
        stack_top -= sizeof(RegisterState) - 2 * sizeof(u32);
    } else {
        stack_top -= sizeof(RegisterState);
    }

    // we want to end up 16-byte aligned, %esp + 4 should be aligned
    stack_top -= sizeof(u32);
    *reinterpret_cast<u32*>(kernel_stack_top - 4) = 0;

    // set up the stack so that after returning from thread_context_first_enter()
    // we will end up either in kernel mode or user mode, depending on how the thread is set up
    // However, the first step is to always start in kernel mode with thread_context_first_enter
    RegisterState& iretframe = *reinterpret_cast<RegisterState*>(stack_top);
    iretframe.ss = tss.ss;
    iretframe.gs = tss.gs;
    iretframe.fs = tss.fs;
    iretframe.es = tss.es;
    iretframe.ds = tss.ds;
    iretframe.edi = tss.edi;
    iretframe.esi = tss.esi;
    iretframe.ebp = tss.ebp;
    iretframe.esp = 0;
    iretframe.ebx = tss.ebx;
    iretframe.edx = tss.edx;
    iretframe.ecx = tss.ecx;
    iretframe.eax = tss.eax;
    iretframe.eflags = tss.eflags;
    iretframe.eip = tss.eip;
    iretframe.cs = tss.cs;
    if (return_to_user) {
        iretframe.userspace_esp = tss.esp;
        iretframe.userspace_ss = tss.ss;
    }

    // make space for a trap frame
    stack_top -= sizeof(TrapFrame);
    TrapFrame& trap = *reinterpret_cast<TrapFrame*>(stack_top);
    trap.regs = &iretframe;
    trap.prev_irq_level = 0;

    stack_top -= sizeof(u32); // pointer to TrapFrame
    *reinterpret_cast<u32*>(stack_top) = stack_top + 4;

#ifdef CONTEXT_SWITCH_DEBUG
    if (return_to_user)
        dbg() << "init_context " << thread << " (" << VirtualAddress(&thread) << ") set up to execute at eip: " << String::format("%02x:%08x", iretframe.cs, (u32)tss.eip) << " esp: " << VirtualAddress(tss.esp) << " stack top: " << VirtualAddress(stack_top) << " user esp: " << String::format("%02x:%08x", iretframe.userspace_ss, (u32)iretframe.userspace_esp);
    else
        dbg() << "init_context " << thread << " (" << VirtualAddress(&thread) << ") set up to execute at eip: " << String::format("%02x:%08x", iretframe.cs, (u32)tss.eip) << " esp: " << VirtualAddress(tss.esp) << " stack top: " << VirtualAddress(stack_top);
#endif

    // make switch_context() always first return to thread_context_first_enter()
    // in kernel mode, so set up these values so that we end up popping iretframe
    // off the stack right after the context switch completed, at which point
    // control is transferred to what iretframe is pointing to.
    tss.eip = FlatPtr(&thread_context_first_enter);
    tss.esp0 = kernel_stack_top;
    tss.esp = stack_top;
    tss.cs = GDT_SELECTOR_CODE0;
    tss.ds = GDT_SELECTOR_DATA0;
    tss.es = GDT_SELECTOR_DATA0;
    tss.gs = GDT_SELECTOR_DATA0;
    tss.ss = GDT_SELECTOR_DATA0;
    tss.fs = GDT_SELECTOR_PROC;
    return stack_top;
}

extern "C" u32 do_init_context(Thread* thread, u32 flags)
{
    ASSERT_INTERRUPTS_DISABLED();
    thread->tss().eflags = flags;
    return Processor::current().init_context(*thread, true);
}

extern "C" void do_assume_context(Thread* thread, u32 flags);

asm(
".global do_assume_context \n"
"do_assume_context: \n"
"    movl 4(%esp), %ebx \n"
"    movl 8(%esp), %esi \n"
// We're going to call Processor::init_context, so just make sure
// we have enough stack space so we don't stomp over it
"    subl $(" __STRINGIFY(4 + REGISTER_STATE_SIZE + TRAP_FRAME_SIZE + 4) "), %esp \n"
"    pushl %esi \n"
"    pushl %ebx \n"
"    cld \n"
"    call do_init_context \n"
"    addl $8, %esp \n"
"    movl %eax, %esp \n" // move stack pointer to what Processor::init_context set up for us
"    pushl %ebx \n" // push to_thread
"    pushl %ebx \n" // push from_thread
"    pushl $thread_context_first_enter \n" // should be same as tss.eip
"    jmp enter_thread_context \n"
);

void Processor::assume_context(Thread& thread, u32 flags)
{
#ifdef CONTEXT_SWITCH_DEBUG
    dbg() << "Assume context for thread " << VirtualAddress(&thread) << " " << thread;
#endif
    ASSERT_INTERRUPTS_DISABLED();
    Scheduler::prepare_after_exec();
    // in_critical() should be 2 here. The critical section in Process::exec
    // and then the scheduler lock
    ASSERT(Processor::current().in_critical() == 2);
    do_assume_context(&thread, flags);
    ASSERT_NOT_REACHED();
}

extern "C" void pre_init_finished(void)
{
    ASSERT(g_scheduler_lock.own_lock());

    // Because init_finished() will wait on the other APs, we need
    // to release the scheduler lock so that the other APs can also get
    // to this point

    // The target flags will get restored upon leaving the trap
    u32 prev_flags = cpu_flags();
    Scheduler::leave_on_first_switch(prev_flags);
}

extern "C" void post_init_finished(void)
{
    // We need to re-acquire the scheduler lock before a context switch
    // transfers control into the idle loop, which needs the lock held
    Scheduler::prepare_for_idle_loop();
}

void Processor::initialize_context_switching(Thread& initial_thread)
{
    ASSERT(initial_thread.process().is_kernel_process());

    auto& tss = initial_thread.tss();
    m_tss = tss;
    m_tss.esp0 = tss.esp0;
    m_tss.ss0 = GDT_SELECTOR_DATA0;
    // user mode needs to be able to switch to kernel mode:
    m_tss.cs = m_tss.ds = m_tss.es = m_tss.gs = m_tss.ss = GDT_SELECTOR_CODE0 | 3;
    m_tss.fs = GDT_SELECTOR_PROC | 3;

    m_scheduler_initialized = true;

    asm volatile(
        "movl %[new_esp], %%esp \n" // switch to new stack
        "pushl %[from_to_thread] \n" // to_thread
        "pushl %[from_to_thread] \n" // from_thread
        "pushl $" __STRINGIFY(GDT_SELECTOR_CODE0) " \n"
        "pushl %[new_eip] \n" // save the entry eip to the stack
        "movl %%esp, %%ebx \n"
        "addl $20, %%ebx \n" // calculate pointer to TrapFrame
        "pushl %%ebx \n"
        "cld \n"
        "pushl %[cpu] \n" // push argument for init_finished before register is clobbered
        "call pre_init_finished \n"
        "call init_finished \n"
        "addl $4, %%esp \n"
        "call post_init_finished \n"
        "call enter_trap_no_irq \n"
        "addl $4, %%esp \n"
        "lret \n"
        :: [new_esp] "g" (tss.esp),
           [new_eip] "a" (tss.eip),
           [from_to_thread] "b" (&initial_thread),
           [cpu] "c" (id())
    );
    
    ASSERT_NOT_REACHED();
}

void Processor::enter_trap(TrapFrame& trap, bool raise_irq)
{
    InterruptDisabler disabler;
    trap.prev_irq_level = m_in_irq;
    if (raise_irq)
        m_in_irq++;
}

void Processor::exit_trap(TrapFrame& trap)
{
    InterruptDisabler disabler;
    ASSERT(m_in_irq >= trap.prev_irq_level);
    m_in_irq = trap.prev_irq_level;

    smp_process_pending_messages();

    if (!m_in_irq && !m_in_critical)
        check_invoke_scheduler();
}

void Processor::check_invoke_scheduler()
{
    ASSERT(!m_in_irq);
    ASSERT(!m_in_critical);
    if (m_invoke_scheduler_async && m_scheduler_initialized) {
        m_invoke_scheduler_async = false;
        Scheduler::invoke_async();
    }
}

void Processor::flush_tlb_local(VirtualAddress vaddr, size_t page_count)
{
    auto ptr = vaddr.as_ptr();
    while (page_count > 0) {
        asm volatile("invlpg %0"
             :
             : "m"(*ptr)
             : "memory");
        ptr += PAGE_SIZE;
        page_count--;
    }
}

void Processor::flush_tlb(VirtualAddress vaddr, size_t page_count)
{
    flush_tlb_local(vaddr, page_count);
    if (s_smp_enabled)
        smp_broadcast_flush_tlb(vaddr, page_count);
}

static volatile ProcessorMessage* s_message_pool;

void Processor::smp_return_to_pool(ProcessorMessage& msg)
{
    ProcessorMessage* next = nullptr;
    do {
        msg.next = next;
    } while (!atomic_compare_exchange_strong(&s_message_pool, next, &msg, AK::MemoryOrder::memory_order_acq_rel));
}

ProcessorMessage& Processor::smp_get_from_pool()
{
    ProcessorMessage* msg;

    // The assumption is that messages are never removed from the pool!
    for (;;) {
        msg = atomic_load(&s_message_pool, AK::MemoryOrder::memory_order_consume);
        if (!msg) {
            if (!Processor::current().smp_process_pending_messages()) {
                // TODO: pause for a bit?
            }
            continue;
        }
        // If another processor were to use this message in the meanwhile,
        // "msg" is still valid (because it never gets freed). We'd detect
        // this because the expected value "msg" and pool would
        // no longer match, and the compare_exchange will fail. But accessing
        // "msg->next" is always safe here.
        if (atomic_compare_exchange_strong(&s_message_pool, msg, msg->next, AK::MemoryOrder::memory_order_acq_rel)) {
            // We successfully "popped" this available message
            break;
        }
    }

    ASSERT(msg != nullptr);
    return *msg;
}

void Processor::smp_enable()
{
    size_t msg_pool_size = Processor::count() * 100u;
    size_t msg_entries_cnt = Processor::count();

    auto msgs = new ProcessorMessage[msg_pool_size];
    auto msg_entries = new ProcessorMessageEntry[msg_pool_size * msg_entries_cnt];
    size_t msg_entry_i = 0;
    for (size_t i = 0; i < msg_pool_size; i++, msg_entry_i += msg_entries_cnt) {
        auto& msg = msgs[i];
        msg.next = i < msg_pool_size - 1 ? &msgs[i + 1] : nullptr;
        msg.per_proc_entries = &msg_entries[msg_entry_i];
        for (size_t k = 0; k < msg_entries_cnt; k++)
            msg_entries[msg_entry_i + k].msg = &msg;
    }

    atomic_store(&s_message_pool, &msgs[0], AK::MemoryOrder::memory_order_release);

    // Start sending IPI messages
    s_smp_enabled = true;
}

void Processor::smp_cleanup_message(ProcessorMessage& msg)
{
    switch (msg.type) {
    case ProcessorMessage::CallbackWithData:
        if (msg.callback_with_data.free)
            msg.callback_with_data.free(msg.callback_with_data.data);
        break;
    default:
        break;
    }
}

bool Processor::smp_process_pending_messages()
{
    bool did_process = false;
    u32 prev_flags;
    enter_critical(prev_flags);

    if (auto pending_msgs = atomic_exchange(&m_message_queue, nullptr, AK::MemoryOrder::memory_order_acq_rel)) {
        // We pulled the stack of pending messages in LIFO order, so we need to reverse the list first
        auto reverse_list =
            [](ProcessorMessageEntry* list) -> ProcessorMessageEntry*
            {
                ProcessorMessageEntry* rev_list = nullptr;
                while (list) {
                    auto next = list->next;
                    list->next = rev_list;
                    rev_list = list;
                    list = next;
                }
                return rev_list;
            };

        pending_msgs = reverse_list(pending_msgs);

        // now process in the right order
        ProcessorMessageEntry* next_msg;
        for (auto cur_msg = pending_msgs; cur_msg; cur_msg = next_msg) {
            next_msg = cur_msg->next;
            auto msg = cur_msg->msg;

#ifdef SMP_DEBUG
            dbg() << "SMP[" << id() << "]: Processing message " << VirtualAddress(msg);
#endif

            switch (msg->type) {
            case ProcessorMessage::Callback:
                msg->callback.handler();
                break;
            case ProcessorMessage::CallbackWithData:
                msg->callback_with_data.handler(msg->callback_with_data.data);
                break;
            case ProcessorMessage::FlushTlb:
                flush_tlb_local(VirtualAddress(msg->flush_tlb.ptr), msg->flush_tlb.page_count);
                break;
            }

            bool is_async = msg->async; // Need to cache this value *before* dropping the ref count!
            auto prev_refs = atomic_fetch_sub(&msg->refs, 1u, AK::MemoryOrder::memory_order_acq_rel);
            ASSERT(prev_refs != 0);
            if (prev_refs == 1) {
                // All processors handled this. If this is an async message,
                // we need to clean it up and return it to the pool
                if (is_async) {
                    smp_cleanup_message(*msg);
                    smp_return_to_pool(*msg);
                }
            }

            if (m_halt_requested)
                halt_this();
        }
        did_process = true;
    } else if (m_halt_requested) {
        halt_this();
    }

    leave_critical(prev_flags);
    return did_process;
}

bool Processor::smp_queue_message(ProcessorMessage& msg)
{
    // Note that it's quite possible that the other processor may pop
    // the queue at any given time. We rely on the fact that the messages
    // are pooled and never get freed!
    auto& msg_entry = msg.per_proc_entries[id()];
    ASSERT(msg_entry.msg == &msg);
    ProcessorMessageEntry* next = nullptr;
    do {
        msg_entry.next = next;
    } while (!atomic_compare_exchange_strong(&m_message_queue, next, &msg_entry, AK::MemoryOrder::memory_order_acq_rel));
    return next == nullptr;
}

void Processor::smp_broadcast_message(ProcessorMessage& msg, bool async)
{
    auto& cur_proc = Processor::current();
    msg.async = async;
#ifdef SMP_DEBUG
    dbg() << "SMP[" << cur_proc.id() << "]: Broadcast message " << VirtualAddress(&msg) << " to cpus: " << (count()) << " proc: " << VirtualAddress(&cur_proc);
#endif
    atomic_store(&msg.refs, count() - 1, AK::MemoryOrder::memory_order_release);
    ASSERT(msg.refs > 0);
    for_each(
        [&](Processor& proc) -> IterationDecision {
            if (&proc != &cur_proc) {
                if (proc.smp_queue_message(msg)) {
                    // TODO: only send IPI to that CPU if we queued the first
                }
            }
            return IterationDecision::Continue;
        });

    // Now trigger an IPI on all other APs
    APIC::the().broadcast_ipi();

    if (!async) {
        // If synchronous then we must cleanup and return the message back
        // to the pool. Otherwise, the last processor to complete it will return it
        while (atomic_load(&msg.refs, AK::MemoryOrder::memory_order_consume) != 0) {
            // TODO: pause for a bit?
        }

        smp_cleanup_message(msg);
        smp_return_to_pool(msg);
    }
}

void Processor::smp_broadcast(void (*callback)(void*), void* data, void (*free_data)(void*), bool async)
{
    auto& msg = smp_get_from_pool();
    msg.type = ProcessorMessage::CallbackWithData;
    msg.callback_with_data.handler = callback;
    msg.callback_with_data.data = data;
    msg.callback_with_data.free = free_data;
    smp_broadcast_message(msg, async);
}

void Processor::smp_broadcast(void (*callback)(), bool async)
{
    auto& msg = smp_get_from_pool();
    msg.type = ProcessorMessage::CallbackWithData;
    msg.callback.handler = callback;
    smp_broadcast_message(msg, async);
}

void Processor::smp_broadcast_flush_tlb(VirtualAddress vaddr, size_t page_count)
{
    auto& msg = smp_get_from_pool();
    msg.type = ProcessorMessage::FlushTlb;
    msg.flush_tlb.ptr = vaddr.as_ptr();
    msg.flush_tlb.page_count = page_count;
    smp_broadcast_message(msg, false);
}

void Processor::smp_broadcast_halt()
{
    // We don't want to use a message, because this could have been triggered
    // by being out of memory and we might not be able to get a message
    for_each(
        [&](Processor& proc) -> IterationDecision {
            proc.m_halt_requested = true;
            return IterationDecision::Continue;
        });

    // Now trigger an IPI on all other APs
    APIC::the().broadcast_ipi();
}

void Processor::Processor::halt()
{
    if (s_smp_enabled)
        smp_broadcast_halt();

    halt_this();
}

void Processor::gdt_init()
{
    m_gdt_length = 0;
    m_gdtr.address = nullptr;
    m_gdtr.limit = 0;

    write_raw_gdt_entry(0x0000, 0x00000000, 0x00000000);
    write_raw_gdt_entry(GDT_SELECTOR_CODE0, 0x0000ffff, 0x00cf9a00); // code0
    write_raw_gdt_entry(GDT_SELECTOR_DATA0, 0x0000ffff, 0x00cf9200); // data0
    write_raw_gdt_entry(GDT_SELECTOR_CODE3, 0x0000ffff, 0x00cffa00); // code3
    write_raw_gdt_entry(GDT_SELECTOR_DATA3, 0x0000ffff, 0x00cff200); // data3

    Descriptor tls_descriptor;
    tls_descriptor.low = tls_descriptor.high = 0;
    tls_descriptor.dpl = 3;
    tls_descriptor.segment_present = 1;
    tls_descriptor.granularity = 0;
    tls_descriptor.zero = 0;
    tls_descriptor.operation_size = 1;
    tls_descriptor.descriptor_type = 1;
    tls_descriptor.type = 2;
    write_gdt_entry(GDT_SELECTOR_TLS, tls_descriptor); // tls3

    Descriptor fs_descriptor;
    fs_descriptor.set_base(this);
    fs_descriptor.set_limit(sizeof(Processor));
    fs_descriptor.dpl = 0;
    fs_descriptor.segment_present = 1;
    fs_descriptor.granularity = 0;
    fs_descriptor.zero = 0;
    fs_descriptor.operation_size = 1;
    fs_descriptor.descriptor_type = 1;
    fs_descriptor.type = 2;
    write_gdt_entry(GDT_SELECTOR_PROC, fs_descriptor); // fs0

    Descriptor tss_descriptor;
    tss_descriptor.set_base(&m_tss);
    tss_descriptor.set_limit(sizeof(TSS32));
    tss_descriptor.dpl = 0;
    tss_descriptor.segment_present = 1;
    tss_descriptor.granularity = 0;
    tss_descriptor.zero = 0;
    tss_descriptor.operation_size = 1;
    tss_descriptor.descriptor_type = 0;
    tss_descriptor.type = 9;
    write_gdt_entry(GDT_SELECTOR_TSS, tss_descriptor); // tss

    flush_gdt();
    load_task_register(GDT_SELECTOR_TSS);

    asm volatile(
        "mov %%ax, %%ds\n"
        "mov %%ax, %%es\n"
        "mov %%ax, %%gs\n"
        "mov %%ax, %%ss\n" ::"a"(GDT_SELECTOR_DATA0)
        : "memory");
    set_fs(GDT_SELECTOR_PROC);

    // Make sure CS points to the kernel code descriptor.
    asm volatile(
        "ljmpl $" __STRINGIFY(GDT_SELECTOR_CODE0) ", $sanity\n"
        "sanity:\n");
}

void Processor::set_thread_specific(u8* data, size_t len)
{
    auto& descriptor = get_gdt_entry(GDT_SELECTOR_TLS);
    descriptor.set_base(data);
    descriptor.set_limit(len);
}

}

#ifdef DEBUG
void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func)
{
    asm volatile("cli");
    klog() << "ASSERTION FAILED: " << msg << "\n"
           << file << ":" << line << " in " << func;

    // Switch back to the current process's page tables if there are any.
    // Otherwise stack walking will be a disaster.
    auto process = Process::current();
    if (process)
        MM.enter_process_paging_scope(*process);

    Kernel::dump_backtrace();
    asm volatile("hlt");
    for (;;)
        ;
}
#endif

NonMaskableInterruptDisabler::NonMaskableInterruptDisabler()
{
    IO::out8(0x70, IO::in8(0x70) | 0x80);
}

NonMaskableInterruptDisabler::~NonMaskableInterruptDisabler()
{
    IO::out8(0x70, IO::in8(0x70) & 0x7F);
}
