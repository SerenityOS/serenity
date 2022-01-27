/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/ZeroDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<ZeroDevice> ZeroDevice::must_create()
{
    auto zero_device_or_error = DeviceManagement::try_create_device<ZeroDevice>();
    // FIXME: Find a way to propagate errors
    VERIFY(!zero_device_or_error.is_error());
    return zero_device_or_error.release_value();
}

UNMAP_AFTER_INIT ZeroDevice::ZeroDevice()
    : CharacterDevice(1, 5)
{
}

UNMAP_AFTER_INIT ZeroDevice::~ZeroDevice()
{
}

bool ZeroDevice::can_read(const OpenFileDescription&, u64) const
{
    return true;
}

ErrorOr<size_t> ZeroDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    TRY(buffer.memset(0, size));
    return size;
}

ErrorOr<size_t> ZeroDevice::write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t size)
{
    return size;
}

}
