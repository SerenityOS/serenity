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

#include <AK/Assertions.h>
#include <AK/LogStream.h>
#include <LibPthread/pthread.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>

/*
 * Bug:
 * If the main thread of a process is no longer alive, it cannot receive
 * signals anymore. This can manifest as, for example, an unkillable process.
 *
 * So what needs to happen:
 * - There is process P
 * - It has more than one thread
 * - The main thread calls thread_exit(), leaving the rest of the threads alive
 * - Now the process is unkillable!
 *
 * Here's how to demonstrate the bug:
 * - Time 0: PX forks into PZ (mnemonic: Zombie)
 * - Time 1: PZ's main thread T1 creates a new thread T2
 * - Time 2: Nothing (T2 could communicate to PX both process and thread ID)
 *      (most LibC functions crash currently, which is a different bug I suppose.)
 * - Time 3: T1 calls thread_exit()
 * - Time 4:
 *      * PX tries to kill PZ (should work, but doesn't)
 *      * PX tries to kill PZ using T2's thread ID (shouldn't work, and doesn't)
 *      * PX outputs all results.
 */

static constexpr useconds_t STEP_SIZE = 1100000;

static void fork_into(void(fn)())
{
    const pid_t rc = fork();
    if (rc < 0) {
        perror("fork");
        exit(1);
    }
    if (rc > 0) {
        return;
    }
    fn();
    dbg() << "child finished (?)";
    exit(1);
}

static void thread_into(void* (*fn)(void*))
{
    pthread_t tid;
    const int rc = pthread_create(&tid, nullptr, fn, nullptr);
    if (rc < 0) {
        perror("pthread_create");
        exit(1);
    }
}

static void sleep_steps(useconds_t steps)
{
    const int rc = usleep(steps * STEP_SIZE);
    if (rc < 0) {
        perror("usleep");
        ASSERT_NOT_REACHED();
    }
}

static bool try_kill(pid_t kill_id)
{
    int rc = kill(kill_id, SIGTERM);
    perror("kill");
    printf("kill rc: %d\n", rc);
    return rc >= 0;
}

static void run_pz();
static void* run_pz_t2_wrap(void* fd_ptr);
static void run_pz_t2();

int main(int, char**)
{
    // This entire function is the entirety of process PX.

    // Time 0: PX forks into PZ (mnemonic: Zombie)
    dbg() << "PX forks into PZ";
    fork_into(run_pz);
    sleep_steps(4);

    // Time 4:
    dbg() << "Let's hope everything went fine!";
    pid_t guessed_pid = getpid() + 1;
    pid_t guessed_tid = guessed_pid + 1;
    printf("About to kill PID %d, TID %d.\n", guessed_pid, guessed_tid);
    if (try_kill(guessed_tid)) {
        printf("FAIL, could kill a thread\n");
        exit(1);
    }
    if (!try_kill(guessed_pid)) {
        printf("FAIL, could not kill the process\n");
        exit(1);
    }

    printf("PASS\n");
    return 0;
}

static void run_pz()
{
    // Time 0: PX forks into PZ (mnemonic: Zombie)
    sleep_steps(1);

    // Time 1: PZ's main thread T1 creates a new thread T2
    dbg() << "PZ calls pthread_create";
    thread_into(run_pz_t2_wrap);
    sleep_steps(2);

    // Time 3: T1 calls thread_exit()
    dbg() << "PZ(T1) calls thread_exit";
    pthread_exit(nullptr);
    ASSERT_NOT_REACHED();
}

static void* run_pz_t2_wrap(void*)
{
    run_pz_t2();
    exit(1);
}

static void run_pz_t2()
{
    // Time 1: PZ's main thread T1 creates a new thread T2
    sleep_steps(1);

    // Time 2: Nothing
    // FIXME: For some reason, both printf() and dbg() crash.
    // This also prevents us from using a pipe to communicate to PX both process and thread ID
    // dbg() << "T2: I'm alive and well.";
    sleep_steps(18);

    // Time 20: Cleanup
    printf("PZ(T2) dies from boredom.\n");
    exit(0);
}
