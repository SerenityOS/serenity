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
#include <Kernel/PhysicalAddress.h>

namespace Kernel {
class GenericGPUAdapter
    : public AtomicRefCounted<GenericGPUAdapter>
    , public LockWeakable<GenericGPUAdapter> {
public:
    virtual ~GenericGPUAdapter() = default;

protected:
    GenericGPUAdapter() = default;
};

}
