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

#include <Kernel/API/Syscall.h>
#include <assert.h>
#include <sys/wait.h>
#include <unistd.h>

extern "C" {

pid_t wait(int* wstatus)
{
    return waitpid(-1, wstatus, 0);
}

pid_t waitpid(pid_t waitee, int* wstatus, int options)
{
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

    int rc = waitid(idtype, id, &siginfo, options | WEXITED);

    if (rc < 0)
        return rc;

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
            *wstatus = 0;
            return 0; // return 0 if running
        default:
            ASSERT_NOT_REACHED();
        }
    }

    return siginfo.si_pid;
}

int waitid(idtype_t idtype, id_t id, siginfo_t* infop, int options)
{
    Syscall::SC_waitid_params params { idtype, id, infop, options };
    int rc = syscall(SC_waitid, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
