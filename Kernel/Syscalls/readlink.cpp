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

#include <AK/StringView.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

int Process::sys$readlink(Userspace<const Syscall::SC_readlink_params*> user_params)
{
    REQUIRE_PROMISE(rpath);

    Syscall::SC_readlink_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();

    auto result = VFS::the().open(path.value(), O_RDONLY | O_NOFOLLOW_NOERROR, 0, current_directory());
    if (result.is_error())
        return result.error();
    auto description = result.value();

    if (!description->metadata().is_symlink())
        return -EINVAL;

    auto contents = description->read_entire_file();
    if (contents.is_error())
        return contents.error();

    auto& link_target = contents.value();
    auto size_to_copy = min(link_target.size(), params.buffer.size);
    if (!copy_to_user(params.buffer.data, link_target.data(), size_to_copy))
        return -EFAULT;
    // Note: we return the whole size here, not the copied size.
    return link_target.size();
}

}
