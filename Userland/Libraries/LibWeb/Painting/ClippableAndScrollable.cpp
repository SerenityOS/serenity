/*
 * Copyright (c) 2024, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/ClippableAndScrollable.h>

namespace Web::Painting {

Optional<int> ClippableAndScrollable::scroll_frame_id() const
{
    if (m_enclosing_scroll_frame)
        return m_enclosing_scroll_frame->id;
    return {};
}

Optional<CSSPixelPoint> ClippableAndScrollable::enclosing_scroll_frame_offset() const
{
    if (m_enclosing_scroll_frame)
        return m_enclosing_scroll_frame->offset;
    return {};
}

Optional<CSSPixelRect> ClippableAndScrollable::clip_rect() const
{
    if (m_enclosing_clip_frame) {
        auto rect = m_enclosing_clip_frame->rect();
        // NOTE: Since the painting command executor applies a CSS transform and the clip rect is calculated
        //       with this transform taken into account, we need to remove the transform from the clip rect.
        //       Otherwise, the transform will be applied twice to the clip rect.
        //       Similarly, for hit-testing, the transform must be removed from the clip rectangle since the position
        //       includes the transform.
        rect.translate_by(-m_combined_css_transform.translation().to_type<CSSPixels>());
        return rect;
    }
    return {};
}

Span<BorderRadiiClip const> ClippableAndScrollable::border_radii_clips() const
{
    if (m_enclosing_clip_frame)
        return m_enclosing_clip_frame->border_radii_clips();
    return {};
}

}
