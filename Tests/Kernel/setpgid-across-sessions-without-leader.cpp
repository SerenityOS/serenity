/*
 * Copyright (c) 2020, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <errno.h>
#include <fcntl.h>
#include <serenity.h>
#include <stdio.h>
#include <unistd.h>

/*
 * Bug:
 * A process can join a process group across sessions if both process groups
 * do not have a leader (anymore). This can be used to join a session
 * illegitimately. (Or, more harmlessly, to change the own PGID to an unused
 * but arbitrary one, for example the PGID 0xDEADBEEF or the one that's going
 * to be your program's session ID in the short-term future.)
 *
 * So what needs to happen:
 * - There is session SA
 * - There is session SB
 * - There is a Process Group PGA in SA
 * - There is a Process Group PGB in SB
 * - PGA does not have a leader
 * - PGB does not have a leader
 * - There is a Process PA2 in PGA
 * - There is a Process PB2 in PGB
 * - PA2 calls setpgid(0, PGB)
 * - Now PA2 and PB2 are in the same processgroup, but not in the same session. WHAAAAT! :^)
 *
 * Here's how to demonstrate the bug:
 * - Time 0: PX forks into PA1
 * - Time 1: PA1 creates a new session (SA) and pgrp (PGA)
 * - Time 2: PA1 forks into PA2
 * - Time 3: PA1 dies (PGA now has no leader)
 *     Note: PA2 never dies. Too much hassle.
 * - Time 4: PX forks into PB1
 * - Time 5: PB1 creates a new session (SB) and pgrp (PGB)
 * - Time 6: PB1 forks into PB2
 * - Time 7: PB1 dies (PGB now has no leader)
 * - Time 8: PB2 calls pgrp(0, PGA)
 *     Note: PB2 writes "1" (exploit successful) or "0" (bug is fixed) to a pipe
 * - Time 9: If PX hasn't received any message yet through the pipe, it declares the test as failed (for lack of knowledge). Otherwise, it outputs accordingly.
 */

static constexpr useconds_t STEP_SIZE = 1100000;

static void fork_into(void (*fn)(void*), void* arg)
{
    pid_t const rc = fork();
    if (rc < 0) {
        perror("fork");
        exit(1);
    }
    if (rc > 0) {
        int const disown_rc = disown(rc);
        if (disown_rc < 0) {
            perror("disown");
            dbgln("This might cause PA1 to remain in the Zombie state, "
                  "and thus in the process list, meaning the leader is "
                  "still 'alive' for the purpose of lookup.");
        }
        return;
    }
    fn(arg);
    dbgln("child finished (?)");
    exit(1);
}

static void sleep_steps(useconds_t steps)
{
    int const rc = usleep(steps * STEP_SIZE);
    if (rc < 0) {
        perror("usleep");
        VERIFY_NOT_REACHED();
    }
}

static void run_pa1(void*);
static void run_pa2(void*);
static void run_pb1(void*);
static void run_pb2(void*);

int main(int, char**)
{
    // This entire function is the entirety of process PX.

    // Time 0: PX forks into PA1
    int fds[2];
    // Serenity doesn't support O_NONBLOCK for pipes yet, so
    // sadly the test will hang if something goes wrong.
    if (pipe2(fds, 0) < 0) {
        perror("pipe");
        exit(1);
    }
    dbgln("PX starts with SID={}, PGID={}, PID={}.", getsid(0), getpgid(0), getpid());
    dbgln("PX forks into PA1");
    fork_into(run_pa1, nullptr);
    sleep_steps(4);

    // Time 4: PX forks into PB1
    dbgln("PX forks into PB1");
    fork_into(run_pb1, &(fds[1]));
    sleep_steps(5);

    // Time 9: If PX hasn't received any message yet through the pipe, it declares
    // the test as failed (for lack of knowledge). Otherwise, it outputs accordingly.
    dbgln("PX reads from pipe");
    unsigned char buf = 42;
    ssize_t rc = read(fds[0], &buf, 1);
    if (rc == 0) {
        // In fact, we only reach this branch when *all* processes have died,
        // including this one. So … should be unreachable.
        printf("DOUBLE FAIL: pipe is closed, but we still have it open.\n"
               "See debug log, some process probably crashed.\n");
        exit(1);
    }
    if (rc < 0) {
        if (errno == EAGAIN) {
            printf("FAIL: pipe has no data. See debug log, some process os probably hanging.\n");
        } else {
            perror("read (unknown)");
        }
        exit(1);
    }
    VERIFY(rc == 1);
    if (buf == 0) {
        printf("PASS\n");
        return 0;
    }
    if (buf == 1) {
        printf("FAIL (exploit successful)\n");
        return 1;
    }
    printf("FAIL, for some reason %c\n", buf);

    return 1;
}

