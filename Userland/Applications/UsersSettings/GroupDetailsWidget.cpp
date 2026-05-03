/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GroupDetailsWidget.h"
#include "AddMemberDialog.h"
#include "Constants.h"
#include <AK/HashTable.h>
#include <AK/String.h>
#include <LibCore/Account.h>
#include <LibCore/System.h>
#include <LibGUI/Button.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/MessageBox.h>

namespace UsersSettings {

ErrorOr<NonnullRefPtr<GroupDetailsWidget>> GroupDetailsWidget::create(Core::Group const& group)
{
    auto widget = TRY(try_create());
    TRY(widget->initialize(group));
    return widget;
}

ErrorOr<void> GroupDetailsWidget::initialize(Core::Group const& group)
{
    m_group = group;

    m_group_name_textbox = find_descendant_of_type_named<GUI::TextBox>("group_name_textbox");
    m_gid_textbox = find_descendant_of_type_named<GUI::TextBox>("gid_textbox");
    m_members_list = find_descendant_of_type_named<GUI::ListView>("members_list");
    m_add_member_button = find_descendant_of_type_named<GUI::Button>("add_member_button");
    m_remove_member_button = find_descendant_of_type_named<GUI::Button>("remove_member_button");

    m_group_name_textbox->set_text(group.name());
    m_group_name_textbox->on_change = [this] {
        if (on_modified)
            on_modified();
    };
    m_gid_textbox->set_text(ByteString::formatted("{}", group.id()));

    TRY(refresh_members());

    m_remove_member_button->set_enabled(false);

    m_members_list->on_selection_change = [this]() {
        auto index = m_members_list->selection().first();
        m_remove_member_button->set_enabled(index.is_valid() && index.row() < (int)m_members.size() && !m_members[index.row()].is_primary_group_member);
    };

    m_add_member_button->on_click = [this](auto) {
        if (auto result = add_member(); result.is_error())
            GUI::MessageBox::show_error(window(), "Failed to add member"sv);
    };

    m_remove_member_button->on_click = [this](auto) {
        remove_member();
    };

    return {};
}

ErrorOr<void> GroupDetailsWidget::refresh_members()
{
    VERIFY(m_group.has_value());
    auto accounts = TRY(Core::Account::all(Core::Account::Read::PasswdOnly));
    HashTable<ByteString> primary_group_members;
    for (auto const& account : accounts) {
        if (account.gid() == m_group->id())
            primary_group_members.set(account.username());
    }

    m_members.clear();
    m_member_names.clear();
    for (auto const& member : m_group->members()) {
        auto username = TRY(String::from_byte_string(member));
        m_members.append({ username, primary_group_members.contains(member) });
        m_member_names.append(move(username));
    }

    for (auto const& account : accounts) {
        if (account.gid() != m_group->id())
            continue;

        auto username = TRY(String::from_byte_string(account.username()));
        bool already_listed = m_members.find_if([&](auto const& member) {
            return member.username == username;
        }) != m_members.end();
        if (already_listed)
            continue;

        m_members.append({ username, true });
        m_member_names.append(move(username));
    }

    if (m_members_list)
        m_members_list->set_model(*GUI::ItemListModel<String>::create(m_member_names));
    if (m_remove_member_button)
        m_remove_member_button->set_enabled(false);
    return {};
}

ErrorOr<void> GroupDetailsWidget::add_member()
{
    VERIFY(m_group.has_value());

    auto accounts = TRY(Core::Account::all(Core::Account::Read::PasswdOnly));
    Vector<String> available_usernames;
    for (auto const& account : accounts) {
        if (account.uid() < MIN_NORMAL_UID)
            continue;
        if (account.gid() == m_group->id())
            continue;
        if (!m_group->members().contains_slow(account.username()))
            available_usernames.append(TRY(String::from_byte_string(account.username())));
    }

    if (available_usernames.is_empty()) {
        GUI::MessageBox::show(window(), "All users are already members of this group."sv,
            "No Users Available"sv, GUI::MessageBox::Type::Information);
        return {};
    }

    auto selected = TRY(AddMemberDialog::show(window(), available_usernames));
    if (!selected.has_value())
        return {};

    m_group->members().append(selected->to_byte_string());
    TRY(refresh_members());
    if (on_modified)
        on_modified();
    return {};
}

void GroupDetailsWidget::remove_member()
{
    VERIFY(m_group.has_value());

    auto index = m_members_list->selection().first();
    if (!index.is_valid())
        return;

    VERIFY(index.row() < (int)m_members.size());
    auto const& member = m_members[index.row()];
    if (member.is_primary_group_member) {
        GUI::MessageBox::show_error(window(), MUST(String::formatted("Cannot remove \"{}\" from its primary group.", member.username)));
        return;
    }

    m_group->members().remove_all_matching([&](auto const& username) {
        return username == member.username.bytes_as_string_view();
    });
    MUST(refresh_members());
    if (on_modified)
        on_modified();
}

ErrorOr<void> GroupDetailsWidget::apply_changes()
{
    VERIFY(m_group.has_value());

    auto new_name = m_group_name_textbox->text();
    if (new_name != m_group->name()) {
        TRY(Core::Group::validate_name(new_name));

        if (m_group->id() < MIN_NORMAL_GID)
            return Error::from_string_literal("Cannot rename a system group.");

        auto existing = TRY(Core::System::getgrnam(new_name));
        if (existing.has_value())
            return Error::from_string_literal("A group with that name already exists.");

        m_group->set_name(new_name);
    }

    TRY(m_group->sync());
    return {};
}

}
