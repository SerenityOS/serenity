/*
 * Copyright (c) 2021, Conor Byrne <conor@cbyrne.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GitCommitDialog.h"
#include <DevTools/HackStudio/Dialogs/Git/GitCommitDialogGML.h>

namespace HackStudio {

GitCommitDialog::GitCommitDialog(GUI::Window* parent)
    : Dialog(parent)
{
    resize(400, 260);
    center_within(*parent);
    set_title("Commit");
    set_icon(parent->icon());

    auto widget = set_main_widget<GUI::Widget>();
    widget->load_from_gml(git_commit_dialog_gml).release_value_but_fixme_should_propagate_errors();

    m_message_editor = widget->find_descendant_of_type_named<GUI::TextEditor>("message_editor");
    m_cancel_button = widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    m_commit_button = widget->find_descendant_of_type_named<GUI::Button>("commit_button");
    m_line_and_col_label = widget->find_descendant_of_type_named<GUI::Label>("line_and_col_label");

    m_message_editor->on_change = [this]() {
        m_commit_button->set_enabled(!m_message_editor->text().is_empty() && on_commit);
    };
    m_message_editor->on_cursor_change = [this]() {
        auto line = m_message_editor->cursor().line() + 1;
        auto col = m_message_editor->cursor().column();

        m_line_and_col_label->set_text(String::formatted("Line: {}, Col: {}", line, col).release_value_but_fixme_should_propagate_errors());
    };

    m_commit_button->set_enabled(!m_message_editor->text().is_empty() && on_commit);
    m_commit_button->on_click = [this](auto) {
        on_commit(m_message_editor->text());
        done(ExecResult::OK);
    };

    m_cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };
}

}
