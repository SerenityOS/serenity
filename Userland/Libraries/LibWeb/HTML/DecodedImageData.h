/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibGfx/Forward.h>
#include <LibJS/Heap/Cell.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/images.html#img-req-data
class DecodedImageData final : public RefCounted<DecodedImageData> {
public:
    struct Frame {
        RefPtr<Gfx::Bitmap const> bitmap;
        int duration { 0 };
    };

    static ErrorOr<NonnullRefPtr<DecodedImageData>> create(Vector<Frame>&&, size_t loop_count, bool animated);
    ~DecodedImageData();

    RefPtr<Gfx::Bitmap const> bitmap(size_t frame_index) const;
    int frame_duration(size_t frame_index) const;

    size_t frame_count() const { return m_frames.size(); }
    size_t loop_count() const { return m_loop_count; }
    bool is_animated() const { return m_animated; }

    Optional<int> natural_width() const;
    Optional<int> natural_height() const;

private:
    DecodedImageData(Vector<Frame>&&, size_t loop_count, bool animated);

    Vector<Frame> m_frames;
    size_t m_loop_count { 0 };
    bool m_animated { false };
};

}
