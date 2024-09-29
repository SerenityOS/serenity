/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// FIXME: Stop including Kernel here eventually.
#include <Kernel/API/KeyCode.h>

namespace Web::UIEvents {

using KeyCode = ::KeyCode;
using enum KeyCode;

using KeyModifier = ::KeyModifier;
using enum KeyModifier;

inline KeyCode code_point_to_key_code(u32 code_point)
{
    return ::code_point_to_key_code(code_point);
}

}
