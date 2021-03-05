/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Project.h"
#include "HackStudio.h"
#include <LibCore/File.h>

namespace HackStudio {

Project::Project(const String& root_path)
    : m_root_path(root_path)
{
    m_model = GUI::FileSystemModel::create(root_path, GUI::FileSystemModel::Mode::FilesAndDirectories);
}

Project::~Project()
{
}

OwnPtr<Project> Project::open_with_root_path(const String& root_path)
{
    if (!Core::File::is_directory(root_path))
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

void Project::for_each_text_file(Function<void(const ProjectFile&)> callback) const
{
    traverse_model(model(), {}, [&](auto& index) {
        auto file = get_file(model().full_path(index));
        if (file)
            callback(*file);
    });
}

NonnullRefPtr<ProjectFile> Project::get_file(const String& path) const
{
    auto full_path = to_absolute_path(path);
    for (auto& file : m_files) {
        if (file.name() == full_path)
            return file;
    }
    auto file = ProjectFile::construct_with_name(full_path);
    m_files.append(file);
    return file;
}

String Project::to_absolute_path(const String& path) const
{
    if (LexicalPath { path }.is_absolute()) {
        return path;
    }
    return LexicalPath { String::formatted("{}/{}", m_root_path, path) }.string();
}

}
