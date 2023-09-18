/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

VALIDATE_IS_AARCH64()

namespace Kernel {

struct [[gnu::aligned(16)]] FPUState {
    u8 buffer[512];
};

}
