/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibThreading/Thread.h>
#include <errno.h>
#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>

static void test_once()
{
    constexpr size_t threads_count = 10;

    static Vector<int> v;
    v.clear();
    pthread_once_t once = PTHREAD_ONCE_INIT;
    NonnullRefPtrVector<Threading::Thread, threads_count> threads;

    for (size_t i = 0; i < threads_count; i++) {
        threads.append(Threading::Thread::construct([&] {
            return pthread_once(&once, [] {
                v.append(35);
                sleep(1);
            });
        }));
        threads.last().start();
    }
    for (auto& thread : threads)
        [[maybe_unused]] auto res = thread.join();

    VERIFY(v.size() == 1);
}

static void test_mutex()
{
    constexpr size_t threads_count = 10;
    constexpr size_t num_times = 100;

    Vector<int> v;
    NonnullRefPtrVector<Threading::Thread, threads_count> threads;
    pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

    for (size_t i = 0; i < threads_count; i++) {
        threads.append(Threading::Thread::construct([&] {
            for (size_t j = 0; j < num_times; j++) {
                pthread_mutex_lock(&mutex);
                v.append(35);
                sched_yield();
                pthread_mutex_unlock(&mutex);
                sched_yield();
            }
            return 0;
        }));
        threads.last().start();
    }
    for (auto& thread : threads)
        [[maybe_unused]] auto res = thread.join();

    VERIFY(v.size() == threads_count * num_times);
    VERIFY(pthread_mutex_trylock(&mutex) == 0);
    VERIFY(pthread_mutex_trylock(&mutex) == EBUSY);
}

static void test_semaphore_as_lock()
{
    constexpr size_t threads_count = 10;
    constexpr size_t num_times = 100;

    Vector<int> v;
    NonnullRefPtrVector<Threading::Thread, threads_count> threads;
    sem_t semaphore;
    sem_init(&semaphore, 0, 1);

    for (size_t i = 0; i < threads_count; i++) {
        threads.append(Threading::Thread::construct([&] {
            for (size_t j = 0; j < num_times; j++) {
                sem_wait(&semaphore);
                v.append(35);
                sched_yield();
                sem_post(&semaphore);
                sched_yield();
            }
            return 0;
        }));
        threads.last().start();
    }
    for (auto& thread : threads)
        [[maybe_unused]] auto res = thread.join();

    VERIFY(v.size() == threads_count * num_times);
    VERIFY(sem_trywait(&semaphore) == 0);
    VERIFY(sem_trywait(&semaphore) == EAGAIN);
}

static void test_semaphore_as_event()
{
    Vector<int> v;
    sem_t semaphore;
    sem_init(&semaphore, 0, 0);

    auto reader = Threading::Thread::construct([&] {
        sem_wait(&semaphore);
        VERIFY(v.size() == 1);
        return 0;
    });
    reader->start();

    auto writer = Threading::Thread::construct([&] {
        sched_yield();
        v.append(35);
        sem_post(&semaphore);
        return 0;
    });
    writer->start();

    [[maybe_unused]] auto r1 = reader->join();
    [[maybe_unused]] auto r2 = writer->join();

    VERIFY(sem_trywait(&semaphore) == EAGAIN);
}

static void test_semaphore_nonbinary()
{
    constexpr size_t num = 5;
    constexpr size_t threads_count = 10;
    constexpr size_t num_times = 100;

    NonnullRefPtrVector<Threading::Thread, threads_count> threads;
    sem_t semaphore;
    sem_init(&semaphore, 0, num);

    Atomic<u32, AK::memory_order_relaxed> value = 0;
    Atomic<bool, AK::memory_order_relaxed> seen_more_than_two = false;

    for (size_t i = 0; i < threads_count; i++) {
        threads.append(Threading::Thread::construct([&] {
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
        }));
        threads.last().start();
    }

    for (auto& thread : threads)
        [[maybe_unused]] auto res = thread.join();

    VERIFY(value.load() == 0);
    VERIFY(seen_more_than_two.load());
    for (size_t i = 0; i < num; i++) {
        VERIFY(sem_trywait(&semaphore) == 0);
    }
    VERIFY(sem_trywait(&semaphore) == EAGAIN);
}

int main()
{
    test_once();
    test_mutex();

    test_semaphore_as_lock();
    test_semaphore_as_event();
    test_semaphore_nonbinary();

    return 0;
}
