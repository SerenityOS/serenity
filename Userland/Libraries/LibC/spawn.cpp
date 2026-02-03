/*
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

/* posix_spawn and friends
 *
 * values from POSIX standard unix specification
 *
 * https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/spawn.h.html
 */

#include <spawn.h>

#include <AK/ByteBuffer.h>
#include <AK/Vector.h>
#include <Kernel/API/spawn.h>
#include <LibFileSystem/FileSystem.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <syscall.h>
#include <unistd.h>

struct posix_spawn_file_actions_state {
    ByteBuffer buffer;
    u8 action_types_present { 0 };
};

extern "C" {

[[noreturn]] static void posix_spawn_child(char const* path, posix_spawn_file_actions_t const* file_actions, posix_spawnattr_t const* attr, char* const argv[], char* const envp[], int (*exec)(char const*, char* const[], char* const[]))
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

    if (file_actions && !file_actions->state->buffer.is_empty()) {
        using namespace Kernel;
        auto const& buffer = file_actions->state->buffer;
        size_t offset = 0;

        while (offset < buffer.size()) {
            if (offset + sizeof(SpawnFileActionHeader) > buffer.size())
                _exit(127);

            auto const* header = reinterpret_cast<SpawnFileActionHeader const*>(buffer.data() + offset);
            if (header->record_length < sizeof(SpawnFileActionHeader) || offset + header->record_length > buffer.size())
                _exit(127);

            switch (header->type) {
            case SpawnFileActionType::Dup2: {
                auto const* action = reinterpret_cast<SpawnFileActionDup2 const*>(header);
                if (dup2(action->old_fd, action->new_fd) < 0) {
                    perror("posix_spawn dup2");
                    _exit(127);
                }
                break;
            }
            case SpawnFileActionType::Close: {
                auto const* action = reinterpret_cast<SpawnFileActionClose const*>(header);
                if (close(action->fd) < 0) {
                    perror("posix_spawn close");
                    _exit(127);
                }
                break;
            }
            case SpawnFileActionType::Open: {
                auto const* action = reinterpret_cast<SpawnFileActionOpen const*>(header);
                int opened_fd = open(action->path, action->flags, action->mode);
                if (opened_fd < 0) {
                    perror("posix_spawn open");
                    _exit(127);
                }
                if (opened_fd != action->fd) {
                    if (dup2(opened_fd, action->fd) < 0) {
                        perror("posix_spawn dup2 after open");
                        _exit(127);
                    }
                    close(opened_fd);
                }
                break;
            }
            case SpawnFileActionType::Chdir: {
                auto const* action = reinterpret_cast<SpawnFileActionChdir const*>(header);
                if (chdir(action->path) < 0) {
                    perror("posix_spawn chdir");
                    _exit(127);
                }
                break;
            }
            case SpawnFileActionType::Fchdir: {
                auto const* action = reinterpret_cast<SpawnFileActionFchdir const*>(header);
                if (fchdir(action->fd) < 0) {
                    perror("posix_spawn fchdir");
                    _exit(127);
                }
                break;
            }
            default:
                _exit(127);
            }

            offset += header->record_length;
        }
    }

    exec(path, argv, envp);
    perror("posix_spawn exec");
    _exit(127);
}

