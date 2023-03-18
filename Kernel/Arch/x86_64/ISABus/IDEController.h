/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Devices/Storage/ATA/GenericIDE/Controller.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Library/LockRefPtr.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class ISAIDEController final : public IDEController {
public:
    static ErrorOr<NonnullRefPtr<ISAIDEController>> initialize();

private:
    ISAIDEController();

    ErrorOr<void> initialize_channels();
};
}
