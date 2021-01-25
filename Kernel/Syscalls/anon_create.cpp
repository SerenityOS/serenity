/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include <Kernel/FileSystem/AnonymousFile.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>

namespace Kernel {

int Process::sys$anon_create(size_t size, int options)
{
    REQUIRE_PROMISE(stdio);

    if (!size)
        return -EINVAL;

    if (size % PAGE_SIZE)
        return -EINVAL;

    int new_fd = alloc_fd();
    if (new_fd < 0)
        return new_fd;

    auto vmobject = AnonymousVMObject::create_with_size(size, AllocationStrategy::Reserve);
    if (!vmobject)
        return -ENOMEM;

    auto anon_file = AnonymousFile::create(vmobject.release_nonnull());
    auto description_or_error = FileDescription::create(*anon_file);
    if (description_or_error.is_error())
        return description_or_error.error();

    auto description = description_or_error.release_value();
    description->set_writable(true);
    description->set_readable(true);

    u32 fd_flags = 0;
    if (options & O_CLOEXEC)
        fd_flags |= FD_CLOEXEC;

    m_fds[new_fd].set(move(description), fd_flags);
    return new_fd;
}

}
