/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Storage/AHCI/Controller.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel {

class ATADevice : public StorageDevice {
public:
    virtual ~ATADevice() override;

    // ^BlockDevice
    virtual void start_request(AsyncBlockDeviceRequest&) override;

    u16 ata_capabilites() const { return m_capabilities; }
    ATA::Address const& ata_address() const { return m_ata_address; }

protected:
    ATADevice(AHCIController const&, ATA::Address, u16, u16, u64);

    // FIXME: Add proper locking to ensure hotplug can work.
    LockRefPtr<AHCIController> m_controller;
    ATA::Address const m_ata_address;
    u16 const m_capabilities;
};

}
