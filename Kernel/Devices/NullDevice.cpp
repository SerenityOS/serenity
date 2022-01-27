/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/NullDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<NullDevice> NullDevice::must_initialize()
{
    auto null_device_or_error = DeviceManagement::try_create_device<NullDevice>();
    // FIXME: Find a way to propagate errors
    VERIFY(!null_device_or_error.is_error());
    return null_device_or_error.release_value();
}

UNMAP_AFTER_INIT NullDevice::NullDevice()
    : CharacterDevice(1, 3)
{
}

UNMAP_AFTER_INIT NullDevice::~NullDevice()
{
}

bool NullDevice::can_read(const OpenFileDescription&, u64) const
{
    return true;
}

ErrorOr<size_t> NullDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t)
{
    return 0;
}

ErrorOr<size_t> NullDevice::write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t buffer_size)
{
    return buffer_size;
}

}
