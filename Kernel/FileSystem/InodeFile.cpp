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

KResultOr<size_t> InodeFile::read(FileDescription& description, size_t offset, UserOrKernelBuffer& buffer, size_t count)
{
    if (Checked<off_t>::addition_would_overflow(offset, count))
        return EOVERFLOW;

    ssize_t nread = m_inode->read_bytes(offset, count, buffer, &description);
    if (nread > 0) {
        Thread::current()->did_file_read(nread);
        evaluate_block_conditions();
    }
    if (nread < 0)
        return KResult((ErrnoCode)-nread);
    return nread;
}

KResultOr<size_t> InodeFile::write(FileDescription& description, size_t offset, const UserOrKernelBuffer& data, size_t count)
{
    if (Checked<off_t>::addition_would_overflow(offset, count))
        return EOVERFLOW;

    ssize_t nwritten = m_inode->write_bytes(offset, count, data, &description);
    if (nwritten > 0) {
        m_inode->set_mtime(kgettimeofday().tv_sec);
        Thread::current()->did_file_write(nwritten);
        evaluate_block_conditions();
    }
    if (nwritten < 0)
        return KResult((ErrnoCode)-nwritten);
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

KResultOr<Region*> InodeFile::mmap(Process& process, FileDescription& description, const Range& range, size_t offset, int prot, bool shared)
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
    auto truncate_result = m_inode->truncate(size);
    if (truncate_result.is_error())
        return truncate_result;
    int mtime_result = m_inode->set_mtime(kgettimeofday().tv_sec);
    if (mtime_result < 0)
        return KResult((ErrnoCode)-mtime_result);
    return KSuccess;
}

KResult InodeFile::chown(FileDescription& description, uid_t uid, gid_t gid)
{
    VERIFY(description.inode() == m_inode);
    VERIFY(description.custody());
    return VFS::the().chown(*description.custody(), uid, gid);
}

KResult InodeFile::chmod(FileDescription& description, mode_t mode)
{
    VERIFY(description.inode() == m_inode);
    VERIFY(description.custody());
    return VFS::the().chmod(*description.custody(), mode);
}

}
