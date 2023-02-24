/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BuiltinWrappers.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

Thread::BlockTimeout::BlockTimeout(bool is_absolute, Duration const* time, Duration const* start_time, clockid_t clock_id)
    : m_clock_id(clock_id)
    , m_infinite(!time)
{
    if (m_infinite)
        return;
    if (*time > Duration::zero())
        m_time = *time;
    m_start_time = start_time ? *start_time : TimeManagement::the().current_time(clock_id);
    if (!is_absolute)
        m_time += m_start_time;
}

bool Thread::Blocker::add_to_blocker_set(Thread::BlockerSet& blocker_set, void* data)
{
    VERIFY(!m_blocker_set);
    if (blocker_set.add_blocker(*this, data)) {
        m_blocker_set = &blocker_set;
        return true;
    }
    return false;
}

Thread::Blocker::~Blocker() = default;

void Thread::Blocker::finalize()
{
    if (m_blocker_set)
        m_blocker_set->remove_blocker(*this);
}

bool Thread::Blocker::setup_blocker()
{
    return true;
}

void Thread::Blocker::begin_blocking(Badge<Thread>)
{
    SpinlockLocker lock(m_lock);
    VERIFY(!m_is_blocking);
    m_is_blocking = true;
}

auto Thread::Blocker::end_blocking(Badge<Thread>, bool did_timeout) -> BlockResult
{
    SpinlockLocker lock(m_lock);
    // if m_is_blocking is false here, some thread forced to
    // unblock us when we get here. This is only called from the
    // thread that was blocked.
    VERIFY(Thread::current() == m_thread);
    m_is_blocking = false;

    was_unblocked(did_timeout);
    return block_result();
}

Thread::JoinBlocker::JoinBlocker(Thread& joinee, ErrorOr<void>& try_join_result, void*& joinee_exit_value)
    : m_joinee(joinee)
    , m_joinee_exit_value(joinee_exit_value)
    , m_try_join_result(try_join_result)
{
}

bool Thread::JoinBlocker::setup_blocker()
{
    // We need to hold our lock to avoid a race where try_join succeeds
    // but the joinee is joining immediately
    SpinlockLocker lock(m_lock);
    bool should_block = true;
    m_try_join_result = m_joinee->try_join([&]() {
        if (!add_to_blocker_set(m_joinee->m_join_blocker_set))
            should_block = false;
    });
    if (m_try_join_result.is_error())
        return false;
    return should_block;
}

void Thread::JoinBlocker::will_unblock_immediately_without_blocking(UnblockImmediatelyReason reason)
{
    // If we should have blocked but got here it must have been that the
    // timeout was already in the past. So we need to ask the BlockerSet
    // to supply us the information. We cannot hold the lock as unblock
    // could be called by the BlockerSet at any time!
    if (reason == UnblockImmediatelyReason::TimeoutInThePast) {
        m_joinee->m_join_blocker_set.try_unblock(*this);
    }
}

