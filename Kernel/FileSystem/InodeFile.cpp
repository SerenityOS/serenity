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
    ssize_t nread = m_inode->read_bytes(offset, count, buffer, &description);
    if (nread > 0)
        Thread::current()->did_file_read(nread);
    if (nread < 0)
        return KResult(nread);
    return nread;
}

KResultOr<size_t> InodeFile::write(FileDescription& description, size_t offset, const UserOrKernelBuffer& data, size_t count)
{
    ssize_t nwritten = m_inode->write_bytes(offset, count, data, &description);
    if (nwritten > 0) {
        m_inode->set_mtime(kgettimeofday().tv_sec);
        Thread::current()->did_file_write(nwritten);
    }
    if (nwritten < 0)
        return KResult(nwritten);
    return nwritten;
}

KResultOr<Region*> InodeFile::mmap(Process& process, FileDescription& description, VirtualAddress preferred_vaddr, size_t offset, size_t size, int prot, bool shared)
{
    // FIXME: If PROT_EXEC, check that the underlying file system isn't mounted noexec.
    RefPtr<InodeVMObject> vmobject;
    if (shared)
        vmobject = SharedInodeVMObject::create_with_inode(inode());
    else
        vmobject = PrivateInodeVMObject::create_with_inode(inode());
    if (!vmobject)
        return KResult(-ENOMEM);
    auto* region = process.allocate_region_with_vmobject(preferred_vaddr, size, *vmobject, offset, description.absolute_path(), prot);
    if (!region)
        return KResult(-ENOMEM);
    return region;
}

String InodeFile::absolute_path(const FileDescription& description) const
{
    ASSERT_NOT_REACHED();
    ASSERT(description.custody());
    return description.absolute_path();
}

KResult InodeFile::truncate(u64 size)
{
    auto truncate_result = m_inode->truncate(size);
    if (truncate_result.is_error())
        return truncate_result;
    int mtime_result = m_inode->set_mtime(kgettimeofday().tv_sec);
    if (mtime_result != 0)
        return KResult(mtime_result);
    return KSuccess;
}

KResult InodeFile::chown(FileDescription& description, uid_t uid, gid_t gid)
{
    ASSERT(description.inode() == m_inode);
    ASSERT(description.custody());
    return VFS::the().chown(*description.custody(), uid, gid);
}

KResult InodeFile::chmod(FileDescription& description, mode_t mode)
{
    ASSERT(description.inode() == m_inode);
    ASSERT(description.custody());
    return VFS::the().chmod(*description.custody(), mode);
}

}
