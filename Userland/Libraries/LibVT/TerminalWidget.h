/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/Notifier.h>
#include <LibCore/Timer.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Frame.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>
#include <LibVT/Color.h>
#include <LibVT/Range.h>
#include <LibVT/Terminal.h>

namespace VT {

class TerminalWidget final
    : public GUI::Frame
    , public VT::TerminalClient
    , public GUI::Clipboard::ClipboardClient {
    C_OBJECT(TerminalWidget);

public:
    virtual ~TerminalWidget() override;

    void set_pty_master_fd(int fd);
    void inject_string(StringView string)
    {
        m_terminal.inject_string(string);
        flush_dirty_lines();
    }

    void flush_dirty_lines();

    void apply_size_increments_to_window(GUI::Window&);

    void set_opacity(u8);
    float opacity() { return m_opacity; };

    enum class BellMode {
        Visible,
        AudibleBeep,
        Disabled
    };

    BellMode bell_mode() { return m_bell_mode; }
    void set_bell_mode(BellMode bm) { m_bell_mode = bm; };

    bool has_selection() const;
    bool selection_contains(const VT::Position&) const;
    String selected_text() const;
    VT::Range normalized_selection() const { return m_selection.normalized(); }
    void set_selection(const VT::Range& selection);
    VT::Position buffer_position_at(const Gfx::IntPoint&) const;

    VT::Range find_next(StringView, const VT::Position& start = {}, bool case_sensitivity = false, bool should_wrap = false);
    VT::Range find_previous(StringView, const VT::Position& start = {}, bool case_sensitivity = false, bool should_wrap = false);

    void scroll_to_bottom();
    void scroll_to_row(int);

    bool is_scrollable() const;
    int scroll_length() const;

    size_t max_history_size() const { return m_terminal.max_history_size(); }
    void set_max_history_size(size_t value) { m_terminal.set_max_history_size(value); }

    GUI::Action& copy_action() { return *m_copy_action; }
    GUI::Action& paste_action() { return *m_paste_action; }
    GUI::Action& clear_including_history_action() { return *m_clear_including_history_action; }

    void copy();
    void paste();
    void clear_including_history();

    const StringView color_scheme_name() const { return m_color_scheme_name; }

    Function<void(StringView)> on_title_change;
    Function<void(const Gfx::IntSize&)> on_terminal_size_change;
    Function<void()> on_command_exit;

    GUI::Menu& context_menu() { return *m_context_menu; }

    constexpr Gfx::Color terminal_color_to_rgb(VT::Color) const;

    void set_font_and_resize_to_fit(const Gfx::Font&);

    void set_color_scheme(StringView);

private:
    TerminalWidget(int ptm_fd, bool automatic_size_policy);

    // ^GUI::Widget
    virtual void event(Core::Event&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void focusin_event(GUI::FocusEvent&) override;
    virtual void focusout_event(GUI::FocusEvent&) override;
    virtual void context_menu_event(GUI::ContextMenuEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void did_change_font() override;

    // ^TerminalClient
    virtual void beep() override;
    virtual void set_window_title(StringView) override;
    virtual void set_window_progress(int value, int max) override;
    virtual void terminal_did_resize(u16 columns, u16 rows) override;
    virtual void terminal_history_changed(int delta) override;
    virtual void emit(const u8*, size_t) override;
    virtual void set_cursor_style(CursorStyle) override;

    // ^GUI::Clipboard::ClipboardClient
    virtual void clipboard_content_did_change(const String&) override { update_paste_action(); }

    void set_logical_focus(bool);

    void send_non_user_input(ReadonlyBytes);

    Gfx::IntRect glyph_rect(u16 row, u16 column);
    Gfx::IntRect row_rect(u16 row);

    Gfx::IntSize widget_size_for_font(const Gfx::Font&) const;

    void update_cursor();
    void invalidate_cursor();

    void relayout(const Gfx::IntSize&);

    void update_copy_action();
    void update_paste_action();

    Gfx::IntSize compute_base_size() const;
    int first_selection_column_on_row(int row) const;
    int last_selection_column_on_row(int row) const;

    u32 code_point_at(const VT::Position&) const;
    VT::Position next_position_after(const VT::Position&, bool should_wrap) const;
    VT::Position previous_position_before(const VT::Position&, bool should_wrap) const;

    VT::Terminal m_terminal;

    VT::Range m_selection;

    String m_hovered_href;
    String m_hovered_href_id;

    String m_active_href;
    String m_active_href_id;

    // Snapshot of m_hovered_href when opening a context menu for a hyperlink.
    String m_context_menu_href;

    unsigned m_colors[256];
    Gfx::Color m_default_foreground_color;
    Gfx::Color m_default_background_color;
    bool m_show_bold_text_as_bright { true };

    String m_color_scheme_name;

    BellMode m_bell_mode { BellMode::Visible };
    bool m_alt_key_held { false };
    bool m_rectangle_selection { false };

    int m_pixel_width { 0 };
    int m_pixel_height { 0 };

    int m_inset { 2 };
    int m_line_spacing { 4 };
    int m_line_height { 0 };

    int m_ptm_fd { -1 };

    bool m_has_logical_focus { false };
    bool m_in_relayout { false };

    RefPtr<Core::Notifier> m_notifier;

    u8 m_opacity { 255 };
    bool m_cursor_blink_state { true };
    bool m_automatic_size_policy { false };

    VT::CursorStyle m_cursor_style { BlinkingBlock };

    enum class AutoScrollDirection {
        None,
        Up,
        Down
    };

    AutoScrollDirection m_auto_scroll_direction { AutoScrollDirection::None };

    RefPtr<Core::Timer> m_cursor_blink_timer;
    RefPtr<Core::Timer> m_visual_beep_timer;
    RefPtr<Core::Timer> m_auto_scroll_timer;

    RefPtr<GUI::Scrollbar> m_scrollbar;

    RefPtr<GUI::Action> m_copy_action;
    RefPtr<GUI::Action> m_paste_action;
    RefPtr<GUI::Action> m_clear_including_history_action;

    RefPtr<GUI::Menu> m_context_menu;
    RefPtr<GUI::Menu> m_context_menu_for_hyperlink;

    Core::ElapsedTimer m_triple_click_timer;

    Gfx::IntPoint m_left_mousedown_position;
    VT::Position m_left_mousedown_position_buffer;
};

}
