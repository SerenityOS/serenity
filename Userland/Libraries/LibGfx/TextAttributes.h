/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibGfx/Color.h>

namespace Gfx {

struct TextAttributes {
    enum class UnderlineStyle {
        Solid,
        Wavy
    };

    Color color;
    Optional<Color> background_color {};
    bool underline { false };
    bool bold { false };

    Optional<Color> underline_color {};
    UnderlineStyle underline_style { UnderlineStyle::Solid };
};

}
