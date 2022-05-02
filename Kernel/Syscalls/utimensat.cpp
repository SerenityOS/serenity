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

    int dirfd = params.dirfd;
    auto path = TRY(get_syscall_path_argument(params.path));

    RefPtr<Custody> base;
    if (dirfd == AT_FDCWD) {
        base = current_directory();
    } else {
        auto base_description = TRY(open_file_description(dirfd));
        if (!KLexicalPath::is_absolute(path->view()) && !base_description->is_directory())
            return ENOTDIR;
        if (!base_description->custody())
            return EINVAL;
        base = base_description->custody();
    }

    auto& atime = times[0];
    auto& mtime = times[1];
    int follow_symlink = params.flag & AT_SYMLINK_NOFOLLOW ? O_NOFOLLOW_NOERROR : 0;
    TRY(VirtualFileSystem::the().utimensat(path->view(), *base, atime, mtime, follow_symlink));
    return 0;
}

}
