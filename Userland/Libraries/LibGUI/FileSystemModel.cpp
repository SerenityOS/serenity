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
#include <LibGUI/AbstractView.h>
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
    if (!m_parent)
        return {};
    for (size_t row = 0; row < m_parent->m_children.size(); ++row) {
        if (&m_parent->m_children[row] == this)
            return m_model.create_index(row, column, const_cast<Node*>(this));
    }
    VERIFY_NOT_REACHED();
}

bool FileSystemModel::Node::fetch_data(String const& full_path, bool is_root)
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
    if (!is_directory() || m_has_traversed)
        return;

    m_has_traversed = true;

    if (m_parent_of_root) {
        auto root = adopt_own(*new Node(m_model));
        root->fetch_data("/", true);
        root->name = "/";
        root->m_parent = this;
        m_children.append(move(root));
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
        auto maybe_child = create_child(child_name);
        if (!maybe_child)
            continue;

        auto child = maybe_child.release_nonnull();
        total_size += child->size;
        if (S_ISDIR(child->mode))
            directory_children.append(move(child));
        else
            file_children.append(move(child));
    }

    m_children.extend(move(directory_children));
    m_children.extend(move(file_children));

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

OwnPtr<FileSystemModel::Node> FileSystemModel::Node::create_child(String const& child_name)
{
    String child_path = LexicalPath::join(full_path(), child_name).string();
    auto child = adopt_own(*new Node(m_model));

    bool ok = child->fetch_data(child_path, false);
    if (!ok)
        return {};

    if (m_model.m_mode == DirectoriesOnly && !S_ISDIR(child->mode))
        return {};

    child->name = child_name;
    child->m_parent = this;
    return child;
}

