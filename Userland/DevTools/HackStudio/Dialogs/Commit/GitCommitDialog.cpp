/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GitCommitDialog.h"
#include <DevTools/HackStudio/Dialogs/Commit/GitCommitDialogGML.h>

namespace HackStudio {

GitCommitDialog::GitCommitDialog(GUI::Window* parent)
    : Dialog(parent)
{
    resize(400, 260);
    center_within(*parent);
    set_resizable(false);
    set_modal(true);
    set_title("Commit");
    set_icon(parent->icon());

    auto& widget = set_main_widget<GUI::Widget>();
    widget.load_from_gml(git_commit_dialog_gml);

    m_message_editor = widget.find_descendant_of_type_named<GUI::TextEditor>("message_editor");
    m_cancel_button = widget.find_descendant_of_type_named<GUI::Button>("cancel_button");
    m_commit_button = widget.find_descendant_of_type_named<GUI::Button>("commit_button");
    m_commit_button->set_enabled(!m_message_editor->text().is_empty() && on_commit);

    m_message_editor->on_change = [this]() {
        m_commit_button->set_enabled(!m_message_editor->text().is_empty() && on_commit);
    };

    m_commit_button->on_click = [this](auto) {
        on_commit(m_message_editor->text());
        done(ExecResult::ExecOK);
    };

    m_cancel_button->on_click = [this](auto) {
        done(ExecResult::ExecCancel);
    };
}
}