bool Thread::JoinBlocker::unblock(void* value, bool from_add_blocker)
{
    {
        SpinlockLocker lock(m_lock);
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

Thread::WaitQueueBlocker::WaitQueueBlocker(WaitQueue& wait_queue, StringView block_reason)
    : m_wait_queue(wait_queue)
    , m_block_reason(block_reason)
{
}

bool Thread::WaitQueueBlocker::setup_blocker()
{
    return add_to_blocker_set(m_wait_queue);
}

Thread::WaitQueueBlocker::~WaitQueueBlocker() = default;

bool Thread::WaitQueueBlocker::unblock()
{
    {
        SpinlockLocker lock(m_lock);
        if (m_did_unblock)
            return false;
        m_did_unblock = true;
    }

    unblock_from_blocker();
    return true;
}

Thread::FutexBlocker::FutexBlocker(FutexQueue& futex_queue, u32 bitset)
    : m_futex_queue(futex_queue)
    , m_bitset(bitset)
{
}

bool Thread::FutexBlocker::setup_blocker()
{
    return add_to_blocker_set(m_futex_queue);
}

Thread::FutexBlocker::~FutexBlocker() = default;

void Thread::FutexBlocker::finish_requeue(FutexQueue& futex_queue)
{
    VERIFY(m_lock.is_locked_by_current_processor());
    set_blocker_set_raw_locked(&futex_queue);
    // We can now release the lock
    m_lock.unlock(m_previous_interrupts_state);
}

bool Thread::FutexBlocker::unblock_bitset(u32 bitset)
{
    {
        SpinlockLocker lock(m_lock);
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
        SpinlockLocker lock(m_lock);
        if (m_did_unblock)
            return force;
        m_did_unblock = true;
    }

    unblock_from_blocker();
    return true;
}

Thread::OpenFileDescriptionBlocker::OpenFileDescriptionBlocker(OpenFileDescription& description, BlockFlags flags, BlockFlags& unblocked_flags)
    : m_blocked_description(description)
    , m_flags(flags)
    , m_unblocked_flags(unblocked_flags)
{
}

bool Thread::OpenFileDescriptionBlocker::setup_blocker()
{
    m_unblocked_flags = BlockFlags::None;
    return add_to_blocker_set(m_blocked_description->blocker_set());
}

bool Thread::OpenFileDescriptionBlocker::unblock_if_conditions_are_met(bool from_add_blocker, void*)
{
    auto unblock_flags = m_blocked_description->should_unblock(m_flags);
    if (unblock_flags == BlockFlags::None)
        return false;

    {
        SpinlockLocker lock(m_lock);
        if (m_did_unblock)
            return false;
        m_did_unblock = true;
        m_unblocked_flags = unblock_flags;
    }

    if (!from_add_blocker)
        unblock_from_blocker();
    return true;
}

void Thread::OpenFileDescriptionBlocker::will_unblock_immediately_without_blocking(UnblockImmediatelyReason reason)
{
    if (reason == UnblockImmediatelyReason::UnblockConditionAlreadyMet)
        return;

    // If we should have blocked but got here it must have been that the
    // timeout was already in the past. So we need to ask the BlockerSet
    // to supply us the information. We cannot hold the lock as unblock
    // could be called by the BlockerSet at any time!
    VERIFY(reason == UnblockImmediatelyReason::TimeoutInThePast);

    // Just call unblock_if_conditions_are_met here because we will query the file description
    // for the data and don't need any input from the FileBlockerSet.
    // However, it's possible that if timeout_in_past is true then FileBlockerSet
    // may call us at any given time, so our call to unblock here may fail.
    // Either way, unblock will be called at least once, which provides
    // all the data we need.
    unblock_if_conditions_are_met(false, nullptr);
}

OpenFileDescription const& Thread::OpenFileDescriptionBlocker::blocked_description() const
{
    return m_blocked_description;
}

Thread::AcceptBlocker::AcceptBlocker(OpenFileDescription& description, BlockFlags& unblocked_flags)
    : OpenFileDescriptionBlocker(description, BlockFlags::Accept | BlockFlags::Exception, unblocked_flags)
{
}

Thread::ConnectBlocker::ConnectBlocker(OpenFileDescription& description, BlockFlags& unblocked_flags)
    : OpenFileDescriptionBlocker(description, BlockFlags::Connect | BlockFlags::Exception, unblocked_flags)
{
}

Thread::WriteBlocker::WriteBlocker(OpenFileDescription& description, BlockFlags& unblocked_flags)
    : OpenFileDescriptionBlocker(description, BlockFlags::Write | BlockFlags::Exception, unblocked_flags)
{
}

auto Thread::WriteBlocker::override_timeout(BlockTimeout const& timeout) -> BlockTimeout const&
{
    auto const& description = blocked_description();
    if (description.is_socket()) {
        auto const& socket = *description.socket();
        if (socket.has_send_timeout()) {
            Duration send_timeout = socket.send_timeout();
            m_timeout = BlockTimeout(false, &send_timeout, timeout.start_time(), timeout.clock_id());
            if (timeout.is_infinite() || (!m_timeout.is_infinite() && m_timeout.absolute_time() < timeout.absolute_time()))
                return m_timeout;
        }
    }
    return timeout;
}

Thread::ReadBlocker::ReadBlocker(OpenFileDescription& description, BlockFlags& unblocked_flags)
    : OpenFileDescriptionBlocker(description, BlockFlags::Read | BlockFlags::Exception, unblocked_flags)
{
}

auto Thread::ReadBlocker::override_timeout(BlockTimeout const& timeout) -> BlockTimeout const&
{
    auto const& description = blocked_description();
    if (description.is_socket()) {
        auto const& socket = *description.socket();
        if (socket.has_receive_timeout()) {
            Duration receive_timeout = socket.receive_timeout();
            m_timeout = BlockTimeout(false, &receive_timeout, timeout.start_time(), timeout.clock_id());
            if (timeout.is_infinite() || (!m_timeout.is_infinite() && m_timeout.absolute_time() < timeout.absolute_time()))
                return m_timeout;
        }
    }
    return timeout;
}

Thread::SleepBlocker::SleepBlocker(BlockTimeout const& deadline, Duration* remaining)
    : m_deadline(deadline)
    , m_remaining(remaining)
{
}

auto Thread::SleepBlocker::override_timeout(BlockTimeout const& timeout) -> BlockTimeout const&
{
    VERIFY(timeout.is_infinite()); // A timeout should not be provided
    // To simplify things only use the sleep deadline.
    return m_deadline;
}

void Thread::SleepBlocker::will_unblock_immediately_without_blocking(UnblockImmediatelyReason reason)
{
    // SleepBlocker::should_block should always return true, so timeout
    // in the past is the only valid case when this function is called
    VERIFY(reason == UnblockImmediatelyReason::TimeoutInThePast);
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
    auto time_now = TimeManagement::the().current_time(m_deadline.clock_id());
    if (time_now < m_deadline.absolute_time())
        *m_remaining = m_deadline.absolute_time() - time_now;
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
}

bool Thread::SelectBlocker::setup_blocker()
{
    bool should_block = true;
    for (auto& fd_entry : m_fds) {
        fd_entry.unblocked_flags = FileBlocker::BlockFlags::None;

        if (!should_block)
            continue;
        if (!fd_entry.description) {
            should_block = false;
            continue;
        }
        if (!fd_entry.description->blocker_set().add_blocker(*this, &fd_entry))
            should_block = false;
    }
    return should_block;
}

Thread::SelectBlocker::~SelectBlocker() = default;

void Thread::SelectBlocker::finalize()
{
    Thread::FileBlocker::finalize();
    for (auto& fd_entry : m_fds) {
        if (fd_entry.description)
            fd_entry.description->blocker_set().remove_blocker(*this);
    }
}

void Thread::SelectBlocker::will_unblock_immediately_without_blocking(UnblockImmediatelyReason reason)
{
    SpinlockLocker lock(m_lock);
    if (m_did_unblock)
        return;
    m_did_unblock = true;
    if (reason == UnblockImmediatelyReason::UnblockConditionAlreadyMet) {
        auto count = collect_unblocked_flags();
        VERIFY(count > 0);
    }
}

bool Thread::SelectBlocker::unblock_if_conditions_are_met(bool from_add_blocker, void* data)
{
    VERIFY(data); // data is a pointer to an entry in the m_fds vector
    auto& fd_info = *static_cast<FDInfo*>(data);

    {
        SpinlockLocker lock(m_lock);
        if (m_did_unblock)
            return false;

        VERIFY(fd_info.description);
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
        VERIFY(fd_entry.block_flags != FileBlocker::BlockFlags::None);

        if (!fd_entry.description) {
            count++;
            continue;
        }

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
            SpinlockLocker lock(m_lock);
            VERIFY(m_did_unblock);
        }
        size_t count = collect_unblocked_flags();
        // If we were blocked and didn't time out, we should have at least one unblocked fd!
        VERIFY(count > 0);
    }
}

Thread::SignalBlocker::SignalBlocker(sigset_t pending_set, siginfo_t& result)
    : m_pending_set(pending_set)
    , m_result(result)
{
}

void Thread::SignalBlocker::will_unblock_immediately_without_blocking(UnblockImmediatelyReason unblock_immediately_reason)
{
    if (unblock_immediately_reason != UnblockImmediatelyReason::TimeoutInThePast)
        return;
    // If the specified timeout is 0 the caller is simply trying to poll once for pending signals,
    // so simply calling check_pending_signals should populate the requested information.
    check_pending_signals(false);
}

bool Thread::SignalBlocker::setup_blocker()
{
    return add_to_blocker_set(thread().m_signal_blocker_set);
}

bool Thread::SignalBlocker::check_pending_signals(bool from_add_blocker)
{
    {
        SpinlockLocker lock(m_lock);
        if (m_did_unblock)
            return false;

        auto pending_signals = thread().pending_signals() & m_pending_set;

        // Also unblock if we have just "handled" that signal and are in the procecss
        // of running their signal handler (i.e. we just unmarked the signal as pending).
        if (thread().m_currently_handled_signal)
            pending_signals |= (1 << (thread().m_currently_handled_signal - 1)) & m_pending_set;

        auto matching_pending_signal = bit_scan_forward(pending_signals);

        if (matching_pending_signal == 0)
            return false;

        m_did_unblock = true;
        m_result = {};
        m_result.si_signo = matching_pending_signal;
        m_result.si_code = 0; // FIXME: How can we determine this?
    }

    if (!from_add_blocker)
        unblock_from_blocker();
    return true;
}

Thread::WaitBlockerSet::ProcessBlockInfo::ProcessBlockInfo(NonnullRefPtr<Process>&& process, WaitBlocker::UnblockFlags flags, u8 signal)
    : process(move(process))
    , flags(flags)
    , signal(signal)
{
}

Thread::WaitBlockerSet::ProcessBlockInfo::~ProcessBlockInfo() = default;

void Thread::WaitBlockerSet::try_unblock(Thread::WaitBlocker& blocker)
{
    SpinlockLocker lock(m_lock);
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
                    dbgln_if(WAITBLOCK_DEBUG, "WaitBlockerSet[{}] terminated, remove {}", m_process, *info.process);
                } else {
                    dbgln_if(WAITBLOCK_DEBUG, "WaitBlockerSet[{}] terminated, mark as waited {}", m_process, *info.process);
                    info.was_waited = true;
                }
            }
            break;
        }
    }
}

