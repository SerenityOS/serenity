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

ssize_t InodeFile::read(FileDescription& description, byte* buffer, ssize_t count)
{
    return m_inode->read_bytes(description.offset(), count, buffer, &description);
}

ssize_t InodeFile::write(FileDescription& description, const byte* data, ssize_t count)
{
    return m_inode->write_bytes(description.offset(), count, data, &description);
}

KResultOr<Region*> InodeFile::mmap(Process& process, FileDescription& description, VirtualAddress preferred_vaddr, size_t offset, size_t size, int prot)
{
    ASSERT(offset == 0);
    // FIXME: If PROT_EXEC, check that the underlying file system isn't mounted noexec.
    InterruptDisabler disabler;
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
    return m_inode->truncate(size);
}
