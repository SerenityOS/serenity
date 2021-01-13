/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/Debug.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/Process.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Thread.h>

//#define WAITBLOCK_DEBUG

namespace Kernel {

bool Thread::Blocker::set_block_condition(Thread::BlockCondition& block_condition, void* data)
{
    ASSERT(!m_block_condition);
    if (block_condition.add_blocker(*this, data)) {
        m_block_condition = &block_condition;
        m_block_data = data;
        return true;
    }
    return false;
}

Thread::Blocker::~Blocker()
{
    ScopedSpinLock lock(m_lock);
    if (m_block_condition)
        m_block_condition->remove_blocker(*this, m_block_data);
}

void Thread::Blocker::begin_blocking(Badge<Thread>)
{
    ScopedSpinLock lock(m_lock);
    ASSERT(!m_is_blocking);
    ASSERT(!m_blocked_thread);
    m_blocked_thread = Thread::current();
    m_is_blocking = true;
}

auto Thread::Blocker::end_blocking(Badge<Thread>, bool did_timeout) -> BlockResult
{
    ScopedSpinLock lock(m_lock);
    // if m_is_blocking is false here, some thread forced to
    // unblock us when we get here. This is only called from the
    // thread that was blocked.
    ASSERT(Thread::current() == m_blocked_thread);
    m_is_blocking = false;
    m_blocked_thread = nullptr;

    was_unblocked(did_timeout);
    return block_result();
}

Thread::JoinBlocker::JoinBlocker(Thread& joinee, KResult& try_join_result, void*& joinee_exit_value)
    : m_joinee(joinee)
    , m_joinee_exit_value(joinee_exit_value)
{
    {
        // We need to hold our lock to avoid a race where try_join succeeds
        // but the joinee is joining immediately
        ScopedSpinLock lock(m_lock);
        try_join_result = joinee.try_join([&]() {
            if (!set_block_condition(joinee.m_join_condition))
                m_should_block = false;
        });
        m_join_error = try_join_result.is_error();
        if (m_join_error)
            m_should_block = false;
    }
}

void Thread::JoinBlocker::not_blocking(bool timeout_in_past)
{
    if (!m_should_block) {
        // set_block_condition returned false, so unblock was already called
        ASSERT(!timeout_in_past);
        return;
    }
    // If we should have blocked but got here it must have been that the
    // timeout was already in the past. So we need to ask the BlockCondition
    // to supply us the information. We cannot hold the lock as unblock
    // could be called by the BlockCondition at any time!
    ASSERT(timeout_in_past);
    m_joinee->m_join_condition.try_unblock(*this);
}

bool Thread::JoinBlocker::unblock(void* value, bool from_add_blocker)
{
    {
        ScopedSpinLock lock(m_lock);
        if (m_did_unblock)
            return false;
        m_did_unblock = true;
        m_joinee_exit_value = value;
        do_set_interrupted_by_death();
    }

    if (!from_add_blocker)
        unblock_from_blocker();
    return true;
}

Thread::QueueBlocker::QueueBlocker(WaitQueue& wait_queue, const char* block_reason)
    : m_block_reason(block_reason)
{
    if (!set_block_condition(wait_queue, Thread::current()))
        m_should_block = false;
}

Thread::QueueBlocker::~QueueBlocker()
{
}

bool Thread::QueueBlocker::unblock()
{
    {
        ScopedSpinLock lock(m_lock);
        if (m_did_unblock)
            return false;
        m_did_unblock = true;
    }

    unblock_from_blocker();
    return true;
}

Thread::FutexBlocker::FutexBlocker(FutexQueue& futex_queue, u32 bitset)
    : m_bitset(bitset)
{
    if (!set_block_condition(futex_queue, Thread::current()))
        m_should_block = false;
}

Thread::FutexBlocker::~FutexBlocker()
{
}

void Thread::FutexBlocker::finish_requeue(FutexQueue& futex_queue)
{
    ASSERT(m_lock.own_lock());
    set_block_condition_raw_locked(&futex_queue);
    // We can now release the lock
    m_lock.unlock(m_relock_flags);
}

bool Thread::FutexBlocker::unblock_bitset(u32 bitset)
{
    {
        ScopedSpinLock lock(m_lock);
        if (m_did_unblock || (bitset != FUTEX_BITSET_MATCH_ANY && (m_bitset & bitset) == 0))
            return false;

        m_did_unblock = true;
    }

    unblock_from_blocker();
    return true;
}

bool Thread::FutexBlocker::unblock(bool force)
{
    {
        ScopedSpinLock lock(m_lock);
        if (m_did_unblock)
            return force;
        m_did_unblock = true;
    }

    unblock_from_blocker();
    return true;
}

Thread::FileDescriptionBlocker::FileDescriptionBlocker(FileDescription& description, BlockFlags flags, BlockFlags& unblocked_flags)
    : m_blocked_description(description)
    , m_flags(flags)
    , m_unblocked_flags(unblocked_flags)
{
    m_unblocked_flags = BlockFlags::None;
    if (!set_block_condition(description.block_condition()))
        m_should_block = false;
}

bool Thread::FileDescriptionBlocker::unblock(bool from_add_blocker, void*)
{
    auto unblock_flags = m_blocked_description->should_unblock(m_flags);
    if (unblock_flags == BlockFlags::None)
        return false;

    {
        ScopedSpinLock lock(m_lock);
        if (m_did_unblock)
            return false;
        m_did_unblock = true;
        m_unblocked_flags = unblock_flags;
    }

    if (!from_add_blocker)
        unblock_from_blocker();
    return true;
}

void Thread::FileDescriptionBlocker::not_blocking(bool timeout_in_past)
{
    if (!m_should_block) {
        // set_block_condition returned false, so unblock was already called
        ASSERT(!timeout_in_past);
        return;
    }
    // If we should have blocked but got here it must have been that the
    // timeout was already in the past. So we need to ask the BlockCondition
    // to supply us the information. We cannot hold the lock as unblock
    // could be called by the BlockCondition at any time!
    ASSERT(timeout_in_past);

    // Just call unblock here because we will query the file description
    // for the data and don't need any input from the FileBlockCondition.
    // However, it's possible that if timeout_in_past is true then FileBlockCondition
    // may call us at any given time, so our call to unblock here may fail.
    // Either way, unblock will be called at least once, which provides
    // all the data we need.
    unblock(false, nullptr);
}

const FileDescription& Thread::FileDescriptionBlocker::blocked_description() const
{
    return m_blocked_description;
}

Thread::AcceptBlocker::AcceptBlocker(FileDescription& description, BlockFlags& unblocked_flags)
    : FileDescriptionBlocker(description, (BlockFlags)((u32)BlockFlags::Accept | (u32)BlockFlags::Exception), unblocked_flags)
{
}

Thread::ConnectBlocker::ConnectBlocker(FileDescription& description, BlockFlags& unblocked_flags)
    : FileDescriptionBlocker(description, (BlockFlags)((u32)BlockFlags::Connect | (u32)BlockFlags::Exception), unblocked_flags)
{
}

Thread::WriteBlocker::WriteBlocker(FileDescription& description, BlockFlags& unblocked_flags)
    : FileDescriptionBlocker(description, (BlockFlags)((u32)BlockFlags::Write | (u32)BlockFlags::Exception), unblocked_flags)
{
}

auto Thread::WriteBlocker::override_timeout(const BlockTimeout& timeout) -> const BlockTimeout&
{
    auto& description = blocked_description();
    if (description.is_socket()) {
        auto& socket = *description.socket();
        if (socket.has_send_timeout()) {
            m_timeout = BlockTimeout(false, &socket.send_timeout(), timeout.start_time(), timeout.clock_id());
            if (timeout.is_infinite() || (!m_timeout.is_infinite() && m_timeout.absolute_time() < timeout.absolute_time()))
                return m_timeout;
        }
    }
    return timeout;
}

Thread::ReadBlocker::ReadBlocker(FileDescription& description, BlockFlags& unblocked_flags)
    : FileDescriptionBlocker(description, (BlockFlags)((u32)BlockFlags::Read | (u32)BlockFlags::Exception), unblocked_flags)
{
}

auto Thread::ReadBlocker::override_timeout(const BlockTimeout& timeout) -> const BlockTimeout&
{
    auto& description = blocked_description();
    if (description.is_socket()) {
        auto& socket = *description.socket();
        if (socket.has_receive_timeout()) {
            m_timeout = BlockTimeout(false, &socket.receive_timeout(), timeout.start_time(), timeout.clock_id());
            if (timeout.is_infinite() || (!m_timeout.is_infinite() && m_timeout.absolute_time() < timeout.absolute_time()))
                return m_timeout;
        }
    }
    return timeout;
}

Thread::SleepBlocker::SleepBlocker(const BlockTimeout& deadline, timespec* remaining)
    : m_deadline(deadline)
    , m_remaining(remaining)
{
}

auto Thread::SleepBlocker::override_timeout(const BlockTimeout& timeout) -> const BlockTimeout&
{
    ASSERT(timeout.is_infinite()); // A timeout should not be provided
    // To simplify things only use the sleep deadline.
    return m_deadline;
}

void Thread::SleepBlocker::not_blocking(bool timeout_in_past)
{
    // SleepBlocker::should_block should always return true, so timeout
    // in the past is the only valid case when this function is called
    ASSERT(timeout_in_past);
    calculate_remaining();
}

void Thread::SleepBlocker::was_unblocked(bool did_timeout)
{
    Blocker::was_unblocked(did_timeout);

    calculate_remaining();
}

void Thread::SleepBlocker::calculate_remaining()
{
    if (!m_remaining)
        return;
    auto time_now = TimeManagement::the().current_time(m_deadline.clock_id()).value();
    if (time_now < m_deadline.absolute_time())
        timespec_sub(m_deadline.absolute_time(), time_now, *m_remaining);
    else
        *m_remaining = {};
}

Thread::BlockResult Thread::SleepBlocker::block_result()
{
    auto result = Blocker::block_result();
    if (result == Thread::BlockResult::InterruptedByTimeout)
        return Thread::BlockResult::WokeNormally;
    return result;
}

Thread::SelectBlocker::SelectBlocker(FDVector& fds)
    : m_fds(fds)
{
    for (auto& fd_entry : m_fds) {
        fd_entry.unblocked_flags = FileBlocker::BlockFlags::None;

        if (!m_should_block)
            continue;
        if (!fd_entry.description->block_condition().add_blocker(*this, &fd_entry))
            m_should_block = false;
    }
}

Thread::SelectBlocker::~SelectBlocker()
{
    for (auto& fd_entry : m_fds)
        fd_entry.description->block_condition().remove_blocker(*this, &fd_entry);
}

void Thread::SelectBlocker::not_blocking(bool timeout_in_past)
{
    // Either the timeout was in the past or we didn't add all blockers
    ASSERT(timeout_in_past || !m_should_block);
    ScopedSpinLock lock(m_lock);
    if (!m_did_unblock) {
        m_did_unblock = true;
        if (!timeout_in_past) {
            auto count = collect_unblocked_flags();
            ASSERT(count > 0);
        }
    }
}

bool Thread::SelectBlocker::unblock(bool from_add_blocker, void* data)
{
    ASSERT(data); // data is a pointer to an entry in the m_fds vector
    auto& fd_info = *static_cast<FDInfo*>(data);

    {
        ScopedSpinLock lock(m_lock);
        if (m_did_unblock)
            return false;

        auto unblock_flags = fd_info.description->should_unblock(fd_info.block_flags);
        if (unblock_flags == BlockFlags::None)
            return false;

        m_did_unblock = true;

        // We need to store unblock_flags here, otherwise someone else
        // affecting this file descriptor could change the information
        // between now and when was_unblocked is called!
        fd_info.unblocked_flags = unblock_flags;
    }

    // Only do this once for the first one
    if (!from_add_blocker)
        unblock_from_blocker();
    return true;
}

size_t Thread::SelectBlocker::collect_unblocked_flags()
{
    size_t count = 0;
    for (auto& fd_entry : m_fds) {
        ASSERT(fd_entry.block_flags != FileBlocker::BlockFlags::None);

        // unblock will have set at least the first descriptor's unblock
        // flags that triggered the unblock. Make sure we don't discard that
        // information as it may have changed by now!
        if (fd_entry.unblocked_flags == FileBlocker::BlockFlags::None)
            fd_entry.unblocked_flags = fd_entry.description->should_unblock(fd_entry.block_flags);

        if (fd_entry.unblocked_flags != FileBlocker::BlockFlags::None)
            count++;
    }
    return count;
}

void Thread::SelectBlocker::was_unblocked(bool did_timeout)
{
    Blocker::was_unblocked(did_timeout);
    if (!did_timeout && !was_interrupted()) {
        {
            ScopedSpinLock lock(m_lock);
            ASSERT(m_did_unblock);
        }
        size_t count = collect_unblocked_flags();
        // If we were blocked and didn't time out, we should have at least one unblocked fd!
        ASSERT(count > 0);
    }
}

Thread::WaitBlockCondition::ProcessBlockInfo::ProcessBlockInfo(NonnullRefPtr<Process>&& process, WaitBlocker::UnblockFlags flags, u8 signal)
    : process(move(process))
    , flags(flags)
    , signal(signal)
{
}

Thread::WaitBlockCondition::ProcessBlockInfo::~ProcessBlockInfo()
{
}

void Thread::WaitBlockCondition::try_unblock(Thread::WaitBlocker& blocker)
{
    ScopedSpinLock lock(m_lock);
    // We if we have any processes pending
    for (size_t i = 0; i < m_processes.size(); i++) {
        auto& info = m_processes[i];
        // We need to call unblock as if we were called from add_blocker
        // so that we don't trigger a context switch by yielding!
        if (info.was_waited && blocker.is_wait())
            continue; // This state was already waited on, do not unblock
        if (blocker.unblock(info.process, info.flags, info.signal, true)) {
            if (blocker.is_wait()) {
                if (info.flags == Thread::WaitBlocker::UnblockFlags::Terminated) {
                    m_processes.remove(i);
                    dbgln<debug_waitblock>("WaitBlockCondition[{}] terminated, remove {}", m_process, *info.process);
                } else {
                    dbgln<debug_waitblock>("WaitBlockCondition[{}] terminated, mark as waited {}", m_process, *info.process);
                    info.was_waited = true;
                }
            }
            break;
        }
    }
}

void Thread::WaitBlockCondition::disowned_by_waiter(Process& process)
{
    ScopedSpinLock lock(m_lock);
    if (m_finalized)
        return;
    for (size_t i = 0; i < m_processes.size();) {
        auto& info = m_processes[i];
        if (info.process == &process) {
            do_unblock([&](Blocker& b, void*, bool&) {
                ASSERT(b.blocker_type() == Blocker::Type::Wait);
                auto& blocker = static_cast<WaitBlocker&>(b);
                bool did_unblock = blocker.unblock(info.process, WaitBlocker::UnblockFlags::Disowned, 0, false);
                ASSERT(did_unblock); // disowning must unblock everyone
                return true;
            });
            dbgln<debug_waitblock>("WaitBlockCondition[{}] disowned {}", m_process, *info.process);
            m_processes.remove(i);
            continue;
        }

        i++;
    }
}

bool Thread::WaitBlockCondition::unblock(Process& process, WaitBlocker::UnblockFlags flags, u8 signal)
{
    ASSERT(flags != WaitBlocker::UnblockFlags::Disowned);

    bool did_unblock_any = false;
    bool did_wait = false;
    bool was_waited_already = false;

    ScopedSpinLock lock(m_lock);
    if (m_finalized)
        return false;
    if (flags != WaitBlocker::UnblockFlags::Terminated) {
        // First check if this state was already waited on
        for (auto& info : m_processes) {
            if (info.process == &process) {
                was_waited_already = info.was_waited;
                break;
            }
        }
    }

    do_unblock([&](Blocker& b, void*, bool&) {
        ASSERT(b.blocker_type() == Blocker::Type::Wait);
        auto& blocker = static_cast<WaitBlocker&>(b);
        if (was_waited_already && blocker.is_wait())
            return false; // This state was already waited on, do not unblock
        if (blocker.unblock(process, flags, signal, false)) {
            did_wait |= blocker.is_wait(); // anyone requesting a wait
            did_unblock_any = true;
            return true;
        }
        return false;
    });

    // If no one has waited (yet), or this wasn't a wait, or if it's anything other than
    // UnblockFlags::Terminated then add it to your list
    if (!did_unblock_any || !did_wait || flags != WaitBlocker::UnblockFlags::Terminated) {
        bool updated_existing = false;
        for (auto& info : m_processes) {
            if (info.process == &process) {
                ASSERT(info.flags != WaitBlocker::UnblockFlags::Terminated);
                info.flags = flags;
                info.signal = signal;
                info.was_waited = did_wait;
                dbgln<debug_waitblock>("WaitBlockCondition[{}] update {} flags={}, waited={}", m_process, process, (int)flags, info.was_waited);
                updated_existing = true;
                break;
            }
        }
        if (!updated_existing) {
            dbgln<debug_waitblock>("WaitBlockCondition[{}] add {} flags: {}", m_process, process, (int)flags);
            m_processes.append(ProcessBlockInfo(process, flags, signal));
        }
    }
    return did_unblock_any;
}

bool Thread::WaitBlockCondition::should_add_blocker(Blocker& b, void*)
{
    // NOTE: m_lock is held already!
    if (m_finalized)
        return false;
    ASSERT(b.blocker_type() == Blocker::Type::Wait);
    auto& blocker = static_cast<WaitBlocker&>(b);
    // See if we can match any process immediately
    for (size_t i = 0; i < m_processes.size(); i++) {
        auto& info = m_processes[i];
        if (blocker.unblock(info.process, info.flags, info.signal, true)) {
            // Only remove the entry if UnblockFlags::Terminated
            if (info.flags == Thread::WaitBlocker::UnblockFlags::Terminated && blocker.is_wait())
                m_processes.remove(i);
            return false;
        }
    }
    return true;
}

void Thread::WaitBlockCondition::finalize()
{
    ScopedSpinLock lock(m_lock);
    ASSERT(!m_finalized);
    m_finalized = true;

    // Clear the list of threads here so we can drop the references to them
    m_processes.clear();

    // No more waiters, drop the last reference immediately. This may
    // cause us to be destructed ourselves!
    ASSERT(m_process.ref_count() > 0);
    m_process.unref();
}

Thread::WaitBlocker::WaitBlocker(int wait_options, idtype_t id_type, pid_t id, KResultOr<siginfo_t>& result)
    : m_wait_options(wait_options)
    , m_id_type(id_type)
    , m_waitee_id(id)
    , m_result(result)
    , m_should_block(!(m_wait_options & WNOHANG))
{
    switch (id_type) {
    case P_PID: {
        m_waitee = Process::from_pid(m_waitee_id);
        if (!m_waitee || m_waitee->ppid() != Process::current()->pid()) {
            m_result = ECHILD;
            m_error = true;
            return;
        }
        break;
    }
    case P_PGID: {
        m_waitee_group = ProcessGroup::from_pgid(m_waitee_id);
        if (!m_waitee_group) {
            m_result = ECHILD;
            m_error = true;
            return;
        }
        break;
    }
    case P_ALL:
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    // NOTE: unblock may be called within set_block_condition, in which
    // case it means that we already have a match without having to block.
    // In that case set_block_condition will return false.
    if (m_error || !set_block_condition(Process::current()->wait_block_condition()))
        m_should_block = false;
}

void Thread::WaitBlocker::not_blocking(bool timeout_in_past)
{
    ASSERT(timeout_in_past || !m_should_block);
    if (!m_error)
        Process::current()->wait_block_condition().try_unblock(*this);
}

void Thread::WaitBlocker::was_unblocked(bool)
{
    bool got_sigchld, try_unblock;
    {
        ScopedSpinLock lock(m_lock);
        try_unblock = !m_did_unblock;
        got_sigchld = m_got_sigchild;
    }

    if (try_unblock)
        Process::current()->wait_block_condition().try_unblock(*this);

    // If we were interrupted by SIGCHLD (which gets special handling
    // here) we're not going to return with EINTR. But we're going to
    // deliver SIGCHLD (only) here.
    auto* current_thread = Thread::current();
    if (got_sigchld && current_thread->state() != State::Stopped)
        current_thread->try_dispatch_one_pending_signal(SIGCHLD);
}

void Thread::WaitBlocker::do_was_disowned()
{
    ASSERT(!m_did_unblock);
    m_did_unblock = true;
    m_result = ECHILD;
}

void Thread::WaitBlocker::do_set_result(const siginfo_t& result)
{
    ASSERT(!m_did_unblock);
    m_did_unblock = true;
    m_result = result;

    if (do_get_interrupted_by_signal() == SIGCHLD) {
        // This makes it so that wait() will return normally despite the
        // fact that SIGCHLD was delivered. Calling do_clear_interrupted_by_signal
        // will disable dispatching signals in Thread::block and prevent
        // it from returning with EINTR. We will then manually dispatch
        // SIGCHLD (and only SIGCHLD) in was_unblocked.
        m_got_sigchild = true;
        do_clear_interrupted_by_signal();
    }
}

bool Thread::WaitBlocker::unblock(Process& process, UnblockFlags flags, u8 signal, bool from_add_blocker)
{
    ASSERT(flags != UnblockFlags::Terminated || signal == 0); // signal argument should be ignored for Terminated

    switch (m_id_type) {
    case P_PID:
        ASSERT(m_waitee);
        if (process.pid() != m_waitee_id)
            return false;
        break;
    case P_PGID:
        ASSERT(m_waitee_group);
        if (process.pgid() != m_waitee_group->pgid())
            return false;
        break;
    case P_ALL:
        if (flags == UnblockFlags::Disowned) {
            // Generic waiter won't be unblocked by disown
            return false;
        }
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    switch (flags) {
    case UnblockFlags::Terminated:
        if (!(m_wait_options & WEXITED))
            return false;
        break;
    case UnblockFlags::Stopped:
        if (!(m_wait_options & WSTOPPED))
            return false;
        if (!(m_wait_options & WUNTRACED) && !process.is_traced())
            return false;
        break;
    case UnblockFlags::Continued:
        if (!(m_wait_options & WCONTINUED))
            return false;
        if (!(m_wait_options & WUNTRACED) && !process.is_traced())
            return false;
        break;
    case UnblockFlags::Disowned:
        ScopedSpinLock lock(m_lock);
        // Disowning must unblock anyone waiting for this process explicitly
        if (!m_did_unblock)
            do_was_disowned();
        return true;
    }

    if (flags == UnblockFlags::Terminated) {
        ASSERT(process.is_dead());

        ScopedSpinLock lock(m_lock);
        if (m_did_unblock)
            return false;
        // Up until this point, this function may have been called
        // more than once!
        do_set_result(process.wait_info());
    } else {
        siginfo_t siginfo;
        memset(&siginfo, 0, sizeof(siginfo));
        {
            ScopedSpinLock lock(g_scheduler_lock);
            // We need to gather the information before we release the sheduler lock!
            siginfo.si_signo = SIGCHLD;
            siginfo.si_pid = process.pid().value();
            siginfo.si_uid = process.uid();
            siginfo.si_status = signal;

            switch (flags) {
            case UnblockFlags::Terminated:
            case UnblockFlags::Disowned:
                ASSERT_NOT_REACHED();
            case UnblockFlags::Stopped:
                siginfo.si_code = CLD_STOPPED;
                break;
            case UnblockFlags::Continued:
                siginfo.si_code = CLD_CONTINUED;
                break;
            }
        }

        ScopedSpinLock lock(m_lock);
        if (m_did_unblock)
            return false;
        // Up until this point, this function may have been called
        // more than once!
        do_set_result(siginfo);
    }

    if (!from_add_blocker) {
        // Only call unblock if we weren't called from within set_block_condition!
        ASSERT(flags != UnblockFlags::Disowned);
        unblock_from_blocker();
    }
    // Because this may be called from add_blocker, in which case we should
    // not be actually trying to unblock the thread (because it hasn't actually
    // been blocked yet), we need to return true anyway
    return true;
}

}
