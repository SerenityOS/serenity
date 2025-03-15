/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>

namespace Kernel {

extern "C" u8 asm_trap_handler[];

Processor* g_current_processor;

static void store_fpu_state(FPUState* fpu_state)
{
    asm volatile(
        "fsd f0, 0*8(%0) \n"
        "fsd f1, 1*8(%0) \n"
        "fsd f2, 2*8(%0) \n"
        "fsd f3, 3*8(%0) \n"
        "fsd f4, 4*8(%0) \n"
        "fsd f5, 5*8(%0) \n"
        "fsd f6, 6*8(%0) \n"
        "fsd f7, 7*8(%0) \n"
        "fsd f8, 8*8(%0) \n"
        "fsd f9, 9*8(%0) \n"
        "fsd f10, 10*8(%0) \n"
        "fsd f11, 11*8(%0) \n"
        "fsd f12, 12*8(%0) \n"
        "fsd f13, 13*8(%0) \n"
        "fsd f14, 14*8(%0) \n"
        "fsd f15, 15*8(%0) \n"
        "fsd f16, 16*8(%0) \n"
        "fsd f17, 17*8(%0) \n"
        "fsd f18, 18*8(%0) \n"
        "fsd f19, 19*8(%0) \n"
        "fsd f20, 20*8(%0) \n"
        "fsd f21, 21*8(%0) \n"
        "fsd f22, 22*8(%0) \n"
        "fsd f23, 23*8(%0) \n"
        "fsd f24, 24*8(%0) \n"
        "fsd f25, 25*8(%0) \n"
        "fsd f26, 26*8(%0) \n"
        "fsd f27, 27*8(%0) \n"
        "fsd f28, 28*8(%0) \n"
        "fsd f29, 29*8(%0) \n"
        "fsd f30, 30*8(%0) \n"
        "fsd f31, 31*8(%0) \n"

        "csrr t0, fcsr \n"
        "sd t0, 32*8(%0) \n" ::"r"(fpu_state)
        : "t0", "memory");
}

static void load_fpu_state(FPUState* fpu_state)
{
    asm volatile(
        "fld f0, 0*8(%0) \n"
        "fld f1, 1*8(%0) \n"
        "fld f2, 2*8(%0) \n"
        "fld f3, 3*8(%0) \n"
        "fld f4, 4*8(%0) \n"
        "fld f5, 5*8(%0) \n"
        "fld f6, 6*8(%0) \n"
        "fld f7, 7*8(%0) \n"
        "fld f8, 8*8(%0) \n"
        "fld f9, 9*8(%0) \n"
        "fld f10, 10*8(%0) \n"
        "fld f11, 11*8(%0) \n"
        "fld f12, 12*8(%0) \n"
        "fld f13, 13*8(%0) \n"
        "fld f14, 14*8(%0) \n"
        "fld f15, 15*8(%0) \n"
        "fld f16, 16*8(%0) \n"
        "fld f17, 17*8(%0) \n"
        "fld f18, 18*8(%0) \n"
        "fld f19, 19*8(%0) \n"
        "fld f20, 20*8(%0) \n"
        "fld f21, 21*8(%0) \n"
        "fld f22, 22*8(%0) \n"
        "fld f23, 23*8(%0) \n"
        "fld f24, 24*8(%0) \n"
        "fld f25, 25*8(%0) \n"
        "fld f26, 26*8(%0) \n"
        "fld f27, 27*8(%0) \n"
        "fld f28, 28*8(%0) \n"
        "fld f29, 29*8(%0) \n"
        "fld f30, 30*8(%0) \n"
        "fld f31, 31*8(%0) \n"

        "ld t0, 32*8(%0) \n"
        "csrw fcsr, t0 \n" ::"r"(fpu_state)
        : "t0", "memory");
}

template<typename T>
void ProcessorBase<T>::early_initialize(u32 cpu)
{
    VERIFY(g_current_processor == nullptr);
    m_cpu = cpu;

    g_current_processor = static_cast<Processor*>(this);
}

template<typename T>
void ProcessorBase<T>::initialize(u32)
{
    m_deferred_call_pool.init();

    // FIXME: Actually set the correct count when we support SMP on riscv64.
    g_total_processors.store(1, AK::MemoryOrder::memory_order_release);

    // Enable the FPU
    auto sstatus = RISCV64::CSR::SSTATUS::read();
    sstatus.FS = RISCV64::CSR::SSTATUS::FloatingPointStatus::Initial;
    RISCV64::CSR::SSTATUS::write(sstatus);

    store_fpu_state(&s_clean_fpu_state);

    RISCV64::CSR::write(RISCV64::CSR::Address::STVEC, bit_cast<FlatPtr>(+asm_trap_handler));

    initialize_interrupts();
}

template<typename T>
[[noreturn]] void ProcessorBase<T>::halt()
{
    // WFI ignores the value of sstatus.SIE, so we can't use disable_interrupts().
    // Instead, disable all interrupts sources by setting sie to zero.
    RISCV64::CSR::write(RISCV64::CSR::Address::SIE, 0);
    for (;;)
        asm volatile("wfi");
}

template<typename T>
void ProcessorBase<T>::flush_tlb_local(VirtualAddress vaddr, size_t page_count)
{
    auto addr = vaddr.get();
    while (page_count > 0) {
        asm volatile("sfence.vma %0"
                     :
                     : "r"(addr)
                     : "memory");
        addr += PAGE_SIZE;
        page_count--;
    }
}

template<typename T>
void ProcessorBase<T>::flush_entire_tlb_local()
{
    asm volatile("sfence.vma");
}

template<typename T>
void ProcessorBase<T>::flush_tlb(Memory::PageDirectory const*, VirtualAddress vaddr, size_t page_count)
{
    // FIXME: Use the SBI RFENCE extension to flush the TLB of other harts when we support SMP on riscv64.
    flush_tlb_local(vaddr, page_count);
}

template<typename T>
void ProcessorBase<T>::flush_instruction_cache(VirtualAddress, size_t)
{
    // FIXME: Use the SBI RFENCE extension to flush the instruction cache of other harts when we support SMP on riscv64.
    asm volatile("fence.i" ::: "memory");
}

template<typename T>
u32 ProcessorBase<T>::clear_critical()
{
    InterruptDisabler disabler;
    auto prev_critical = in_critical();
    auto& proc = current();
    proc.m_in_critical = 0;
    if (proc.m_in_irq == 0)
        proc.check_invoke_scheduler();
    return prev_critical;
}

template<typename T>
u32 ProcessorBase<T>::smp_wake_n_idle_processors(u32)
{
    // FIXME: Actually wake up other cores when SMP is supported for riscv64.
    return 0;
}

template<typename T>
void ProcessorBase<T>::initialize_context_switching(Thread& initial_thread)
{
    VERIFY(initial_thread.process().is_kernel_process());

    m_scheduler_initialized.set();

    // FIXME: Figure out if we need to call {pre_,post_,}init_finished once riscv64 supports SMP
    Processor::set_current_in_scheduler(true);

    auto& regs = initial_thread.regs();
    asm volatile(
        "mv sp, %[new_sp] \n"

        "addi sp, sp, -32 \n"
        "sd %[from_to_thread], 0(sp) \n"
        "sd %[from_to_thread], 8(sp) \n"

        "jr %[new_ip] \n" ::[new_sp] "r"(regs.sp()),
        [new_ip] "r"(regs.ip()),
        [from_to_thread] "r"(&initial_thread)
        : "t0");

    VERIFY_NOT_REACHED();
}

template<typename T>
void ProcessorBase<T>::switch_context(Thread*& from_thread, Thread*& to_thread)
{
    VERIFY(!m_in_irq);
    VERIFY(m_in_critical == 1);

    dbgln_if(CONTEXT_SWITCH_DEBUG, "switch_context --> switching out of: {} {}", VirtualAddress(from_thread), *from_thread);

    // m_in_critical is restored in enter_thread_context
    from_thread->save_critical(m_in_critical);

    asm volatile(
        // Store a RegisterState of from_thread on from_thread's stack
        "addi sp, sp, -(34 * 8) \n"

        "sd x1, 0*8(sp) \n"
        // sp
        "sd x3, 2*8(sp) \n"
        "sd x4, 3*8(sp) \n"
        "sd x5, 4*8(sp) \n"
        "sd x6, 5*8(sp) \n"
        "sd x7, 6*8(sp) \n"
        "sd x8, 7*8(sp) \n"
        "sd x9, 8*8(sp) \n"
        "sd x10, 9*8(sp) \n"
        "sd x11, 10*8(sp) \n"
        "sd x12, 11*8(sp) \n"
        "sd x13, 12*8(sp) \n"
        "sd x14, 13*8(sp) \n"
        "sd x15, 14*8(sp) \n"
        "sd x16, 15*8(sp) \n"
        "sd x17, 16*8(sp) \n"
        "sd x18, 17*8(sp) \n"
        "sd x19, 18*8(sp) \n"
        "sd x20, 19*8(sp) \n"
        "sd x21, 20*8(sp) \n"
        "sd x22, 21*8(sp) \n"
        "sd x23, 22*8(sp) \n"
        "sd x24, 23*8(sp) \n"
        "sd x25, 24*8(sp) \n"
        "sd x26, 25*8(sp) \n"
        "sd x27, 26*8(sp) \n"
        "sd x28, 27*8(sp) \n"
        "sd x29, 28*8(sp) \n"
        "sd x30, 29*8(sp) \n"
        "sd x31, 30*8(sp) \n"

        // Store current sp as from_thread's sp.
        "sd sp, %[from_sp] \n"

        // Store current fp as from_thread's sp.
        // This is needed to make capture_stack_trace() work.
        "sd fp, %[from_fp] \n"

        // Set from_thread's pc to label "1"
        "la t0, 1f \n"
        "sd t0, %[from_ip] \n"

        // Switch to to_thread's stack
        "ld sp, %[to_sp] \n"

        // Store from_thread, to_thread, to_ip on to_thread's stack
        "addi sp, sp, -(4 * 8) \n"
        "ld a0, %[from_thread] \n"
        "sd a0, 0*8(sp) \n"
        "ld a1, %[to_thread] \n"
        "sd a1, 1*8(sp) \n"
        "ld s1, %[to_ip] \n"
        "sd s1, 2*8(sp) \n"

        // enter_thread_context(from_thread, to_thread)
        "call enter_thread_context \n"

        // Jump to to_ip
        "jr s1 \n"

        // A thread enters here when they were already scheduled at least once
        "1: \n"
        "addi sp, sp, (4 * 8) \n"

        // Restore the RegisterState of to_thread
        "ld x1, 0*8(sp) \n"
        // sp
        "ld x3, 2*8(sp) \n"
        "ld x4, 3*8(sp) \n"
        "ld x5, 4*8(sp) \n"
        "ld x6, 5*8(sp) \n"
        "ld x7, 6*8(sp) \n"
        "ld x8, 7*8(sp) \n"
        "ld x9, 8*8(sp) \n"
        "ld x10, 9*8(sp) \n"
        "ld x11, 10*8(sp) \n"
        "ld x12, 11*8(sp) \n"
        "ld x13, 12*8(sp) \n"
        "ld x14, 13*8(sp) \n"
        "ld x15, 14*8(sp) \n"
        "ld x16, 15*8(sp) \n"
        "ld x17, 16*8(sp) \n"
        "ld x18, 17*8(sp) \n"
        "ld x19, 18*8(sp) \n"
        "ld x20, 19*8(sp) \n"
        "ld x21, 20*8(sp) \n"
        "ld x22, 21*8(sp) \n"
        "ld x23, 22*8(sp) \n"
        "ld x24, 23*8(sp) \n"
        "ld x25, 24*8(sp) \n"
        "ld x26, 25*8(sp) \n"
        "ld x27, 26*8(sp) \n"
        "ld x28, 27*8(sp) \n"
        "ld x29, 28*8(sp) \n"
        "ld x30, 29*8(sp) \n"
        "ld x31, 30*8(sp) \n"

        "addi sp, sp, -(4 * 8) \n"
        "ld t0, 0*8(sp) \n"
        "sd t0, %[from_thread] \n"
        "ld t0, 1*8(sp) \n"
        "sd t0, %[to_thread] \n"

        "addi sp, sp, (34 * 8) + (4 * 8) \n"
        :
        [from_ip] "=m"(from_thread->regs().pc),
        [from_sp] "=m"(from_thread->regs().x[1]),
        [from_fp] "=m"(from_thread->regs().x[7]),
        "=m"(from_thread),
        "=m"(to_thread)

        : [to_ip] "m"(to_thread->regs().pc),
        [to_sp] "m"(to_thread->regs().x[1]),
        [from_thread] "m"(from_thread),
        [to_thread] "m"(to_thread)
        : "memory", "t0", "s1", "a0", "a1");

    dbgln_if(CONTEXT_SWITCH_DEBUG, "switch_context <-- from {} {} to {} {}", VirtualAddress(from_thread), *from_thread, VirtualAddress(to_thread), *to_thread);
}

extern "C" FlatPtr do_init_context(Thread* thread, u32 new_interrupts_state)
{
    VERIFY_INTERRUPTS_DISABLED();

    thread->regs().sstatus.SPIE = (new_interrupts_state == to_underlying(InterruptsState::Enabled));

    return Processor::current().init_context(*thread, true);
}

template<typename T>
void ProcessorBase<T>::assume_context(Thread& thread, InterruptsState new_interrupts_state)
{
    dbgln_if(CONTEXT_SWITCH_DEBUG, "Assume context for thread {} {}", VirtualAddress(&thread), thread);

    VERIFY_INTERRUPTS_DISABLED();
    Scheduler::prepare_after_exec();
    // in_critical() should be 2 here. The critical section in Process::exec
    // and then the scheduler lock
    VERIFY(Processor::in_critical() == 2);

    do_assume_context(&thread, to_underlying(new_interrupts_state));

    VERIFY_NOT_REACHED();
}

template<typename T>
FlatPtr ProcessorBase<T>::init_context(Thread& thread, bool leave_crit)
{
    VERIFY(g_scheduler_lock.is_locked());
    if (leave_crit) {
        // Leave the critical section we set up in Process::exec,
        // but because we still have the scheduler lock we should end up with 1
        VERIFY(in_critical() == 2);
        m_in_critical = 1; // leave it without triggering anything or restoring flags
    }

    u64 kernel_stack_top = thread.kernel_stack_top();

    // Add a random offset between 0-256 (16-byte aligned)
    kernel_stack_top -= round_up_to_power_of_two(get_fast_random<u8>(), 16);

    u64 stack_top = kernel_stack_top;

    auto& thread_regs = thread.regs();

    // Push a RegisterState and TrapFrame onto the stack, which will be popped of the stack and restored into the
    // state of the processor by restore_previous_context.
    stack_top -= sizeof(RegisterState);
    RegisterState& frame = *reinterpret_cast<RegisterState*>(stack_top);
    memcpy(frame.x, thread_regs.x, sizeof(thread_regs.x));

    // We don't overwrite the return address register if it's not 0, since that means this thread's register state was already initialized with
    // an existing return address register value (e.g. it was fork()'ed), so we assume exit_kernel_thread is already saved as previous RA on the
    // stack somewhere.
    if (frame.x[0] == 0x0) {
        // x1 is the return address register for the riscv64 ABI, so this will return to exit_kernel_thread when main thread function returns.
        frame.x[0] = FlatPtr(&exit_kernel_thread);
    }
    frame.sepc = thread_regs.pc;
    frame.set_userspace_sp(thread_regs.sp());
    frame.sstatus = thread_regs.sstatus;

    // Push a TrapFrame onto the stack
    stack_top -= sizeof(TrapFrame);
    TrapFrame& trap = *reinterpret_cast<TrapFrame*>(stack_top);
    trap.regs = &frame;
    trap.next_trap = nullptr;

    if constexpr (CONTEXT_SWITCH_DEBUG) {
        dbgln("init_context {} ({}) set up to execute at ip={}, sp={}, stack_top={}",
            thread,
            VirtualAddress(&thread),
            VirtualAddress(thread_regs.pc),
            VirtualAddress(thread_regs.sp()),
            VirtualAddress(stack_top));
    }

    // This make sure the thread first executes thread_context_first_enter, which will actually call restore_previous_context
    // which restores the context set up above.
    thread_regs.set_sp(stack_top);
    thread_regs.set_ip(FlatPtr(&thread_context_first_enter));

    return stack_top;
}

// FIXME: Figure out if we can fully share this code with x86.
template<typename T>
void ProcessorBase<T>::exit_trap(TrapFrame& trap)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(&Processor::current() == this);

