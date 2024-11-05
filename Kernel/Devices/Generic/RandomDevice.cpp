/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Generic/RandomDevice.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<RandomDevice> RandomDevice::must_create()
{
    return MUST(Device::try_create_device<RandomDevice>());
}

UNMAP_AFTER_INIT RandomDevice::RandomDevice()
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::Generic, 8)
{
}

UNMAP_AFTER_INIT RandomDevice::~RandomDevice() = default;

bool RandomDevice::can_read(OpenFileDescription const&, u64) const
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

ErrorOr<size_t> RandomDevice::write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t size)
{
    // FIXME: Use input for entropy? I guess that could be a neat feature?
    return size;
}

}
