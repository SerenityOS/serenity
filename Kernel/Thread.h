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
#include <AK/IntrusiveList.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/Time.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Forward.h>
#include <Kernel/KResult.h>
#include <Kernel/Scheduler.h>
#include <Kernel/ThreadTracer.h>
#include <Kernel/TimerQueue.h>
#include <Kernel/UnixTypes.h>
#include <LibC/fd_set.h>
#include <LibELF/AuxiliaryVector.h>

namespace Kernel {

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

public:
    inline static Thread* current()
    {
        return Processor::current().current_thread();
    }

    explicit Thread(NonnullRefPtr<Process>);
    ~Thread();

    static RefPtr<Thread> from_tid(ThreadID);
    static void finalize_dying_threads();

    ThreadID tid() const { return m_tid; }
    ProcessID pid() const;

    void set_priority(u32 p) { m_priority = p; }
    u32 priority() const { return m_priority; }

    void set_priority_boost(u32 boost) { m_priority_boost = boost; }
    u32 priority_boost() const { return m_priority_boost; }

    u32 effective_priority() const;

    void detach()
    {
        ScopedSpinLock lock(m_lock);
        m_is_joinable = false;
    }

    bool is_joinable() const
    {
        ScopedSpinLock lock(m_lock);
        return m_is_joinable;
    }

    Process& process() { return m_process; }
    const Process& process() const { return m_process; }

    String backtrace();
    Vector<FlatPtr> raw_backtrace(FlatPtr ebp, FlatPtr eip) const;

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
        Blocked,
        Queued,
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

        bool was_interrupted() const
        {
            switch (m_type) {
            case InterruptedBySignal:
            case InterruptedByDeath:
                return true;
            default:
                return false;
            }
        }

        bool timed_out() const
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
        BlockTimeout(std::nullptr_t)
            : m_infinite(true)
        {
        }
        explicit BlockTimeout(bool is_absolute, const timeval* time, const timespec* start_time = nullptr)
            : m_infinite(!time)
        {
            if (!m_infinite) {
                if (time->tv_sec > 0 || time->tv_usec > 0) {
                    timeval_to_timespec(*time, m_time);
                    m_should_block = true;
                }
                m_start_time = start_time ? *start_time : TimeManagement::the().monotonic_time();
                if (!is_absolute)
                    timespec_add(m_time, m_start_time, m_time);
            }
        }
        explicit BlockTimeout(bool is_absolute, const timespec* time, const timespec* start_time = nullptr)
            : m_infinite(!time)
        {
            if (!m_infinite) {
                if (time->tv_sec > 0 || time->tv_nsec > 0) {
                    m_time = *time;
                    m_should_block = true;
                }
                m_start_time = start_time ? *start_time : TimeManagement::the().monotonic_time();
                if (!is_absolute)
                    timespec_add(m_time, m_start_time, m_time);
            }
        }

        const timespec& absolute_time() const { return m_time; }
        const timespec* start_time() const { return !m_infinite ? &m_start_time : nullptr; }
        bool is_infinite() const { return m_infinite; }
        bool should_block() const { return m_infinite || m_should_block; };

