/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace Kernel {

void raspberry_pi_3_4_platform_init(StringView compatible_string);
void raspberry_pi_5_platform_init(StringView compatible_string);
void virt_platform_init(StringView compatible_string);

}
