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

#include <mman.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static int mutex_test();
static int detached_test();
static int priority_test();
static int stack_size_test();
static int set_stack_test();

int main(int argc, char** argv)
{
    if (argc == 2 && *argv[1] == 'm')
        return mutex_test();
    if (argc == 2 && *argv[1] == 'd')
        return detached_test();
    if (argc == 2 && *argv[1] == 'p')
        return priority_test();
    if (argc == 2 && *argv[1] == 's')
        return stack_size_test();
    if (argc == 2 && *argv[1] == 'x')
        return set_stack_test();

    printf("Hello from the first thread!\n");
    pthread_t thread_id;
    int rc = pthread_create(
        &thread_id, nullptr, [](void*) -> void* {
            printf("Hi there, from the second thread!\n");
            pthread_exit((void*)0xDEADBEEF);
            return nullptr;
        },
        nullptr);
    if (rc < 0) {
        perror("pthread_create");
        return 1;
    }
    void* retval;
    rc = pthread_join(thread_id, &retval);
    if (rc < 0) {
        perror("pthread_join");
        return 1;
    }
    printf("Okay, joined and got retval=%p\n", retval);
    return 0;
}

static pthread_mutex_t mutex;

int mutex_test()
{
    int rc = pthread_mutex_init(&mutex, nullptr);
    if (rc < 0) {
        perror("pthread_mutex_init");
        return 1;
    }
    pthread_t thread_id;
    rc = pthread_create(
        &thread_id, nullptr, [](void*) -> void* {
            printf("I'm the secondary thread :^)\n");
            for (;;) {
                pthread_mutex_lock(&mutex);
                printf("Second thread stole mutex\n");
                sleep(1);
                printf("Second thread giving back mutex\n");
                pthread_mutex_unlock(&mutex);
                sleep(1);
            }
            pthread_exit((void*)0xDEADBEEF);
            return nullptr;
        },
        nullptr);
    if (rc < 0) {
        perror("pthread_create");
        return 1;
    }
    for (;;) {
        pthread_mutex_lock(&mutex);
        printf("Obnoxious spam!\n");
        pthread_mutex_unlock(&mutex);
        usleep(10000);
    }
    return 0;
}

int detached_test()
{
    pthread_attr_t attributes;
    int rc = pthread_attr_init(&attributes);
    if (rc != 0) {
        printf("pthread_attr_setdetachstate: %s\n", strerror(rc));
        return 1;
    }

    int detach_state = 99; // clearly invalid
    rc = pthread_attr_getdetachstate(&attributes, &detach_state);
    if (rc != 0) {
        printf("pthread_attr_setdetachstate: %s\n", strerror(rc));
        return 2;
    }
    printf("Default detach state: %s\n", detach_state == PTHREAD_CREATE_JOINABLE ? "joinable" : "detached");

    detach_state = PTHREAD_CREATE_DETACHED;
    rc = pthread_attr_setdetachstate(&attributes, detach_state);
    if (rc != 0) {
        printf("pthread_attr_setdetachstate: %s\n", strerror(rc));
        return 3;
    }
    printf("Set detach state on new thread to detached\n");

    pthread_t thread_id;
    rc = pthread_create(
        &thread_id, &attributes, [](void*) -> void* {
            printf("I'm the secondary thread :^)\n");
            sleep(1);
            pthread_exit((void*)0xDEADBEEF);
            return nullptr;
        },
        nullptr);
    if (rc < 0) {
        perror("pthread_create");
        return 4;
    }

    void* ret_val;
    errno = 0;
    rc = pthread_join(thread_id, &ret_val);
    if (rc < 0 && errno != EINVAL) {
        perror("pthread_join");
        return 5;
    }
    if (errno != EINVAL) {
        printf("Expected EINVAL! Thread was joinable?\n");
        return 6;
    }

    sleep(2);
    printf("Thread was created detached. I sure hope it exited on its own.\n");

    rc = pthread_attr_destroy(&attributes);
    if (rc != 0) {
        printf("pthread_attr_setdetachstate: %s\n", strerror(rc));
        return 7;
    }

    return 0;
}