void Thread::WaitBlockerSet::disowned_by_waiter(Process& process)
{
    SpinlockLocker lock(m_lock);
    if (m_finalized)
        return;
    for (size_t i = 0; i < m_processes.size();) {
        auto& info = m_processes[i];
        if (info.process == &process) {
            unblock_all_blockers_whose_conditions_are_met_locked([&](Blocker& b, void*, bool&) {
                VERIFY(b.blocker_type() == Blocker::Type::Wait);
                auto& blocker = static_cast<WaitBlocker&>(b);
                bool did_unblock = blocker.unblock(info.process, WaitBlocker::UnblockFlags::Disowned, 0, false);
                VERIFY(did_unblock); // disowning must unblock everyone
                return true;
            });
            dbgln_if(WAITBLOCK_DEBUG, "WaitBlockerSet[{}] disowned {}", m_process, *info.process);
            m_processes.remove(i);
            continue;
        }

        i++;
    }
}

bool Thread::WaitBlockerSet::unblock(Process& process, WaitBlocker::UnblockFlags flags, u8 signal)
{
    VERIFY(flags != WaitBlocker::UnblockFlags::Disowned);

    bool did_unblock_any = false;
    bool did_wait = false;
    bool was_waited_already = false;

    SpinlockLocker lock(m_lock);
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

    unblock_all_blockers_whose_conditions_are_met_locked([&](Blocker& b, void*, bool&) {
        VERIFY(b.blocker_type() == Blocker::Type::Wait);
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
                VERIFY(info.flags != WaitBlocker::UnblockFlags::Terminated);
                info.flags = flags;
                info.signal = signal;
                info.was_waited = did_wait;
                dbgln_if(WAITBLOCK_DEBUG, "WaitBlockerSet[{}] update {} flags={}, waited={}", m_process, process, (int)flags, info.was_waited);
                updated_existing = true;
                break;
            }
        }
        if (!updated_existing) {
            dbgln_if(WAITBLOCK_DEBUG, "WaitBlockerSet[{}] add {} flags: {}", m_process, process, (int)flags);
            m_processes.append(ProcessBlockInfo(process, flags, signal));
        }
    }
    return did_unblock_any;
}

