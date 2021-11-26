/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/KBufferBuilder.h>

namespace Kernel {

class Plan9FSInode;

class Plan9FS final : public FileBackedFileSystem {
    friend class Plan9FSInode;

public:
    virtual ~Plan9FS() override;
    static ErrorOr<NonnullRefPtr<Plan9FS>> try_create(OpenFileDescription&);

    virtual ErrorOr<void> initialize() override;

    virtual bool supports_watchers() const override { return false; }

    virtual Inode& root_inode() override;

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
    Plan9FS(OpenFileDescription&);

    class Blocker;

    class Plan9FSBlockerSet final : public Thread::BlockerSet {
    public:
        Plan9FSBlockerSet(Plan9FS& fs)
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
        mutable Spinlock m_lock;
    };

    struct ReceiveCompletion : public RefCounted<ReceiveCompletion> {
        mutable Spinlock lock;
        bool completed { false };
        const u16 tag;
        OwnPtr<Message> message;
        ErrorOr<void> result;

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
        }
        virtual bool setup_blocker() override;
        virtual StringView state_string() const override { return "Waiting"sv; }
        virtual Type blocker_type() const override { return Type::Plan9FS; }
        virtual void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) override;

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

    virtual StringView class_name() const override { return "Plan9FS"sv; }

    bool is_complete(const ReceiveCompletion&);
    ErrorOr<void> post_message(Message&, RefPtr<ReceiveCompletion>);
    ErrorOr<void> do_read(u8* buffer, size_t);
    ErrorOr<void> read_and_dispatch_one_message();
    ErrorOr<void> post_message_and_wait_for_a_reply(Message&);
    ErrorOr<void> post_message_and_explicitly_ignore_reply(Message&);

    ProtocolVersion parse_protocol_version(StringView) const;
    size_t adjust_buffer_size(size_t size) const;

    void thread_main();
    void ensure_thread();

    RefPtr<Plan9FSInode> m_root_inode;
    Atomic<u16> m_next_tag { (u16)-1 };
    Atomic<u32> m_next_fid { 1 };

    ProtocolVersion m_remote_protocol_version { ProtocolVersion::v9P2000 };
    size_t m_max_message_size { 4 * KiB };

    Mutex m_send_lock { "Plan9FS send" };
    Plan9FSBlockerSet m_completion_blocker;
    HashMap<u16, NonnullRefPtr<ReceiveCompletion>> m_completions;

    Spinlock m_thread_lock;
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
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& data, OpenFileDescription*) override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate(u64) override;

private:
    Plan9FSInode(Plan9FS&, u32 fid);
    static ErrorOr<NonnullRefPtr<Plan9FSInode>> try_create(Plan9FS&, u32 fid);

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
    ErrorOr<void> ensure_open_for_mode(int mode);

    Plan9FS& fs() { return reinterpret_cast<Plan9FS&>(Inode::fs()); }
    Plan9FS& fs() const
    {
        return const_cast<Plan9FS&>(reinterpret_cast<const Plan9FS&>(Inode::fs()));
    }
};

}
