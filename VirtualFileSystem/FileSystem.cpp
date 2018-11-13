#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include "FileSystem.h"

static dword s_lastFileSystemID;
static HashMap<dword, FileSystem*>* map;

static HashMap<dword, FileSystem*>& fileSystems()
{
    if (!map)
        map = new HashMap<dword, FileSystem*>();
    return *map;
}

void FileSystem::initializeGlobals()
{
    s_lastFileSystemID = 0;
    map = 0;
}

FileSystem::FileSystem()
    : m_id(++s_lastFileSystemID)
{
    fileSystems().set(m_id, this);
}

FileSystem::~FileSystem()
{
    fileSystems().remove(m_id);
}

FileSystem* FileSystem::fromID(dword id)
{
    auto it = fileSystems().find(id);
    if (it != fileSystems().end())
        return (*it).value;
    return nullptr;
}

InodeIdentifier FileSystem::childOfDirectoryInodeWithName(InodeIdentifier inode, const String& name) const
{
    InodeIdentifier foundInode;
    enumerateDirectoryInode(inode, [&] (const DirectoryEntry& entry) {
        if (!strcmp(entry.name, name.characters())) {
            foundInode = entry.inode;
            return false;
        }
        return true;
    });
    return foundInode;
}

String FileSystem::nameOfChildInDirectory(InodeIdentifier parent, InodeIdentifier child) const
{
    String name;
    bool success = enumerateDirectoryInode(parent, [&] (auto& entry) {
        if (entry.inode == child) {
            name = entry.name;
            return false;
        }
        return true;
    });
    ASSERT(success);
    return name;
}

ByteBuffer FileSystem::readEntireInode(InodeIdentifier inode, FileDescriptor* handle) const
{
    ASSERT(inode.fileSystemID() == id());

    auto metadata = inodeMetadata(inode);
    if (!metadata.isValid()) {
        kprintf("[fs] readInode: metadata lookup for inode %u failed\n", inode.index());
        return nullptr;
    }

    size_t initialSize = metadata.size ? metadata.size : 4096;
    auto contents = ByteBuffer::createUninitialized(initialSize);

    Unix::ssize_t nread;
    byte buffer[4096];
    byte* out = contents.pointer();
    Unix::off_t offset = 0;
    for (;;) {
        nread = readInodeBytes(inode, offset, sizeof(buffer), buffer, handle);
        //kprintf("nread: %u, bufsiz: %u, initialSize: %u\n", nread, sizeof(buffer), initialSize);
        ASSERT(nread <= (Unix::ssize_t)sizeof(buffer));
        if (nread <= 0)
            break;
        memcpy(out, buffer, nread);
        out += nread;
        offset += nread;
        ASSERT(offset <= (Unix::ssize_t)initialSize); // FIXME: Support dynamically growing the buffer.
    }
    if (nread < 0) {
        kprintf("[fs] readInode: ERROR: %d\n", nread);
        return nullptr;
    }

    contents.trim(offset);
    return contents;
}

FileSystem::DirectoryEntry::DirectoryEntry(const char* n, InodeIdentifier i, byte ft)
    : name_length(strlen(name))
    , inode(i)
    , fileType(ft)
{
    memcpy(name, n, name_length);
    name[name_length] = '\0';
}

FileSystem::DirectoryEntry::DirectoryEntry(const char* n, Unix::size_t nl, InodeIdentifier i, byte ft)
    : name_length(nl)
    , inode(i)
    , fileType(ft)
{
    memcpy(name, n, nl);
    name[nl] = '\0';
}

CoreInode::~CoreInode()
{
}
