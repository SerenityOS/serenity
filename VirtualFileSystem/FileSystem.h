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
#include <AK/AKString.h>
#include <AK/Function.h>
#include <AK/kstdio.h>

static const dword mepoch = 476763780;

class Inode;
class FileDescriptor;

class FS : public Retainable<FS> {
public:
    static void initializeGlobals();
    virtual ~FS();

    dword id() const { return m_fsid; }
    static FS* from_fsid(dword);

    virtual bool initialize() = 0;
    virtual const char* class_name() const = 0;
    virtual InodeIdentifier root_inode() const = 0;
    virtual bool write_inode(InodeIdentifier, const ByteBuffer&) = 0;
    virtual InodeMetadata inode_metadata(InodeIdentifier) const = 0;

    virtual ssize_t read_inode_bytes(InodeIdentifier, Unix::off_t offset, size_t count, byte* buffer, FileDescriptor*) const = 0;

    struct DirectoryEntry {
        DirectoryEntry(const char* name, InodeIdentifier, byte fileType);
        DirectoryEntry(const char* name, size_t name_length, InodeIdentifier, byte fileType);
        char name[256];
        size_t name_length { 0 };
        InodeIdentifier inode;
        byte fileType { 0 };
    };

    virtual int set_atime_and_mtime(InodeIdentifier, dword atime, dword mtime) = 0;
    virtual InodeIdentifier create_inode(InodeIdentifier parentInode, const String& name, Unix::mode_t, unsigned size, int& error) = 0;
    virtual InodeIdentifier create_directory(InodeIdentifier parentInode, const String& name, Unix::mode_t, int& error) = 0;

    virtual InodeIdentifier find_parent_of_inode(InodeIdentifier) const = 0;

    virtual RetainPtr<Inode> get_inode(InodeIdentifier) const = 0;

    ByteBuffer read_entire_inode(InodeIdentifier, FileDescriptor* = nullptr) const;

protected:
    FS();

private:
    dword m_fsid { 0 };
};

class Inode : public Retainable<Inode> {
    friend class VFS;
public:
    virtual ~Inode();

    FS& fs() { return m_fs; }
    const FS& fs() const { return m_fs; }
    unsigned fsid() const;
    unsigned index() const { return m_index; }

    size_t size() const { return metadata().size; }
    bool is_symlink() const { return metadata().isSymbolicLink(); }
    bool is_directory() const { return metadata().isDirectory(); }

    InodeIdentifier identifier() const { return { fsid(), index() }; }
    const InodeMetadata& metadata() const { if (!m_metadata.isValid()) { populate_metadata(); } return m_metadata; }

    ByteBuffer read_entire(FileDescriptor* = nullptr);

    virtual ssize_t read_bytes(Unix::off_t, size_t, byte* buffer, FileDescriptor*) = 0;
    virtual bool traverse_as_directory(Function<bool(const FS::DirectoryEntry&)>) = 0;
    virtual InodeIdentifier lookup(const String& name) = 0;
    virtual String reverse_lookup(InodeIdentifier) = 0;

    int set_atime_and_mtime(Unix::time_t atime, Unix::time_t mtime);

protected:
    Inode(FS& fs, unsigned index)
        : m_fs(fs)
        , m_index(index)
    {
    }

    virtual void populate_metadata() const = 0;

    mutable InodeMetadata m_metadata;
private:
    FS& m_fs;
    unsigned m_index { 0 };
};

inline FS* InodeIdentifier::fs()
{
    return FS::from_fsid(m_fsid);
}

inline const FS* InodeIdentifier::fs() const
{
    return FS::from_fsid(m_fsid);
}

inline InodeMetadata InodeIdentifier::metadata() const
{
    if (!is_valid())
        return InodeMetadata();
    return fs()->inode_metadata(*this);
}

inline bool InodeIdentifier::is_root_inode() const
{
    return (*this) == fs()->root_inode();
}

inline unsigned Inode::fsid() const
{
    return m_fs.id();
}

namespace AK {

template<>
struct Traits<InodeIdentifier> {
    // FIXME: This is a shitty hash.
    static unsigned hash(const InodeIdentifier& inode) { return Traits<unsigned>::hash(inode.fsid()) + Traits<unsigned>::hash(inode.index()); }
    static void dump(const InodeIdentifier& inode) { kprintf("%02u:%08u", inode.fsid(), inode.index()); }
};

}

