/*
 * Copyright (c) 2022-2023, Jakub Berkop <jakub.berkop@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <AK/Time.h>
#include <LibGUI/Model.h>

namespace Profiler {

class Profile;

class FileEventNode : public RefCounted<FileEventNode> {
public:
    static NonnullRefPtr<FileEventNode> create(ByteString const& path, FileEventNode* parent = nullptr)
    {
        return adopt_ref(*new FileEventNode(path, parent));
    }

    FileEventNode& find_or_create_node(ByteString const&);

    Vector<NonnullRefPtr<FileEventNode>>& children() { return m_children; }
    Vector<NonnullRefPtr<FileEventNode>> const& children() const { return m_children; }

    FileEventNode* parent() { return m_parent; }

    FileEventNode& create_recursively(ByteString);

    void for_each_parent_node(Function<void(FileEventNode&)> callback);

    ByteString const& path() const { return m_path; }

    u64 total_count() const;
    Duration total_duration() const;

    struct FileEventType {
        u64 count = 0;
        Duration duration = {};
    };

    FileEventType& open() { return m_open; }
    FileEventType& close() { return m_close; }
    FileEventType& readv() { return m_readv; }
    FileEventType& read() { return m_read; }
    FileEventType& pread() { return m_pread; }

private:
    FileEventNode(ByteString const& path, FileEventNode* parent = nullptr)
        : m_path(path)
        , m_parent(parent) {};

    ByteString m_path;

    FileEventType m_open;
    FileEventType m_close;
    FileEventType m_readv;
    FileEventType m_read;
    FileEventType m_pread;

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
        TotalCount,
        TotalDuration,
        OpenCount,
        OpenDuration,
        CloseCount,
        CloseDuration,
        ReadvCount,
        ReadvDuration,
        ReadCount,
        ReadDuration,
        PreadCount,
        PreadDuration,
        __Count
    };

    virtual ~FileEventModel() override;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual ErrorOr<String> column_name(int) const override;
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
