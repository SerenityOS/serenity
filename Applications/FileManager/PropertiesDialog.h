#pragma once

#include <AK/FileSystemPath.h>
#include <LibCore/CFile.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GDialog.h>
#include <LibGUI/GDirectoryModel.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GTextBox.h>

class PropertiesDialog final : public GDialog {
    C_OBJECT(PropertiesDialog)
public:
    virtual ~PropertiesDialog() override;

private:
    explicit PropertiesDialog(GDirectoryModel&, String, bool disable_rename, CObject* parent = nullptr);

    struct PropertyValuePair {
        String property;
        String value;
    };

    struct PermissionMasks {
        mode_t read;
        mode_t write;
        mode_t execute;
    };

    static const String get_description(const mode_t mode)
    {
        if (S_ISREG(mode))
            return "File";
        if (S_ISDIR(mode))
            return "Directory";
        if (S_ISLNK(mode))
            return "Symbolic link";
        if (S_ISCHR(mode))
            return "Character device";
        if (S_ISBLK(mode))
            return "Block device";
        if (S_ISFIFO(mode))
            return "FIFO (named pipe)";
        if (S_ISSOCK(mode))
            return "Socket";
        if (mode & S_IXUSR)
            return "Executable";

        return "Unknown";
    }

    NonnullRefPtr<GButton> make_button(String, NonnullRefPtr<GWidget>&);
    void make_divider(NonnullRefPtr<GWidget>&);
    void make_property_value_pairs(const Vector<PropertyValuePair>& pairs, NonnullRefPtr<GWidget>& parent);
    void make_permission_checkboxes(NonnullRefPtr<GWidget>& parent, PermissionMasks, String label_string, mode_t mode);
    void permission_changed(mode_t mask, bool set);
    bool apply_changes();
    void update();
    String make_full_path(String name);

    GDirectoryModel& m_model;
    RefPtr<GButton> m_apply_button;
    RefPtr<GTextBox> m_name_box;
    RefPtr<GLabel> m_icon;
    String m_name;
    String m_path;
    mode_t m_mode;
    int m_row;
    bool m_permissions_dirty { false };
    bool m_name_dirty { false };
};
