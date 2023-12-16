/*
 * Copyright (c) 2022, Dylan Katz <dykatz@uw.edu>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

void ScriptEditor::new_script_with_temp_name(ByteString name)
{
    set_name(name);
}

ErrorOr<void> ScriptEditor::open_script_from_file(LexicalPath const& file_path)
{
    auto file = TRY(Core::File::open(file_path.string(), Core::File::OpenMode::Read));
    auto buffer = TRY(file->read_until_eof());

    set_text({ buffer.bytes() });
    m_path = file_path.string();
    set_name(file_path.title());
    return {};
}

static ErrorOr<void> save_text_to_file(StringView filename, ByteString text)
{
    auto file = TRY(Core::File::open(filename, Core::File::OpenMode::Write));

    if (!text.is_empty())
        TRY(file->write_until_depleted(text.bytes()));

    return {};
}

ErrorOr<bool> ScriptEditor::save()
{
    if (m_path.is_empty())
        return save_as();

    TRY(save_text_to_file(m_path, text()));
    document().set_unmodified();
    return true;
}

ErrorOr<bool> ScriptEditor::save_as()
{
    auto maybe_save_path = GUI::FilePicker::get_save_filepath(window(), name(), "sql");
    if (!maybe_save_path.has_value())
        return false;

    auto save_path = maybe_save_path.release_value();
    TRY(save_text_to_file(save_path, text()));
    m_path = save_path;

    auto lexical_path = LexicalPath(save_path);
    set_name(lexical_path.title());

    auto parent = static_cast<GUI::TabWidget*>(parent_widget());
    if (parent)
        parent->set_tab_title(*this, String::from_utf8(lexical_path.title()).release_value_but_fixme_should_propagate_errors());

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
