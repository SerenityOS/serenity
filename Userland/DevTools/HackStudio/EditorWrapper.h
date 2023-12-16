/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Debugger/BreakpointCallback.h"
#include "Git/GitRepo.h"
#include "LanguageClient.h"
#include <AK/Function.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibDiff/Hunks.h>
#include <LibGUI/Widget.h>
#include <string.h>

namespace HackStudio {

class Editor;

class EditorWrapper : public GUI::Widget {
    C_OBJECT(EditorWrapper)

public:
    virtual ~EditorWrapper() override = default;

    Editor& editor() { return *m_editor; }
    Editor const& editor() const { return *m_editor; }

    bool save();

    LanguageClient& language_client();

    void set_mode_displayable();
    void set_mode_non_displayable();
    void set_debug_mode(bool);
    void set_filename(ByteString const&);
    ByteString const& filename() const { return m_filename; }
    ByteString const& filename_title() const { return m_filename_title; }

    Optional<ByteString> const& project_root() const { return m_project_root; }
    void set_project_root(ByteString const& project_root);

    GitRepo const* git_repo() const { return m_git_repo; }

    void update_diff();
    Vector<Diff::Hunk> const& hunks() const { return m_hunks; }

    Function<void()> on_change;
    Function<void(EditorWrapper&)> on_tab_close_request;

private:
    static constexpr auto untitled_label = "(Untitled)"sv;

    EditorWrapper();

    void update_title();

    ByteString m_filename;
    ByteString m_filename_title;
    RefPtr<Editor> m_editor;

    Optional<ByteString> m_project_root;
    RefPtr<GitRepo> m_git_repo;
    Vector<Diff::Hunk> m_hunks;
};

}
