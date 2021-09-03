/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Vector.h>
#include <YAK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>

namespace WindowServer {

class KeyEvent;
class Window;

class WindowSwitcher final : public Core::Object {
    C_OBJECT(WindowSwitcher)
public:
    enum class Mode {
        ShowAllWindows,
        ShowCurrentDesktop
    };
    static WindowSwitcher& the();

    WindowSwitcher();
    virtual ~WindowSwitcher() override;

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    void show(Mode mode)
    {
        m_mode = mode;
        set_visible(true);
    }
    void hide() { set_visible(false); }

    void on_key_event(const KeyEvent&);

    void refresh();
    void refresh_if_needed();

    void select_window(Window&);

    Mode mode() const { return m_mode; }

private:
    int thumbnail_width() const { return 40; }
    int thumbnail_height() const { return 40; }
    int item_height() const { return 10 + thumbnail_height(); }
    int padding() const { return 8; }
    int item_padding() const { return 8; }

    void draw();
    void redraw();
    void select_window_at_index(int index);
    Gfx::IntRect item_rect(int index) const;
    Window* selected_window();

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
