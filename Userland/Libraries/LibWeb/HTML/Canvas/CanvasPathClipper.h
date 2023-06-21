/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>

namespace Web::HTML {

struct CanvasClip {
    Gfx::Path path;
    Gfx::Painter::WindingRule winding_rule;
};

class CanvasPathClipper {
public:
    static ErrorOr<CanvasPathClipper> create(Gfx::Painter&, CanvasClip const& canvas_clip);
    ErrorOr<void> apply_clip(Gfx::Painter& painter);

private:
    CanvasPathClipper(RefPtr<Gfx::Bitmap const> saved_clip_region, Gfx::IntRect bounding_box, CanvasClip const& canvas_clip)
        : m_saved_clip_region(saved_clip_region)
        , m_bounding_box(bounding_box)
        , m_canvas_clip(canvas_clip)
    {
    }

    RefPtr<Gfx::Bitmap const> m_saved_clip_region;
    Gfx::IntRect m_bounding_box;
    CanvasClip const& m_canvas_clip;
};

class ScopedCanvasPathClip {
    AK_MAKE_NONMOVABLE(ScopedCanvasPathClip);
    AK_MAKE_NONCOPYABLE(ScopedCanvasPathClip);

public:
    ScopedCanvasPathClip(Gfx::Painter& painter, Optional<CanvasClip> const& canvas_clip)
        : m_painter(painter)
    {
        if (canvas_clip.has_value()) {
            auto clipper = CanvasPathClipper::create(painter, *canvas_clip);
            if (!clipper.is_error())
                m_canvas_clipper = clipper.release_value();
            else
                dbgln("CRC2D Error: Failed to apply canvas clip path: {}", clipper.error());
        }
    }

    ~ScopedCanvasPathClip()
    {
        if (m_canvas_clipper.has_value())
            m_canvas_clipper->apply_clip(m_painter).release_value_but_fixme_should_propagate_errors();
    }

private:
    Gfx::Painter& m_painter;
    Optional<CanvasPathClipper> m_canvas_clipper;
};

}
