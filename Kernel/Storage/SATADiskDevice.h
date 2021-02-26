/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
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

#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Lock.h>
#include <Kernel/Storage/AHCIPort.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class AHCIController;
class SATADiskDevice final : public StorageDevice {
    friend class AHCIController;

public:
    enum class InterfaceType : u8 {
        SATA,
        SATAPI,
    };

public:
    static NonnullRefPtr<SATADiskDevice> create(const AHCIController&, const AHCIPort&, size_t sector_size, size_t max_addressable_block);
    virtual ~SATADiskDevice() override;

    // ^StorageDevice
    virtual Type type() const override { return StorageDevice::Type::SATA; }
    // ^BlockDevice
    virtual void start_request(AsyncBlockDeviceRequest&) override;
    virtual String device_name() const override;

private:
    SATADiskDevice(const AHCIController&, const AHCIPort&, size_t sector_size, size_t max_addressable_block);

    // ^DiskDevice
    virtual const char* class_name() const override;
    NonnullRefPtr<AHCIPort> m_port;
};

}
