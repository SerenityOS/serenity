/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include <Kernel/Process.h>
#include <Kernel/Ptrace.h>
#include <Kernel/Thread.h>
#include <Kernel/ThreadTracer.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/ProcessPagingScope.h>

namespace Ptrace {

KResultOr<u32> handle_syscall(const Kernel::Syscall::SC_ptrace_params& params, Process& caller)
{
    if (params.request == PT_TRACE_ME) {
        if (Thread::current()->tracer())
            return KResult(-EBUSY);

        caller.set_wait_for_tracer_at_next_execve(true);
        return KSuccess;
    }

    // FIXME: PID/TID BUG
    // This bug allows to request PT_ATTACH (or anything else) the same process, as
    // long it is not the main thread. Alternatively, if this is desired, then the
    // bug is that this prevents PT_ATTACH to the main thread from another thread.
    if (params.tid == caller.pid().value())
        return KResult(-EINVAL);

    Thread* peer = nullptr;
    {
        InterruptDisabler disabler;
        peer = Thread::from_tid(params.tid);
    }
    if (!peer)
        return KResult(-ESRCH);

    if ((peer->process().uid() != caller.euid())
        || (peer->process().uid() != peer->process().euid())) // Disallow tracing setuid processes
        return KResult(-EACCES);

    if (params.request == PT_ATTACH) {
        if (peer->tracer()) {
            return KResult(-EBUSY);
        }
        peer->start_tracing_from(caller.pid());
        if (peer->state() != Thread::State::Stopped) {
            ScopedSpinLock lock(peer->get_lock());
            if (!(peer->has_blocker() && peer->blocker().is_reason_signal()))
                peer->send_signal(SIGSTOP, &caller);
        }
        return KSuccess;
    }

    auto* tracer = peer->tracer();

    if (!tracer)
        return KResult(-EPERM);

    if (tracer->tracer_pid() != caller.pid())
        return KResult(-EBUSY);

    if (peer->state() == Thread::State::Running)
        return KResult(-EBUSY);

    switch (params.request) {
    case PT_CONTINUE:
        peer->send_signal(SIGCONT, &caller);
        break;

    case PT_DETACH:
        peer->stop_tracing();
        peer->send_signal(SIGCONT, &caller);
        break;

    case PT_SYSCALL:
        tracer->set_trace_syscalls(true);
        peer->send_signal(SIGCONT, &caller);
        break;

    case PT_GETREGS: {
        if (!tracer->has_regs())
            return KResult(-EINVAL);

        auto* regs = reinterpret_cast<PtraceRegisters*>(params.addr);
        if (!copy_to_user(regs, &tracer->regs()))
            return KResult(-EFAULT);
        break;
    }

    case PT_SETREGS: {
        if (!tracer->has_regs())
            return KResult(-EINVAL);

        PtraceRegisters regs;
        if (!copy_from_user(&regs, (const PtraceRegisters*)params.addr))
            return KResult(-EFAULT);

        auto& peer_saved_registers = peer->get_register_dump_from_stack();
        // Verify that the saved registers are in usermode context
        if ((peer_saved_registers.cs & 0x03) != 3)
            return KResult(-EFAULT);

        tracer->set_regs(regs);
        copy_ptrace_registers_into_kernel_registers(peer_saved_registers, regs);
        break;
    }

    case PT_PEEK: {
        Kernel::Syscall::SC_ptrace_peek_params peek_params;
        if (!copy_from_user(&peek_params, reinterpret_cast<Kernel::Syscall::SC_ptrace_peek_params*>(params.addr)))
            return -EFAULT;

        // read validation is done inside 'peek_user_data'
        auto result = peer->process().peek_user_data((FlatPtr)peek_params.address);
        if (result.is_error())
            return -EFAULT;
        if (!copy_to_user(peek_params.out_data, &result.value()))
            return -EFAULT;
        break;
    }

    case PT_POKE: {
        Userspace<u32*> addr = reinterpret_cast<FlatPtr>(params.addr);
        // write validation is done inside 'poke_user_data'
        return peer->process().poke_user_data(addr, params.data);
    }

    default:
        return -EINVAL;
    }

    return 0;
}

void copy_kernel_registers_into_ptrace_registers(PtraceRegisters& ptrace_regs, const RegisterState& kernel_regs)
{
    ptrace_regs.eax = kernel_regs.eax,
    ptrace_regs.ecx = kernel_regs.ecx,
    ptrace_regs.edx = kernel_regs.edx,
    ptrace_regs.ebx = kernel_regs.ebx,
    ptrace_regs.esp = kernel_regs.userspace_esp,
    ptrace_regs.ebp = kernel_regs.ebp,
    ptrace_regs.esi = kernel_regs.esi,
    ptrace_regs.edi = kernel_regs.edi,
    ptrace_regs.eip = kernel_regs.eip,
    ptrace_regs.eflags = kernel_regs.eflags,
    ptrace_regs.cs = 0;
    ptrace_regs.ss = 0;
    ptrace_regs.ds = 0;
    ptrace_regs.es = 0;
    ptrace_regs.fs = 0;
    ptrace_regs.gs = 0;
}

void copy_ptrace_registers_into_kernel_registers(RegisterState& kernel_regs, const PtraceRegisters& ptrace_regs)
{
    kernel_regs.eax = ptrace_regs.eax;
    kernel_regs.ecx = ptrace_regs.ecx;
    kernel_regs.edx = ptrace_regs.edx;
    kernel_regs.ebx = ptrace_regs.ebx;
    kernel_regs.esp = ptrace_regs.esp;
    kernel_regs.ebp = ptrace_regs.ebp;
    kernel_regs.esi = ptrace_regs.esi;
    kernel_regs.edi = ptrace_regs.edi;
    kernel_regs.eip = ptrace_regs.eip;
    kernel_regs.eflags = ptrace_regs.eflags;
}

}
