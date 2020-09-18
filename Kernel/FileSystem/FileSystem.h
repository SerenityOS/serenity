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

#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/KResult.h>
#include <Kernel/Lock.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

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
    virtual NonnullRefPtr<Inode> root_inode() const = 0;
    virtual bool supports_watchers() const { return false; }

    bool is_readonly() const { return m_readonly; }

    virtual unsigned total_block_count() const { return 0; }
    virtual unsigned free_block_count() const { return 0; }
    virtual unsigned total_inode_count() const { return 0; }
    virtual unsigned free_inode_count() const { return 0; }

    virtual KResult prepare_to_unmount() const { return KSuccess; }

    struct DirectoryEntryView {
        DirectoryEntryView(const StringView& name, InodeIdentifier, u8 file_type);

        StringView name;
        InodeIdentifier inode;
        u8 file_type { 0 };
    };

    virtual void flush_writes() { }

    size_t block_size() const { return m_block_size; }

    virtual bool is_file_backed() const { return false; }

    // Converts file types that are used internally by the filesystem to DT_* types
    virtual u8 internal_file_type_to_directory_entry_type(const DirectoryEntryView& entry) const { return entry.file_type; }

protected:
    FS();

    void set_block_size(size_t);

    mutable Lock m_lock { "FS" };

private:
    unsigned m_fsid { 0 };
    size_t m_block_size { 0 };
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

}

namespace AK {

template<>
struct Traits<Kernel::InodeIdentifier> : public GenericTraits<Kernel::InodeIdentifier> {
    static unsigned hash(const Kernel::InodeIdentifier& inode) { return pair_int_hash(inode.fsid(), inode.index()); }
};

}
