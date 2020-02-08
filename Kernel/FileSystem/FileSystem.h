/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <AK/kstdio.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/KResult.h>
#include <Kernel/Lock.h>
#include <Kernel/UnixTypes.h>

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

    virtual KResultOr<NonnullRefPtr<Inode>> create_inode(InodeIdentifier parent_id, const String& name, mode_t, off_t size, dev_t, uid_t, gid_t) = 0;
    virtual KResult create_directory(InodeIdentifier parent_inode, const String& name, mode_t, uid_t, gid_t) = 0;

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
