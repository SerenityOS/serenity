/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibThreading/BackgroundAction.h>
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
    VERIFY_NOT_REACHED();
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
        warnln("DirIterator: {}", di.error_string());
        return;
    }

    Vector<String> child_names;
    while (di.has_next()) {
        child_names.append(di.next_path());
    }
    quick_sort(child_names);

    NonnullOwnPtrVector<Node> directory_children;
    NonnullOwnPtrVector<Node> file_children;

    for (auto& child_name : child_names) {
        String child_path = String::formatted("{}/{}", full_path, child_name);
        auto child = adopt_own(*new Node(m_model));
        bool ok = child->fetch_data(child_path, false);
        if (!ok)
            continue;
        if (m_model.m_mode == DirectoriesOnly && !S_ISDIR(child->mode))
            continue;
        child->name = child_name;
        child->parent = this;
        total_size += child->size;
        if (S_ISDIR(child->mode))
            directory_children.append(move(child));
        else
            file_children.append(move(child));
    }

    children.extend(move(directory_children));
    children.extend(move(file_children));

    if (!m_model.m_file_watcher->is_watching(full_path)) {
        // We are not already watching this file, watch it
        auto result = m_model.m_file_watcher->add_watch(full_path,
            Core::FileWatcherEvent::Type::MetadataModified
                | Core::FileWatcherEvent::Type::ChildCreated
                | Core::FileWatcherEvent::Type::ChildDeleted
                | Core::FileWatcherEvent::Type::Deleted);

        if (result.is_error()) {
            dbgln("Couldn't watch '{}': {}", full_path, result.error());
        } else if (result.value() == false) {
            dbgln("Couldn't watch '{}', probably already watching", full_path);
        }
    }
}

void FileSystemModel::Node::reify_if_needed()
{
    traverse_if_needed();
    if (mode != 0)
        return;
    fetch_data(full_path(), parent == nullptr || parent->m_parent_of_root);
}

