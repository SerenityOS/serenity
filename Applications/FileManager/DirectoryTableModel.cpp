#include "DirectoryTableModel.h"
#include <dirent.h>
#include <stdio.h>
#include <unistd.h>
#include <grp.h>
#include <pwd.h>
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>

DirectoryTableModel::DirectoryTableModel()
{
    m_directory_icon = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/folder16.rgb", { 16, 16 });
    m_file_icon = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/file16.rgb", { 16, 16 });
    m_symlink_icon = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/link16.rgb", { 16, 16 });
    m_socket_icon = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/socket16.rgb", { 16, 16 });
    m_executable_icon = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/executable16.rgb", { 16, 16 });

    setpwent();
    while (auto* passwd = getpwent())
        m_user_names.set(passwd->pw_uid, passwd->pw_name);
    endpwent();

    setgrent();
    while (auto* group = getgrent())
        m_group_names.set(group->gr_gid, group->gr_name);
    endgrent();
}

DirectoryTableModel::~DirectoryTableModel()
{
}

int DirectoryTableModel::row_count() const
{
    return m_directories.size() + m_files.size();
}

int DirectoryTableModel::column_count() const
{
    return Column::__Count;
}

String DirectoryTableModel::column_name(int column) const
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

GTableModel::ColumnMetadata DirectoryTableModel::column_metadata(int column) const
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

const GraphicsBitmap& DirectoryTableModel::icon_for(const Entry& entry) const
{
    if (S_ISDIR(entry.mode))
        return *m_directory_icon;
    if (S_ISLNK(entry.mode))
        return *m_symlink_icon;
    if (S_ISSOCK(entry.mode))
        return *m_socket_icon;
    if (entry.mode & S_IXUSR)
        return *m_executable_icon;
    return *m_file_icon;
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

String DirectoryTableModel::name_for_uid(uid_t uid) const
{
    auto it = m_user_names.find(uid);
    if (it == m_user_names.end())
        return String::format("%u", uid);
    return (*it).value;
}

String DirectoryTableModel::name_for_gid(uid_t gid) const
{
    auto it = m_user_names.find(gid);
    if (it == m_user_names.end())
        return String::format("%u", gid);
    return (*it).value;
}

GVariant DirectoryTableModel::data(const GModelIndex& index) const
{
    auto& entry = this->entry(index.row());
    switch (index.column()) {
    case Column::Icon: return icon_for(entry);
    case Column::Name: return entry.name;
    case Column::Size: return (int)entry.size;
    case Column::Owner: return name_for_uid(entry.uid);
    case Column::Group: return name_for_gid(entry.gid);
    case Column::Permissions: return permission_string(entry.mode);
    case Column::Inode: return (int)entry.inode;
    }
    ASSERT_NOT_REACHED();

}

void DirectoryTableModel::update()
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

void DirectoryTableModel::open(const String& a_path)
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

void DirectoryTableModel::activate(const GModelIndex& index)
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

    if (fork() == 0) {
        int rc = execl("/bin/TextEditor", "/bin/TextEditor", path.string().characters(), nullptr);
        if (rc < 0)
            perror("exec");
        ASSERT_NOT_REACHED();
    }
    return;
}
