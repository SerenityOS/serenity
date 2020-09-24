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

#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibThread/BackgroundAction.h>
#include <dirent.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

namespace GUI {

ModelIndex FileSystemModel::Node::index(int column) const
{
    if (!parent)
        return {};
    for (size_t row = 0; row < parent->children.size(); ++row) {
        if (&parent->children[row] == this)
            return m_model.create_index(row, column, const_cast<Node*>(this));
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
        m_error = errno;
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
        symlink_target = Core::File::read_link(full_path);
        if (symlink_target.is_null())
            perror("readlink");
    }

    if (S_ISDIR(mode)) {
        is_accessible_directory = access(full_path.characters(), R_OK | X_OK) == 0;
    }

    return true;
}

void FileSystemModel::Node::traverse_if_needed()
{
    if (!is_directory() || has_traversed)
        return;

    has_traversed = true;

    if (m_parent_of_root) {
        auto root = adopt_own(*new Node(m_model));
        root->fetch_data("/", true);
        root->name = "/";
        root->parent = this;
        children.append(move(root));
        return;
    }

    total_size = 0;

    auto full_path = this->full_path();
    Core::DirIterator di(full_path, m_model.should_show_dotfiles() ? Core::DirIterator::SkipParentAndBaseDir : Core::DirIterator::SkipDots);
    if (di.has_error()) {
        m_error = di.error();
        fprintf(stderr, "DirIterator: %s\n", di.error_string());
        return;
    }

    Vector<String> child_names;
    while (di.has_next()) {
        child_names.append(di.next_path());
    }
    quick_sort(child_names);

    for (auto& name : child_names) {
        String child_path = String::format("%s/%s", full_path.characters(), name.characters());
        auto child = adopt_own(*new Node(m_model));
        bool ok = child->fetch_data(child_path, false);
        if (!ok)
            continue;
        if (m_model.m_mode == DirectoriesOnly && !S_ISDIR(child->mode))
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
    m_notifier->on_ready_to_read = [this] {
        char buffer[32];
        int rc = read(m_notifier->fd(), buffer, sizeof(buffer));
        ASSERT(rc >= 0);

        has_traversed = false;
        mode = 0;
        children.clear();
        reify_if_needed();
        m_model.did_update();
    };
}

void FileSystemModel::Node::reify_if_needed()
{
    traverse_if_needed();
    if (mode != 0)
        return;
    fetch_data(full_path(), parent == nullptr || parent->m_parent_of_root);
}

String FileSystemModel::Node::full_path() const
{
    Vector<String, 32> lineage;
    for (auto* ancestor = parent; ancestor; ancestor = ancestor->parent) {
        lineage.append(ancestor->name);
    }
    StringBuilder builder;
    builder.append(m_model.root_path());
    for (int i = lineage.size() - 1; i >= 0; --i) {
        builder.append('/');
        builder.append(lineage[i]);
    }
    builder.append('/');
    builder.append(name);
    return LexicalPath::canonicalized_path(builder.to_string());
}

ModelIndex FileSystemModel::index(const StringView& path, int column) const
{
    LexicalPath lexical_path(path);
    const Node* node = m_root->m_parent_of_root ? &m_root->children.first() : m_root;
    if (lexical_path.string() == "/")
        return node->index(column);
    for (size_t i = 0; i < lexical_path.parts().size(); ++i) {
        auto& part = lexical_path.parts()[i];
        bool found = false;
        for (auto& child : node->children) {
            if (child.name == part) {
                const_cast<Node&>(child).reify_if_needed();
                node = &child;
                found = true;
                if (i == lexical_path.parts().size() - 1)
                    return child.index(column);
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
    const_cast<Node&>(node).reify_if_needed();
    return node.full_path();
}

FileSystemModel::FileSystemModel(const StringView& root_path, Mode mode)
    : m_root_path(LexicalPath::canonicalized_path(root_path))
    , m_mode(mode)
{
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

String FileSystemModel::name_for_gid(gid_t gid) const
{
    auto it = m_group_names.find(gid);
    if (it == m_group_names.end())
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

void FileSystemModel::Node::set_selected(bool selected)
{
    if (m_selected == selected)
        return;
    m_selected = selected;
}

void FileSystemModel::update_node_on_selection(const ModelIndex& index, const bool selected)
{
    Node& node = const_cast<Node&>(this->node(index));
    node.set_selected(selected);
}

void FileSystemModel::set_root_path(const StringView& root_path)
{
    if (root_path.is_null())
        m_root_path = {};
    else
        m_root_path = LexicalPath::canonicalized_path(root_path);
    update();

    if (m_root->has_error()) {
        if (on_error)
            on_error(m_root->error(), m_root->error_string());
    } else if (on_complete) {
        on_complete();
    }
}

void FileSystemModel::update()
{
    m_root = adopt_own(*new Node(*this));

    if (m_root_path.is_null())
        m_root->m_parent_of_root = true;

    m_root->reify_if_needed();

    did_update();
}

int FileSystemModel::row_count(const ModelIndex& index) const
{
    Node& node = const_cast<Node&>(this->node(index));
    node.reify_if_needed();
    if (node.is_directory())
        return node.children.size();
    return 0;
}

const FileSystemModel::Node& FileSystemModel::node(const ModelIndex& index) const
{
    if (!index.is_valid())
        return *m_root;
    ASSERT(index.internal_data());
    return *(Node*)index.internal_data();
}

ModelIndex FileSystemModel::index(int row, int column, const ModelIndex& parent) const
{
    if (row < 0 || column < 0)
        return {};
    auto& node = this->node(parent);
    const_cast<Node&>(node).reify_if_needed();
    if (static_cast<size_t>(row) >= node.children.size())
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
    return node.parent->index(index.column());
}

Variant FileSystemModel::data(const ModelIndex& index, ModelRole role) const
{
    ASSERT(index.is_valid());

    if (role == ModelRole::TextAlignment) {
        switch (index.column()) {
        case Column::Icon:
            return Gfx::TextAlignment::Center;
        case Column::Size:
        case Column::Inode:
            return Gfx::TextAlignment::CenterRight;
        case Column::Name:
        case Column::Owner:
        case Column::Group:
        case Column::ModificationTime:
        case Column::Permissions:
        case Column::SymlinkTarget:
            return Gfx::TextAlignment::CenterLeft;
        default:
            ASSERT_NOT_REACHED();
        }
    }

    auto& node = this->node(index);

    if (role == ModelRole::Custom) {
        // For GUI::FileSystemModel, custom role means the full path.
        ASSERT(index.column() == Column::Name);
        return node.full_path();
    }

    if (role == ModelRole::DragData) {
        if (index.column() == Column::Name) {
            StringBuilder builder;
            builder.append("file://");
            builder.append(node.full_path());
            return builder.to_string();
        }
        return {};
    }

    if (role == ModelRole::Sort) {
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

    if (role == ModelRole::Display) {
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

    if (role == ModelRole::Icon) {
        return icon_for(node);
    }
    return {};
}

Icon FileSystemModel::icon_for(const Node& node) const
{
    if (node.full_path() == "/")
        return FileIconProvider::icon_for_path("/");

    if (Gfx::Bitmap::is_path_a_supported_image_format(node.name.to_lowercase())) {
        if (!node.thumbnail) {
            if (!const_cast<FileSystemModel*>(this)->fetch_thumbnail_for(node))
                return FileIconProvider::filetype_image_icon();
        }
        return GUI::Icon(FileIconProvider::filetype_image_icon().bitmap_for_size(16), *node.thumbnail);
    }

    if (node.is_directory()) {
        if (node.full_path() == Core::StandardPaths::home_directory()) {
            if (node.is_selected())
                return FileIconProvider::home_directory_open_icon();
            return FileIconProvider::home_directory_icon();
        }
        if (node.is_selected() && node.is_accessible_directory)
            return FileIconProvider::directory_open_icon();
    }

    return FileIconProvider::icon_for_path(node.full_path(), node.mode);
}

static HashMap<String, RefPtr<Gfx::Bitmap>> s_thumbnail_cache;

static RefPtr<Gfx::Bitmap> render_thumbnail(const StringView& path)
{
    auto png_bitmap = Gfx::Bitmap::load_from_file(path);
    if (!png_bitmap)
        return nullptr;

    double scale = min(32 / (double)png_bitmap->width(), 32 / (double)png_bitmap->height());

    auto thumbnail = Gfx::Bitmap::create(png_bitmap->format(), { 32, 32 });
    Gfx::IntRect destination = Gfx::IntRect(0, 0, (int)(png_bitmap->width() * scale), (int)(png_bitmap->height() * scale));
    destination.center_within(thumbnail->rect());

    Painter painter(*thumbnail);
    painter.draw_scaled_bitmap(destination, *png_bitmap, png_bitmap->rect());
    return thumbnail;
}

bool FileSystemModel::fetch_thumbnail_for(const Node& node)
{
    // See if we already have the thumbnail
    // we're looking for in the cache.
    auto path = node.full_path();
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

bool FileSystemModel::accepts_drag(const ModelIndex& index, const StringView& data_type)
{
    if (!index.is_valid())
        return false;
    if (data_type != "text/uri-list")
        return false;
    auto& node = this->node(index);
    return node.is_directory();
}

void FileSystemModel::set_should_show_dotfiles(bool show)
{
    if (m_should_show_dotfiles == show)
        return;
    m_should_show_dotfiles = show;
    update();
}

bool FileSystemModel::is_editable(const ModelIndex& index) const
{
    if (!index.is_valid())
        return false;
    return index.column() == Column::Name;
}

void FileSystemModel::set_data(const ModelIndex& index, const Variant& data)
{
    ASSERT(is_editable(index));
    Node& node = const_cast<Node&>(this->node(index));
    auto dirname = LexicalPath(node.full_path()).dirname();
    auto new_full_path = String::formatted("{}/{}", dirname, data.to_string());
    int rc = rename(node.full_path().characters(), new_full_path.characters());
    if (rc < 0) {
        if (on_error)
            on_error(errno, strerror(errno));
    }
}

}
