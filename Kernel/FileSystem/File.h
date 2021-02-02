/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Weakable.h>
#include <Kernel/Forward.h>
#include <Kernel/KResult.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/UserOrKernelBuffer.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel {

class File;

class FileBlockCondition : public Thread::BlockCondition {
    friend class Thread::ReadBlocker;
    friend class Thread::WriteBlocker;
public:
    FileBlockCondition(File& file)
        : m_file(file)
    {
    }

    virtual bool should_add_blocker(Thread::Blocker& b, void* data) override
    {
        VERIFY(m_lock.is_locked());
        VERIFY(b.blocker_type() == Thread::Blocker::Type::File);
        auto& blocker = static_cast<Thread::FileBlocker&>(b);
        // If this is a ReadBlocker or a WriteBlocker, check if there is
        // one pending already. If so, block until those are done.
        switch (blocker.file_block_type()) {
        case Thread::FileBlocker::FileBlockType::Select:
        case Thread::FileBlocker::FileBlockType::FileDescription:
            break;
        case Thread::FileBlocker::FileBlockType::Read: {
            auto& read_blocker = static_cast<Thread::ReadBlocker&>(blocker);
            VERIFY(!read_blocker.m_list_node.is_in_list());
            if (!m_pending_readers.is_empty())
                return true;
            break;
        }
        case Thread::FileBlocker::FileBlockType::Write: {
            auto& write_blocker = static_cast<Thread::WriteBlocker&>(blocker);
            VERIFY(!write_blocker.m_list_node.is_in_list());
            if (!m_pending_writers.is_empty())
                return true;
            break;
        }
        }

        bool should_add = !blocker.unblock(true, data);
        switch (blocker.file_block_type()) {
        case Thread::FileBlocker::FileBlockType::Select:
        case Thread::FileBlocker::FileBlockType::FileDescription:
            break;
        case Thread::FileBlocker::FileBlockType::Read: {
            VERIFY(!should_add);
            auto& read_blocker = static_cast<Thread::ReadBlocker&>(blocker);
            // A read by the blocked thread is now pending
            VERIFY(!read_blocker.m_list_node.is_in_list());
            m_pending_readers.append(read_blocker);
            break;
        }
        case Thread::FileBlocker::FileBlockType::Write: {
            VERIFY(!should_add);
            auto& write_blocker = static_cast<Thread::WriteBlocker&>(blocker);
            // A write by the blocked thread is now pending
            VERIFY(!write_blocker.m_list_node.is_in_list());
            m_pending_writers.append(write_blocker);
            break;
        }
        }
        return should_add;
    }

    void unblock()
    {
        VERIFY(!m_state_lock.own_exclusive());
        ScopedSpinLock lock(m_lock);
        unblock_locked();
    }

    ALWAYS_INLINE RecursiveSharedSpinLock& state_lock() { return m_state_lock; }

protected:
    void unblock_locked()
    {
        bool unblocked_reader = !m_pending_readers.is_empty();
        bool unblocked_writer = !m_pending_writers.is_empty();
        
        do_unblock([&](auto& b, void* data, bool&) {
            VERIFY(b.blocker_type() == Thread::Blocker::Type::File);
            auto& blocker = static_cast<Thread::FileBlocker&>(b);
            switch (blocker.file_block_type()) {
            case Thread::FileBlocker::FileBlockType::Select:
            case Thread::FileBlocker::FileBlockType::FileDescription:
                break;
            case Thread::FileBlocker::FileBlockType::Read:
                // If a read is already in progress, don't unblock more readers
                if (unblocked_reader)
                    return false;
                break;
            case Thread::FileBlocker::FileBlockType::Write:
                // If a write is already in progress, don't unblock more writers
                if (unblocked_writer)
                    return false;
                break;
            }
            if (blocker.unblock(false, data)) {
                switch (blocker.file_block_type()) {
                case Thread::FileBlocker::FileBlockType::Select:
                case Thread::FileBlocker::FileBlockType::FileDescription:
                    break;
                case Thread::FileBlocker::FileBlockType::Read:
                    // We unblocked one reader
                    unblocked_reader = true;
                    VERIFY(m_pending_readers.contains(static_cast<Thread::ReadBlocker&>(blocker)));
                    break;
                case Thread::FileBlocker::FileBlockType::Write:
                    // We unblocked one writer
                    unblocked_writer = true;
                    VERIFY(m_pending_writers.contains(static_cast<Thread::WriteBlocker&>(blocker)));
                    break;
                }
                return true;
            }
            return false;
        });
    }

    void async_read_complete(Thread::ReadBlocker& blocker)
    {
        ScopedSpinLock lock(m_lock);
        VERIFY(m_pending_readers.contains(blocker));
        VERIFY(m_pending_readers.first() == &blocker);
        m_pending_readers.remove(blocker);
        unblock_locked();
    }

