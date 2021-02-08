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

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/IntrusiveList.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/Time.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/SafeMem.h>
#include <Kernel/Debug.h>
#include <Kernel/Forward.h>
#include <Kernel/KResult.h>
#include <Kernel/LockMode.h>
#include <Kernel/Scheduler.h>
#include <Kernel/ThreadTracer.h>
#include <Kernel/TimerQueue.h>
#include <Kernel/UnixTypes.h>
#include <LibC/fd_set.h>

namespace Kernel {

extern RecursiveSpinLock s_mm_lock;

enum class DispatchSignalResult {
    Deferred = 0,
    Yield,
    Terminate,
    Continue
};

struct SignalActionData {
    VirtualAddress handler_or_sigaction;
    u32 mask { 0 };
    int flags { 0 };
};

struct ThreadSpecificData {
    ThreadSpecificData* self;
};

#define THREAD_PRIORITY_MIN 1
#define THREAD_PRIORITY_LOW 10
#define THREAD_PRIORITY_NORMAL 30
#define THREAD_PRIORITY_HIGH 50
#define THREAD_PRIORITY_MAX 99

#define THREAD_AFFINITY_DEFAULT 0xffffffff

class Thread
    : public RefCounted<Thread>
    , public Weakable<Thread> {
    AK_MAKE_NONCOPYABLE(Thread);
    AK_MAKE_NONMOVABLE(Thread);

    friend class Process;
    friend class Scheduler;
    friend class ThreadReadyQueue;

    static SpinLock<u8> g_tid_map_lock;
    static HashMap<ThreadID, Thread*>* g_tid_map;

public:
    inline static Thread* current()
    {
        return Processor::current_thread();
    }

    static void initialize();

    static KResultOr<NonnullRefPtr<Thread>> try_create(NonnullRefPtr<Process>);
    ~Thread();

    static RefPtr<Thread> from_tid(ThreadID);
    static void finalize_dying_threads();

    ThreadID tid() const { return m_tid; }
    ProcessID pid() const;

    void set_priority(u32 p) { m_priority = p; }
    u32 priority() const { return m_priority; }

    void detach()
    {
        ScopedSpinLock lock(m_lock);
        m_is_joinable = false;
    }

    [[nodiscard]] bool is_joinable() const
    {
        ScopedSpinLock lock(m_lock);
        return m_is_joinable;
    }

    Process& process() { return m_process; }
    const Process& process() const { return m_process; }

    String name() const
    {
        // Because the name can be changed, we can't return a const
        // reference here. We must make a copy
        ScopedSpinLock lock(m_lock);
        return m_name;
    }
    void set_name(const StringView& s)
    {
        ScopedSpinLock lock(m_lock);
        m_name = s;
    }
    void set_name(String&& name)
    {
        ScopedSpinLock lock(m_lock);
        m_name = move(name);
    }

    void finalize();

    enum State : u8 {
        Invalid = 0,
        Runnable,
        Running,
        Dying,
        Dead,
        Stopped,
        Blocked
    };

    class BlockResult {
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

        [[nodiscard]] bool timed_out() const
        {
            return m_type == InterruptedByTimeout;
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
        explicit BlockTimeout(bool is_absolute, const timeval* time, const timespec* start_time = nullptr, clockid_t clock_id = CLOCK_MONOTONIC_COARSE)
            : m_clock_id(clock_id)
            , m_infinite(!time)
        {
            if (!m_infinite) {
                if (time->tv_sec > 0 || time->tv_usec > 0) {
                    timeval_to_timespec(*time, m_time);
                    m_should_block = true;
                }
                m_start_time = start_time ? *start_time : TimeManagement::the().current_time(clock_id).value();
                if (!is_absolute)
                    timespec_add(m_time, m_start_time, m_time);
            }
        }
        explicit BlockTimeout(bool is_absolute, const timespec* time, const timespec* start_time = nullptr, clockid_t clock_id = CLOCK_MONOTONIC_COARSE)
            : m_clock_id(clock_id)
            , m_infinite(!time)
        {
            if (!m_infinite) {
                if (time->tv_sec > 0 || time->tv_nsec > 0) {
                    m_time = *time;
                    m_should_block = true;
                }
                m_start_time = start_time ? *start_time : TimeManagement::the().current_time(clock_id).value();
                if (!is_absolute)
                    timespec_add(m_time, m_start_time, m_time);
            }
        }

        const timespec& absolute_time() const { return m_time; }
        const timespec* start_time() const { return !m_infinite ? &m_start_time : nullptr; }
        clockid_t clock_id() const { return m_clock_id; }
        bool is_infinite() const { return m_infinite; }
        bool should_block() const { return m_infinite || m_should_block; };

    private:
        timespec m_time { 0, 0 };
        timespec m_start_time { 0, 0 };
        clockid_t m_clock_id { CLOCK_MONOTONIC_COARSE };
        bool m_infinite { false };
        bool m_should_block { false };
    };

    class BlockCondition;

    class Blocker {
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
            Wait
        };
        virtual ~Blocker();
        virtual const char* state_string() const = 0;
        virtual bool should_block() { return true; }
        virtual Type blocker_type() const = 0;
        virtual const BlockTimeout& override_timeout(const BlockTimeout& timeout) { return timeout; }
        virtual bool can_be_interrupted() const { return true; }
        virtual void not_blocking(bool) = 0;
        virtual void was_unblocked(bool did_timeout)
        {
            if (did_timeout) {
                ScopedSpinLock lock(m_lock);
                m_did_timeout = true;
            }
        }
        void set_interrupted_by_death()
        {
            ScopedSpinLock lock(m_lock);
            do_set_interrupted_by_death();
        }
        void set_interrupted_by_signal(u8 signal)
        {
            ScopedSpinLock lock(m_lock);
            do_set_interrupted_by_signal(signal);
        }
        u8 was_interrupted_by_signal() const
        {
            ScopedSpinLock lock(m_lock);
            return do_get_interrupted_by_signal();
        }
        virtual Thread::BlockResult block_result()
        {
            ScopedSpinLock lock(m_lock);
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
        void do_set_interrupted_by_death()
        {
            m_was_interrupted_by_death = true;
        }
        void do_set_interrupted_by_signal(u8 signal)
        {
            ASSERT(signal != 0);
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
            RefPtr<Thread> thread;

            {
                ScopedSpinLock lock(m_lock);
                if (m_is_blocking) {
                    m_is_blocking = false;
                    ASSERT(m_blocked_thread);
                    thread = m_blocked_thread;
                }
            }

            if (thread)
                thread->unblock_from_blocker(*this);
        }

        bool set_block_condition(BlockCondition&, void* = nullptr);
        void set_block_condition_raw_locked(BlockCondition* block_condition)
        {
            m_block_condition = block_condition;
        }

        mutable RecursiveSpinLock m_lock;

    private:
        BlockCondition* m_block_condition { nullptr };
        void* m_block_data { nullptr };
        Thread* m_blocked_thread { nullptr };
        u8 m_was_interrupted_by_signal { 0 };
        bool m_is_blocking { false };
        bool m_was_interrupted_by_death { false };
        bool m_did_timeout { false };
    };

    class BlockCondition {
        AK_MAKE_NONCOPYABLE(BlockCondition);
        AK_MAKE_NONMOVABLE(BlockCondition);

    public:
        BlockCondition() = default;

        virtual ~BlockCondition()
        {
            ScopedSpinLock lock(m_lock);
            ASSERT(m_blockers.is_empty());
        }

        bool add_blocker(Blocker& blocker, void* data)
        {
            ScopedSpinLock lock(m_lock);
            if (!should_add_blocker(blocker, data))
                return false;
            m_blockers.append({ &blocker, data });
            return true;
        }

        void remove_blocker(Blocker& blocker, void* data)
        {
            ScopedSpinLock lock(m_lock);
            // NOTE: it's possible that the blocker is no longer present
            m_blockers.remove_first_matching([&](auto& info) {
                return info.blocker == &blocker && info.data == data;
            });
        }

        bool is_empty() const
        {
            ScopedSpinLock lock(m_lock);
            return is_empty_locked();
        }

    protected:
        template<typename UnblockOne>
        bool unblock(UnblockOne unblock_one)
        {
            ScopedSpinLock lock(m_lock);
            return do_unblock(unblock_one);
        }

        template<typename UnblockOne>
        bool do_unblock(UnblockOne unblock_one)
        {
            ASSERT(m_lock.is_locked());
            bool stop_iterating = false;
            bool did_unblock = false;
            for (size_t i = 0; i < m_blockers.size() && !stop_iterating;) {
                auto& info = m_blockers[i];
                if (unblock_one(*info.blocker, info.data, stop_iterating)) {
                    m_blockers.remove(i);
                    did_unblock = true;
                    continue;
                }

                i++;
            }
            return did_unblock;
        }

        bool is_empty_locked() const
        {
            ASSERT(m_lock.is_locked());
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
            ASSERT(move_count > 0);

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

        mutable SpinLock<u8> m_lock;

    private:
        Vector<BlockerInfo, 4> m_blockers;
    };

    friend class JoinBlocker;
    class JoinBlocker final : public Blocker {
    public:
        explicit JoinBlocker(Thread& joinee, KResult& try_join_result, void*& joinee_exit_value);
        virtual Type blocker_type() const override { return Type::Join; }
        virtual const char* state_string() const override { return "Joining"; }
        virtual bool can_be_interrupted() const override { return false; }
        virtual bool should_block() override { return !m_join_error && m_should_block; }
        virtual void not_blocking(bool) override;

        bool unblock(void*, bool);

    private:
        NonnullRefPtr<Thread> m_joinee;
        void*& m_joinee_exit_value;
        bool m_join_error { false };
        bool m_did_unblock { false };
        bool m_should_block { true };
    };

    class QueueBlocker : public Blocker {
    public:
        explicit QueueBlocker(WaitQueue&, const char* block_reason = nullptr);
        virtual ~QueueBlocker();

        virtual Type blocker_type() const override { return Type::Queue; }
        virtual const char* state_string() const override { return m_block_reason ? m_block_reason : "Queue"; }
        virtual void not_blocking(bool) override { }

        virtual bool should_block() override
        {
            return m_should_block;
        }

        bool unblock();

    protected:
        const char* const m_block_reason;
        bool m_should_block { true };
        bool m_did_unblock { false };
    };

    class FutexBlocker : public Blocker {
    public:
        explicit FutexBlocker(FutexQueue&, u32);
        virtual ~FutexBlocker();

        virtual Type blocker_type() const override { return Type::Futex; }
        virtual const char* state_string() const override { return "Futex"; }
        virtual void not_blocking(bool) override { }

        virtual bool should_block() override
        {
            return m_should_block;
        }

        u32 bitset() const { return m_bitset; }

        void begin_requeue()
        {
            // We need to hold the lock until we moved it over
            m_relock_flags = m_lock.lock();
        }
        void finish_requeue(FutexQueue&);

        bool unblock_bitset(u32 bitset);
        bool unblock(bool force = false);

    protected:
        u32 m_bitset;
        u32 m_relock_flags { 0 };
        bool m_should_block { true };
        bool m_did_unblock { false };
    };

    class FileBlocker : public Blocker {
    public:
        enum class BlockFlags : u32 {
            None = 0,

            Read = 1 << 0,
            Write = 1 << 1,
            ReadPriority = 1 << 2,

            Accept = 1 << 3,
            Connect = 1 << 4,
            SocketFlags = Accept | Connect,

            WriteNotOpen = 1 << 5,
            WriteError = 1 << 6,
            WriteHangUp = 1 << 7,
            ReadHangUp = 1 << 8,
            Exception = WriteNotOpen | WriteError | WriteHangUp | ReadHangUp,
        };

        virtual Type blocker_type() const override { return Type::File; }

        virtual bool should_block() override
        {
            return m_should_block;
        }

        virtual bool unblock(bool, void*) = 0;

    protected:
        bool m_should_block { true };
    };

    class FileDescriptionBlocker : public FileBlocker {
    public:
        const FileDescription& blocked_description() const;

        virtual bool unblock(bool, void*) override;
        virtual void not_blocking(bool) override;

    protected:
        explicit FileDescriptionBlocker(FileDescription&, BlockFlags, BlockFlags&);

    private:
        NonnullRefPtr<FileDescription> m_blocked_description;
        const BlockFlags m_flags;
        BlockFlags& m_unblocked_flags;
        bool m_did_unblock { false };
        bool m_should_block { true };
    };

    class AcceptBlocker final : public FileDescriptionBlocker {
    public:
        explicit AcceptBlocker(FileDescription&, BlockFlags&);
        virtual const char* state_string() const override { return "Accepting"; }
    };

    class ConnectBlocker final : public FileDescriptionBlocker {
    public:
        explicit ConnectBlocker(FileDescription&, BlockFlags&);
        virtual const char* state_string() const override { return "Connecting"; }
    };

    class WriteBlocker final : public FileDescriptionBlocker {
    public:
        explicit WriteBlocker(FileDescription&, BlockFlags&);
        virtual const char* state_string() const override { return "Writing"; }
        virtual const BlockTimeout& override_timeout(const BlockTimeout&) override;

    private:
        BlockTimeout m_timeout;
    };

    class ReadBlocker final : public FileDescriptionBlocker {
    public:
        explicit ReadBlocker(FileDescription&, BlockFlags&);
        virtual const char* state_string() const override { return "Reading"; }
        virtual const BlockTimeout& override_timeout(const BlockTimeout&) override;

    private:
        BlockTimeout m_timeout;
    };

    class SleepBlocker final : public Blocker {
    public:
        explicit SleepBlocker(const BlockTimeout&, timespec* = nullptr);
        virtual const char* state_string() const override { return "Sleeping"; }
        virtual Type blocker_type() const override { return Type::Sleep; }
        virtual const BlockTimeout& override_timeout(const BlockTimeout&) override;
        virtual void not_blocking(bool) override;
        virtual void was_unblocked(bool) override;
        virtual Thread::BlockResult block_result() override;

    private:
        void calculate_remaining();

        BlockTimeout m_deadline;
        timespec* m_remaining;
    };

    class SelectBlocker final : public FileBlocker {
    public:
        struct FDInfo {
            NonnullRefPtr<FileDescription> description;
            BlockFlags block_flags;
            BlockFlags unblocked_flags { BlockFlags::None };
        };

        typedef Vector<FDInfo, FD_SETSIZE> FDVector;
        SelectBlocker(FDVector& fds);
        virtual ~SelectBlocker();

        virtual bool unblock(bool, void*) override;
        virtual void not_blocking(bool) override;
        virtual void was_unblocked(bool) override;
        virtual const char* state_string() const override { return "Selecting"; }

    private:
        size_t collect_unblocked_flags();

        FDVector& m_fds;
        bool m_did_unblock { false };
    };

    class WaitBlocker final : public Blocker {
    public:
        enum class UnblockFlags {
            Terminated,
            Stopped,
            Continued,
            Disowned
        };

        WaitBlocker(int wait_options, idtype_t id_type, pid_t id, KResultOr<siginfo_t>& result);
        virtual const char* state_string() const override { return "Waiting"; }
        virtual Type blocker_type() const override { return Type::Wait; }
        virtual bool should_block() override { return m_should_block; }
        virtual void not_blocking(bool) override;
        virtual void was_unblocked(bool) override;

        bool unblock(Process& process, UnblockFlags flags, u8 signal, bool from_add_blocker);
        bool is_wait() const { return !(m_wait_options & WNOWAIT); }

    private:
        void do_was_disowned();
        void do_set_result(const siginfo_t&);

        const int m_wait_options;
        const idtype_t m_id_type;
        const pid_t m_waitee_id;
        KResultOr<siginfo_t>& m_result;
        RefPtr<Process> m_waitee;
        RefPtr<ProcessGroup> m_waitee_group;
        bool m_did_unblock { false };
        bool m_error { false };
        bool m_got_sigchild { false };
        bool m_should_block;
    };

    class WaitBlockCondition final : public BlockCondition {
        friend class WaitBlocker;

    public:
        WaitBlockCondition(Process& process)
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
            NonnullRefPtr<Process> process;
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

    template<typename AddBlockerHandler>
    KResult try_join(AddBlockerHandler add_blocker)
    {
        if (Thread::current() == this)
            return EDEADLK;

        ScopedSpinLock lock(m_lock);
        if (!m_is_joinable || state() == Dead)
            return EINVAL;

        add_blocker();

        // From this point on the thread is no longer joinable by anyone
        // else. It also means that if the join is timed, it becomes
        // detached when a timeout happens.
        m_is_joinable = false;
        return KSuccess;
    }

    void did_schedule() { ++m_times_scheduled; }
    u32 times_scheduled() const { return m_times_scheduled; }

    void resume_from_stopped();

    [[nodiscard]] bool should_be_stopped() const;
    [[nodiscard]] bool is_stopped() const { return m_state == Stopped; }
    [[nodiscard]] bool is_blocked() const { return m_state == Blocked; }
    [[nodiscard]] bool is_in_block() const
    {
        ScopedSpinLock lock(m_block_lock);
        return m_in_block;
    }

    u32 cpu() const { return m_cpu.load(AK::MemoryOrder::memory_order_consume); }
    void set_cpu(u32 cpu) { m_cpu.store(cpu, AK::MemoryOrder::memory_order_release); }
    u32 affinity() const { return m_cpu_affinity; }
    void set_affinity(u32 affinity) { m_cpu_affinity = affinity; }

    RegisterState& get_register_dump_from_stack();
    const RegisterState& get_register_dump_from_stack() const { return const_cast<Thread*>(this)->get_register_dump_from_stack(); }

    TSS32& tss() { return m_tss; }
    const TSS32& tss() const { return m_tss; }
    State state() const { return m_state; }
    const char* state_string() const;

    VirtualAddress thread_specific_data() const { return m_thread_specific_data; }
    size_t thread_specific_region_size() const;
    size_t thread_specific_region_alignment() const;

    ALWAYS_INLINE void yield_if_stopped()
    {
        // If some thread stopped us, we need to yield to someone else
        // We check this when entering/exiting a system call. A thread
        // may continue to execute in user land until the next timer
        // tick or entering the next system call, or if it's in kernel
        // mode then we will intercept prior to returning back to user
        // mode.
        ScopedSpinLock lock(m_lock);
        while (state() == Thread::Stopped) {
            lock.unlock();
            // We shouldn't be holding the big lock here
            yield_while_not_holding_big_lock();
            lock.lock();
        }
    }

    template<typename T, class... Args>
    [[nodiscard]] BlockResult block(const BlockTimeout& timeout, Args&&... args)
    {
        ASSERT(!Processor::current().in_irq());
        ASSERT(this == Thread::current());
        ScopedCritical critical;
        ASSERT(!s_mm_lock.own_lock());

        ScopedSpinLock block_lock(m_block_lock);
        // We need to hold m_block_lock so that nobody can unblock a blocker as soon
        // as it is constructed and registered elsewhere
        m_in_block = true;
        T t(forward<Args>(args)...);

        ScopedSpinLock scheduler_lock(g_scheduler_lock);
        // Relaxed semantics are fine for timeout_unblocked because we
        // synchronize on the spin locks already.
        Atomic<bool, AK::MemoryOrder::memory_order_relaxed> timeout_unblocked(false);
        RefPtr<Timer> timer;
        {
            switch (state()) {
            case Thread::Stopped:
                // It's possible that we were requested to be stopped!
                break;
            case Thread::Running:
                ASSERT(m_blocker == nullptr);
                break;
            default:
                ASSERT_NOT_REACHED();
            }

            m_blocker = &t;
            if (!t.should_block()) {
                // Don't block if the wake condition is already met
                t.not_blocking(false);
                m_blocker = nullptr;
                m_in_block = false;
                return BlockResult::NotBlocked;
            }

            auto& block_timeout = t.override_timeout(timeout);
            if (!block_timeout.is_infinite()) {
                // Process::kill_all_threads may be called at any time, which will mark all
                // threads to die. In that case
                timer = TimerQueue::the().add_timer_without_id(block_timeout.clock_id(), block_timeout.absolute_time(), [&]() {
                    ASSERT(!Processor::current().in_irq());
                    ASSERT(!g_scheduler_lock.own_lock());
                    ASSERT(!m_block_lock.own_lock());
                    // NOTE: this may execute on the same or any other processor!
                    ScopedSpinLock scheduler_lock(g_scheduler_lock);
                    ScopedSpinLock block_lock(m_block_lock);
                    if (m_blocker && timeout_unblocked.exchange(true) == false)
                        unblock();
                });
                if (!timer) {
                    // Timeout is already in the past
                    t.not_blocking(true);
                    m_blocker = nullptr;
                    m_in_block = false;
                    return BlockResult::InterruptedByTimeout;
                }
            }

            t.begin_blocking({});

            set_state(Thread::Blocked);
        }

        scheduler_lock.unlock();
        block_lock.unlock();

        dbgln_if(THREAD_DEBUG, "Thread {} blocking on {} ({}) -->", *this, &t, t.state_string());
        bool did_timeout = false;
        u32 lock_count_to_restore = 0;
        auto previous_locked = unlock_process_if_locked(lock_count_to_restore);
        for (;;) {
            // Yield to the scheduler, and wait for us to resume unblocked.
            ASSERT(!g_scheduler_lock.own_lock());
            ASSERT(Processor::current().in_critical());
            yield_while_not_holding_big_lock();
            ASSERT(Processor::current().in_critical());

            ScopedSpinLock block_lock2(m_block_lock);
            if (should_be_stopped() || state() == Stopped) {
                dbgln("Thread should be stopped, current state: {}", state_string());
                set_state(Thread::Blocked);
                continue;
            }
            if (m_blocker && !m_blocker->can_be_interrupted() && !m_should_die) {
                block_lock2.unlock();
                dbgln("Thread should not be unblocking, current state: {}", state_string());
                set_state(Thread::Blocked);
                continue;
            }
            // Prevent the timeout from unblocking this thread if it happens to
            // be in the process of firing already
            did_timeout |= timeout_unblocked.exchange(true);
            if (m_blocker) {
                // Remove ourselves...
                ASSERT(m_blocker == &t);
                m_blocker = nullptr;
            }
            dbgln_if(THREAD_DEBUG, "<-- Thread {} unblocked from {} ({})", *this, &t, t.state_string());
            m_in_block = false;
            break;
        }

        if (t.was_interrupted_by_signal()) {
            ScopedSpinLock scheduler_lock(g_scheduler_lock);
            ScopedSpinLock lock(m_lock);
            dispatch_one_pending_signal();
        }

        // Notify the blocker that we are no longer blocking. It may need
        // to clean up now while we're still holding m_lock
        auto result = t.end_blocking({}, did_timeout); // calls was_unblocked internally

        if (timer && !did_timeout) {
            // Cancel the timer while not holding any locks. This allows
            // the timer function to complete before we remove it
            // (e.g. if it's on another processor)
            TimerQueue::the().cancel_timer(timer.release_nonnull());
        }
        if (previous_locked != LockMode::Unlocked) {
            // NOTE: this may trigger another call to Thread::block(), so
            // we need to do this after we're all done and restored m_in_block!
            relock_process(previous_locked, lock_count_to_restore);
        }
        return result;
    }

    void unblock_from_blocker(Blocker&);
    void unblock(u8 signal = 0);

    template<class... Args>
    Thread::BlockResult wait_on(WaitQueue& wait_queue, const Thread::BlockTimeout& timeout, Args&&... args)
    {
        ASSERT(this == Thread::current());
        return block<Thread::QueueBlocker>(timeout, wait_queue, forward<Args>(args)...);
    }

    BlockResult sleep(clockid_t, const timespec&, timespec* = nullptr);
    BlockResult sleep(const timespec& duration, timespec* remaining_time = nullptr)
    {
        return sleep(CLOCK_MONOTONIC_COARSE, duration, remaining_time);
    }
    BlockResult sleep_until(clockid_t, const timespec&);
    BlockResult sleep_until(const timespec& duration)
    {
        return sleep_until(CLOCK_MONOTONIC_COARSE, duration);
    }

    // Tell this thread to unblock if needed,
    // gracefully unwind the stack and die.
    void set_should_die();
    [[nodiscard]] bool should_die() const { return m_should_die; }
    void die_if_needed();

    void exit(void* = nullptr);

    bool tick();
    void set_ticks_left(u32 t) { m_ticks_left = t; }
    u32 ticks_left() const { return m_ticks_left; }

    u32 kernel_stack_base() const { return m_kernel_stack_base; }
    u32 kernel_stack_top() const { return m_kernel_stack_top; }

    void set_state(State, u8 = 0);

    [[nodiscard]] bool is_initialized() const { return m_initialized; }
    void set_initialized(bool initialized) { m_initialized = initialized; }

    void send_urgent_signal_to_self(u8 signal);
    void send_signal(u8 signal, Process* sender);

    u32 update_signal_mask(u32 signal_mask);
    u32 signal_mask_block(sigset_t signal_set, bool block);
    u32 signal_mask() const;
    void clear_signals();

    void set_dump_backtrace_on_finalization() { m_dump_backtrace_on_finalization = true; }

    DispatchSignalResult dispatch_one_pending_signal();
    DispatchSignalResult try_dispatch_one_pending_signal(u8 signal);
    DispatchSignalResult dispatch_signal(u8 signal);
    void check_dispatch_pending_signal();
    [[nodiscard]] bool has_unmasked_pending_signals() const { return m_have_any_unmasked_pending_signals.load(AK::memory_order_consume); }
    [[nodiscard]] bool should_ignore_signal(u8 signal) const;
    [[nodiscard]] bool has_signal_handler(u8 signal) const;
    u32 pending_signals() const;
    u32 pending_signals_for_state() const;

    FPUState& fpu_state() { return *m_fpu_state; }

    void set_default_signal_dispositions();

    KResult make_thread_specific_region(Badge<Process>);

    unsigned syscall_count() const { return m_syscall_count; }
    void did_syscall() { ++m_syscall_count; }
    unsigned inode_faults() const { return m_inode_faults; }
    void did_inode_fault() { ++m_inode_faults; }
    unsigned zero_faults() const { return m_zero_faults; }
    void did_zero_fault() { ++m_zero_faults; }
    unsigned cow_faults() const { return m_cow_faults; }
    void did_cow_fault() { ++m_cow_faults; }

    unsigned file_read_bytes() const { return m_file_read_bytes; }
    unsigned file_write_bytes() const { return m_file_write_bytes; }

    void did_file_read(unsigned bytes)
    {
        m_file_read_bytes += bytes;
    }

    void did_file_write(unsigned bytes)
    {
        m_file_write_bytes += bytes;
    }

    unsigned unix_socket_read_bytes() const { return m_unix_socket_read_bytes; }
    unsigned unix_socket_write_bytes() const { return m_unix_socket_write_bytes; }

    void did_unix_socket_read(unsigned bytes)
    {
        m_unix_socket_read_bytes += bytes;
    }

    void did_unix_socket_write(unsigned bytes)
    {
        m_unix_socket_write_bytes += bytes;
    }

    unsigned ipv4_socket_read_bytes() const { return m_ipv4_socket_read_bytes; }
    unsigned ipv4_socket_write_bytes() const { return m_ipv4_socket_write_bytes; }

    void did_ipv4_socket_read(unsigned bytes)
    {
        m_ipv4_socket_read_bytes += bytes;
    }

    void did_ipv4_socket_write(unsigned bytes)
    {
        m_ipv4_socket_write_bytes += bytes;
    }

    void set_active(bool active) { m_is_active = active; }

    u32 saved_critical() const { return m_saved_critical; }
    void save_critical(u32 critical) { m_saved_critical = critical; }

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
        ScopedSpinLock lock(m_lock);
        return !m_is_joinable;
    }

    RefPtr<Thread> clone(Process&);

    template<typename Callback>
    static IterationDecision for_each_in_state(State, Callback);
    template<typename Callback>
    static IterationDecision for_each(Callback);

    static constexpr u32 default_kernel_stack_size = 65536;
    static constexpr u32 default_userspace_stack_size = 4 * MiB;

    u32 ticks_in_user() const { return m_ticks_in_user; }
    u32 ticks_in_kernel() const { return m_ticks_in_kernel; }

    enum class PreviousMode : u8 {
        KernelMode = 0,
        UserMode
    };
    PreviousMode previous_mode() const { return m_previous_mode; }
    void set_previous_mode(PreviousMode mode) { m_previous_mode = mode; }
    TrapFrame*& current_trap() { return m_current_trap; }

    RecursiveSpinLock& get_lock() const { return m_lock; }

#if LOCK_DEBUG
    void holding_lock(Lock& lock, int refs_delta, const char* file = nullptr, int line = 0)
    {
        ASSERT(refs_delta != 0);
        m_holding_locks.fetch_add(refs_delta, AK::MemoryOrder::memory_order_relaxed);
        ScopedSpinLock list_lock(m_holding_locks_lock);
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
                m_holding_locks_list.append({ &lock, file ? file : "unknown", line, 1 });
        } else {
            ASSERT(refs_delta < 0);
            bool found = false;
            for (size_t i = 0; i < m_holding_locks_list.size(); i++) {
                auto& info = m_holding_locks_list[i];
                if (info.lock == &lock) {
                    ASSERT(info.count >= (unsigned)-refs_delta);
                    info.count -= (unsigned)-refs_delta;
                    if (info.count == 0)
                        m_holding_locks_list.remove(i);
                    found = true;
                    break;
                }
            }
            ASSERT(found);
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

private:
    Thread(NonnullRefPtr<Process>, NonnullOwnPtr<Region> kernel_stack_region);

    IntrusiveListNode m_process_thread_list_node;
    int m_runnable_priority { -1 };

    friend class WaitQueue;

    class JoinBlockCondition : public BlockCondition {
    public:
        void thread_did_exit(void* exit_value)
        {
            ScopedSpinLock lock(m_lock);
            ASSERT(!m_thread_did_exit);
            m_thread_did_exit = true;
            m_exit_value.store(exit_value, AK::MemoryOrder::memory_order_release);
            do_unblock_joiner();
        }
        void thread_finalizing()
        {
            ScopedSpinLock lock(m_lock);
            do_unblock_joiner();
        }
        void* exit_value() const
        {
            ASSERT(m_thread_did_exit);
            return m_exit_value.load(AK::MemoryOrder::memory_order_acquire);
        }

        void try_unblock(JoinBlocker& blocker)
        {
            ScopedSpinLock lock(m_lock);
            if (m_thread_did_exit)
                blocker.unblock(exit_value(), false);
        }

    protected:
        virtual bool should_add_blocker(Blocker& b, void*) override
        {
            ASSERT(b.blocker_type() == Blocker::Type::Join);
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
            do_unblock([&](Blocker& b, void*, bool&) {
                ASSERT(b.blocker_type() == Blocker::Type::Join);
                auto& blocker = static_cast<JoinBlocker&>(b);
                return blocker.unblock(exit_value(), false);
            });
        }

        Atomic<void*> m_exit_value { nullptr };
        bool m_thread_did_exit { false };
    };

    LockMode unlock_process_if_locked(u32&);
    void relock_process(LockMode, u32);
    String backtrace();
    void reset_fpu_state();

    mutable RecursiveSpinLock m_lock;
    mutable RecursiveSpinLock m_block_lock;
    NonnullRefPtr<Process> m_process;
    ThreadID m_tid { -1 };
    TSS32 m_tss {};
    TrapFrame* m_current_trap { nullptr };
    u32 m_saved_critical { 1 };
    IntrusiveListNode m_ready_queue_node;
    Atomic<u32> m_cpu { 0 };
    u32 m_cpu_affinity { THREAD_AFFINITY_DEFAULT };
    u32 m_ticks_left { 0 };
    u32 m_times_scheduled { 0 };
    u32 m_ticks_in_user { 0 };
    u32 m_ticks_in_kernel { 0 };
    u32 m_pending_signals { 0 };
    u32 m_signal_mask { 0 };
    u32 m_kernel_stack_base { 0 };
    u32 m_kernel_stack_top { 0 };
    OwnPtr<Region> m_kernel_stack_region;
    VirtualAddress m_thread_specific_data;
    SignalActionData m_signal_action_data[32];
    Blocker* m_blocker { nullptr };

#if LOCK_DEBUG
    struct HoldingLockInfo {
        Lock* lock;
        const char* file;
        int line;
        unsigned count;
    };
    Atomic<u32> m_holding_locks { 0 };
    SpinLock<u8> m_holding_locks_lock;
    Vector<HoldingLockInfo> m_holding_locks_list;
#endif

    JoinBlockCondition m_join_condition;
    Atomic<bool, AK::MemoryOrder::memory_order_relaxed> m_is_active { false };
    bool m_is_joinable { true };
    bool m_handling_page_fault { false };
    PreviousMode m_previous_mode { PreviousMode::UserMode };

    unsigned m_syscall_count { 0 };
    unsigned m_inode_faults { 0 };
    unsigned m_zero_faults { 0 };
    unsigned m_cow_faults { 0 };

    unsigned m_file_read_bytes { 0 };
    unsigned m_file_write_bytes { 0 };

    unsigned m_unix_socket_read_bytes { 0 };
    unsigned m_unix_socket_write_bytes { 0 };

    unsigned m_ipv4_socket_read_bytes { 0 };
    unsigned m_ipv4_socket_write_bytes { 0 };

    FPUState* m_fpu_state { nullptr };
    State m_state { Invalid };
    String m_name;
    u32 m_priority { THREAD_PRIORITY_NORMAL };

    State m_stop_state { Invalid };

    bool m_dump_backtrace_on_finalization { false };
    bool m_should_die { false };
    bool m_initialized { false };
    bool m_in_block { false };
    Atomic<bool> m_have_any_unmasked_pending_signals { false };

    void yield_without_holding_big_lock();
    void donate_without_holding_big_lock(RefPtr<Thread>&, const char*);
    void yield_while_not_holding_big_lock();
    void drop_thread_count(bool);
};

template<typename Callback>
inline IterationDecision Thread::for_each(Callback callback)
{
    ScopedSpinLock lock(g_tid_map_lock);
    for (auto& it : *g_tid_map) {
        IterationDecision decision = callback(*it.value);
        if (decision != IterationDecision::Continue)
            return decision;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
inline IterationDecision Thread::for_each_in_state(State state, Callback callback)
{
    ScopedSpinLock lock(g_tid_map_lock);
    for (auto& it : *g_tid_map) {
        auto& thread = *it.value;
        if (thread.state() != state)
            continue;
        IterationDecision decision = callback(thread);
        if (decision != IterationDecision::Continue)
            return decision;
    }
    return IterationDecision::Continue;
}

const LogStream& operator<<(const LogStream&, const Thread&);

}

template<>
struct AK::Formatter<Kernel::Thread> : AK::Formatter<FormatString> {
    void format(FormatBuilder&, const Kernel::Thread&);
};
