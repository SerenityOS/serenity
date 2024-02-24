/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Library/LockWeakable.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel {
class GPUDevice
    : public AtomicRefCounted<GPUDevice>
    , public LockWeakable<GPUDevice> {
public:
    virtual ~GPUDevice() = default;

protected:
    GPUDevice() = default;
};

}
