/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Types.h>

#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/SharedIRQHandler.h>
#include <Kernel/Interrupts/SpuriousInterruptHandler.h>
#include <Kernel/Interrupts/UnhandledInterruptHandler.h>
#include <Kernel/Panic.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>
#include <Kernel/Sections.h>
#include <Kernel/Thread.h>
#include <Kernel/ThreadTracer.h>

#include <LibC/mallocdefs.h>

#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Arch/x86/ISRStubs.h>
#include <Kernel/Arch/x86/PageFault.h>
#include <Kernel/Arch/x86/TrapFrame.h>
#include <Kernel/KSyms.h>

extern FlatPtr start_of_unmap_after_init;
extern FlatPtr end_of_unmap_after_init;
extern FlatPtr start_of_ro_after_init;
extern FlatPtr end_of_ro_after_init;
extern FlatPtr start_of_kernel_ksyms;
extern FlatPtr end_of_kernel_ksyms;

namespace Kernel {

READONLY_AFTER_INIT static DescriptorTablePointer s_idtr;
READONLY_AFTER_INIT static IDTEntry s_idt[256];

static GenericInterruptHandler* s_interrupt_handler[GENERIC_INTERRUPT_HANDLERS_COUNT];

static EntropySource s_entropy_source_interrupts { EntropySource::Static::Interrupts };

// clang-format off

#if ARCH(I386)
#define EH_ENTRY(ec, title)                                                    \
    extern "C" void title##_asm_entry();                                       \
    extern "C" void title##_handler(TrapFrame*) __attribute__((used));         \
    NAKED void title##_asm_entry() {                                           \
        asm(                                                                   \
            "    pusha\n"                                                      \
            "    pushl %ds\n"                                                  \
            "    pushl %es\n"                                                  \
            "    pushl %fs\n"                                                  \
            "    pushl %gs\n"                                                  \
            "    pushl %ss\n"                                                  \
            "    mov $" __STRINGIFY(GDT_SELECTOR_DATA0) ", %ax\n"              \
            "    mov %ax, %ds\n"                                               \
            "    mov %ax, %es\n"                                               \
            "    mov $" __STRINGIFY(GDT_SELECTOR_PROC) ", %ax\n"               \
            "    mov %ax, %gs\n"                                               \
            "    pushl %esp \n" /* set TrapFrame::regs */                      \
            "    subl $" __STRINGIFY(TRAP_FRAME_SIZE - 4) ", %esp \n"          \
            "    pushl %esp \n"                                                \
            "    cld\n"                                                        \
            "    call enter_trap_no_irq \n"                                    \
            "    call " #title "_handler\n"                                    \
            "    jmp common_trap_exit \n"                                      \
        );                                                                     \
    }

#define EH_ENTRY_NO_CODE(ec, title)                                            \
    extern "C" void title##_asm_entry();                                       \
    extern "C" void title##_handler(TrapFrame*) __attribute__((used));         \
    NAKED void title##_asm_entry() {                                           \
        asm(                                                                   \
            "    pushl $0x0\n"                                                 \
            "    pusha\n"                                                      \
            "    pushl %ds\n"                                                  \
            "    pushl %es\n"                                                  \
            "    pushl %fs\n"                                                  \
            "    pushl %gs\n"                                                  \
            "    pushl %ss\n"                                                  \
            "    mov $" __STRINGIFY(GDT_SELECTOR_DATA0) ", %ax\n"              \
            "    mov %ax, %ds\n"                                               \
            "    mov %ax, %es\n"                                               \
            "    mov $" __STRINGIFY(GDT_SELECTOR_PROC) ", %ax\n"               \
            "    mov %ax, %gs\n"                                               \
            "    pushl %esp \n" /* set TrapFrame::regs */                      \
            "    subl $" __STRINGIFY(TRAP_FRAME_SIZE - 4) ", %esp \n"          \
            "    pushl %esp \n"                                                \
            "    cld\n"                                                        \
            "    call enter_trap_no_irq \n"                                    \
            "    call " #title "_handler\n"                                    \
            "    jmp common_trap_exit \n"                                      \
        );                                                                     \
    }

