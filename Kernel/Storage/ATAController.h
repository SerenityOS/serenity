/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Storage/StorageController.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class ATADevice;
class ATAController
    : public StorageController
    , public Weakable<ATAController> {
public:
    virtual void start_request(const ATADevice&, AsyncBlockDeviceRequest&) = 0;

protected:
    ATAController() = default;
};
}
