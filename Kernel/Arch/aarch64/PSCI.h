/*
 * Copyright (c) 2025, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel::PSCI {

bool is_supported();
void poweroff();
void reset();

}
