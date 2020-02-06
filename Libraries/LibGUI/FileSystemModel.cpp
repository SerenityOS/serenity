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

#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/DirIterator.h>
#include <LibGfx/Bitmap.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/Painter.h>
#include <LibThread/BackgroundAction.h>
#include <dirent.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

namespace GUI {

ModelIndex FileSystemModel::Node::index(const FileSystemModel& model, int column) const
{
    if (!parent)
        return {};
    for (int row = 0; row < parent->children.size(); ++row) {
        if (&parent->children[row] == this)
            return model.create_index(row, column, const_cast<Node*>(this));
    }
    ASSERT_NOT_REACHED();
}

bool FileSystemModel::Node::fetch_data(const String& full_path, bool is_root)
{
    struct stat st;
    int rc;
    if (is_root)
        rc = stat(full_path.characters(), &st);
    else
        rc = lstat(full_path.characters(), &st);
    if (rc < 0) {
        perror("stat/lstat");
        return false;
    }

    size = st.st_size;
    mode = st.st_mode;
    uid = st.st_uid;
    gid = st.st_gid;
    inode = st.st_ino;
    mtime = st.st_mtime;

    if (S_ISLNK(mode)) {
        char buffer[PATH_MAX];
        int length = readlink(full_path.characters(), buffer, sizeof(buffer));
        if (length < 0) {
            perror("readlink");
        } else {
            ASSERT(length > 0);
            symlink_target = String(buffer, length - 1);
        }
    }

    return true;
}

void FileSystemModel::Node::traverse_if_needed(const FileSystemModel& model)
{
    if (!is_directory() || has_traversed)
        return;
    has_traversed = true;
    total_size = 0;

    auto full_path = this->full_path(model);
    Core::DirIterator di(full_path, Core::DirIterator::SkipDots);
    if (di.has_error()) {
        fprintf(stderr, "CDirIterator: %s\n", di.error_string());
        return;
    }

    while (di.has_next()) {
        String name = di.next_path();
        String child_path = String::format("%s/%s", full_path.characters(), name.characters());
        NonnullOwnPtr<Node> child = make<Node>();
        bool ok = child->fetch_data(child_path, false);
        if (!ok)
            continue;
        if (model.m_mode == DirectoriesOnly && !S_ISDIR(child->mode))
            continue;
        child->name = name;
        child->parent = this;
        total_size += child->size;
        children.append(move(child));
    }

    if (m_watch_fd >= 0)
        return;

    m_watch_fd = watch_file(full_path.characters(), full_path.length());
    if (m_watch_fd < 0) {
        perror("watch_file");
        return;
    }
    fcntl(m_watch_fd, F_SETFD, FD_CLOEXEC);
    dbg() << "Watching " << full_path << " for changes, m_watch_fd = " << m_watch_fd;
    m_notifier = Core::Notifier::construct(m_watch_fd, Core::Notifier::Event::Read);
    m_notifier->on_ready_to_read = [this, &model] {
        char buffer[32];
        int rc = read(m_notifier->fd(), buffer, sizeof(buffer));
        ASSERT(rc >= 0);

        has_traversed = false;
        mode = 0;
        children.clear();
        reify_if_needed(model);
        const_cast<FileSystemModel&>(model).did_update();
    };
}

void FileSystemModel::Node::reify_if_needed(const FileSystemModel& model)
{
    traverse_if_needed(model);
    if (mode != 0)
        return;
    fetch_data(full_path(model), parent == nullptr);
}

String FileSystemModel::Node::full_path(const FileSystemModel& model) const
{
    Vector<String, 32> lineage;
    for (auto* ancestor = parent; ancestor; ancestor = ancestor->parent) {
        lineage.append(ancestor->name);
    }
    StringBuilder builder;
    builder.append(model.root_path());
    for (int i = lineage.size() - 1; i >= 0; --i) {
        builder.append('/');
        builder.append(lineage[i]);
    }
    builder.append('/');
    builder.append(name);
    return canonicalized_path(builder.to_string());
}

ModelIndex FileSystemModel::index(const StringView& path, int column) const
{
    FileSystemPath canonical_path(path);
    const Node* node = m_root;
    if (canonical_path.string() == "/")
        return m_root->index(*this, column);
    for (int i = 0; i < canonical_path.parts().size(); ++i) {
        auto& part = canonical_path.parts()[i];
        bool found = false;
        for (auto& child : node->children) {
            if (child.name == part) {
                const_cast<Node&>(child).reify_if_needed(*this);
                node = &child;
                found = true;
                if (i == canonical_path.parts().size() - 1)
                    return child.index(*this, column);
                break;
            }
        }
        if (!found)
            return {};
    }
    return {};
}

String FileSystemModel::full_path(const ModelIndex& index) const
{
    auto& node = this->node(index);
    const_cast<Node&>(node).reify_if_needed(*this);
    return node.full_path(*this);
}

FileSystemModel::FileSystemModel(const StringView& root_path, Mode mode)
    : m_root_path(canonicalized_path(root_path))
    , m_mode(mode)
{
    m_directory_icon = GIcon::default_icon("filetype-folder");
    m_file_icon = GIcon::default_icon("filetype-unknown");
    m_symlink_icon = GIcon::default_icon("filetype-symlink");
    m_socket_icon = GIcon::default_icon("filetype-socket");
    m_executable_icon = GIcon::default_icon("filetype-executable");
    m_filetype_image_icon = GIcon::default_icon("filetype-image");
    m_filetype_sound_icon = GIcon::default_icon("filetype-sound");
    m_filetype_html_icon = GIcon::default_icon("filetype-html");

    setpwent();
    while (auto* passwd = getpwent())
        m_user_names.set(passwd->pw_uid, passwd->pw_name);
    endpwent();

    setgrent();
    while (auto* group = getgrent())
        m_group_names.set(group->gr_gid, group->gr_name);
    endgrent();

    update();
}

FileSystemModel::~FileSystemModel()
{
}

String FileSystemModel::name_for_uid(uid_t uid) const
{
    auto it = m_user_names.find(uid);
    if (it == m_user_names.end())
        return String::number(uid);
    return (*it).value;
}

String FileSystemModel::name_for_gid(uid_t gid) const
{
    auto it = m_user_names.find(gid);
    if (it == m_user_names.end())
        return String::number(gid);
    return (*it).value;
}

static String permission_string(mode_t mode)
{
    StringBuilder builder;
    if (S_ISDIR(mode))
        builder.append("d");
    else if (S_ISLNK(mode))
        builder.append("l");
    else if (S_ISBLK(mode))
        builder.append("b");
    else if (S_ISCHR(mode))
        builder.append("c");
    else if (S_ISFIFO(mode))
        builder.append("f");
    else if (S_ISSOCK(mode))
        builder.append("s");
    else if (S_ISREG(mode))
        builder.append("-");
    else
        builder.append("?");

    builder.appendf("%c%c%c%c%c%c%c%c",
        mode & S_IRUSR ? 'r' : '-',
        mode & S_IWUSR ? 'w' : '-',
        mode & S_ISUID ? 's' : (mode & S_IXUSR ? 'x' : '-'),
        mode & S_IRGRP ? 'r' : '-',
        mode & S_IWGRP ? 'w' : '-',
        mode & S_ISGID ? 's' : (mode & S_IXGRP ? 'x' : '-'),
        mode & S_IROTH ? 'r' : '-',
        mode & S_IWOTH ? 'w' : '-');

    if (mode & S_ISVTX)
        builder.append("t");
    else
        builder.appendf("%c", mode & S_IXOTH ? 'x' : '-');
    return builder.to_string();
}

void FileSystemModel::set_root_path(const StringView& root_path)
{
    m_root_path = canonicalized_path(root_path);

    if (on_root_path_change)
        on_root_path_change();

    update();
}

void FileSystemModel::update()
{
    m_root = make<Node>();
    m_root->reify_if_needed(*this);

    did_update();
}

int FileSystemModel::row_count(const ModelIndex& index) const
{
    Node& node = const_cast<Node&>(this->node(index));
    node.reify_if_needed(*this);
    if (node.is_directory())
        return node.children.size();
    return 0;
}

const FileSystemModel::Node& FileSystemModel::node(const ModelIndex& index) const
{
    if (!index.is_valid())
        return *m_root;
    return *(Node*)index.internal_data();
}

ModelIndex FileSystemModel::index(int row, int column, const ModelIndex& parent) const
{
    if (row < 0 || column < 0)
        return {};
    auto& node = this->node(parent);
    const_cast<Node&>(node).reify_if_needed(*this);
    if (row >= node.children.size())
        return {};
    return create_index(row, column, &node.children[row]);
}

ModelIndex FileSystemModel::parent_index(const ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto& node = this->node(index);
    if (!node.parent) {
        ASSERT(&node == m_root);
        return {};
    }
    return node.parent->index(*this, index.column());
}

Variant FileSystemModel::data(const ModelIndex& index, Role role) const
{
    ASSERT(index.is_valid());
    auto& node = this->node(index);

    if (role == Role::Custom) {
        // For GFileSystemModel, custom role means the full path.
        ASSERT(index.column() == Column::Name);
        return node.full_path(*this);
    }

    if (role == Role::DragData) {
        if (index.column() == Column::Name) {
            StringBuilder builder;
            builder.append("file://");
            builder.append(node.full_path(*this));
            return builder.to_string();
        }
        return {};
    }

    if (role == Role::Sort) {
        switch (index.column()) {
        case Column::Icon:
            return node.is_directory() ? 0 : 1;
        case Column::Name:
            return node.name;
        case Column::Size:
            return (int)node.size;
        case Column::Owner:
            return name_for_uid(node.uid);
        case Column::Group:
            return name_for_gid(node.gid);
        case Column::Permissions:
            return permission_string(node.mode);
        case Column::ModificationTime:
            return node.mtime;
        case Column::Inode:
            return (int)node.inode;
        case Column::SymlinkTarget:
            return node.symlink_target;
        }
        ASSERT_NOT_REACHED();
    }

    if (role == Role::Display) {
        switch (index.column()) {
        case Column::Icon:
            return icon_for(node);
        case Column::Name:
            return node.name;
        case Column::Size:
            return (int)node.size;
        case Column::Owner:
            return name_for_uid(node.uid);
        case Column::Group:
            return name_for_gid(node.gid);
        case Column::Permissions:
            return permission_string(node.mode);
        case Column::ModificationTime:
            return timestamp_string(node.mtime);
        case Column::Inode:
            return (int)node.inode;
        case Column::SymlinkTarget:
            return node.symlink_target;
        }
    }

    if (role == Role::Icon) {
        return icon_for(node);
    }
    return {};
}

GIcon FileSystemModel::icon_for_file(const mode_t mode, const String& name) const
{
    if (S_ISDIR(mode))
        return m_directory_icon;
    if (S_ISLNK(mode))
        return m_symlink_icon;
    if (S_ISSOCK(mode))
        return m_socket_icon;
    if (mode & (S_IXUSR | S_IXGRP | S_IXOTH))
        return m_executable_icon;
    if (name.to_lowercase().ends_with(".wav"))
        return m_filetype_sound_icon;
    if (name.to_lowercase().ends_with(".html"))
        return m_filetype_html_icon;
    if (name.to_lowercase().ends_with(".png"))
        return m_filetype_image_icon;
    return m_file_icon;
}

GIcon FileSystemModel::icon_for(const Node& node) const
{
    if (node.name.to_lowercase().ends_with(".png")) {
        if (!node.thumbnail) {
            if (!const_cast<FileSystemModel*>(this)->fetch_thumbnail_for(node))
                return m_filetype_image_icon;
        }
        return GIcon(m_filetype_image_icon.bitmap_for_size(16), *node.thumbnail);
    }

    return icon_for_file(node.mode, node.name);
}

static HashMap<String, RefPtr<Gfx::Bitmap>> s_thumbnail_cache;

static RefPtr<Gfx::Bitmap> render_thumbnail(const StringView& path)
{
    auto png_bitmap = Gfx::Bitmap::load_from_file(path);
    if (!png_bitmap)
        return nullptr;
    auto thumbnail = Gfx::Bitmap::create(png_bitmap->format(), { 32, 32 });
    Painter painter(*thumbnail);
    painter.draw_scaled_bitmap(thumbnail->rect(), *png_bitmap, png_bitmap->rect());
    return thumbnail;
}

bool FileSystemModel::fetch_thumbnail_for(const Node& node)
{
    // See if we already have the thumbnail
    // we're looking for in the cache.
    auto path = node.full_path(*this);
    auto it = s_thumbnail_cache.find(path);
    if (it != s_thumbnail_cache.end()) {
        if (!(*it).value)
            return false;
        node.thumbnail = (*it).value;
        return true;
    }

    // Otherwise, arrange to render the thumbnail
    // in background and make it available later.

    s_thumbnail_cache.set(path, nullptr);
    m_thumbnail_progress_total++;

    auto weak_this = make_weak_ptr();

    LibThread::BackgroundAction<RefPtr<Gfx::Bitmap>>::create(
        [path] {
            return render_thumbnail(path);
        },

        [this, path, weak_this](auto thumbnail) {
            s_thumbnail_cache.set(path, move(thumbnail));

            // The model was destroyed, no need to update
            // progress or call any event handlers.
            if (weak_this.is_null())
                return;

            m_thumbnail_progress++;
            if (on_thumbnail_progress)
                on_thumbnail_progress(m_thumbnail_progress, m_thumbnail_progress_total);
            if (m_thumbnail_progress == m_thumbnail_progress_total) {
                m_thumbnail_progress = 0;
                m_thumbnail_progress_total = 0;
            }

            did_update();
        });

    return false;
}

int FileSystemModel::column_count(const ModelIndex&) const
{
    return Column::__Count;
}

String FileSystemModel::column_name(int column) const
{
    switch (column) {
    case Column::Icon:
        return "";
    case Column::Name:
        return "Name";
    case Column::Size:
        return "Size";
    case Column::Owner:
        return "Owner";
    case Column::Group:
        return "Group";
    case Column::Permissions:
        return "Mode";
    case Column::ModificationTime:
        return "Modified";
    case Column::Inode:
        return "Inode";
    case Column::SymlinkTarget:
        return "Symlink target";
    }
    ASSERT_NOT_REACHED();
}

Model::ColumnMetadata FileSystemModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Icon:
        return { 16, Gfx::TextAlignment::Center, nullptr, Model::ColumnMetadata::Sortable::False };
    case Column::Name:
        return { 120, Gfx::TextAlignment::CenterLeft };
    case Column::Size:
        return { 80, Gfx::TextAlignment::CenterRight };
    case Column::Owner:
        return { 50, Gfx::TextAlignment::CenterLeft };
    case Column::Group:
        return { 50, Gfx::TextAlignment::CenterLeft };
    case Column::ModificationTime:
        return { 110, Gfx::TextAlignment::CenterLeft };
    case Column::Permissions:
        return { 65, Gfx::TextAlignment::CenterLeft };
    case Column::Inode:
        return { 60, Gfx::TextAlignment::CenterRight };
    case Column::SymlinkTarget:
        return { 120, Gfx::TextAlignment::CenterLeft };
    }
    ASSERT_NOT_REACHED();
}

}
