/*
 * Copyright (c) 2022, Dylan Katz <dykatz@uw.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/LexicalPath.h>
#include <LibGUI/TextEditor.h>

namespace SQLStudio {

class ScriptEditor : public GUI::TextEditor {
    C_OBJECT(ScriptEditor)

public:
    virtual ~ScriptEditor() = default;

    void new_script_with_temp_name(ByteString);
    ErrorOr<void> open_script_from_file(LexicalPath const&);

    ErrorOr<bool> save();
    ErrorOr<bool> save_as();
    ErrorOr<bool> attempt_to_close();

    ByteString const& path() const { return m_path; }

private:
    ScriptEditor();

    ByteString m_path;
};

}