    // Temporarily enter a critical section. This is to prevent critical
    // sections entered and left within e.g. smp_process_pending_messages
    // to trigger a context switch while we're executing this function
    // See the comment at the end of the function why we don't use
    // ScopedCritical here.
    m_in_critical = m_in_critical + 1;

    // FIXME: Figure out if we need prev_irq_level, see duplicated code in Kernel/Arch/x86/common/Processor.cpp
    m_in_irq = 0;

    // Process the deferred call queue. Among other things, this ensures
    // that any pending thread unblocks happen before we enter the scheduler.
    m_deferred_call_pool.execute_pending();

    auto* current_thread = Processor::current_thread();
    if (current_thread) {
        auto& current_trap = current_thread->current_trap();
        current_trap = trap.next_trap;
        ExecutionMode new_previous_mode;
        if (current_trap) {
            VERIFY(current_trap->regs);
            new_previous_mode = current_trap->regs->previous_mode();
        } else {
            // If we don't have a higher level trap then we're back in user mode.
            // Which means that the previous mode prior to being back in user mode was kernel mode
            new_previous_mode = ExecutionMode::Kernel;
        }

        if (current_thread->set_previous_mode(new_previous_mode))
            current_thread->update_time_scheduled(TimeManagement::scheduler_current_time(), true, false);
    }

