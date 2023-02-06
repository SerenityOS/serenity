/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "KeysWidget.h"
#include "Music.h"
#include <LibDSP/Music.h>
#include <LibGUI/AbstractScrollableWidget.h>

class TrackManager;
using DSP::RollNote;

class RollWidget final : public GUI::AbstractScrollableWidget {
    C_OBJECT(RollWidget)
public:
    virtual ~RollWidget() override = default;

    KeysWidget const* keys_widget() const { return m_keys_widget; }
    void set_keys_widget(KeysWidget const* widget) { m_keys_widget = widget; }

private:
    explicit RollWidget(TrackManager&);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;
    virtual void mouseup_event(GUI::MouseEvent& event) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    bool viewport_changed() const;

    TrackManager& m_track_manager;
    KeysWidget const* m_keys_widget;

    int m_roll_width { 0 };
    int m_num_notes { 0 };
    double m_note_width { 0.0 };
    int m_zoom_level { 1 };

    Optional<GUI::MouseEvent> m_mousedown_event;

    RefPtr<Gfx::Bitmap> m_background;
    int m_prev_zoom_level { m_zoom_level };
    int m_prev_scroll_x { horizontal_scrollbar().value() };
    int m_prev_scroll_y { vertical_scrollbar().value() };

    u8 get_pitch_for_y(int y) const;
    int get_note_for_x(int x) const;
};
