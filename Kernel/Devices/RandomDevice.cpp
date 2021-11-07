/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Random.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<RandomDevice> RandomDevice::must_create()
{
    auto random_device_or_error = DeviceManagement::try_create_device<RandomDevice>();
    // FIXME: Find a way to propagate errors
    VERIFY(!random_device_or_error.is_error());
    return random_device_or_error.release_value();
}

UNMAP_AFTER_INIT RandomDevice::RandomDevice()
    : CharacterDevice(1, 8)
{
}

UNMAP_AFTER_INIT RandomDevice::~RandomDevice()
{
}

bool RandomDevice::can_read(const OpenFileDescription&, size_t) const
{
    return true;
}

ErrorOr<size_t> RandomDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    return buffer.write_buffered<256>(size, [&](Bytes bytes) {
        get_good_random_bytes(bytes);
        return bytes.size();
    });
}

ErrorOr<size_t> RandomDevice::write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t size)
{
    // FIXME: Use input for entropy? I guess that could be a neat feature?
    return size;
}

}
