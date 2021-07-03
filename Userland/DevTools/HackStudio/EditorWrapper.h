/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
    virtual ~EditorWrapper() override;

    Editor& editor() { return *m_editor; }
    const Editor& editor() const { return *m_editor; }

    void save();

    GUI::Label& filename_label() { return *m_filename_label; }
    const GUI::Label& filename_label() const { return *m_filename_label; }

    void set_editor_has_focus(Badge<Editor>, bool);
    LanguageClient& language_client();

    void set_mode_displayable();
    void set_mode_non_displayable();
    void set_filename(const String&);
    const String& filename() const { return m_filename; }
    bool document_dirty() const { return m_document_dirty; }

    Optional<LexicalPath> const& project_root() const { return m_project_root; }
    void set_project_root(LexicalPath const& project_root);

    GitRepo const* git_repo() const { return m_git_repo; }

    void update_diff();
    Vector<Diff::Hunk> const& hunks() const { return m_hunks; }

private:
    EditorWrapper();

    void update_title();

    String m_filename;
    RefPtr<GUI::Label> m_filename_label;
    RefPtr<Editor> m_editor;
    bool m_document_dirty { false };

    Optional<LexicalPath> m_project_root;
    RefPtr<GitRepo> m_git_repo;
    Vector<Diff::Hunk> m_hunks;
};

}
