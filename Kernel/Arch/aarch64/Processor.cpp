/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Vector.h>

#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/CPU.h>
#include <Kernel/Arch/aarch64/CPUID.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/Thread.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

extern "C" u8 vector_table_el1[];

extern "C" void context_first_init(Thread* from_thread, Thread* to_thread) __attribute__((used));
extern "C" void enter_thread_context(Thread* from_thread, Thread* to_thread) __attribute__((used));

Processor* g_current_processor;

template<typename T>
void ProcessorBase<T>::early_initialize(u32 cpu)
{
    VERIFY(g_current_processor == nullptr);
    m_cpu = cpu;
    m_features = detect_cpu_features();
    m_physical_address_bit_width = detect_physical_address_bit_width();
    m_virtual_address_bit_width = detect_virtual_address_bit_width();

    g_current_processor = static_cast<Processor*>(this);
}

template<typename T>
void ProcessorBase<T>::initialize(u32)
{
    m_deferred_call_pool.init();

    // FIXME: Actually set the correct count when we support SMP on AArch64.
    g_total_processors.store(1, AK::MemoryOrder::memory_order_release);

    dmesgln("CPU[{}]: Supports {}", m_cpu, build_cpu_feature_names(m_features));
    dmesgln("CPU[{}]: Physical address bit width: {}", m_cpu, m_physical_address_bit_width);
    dmesgln("CPU[{}]: Virtual address bit width: {}", m_cpu, m_virtual_address_bit_width);
    if (!has_feature(CPUFeature::RNG))
        dmesgln("CPU[{}]: {} not detected, randomness will be poor", m_cpu, cpu_feature_to_description(CPUFeature::RNG));

    store_fpu_state(&s_clean_fpu_state);

    Aarch64::Asm::load_el1_vector_table(+vector_table_el1);

    initialize_interrupts();
}

template<typename T>
[[noreturn]] void ProcessorBase<T>::halt()
{
    disable_interrupts();
    for (;;)
        asm volatile("wfi");
}

template<typename T>
void ProcessorBase<T>::flush_tlb_local(VirtualAddress, size_t)
{
    // FIXME: Figure out how to flush a single page
    asm volatile("dsb ishst");
    asm volatile("tlbi vmalle1");
    asm volatile("dsb ish");
    asm volatile("isb");
}

template<typename T>
void ProcessorBase<T>::flush_entire_tlb_local()
{
    asm volatile("dsb ishst");
    asm volatile("tlbi vmalle1");
    asm volatile("dsb ish");
    asm volatile("isb");
}

template<typename T>
void ProcessorBase<T>::flush_tlb(Memory::PageDirectory const*, VirtualAddress vaddr, size_t page_count)
{
    flush_tlb_local(vaddr, page_count);
}

