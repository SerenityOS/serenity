/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <AK/Atomic.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/KBufferBuilder.h>

namespace Kernel {

class Plan9FSInode;

class Plan9FS final : public FileBackedFS {
    friend class Plan9FSInode;

public:
    virtual ~Plan9FS() override;
    static NonnullRefPtr<Plan9FS> create(FileDescription&);

    virtual bool initialize() override;

    virtual bool supports_watchers() const override { return false; }

    virtual NonnullRefPtr<Inode> root_inode() const override;

    u16 allocate_tag() { return m_next_tag++; }
    u32 allocate_fid() { return m_next_fid++; }

    enum class ProtocolVersion {
        v9P2000,
        v9P2000u,
        v9P2000L
    };

    struct qid {
        u8 type;
        u32 version;
        u64 path;
    };

    class Message;

private:
    Plan9FS(FileDescription&);

    class Blocker;

    class Plan9FSBlockCondition : public Thread::BlockCondition {
    public:
        Plan9FSBlockCondition(Plan9FS& fs)
            : m_fs(fs)
        {
        }

        void unblock_completed(u16);
        void unblock_all();
        void try_unblock(Blocker&);

    protected:
        virtual bool should_add_blocker(Thread::Blocker&, void*) override;

    private:
        Plan9FS& m_fs;
        mutable SpinLock<u8> m_lock;
    };

    struct ReceiveCompletion : public RefCounted<ReceiveCompletion> {
        mutable SpinLock<u8> lock;
        bool completed { false };
        const u16 tag;
        OwnPtr<Message> message;
        KResult result { KSuccess };

        ReceiveCompletion(u16 tag);
        ~ReceiveCompletion();
    };

    class Blocker final : public Thread::Blocker {
    public:
        Blocker(Plan9FS& fs, Message& message, NonnullRefPtr<ReceiveCompletion> completion)
            : m_fs(fs)
            , m_message(message)
            , m_completion(move(completion))
        {
            set_block_condition(fs.m_completion_blocker);
        }
        virtual const char* state_string() const override { return "Waiting"; }
        virtual Type blocker_type() const override { return Type::Plan9FS; }
        virtual void not_blocking(bool) override;

        const NonnullRefPtr<ReceiveCompletion>& completion() const { return m_completion; }
        u16 tag() const { return m_completion->tag; }
        bool is_completed() const;

        bool unblock()
        {
            unblock_from_blocker();
            return true;
        }

        bool unblock(u16 tag);

    private:
        Plan9FS& m_fs;
        Message& m_message;
        NonnullRefPtr<ReceiveCompletion> m_completion;
        bool m_did_unblock { false };
    };
    friend class Blocker;

    virtual const char* class_name() const override { return "Plan9FS"; }

    bool is_complete(const ReceiveCompletion&);
    KResult post_message(Message&, RefPtr<ReceiveCompletion>);
    KResult do_read(u8* buffer, size_t);
    KResult read_and_dispatch_one_message();
    KResult post_message_and_wait_for_a_reply(Message&);
    KResult post_message_and_explicitly_ignore_reply(Message&);

    ProtocolVersion parse_protocol_version(const StringView&) const;
    ssize_t adjust_buffer_size(ssize_t size) const;

    void thread_main();
    void ensure_thread();

    RefPtr<Plan9FSInode> m_root_inode;
    Atomic<u16> m_next_tag { (u16)-1 };
    Atomic<u32> m_next_fid { 1 };

    ProtocolVersion m_remote_protocol_version { ProtocolVersion::v9P2000 };
    size_t m_max_message_size { 4 * KiB };

    Lock m_send_lock { "Plan9FS send" };
    Plan9FSBlockCondition m_completion_blocker;
    HashMap<u16, NonnullRefPtr<ReceiveCompletion>> m_completions;

    SpinLock<u8> m_thread_lock;
    RefPtr<Thread> m_thread;
    Atomic<bool> m_thread_running { false };
    Atomic<bool, AK::MemoryOrder::memory_order_relaxed> m_thread_shutdown { false };
};

class Plan9FSInode final : public Inode {
    friend class Plan9FS;

public:
    virtual ~Plan9FSInode() override;

    u32 fid() const { return index().value(); }

    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual void flush_metadata() override;
    virtual ssize_t read_bytes(off_t, ssize_t, UserOrKernelBuffer& buffer, FileDescription*) const override;
    virtual ssize_t write_bytes(off_t, ssize_t, const UserOrKernelBuffer& data, FileDescription*) override;
    virtual KResult traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(const String& name, mode_t, dev_t, uid_t, gid_t) override;
    virtual KResult add_child(Inode&, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual KResultOr<size_t> directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;
    virtual KResult truncate(u64) override;

private:
    Plan9FSInode(Plan9FS&, u32 fid);
    static NonnullRefPtr<Plan9FSInode> create(Plan9FS&, u32 fid);

    enum class GetAttrMask : u64 {
        Mode = 0x1,
        NLink = 0x2,
        UID = 0x4,
        GID = 0x8,
        RDev = 0x10,
        ATime = 0x20,
        MTime = 0x40,
        CTime = 0x80,
        Ino = 0x100,
        Size = 0x200,
        Blocks = 0x400,

        BTime = 0x800,
        Gen = 0x1000,
        DataVersion = 0x2000,

        Basic = 0x7ff,
        All = 0x3fff
    };

    enum class SetAttrMask : u64 {
        Mode = 0x1,
        UID = 0x2,
        GID = 0x4,
        Size = 0x8,
        ATime = 0x10,
        MTime = 0x20,
        CTime = 0x40,
        ATimeSet = 0x80,
        MTimeSet = 0x100
    };

    // Mode in which the file is already open, using SerenityOS constants.
    int m_open_mode { 0 };
    KResult ensure_open_for_mode(int mode);

    Plan9FS& fs() { return reinterpret_cast<Plan9FS&>(Inode::fs()); }
    Plan9FS& fs() const
    {
        return const_cast<Plan9FS&>(reinterpret_cast<const Plan9FS&>(Inode::fs()));
    }
};

}
