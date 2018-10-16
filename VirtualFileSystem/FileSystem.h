#pragma once

#include "DiskDevice.h"
#include "InodeIdentifier.h"
#include "InodeMetadata.h"
#include "Limits.h"
#include "UnixTypes.h"
#include <AK/ByteBuffer.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/String.h>
#include <functional>

static const dword mepoch = 476763780;

class FileSystem : public Retainable<FileSystem> {
public:
    virtual ~FileSystem();

    dword id() const { return m_id; }
    static FileSystem* fromID(dword);

    virtual bool initialize() = 0;
    virtual const char* className() const = 0;
    virtual InodeIdentifier rootInode() const = 0;
    virtual bool writeInode(InodeIdentifier, const ByteBuffer&) = 0;
    virtual InodeMetadata inodeMetadata(InodeIdentifier) const = 0;

    virtual Unix::ssize_t readInodeBytes(InodeIdentifier, Unix::off_t offset, Unix::size_t count, byte* buffer) const = 0;

    struct DirectoryEntry {
        String name;
        InodeIdentifier inode;
        byte fileType { 0 };
    };
    virtual bool enumerateDirectoryInode(InodeIdentifier, std::function<bool(const DirectoryEntry&)>) const = 0;

    virtual bool setModificationTime(InodeIdentifier, dword timestamp) = 0;
    virtual InodeIdentifier createInode(InodeIdentifier parentInode, const String& name, Unix::mode_t, unsigned size) = 0;
    virtual InodeIdentifier makeDirectory(InodeIdentifier parentInode, const String& name, Unix::mode_t) = 0;

    InodeIdentifier childOfDirectoryInodeWithName(InodeIdentifier, const String& name) const;
    ByteBuffer readEntireInode(InodeIdentifier) const;

protected:
    FileSystem();

private:
    dword m_id { 0 };
};

inline FileSystem* InodeIdentifier::fileSystem()
{
    return FileSystem::fromID(m_fileSystemID);
}

inline const FileSystem* InodeIdentifier::fileSystem() const
{
    return FileSystem::fromID(m_fileSystemID);
}

inline InodeMetadata InodeIdentifier::metadata() const
{
    if (!isValid())
        return InodeMetadata();
    return fileSystem()->inodeMetadata(*this);
}

inline bool InodeIdentifier::isRootInode() const
{
    return (*this) == fileSystem()->rootInode();
}

namespace AK {

template<>
struct Traits<InodeIdentifier> {
    // FIXME: This is a shitty hash.
    static unsigned hash(const InodeIdentifier& inode) { return Traits<unsigned>::hash(inode.fileSystemID()) + Traits<unsigned>::hash(inode.index()); }
    static void dump(const InodeIdentifier& inode) { printf("%02u:%08u", inode.fileSystemID(), inode.index()); }
};

}

