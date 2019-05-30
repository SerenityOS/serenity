#include <Kernel/FileSystem/InodeFile.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/FileDescriptor.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

InodeFile::InodeFile(Retained<Inode>&& inode)
    : m_inode(move(inode))
{
}

InodeFile::~InodeFile()
{
}

ssize_t InodeFile::read(FileDescriptor& descriptor, byte* buffer, ssize_t count)
{
    return m_inode->read_bytes(descriptor.offset(), count, buffer, &descriptor);
}

ssize_t InodeFile::write(FileDescriptor& descriptor, const byte* data, ssize_t count)
{
    return m_inode->write_bytes(descriptor.offset(), count, data, &descriptor);
}

KResultOr<Region*> InodeFile::mmap(Process& process, LinearAddress preferred_laddr, size_t offset, size_t size, int prot)
{
    ASSERT(offset == 0);
    // FIXME: If PROT_EXEC, check that the underlying file system isn't mounted noexec.
    String region_name;
#if 0
    // FIXME: I would like to do this, but it would instantiate all the damn inodes.
    region_name = absolute_path();
#else
    region_name = "Memory-mapped file";
#endif
    InterruptDisabler disabler;
    auto* region = process.allocate_file_backed_region(preferred_laddr, size, inode(), move(region_name), prot);
    if (!region)
        return KResult(-ENOMEM);
    return region;
}

String InodeFile::absolute_path(FileDescriptor& descriptor) const
{
    ASSERT_NOT_REACHED();
    ASSERT(descriptor.custody());
    return descriptor.absolute_path();
}

KResult InodeFile::truncate(off_t size)
{
    return m_inode->truncate(size);
}
