/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtrVector.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$fstat(int fd, Userspace<stat*> user_statbuf)
{
    REQUIRE_PROMISE(stdio);
    auto description = file_description(fd);
    if (!description)
        return EBADF;
    stat buffer = {};
    int rc = description->stat(buffer);
    if (!copy_to_user(user_statbuf, &buffer))
        return EFAULT;
    return rc;
}

KResultOr<int> Process::sys$stat(Userspace<const Syscall::SC_stat_params*> user_params)
{
    REQUIRE_PROMISE(rpath);
    Syscall::SC_stat_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
    RefPtr<Custody> base;
    if (params.dirfd == AT_FDCWD) {
        base = current_directory();
    } else {
        auto base_description = file_description(params.dirfd);
        if (!base_description)
            return EBADF;
        if (!base_description->is_directory())
            return ENOTDIR;
        if (!base_description->custody())
            return EINVAL;
        base = base_description->custody();
    }
    auto metadata_or_error = VFS::the().lookup_metadata(path.value()->view(), *base, params.follow_symlinks ? 0 : O_NOFOLLOW_NOERROR);
    if (metadata_or_error.is_error())
        return metadata_or_error.error();
    stat statbuf;
    auto result = metadata_or_error.value().stat(statbuf);
    if (result.is_error())
        return result;
    if (!copy_to_user(params.statbuf, &statbuf))
        return EFAULT;
    return 0;
}

}
