/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibConfig/Client.h>
#include <LibConfig/Listener.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

class GlobalMenuWindow final : public GUI::Window
    , public Config::Listener {
    C_OBJECT(GlobalMenuWindow);

public:
    virtual ~GlobalMenuWindow() override = default;

    static int global_menu_height() { return 26; }

    virtual void config_bool_did_change(StringView, StringView, StringView, bool) override;

private:
    explicit GlobalMenuWindow();

    void on_screen_rects_change(Vector<Gfx::IntRect, 4> const&, size_t);

    void update_global_menu_area();
    void update_global_menu_enabled();

    virtual void screen_rects_change_event(GUI::ScreenRectsChangeEvent&) override;

    bool m_enabled { false };
    Gfx::IntSize m_global_menu_area_size;
    RefPtr<GUI::Frame> m_global_menu_area_container;
};
