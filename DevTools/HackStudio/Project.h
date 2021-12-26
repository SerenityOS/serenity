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

#pragma once

#include "ProjectFile.h"
#include <AK/LexicalPath.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Model.h>

namespace HackStudio {

enum class ProjectType {
    Unknown,
    Cpp,
    JavaScript
};

class Project {
    AK_MAKE_NONCOPYABLE(Project);
    AK_MAKE_NONMOVABLE(Project);

public:
    ~Project();

    static OwnPtr<Project> load_from_file(const String& path);

    [[nodiscard]] bool add_file(const String& filename);
    [[nodiscard]] bool remove_file(const String& filename);
    [[nodiscard]] bool save();

    ProjectFile* get_file(const String& filename);

    ProjectType type() const { return m_type; }
    GUI::Model& model() { return *m_model; }
    String default_file() const;
    String name() const { return m_name; }
    String path() const { return m_path; }
    String root_directory() const { return LexicalPath(m_path).dirname(); }

    template<typename Callback>
    void for_each_text_file(Callback callback) const
    {
        for (auto& file : m_files) {
            callback(file);
        }
    }

private:
    friend class ProjectModel;
    struct ProjectTreeNode;
    explicit Project(const String& path, Vector<String>&& files);

    const ProjectTreeNode& root_node() const { return *m_root_node; }
    void rebuild_tree();

    ProjectType m_type { ProjectType::Unknown };
    String m_name;
    String m_path;
    RefPtr<GUI::Model> m_model;
    NonnullRefPtrVector<ProjectFile> m_files;
    RefPtr<ProjectTreeNode> m_root_node;

    GUI::Icon m_directory_icon;
    GUI::Icon m_file_icon;
    GUI::Icon m_cplusplus_icon;
    GUI::Icon m_header_icon;
    GUI::Icon m_project_icon;
    GUI::Icon m_javascript_icon;
    GUI::Icon m_hackstudio_icon;
    GUI::Icon m_form_icon;
};

}
