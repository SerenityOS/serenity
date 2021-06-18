/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ZeroDevice.h"
#include <AK/Memory.h>
#include <AK/StdLibExtras.h>
#include <Kernel/Panic.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<ZeroDevice> ZeroDevice::must_create()
{
    return adopt_ref_if_nonnull(new ZeroDevice).release_nonnull();
}

UNMAP_AFTER_INIT ZeroDevice::ZeroDevice()
    : CharacterDevice(1, 5)
{
}

UNMAP_AFTER_INIT ZeroDevice::~ZeroDevice()
{
}

bool ZeroDevice::can_read(const FileDescription&, size_t) const
{
    return true;
}

KResultOr<size_t> ZeroDevice::read(FileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (!buffer.memset(0, size))
        return EFAULT;
    return size;
}

KResultOr<size_t> ZeroDevice::write(FileDescription&, u64, const UserOrKernelBuffer&, size_t size)
{
    return size;
}

}
