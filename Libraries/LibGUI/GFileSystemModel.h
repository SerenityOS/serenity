#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/CNotifier.h>
#include <LibGUI/GModel.h>
#include <sys/stat.h>
#include <time.h>

class GFileSystemModel : public GModel
    , public Weakable<GFileSystemModel> {
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
        __Count,
    };

    struct Node {
        ~Node() { close(m_watch_fd); }

        String name;
        size_t size { 0 };
        mode_t mode { 0 };
        uid_t uid { 0 };
        gid_t gid { 0 };
        ino_t inode { 0 };
        time_t mtime { 0 };

        size_t total_size { 0 };

        mutable RefPtr<GraphicsBitmap> thumbnail;
        bool is_directory() const { return S_ISDIR(mode); }
        bool is_executable() const { return mode & S_IXUSR; }

        String full_path(const GFileSystemModel&) const;

    private:
        friend class GFileSystemModel;

        Node* parent { nullptr };
        NonnullOwnPtrVector<Node> children;
        bool has_traversed { false };

        int m_watch_fd { -1 };
        RefPtr<CNotifier> m_notifier;

        GModelIndex index(const GFileSystemModel&, int column) const;
        void traverse_if_needed(const GFileSystemModel&);
        void reify_if_needed(const GFileSystemModel&);
        bool fetch_data_using_lstat(const String& full_path);
    };

    static NonnullRefPtr<GFileSystemModel> create(const StringView& root_path = "/", Mode mode = Mode::FilesAndDirectories)
    {
        return adopt(*new GFileSystemModel(root_path, mode));
    }
    virtual ~GFileSystemModel() override;

    String root_path() const { return m_root_path; }
    void set_root_path(const StringView&);
    String full_path(const GModelIndex&) const;
    GModelIndex index(const StringView& path, int column) const;

    const Node& node(const GModelIndex& index) const;
    GIcon icon_for_file(const mode_t mode, const String& name) const;

    Function<void(int done, int total)> on_thumbnail_progress;
    Function<void()> on_root_path_change;

    virtual int tree_column() const { return Column::Name; }
    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override;
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;
    virtual GModelIndex parent_index(const GModelIndex&) const override;
    virtual GModelIndex index(int row, int column = 0, const GModelIndex& parent = GModelIndex()) const override;

    static String timestamp_string(time_t timestamp)
    {
        auto* tm = localtime(&timestamp);
        return String::format("%4u-%02u-%02u %02u:%02u:%02u",
            tm->tm_year + 1900,
            tm->tm_mon + 1,
            tm->tm_mday,
            tm->tm_hour,
            tm->tm_min,
            tm->tm_sec);
    }

private:
    GFileSystemModel(const StringView& root_path, Mode);

    String name_for_uid(uid_t) const;
    String name_for_gid(gid_t) const;

    HashMap<uid_t, String> m_user_names;
    HashMap<gid_t, String> m_group_names;

    bool fetch_thumbnail_for(const Node& node);
    GIcon icon_for(const Node& node) const;

    String m_root_path;
    Mode m_mode { Invalid };
    OwnPtr<Node> m_root { nullptr };

    GIcon m_directory_icon;
    GIcon m_file_icon;
    GIcon m_symlink_icon;
    GIcon m_socket_icon;
    GIcon m_executable_icon;
    GIcon m_filetype_image_icon;
    GIcon m_filetype_sound_icon;
    GIcon m_filetype_html_icon;

    unsigned m_thumbnail_progress { 0 };
    unsigned m_thumbnail_progress_total { 0 };
};
