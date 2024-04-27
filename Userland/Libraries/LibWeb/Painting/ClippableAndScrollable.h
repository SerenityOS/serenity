/*
 * Copyright (c) 2024, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Painting/ClipFrame.h>

namespace Web::Painting {

struct ScrollFrame : public RefCounted<ScrollFrame> {
    i32 id { -1 };
    CSSPixelPoint offset;
};

class ClippableAndScrollable {
public:
    virtual ~ClippableAndScrollable() = default;

    void set_enclosing_scroll_frame(RefPtr<ScrollFrame> scroll_frame) { m_enclosing_scroll_frame = scroll_frame; }
    void set_enclosing_clip_frame(RefPtr<ClipFrame> clip_frame) { m_enclosing_clip_frame = clip_frame; }

    [[nodiscard]] Optional<int> scroll_frame_id() const;
    [[nodiscard]] Optional<CSSPixelPoint> enclosing_scroll_frame_offset() const;
    [[nodiscard]] Optional<CSSPixelRect> clip_rect() const;
    [[nodiscard]] Span<BorderRadiiClip const> border_radii_clips() const;

    virtual Gfx::AffineTransform compute_combined_css_transform_for_clippable_and_scrollable() const = 0;

private:
    RefPtr<ScrollFrame const> m_enclosing_scroll_frame;
    RefPtr<ClipFrame const> m_enclosing_clip_frame;
};

}