#elif ARCH(X86_64)
#define EH_ENTRY(ec, title)                                                    \
    extern "C" void title##_asm_entry();                                       \
    extern "C" void title##_handler(TrapFrame*) __attribute__((used));         \
    NAKED void title##_asm_entry() {                                           \
        asm(                                                                   \
            "    pushq %r15\n"                                                 \
            "    pushq %r14\n"                                                 \
            "    pushq %r13\n"                                                 \
            "    pushq %r12\n"                                                 \
            "    pushq %r11\n"                                                 \
            "    pushq %r10\n"                                                 \
            "    pushq %r9\n"                                                  \
            "    pushq %r8\n"                                                  \
            "    pushq %rax\n"                                                 \
            "    pushq %rcx\n"                                                 \
            "    pushq %rdx\n"                                                 \
            "    pushq %rbx\n"                                                 \
            "    pushq %rsp\n"                                                 \
            "    pushq %rbp\n"                                                 \
            "    pushq %rsi\n"                                                 \
            "    pushq %rdi\n"                                                 \
            "    pushq %rsp \n" /* set TrapFrame::regs */                      \
            "    subq $" __STRINGIFY(TRAP_FRAME_SIZE - 8) ", %rsp \n"          \
            "    subq $0x8, %rsp\n" /* align stack */                          \
            "    lea 0x8(%rsp), %rdi \n"                                       \
            "    cld\n"                                                        \
            "    call enter_trap_no_irq \n"                                    \
            "    lea 0x8(%rsp), %rdi \n"                                       \
            "    call " #title "_handler\n"                                    \
            "    addq $0x8, %rsp\n" /* undo alignment */                       \
            "    jmp common_trap_exit \n"                                      \
        );                                                                     \
    }

#define EH_ENTRY_NO_CODE(ec, title)                                            \
    extern "C" void title##_handler(TrapFrame*) __attribute__((used));         \
    extern "C" void title##_asm_entry();                                       \
    NAKED void title##_asm_entry() {                                           \
        asm(                                                                   \
            "    pushq $0x0\n"                                                 \
            "    pushq %r15\n"                                                 \
            "    pushq %r14\n"                                                 \
            "    pushq %r13\n"                                                 \
            "    pushq %r12\n"                                                 \
            "    pushq %r11\n"                                                 \
            "    pushq %r10\n"                                                 \
            "    pushq %r9\n"                                                  \
            "    pushq %r8\n"                                                  \
            "    pushq %rax\n"                                                 \
            "    pushq %rcx\n"                                                 \
            "    pushq %rdx\n"                                                 \
            "    pushq %rbx\n"                                                 \
            "    pushq %rsp\n"                                                 \
            "    pushq %rbp\n"                                                 \
            "    pushq %rsi\n"                                                 \
            "    pushq %rdi\n"                                                 \
            "    pushq %rsp \n" /* set TrapFrame::regs */                      \
            "    subq $" __STRINGIFY(TRAP_FRAME_SIZE - 8) ", %rsp \n"          \
            "    movq %rsp, %rdi \n"                                           \
            "    cld\n"                                                        \
            "    call enter_trap_no_irq \n"                                    \
            "    movq %rsp, %rdi \n"                                           \
            "    call " #title "_handler\n"                                    \
            "    jmp common_trap_exit \n"                                      \
        );                                                                     \
    }
#endif

// clang-format on

