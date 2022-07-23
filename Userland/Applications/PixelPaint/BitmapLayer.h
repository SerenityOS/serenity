/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 *
 */

#pragma once

#include "Layer.h"

namespace PixelPaint {

class BitmapLayer final : public Layer {
public:
    virtual ~BitmapLayer() = default;

    static ErrorOr<NonnullRefPtr<BitmapLayer>> try_create_with_size(Image&, Gfx::IntSize const&, String name);
    static ErrorOr<NonnullRefPtr<BitmapLayer>> try_create_with_bitmap(Image&, NonnullRefPtr<Gfx::Bitmap>, String name);
    static ErrorOr<NonnullRefPtr<BitmapLayer>> try_create_snapshot(Image&, Layer const&);

    virtual Gfx::Bitmap const& content_bitmap() const override { return m_content_bitmap; }
    virtual Gfx::Bitmap& content_bitmap() override { return m_content_bitmap; }

    virtual void flip(Gfx::Orientation orientation) override;
    virtual void rotate(Gfx::RotationDirection direction) override;
    virtual void crop(Gfx::IntRect const& rect) override;
    virtual void resize(Gfx::IntSize const& new_size, Gfx::IntPoint const& new_location, Gfx::Painter::ScalingMode scaling_mode) override;

    void erase_selection(Selection const&) override;

    void did_modify(Gfx::IntRect const& = {}) override;

    ErrorOr<void> try_set_bitmaps(NonnullRefPtr<Gfx::Bitmap> content, RefPtr<Gfx::Bitmap> mask);

private:
    BitmapLayer(Image&, NonnullRefPtr<Gfx::Bitmap>, String name, NonnullRefPtr<Gfx::Bitmap> cached_display_bitmap);

    NonnullRefPtr<Gfx::Bitmap> m_content_bitmap;
};

}
