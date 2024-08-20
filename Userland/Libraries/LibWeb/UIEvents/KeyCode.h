/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

// FIXME: Stop including Kernel here eventually.
#include <Kernel/API/KeyCode.h>

namespace Web::UIEvents {

using KeyCode = ::KeyCode;
using enum KeyCode;

using KeyModifier = ::KeyModifier;
using enum KeyModifier;

constexpr KeyCode key_code_from_string(StringView key_name)
{
#define __ENUMERATE_KEY_CODE(name, ui_name) \
    if (key_name == ui_name##sv)            \
        return KeyCode::Key_##name;
    ENUMERATE_KEY_CODES
#undef __ENUMERATE_KEY_CODE

    VERIFY_NOT_REACHED();
}

inline KeyCode code_point_to_key_code(u32 code_point)
{
    return ::code_point_to_key_code(code_point);
}

}
