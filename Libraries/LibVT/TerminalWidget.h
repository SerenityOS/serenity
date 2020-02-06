/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/String.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/Notifier.h>
#include <LibCore/Timer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>
#include <LibGUI/Frame.h>
#include <LibVT/Terminal.h>

namespace GUI {
class ScrollBar;
}

class TerminalWidget final : public GUI::Frame
    , public VT::TerminalClient {
    C_OBJECT(TerminalWidget)
public:
    TerminalWidget(int ptm_fd, bool automatic_size_policy, RefPtr<Core::ConfigFile> config);
    virtual ~TerminalWidget() override;

    void set_pty_master_fd(int fd);
    void inject_string(const StringView& string)
    {
        m_terminal.inject_string(string);
        flush_dirty_lines();
    }

    void create_window();

    void flush_dirty_lines();
    void force_repaint();

    void apply_size_increments_to_window(GUI::Window&);

    const Gfx::Font& bold_font() const { return *m_bold_font; }

    void set_opacity(u8);
    float opacity() { return m_opacity; };
    bool should_beep() { return m_should_beep; }
    void set_should_beep(bool sb) { m_should_beep = sb; };

    RefPtr<Core::ConfigFile> config() const { return m_config; }

    bool has_selection() const;
    bool selection_contains(const VT::Position&) const;
    String selected_text() const;
    VT::Position buffer_position_at(const Gfx::Point&) const;
    VT::Position normalized_selection_start() const;
    VT::Position normalized_selection_end() const;

    bool is_scrollable() const;

    GUI::Action& copy_action() { return *m_copy_action; }
    GUI::Action& paste_action() { return *m_paste_action; }

    void copy();
    void paste();

    virtual bool accepts_focus() const override { return true; }

    Function<void(const StringView&)> on_title_change;
    Function<void()> on_command_exit;

private:
    // ^GUI::Widget
    virtual void event(Core::Event&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void focusin_event(Core::Event&) override;
    virtual void focusout_event(Core::Event&) override;
    virtual void context_menu_event(GUI::ContextMenuEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;
    virtual void did_change_font() override;

    // ^TerminalClient
    virtual void beep() override;
    virtual void set_window_title(const StringView&) override;
    virtual void terminal_did_resize(u16 columns, u16 rows) override;
    virtual void terminal_history_changed() override;
    virtual void emit_char(u8) override;

    void set_logical_focus(bool);

    Gfx::Rect glyph_rect(u16 row, u16 column);
    Gfx::Rect row_rect(u16 row);

    void update_cursor();
    void invalidate_cursor();

    void relayout(const Gfx::Size&);

    Gfx::Size compute_base_size() const;
    int first_selection_column_on_row(int row) const;
    int last_selection_column_on_row(int row) const;

    VT::Terminal m_terminal;

    VT::Position m_selection_start;
    VT::Position m_selection_end;

    bool m_should_beep { false };
    bool m_belling { false };

    int m_pixel_width { 0 };
    int m_pixel_height { 0 };

    int m_inset { 2 };
    int m_line_spacing { 4 };
    int m_line_height { 0 };

    int m_ptm_fd { -1 };

    bool m_has_logical_focus { false };

    RefPtr<Core::Notifier> m_notifier;

    u8 m_opacity { 255 };
    bool m_needs_background_fill { true };
    bool m_cursor_blink_state { true };
    bool m_automatic_size_policy { false };

    RefPtr<Gfx::Font> m_bold_font;

    int m_glyph_width { 0 };

    RefPtr<Core::Timer> m_cursor_blink_timer;
    RefPtr<Core::Timer> m_visual_beep_timer;
    RefPtr<Core::ConfigFile> m_config;

    RefPtr<GUI::ScrollBar> m_scrollbar;

    RefPtr<GUI::Action> m_copy_action;
    RefPtr<GUI::Action> m_paste_action;

    RefPtr<GUI::Menu> m_context_menu;

    Core::ElapsedTimer m_triple_click_timer;
};
