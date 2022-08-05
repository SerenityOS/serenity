/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Storage/ATA/ATADevice.h>

namespace Kernel {

class IDEController;
class ATADiskDevice final : public ATADevice {
    friend class IDEController;
    friend class DeviceManagement;

public:
    static NonnullLockRefPtr<ATADiskDevice> create(ATAController const&, ATADevice::Address, u16 capabilities, u16 logical_sector_size, u64 max_addressable_block);
    virtual ~ATADiskDevice() override;

    // ^StorageDevice
    virtual CommandSet command_set() const override { return CommandSet::ATA; }

private:
    ATADiskDevice(ATAController const&, Address, u16, u16, u64);

    // ^DiskDevice
    virtual StringView class_name() const override;
};

}
