/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileSystemSpecificOption.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/Plan9FS/Definitions.h>
#include <Kernel/FileSystem/Plan9FS/Message.h>
#include <Kernel/Library/KBufferBuilder.h>

namespace Kernel {

class Plan9FSInode;

class Plan9FS final : public FileBackedFileSystem {
    friend class Plan9FSInode;

public:
    virtual ~Plan9FS() override;
    static ErrorOr<NonnullRefPtr<FileSystem>> try_create(OpenFileDescription&, FileSystemSpecificOptions const&);

    virtual bool supports_watchers() const override { return false; }

    virtual Inode& root_inode() override;

    virtual ErrorOr<void> rename(Inode& old_parent_inode, StringView old_basename, Inode& new_parent_inode, StringView new_basename) override;

    u16 allocate_tag() { return m_next_tag++; }
    u32 allocate_fid() { return m_next_fid++; }

    enum class ProtocolVersion {
        v9P2000,
        v9P2000u,
        v9P2000L
    };

private:
    Plan9FS(OpenFileDescription&);

    virtual ErrorOr<void> prepare_to_clear_last_mount(Inode&) override;

    virtual bool is_initialized_while_locked() override;
    virtual ErrorOr<void> initialize_while_locked() override;

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
        mutable Spinlock<LockRank::None> m_lock {};
    };

    struct ReceiveCompletion final : public AtomicRefCounted<ReceiveCompletion> {
        mutable Spinlock<LockRank::None> lock {};
        bool completed { false };
        u16 const tag;
        OwnPtr<Plan9FSMessage> message;
        ErrorOr<void> result;

        ReceiveCompletion(u16 tag);
        ~ReceiveCompletion();
    };

    class Blocker final : public Thread::Blocker {
    public:
        Blocker(Plan9FS& fs, Plan9FSMessage& message, NonnullLockRefPtr<ReceiveCompletion> completion)
            : m_fs(fs)
            , m_message(message)
            , m_completion(move(completion))
        {
        }
        virtual bool setup_blocker() override;
        virtual StringView state_string() const override { return "Waiting"sv; }
        virtual Type blocker_type() const override { return Type::Plan9FS; }
        virtual void will_unblock_immediately_without_blocking(UnblockImmediatelyReason) override;

        NonnullLockRefPtr<ReceiveCompletion> const& completion() const { return m_completion; }
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
        Plan9FSMessage& m_message;
        NonnullLockRefPtr<ReceiveCompletion> m_completion;
        bool m_did_unblock { false };
    };
    friend class Blocker;

    virtual StringView class_name() const override { return "Plan9FS"sv; }

    bool is_complete(ReceiveCompletion const&);
    ErrorOr<void> post_message(Plan9FSMessage&, LockRefPtr<ReceiveCompletion>);
    ErrorOr<void> do_read(u8* buffer, size_t);
    ErrorOr<void> read_and_dispatch_one_message();
    ErrorOr<void> post_message_and_wait_for_a_reply(Plan9FSMessage&);
    ErrorOr<void> post_message_and_explicitly_ignore_reply(Plan9FSMessage&);

    ProtocolVersion parse_protocol_version(StringView) const;
    size_t adjust_buffer_size(size_t size) const;

    void thread_main();
    void ensure_thread();

    RefPtr<Plan9FSInode> m_root_inode;
    Atomic<u16> m_next_tag { (u16)-1 };
    Atomic<u32> m_next_fid { 1 };

    ProtocolVersion m_remote_protocol_version { ProtocolVersion::v9P2000 };
    size_t m_max_message_size { 4 * KiB };

    Mutex m_send_lock { "Plan9FS send"sv };
    Plan9FSBlockerSet m_completion_blocker;
    HashMap<u16, NonnullLockRefPtr<ReceiveCompletion>> m_completions;

    Spinlock<LockRank::None> m_thread_lock {};
    RefPtr<Thread> m_thread;
    Atomic<bool> m_thread_running { false };
};

}
