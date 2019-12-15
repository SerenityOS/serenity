#include "GDirectoryModel.h"
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/CDirIterator.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibGUI/GPainter.h>
#include <LibThread/BackgroundAction.h>
#include <dirent.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <unistd.h>

static HashMap<String, RefPtr<GraphicsBitmap>> s_thumbnail_cache;

static RefPtr<GraphicsBitmap> render_thumbnail(const StringView& path)
{
    auto png_bitmap = GraphicsBitmap::load_from_file(path);
    if (!png_bitmap)
        return nullptr;
    auto thumbnail = GraphicsBitmap::create(png_bitmap->format(), { 32, 32 });
    Painter painter(*thumbnail);
    painter.draw_scaled_bitmap(thumbnail->rect(), *png_bitmap, png_bitmap->rect());
    return thumbnail;
}

GDirectoryModel::GDirectoryModel()
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
}

GDirectoryModel::~GDirectoryModel()
{
}

int GDirectoryModel::row_count(const GModelIndex&) const
{
    return m_directories.size() + m_files.size();
}

int GDirectoryModel::column_count(const GModelIndex&) const
{
    return Column::__Count;
}

String GDirectoryModel::column_name(int column) const
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
    }
    ASSERT_NOT_REACHED();
}

GModel::ColumnMetadata GDirectoryModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Icon:
        return { 16, TextAlignment::Center, nullptr, GModel::ColumnMetadata::Sortable::False };
    case Column::Name:
        return { 120, TextAlignment::CenterLeft };
    case Column::Size:
        return { 80, TextAlignment::CenterRight };
    case Column::Owner:
        return { 50, TextAlignment::CenterLeft };
    case Column::Group:
        return { 50, TextAlignment::CenterLeft };
    case Column::ModificationTime:
        return { 110, TextAlignment::CenterLeft };
    case Column::Permissions:
        return { 65, TextAlignment::CenterLeft };
    case Column::Inode:
        return { 60, TextAlignment::CenterRight };
    }
    ASSERT_NOT_REACHED();
}