    private:
        timespec m_time { 0, 0 };
        timespec m_start_time { 0, 0 };
        bool m_infinite { false };
        bool m_should_block { false };
    };

    class BlockCondition;

    class Blocker {
    public:
        enum class Type {
            Unknown = 0,
            File,
            Plan9FS,
            Join,
            Routing,
            Sleep,
            Wait
        };
        virtual ~Blocker();
        virtual const char* state_string() const = 0;
        virtual bool should_block() { return true; }
        virtual Type blocker_type() const = 0;
        virtual const BlockTimeout& override_timeout(const BlockTimeout& timeout) { return timeout; }
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
        bool was_interrupted() const
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

    protected:
        template<typename UnblockOne>
        void unblock(UnblockOne unblock_one)
        {
            ScopedSpinLock lock(m_lock);
            do_unblock(unblock_one);
        }

        template<typename UnblockOne>
        void do_unblock(UnblockOne unblock_one)
        {
            ASSERT(m_lock.is_locked());
            for (size_t i = 0; i < m_blockers.size();) {
                auto& info = m_blockers[i];
                if (unblock_one(*info.blocker, info.data)) {
                    m_blockers.remove(i);
                    continue;
                }

                i++;
            }
        }

        template<typename UnblockOne>
        void unblock_all(UnblockOne unblock_one)
        {
            ScopedSpinLock lock(m_lock);
            do_unblock_all(unblock_one);
        }

        template<typename UnblockOne>
        void do_unblock_all(UnblockOne unblock_one)
        {
            ASSERT(m_lock.is_locked());
            for (auto& info : m_blockers) {
                bool did_unblock = unblock_one(*info.blocker, info.data);
                ASSERT(did_unblock);
            }
            m_blockers.clear();
        }

        virtual bool should_add_blocker(Blocker&, void*) { return true; }

        SpinLock<u8> m_lock;

    private:
        struct BlockerInfo {
            Blocker* blocker;
            void* data;
        };
        Vector<BlockerInfo, 4> m_blockers;
    };

    friend class JoinBlocker;
    class JoinBlocker final : public Blocker {
    public:
        explicit JoinBlocker(Thread& joinee, KResult& try_join_result, void*& joinee_exit_value);
        virtual Type blocker_type() const override { return Type::Join; }
        virtual const char* state_string() const override { return "Joining"; }
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
        size_t m_registered_count { 0 };
        bool m_did_unblock { false };
    };

    class WaitBlocker final : public Blocker {
    public:
        enum class UnblockFlags {
            Terminated,
            Stopped,
            Continued
        };

        WaitBlocker(int wait_options, idtype_t id_type, pid_t id, KResultOr<siginfo_t>& result);
        virtual const char* state_string() const override { return "Waiting"; }
        virtual Type blocker_type() const override { return Type::Wait; }
        virtual bool should_block() override { return m_should_block; }
        virtual void not_blocking(bool) override;
        virtual void was_unblocked(bool) override;

        bool unblock(Thread& thread, UnblockFlags flags, u8 signal, bool from_add_blocker);
        bool is_wait() const { return !(m_wait_options & WNOWAIT); }

    private:
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

        bool unblock(Thread&, WaitBlocker::UnblockFlags, u8);
        void try_unblock(WaitBlocker&);
        void finalize();

    protected:
        virtual bool should_add_blocker(Blocker&, void*) override;

    private:
        struct ThreadBlockInfo {
            NonnullRefPtr<Thread> thread;
            WaitBlocker::UnblockFlags flags;
            u8 signal;
            bool was_waited { false };

            explicit ThreadBlockInfo(NonnullRefPtr<Thread>&& thread, WaitBlocker::UnblockFlags flags, u8 signal)
                : thread(move(thread))
                , flags(flags)
                , signal(signal)
            {
            }
        };

        Process& m_process;
        Vector<ThreadBlockInfo, 2> m_threads;
        bool m_finalized { false };
    };

    KResult try_join(JoinBlocker& blocker)
    {
        if (Thread::current() == this)
            return KResult(-EDEADLK);

        ScopedSpinLock lock(m_lock);
        if (!m_is_joinable || state() == Dead)
            return KResult(-EINVAL);

        bool added = m_join_condition.add_blocker(blocker, nullptr);
        ASSERT(added);

        // From this point on the thread is no longer joinable by anyone
        // else. It also means that if the join is timed, it becomes
        // detached when a timeout happens.
        m_is_joinable = false;
        return KSuccess;
    }

    void did_schedule() { ++m_times_scheduled; }
    u32 times_scheduled() const { return m_times_scheduled; }

    void resume_from_stopped();

    bool is_stopped() const { return m_state == Stopped; }
    bool is_blocked() const { return m_state == Blocked; }
    bool has_blocker() const
    {
        ASSERT(m_lock.own_lock());
        return m_blocker != nullptr;
    }
    const Blocker& blocker() const;

    u32 cpu() const { return m_cpu.load(AK::MemoryOrder::memory_order_consume); }
    void set_cpu(u32 cpu) { m_cpu.store(cpu, AK::MemoryOrder::memory_order_release); }
    u32 affinity() const { return m_cpu_affinity; }
    void set_affinity(u32 affinity) { m_cpu_affinity = affinity; }

    u32 stack_ptr() const { return m_tss.esp; }

    RegisterState& get_register_dump_from_stack();

    TSS32& tss() { return m_tss; }
    const TSS32& tss() const { return m_tss; }
    State state() const { return m_state; }
    const char* state_string() const;
    u32 ticks() const { return m_ticks; }

    VirtualAddress thread_specific_data() const { return m_thread_specific_data; }
    size_t thread_specific_region_size() const { return m_thread_specific_region_size; }

    template<typename T, class... Args>
    [[nodiscard]] BlockResult block(const BlockTimeout& timeout, Args&&... args)
    {
        ScopedSpinLock lock(m_lock);
        // We need to hold m_lock so that nobody can unblock a blocker as soon
        // as it is constructed and registered elsewhere
        T t(forward<Args>(args)...);

        bool did_timeout = false;
        RefPtr<Timer> timer;
        {
            ScopedSpinLock scheduler_lock(g_scheduler_lock);
            // We should never be blocking a blocked (or otherwise non-active) thread.
            ASSERT(state() == Thread::Running);
            ASSERT(m_blocker == nullptr);

            m_blocker = &t;
            if (!t.should_block()) {
                // Don't block if the wake condition is already met
                t.not_blocking(false);
                m_blocker = nullptr;
                return BlockResult::NotBlocked;
            }

            auto& block_timeout = t.override_timeout(timeout);
            if (!block_timeout.is_infinite()) {
                m_blocker_timeout = timer = TimerQueue::the().add_timer_without_id(block_timeout.absolute_time(), [&]() {
                    // NOTE: this may execute on the same or any other processor!
                    ScopedSpinLock scheduler_lock(g_scheduler_lock);
                    ScopedSpinLock lock(m_lock);
                    if (m_blocker) {
                        m_blocker_timeout = nullptr;
                        if (!is_stopped()) {
                            // Only unblock if we're not stopped. In either
                            // case the blocker should be marked as timed out
                            unblock();
                        }
                    }
                });
                if (!m_blocker_timeout) {
                    // Timeout is already in the past
                    t.not_blocking(true);
                    m_blocker = nullptr;
                    return BlockResult::InterruptedByTimeout;
                }
            } else {
                m_blocker_timeout = nullptr;
            }

            t.begin_blocking({});

            set_state(Thread::Blocked);
        }

        lock.unlock();

        // Yield to the scheduler, and wait for us to resume unblocked.
        yield_without_holding_big_lock();

        lock.lock();

        bool is_stopped = false;
        {
            ScopedSpinLock scheduler_lock(g_scheduler_lock);
            if (t.was_interrupted_by_signal())
                dispatch_one_pending_signal();

            auto current_state = state();
            // We should no longer be blocked once we woke up, but we may be stopped
            if (current_state == Stopped)
                is_stopped = true;
            else
                ASSERT(current_state == Thread::Running);

            // Remove ourselves...
            m_blocker = nullptr;
            if (timer && !m_blocker_timeout)
                did_timeout = true;
        }

        if (timer && !did_timeout) {
            // Cancel the timer while not holding any locks. This allows
            // the timer function to complete before we remove it
            // (e.g. if it's on another processor)
            TimerQueue::the().cancel_timer(timer.release_nonnull());
        }

        // Notify the blocker that we are no longer blocking. It may need
        // to clean up now while we're still holding m_lock
        auto result = t.end_blocking({}, did_timeout); // calls was_unblocked internally

        if (is_stopped) {
            // If we're stopped we need to yield
            yield_without_holding_big_lock();
        }
        return result;
    }

    BlockResult wait_on(WaitQueue& queue, const char* reason, const BlockTimeout& = nullptr, Atomic<bool>* lock = nullptr, RefPtr<Thread> beneficiary = {});
    void wake_from_queue();

    void unblock_from_blocker(Blocker&);
    void unblock(u8 signal = 0);

    BlockResult sleep(const timespec&, timespec* = nullptr);
    BlockResult sleep_until(const timespec&);

    // Tell this thread to unblock if needed,
    // gracefully unwind the stack and die.
    void set_should_die();
    bool should_die() const { return m_should_die; }
    void die_if_needed();

    void exit(void* = nullptr);

    bool tick();
    void set_ticks_left(u32 t) { m_ticks_left = t; }
    u32 ticks_left() const { return m_ticks_left; }

    u32 kernel_stack_base() const { return m_kernel_stack_base; }
    u32 kernel_stack_top() const { return m_kernel_stack_top; }

    void set_state(State);

    bool is_initialized() const { return m_initialized; }
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
    bool has_unmasked_pending_signals() const { return m_have_any_unmasked_pending_signals.load(AK::memory_order_consume); }
    void terminate_due_to_signal(u8 signal);
    bool should_ignore_signal(u8 signal) const;
    bool has_signal_handler(u8 signal) const;
    bool has_pending_signal(u8 signal) const;
    u32 pending_signals() const;
    u32 pending_signals_for_state() const;

    FPUState& fpu_state() { return *m_fpu_state; }

    void set_default_signal_dispositions();
    bool push_value_on_stack(FlatPtr);

    KResultOr<u32> make_userspace_stack_for_main_thread(Vector<String> arguments, Vector<String> environment, Vector<AuxiliaryValue>);

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

    const char* wait_reason() const
    {
        return m_wait_reason;
    }

    void set_active(bool active)
    {
        m_is_active.store(active, AK::memory_order_release);
    }

    bool is_finalizable() const
    {
        // We can't finalize as long as this thread is still running
        // Note that checking for Running state here isn't sufficient
        // as the thread may not be in Running state but switching out.
        // m_is_active is set to false once the context switch is
        // complete and the thread is not executing on any processor.
        if (m_is_active.load(AK::memory_order_consume))
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
    static IterationDecision for_each_living(Callback);
    template<typename Callback>
    static IterationDecision for_each(Callback);

    static bool is_runnable_state(Thread::State state)
    {
        return state == Thread::State::Running || state == Thread::State::Runnable;
    }

    static constexpr u32 default_kernel_stack_size = 65536;
    static constexpr u32 default_userspace_stack_size = 4 * MiB;

    ThreadTracer* tracer() { return m_tracer.ptr(); }
    void start_tracing_from(ProcessID tracer);
    void stop_tracing();
    void tracer_trap(const RegisterState&);
    bool is_traced() const { return !!m_tracer; }

    RecursiveSpinLock& get_lock() const { return m_lock; }

private:
    IntrusiveListNode m_runnable_list_node;
    IntrusiveListNode m_wait_queue_node;

private:
    friend struct SchedulerData;
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
            if (m_thread_did_exit)
                blocker.unblock(exit_value(), true);
            return m_thread_did_exit;
        }

    private:
        void do_unblock_joiner()
        {
            do_unblock_all([&](Blocker& b, void*) {
                ASSERT(b.blocker_type() == Blocker::Type::Join);
                auto& blocker = static_cast<JoinBlocker&>(b);
                return blocker.unblock(exit_value(), false);
            });
        }

        Atomic<void*> m_exit_value { nullptr };
        bool m_thread_did_exit { false };
    };

    bool unlock_process_if_locked();
    void relock_process(bool did_unlock);
    String backtrace_impl();
    void reset_fpu_state();

    mutable RecursiveSpinLock m_lock;
    NonnullRefPtr<Process> m_process;
    ThreadID m_tid { -1 };
    TSS32 m_tss;
    Atomic<u32> m_cpu { 0 };
    u32 m_cpu_affinity { THREAD_AFFINITY_DEFAULT };
    u32 m_ticks { 0 };
    u32 m_ticks_left { 0 };
    u32 m_times_scheduled { 0 };
    u32 m_pending_signals { 0 };
    u32 m_signal_mask { 0 };
    u32 m_kernel_stack_base { 0 };
    u32 m_kernel_stack_top { 0 };
    OwnPtr<Region> m_kernel_stack_region;
    VirtualAddress m_thread_specific_data;
    size_t m_thread_specific_region_size { 0 };
    SignalActionData m_signal_action_data[32];
    Blocker* m_blocker { nullptr };
    RefPtr<Timer> m_blocker_timeout;
    const char* m_wait_reason { nullptr };
    WaitQueue* m_queue { nullptr };

    JoinBlockCondition m_join_condition;
    Atomic<bool> m_is_active { false };
    bool m_is_joinable { true };

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
    u32 m_extra_priority { 0 };
    u32 m_priority_boost { 0 };

    u8 m_stop_signal { 0 };
    State m_stop_state { Invalid };

    bool m_dump_backtrace_on_finalization { false };
    bool m_should_die { false };
    bool m_initialized { false };
    Atomic<bool> m_have_any_unmasked_pending_signals { false };

    OwnPtr<ThreadTracer> m_tracer;

    void yield_without_holding_big_lock();
    void update_state_for_thread(Thread::State previous_state);
};

