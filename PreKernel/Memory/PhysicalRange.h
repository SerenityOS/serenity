/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/Types.h>
#include <Kernel/PhysicalAddress.h>

namespace Prekernel {

struct PhysicalRange {
    bool is_null() const { return base_address.is_null() && length == 0; }
    PhysicalAddress base_address;
    size_t length;
};

}
