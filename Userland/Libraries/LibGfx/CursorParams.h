/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGfx/Point.h>

namespace Gfx {

class CursorParams {
public:
    static CursorParams parse_from_filename(StringView, Gfx::IntPoint);

    CursorParams() = default;

    CursorParams(Gfx::IntPoint hotspot)
        : m_hotspot(hotspot)
    {
    }
    CursorParams constrained(Gfx::Bitmap const&) const;

    Gfx::IntPoint hotspot() const { return m_hotspot; }
    unsigned frames() const { return m_frames; }
    unsigned frame_ms() const { return m_frame_ms; }

private:
    Gfx::IntPoint m_hotspot;
    unsigned m_frames { 1 };
    unsigned m_frame_ms { 0 };
    bool m_have_hotspot { false };
};

}
