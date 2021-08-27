/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Storage/ATAController.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class ATADevice : public StorageDevice {
public:
    // Note: For IDE drives, port means Primary or Secondary (0 or 1),
    // and subport means Master or Slave (0 or 1).
    // For SATA drives (AHCI driven HBAs), a port can be a number from 0 to 31,
    // and subport can be a number from 0 to 14 (only 15 devices are allowed to
    // be connected to one SATA port multiplier).
    struct Address {
        u8 port;
        u8 subport;
    };

public:
    virtual ~ATADevice() override;

    // ^BlockDevice
    virtual void start_request(AsyncBlockDeviceRequest&) override;
    virtual String storage_name() const override;

    u16 ata_capabilites() const { return m_capabilities; }
    const Address& ata_address() const { return m_ata_address; }

protected:
    ATADevice(const ATAController&, Address, u16, u16, u64);

    WeakPtr<ATAController> m_controller;
    const Address m_ata_address;
    const u16 m_capabilities;
};

}
