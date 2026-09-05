/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Time.h>
#include <LibTest/TestCase.h>
#include <errno.h>
#include <pthread.h>
#include <time.h>

namespace {

struct Worker {
    pthread_t thread;
    pthread_mutex_t lock;
    pthread_cond_t cond;
    Duration wait_time;
    u32 count;

    ~Worker()
    {
        pthread_join(thread, nullptr);
    }
};

void* run_worker(void* args)
{
    auto& worker = *static_cast<Worker*>(args);

    for (u32 i = 0; i < worker.count; ++i) {
        timespec time_to_wait = (UnixDateTime::now() + worker.wait_time).to_timespec();
        pthread_mutex_lock(&worker.lock);
        int rc = pthread_cond_timedwait(&worker.cond, &worker.lock, &time_to_wait);

        // Validate return code is always timed out.
        EXPECT_EQ(rc, ETIMEDOUT);

        pthread_mutex_unlock(&worker.lock);
    }

    return nullptr;
}

void init_worker(Worker& worker, Duration duration, u32 count)
{
    worker.wait_time = duration;
    worker.count = count;

    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

    pthread_mutex_init(&worker.lock, nullptr);
    pthread_cond_init(&worker.cond, nullptr);
    pthread_create(&worker.thread, &attr, &run_worker, &worker);

    pthread_attr_destroy(&attr);
}

}

TEST_CASE(cond_timedwait_returns_etimedout)
{
    Worker worker_a;
    init_worker(worker_a, Duration::from_milliseconds(-1), 50);

    Worker worker_b;
    init_worker(worker_b, Duration::from_microseconds(500), 50);

    Worker worker_c;
    init_worker(worker_c, Duration::from_milliseconds(20), 20);

    Worker worker_d;
    init_worker(worker_d, Duration::from_milliseconds(100), 5);

    Worker worker_e;
    init_worker(worker_e, Duration::from_seconds(2), 1);
}
