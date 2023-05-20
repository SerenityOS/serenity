/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/DecodedImageData.h>

namespace Web::SVG {

class SVGDecodedImageData final : public HTML::DecodedImageData {
public:
    static ErrorOr<NonnullRefPtr<SVGDecodedImageData>> create(ByteBuffer encoded_svg);
    virtual ~SVGDecodedImageData() override;

    virtual RefPtr<Gfx::Bitmap const> bitmap(size_t frame_index, Gfx::IntSize) const override;

    virtual Optional<CSSPixels> intrinsic_width() const override;
    virtual Optional<CSSPixels> intrinsic_height() const override;
    virtual Optional<float> intrinsic_aspect_ratio() const override;

    // FIXME: Support SVG animations. :^)
    virtual int frame_duration(size_t) const override { return 0; }
    virtual size_t frame_count() const override { return 1; }
    virtual size_t loop_count() const override { return 0; }
    virtual bool is_animated() const override { return false; }

private:
    SVGDecodedImageData();
};

}
