#pragma once

#include "InodeIdentifier.h"
#include "InodeMetadata.h"
#include "UnixTypes.h"
#include <AK/String.h>
#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/WeakPtr.h>
#include <AK/kstdio.h>
#include <Kernel/Devices/DiskDevice.h>
#include <Kernel/KResult.h>
#include <Kernel/Lock.h>

static const u32 mepoch = 476763780;

class Inode;
class FileDescription;
class LocalSocket;
class VMObject;

class FS : public RefCounted<FS> {
    friend class Inode;

public:
    virtual ~FS();

    unsigned fsid() const { return m_fsid; }
    static FS* from_fsid(u32);
    static void sync();
    static void lock_all();

    virtual bool initialize() = 0;
    virtual const char* class_name() const = 0;
    virtual InodeIdentifier root_inode() const = 0;
    virtual bool supports_watchers() const { return false; }

    bool is_readonly() const { return m_readonly; }

    virtual unsigned total_block_count() const { return 0; }
    virtual unsigned free_block_count() const { return 0; }
    virtual unsigned total_inode_count() const { return 0; }
    virtual unsigned free_inode_count() const { return 0; }

    virtual KResult prepare_to_unmount() const { return KSuccess; }

    // FIXME: This data structure is very clunky and unpleasant. Replace it with something nicer.
    struct DirectoryEntry {
        DirectoryEntry(const char* name, InodeIdentifier, u8 file_type);
        DirectoryEntry(const char* name, size_t name_length, InodeIdentifier, u8 file_type);
        char name[256];
        size_t name_length { 0 };
        InodeIdentifier inode;
        u8 file_type { 0 };
    };

    virtual RefPtr<Inode> create_inode(InodeIdentifier parentInode, const String& name, mode_t, off_t size, dev_t, int& error) = 0;
    virtual RefPtr<Inode> create_directory(InodeIdentifier parentInode, const String& name, mode_t, int& error) = 0;

    virtual RefPtr<Inode> get_inode(InodeIdentifier) const = 0;

    virtual void flush_writes() {}

    int block_size() const { return m_block_size; }

    virtual bool is_disk_backed() const { return false; }

protected:
    FS();

    void set_block_size(int);

    mutable Lock m_lock { "FS" };

private:
    unsigned m_fsid { 0 };
    int m_block_size { 0 };
    bool m_readonly { false };
};

inline FS* InodeIdentifier::fs()
{
    return FS::from_fsid(m_fsid);
}

inline const FS* InodeIdentifier::fs() const
{
    return FS::from_fsid(m_fsid);
}

inline bool InodeIdentifier::is_root_inode() const
{
    return (*this) == fs()->root_inode();
}

namespace AK {

template<>
struct Traits<InodeIdentifier> : public GenericTraits<InodeIdentifier> {
    static unsigned hash(const InodeIdentifier& inode) { return pair_int_hash(inode.fsid(), inode.index()); }
    static void dump(const InodeIdentifier& inode) { kprintf("%02u:%08u", inode.fsid(), inode.index()); }
};

}