    VERIFY_INTERRUPTS_DISABLED();

    // Leave the critical section without actually enabling interrupts.
    // We don't want context switches to happen until we're explicitly
    // triggering a switch in check_invoke_scheduler.
    m_in_critical = m_in_critical - 1;
    if (!m_in_irq && !m_in_critical)
        check_invoke_scheduler();
}

extern "C" void context_first_init(Thread* from_thread, Thread* to_thread);
extern "C" void context_first_init(Thread* from_thread, Thread* to_thread)
{
    do_context_first_init(from_thread, to_thread);
}

extern "C" void enter_thread_context(Thread* from_thread, Thread* to_thread);
extern "C" void enter_thread_context(Thread* from_thread, Thread* to_thread)
{
    VERIFY(from_thread == to_thread || from_thread->state() != Thread::State::Running);
    VERIFY(to_thread->state() == Thread::State::Running);

    Processor::set_current_thread(*to_thread);

    store_fpu_state(&from_thread->fpu_state());

    auto& from_regs = from_thread->regs();
    auto& to_regs = to_thread->regs();
    if (from_regs.satp != to_regs.satp) {
        RISCV64::CSR::SATP::write(to_regs.satp);
        Processor::flush_entire_tlb_local();
    }

    to_thread->set_cpu(Processor::current().id());

    auto in_critical = to_thread->saved_critical();
    VERIFY(in_critical > 0);
    Processor::restore_critical(in_critical);

    load_fpu_state(&to_thread->fpu_state());
}

