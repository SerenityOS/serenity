/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/FileSystemPath.h>
#include <LibCore/CFile.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GDialog.h>
#include <LibGUI/GFileSystemModel.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GTextBox.h>

class PropertiesDialog final : public GDialog {
    C_OBJECT(PropertiesDialog)
public:
    virtual ~PropertiesDialog() override;

private:
    PropertiesDialog(GFileSystemModel&, String, bool disable_rename, Core::Object* parent = nullptr);

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

    GFileSystemModel& m_model;
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
