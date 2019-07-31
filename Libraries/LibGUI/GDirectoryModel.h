#pragma once

#include <AK/HashMap.h>
#include <LibCore/CNotifier.h>
#include <LibGUI/GModel.h>
#include <sys/stat.h>

class GDirectoryModel final : public GModel {
    friend int thumbnail_thread(void*);

public:
    static NonnullRefPtr<GDirectoryModel> create() { return adopt(*new GDirectoryModel); }
    virtual ~GDirectoryModel() override;

    enum Column {
        Icon = 0,
        Name,
        Size,
        Owner,
        Group,
        Permissions,
        ModificationTime,
        Inode,
        __Count,
    };

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override;
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

    String path() const { return m_path; }
    void open(const StringView& path);
    size_t bytes_in_files() const { return m_bytes_in_files; }

    Function<void(int done, int total)> on_thumbnail_progress;

    struct Entry {
        String name;
        size_t size { 0 };
        mode_t mode { 0 };
        uid_t uid { 0 };
        uid_t gid { 0 };
        ino_t inode { 0 };
        time_t mtime { 0 };
        mutable RefPtr<GraphicsBitmap> thumbnail;
        bool is_directory() const { return S_ISDIR(mode); }
        bool is_executable() const { return mode & S_IXUSR; }
        String full_path(const GDirectoryModel& model) const { return String::format("%s/%s", model.path().characters(), name.characters()); }
    };

    const Entry& entry(int index) const
    {
        if (index < m_directories.size())
            return m_directories[index];
        return m_files[index - m_directories.size()];
    }

private:
    GDirectoryModel();

    String name_for_uid(uid_t) const;
    String name_for_gid(gid_t) const;

    GIcon icon_for(const Entry& entry) const;

    String m_path;
    Vector<Entry> m_files;
    Vector<Entry> m_directories;
    size_t m_bytes_in_files;

    GIcon m_directory_icon;
    GIcon m_file_icon;
    GIcon m_symlink_icon;
    GIcon m_socket_icon;
    GIcon m_executable_icon;
    GIcon m_filetype_image_icon;

    HashMap<uid_t, String> m_user_names;
    HashMap<gid_t, String> m_group_names;

    OwnPtr<CNotifier> m_notifier;
};
