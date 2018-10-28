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
        if (entry.name == name) {
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

ByteBuffer FileSystem::readEntireInode(InodeIdentifier inode, FileHandle* handle) const
{
    ASSERT(inode.fileSystemID() == id());

    auto metadata = inodeMetadata(inode);
    if (!metadata.isValid()) {
        kprintf("[fs] readInode: metadata lookup for inode %u failed\n", inode.index());
        return nullptr;
    }

    auto contents = ByteBuffer::createUninitialized(metadata.size);

    Unix::ssize_t nread;
    byte buffer[512];
    byte* out = contents.pointer();
    Unix::off_t offset = 0;
    for (;;) {
        nread = readInodeBytes(inode, offset, sizeof(buffer), buffer, handle);
        if (nread <= 0)
            break;
        memcpy(out, buffer, nread);
        out += nread;
        offset += nread;
    }
    if (nread < 0) {
        kprintf("[fs] readInode: ERROR: %d\n", nread);
        return nullptr;
    }

    return contents;
}

