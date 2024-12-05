/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace Kernel {

void raspberry_pi_platform_init(StringView compatible_string);
void virt_platform_init(StringView compatible_string);

}
