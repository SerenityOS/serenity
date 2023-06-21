/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

static int mutex_test();
static int detached_test();
static int priority_test();
static int stack_size_test();
static int staying_alive_test();
static int set_stack_test();
static int kill_test();

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView test_name = "n"sv;

    Core::ArgsParser args_parser;
    args_parser.set_general_help(
        "Exercise error-handling and edge-case paths of the execution environment "
        "(i.e., Kernel or UE) by doing unusual thread-related things.");
    args_parser.add_positional_argument(test_name, "Test to run (m = mutex, d = detached, p = priority, s = stack size, t = simple thread test, x = set stack, k = kill, nothing = join race)", "test-name", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (test_name[0] == 'm')
        return mutex_test();
    if (test_name[0] == 'd')
        return detached_test();
    if (test_name[0] == 'p')
        return priority_test();
    if (test_name[0] == 's')
        return stack_size_test();
    if (test_name[0] == 't')
        return staying_alive_test();
    if (test_name[0] == 'x')
        return set_stack_test();
    if (test_name[0] == 'k')
        return kill_test();
    if (test_name[0] != 'n') {
        args_parser.print_usage(stdout, arguments.strings[0]);
        return 1;
    }

    outln("Hello from the first thread!");
    pthread_t thread_id;
    int rc = pthread_create(
        &thread_id, nullptr, [](void*) -> void* {
            outln("Hi there, from the second thread!");
            pthread_exit((void*)0xDEADBEEF);
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
    outln("Okay, joined and got retval={}", retval);
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
            outln("I'm the secondary thread :^)");
            for (;;) {
                pthread_mutex_lock(&mutex);
                outln("Second thread stole mutex");
                sleep(1);
                outln("Second thread giving back mutex");
                pthread_mutex_unlock(&mutex);
                sleep(1);
            }
        },
        nullptr);
    if (rc < 0) {
        perror("pthread_create");
        return 1;
    }
    for (;;) {
        pthread_mutex_lock(&mutex);
        outln("Obnoxious spam!");
        pthread_mutex_unlock(&mutex);
        usleep(10000);
    }
}

int detached_test()
{
    pthread_attr_t attributes;
    int rc = pthread_attr_init(&attributes);
    if (rc != 0) {
        outln("pthread_attr_init: {}", strerror(rc));
        return 1;
    }

    int detach_state = 99; // clearly invalid
    rc = pthread_attr_getdetachstate(&attributes, &detach_state);
    if (rc != 0) {
        outln("pthread_attr_getdetachstate: {}", strerror(rc));
        return 2;
    }
    outln("Default detach state: {}", detach_state == PTHREAD_CREATE_JOINABLE ? "joinable" : "detached");

    detach_state = PTHREAD_CREATE_DETACHED;
    rc = pthread_attr_setdetachstate(&attributes, detach_state);
    if (rc != 0) {
        outln("pthread_attr_setdetachstate: {}", strerror(rc));
        return 3;
    }
    outln("Set detach state on new thread to detached");

    pthread_t thread_id;
    rc = pthread_create(
        &thread_id, &attributes, [](void*) -> void* {
            outln("I'm the secondary thread :^)");
            sleep(1);
            pthread_exit((void*)0xDEADBEEF);
        },
        nullptr);
    if (rc != 0) {
        outln("pthread_create: {}", strerror(rc));
        return 4;
    }

    void* ret_val;
    rc = pthread_join(thread_id, &ret_val);
    if (rc != 0 && rc != EINVAL) {
        outln("pthread_join: {}", strerror(rc));
        return 5;
    }
    if (rc != EINVAL) {
        outln("Expected EINVAL! Thread was joinable?");
        return 6;
    }

    sleep(2);
    outln("Thread was created detached. I sure hope it exited on its own.");

    rc = pthread_attr_destroy(&attributes);
    if (rc != 0) {
        outln("pthread_attr_destroy: {}", strerror(rc));
        return 7;
    }

    return 0;
}

