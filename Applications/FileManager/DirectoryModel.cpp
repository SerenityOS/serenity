#include "DirectoryModel.h"
#include <dirent.h>
#include <stdio.h>
#include <unistd.h>
#include <grp.h>
#include <pwd.h>
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>

static HashMap<String, RetainPtr<GraphicsBitmap>>& thumbnail_cache()
{
    static HashMap<String, RetainPtr<GraphicsBitmap>>* s_map;
    if (!s_map)
        s_map = new HashMap<String, RetainPtr<GraphicsBitmap>>();
    return *s_map;
}

int thumbnail_thread(void* model)
{
    for (;;) {
        sleep(1);
        for (auto& it : thumbnail_cache()) {
            if (it.value)
                continue;
            if (auto png_bitmap = GraphicsBitmap::load_from_file(it.key)) {
                auto thumbnail = GraphicsBitmap::create(png_bitmap->format(), { 32, 32 });
                Painter painter(*thumbnail);
                painter.draw_scaled_bitmap(thumbnail->rect(), *png_bitmap, png_bitmap->rect());
                it.value = move(thumbnail);
                ((DirectoryModel*)model)->did_update();
            }
        }
    }
}

DirectoryModel::DirectoryModel()
{
    create_thread(thumbnail_thread, this);

    m_directory_icon = GIcon(GraphicsBitmap::load_from_file("/res/icons/folder16.png"), GraphicsBitmap::load_from_file("/res/icons/32x32/folder.png"));
    m_file_icon = GIcon(GraphicsBitmap::load_from_file("/res/icons/file16.png"), GraphicsBitmap::load_from_file("/res/icons/32x32/file.png"));
    m_symlink_icon = GIcon(GraphicsBitmap::load_from_file("/res/icons/link16.png"));
    m_socket_icon = GIcon(GraphicsBitmap::load_from_file("/res/icons/socket16.png"));
    m_executable_icon = GIcon(GraphicsBitmap::load_from_file("/res/icons/executable16.png"), GraphicsBitmap::load_from_file("/res/icons/32x32/filetype-executable.png"));
    m_filetype_image_icon = GIcon(GraphicsBitmap::load_from_file("/res/icons/16x16/filetype-image.png"), GraphicsBitmap::load_from_file("/res/icons/32x32/filetype-image.png"));

    setpwent();
    while (auto* passwd = getpwent())
        m_user_names.set(passwd->pw_uid, passwd->pw_name);
    endpwent();

    setgrent();
    while (auto* group = getgrent())
        m_group_names.set(group->gr_gid, group->gr_name);
    endgrent();
}

DirectoryModel::~DirectoryModel()
{
}

int DirectoryModel::row_count() const
{
    return m_directories.size() + m_files.size();
}

int DirectoryModel::column_count() const
{
    return Column::__Count;
}

String DirectoryModel::column_name(int column) const
{
    switch (column) {
    case Column::Icon: return "";
    case Column::Name: return "Name";
    case Column::Size: return "Size";
    case Column::Owner: return "Owner";
    case Column::Group: return "Group";
    case Column::Permissions: return "Mode";
    case Column::Inode: return "Inode";
    }
    ASSERT_NOT_REACHED();
}

GModel::ColumnMetadata DirectoryModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Icon: return { 16, TextAlignment::Center };
    case Column::Name: return { 120, TextAlignment::CenterLeft };
    case Column::Size: return { 80, TextAlignment::CenterRight };
    case Column::Owner: return { 50, TextAlignment::CenterLeft };
    case Column::Group: return { 50, TextAlignment::CenterLeft };
    case Column::Permissions: return { 80, TextAlignment::CenterLeft };
    case Column::Inode: return { 80, TextAlignment::CenterRight };
    }
    ASSERT_NOT_REACHED();
}

GIcon DirectoryModel::icon_for(const Entry& entry) const
{
    if (S_ISDIR(entry.mode))
        return m_directory_icon;
    if (S_ISLNK(entry.mode))
        return m_symlink_icon;
    if (S_ISSOCK(entry.mode))
        return m_socket_icon;
    if (entry.mode & S_IXUSR)
        return m_executable_icon;
    if (entry.name.ends_with(".png")) {
        if (!entry.thumbnail) {
            auto path = entry.full_path(*this);
            auto it = thumbnail_cache().find(path);
            if (it != thumbnail_cache().end()) {
                entry.thumbnail = (*it).value.copy_ref();
            } else {
                thumbnail_cache().set(path, nullptr);
            }
        }
        if (!entry.thumbnail)
            return m_filetype_image_icon;
        return GIcon(m_filetype_image_icon.bitmap_for_size(16), *entry.thumbnail);
    }
    return m_file_icon;
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
        mode & S_IWOTH ? 'w' : '-'
    );

    if (mode & S_ISVTX)
        builder.append("t");
    else
        builder.appendf("%c", mode & S_IXOTH ? 'x' : '-');
    return builder.to_string();
}

