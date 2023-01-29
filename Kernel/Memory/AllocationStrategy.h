/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel {

enum class AllocationStrategy {
    Reserve = 0,
    AllocateNow,
    None
};

}
