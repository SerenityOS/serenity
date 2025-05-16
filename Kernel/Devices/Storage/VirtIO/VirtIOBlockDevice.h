/*
 * Copyright (c) 2023, Kirill Nikolaev <cyril7@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Result.h>
#include <AK/Types.h>
#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel {

class VirtIOBlockDevice : public StorageDevice
    , VirtIO::Device {
public:
    // ^StorageDevice
    virtual CommandSet command_set() const override { return CommandSet::SCSI; }

    // ^BlockDevice
    virtual void start_request(AsyncBlockDeviceRequest&) override;

protected:
    // ^VirtIO::Device
    virtual ErrorOr<void> initialize_virtio_resources() override;
    virtual void handle_queue_update(u16 queue_index) override;
    ErrorOr<void> handle_device_config_change() override;

private:
    friend class VirtIOBlockController;
    VirtIOBlockDevice(NonnullOwnPtr<VirtIO::TransportEntity> transport,
        StorageDevice::LUNAddress lun,
        u32 hardware_relative_controller_id);

    ErrorOr<void> maybe_start_request(AsyncBlockDeviceRequest&);
    void respond();

private:
    OwnPtr<Memory::Region> m_header_buf;
    OwnPtr<Memory::Region> m_data_buf;

    SpinlockProtected<RefPtr<AsyncBlockDeviceRequest>, LockRank::None> m_current_request {};
};

}
