/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Generic/NullDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<NullDevice> NullDevice::must_initialize()
{
    return MUST(Device::try_create_device<NullDevice>());
}

UNMAP_AFTER_INIT NullDevice::NullDevice()
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::Generic, 3)
{
}

UNMAP_AFTER_INIT NullDevice::~NullDevice() = default;

bool NullDevice::can_read(OpenFileDescription const&, u64) const
{
    return true;
}

ErrorOr<size_t> NullDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t)
{
    return 0;
}

ErrorOr<size_t> NullDevice::write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t buffer_size)
{
    return buffer_size;
}

}
