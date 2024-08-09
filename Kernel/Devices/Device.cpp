/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Devices/BaseDevices.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/BlockDevicesDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/CharacterDevicesDirectory.h>
#include <Kernel/Sections.h>

namespace Kernel {

struct AllDevicesDetails {
    SpinlockProtected<HashMap<u64, BlockDevice*>, LockRank::None> block_devices {};
    SpinlockProtected<HashMap<u64, CharacterDevice*>, LockRank::None> char_devices {};
    SpinlockProtected<CircularQueue<DeviceEvent, 100>, LockRank::None> event_queue {};
    // NOTE: There's no locking on this pointer because we expect to initialize it once
    // and never touch it again.
    OwnPtr<BaseDevices> base_devices;
};

static Singleton<AllDevicesDetails> s_all_details;

SpinlockProtected<CircularQueue<DeviceEvent, 100>, LockRank::None>& Device::event_queue()
{
    return s_all_details->event_queue;
}

BaseDevices* Device::base_devices()
{
    return s_all_details->base_devices.ptr();
}

UNMAP_AFTER_INIT void Device::initialize_base_devices()
{
    auto base_devices = MUST(adopt_nonnull_own_or_enomem(new (nothrow) BaseDevices(*NullDevice::must_initialize(), *ConsoleDevice::must_create(), *DeviceControlDevice::must_create())));
    s_all_details->base_devices = move(base_devices);
}

RefPtr<Device> Device::acquire_by_type_and_major_minor_numbers(DeviceNodeType type, MajorNumber major, MinorNumber minor)
{
    VERIFY(type == DeviceNodeType::Block || type == DeviceNodeType::Character);

    auto find_device_in_map = [major, minor](auto& map) -> RefPtr<Device> {
        auto it = map.find(encoded_device(major.value(), minor.value()));
        if (it == map.end())
            return nullptr;
        return *it->value;
    };

    if (type == DeviceNodeType::Block) {
        return s_all_details->block_devices.with([&](auto& map) -> RefPtr<Device> {
            return find_device_in_map(map);
        });
    }

    return s_all_details->char_devices.with([&](auto& map) -> RefPtr<Device> {
        return find_device_in_map(map);
    });
}

void Device::before_will_be_destroyed_remove_from_device_management()
{
    before_device_removal({}, *this);
    m_state = State::BeingRemoved;
}

void Device::after_inserting_add_to_device_management()
{
    after_inserting_device({}, *this);
}

Device::Device(MajorNumber major, MinorNumber minor)
    : m_major(major)
    , m_minor(minor)
{
}

ErrorOr<void> Device::after_inserting()
{
    VERIFY(!m_sysfs_component);
    auto sys_fs_component = SysFSDeviceComponent::must_create(*this);
    m_sysfs_component = sys_fs_component;
    after_inserting_add_to_device_identifier_directory();
    after_inserting_add_to_device_management();
    return {};
}

void Device::will_be_destroyed()
{
    VERIFY(m_sysfs_component);
    before_will_be_destroyed_remove_from_device_management();
    before_will_be_destroyed_remove_from_device_identifier_directory();
}

Device::~Device()
{
    VERIFY(m_state == State::BeingRemoved);
}

ErrorOr<NonnullOwnPtr<KString>> Device::pseudo_path(OpenFileDescription const&) const
{
    return KString::formatted("device:{},{}", major(), minor());
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> Device::open(int options)
{
    if (Process::current().is_jailed() && !is_openable_by_jailed_processes())
        return Error::from_errno(EPERM);
    return File::open(options);
}

void Device::process_next_queued_request(Badge<AsyncDeviceRequest>, AsyncDeviceRequest const& completed_request)
{
    SpinlockLocker lock(m_requests_lock);
    VERIFY(!m_requests.is_empty());
    VERIFY(m_requests.first().ptr() == &completed_request);
    m_requests.remove(m_requests.begin());
    if (!m_requests.is_empty()) {
        auto* next_request = m_requests.first().ptr();
        next_request->do_start(move(lock));
    }

    evaluate_block_conditions();
}

void Device::after_inserting_device(Badge<Device>, Device& device)
{
    if (device.is_block_device()) {
        s_all_details->block_devices.with([&](auto& map) -> void {
            add_device_to_map<BlockDevice>(map, device);
        });
    } else {
        VERIFY(device.is_character_device());
        s_all_details->char_devices.with([&](auto& map) -> void {
            add_device_to_map<CharacterDevice>(map, device);
        });
    }

    s_all_details->event_queue.with([&](auto& queue) {
        DeviceEvent event { DeviceEvent::State::Inserted, device.is_block_device(), device.major().value(), device.minor().value() };
        queue.enqueue(event);
    });

    if (s_all_details->base_devices)
        s_all_details->base_devices->device_control_device->evaluate_block_conditions();
}

void Device::before_device_removal(Badge<Device>, Device& device)
{
    u64 device_id = encoded_device(device.major(), device.minor());

    if (device.is_block_device()) {
        s_all_details->block_devices.with([&](auto& map) -> void {
            VERIFY(map.contains(device_id));
            map.remove(encoded_device(device.major(), device.minor()));
        });
    } else {
        VERIFY(device.is_character_device());
        s_all_details->char_devices.with([&](auto& map) -> void {
            VERIFY(map.contains(device_id));
            map.remove(encoded_device(device.major(), device.minor()));
        });
    }

    s_all_details->event_queue.with([&](auto& queue) {
        DeviceEvent event { DeviceEvent::State::Removed, device.is_block_device(), device.major().value(), device.minor().value() };
        queue.enqueue(event);
    });

    if (s_all_details->base_devices)
        s_all_details->base_devices->device_control_device->evaluate_block_conditions();
}

}
