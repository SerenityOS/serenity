/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Generic/FullDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<FullDevice> FullDevice::must_create()
{
    return MUST(Device::try_create_device<FullDevice>());
}

UNMAP_AFTER_INIT FullDevice::FullDevice()
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::Generic, 7)
{
}

UNMAP_AFTER_INIT FullDevice::~FullDevice() = default;

bool FullDevice::can_read(OpenFileDescription const&, u64) const
{
    return true;
}

ErrorOr<size_t> FullDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    TRY(buffer.memset(0, size));
    return size;
}

ErrorOr<size_t> FullDevice::write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t size)
{
    if (size == 0)
        return 0;
    return ENOSPC;
}
}
