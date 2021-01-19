/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/IO.h>
#include <Kernel/Lock.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/DeviceController.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Random.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;
class StorageDevice;
class StorageController : public RefCounted<StorageController> {
    AK_MAKE_ETERNAL
public:
    enum class Type : u8 {
        Ramdisk,
        IDE,
        NVMe
    };

    virtual ~StorageController() = default;

    virtual Type type() const = 0;
    virtual RefPtr<StorageDevice> device(u32 index) const = 0;
    virtual size_t devices_count() const = 0;

protected:
    virtual void start_request(const StorageDevice&, AsyncBlockDeviceRequest&) = 0;

protected:
    virtual bool reset() = 0;
    virtual bool shutdown() = 0;

    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) = 0;
};
}
