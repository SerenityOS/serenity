/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>

namespace GUI {

// FocusPolicy determines how GUI widgets gain focus.
//
// - NoFocus:     The widget is not focusable.
// - TabFocus:    The widget can gain focus by cycling through focusable widgets with the Tab key.
// - ClickFocus:  The widget gains focus when clicked.
// - StrongFocus: The widget can gain focus both via Tab, and by clicking on it.
enum class FocusPolicy {
    NoFocus = 0,
    TabFocus = 0x1,
    ClickFocus = 0x2,
    StrongFocus = TabFocus | ClickFocus,
};

AK_ENUM_BITWISE_OPERATORS(FocusPolicy)

}
