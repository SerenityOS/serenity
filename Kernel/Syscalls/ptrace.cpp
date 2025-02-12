/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <Kernel/Memory/PrivateInodeVMObject.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/ScopedAddressSpaceSwitcher.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/ThreadTracer.h>

namespace Kernel {

static ErrorOr<FlatPtr> handle_ptrace(Kernel::Syscall::SC_ptrace_params const& params, Process& caller)
{
    if (params.request == PT_TRACE_ME) {
        if (Process::current().tracer())
            return EBUSY;

        caller.set_wait_for_tracer_at_next_execve(true);
        return 0;
    }

    // FIXME: PID/TID BUG
    // This bug allows to request PT_ATTACH (or anything else) the same process, as
    // long it is not the main thread. Alternatively, if this is desired, then the
    // bug is that this prevents PT_ATTACH to the main thread from another thread.
    if (params.tid == caller.pid().value())
        return EINVAL;

    auto peer = Thread::from_tid_in_same_process_list(params.tid);
    if (!peer)
        return ESRCH;

    MutexLocker ptrace_locker(peer->process().ptrace_lock());
    SpinlockLocker scheduler_lock(g_scheduler_lock);

    auto peer_credentials = peer->process().credentials();
    auto caller_credentials = caller.credentials();
    if (!caller_credentials->is_superuser() && ((peer_credentials->uid() != caller_credentials->euid()) || (peer_credentials->uid() != peer_credentials->euid()))) // Disallow tracing setuid processes
        return EACCES;

    if (!peer->process().is_dumpable())
        return EACCES;

    auto& peer_process = peer->process();
    if (params.request == PT_ATTACH) {
        if (peer_process.tracer()) {
            return EBUSY;
        }
        TRY(peer_process.start_tracing_from(caller.pid()));
        SpinlockLocker lock(peer->get_lock());
        if (peer->state() == Thread::State::Stopped) {
            peer_process.tracer()->set_regs(peer->get_register_dump_from_stack());
        } else {
            peer->send_signal(SIGSTOP, &caller);
        }
        return 0;
    }

    auto* tracer = peer_process.tracer();

    if (!tracer)
        return EPERM;

    if (tracer->tracer_pid() != caller.pid())
        return EBUSY;

    if (peer->state() == Thread::State::Running)
        return EBUSY;

    scheduler_lock.unlock();

    switch (params.request) {
    case PT_CONTINUE:
        peer->send_signal(SIGCONT, &caller);
        break;

    case PT_DETACH:
        peer_process.stop_tracing();
        peer->send_signal(SIGCONT, &caller);
        break;

    case PT_SYSCALL:
        tracer->set_trace_syscalls(true);
        peer->send_signal(SIGCONT, &caller);
        break;

    case PT_GETREGS: {
        if (!tracer->has_regs())
            return EINVAL;
        auto* regs = reinterpret_cast<PtraceRegisters*>(params.addr);
        TRY(copy_to_user(regs, &tracer->regs()));
        break;
    }

    case PT_SETREGS: {
        if (!tracer->has_regs())
            return EINVAL;

        PtraceRegisters regs {};
        TRY(copy_from_user(&regs, (PtraceRegisters const*)params.addr));

        auto& peer_saved_registers = peer->get_register_dump_from_stack();
        // Verify that the saved registers are in usermode context
        if (peer_saved_registers.previous_mode() != ExecutionMode::User)
            return EFAULT;

        tracer->set_regs(regs);
        copy_ptrace_registers_into_kernel_registers(peer_saved_registers, regs);
        break;
    }

    case PT_PEEK: {
        auto data = TRY(peer->process().peek_user_data(Userspace<FlatPtr const*> { (FlatPtr)params.addr }));
        TRY(copy_to_user((FlatPtr*)params.data, &data));
        break;
    }

    case PT_POKE:
        TRY(peer->process().poke_user_data(Userspace<FlatPtr*> { (FlatPtr)params.addr }, params.data));
        return 0;

    case PT_PEEKBUF: {
        Kernel::Syscall::SC_ptrace_buf_params buf_params {};
        TRY(copy_from_user(&buf_params, reinterpret_cast<Kernel::Syscall::SC_ptrace_buf_params*>(params.data)));
        // This is a comparatively large allocation on the Kernel stack.
        // However, we know that we're close to the root of the call stack, and the following calls shouldn't go too deep.
        Array<u8, PAGE_SIZE> buf;
        FlatPtr tracee_ptr = (FlatPtr)params.addr;
        while (buf_params.buf.size > 0) {
            size_t copy_this_iteration = min(buf.size(), buf_params.buf.size);
            TRY(peer->process().peek_user_data(buf.span().slice(0, copy_this_iteration), Userspace<u8 const*> { tracee_ptr }));
            TRY(copy_to_user((void*)buf_params.buf.data, buf.data(), copy_this_iteration));
            tracee_ptr += copy_this_iteration;
            buf_params.buf.data += copy_this_iteration;
            buf_params.buf.size -= copy_this_iteration;
        }
        break;
    }

    case PT_PEEKDEBUG: {
        auto data = TRY(peer->peek_debug_register(reinterpret_cast<uintptr_t>(params.addr)));
        TRY(copy_to_user((FlatPtr*)params.data, &data));
        break;
    }
    case PT_POKEDEBUG:
        TRY(peer->poke_debug_register(reinterpret_cast<uintptr_t>(params.addr), params.data));
        return 0;
    default:
        return EINVAL;
    }

    return 0;
}

ErrorOr<FlatPtr> Process::sys$ptrace(Userspace<Syscall::SC_ptrace_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::ptrace));
    auto params = TRY(copy_typed_from_user(user_params));

    return handle_ptrace(params, *this);
}