template<typename Callback>
inline IterationDecision Thread::for_each_living(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    return Thread::for_each([callback](Thread& thread) -> IterationDecision {
        if (thread.state() != Thread::State::Dead && thread.state() != Thread::State::Dying)
            return callback(thread);
        return IterationDecision::Continue;
    });
}

template<typename Callback>
inline IterationDecision Thread::for_each(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    ScopedSpinLock lock(g_scheduler_lock);
    auto ret = Scheduler::for_each_runnable(callback);
    if (ret == IterationDecision::Break)
        return ret;
    return Scheduler::for_each_nonrunnable(callback);
}

template<typename Callback>
inline IterationDecision Thread::for_each_in_state(State state, Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    ScopedSpinLock lock(g_scheduler_lock);
    auto new_callback = [=](Thread& thread) -> IterationDecision {
        if (thread.state() == state)
            return callback(thread);
        return IterationDecision::Continue;
    };
    if (is_runnable_state(state))
        return Scheduler::for_each_runnable(new_callback);
    return Scheduler::for_each_nonrunnable(new_callback);
}

const LogStream& operator<<(const LogStream&, const Thread&);

struct SchedulerData {
    typedef IntrusiveList<Thread, &Thread::m_runnable_list_node> ThreadList;

    ThreadList m_runnable_threads;
    ThreadList m_nonrunnable_threads;

    bool has_thread(Thread& thread) const
    {
        return m_runnable_threads.contains(thread) || m_nonrunnable_threads.contains(thread);
    }

    ThreadList& thread_list_for_state(Thread::State state)
    {
        if (Thread::is_runnable_state(state))
            return m_runnable_threads;
        return m_nonrunnable_threads;
    }
};

template<typename Callback>
inline IterationDecision Scheduler::for_each_runnable(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(g_scheduler_lock.own_lock());
    auto& tl = g_scheduler_data->m_runnable_threads;
    for (auto it = tl.begin(); it != tl.end();) {
        auto& thread = *it;
        it = ++it;
        if (callback(thread) == IterationDecision::Break)
            return IterationDecision::Break;
    }

    return IterationDecision::Continue;
}

template<typename Callback>
inline IterationDecision Scheduler::for_each_nonrunnable(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(g_scheduler_lock.own_lock());
    auto& tl = g_scheduler_data->m_nonrunnable_threads;
    for (auto it = tl.begin(); it != tl.end();) {
        auto& thread = *it;
        it = ++it;
        if (callback(thread) == IterationDecision::Break)
            return IterationDecision::Break;
    }

    return IterationDecision::Continue;
}

}