bool GDirectoryModel::fetch_thumbnail_for(const Entry& entry)
{
    // See if we already have the thumbnail
    // we're looking for in the cache.
    auto path = entry.full_path(*this);
    auto it = s_thumbnail_cache.find(path);
    if (it != s_thumbnail_cache.end()) {
        if (!(*it).value)
            return false;
        entry.thumbnail = (*it).value;
        return true;
    }

    // Otherwise, arrange to render the thumbnail
    // in background and make it available later.

    s_thumbnail_cache.set(path, nullptr);
    m_thumbnail_progress_total++;

    auto directory_model = make_weak_ptr();

    LibThread::BackgroundAction<RefPtr<GraphicsBitmap>>::create(
        [path] {
            return render_thumbnail(path);
        },

        [this, path, directory_model](auto thumbnail) {
            s_thumbnail_cache.set(path, move(thumbnail));

            // class was destroyed, no need to update progress or call any event handlers.
            if (directory_model.is_null())
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

GIcon GDirectoryModel::icon_for_file(const mode_t mode, const String name) const
{
    if (S_ISDIR(mode))
        return m_directory_icon;
    if (S_ISLNK(mode))
        return m_symlink_icon;
    if (S_ISSOCK(mode))
        return m_socket_icon;
    if (mode & S_IXUSR)
        return m_executable_icon;
    if (name.to_lowercase().ends_with(".wav"))
        return m_filetype_sound_icon;
    if (name.to_lowercase().ends_with(".html"))
        return m_filetype_html_icon;
    if (name.to_lowercase().ends_with(".png")) {
        return m_filetype_image_icon;
    }
    return m_file_icon;
}

GIcon GDirectoryModel::icon_for(const Entry& entry) const
{
    if (entry.name.to_lowercase().ends_with(".png")) {
        if (!entry.thumbnail) {
            if (!const_cast<GDirectoryModel*>(this)->fetch_thumbnail_for(entry))
                return m_filetype_image_icon;
        }
        return GIcon(m_filetype_image_icon.bitmap_for_size(16), *entry.thumbnail);
    }

    return icon_for_file(entry.mode, entry.name);
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

String GDirectoryModel::name_for_uid(uid_t uid) const
{
    auto it = m_user_names.find(uid);
    if (it == m_user_names.end())
        return String::number(uid);
    return (*it).value;
}

String GDirectoryModel::name_for_gid(uid_t gid) const
{
    auto it = m_user_names.find(gid);
    if (it == m_user_names.end())
        return String::number(gid);
    return (*it).value;
}

GVariant GDirectoryModel::data(const GModelIndex& index, Role role) const
{
    ASSERT(is_valid(index));
    auto& entry = this->entry(index.row());
    if (role == Role::Custom) {
        ASSERT(index.column() == Column::Name);
        return entry.full_path(*this);
    }
    if (role == Role::Sort) {
        switch (index.column()) {
        case Column::Icon:
            return entry.is_directory() ? 0 : 1;
        case Column::Name:
            return entry.name;
        case Column::Size:
            return (int)entry.size;
        case Column::Owner:
            return name_for_uid(entry.uid);
        case Column::Group:
            return name_for_gid(entry.gid);
        case Column::Permissions:
            return permission_string(entry.mode);
        case Column::ModificationTime:
            return entry.mtime;
        case Column::Inode:
            return (int)entry.inode;
        }
        ASSERT_NOT_REACHED();
    }
    if (role == Role::Display) {
        switch (index.column()) {
        case Column::Icon:
            return icon_for(entry);
        case Column::Name:
            return entry.name;
        case Column::Size:
            return (int)entry.size;
        case Column::Owner:
            return name_for_uid(entry.uid);
        case Column::Group:
            return name_for_gid(entry.gid);
        case Column::Permissions:
            return permission_string(entry.mode);
        case Column::ModificationTime:
            return timestamp_string(entry.mtime);
        case Column::Inode:
            return (int)entry.inode;
        }
    }
    if (role == Role::Icon) {
        return icon_for(entry);
    }
    return {};
}

void GDirectoryModel::update()
{
    CDirIterator di(m_path, CDirIterator::SkipDots);
    if (di.has_error()) {
        fprintf(stderr, "CDirIterator: %s\n", di.error_string());
        exit(1);
    }

    m_directories.clear();
    m_files.clear();

    m_bytes_in_files = 0;
    while (di.has_next()) {
        String name = di.next_path();
        Entry entry;
        entry.name = name;

        struct stat st;
        int rc = lstat(String::format("%s/%s", m_path.characters(), name.characters()).characters(), &st);
        if (rc < 0) {
            perror("lstat");
            continue;
        }
        entry.size = st.st_size;
        entry.mode = st.st_mode;
        entry.uid = st.st_uid;
        entry.gid = st.st_gid;
        entry.inode = st.st_ino;
        entry.mtime = st.st_mtime;
        auto& entries = S_ISDIR(st.st_mode) ? m_directories : m_files;
        entries.append(move(entry));

        m_bytes_in_files += st.st_size;
    }

    did_update();
}

void GDirectoryModel::open(const StringView& a_path)
{
    auto path = canonicalized_path(a_path);
    if (m_path == path)
        return;
    DIR* dirp = opendir(path.characters());
    if (!dirp)
        return;
    closedir(dirp);
    if (m_notifier) {
        close(m_notifier->fd());
        m_notifier = nullptr;
    }
    m_path = path;
    int watch_fd = watch_file(path.characters(), path.length());
    if (watch_fd < 0) {
        perror("watch_file");
    } else {
        m_notifier = CNotifier::construct(watch_fd, CNotifier::Event::Read);
        m_notifier->on_ready_to_read = [this] {
            update();
            char buffer[32];
            int rc = read(m_notifier->fd(), buffer, sizeof(buffer));
            ASSERT(rc >= 0);
        };
    }
    if (on_path_change)
        on_path_change();
    update();
}
