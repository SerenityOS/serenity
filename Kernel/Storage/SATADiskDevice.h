/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Storage/AHCIPort.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class AHCIController;
class SATADiskDevice final : public StorageDevice {
    friend class AHCIController;
    friend class DeviceManagement;

public:
    enum class InterfaceType : u8 {
        SATA,
        SATAPI,
    };

public:
    static NonnullRefPtr<SATADiskDevice> create(const AHCIController&, const AHCIPort&, size_t sector_size, u64 max_addressable_block);
    virtual ~SATADiskDevice() override;

    // ^StorageDevice
    // ^BlockDevice
    virtual void start_request(AsyncBlockDeviceRequest&) override;
    virtual String storage_name() const override;

private:
    SATADiskDevice(const AHCIController&, const AHCIPort&, size_t sector_size, u64 max_addressable_block);

    // ^DiskDevice
    virtual StringView class_name() const override;
    WeakPtr<AHCIPort> m_port;
};

}
