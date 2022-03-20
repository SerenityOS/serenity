/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Locking/Mutex.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class RamdiskController;

class RamdiskDevice final : public StorageDevice {
    friend class RamdiskController;
    friend class DeviceManagement;

public:
    static NonnullRefPtr<RamdiskDevice> create(const RamdiskController&, NonnullOwnPtr<Memory::Region>&& region, int major, int minor);
    virtual ~RamdiskDevice() override;

    // ^DiskDevice
    virtual StringView class_name() const override;

private:
    RamdiskDevice(const RamdiskController&, NonnullOwnPtr<Memory::Region>&&, int major, int minor, NonnullOwnPtr<KString> device_name);

    // ^BlockDevice
    virtual void start_request(AsyncBlockDeviceRequest&) override;

    // ^StorageDevice
    virtual CommandSet command_set() const override { return CommandSet::PlainMemory; }

    Mutex m_lock { "RamdiskDevice" };

    NonnullOwnPtr<Memory::Region> m_region;
};

}
