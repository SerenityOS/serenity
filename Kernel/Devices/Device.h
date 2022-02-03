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
#include <AK/RefPtr.h>
#include <Kernel/Devices/AsyncDeviceRequest.h>
#include <Kernel/FileSystem/DeviceFileTypes.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/SysFS.h>
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

    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(const OpenFileDescription&) const override;

    UserID uid() const { return m_uid; }
    GroupID gid() const { return m_gid; }

    virtual bool is_device() const override { return true; }
    virtual void will_be_destroyed() override;
    virtual void after_inserting();
    void process_next_queued_request(Badge<AsyncDeviceRequest>, const AsyncDeviceRequest&);

    template<typename AsyncRequestType, typename... Args>
    ErrorOr<NonnullRefPtr<AsyncRequestType>> try_make_request(Args&&... args)
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
    Device(MajorNumber major, MinorNumber minor);
    void set_uid(UserID uid) { m_uid = uid; }
    void set_gid(GroupID gid) { m_gid = gid; }

private:
    MajorNumber m_major { 0 };
    MinorNumber m_minor { 0 };
    UserID m_uid { 0 };
    GroupID m_gid { 0 };

    State m_state { State::Normal };

    Spinlock m_requests_lock;
    DoublyLinkedList<RefPtr<AsyncDeviceRequest>> m_requests;
    RefPtr<SysFSDeviceComponent> m_sysfs_component;
};

}
