/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StackUnwinder.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Memory/ScopedAddressSpaceSwitcher.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

READONLY_AFTER_INIT FPUState s_clean_fpu_state;
READONLY_AFTER_INIT Atomic<u32> g_total_processors;

template<typename T>
void ProcessorBase<T>::check_invoke_scheduler()
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(!m_in_irq);
    VERIFY(!m_in_critical);
    VERIFY(&Processor::current() == this);
    if (m_invoke_scheduler_async && m_scheduler_initialized.was_set()) {
        m_invoke_scheduler_async = false;
        Scheduler::invoke_async();
    }
}
template void ProcessorBase<Processor>::check_invoke_scheduler();

template<typename T>
void ProcessorBase<T>::deferred_call_queue(Function<void()> callback)
{
    // NOTE: If we are called outside of a critical section and outside
    // of an irq handler, the function will be executed before we return!
    ScopedCritical critical;
    auto& cur_proc = Processor::current();

    auto* entry = cur_proc.m_deferred_call_pool.get_free();
    entry->handler_value() = move(callback);

    cur_proc.m_deferred_call_pool.queue_entry(entry);
}
template void ProcessorBase<Processor>::deferred_call_queue(Function<void()>);

template<typename T>
void ProcessorBase<T>::enter_trap(TrapFrame& trap, bool raise_irq)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(&Processor::current() == this);
#if ARCH(X86_64)
    // FIXME: Figure out if we need prev_irq_level
    trap.prev_irq_level = m_in_irq;
#endif
    if (raise_irq)
        m_in_irq++;
    auto* current_thread = Processor::current_thread();
    if (current_thread) {
        auto& current_trap = current_thread->current_trap();
        trap.next_trap = current_trap;
        current_trap = &trap;
        auto new_previous_mode = trap.regs->previous_mode();
        if (current_thread->set_previous_mode(new_previous_mode)) {
            current_thread->update_time_scheduled(TimeManagement::scheduler_current_time(), new_previous_mode == ExecutionMode::Kernel, false);
        }
    } else {
        trap.next_trap = nullptr;
    }
}
template void ProcessorBase<Processor>::enter_trap(TrapFrame&, bool);

template<typename T>
u64 ProcessorBase<T>::time_spent_idle() const
{
    return m_idle_thread->time_in_user() + m_idle_thread->time_in_kernel();
}
template u64 ProcessorBase<Processor>::time_spent_idle() const;

template<typename T>
void ProcessorBase<T>::leave_critical()
{
    InterruptDisabler disabler;
    current().do_leave_critical();
}
template void ProcessorBase<Processor>::leave_critical();

template<typename T>
void ProcessorBase<T>::do_leave_critical()
{
    VERIFY(m_in_critical > 0);
    if (m_in_critical == 1) {
        if (m_in_irq == 0) {
            m_deferred_call_pool.execute_pending();
            VERIFY(m_in_critical == 1);
        }
        m_in_critical = 0;
        if (m_in_irq == 0)
            check_invoke_scheduler();
    } else {
        m_in_critical = m_in_critical - 1;
    }
}
template void ProcessorBase<Processor>::do_leave_critical();

void exit_kernel_thread(void)
{
    Thread::current()->exit();
}

void do_context_first_init(Thread* from_thread, Thread* to_thread)
{
    VERIFY(!Processor::are_interrupts_enabled());

    dbgln_if(CONTEXT_SWITCH_DEBUG, "switch_context <-- from {} {} to {} {} (context_first_init)", VirtualAddress(from_thread), *from_thread, VirtualAddress(to_thread), *to_thread);

    VERIFY(to_thread == Thread::current());

    Scheduler::enter_current(*from_thread);

    auto in_critical = to_thread->saved_critical();
    VERIFY(in_critical > 0);
    Processor::restore_critical(in_critical);

    // Since we got here and don't have Scheduler::context_switch in the
    // call stack (because this is the first time we switched into this
    // context), we need to notify the scheduler so that it can release
    // the scheduler lock. We don't want to enable interrupts at this point
    // as we're still in the middle of a context switch. Doing so could
    // trigger a context switch within a context switch, leading to a crash.
    Scheduler::leave_on_first_switch(InterruptsState::Disabled);
}