static void dump(const RegisterState& regs)
{
#if ARCH(I386)
    u16 ss;
    u32 esp;

    if (!(regs.cs & 3)) {
        ss = regs.ss;
        esp = regs.esp;
    } else {
        ss = regs.userspace_ss;
        esp = regs.userspace_esp;
    }
#else
    u64 rsp;

    if (!(regs.cs & 3))
        rsp = regs.rsp;
    else
        rsp = regs.userspace_rsp;
#endif

    dbgln("Exception code: {:04x} (isr: {:04x})", regs.exception_code, regs.isr_number);
#if ARCH(I386)
    dbgln("    pc={:#04x}:{:p} eflags={:p}", (u16)regs.cs, regs.eip, regs.eflags);
    dbgln(" stack={:#04x}:{:p}", ss, esp);
    dbgln("    ds={:#04x} es={:#04x} fs={:#04x} gs={:#04x}", (u16)regs.ds, (u16)regs.es, (u16)regs.fs, (u16)regs.gs);
    dbgln("   eax={:p} ebx={:p} ecx={:p} edx={:p}", regs.eax, regs.ebx, regs.ecx, regs.edx);
    dbgln("   ebp={:p} esp={:p} esi={:p} edi={:p}", regs.ebp, regs.esp, regs.esi, regs.edi);
    dbgln("   cr0={:p} cr2={:p} cr3={:p} cr4={:p}", read_cr0(), read_cr2(), read_cr3(), read_cr4());
#else
    dbgln("    pc={:#04x}:{:p} rflags={:p}", (u16)regs.cs, regs.rip, regs.rflags);
    dbgln(" stack={:p}", rsp);
    // FIXME: Add fs_base and gs_base here
    dbgln("   rax={:p} rbx={:p} rcx={:p} rdx={:p}", regs.rax, regs.rbx, regs.rcx, regs.rdx);
    dbgln("   rbp={:p} rsp={:p} rsi={:p} rdi={:p}", regs.rbp, regs.rsp, regs.rsi, regs.rdi);
    dbgln("    r8={:p}  r9={:p} r10={:p} r11={:p}", regs.r8, regs.r9, regs.r10, regs.r11);
    dbgln("   r12={:p} r13={:p} r14={:p} r15={:p}", regs.r12, regs.r13, regs.r14, regs.r15);
    dbgln("   cr0={:p} cr2={:p} cr3={:p} cr4={:p}", read_cr0(), read_cr2(), read_cr3(), read_cr4());
#endif
}

