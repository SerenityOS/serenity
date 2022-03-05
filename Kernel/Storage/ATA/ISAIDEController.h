/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Storage/ATA/IDEChannel.h>
#include <Kernel/Storage/ATA/IDEController.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class ISAIDEController final : public IDEController {
public:
    static NonnullRefPtr<ISAIDEController> initialize();

private:
    ISAIDEController();

    RefPtr<StorageDevice> device_by_channel_and_position(u32 index) const;
    void initialize_channels();
};
}
