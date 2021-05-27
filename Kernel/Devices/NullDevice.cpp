/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NullDevice.h"
#include <AK/Singleton.h>
#include <AK/StdLibExtras.h>

namespace Kernel {

static AK::Singleton<NullDevice> s_the;

UNMAP_AFTER_INIT void NullDevice::initialize()
{
    s_the.ensure_instance();
}

NullDevice& NullDevice::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT NullDevice::NullDevice()
    : CharacterDevice(1, 3)
{
}

UNMAP_AFTER_INIT NullDevice::~NullDevice()
{
}

bool NullDevice::can_read(const FileDescription&, size_t) const
{
    return true;
}

KResultOr<size_t> NullDevice::read(FileDescription&, u64, UserOrKernelBuffer&, size_t)
{
    return 0;
}

KResultOr<size_t> NullDevice::write(FileDescription&, u64, const UserOrKernelBuffer&, size_t buffer_size)
{
    return buffer_size;
}

}
