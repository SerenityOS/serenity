/*
 * Copyright (c) 2021, Conor Byrne <conor@cbyrne.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>

namespace HackStudio {

using OnCommitCallback = Function<void(ByteString const& message)>;

class GitCommitDialog final : public GUI::Dialog {
    C_OBJECT(GitCommitDialog);

public:
    OnCommitCallback on_commit;

private:
    GitCommitDialog(GUI::Window* parent);

    RefPtr<GUI::Button> m_commit_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::TextEditor> m_message_editor;
    RefPtr<GUI::Label> m_line_and_col_label;
};

}
