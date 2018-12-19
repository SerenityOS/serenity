#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <LibC/errno_numbers.h>
#include "FileSystem.h"

static dword s_lastFileSystemID;
static HashMap<dword, FS*>* map;

static HashMap<dword, FS*>& fileSystems()
{
    if (!map)
        map = new HashMap<dword, FS*>();
    return *map;
}

void FS::initializeGlobals()
{
    s_lastFileSystemID = 0;
    map = 0;
}

FS::FS()
    : m_fsid(++s_lastFileSystemID)
{
    fileSystems().set(m_fsid, this);
}

FS::~FS()
{
    fileSystems().remove(m_fsid);
}

FS* FS::from_fsid(dword id)
{
    auto it = fileSystems().find(id);
    if (it != fileSystems().end())
        return (*it).value;
    return nullptr;
}

ByteBuffer Inode::read_entire(FileDescriptor* descriptor)
{
    return fs().read_entire_inode(identifier(), descriptor);
/*
    size_t initial_size = metadata().size ? metadata().size : 4096;
    auto contents = ByteBuffer::createUninitialized(initial_size);

    ssize_t nread;
    byte buffer[4096];
    byte* out = contents.pointer();
    Unix::off_t offset = 0;
    for (;;) {
        nread = read_bytes(offset, sizeof(buffer), buffer, descriptor);
        //kprintf("nread: %u, bufsiz: %u, initial_size: %u\n", nread, sizeof(buffer), initial_size);
        ASSERT(nread <= (ssize_t)sizeof(buffer));
        if (nread <= 0)
            break;
        memcpy(out, buffer, nread);
        out += nread;
        offset += nread;
        ASSERT(offset <= (ssize_t)initial_size); // FIXME: Support dynamically growing the buffer.
    }
    if (nread < 0) {
        kprintf("Inode::read_entire: ERROR: %d\n", nread);
        return nullptr;
    }

    contents.trim(offset);
    return contents;
    */
}

ByteBuffer FS::read_entire_inode(InodeIdentifier inode, FileDescriptor* handle) const
{
    ASSERT(inode.fsid() == id());

    auto metadata = inode_metadata(inode);
    if (!metadata.isValid()) {
        kprintf("[fs] readInode: metadata lookup for inode %u failed\n", inode.index());
        return nullptr;
    }

    size_t initialSize = metadata.size ? metadata.size : 4096;
    auto contents = ByteBuffer::createUninitialized(initialSize);

    ssize_t nread;
    byte buffer[4096];
    byte* out = contents.pointer();
    Unix::off_t offset = 0;
    for (;;) {
        nread = read_inode_bytes(inode, offset, sizeof(buffer), buffer, handle);
        //kprintf("nread: %u, bufsiz: %u, initialSize: %u\n", nread, sizeof(buffer), initialSize);
        ASSERT(nread <= (ssize_t)sizeof(buffer));
        if (nread <= 0)
            break;
        memcpy(out, buffer, nread);
        out += nread;
        offset += nread;
        ASSERT(offset <= (ssize_t)initialSize); // FIXME: Support dynamically growing the buffer.
    }
    if (nread < 0) {
        kprintf("[fs] readInode: ERROR: %d\n", nread);
        return nullptr;
    }

    contents.trim(offset);
    return contents;
}

FS::DirectoryEntry::DirectoryEntry(const char* n, InodeIdentifier i, byte ft)
    : name_length(strlen(n))
    , inode(i)
    , fileType(ft)
{
    memcpy(name, n, name_length);
    name[name_length] = '\0';
}

FS::DirectoryEntry::DirectoryEntry(const char* n, size_t nl, InodeIdentifier i, byte ft)
    : name_length(nl)
    , inode(i)
    , fileType(ft)
{
    memcpy(name, n, nl);
    name[nl] = '\0';
}

Inode::~Inode()
{
}

int Inode::set_atime(Unix::time_t ts)
{
    if (fs().is_readonly())
        return -EROFS;
    if (m_metadata.atime == ts)
        return 0;
    m_metadata.atime = ts;
    m_dirty = true;
    return 0;
}

int Inode::set_ctime(Unix::time_t ts)
{
    if (fs().is_readonly())
        return -EROFS;
    if (m_metadata.ctime == ts)
        return 0;
    m_metadata.ctime = ts;
    m_dirty = true;
    return 0;
}

int Inode::set_mtime(Unix::time_t ts)
{
    if (fs().is_readonly())
        return -EROFS;
    if (m_metadata.mtime == ts)
        return 0;
    m_metadata.mtime = ts;
    m_dirty = true;
    return 0;
}
