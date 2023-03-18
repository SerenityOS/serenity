/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Devices/Storage/ATA/ATAController.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Library/LockRefPtr.h>

namespace Kernel {

class AsyncBlockDeviceRequest;
class IDEChannel;
class IDEController : public ATAController {
public:
    virtual ~IDEController() override;

    virtual LockRefPtr<StorageDevice> device(u32 index) const override final;
    virtual ErrorOr<void> reset() override final;
    virtual ErrorOr<void> shutdown() override final;
    virtual size_t devices_count() const override final;
    virtual void start_request(ATADevice const&, AsyncBlockDeviceRequest&) override final;
    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) override final;

protected:
    IDEController();

    LockRefPtr<StorageDevice> device_by_channel_and_position(u32 index) const;
    Array<RefPtr<IDEChannel>, 2> m_channels;
};
}
