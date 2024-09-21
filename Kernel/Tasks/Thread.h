/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/EnumBits.h>
#include <AK/Error.h>
#include <AK/FixedStringBuffer.h>
#include <AK/IntrusiveList.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/Platform.h>
#include <AK/Time.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <Kernel/API/POSIX/sched.h>
#include <Kernel/API/POSIX/select.h>
#include <Kernel/API/POSIX/signal_numbers.h>
#include <Kernel/Arch/ArchSpecificThreadData.h>
#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Arch/ThreadRegisters.h>
#include <Kernel/Debug.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/KString.h>
#include <Kernel/Library/ListedRefCounted.h>
#include <Kernel/Library/LockWeakPtr.h>
#include <Kernel/Library/LockWeakable.h>
#include <Kernel/Locking/LockLocation.h>
#include <Kernel/Locking/LockMode.h>
#include <Kernel/Locking/LockRank.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Memory/VirtualRange.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class Timer;

enum class DispatchSignalResult {
    Deferred = 0,
    Yield,
    Terminate,
    Continue
};

struct ThreadSpecificData {
    ThreadSpecificData* self;
};

#define THREAD_AFFINITY_DEFAULT 0xffffffff

class Thread
    : public ListedRefCounted<Thread, LockType::Spinlock>
    , public LockWeakable<Thread> {
    AK_MAKE_NONCOPYABLE(Thread);
    AK_MAKE_NONMOVABLE(Thread);

    friend class Mutex;
    friend class Process;
    friend class Scheduler;
    friend struct ThreadReadyQueue;

public:
    static Thread* current()
    {
        return Processor::current_thread();
    }

    static ErrorOr<NonnullRefPtr<Thread>> create(NonnullRefPtr<Process>);
    ~Thread();

    static RefPtr<Thread> from_tid_ignoring_process_lists(ThreadID);
    static RefPtr<Thread> from_tid_in_same_process_list(ThreadID);
    static void finalize_dying_threads();

    ThreadID tid() const { return m_tid; }
    ProcessID pid() const;

    void set_priority(u32 p) { m_priority = p; }
    u32 priority() const { return m_priority; }

    void detach()
    {
        SpinlockLocker lock(m_lock);
        m_is_joinable = false;
    }

    [[nodiscard]] bool is_joinable() const
    {
        SpinlockLocker lock(m_lock);
        return m_is_joinable;
    }

    NO_SANITIZE_COVERAGE Process& process() { return m_process; }
    NO_SANITIZE_COVERAGE Process const& process() const { return m_process; }

    using Name = FixedStringBuffer<64>;
    SpinlockProtected<Name, LockRank::None> const& name() const
    {
        return m_name;
    }

    void set_name(StringView);

    void finalize();

    enum class State : u8 {
        Invalid = 0,
        Runnable,
        Running,
        Dying,
        Dead,
        Stopped,
        Blocked,
    };

    class [[nodiscard]] BlockResult {
    public:
        enum Type {
            WokeNormally,
            NotBlocked,
            InterruptedBySignal,
            InterruptedByDeath,
            InterruptedByTimeout,
        };

        BlockResult() = delete;

        BlockResult(Type type)
            : m_type(type)
        {
        }

        bool operator==(Type type) const
        {
            return m_type == type;
        }
        bool operator!=(Type type) const
        {
            return m_type != type;
        }

        [[nodiscard]] bool was_interrupted() const
        {
            switch (m_type) {
            case InterruptedBySignal:
            case InterruptedByDeath:
                return true;
            default:
                return false;
            }
        }

    private:
        Type m_type;
    };

    class BlockTimeout {
    public:
        BlockTimeout()
            : m_infinite(true)
        {
        }
        explicit BlockTimeout(bool is_absolute, Duration const* time, Duration const* start_time = nullptr, clockid_t clock_id = CLOCK_MONOTONIC_COARSE);

        Duration const& absolute_time() const { return m_time; }
        Duration const* start_time() const { return !m_infinite ? &m_start_time : nullptr; }
        clockid_t clock_id() const { return m_clock_id; }
        bool is_infinite() const { return m_infinite; }

    private:
        Duration m_time {};
        Duration m_start_time {};
        clockid_t m_clock_id { CLOCK_MONOTONIC_COARSE };
        bool m_infinite { false };
    };

    class BlockerSet;

    class Blocker {
        AK_MAKE_NONMOVABLE(Blocker);
        AK_MAKE_NONCOPYABLE(Blocker);

    public:
        enum class Type {
            Unknown = 0,
            File,
            Futex,
            Plan9FS,
            Join,
            Queue,
            Routing,
            Sleep,
            Signal,
            Wait,
            Flock
        };
        virtual ~Blocker();
        virtual StringView state_string() const = 0;
        virtual Type blocker_type() const = 0;
        virtual BlockTimeout const& override_timeout(BlockTimeout const& timeout) { return timeout; }
        virtual bool can_be_interrupted() const { return true; }
        virtual bool setup_blocker();
        virtual void finalize();

        Thread& thread() { return m_thread; }

        enum class UnblockImmediatelyReason {
            UnblockConditionAlreadyMet,
            TimeoutInThePast,
        };

        virtual void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) = 0;

        virtual void was_unblocked(bool did_timeout)
        {
            if (did_timeout) {
                SpinlockLocker lock(m_lock);
                m_did_timeout = true;
            }
        }
        void set_interrupted_by_death()
        {
            SpinlockLocker lock(m_lock);
            do_set_interrupted_by_death();
        }
        void set_interrupted_by_signal(u8 signal)
        {
            SpinlockLocker lock(m_lock);
            do_set_interrupted_by_signal(signal);
        }
        u8 was_interrupted_by_signal() const
        {
            SpinlockLocker lock(m_lock);
            return do_get_interrupted_by_signal();
        }
        virtual Thread::BlockResult block_result()
        {
            SpinlockLocker lock(m_lock);
            if (m_was_interrupted_by_death)
                return Thread::BlockResult::InterruptedByDeath;
            if (m_was_interrupted_by_signal != 0)
                return Thread::BlockResult::InterruptedBySignal;
            if (m_did_timeout)
                return Thread::BlockResult::InterruptedByTimeout;
            return Thread::BlockResult::WokeNormally;
        }

        void begin_blocking(Badge<Thread>);
        BlockResult end_blocking(Badge<Thread>, bool);

    protected:
        Blocker()
            : m_thread(*Thread::current())
        {
        }

        void do_set_interrupted_by_death()
        {
            m_was_interrupted_by_death = true;
        }
        void do_set_interrupted_by_signal(u8 signal)
        {
            VERIFY(signal != 0);
            m_was_interrupted_by_signal = signal;
        }
        void do_clear_interrupted_by_signal()
        {
            m_was_interrupted_by_signal = 0;
        }
        u8 do_get_interrupted_by_signal() const
        {
            return m_was_interrupted_by_signal;
        }
        [[nodiscard]] bool was_interrupted() const
        {
            return m_was_interrupted_by_death || m_was_interrupted_by_signal != 0;
        }
        void unblock_from_blocker()
        {
            {
                SpinlockLocker lock(m_lock);
                if (!m_is_blocking)
                    return;
                m_is_blocking = false;
            }

            m_thread->unblock_from_blocker(*this);
        }

        bool add_to_blocker_set(BlockerSet&, void* = nullptr);
        void set_blocker_set_raw_locked(BlockerSet* blocker_set) { m_blocker_set = blocker_set; }

        // FIXME: Figure out whether this can be Thread.
        mutable RecursiveSpinlock<LockRank::None> m_lock {};

    private:
        BlockerSet* m_blocker_set { nullptr };
        NonnullRefPtr<Thread> const m_thread;
        u8 m_was_interrupted_by_signal { 0 };
        bool m_is_blocking { false };
        bool m_was_interrupted_by_death { false };
        bool m_did_timeout { false };
    };

    class BlockerSet {
        AK_MAKE_NONCOPYABLE(BlockerSet);
        AK_MAKE_NONMOVABLE(BlockerSet);

    public:
        BlockerSet() = default;

        virtual ~BlockerSet()
        {
            VERIFY(!m_lock.is_locked());
            VERIFY(m_blockers.is_empty());
        }

        bool add_blocker(Blocker& blocker, void* data)
        {
            SpinlockLocker lock(m_lock);
            if (!should_add_blocker(blocker, data))
                return false;
            m_blockers.append({ &blocker, data });
            return true;
        }

        void remove_blocker(Blocker& blocker)
        {
            SpinlockLocker lock(m_lock);
            // NOTE: it's possible that the blocker is no longer present
            m_blockers.remove_all_matching([&](auto& info) {
                return info.blocker == &blocker;
            });
        }

        bool is_empty() const
        {
            SpinlockLocker lock(m_lock);
            return is_empty_locked();
        }

    protected:
        template<typename Callback>
        bool unblock_all_blockers_whose_conditions_are_met(Callback try_to_unblock_one)
        {
            SpinlockLocker lock(m_lock);
            return unblock_all_blockers_whose_conditions_are_met_locked(try_to_unblock_one);
        }

        template<typename Callback>
        bool unblock_all_blockers_whose_conditions_are_met_locked(Callback try_to_unblock_one)
        {
            VERIFY(m_lock.is_locked());
            bool stop_iterating = false;
            bool did_unblock_any = false;
            for (size_t i = 0; i < m_blockers.size() && !stop_iterating;) {
                auto& info = m_blockers[i];
                if (try_to_unblock_one(*info.blocker, info.data, stop_iterating)) {
                    m_blockers.remove(i);
                    did_unblock_any = true;
                    continue;
                }

                i++;
            }
            return did_unblock_any;
        }

        bool is_empty_locked() const
        {
            VERIFY(m_lock.is_locked());
            return m_blockers.is_empty();
        }

        virtual bool should_add_blocker(Blocker&, void*) { return true; }

        struct BlockerInfo {
            Blocker* blocker;
            void* data;
        };

        Vector<BlockerInfo, 4> do_take_blockers(size_t count)
        {
            if (m_blockers.size() <= count)
                return move(m_blockers);

            size_t move_count = (count <= m_blockers.size()) ? count : m_blockers.size();
            VERIFY(move_count > 0);

            Vector<BlockerInfo, 4> taken_blockers;
            taken_blockers.ensure_capacity(move_count);
            for (size_t i = 0; i < move_count; i++)
                taken_blockers.append(m_blockers.take(i));
            m_blockers.remove(0, move_count);
            return taken_blockers;
        }

        void do_append_blockers(Vector<BlockerInfo, 4>&& blockers_to_append)
        {
            if (blockers_to_append.is_empty())
                return;
            if (m_blockers.is_empty()) {
                m_blockers = move(blockers_to_append);
                return;
            }
            m_blockers.ensure_capacity(m_blockers.size() + blockers_to_append.size());
            for (size_t i = 0; i < blockers_to_append.size(); i++)
                m_blockers.append(blockers_to_append.take(i));
            blockers_to_append.clear();
        }

        // FIXME: Check whether this can be Thread.
        mutable Spinlock<LockRank::None> m_lock {};

    private:
        Vector<BlockerInfo, 4> m_blockers;
    };

    friend class JoinBlocker;
    class JoinBlocker final : public Blocker {
    public:
        explicit JoinBlocker(Thread& joinee, ErrorOr<void>& try_join_result, void*& joinee_exit_value);
        virtual Type blocker_type() const override { return Type::Join; }
        virtual StringView state_string() const override { return "Joining"sv; }
        virtual bool can_be_interrupted() const override { return false; }
        virtual void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) override;

        virtual bool setup_blocker() override;

        bool unblock(void*, bool);

    private:
        NonnullRefPtr<Thread> const m_joinee;
        void*& m_joinee_exit_value;
        ErrorOr<void>& m_try_join_result;
        bool m_did_unblock { false };
    };

    class WaitQueueBlocker final : public Blocker {
    public:
        explicit WaitQueueBlocker(WaitQueue&, StringView block_reason = {});
        virtual ~WaitQueueBlocker();

        virtual Type blocker_type() const override { return Type::Queue; }
        virtual StringView state_string() const override { return m_block_reason.is_null() ? m_block_reason : "Queue"sv; }
        virtual void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) override { }
        virtual bool setup_blocker() override;

        bool unblock();

    protected:
        WaitQueue& m_wait_queue;
        StringView m_block_reason;
        bool m_did_unblock { false };
    };

    class FutexBlocker final : public Blocker {
    public:
        explicit FutexBlocker(FutexQueue&, u32);
        virtual ~FutexBlocker();

        virtual Type blocker_type() const override { return Type::Futex; }
        virtual StringView state_string() const override { return "Futex"sv; }
        virtual void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) override { }
        virtual bool setup_blocker() override;

        u32 bitset() const { return m_bitset; }

        void begin_requeue()
        {
            // We need to hold the lock until we moved it over
            m_previous_interrupts_state = m_lock.lock();
        }
        void finish_requeue(FutexQueue&);

        bool unblock_bitset(u32 bitset);
        bool unblock(bool force = false);

    protected:
        FutexQueue& m_futex_queue;
        u32 m_bitset { 0 };
        InterruptsState m_previous_interrupts_state { InterruptsState::Disabled };
        bool m_did_unblock { false };
    };

    class FileBlocker : public Blocker {
    public:
        enum class BlockFlags : u16 {
            None = 0,

            Read = 1 << 0,
            Write = 1 << 1,
            ReadPriority = 1 << 2,
            WritePriority = 1 << 3,

            Accept = 1 << 4,
            Connect = 1 << 5,
            SocketFlags = Accept | Connect,

            WriteError = 1 << 6,
            WriteHangUp = 1 << 7,
            ReadHangUp = 1 << 8,
            Exception = WriteError | WriteHangUp | ReadHangUp,
        };

        virtual Type blocker_type() const override { return Type::File; }

        virtual bool unblock_if_conditions_are_met(bool, void*) = 0;
    };

    class OpenFileDescriptionBlocker : public FileBlocker {
    public:
        OpenFileDescription const& blocked_description() const;

        virtual bool unblock_if_conditions_are_met(bool, void*) override;
        virtual void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) override;
        virtual bool setup_blocker() override;

    protected:
        explicit OpenFileDescriptionBlocker(OpenFileDescription&, BlockFlags, BlockFlags&);

    private:
        NonnullRefPtr<OpenFileDescription> m_blocked_description;
        BlockFlags const m_flags;
        BlockFlags& m_unblocked_flags;
        bool m_did_unblock { false };
    };

    class AcceptBlocker final : public OpenFileDescriptionBlocker {
    public:
        explicit AcceptBlocker(OpenFileDescription&, BlockFlags&);
        virtual StringView state_string() const override { return "Accepting"sv; }
    };

    class ConnectBlocker final : public OpenFileDescriptionBlocker {
    public:
        explicit ConnectBlocker(OpenFileDescription&, BlockFlags&);
        virtual StringView state_string() const override { return "Connecting"sv; }
    };

    class WriteBlocker final : public OpenFileDescriptionBlocker {
    public:
        explicit WriteBlocker(OpenFileDescription&, BlockFlags&);
        virtual StringView state_string() const override { return "Writing"sv; }
        virtual BlockTimeout const& override_timeout(BlockTimeout const&) override;

    private:
        BlockTimeout m_timeout;
    };

    class ReadBlocker final : public OpenFileDescriptionBlocker {
    public:
        explicit ReadBlocker(OpenFileDescription&, BlockFlags&);
        virtual StringView state_string() const override { return "Reading"sv; }
        virtual BlockTimeout const& override_timeout(BlockTimeout const&) override;

    private:
        BlockTimeout m_timeout;
    };

    class SleepBlocker final : public Blocker {
    public:
        explicit SleepBlocker(BlockTimeout const&, Duration* = nullptr);
        virtual StringView state_string() const override { return "Sleeping"sv; }
        virtual Type blocker_type() const override { return Type::Sleep; }
        virtual BlockTimeout const& override_timeout(BlockTimeout const&) override;
        virtual void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) override;
        virtual void was_unblocked(bool) override;
        virtual Thread::BlockResult block_result() override;

    private:
        void calculate_remaining();

        BlockTimeout m_deadline;
        Duration* m_remaining;
    };

    class SelectBlocker final : public FileBlocker {
    public:
        struct FDInfo {
            RefPtr<OpenFileDescription> description;
            BlockFlags block_flags { BlockFlags::None };
            BlockFlags unblocked_flags { BlockFlags::None };
        };

        using FDVector = Vector<FDInfo, FD_SETSIZE>;
        explicit SelectBlocker(FDVector&);
        virtual ~SelectBlocker();

        virtual bool unblock_if_conditions_are_met(bool, void*) override;
        virtual void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) override;
        virtual void was_unblocked(bool) override;
        virtual StringView state_string() const override { return "Selecting"sv; }
        virtual bool setup_blocker() override;
        virtual void finalize() override;

    private:
        size_t collect_unblocked_flags();

        FDVector& m_fds;
        bool m_did_unblock { false };
    };

    class SignalBlocker final : public Blocker {
    public:
        explicit SignalBlocker(sigset_t pending_set, siginfo_t& result);
        virtual StringView state_string() const override { return "Pending Signal"sv; }
        virtual Type blocker_type() const override { return Type::Signal; }
        void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) override;
        virtual bool setup_blocker() override;
        bool check_pending_signals(bool from_add_blocker);

    private:
        sigset_t m_pending_set { 0 };
        siginfo_t& m_result;
        bool m_did_unblock { false };
    };

    class SignalBlockerSet final : public BlockerSet {
    public:
        void unblock_all_blockers_whose_conditions_are_met()
        {
            BlockerSet::unblock_all_blockers_whose_conditions_are_met([&](auto& b, void*, bool&) {
                VERIFY(b.blocker_type() == Blocker::Type::Signal);
                auto& blocker = static_cast<Thread::SignalBlocker&>(b);
                return blocker.check_pending_signals(false);
            });
        }

    private:
        bool should_add_blocker(Blocker& b, void*) override
        {
            VERIFY(b.blocker_type() == Blocker::Type::Signal);
            auto& blocker = static_cast<Thread::SignalBlocker&>(b);
            return !blocker.check_pending_signals(true);
        }
    };

    class WaitBlocker final : public Blocker {
    public:
        enum class UnblockFlags {
            Terminated,
            Stopped,
            Continued,
            Disowned
        };

        WaitBlocker(int wait_options, Variant<Empty, NonnullRefPtr<Process>, NonnullRefPtr<ProcessGroup>> waitee, ErrorOr<siginfo_t>& result);
        virtual StringView state_string() const override { return "Waiting"sv; }
        virtual Type blocker_type() const override { return Type::Wait; }
        virtual void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) override;
        virtual void was_unblocked(bool) override;
        virtual bool setup_blocker() override;

        bool unblock(Process& process, UnblockFlags flags, u8 signal, bool from_add_blocker);
        bool is_wait() const { return (m_wait_options & WNOWAIT) != WNOWAIT; }

    private:
        void do_was_disowned();
        void do_set_result(siginfo_t const&);

        int const m_wait_options;
        ErrorOr<siginfo_t>& m_result;
        Variant<Empty, NonnullRefPtr<Process>, NonnullRefPtr<ProcessGroup>> const m_waitee;
        bool m_did_unblock { false };
        bool m_got_sigchild { false };
    };

    class WaitBlockerSet final : public BlockerSet {
        friend class WaitBlocker;

    public:
        explicit WaitBlockerSet(Process& process)
            : m_process(process)
        {
        }

        void disowned_by_waiter(Process&);
        bool unblock(Process&, WaitBlocker::UnblockFlags, u8);
        void try_unblock(WaitBlocker&);
        void finalize();

    protected:
        virtual bool should_add_blocker(Blocker&, void*) override;

    private:
        struct ProcessBlockInfo {
            NonnullRefPtr<Process> const process;
            WaitBlocker::UnblockFlags flags;
            u8 signal;
            bool was_waited { false };

            explicit ProcessBlockInfo(NonnullRefPtr<Process>&&, WaitBlocker::UnblockFlags, u8);
            ~ProcessBlockInfo();
        };

        Process& m_process;
        Vector<ProcessBlockInfo, 2> m_processes;
        bool m_finalized { false };
    };

    class FlockBlocker final : public Blocker {
    public:
        FlockBlocker(NonnullRefPtr<Inode>, flock const&);
        virtual StringView state_string() const override { return "Locking File"sv; }
        virtual Type blocker_type() const override { return Type::Flock; }
        virtual void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) override;
        virtual bool setup_blocker() override;
        bool try_unblock(bool from_add_blocker);

    private:
        NonnullRefPtr<Inode> m_inode;
        flock const& m_flock;
        bool m_did_unblock { false };
    };

    class FlockBlockerSet final : public BlockerSet {
    public:
        void unblock_all_blockers_whose_conditions_are_met()
        {
            BlockerSet::unblock_all_blockers_whose_conditions_are_met([&](auto& b, void*, bool&) {
                VERIFY(b.blocker_type() == Blocker::Type::Flock);
                auto& blocker = static_cast<Thread::FlockBlocker&>(b);
                return blocker.try_unblock(false);
            });
        }

    private:
        bool should_add_blocker(Blocker& b, void*) override
        {
            VERIFY(b.blocker_type() == Blocker::Type::Flock);
            auto& blocker = static_cast<Thread::FlockBlocker&>(b);
            return !blocker.try_unblock(true);
        }
    };

    template<typename AddBlockerHandler>
    ErrorOr<void> try_join(AddBlockerHandler add_blocker)
    {
        if (Thread::current() == this)
            return EDEADLK;

        SpinlockLocker lock(m_lock);

        // Joining dead threads is allowed for two main reasons:
        // - Thread join behavior should not be racy when a thread is joined and exiting at roughly the same time.
        //   This is common behavior when threads are given a signal to end (meaning they are going to exit ASAP) and then joined.
        // - POSIX requires that exited threads are joinable (at least, there is no language in the specification forbidding it).
        if (!m_is_joinable || state() == Thread::State::Invalid)
            return EINVAL;

        add_blocker();

        // From this point on the thread is no longer joinable by anyone
        // else. It also means that if the join is timed, it becomes
        // detached when a timeout happens.
        m_is_joinable = false;
        return {};
    }

    void did_schedule() { ++m_times_scheduled; }
    u32 times_scheduled() const { return m_times_scheduled; }

    void resume_from_stopped();

    [[nodiscard]] bool should_be_stopped() const;
    [[nodiscard]] bool is_stopped() const { return m_state == Thread::State::Stopped; }
    [[nodiscard]] bool is_blocked() const { return m_state == Thread::State::Blocked; }

    u32 cpu() const { return m_cpu.load(AK::MemoryOrder::memory_order_consume); }
    void set_cpu(u32 cpu) { m_cpu.store(cpu, AK::MemoryOrder::memory_order_release); }
    u32 affinity() const { return m_cpu_affinity; }
    void set_affinity(u32 affinity) { m_cpu_affinity = affinity; }

    RegisterState& get_register_dump_from_stack();
    RegisterState const& get_register_dump_from_stack() const { return const_cast<Thread*>(this)->get_register_dump_from_stack(); }

    DebugRegisterState& debug_register_state() { return m_debug_register_state; }
    DebugRegisterState const& debug_register_state() const { return m_debug_register_state; }

    ThreadRegisters& regs() { return m_regs; }
    ThreadRegisters const& regs() const { return m_regs; }

    State state() const { return m_state; }
    StringView state_string() const;

    ArchSpecificThreadData& arch_specific_data() { return m_arch_specific_data; }
    ArchSpecificThreadData const& arch_specific_data() const { return m_arch_specific_data; }

    ALWAYS_INLINE void yield_if_should_be_stopped()
    {
        // A thread may continue to execute in user land until the next timer
        // tick or until entering the next system call/exiting the current one.
        if (!is_stopped() && should_be_stopped()) {
            SpinlockLocker scheduler_lock(g_scheduler_lock);
            set_state(State::Stopped);
        }
        // If we're stopped, we need to yield to someone else.
        SpinlockLocker lock(m_lock);
        while (is_stopped()) {
            lock.unlock();
            // We shouldn't be holding the big lock here
            yield_without_releasing_big_lock();
            lock.lock();
        }
    }

    void block(Kernel::Mutex&, SpinlockLocker<Spinlock<LockRank::None>>&, u32);

    template<typename BlockerType, class... Args>
    BlockResult block(BlockTimeout const& timeout, Args&&... args)
    {
        BlockerType blocker(forward<Args>(args)...);
        return block_impl(timeout, blocker);
    }

    u32 unblock_from_mutex(Kernel::Mutex&);
    void unblock_from_blocker(Blocker&);
    void unblock(u8 signal = 0);

    template<class... Args>
    Thread::BlockResult wait_on(WaitQueue& wait_queue, Thread::BlockTimeout const& timeout, Args&&... args)
    {
        VERIFY(this == Thread::current());
        return block<Thread::WaitQueueBlocker>(timeout, wait_queue, forward<Args>(args)...);
    }

    BlockResult sleep(clockid_t, Duration const&, Duration* = nullptr);
    BlockResult sleep(Duration const& duration, Duration* remaining_time = nullptr)
    {
        return sleep(CLOCK_MONOTONIC_COARSE, duration, remaining_time);
    }
    BlockResult sleep_until(clockid_t, Duration const&);
    BlockResult sleep_until(Duration const& duration)
    {
        return sleep_until(CLOCK_MONOTONIC_COARSE, duration);
    }

    // Tell this thread to unblock if needed,
    // gracefully unwind the stack and die.
    void set_should_die();
    [[nodiscard]] bool should_die() const { return m_should_die; }
    void die_if_needed();

    void exit(void* = nullptr);

    void update_time_scheduled(u64, bool, bool);
    bool tick();
    void set_ticks_left(u32 t) { m_ticks_left = t; }
    u32 ticks_left() const { return m_ticks_left; }

    FlatPtr kernel_stack_base() const { return m_kernel_stack_base; }
    FlatPtr kernel_stack_top() const { return m_kernel_stack_top; }

    void set_state(State, u8 = 0);

    [[nodiscard]] bool is_initialized() const { return m_initialized; }
    void set_initialized(bool initialized) { m_initialized = initialized; }

    void send_urgent_signal_to_self(u8 signal);
    void send_signal(u8 signal, Process* sender);

    u32 update_signal_mask(u32 signal_mask);
    u32 signal_mask_block(sigset_t signal_set, bool block);
    u32 signal_mask() const;
    void reset_signals_for_exec();

    ErrorOr<FlatPtr> peek_debug_register(u32 register_index);
    ErrorOr<void> poke_debug_register(u32 register_index, FlatPtr data);

    void set_dump_backtrace_on_finalization() { m_dump_backtrace_on_finalization = true; }

    DispatchSignalResult dispatch_one_pending_signal();
    DispatchSignalResult try_dispatch_one_pending_signal(u8 signal);
    DispatchSignalResult dispatch_signal(u8 signal);
    void check_dispatch_pending_signal();
    [[nodiscard]] bool has_unmasked_pending_signals() const { return m_have_any_unmasked_pending_signals.load(AK::memory_order_consume); }
    [[nodiscard]] bool should_ignore_signal(u8 signal) const;
    [[nodiscard]] bool has_signal_handler(u8 signal) const;
    [[nodiscard]] bool is_signal_masked(u8 signal) const;
    u32 pending_signals() const;
    u32 pending_signals_for_state() const;

    [[nodiscard]] bool is_in_alternative_signal_stack() const;

    FPUState& fpu_state() { return m_fpu_state; }

    unsigned syscall_count() const { return m_syscall_count; }
    void did_syscall() { ++m_syscall_count; }
    unsigned inode_faults() const { return m_inode_faults; }
    void did_inode_fault() { ++m_inode_faults; }
    unsigned zero_faults() const { return m_zero_faults; }
    void did_zero_fault() { ++m_zero_faults; }
    unsigned cow_faults() const { return m_cow_faults; }
    void did_cow_fault() { ++m_cow_faults; }

    u64 file_read_bytes() const { return m_file_read_bytes; }
    u64 file_write_bytes() const { return m_file_write_bytes; }

    void did_file_read(u64 bytes)
    {
        m_file_read_bytes += bytes;
    }

    void did_file_write(u64 bytes)
    {
        m_file_write_bytes += bytes;
    }

    u64 unix_socket_read_bytes() const { return m_unix_socket_read_bytes; }
    u64 unix_socket_write_bytes() const { return m_unix_socket_write_bytes; }

    void did_unix_socket_read(u64 bytes)
    {
        m_unix_socket_read_bytes += bytes;
    }

    void did_unix_socket_write(u64 bytes)
    {
        m_unix_socket_write_bytes += bytes;
    }

    u64 ipv4_socket_read_bytes() const { return m_ipv4_socket_read_bytes; }
    u64 ipv4_socket_write_bytes() const { return m_ipv4_socket_write_bytes; }

    void did_ipv4_socket_read(u64 bytes)
    {
        m_ipv4_socket_read_bytes += bytes;
    }

    void did_ipv4_socket_write(u64 bytes)
    {
        m_ipv4_socket_write_bytes += bytes;
    }

    void set_active(bool active) { m_is_active = active; }

    u32 saved_critical() const { return m_saved_critical; }
    void save_critical(u32 critical) { m_saved_critical = critical; }

    void track_lock_acquire(LockRank rank);
    void track_lock_release(LockRank rank);

    [[nodiscard]] bool is_active() const { return m_is_active; }

    [[nodiscard]] bool is_finalizable() const
    {
        // We can't finalize as long as this thread is still running
        // Note that checking for Running state here isn't sufficient
        // as the thread may not be in Running state but switching out.
        // m_is_active is set to false once the context switch is
        // complete and the thread is not executing on any processor.
        if (m_is_active.load(AK::memory_order_acquire))
            return false;
        // We can't finalize until the thread is either detached or
        // a join has started. We can't make m_is_joinable atomic
        // because that would introduce a race in try_join.
        SpinlockLocker lock(m_lock);
        return !m_is_joinable;
    }

    ErrorOr<NonnullRefPtr<Thread>> clone(NonnullRefPtr<Process>);

    template<IteratorFunction<Thread&> Callback>
    static IterationDecision for_each_in_state_ignoring_process_lists(State, Callback);
    template<IteratorFunction<Thread&> Callback>
    static IterationDecision for_each_ignoring_process_lists(Callback);

    template<VoidFunction<Thread&> Callback>
    static IterationDecision for_each_in_state_ignoring_process_lists(State, Callback);
    template<VoidFunction<Thread&> Callback>
    static IterationDecision for_each_ignoring_process_lists(Callback);

    static constexpr u32 default_kernel_stack_size = 65536;
    static constexpr u32 default_userspace_stack_size = 1 * MiB;

    u64 time_in_user() const { return m_total_time_scheduled_user.load(AK::MemoryOrder::memory_order_relaxed); }
    u64 time_in_kernel() const { return m_total_time_scheduled_kernel.load(AK::MemoryOrder::memory_order_relaxed); }

    ExecutionMode previous_mode() const { return m_previous_mode; }
    bool set_previous_mode(ExecutionMode mode)
    {
        if (m_previous_mode == mode)
            return false;
        m_previous_mode = mode;
        return true;
    }

    TrapFrame*& current_trap() { return m_current_trap; }
    TrapFrame const* const& current_trap() const { return m_current_trap; }

    RecursiveSpinlock<LockRank::Thread>& get_lock() const { return m_lock; }

