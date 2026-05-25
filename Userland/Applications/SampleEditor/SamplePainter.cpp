/*
 * Copyright (c) 2026, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SamplePainter.h"

#include <AK/Math.h>
#include <AK/Span.h>
#include <AK/StringBuilder.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Point.h>

// Cubic curve mapping [0,1]→[0,1] that boosts mid-range amplitudes for visual clarity.
static double emphasized_amplitude(double amplitude)
{
    double clamped = AK::clamp(amplitude, 0.0, 1.0);
    return (4.0 / 3.0) * clamped * clamped * clamped - 3.0 * clamped * clamped + (8.0 / 3.0) * clamped;
}

void SamplePainter::reset()
{
    m_render_buffer.clear();
    m_render_width = 0;
    m_bitmap = nullptr;
    m_previous_width = -1;
    m_previous_height = -1;
    m_previous_scale = -1;
    m_previous_start = -1;
    m_has_bitmap = false;
    m_must_redraw = false;
    m_drag_redraw = false;
    m_awaiting_repaint = false;
}

void SamplePainter::invalidate_scene_and_repaint(GUI::Widget& widget)
{
    m_must_redraw = true;
    widget.repaint();
}

void SamplePainter::invalidate_drag_overlay_and_repaint(GUI::Widget& widget)
{
    m_drag_redraw = true;
    widget.repaint();
}

SamplePainter::PaintGeometry SamplePainter::make_geometry(GUI::AbstractScrollableWidget& widget, double scale) const
{
    auto frame = widget.frame_inner_rect();
    double width = frame.width();
    int h_pos = widget.horizontal_scrollbar().value();

    return {
        .frame = frame,
        .width = width,
        .height = static_cast<double>(widget.content_rect().height()) - timeline_offset - widget.horizontal_scrollbar().height(),
        .h_pos = h_pos,
        .viewport_start = static_cast<double>(h_pos) / width / scale,
    };
}

SamplePainter::SceneDecision SamplePainter::decide_scene_path(PaintGeometry const& geometry, SampleDocument& samples, double scale) const
{
    SceneDecision decision {
        .end_col = static_cast<int>(geometry.width),
    };

    if (geometry.width == m_previous_width && geometry.height == m_previous_height && m_previous_scale == scale && m_previous_start == geometry.viewport_start && samples.is_render_valid())
        return decision;

    decision.full_redraw = true;
    decision.changed = true;

    if (geometry.width == m_previous_width && geometry.height == m_previous_height && scale == m_previous_scale) {
        decision.full_redraw = false;
        decision.col_shift = static_cast<int>((geometry.viewport_start - m_previous_start) * scale * geometry.width);

        if ((decision.col_shift > 0 && decision.col_shift > geometry.width) || (decision.col_shift < 0 && -decision.col_shift > geometry.width)) {
            decision.full_redraw = true;
        } else if (decision.col_shift > 0) {
            decision.start_col = static_cast<int>(geometry.width) - decision.col_shift;
            decision.end_col = static_cast<int>(geometry.width);
        } else if (decision.col_shift < 0) {
            decision.start_col = 0;
            decision.end_col = -decision.col_shift;
        } else {
            decision.changed = false;
        }
    }

    return decision;
}

void SamplePainter::draw_timeline(
    GUI::Painter& painter,
    Gfx::Rect<int> frame,
    SampleEditorPalette& colors,
    int offset,
    double duration,
    double h_pos,
    double width,
    double scale)
{
    auto timeline_rect = frame;
    timeline_rect.set_height(offset);
    painter.fill_rect(timeline_rect, colors.timeline_background_color);

    double duration_on_view = duration / scale;
    double start_seconds = h_pos / width * duration / scale;
    double mag = floor(log10(duration_on_view));
    double tick = pow(10, mag);
    double first_tick = start_seconds - fmod(start_seconds, tick);

    for (double t = first_tick; t < start_seconds + duration_on_view; t += tick) {
        for (double t2 = t; t2 < t + tick; t2 += (tick / 10)) {
            double x2 = (t2 - start_seconds) / duration_on_view * width;
            Gfx::IntPoint sub_mark_top = { static_cast<int>(x2), offset - (offset / 8) };
            Gfx::IntPoint sub_mark_bottom = { static_cast<int>(x2), offset };
            painter.draw_line(sub_mark_top, sub_mark_bottom, colors.timeline_sub_mark_color);
        }

        double x = (t - start_seconds) / duration_on_view * width;

        Gfx::IntPoint mark_top = { static_cast<int>(x), offset / 2 };
        Gfx::IntPoint mark_bottom = { static_cast<int>(x), offset };
        painter.draw_line(mark_top, mark_bottom, colors.timeline_main_mark_color);

        Gfx::IntRect text_rec = { static_cast<int>(x) + 3, (offset / 2) - (offset / 8),
            (offset / 2), (offset / 2) };

        StringBuilder time;

        if (mag < 0) {
            StringBuilder format;
            format.appendff("{{:.{}}}", mag * -1);
            time.appendff(format.string_view(), t + (tick / 2));
        } else {
            time.appendff("{}", t);
        }

        painter.draw_text(text_rec, time.string_view(), Gfx::TextAlignment::TopLeft,
            colors.black);
    }
}

void SamplePainter::draw_waveform_scene(
    GUI::AbstractScrollableWidget& widget,
    GUI::Painter& painter,
    SampleDocument& samples,
    SelectionState const& selection,
    CursorState const& cursor_state,
    SampleEditorPalette& colors,
    PaintGeometry const& geometry,
    SceneDecision const& decision,
    double scale)
{
    double duration = samples.duration();
    double half = geometry.height / 2;

    painter.fill_rect(geometry.frame, colors.window_color);
    Gfx::IntPoint left = { 0, static_cast<int>(half) + timeline_offset };
    Gfx::IntPoint right = { static_cast<int>(geometry.width), static_cast<int>(half) + timeline_offset };
    int top_y = geometry.frame.y();
    int bottom_y = geometry.frame.height() - geometry.frame.y() - widget.horizontal_scrollbar().height();

    draw_timeline(painter, geometry.frame, colors, timeline_offset, duration, geometry.h_pos, geometry.width, scale);

    if (!decision.changed && !decision.full_redraw && !m_must_redraw)
        return;

    int selection_start = static_cast<int>(geometry.width * scale * (selection.start - geometry.viewport_start));
    int selection_end = static_cast<int>(geometry.width * scale * (selection.end - geometry.viewport_start));
    int cursor = static_cast<int>(geometry.width * scale * (cursor_state.position - geometry.viewport_start));

    if (selection_start > selection_end + 1) {
        auto temp = selection_end;
        selection_end = selection_start;
        selection_start = temp;
    }

    for (int col = decision.start_col; col < decision.end_col; col++) {
        Gfx::Color waveform_light = colors.light_blue;
        Gfx::Color waveform_dark = colors.dark_blue;

        Gfx::IntPoint top = { col, top_y + timeline_offset };
        Gfx::IntPoint bottom = { col, bottom_y + timeline_offset };
        Gfx::IntPoint timeline_top = { col, 0 };
        Gfx::IntPoint timeline_bottom = { col, timeline_offset };

        if (selection.selected && (col >= selection_start) && (col <= selection_end)) {
            painter.draw_line(top, bottom, colors.selection_color);
            painter.draw_line(timeline_top, timeline_bottom,
                colors.timeline_selection_color);
            waveform_light = colors.light_gray.lightened();
            waveform_dark = colors.dark_gray.lightened();
        }

        if (cursor_state.placed && (col == cursor)) {
            painter.draw_line(top, bottom, colors.cursor_color);
            painter.draw_line(timeline_top, timeline_bottom,
                colors.timeline_cursor_color);
        }

        RenderStruct value = m_render_buffer[col];
        auto emphasized_peak_plus = emphasized_amplitude(value.peak_plus);
        auto emphasized_peak_minus = emphasized_amplitude(value.peak_minus);

        double y_max = half * (1 + (emphasized_peak_plus * 0.9));
        double y_min = half * (1 - (emphasized_peak_minus * 0.9));

        Gfx::IntPoint min_point = { col, static_cast<int>(y_max) + timeline_offset };
        Gfx::IntPoint max_point = { col, static_cast<int>(y_min) + timeline_offset };

        painter.draw_line(min_point, max_point, waveform_dark);

        auto emphasized_rms_plus = emphasized_amplitude(value.rms_plus);
        auto emphasized_rms_minus = emphasized_amplitude(value.rms_minus);
        y_max = half * (1 + (emphasized_rms_plus * 0.45));
        y_min = half * (1 - (emphasized_rms_minus * 0.45));

        min_point = { col, static_cast<int>(y_max) + timeline_offset };
        max_point = { col, static_cast<int>(y_min) + timeline_offset };

        painter.draw_line(min_point, max_point, waveform_light);
    }

    painter.draw_line(left, right, colors.dark_blue);
}

void SamplePainter::paint_full_scene(GUI::AbstractScrollableWidget& widget, GUI::Painter& real_painter, Gfx::Bitmap& paint_bitmap)
{
    GUI::Painter composite_painter(*m_bitmap);
    composite_painter.blit(widget.frame_inner_rect().top_left(), paint_bitmap, widget.frame_inner_rect());
    real_painter.blit(widget.frame_inner_rect().top_left(), *m_bitmap, widget.frame_inner_rect());
    m_has_bitmap = true;
}

void SamplePainter::paint_partial_scroll_scene(
    GUI::AbstractScrollableWidget& widget,
    GUI::Painter& real_painter,
    Gfx::Bitmap& old_bitmap,
    Gfx::Bitmap& paint_bitmap,
    PaintGeometry const& geometry,
    SceneDecision const& decision)
{
    GUI::Painter composite_painter(*m_bitmap);

    Gfx::Point<int> old_dest;
    Gfx::Rect<int> old_source;
    Gfx::Point<int> new_dest;
    Gfx::Rect<int> new_source;

    auto x = geometry.frame.x();
    auto y = geometry.frame.y();
    auto w = geometry.frame.width();
    auto h = geometry.frame.height();

    if (decision.col_shift < 0) {
        old_dest = Gfx::Point<int>(-decision.col_shift, y);
        old_source = Gfx::Rect(x, y, w + decision.col_shift, h);
        new_dest = Gfx::Point<int>(x, y);
        new_source = Gfx::Rect(x, y, -decision.col_shift, h);
    } else {
        old_dest = Gfx::Point<int>(x, y);
        old_source = Gfx::Rect(decision.col_shift, y, w - decision.col_shift, h);
        new_dest = Gfx::Point<int>(x + w - decision.col_shift, y);
        new_source = Gfx::Rect(x + w - decision.col_shift, y, decision.col_shift, h);
    }

    composite_painter.blit(new_dest, paint_bitmap, new_source);
    composite_painter.blit(old_dest, old_bitmap, old_source);
    real_painter.blit(widget.frame_inner_rect().top_left(), *m_bitmap, widget.frame_inner_rect());
    m_has_bitmap = true;
}

void SamplePainter::paint_cached_scene(GUI::AbstractScrollableWidget& widget, GUI::Painter& real_painter, Gfx::Bitmap& old_bitmap)
{
    GUI::Painter composite_painter(*m_bitmap);
    composite_painter.blit(widget.frame_inner_rect().top_left(), old_bitmap, widget.frame_inner_rect());
    real_painter.blit(widget.frame_inner_rect().top_left(), *m_bitmap, widget.frame_inner_rect());
}

void SamplePainter::paint_drag_overlay(GUI::Painter& real_painter, SelectionState const& selection, PaintGeometry const& geometry)
{
    if (!m_has_bitmap)
        return;

    int start = min(selection.start_absolute, selection.end_absolute);
    int end = max(selection.start_absolute, selection.end_absolute);
    auto y = geometry.frame.y();
    auto h = geometry.frame.height();

    if (start > 0) {
        auto left_dest = Gfx::Point<int>(0, y);
        auto left_source = Gfx::Rect<int>(0, y, start - 1, h);
        real_painter.blit(left_dest, *m_bitmap, left_source);
    }

    if (end < geometry.width - 1) {
        auto right_dest = Gfx::Point<int>(end, y);
        auto right_source = Gfx::Rect<int>(end, y, static_cast<int>(geometry.width) - end - 1, h);
        real_painter.blit(right_dest, *m_bitmap, right_source);
    }

    auto dest = Gfx::Point<int>(start, y);
    auto source = Gfx::Rect<int>(start, y, end - start, h);

    real_painter.blit_dimmed(dest, *m_bitmap, source);
}

void SamplePainter::paint_playback_overlay(
    GUI::AbstractScrollableWidget& widget,
    GUI::Painter& real_painter,
    SampleEditorPalette& colors,
    PaintGeometry const& geometry,
    double scale,
    Optional<double> playback_position)
{
    if (!playback_position.has_value() || scale == 0.0)
        return;

    double visible_start = geometry.viewport_start;
    double visible_end = geometry.viewport_start + (1.0 / scale);
    if (playback_position.value() < visible_start || playback_position.value() > visible_end)
        return;

    int playback_cursor = AK::round_to<int>(geometry.width * scale * (playback_position.value() - geometry.viewport_start));
    if (playback_cursor < 0 || playback_cursor >= static_cast<int>(geometry.width))
        return;

    int absolute_x = geometry.frame.x() + playback_cursor;
    int inner_top = geometry.frame.y();
    int inner_bottom = geometry.frame.y() + geometry.frame.height();
    int scrollbar_height = widget.horizontal_scrollbar().is_visible() ? widget.horizontal_scrollbar().height() : 0;
    int timeline_top_y = inner_top;
    int timeline_bottom_y = min(inner_top + timeline_offset, inner_bottom);
    int waveform_top_y = timeline_bottom_y;
    int waveform_bottom_y = inner_bottom - scrollbar_height;
    waveform_bottom_y = max(waveform_top_y, waveform_bottom_y);

    if (timeline_bottom_y > timeline_top_y)
        real_painter.draw_line({ absolute_x, timeline_top_y }, { absolute_x, timeline_bottom_y }, colors.timeline_cursor_color);
    if (waveform_bottom_y > waveform_top_y)
        real_painter.draw_line({ absolute_x, waveform_top_y }, { absolute_x, waveform_bottom_y }, colors.cursor_color);
}

void SamplePainter::update_stabilization_timer(GUI::AbstractScrollableWidget& widget, SceneDecision const& decision)
{
    if (!m_timer) {
        m_timer = Core::Timer::create_single_shot(25, [this, &widget]() {
            m_awaiting_repaint = false;
            invalidate_scene_and_repaint(widget);
        });
    }

    if (decision.changed) {
        m_awaiting_repaint = true;
        m_timer->restart();
    }
}

SamplePainter::PaintResult SamplePainter::paint(
    GUI::AbstractScrollableWidget& widget,
    [[maybe_unused]] GUI::PaintEvent& event,
    SampleDocument& samples,
    SelectionState const& selection,
    CursorState const& cursor_state,
    double scale,
    Optional<double> playback_position)
{
    GUI::Painter real_painter(widget);

    RefPtr<Gfx::Bitmap> old_bitmap;
    if (m_has_bitmap) {
        auto clone_or_error = m_bitmap->clone();
        if (!clone_or_error.is_error())
            old_bitmap = clone_or_error.release_value();
    }

    auto colors = SampleEditorPalette(widget.palette());
    auto geometry = make_geometry(widget, scale);
    bool samples_were_unused = !samples.is_render_valid();

    SceneDecision decision;

    if (!m_drag_redraw) {
        decision = decide_scene_path(geometry, samples, scale);
        samples.mark_render_valid();

        bool should_rebuild_scene = decision.changed || decision.full_redraw || m_must_redraw || !m_has_bitmap;
        if (should_rebuild_scene) {
            m_render_width = static_cast<size_t>(geometry.width);
            m_render_buffer.resize(m_render_width);
            size_t start_col = static_cast<size_t>(decision.start_col);
            size_t end_col = static_cast<size_t>(decision.end_col);
            if (start_col < end_col && end_col <= m_render_width) {
                samples.fill_render_columns(
                    geometry.viewport_start, scale,
                    start_col, end_col, m_render_width,
                    Span<RenderStruct>(m_render_buffer.data() + start_col, end_col - start_col));
            }

            auto paint_bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, widget.frame_inner_rect().size()));
            GUI::Painter painter(*paint_bitmap);

            m_bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, widget.frame_inner_rect().size()));

            draw_waveform_scene(widget, painter, samples, selection, cursor_state, colors, geometry, decision, scale);

            if (decision.full_redraw || !m_has_bitmap || m_must_redraw) {
                paint_full_scene(widget, real_painter, *paint_bitmap);
            } else {
                paint_partial_scroll_scene(widget, real_painter, *old_bitmap, *paint_bitmap, geometry, decision);
            }
        } else if (m_has_bitmap) {
            paint_cached_scene(widget, real_painter, *old_bitmap);
        }

    } else {
        paint_drag_overlay(real_painter, selection, geometry);
    }

    m_must_redraw = false;
    m_drag_redraw = false;

    m_previous_width = geometry.width;
    m_previous_height = geometry.height;
    m_previous_scale = scale;
    m_previous_start = geometry.viewport_start;

    update_stabilization_timer(widget, decision);
    paint_playback_overlay(widget, real_painter, colors, geometry, scale, playback_position);

    return {
        .viewport_start = geometry.viewport_start,
        .samples_were_unused = samples_were_unused,
    };
}
