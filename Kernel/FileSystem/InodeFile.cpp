#include <Kernel/FileSystem/InodeFile.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

InodeFile::InodeFile(Retained<Inode>&& inode)
    : m_inode(move(inode))
{
}

InodeFile::~InodeFile()
{
}

ssize_t InodeFile::read(FileDescription& descriptor, byte* buffer, ssize_t count)
{
    return m_inode->read_bytes(descriptor.offset(), count, buffer, &descriptor);
}

ssize_t InodeFile::write(FileDescription& descriptor, const byte* data, ssize_t count)
{
    return m_inode->write_bytes(descriptor.offset(), count, data, &descriptor);
}

KResultOr<Region*> InodeFile::mmap(Process& process, FileDescription& descriptor, LinearAddress preferred_laddr, size_t offset, size_t size, int prot)
{
    ASSERT(offset == 0);
    // FIXME: If PROT_EXEC, check that the underlying file system isn't mounted noexec.
    InterruptDisabler disabler;
    auto* region = process.allocate_file_backed_region(preferred_laddr, size, inode(), descriptor.absolute_path(), prot);
    if (!region)
        return KResult(-ENOMEM);
    return region;
}

String InodeFile::absolute_path(const FileDescription& descriptor) const
{
    ASSERT_NOT_REACHED();
    ASSERT(descriptor.custody());
    return descriptor.absolute_path();
}

KResult InodeFile::truncate(off_t size)
{
    return m_inode->truncate(size);
}
