/*
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/* posix_spawn and friends
 *
 * values from POSIX standard unix specification
 *
 * https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/spawn.h.html
 */

#include <spawn.h>

#include <AK/Function.h>
#include <AK/Vector.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

struct posix_spawn_file_actions_state {
    Vector<Function<int()>, 4> actions;
};

extern "C" {

[[noreturn]] static void posix_spawn_child(const char* path, const posix_spawn_file_actions_t* file_actions, const posix_spawnattr_t* attr, char* const argv[], char* const envp[], int (*exec)(const char*, char* const[], char* const[]))
{
    if (attr) {
        short flags = attr->flags;
        if (flags & POSIX_SPAWN_RESETIDS) {
            if (seteuid(getuid()) < 0) {
                perror("posix_spawn seteuid");
                _exit(127);
            }
            if (setegid(getgid()) < 0) {
                perror("posix_spawn setegid");
                _exit(127);
            }
        }
        if (flags & POSIX_SPAWN_SETPGROUP) {
            if (setpgid(0, attr->pgroup) < 0) {
                perror("posix_spawn setpgid");
                _exit(127);
            }
        }
        if (flags & POSIX_SPAWN_SETSCHEDPARAM) {
            if (sched_setparam(0, &attr->schedparam) < 0) {
                perror("posix_spawn sched_setparam");
                _exit(127);
            }
        }
        if (flags & POSIX_SPAWN_SETSIGDEF) {
            struct sigaction default_action;
            default_action.sa_flags = 0;
            sigemptyset(&default_action.sa_mask);
            default_action.sa_handler = SIG_DFL;

            sigset_t sigdefault = attr->sigdefault;
            for (int i = 0; i < NSIG; ++i) {
                if (sigismember(&sigdefault, i) && sigaction(i, &default_action, nullptr) < 0) {
                    perror("posix_spawn sigaction");
                    _exit(127);
                }
            }
        }
        if (flags & POSIX_SPAWN_SETSIGMASK) {
            if (sigprocmask(SIG_SETMASK, &attr->sigmask, nullptr) < 0) {
                perror("posix_spawn sigprocmask");
                _exit(127);
            }
        }
        if (flags & POSIX_SPAWN_SETSID) {
            if (setsid() < 0) {
                perror("posix_spawn setsid");
                _exit(127);
            }
        }

        // FIXME: POSIX_SPAWN_SETSCHEDULER
    }

    if (file_actions) {
        for (const auto& action : file_actions->state->actions) {
            if (action() < 0) {
                perror("posix_spawn file action");
                _exit(127);
            }
        }
    }

    exec(path, argv, envp);
    perror("posix_spawn exec");
    _exit(127);
}

int posix_spawn(pid_t* out_pid, const char* path, const posix_spawn_file_actions_t* file_actions, const posix_spawnattr_t* attr, char* const argv[], char* const envp[])
{
    pid_t child_pid = fork();
    if (child_pid < 0)
        return errno;

    if (child_pid != 0) {
        *out_pid = child_pid;
        return 0;
    }

    posix_spawn_child(path, file_actions, attr, argv, envp, execve);
}

int posix_spawnp(pid_t* out_pid, const char* path, const posix_spawn_file_actions_t* file_actions, const posix_spawnattr_t* attr, char* const argv[], char* const envp[])
{
    pid_t child_pid = fork();
    if (child_pid < 0)
        return errno;

    if (child_pid != 0) {
        *out_pid = child_pid;
        return 0;
    }

    posix_spawn_child(path, file_actions, attr, argv, envp, execvpe);
}

int posix_spawn_file_actions_addchdir(posix_spawn_file_actions_t* actions, const char* path)
{
    actions->state->actions.append([path]() { return chdir(path); });
    return 0;
}

int posix_spawn_file_actions_addfchdir(posix_spawn_file_actions_t* actions, int fd)
{
    actions->state->actions.append([fd]() { return fchdir(fd); });
    return 0;
}

int posix_spawn_file_actions_addclose(posix_spawn_file_actions_t* actions, int fd)
{
    actions->state->actions.append([fd]() { return close(fd); });
    return 0;
}

int posix_spawn_file_actions_adddup2(posix_spawn_file_actions_t* actions, int old_fd, int new_fd)
{
    actions->state->actions.append([old_fd, new_fd]() { return dup2(old_fd, new_fd); });
    return 0;
}

int posix_spawn_file_actions_addopen(posix_spawn_file_actions_t* actions, int want_fd, const char* path, int flags, mode_t mode)
{
    actions->state->actions.append([want_fd, path, flags, mode]() {
        int opened_fd = open(path, flags, mode);
        if (opened_fd < 0 || opened_fd == want_fd)
            return opened_fd;
        if (int rc = dup2(opened_fd, want_fd); rc < 0)
            return rc;
        return close(opened_fd);
    });
    return 0;
}

int posix_spawn_file_actions_destroy(posix_spawn_file_actions_t* actions)
{
    delete actions->state;
    return 0;
}

int posix_spawn_file_actions_init(posix_spawn_file_actions_t* actions)
{
    actions->state = new posix_spawn_file_actions_state;
    return 0;
}

int posix_spawnattr_destroy(posix_spawnattr_t*)
{
    return 0;
}

int posix_spawnattr_getflags(const posix_spawnattr_t* attr, short* out_flags)
{
    *out_flags = attr->flags;
    return 0;
}

int posix_spawnattr_getpgroup(const posix_spawnattr_t* attr, pid_t* out_pgroup)
{
    *out_pgroup = attr->pgroup;
    return 0;
}

int posix_spawnattr_getschedparam(const posix_spawnattr_t* attr, struct sched_param* out_schedparam)
{
    *out_schedparam = attr->schedparam;
    return 0;
}

int posix_spawnattr_getschedpolicy(const posix_spawnattr_t* attr, int* out_schedpolicty)
{
    *out_schedpolicty = attr->schedpolicy;
    return 0;
}

int posix_spawnattr_getsigdefault(const posix_spawnattr_t* attr, sigset_t* out_sigdefault)
{
    *out_sigdefault = attr->sigdefault;
    return 0;
}

int posix_spawnattr_getsigmask(const posix_spawnattr_t* attr, sigset_t* out_sigmask)
{
    *out_sigmask = attr->sigmask;
    return 0;
}

int posix_spawnattr_init(posix_spawnattr_t* attr)
{
    attr->flags = 0;
    attr->pgroup = 0;
    // attr->schedparam intentionally not written; its default value is unspecified.
    // attr->schedpolicy intentionally not written; its default value is unspecified.
    sigemptyset(&attr->sigdefault);
    // attr->sigmask intentionally not written; its default value is unspecified.
    return 0;
}

int posix_spawnattr_setflags(posix_spawnattr_t* attr, short flags)
{
    if (flags & ~(POSIX_SPAWN_RESETIDS | POSIX_SPAWN_SETPGROUP | POSIX_SPAWN_SETSCHEDPARAM | POSIX_SPAWN_SETSCHEDULER | POSIX_SPAWN_SETSIGDEF | POSIX_SPAWN_SETSIGMASK | POSIX_SPAWN_SETSID))
        return EINVAL;

    attr->flags = flags;
    return 0;
}

int posix_spawnattr_setpgroup(posix_spawnattr_t* attr, pid_t pgroup)
{
    attr->pgroup = pgroup;
    return 0;
}

int posix_spawnattr_setschedparam(posix_spawnattr_t* attr, const struct sched_param* schedparam)
{
    attr->schedparam = *schedparam;
    return 0;
}

int posix_spawnattr_setschedpolicy(posix_spawnattr_t* attr, int schedpolicy)
{
    attr->schedpolicy = schedpolicy;
    return 0;
}

int posix_spawnattr_setsigdefault(posix_spawnattr_t* attr, const sigset_t* sigdefault)
{
    attr->sigdefault = *sigdefault;
    return 0;
}

int posix_spawnattr_setsigmask(posix_spawnattr_t* attr, const sigset_t* sigmask)
{
    attr->sigmask = *sigmask;
    return 0;
}
}
