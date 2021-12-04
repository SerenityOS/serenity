/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/NonnullRefPtr.h"
#include "KeysWidget.h"
#include "Music.h"
#include <LibDSP/Music.h>
#include <LibDSP/TrackManager.h>
#include <LibDSP/Transport.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <typeinfo>

using LibDSP::RollNote;

class RollWidget final : public GUI::AbstractScrollableWidget {
    C_OBJECT(RollWidget)
public:
    virtual ~RollWidget() override;

    const KeysWidget* keys_widget() const { return m_keys_widget; }
    void set_keys_widget(const KeysWidget* widget) { m_keys_widget = widget; }
    // FIXME: We assume the current track is a note track.
    NonnullRefPtr<LibDSP::NoteTrack> current_track() const
    {
        auto const& track = m_track_manager->track_at(m_current_track);
        VERIFY(typeid(track) == typeid(LibDSP::NoteTrack const&));
        return dynamic_cast<LibDSP::NoteTrack const&>(track);
    }

private:
    RollWidget(NonnullRefPtr<LibDSP::TrackManager>, NonnullRefPtr<LibDSP::Transport>);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;
    virtual void mouseup_event(GUI::MouseEvent& event) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    bool viewport_changed() const;

    NonnullRefPtr<LibDSP::TrackManager> m_track_manager;
    NonnullRefPtr<LibDSP::Transport> m_transport;
    size_t m_current_track;
    const KeysWidget* m_keys_widget;

    int m_roll_width { 0 };
    int m_num_notes { 0 };
    double m_note_width { 0.0 };
    int m_zoom_level { 1 };

    Optional<Gfx::IntPoint> m_note_drag_start;
    Optional<RollNote> m_note_drag_location;
    int m_drag_note;

    RefPtr<Gfx::Bitmap> m_background;
    int m_prev_zoom_level { m_zoom_level };
    int m_prev_scroll_x { horizontal_scrollbar().value() };
    int m_prev_scroll_y { vertical_scrollbar().value() };
};
