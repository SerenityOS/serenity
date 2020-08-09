/*
 * Copyright (c) 2020, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
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

#include <assert.h>
#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>

static void signal_printer(int)
{
    // no-op
}

typedef struct yank_shared_t {
    timespec* remaining_sleep;
    // TODO: Be nice and use thread ID
    //pthread_t sleeper_thread;
} yank_shared_t;

static void* yanker_fn(void* shared_)
{
    yank_shared_t* shared = static_cast<yank_shared_t*>(shared_);

    timespec requested_sleep = { 1, 0 };
    int rc = clock_nanosleep(CLOCK_MONOTONIC, 0, &requested_sleep, nullptr);
    if (rc != 0) {
        printf("Yanker: Failed during sleep: %d\n", rc);
        return nullptr;
    }

    delete shared->remaining_sleep; // T2

    // Send SIGUSR1.

    // Use pthread:
    // pthread_kill(somewhere, SIGUSR1);
    // But wait!  pthread_kill isn't implemented yet, and therefore causes
    // a linker error.  It also looks like the corresponding syscall is missing.

    // Use normal IPC syscall:
    // kill(getpid(), SIGUSR1);
    // But wait!  If destination_pid == own_pid, then the signal is sent
    // to the calling thread, *no matter what*.

    // So, we have to go the very ugly route of fork():
    // (Thank goodness this is only a demo of a kernel bug!)
    pid_t pid_to_kill = getpid();

    pid_t child_pid = fork();
    if (child_pid < 0) {
        printf("Yanker: Fork failed: %d\n", child_pid);
        pthread_exit(nullptr); // See below
        return nullptr;
    }

    if (child_pid > 0) {
        // Success.  Terminate quickly.  T3
        // FIXME: LibPthread bug: returning during normal operation causes nullptr deref.
        // Workaround: Exit manually.
        pthread_exit(nullptr);
        return nullptr;
    }

    // Give parent *thread* a moment to die.
    requested_sleep = { 1, 0 };
    rc = clock_nanosleep(CLOCK_MONOTONIC, 0, &requested_sleep, nullptr);
    if (rc != 0) {
        printf("Yanker-child: Failed during sleep: %d\n", rc);
        return nullptr;
    }

    // Prod the parent *process*
    kill(pid_to_kill, SIGUSR1); // T4

    // Wait a moment, to ensure the log output is as well-separated as possible.
    requested_sleep = { 2, 0 };
    rc = clock_nanosleep(CLOCK_MONOTONIC, 0, &requested_sleep, nullptr);
    if (rc != 0) {
        printf("Yanker-child: Failed during after-sleep: %d\n", rc);
        return nullptr;
    }

    pthread_exit(nullptr);
    assert(false);
    // FIXME: return nullptr;
}

int main()
{
    // Chronological order:
    // T0: Main thread allocates region for the outvalue of clock_nanosleep
    // T1: Main thread enters clock_nanosleep
    // T2: Side thread deallocates that region
    // T3: Side thread dies
    // T4: A different process sends SIGUSR1, waking up the main thread,
    //     forcing the kernel to write to the deallocated Region.

    // I'm sorry that both a side *thread* and a side *process* are necessary.
    // Maybe in the future this test can be simplified, see above.

    yank_shared_t shared = { nullptr };
    shared.remaining_sleep = new timespec({ 0xbad, 0xf00d }); // T0

    pthread_t yanker_thread;
    int rc = pthread_create(&yanker_thread, nullptr, yanker_fn, &shared);
    if (rc != 0) {
        perror("pthread");
        printf("FAIL\n");
        return 1;
    }

    // Set an action for SIGUSR1, so that the sleep can be interrupted:
    signal(SIGUSR1, signal_printer);

    // T1: Go to sleep.
    const timespec requested_sleep = { 3, 0 };
    rc = clock_nanosleep(CLOCK_MONOTONIC, 0, &requested_sleep, shared.remaining_sleep);
    // Now we are beyond T4.

    if (rc == 0) {
        // We somehow weren't interrupted.  Bad.
        printf("Not interrupted.\n");
        printf("FAIL\n");
        return 1;
    }

    // nanosleep was interrupted and the kernel didn't crash.  Good!
    printf("PASS\n");
    return 0;
}