bool Thread::WaitBlockerSet::should_add_blocker(Blocker& b, void*)
{
    // NOTE: m_lock is held already!
    if (m_finalized)
        return false;
    VERIFY(b.blocker_type() == Blocker::Type::Wait);
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

void Thread::WaitBlockerSet::finalize()
{
    SpinlockLocker lock(m_lock);
    VERIFY(!m_finalized);
    m_finalized = true;

    // Clear the list of threads here so we can drop the references to them
    m_processes.clear();

    // NOTE: Kernel processes don't have a leaked ref on them.
    if (!m_process.is_kernel_process()) {
        // No more waiters, drop the last reference immediately. This may
        // cause us to be destructed ourselves!
        VERIFY(m_process.ref_count() > 0);
        m_process.unref();
    }
}

Thread::WaitBlocker::WaitBlocker(int wait_options, Variant<Empty, NonnullRefPtr<Process>, NonnullRefPtr<ProcessGroup>> waitee, ErrorOr<siginfo_t>& result)
    : m_wait_options(wait_options)
    , m_result(result)
    , m_waitee(move(waitee))
{
}

bool Thread::WaitBlocker::setup_blocker()
{
    if (m_wait_options & WNOHANG)
        return false;
    return add_to_blocker_set(Process::current().wait_blocker_set());
}

void Thread::WaitBlocker::will_unblock_immediately_without_blocking(UnblockImmediatelyReason)
{
    Process::current().wait_blocker_set().try_unblock(*this);
}

void Thread::WaitBlocker::was_unblocked(bool)
{
    bool got_sigchld, try_unblock;
    {
        SpinlockLocker lock(m_lock);
        try_unblock = !m_did_unblock;
        got_sigchld = m_got_sigchild;
    }

    if (try_unblock)
        Process::current().wait_blocker_set().try_unblock(*this);

    // If we were interrupted by SIGCHLD (which gets special handling
    // here) we're not going to return with EINTR. But we're going to
    // deliver SIGCHLD (only) here.
    auto* current_thread = Thread::current();
    if (got_sigchld && current_thread->state() != State::Stopped)
        current_thread->try_dispatch_one_pending_signal(SIGCHLD);
}

void Thread::WaitBlocker::do_was_disowned()
{
    VERIFY(!m_did_unblock);
    m_did_unblock = true;
    m_result = ECHILD;
}

void Thread::WaitBlocker::do_set_result(siginfo_t const& result)
{
    VERIFY(!m_did_unblock);
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
    VERIFY(flags != UnblockFlags::Terminated || signal == 0); // signal argument should be ignored for Terminated

    bool do_not_unblock = m_waitee.visit(
        [&](NonnullRefPtr<Process> const& waitee_process) {
            return &process != waitee_process;
        },
        [&](NonnullRefPtr<ProcessGroup> const& waitee_process_group) {
            return waitee_process_group->pgid() != process.pgid();
        },
        [&](Empty const&) {
            // Generic waiter won't be unblocked by disown
            return flags == UnblockFlags::Disowned;
        });

    if (do_not_unblock)
        return false;

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
        SpinlockLocker lock(m_lock);
        // Disowning must unblock anyone waiting for this process explicitly
        if (!m_did_unblock)
            do_was_disowned();
        return true;
    }

    if (flags == UnblockFlags::Terminated) {
        VERIFY(process.is_dead());

        SpinlockLocker lock(m_lock);
        if (m_did_unblock)
            return false;
        // Up until this point, this function may have been called
        // more than once!
        do_set_result(process.wait_info());
    } else {
        siginfo_t siginfo {};
        {
            SpinlockLocker lock(g_scheduler_lock);
            auto credentials = process.credentials();
            // We need to gather the information before we release the scheduler lock!
            siginfo.si_signo = SIGCHLD;
            siginfo.si_pid = process.pid().value();
            siginfo.si_uid = credentials->uid().value();
            siginfo.si_status = signal;

            switch (flags) {
            case UnblockFlags::Terminated:
            case UnblockFlags::Disowned:
                VERIFY_NOT_REACHED();
            case UnblockFlags::Stopped:
                siginfo.si_code = CLD_STOPPED;
                break;
            case UnblockFlags::Continued:
                siginfo.si_code = CLD_CONTINUED;
                break;
            }
        }

        SpinlockLocker lock(m_lock);
        if (m_did_unblock)
            return false;
        // Up until this point, this function may have been called
        // more than once!
        do_set_result(siginfo);
    }

    if (!from_add_blocker) {
        // Only call unblock if we weren't called from within add_to_blocker_set!
        VERIFY(flags != UnblockFlags::Disowned);
        unblock_from_blocker();
    }
    // Because this may be called from add_blocker, in which case we should
    // not be actually trying to unblock the thread (because it hasn't actually
    // been blocked yet), we need to return true anyway
    return true;
}

Thread::FlockBlocker::FlockBlocker(NonnullRefPtr<Inode> inode, flock const& flock)
    : m_inode(move(inode))
    , m_flock(flock)
{
}

void Thread::FlockBlocker::will_unblock_immediately_without_blocking(UnblockImmediatelyReason reason)
{
    VERIFY(reason == UnblockImmediatelyReason::UnblockConditionAlreadyMet);
}

bool Thread::FlockBlocker::setup_blocker()
{
    return add_to_blocker_set(m_inode->flock_blocker_set());
}

bool Thread::FlockBlocker::try_unblock(bool from_add_blocker)
{
    if (!m_inode->can_apply_flock(m_flock))
        return false;

    {
        SpinlockLocker lock(m_lock);
        if (m_did_unblock)
            return false;
        m_did_unblock = true;
    }

    if (!from_add_blocker)
        unblock_from_blocker();
    return true;
}

}