static ErrorOr<pid_t> posix_spawn_syscall(char const* path, posix_spawn_file_actions_t const* file_actions, char* const argv[], char* const envp[])
{
    if (!path || !argv || !argv[0])
        return EINVAL;

    Syscall::SC_posix_spawn_params posix_spawn_params;

    posix_spawn_params.path.characters = path;
    posix_spawn_params.path.length = strlen(path);

    Vector<Syscall::StringArgument, 32> argv_string_args;
    for (size_t argc = 0; argv[argc] != nullptr; ++argc)
        TRY(argv_string_args.try_append({ argv[argc], strlen(argv[argc]) }));

    posix_spawn_params.arguments.strings = argv_string_args.is_empty() ? nullptr : argv_string_args.data();
    posix_spawn_params.arguments.length = argv_string_args.size();

    Vector<Syscall::StringArgument, 32> envp_string_args;
    if (envp) {
        for (size_t envc = 0; envp[envc] != nullptr; ++envc)
            TRY(envp_string_args.try_append({ envp[envc], strlen(envp[envc]) }));
    }

    posix_spawn_params.environment.strings = envp_string_args.is_empty() ? nullptr : envp_string_args.data();
    posix_spawn_params.environment.length = envp_string_args.size();

    posix_spawn_params.attr_data = nullptr;
    posix_spawn_params.attr_data_size = 0;

    if (file_actions && !file_actions->state->buffer.is_empty()) {
        posix_spawn_params.serialized_file_actions_data = file_actions->state->buffer.data();
        posix_spawn_params.serialized_file_actions_data_size = file_actions->state->buffer.size();
        posix_spawn_params.file_action_types_present = file_actions->state->action_types_present;
    } else {
        posix_spawn_params.serialized_file_actions_data = nullptr;
        posix_spawn_params.serialized_file_actions_data_size = 0;
        posix_spawn_params.file_action_types_present = 0;
    }

    pid_t rc = syscall(SC_posix_spawn, &posix_spawn_params);

    if (rc < 0)
        return Error::from_syscall("posix_spawn"sv, rc);

    return rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawn.html
int posix_spawn(pid_t* out_pid, char const* path, posix_spawn_file_actions_t const* file_actions, posix_spawnattr_t const* attr, char* const argv[], char* const envp[])
{
    // FIXME: Support spawnattr in the posix_spawn syscall.
    if (attr) {
        pid_t child_pid = fork();
        if (child_pid < 0)
            return errno;

        if (child_pid != 0) {
            *out_pid = child_pid;
            return 0;
        }

        posix_spawn_child(path, file_actions, attr, argv, envp, execve);
    }

    auto child_pid_or_error = posix_spawn_syscall(path, file_actions, argv, envp);
    if (child_pid_or_error.is_error()) {
        // ENOTSUP means kernel doesn't support these file actions yet, fall back to fork()
        if (child_pid_or_error.error().code() == ENOTSUP) {
            pid_t child_pid = fork();
            if (child_pid < 0)
                return errno;

            if (child_pid != 0) {
                if (out_pid)
                    *out_pid = child_pid;
                return 0;
            }

            posix_spawn_child(path, file_actions, attr, argv, envp, execve);
        }
        return child_pid_or_error.error().code();
    }

    if (out_pid)
        *out_pid = child_pid_or_error.value();
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnp.html
int posix_spawnp(pid_t* out_pid, char const* file, posix_spawn_file_actions_t const* file_actions, posix_spawnattr_t const* attr, char* const argv[], char* const envp[])
{
    if (strchr(file, '/') != nullptr)
        return posix_spawn(out_pid, file, file_actions, attr, argv, envp);

    // FIXME: Support spawnattr in the posix_spawn syscall.
    if (attr) {
        // Fall back to fork() for spawnattr (not yet implemented in kernel)
        pid_t child_pid = fork();
        if (child_pid < 0)
            return errno;

        if (child_pid != 0) {
            *out_pid = child_pid;
            return 0;
        }

        posix_spawn_child(file, file_actions, attr, argv, envp, execvpe);
    }

    // Use posix_spawn which handles the syscall path
    // FIXME: This is currently not OOM-safe because ByteString does not handle OOMs!
    ByteString path = getenv("PATH");
    if (path.is_empty())
        path = DEFAULT_PATH;

    int rc = ENOENT;

    path.view().for_each_split_view(":"sv, SplitBehavior::Nothing, [out_pid, file, file_actions, attr, argv, envp, &rc](auto const directory) -> IterationDecision {
        auto absolute_path = ByteString::formatted("{}/{}", directory, file);

        rc = posix_spawn(out_pid, absolute_path.characters(), file_actions, attr, argv, envp);
        if (rc == ENOENT)
            return IterationDecision::Continue;
        return IterationDecision::Break;
    });

    return rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawn_file_actions_addchdir.html
int posix_spawn_file_actions_addchdir(posix_spawn_file_actions_t* actions, char const* path)
{
    using namespace Kernel;
    size_t path_len = strlen(path);
    size_t record_size = sizeof(SpawnFileActionChdir) + path_len + 1;

    record_size = align_up_to(record_size, SPAWN_FILE_ACTION_ALIGNMENT);

    auto buffer_or_error = ByteBuffer::create_uninitialized(record_size);
    if (buffer_or_error.is_error())
        return ENOMEM;
    auto record_buffer = buffer_or_error.release_value();

    auto* action = reinterpret_cast<SpawnFileActionChdir*>(record_buffer.data());
    action->header.type = SpawnFileActionType::Chdir;
    action->header.record_length = record_size;
    action->path_length = path_len;
    memcpy(action->path, path, path_len + 1);

    if (actions->state->buffer.try_append(record_buffer.data(), record_size).is_error())
        return ENOMEM;
    actions->state->action_types_present |= (1 << static_cast<u8>(SpawnFileActionType::Chdir));
    return 0;
}

int posix_spawn_file_actions_addfchdir(posix_spawn_file_actions_t* actions, int fd)
{
    using namespace Kernel;
    SpawnFileActionFchdir action;
    action.header.type = SpawnFileActionType::Fchdir;
    action.header.record_length = sizeof(SpawnFileActionFchdir);
    action.fd = fd;
    if (actions->state->buffer.try_append(&action, sizeof(action)).is_error())
        return ENOMEM;
    actions->state->action_types_present |= (1 << static_cast<u8>(SpawnFileActionType::Fchdir));
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawn_file_actions_addclose.html
int posix_spawn_file_actions_addclose(posix_spawn_file_actions_t* actions, int fd)
{
    using namespace Kernel;
    SpawnFileActionClose action;
    action.header.type = SpawnFileActionType::Close;
    action.header.record_length = sizeof(SpawnFileActionClose);
    action.fd = fd;
    if (actions->state->buffer.try_append(&action, sizeof(action)).is_error())
        return ENOMEM;
    actions->state->action_types_present |= (1 << static_cast<u8>(SpawnFileActionType::Close));
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawn_file_actions_adddup2.html
int posix_spawn_file_actions_adddup2(posix_spawn_file_actions_t* actions, int old_fd, int new_fd)
{
    using namespace Kernel;
    SpawnFileActionDup2 action;
    action.header.type = SpawnFileActionType::Dup2;
    action.header.record_length = sizeof(SpawnFileActionDup2);
    action.old_fd = old_fd;
    action.new_fd = new_fd;
    if (actions->state->buffer.try_append(&action, sizeof(action)).is_error())
        return ENOMEM;
    actions->state->action_types_present |= (1 << static_cast<u8>(SpawnFileActionType::Dup2));
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawn_file_actions_addopen.html
int posix_spawn_file_actions_addopen(posix_spawn_file_actions_t* actions, int want_fd, char const* path, int flags, mode_t mode)
{
    using namespace Kernel;
    size_t path_len = strlen(path);
    size_t record_size = sizeof(SpawnFileActionOpen) + path_len + 1;

    record_size = align_up_to(record_size, SPAWN_FILE_ACTION_ALIGNMENT);

    auto buffer_or_error = ByteBuffer::create_uninitialized(record_size);
    if (buffer_or_error.is_error())
        return ENOMEM;
    auto record_buffer = buffer_or_error.release_value();

    auto* action = reinterpret_cast<SpawnFileActionOpen*>(record_buffer.data());
    action->header.type = SpawnFileActionType::Open;
    action->header.record_length = record_size;
    action->fd = want_fd;
    action->flags = flags;
    action->mode = mode;
    action->path_length = path_len;
    memcpy(action->path, path, path_len + 1);

    if (actions->state->buffer.try_append(record_buffer.data(), record_size).is_error())
        return ENOMEM;
    actions->state->action_types_present |= (1 << static_cast<u8>(SpawnFileActionType::Open));
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawn_file_actions_destroy.html
int posix_spawn_file_actions_destroy(posix_spawn_file_actions_t* actions)
{
    delete actions->state;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawn_file_actions_init.html
int posix_spawn_file_actions_init(posix_spawn_file_actions_t* actions)
{
    actions->state = new posix_spawn_file_actions_state;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_destroy.html
int posix_spawnattr_destroy(posix_spawnattr_t*)
{
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_getflags.html
int posix_spawnattr_getflags(posix_spawnattr_t const* attr, short* out_flags)
{
    *out_flags = attr->flags;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_getpgroup.html
int posix_spawnattr_getpgroup(posix_spawnattr_t const* attr, pid_t* out_pgroup)
{
    *out_pgroup = attr->pgroup;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_getschedparam.html
int posix_spawnattr_getschedparam(posix_spawnattr_t const* attr, struct sched_param* out_schedparam)
{
    *out_schedparam = attr->schedparam;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_getschedpolicy.html
int posix_spawnattr_getschedpolicy(posix_spawnattr_t const* attr, int* out_schedpolicy)
{
    *out_schedpolicy = attr->schedpolicy;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_getsigdefault.html
int posix_spawnattr_getsigdefault(posix_spawnattr_t const* attr, sigset_t* out_sigdefault)
{
    *out_sigdefault = attr->sigdefault;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_getsigmask.html
int posix_spawnattr_getsigmask(posix_spawnattr_t const* attr, sigset_t* out_sigmask)
{
    *out_sigmask = attr->sigmask;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_init.html
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_setflags.html
int posix_spawnattr_setflags(posix_spawnattr_t* attr, short flags)
{
    if (flags & ~(POSIX_SPAWN_RESETIDS | POSIX_SPAWN_SETPGROUP | POSIX_SPAWN_SETSCHEDPARAM | POSIX_SPAWN_SETSCHEDULER | POSIX_SPAWN_SETSIGDEF | POSIX_SPAWN_SETSIGMASK | POSIX_SPAWN_SETSID))
        return EINVAL;

    attr->flags = flags;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_setpgroup.html
int posix_spawnattr_setpgroup(posix_spawnattr_t* attr, pid_t pgroup)
{
    attr->pgroup = pgroup;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_setschedparam.html
int posix_spawnattr_setschedparam(posix_spawnattr_t* attr, const struct sched_param* schedparam)
{
    attr->schedparam = *schedparam;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_setschedpolicy.html
int posix_spawnattr_setschedpolicy(posix_spawnattr_t* attr, int schedpolicy)
{
    attr->schedpolicy = schedpolicy;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_setsigdefault.html
int posix_spawnattr_setsigdefault(posix_spawnattr_t* attr, sigset_t const* sigdefault)
{
    attr->sigdefault = *sigdefault;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_spawnattr_setsigmask.html
int posix_spawnattr_setsigmask(posix_spawnattr_t* attr, sigset_t const* sigmask)
{
    attr->sigmask = *sigmask;
    return 0;
}
}
