/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeFile.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>
#include <Kernel/VM/PrivateInodeVMObject.h>
#include <Kernel/VM/SharedInodeVMObject.h>
#include <LibC/errno_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

InodeFile::InodeFile(NonnullRefPtr<Inode>&& inode)
    : m_inode(move(inode))
{
}

InodeFile::~InodeFile()
{
}

KResultOr<size_t> InodeFile::read(FileDescription& description, u64 offset, UserOrKernelBuffer& buffer, size_t count)
{
    if (Checked<off_t>::addition_would_overflow(offset, count))
        return EOVERFLOW;

    auto result = m_inode->read_bytes(offset, count, buffer, &description);
    if (result.is_error())
        return result.error();
    auto nread = result.value();
    if (nread > 0) {
        Thread::current()->did_file_read(nread);
        evaluate_block_conditions();
    }
    return nread;
}

KResultOr<size_t> InodeFile::write(FileDescription& description, u64 offset, const UserOrKernelBuffer& data, size_t count)
{
    if (Checked<off_t>::addition_would_overflow(offset, count))
        return EOVERFLOW;

    auto result = m_inode->write_bytes(offset, count, data, &description);
    if (result.is_error())
        return result.error();

    auto nwritten = result.value();
    if (nwritten > 0) {
        auto mtime_result = m_inode->set_mtime(kgettimeofday().to_truncated_seconds());
        Thread::current()->did_file_write(nwritten);
        evaluate_block_conditions();
        if (mtime_result.is_error())
            return mtime_result;
    }
    return nwritten;
}

int InodeFile::ioctl(FileDescription& description, unsigned request, FlatPtr arg)
{
    (void)description;

    switch (request) {
    case FIBMAP: {
        if (!Process::current()->is_superuser())
            return -EPERM;

        int block_number = 0;
        if (!copy_from_user(&block_number, (int*)arg))
            return -EFAULT;

        if (block_number < 0)
            return -EINVAL;

        auto block_address = inode().get_block_address(block_number);
        if (block_address.is_error())
            return block_address.error();

        if (!copy_to_user((int*)arg, &block_address.value()))
            return -EFAULT;

        return 0;
    }
    default:
        return -EINVAL;
    }
}

KResultOr<Region*> InodeFile::mmap(Process& process, FileDescription& description, const Range& range, u64 offset, int prot, bool shared)
{
    // FIXME: If PROT_EXEC, check that the underlying file system isn't mounted noexec.
    RefPtr<InodeVMObject> vmobject;
    if (shared)
        vmobject = SharedInodeVMObject::create_with_inode(inode());
    else
        vmobject = PrivateInodeVMObject::create_with_inode(inode());
    if (!vmobject)
        return ENOMEM;
    return process.space().allocate_region_with_vmobject(range, vmobject.release_nonnull(), offset, description.absolute_path(), prot, shared);
}

String InodeFile::absolute_path(const FileDescription& description) const
{
    VERIFY_NOT_REACHED();
    VERIFY(description.custody());
    return description.absolute_path();
}

KResult InodeFile::truncate(u64 size)
{
    if (auto result = m_inode->truncate(size); result.is_error())
        return result;
    if (auto result = m_inode->set_mtime(kgettimeofday().to_truncated_seconds()); result.is_error())
        return result;
    return KSuccess;
}

KResult InodeFile::chown(FileDescription& description, uid_t uid, gid_t gid)
{
    VERIFY(description.inode() == m_inode);
    VERIFY(description.custody());
    return VirtualFileSystem::the().chown(*description.custody(), uid, gid);
}

KResult InodeFile::chmod(FileDescription& description, mode_t mode)
{
    VERIFY(description.inode() == m_inode);
    VERIFY(description.custody());
    return VirtualFileSystem::the().chmod(*description.custody(), mode);
}

}
