/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/RefCounted.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KBuffer.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel {

class FileDescriptionData {
public:
    virtual ~FileDescriptionData() = default;
};

class FileDescription : public RefCounted<FileDescription> {
    MAKE_SLAB_ALLOCATED(FileDescription)
public:
    static KResultOr<NonnullRefPtr<FileDescription>> create(Custody&);
    static KResultOr<NonnullRefPtr<FileDescription>> create(File&);
    ~FileDescription();

    Thread::FileBlocker::BlockFlags should_unblock(Thread::FileBlocker::BlockFlags) const;

    bool is_readable() const { return m_readable; }
    bool is_writable() const { return m_writable; }

    void set_readable(bool b) { m_readable = b; }
    void set_writable(bool b) { m_writable = b; }

    void set_rw_mode(int options)
    {
        set_readable(options & O_RDONLY);
        set_writable(options & O_WRONLY);
    }

    KResult close();

    KResultOr<off_t> seek(off_t, int whence);
    KResultOr<size_t> read(UserOrKernelBuffer&, size_t);
    KResultOr<size_t> write(const UserOrKernelBuffer& data, size_t);
    KResult stat(::stat&);

    // NOTE: These ignore the current offset of this file description.
    KResultOr<size_t> read(UserOrKernelBuffer&, u64 offset, size_t);
    KResultOr<size_t> write(u64 offset, UserOrKernelBuffer const&, size_t);

    KResult chmod(mode_t);

    bool can_read() const;
    bool can_write() const;

    KResultOr<size_t> get_dir_entries(UserOrKernelBuffer& buffer, size_t);

    KResultOr<NonnullOwnPtr<KBuffer>> read_entire_file();

    String absolute_path() const;

    bool is_direct() const { return m_direct; }

    bool is_directory() const { return m_is_directory; }

    File& file() { return *m_file; }
    const File& file() const { return *m_file; }

    bool is_device() const;
    const Device* device() const;
    Device* device();

    bool is_tty() const;
    const TTY* tty() const;
    TTY* tty();

    bool is_inode_watcher() const;
    const InodeWatcher* inode_watcher() const;
    InodeWatcher* inode_watcher();

    bool is_master_pty() const;
    const MasterPTY* master_pty() const;
    MasterPTY* master_pty();

    InodeMetadata metadata() const;
    Inode* inode() { return m_inode.ptr(); }
    const Inode* inode() const { return m_inode.ptr(); }

    Custody* custody() { return m_custody.ptr(); }
    const Custody* custody() const { return m_custody.ptr(); }

    KResultOr<Memory::Region*> mmap(Process&, Memory::VirtualRange const&, u64 offset, int prot, bool shared);

    bool is_blocking() const { return m_is_blocking; }
    void set_blocking(bool b) { m_is_blocking = b; }
    bool should_append() const { return m_should_append; }
    void set_should_append(bool s) { m_should_append = s; }

    u32 file_flags() const { return m_file_flags; }
    void set_file_flags(u32);

    bool is_socket() const;
    Socket* socket();
    const Socket* socket() const;

    bool is_fifo() const;
    FIFO* fifo();
    FIFO::Direction fifo_direction() const { return m_fifo_direction; }
    void set_fifo_direction(Badge<FIFO>, FIFO::Direction direction) { m_fifo_direction = direction; }

    OwnPtr<FileDescriptionData>& data() { return m_data; }

    void set_original_inode(Badge<VirtualFileSystem>, NonnullRefPtr<Inode>&& inode) { m_inode = move(inode); }

    KResult truncate(u64);

    off_t offset() const { return m_current_offset; }

    KResult chown(uid_t, gid_t);

    FileBlockerSet& blocker_set();

    KResult apply_flock(Process const&, Userspace<flock const*>);
    KResult get_flock(Userspace<flock*>) const;

private:
    friend class VirtualFileSystem;
    explicit FileDescription(File&);

    KResult attach();

    void evaluate_block_conditions()
    {
        blocker_set().unblock_all_blockers_whose_conditions_are_met();
    }

    RefPtr<Custody> m_custody;
    RefPtr<Inode> m_inode;
    NonnullRefPtr<File> m_file;

    off_t m_current_offset { 0 };

    OwnPtr<FileDescriptionData> m_data;

    u32 m_file_flags { 0 };

    bool m_readable : 1 { false };
    bool m_writable : 1 { false };
    bool m_is_blocking : 1 { true };
    bool m_is_directory : 1 { false };
    bool m_should_append : 1 { false };
    bool m_direct : 1 { false };
    FIFO::Direction m_fifo_direction { FIFO::Direction::Neither };

    Mutex m_lock { "FileDescription" };
};

}
