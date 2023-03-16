/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "SnakeSkin.h"
#include <LibGfx/Color.h>

namespace Snake {

class ClassicSkin : public SnakeSkin {
public:
    ClassicSkin(Color);

    virtual ~ClassicSkin() override = default;

    void draw_head(Gfx::Painter&, Gfx::IntRect const& head, Direction body_direction) override;
    void draw_body(Gfx::Painter&, Gfx::IntRect const& rect, Direction previous_direction, Direction next_direction) override;
    void draw_tail(Gfx::Painter& painter, Gfx::IntRect const& tail, Direction body_direction) override;

private:
    void draw_tile_at(Gfx::Painter&, Gfx::IntRect const&);

    Gfx::Color m_skin_color = { Color::Yellow };
};

}