void handle_crash(RegisterState const& regs, char const* description, int signal, bool out_of_memory)
{
    auto* current_thread = Thread::current();
    if (!current_thread)
        PANIC("{} with !Thread::current()", description);

    auto crashed_in_kernel = (regs.cs & 3) == 0;
    if (!crashed_in_kernel && !current_thread->should_ignore_signal(signal) && !current_thread->is_signal_masked(signal)) {
        current_thread->send_urgent_signal_to_self(signal);
        return;
    }

    auto& process = current_thread->process();

    // If a process crashed while inspecting another process,
    // make sure we switch back to the right page tables.
    Memory::MemoryManager::enter_process_address_space(process);

    dmesgln("CRASH: CPU #{} {} in ring {}", Processor::current_id(), description, (regs.cs & 3));
    dump(regs);

    if (crashed_in_kernel)
        PANIC("Crash in ring 0");

    process.crash(signal, regs.ip(), out_of_memory);
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

// 14: Page Fault
EH_ENTRY(14, page_fault);
void page_fault_handler(TrapFrame* trap)
{
    clac();

    auto& regs = *trap->regs;
    auto fault_address = read_cr2();

    if constexpr (PAGE_FAULT_DEBUG) {
        u32 fault_page_directory = read_cr3();
        dbgln("CPU #{} ring {} {} page fault in PD={:#x}, {}{} {}",
            Processor::is_initialized() ? Processor::current_id() : 0,
            regs.cs & 3,
            regs.exception_code & 1 ? "PV" : "NP",
            fault_page_directory,
            regs.exception_code & 8 ? "reserved-bit " : "",
            regs.exception_code & 2 ? "write" : "read",
            VirtualAddress(fault_address));

        dump(regs);
    }

    bool faulted_in_kernel = !(regs.cs & 3);

    if (faulted_in_kernel && Processor::current_in_irq()) {
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

    if (current_thread) {
        current_thread->set_handling_page_fault(true);
        PerformanceManager::add_page_fault_event(*current_thread, regs);
    }

    ScopeGuard guard = [current_thread] {
        if (current_thread)
            current_thread->set_handling_page_fault(false);
    };

    VirtualAddress userspace_sp = VirtualAddress { regs.userspace_sp() };
    if (!faulted_in_kernel && !MM.validate_user_stack(current_thread->process().address_space(), userspace_sp)) {
        dbgln("Invalid stack pointer: {}", userspace_sp);
        return handle_crash(regs, "Bad stack on page fault", SIGSEGV);
    }

    if (fault_address >= (FlatPtr)&start_of_ro_after_init && fault_address < (FlatPtr)&end_of_ro_after_init) {
        dump(regs);
        PANIC("Attempt to write into READONLY_AFTER_INIT section");
    }

    if (fault_address >= (FlatPtr)&start_of_unmap_after_init && fault_address < (FlatPtr)&end_of_unmap_after_init) {
        dump(regs);
        auto sym = symbolicate_kernel_address(fault_address);
        PANIC("Attempt to access UNMAP_AFTER_INIT section ({:p}: {})", fault_address, sym ? sym->name : "(Unknown)");
    }

    if (fault_address >= (FlatPtr)&start_of_kernel_ksyms && fault_address < (FlatPtr)&end_of_kernel_ksyms) {
        dump(regs);
        PANIC("Attempt to access KSYMS section");
    }

    PageFault fault { regs.exception_code, VirtualAddress { fault_address } };
    auto response = MM.handle_page_fault(fault);

    if (response == PageFaultResponse::ShouldCrash || response == PageFaultResponse::OutOfMemory) {
        if (faulted_in_kernel && handle_safe_access_fault(regs, fault_address)) {
            // If this would be a ring0 (kernel) fault and the fault was triggered by
            // safe_memcpy, safe_strnlen, or safe_memset then we resume execution at
            // the appropriate _fault label rather than crashing
            return;
        }

        if (response != PageFaultResponse::OutOfMemory && current_thread) {
            if (current_thread->has_signal_handler(SIGSEGV)) {
                current_thread->send_urgent_signal_to_self(SIGSEGV);
                return;
            }
        }

        dbgln("Unrecoverable page fault, {}{}{} address {}",
            regs.exception_code & PageFaultFlags::ReservedBitViolation ? "reserved bit violation / " : "",
            regs.exception_code & PageFaultFlags::InstructionFetch ? "instruction fetch / " : "",
            regs.exception_code & PageFaultFlags::Write ? "write to" : "read from",
            VirtualAddress(fault_address));
        constexpr FlatPtr malloc_scrub_pattern = explode_byte(MALLOC_SCRUB_BYTE);
        constexpr FlatPtr free_scrub_pattern = explode_byte(FREE_SCRUB_BYTE);
        constexpr FlatPtr kmalloc_scrub_pattern = explode_byte(KMALLOC_SCRUB_BYTE);
        constexpr FlatPtr kfree_scrub_pattern = explode_byte(KFREE_SCRUB_BYTE);
        constexpr FlatPtr slab_alloc_scrub_pattern = explode_byte(SLAB_ALLOC_SCRUB_BYTE);
        constexpr FlatPtr slab_dealloc_scrub_pattern = explode_byte(SLAB_DEALLOC_SCRUB_BYTE);
        if ((fault_address & 0xffff0000) == (malloc_scrub_pattern & 0xffff0000)) {
            dbgln("Note: Address {} looks like it may be uninitialized malloc() memory", VirtualAddress(fault_address));
        } else if ((fault_address & 0xffff0000) == (free_scrub_pattern & 0xffff0000)) {
            dbgln("Note: Address {} looks like it may be recently free()'d memory", VirtualAddress(fault_address));
        } else if ((fault_address & 0xffff0000) == (kmalloc_scrub_pattern & 0xffff0000)) {
            dbgln("Note: Address {} looks like it may be uninitialized kmalloc() memory", VirtualAddress(fault_address));
        } else if ((fault_address & 0xffff0000) == (kfree_scrub_pattern & 0xffff0000)) {
            dbgln("Note: Address {} looks like it may be recently kfree()'d memory", VirtualAddress(fault_address));
        } else if ((fault_address & 0xffff0000) == (slab_alloc_scrub_pattern & 0xffff0000)) {
            dbgln("Note: Address {} looks like it may be uninitialized slab_alloc() memory", VirtualAddress(fault_address));
        } else if ((fault_address & 0xffff0000) == (slab_dealloc_scrub_pattern & 0xffff0000)) {
            dbgln("Note: Address {} looks like it may be recently slab_dealloc()'d memory", VirtualAddress(fault_address));
        } else if (fault_address < 4096) {
            dbgln("Note: Address {} looks like a possible nullptr dereference", VirtualAddress(fault_address));
        } else if constexpr (SANITIZE_PTRS) {
            constexpr FlatPtr refptr_scrub_pattern = explode_byte(REFPTR_SCRUB_BYTE);
            constexpr FlatPtr nonnullrefptr_scrub_pattern = explode_byte(NONNULLREFPTR_SCRUB_BYTE);
            constexpr FlatPtr ownptr_scrub_pattern = explode_byte(OWNPTR_SCRUB_BYTE);
            constexpr FlatPtr nonnullownptr_scrub_pattern = explode_byte(NONNULLOWNPTR_SCRUB_BYTE);
            constexpr FlatPtr threadsaferefptr_scrub_pattern = explode_byte(THREADSAFEREFPTR_SCRUB_BYTE);
            constexpr FlatPtr threadsafenonnullrefptr_scrub_pattern = explode_byte(THREADSAFENONNULLREFPTR_SCRUB_BYTE);

            if ((fault_address & 0xffff0000) == (refptr_scrub_pattern & 0xffff0000)) {
                dbgln("Note: Address {} looks like it may be a recently destroyed RefPtr", VirtualAddress(fault_address));
            } else if ((fault_address & 0xffff0000) == (nonnullrefptr_scrub_pattern & 0xffff0000)) {
                dbgln("Note: Address {} looks like it may be a recently destroyed NonnullRefPtr", VirtualAddress(fault_address));
            } else if ((fault_address & 0xffff0000) == (ownptr_scrub_pattern & 0xffff0000)) {
                dbgln("Note: Address {} looks like it may be a recently destroyed OwnPtr", VirtualAddress(fault_address));
            } else if ((fault_address & 0xffff0000) == (nonnullownptr_scrub_pattern & 0xffff0000)) {
                dbgln("Note: Address {} looks like it may be a recently destroyed NonnullOwnPtr", VirtualAddress(fault_address));
            } else if ((fault_address & 0xffff0000) == (threadsaferefptr_scrub_pattern & 0xffff0000)) {
                dbgln("Note: Address {} looks like it may be a recently destroyed ThreadSafeRefPtr", VirtualAddress(fault_address));
            } else if ((fault_address & 0xffff0000) == (threadsafenonnullrefptr_scrub_pattern & 0xffff0000)) {
                dbgln("Note: Address {} looks like it may be a recently destroyed ThreadSafeNonnullRefPtr", VirtualAddress(fault_address));
            }
        }

        if (current_thread) {
            auto& current_process = current_thread->process();
            if (current_process.is_user_process()) {
                auto fault_address_string = KString::formatted("{:p}", fault_address);
                auto fault_address_view = fault_address_string.is_error() ? ""sv : fault_address_string.value()->view();
                (void)current_process.try_set_coredump_property("fault_address"sv, fault_address_view);
                (void)current_process.try_set_coredump_property("fault_type"sv, fault.type() == PageFault::Type::PageNotPresent ? "NotPresent"sv : "ProtectionViolation"sv);
                StringView fault_access;
                if (fault.is_instruction_fetch())
                    fault_access = "Execute"sv;
                else
                    fault_access = fault.access() == PageFault::Access::Read ? "Read"sv : "Write"sv;
                (void)current_process.try_set_coredump_property("fault_access"sv, fault_access);
            }
        }

        return handle_crash(regs, "Page Fault", SIGSEGV, response == PageFaultResponse::OutOfMemory);
    } else if (response == PageFaultResponse::Continue) {
        dbgln_if(PAGE_FAULT_DEBUG, "Continuing after resolved page fault");
    } else {
        VERIFY_NOT_REACHED();
    }
}

EH_ENTRY_NO_CODE(1, debug);
void debug_handler(TrapFrame* trap)
{
    clac();
    auto& regs = *trap->regs;
    auto current_thread = Thread::current();
    auto& process = current_thread->process();
    if ((regs.cs & 3) == 0) {
        PANIC("Debug exception in ring 0");
    }
    constexpr u8 REASON_SINGLESTEP = 14;
    auto debug_status = read_dr6();
    auto should_trap_mask = (1 << REASON_SINGLESTEP) | 0b1111;
    if ((debug_status & should_trap_mask) == 0)
        return;
    if (auto tracer = process.tracer()) {
        tracer->set_regs(regs);
    }
    current_thread->send_urgent_signal_to_self(SIGTRAP);
    write_dr6(debug_status & ~(should_trap_mask));
}

EH_ENTRY_NO_CODE(3, breakpoint);
void breakpoint_handler(TrapFrame* trap)
{
    clac();
    auto& regs = *trap->regs;
    auto current_thread = Thread::current();
    auto& process = current_thread->process();
    if ((regs.cs & 3) == 0) {
        PANIC("Breakpoint trap in ring 0");
    }
    if (auto tracer = process.tracer()) {
        tracer->set_regs(regs);
    }
    current_thread->send_urgent_signal_to_self(SIGTRAP);
}

#define EH(i, msg)                                                                                            \
    static void _exception##i()                                                                               \
    {                                                                                                         \
        dbgln("{}", msg);                                                                                     \
        PANIC("cr0={:08x} cr2={:08x} cr3={:08x} cr4={:08x}", read_cr0(), read_cr2(), read_cr3(), read_cr4()); \
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

extern "C" void pre_init_finished(void) __attribute__((used));
extern "C" void post_init_finished(void) __attribute__((used));
extern "C" void handle_interrupt(TrapFrame*) __attribute__((used));

extern "C" UNMAP_AFTER_INIT void pre_init_finished(void)
{
    VERIFY(g_scheduler_lock.is_locked_by_current_processor());

    // Because init_finished() will wait on the other APs, we need
    // to release the scheduler lock so that the other APs can also get
    // to this point

    // The target flags will get restored upon leaving the trap
    u32 prev_flags = cpu_flags();
    Scheduler::leave_on_first_switch(prev_flags);
}

extern "C" UNMAP_AFTER_INIT void post_init_finished(void)
{
    // We need to re-acquire the scheduler lock before a context switch
    // transfers control into the idle loop, which needs the lock held
    Scheduler::prepare_for_idle_loop();
}

void handle_interrupt(TrapFrame* trap)
{
    clac();
    auto& regs = *trap->regs;
    VERIFY(regs.isr_number >= IRQ_VECTOR_BASE && regs.isr_number <= (IRQ_VECTOR_BASE + GENERIC_INTERRUPT_HANDLERS_COUNT));
    u8 irq = (u8)(regs.isr_number - 0x50);
    s_entropy_source_interrupts.add_random_event(irq);
    auto* handler = s_interrupt_handler[irq];
    VERIFY(handler);
    handler->increment_invoking_counter();
    handler->handle_interrupt(regs);
    handler->eoi();
}

const DescriptorTablePointer& get_idtr()
{
    return s_idtr;
}

static void unimp_trap()
{
    PANIC("Unhandled IRQ");
}

GenericInterruptHandler& get_interrupt_handler(u8 interrupt_number)
{
    auto*& handler_slot = s_interrupt_handler[interrupt_number];
    VERIFY(handler_slot != nullptr);
    return *handler_slot;
}

static void revert_to_unused_handler(u8 interrupt_number)
{
    auto handler = new UnhandledInterruptHandler(interrupt_number);
    handler->register_interrupt_handler();
}

void register_generic_interrupt_handler(u8 interrupt_number, GenericInterruptHandler& handler)
{
    VERIFY(interrupt_number < GENERIC_INTERRUPT_HANDLERS_COUNT);
    auto*& handler_slot = s_interrupt_handler[interrupt_number];
    if (handler_slot != nullptr) {
        if (handler_slot->type() == HandlerType::UnhandledInterruptHandler) {
            if (handler_slot) {
                auto* unhandled_handler = static_cast<UnhandledInterruptHandler*>(handler_slot);
                unhandled_handler->unregister_interrupt_handler();
                delete unhandled_handler;
            }
            handler_slot = &handler;
            return;
        }
        if (handler_slot->is_shared_handler() && !handler_slot->is_sharing_with_others()) {
            VERIFY(handler_slot->type() == HandlerType::SharedIRQHandler);
            static_cast<SharedIRQHandler*>(handler_slot)->register_handler(handler);
            return;
        }
        if (!handler_slot->is_shared_handler()) {
            if (handler_slot->type() == HandlerType::SpuriousInterruptHandler) {
                static_cast<SpuriousInterruptHandler*>(handler_slot)->register_handler(handler);
                return;
            }
            VERIFY(handler_slot->type() == HandlerType::IRQHandler);
            auto& previous_handler = *handler_slot;
            handler_slot = nullptr;
            SharedIRQHandler::initialize(interrupt_number);
            VERIFY(handler_slot);
            static_cast<SharedIRQHandler*>(handler_slot)->register_handler(previous_handler);
            static_cast<SharedIRQHandler*>(handler_slot)->register_handler(handler);
            return;
        }
        VERIFY_NOT_REACHED();
    } else {
        handler_slot = &handler;
    }
}

void unregister_generic_interrupt_handler(u8 interrupt_number, GenericInterruptHandler& handler)
{
    auto*& handler_slot = s_interrupt_handler[interrupt_number];
    VERIFY(handler_slot != nullptr);
    if (handler_slot->type() == HandlerType::UnhandledInterruptHandler) {
        dbgln("Trying to unregister unused handler (?)");
        return;
    }
    if (handler_slot->is_shared_handler() && !handler_slot->is_sharing_with_others()) {
        VERIFY(handler_slot->type() == HandlerType::SharedIRQHandler);
        auto* shared_handler = static_cast<SharedIRQHandler*>(handler_slot);
        shared_handler->unregister_handler(handler);
        if (!shared_handler->sharing_devices_count()) {
            handler_slot = nullptr;
            revert_to_unused_handler(interrupt_number);
        }
        return;
    }
    if (!handler_slot->is_shared_handler()) {
        VERIFY(handler_slot->type() == HandlerType::IRQHandler);
        handler_slot = nullptr;
        revert_to_unused_handler(interrupt_number);
        return;
    }
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT void register_interrupt_handler(u8 index, void (*handler)())
{
    // FIXME: Is the Gate Type really required to be an Interrupt
    // FIXME: What's up with that storage segment 0?
    s_idt[index] = IDTEntry((FlatPtr)handler, GDT_SELECTOR_CODE0, IDTEntryType::InterruptGate32, 0, 0);
}

UNMAP_AFTER_INIT void register_user_callable_interrupt_handler(u8 index, void (*handler)())
{
    // FIXME: Is the Gate Type really required to be a Trap
    // FIXME: What's up with that storage segment 0?
    s_idt[index] = IDTEntry((FlatPtr)handler, GDT_SELECTOR_CODE0, IDTEntryType::TrapGate32, 0, 3);
}

UNMAP_AFTER_INIT void flush_idt()
{
    asm("lidt %0" ::"m"(s_idtr));
}

UNMAP_AFTER_INIT void idt_init()
{
    s_idtr.address = s_idt;
    s_idtr.limit = 256 * sizeof(IDTEntry) - 1;

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

    dbgln("Initializing unhandled interrupt handlers");
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

    for (u8 i = 0; i < GENERIC_INTERRUPT_HANDLERS_COUNT; ++i) {
        auto* handler = new UnhandledInterruptHandler(i);
        handler->register_interrupt_handler();
    }

    flush_idt();
}

}
