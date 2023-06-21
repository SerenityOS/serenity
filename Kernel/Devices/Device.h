/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Device is the base class of everything that lives in the /dev directory.
//
// To expose a Device to the filesystem, simply pass two unique numbers to the constructor,
// and then mknod a file in /dev with those numbers.
//
// There are two main subclasses:
//   - BlockDevice (random access)
//   - CharacterDevice (sequential)
#include <AK/DoublyLinkedList.h>
#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <Kernel/Devices/AsyncDeviceRequest.h>
#include <Kernel/FileSystem/DeviceFileTypes.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/DeviceComponent.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/SymbolicLinkDeviceComponent.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class Device : public File {
protected:
    enum class State {
        Normal,
        BeingRemoved,
    };

public:
    virtual ~Device() override;

    MajorNumber major() const { return m_major; }
    MinorNumber minor() const { return m_minor; }

    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(OpenFileDescription const&) const override;
    virtual ErrorOr<NonnullRefPtr<OpenFileDescription>> open(int options) override;

    UserID uid() const { return m_uid; }
    GroupID gid() const { return m_gid; }

    virtual bool is_device() const override { return true; }
    virtual void will_be_destroyed() override;
    virtual ErrorOr<void> after_inserting();
    virtual bool is_openable_by_jailed_processes() const { return false; }
    void process_next_queued_request(Badge<AsyncDeviceRequest>, AsyncDeviceRequest const&);

    template<typename AsyncRequestType, typename... Args>
    ErrorOr<NonnullLockRefPtr<AsyncRequestType>> try_make_request(Args&&... args)
    {
        auto request = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) AsyncRequestType(*this, forward<Args>(args)...)));
        SpinlockLocker lock(m_requests_lock);
        bool was_empty = m_requests.is_empty();
        TRY(m_requests.try_append(request));
        if (was_empty)
            request->do_start(move(lock));
        return request;
    }

protected:
    Device(MajorNumber major, MinorNumber minor);
    void set_uid(UserID uid) { m_uid = uid; }
    void set_gid(GroupID gid) { m_gid = gid; }

    void after_inserting_add_to_device_management();
    void before_will_be_destroyed_remove_from_device_management();

    virtual void after_inserting_add_symlink_to_device_identifier_directory() = 0;
    virtual void before_will_be_destroyed_remove_symlink_from_device_identifier_directory() = 0;

    // FIXME: These methods will be eventually removed after all nodes in /sys/dev/block/ and
    // /sys/dev/char/ are symlinks.
    virtual void after_inserting_add_to_device_identifier_directory() = 0;
    virtual void before_will_be_destroyed_remove_from_device_identifier_directory() = 0;

private:
    MajorNumber const m_major { 0 };
    MinorNumber const m_minor { 0 };
    UserID m_uid { 0 };
    GroupID m_gid { 0 };

    State m_state { State::Normal };

    Spinlock<LockRank::None> m_requests_lock {};
    DoublyLinkedList<LockRefPtr<AsyncDeviceRequest>> m_requests;

protected:
    // FIXME: This pointer will be eventually removed after all nodes in /sys/dev/block/ and
    // /sys/dev/char/ are symlinks.
    RefPtr<SysFSDeviceComponent> m_sysfs_component;

    RefPtr<SysFSSymbolicLinkDeviceComponent> m_symlink_sysfs_component;
    RefPtr<SysFSDirectory> m_sysfs_device_directory;
};

}