    void async_write_complete(Thread::WriteBlocker& blocker)
    {
        ScopedSpinLock lock(m_lock);
        VERIFY(m_pending_writers.contains(blocker));
        VERIFY(m_pending_writers.first() == &blocker);
        m_pending_writers.remove(blocker);
        unblock_locked();
    }

private:
    File& m_file;
    IntrusiveList<Thread::ReadBlocker, RawPtr<Thread::ReadBlocker>, &Thread::ReadBlocker::m_list_node> m_pending_readers;
    IntrusiveList<Thread::WriteBlocker, RawPtr<Thread::WriteBlocker>, &Thread::WriteBlocker::m_list_node> m_pending_writers;
    RecursiveSharedSpinLock m_state_lock;
};

// File is the base class for anything that can be referenced by a FileDescription.
//
// The most important functions in File are:
//
// read() and write()
//   - Implement reading and writing.
//   - Return the number of bytes read/written, OR a negative error code.
//
// can_read() and can_write()
//
//   - Used to implement blocking I/O, and the select() and poll() syscalls.
//   - Return true if read() or write() would succeed, respectively.
//   - Note that can_read() should return true in EOF conditions,
//     and a subsequent call to read() should return 0.
//
// ioctl()
//
//   - Optional. If unimplemented, ioctl() on this File will fail with -ENOTTY.
//   - Can be overridden in subclasses to implement arbitrary functionality.
//   - Subclasses should take care to validate incoming addresses before dereferencing.
//
// mmap()
//
//   - Optional. If unimplemented, mmap() on this File will fail with -ENODEV.
//   - Called by mmap() when userspace wants to memory-map this File somewhere.
//   - Should create a Region in the Process and return it if successful.

class File
    : public RefCounted<File>
    , public Weakable<File> {
    
    friend class ScopedFileStateUpdateLock;

public:
    virtual ~File();

    virtual KResultOr<NonnullRefPtr<FileDescription>> open(int options);
    virtual KResult close();

    virtual bool can_read(const FileDescription&, size_t) const = 0;
    virtual bool can_write(const FileDescription&, size_t) const = 0;

    virtual KResult attach(FileDescription&);
    virtual void detach(FileDescription&);
    virtual void did_seek(FileDescription&, off_t) { }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) = 0;
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) = 0;
    virtual int ioctl(FileDescription&, unsigned request, FlatPtr arg);
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, const Range&, u64 offset, int prot, bool shared);
    virtual KResult stat(::stat&) const { return EBADF; }

    virtual String absolute_path(const FileDescription&) const = 0;

    virtual KResult truncate(u64) { return EINVAL; }
    virtual KResult chown(FileDescription&, uid_t, gid_t) { return EBADF; }
    virtual KResult chmod(FileDescription&, mode_t) { return EBADF; }

    virtual const char* class_name() const = 0;

    virtual bool is_seekable() const { return false; }

    virtual bool is_inode() const { return false; }
    virtual bool is_fifo() const { return false; }
    virtual bool is_device() const { return false; }
    virtual bool is_tty() const { return false; }
    virtual bool is_master_pty() const { return false; }
    virtual bool is_block_device() const { return false; }
    virtual bool is_character_device() const { return false; }
    virtual bool is_socket() const { return false; }
    virtual bool is_inode_watcher() const { return false; }

    virtual FileBlockCondition& block_condition() const { return m_block_condition; }

    ALWAYS_INLINE RecursiveSharedSpinLock& state_lock() const
    {
        return block_condition().state_lock();
    }

    size_t attach_count() const { return m_attach_count; }

protected:
    File();

    void evaluate_block_conditions()
    {
        if (Processor::in_irq()) {
            // If called from an IRQ handler we need to delay evaluation
            // and unblocking of waiting threads. Note that this File
            // instance may be deleted until the deferred call is executed!
            Processor::deferred_call_queue([self = make_weak_ptr()]() {
                if (auto file = self.strong_ref())
                    file->do_evaluate_block_conditions();
            });
        } else {
            do_evaluate_block_conditions();
        }
    }

private:
    ALWAYS_INLINE void do_evaluate_block_conditions()
    {
        VERIFY(!Processor::in_irq());
        block_condition().unblock();
    }

    mutable FileBlockCondition m_block_condition;
    size_t m_attach_count { 0 };
};


class ScopedFileStateUpdateLock {
    AK_MAKE_NONCOPYABLE(ScopedFileStateUpdateLock);
    AK_MAKE_NONMOVABLE(ScopedFileStateUpdateLock);
public:
    ScopedFileStateUpdateLock(File& file)
        : m_file(file)
        , m_state_lock(file.state_lock())
    {
    }

    ~ScopedFileStateUpdateLock()
    {
        if (m_state_changed) {
            m_state_lock.unlock();
            m_file.evaluate_block_conditions();
        }
    }

    ALWAYS_INLINE void lock()
    {
        m_state_lock.lock();
    }

    ALWAYS_INLINE void unlock()
    {
        m_state_lock.unlock();
    }

    [[nodiscard]] ALWAYS_INLINE bool own_lock() const
    {
        return m_state_lock.own_lock();
    }

    ALWAYS_INLINE void did_change_state()
    {
        VERIFY(own_lock());
        m_state_changed = true;
    }
private:
    File& m_file;
    ScopedExclusiveSpinLock<RecursiveSharedSpinLock> m_state_lock;
    bool m_state_changed { false };
};

}
