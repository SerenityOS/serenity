/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 *
 */

#pragma once

#include "Layer.h"

namespace PixelPaint {

class ColorLayer final : public Layer {
public:
    virtual ~ColorLayer() = default;

    static ErrorOr<NonnullRefPtr<ColorLayer>> try_create_with_size(Image&, Gfx::IntSize const&, String name);
    static ErrorOr<NonnullRefPtr<ColorLayer>> try_create_with_size_and_color(Image&, Gfx::IntSize const&, Gfx::Color, String name);

    virtual Gfx::Bitmap const& content_bitmap() const override { return m_bitmap; }
    virtual Gfx::Bitmap& content_bitmap() override { return m_bitmap; }

    virtual bool is_current_bitmap_editable() override;

    void set_color(Gfx::Color color);
    Gfx::Color color() const { return m_color; }

    // FIXME: Implement those
    virtual void flip(Gfx::Orientation) override {};
    virtual void rotate(Gfx::RotationDirection) override {};
    virtual void crop(Gfx::IntRect const&) override {};
    virtual void resize(Gfx::IntSize const&, Gfx::IntPoint const&, Gfx::Painter::ScalingMode) override {};

private:
    ColorLayer(Image&, NonnullRefPtr<Gfx::Bitmap>, Gfx::Color, String name, NonnullRefPtr<Gfx::Bitmap> cached_content_bitmap);

    NonnullRefPtr<Gfx::Bitmap> m_bitmap;
    Gfx::Color m_color { Color::White };
};

}