bool FileSystemModel::Node::is_symlink_to_directory() const
{
    if (!S_ISLNK(mode))
        return false;
    struct stat st;
    if (lstat(symlink_target.characters(), &st) < 0)
        return false;
    return S_ISDIR(st.st_mode);
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

ModelIndex FileSystemModel::index(String path, int column) const
{
    Node const* node = node_for_path(move(path));
    if (node != nullptr) {
        return node->index(column);
    }

    return {};
}

FileSystemModel::Node const* FileSystemModel::node_for_path(String const& path) const
{
    String resolved_path;
    if (path == m_root_path)
        resolved_path = "/";
    else if (!m_root_path.is_empty() && path.starts_with(m_root_path))
        resolved_path = LexicalPath::relative_path(path, m_root_path);
    else
        resolved_path = path;
    LexicalPath lexical_path(resolved_path);

    const Node* node = m_root->m_parent_of_root ? &m_root->children.first() : m_root;
    if (lexical_path.string() == "/")
        return node;

    auto& parts = lexical_path.parts_view();
    for (size_t i = 0; i < parts.size(); ++i) {
        auto& part = parts[i];
        bool found = false;
        for (auto& child : node->children) {
            if (child.name == part) {
                const_cast<Node&>(child).reify_if_needed();
                node = &child;
                found = true;
                if (i == parts.size() - 1)
                    return node;
                break;
            }
        }
        if (!found)
            return nullptr;
    }
    return nullptr;
}

String FileSystemModel::full_path(const ModelIndex& index) const
{
    auto& node = this->node(index);
    const_cast<Node&>(node).reify_if_needed();
    return node.full_path();
}

FileSystemModel::FileSystemModel(String root_path, Mode mode)
    : m_root_path(LexicalPath::canonicalized_path(move(root_path)))
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

    auto result = Core::FileWatcher::create();
    if (result.is_error()) {
        dbgln("{}", result.error());
        VERIFY_NOT_REACHED();
    }

    m_file_watcher = result.release_value();
    m_file_watcher->on_change = [this](Core::FileWatcherEvent const& event) {
        Node const* maybe_node = node_for_path(event.event_path);
        if (maybe_node == nullptr) {
            dbgln("Received event at \"{}\" but we don't have that node", event.event_path);
            return;
        }
        auto& node = *const_cast<Node*>(maybe_node);

        dbgln("Event at \"{}\" on Node {}: {}", node.full_path(), &node, event);

        // FIXME: Your time is coming, un-granular updates.
        auto refresh_node = [](Node& node) {
            node.has_traversed = false;
            node.mode = 0;
            node.children.clear();
            node.reify_if_needed();
        };

        if (event.type == Core::FileWatcherEvent::Type::Deleted) {
            auto canonical_event_path = LexicalPath::canonicalized_path(event.event_path);
            if (m_root_path.starts_with(canonical_event_path)) {
                // Deleted directory contains our root, so navigate to our nearest parent.
                auto new_path = LexicalPath(m_root_path).parent();
                while (!Core::File::is_directory(new_path.string()))
                    new_path = new_path.parent();

                set_root_path(new_path.string());
            } else if (node.parent) {
                refresh_node(*node.parent);
            }
        } else {
            refresh_node(node);
        }
        did_update();
    };

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

    builder.append(mode & S_IRUSR ? 'r' : '-');
    builder.append(mode & S_IWUSR ? 'w' : '-');
    builder.append(mode & S_ISUID ? 's' : (mode & S_IXUSR ? 'x' : '-'));
    builder.append(mode & S_IRGRP ? 'r' : '-');
    builder.append(mode & S_IWGRP ? 'w' : '-');
    builder.append(mode & S_ISGID ? 's' : (mode & S_IXGRP ? 'x' : '-'));
    builder.append(mode & S_IROTH ? 'r' : '-');
    builder.append(mode & S_IWOTH ? 'w' : '-');

    if (mode & S_ISVTX)
        builder.append('t');
    else
        builder.append(mode & S_IXOTH ? 'x' : '-');
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

void FileSystemModel::set_root_path(String root_path)
{
    if (root_path.is_null())
        m_root_path = {};
    else
        m_root_path = LexicalPath::canonicalized_path(move(root_path));
    update();

    if (m_root->has_error()) {
        if (on_directory_change_error)
            on_directory_change_error(m_root->error(), m_root->error_string());
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
    VERIFY(index.internal_data());
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
        VERIFY(&node == m_root);
        return {};
    }
    return node.parent->index(index.column());
}

Variant FileSystemModel::data(const ModelIndex& index, ModelRole role) const
{
    VERIFY(index.is_valid());

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
            VERIFY_NOT_REACHED();
        }
    }

    auto& node = this->node(index);

    if (role == ModelRole::Custom) {
        // For GUI::FileSystemModel, custom role means the full path.
        VERIFY(index.column() == Column::Name);
        return node.full_path();
    }

    if (role == ModelRole::MimeData) {
        if (index.column() == Column::Name)
            return URL::create_with_file_scheme(node.full_path()).serialize();
        return {};
    }

    if (role == ModelRole::Sort) {
        switch (index.column()) {
        case Column::Icon:
            return node.is_directory() ? 0 : 1;
        case Column::Name:
            // NOTE: The children of a Node are grouped by directory-or-file and then sorted alphabetically.
            //       Hence, the sort value for the name column is simply the index row. :^)
            return index.row();
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
        VERIFY_NOT_REACHED();
    }

    if (role == ModelRole::Display) {
        switch (index.column()) {
        case Column::Icon:
            return icon_for(node);
        case Column::Name:
            return node.name;
        case Column::Size:
            return human_readable_size(node.size);
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

    if (Gfx::Bitmap::is_path_a_supported_image_format(node.name)) {
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
        if (node.full_path() == Core::StandardPaths::desktop_directory())
            return FileIconProvider::desktop_directory_icon();
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

    auto thumbnail = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { 32, 32 });
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

    Threading::BackgroundAction<RefPtr<Gfx::Bitmap>>::create(
        [path](auto&) {
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

            did_update(UpdateFlag::DontInvalidateIndices);
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
    VERIFY_NOT_REACHED();
}

bool FileSystemModel::accepts_drag(const ModelIndex& index, const Vector<String>& mime_types) const
{
    if (!index.is_valid())
        return false;
    if (!mime_types.contains_slow("text/uri-list"))
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
    VERIFY(is_editable(index));
    Node& node = const_cast<Node&>(this->node(index));
    auto dirname = LexicalPath::dirname(node.full_path());
    auto new_full_path = String::formatted("{}/{}", dirname, data.to_string());
    int rc = rename(node.full_path().characters(), new_full_path.characters());
    if (rc < 0) {
        if (on_rename_error)
            on_rename_error(errno, strerror(errno));
    }
}

Vector<ModelIndex, 1> FileSystemModel::matches(const StringView& searching, unsigned flags, const ModelIndex& index)
{
    Node& node = const_cast<Node&>(this->node(index));
    node.reify_if_needed();
    Vector<ModelIndex, 1> found_indices;
    for (auto& child : node.children) {
        if (string_matches(child.name, searching, flags)) {
            const_cast<Node&>(child).reify_if_needed();
            found_indices.append(child.index(Column::Name));
            if (flags & FirstMatchOnly)
                break;
        }
    }

    return found_indices;
}

}
