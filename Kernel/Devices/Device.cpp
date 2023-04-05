/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/BlockDevicesDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/CharacterDevicesDirectory.h>
#include <Kernel/Sections.h>

namespace Kernel {

Device::Device(MajorNumber major, MinorNumber minor)
    : m_major(major)
    , m_minor(minor)
{
}

void Device::before_will_be_destroyed_remove_from_device_management()
{
    DeviceManagement::the().before_device_removal({}, *this);
    m_state = State::BeingRemoved;
}

void Device::after_inserting_add_to_device_management()
{
    DeviceManagement::the().after_inserting_device({}, *this);
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
    TRY(Process::current().jail().with([&](auto const& my_jail) -> ErrorOr<void> {
        if (my_jail && !is_openable_by_jailed_processes())
            return Error::from_errno(EPERM);
        return {};
    }));
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

}