int priority_test()
{
    pthread_attr_t attributes;
    int rc = pthread_attr_init(&attributes);
    if (rc != 0) {
        outln("pthread_attr_init: {}", strerror(rc));
        return 1;
    }

    struct sched_param sched_params;
    rc = pthread_attr_getschedparam(&attributes, &sched_params);
    if (rc != 0) {
        outln("pthread_attr_getschedparam: {}", strerror(rc));
        return 2;
    }
    outln("Default priority: {}", sched_params.sched_priority);

    sched_params.sched_priority = 3;
    rc = pthread_attr_setschedparam(&attributes, &sched_params);
    if (rc != 0) {
        outln("pthread_attr_setschedparam: {}", strerror(rc));
        return 3;
    }
    outln("Set thread priority to 3");

    pthread_t thread_id;
    rc = pthread_create(
        &thread_id, &attributes, [](void*) -> void* {
            outln("I'm the secondary thread :^)");
            sleep(1);
            pthread_exit((void*)0xDEADBEEF);
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
        outln("pthread_attr_destroy: {}", strerror(rc));
        return 6;
    }

    return 0;
}

int stack_size_test()
{
    pthread_attr_t attributes;
    int rc = pthread_attr_init(&attributes);
    if (rc != 0) {
        outln("pthread_attr_init: {}", strerror(rc));
        return 1;
    }

    size_t stack_size;
    rc = pthread_attr_getstacksize(&attributes, &stack_size);
    if (rc != 0) {
        outln("pthread_attr_getstacksize: {}", strerror(rc));
        return 2;
    }
    outln("Default stack size: {}", stack_size);

    stack_size = 8 * 1024 * 1024;
    rc = pthread_attr_setstacksize(&attributes, stack_size);
    if (rc != 0) {
        outln("pthread_attr_setstacksize: {}", strerror(rc));
        return 3;
    }
    outln("Set thread stack size to 8 MiB");

    pthread_t thread_id;
    rc = pthread_create(
        &thread_id, &attributes, [](void*) -> void* {
            outln("I'm the secondary thread :^)");
            sleep(1);
            pthread_exit((void*)0xDEADBEEF);
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
        outln("pthread_attr_destroy: {}", strerror(rc));
        return 6;
    }

    return 0;
}

int staying_alive_test()
{
    pthread_t thread_id;
    int rc = pthread_create(
        &thread_id, nullptr, [](void*) -> void* {
            outln("I'm the secondary thread :^)");
            sleep(20);
            outln("Secondary thread is still alive");
            sleep(3520);
            outln("Secondary thread exiting");
            pthread_exit((void*)0xDEADBEEF);
        },
        nullptr);
    if (rc < 0) {
        perror("pthread_create");
        return 1;
    }

    sleep(1);
    outln("I'm the main thread :^)");
    sleep(3600);

    outln("Main thread exiting");
    return 0;
}

int set_stack_test()
{
    pthread_attr_t attributes;
    int rc = pthread_attr_init(&attributes);
    if (rc < 0) {
        outln("pthread_attr_init: {}", strerror(rc));
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
        outln("pthread_attr_setstack: {}", strerror(rc));
        return 2;
    }
    outln("Set thread stack to {:p}, size {}", stack_addr, stack_size);

    size_t stack_size_verify;
    void* stack_addr_verify;

    rc = pthread_attr_getstack(&attributes, &stack_addr_verify, &stack_size_verify);
    if (rc != 0) {
        outln("pthread_attr_getstack: {}", strerror(rc));
        return 3;
    }

    if (stack_addr != stack_addr_verify || stack_size != stack_size_verify) {
        outln("Stack address and size don't match! addr: {:p} {:p}, size: {} {}", stack_addr, stack_addr_verify, stack_size, stack_size_verify);
        return 4;
    }

    pthread_t thread_id;
    rc = pthread_create(
        &thread_id, &attributes, [](void*) -> void* {
            outln("I'm the secondary thread :^)");
            sleep(1);
            pthread_exit((void*)0xDEADBEEF);
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
        outln("pthread_attr_destroy: {}", strerror(rc));
        return 7;
    }

    return 0;
}

int kill_test()
{
    pthread_t thread_id;
    int rc = pthread_create(
        &thread_id, nullptr, [](void*) -> void* {
            outln("I'm the secondary thread :^)");
            sleep(100);
            outln("Secondary thread is still alive :^(");
            pthread_exit((void*)0xDEADBEEF);
        },
        nullptr);
    if (rc < 0) {
        perror("pthread_create");
        return 1;
    }

    int result = 0;

    sleep(1);
    outln("I'm the main thread :^)");
    if (pthread_kill(thread_id, 0) != 0) {
        perror("pthread_kill");
        result = 1;
    }

    if (pthread_kill(thread_id, SIGKILL) != 0) {
        perror("pthread_kill(SIGKILL)");
        result = 1;
    }

    outln("Main thread exiting");
    return result;
}
