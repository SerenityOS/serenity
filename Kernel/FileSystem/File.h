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
public:
    FileBlockCondition() { }

    virtual bool should_add_blocker(Thread::Blocker& b, void* data) override
    {
        VERIFY(b.blocker_type() == Thread::Blocker::Type::File);
        auto& blocker = static_cast<Thread::FileBlocker&>(b);
        return !blocker.unblock(true, data);
    }

    void unblock()
    {
        ScopedSpinLock lock(m_lock);
        do_unblock([&](auto& b, void* data, bool&) {
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
    : public RefCounted<File>
    , public Weakable<File> {
public:
    virtual ~File();

    virtual KResultOr<NonnullRefPtr<FileDescription>> open(int options);
    virtual KResult close();

    virtual bool can_read(const FileDescription&, size_t) const = 0;
    virtual bool can_write(const FileDescription&, size_t) const = 0;

    virtual KResult attach(FileDescription&) { return KSuccess; }
    virtual void detach(FileDescription&) { }
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

    virtual FileBlockCondition& block_condition() { return m_block_condition; }

protected:
    File();

    void evaluate_block_conditions()
    {
        if (Processor::current().in_irq()) {
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
        VERIFY(!Processor::current().in_irq());
        block_condition().unblock();
    }

    FileBlockCondition m_block_condition;
};

}
