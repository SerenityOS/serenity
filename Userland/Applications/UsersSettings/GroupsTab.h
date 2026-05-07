/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GroupDetailsWidget.h"
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/ListView.h>
#include <LibGUI/SettingsWindow.h>

namespace UsersSettings {

class GroupsTab final : public GUI::SettingsWindow::Tab {
    C_OBJECT(GroupsTab)

public:
    static ErrorOr<NonnullRefPtr<GroupsTab>> try_create();

    virtual void apply_settings() override;

    ErrorOr<void> initialize();

private:
    ErrorOr<void> refresh_groups();
    ErrorOr<void> add_group();
    ErrorOr<void> delete_group(StringView group_name);

    RefPtr<GUI::ListView> m_groups_list;
    Vector<String> m_group_names;
    RefPtr<GUI::Button> m_add_button;
    RefPtr<GUI::Button> m_delete_button;
    RefPtr<GUI::Widget> m_details_container;
    RefPtr<GUI::Label> m_no_selection_label;
    RefPtr<GroupDetailsWidget> m_group_details_widget;
};

}
