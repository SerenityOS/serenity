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
    static OwnPtr<Project> open_with_root_path(const String& root_path);

    GUI::FileSystemModel& model() { return *m_model; }
    const GUI::FileSystemModel& model() const { return *m_model; }
    String name() const { return LexicalPath::basename(m_root_path); }
    String root_path() const { return m_root_path; }

    NonnullRefPtr<ProjectFile> create_file(const String& path) const;

    void for_each_text_file(Function<void(const ProjectFile&)>) const;
    String to_absolute_path(String const&) const;
    bool project_is_serenity() const;

private:
    explicit Project(const String& root_path);

    RefPtr<GUI::FileSystemModel> m_model;

    String m_root_path;
};

}
