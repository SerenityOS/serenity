/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

/* The Device is the base class of everything that lives in the /dev directory.
 * Although it doesn't inherit from the File class, they're closely related,
 * the DeviceFile class will weakly hold an object of this class, so this type
 * of object can vanish (being removed) at any time.
 * 
 * To expose a Device to the filesystem, simply pass two unique numbers to the constructor,
 * and then mknod a file in /dev with those numbers.
 *
 * There are two main subclasses:
 *   - BlockDevice (random access)
 *   - CharacterDevice (sequential)
*/
#include <AK/DoublyLinkedList.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Weakable.h>
#include <Kernel/API/KResult.h>
#include <Kernel/Devices/AsyncDeviceRequest.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/UserOrKernelBuffer.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel {

class DeviceFile;
class Device
    : public RefCountedBase
    , public Weakable<Device> {
protected:
    enum class State {
        Normal,
        BeingRemoved,
    };

public:
    virtual bool unref() const;
    virtual ~Device();

    virtual void did_seek(OpenFileDescription&, off_t) {};
    virtual String absolute_path() const;
    virtual KResultOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) = 0;
    virtual bool can_read() const = 0;
    virtual KResultOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) = 0;
    virtual bool can_write() const = 0;
    virtual KResult ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg);
    virtual KResultOr<Memory::Region*> mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared);
    virtual KResultOr<NonnullRefPtr<OpenFileDescription>> open(int options);
    virtual KResult close() { return KSuccess; }
    virtual bool is_block_device() const { return false; }
    virtual bool is_character_device() const { return false; }
    virtual bool is_seekable() const { return false; }
    virtual bool is_tty() const { return false; }
    virtual bool is_master_pty() const { return false; }
    virtual StringView class_name() const = 0;

    const TTY* as_tty() const;
    TTY* as_tty();
    const MasterPTY* as_master_pty() const;
    MasterPTY* as_master_pty();
    const BlockDevice* as_block_device() const;
    BlockDevice* as_block_device();
    const CharacterDevice* as_character_device() const;
    CharacterDevice* as_character_device();

    virtual FileBlockerSet& blocker_set() { return m_blocker_set; }

    unsigned major() const { return m_major; }
    unsigned minor() const { return m_minor; }

    UserID uid() const { return m_uid; }
    GroupID gid() const { return m_gid; }

    virtual void before_removing();
    virtual void after_inserting();
    ALWAYS_INLINE void do_evaluate_device_block_conditions(Badge<DeviceFile>)
    {
        VERIFY(!Processor::current_in_irq());
        m_blocker_set.unblock_all_blockers_whose_conditions_are_met();
    }
    void process_next_queued_request(Badge<AsyncDeviceRequest>, const AsyncDeviceRequest&);

    template<typename AsyncRequestType, typename... Args>
    KResultOr<NonnullRefPtr<AsyncRequestType>> try_make_request(Args&&... args)
    {
        auto request = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) AsyncRequestType(*this, forward<Args>(args)...)));
        SpinlockLocker lock(m_requests_lock);
        bool was_empty = m_requests.is_empty();
        m_requests.append(request);
        if (was_empty)
            request->do_start(move(lock));
        return request;
    }

protected:
    Device(unsigned major, unsigned minor);
    void set_uid(UserID uid) { m_uid = uid; }
    void set_gid(GroupID gid) { m_gid = gid; }

    void evaluate_block_conditions()
    {
        if (Processor::current_in_irq()) {
            // If called from an IRQ handler we need to delay evaluation
            // and unblocking of waiting threads. Note that this File
            // instance may be deleted until the deferred call is executed!
            Processor::deferred_call_queue([self = make_weak_ptr()]() {
                if (auto device = self.strong_ref())
                    device->do_evaluate_block_conditions();
            });
        } else {
            do_evaluate_block_conditions();
        }
    }

    ALWAYS_INLINE void do_evaluate_block_conditions()
    {
        VERIFY(!Processor::current_in_irq());
        m_blocker_set.unblock_all_blockers_whose_conditions_are_met();
    }

private:
    unsigned m_major { 0 };
    unsigned m_minor { 0 };
    UserID m_uid { 0 };
    GroupID m_gid { 0 };

    State m_state { State::Normal };

    Spinlock m_requests_lock;
    DoublyLinkedList<RefPtr<AsyncDeviceRequest>> m_requests;
    RefPtr<SysFSDeviceComponent> m_sysfs_component;
    FileBlockerSet m_blocker_set;
};

}
