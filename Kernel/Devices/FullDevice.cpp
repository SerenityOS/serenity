/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <Kernel/Devices/FullDevice.h>
#include <Kernel/Sections.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<FullDevice> FullDevice::must_create()
{
    auto device = adopt_ref(*new (nothrow) FullDevice);
    device->after_inserting();
    return device;
}

UNMAP_AFTER_INIT FullDevice::FullDevice()
    : CharacterDevice(1, 7)
{
}

UNMAP_AFTER_INIT FullDevice::~FullDevice()
{
}

bool FullDevice::can_read(const OpenFileDescription&, size_t) const
{
    return true;
}

KResultOr<size_t> FullDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    TRY(buffer.memset(0, size));
    return size;
}

KResultOr<size_t> FullDevice::write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t size)
{
    if (size == 0)
        return 0;
    return ENOSPC;
}
}
