#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include "FileSystem.h"

static dword s_lastFileSystemID = 0;

static HashMap<dword, FileSystem*>& fileSystems()
{
    static auto* map = new HashMap<dword, FileSystem*>();
    return *map;
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

InodeIdentifier FileSystem::childOfDirectoryInodeWithName(InodeIdentifier inode, const String& name)
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

