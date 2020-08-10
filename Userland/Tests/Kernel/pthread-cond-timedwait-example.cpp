/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
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

#include <cassert>
#include <cstring>
#include <ctime>
#include <errno.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

struct worker_t {
    const char* name;
    int count;
    pthread_t thread;
    pthread_mutex_t lock;
    pthread_cond_t cond;
    long int wait_time;
};

static void* run_worker(void* args)
{
    struct timespec time_to_wait = { 0, 0 };
    worker_t* worker = (worker_t*)args;
    worker->count = 0;

    while (worker->count < 25) {
        time_to_wait.tv_sec = time(nullptr) + worker->wait_time;
        pthread_mutex_lock(&worker->lock);
        int rc = pthread_cond_timedwait(&worker->cond, &worker->lock, &time_to_wait);

        // Validate return code is always timed out.
        assert(rc == -1);
        assert(errno == ETIMEDOUT);

        worker->count++;
        printf("Increase worker[%s] count to [%d]\n", worker->name, worker->count);
        pthread_mutex_unlock(&worker->lock);
    }

    return nullptr;
}

static void init_worker(worker_t* worker, const char* name, long int wait_time)
{
    worker->name = name;
    worker->wait_time = wait_time;

    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

    pthread_mutex_init(&worker->lock, nullptr);
    pthread_cond_init(&worker->cond, nullptr);
    pthread_create(&worker->thread, &attr, &run_worker, (void*)worker);

    pthread_attr_destroy(&attr);
}

int main()
{
    worker_t worker_a;
    init_worker(&worker_a, "A", 2L);

    worker_t worker_b;
    init_worker(&worker_b, "B", 4L);

    pthread_join(worker_a.thread, nullptr);
    pthread_join(worker_b.thread, nullptr);

    return EXIT_SUCCESS;
}
