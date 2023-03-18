/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <Kernel/Devices/GPU/Intel/Definitions.h>

namespace Kernel::IntelGraphics {

PLLMaxSettings const& pll_max_settings_for_generation(Generation);
Optional<PLLSettings> create_pll_settings(Generation, u64 target_frequency, u64 reference_clock);
bool check_pll_settings(PLLSettings const& settings, size_t reference_clock, PLLMaxSettings const& limits);

}
