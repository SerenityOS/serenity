/*
 * Copyright (c) 2024, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Painting/BorderRadiiData.h>
#include <LibWeb/PixelUnits.h>

namespace Web::Painting {

struct BorderRadiiClip {
    CSSPixelRect rect;
    BorderRadiiData radii;
};

struct ClipFrame : public RefCounted<ClipFrame> {
    Vector<BorderRadiiClip> const& border_radii_clips() const { return m_border_radii_clips; }
    void add_border_radii_clip(BorderRadiiClip border_radii_clip)
    {
        for (auto& existing_clip : m_border_radii_clips) {
            if (border_radii_clip.rect == existing_clip.rect) {
                existing_clip.radii.union_max_radii(border_radii_clip.radii);
                return;
            }
        }
        m_border_radii_clips.append(border_radii_clip);
    }
    void clear_border_radii_clips() { m_border_radii_clips.clear(); }

    CSSPixelRect rect() const { return m_rect; }
    void set_rect(CSSPixelRect rect) { m_rect = rect; }

private:
    CSSPixelRect m_rect;
    Vector<BorderRadiiClip> m_border_radii_clips;
};

}
