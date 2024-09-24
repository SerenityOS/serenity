/*
 * Copyright (c) 2022, Ariel Don <ariel@arieldon.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/KLexicalPath.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$futimens(Userspace<Syscall::SC_futimens_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::fattr));

    auto params = TRY(copy_typed_from_user(user_params));
    auto now = kgettimeofday().to_timespec();

    timespec times[2];
    if (params.times) {
        TRY(copy_from_user(times, params.times, sizeof(times)));
        if (times[0].tv_nsec == UTIME_NOW)
            times[0] = now;
        if (times[1].tv_nsec == UTIME_NOW)
            times[1] = now;
    } else {
        // According to POSIX, both access and modification times are set to
        // the current time given a nullptr.
        times[0] = now;
        times[1] = now;
    }

    auto description = TRY(open_file_description(params.fd));
    if (!description->inode())
        return EBADF;
    if (!description->custody())
        return EBADF;

    auto& atime = times[0];
    auto& mtime = times[1];
    TRY(VirtualFileSystem::do_utimens(credentials(), *description->custody(), atime, mtime));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$utimensat(Userspace<Syscall::SC_utimensat_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::fattr));

    auto params = TRY(copy_typed_from_user(user_params));
    auto now = kgettimeofday().to_timespec();
    int follow_symlink = params.flag & AT_SYMLINK_NOFOLLOW ? O_NOFOLLOW_NOERROR : 0;

    timespec times[2];
    if (params.times) {
        TRY(copy_from_user(times, params.times, sizeof(times)));
        if (times[0].tv_nsec == UTIME_NOW)
            times[0] = now;
        if (times[1].tv_nsec == UTIME_NOW)
            times[1] = now;
    } else {
        // According to POSIX, both access and modification times are set to
        // the current time given a nullptr.
        times[0] = now;
        times[1] = now;
    }

    auto path = TRY(get_syscall_path_argument(params.path));
    auto& atime = times[0];
    auto& mtime = times[1];

    CustodyBase base(params.dirfd, path->view());
    TRY(VirtualFileSystem::utimensat(vfs_root_context(), credentials(), path->view(), base, atime, mtime, follow_symlink));
    return 0;
}

}
