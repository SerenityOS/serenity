/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <LibDSP/Keyboard.h>
#include <LibDSP/TrackManager.h>
#include <LibGUI/Frame.h>

class KeysWidget final : public GUI::Frame {
    C_OBJECT(KeysWidget)
public:
    virtual ~KeysWidget() override;

    int key_code_to_key(int key_code) const;
    int mouse_note() const;

    void set_key(i8 key, LibDSP::Keyboard::Switch);
    bool note_is_set(int note) const;

private:
    explicit KeysWidget(NonnullRefPtr<LibDSP::TrackManager>, NonnullRefPtr<LibDSP::Keyboard>);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;

    int note_for_event_position(const Gfx::IntPoint&) const;

    NonnullRefPtr<LibDSP::TrackManager> m_track_manager;
    NonnullRefPtr<LibDSP::Keyboard> m_keyboard;

    u8 m_key_on[note_count] { 0 };

    bool m_mouse_down { false };
    int m_mouse_note { -1 };
};
