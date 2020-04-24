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

#include "BreakpointCallback.h"
#include <AK/Optional.h>
#include <LibGUI/TextEditor.h>
#include <LibWeb/Forward.h>

class EditorWrapper;

class Editor final : public GUI::TextEditor {
    C_OBJECT(Editor)
public:
    virtual ~Editor() override;

    Function<void()> on_focus;
    Function<void(String)> on_open;

    EditorWrapper& wrapper();
    const EditorWrapper& wrapper() const;

    const Vector<size_t>& breakpoint_lines() const { return m_breakpoint_lines; }
    void set_execution_position(size_t line_number);
    void clear_execution_position();

    BreakpointChangeCallback on_breakpoint_change;

private:
    virtual void focusin_event(Core::Event&) override;
    virtual void focusout_event(Core::Event&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;

    void show_documentation_tooltip_if_available(const String&, const Gfx::Point& screen_location);
    void navigate_to_include_if_available(String);

    Gfx::Rect breakpoint_icon_rect(size_t line_number) const;
    static const Gfx::Bitmap& breakpoint_icon_bitmap();
    static const Gfx::Bitmap& current_position_icon_bitmap();

    explicit Editor();

    RefPtr<GUI::Window> m_documentation_tooltip_window;
    RefPtr<Web::HtmlView> m_documentation_html_view;
    String m_last_parsed_token;
    GUI::TextPosition m_previous_text_position { 0, 0 };
    bool m_hovering_editor { false };
    bool m_hovering_link { false };
    bool m_holding_ctrl { false };
    bool m_hovering_lines_ruler { false };

    Vector<size_t> m_breakpoint_lines;
    Optional<size_t> m_execution_position;
};