template<typename T>
void ProcessorBase<T>::flush_instruction_cache(VirtualAddress vaddr, size_t byte_count)
{
    __builtin___clear_cache(reinterpret_cast<char*>(vaddr.as_ptr()), reinterpret_cast<char*>(vaddr.offset(byte_count).as_ptr()));
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
u32 ProcessorBase<T>::smp_wake_n_idle_processors(u32 wake_count)
{
    (void)wake_count;
    // FIXME: Actually wake up other cores when SMP is supported for aarch64.
    return 0;
}

template<typename T>
void ProcessorBase<T>::initialize_context_switching(Thread& initial_thread)
{
    VERIFY(initial_thread.process().is_kernel_process());

    m_scheduler_initialized.set();

    // FIXME: Figure out if we need to call {pre_,post_,}init_finished once aarch64 supports SMP
    Processor::set_current_in_scheduler(true);

    auto& regs = initial_thread.regs();
    // clang-format off
    asm volatile(
        "mov sp, %[new_sp] \n"

        "sub sp, sp, 32 \n"
        "str %[from_to_thread], [sp, #0] \n"
        "str %[from_to_thread], [sp, #8] \n"
        "br %[new_ip] \n"
        :: [new_sp] "r" (regs.sp_el0),
        [new_ip] "r" (regs.elr_el1),
        [from_to_thread] "r" (&initial_thread)
    );
    // clang-format on

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
        "sub sp, sp, #256 \n"
        "stp x0, x1,     [sp, #(0 * 0)] \n"
        "stp x2, x3,     [sp, #(2 * 8)] \n"
        "stp x4, x5,     [sp, #(4 * 8)] \n"
        "stp x6, x7,     [sp, #(6 * 8)] \n"
        "stp x8, x9,     [sp, #(8 * 8)] \n"
        "stp x10, x11,   [sp, #(10 * 8)] \n"
        "stp x12, x13,   [sp, #(12 * 8)] \n"
        "stp x14, x15,   [sp, #(14 * 8)] \n"
        "stp x16, x17,   [sp, #(16 * 8)] \n"
        "stp x18, x19,   [sp, #(18 * 8)] \n"
        "stp x20, x21,   [sp, #(20 * 8)] \n"
        "stp x22, x23,   [sp, #(22 * 8)] \n"
        "stp x24, x25,   [sp, #(24 * 8)] \n"
        "stp x26, x27,   [sp, #(26 * 8)] \n"
        "stp x28, x29,   [sp, #(28 * 8)] \n"
        "str x30,        [sp, #(30 * 8)] \n"
        "mov x0, sp \n"
        "str x0, %[from_sp] \n"
        "str fp, %[from_fp] \n"
        "adrp x0, 1f \n"
        "add x0, x0, :lo12:1f \n"
        "str x0, %[from_ip] \n"

        "ldr x0, %[to_sp] \n"
        "mov sp, x0 \n"

        "sub sp, sp, 32 \n"
        "ldr x0, %[from_thread] \n"
        "ldr x1, %[to_thread] \n"
        "ldr x2, %[to_ip] \n"
        "str x0, [sp, #0] \n"
        "str x1, [sp, #8] \n"
        "str x2, [sp, #16] \n"

        "bl enter_thread_context \n"
        "ldr x0, [sp, #16]\n"
        "br x0 \n"

        "1: \n"
        "add sp, sp, 32 \n"

        "ldp x0, x1,     [sp, #(0 * 0)] \n"
        "ldp x2, x3,     [sp, #(2 * 8)] \n"
        "ldp x4, x5,     [sp, #(4 * 8)] \n"
        "ldp x6, x7,     [sp, #(6 * 8)] \n"
        "ldp x8, x9,     [sp, #(8 * 8)] \n"
        "ldp x10, x11,   [sp, #(10 * 8)] \n"
        "ldp x12, x13,   [sp, #(12 * 8)] \n"
        "ldp x14, x15,   [sp, #(14 * 8)] \n"
        "ldp x16, x17,   [sp, #(16 * 8)] \n"
        "ldp x18, x19,   [sp, #(18 * 8)] \n"
        "ldp x20, x21,   [sp, #(20 * 8)] \n"
        "ldp x22, x23,   [sp, #(22 * 8)] \n"
        "ldp x24, x25,   [sp, #(24 * 8)] \n"
        "ldp x26, x27,   [sp, #(26 * 8)] \n"
        "ldp x28, x29,   [sp, #(28 * 8)] \n"
        "ldr x30,        [sp, #(30 * 8)] \n"

        "sub sp, sp, 32 \n"
        "ldr x0, [sp, #0] \n"
        "ldr x1, [sp, #8] \n"
        "str x0, %[from_thread] \n"
        "str x1, %[to_thread] \n"

        "add sp, sp, #288 \n"
        :
        [from_ip] "=m"(from_thread->regs().elr_el1),
        [from_sp] "=m"(from_thread->regs().sp_el0),
        [from_fp] "=m"(from_thread->regs().x[29]),
        "=m"(from_thread),
        "=m"(to_thread)

        : [to_ip] "m"(to_thread->regs().elr_el1),
        [to_sp] "m"(to_thread->regs().sp_el0),
        [from_thread] "m"(from_thread),
        [to_thread] "m"(to_thread)
        : "memory", "x0", "x1", "x2");

    dbgln_if(CONTEXT_SWITCH_DEBUG, "switch_context <-- from {} {} to {} {}", VirtualAddress(from_thread), *from_thread, VirtualAddress(to_thread), *to_thread);
}

extern "C" FlatPtr do_init_context(Thread* thread, u32 new_interrupts_state)
{
    VERIFY_INTERRUPTS_DISABLED();

    Aarch64::SPSR_EL1 spsr_el1 = {};
    memcpy(&spsr_el1, (u8 const*)&thread->regs().spsr_el1, sizeof(u64));
    spsr_el1.I = (new_interrupts_state == to_underlying(InterruptsState::Disabled));
    memcpy((void*)&thread->regs().spsr_el1, &spsr_el1, sizeof(u64));

    return Processor::current().init_context(*thread, true);
}

