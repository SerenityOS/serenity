/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ProjectConfig.h"
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
    static OwnPtr<Project> open_with_root_path(ByteString const& root_path);

    GUI::FileSystemModel& model() { return *m_model; }
    const GUI::FileSystemModel& model() const { return *m_model; }
    ByteString name() const { return LexicalPath::basename(m_root_path); }
    ByteString root_path() const { return m_root_path; }

    NonnullRefPtr<ProjectFile> create_file(ByteString const& path) const;

    void for_each_text_file(Function<void(ProjectFile const&)>) const;
    ByteString to_absolute_path(ByteString const&) const;
    bool project_is_serenity() const;

    static constexpr auto config_file_path = ".hackstudio/config.json"sv;
    NonnullOwnPtr<ProjectConfig> config() const;

private:
    explicit Project(ByteString const& root_path);

    RefPtr<GUI::FileSystemModel> m_model;

    ByteString m_root_path;
};

}
