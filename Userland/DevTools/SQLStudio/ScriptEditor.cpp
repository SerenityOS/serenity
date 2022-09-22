/*
 * Copyright (c) 2022, Dylan Katz <dykatz@uw.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Stream.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/TabWidget.h>
#include <LibSQL/AST/SyntaxHighlighter.h>

#include "ScriptEditor.h"

namespace SQLStudio {

ScriptEditor::ScriptEditor()
{
    set_syntax_highlighter(make<SQL::AST::SyntaxHighlighter>());
    set_ruler_visible(true);
}

void ScriptEditor::new_script_with_temp_name(String name)
{
    set_name(name);
}

ErrorOr<void> ScriptEditor::open_script_from_file(LexicalPath const& file_path)
{
    auto file = TRY(Core::Stream::File::open(file_path.string(), Core::Stream::OpenMode::Read));
    auto buffer = TRY(file->read_all());

    set_text({ buffer.bytes() });
    m_path = file_path.string();
    set_name(file_path.title());
    return {};
}

ErrorOr<bool> ScriptEditor::save()
{
    if (m_path.is_empty())
        return save_as();

    auto file = TRY(Core::Stream::File::open(m_path, Core::Stream::OpenMode::Write));
    auto editor_text = text();
    if (editor_text.length() && !file->write_or_error(editor_text.bytes()))
        return Error::from_string_literal("Failed to write to file");

    document().set_unmodified();
    return true;
}

ErrorOr<bool> ScriptEditor::save_as()
{
    auto maybe_save_path = GUI::FilePicker::get_save_filepath(window(), name(), "sql");
    if (!maybe_save_path.has_value())
        return false;
    auto save_path = maybe_save_path.release_value();

    auto file = TRY(Core::Stream::File::open(save_path, Core::Stream::OpenMode::Write));
    auto editor_text = text();
    if (editor_text.length() && !file->write_or_error(editor_text.bytes()))
        return Error::from_string_literal("Failed to write to file");

    m_path = save_path;

    auto lexical_path = LexicalPath(save_path);
    set_name(lexical_path.title());

    auto parent = static_cast<GUI::TabWidget*>(parent_widget());
    if (parent)
        parent->set_tab_title(*this, lexical_path.title());

    document().set_unmodified();
    return true;
}

ErrorOr<bool> ScriptEditor::attempt_to_close()
{
    if (!document().is_modified())
        return true;

    auto result = GUI::MessageBox::ask_about_unsaved_changes(window(), m_path.is_empty() ? name() : m_path, document().undo_stack().last_unmodified_timestamp());
    switch (result) {
    case GUI::Dialog::ExecResult::Yes:
        return save();
    case GUI::Dialog::ExecResult::No:
        return true;
    default:
        return false;
    }
}

}