#if LOCK_DEBUG
    void holding_lock(Mutex& lock, int refs_delta, LockLocation const& location)
    {
        VERIFY(refs_delta != 0);
        m_holding_locks.fetch_add(refs_delta, AK::MemoryOrder::memory_order_relaxed);
        SpinlockLocker list_lock(m_holding_locks_lock);
        if (refs_delta > 0) {
            bool have_existing = false;
            for (size_t i = 0; i < m_holding_locks_list.size(); i++) {
                auto& info = m_holding_locks_list[i];
                if (info.lock == &lock) {
                    have_existing = true;
                    info.count += refs_delta;
                    break;
                }
            }
            if (!have_existing)
                m_holding_locks_list.append({ &lock, location, 1 });
        } else {
            VERIFY(refs_delta < 0);
            bool found = false;
            for (size_t i = 0; i < m_holding_locks_list.size(); i++) {
                auto& info = m_holding_locks_list[i];
                if (info.lock == &lock) {
                    VERIFY(info.count >= (unsigned)-refs_delta);
                    info.count -= (unsigned)-refs_delta;
                    if (info.count == 0)
                        m_holding_locks_list.remove(i);
                    found = true;
                    break;
                }
            }
            VERIFY(found);
        }
    }
    u32 lock_count() const
    {
        return m_holding_locks.load(AK::MemoryOrder::memory_order_relaxed);
    }
