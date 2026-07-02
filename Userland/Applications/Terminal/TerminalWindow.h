/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Vector.h>
#include <LibConfig/Listener.h>
#include <LibCore/Timer.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/Font.h>
#include <LibVT/TerminalWidget.h>

class TerminalWindow final : public GUI::Window
    , public Config::Listener {
    C_OBJECT(TerminalWindow);

public:
    virtual ~TerminalWindow() override = default;

    ErrorOr<void> initialize(StringView initial_command, bool keep_open);
    ErrorOr<void> cleanup();

private:
    TerminalWindow();

    struct TabData {
        int ptm_fd;
        pid_t shell_pid;
        ByteString ptsname;
        ByteString title;
        RefPtr<VT::TerminalWidget> terminal;
        RefPtr<GUI::Window> find_window;
    };

    ErrorOr<void> build_menus(GUI::Icon const& app_icon);
    VT::TerminalWidget& active_terminal();
    ErrorOr<void> create_terminal_tab(int ptm_fd, pid_t shell_pid, ByteString ptsname);
    void close_terminal_tab(VT::TerminalWidget&);
    ErrorOr<void> open_new_terminal_tab(StringView cmd = {}, bool keep_open = false);
    GUI::Dialog::ExecResult check_terminal_quit();
    GUI::Dialog::ExecResult check_tab_quit(VT::TerminalWidget&);
    void adjust_font_size(float adjustment, Gfx::Font::AllowInexactSizeMatch);

    // ^Config::Listener
    virtual void config_bool_did_change(StringView domain, StringView group, StringView key, bool value) override;
    virtual void config_string_did_change(StringView domain, StringView group, StringView key, StringView value) override;
    virtual void config_i32_did_change(StringView domain, StringView group, StringView key, i32 value) override;

    RefPtr<GUI::TabWidget> m_tab_widget;
    RefPtr<GUI::HorizontalSeparator> m_top_line;
    RefPtr<GUI::Action> m_open_settings_action;
    Vector<TabData> m_tab_data_list;
    bool m_should_confirm_close { true };
    RefPtr<Core::Timer> m_modified_state_check_timer;

    // Cached config values
    VT::TerminalWidget::BellMode m_bell;
    VT::TerminalWidget::AutoMarkMode m_automark;
    VT::CursorShape m_cursor_shape { VT::CursorShape::Block };
    bool m_cursor_blinking { true };
    i32 m_opacity { 255 };
    bool m_show_scroll_bar { true };
};
