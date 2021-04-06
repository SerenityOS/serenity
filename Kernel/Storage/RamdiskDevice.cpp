/*
 * Copyright (c) 2021, the SerenityOS developers
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

#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Storage/RamdiskController.h>
#include <Kernel/Storage/RamdiskDevice.h>

namespace Kernel {

NonnullRefPtr<RamdiskDevice> RamdiskDevice::create(const RamdiskController& controller, NonnullOwnPtr<Region>&& region, int major, int minor)
{
    return adopt(*new RamdiskDevice(controller, move(region), major, minor));
}

RamdiskDevice::RamdiskDevice(const RamdiskController& controller, NonnullOwnPtr<Region>&& region, int major, int minor)
    : StorageDevice(controller, major, minor, 512, region->size() / 512)
    , m_region(move(region))
{
    dmesgln("Ramdisk: Device #{} @ {}, Capacity={}", minor, m_region->vaddr(), max_addressable_block() * 512);
}

RamdiskDevice::~RamdiskDevice()
{
}

const char* RamdiskDevice::class_name() const
{
    return "RamdiskDevice";
}

void RamdiskDevice::start_request(AsyncBlockDeviceRequest& request)
{
    LOCKER(m_lock);

    u8* base = m_region->vaddr().as_ptr();
    size_t size = m_region->size();
    u8* offset = base + request.block_index() * 512;
    size_t length = request.block_count() * 512;

    if ((offset + length > base + size) || (offset + length < base)) {
        request.complete(AsyncDeviceRequest::Failure);
    } else {
        bool success;

        if (request.request_type() == AsyncBlockDeviceRequest::Read) {
            success = request.buffer().write(offset, length);
        } else {
            success = request.buffer().read(offset, length);
        }

        request.complete(success ? AsyncDeviceRequest::Success : AsyncDeviceRequest::MemoryFault);
    }
}

String RamdiskDevice::device_name() const
{
    // FIXME: Try to not hardcode a maximum of 16 partitions per drive!
    size_t drive_index = minor() / 16;
    return String::formatted("ramdisk{}", drive_index);
}

}
