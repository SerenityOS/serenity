/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <AK/Weakable.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {
class GenericGraphicsAdapter
    : public RefCounted<GenericGraphicsAdapter>
    , public Weakable<GenericGraphicsAdapter> {
public:
    virtual ~GenericGraphicsAdapter() = default;

    virtual bool vga_compatible() const = 0;

protected:
    GenericGraphicsAdapter() = default;
};

}
