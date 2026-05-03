/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <LibCore/Group.h>
#include <LibGUI/Button.h>
#include <LibGUI/ListView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>

namespace UsersSettings {

class GroupDetailsWidget final : public GUI::Widget {
    C_OBJECT(GroupDetailsWidget)

public:
    static ErrorOr<NonnullRefPtr<GroupDetailsWidget>> create(Core::Group const& group);
    static ErrorOr<NonnullRefPtr<GroupDetailsWidget>> try_create();

    ErrorOr<void> apply_changes();

    ByteString current_name() const { return m_group_name_textbox->text(); }

    Function<void()> on_modified;

private:
    struct MemberEntry {
        String username;
        bool is_primary_group_member { false };
    };

    GroupDetailsWidget() = default;

    ErrorOr<void> initialize(Core::Group const& group);
    ErrorOr<void> refresh_members();
    ErrorOr<void> add_member();
    void remove_member();

    Optional<Core::Group> m_group;
    RefPtr<GUI::TextBox> m_group_name_textbox;
    RefPtr<GUI::TextBox> m_gid_textbox;
    RefPtr<GUI::ListView> m_members_list;
    Vector<MemberEntry> m_members;
    Vector<String> m_member_names;
    RefPtr<GUI::Button> m_add_member_button;
    RefPtr<GUI::Button> m_remove_member_button;
};

}
