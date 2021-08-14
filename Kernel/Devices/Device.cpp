/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<MutexProtected<HashMap<u32, Device*>>> s_all_devices;

MutexProtected<HashMap<u32, Device*>>& Device::all_devices()
{
    return *s_all_devices;
}

void Device::for_each(Function<void(Device&)> callback)
{
    all_devices().with_exclusive([&](auto& map) -> void {
        for (auto& entry : map)
            callback(*entry.value);
    });
}

Device* Device::get_device(unsigned major, unsigned minor)
{
    return all_devices().with_exclusive([&](auto& map) -> Device* {
        auto it = map.find(encoded_device(major, minor));
        if (it == map.end())
            return nullptr;
        return it->value;
    });
}

Device::Device(unsigned major, unsigned minor)
    : m_major(major)
    , m_minor(minor)
{
    u32 device_id = encoded_device(major, minor);
    all_devices().with_exclusive([&](auto& map) -> void {
        auto it = map.find(device_id);
        if (it != map.end()) {
            dbgln("Already registered {},{}: {}", major, minor, it->value->class_name());
        }
        VERIFY(!map.contains(device_id));
        map.set(device_id, this);
    });
}

Device::~Device()
{
    u32 device_id = encoded_device(m_major, m_minor);
    all_devices().with_exclusive([&](auto& map) -> void {
        VERIFY(map.contains(device_id));
        map.remove(encoded_device(m_major, m_minor));
    });
}

String Device::absolute_path() const
{
    // FIXME: I assume we can't really provide a well known path in the kernel
    // because this is a violation of abstraction layers between userland and the
    // kernel, but maybe the whole name of "absolute_path" is just wrong as this
    // is really not an "absolute_path".
    return String::formatted("device:{},{}", major(), minor());
}

String Device::absolute_path(const OpenFileDescription&) const
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
