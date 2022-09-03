/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Storage/ATA/GenericIDE/Controller.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class ISAIDEController final : public IDEController {
public:
    static NonnullLockRefPtr<ISAIDEController> initialize();

private:
    ISAIDEController();

    LockRefPtr<StorageDevice> device_by_channel_and_position(u32 index) const;
    void initialize_channels();
};
}
