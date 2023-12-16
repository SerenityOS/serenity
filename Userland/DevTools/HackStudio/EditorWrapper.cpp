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
#include <LibGUI/FilePicker.h>
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

    add_child(*m_editor);
    m_editor->set_ruler_visible(true);
    m_editor->set_automatic_indentation_enabled(true);

    m_editor->on_focusin = [this] {
        set_current_editor_wrapper(this);
    };

    m_editor->on_open = [](ByteString const& path) {
        open_file(path);
    };

    m_editor->on_modified_change = [this](bool) {
        update_title();
        update_editor_window_title();
    };
}

LanguageClient& EditorWrapper::language_client() { return m_editor->language_client(); }

void EditorWrapper::set_mode_displayable()
{
    editor().set_mode(GUI::TextEditor::Editable);
    editor().set_background_role(Gfx::ColorRole::Base);
    auto palette = GUI::Application::the()->palette();
    editor().set_palette(palette);
}

void EditorWrapper::set_mode_non_displayable()
{
    editor().set_mode(GUI::TextEditor::ReadOnly);
    editor().set_background_role(Gfx::ColorRole::InactiveSelection);
    auto palette = editor().palette();
    palette.set_color(Gfx::ColorRole::BaseText, Color::from_rgb(0xffffff));
    editor().set_palette(palette);
    editor().document().set_text("The contents of this file could not be displayed. Is it a binary file?"sv);
}

void EditorWrapper::set_filename(ByteString const& filename)
{
    m_filename = filename;
    update_title();
    update_diff();
}

bool EditorWrapper::save()
{
    if (filename().is_empty()) {
        auto file_picker_action = GUI::CommonActions::make_save_as_action([&](auto&) {
            Optional<ByteString> save_path = GUI::FilePicker::get_save_filepath(window(), "file"sv, "txt"sv, project_root().value());
            if (save_path.has_value())
                set_filename(save_path.value());
        });
        file_picker_action->activate();

        if (filename().is_empty())
            return false;
    }
    editor().write_to_file(filename()).release_value_but_fixme_should_propagate_errors();
    update_diff();
    editor().update();

    return true;
}

void EditorWrapper::update_diff()
{
    if (m_git_repo) {
        m_hunks = Diff::parse_hunks(m_git_repo->unstaged_diff(filename()).value()).release_value_but_fixme_should_propagate_errors();
        editor().update_git_diff_indicators().release_value_but_fixme_should_propagate_errors();
    }
}

void EditorWrapper::set_project_root(ByteString const& project_root)
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
    if (m_filename.is_empty())
        title.append(untitled_label);
    else
        title.append(m_filename);

    if (editor().document().is_modified())
        title.append(" (*)"sv);
    m_filename_title = title.to_byte_string();
}

void EditorWrapper::set_debug_mode(bool enabled)
{
    m_editor->set_debug_mode(enabled);
}

}
