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

#include "EditorWrapper.h"
#include "Editor.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGfx/Font.h>

extern RefPtr<EditorWrapper> g_current_editor_wrapper;

EditorWrapper::EditorWrapper()
{
    set_layout(make<GUI::VerticalBoxLayout>());

    auto label_wrapper = add<GUI::Widget>();
    label_wrapper->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    label_wrapper->set_preferred_size(0, 14);
    label_wrapper->set_fill_with_background_color(true);
    label_wrapper->set_layout(make<GUI::HorizontalBoxLayout>());
    label_wrapper->layout()->set_margins({ 2, 0, 2, 0 });

    m_filename_label = label_wrapper->add<GUI::Label>("(Untitled)");
    m_filename_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    m_filename_label->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_filename_label->set_preferred_size(0, 14);

    m_cursor_label = label_wrapper->add<GUI::Label>("(Cursor)");
    m_cursor_label->set_text_alignment(Gfx::TextAlignment::CenterRight);
    m_cursor_label->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_cursor_label->set_preferred_size(0, 14);

    m_editor = add<Editor>();
    m_editor->set_ruler_visible(true);
    m_editor->set_line_wrapping_enabled(true);
    m_editor->set_automatic_indentation_enabled(true);

    m_editor->on_cursor_change = [this] {
        m_cursor_label->set_text(String::format("Line: %d, Column: %d", m_editor->cursor().line() + 1, m_editor->cursor().column()));
    };

    m_editor->on_focus = [this] {
        g_current_editor_wrapper = this;
    };
}

EditorWrapper::~EditorWrapper()
{
}

void EditorWrapper::set_editor_has_focus(Badge<Editor>, bool focus)
{
    m_filename_label->set_font(focus ? Gfx::Font::default_bold_font() : Gfx::Font::default_font());
}
