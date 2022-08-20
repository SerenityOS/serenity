/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Badge.h>
#include <AK/RefPtr.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KBuffer.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel {

class OpenFileDescriptionData {
public:
    virtual ~OpenFileDescriptionData() = default;
};

class OpenFileDescription final : public AtomicRefCounted<OpenFileDescription> {
public:
    static ErrorOr<NonnullLockRefPtr<OpenFileDescription>> try_create(Custody&);
    static ErrorOr<NonnullLockRefPtr<OpenFileDescription>> try_create(File&);
    ~OpenFileDescription();

    Thread::FileBlocker::BlockFlags should_unblock(Thread::FileBlocker::BlockFlags) const;

    bool is_readable() const;
    void set_readable(bool);

    bool is_writable() const;
    void set_writable(bool);

    void set_rw_mode(int options);

    ErrorOr<void> close();

    ErrorOr<off_t> seek(off_t, int whence);
    ErrorOr<size_t> read(UserOrKernelBuffer&, size_t);
    ErrorOr<size_t> write(UserOrKernelBuffer const& data, size_t);
    ErrorOr<struct stat> stat();

    // NOTE: These ignore the current offset of this file description.
    ErrorOr<size_t> read(UserOrKernelBuffer&, u64 offset, size_t);
    ErrorOr<size_t> write(u64 offset, UserOrKernelBuffer const&, size_t);

    ErrorOr<void> chmod(mode_t);

    bool can_read() const;
    bool can_write() const;

    ErrorOr<size_t> get_dir_entries(UserOrKernelBuffer& buffer, size_t);

    ErrorOr<NonnullOwnPtr<KBuffer>> read_entire_file();

    ErrorOr<NonnullOwnPtr<KString>> original_absolute_path() const;
    ErrorOr<NonnullOwnPtr<KString>> pseudo_path() const;

    bool is_direct() const;

    bool is_directory() const;

    File& file() { return *m_file; }
    File const& file() const { return *m_file; }

    bool is_device() const;
    Device const* device() const;
    Device* device();

    bool is_tty() const;
    const TTY* tty() const;
    TTY* tty();

    bool is_inode_watcher() const;
    InodeWatcher const* inode_watcher() const;
    InodeWatcher* inode_watcher();

    bool is_master_pty() const;
    MasterPTY const* master_pty() const;
    MasterPTY* master_pty();

    InodeMetadata metadata() const;
    Inode* inode() { return m_inode.ptr(); }
    Inode const* inode() const { return m_inode.ptr(); }

    RefPtr<Custody> custody();
    RefPtr<Custody const> custody() const;

    ErrorOr<Memory::Region*> mmap(Process&, Memory::VirtualRange const&, u64 offset, int prot, bool shared);

    bool is_blocking() const;
    void set_blocking(bool b);

    bool should_append() const;

    u32 file_flags() const;
    void set_file_flags(u32);

    bool is_socket() const;
    Socket* socket();
    Socket const* socket() const;

    bool is_fifo() const;
    FIFO* fifo();
    FIFO::Direction fifo_direction() const;
    void set_fifo_direction(Badge<FIFO>, FIFO::Direction direction);

    OwnPtr<OpenFileDescriptionData>& data();

    void set_original_inode(Badge<VirtualFileSystem>, NonnullLockRefPtr<Inode>&& inode) { m_inode = move(inode); }
    void set_original_custody(Badge<VirtualFileSystem>, Custody& custody);

    ErrorOr<void> truncate(u64);
    ErrorOr<void> sync();

    off_t offset() const;

    ErrorOr<void> chown(UserID, GroupID);

    FileBlockerSet& blocker_set();

    ErrorOr<void> apply_flock(Process const&, Userspace<flock const*>, ShouldBlock);
    ErrorOr<void> get_flock(Userspace<flock*>) const;

private:
    friend class VirtualFileSystem;
    explicit OpenFileDescription(File&);

    ErrorOr<void> attach();

    void evaluate_block_conditions()
    {
        blocker_set().unblock_all_blockers_whose_conditions_are_met();
    }

    LockRefPtr<Inode> m_inode;
    NonnullLockRefPtr<File> m_file;

    struct State {
        OwnPtr<OpenFileDescriptionData> data;
        RefPtr<Custody> custody;
        off_t current_offset { 0 };
        u32 file_flags { 0 };
        bool readable : 1 { false };
        bool writable : 1 { false };
        bool is_blocking : 1 { true };
        bool is_directory : 1 { false };
        bool should_append : 1 { false };
        bool direct : 1 { false };
        FIFO::Direction fifo_direction : 2 { FIFO::Direction::Neither };
    };

    SpinlockProtected<State> m_state { LockRank::None };
};
}
