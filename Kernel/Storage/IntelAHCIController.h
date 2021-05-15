/*
 * Copyright (c) 2021, Alexander Richards <electrodeyt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Storage/AHCIController.h>

namespace Kernel {

class IntelAHCIController : public AHCIController {
public:
    UNMAP_AFTER_INIT static NonnullRefPtr<AHCIController> initialize(PCI::Address address);

protected:
    UNMAP_AFTER_INIT explicit IntelAHCIController(PCI::Address address);
    UNMAP_AFTER_INIT virtual void initialize() override;
};

}
