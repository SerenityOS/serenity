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

#include <Kernel/Process.h>

//#define PROCESS_DEBUG

namespace Kernel {

KResultOr<siginfo_t> Process::do_waitid(idtype_t idtype, int id, int options)
{
    if (idtype == P_PID) {
        ScopedSpinLock lock(g_processes_lock);
        if (idtype == P_PID && !Process::from_pid(id))
            return KResult(-ECHILD);
        // FIXME: Race: After 'lock' releases, the 'id' process might vanish.
        // If that is not a problem, why check for it?
        // If it is a problem, let's fix it! (Eventually.)
    }

    ProcessID waitee_pid { 0 };

    // FIXME: WaitBlocker should support idtype/id specs directly.
    if (idtype == P_ALL) {
        waitee_pid = -1;
    } else if (idtype == P_PID) {
        waitee_pid = id;
    } else {
        // FIXME: Implement other PID specs.
        return KResult(-EINVAL);
    }

    if (Thread::current()->block<Thread::WaitBlocker>(nullptr, options, waitee_pid).was_interrupted())
        return KResult(-EINTR);

    ScopedSpinLock lock(g_processes_lock);

    // NOTE: If waitee was -1, m_waitee_pid will have been filled in by the scheduler.
    auto waitee_process = Process::from_pid(waitee_pid);
    if (!waitee_process)
        return KResult(-ECHILD);

    ASSERT(waitee_process);
    if (waitee_process->is_dead()) {
        return reap(*waitee_process);
    } else {
        // FIXME: PID/TID BUG
        // Make sure to hold the scheduler lock so that we operate on a consistent state
        ScopedSpinLock scheduler_lock(g_scheduler_lock);
        auto waitee_thread = Thread::from_tid(waitee_pid.value());
        if (!waitee_thread)
            return KResult(-ECHILD);
        ASSERT((options & WNOHANG) || waitee_thread->state() == Thread::State::Stopped);
        siginfo_t siginfo;
        memset(&siginfo, 0, sizeof(siginfo));
        siginfo.si_signo = SIGCHLD;
        siginfo.si_pid = waitee_process->pid().value();
        siginfo.si_uid = waitee_process->uid();

        switch (waitee_thread->state()) {
        case Thread::State::Stopped:
            siginfo.si_code = CLD_STOPPED;
            break;
        case Thread::State::Running:
        case Thread::State::Runnable:
        case Thread::State::Blocked:
        case Thread::State::Dying:
        case Thread::State::Dead:
        case Thread::State::Queued:
            siginfo.si_code = CLD_CONTINUED;
            break;
        default:
            ASSERT_NOT_REACHED();
            break;
        }

        siginfo.si_status = waitee_thread->m_stop_signal;
        return siginfo;
    }
}

pid_t Process::sys$waitid(Userspace<const Syscall::SC_waitid_params*> user_params)
{
    REQUIRE_PROMISE(proc);

    Syscall::SC_waitid_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

#ifdef PROCESS_DEBUG
    dbg() << "sys$waitid(" << params.idtype << ", " << params.id << ", " << params.infop << ", " << params.options << ")";
#endif

    auto siginfo_or_error = do_waitid(static_cast<idtype_t>(params.idtype), params.id, params.options);
    if (siginfo_or_error.is_error())
        return siginfo_or_error.error();

    if (!copy_to_user(params.infop, &siginfo_or_error.value()))
        return -EFAULT;
    return 0;
}

}
