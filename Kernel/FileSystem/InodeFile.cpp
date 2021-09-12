/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeFile.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Memory/PrivateInodeVMObject.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Process.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

InodeFile::InodeFile(NonnullRefPtr<Inode>&& inode)
    : m_inode(move(inode))
{
}

InodeFile::~InodeFile()
{
}

ErrorOr<size_t> InodeFile::read(OpenFileDescription& description, u64 offset, UserOrKernelBuffer& buffer, size_t count)
{
    if (Checked<off_t>::addition_would_overflow(offset, count))
        return EOVERFLOW;

    auto nread = TRY(m_inode->read_bytes(offset, count, buffer, &description));
    if (nread > 0) {
        Thread::current()->did_file_read(nread);
        evaluate_block_conditions();
    }
    return nread;
}

ErrorOr<size_t> InodeFile::write(OpenFileDescription& description, u64 offset, const UserOrKernelBuffer& data, size_t count)
{
    if (Checked<off_t>::addition_would_overflow(offset, count))
        return EOVERFLOW;

    auto nwritten = TRY(m_inode->write_bytes(offset, count, data, &description));
    if (nwritten > 0) {
        auto mtime_result = m_inode->set_mtime(kgettimeofday().to_truncated_seconds());
        Thread::current()->did_file_write(nwritten);
        evaluate_block_conditions();
        if (mtime_result.is_error())
            return mtime_result.release_error();
    }
    return nwritten;
}

ErrorOr<void> InodeFile::ioctl(OpenFileDescription& description, unsigned request, Userspace<void*> arg)
{
    switch (request) {
    case FIBMAP: {
        if (!Process::current().is_superuser())
            return EPERM;

        auto user_block_number = static_ptr_cast<int*>(arg);
        int block_number = 0;
        TRY(copy_from_user(&block_number, user_block_number));

        if (block_number < 0)
            return EINVAL;

        auto block_address = TRY(inode().get_block_address(block_number));
        return copy_to_user(user_block_number, &block_address);
    }
    case FIONREAD: {
        int remaining_bytes = inode().size() - description.offset();
        return copy_to_user(static_ptr_cast<int*>(arg), &remaining_bytes);
    }
    default:
        return EINVAL;
    }
}

ErrorOr<Memory::Region*> InodeFile::mmap(Process& process, OpenFileDescription& description, Memory::VirtualRange const& range, u64 offset, int prot, bool shared)
{
    // FIXME: If PROT_EXEC, check that the underlying file system isn't mounted noexec.
    RefPtr<Memory::InodeVMObject> vmobject;
    if (shared)
        vmobject = TRY(Memory::SharedInodeVMObject::try_create_with_inode(inode()));
    else
        vmobject = TRY(Memory::PrivateInodeVMObject::try_create_with_inode(inode()));
    auto path = TRY(description.pseudo_path());
    return process.address_space().allocate_region_with_vmobject(range, vmobject.release_nonnull(), offset, path->view(), prot, shared);
}

ErrorOr<NonnullOwnPtr<KString>> InodeFile::pseudo_path(const OpenFileDescription&) const
{
    // If it has an inode, then it has a path, and therefore the caller should have been able to get a custody at some point.
    VERIFY_NOT_REACHED();
}

ErrorOr<void> InodeFile::truncate(u64 size)
{
    TRY(m_inode->truncate(size));
    TRY(m_inode->set_mtime(kgettimeofday().to_truncated_seconds()));
    return {};
}

ErrorOr<void> InodeFile::sync()
{
    m_inode->sync();
    return {};
}

ErrorOr<void> InodeFile::chown(OpenFileDescription& description, UserID uid, GroupID gid)
{
    VERIFY(description.inode() == m_inode);
    VERIFY(description.custody());
    return VirtualFileSystem::the().chown(*description.custody(), uid, gid);
}

ErrorOr<void> InodeFile::chmod(OpenFileDescription& description, mode_t mode)
{
    VERIFY(description.inode() == m_inode);
    VERIFY(description.custody());
    return VirtualFileSystem::the().chmod(*description.custody(), mode);
}

}
