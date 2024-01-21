/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/HTML/AnimatedBitmapDecodedImageData.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(AnimatedBitmapDecodedImageData);

ErrorOr<JS::NonnullGCPtr<AnimatedBitmapDecodedImageData>> AnimatedBitmapDecodedImageData::create(JS::Realm& realm, Vector<Frame>&& frames, Gfx::IntSize size, size_t loop_count, bool animated)
{
    return realm.heap().allocate<AnimatedBitmapDecodedImageData>(realm, move(frames), size, loop_count, animated);
}

AnimatedBitmapDecodedImageData::AnimatedBitmapDecodedImageData(Vector<Frame>&& frames, Gfx::IntSize size, size_t loop_count, bool animated)
    : m_intrinsic_size(size)
    , m_frames(move(frames))
    , m_loop_count(loop_count)
    , m_animated(animated)
{
}

AnimatedBitmapDecodedImageData::~AnimatedBitmapDecodedImageData() = default;

RefPtr<Gfx::ImmutableBitmap> AnimatedBitmapDecodedImageData::bitmap(size_t frame_index, Gfx::IntSize) const
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
    return m_intrinsic_size.width();
}

Optional<CSSPixels> AnimatedBitmapDecodedImageData::intrinsic_height() const
{
    return m_intrinsic_size.height();
}

Optional<CSSPixelFraction> AnimatedBitmapDecodedImageData::intrinsic_aspect_ratio() const
{
    return CSSPixels(m_intrinsic_size.width()) / CSSPixels(m_intrinsic_size.height());
}

}