#endif

    bool is_handling_page_fault() const
    {
        return m_handling_page_fault;
    }
    void set_handling_page_fault(bool b) { m_handling_page_fault = b; }
    void set_idle_thread() { m_is_idle_thread = true; }
    bool is_idle_thread() const { return m_is_idle_thread; }

    void set_crashing() { m_is_crashing = true; }
    [[nodiscard]] bool is_crashing() const { return m_is_crashing; }

    ALWAYS_INLINE u32 enter_profiler()
    {
        return m_nested_profiler_calls.fetch_add(1, AK::MemoryOrder::memory_order_acq_rel);
    }

    ALWAYS_INLINE u32 leave_profiler()
    {
        return m_nested_profiler_calls.fetch_sub(1, AK::MemoryOrder::memory_order_acquire);
    }

    bool is_profiling_suppressed() const { return m_is_profiling_suppressed; }
    void set_profiling_suppressed() { m_is_profiling_suppressed = true; }

    bool is_promise_violation_pending() const { return m_is_promise_violation_pending; }
    void set_promise_violation_pending(bool value) { m_is_promise_violation_pending = value; }

    bool is_allocation_enabled() const { return m_allocation_enabled; }
    void set_allocation_enabled(bool value) { m_allocation_enabled = value; }

    ErrorOr<NonnullOwnPtr<KString>> backtrace();
    void print_backtrace();

    Blocker const* blocker() const { return m_blocker; }
    Kernel::Mutex const* blocking_mutex() const { return m_blocking_mutex; }

