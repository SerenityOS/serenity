/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Weakable.h>
#include <Kernel/Forward.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/UserOrKernelBuffer.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel {

class File;

class FileBlockerSet final : public Thread::BlockerSet {
public:
    FileBlockerSet() { }

    virtual bool should_add_blocker(Thread::Blocker& b, void* data) override
    {
        VERIFY(b.blocker_type() == Thread::Blocker::Type::File);
        auto& blocker = static_cast<Thread::FileBlocker&>(b);
        return !blocker.unblock_if_conditions_are_met(true, data);
    }

    void unblock_all_blockers_whose_conditions_are_met()
    {
        SpinlockLocker lock(m_lock);
        BlockerSet::unblock_all_blockers_whose_conditions_are_met_locked([&](auto& b, void* data, bool&) {
            VERIFY(b.blocker_type() == Thread::Blocker::Type::File);
            auto& blocker = static_cast<Thread::FileBlocker&>(b);
            return blocker.unblock_if_conditions_are_met(false, data);
        });
    }
};

// File is the base class for anything that can be referenced by a OpenFileDescription.
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
public:
    virtual bool unref() const { return RefCounted<File>::unref(); }
    virtual void will_be_destroyed() { }
    virtual ~File();

    virtual ErrorOr<NonnullRefPtr<OpenFileDescription>> open(int options);
    virtual ErrorOr<void> close();

    virtual bool can_read(const OpenFileDescription&, u64) const = 0;
    virtual bool can_write(const OpenFileDescription&, u64) const = 0;

    virtual ErrorOr<void> attach(OpenFileDescription&);
    virtual void detach(OpenFileDescription&);
    virtual void did_seek(OpenFileDescription&, off_t) { }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) = 0;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) = 0;
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg);
    virtual ErrorOr<Memory::Region*> mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared);
    virtual ErrorOr<struct stat> stat() const { return EBADF; }

    // Although this might be better described "name" or "description", these terms already have other meanings.
    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(const OpenFileDescription&) const = 0;

    virtual ErrorOr<void> truncate(u64) { return EINVAL; }
    virtual ErrorOr<void> sync() { return EINVAL; }
    virtual ErrorOr<void> chown(OpenFileDescription&, UserID, GroupID) { return EBADF; }
    virtual ErrorOr<void> chmod(OpenFileDescription&, mode_t) { return EBADF; }

    virtual StringView class_name() const = 0;

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

    virtual FileBlockerSet& blocker_set() { return m_blocker_set; }

    size_t attach_count() const { return m_attach_count; }

protected:
    File();

    void evaluate_block_conditions()
    {
        if (Processor::current_in_irq() != 0) {
            // If called from an IRQ handler we need to delay evaluation
            // and unblocking of waiting threads. Note that this File
            // instance may be deleted until the deferred call is executed!
            Processor::deferred_call_queue([self = try_make_weak_ptr().release_value_but_fixme_should_propagate_errors()]() {
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
        VERIFY(!Processor::current_in_irq());
        blocker_set().unblock_all_blockers_whose_conditions_are_met();
    }

    FileBlockerSet m_blocker_set;
    size_t m_attach_count { 0 };
};

}
