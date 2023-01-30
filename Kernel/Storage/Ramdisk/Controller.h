/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Storage/Ramdisk/Device.h>
#include <Kernel/Storage/StorageController.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class RamdiskController final : public StorageController {
public:
    static NonnullLockRefPtr<RamdiskController> initialize();
    virtual ~RamdiskController() override;

    virtual LockRefPtr<StorageDevice> device(u32 index) const override;
    virtual bool reset() override;
    virtual bool shutdown() override;
    virtual size_t devices_count() const override;
    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) override;

private:
    RamdiskController();

    NonnullLockRefPtrVector<RamdiskDevice> m_devices;
};
}