NAKED void thread_context_first_enter()
{
    asm(
        "ld a0, 0(sp) \n"
        "ld a1, 8(sp) \n"
        "addi sp, sp, 32 \n"
        "call context_first_init \n"
        "mv a0, sp \n"
        "call exit_trap \n"
        "tail restore_context_and_sret \n");
}

NAKED void do_assume_context(Thread*, u32)
{
    // clang-format off
    asm(
        "mv s1, a0 \n" // save thread ptr
        // We're going to call Processor::init_context, so just make sure
        // we have enough stack space so we don't stomp over it
        "addi sp, sp, -" __STRINGIFY(8 + REGISTER_STATE_SIZE + TRAP_FRAME_SIZE + 8) " \n"
        "call do_init_context \n"
        "mv sp, a0 \n"  // move stack pointer to what Processor::init_context set up for us
        "mv a0, s1 \n" // to_thread
        "mv a1, s1 \n" // from_thread
        "addi sp, sp, -32 \n"
        "sd s1, 0(sp) \n"
        "sd s1, 8(sp) \n"
        "la ra, thread_context_first_enter \n" // should be same as regs.sepc
        "tail enter_thread_context \n");
    // clang-format on
}

template<typename T>
StringView ProcessorBase<T>::platform_string()
{
    return "riscv64"sv;
}

template<typename T>
void ProcessorBase<T>::wait_for_interrupt() const
{
    asm("wfi");
}

template<typename T>
Processor& ProcessorBase<T>::by_id(u32)
{
    TODO_RISCV64();
}

}

#include <Kernel/Arch/ProcessorFunctions.include>
