/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Random.h>

namespace Kernel {

UNMAP_AFTER_INIT RandomDevice::RandomDevice()
    : CharacterDevice(1, 8)
{
}

UNMAP_AFTER_INIT RandomDevice::~RandomDevice()
{
}

bool RandomDevice::can_read(const FileDescription&, size_t) const
{
    return true;
}

KResultOr<size_t> RandomDevice::read(FileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    return buffer.write_buffered<256>(size, [&](u8* data, size_t data_size) {
        get_good_random_bytes(data, data_size);
        return data_size;
    });
}

KResultOr<size_t> RandomDevice::write(FileDescription&, u64, const UserOrKernelBuffer&, size_t size)
{
    // FIXME: Use input for entropy? I guess that could be a neat feature?
    return size;
}

}
