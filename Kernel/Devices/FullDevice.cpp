/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/FullDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<FullDevice> FullDevice::must_create()
{
    auto full_device_or_error = DeviceManagement::try_create_device<FullDevice>();
    // FIXME: Find a way to propagate errors
    VERIFY(!full_device_or_error.is_error());
    return full_device_or_error.release_value();
}

UNMAP_AFTER_INIT FullDevice::FullDevice()
    : CharacterDevice(1, 7)
{
}

UNMAP_AFTER_INIT FullDevice::~FullDevice()
{
}

bool FullDevice::can_read(const OpenFileDescription&, u64) const
{
    return true;
}

ErrorOr<size_t> FullDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    TRY(buffer.memset(0, size));
    return size;
}

ErrorOr<size_t> FullDevice::write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t size)
{
    if (size == 0)
        return 0;
    return ENOSPC;
}
}
