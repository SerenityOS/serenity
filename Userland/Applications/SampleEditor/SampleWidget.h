/*
 * Copyright (c) 2022, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RenderStruct.h"
#include "SampleBlockContainer.h"
#include "SampleEditorPalette.h"
#include "SampleFormatStruct.h"
#include "SampleRenderer.h"
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <LibAudio/ConnectionToServer.h>
#include <LibAudio/Sample.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/Timer.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Rect.h>
#include <LibGfx/TextAlignment.h>

class SampleWidget : public GUI::AbstractScrollableWidget {
    C_OBJECT(SampleWidget)
public:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;
    virtual void mouseup_event(GUI::MouseEvent& event) override;

    void zoom_in();
    void zoom_out();
    void select_all();
    ErrorOr<void> paste_from_text(String clipboard_text);
    void clear_selection();
    void set(NonnullRefPtr<SampleBlock> block);
    void clear();
    ErrorOr<void> save(StringView path);
    ErrorOr<String> selection();
    ErrorOr<String> cut();
    void play();
    void stop();
    bool is_playing() const { return m_playing; }
    bool has_selection() const { return m_selected; }
    bool has_cursor_placed() const { return m_cursor_placed; }
    bool is_initial_null_block() const { return m_samples.is_initial_null_block(); }

    Function<void()> on_playback_finished;
    Function<void()> on_selection_changed;

    void must_repaint()
    {
        m_must_redraw = true;
        repaint();
    }
    void drag_repaint()
    {
        m_drag_redraw = true;
        repaint();
    }

protected:
    SampleWidget();
    void draw_timeline(
        GUI::Painter painter,
        Gfx::Rect<int> frame,
        SampleEditorPalette& palette,
        int offset,
        double duration,
        double h_pos,
        double width);
    void zoom();
    void next_audio_buffer();
    AK::Optional<double> current_playback_position_ratio();
    SampleBlockContainer m_samples;
    RefPtr<SampleRenderer> m_renderer { nullptr };
    RefPtr<Gfx::Bitmap> m_bitmap;
    RefPtr<Core::Timer> m_timer;
    double m_cursor { 0.0 };
    double m_start { 0.0 };
    double m_scale { 1.0 };
    double m_previous_width { -1 };
    double m_previous_height { -1 };
    double m_previous_scale { -1 };
    double m_previous_start { -1 };
    bool m_selected { false };
    bool m_dragging { false };
    double m_selection_start { 0 };
    double m_selection_end { 1 };
    int m_selection_start_absolute { 0 };
    int m_selection_end_absolute { 1 };
    bool m_painting { false };
    bool m_waiting { false };
    bool m_cursor_placed { false };
    bool m_has_bitmap { false };
    bool m_must_redraw { false };
    bool m_drag_redraw { false };
    bool m_awaiting_repaint { false };
    bool m_drag_elapsed_timer_active { false };
    Core::ElapsedTimer m_drag_elapsed_timer;
    bool m_drag_paint_position_valid { false };
    int m_last_drag_paint_absolute { 0 };

    // Audio playback
    RefPtr<Audio::ConnectionToServer> m_audio_connection;
    RefPtr<Core::Timer> m_playback_timer;
    RefPtr<Core::Timer> m_playback_visual_timer;
    u64 m_playback_start_device_sample_index { 0 };
    size_t m_playback_total_sample_count { 0 };
    bool m_playing { false };
    bool m_finished_loading { false };
    size_t m_samples_to_load_per_buffer { 0 };
    FixedArray<Audio::Sample> m_current_audio_buffer;
    double m_playback_start { 0.0 };
    double m_playback_end { 1.0 };
    size_t m_samples_played { 0 };

    static constexpr size_t always_enqueued_buffer_count = 5;
    static constexpr u32 playback_update_rate_ms = 50;
    static constexpr u32 playback_visual_update_rate_ms = 16;
    static constexpr u32 buffer_size_ms = 100;
};
