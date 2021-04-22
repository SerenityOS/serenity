/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Forward.h>
#include <LibWeb/CSS/ComputedValues.h>

namespace Web::Painting {

enum class BorderEdge {
    Top,
    Right,
    Bottom,
    Left,
};
void paint_border(PaintContext&, BorderEdge, const Gfx::FloatRect&, const CSS::ComputedValues&);

}
