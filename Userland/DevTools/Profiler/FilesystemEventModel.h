/*
 * Copyright (c) 2022, Jakub Berkop <jakub.berkop@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <LibGUI/Model.h>

namespace Profiler {

class Profile;

class FileEventNode : public RefCounted<FileEventNode> {
public:
    static NonnullRefPtr<FileEventNode> create(String const& path, FileEventNode* parent = nullptr)
    {
        return adopt_ref(*new FileEventNode(path, parent));
    }

    FileEventNode& find_or_create_node(String const&);

    Vector<NonnullRefPtr<FileEventNode>>& children() { return m_children; }
    Vector<NonnullRefPtr<FileEventNode>> const& children() const { return m_children; }

    FileEventNode* parent() { return m_parent; };

    FileEventNode& create_recursively(String);

    void for_each_parent_node(Function<void(FileEventNode&)> callback);

    String const& path() const { return m_path; }

    void increment_count() { m_count++; }
    u64 count() const { return m_count; }

    void add_to_duration(u64 duration) { duration += duration; }
    u64 duration() const { return m_duration; }

private:
    FileEventNode(String const& path, FileEventNode* parent = nullptr)
        : m_path(path)
        , m_count(0)
        , m_duration(0)
        , m_parent(parent) {};

    String m_path;
    u64 m_count;
    u64 m_duration;

    Vector<NonnullRefPtr<FileEventNode>> m_children;
    FileEventNode* m_parent = nullptr;
};

class FileEventModel final : public GUI::Model {
public:
    static NonnullRefPtr<FileEventModel> create(Profile& profile)
    {
        return adopt_ref(*new FileEventModel(profile));
    }

    enum Column {
        Path,
        Count,
        Duration,
        __Count
    };

    virtual ~FileEventModel() override;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual String column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual GUI::ModelIndex index(int row, int column, GUI::ModelIndex const& parent = GUI::ModelIndex()) const override;
    virtual GUI::ModelIndex parent_index(GUI::ModelIndex const&) const override;
    virtual int tree_column() const override { return Column::Path; }
    virtual bool is_column_sortable(int) const override { return false; }
    virtual bool is_searchable() const override { return true; }

private:
    explicit FileEventModel(Profile&);

    Profile& m_profile;

    GUI::Icon m_user_frame_icon;
    GUI::Icon m_kernel_frame_icon;
};

}
