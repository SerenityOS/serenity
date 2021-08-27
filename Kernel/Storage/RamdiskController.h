/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Storage/RamdiskDevice.h>
#include <Kernel/Storage/StorageController.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class RamdiskController final : public StorageController {
    AK_MAKE_ETERNAL
public:
public:
    static NonnullRefPtr<RamdiskController> initialize();
    virtual ~RamdiskController() override;

    virtual RefPtr<StorageDevice> search_for_device(StorageAddress) const override;
    virtual RefPtr<StorageDevice> device_by_index(u32) const override;
    virtual bool reset() override;
    virtual bool shutdown() override;
    virtual size_t devices_count() const override;
    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) override;
    virtual Optional<size_t> max_devices_count() const override { return {}; }

private:
    RamdiskController();

    NonnullRefPtrVector<RamdiskDevice> m_devices;
};
}
