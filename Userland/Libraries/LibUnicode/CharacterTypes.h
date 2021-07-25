/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Unicode {

u32 to_unicode_lowercase(u32 code_point);
u32 to_unicode_uppercase(u32 code_point);

}
