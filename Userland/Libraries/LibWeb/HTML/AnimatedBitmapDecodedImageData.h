/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/DecodedImageData.h>

namespace Web::HTML {

class AnimatedBitmapDecodedImageData final : public DecodedImageData {
public:
    struct Frame {
        RefPtr<Gfx::Bitmap const> bitmap;
        int duration { 0 };
    };

    static ErrorOr<NonnullRefPtr<AnimatedBitmapDecodedImageData>> create(Vector<Frame>&&, size_t loop_count, bool animated);
    virtual ~AnimatedBitmapDecodedImageData() override;

    virtual RefPtr<Gfx::Bitmap const> bitmap(size_t frame_index, Gfx::IntSize = {}) const override;
    virtual int frame_duration(size_t frame_index) const override;

    virtual size_t frame_count() const override { return m_frames.size(); }
    virtual size_t loop_count() const override { return m_loop_count; }
    virtual bool is_animated() const override { return m_animated; }

    virtual Optional<CSSPixels> intrinsic_width() const override;
    virtual Optional<CSSPixels> intrinsic_height() const override;
    virtual Optional<CSSPixelFraction> intrinsic_aspect_ratio() const override;

private:
    AnimatedBitmapDecodedImageData(Vector<Frame>&&, size_t loop_count, bool animated);

    Vector<Frame> m_frames;
    size_t m_loop_count { 0 };
    bool m_animated { false };
};

}
