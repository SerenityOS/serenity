/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/InodeMetadata.h>

namespace Kernel {

static Singleton<HashMap<u32, Device*>> s_all_devices;

HashMap<u32, Device*>& Device::all_devices()
{
    return *s_all_devices;
}

void Device::for_each(Function<void(Device&)> callback)
{
    for (auto& entry : all_devices())
        callback(*entry.value);
}

Device* Device::get_device(unsigned major, unsigned minor)
{
    auto it = all_devices().find(encoded_device(major, minor));
    if (it == all_devices().end())
        return nullptr;
    return it->value;
}

Device::Device(unsigned major, unsigned minor)
    : m_major(major)
    , m_minor(minor)
{
    u32 device_id = encoded_device(major, minor);
    auto it = all_devices().find(device_id);
    if (it != all_devices().end()) {
        dbgln("Already registered {},{}: {}", major, minor, it->value->class_name());
    }
    VERIFY(!all_devices().contains(device_id));
    all_devices().set(device_id, this);
}

Device::~Device()
{
    all_devices().remove(encoded_device(m_major, m_minor));
}

String Device::absolute_path() const
{
    // FIXME: Don't assume mount point for DevFs
    return String::formatted("/dev/{}", device_name());
}

String Device::absolute_path(const FileDescription&) const
{
    return absolute_path();
}

void Device::process_next_queued_request(Badge<AsyncDeviceRequest>, const AsyncDeviceRequest& completed_request)
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
