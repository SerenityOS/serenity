/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Random.h>

namespace Kernel {

RandomDevice::RandomDevice()
    : CharacterDevice(1, 8)
{
}

RandomDevice::~RandomDevice()
{
}

bool RandomDevice::can_read(const FileDescription&, size_t) const
{
    return true;
}

KResultOr<size_t> RandomDevice::read(FileDescription&, size_t, UserOrKernelBuffer& buffer, size_t size)
{
    bool success = buffer.write_buffered<256>(size, [&](u8* data, size_t data_size) {
        get_good_random_bytes(data, data_size);
        return (ssize_t)data_size;
    });
    if (!success)
        return KResult(-EFAULT);
    return size;
}

KResultOr<size_t> RandomDevice::write(FileDescription&, size_t, const UserOrKernelBuffer&, size_t size)
{
    // FIXME: Use input for entropy? I guess that could be a neat feature?
    return min(static_cast<size_t>(PAGE_SIZE), size);
}

}
