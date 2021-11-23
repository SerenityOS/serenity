/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <AK/Singleton.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<DeviceManagement> s_the;

UNMAP_AFTER_INIT DeviceManagement::DeviceManagement()
{
}
UNMAP_AFTER_INIT void DeviceManagement::initialize()
{
    s_the.ensure_instance();
}

UNMAP_AFTER_INIT void DeviceManagement::attach_audio_device(CharacterDevice const& device)
{
    m_audio_devices.append(device);
}

UNMAP_AFTER_INIT void DeviceManagement::attach_console_device(ConsoleDevice const& device)
{
    m_console_device = device;
}

UNMAP_AFTER_INIT void DeviceManagement::attach_null_device(NullDevice const& device)
{
    m_null_device = device;
}

DeviceManagement& DeviceManagement::the()
{
    return *s_the;
}

Device* DeviceManagement::get_device(unsigned major, unsigned minor)
{
    return m_devices.with_exclusive([&](auto& map) -> Device* {
        auto it = map.find(encoded_device(major, minor));
        if (it == map.end())
            return nullptr;
        return it->value;
    });
}

void DeviceManagement::before_device_removal(Badge<Device>, Device& device)
{
    u32 device_id = encoded_device(device.major(), device.minor());
    m_devices.with_exclusive([&](auto& map) -> void {
        VERIFY(map.contains(device_id));
        map.remove(encoded_device(device.major(), device.minor()));
    });
}

void DeviceManagement::after_inserting_device(Badge<Device>, Device& device)
{
    u32 device_id = encoded_device(device.major(), device.minor());
    m_devices.with_exclusive([&](auto& map) -> void {
        if (map.contains(device_id)) {
            dbgln("Already registered {},{}: {}", device.major(), device.minor(), device.class_name());
            VERIFY_NOT_REACHED();
        }
        auto result = map.set(device_id, &device);
        if (result != AK::HashSetResult::InsertedNewEntry) {
            dbgln("Failed to register {},{}: {}", device.major(), device.minor(), device.class_name());
            VERIFY_NOT_REACHED();
        }
    });
}

void DeviceManagement::for_each(Function<void(Device&)> callback)
{
    m_devices.with_exclusive([&](auto& map) -> void {
        for (auto& entry : map)
            callback(*entry.value);
    });
}

NullDevice& DeviceManagement::null_device()
{
    return *m_null_device;
}

NullDevice const& DeviceManagement::null_device() const
{
    return *m_null_device;
}

ConsoleDevice const& DeviceManagement::console_device() const
{
    return *m_console_device;
}
ConsoleDevice& DeviceManagement::console_device()
{
    return *m_console_device;
}

}
