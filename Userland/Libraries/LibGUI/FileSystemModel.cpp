/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <LibCore/DirIterator.h>
#include <LibCore/StandardPaths.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/AbstractView.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibImageDecoderClient/Client.h>
#include <LibThreading/BackgroundAction.h>
#include <LibThreading/MutexProtected.h>
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
        if (m_parent->m_children[row] == this)
            return m_model.create_index(row, column, const_cast<Node*>(this));
    }
    VERIFY_NOT_REACHED();
}

bool FileSystemModel::Node::fetch_data(ByteString const& full_path, bool is_root)
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
        auto sym_link_target_or_error = FileSystem::read_link(full_path);
        if (sym_link_target_or_error.is_error())
            perror("readlink");
        else {
            symlink_target = sym_link_target_or_error.release_value();
            if (symlink_target.is_empty())
                perror("readlink");
        }
    }

    if (S_ISDIR(mode)) {
        is_accessible_directory = access(full_path.characters(), R_OK | X_OK) == 0;
    }

    return true;
}

void FileSystemModel::Node::traverse_if_needed()
{
    if (m_has_traversed)
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

    auto full_path = this->full_path();

    if (!is_directory()) {
        if (m_model.m_mode != DirectoriesOnly && !m_model.m_file_watcher->is_watching(full_path)) {
            // We are not already watching this file, watch it
            auto result = m_model.m_file_watcher->add_watch(full_path, Core::FileWatcherEvent::Type::MetadataModified);

            if (result.is_error()) {
                dbgln("Couldn't watch '{}': {}", full_path, result.error());
            } else if (!result.value()) {
                dbgln("Couldn't watch '{}', probably already watching", full_path);
            }
        }
        return;
    }

    total_size = 0;

    Core::DirIterator di(full_path, m_model.should_show_dotfiles() ? Core::DirIterator::SkipParentAndBaseDir : Core::DirIterator::SkipDots);
    if (di.has_error()) {
        auto error = di.error();
        m_error = error.code();
        warnln("DirIterator: {}", error);
        return;
    }

    Vector<ByteString> child_names;
    while (di.has_next()) {
        child_names.append(di.next_path());
    }
    quick_sort(child_names);

    Vector<NonnullOwnPtr<Node>> directory_children;
    Vector<NonnullOwnPtr<Node>> file_children;

    for (auto& child_name : child_names) {
        auto maybe_child = create_child(child_name);
        if (!maybe_child)
            continue;

        auto child = maybe_child.release_nonnull();
        total_size += child->size;
        if (S_ISDIR(child->mode)) {
            directory_children.append(move(child));
        } else {
            if (!m_model.m_allowed_file_extensions.has_value()) {
                file_children.append(move(child));
                continue;
            }

            for (auto& extension : *m_model.m_allowed_file_extensions) {
                if (child_name.ends_with(ByteString::formatted(".{}", extension))) {
                    file_children.append(move(child));
                    break;
                }
            }
        }
    }

    m_children.extend(move(directory_children));
    m_children.extend(move(file_children));

    if (!m_model.m_file_watcher->is_watching(full_path)) {
        // We are not already watching this directory, watch it
        auto result = m_model.m_file_watcher->add_watch(full_path,
            Core::FileWatcherEvent::Type::MetadataModified
                | Core::FileWatcherEvent::Type::ChildCreated
                | Core::FileWatcherEvent::Type::ChildDeleted
                | Core::FileWatcherEvent::Type::Deleted);

        if (result.is_error()) {
            dbgln("Couldn't watch '{}': {}", full_path, result.error());
        } else if (!result.value()) {
            dbgln("Couldn't watch '{}', probably already watching", full_path);
        }
    }
}

bool FileSystemModel::Node::can_delete_or_move() const
{
    if (!m_can_delete_or_move.has_value())
        m_can_delete_or_move = FileSystem::can_delete_or_move(full_path());
    return m_can_delete_or_move.value();
}

