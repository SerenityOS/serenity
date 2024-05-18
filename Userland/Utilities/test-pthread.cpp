/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <LibMain/Main.h>
#include <LibThreading/Thread.h>
#include <errno.h>
#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>

static ErrorOr<void> test_once()
{
    constexpr size_t threads_count = 10;

    static Vector<int> v;
    v.clear();
    IGNORE_USE_IN_ESCAPING_LAMBDA pthread_once_t once = PTHREAD_ONCE_INIT;
    Vector<NonnullRefPtr<Threading::Thread>, threads_count> threads;

    for (size_t i = 0; i < threads_count; i++) {
        threads.unchecked_append(TRY(Threading::Thread::try_create([&] {
            return pthread_once(&once, [] {
                v.append(35);
                sleep(1);
            });
        })));
        threads.last()->start();
    }

    for (auto& thread : threads)
        (void)thread->join();

    VERIFY(v.size() == 1);

    return {};
}

static ErrorOr<void> test_mutex()
{
    constexpr size_t threads_count = 10;
    constexpr size_t num_times = 100;

    IGNORE_USE_IN_ESCAPING_LAMBDA Vector<int> v;
    Vector<NonnullRefPtr<Threading::Thread>, threads_count> threads;
    IGNORE_USE_IN_ESCAPING_LAMBDA pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

    for (size_t i = 0; i < threads_count; i++) {
        threads.unchecked_append(TRY(Threading::Thread::try_create([&] {
            for (size_t j = 0; j < num_times; j++) {
                pthread_mutex_lock(&mutex);
                v.append(35);
                sched_yield();
                pthread_mutex_unlock(&mutex);
                sched_yield();
            }
            return 0;
        })));
        threads.last()->start();
    }

    for (auto& thread : threads)
        (void)thread->join();

    VERIFY(v.size() == threads_count * num_times);
    VERIFY(pthread_mutex_trylock(&mutex) == 0);
    VERIFY(pthread_mutex_trylock(&mutex) == EBUSY);

    return {};
}

static ErrorOr<void> test_semaphore_as_lock()
{
    constexpr size_t threads_count = 10;
    constexpr size_t num_times = 100;

    IGNORE_USE_IN_ESCAPING_LAMBDA Vector<int> v;
    Vector<NonnullRefPtr<Threading::Thread>, threads_count> threads;
    IGNORE_USE_IN_ESCAPING_LAMBDA sem_t semaphore;
    sem_init(&semaphore, 0, 1);

    for (size_t i = 0; i < threads_count; i++) {
        threads.unchecked_append(TRY(Threading::Thread::try_create([&] {
            for (size_t j = 0; j < num_times; j++) {
                sem_wait(&semaphore);
                v.append(35);
                sched_yield();
                sem_post(&semaphore);
                sched_yield();
            }
            return 0;
        })));
        threads.last()->start();
    }

    for (auto& thread : threads)
        (void)thread->join();

    VERIFY(v.size() == threads_count * num_times);
    VERIFY(sem_trywait(&semaphore) == 0);
    VERIFY(sem_trywait(&semaphore) == -1);
    VERIFY(errno == EAGAIN);

    return {};
}

static ErrorOr<void> test_semaphore_as_event()
{
    IGNORE_USE_IN_ESCAPING_LAMBDA Vector<int> v;
    IGNORE_USE_IN_ESCAPING_LAMBDA sem_t semaphore;
    sem_init(&semaphore, 0, 0);

    auto reader = TRY(Threading::Thread::try_create([&] {
        sem_wait(&semaphore);
        VERIFY(v.size() == 1);
        return 0;
    }));
    reader->start();

    auto writer = TRY(Threading::Thread::try_create([&] {
        sched_yield();
        v.append(35);
        sem_post(&semaphore);
        return 0;
    }));
    writer->start();

    [[maybe_unused]] auto r1 = reader->join();
    [[maybe_unused]] auto r2 = writer->join();

    VERIFY(sem_trywait(&semaphore) == -1);
    VERIFY(errno == EAGAIN);

    return {};
}

static ErrorOr<void> test_semaphore_nonbinary()
{
    constexpr size_t num = 5;
    constexpr size_t threads_count = 10;
    constexpr size_t num_times = 100;

    Vector<NonnullRefPtr<Threading::Thread>, threads_count> threads;
    IGNORE_USE_IN_ESCAPING_LAMBDA sem_t semaphore;
    sem_init(&semaphore, 0, num);

    IGNORE_USE_IN_ESCAPING_LAMBDA Atomic<u32, AK::memory_order_relaxed> value = 0;
    IGNORE_USE_IN_ESCAPING_LAMBDA Atomic<bool, AK::memory_order_relaxed> seen_more_than_two = false;

    for (size_t i = 0; i < threads_count; i++) {
        threads.unchecked_append(TRY(Threading::Thread::try_create([&] {
            for (size_t j = 0; j < num_times; j++) {
                sem_wait(&semaphore);
                u32 v = 1 + value.fetch_add(1);
                VERIFY(v <= num);
                if (v > 2)
                    seen_more_than_two.store(true);
                sched_yield();
                value.fetch_sub(1);
                sem_post(&semaphore);
            }
            return 0;
        })));
        threads.last()->start();
    }

    for (auto& thread : threads)
        (void)thread->join();

    VERIFY(value.load() == 0);
    VERIFY(seen_more_than_two.load());
    for (size_t i = 0; i < num; i++) {
        VERIFY(sem_trywait(&semaphore) == 0);
    }
    VERIFY(sem_trywait(&semaphore) == -1);
    VERIFY(errno == EAGAIN);

    return {};
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(test_once());
    TRY(test_mutex());

    TRY(test_semaphore_as_lock());
    TRY(test_semaphore_as_event());
    TRY(test_semaphore_nonbinary());

    return 0;
}