#if LOCK_DEBUG
    struct HoldingLockInfo {
        Mutex* lock;
        LockLocation lock_location;
        unsigned count;
    };

    template<IteratorFunction<HoldingLockInfo const&> Callback>
    void for_each_held_lock(Callback);
    template<VoidFunction<HoldingLockInfo const&> Callback>
    void for_each_held_lock(Callback);
#endif

private:
    Thread(NonnullRefPtr<Process>, NonnullOwnPtr<Memory::Region>, NonnullRefPtr<Timer>);

    BlockResult block_impl(BlockTimeout const&, Blocker&);

    IntrusiveListNode<Thread> m_process_thread_list_node;
    int m_runnable_priority { -1 };

    friend class WaitQueue;

    class JoinBlockerSet final : public BlockerSet {
    public:
        void thread_did_exit(void* exit_value)
        {
            SpinlockLocker lock(m_lock);
            VERIFY(!m_thread_did_exit);
            m_thread_did_exit = true;
            m_exit_value.store(exit_value, AK::MemoryOrder::memory_order_release);
            do_unblock_joiner();
        }
        void thread_finalizing()
        {
            SpinlockLocker lock(m_lock);
            do_unblock_joiner();
        }
        void* exit_value() const
        {
            VERIFY(m_thread_did_exit);
            return m_exit_value.load(AK::MemoryOrder::memory_order_acquire);
        }

        void try_unblock(JoinBlocker& blocker)
        {
            SpinlockLocker lock(m_lock);
            if (m_thread_did_exit)
                blocker.unblock(exit_value(), false);
        }

    protected:
        virtual bool should_add_blocker(Blocker& b, void*) override
        {
            VERIFY(b.blocker_type() == Blocker::Type::Join);
            auto& blocker = static_cast<JoinBlocker&>(b);

            // NOTE: m_lock is held already!
            if (m_thread_did_exit) {
                blocker.unblock(exit_value(), true);
                return false;
            }
            return true;
        }

    private:
        void do_unblock_joiner()
        {
            unblock_all_blockers_whose_conditions_are_met_locked([&](Blocker& b, void*, bool&) {
                VERIFY(b.blocker_type() == Blocker::Type::Join);
                auto& blocker = static_cast<JoinBlocker&>(b);
                return blocker.unblock(exit_value(), false);
            });
        }

        Atomic<void*> m_exit_value { nullptr };
        bool m_thread_did_exit { false };
    };

    LockMode unlock_process_if_locked(u32&);
    void relock_process(LockMode, u32);
    void reset_fpu_state();

    mutable RecursiveSpinlock<LockRank::Thread> m_lock {};
    mutable RecursiveSpinlock<LockRank::None> m_block_lock {};
    NonnullRefPtr<Process> const m_process;
    ThreadID m_tid { -1 };
    ThreadRegisters m_regs {};
    DebugRegisterState m_debug_register_state {};
    TrapFrame* m_current_trap { nullptr };
    u32 m_saved_critical { 1 };
    IntrusiveListNode<Thread> m_ready_queue_node;
    Atomic<u32> m_cpu { 0 };
    u32 m_cpu_affinity { THREAD_AFFINITY_DEFAULT };
    Optional<u64> m_last_time_scheduled;
    Atomic<u64> m_total_time_scheduled_user { 0 };
    Atomic<u64> m_total_time_scheduled_kernel { 0 };
    u32 m_ticks_left { 0 };
    u32 m_times_scheduled { 0 };
    u32 m_ticks_in_user { 0 };
    u32 m_ticks_in_kernel { 0 };
    u32 m_pending_signals { 0 };
    u8 m_currently_handled_signal { 0 };
    u32 m_signal_mask { 0 };
    Optional<Memory::VirtualRange> m_alternative_signal_stack;
    SignalBlockerSet m_signal_blocker_set;
    FlatPtr m_kernel_stack_base { 0 };
    FlatPtr m_kernel_stack_top { 0 };
    NonnullOwnPtr<Memory::Region> m_kernel_stack_region;
    Array<Optional<u32>, NSIG> m_signal_action_masks;
    Array<ProcessID, NSIG> m_signal_senders;
    Blocker* m_blocker { nullptr };
    Kernel::Mutex* m_blocking_mutex { nullptr };
    u32 m_lock_requested_count { 0 };
    IntrusiveListNode<Thread> m_blocked_threads_list_node;
    LockRank m_lock_rank_mask {};
    bool m_allocation_enabled { true };
    ArchSpecificThreadData m_arch_specific_data;

    // FIXME: remove this after annihilating Process::m_big_lock
    IntrusiveListNode<Thread> m_big_lock_blocked_threads_list_node;

