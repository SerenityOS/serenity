/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EditorWrapper.h"
#include "Editor.h"
#include "HackStudio.h"
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
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

    m_editor = add<Editor>();
    m_editor->set_ruler_visible(true);
    m_editor->set_automatic_indentation_enabled(true);

    m_editor->on_focus = [this] {
        set_current_editor_wrapper(this);
    };

    m_editor->on_open = [](String path) {
        open_file(path);
    };

    m_editor->on_change = [this] {
        bool was_dirty = m_document_dirty;
        m_document_dirty = true;
        if (!was_dirty)
            update_title();
    };
}

EditorWrapper::~EditorWrapper()
{
}

void EditorWrapper::set_editor_has_focus(Badge<Editor>, bool focus)
{
    auto& font = Gfx::FontDatabase::default_font();
    m_filename_label->set_font(focus ? font.bold_variant() : font);
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

void EditorWrapper::set_filename(const String& filename)
{
    m_filename = filename;
    update_title();
    update_diff();
}

void EditorWrapper::save()
{
    editor().write_to_file(filename());
    m_document_dirty = false;
    update_title();
    update_diff();
    editor().update();
}

void EditorWrapper::update_diff()
{
    if (m_git_repo)
        m_hunks = Diff::parse_hunks(m_git_repo->unstaged_diff(LexicalPath(filename())).value());
}

void EditorWrapper::set_project_root(LexicalPath const& project_root)
{
    m_project_root = project_root;
    auto result = GitRepo::try_to_create(*m_project_root);
    switch (result.type) {
    case GitRepo::CreateResult::Type::Success:
        m_git_repo = result.repo;
        break;
    case GitRepo::CreateResult::Type::GitProgramNotFound:
        break;
    case GitRepo::CreateResult::Type::NoGitRepo:
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void EditorWrapper::update_title()
{
    StringBuilder title;
    title.append(m_filename);

    if (m_document_dirty)
        title.append(" (*)");
    m_filename_label->set_text(title.to_string());
}

}
