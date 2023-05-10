/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibWeb/HTML/DecodedImageData.h>

namespace Web::HTML {

ErrorOr<NonnullRefPtr<DecodedImageData>> DecodedImageData::create(Vector<Frame>&& frames, size_t loop_count, bool animated)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) DecodedImageData(move(frames), loop_count, animated));
}

DecodedImageData::DecodedImageData(Vector<Frame>&& frames, size_t loop_count, bool animated)
    : m_frames(move(frames))
    , m_loop_count(loop_count)
    , m_animated(animated)
{
}

DecodedImageData::~DecodedImageData() = default;

RefPtr<Gfx::Bitmap const> DecodedImageData::bitmap(size_t frame_index) const
{
    if (frame_index >= m_frames.size())
        return nullptr;
    return m_frames[frame_index].bitmap;
}

int DecodedImageData::frame_duration(size_t frame_index) const
{
    if (frame_index >= m_frames.size())
        return 0;
    return m_frames[frame_index].duration;
}

Optional<int> DecodedImageData::natural_width() const
{
    return m_frames.first().bitmap->width();
}

Optional<int> DecodedImageData::natural_height() const
{
    return m_frames.first().bitmap->height();
}

}
