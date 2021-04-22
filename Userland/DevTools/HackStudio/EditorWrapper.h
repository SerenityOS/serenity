/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Debugger/BreakpointCallback.h"
#include "LanguageClient.h"
#include <AK/Function.h>
#include <AK/Vector.h>
#include <LibGUI/Widget.h>
#include <string.h>

namespace HackStudio {

class Editor;

class EditorWrapper : public GUI::Widget {
    C_OBJECT(EditorWrapper)

public:
    virtual ~EditorWrapper() override;

    Editor& editor() { return *m_editor; }
    const Editor& editor() const { return *m_editor; }

    GUI::Label& filename_label() { return *m_filename_label; }
    const GUI::Label& filename_label() const { return *m_filename_label; }

    void set_editor_has_focus(Badge<Editor>, bool);
    LanguageClient& language_client();

    void set_mode_displayable();
    void set_mode_non_displayable();

private:
    EditorWrapper();

    RefPtr<GUI::Label> m_filename_label;
    RefPtr<GUI::Label> m_cursor_label;
    RefPtr<Editor> m_editor;
};

}
