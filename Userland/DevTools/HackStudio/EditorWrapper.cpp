/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EditorWrapper.h"
#include "Editor.h"
#include "HackStudio.h"
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>

namespace HackStudio {

EditorWrapper::EditorWrapper()
{
    set_layout<GUI::VerticalBoxLayout>();
    m_filename_title = untitled_label;

    // FIXME: Propagate errors instead of giving up
    m_editor = MUST(Editor::try_create());
    m_find_widget = add<FindWidget>(*m_editor);

    add_child(*m_editor);
    m_editor->set_ruler_visible(true);
    m_editor->set_automatic_indentation_enabled(true);

    m_editor->on_focus = [this] {
        set_current_editor_wrapper(this);
    };

    m_editor->on_open = [](String const& path) {
        open_file(path);
    };

    m_editor->on_modified_change = [this](bool) {
        update_title();
    };
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

void EditorWrapper::set_filename(String const& filename)
{
    m_filename = filename;
    update_title();
    update_diff();
}

void EditorWrapper::save()
{
    editor().write_to_file(filename());
    update_diff();
    editor().update();
}

void EditorWrapper::update_diff()
{
    if (m_git_repo)
        m_hunks = Diff::parse_hunks(m_git_repo->unstaged_diff(filename()).value());
}

void EditorWrapper::set_project_root(String const& project_root)
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
    if (m_filename.is_null())
        title.append(untitled_label);
    else
        title.append(m_filename);

    if (editor().document().is_modified())
        title.append(" (*)");
    m_filename_title = title.to_string();
}

void EditorWrapper::set_debug_mode(bool enabled)
{
    m_editor->set_debug_mode(enabled);
}

void EditorWrapper::search_action()
{
    if (m_find_widget->visible())
        m_find_widget->hide();
    else
        m_find_widget->show();
}

}