#if LOCK_DEBUG
    Atomic<u32> m_holding_locks { 0 };
    Spinlock<LockRank::None> m_holding_locks_lock {};
    Vector<HoldingLockInfo> m_holding_locks_list;
#endif

    JoinBlockerSet m_join_blocker_set;
    Atomic<bool, AK::MemoryOrder::memory_order_relaxed> m_is_active { false };
    bool m_is_joinable { true };
    bool m_handling_page_fault { false };
    ExecutionMode m_previous_mode { ExecutionMode::Kernel }; // We always start out in kernel mode

    unsigned m_syscall_count { 0 };
    unsigned m_inode_faults { 0 };
    unsigned m_zero_faults { 0 };
    unsigned m_cow_faults { 0 };

    u64 m_file_read_bytes { 0 };
    u64 m_file_write_bytes { 0 };

    u64 m_unix_socket_read_bytes { 0 };
    u64 m_unix_socket_write_bytes { 0 };

    u64 m_ipv4_socket_read_bytes { 0 };
    u64 m_ipv4_socket_write_bytes { 0 };

    FPUState m_fpu_state {};
    State m_state { Thread::State::Invalid };
    SpinlockProtected<Name, LockRank::None> m_name;
    u32 m_priority { THREAD_PRIORITY_NORMAL };

    State m_stop_state { Thread::State::Invalid };

    bool m_dump_backtrace_on_finalization { false };
    bool m_should_die { false };
    bool m_initialized { false };
    bool m_is_idle_thread { false };
    bool m_is_crashing { false };
    bool m_is_promise_violation_pending { false };
    Atomic<bool> m_have_any_unmasked_pending_signals { false };
    Atomic<u32> m_nested_profiler_calls { 0 };

    NonnullRefPtr<Timer> const m_block_timer;

    bool m_is_profiling_suppressed { false };

    void yield_and_release_relock_big_lock();

    enum class VerifyLockNotHeld {
        Yes,
        No
    };

    void yield_without_releasing_big_lock(VerifyLockNotHeld verify_lock_not_held = VerifyLockNotHeld::Yes);
    void drop_thread_count();

    mutable IntrusiveListNode<Thread> m_global_thread_list_node;

