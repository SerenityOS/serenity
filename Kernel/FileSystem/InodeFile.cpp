#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeFile.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

InodeFile::InodeFile(NonnullRefPtr<Inode>&& inode)
    : m_inode(move(inode))
{
}

InodeFile::~InodeFile()
{
}

ssize_t InodeFile::read(FileDescription& description, u8* buffer, ssize_t count)
{
    ssize_t nread = m_inode->read_bytes(description.offset(), count, buffer, &description);
    if (nread > 0)
        current->did_file_read(nread);
    return nread;
}

ssize_t InodeFile::write(FileDescription& description, const u8* data, ssize_t count)
{
    ssize_t nwritten = m_inode->write_bytes(description.offset(), count, data, &description);
    if (nwritten > 0) {
        m_inode->set_mtime(kgettimeofday().tv_sec);
        current->did_file_write(nwritten);
    }
    return nwritten;
}

KResultOr<Region*> InodeFile::mmap(Process& process, FileDescription& description, VirtualAddress preferred_vaddr, size_t offset, size_t size, int prot)
{
    ASSERT(offset == 0);
    // FIXME: If PROT_EXEC, check that the underlying file system isn't mounted noexec.
    auto* region = process.allocate_file_backed_region(preferred_vaddr, size, inode(), description.absolute_path(), prot);
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

KResult InodeFile::truncate(off_t size)
{
    auto truncate_result = m_inode->truncate(size);
    if (truncate_result.is_error())
        return truncate_result;
    int mtime_result = m_inode->set_mtime(kgettimeofday().tv_sec);
    if (mtime_result != 0)
        return KResult(mtime_result);
    return KSuccess;
}

KResult InodeFile::chown(uid_t uid, gid_t gid)
{
    return VFS::the().chown(*m_inode, uid, gid);
}

KResult InodeFile::chmod(mode_t mode)
{
    return VFS::the().chmod(*m_inode, mode);
}
