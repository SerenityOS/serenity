/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Storage/ATA/ATAController.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class AsyncBlockDeviceRequest;
class IDEChannel;
class IDEController : public ATAController {
public:
    static NonnullLockRefPtr<IDEController> initialize();
    virtual ~IDEController() override;

    virtual LockRefPtr<StorageDevice> device(u32 index) const override final;
    virtual bool reset() override final;
    virtual bool shutdown() override final;
    virtual size_t devices_count() const override final;
    virtual void start_request(ATADevice const&, AsyncBlockDeviceRequest&) override final;
    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) override final;

protected:
    IDEController();

    LockRefPtr<StorageDevice> device_by_channel_and_position(u32 index) const;
    NonnullLockRefPtrVector<IDEChannel> m_channels;
};
}
