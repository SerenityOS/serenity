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
#include "HackStudio.h"
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>

namespace HackStudio {

EditorWrapper::EditorWrapper()
{
    set_layout<GUI::VerticalBoxLayout>();

    auto& label_wrapper = add<GUI::Widget>();
    label_wrapper.set_fixed_height(14);
    label_wrapper.set_fill_with_background_color(true);
    label_wrapper.set_layout<GUI::HorizontalBoxLayout>();
    label_wrapper.layout()->set_margins({ 2, 0, 2, 0 });

    m_filename_label = label_wrapper.add<GUI::Label>("(Untitled)");
    m_filename_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    m_filename_label->set_fixed_height(14);

    m_cursor_label = label_wrapper.add<GUI::Label>("(Cursor)");
    m_cursor_label->set_text_alignment(Gfx::TextAlignment::CenterRight);
    m_cursor_label->set_fixed_height(14);

    m_editor = add<Editor>();
    m_editor->set_ruler_visible(true);
    m_editor->set_automatic_indentation_enabled(true);

    m_editor->on_cursor_change = [this] {
        m_cursor_label->set_text(String::formatted("Line: {}, Column: {}", m_editor->cursor().line() + 1, m_editor->cursor().column()));
    };

    m_editor->on_focus = [this] {
        set_current_editor_wrapper(this);
    };

    m_editor->on_open = [](String path) {
        open_file(path);
    };
}

EditorWrapper::~EditorWrapper()
{
}

void EditorWrapper::set_editor_has_focus(Badge<Editor>, bool focus)
{
    m_filename_label->set_font(focus ? Gfx::FontDatabase::default_bold_font() : Gfx::FontDatabase::default_font());
}

LanguageClient& EditorWrapper::language_client() { return m_editor->language_client(); }

void EditorWrapper::set_mode_displayable()
{
    editor().set_mode(GUI::TextEditor::Editable);
    editor().set_background_role(Gfx::ColorRole::Base);
    editor().set_palette(GUI::Application::the()->palette());
}

void EditorWrapper::set_mode_non_displayable()
{
    editor().set_mode(GUI::TextEditor::ReadOnly);
    editor().set_background_role(Gfx::ColorRole::InactiveSelection);
    auto palette = editor().palette();
    palette.set_color(Gfx::ColorRole::BaseText, Color::from_rgb(0xffffff));
    editor().set_palette(palette);
    editor().document().set_text("The contents of this file could not be displayed. Is it a binary file?");
}

}
