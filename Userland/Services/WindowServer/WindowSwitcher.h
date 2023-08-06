/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibCore/EventReceiver.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>

namespace WindowServer {

class KeyEvent;
class Window;

class WindowSwitcher final : public Core::EventReceiver {
    C_OBJECT(WindowSwitcher)
public:
    enum class Mode {
        ShowAllWindows,
        ShowCurrentDesktop
    };
    static WindowSwitcher& the();

    virtual ~WindowSwitcher() override = default;

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    void show(Mode mode)
    {
        m_mode = mode;
        set_visible(true);
    }
    void hide() { set_visible(false); }

    void on_key_event(KeyEvent const&);

    void refresh();
    void refresh_if_needed();

    void select_window(Window&);

    Mode mode() const { return m_mode; }

private:
    WindowSwitcher();

    int thumbnail_width() const { return 64; }
    int thumbnail_height() const { return 64; }
    int item_height() const { return 14 + thumbnail_height(); }
    int padding() const { return 30; }
    int item_padding() const { return 10; }

    void draw();
    void redraw();
    void select_window_at_index(int index);
    Gfx::IntRect item_rect(int index) const;
    Window* selected_window();
    void clear_hovered_index();

    virtual void event(Core::Event&) override;

    RefPtr<Window> m_switcher_window;
    Mode m_mode { Mode::ShowCurrentDesktop };
    Gfx::IntRect m_rect;
    bool m_visible { false };
    bool m_windows_on_multiple_stacks { false };
    Vector<WeakPtr<Window>> m_windows;
    int m_selected_index { 0 };
    int m_hovered_index { -1 };
};

}
