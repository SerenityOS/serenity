/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/AnonymousFile.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>

namespace Kernel {

KResultOr<int> Process::sys$anon_create(size_t size, int options)
{
    REQUIRE_PROMISE(stdio);

    if (!size)
        return EINVAL;

    if (size % PAGE_SIZE)
        return EINVAL;

    int new_fd = alloc_fd();
    if (new_fd < 0)
        return new_fd;

    auto vmobject = AnonymousVMObject::create_with_size(size, AllocationStrategy::Reserve);
    if (!vmobject)
        return ENOMEM;

    auto anon_file = AnonymousFile::create(vmobject.release_nonnull());
    if (!anon_file)
        return ENOMEM;
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
