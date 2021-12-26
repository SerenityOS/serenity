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
#include <AK/FileSystemPath.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>

struct Project::ProjectTreeNode : public RefCounted<ProjectTreeNode> {
    enum class Type {
        Invalid,
        Project,
        Directory,
        File,
    };

    ProjectTreeNode& find_or_create_subdirectory(const String& name)
    {
        for (auto& child : children) {
            if (child->type == Type::Directory && child->name == name)
                return *child;
        }
        auto new_child = adopt(*new ProjectTreeNode);
        new_child->type = Type::Directory;
        new_child->name = name;
        new_child->parent = this;
        auto* ptr = new_child.ptr();
        children.append(move(new_child));
        return *ptr;
    }

    void sort()
    {
        if (type == Type::File)
            return;
        quick_sort(children.begin(), children.end(), [](auto& a, auto& b) {
            return a->name < b->name;
        });
        for (auto& child : children)
            child->sort();
    }

    Type type { Type::Invalid };
    String name;
    String path;
    Vector<NonnullRefPtr<ProjectTreeNode>> children;
    ProjectTreeNode* parent { nullptr };
};

class ProjectModel final : public GUI::Model {
public:
    explicit ProjectModel(Project& project)
        : m_project(project)
    {
    }

    virtual int row_count(const GUI::ModelIndex& index) const override
    {
        if (!index.is_valid())
            return 1;
        auto* node = static_cast<Project::ProjectTreeNode*>(index.internal_data());
        return node->children.size();
    }

    virtual int column_count(const GUI::ModelIndex&) const override
    {
        return 1;
    }

    virtual GUI::Variant data(const GUI::ModelIndex& index, Role role = Role::Display) const override
    {
        auto* node = static_cast<Project::ProjectTreeNode*>(index.internal_data());
        if (role == Role::Display) {
            return node->name;
        }
        if (role == Role::Custom) {
            return node->path;
        }
        if (role == Role::Icon) {
            if (node->type == Project::ProjectTreeNode::Type::Project)
                return m_project.m_project_icon;
            if (node->type == Project::ProjectTreeNode::Type::Directory)
                return m_project.m_directory_icon;
            if (node->name.ends_with(".cpp"))
                return m_project.m_cplusplus_icon;
            if (node->name.ends_with(".h"))
                return m_project.m_header_icon;
            return m_project.m_file_icon;
        }
        if (role == Role::Font) {
            extern String g_currently_open_file;
            if (node->name == g_currently_open_file)
                return Gfx::Font::default_bold_font();
            return {};
        }
        return {};
    }

    virtual GUI::ModelIndex index(int row, int column = 0, const GUI::ModelIndex& parent = GUI::ModelIndex()) const override
    {
        if (!parent.is_valid()) {
            return create_index(row, column, &m_project.root_node());
        }
        auto& node = *static_cast<Project::ProjectTreeNode*>(parent.internal_data());
        return create_index(row, column, node.children.at(row).ptr());
    }

    GUI::ModelIndex parent_index(const GUI::ModelIndex& index) const override
    {
        if (!index.is_valid())
            return {};
        auto& node = *static_cast<Project::ProjectTreeNode*>(index.internal_data());
        if (!node.parent)
            return {};

        if (!node.parent->parent) {
            return create_index(0, 0, &m_project.root_node());
            ASSERT_NOT_REACHED();
            return {};
        }

        for (int row = 0; row < node.parent->parent->children.size(); ++row) {
            if (node.parent->parent->children[row].ptr() == node.parent)
                return create_index(row, 0, node.parent);
        }

        ASSERT_NOT_REACHED();
        return {};
    }

    virtual void update() override
    {
        did_update();
    }

private:
    Project& m_project;
};

Project::Project(const String& path, Vector<String>&& filenames)
    : m_path(path)
{
    m_name = FileSystemPath(m_path).basename();

    m_file_icon = GIcon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-unknown.png"));
    m_cplusplus_icon = GIcon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-cplusplus.png"));
    m_header_icon = GIcon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-header.png"));
    m_directory_icon = GIcon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-folder.png"));
    m_project_icon = GIcon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-hack-studio.png"));

    for (auto& filename : filenames) {
        m_files.append(ProjectFile::construct_with_name(filename));
    }

    m_model = adopt(*new ProjectModel(*this));

    rebuild_tree();
}

Project::~Project()
{
}

OwnPtr<Project> Project::load_from_file(const String& path)
{
    auto file = Core::File::construct(path);
    if (!file->open(Core::File::ReadOnly))
        return nullptr;

    Vector<String> files;
    for (;;) {
        auto line = file->read_line(1024);
        if (line.is_null())
            break;
        files.append(String::copy(line, Chomp));
    }

    return OwnPtr(new Project(path, move(files)));
}

bool Project::add_file(const String& filename)
{
    m_files.append(ProjectFile::construct_with_name(filename));
    rebuild_tree();
    m_model->update();
    return save();
}

bool Project::remove_file(const String& filename)
{
    if (!get_file(filename))
        return false;
    m_files.remove_first_matching([filename](auto& file) { return file->name() == filename; });
    rebuild_tree();
    m_model->update();
    return save();
}

bool Project::save()
{
    auto project_file = Core::File::construct(m_path);
    if (!project_file->open(Core::File::WriteOnly))
        return false;

    for (auto& file : m_files) {
        // FIXME: Check for error here. IODevice::printf() needs some work on error reporting.
        project_file->printf("%s\n", file.name().characters());
    }

    if (!project_file->close())
        return false;

    return true;
}

ProjectFile* Project::get_file(const String& filename)
{
    for (auto& file : m_files) {
        if (FileSystemPath(file.name()).string() == FileSystemPath(filename).string())
            return &file;
    }
    return nullptr;
}

void Project::rebuild_tree()
{
    auto root = adopt(*new ProjectTreeNode);
    root->name = m_name;
    root->type = ProjectTreeNode::Type::Project;

    for (auto& file : m_files) {
        FileSystemPath path(file.name());
        ProjectTreeNode* current = root.ptr();
        StringBuilder partial_path;

        for (int i = 0; i < path.parts().size(); ++i) {
            auto& part = path.parts().at(i);
            if (part == ".")
                continue;
            if (i != path.parts().size() - 1) {
                current = &current->find_or_create_subdirectory(part);
                continue;
            }
            struct stat st;
            if (lstat(path.string().characters(), &st) < 0)
                continue;

            if (S_ISDIR(st.st_mode)) {
                current = &current->find_or_create_subdirectory(part);
                continue;
            }
            auto file_node = adopt(*new ProjectTreeNode);
            file_node->name = part;
            file_node->path = path.string();
            file_node->type = Project::ProjectTreeNode::Type::File;
            file_node->parent = current;
            current->children.append(move(file_node));
            break;
        }
    }

    root->sort();

#if 0
    Function<void(ProjectTreeNode&, int indent)> dump_tree = [&](ProjectTreeNode& node, int indent) {
        for (int i = 0; i < indent; ++i)
            printf(" ");
        if (node.name.is_null())
            printf("(null)\n");
        else
            printf("%s\n", node.name.characters());
        for (auto& child : node.children) {
            dump_tree(*child, indent + 2);
        }
    };

    dump_tree(*root, 0);
#endif

    m_root_node = move(root);
    m_model->update();
}
