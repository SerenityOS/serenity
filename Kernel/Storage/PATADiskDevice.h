/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

//
// A Disk Device Connected to a PATA Channel
//

#pragma once

#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class IDEController;
class IDEChannel;
class PATADiskDevice final : public StorageDevice {
    friend class IDEController;
    friend class DeviceManagement;
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

    // ^BlockDevice
    virtual void start_request(AsyncBlockDeviceRequest&) override;
    virtual String storage_name() const override;

private:
    PATADiskDevice(const IDEController&, IDEChannel&, DriveType, InterfaceType, u16, u64);

    // ^DiskDevice
    virtual StringView class_name() const override;

    bool is_slave() const;

    u16 m_capabilities { 0 };
    NonnullRefPtr<IDEChannel> m_channel;
    DriveType m_drive_type { DriveType::Master };
    InterfaceType m_interface_type { InterfaceType::ATA };
};

}