OwnPtr<FileSystemModel::Node> FileSystemModel::Node::create_child(ByteString const& child_name)
{
    ByteString child_path = LexicalPath::join(full_path(), child_name).string();
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
    if (mode == 0)
        fetch_data(full_path(), m_parent == nullptr || m_parent->m_parent_of_root);
    traverse_if_needed();
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

ByteString FileSystemModel::Node::full_path() const
{
    Vector<ByteString, 32> lineage;
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
    return LexicalPath::canonicalized_path(builder.to_byte_string());
}

ModelIndex FileSystemModel::index(ByteString path, int column) const
{
    auto node = node_for_path(move(path));
    if (node.has_value())
        return node->index(column);

    return {};
}

Optional<FileSystemModel::Node const&> FileSystemModel::node_for_path(ByteString const& path) const
{
    ByteString resolved_path;
    if (path == m_root_path)
        resolved_path = "/";
    else if (m_root_path.has_value() && !m_root_path->is_empty() && path.starts_with(*m_root_path))
        resolved_path = LexicalPath::relative_path(path, *m_root_path);
    else
        resolved_path = path;
    LexicalPath lexical_path(resolved_path);

    Node const* node = m_root->m_parent_of_root ? m_root->m_children.first() : m_root.ptr();
    if (lexical_path.string() == "/")
        return *node;

    auto& parts = lexical_path.parts_view();
    for (size_t i = 0; i < parts.size(); ++i) {
        auto& part = parts[i];
        bool found = false;
        for (auto& child : node->m_children) {
            if (child->name == part) {
                const_cast<Node&>(*child).reify_if_needed();
                node = child;
                found = true;
                if (i == parts.size() - 1)
                    return *node;
                break;
            }
        }
        if (!found)
            return {};
    }
    return {};
}

ByteString FileSystemModel::full_path(ModelIndex const& index) const
{
    auto& node = this->node(index);
    const_cast<Node&>(node).reify_if_needed();
    return node.full_path();
}

FileSystemModel::FileSystemModel(Optional<ByteString> root_path, Mode mode)
    : m_root_path(root_path.map(LexicalPath::canonicalized_path))
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

ByteString FileSystemModel::name_for_uid(uid_t uid) const
{
    auto it = m_user_names.find(uid);
    if (it == m_user_names.end())
        return ByteString::number(uid);
    return (*it).value;
}

ByteString FileSystemModel::name_for_gid(gid_t gid) const
{
    auto it = m_group_names.find(gid);
    if (it == m_group_names.end())
        return ByteString::number(gid);
    return (*it).value;
}

static ByteString permission_string(mode_t mode)
{
    StringBuilder builder;
    if (S_ISDIR(mode))
        builder.append('d');
    else if (S_ISLNK(mode))
        builder.append('l');
    else if (S_ISBLK(mode))
        builder.append('b');
    else if (S_ISCHR(mode))
        builder.append('c');
    else if (S_ISFIFO(mode))
        builder.append('f');
    else if (S_ISSOCK(mode))
        builder.append('s');
    else if (S_ISREG(mode))
        builder.append('-');
    else
        builder.append('?');

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
    return builder.to_byte_string();
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

void FileSystemModel::set_root_path(Optional<ByteString> root_path)
{
    if (!root_path.has_value())
        m_root_path = {};
    else
        m_root_path = LexicalPath::canonicalized_path(move(*root_path));
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

    if (!m_root_path.has_value())
        m_root->m_parent_of_root = true;

    m_root->reify_if_needed();
    for (auto const& child : m_root->m_children)
        child->reify_if_needed();

    Model::invalidate();
}

void FileSystemModel::handle_file_event(Core::FileWatcherEvent const& event)
{
    if (event.type == Core::FileWatcherEvent::Type::ChildCreated) {
        if (node_for_path(event.event_path).has_value())
            return;
    } else {
        if (!node_for_path(event.event_path).has_value())
            return;
    }

    switch (event.type) {
    case Core::FileWatcherEvent::Type::ChildCreated: {
        LexicalPath path { event.event_path };
        auto& parts = path.parts_view();
        StringView child_name = parts.last();
        if (!m_should_show_dotfiles && child_name.starts_with('.'))
            break;

        auto parent_name = path.parent().string();
        auto parent = node_for_path(parent_name);
        if (!parent.has_value()) {
            dbgln("Got a ChildCreated on '{}' but that path does not exist?!", parent_name);
            break;
        }

        auto& mutable_parent = const_cast<Node&>(*parent);
        auto maybe_child = mutable_parent.create_child(child_name);
        if (!maybe_child)
            break;

        auto child = maybe_child.release_nonnull();
        child->reify_if_needed();

        bool const is_new_child_dir = S_ISDIR(child->mode);
        int insert_index = 0;
        for (auto const& other_child : mutable_parent.m_children) {
            bool const is_other_child_dir = S_ISDIR(other_child->mode);
            if (!is_new_child_dir && is_other_child_dir) {
                ++insert_index;
                continue;
            }
            if (is_new_child_dir && !is_other_child_dir)
                break;

            if (other_child->name.view() > child_name)
                break;
            ++insert_index;
        }

        begin_insert_rows(parent->index(0), insert_index, insert_index);

        mutable_parent.total_size += child->size;
        mutable_parent.m_children.insert(insert_index, move(child));

        end_insert_rows();
        break;
    }
    case Core::FileWatcherEvent::Type::Deleted:
    case Core::FileWatcherEvent::Type::ChildDeleted: {
        auto child = node_for_path(event.event_path);
        if (!child.has_value()) {
            dbgln("Got a ChildDeleted/Deleted on '{}' but the child does not exist?! (already gone?)", event.event_path);
            break;
        }

        if (&child.value() == m_root) {
            // Root directory of the filesystem model has been removed. All items became invalid.
            invalidate();
            on_root_path_removed();
            break;
        }

        auto index = child->index(0);
        begin_delete_rows(index.parent(), index.row(), index.row());

        Node* parent = child->m_parent;
        parent->m_children.remove(index.row());

        end_delete_rows();

        for_each_view([&](AbstractView& view) {
            view.selection().remove_all_matching([&](auto& selection_index) {
                return selection_index.internal_data() == index.internal_data();
            });
            if (view.cursor_index().internal_data() == index.internal_data()) {
                view.set_cursor({}, GUI::AbstractView::SelectionUpdate::None);
            }
        });

        break;
    }
    case Core::FileWatcherEvent::Type::MetadataModified: {
        auto child = node_for_path(event.event_path);
        if (!child.has_value()) {
            dbgln("Got a MetadataModified on '{}' but the child does not exist?!", event.event_path);
            break;
        }

        auto& mutable_child = const_cast<Node&>(child.value());
        mutable_child.fetch_data(mutable_child.full_path(), &mutable_child == m_root);
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
    return create_index(row, column, node.m_children[row].ptr());
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
        return FileIconProvider::icon_for_path("/"sv);

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
        if (node.full_path().ends_with(".git"sv)) {
            if (node.is_selected())
                return FileIconProvider::git_directory_open_icon();
            return FileIconProvider::git_directory_icon();
        }
        if (node.full_path() == Core::StandardPaths::desktop_directory())
            return FileIconProvider::desktop_directory_icon();
        if (node.is_selected() && node.is_accessible_directory)
            return FileIconProvider::directory_open_icon();
    }

    return FileIconProvider::icon_for_path(node.full_path(), node.mode);
}

using BitmapBackgroundAction = Threading::BackgroundAction<NonnullRefPtr<Gfx::Bitmap>>;

// Mutex protected thumbnail cache data shared between threads.
struct ThumbnailCache {
    // Null pointers indicate an image that couldn't be loaded due to errors.
    HashMap<ByteString, RefPtr<Gfx::Bitmap>> thumbnail_cache {};
    HashMap<ByteString, NonnullRefPtr<BitmapBackgroundAction>> loading_thumbnails {};
};

static Threading::MutexProtected<ThumbnailCache> s_thumbnail_cache {};
static Threading::MutexProtected<RefPtr<ImageDecoderClient::Client>> s_image_decoder_client {};

static ErrorOr<NonnullRefPtr<Gfx::Bitmap>> render_thumbnail(StringView path)
{
    Core::EventLoop event_loop;
    Gfx::IntSize const thumbnail_size { 32, 32 };

    auto file = TRY(Core::MappedFile::map(path));
    auto decoded_image = TRY(s_image_decoder_client.with_locked([=, &file](auto& maybe_client) -> ErrorOr<Optional<ImageDecoderClient::DecodedImage>> {
        if (!maybe_client) {
            maybe_client = TRY(ImageDecoderClient::Client::try_create());
            maybe_client->on_death = []() {
                s_image_decoder_client.with_locked([](auto& client) {
                    client = nullptr;
                });
            };
        }

        auto mime_type = Core::guess_mime_type_based_on_filename(path);

        // FIXME: Refactor thumbnail rendering to be more async-aware. Possibly return this promise to the caller.
        auto decoded_image = TRY(maybe_client->decode_image(file->bytes(), {}, {}, thumbnail_size, mime_type)->await());

        return decoded_image;
    }));

    auto bitmap = decoded_image.value().frames[0].bitmap;

    auto thumbnail = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, thumbnail_size));

    double scale = min(thumbnail_size.width() / (double)bitmap->width(), thumbnail_size.height() / (double)bitmap->height());
    auto destination = Gfx::IntRect(0, 0, (int)(bitmap->width() * scale), (int)(bitmap->height() * scale)).centered_within(thumbnail->rect());

    Painter painter(thumbnail);
    painter.draw_scaled_bitmap(destination, *bitmap, bitmap->rect(), 1.f, Gfx::ScalingMode::BoxSampling);
    return thumbnail;
}

bool FileSystemModel::fetch_thumbnail_for(Node const& node)
{
    auto path = node.full_path();

    // See if we already have the thumbnail we're looking for in the cache.
    auto was_in_cache = s_thumbnail_cache.with_locked([&](auto& cache) {
        auto it = cache.thumbnail_cache.find(path);
        if (it != cache.thumbnail_cache.end()) {
            // Loading was unsuccessful.
            if (!(*it).value)
                return TriState::False;
            // Loading was successful.
            node.thumbnail = (*it).value;
            return TriState::True;
        }
        // Loading is in progress.
        if (cache.loading_thumbnails.contains(path))
            return TriState::False;
        return TriState::Unknown;
    });
    if (was_in_cache != TriState::Unknown)
        return was_in_cache == TriState::True;

    // Otherwise, arrange to render the thumbnail in background and make it available later.

    m_thumbnail_progress_total++;

    auto weak_this = make_weak_ptr();

    auto const action = [path](auto&) {
        return render_thumbnail(path);
    };
    auto const update_progress = [weak_this](bool with_success) {
        using namespace AK::TimeLiterals;
        if (auto strong_this = weak_this.strong_ref(); !strong_this.is_null()) {
            strong_this->m_thumbnail_progress++;
            if (strong_this->on_thumbnail_progress)
                strong_this->on_thumbnail_progress(strong_this->m_thumbnail_progress, strong_this->m_thumbnail_progress_total);
            if (strong_this->m_thumbnail_progress == strong_this->m_thumbnail_progress_total) {
                strong_this->m_thumbnail_progress = 0;
                strong_this->m_thumbnail_progress_total = 0;
            }

            if (with_success && (!strong_this->m_ui_update_timer.is_valid() || strong_this->m_ui_update_timer.elapsed_time() > 100_ms)) {
                strong_this->did_update(UpdateFlag::DontInvalidateIndices);
                strong_this->m_ui_update_timer.start();
            }
        }
    };

    auto const on_complete = [weak_this, path, update_progress](auto thumbnail) -> ErrorOr<void> {
        auto finished_generating_thumbnails = false;
        s_thumbnail_cache.with_locked([path, thumbnail, &finished_generating_thumbnails](auto& cache) {
            cache.thumbnail_cache.set(path, thumbnail);
            cache.loading_thumbnails.remove(path);
            finished_generating_thumbnails = cache.loading_thumbnails.is_empty();
        });

        if (auto strong_this = weak_this.strong_ref(); finished_generating_thumbnails && !strong_this.is_null())
            strong_this->m_ui_update_timer.reset();

        update_progress(true);

        return {};
    };

    auto const on_error = [path, update_progress](Error error) -> void {
        // Note: We need to defer that to avoid the function removing its last reference
        //       i.e. trying to destroy itself, which is prohibited.
        Core::EventLoop::current().deferred_invoke([path, error = Error::copy(error)]() mutable {
            s_thumbnail_cache.with_locked([path, error = move(error)](auto& cache) {
                if (error != Error::from_errno(ECANCELED)) {
                    cache.thumbnail_cache.set(path, nullptr);
                    dbgln("Failed to load thumbnail for {}: {}", path, error);
                }
                cache.loading_thumbnails.remove(path);
            });
        });

        update_progress(false);
    };

    s_thumbnail_cache.with_locked([path, action, on_complete, on_error](auto& cache) {
        cache.loading_thumbnails.set(path, BitmapBackgroundAction::construct(move(action), move(on_complete), move(on_error)));
    });

    return false;
}

int FileSystemModel::column_count(ModelIndex const&) const
{
    return Column::__Count;
}

ErrorOr<String> FileSystemModel::column_name(int column) const
{
    switch (column) {
    case Column::Icon:
        return String {};
    case Column::Name:
        return "Name"_string;
    case Column::Size:
        return "Size"_string;
    case Column::User:
        return "User"_string;
    case Column::Group:
        return "Group"_string;
    case Column::Permissions:
        return "Mode"_string;
    case Column::ModificationTime:
        return "Modified"_string;
    case Column::Inode:
        return "Inode"_string;
    case Column::SymlinkTarget:
        return "Symlink target"_string;
    }
    VERIFY_NOT_REACHED();
}

bool FileSystemModel::accepts_drag(ModelIndex const& index, Core::MimeData const& mime_data) const
{
    if (!mime_data.has_urls())
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

void FileSystemModel::set_allowed_file_extensions(Optional<Vector<ByteString>> const& allowed_file_extensions)
{
    if (m_allowed_file_extensions == allowed_file_extensions)
        return;
    m_allowed_file_extensions = allowed_file_extensions;

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
    auto new_full_path = ByteString::formatted("{}/{}", dirname, data.to_byte_string());
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
        if (string_matches(child->name, searching, flags)) {
            const_cast<Node&>(*child).reify_if_needed();
            found_indices.append(child->index(Column::Name));
            if (flags & FirstMatchOnly)
                break;
        }
    }

    return found_indices;
}

}
