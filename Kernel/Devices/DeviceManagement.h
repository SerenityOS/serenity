/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Error.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <Kernel/API/TimePage.h>
#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/ConsoleDevice.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/NullDevice.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class DeviceManagement {
    AK_MAKE_ETERNAL;

public:
    DeviceManagement();
    static void initialize();
    static DeviceManagement& the();
    void attach_null_device(NullDevice const&);

    bool is_console_device_attached() const { return !m_console_device.is_null(); }
    void attach_console_device(ConsoleDevice const&);

    // FIXME: Once we have a singleton for managing many sound cards, remove this from here
    void attach_audio_device(CharacterDevice const&);

    void after_inserting_device(Badge<Device>, Device&);
    void before_device_removal(Badge<Device>, Device&);

    void for_each(Function<void(Device&)>);
    Device* get_device(unsigned major, unsigned minor);

    NullDevice const& null_device() const;
    NullDevice& null_device();

    ConsoleDevice const& console_device() const;
    ConsoleDevice& console_device();

    template<typename DeviceType, typename... Args>
    static inline ErrorOr<NonnullRefPtr<DeviceType>> try_create_device(Args&&... args)
    {
        auto device = TRY(adopt_nonnull_ref_or_enomem(new DeviceType(forward<Args>(args)...)));
        device->after_inserting();
        return device;
    }

private:
    RefPtr<NullDevice> m_null_device;
    RefPtr<ConsoleDevice> m_console_device;
    // FIXME: Once we have a singleton for managing many sound cards, remove this from here
    NonnullRefPtrVector<CharacterDevice, 1> m_audio_devices;
    MutexProtected<HashMap<u32, Device*>> m_devices;
};

}
