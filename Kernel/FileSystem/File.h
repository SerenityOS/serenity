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

class FileBlockerSet final : public Thread::BlockerSet {
public:
    FileBlockerSet() { }

    virtual bool should_add_blocker(Thread::Blocker& b, void* data) override
    {
        VERIFY(b.blocker_type() == Thread::Blocker::Type::File);
        auto& blocker = static_cast<Thread::FileBlocker&>(b);
        return !blocker.unblock(true, data);
    }

    void unblock_all_blockers_whose_conditions_are_met()
    {
        SpinlockLocker lock(m_lock);
        BlockerSet::unblock_all_blockers_whose_conditions_are_met_locked([&](auto& b, void* data, bool&) {
            VERIFY(b.blocker_type() == Thread::Blocker::Type::File);
            auto& blocker = static_cast<Thread::FileBlocker&>(b);
            return blocker.unblock(false, data);
        });
    }
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
    : public RefCountedBase
    , public Weakable<File> {
public:
    virtual bool unref() const;
    virtual ~File();

    virtual KResultOr<NonnullRefPtr<FileDescription>> open(int options);
    virtual KResult close();

    virtual bool can_read(FileDescription const&, size_t) const = 0;
    virtual bool can_write(FileDescription const&, size_t) const = 0;

    virtual KResult attach(FileDescription&);
    virtual void detach(FileDescription&);
    virtual void did_seek(FileDescription&, off_t) { }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) = 0;
    virtual KResultOr<size_t> write(FileDescription&, u64, UserOrKernelBuffer const&, size_t) = 0;
    virtual KResult ioctl(FileDescription&, unsigned request, Userspace<void*> arg);
    virtual KResultOr<Memory::Region*> mmap(Process&, FileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared);
    virtual KResult stat(::stat&) const { return EBADF; }

    virtual String absolute_path(FileDescription const&) const = 0;

    virtual KResult truncate(u64) { return EINVAL; }
    virtual KResult chown(FileDescription&, UserID, GroupID) { return EBADF; }
    virtual KResult chmod(FileDescription&, mode_t) { return EBADF; }

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
        if (Processor::current_in_irq()) {
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
        VERIFY(!Processor::current_in_irq());
        blocker_set().unblock_all_blockers_whose_conditions_are_met();
    }

    FileBlockerSet m_blocker_set;
    size_t m_attach_count { 0 };
};

}
