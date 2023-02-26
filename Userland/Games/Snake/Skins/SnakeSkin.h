/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Painter.h>

namespace Snake {

class SnakeSkin {
public:
    virtual ~SnakeSkin() = default;

    virtual void draw_head(Gfx::Painter&, Gfx::IntRect const& head, Gfx::IntRect const& body) = 0;
    virtual void draw_body(Gfx::Painter&, Gfx::IntRect const& head, Gfx::IntRect const& body, Gfx::IntRect const& tail) = 0;
    virtual void draw_tail(Gfx::Painter&, Gfx::IntRect const& body, Gfx::IntRect const& tail) = 0;
};

}
