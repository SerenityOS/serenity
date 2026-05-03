/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GroupsTab.h"
#include "Constants.h"
#include "GroupAddDialog.h"
#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <LibCore/Account.h>
#include <LibCore/Group.h>
#include <LibGUI/AbstractView.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>

namespace UsersSettings {

static ErrorOr<bool> can_delete_group(Core::Group const& group)
{
    if (group.id() < MIN_NORMAL_GID)
        return false;

    auto accounts = TRY(Core::Account::all(Core::Account::Read::PasswdOnly));
    for (auto const& account : accounts) {
        if (account.gid() == group.id())
            return false;
    }

    return true;
}

ErrorOr<void> GroupsTab::initialize()
{
    m_groups_list = *find_descendant_of_type_named<GUI::ListView>("groups_list");
    m_add_button = *find_descendant_of_type_named<GUI::Button>("add_button");
    m_delete_button = *find_descendant_of_type_named<GUI::Button>("delete_button");
    m_details_container = *find_descendant_of_type_named<GUI::Widget>("details_container");

    m_no_selection_label = TRY(GUI::Label::try_create());
    m_no_selection_label->set_text("Select a group to view details"_string);
    m_no_selection_label->set_text_alignment(Gfx::TextAlignment::Center);
    m_details_container->add_child(*m_no_selection_label);

    m_delete_button->set_enabled(false);

    TRY(refresh_groups());

    m_groups_list->on_selection_change = [this]() {
        auto index = m_groups_list->selection().first();
        m_delete_button->set_enabled(false);

        m_details_container->remove_all_children();
        m_group_details_widget = nullptr;
        set_modified(false);
        ArmedScopeGuard on_error_path = [this]() {
            m_details_container->add_child(*m_no_selection_label);
        };

        if (!index.is_valid())
            return;

        auto groups = Core::Group::all();
        if (groups.is_error())
            return;

        Optional<Core::Group> selected_group;
        for (auto& group : groups.value()) {
            if (group.name() == m_group_names[index.row()].bytes_as_string_view()) {
                selected_group = group;
                break;
            }
        }
        if (!selected_group.has_value())
            return;

        auto can_delete = can_delete_group(selected_group.value());
        if (can_delete.is_error())
            return;
        m_delete_button->set_enabled(can_delete.release_value());

        auto widget = GroupDetailsWidget::create(selected_group.value());
        if (widget.is_error()) {
            GUI::MessageBox::show_error(window(), "Failed to load group details"sv);
            return;
        }

        on_error_path.disarm();
        m_group_details_widget = widget.release_value();
        m_group_details_widget->on_modified = [this] { set_modified(true); };
        m_details_container->add_child(*m_group_details_widget);
    };

    m_add_button->on_click = [this](auto) {
        if (auto result = add_group(); result.is_error())
            GUI::MessageBox::show_error(window(), "Failed to add group"sv);
    };

    m_delete_button->on_click = [this](auto) {
        auto index = m_groups_list->selection().first();
        if (!index.is_valid())
            return;
        if (auto result = delete_group(m_group_names[index.row()].bytes_as_string_view()); result.is_error())
            GUI::MessageBox::show_error(window(), "Failed to delete group"sv);
    };

    return {};
}

ErrorOr<void> GroupsTab::refresh_groups()
{
    auto groups = TRY(Core::Group::all());
    m_group_names.clear();
    for (auto& group : groups)
        m_group_names.append(TRY(String::from_byte_string(group.name())));
    if (m_groups_list)
        m_groups_list->set_model(*GUI::ItemListModel<String>::create(m_group_names));
    return {};
}

ErrorOr<void> GroupsTab::add_group()
{
    auto group_name = TRY(GroupAddDialog::show(window()));
    if (!group_name.has_value())
        return {};

    TRY(refresh_groups());

    // Select the newly created group.
    auto it = m_group_names.find_if([&group_name](auto const& name) { return name == group_name.value(); });
    if (!it.is_end()) {
        auto index = m_groups_list->model()->index((int)it.index());
        m_groups_list->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
    }
    return {};
}

ErrorOr<void> GroupsTab::delete_group(StringView group_name)
{
    auto groups = TRY(Core::Group::all());
    Optional<Core::Group> target;
    for (auto& group : groups) {
        if (group.name() == group_name) {
            target = group;
            break;
        }
    }
    if (!target.has_value() || !TRY(can_delete_group(target.value())))
        return {};

    auto result = GUI::MessageBox::show(window(),
        TRY(String::formatted("Are you sure you want to delete group \"{}\"?", group_name)),
        "Confirm Deletion"sv,
        GUI::MessageBox::Type::Warning,
        GUI::MessageBox::InputType::YesNo);
    if (result != GUI::MessageBox::ExecResult::Yes)
        return {};

    TRY(Core::Group::delete_group(group_name));

    m_details_container->remove_all_children();
    m_group_details_widget = nullptr;
    m_details_container->add_child(*m_no_selection_label);
    TRY(refresh_groups());
    return {};
}

void GroupsTab::apply_settings()
{
    if (!m_group_details_widget)
        return;

    auto new_name = m_group_details_widget->current_name();
    if (auto result = m_group_details_widget->apply_changes(); result.is_error()) {
        GUI::MessageBox::show_error(window(), MUST(String::formatted("Failed to apply settings: {}", result.error())));
        return;
    }

    if (auto result = refresh_groups(); result.is_error())
        return;

    auto it = m_group_names.find_if([&new_name](auto const& name) { return name.bytes_as_string_view() == new_name; });
    if (!it.is_end()) {
        auto index = m_groups_list->model()->index((int)it.index());
        m_groups_list->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
    }
}

}
