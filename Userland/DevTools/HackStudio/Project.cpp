/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Project.h"
#include "HackStudio.h"
#include <LibFileSystem/FileSystem.h>

namespace HackStudio {

Project::Project(ByteString const& root_path)
    : m_root_path(root_path)
{
    m_model = GUI::FileSystemModel::create(root_path, GUI::FileSystemModel::Mode::FilesAndDirectories);
}

OwnPtr<Project> Project::open_with_root_path(ByteString const& root_path)
{
    VERIFY(LexicalPath(root_path).is_absolute());
    if (!FileSystem::is_directory(root_path))
        return {};
    return adopt_own(*new Project(root_path));
}

template<typename Callback>
static void traverse_model(const GUI::FileSystemModel& model, const GUI::ModelIndex& index, Callback callback)
{
    if (index.is_valid())
        callback(index);
    auto row_count = model.row_count(index);
    if (!row_count)
        return;
    for (int row = 0; row < row_count; ++row) {
        auto child_index = model.index(row, GUI::FileSystemModel::Column::Name, index);
        traverse_model(model, child_index, callback);
    }
}

void Project::for_each_text_file(Function<void(ProjectFile const&)> callback) const
{
    traverse_model(model(), {}, [&](auto& index) {
        auto file = create_file(model().full_path(index));
        callback(*file);
    });
}

NonnullRefPtr<ProjectFile> Project::create_file(ByteString const& path) const
{
    auto full_path = to_absolute_path(path);
    return ProjectFile::construct_with_name(full_path);
}

ByteString Project::to_absolute_path(ByteString const& path) const
{
    if (LexicalPath { path }.is_absolute()) {
        return path;
    }
    return LexicalPath { ByteString::formatted("{}/{}", m_root_path, path) }.string();
}

bool Project::project_is_serenity() const
{
    // FIXME: Improve this heuristic
    // Running "Meta/serenity.sh copy-src" installs the serenity repository at this path in the home directory
    return m_root_path.ends_with("Source/serenity"sv);
}

NonnullOwnPtr<ProjectConfig> Project::config() const
{
    auto config_or_error = ProjectConfig::try_load_project_config(LexicalPath::absolute_path(m_root_path, config_file_path));
    if (config_or_error.is_error())
        return ProjectConfig::create_empty();

    return config_or_error.release_value();
}

}
