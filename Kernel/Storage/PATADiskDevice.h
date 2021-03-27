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

//
// A Disk Device Connected to a PATA Channel
//

#pragma once

#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Lock.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class IDEController;
class IDEChannel;
class PATADiskDevice final : public StorageDevice {
    friend class IDEController;
    AK_MAKE_ETERNAL
public:
    // Type of drive this IDEDiskDevice is on the ATA channel.
    //
    // Each PATA channel can contain only two devices, which (I think) are
    // jumper selectable on the drive itself by shorting two pins.
    enum class DriveType : u8 {
        Master,
        Slave
    };

    enum class InterfaceType : u8 {
        ATA,
        ATAPI,
    };

public:
    static NonnullRefPtr<PATADiskDevice> create(const IDEController&, IDEChannel&, DriveType, InterfaceType, u16, u64);
    virtual ~PATADiskDevice() override;

    // ^StorageDevice
    virtual Type type() const override { return StorageDevice::Type::IDE; }

    // ^BlockDevice
    virtual void start_request(AsyncBlockDeviceRequest&) override;
    virtual String device_name() const override;

private:
    PATADiskDevice(const IDEController&, IDEChannel&, DriveType, InterfaceType, u16, u64);

    // ^DiskDevice
    virtual const char* class_name() const override;

    bool is_slave() const;

    Lock m_lock { "IDEDiskDevice" };
    u16 m_capabilities { 0 };
    NonnullRefPtr<IDEChannel> m_channel;
    DriveType m_drive_type { DriveType::Master };
    InterfaceType m_interface_type { InterfaceType::ATA };
};

}
