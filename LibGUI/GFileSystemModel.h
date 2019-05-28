#pragma once

#include <LibGUI/GModel.h>

class GFileSystemModel : public GModel {
    friend class Node;

public:
    enum Mode
    {
        Invalid,
        DirectoriesOnly,
        FilesAndDirectories
    };

    static Retained<GFileSystemModel> create(const String& root_path = "/", Mode mode = Mode::FilesAndDirectories)
    {
        return adopt(*new GFileSystemModel(root_path, mode));
    }
    virtual ~GFileSystemModel() override;

    String root_path() const { return m_root_path; }
    String path(const GModelIndex&) const;
    GModelIndex index(const String& path) const;

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;
    virtual GModelIndex parent_index(const GModelIndex&) const override;
    virtual GModelIndex index(int row, int column = 0, const GModelIndex& parent = GModelIndex()) const override;

private:
    GFileSystemModel(const String& root_path, Mode);

    String m_root_path;
    Mode m_mode { Invalid };

    struct Node;
    Node* m_root { nullptr };

    GIcon m_open_folder_icon;
    GIcon m_closed_folder_icon;
    GIcon m_file_icon;
};
