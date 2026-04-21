/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <LibConfig/Listener.h>
#include <LibCore/Timer.h>
#include <LibGUI/Action.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Icon.h>
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

    ErrorOr<void> build_menus(GUI::Icon const& app_icon);
    GUI::Dialog::ExecResult check_terminal_quit();
    void adjust_font_size(float adjustment, Gfx::Font::AllowInexactSizeMatch);

    // ^Config::Listener
    virtual void config_bool_did_change(StringView domain, StringView group, StringView key, bool value) override;
    virtual void config_string_did_change(StringView domain, StringView group, StringView key, StringView value) override;
    virtual void config_i32_did_change(StringView domain, StringView group, StringView key, i32 value) override;

    RefPtr<VT::TerminalWidget> m_terminal;
    RefPtr<GUI::Window> m_find_window;
    RefPtr<GUI::Action> m_open_settings_action;
    RefPtr<Core::Timer> m_modified_state_check_timer;
    int m_ptm_fd { -1 };
    pid_t m_shell_pid { -1 };
    ByteString m_ptsname;
    bool m_should_confirm_close { true };

    // Cached config values
    VT::TerminalWidget::BellMode m_bell;
    VT::TerminalWidget::AutoMarkMode m_automark;
    VT::CursorShape m_cursor_shape { VT::CursorShape::Block };
    bool m_cursor_blinking { true };
    i32 m_opacity { 255 };
    bool m_show_scroll_bar { true };
};
