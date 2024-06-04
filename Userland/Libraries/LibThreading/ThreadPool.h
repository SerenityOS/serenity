/*
 * Copyright (c) 2024, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Noncopyable.h>
#include <AK/Queue.h>
#include <LibCore/System.h>
#include <LibThreading/ConditionVariable.h>
#include <LibThreading/MutexProtected.h>
#include <LibThreading/Thread.h>

namespace Threading {

template<typename Pool>
struct ThreadPoolLooper {
    IterationDecision next(Pool& pool, bool wait)
    {
        Optional<typename Pool::Work> entry;
        while (true) {
            entry = pool.m_work_queue.with_locked([&](auto& queue) -> Optional<typename Pool::Work> {
                if (queue.is_empty())
                    return {};
                return queue.dequeue();
            });
            if (entry.has_value())
                break;
            if (pool.m_should_exit)
                return IterationDecision::Break;

            if (!wait)
                return IterationDecision::Continue;

            pool.m_mutex.lock();
            pool.m_work_available.wait();
            pool.m_mutex.unlock();
        }

        pool.m_busy_count++;
        pool.m_handler(entry.release_value());
        return IterationDecision::Continue;
    }
};

template<typename TWork, template<typename> class Looper = ThreadPoolLooper>
class ThreadPool {
    AK_MAKE_NONCOPYABLE(ThreadPool);
    AK_MAKE_NONMOVABLE(ThreadPool);

public:
    using Work = TWork;
    friend struct ThreadPoolLooper<ThreadPool>;

    template<typename... Args>
    ThreadPool(Optional<size_t> concurrency = {}, Args&&... looper_args)
    requires(IsFunction<Work>)
        : m_handler([](Work work) { return work(); })
        , m_work_available(m_mutex)
        , m_work_done(m_mutex)
    {
        initialize_workers(concurrency.value_or(Core::System::hardware_concurrency()), forward<Args>(looper_args)...);
    }

    template<typename... Args>
    explicit ThreadPool(Function<void(Work)> handler, Optional<size_t> concurrency = {}, Args&&... looper_args)
        : m_handler(move(handler))
        , m_work_available(m_mutex)
        , m_work_done(m_mutex)
    {
        initialize_workers(concurrency.value_or(Core::System::hardware_concurrency()), forward<Args>(looper_args)...);
    }

    ~ThreadPool()
    {
        request_exit();
        for (auto& worker : m_workers) {
            m_work_available.broadcast();
            (void)worker->join();
        }
    }

    void request_exit()
    {
        m_should_exit.store(true, AK::MemoryOrder::memory_order_release);
        m_work_available.broadcast();
    }

    bool was_exit_requested() const
    {
        return m_should_exit.load(AK::MemoryOrder::memory_order_acquire);
    }

    void submit(Work work)
    {
        m_work_queue.with_locked([&](auto& queue) {
            queue.enqueue({ move(work) });
        });
        m_work_available.broadcast();
    }

    void wait_for_all()
    {
        while (true) {
            if (m_work_queue.with_locked([](auto& queue) { return queue.is_empty(); }))
                break;
            m_mutex.lock();
            m_work_done.wait();
            m_mutex.unlock();
        }

        while (m_busy_count.load(AK::MemoryOrder::memory_order_acquire) > 0) {
            m_mutex.lock();
            m_work_done.wait();
            m_mutex.unlock();
        }
    }

private:
    template<typename... Args>
    void initialize_workers(size_t concurrency, Args&&... looper_args)
    {
        for (size_t i = 0; i < concurrency; ++i) {
            m_workers.append(Thread::construct([this, looper_args...]() -> intptr_t {
                Looper<ThreadPool> thread_looper { move(looper_args)... };
                for (; !m_should_exit;) {
                    auto result = thread_looper.next(*this, true);
                    m_busy_count--;
                    m_work_done.signal();
                    if (result == IterationDecision::Break)
                        break;
                }

                return 0;
            },
                "ThreadPool worker"sv));
        }

        for (auto& worker : m_workers)
            worker->start();
    }

    Vector<NonnullRefPtr<Thread>> m_workers;
    MutexProtected<Queue<Work>> m_work_queue;
    Function<void(Work)> m_handler;
    Mutex m_mutex;
    ConditionVariable m_work_available;
    ConditionVariable m_work_done;
    Atomic<bool> m_should_exit { false };
    Atomic<size_t> m_busy_count { 0 };
};

}
