/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Syscall.h>
#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/ThreadTracer.h>

namespace Kernel {

extern bool g_in_system_shutdown;

namespace Syscall {

using Handler = auto (Process::*)(FlatPtr, FlatPtr, FlatPtr, FlatPtr) -> ErrorOr<FlatPtr>;
using HandlerWithRegisterState = auto (Process::*)(RegisterState&) -> ErrorOr<FlatPtr>;

struct HandlerMetadata {
    Handler handler;
    NeedsBigProcessLock needs_lock;
};

#define __ENUMERATE_SYSCALL(sys_call, needs_lock) { bit_cast<Handler>(&Process::sys$##sys_call), needs_lock },
static HandlerMetadata const s_syscall_table[] = {
    ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
};
#undef __ENUMERATE_SYSCALL

ErrorOr<FlatPtr> handle(RegisterState& regs, FlatPtr function, FlatPtr arg1, FlatPtr arg2, FlatPtr arg3, FlatPtr arg4)
{
    VERIFY_INTERRUPTS_ENABLED();
    auto* current_thread = Thread::current();
    auto& process = current_thread->process();
    current_thread->did_syscall();

    PerformanceManager::add_syscall_event(*current_thread, regs);

    if (g_in_system_shutdown)
        return ENOSYS;

    if (function >= Function::__Count) {
        dbgln("Unknown syscall {} requested ({:p}, {:p}, {:p}, {:p})", function, arg1, arg2, arg3, arg4);
        return ENOSYS;
    }

    auto const syscall_metadata = s_syscall_table[function];
    if (syscall_metadata.handler == nullptr) {
        dbgln("Null syscall {} requested, you probably need to rebuild this program!", function);
        return ENOSYS;
    }

    MutexLocker mutex_locker;
    auto const needs_big_lock = syscall_metadata.needs_lock == NeedsBigProcessLock::Yes;
    if (needs_big_lock) {
        mutex_locker.attach_and_lock(process.big_lock());
    };

    if (function == SC_exit || function == SC_exit_thread) {
        // These syscalls need special handling since they never return to the caller.
        // In these cases the process big lock will get released on the exit of the thread.

        if (auto* tracer = process.tracer(); tracer && tracer->is_tracing_syscalls()) {
            regs.set_return_reg(0);
            tracer->set_trace_syscalls(false);
            process.tracer_trap(*current_thread, regs); // this triggers SIGTRAP and stops the thread!
        }

        switch (function) {
        case SC_exit:
            process.sys$exit(arg1);
        case SC_exit_thread:
            process.sys$exit_thread(arg1, arg2, arg3);
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ErrorOr<FlatPtr> result { FlatPtr(nullptr) };
    if (function == SC_fork || function == SC_sigreturn) {
        // These syscalls want the RegisterState& rather than individual parameters.
        auto handler = bit_cast<HandlerWithRegisterState>(syscall_metadata.handler);
        result = (process.*(handler))(regs);
    } else {
        result = (process.*(syscall_metadata.handler))(arg1, arg2, arg3, arg4);
    }

    return result;
}

}

extern "C" NEVER_INLINE void syscall_handler(TrapFrame* trap);
NEVER_INLINE void syscall_handler(TrapFrame* trap)
{
#if ARCH(X86_64)
    // Make sure SMAP protection is enabled on syscall entry.
    clac();
#elif ARCH(AARCH64)
    // FIXME: Implement the security mechanism for aarch64
#elif ARCH(RISCV64)
    RISCV64::CSR::clear_bits(RISCV64::CSR::Address::SSTATUS, 1 << to_underlying(RISCV64::CSR::SSTATUS::Offset::SUM));
#else
#    error Unknown architecture
#endif

    auto& regs = *trap->regs;
    auto* current_thread = Thread::current();
    VERIFY(current_thread->previous_mode() == ExecutionMode::User);
    auto& process = current_thread->process();
    if (process.is_dying()) {
        // It's possible this thread is just about to make a syscall while another is
        // is killing our process.
        current_thread->die_if_needed();
        return;
    }

    if (auto* tracer = process.tracer(); tracer && tracer->is_tracing_syscalls()) {
        tracer->set_trace_syscalls(false);
        process.tracer_trap(*current_thread, regs); // this triggers SIGTRAP and stops the thread!
    }

    current_thread->yield_if_should_be_stopped();

    // Apply a random offset in the range 0-255 to the stack pointer,
    // to make kernel stacks a bit less deterministic.
    auto* ptr = static_cast<char*>(__builtin_alloca(get_fast_random<u8>()));
    AK::taint_for_optimizer(ptr);

#if ARCH(X86_64)
    constexpr FlatPtr iopl_mask = 3u << 12;

    FlatPtr flags = regs.flags();
    if ((flags & (iopl_mask)) != 0) {
        PANIC("Syscall from process with IOPL != 0");
    }
#endif

    Memory::MemoryManager::validate_syscall_preconditions(process, regs);

    FlatPtr function;
    FlatPtr arg1;
    FlatPtr arg2;
    FlatPtr arg3;
    FlatPtr arg4;
    regs.capture_syscall_params(function, arg1, arg2, arg3, arg4);

    auto result = Syscall::handle(regs, function, arg1, arg2, arg3, arg4);

    if (result.is_error()) {
        regs.set_return_reg(-result.error().code());
    } else {
        regs.set_return_reg(result.value());
    }

    if (auto* tracer = process.tracer(); tracer && tracer->is_tracing_syscalls()) {
        tracer->set_trace_syscalls(false);
        process.tracer_trap(*current_thread, regs); // this triggers SIGTRAP and stops the thread!
    }

    current_thread->yield_if_should_be_stopped();

    current_thread->check_dispatch_pending_signal();

    // If the previous mode somehow changed something is seriously messed up...
    VERIFY(current_thread->previous_mode() == ExecutionMode::User);

    // Check if we're supposed to return to userspace or just die.
    current_thread->die_if_needed();

    // Crash any processes which have committed a promise violation during syscall handling.
    if (result.is_error() && result.error().code() == EPROMISEVIOLATION) {
        VERIFY(current_thread->is_promise_violation_pending());
        current_thread->set_promise_violation_pending(false);
        process.crash(SIGABRT, {});
    } else {
        VERIFY(!current_thread->is_promise_violation_pending());
    }

    VERIFY(!g_scheduler_lock.is_locked_by_current_processor());
}

}
