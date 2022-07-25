/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <assert.h>
#include <bits/pthread_cancel.h>
#include <errno.h>
#include <sys/wait.h>
#include <syscall.h>
#include <unistd.h>

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wait.html
pid_t wait(int* wstatus)
{
    return waitpid(-1, wstatus, 0);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/waitpid.html
pid_t waitpid(pid_t waitee, int* wstatus, int options)
{
    __pthread_maybe_cancel();

    siginfo_t siginfo;
    idtype_t idtype;
    id_t id;

    if (waitee < -1) {
        idtype = P_PGID;
        id = -waitee;
    } else if (waitee == -1) {
        idtype = P_ALL;
        id = 0;
    } else if (waitee == 0) {
        idtype = P_PGID;
        id = getgid();
    } else {
        idtype = P_PID;
        id = waitee;
    }

    // To be able to detect if a child was found when WNOHANG is set,
    // we need to clear si_pid, which will only be set if it was found.
    siginfo.si_pid = 0;
    int rc = waitid(idtype, id, &siginfo, options | WEXITED);

    if (rc < 0)
        return rc;

    if ((options & WNOHANG) && siginfo.si_pid == 0) {
        // No child in a waitable state was found. All other fields
        // in siginfo are undefined
        return 0;
    }

    if (wstatus) {
        switch (siginfo.si_code) {
        case CLD_EXITED:
            *wstatus = siginfo.si_status << 8;
            break;
        case CLD_KILLED:
            *wstatus = siginfo.si_status;
            break;
        case CLD_STOPPED:
            *wstatus = siginfo.si_status << 8 | 0x7f;
            break;
        case CLD_CONTINUED:
            *wstatus = 0xffff;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    return siginfo.si_pid;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/waitid.html
int waitid(idtype_t idtype, id_t id, siginfo_t* infop, int options)
{
    __pthread_maybe_cancel();

    Syscall::SC_waitid_params params { idtype, id, infop, options };
    int rc = syscall(SC_waitid, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