int priority_test()
{
    pthread_attr_t attributes;
    int rc = pthread_attr_init(&attributes);
    if (rc != 0) {
        printf("pthread_attr_init: %s\n", strerror(rc));
        return 1;
    }

    struct sched_param sched_params;
    rc = pthread_attr_getschedparam(&attributes, &sched_params);
    if (rc != 0) {
        printf("pthread_attr_getschedparam: %s\n", strerror(rc));
        return 2;
    }
    printf("Default priority: %d\n", sched_params.sched_priority);

    sched_params.sched_priority = 3;
    rc = pthread_attr_setschedparam(&attributes, &sched_params);
    if (rc != 0) {
        printf("pthread_attr_setschedparam: %s\n", strerror(rc));
        return 3;
    }
    printf("Set thread priority to 3\n");

    pthread_t thread_id;
    rc = pthread_create(
        &thread_id, &attributes, [](void*) -> void* {
            printf("I'm the secondary thread :^)\n");
            sleep(1);
            pthread_exit((void*)0xDEADBEEF);
            return nullptr;
        },
        nullptr);
    if (rc < 0) {
        perror("pthread_create");
        return 4;
    }

    rc = pthread_join(thread_id, nullptr);
    if (rc < 0) {
        perror("pthread_join");
        return 5;
    }

    rc = pthread_attr_destroy(&attributes);
    if (rc != 0) {
        printf("pthread_attr_destroy: %s\n", strerror(rc));
        return 6;
    }

    return 0;
}

int stack_size_test()
{
    pthread_attr_t attributes;
    int rc = pthread_attr_init(&attributes);
    if (rc != 0) {
        printf("pthread_attr_init: %s\n", strerror(rc));
        return 1;
    }

    size_t stack_size;
    rc = pthread_attr_getstacksize(&attributes, &stack_size);
    if (rc != 0) {
        printf("pthread_attr_getstacksize: %s\n", strerror(rc));
        return 2;
    }
    printf("Default stack size: %zu\n", stack_size);

    stack_size = 8 * 1024 * 1024;
    rc = pthread_attr_setstacksize(&attributes, stack_size);
    if (rc != 0) {
        printf("pthread_attr_setstacksize: %s\n", strerror(rc));
        return 3;
    }
    printf("Set thread stack size to 8 MB\n");

    pthread_t thread_id;
    rc = pthread_create(
        &thread_id, &attributes, [](void*) -> void* {
            printf("I'm the secondary thread :^)\n");
            sleep(1);
            pthread_exit((void*)0xDEADBEEF);
            return nullptr;
        },
        nullptr);
    if (rc < 0) {
        perror("pthread_create");
        return 4;
    }

    rc = pthread_join(thread_id, nullptr);
    if (rc < 0) {
        perror("pthread_join");
        return 5;
    }

    rc = pthread_attr_destroy(&attributes);
    if (rc != 0) {
        printf("pthread_attr_destroy: %s\n", strerror(rc));
        return 6;
    }

    return 0;
}

int set_stack_test()
{
    pthread_attr_t attributes;
    int rc = pthread_attr_init(&attributes);
    if (rc < 0) {
        printf("pthread_attr_init: %s\n", strerror(rc));
        return 1;
    }

    size_t stack_size = 8 * 1024 * 1024;
    void* stack_addr = mmap_with_name(nullptr, stack_size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS | MAP_STACK, 0, 0, "Cool stack");

    if (!stack_addr) {
        perror("mmap_with_name");
        return -1;
    }

    rc = pthread_attr_setstack(&attributes, stack_addr, stack_size);
    if (rc != 0) {
        printf("pthread_attr_setstack: %s\n", strerror(rc));
        return 2;
    }
    printf("Set thread stack to %p, size %zu\n", stack_addr, stack_size);

    size_t stack_size_verify;
    void* stack_addr_verify;

    rc = pthread_attr_getstack(&attributes, &stack_addr_verify, &stack_size_verify);
    if (rc != 0) {
        printf("pthread_attr_getstack: %s\n", strerror(rc));
        return 3;
    }

    if (stack_addr != stack_addr_verify || stack_size != stack_size_verify) {
        printf("Stack address and size don't match! addr: %p %p, size: %zu %zu\n", stack_addr, stack_addr_verify, stack_size, stack_size_verify);
        return 4;
    }

    pthread_t thread_id;
    rc = pthread_create(
        &thread_id, &attributes, [](void*) -> void* {
            printf("I'm the secondary thread :^)\n");
            sleep(1);
            pthread_exit((void*)0xDEADBEEF);
            return nullptr;
        },
        nullptr);
    if (rc < 0) {
        perror("pthread_create");
        return 5;
    }

    rc = pthread_join(thread_id, nullptr);
    if (rc < 0) {
        perror("pthread_join");
        return 6;
    }

    rc = pthread_attr_destroy(&attributes);
    if (rc != 0) {
        printf("pthread_attr_destroy: %s\n", strerror(rc));
        return 7;
    }

    return 0;
}
