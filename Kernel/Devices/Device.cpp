/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/Sections.h>

namespace Kernel {

Device::Device(MajorNumber major, MinorNumber minor)
    : m_major(major)
    , m_minor(minor)
{
}

void Device::after_inserting()
{
    DeviceManagement::the().after_inserting_device({}, *this);
    VERIFY(!m_sysfs_component);
    auto sys_fs_component = SysFSDeviceComponent::must_create(*this);
    m_sysfs_component = sys_fs_component;
    SysFSComponentRegistry::the().devices_list().with_exclusive([&](auto& list) -> void {
        list.append(sys_fs_component);
    });
}

void Device::will_be_destroyed()
{
    VERIFY(m_sysfs_component);
    SysFSComponentRegistry::the().devices_list().with_exclusive([&](auto& list) -> void {
        list.remove(*m_sysfs_component);
    });
    DeviceManagement::the().before_device_removal({}, *this);
    m_state = State::BeingRemoved;
}

Device::~Device()
{
    VERIFY(m_state == State::BeingRemoved);
}

ErrorOr<NonnullOwnPtr<KString>> Device::pseudo_path(OpenFileDescription const&) const
{
    return KString::formatted("device:{},{}", major(), minor());
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
