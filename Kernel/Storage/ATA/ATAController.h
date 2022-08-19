/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Storage/StorageController.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class ATADevice;
class ATAController
    : public StorageController
    , public LockWeakable<ATAController> {
public:
    virtual void start_request(ATADevice const&, AsyncBlockDeviceRequest&) = 0;

protected:
    ATAController() = default;
};
}