/**
 * "Does this process have a thread that is currently being traced by the provided process?"
 */
bool Process::has_tracee_thread(ProcessID tracer_pid)
{
    if (auto const* tracer = this->tracer())
        return tracer->tracer_pid() == tracer_pid;
    return false;
}

ErrorOr<FlatPtr> Process::peek_user_data(Userspace<FlatPtr const*> address)
{
    // This function can be called from the context of another
    // process that called PT_PEEK
    ScopedAddressSpaceSwitcher switcher(*this);
    return TRY(copy_typed_from_user(address));
}

ErrorOr<void> Process::peek_user_data(Span<u8> destination, Userspace<u8 const*> address)
{
    // This function can be called from the context of another
    // process that called PT_PEEKBUF
    ScopedAddressSpaceSwitcher switcher(*this);
    TRY(copy_from_user(destination.data(), address, destination.size()));
    return {};
}

ErrorOr<void> Process::poke_user_data(Userspace<FlatPtr*> address, FlatPtr data)
{
    Memory::VirtualRange range = { address.vaddr(), sizeof(FlatPtr) };

    return address_space().with([&](auto& space) -> ErrorOr<void> {
        auto* region = space->find_region_containing(range);
        if (!region)
            return EFAULT;
        ScopedAddressSpaceSwitcher switcher(*this);
        if (region->is_shared()) {
            // If the region is shared, we change its vmobject to a PrivateInodeVMObject
            // to prevent the write operation from changing any shared inode data
            VERIFY(region->vmobject().is_shared_inode());
            auto vmobject = TRY(Memory::PrivateInodeVMObject::try_create_with_inode(static_cast<Memory::SharedInodeVMObject&>(region->vmobject()).inode()));
            region->set_vmobject(move(vmobject));
            region->set_shared(false);
        }
        bool const was_writable = region->is_writable();
        if (!was_writable) {
            region->set_writable(true);
            region->remap();
        }
        ScopeGuard rollback([&]() {
            if (!was_writable) {
                region->set_writable(false);
                region->remap();
            }
        });

        return copy_to_user(address, &data);
    });
}

ErrorOr<FlatPtr> Thread::peek_debug_register(u32 register_index)
{
#if ARCH(X86_64)
    FlatPtr data;
    switch (register_index) {
    case 0:
        data = m_debug_register_state.dr0;
        break;
    case 1:
        data = m_debug_register_state.dr1;
        break;
    case 2:
        data = m_debug_register_state.dr2;
        break;
    case 3:
        data = m_debug_register_state.dr3;
        break;
    case 6:
        data = m_debug_register_state.dr6;
        break;
    case 7:
        data = m_debug_register_state.dr7;
        break;
    default:
        return EINVAL;
    }
    return data;
#elif ARCH(AARCH64)
    (void)register_index;
    TODO_AARCH64();
#elif ARCH(RISCV64)
    (void)register_index;
    dbgln("FIXME: Implement Thread::peek_debug_register on RISC-V");
    return ENOTSUP;
#else
#    error "Unknown architecture"
#endif
}

ErrorOr<void> Thread::poke_debug_register(u32 register_index, FlatPtr data)
{
#if ARCH(X86_64)
    switch (register_index) {
    case 0:
        m_debug_register_state.dr0 = data;
        break;
    case 1:
        m_debug_register_state.dr1 = data;
        break;
    case 2:
        m_debug_register_state.dr2 = data;
        break;
    case 3:
        m_debug_register_state.dr3 = data;
        break;
    case 7:
        m_debug_register_state.dr7 = data;
        break;
    default:
        return EINVAL;
    }
    return {};
#elif ARCH(AARCH64)
    (void)register_index;
    (void)data;
    TODO_AARCH64();
#elif ARCH(RISCV64)
    (void)register_index;
    (void)data;
    dbgln("FIXME: Implement Thread::poke_debug_register on RISC-V");
    return ENOTSUP;
#else
#    error "Unknown architecture"
#endif
}

}