public:
    using ListInProcess = IntrusiveList<&Thread::m_process_thread_list_node>;
    using GlobalList = IntrusiveList<&Thread::m_global_thread_list_node>;

    static SpinlockProtected<GlobalList, LockRank::None>& all_instances();

#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    // Used by __sanitizer_cov_trace_pc to identify traced threads.
    bool m_kcov_enabled { false };
#    ifdef ENABLE_KERNEL_COVERAGE_COLLECTION_DEBUG
    // Used by __sanitizer_cov_trace_pc to detect an infinite recursion.
    bool m_kcov_recursion_hint { false };
#    endif
#endif
};

AK_ENUM_BITWISE_OPERATORS(Thread::FileBlocker::BlockFlags);

template<IteratorFunction<Thread&> Callback>
inline IterationDecision Thread::for_each_ignoring_process_lists(Callback callback)
{
    return Thread::all_instances().with([&](auto& list) -> IterationDecision {
        for (auto& thread : list) {
            IterationDecision decision = callback(thread);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    });
}

template<IteratorFunction<Thread&> Callback>
inline IterationDecision Thread::for_each_in_state_ignoring_process_lists(State state, Callback callback)
{
    return Thread::all_instances().with([&](auto& list) -> IterationDecision {
        for (auto& thread : list) {
            if (thread.state() != state)
                continue;
            IterationDecision decision = callback(thread);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    });
}

template<VoidFunction<Thread&> Callback>
inline IterationDecision Thread::for_each_ignoring_process_lists(Callback callback)
{
    return Thread::all_instances().with([&](auto& list) {
        for (auto& thread : list) {
            if (callback(thread) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
}

template<VoidFunction<Thread&> Callback>
inline IterationDecision Thread::for_each_in_state_ignoring_process_lists(State state, Callback callback)
{
    return for_each_in_state_ignoring_process_lists(state, [&](auto& thread) {
        callback(thread);
        return IterationDecision::Continue;
    });
}

#if LOCK_DEBUG
template<IteratorFunction<Thread::HoldingLockInfo const&> Callback>
inline void Thread::for_each_held_lock(Callback callback)
{
    SpinlockLocker list_lock(m_holding_locks_lock);

    for (auto const& lock_info : m_holding_locks_list) {
        if (callback(lock_info) == IterationDecision::Break)
            break;
    }
}

template<VoidFunction<Thread::HoldingLockInfo const&> Callback>
inline void Thread::for_each_held_lock(Callback callback)
{
    for_each_held_lock([&](auto const& lock_info) {
        callback(lock_info);
        return IterationDecision::Continue;
    });
}
#endif

}

template<>
struct AK::Formatter<Kernel::Thread> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder&, Kernel::Thread const&);
};
