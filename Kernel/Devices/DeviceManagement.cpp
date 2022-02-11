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

UNMAP_AFTER_INIT void DeviceManagement::attach_console_device(ConsoleDevice const& device)
{
    m_console_device = device;
}

UNMAP_AFTER_INIT void DeviceManagement::attach_null_device(NullDevice const& device)
{
    m_null_device = device;
}

UNMAP_AFTER_INIT void DeviceManagement::attach_device_control_device(DeviceControlDevice const& device)
{
    m_device_control_device = device;
}

DeviceManagement& DeviceManagement::the()
{
    return *s_the;
}

Device* DeviceManagement::get_device(MajorNumber major, MinorNumber minor)
{
    return m_devices.with([&](auto& map) -> Device* {
        auto it = map.find(encoded_device(major.value(), minor.value()));
        if (it == map.end())
            return nullptr;
        return it->value;
    });
}

Optional<DeviceEvent> DeviceManagement::dequeue_top_device_event(Badge<DeviceControlDevice>)
{
    SpinlockLocker locker(m_event_queue_lock);
    if (m_event_queue.is_empty())
        return {};
    return m_event_queue.dequeue();
}

void DeviceManagement::before_device_removal(Badge<Device>, Device& device)
{
    u64 device_id = encoded_device(device.major(), device.minor());
    m_devices.with([&](auto& map) -> void {
        VERIFY(map.contains(device_id));
        map.remove(encoded_device(device.major(), device.minor()));
    });

    {
        DeviceEvent event { DeviceEvent::State::Removed, device.is_block_device(), device.major().value(), device.minor().value() };
        SpinlockLocker locker(m_event_queue_lock);
        m_event_queue.enqueue(event);
    }
    if (m_device_control_device)
        m_device_control_device->evaluate_block_conditions();
}

void DeviceManagement::after_inserting_device(Badge<Device>, Device& device)
{
    u64 device_id = encoded_device(device.major(), device.minor());
    m_devices.with([&](auto& map) -> void {
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

    {
        DeviceEvent event { DeviceEvent::State::Inserted, device.is_block_device(), device.major().value(), device.minor().value() };
        SpinlockLocker locker(m_event_queue_lock);
        m_event_queue.enqueue(event);
    }
    if (m_device_control_device)
        m_device_control_device->evaluate_block_conditions();
}

void DeviceManagement::for_each(Function<void(Device&)> callback)
{
    m_devices.with([&](auto& map) -> void {
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
