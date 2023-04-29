/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Error.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/API/DeviceEvent.h>
#include <Kernel/API/TimePage.h>
#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Generic/ConsoleDevice.h>
#include <Kernel/Devices/Generic/DeviceControlDevice.h>
#include <Kernel/Devices/Generic/NullDevice.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class DeviceManagement {

public:
    DeviceManagement();
    static void initialize();
    static DeviceManagement& the();
    void attach_null_device(NullDevice const&);

    void attach_device_control_device(DeviceControlDevice const&);

    bool is_console_device_attached() const { return !m_console_device.is_null(); }
    void attach_console_device(ConsoleDevice const&);

    void after_inserting_device(Badge<Device>, Device&);
    void before_device_removal(Badge<Device>, Device&);

    void for_each(Function<void(Device&)>);
    ErrorOr<void> try_for_each(Function<ErrorOr<void>(Device&)>);
    Device* get_device(MajorNumber major, MinorNumber minor);

    NullDevice const& null_device() const;
    NullDevice& null_device();

    ConsoleDevice const& console_device() const;
    ConsoleDevice& console_device();

    template<typename DeviceType, typename... Args>
    static inline ErrorOr<NonnullLockRefPtr<DeviceType>> try_create_device(Args&&... args)
    requires(requires(Args... args) { DeviceType::try_create(args...); })
    {
        auto device = TRY(DeviceType::try_create(forward<Args>(args)...));
        TRY(device->after_inserting());
        return device;
    }

    template<typename DeviceType, typename... Args>
    static inline ErrorOr<NonnullLockRefPtr<DeviceType>> try_create_device(Args&&... args)
    {
        auto device = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) DeviceType(forward<Args>(args)...)));
        TRY(device->after_inserting());
        return device;
    }

    SpinlockProtected<CircularQueue<DeviceEvent, 100>, LockRank::None>& event_queue(Badge<DeviceControlDevice>);

private:
    LockRefPtr<NullDevice> m_null_device;
    LockRefPtr<ConsoleDevice> m_console_device;
    LockRefPtr<DeviceControlDevice> m_device_control_device;
    SpinlockProtected<HashMap<u64, Device*>, LockRank::None> m_devices {};
    SpinlockProtected<CircularQueue<DeviceEvent, 100>, LockRank::None> m_event_queue {};
};

}
