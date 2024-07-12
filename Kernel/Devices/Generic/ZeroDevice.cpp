/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Generic/ZeroDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<ZeroDevice> ZeroDevice::must_create()
{
    return MUST(Device::try_create_device<ZeroDevice>());
}

UNMAP_AFTER_INIT ZeroDevice::ZeroDevice()
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::Generic, 5)
{
}

UNMAP_AFTER_INIT ZeroDevice::~ZeroDevice() = default;

bool ZeroDevice::can_read(OpenFileDescription const&, u64) const
{
    return true;
}

ErrorOr<size_t> ZeroDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    TRY(buffer.memset(0, size));
    return size;
}

ErrorOr<size_t> ZeroDevice::write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t size)
{
    return size;
}

}
