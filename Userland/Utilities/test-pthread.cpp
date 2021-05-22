/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibThreading/Thread.h>
#include <pthread.h>
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

int main()
{
    test_once();
    return 0;
}
