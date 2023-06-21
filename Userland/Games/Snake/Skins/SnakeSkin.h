/*
 * Copyright (c) 2023, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../Geometry.h"
#include <AK/Error.h>
#include <LibGfx/Painter.h>

namespace Snake {

class SnakeSkin {
public:
    static ErrorOr<NonnullOwnPtr<SnakeSkin>> create(StringView skin_name, Color color);

    virtual ~SnakeSkin() = default;

    virtual void draw_head(Gfx::Painter&, Gfx::IntRect const& rect, Direction facing_direction) = 0;
    virtual void draw_body(Gfx::Painter&, Gfx::IntRect const& rect, Direction previous_direction, Direction next_direction) = 0;
    virtual void draw_tail(Gfx::Painter&, Gfx::IntRect const& rect, Direction body_direction) = 0;
};

}
