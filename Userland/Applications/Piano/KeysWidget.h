/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <AK/NonnullRefPtr.h>
#include <LibDSP/Keyboard.h>
#include <LibGUI/Frame.h>

class TrackManager;

class KeysWidget final : public GUI::Frame {
    C_OBJECT(KeysWidget)
public:
    virtual ~KeysWidget() override = default;

    static i8 key_code_to_key(int key_code);
    int mouse_note() const;

private:
    KeysWidget(NonnullRefPtr<DSP::Keyboard>);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;

    int note_for_event_position(Gfx::IntPoint) const;

    void set_key(i8 key, DSP::Keyboard::Switch);

    NonnullRefPtr<DSP::Keyboard> m_keyboard;

    bool m_mouse_down { false };
    int m_mouse_note { -1 };
};
