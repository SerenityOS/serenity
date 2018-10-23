#include "SyntheticFileSystem.h"
#include <AK/StdLib.h>

//#define SYNTHFS_DEBUG

RetainPtr<SyntheticFileSystem> SyntheticFileSystem::create()
{
    return adopt(*new SyntheticFileSystem);
}

SyntheticFileSystem::SyntheticFileSystem()
{
}

SyntheticFileSystem::~SyntheticFileSystem()
{
}

bool SyntheticFileSystem::initialize()
{
    // Add a File for the root directory.
    // FIXME: This needs work.
    auto rootDir = make<File>();
    rootDir->metadata.inode = { id(), 1 };
    rootDir->metadata.mode = 0040555;
    rootDir->metadata.uid = 0;
    rootDir->metadata.gid = 0;
    rootDir->metadata.size = 0;
    rootDir->metadata.mtime = mepoch;
    m_files.append(move(rootDir));

#ifndef SERENITY
    addFile(createTextFile("file", "I'm a synthetic file!\n"));
    addFile(createTextFile("message", "Hey! This isn't my bottle!\n"));
#endif
    return true;
}

auto SyntheticFileSystem::createTextFile(String&& name, String&& text) -> OwnPtr<File>
{
    auto file = make<File>();
    file->data = text.toByteBuffer();
    file->name = move(name);
    file->metadata.size = file->data.size();
    file->metadata.uid = 100;
    file->metadata.gid = 200;
    file->metadata.mode = 0100644;
    file->metadata.mtime = mepoch;
    return file;
}

auto SyntheticFileSystem::createGeneratedFile(String&& name, Function<ByteBuffer()>&& generator) -> OwnPtr<File>
{
    auto file = make<File>();
    file->generator = move(generator);
    file->name = move(name);
    file->metadata.size = 0;
    file->metadata.uid = 0;
    file->metadata.gid = 0;
    file->metadata.mode = 0100644;
    file->metadata.mtime = mepoch;
    return file;
}

void SyntheticFileSystem::addFile(OwnPtr<File>&& file)
{
    ASSERT(file);
    file->metadata.inode = { id(), m_files.size() + 1 };
    m_files.append(move(file));
}

const char* SyntheticFileSystem::className() const
{
    return "synthfs";
}

InodeIdentifier SyntheticFileSystem::rootInode() const
{
    return { id(), 1 };
}

bool SyntheticFileSystem::enumerateDirectoryInode(InodeIdentifier inode, Function<bool(const DirectoryEntry&)> callback) const
{
    ASSERT(inode.fileSystemID() == id());
#ifdef SYNTHFS_DEBUG
    kprintf("[synthfs] enumerateDirectoryInode %u\n", inode.index());
#endif
    if (inode.index() != 1)
        return false;

    callback({ ".", m_files[0]->metadata.inode });
    callback({ "..", m_files[0]->metadata.inode });

    for (unsigned i = 1; i < m_files.size(); ++i)
        callback({ m_files[i]->name, m_files[i]->metadata.inode });
    return true;
}

InodeMetadata SyntheticFileSystem::inodeMetadata(InodeIdentifier inode) const
{
    ASSERT(inode.fileSystemID() == id());
#ifdef SYNTHFS_DEBUG
    kprintf("[synthfs] inodeMetadata(%u)\n", inode.index());
#endif
    if (inode.index() == 0 || inode.index() > m_files.size())
        return InodeMetadata();
    auto& file = *m_files[inode.index() - 1];
    auto metadata = file.metadata;
    if (file.generator)
        metadata.size = file.generator().size();
    return metadata;
}

bool SyntheticFileSystem::setModificationTime(InodeIdentifier, dword timestamp)
{
    (void) timestamp;
    kprintf("FIXME: Implement SyntheticFileSystem::setModificationTime().\n");
    return false;
}

InodeIdentifier SyntheticFileSystem::createInode(InodeIdentifier parentInode, const String& name, Unix::mode_t mode, unsigned size)
{
    (void) parentInode;
    (void) name;
    (void) mode;
    kprintf("FIXME: Implement SyntheticFileSystem::createDirectoryInode().\n");
    return { };
}

bool SyntheticFileSystem::writeInode(InodeIdentifier, const ByteBuffer&)
{
    kprintf("FIXME: Implement SyntheticFileSystem::writeInode().\n");
    return false;
}

Unix::ssize_t SyntheticFileSystem::readInodeBytes(InodeIdentifier inode, Unix::off_t offset, Unix::size_t count, byte* buffer) const
{
    ASSERT(inode.fileSystemID() == id());
#ifdef SYNTHFS_DEBUG
    kprintf("[synthfs] readInode %u\n", inode.index());
#endif
    ASSERT(inode.index() != 1);
    ASSERT(inode.index() <= m_files.size());
    ASSERT(offset >= 0);
    ASSERT(buffer);

    auto& file = *m_files[inode.index() - 1];
    ByteBuffer generatedData;
    if (file.generator)
        generatedData = file.generator();
    auto* data = generatedData ? &generatedData : &file.data;

    Unix::ssize_t nread = min(static_cast<Unix::off_t>(data->size() - offset), static_cast<Unix::off_t>(count));
    memcpy(buffer, data->pointer() + offset, nread);
    return nread;
}

InodeIdentifier SyntheticFileSystem::makeDirectory(InodeIdentifier parentInode, const String& name, Unix::mode_t)
{
    kprintf("FIXME: Implement SyntheticFileSystem::makeDirectory().\n");
    return { };
}
