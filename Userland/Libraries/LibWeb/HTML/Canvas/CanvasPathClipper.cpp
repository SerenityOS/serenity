/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibWeb/HTML/Canvas/CanvasPathClipper.h>

namespace Web::HTML {

// FIXME: This pretty naive, we should be able to cut down the allocations here
// (especially for the paint style which is a bit sad).

ErrorOr<CanvasPathClipper> CanvasPathClipper::create(Gfx::Painter& painter, CanvasClip const& canvas_clip)
{
    auto bounding_box = enclosing_int_rect(canvas_clip.path.bounding_box());
    Gfx::IntRect actual_save_rect {};
    auto maybe_bitmap = painter.get_region_bitmap(bounding_box, Gfx::BitmapFormat::BGRA8888, actual_save_rect);
    RefPtr<Gfx::Bitmap> saved_clip_region;
    if (!maybe_bitmap.is_error()) {
        saved_clip_region = maybe_bitmap.release_value();
    } else if (actual_save_rect.is_empty()) {
        // This is okay, no need to report an error.
    } else {
        return maybe_bitmap.release_error();
    }
    painter.save();
    painter.add_clip_rect(bounding_box);
    return CanvasPathClipper(move(saved_clip_region), bounding_box, canvas_clip);
}

ErrorOr<void> CanvasPathClipper::apply_clip(Gfx::Painter& painter)
{
    painter.restore();
    if (!m_saved_clip_region)
        return {};
    Gfx::IntRect actual_save_rect {};
    auto clip_area = TRY(painter.get_region_bitmap(m_bounding_box, Gfx::BitmapFormat::BGRA8888, actual_save_rect));
    painter.blit(actual_save_rect.location(), *m_saved_clip_region, m_saved_clip_region->rect(), 1.0f, false);
    Gfx::AntiAliasingPainter aa_painter { painter };
    auto fill_offset = m_bounding_box.location() - actual_save_rect.location();
    aa_painter.fill_path(m_canvas_clip.path, TRY(Gfx::BitmapPaintStyle::create(clip_area, fill_offset)), 1.0f, m_canvas_clip.winding_rule);
    return {};
}
}
