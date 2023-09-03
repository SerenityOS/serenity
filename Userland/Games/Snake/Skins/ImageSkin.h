/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "SnakeSkin.h"
#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibGfx/Point.h>

namespace Snake {

class ImageSkin : public SnakeSkin {
public:
    static ErrorOr<NonnullOwnPtr<ImageSkin>> create(StringView skin_name);

    virtual ~ImageSkin() override = default;

    void draw_head(Gfx::Painter&, Gfx::IntRect const& head, Direction facing_direction) override;
    void draw_body(Gfx::Painter&, Gfx::IntRect const& rect, Direction previous_direction, Direction next_direction) override;
    void draw_tail(Gfx::Painter&, Gfx::IntRect const& tail, Direction body_direction) override;

private:
    ImageSkin(StringView skin_name, Vector<NonnullRefPtr<Gfx::Bitmap>> head_bitmaps, Vector<NonnullRefPtr<Gfx::Bitmap>> body_bitmaps);

    String m_skin_name;

    Vector<NonnullRefPtr<Gfx::Bitmap>> m_head_bitmaps;
    Vector<NonnullRefPtr<Gfx::Bitmap>> m_body_bitmaps;
};

}
