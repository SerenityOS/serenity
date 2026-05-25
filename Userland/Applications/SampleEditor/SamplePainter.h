/*
 * Copyright (c) 2026, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RenderStruct.h"
#include "SampleDocument.h"
#include "SampleEditorPalette.h"
#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibCore/Timer.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Event.h>
#include <LibGfx/Bitmap.h>

class SamplePainter {
public:
    struct SelectionState {
        bool selected { false };
        double start { 0.0 };
        double end { 1.0 };
        int start_absolute { 0 };
        int end_absolute { 1 };
    };

    struct CursorState {
        bool placed { false };
        double position { 0.0 };
    };

    struct PaintResult {
        double viewport_start { 0.0 };
        bool samples_were_unused { false };
    };

    void reset();
    void invalidate_scene_and_repaint(GUI::Widget&);
    void invalidate_drag_overlay_and_repaint(GUI::Widget&);
    PaintResult paint(
        GUI::AbstractScrollableWidget&,
        GUI::PaintEvent&,
        SampleDocument&,
        SelectionState const&,
        CursorState const&,
        double scale,
        Optional<double> playback_position);

private:
    struct PaintGeometry {
        Gfx::IntRect frame;
        double width { 0.0 };
        double height { 0.0 };
        int h_pos { 0 };
        double viewport_start { 0.0 };
    };

    struct SceneDecision {
        int col_shift { 0 };
        int start_col { 0 };
        int end_col { 0 };
        bool changed { false };
        bool full_redraw { false };
    };

    PaintGeometry make_geometry(GUI::AbstractScrollableWidget&, double scale) const;
    SceneDecision decide_scene_path(PaintGeometry const&, SampleDocument&, double scale) const;
    void draw_timeline(
        GUI::Painter&,
        Gfx::Rect<int>,
        SampleEditorPalette&,
        int offset,
        double duration,
        double h_pos,
        double width,
        double scale);
    void draw_waveform_scene(
        GUI::AbstractScrollableWidget&,
        GUI::Painter&,
        SampleDocument&,
        SelectionState const&,
        CursorState const&,
        SampleEditorPalette&,
        PaintGeometry const&,
        SceneDecision const&,
        double scale);
    void paint_full_scene(GUI::AbstractScrollableWidget&, GUI::Painter&, Gfx::Bitmap&);
    void paint_partial_scroll_scene(
        GUI::AbstractScrollableWidget&,
        GUI::Painter&,
        Gfx::Bitmap& old_bitmap,
        Gfx::Bitmap& paint_bitmap,
        PaintGeometry const&,
        SceneDecision const&);
    void paint_cached_scene(GUI::AbstractScrollableWidget&, GUI::Painter&, Gfx::Bitmap& old_bitmap);
    void paint_drag_overlay(GUI::Painter&, SelectionState const&, PaintGeometry const&);
    void paint_playback_overlay(
        GUI::AbstractScrollableWidget&,
        GUI::Painter&,
        SampleEditorPalette&,
        PaintGeometry const&,
        double scale,
        Optional<double> playback_position);
    void update_stabilization_timer(GUI::AbstractScrollableWidget&, SceneDecision const&);

    Vector<RenderStruct> m_render_buffer;
    size_t m_render_width { 0 };
    RefPtr<Gfx::Bitmap> m_bitmap;
    RefPtr<Core::Timer> m_timer;
    static constexpr int timeline_offset = 16;
    double m_previous_width { -1 };
    double m_previous_height { -1 };
    double m_previous_scale { -1 };
    double m_previous_start { -1 };
    bool m_has_bitmap { false };
    bool m_must_redraw { false };
    bool m_drag_redraw { false };
    bool m_awaiting_repaint { false };
};