// FIXME: Share this code with other architectures.
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
    RegisterState& eretframe = *reinterpret_cast<RegisterState*>(stack_top);
    memcpy(eretframe.x, thread_regs.x, sizeof(thread_regs.x));

    // We don't overwrite the link register if it's not 0, since that means this thread's register state was already initialized with
    // an existing link register value (e.g. it was fork()'ed), so we assume exit_kernel_thread is already saved as previous LR on the
    // stack somewhere.
    if (eretframe.x[30] == 0x0) {
        // x30 is the Link Register for the aarch64 ABI, so this will return to exit_kernel_thread when main thread function returns.
        eretframe.x[30] = FlatPtr(&exit_kernel_thread);
    }
    eretframe.elr_el1 = thread_regs.elr_el1;
    eretframe.sp_el0 = thread_regs.sp_el0;
    eretframe.tpidr_el0 = thread_regs.tpidr_el0;
    eretframe.spsr_el1 = thread_regs.spsr_el1;

    // Push a TrapFrame onto the stack
    stack_top -= sizeof(TrapFrame);
    TrapFrame& trap = *reinterpret_cast<TrapFrame*>(stack_top);
    trap.regs = &eretframe;
    trap.next_trap = nullptr;

    if constexpr (CONTEXT_SWITCH_DEBUG) {
        dbgln("init_context {} ({}) set up to execute at ip={}, sp={}, stack_top={}",
            thread,
            VirtualAddress(&thread),
            VirtualAddress(thread_regs.elr_el1),
            VirtualAddress(thread_regs.sp_el0),
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

NAKED void thread_context_first_enter(void)
{
    asm(
        "ldr x0, [sp, #0] \n"
        "ldr x1, [sp, #8] \n"
        "add sp, sp, 32 \n"
        "bl context_first_init \n"
        "b restore_context_and_eret \n");
}

NAKED void do_assume_context(Thread*, u32)
{
    // clang-format off
    asm(
        "mov x19, x0 \n" // save thread ptr
        // We're going to call Processor::init_context, so just make sure
        // we have enough stack space so we don't stomp over it
        "sub sp, sp, #" __STRINGIFY(8 + REGISTER_STATE_SIZE + TRAP_FRAME_SIZE + 8) " \n"
        "bl do_init_context \n"
        "mov sp, x0 \n"  // move stack pointer to what Processor::init_context set up for us
        "mov x0, x19 \n" // to_thread
        "mov x1, x19 \n" // from_thread
        "sub sp, sp, 32 \n"
        "stp x19, x19, [sp] \n"                           // to_thread, from_thread (for thread_context_first_enter)
        "adrp lr, thread_context_first_enter \n"          // should be same as regs.elr_el1
        "add lr, lr, :lo12:thread_context_first_enter \n" // should be same as regs.elr_el1
        "b enter_thread_context \n");
    // clang-format on
}

extern "C" void context_first_init(Thread* from_thread, Thread* to_thread)
{
    do_context_first_init(from_thread, to_thread);
}

extern "C" void enter_thread_context(Thread* from_thread, Thread* to_thread)
{
    VERIFY(from_thread == to_thread || from_thread->state() != Thread::State::Running);
    VERIFY(to_thread->state() == Thread::State::Running);

    Processor::set_current_thread(*to_thread);

    store_fpu_state(&from_thread->fpu_state());

    auto& from_regs = from_thread->regs();
    auto& to_regs = to_thread->regs();
    if (from_regs.ttbr0_el1 != to_regs.ttbr0_el1) {
        Aarch64::Asm::set_ttbr0_el1(to_regs.ttbr0_el1);
        Processor::flush_entire_tlb_local();
    }

    to_thread->set_cpu(Processor::current().id());

    auto in_critical = to_thread->saved_critical();
    VERIFY(in_critical > 0);
    Processor::restore_critical(in_critical);

    load_fpu_state(&to_thread->fpu_state());
}

template<typename T>
StringView ProcessorBase<T>::platform_string()
{
    return "aarch64"sv;
}

template<typename T>
void ProcessorBase<T>::wait_for_interrupt() const
{
    asm("wfi");
}

template<typename T>
Processor& ProcessorBase<T>::by_id(u32 id)
{
    (void)id;
    TODO_AARCH64();
}

}

#include <Kernel/Arch/ProcessorFunctions.include>
