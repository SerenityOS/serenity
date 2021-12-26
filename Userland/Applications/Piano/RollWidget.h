/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "KeysWidget.h"
#include "Music.h"
#include <LibGUI/ScrollableWidget.h>

class TrackManager;

class RollWidget final : public GUI::ScrollableWidget {
    C_OBJECT(RollWidget)
public:
    virtual ~RollWidget() override;

    const KeysWidget* keys_widget() const { return m_keys_widget; }
    void set_keys_widget(const KeysWidget* widget) { m_keys_widget = widget; }

private:
    explicit RollWidget(TrackManager&);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;
    virtual void mouseup_event(GUI::MouseEvent& event) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;

    TrackManager& m_track_manager;
    const KeysWidget* m_keys_widget;

    int m_roll_width { 0 };
    int m_num_notes { 0 };
    double m_note_width { 0.0 };
    int m_zoom_level { 1 };

    Optional<Gfx::IntPoint> m_note_drag_start;
    Optional<RollNote> m_note_drag_location;
    int m_drag_note;
};
