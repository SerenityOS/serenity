/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Optional.h>
#include <LibGfx/Color.h>

namespace Gfx {

struct TextAttributes {
    Color color;
    Optional<Color> background_color;
    bool underline { false };
    bool bold { false };
};

}