String DirectoryModel::name_for_uid(uid_t uid) const
{
    auto it = m_user_names.find(uid);
    if (it == m_user_names.end())
        return String::format("%u", uid);
    return (*it).value;
}

String DirectoryModel::name_for_gid(uid_t gid) const
{
    auto it = m_user_names.find(gid);
    if (it == m_user_names.end())
        return String::format("%u", gid);
    return (*it).value;
}

GVariant DirectoryModel::data(const GModelIndex& index, Role role) const
{
    ASSERT(is_valid(index));
    auto& entry = this->entry(index.row());
    if (role == Role::Sort) {
        switch (index.column()) {
        case Column::Icon: return entry.is_directory() ? 0 : 1;
        case Column::Name: return entry.name;
        case Column::Size: return (int)entry.size;
        case Column::Owner: return name_for_uid(entry.uid);
        case Column::Group: return name_for_gid(entry.gid);
        case Column::Permissions: return permission_string(entry.mode);
        case Column::Inode: return (int)entry.inode;
        }
        ASSERT_NOT_REACHED();
    }
    if (role == Role::Display) {
        switch (index.column()) {
        case Column::Icon: return icon_for(entry);
        case Column::Name: return entry.name;
        case Column::Size: return (int)entry.size;
        case Column::Owner: return name_for_uid(entry.uid);
        case Column::Group: return name_for_gid(entry.gid);
        case Column::Permissions: return permission_string(entry.mode);
        case Column::Inode: return (int)entry.inode;
        }
    }
    if (role == Role::Icon) {
        return icon_for(entry);
    }
    return { };
}

void DirectoryModel::update()
{
    DIR* dirp = opendir(m_path.characters());
    if (!dirp) {
        perror("opendir");
        exit(1);
    }
    m_directories.clear();
    m_files.clear();

    m_bytes_in_files = 0;
    while (auto* de = readdir(dirp)) {
        Entry entry;
        entry.name = de->d_name;
        if (entry.name == "." || entry.name == "..")
            continue;
        struct stat st;
        int rc = lstat(String::format("%s/%s", m_path.characters(), de->d_name).characters(), &st);
        if (rc < 0) {
            perror("lstat");
            continue;
        }
        entry.size = st.st_size;
        entry.mode = st.st_mode;
        entry.uid = st.st_uid;
        entry.gid = st.st_gid;
        entry.inode = st.st_ino;
        auto& entries = S_ISDIR(st.st_mode) ? m_directories : m_files;
        entries.append(move(entry));

        if (S_ISREG(entry.mode))
            m_bytes_in_files += st.st_size;
    }
    closedir(dirp);

    did_update();
}

void DirectoryModel::open(const String& a_path)
{
    FileSystemPath canonical_path(a_path);
    auto path = canonical_path.string();
    if (m_path == path)
        return;
    DIR* dirp = opendir(path.characters());
    if (!dirp)
        return;
    closedir(dirp);
    m_path = path;
    update();
    set_selected_index({ 0, 0 });
}

void DirectoryModel::activate(const GModelIndex& index)
{
    if (!index.is_valid())
        return;
    auto& entry = this->entry(index.row());
    FileSystemPath path(String::format("%s/%s", m_path.characters(), entry.name.characters()));
    if (entry.is_directory()) {
        open(path.string());
        return;
    }
    if (entry.is_executable()) {
        if (fork() == 0) {
            int rc = execl(path.string().characters(), path.string().characters(), nullptr);
            if (rc < 0)
                perror("exec");
            ASSERT_NOT_REACHED();
        }
        return;
    }

    if (path.string().ends_with(".png")) {
        if (fork() == 0) {
            int rc = execl("/bin/qs", "/bin/qs", path.string().characters(), nullptr);
            if (rc < 0)
                perror("exec");
            ASSERT_NOT_REACHED();
        }
        return;
    }

    if (fork() == 0) {
        int rc = execl("/bin/TextEditor", "/bin/TextEditor", path.string().characters(), nullptr);
        if (rc < 0)
            perror("exec");
        ASSERT_NOT_REACHED();
    }
    return;
}
