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
#include <AK/Function.h>
#include <AK/kstdio.h>

static const dword mepoch = 476763780;

class FileDescriptor;
class FileSystem;

class CoreInode : public Retainable<CoreInode> {
public:
    virtual ~CoreInode();

    FileSystem& fs() const { return m_fs; }
    unsigned fsid() const;
    unsigned index() const { return m_index; }

    InodeIdentifier identifier() const { return { fsid(), index() }; }

    virtual Unix::ssize_t read_bytes(Unix::off_t, Unix::size_t, byte* buffer, FileDescriptor*) = 0;

protected:
    CoreInode(FileSystem& fs, unsigned index)
        : m_fs(fs)
        , m_index(index)
    {
    }

private:
    FileSystem& m_fs;
    unsigned m_index { 0 };
};

class FileSystem : public Retainable<FileSystem> {
public:
    static void initializeGlobals();
    virtual ~FileSystem();

    dword id() const { return m_id; }
    static FileSystem* fromID(dword);

    virtual bool initialize() = 0;
    virtual const char* className() const = 0;
    virtual InodeIdentifier rootInode() const = 0;
    virtual bool writeInode(InodeIdentifier, const ByteBuffer&) = 0;
    virtual InodeMetadata inodeMetadata(InodeIdentifier) const = 0;

    virtual Unix::ssize_t readInodeBytes(InodeIdentifier, Unix::off_t offset, Unix::size_t count, byte* buffer, FileDescriptor*) const = 0;

    struct DirectoryEntry {
        DirectoryEntry(const char* name, InodeIdentifier, byte fileType);
        DirectoryEntry(const char* name, Unix::size_t name_length, InodeIdentifier, byte fileType);
        char name[256];
        Unix::size_t name_length { 0 };
        InodeIdentifier inode;
        byte fileType { 0 };
    };
    virtual bool enumerateDirectoryInode(InodeIdentifier, Function<bool(const DirectoryEntry&)>) const = 0;

    virtual bool setModificationTime(InodeIdentifier, dword timestamp) = 0;
    virtual InodeIdentifier createInode(InodeIdentifier parentInode, const String& name, Unix::mode_t, unsigned size) = 0;
    virtual InodeIdentifier makeDirectory(InodeIdentifier parentInode, const String& name, Unix::mode_t) = 0;

    virtual InodeIdentifier findParentOfInode(InodeIdentifier) const = 0;

    virtual RetainPtr<CoreInode> get_inode(InodeIdentifier) = 0;

    InodeIdentifier childOfDirectoryInodeWithName(InodeIdentifier, const String& name) const;
    ByteBuffer readEntireInode(InodeIdentifier, FileDescriptor* = nullptr) const;
    String nameOfChildInDirectory(InodeIdentifier parent, InodeIdentifier child) const;

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

inline unsigned CoreInode::fsid() const
{
    return m_fs.id();
}

namespace AK {

template<>
struct Traits<InodeIdentifier> {
    // FIXME: This is a shitty hash.
    static unsigned hash(const InodeIdentifier& inode) { return Traits<unsigned>::hash(inode.fileSystemID()) + Traits<unsigned>::hash(inode.index()); }
    static void dump(const InodeIdentifier& inode) { kprintf("%02u:%08u", inode.fileSystemID(), inode.index()); }
};

}

