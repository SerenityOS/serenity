/*
 * Copyright (c) 2022, Ariel Don <ariel@arieldon.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KLexicalPath.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$utimensat(Userspace<Syscall::SC_utimensat_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::fattr));

    auto params = TRY(copy_typed_from_user(user_params));
    auto now = kgettimeofday().to_truncated_seconds();

    int dirfd = params.dirfd;
    int follow_symlink = params.flag & AT_SYMLINK_NOFOLLOW ? O_NOFOLLOW_NOERROR : 0;

    timespec times[2];
    if (params.times) {
        TRY(copy_from_user(times, params.times, sizeof(times)));
        if (times[0].tv_nsec == UTIME_NOW)
            times[0].tv_sec = now;
        if (times[1].tv_nsec == UTIME_NOW)
            times[1].tv_sec = now;
    } else {
        // According to POSIX, both access and modification times are set to
        // the current time given a nullptr.
        times[0].tv_sec = now;
        times[0].tv_nsec = UTIME_NOW;
        times[1].tv_sec = now;
        times[1].tv_nsec = UTIME_NOW;
    }

    OwnPtr<KString> path;
    LockRefPtr<OpenFileDescription> description;
    RefPtr<Custody> base;

    auto path_or_error = get_syscall_path_argument(params.path);
    if (path_or_error.is_error()) {
        // If the path is empty ("") but not a nullptr, attempt to get a path
        // from the file descriptor. This allows futimens() to be implemented
        // in terms of utimensat().
        if (params.path.characters && params.path.length == 0) {
            description = TRY(open_file_description(dirfd));
            path = TRY(description->original_absolute_path());
            base = description->custody();
        } else {
            return path_or_error.release_error();
        }
    } else {
        path = path_or_error.release_value();
        if (dirfd == AT_FDCWD) {
            base = current_directory();
        } else {
            description = TRY(open_file_description(dirfd));
            if (!KLexicalPath::is_absolute(path->view()) && !description->is_directory())
                return ENOTDIR;
            if (!description->custody())
                return EINVAL;
            base = description->custody();
        }
    }

    auto& atime = times[0];
    auto& mtime = times[1];
    TRY(VirtualFileSystem::the().utimensat(credentials(), path->view(), *base, atime, mtime, follow_symlink));
    return 0;
}

}