static void run_pa1(void*)
{
    // Time 0: PX forks into PA1
    sleep_steps(1);

    // Time 1: PA1 creates a new session (SA) and pgrp (PGA)
    dbgln("PA1 starts with SID={}, PGID={}, PID={}.", getsid(0), getpgid(0), getpid());
    dbgln("PA1 calls setsid()");
    int rc = setsid();
    if (rc < 0) {
        perror("setsid (PA)");
        VERIFY_NOT_REACHED();
    }
    dbgln("PA1 did setsid() -> PGA={}, SA={}, yay!", rc, getsid(0));
    sleep_steps(1);

    // Time 2: PA1 forks into PA2
    dbgln("PA1 forks into PA2");
    fork_into(run_pa2, nullptr);
    sleep_steps(1);

    // Time 3: PA1 dies (PGA now has no leader)
    dbgln("PA1 dies. You should see a 'Reaped unparented process' "
          "message with my ID next, OR THIS TEST IS MEANINGLESS "
          "(see fork_into()).");
    exit(0);
}

static void run_pa2(void*)
{
    // Time 2: PA1 forks into PA2
    dbgln("PA2 starts with SID={}, PGID={}, PID={}.", getsid(0), getpgid(0), getpid());
    sleep_steps(18);

    // pa_2 never *does* anything.
    dbgln("PA2 dies from boredom.");
    exit(1);
}

static void run_pb1(void* pipe_fd_ptr)
{
    // Time 4: PX forks into PB1
    sleep_steps(1);

    // Time 5: PB1 creates a new session (SB) and pgrp (PGB)
    dbgln("PB1 starts with SID={}, PGID={}, PID={}.", getsid(0), getpgid(0), getpid());
    dbgln("PB1 calls setsid()");
    int rc = setsid();
    if (rc < 0) {
        perror("setsid (PB)");
        VERIFY_NOT_REACHED();
    }
    dbgln("PB1 did setsid() -> PGB={}, SB={}, yay!", rc, getsid(0));
    sleep_steps(1);

    // Time 6: PB1 forks into PB2
    dbgln("PB1 forks into PB2");
    fork_into(run_pb2, pipe_fd_ptr);
    sleep_steps(1);

    // Time 7: PB1 dies (PGB now has no leader)
    dbgln("PB1 dies. You should see a 'Reaped unparented process' "
          "message with my ID next, OR THIS TEST IS MEANINGLESS "
          "(see fork_into()).");
    exit(0);
}

static void simulate_sid_from_pgid(pid_t pgid)
{
    pid_t rc = getpgid(pgid); // Same confusion as in the Kernel
    int saved_errno = errno;
    if (rc < 0 && saved_errno == ESRCH) {
        dbgln("The old get_sid_from_pgid({}) would return -1", pgid);
    } else if (rc >= 0) {
        dbgln("FAIL: Process {} still exists?! PGID is {}.", pgid, rc);
    } else {
        perror("pgid (probably fail)");
    }
}

static void run_pb2(void* pipe_fd_ptr)
{
    // Time 6: PB1 forks into PB2
    sleep_steps(2);

    // Time 8: PB2 calls pgrp(0, PGA)
    //   Note: PB2 writes "1" (exploit successful) or "0" (bug is fixed) to a pipe
    dbgln("PB2 starts with SID={}, PGID={}, PID={}.", getsid(0), getpgid(0), getpid());
    dbgln("PB2 calls pgrp(0, PGA)");
    int pga = getpid() - 3;
    dbgln("PB2: Actually, what is PGA? I guess it's {}?", pga);
    simulate_sid_from_pgid(pga);
    int rc = setpgid(0, pga);
    unsigned char to_write = 123;
    if (rc == 0) {
        dbgln("PB2: setgpid SUCCESSFUL! CHANGED PGROUP!");
        to_write = 1;
    } else {
        VERIFY(rc == -1);
        switch (errno) {
        case EACCES:
            dbgln("PB2: Failed with EACCES. Huh?!");
            to_write = 101;
            break;
        case EINVAL:
            dbgln("PB2: Failed with EINVAL. Huh?!");
            to_write = 102;
            break;
        case ESRCH:
            dbgln("PB2: Failed with ESRCH. Huh?!");
            to_write = 103;
            break;
        case EPERM:
            dbgln("PB2: Failed with EPERM. Aww, no exploit today :^)");
            to_write = 0;
            break;
        default:
            dbgln("PB2: Failed with errno={}?!", errno);
            perror("setpgid");
            to_write = 104;
            break;
        }
    }

    dbgln("PB2 ends with SID={}, PGID={}, PID={}.", getsid(0), getpgid(0), getpid());
    int* pipe_fd = static_cast<int*>(pipe_fd_ptr);
    VERIFY(*pipe_fd);
    rc = write(*pipe_fd, &to_write, 1);
    if (rc != 1) {
        dbgln("Wrote only {} bytes instead of 1?!", rc);
        exit(1);
    }
    exit(0);
}
