/*
 * Copyright (c) 2022, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeFile.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_fallocate.html
ErrorOr<FlatPtr> Process::sys$posix_fallocate(int fd, off_t offset, off_t length)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    // [EINVAL] The len argument is less than zero, or the offset argument is less than zero, or the underlying file system does not support this operation.
    if (offset < 0)
        return EINVAL;
    if (length <= 0)
        return EINVAL;

    Checked<size_t> checked_size { length };
    checked_size += offset;
    // FIXME: Return EFBIG if offset+length > FileSizeMax
    if (checked_size.has_overflow())
        return EFBIG;

    auto description = TRY(open_file_description(fd));

    // [EBADF] The fd argument references a file that was opened without write permission.
    if (!description->is_writable())
        return EBADF;

    // [ESPIPE] The fd argument is associated with a pipe or FIFO.
    if (description->is_fifo())
        return ESPIPE;

    // [ENODEV] The fd argument does not refer to a regular file.
    if (!description->file().is_regular_file())
        return ENODEV;

    VERIFY(description->file().is_inode());

    auto& file = static_cast<InodeFile&>(description->file());
    if (file.inode().size() >= checked_size.value())
        return 0;

    // Note: truncate essentially calls resize in the inodes implementation
    //       while resize is not a standard member of an inode, so we just call
    //       truncate instead
    TRY(file.inode().truncate(checked_size.value()));

    // FIXME: EINTR: A signal was caught during execution.
    return 0;
}

}
