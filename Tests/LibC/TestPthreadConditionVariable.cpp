/*
 * Copyright (c) 2022, Michael Cullen <michael@michaelcullen.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <pthread.h>
#include <stdio.h>
#include <unistd.h>

struct SyncData {
    pthread_mutex_t mtx;
    pthread_cond_t cond;
};

struct ThreadData {
    SyncData* sd;
    bool done { false };
};

void* thread_worker(void* arg);

void* thread_worker(void* arg)
{
    printf("Thread with TID %d starting\n", pthread_self());
    ThreadData* td = (ThreadData*)arg;
    pthread_mutex_lock(&td->sd->mtx);

    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    // wait up to 10 seconds, as a safety net to prevent the test from completely hanging
    ts.tv_sec += 10;

    printf("Thread %d waiting for cond\n", pthread_self());
    int res = pthread_cond_timedwait(&td->sd->cond, &td->sd->mtx, &ts);
    if (res == 0) {
        printf("Thread %d done waiting for cond\n", pthread_self());
        td->done = true;
    } else if (res == ETIMEDOUT) {
        printf("Thread %d failed to wait for condition (timed out)\n", pthread_self());
    } else {
        printf("Thread %d failed to wait for condition (unknown error)\n", pthread_self());
    }

    pthread_mutex_unlock(&td->sd->mtx);
    printf("Thread %d exiting\n", pthread_self());
    return nullptr;
}

TEST_CASE(conditionvar_broadcast)
{
    pthread_t threadA, threadB;

    SyncData sd;

    pthread_mutex_init(&sd.mtx, NULL);
    pthread_cond_init(&sd.cond, NULL);

    ThreadData tdA {
        .sd = &sd,
    };
    ThreadData tdB {
        .sd = &sd,
    };

    pthread_create(&threadA, NULL, thread_worker, &tdA);
    pthread_create(&threadB, NULL, thread_worker, &tdB);

    printf("Waiting 2s for stuff to get going\n");
    usleep(2000 * 1000);

    printf("Broadcasting condition variable\n");
    pthread_cond_broadcast(&sd.cond);

    printf("Joining threads\n");
    pthread_join(threadA, nullptr);
    pthread_join(threadB, nullptr);

    EXPECT_EQ(true, tdA.done);
    EXPECT_EQ(true, tdB.done);

    pthread_cond_destroy(&sd.cond);
    pthread_mutex_destroy(&sd.mtx);
}
