/*
 * Copyright (c) 2021, Conor Byrne <cbyrneee@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Label.h>

namespace HackStudio {

using CommitCallback = Function<void(String const& message)>;

class GitCommitDialog final : public GUI::Dialog {
    C_OBJECT(GitCommitDialog);

public:
    GitCommitDialog(GUI::Window* parent);

    CommitCallback on_commit;

private:
    RefPtr<GUI::Button> m_commit_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::TextEditor> m_message_editor;
    RefPtr<GUI::Label> m_line_and_column_label;

    void commit();
};
}
