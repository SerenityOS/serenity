/*
 * Copyright (c) 2021, Richard Gráčik <r.gracik@gmail.com>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <LibCore/ElapsedTimer.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MouseTracker.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>
#include <unistd.h>

class CatDog final : public GUI::Widget
    , GUI::MouseTracker {
    C_OBJECT(CatDog);

public:
    static ErrorOr<NonnullRefPtr<CatDog>> create();

    virtual void timer_event(Core::TimerEvent&) override;
    virtual void paint_event(GUI::PaintEvent& event) override;
    virtual void track_mouse_move(Gfx::IntPoint point) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void context_menu_event(GUI::ContextMenuEvent& event) override;

    Function<void()> on_click;
    Function<void(GUI::ContextMenuEvent&)> on_context_menu_request;
    Function<void()> on_state_change;

    void set_roaming(bool roaming);
    void set_sleeping(bool sleeping);

    [[nodiscard]] bool is_artist() const;
    [[nodiscard]] bool is_inspector() const;
    [[nodiscard]] bool is_sleeping() const;

    void update();

private:
    enum class State : u16 {
        Frame1 = 0x0,
        Frame2 = 0x1,

        Up = 0x10,
        Down = 0x20,
        Left = 0x40,
        Right = 0x80,

        Directions = Up | Down | Left | Right,

        Roaming = 0x0100,
        Idle = 0x0200,
        Sleeping = 0x0400,
        Alert = 0x0800,

        GenericCatDog = 0x0000,
        Inspector = 0x1000,
        Artist = 0x2000
    };

    AK_ENUM_BITWISE_FRIEND_OPERATORS(State);

    struct ImageForState {
        State state;
        NonnullRefPtr<Gfx::Bitmap> bitmap;
    };

    Vector<ImageForState> m_images;

    Gfx::IntPoint m_mouse_offset {};
    Core::ElapsedTimer m_idle_sleep_timer;

    NonnullOwnPtr<Core::File> m_proc_all;

    State m_state { State::Roaming };
    State m_last_state { m_state };
    State m_frame { State::Frame1 };

    CatDog();

    [[nodiscard]] Gfx::Bitmap& bitmap_for_state() const;
    [[nodiscard]] State special_application_states() const;
};