template<typename T>
ErrorOr<Vector<FlatPtr, 32>> ProcessorBase<T>::capture_stack_trace(Thread& thread, size_t max_frames)
{
    FlatPtr frame_ptr = 0, pc = 0;
    Vector<FlatPtr, 32> stack_trace;

    auto walk_stack = [&](FlatPtr frame_ptr) -> ErrorOr<void> {
        constexpr size_t max_stack_frames = 4096;
        bool is_walking_userspace_stack = false;
        TRY(stack_trace.try_append(pc));

        TRY(AK::unwind_stack_from_frame_pointer(
            frame_ptr,
            [&is_walking_userspace_stack](FlatPtr address) -> ErrorOr<FlatPtr> {
                if (!Memory::is_user_address(VirtualAddress { address })) {
                    if (is_walking_userspace_stack) {
                        dbgln("SHENANIGANS! Userspace stack points back into kernel memory");
                        return EFAULT;
                    }
                } else {
                    is_walking_userspace_stack = true;
                }

                FlatPtr value;

                if (Memory::is_user_range(VirtualAddress { address }, sizeof(FlatPtr))) {
                    TRY(copy_from_user(&value, bit_cast<FlatPtr*>(address)));
                } else {
                    void* fault_at;
                    if (!safe_memcpy(&value, bit_cast<FlatPtr*>(address), sizeof(FlatPtr), fault_at))
                        return EFAULT;
                }

                return value;
            },
            [&stack_trace, max_frames](AK::StackFrame stack_frame) -> ErrorOr<IterationDecision> {
                if (stack_trace.size() >= max_stack_frames || (max_frames != 0 && stack_trace.size() >= max_frames))
                    return IterationDecision::Break;

                TRY(stack_trace.try_append(stack_frame.return_address));

                return IterationDecision::Continue;
            }));

        return {};
    };

    auto capture_current_thread = [&]() {
        frame_ptr = bit_cast<FlatPtr>(__builtin_frame_address(0));
        pc = bit_cast<FlatPtr>(__builtin_return_address(0));

        return walk_stack(frame_ptr);
    };

    // Since the thread may be running on another processor, there
    // is a chance a context switch may happen while we're trying
    // to get it. It also won't be entirely accurate and merely
    // reflect the status at the last context switch.
    SpinlockLocker lock(g_scheduler_lock);
    if (&thread == Processor::current_thread()) {
        VERIFY(thread.state() == Thread::State::Running);
        // Leave the scheduler lock. If we trigger page faults we may
        // need to be preempted. Since this is our own thread it won't
        // cause any problems as the stack won't change below this frame.
        lock.unlock();
        TRY(capture_current_thread());
    } else if (thread.is_active()) {
#if ARCH(X86_64)
        VERIFY(thread.cpu() != Processor::current_id());
        // If this is the case, the thread is currently running
        // on another processor. We can't trust the kernel stack as
        // it may be changing at any time. We need to probably send
        // an IPI to that processor, have it walk the stack and wait
        // until it returns the data back to us
        auto& proc = Processor::current();
        ErrorOr<void> result;
        Processor::smp_unicast(
            thread.cpu(),
            [&]() {
                dbgln("CPU[{}] getting stack for cpu #{}", Processor::current_id(), proc.id());
                ScopedAddressSpaceSwitcher switcher(thread.process());
                VERIFY(&Processor::current() != &proc);
                VERIFY(&thread == Processor::current_thread());
                // NOTE: Because the other processor is still holding the
                // scheduler lock while waiting for this callback to finish,
                // the current thread on the target processor cannot change

                // TODO: What to do about page faults here? We might deadlock
                //       because the other processor is still holding the
                //       scheduler lock...
                result = capture_current_thread();
            },
            false);
        TRY(result);
#elif ARCH(AARCH64) || ARCH(RISCV64)
        VERIFY_NOT_REACHED(); // We don't support SMP on AArch64 and RISC-V yet, so this should be unreachable.
#else
#    error Unknown architecture
#endif
    } else {
        switch (thread.state()) {
        case Thread::State::Running:
            VERIFY_NOT_REACHED(); // should have been handled above
        case Thread::State::Runnable:
        case Thread::State::Stopped:
        case Thread::State::Blocked:
        case Thread::State::Dying:
        case Thread::State::Dead: {
            ScopedAddressSpaceSwitcher switcher(thread.process());
            auto& regs = thread.regs();

            pc = regs.ip();
            frame_ptr = regs.frame_pointer();

            // TODO: We need to leave the scheduler lock here, but we also
            //       need to prevent the target thread from being run while
            //       we walk the stack
            lock.unlock();
            TRY(walk_stack(frame_ptr));
            break;
        }
        default:
            dbgln("Cannot capture stack trace for thread {} in state {}", thread, thread.state_string());
            break;
        }
    }

    return stack_trace;
}
template ErrorOr<Vector<FlatPtr, 32>> ProcessorBase<Processor>::capture_stack_trace(Thread&, size_t);

}
