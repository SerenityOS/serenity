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

#include <AK/NonnullRefPtrVector.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

int Process::sys$fstat(int fd, stat* user_statbuf)
{
    REQUIRE_PROMISE(stdio);
    if (!validate_write_typed(user_statbuf))
        return -EFAULT;
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    stat buffer;
    memset(&buffer, 0, sizeof(buffer));
    int rc = description->fstat(buffer);
    copy_to_user(user_statbuf, &buffer);
    return rc;
}

int Process::sys$stat(const Syscall::SC_stat_params* user_params)
{
    REQUIRE_PROMISE(rpath);
    Syscall::SC_stat_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;
    if (!validate_write_typed(params.statbuf))
        return -EFAULT;
    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
    auto metadata_or_error = VFS::the().lookup_metadata(path.value(), current_directory(), params.follow_symlinks ? 0 : O_NOFOLLOW_NOERROR);
    if (metadata_or_error.is_error())
        return metadata_or_error.error();
    stat statbuf;
    auto result = metadata_or_error.value().stat(statbuf);
    if (result.is_error())
        return result;
    copy_to_user(params.statbuf, &statbuf);
    return 0;
}

}
