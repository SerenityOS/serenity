/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibWeb/SVG/SVGDecodedImageData.h>

namespace Web::SVG {

ErrorOr<NonnullRefPtr<SVGDecodedImageData>> SVGDecodedImageData::create(ByteBuffer)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) SVGDecodedImageData());
}

SVGDecodedImageData::SVGDecodedImageData()
{
}

SVGDecodedImageData::~SVGDecodedImageData() = default;

RefPtr<Gfx::Bitmap const> SVGDecodedImageData::bitmap(size_t, Gfx::IntSize) const
{
    return nullptr;
}

Optional<CSSPixels> SVGDecodedImageData::intrinsic_width() const
{
    return 0;
}

Optional<CSSPixels> SVGDecodedImageData::intrinsic_height() const
{
    return 0;
}

Optional<float> SVGDecodedImageData::intrinsic_aspect_ratio() const
{
    return 1;
}

}
