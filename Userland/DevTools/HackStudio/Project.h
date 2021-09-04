/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ProjectFile.h"
#include <AK/LexicalPath.h>
#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <LibGUI/FileSystemModel.h>

namespace HackStudio {

class Project {
    AK_MAKE_NONCOPYABLE(Project);
    AK_MAKE_NONMOVABLE(Project);

public:
    static OwnPtr<Project> open_with_root_path(String const& root_path);

    GUI::FileSystemModel& model() { return *m_model; }
    const GUI::FileSystemModel& model() const { return *m_model; }
    String name() const { return LexicalPath::basename(m_root_path); }
    String root_path() const { return m_root_path; }

    NonnullRefPtr<ProjectFile> get_file(String const& path) const;

    void for_each_text_file(Function<void(ProjectFile const&)>) const;

private:
    explicit Project(String const& root_path);

    String to_absolute_path(String const&) const;

    RefPtr<GUI::FileSystemModel> m_model;
    mutable NonnullRefPtrVector<ProjectFile> m_files;

    String m_root_path;
};

}
