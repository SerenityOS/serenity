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

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

int Process::sys$open(Userspace<const Syscall::SC_open_params*> user_params)
{
    Syscall::SC_open_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    int dirfd = params.dirfd;
    int options = params.options;
    u16 mode = params.mode;

    if (options & O_NOFOLLOW_NOERROR)
        return -EINVAL;

    if (options & O_UNLINK_INTERNAL)
        return -EINVAL;

    if (options & O_WRONLY)
        REQUIRE_PROMISE(wpath);
    else if (options & O_RDONLY)
        REQUIRE_PROMISE(rpath);

    if (options & O_CREAT)
        REQUIRE_PROMISE(cpath);

    // Ignore everything except permission bits.
    mode &= 04777;

    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
#ifdef DEBUG_IO
    dbg() << "sys$open(dirfd=" << dirfd << ", path=\"" << path.value() << "\", options=" << options << ", mode=" << mode << ")";
#endif
    int fd = alloc_fd();
    if (fd < 0)
        return fd;

    RefPtr<Custody> base;
    if (dirfd == AT_FDCWD) {
        base = current_directory();
    } else {
        auto base_description = file_description(dirfd);
        if (!base_description)
            return -EBADF;
        if (!base_description->is_directory())
            return -ENOTDIR;
        if (!base_description->custody())
            return -EINVAL;
        base = base_description->custody();
    }

    auto result = VFS::the().open(path.value(), options, mode & ~umask(), *base);
    if (result.is_error())
        return result.error();
    auto description = result.value();

    if (description->inode() && description->inode()->socket())
        return -ENXIO;

    u32 fd_flags = (options & O_CLOEXEC) ? FD_CLOEXEC : 0;
    m_fds[fd].set(move(description), fd_flags);
    return fd;
}

int Process::sys$close(int fd)
{
    REQUIRE_PROMISE(stdio);
    auto description = file_description(fd);
#ifdef DEBUG_IO
    dbg() << "sys$close(" << fd << ") " << description.ptr();
#endif
    if (!description)
        return -EBADF;
    int rc = description->close();
    m_fds[fd] = {};
    return rc;
}

}