void FileSystemModel::Node::reify_if_needed()
{
    traverse_if_needed();
    if (mode != 0)
        return;
    fetch_data(full_path(), m_parent == nullptr || m_parent->m_parent_of_root);
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
    for (auto* ancestor = m_parent; ancestor; ancestor = ancestor->m_parent) {
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

    Node const* node = m_root->m_parent_of_root ? &m_root->m_children.first() : m_root;
    if (lexical_path.string() == "/")
        return node;

    auto& parts = lexical_path.parts_view();
    for (size_t i = 0; i < parts.size(); ++i) {
        auto& part = parts[i];
        bool found = false;
        for (auto& child : node->m_children) {
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

String FileSystemModel::full_path(ModelIndex const& index) const
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
        handle_file_event(event);
    };

    invalidate();
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

void FileSystemModel::update_node_on_selection(ModelIndex const& index, bool const selected)
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
    invalidate();

    if (m_root->has_error()) {
        if (on_directory_change_error)
            on_directory_change_error(m_root->error(), m_root->error_string());
    } else if (on_complete) {
        on_complete();
    }
}

void FileSystemModel::invalidate()
{
    m_root = adopt_own(*new Node(*this));

    if (m_root_path.is_null())
        m_root->m_parent_of_root = true;

    m_root->reify_if_needed();

    Model::invalidate();
}

void FileSystemModel::handle_file_event(Core::FileWatcherEvent const& event)
{
    if (event.type == Core::FileWatcherEvent::Type::ChildCreated) {
        if (node_for_path(event.event_path) != nullptr)
            return;
    } else {
        if (node_for_path(event.event_path) == nullptr)
            return;
    }

    switch (event.type) {
    case Core::FileWatcherEvent::Type::ChildCreated: {
        LexicalPath path { event.event_path };
        auto& parts = path.parts_view();
        StringView child_name = parts.last();

        auto parent_name = path.parent().string();
        Node* parent = const_cast<Node*>(node_for_path(parent_name));
        if (parent == nullptr) {
            dbgln("Got a ChildCreated on '{}' but that path does not exist?!", parent_name);
            break;
        }

        int child_count = parent->m_children.size();

        auto maybe_child = parent->create_child(child_name);
        if (!maybe_child)
            break;

        begin_insert_rows(parent->index(0), child_count, child_count);

        auto child = maybe_child.release_nonnull();
        parent->total_size += child->size;
        parent->m_children.append(move(child));

        end_insert_rows();
        break;
    }
    case Core::FileWatcherEvent::Type::Deleted:
    case Core::FileWatcherEvent::Type::ChildDeleted: {
        Node* child = const_cast<Node*>(node_for_path(event.event_path));
        if (child == nullptr) {
            dbgln("Got a ChildDeleted/Deleted on '{}' but the child does not exist?! (already gone?)", event.event_path);
            break;
        }

        auto index = child->index(0);
        begin_delete_rows(index.parent(), index.row(), index.row());

        Node* parent = child->m_parent;
        parent->m_children.remove(index.row());

        end_delete_rows();

        for_each_view([&](AbstractView& view) {
            view.selection().remove_matching([&](auto& selection_index) {
                return selection_index.internal_data() == index.internal_data();
            });
            if (view.cursor_index().internal_data() == index.internal_data()) {
                view.set_cursor({}, GUI::AbstractView::SelectionUpdate::None);
            }
        });

        break;
    }
    case Core::FileWatcherEvent::Type::MetadataModified: {
        // FIXME: Do we do anything in case the metadata is modified?
        //        Perhaps re-stat'ing the modified node would make sense
        //        here, but let's leave that to when we actually need it.
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    did_update(UpdateFlag::DontInvalidateIndices);
}

int FileSystemModel::row_count(ModelIndex const& index) const
{
    Node& node = const_cast<Node&>(this->node(index));
    node.reify_if_needed();
    if (node.is_directory())
        return node.m_children.size();
    return 0;
}

FileSystemModel::Node const& FileSystemModel::node(ModelIndex const& index) const
{
    if (!index.is_valid())
        return *m_root;
    VERIFY(index.internal_data());
    return *(Node*)index.internal_data();
}

ModelIndex FileSystemModel::index(int row, int column, ModelIndex const& parent) const
{
    if (row < 0 || column < 0)
        return {};
    auto& node = this->node(parent);
    const_cast<Node&>(node).reify_if_needed();
    if (static_cast<size_t>(row) >= node.m_children.size())
        return {};
    return create_index(row, column, &node.m_children[row]);
}

ModelIndex FileSystemModel::parent_index(ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};
    auto& node = this->node(index);
    if (!node.m_parent) {
        VERIFY(&node == m_root);
        return {};
    }
    return node.m_parent->index(index.column());
}

Variant FileSystemModel::data(ModelIndex const& index, ModelRole role) const
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
        case Column::User:
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
        case Column::User:
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
        case Column::User:
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

    if (role == ModelRole::IconOpacity) {
        if (node.name.starts_with('.'))
            return 0.5f;
        return {};
    }

    return {};
}

Icon FileSystemModel::icon_for(Node const& node) const
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

static ErrorOr<NonnullRefPtr<Gfx::Bitmap>> render_thumbnail(StringView path)
{
    auto bitmap = TRY(Gfx::Bitmap::try_load_from_file(path));
    auto thumbnail = TRY(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, { 32, 32 }));

    double scale = min(32 / (double)bitmap->width(), 32 / (double)bitmap->height());
    auto destination = Gfx::IntRect(0, 0, (int)(bitmap->width() * scale), (int)(bitmap->height() * scale)).centered_within(thumbnail->rect());

    Painter painter(thumbnail);
    painter.draw_scaled_bitmap(destination, *bitmap, bitmap->rect());
    return thumbnail;
}

bool FileSystemModel::fetch_thumbnail_for(Node const& node)
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

    (void)Threading::BackgroundAction<ErrorOr<NonnullRefPtr<Gfx::Bitmap>>>::construct(
        [path](auto&) {
            return render_thumbnail(path);
        },

        [this, path, weak_this](auto thumbnail_or_error) {
            if (thumbnail_or_error.is_error()) {
                s_thumbnail_cache.set(path, nullptr);
                return;
            }
            s_thumbnail_cache.set(path, thumbnail_or_error.release_value());

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

int FileSystemModel::column_count(ModelIndex const&) const
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
    case Column::User:
        return "User";
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

bool FileSystemModel::accepts_drag(ModelIndex const& index, Vector<String> const& mime_types) const
{
    if (!mime_types.contains_slow("text/uri-list"))
        return false;

    if (!index.is_valid())
        return true;

    auto& node = this->node(index);
    return node.is_directory();
}

void FileSystemModel::set_should_show_dotfiles(bool show)
{
    if (m_should_show_dotfiles == show)
        return;
    m_should_show_dotfiles = show;

    // FIXME: add a way to granularly update in this case.
    invalidate();
}

bool FileSystemModel::is_editable(ModelIndex const& index) const
{
    if (!index.is_valid())
        return false;
    return index.column() == Column::Name;
}

void FileSystemModel::set_data(ModelIndex const& index, Variant const& data)
{
    VERIFY(is_editable(index));
    Node& node = const_cast<Node&>(this->node(index));
    auto dirname = LexicalPath::dirname(node.full_path());
    auto new_full_path = String::formatted("{}/{}", dirname, data.to_string());
    int rc = rename(node.full_path().characters(), new_full_path.characters());
    if (rc < 0) {
        if (on_rename_error)
            on_rename_error(errno, strerror(errno));
        return;
    }

    if (on_rename_successful)
        on_rename_successful(node.full_path(), new_full_path);
}

Vector<ModelIndex> FileSystemModel::matches(StringView searching, unsigned flags, ModelIndex const& index)
{
    Node& node = const_cast<Node&>(this->node(index));
    node.reify_if_needed();
    Vector<ModelIndex> found_indices;
    for (auto& child : node.m_children) {
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
