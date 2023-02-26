/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "SnakeSkin.h"
#include <AK/NonnullRefPtrVector.h>
#include <LibGfx/Color.h>
#include <LibGfx/Point.h>

namespace Snake {

int configure_direction(int value, Gfx::Point<int> direction);

class ImageSkin : public SnakeSkin {
public:
    ImageSkin();

    void set_skin_name(StringView const& skin);
    ErrorOr<void> load_skins();

    virtual ~ImageSkin() override = default;

    void draw_head(Gfx::Painter&, Gfx::IntRect const& head, Gfx::IntRect const& body) override;
    void draw_body(Gfx::Painter&, Gfx::IntRect const& head, Gfx::IntRect const& body, Gfx::IntRect const& tail) override;
    void draw_tail(Gfx::Painter&, Gfx::IntRect const& body, Gfx::IntRect const& tail) override;

private:
    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> load_skin_bitmap(StringView const& file);

    DeprecatedString m_skin_name = "snake"sv;

    NonnullRefPtrVector<Gfx::Bitmap> m_body_bitmaps;
    NonnullRefPtrVector<Gfx::Bitmap> m_head_bitmaps;
};

}
