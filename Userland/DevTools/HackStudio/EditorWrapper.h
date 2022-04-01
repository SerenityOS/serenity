/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Debugger/BreakpointCallback.h"
#include "FindWidget.h"
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

    void save();

    LanguageClient& language_client();

    void set_mode_displayable();
    void set_mode_non_displayable();
    void set_debug_mode(bool);
    void set_filename(String const&);
    String const& filename() const { return m_filename; }
    String const& filename_title() const { return m_filename_title; }

    Optional<String> const& project_root() const { return m_project_root; }
    void set_project_root(String const& project_root);

    GitRepo const* git_repo() const { return m_git_repo; }

    void update_diff();
    Vector<Diff::Hunk> const& hunks() const { return m_hunks; }

    Function<void()> on_change;
    Function<void(EditorWrapper&)> on_tab_close_request;

    void search_action();
    FindWidget const& find_widget() const { return *m_find_widget; }

private:
    static constexpr auto untitled_label = "(Untitled)";

    EditorWrapper();

    void update_title();

    String m_filename;
    String m_filename_title;
    RefPtr<Editor> m_editor;
    RefPtr<FindWidget> m_find_widget;

    Optional<String> m_project_root;
    RefPtr<GitRepo> m_git_repo;
    Vector<Diff::Hunk> m_hunks;
};

}
