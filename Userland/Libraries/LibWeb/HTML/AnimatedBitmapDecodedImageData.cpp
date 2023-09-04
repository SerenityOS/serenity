/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibWeb/HTML/AnimatedBitmapDecodedImageData.h>

namespace Web::HTML {

ErrorOr<NonnullRefPtr<AnimatedBitmapDecodedImageData>> AnimatedBitmapDecodedImageData::create(Vector<Frame>&& frames, size_t loop_count, bool animated)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) AnimatedBitmapDecodedImageData(move(frames), loop_count, animated));
}

AnimatedBitmapDecodedImageData::AnimatedBitmapDecodedImageData(Vector<Frame>&& frames, size_t loop_count, bool animated)
    : m_frames(move(frames))
    , m_loop_count(loop_count)
    , m_animated(animated)
{
}

AnimatedBitmapDecodedImageData::~AnimatedBitmapDecodedImageData() = default;

RefPtr<Gfx::Bitmap const> AnimatedBitmapDecodedImageData::bitmap(size_t frame_index, Gfx::IntSize) const
{
    if (frame_index >= m_frames.size())
        return nullptr;
    return m_frames[frame_index].bitmap;
}

int AnimatedBitmapDecodedImageData::frame_duration(size_t frame_index) const
{
    if (frame_index >= m_frames.size())
        return 0;
    return m_frames[frame_index].duration;
}

Optional<CSSPixels> AnimatedBitmapDecodedImageData::intrinsic_width() const
{
    return m_frames.first().bitmap->width();
}

Optional<CSSPixels> AnimatedBitmapDecodedImageData::intrinsic_height() const
{
    return m_frames.first().bitmap->height();
}

Optional<CSSPixelFraction> AnimatedBitmapDecodedImageData::intrinsic_aspect_ratio() const
{
    return CSSPixels(m_frames.first().bitmap->width()) / CSSPixels(m_frames.first().bitmap->height());
}

}
