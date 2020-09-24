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

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/DateTime.h>
#include <LibCore/Notifier.h>
#include <LibGUI/Model.h>
#include <string.h>
#include <sys/stat.h>
#include <time.h>

namespace GUI {

class FileSystemModel
    : public Model
    , public Weakable<FileSystemModel> {
    friend struct Node;

public:
    enum Mode {
        Invalid,
        DirectoriesOnly,
        FilesAndDirectories
    };

    enum Column {
        Icon = 0,
        Name,
        Size,
        Owner,
        Group,
        Permissions,
        ModificationTime,
        Inode,
        SymlinkTarget,
        __Count,
    };

    struct Node {
        ~Node() { close(m_watch_fd); }

        String name;
        String symlink_target;
        size_t size { 0 };
        mode_t mode { 0 };
        uid_t uid { 0 };
        gid_t gid { 0 };
        ino_t inode { 0 };
        time_t mtime { 0 };
        bool is_accessible_directory { false };

        size_t total_size { 0 };

        mutable RefPtr<Gfx::Bitmap> thumbnail;
        bool is_directory() const { return S_ISDIR(mode); }
        bool is_executable() const { return mode & (S_IXUSR | S_IXGRP | S_IXOTH); }

        bool is_selected() const { return m_selected; }
        void set_selected(bool selected);

        bool has_error() const { return m_error != 0; }
        int error() const { return m_error; }
        const char* error_string() const { return strerror(m_error); }

        String full_path() const;

    private:
        friend class FileSystemModel;

        explicit Node(FileSystemModel& model)
            : m_model(model)
        {
        }

        FileSystemModel& m_model;

        Node* parent { nullptr };
        NonnullOwnPtrVector<Node> children;
        bool has_traversed { false };

        bool m_selected { false };

        int m_watch_fd { -1 };
        RefPtr<Core::Notifier> m_notifier;

        int m_error { 0 };
        bool m_parent_of_root { false };

        ModelIndex index(int column) const;
        void traverse_if_needed();
        void reify_if_needed();
        bool fetch_data(const String& full_path, bool is_root);
    };

    static NonnullRefPtr<FileSystemModel> create(const StringView& root_path = "/", Mode mode = Mode::FilesAndDirectories)
    {
        return adopt(*new FileSystemModel(root_path, mode));
    }
    virtual ~FileSystemModel() override;

    String root_path() const { return m_root_path; }
    void set_root_path(const StringView&);
    String full_path(const ModelIndex&) const;
    ModelIndex index(const StringView& path, int column) const;

    void update_node_on_selection(const ModelIndex&, const bool);
    ModelIndex m_previously_selected_index {};

    const Node& node(const ModelIndex& index) const;

    Function<void(int done, int total)> on_thumbnail_progress;
    Function<void()> on_complete;
    Function<void(int error, const char* error_string)> on_error;

    virtual int tree_column() const override { return Column::Name; }
    virtual int row_count(const ModelIndex& = ModelIndex()) const override;
    virtual int column_count(const ModelIndex& = ModelIndex()) const override;
    virtual String column_name(int column) const override;
    virtual Variant data(const ModelIndex&, ModelRole = ModelRole::Display) const override;
    virtual void update() override;
    virtual ModelIndex parent_index(const ModelIndex&) const override;
    virtual ModelIndex index(int row, int column = 0, const ModelIndex& parent = ModelIndex()) const override;
    virtual StringView drag_data_type() const override { return "text/uri-list"; }
    virtual bool accepts_drag(const ModelIndex&, const StringView& data_type) override;
    virtual bool is_column_sortable(int column_index) const override { return column_index != Column::Icon; }
    virtual bool is_editable(const ModelIndex&) const override;
    virtual void set_data(const ModelIndex&, const Variant&) override;

    static String timestamp_string(time_t timestamp)
    {
        return Core::DateTime::from_timestamp(timestamp).to_string();
    }

    bool should_show_dotfiles() const { return m_should_show_dotfiles; }
    void set_should_show_dotfiles(bool);

private:
    FileSystemModel(const StringView& root_path, Mode);

    String name_for_uid(uid_t) const;
    String name_for_gid(gid_t) const;

    HashMap<uid_t, String> m_user_names;
    HashMap<gid_t, String> m_group_names;

    bool fetch_thumbnail_for(const Node& node);
    GUI::Icon icon_for(const Node& node) const;

    String m_root_path;
    Mode m_mode { Invalid };
    OwnPtr<Node> m_root { nullptr };

    unsigned m_thumbnail_progress { 0 };
    unsigned m_thumbnail_progress_total { 0 };

    bool m_should_show_dotfiles { false };
};

}
