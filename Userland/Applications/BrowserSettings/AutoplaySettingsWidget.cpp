/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AutoplaySettingsWidget.h"
#include <Applications/BrowserSettings/Defaults.h>
#include <LibConfig/Client.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Event.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>

ErrorOr<String> AutoplayAllowlistModel::filter_list_file_path() const
{
    return String::formatted("{}/BrowserAutoplayAllowlist.txt", Core::StandardPaths::config_directory());
}

void AutoplayAllowlistModel::reset_default_values()
{
    m_domain_list = {};
    m_was_modified = true;
    did_update(UpdateFlag::InvalidateAllIndices);
}

namespace BrowserSettings {

ErrorOr<void> AutoplaySettingsWidget::initialize()
{
    auto allowlist_model = TRY(try_make_ref_counted<AutoplayAllowlistModel>());
    TRY(allowlist_model->load());

    set_allowlist_model(move(allowlist_model));

    m_allow_autoplay_on_all_websites_checkbox = find_descendant_of_type_named<GUI::CheckBox>("allow_autoplay_on_all_websites_checkbox");
    m_allow_autoplay_on_all_websites_checkbox->set_checked(Config::read_bool("Browser"sv, "Preferences"sv, "AllowAutoplayOnAllWebsites"sv, Browser::default_allow_autoplay_on_all_websites), GUI::AllowCallback::No);
    m_allow_autoplay_on_all_websites_checkbox->on_checked = [this](auto) {
        set_modified(true);
    };

    m_allowlist_view = find_descendant_of_type_named<GUI::ListView>("allowlist_view");
    m_allowlist_view->set_model(m_allowlist_model);
    m_allowlist_view->on_context_menu_request = [this](GUI::ModelIndex const& index, GUI::ContextMenuEvent const& event) {
        m_allowlist_view->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
        m_entry_context_menu->popup(event.screen_position());
    };

    m_add_website_button = find_descendant_of_type_named<GUI::Button>("add_website_button");
    m_add_website_button->on_click = [this](unsigned) {
        String text;

        if (GUI::InputBox::show(window(), text, "Enter a website:"sv, "Add Autoplay Entry"sv, GUI::InputType::NonemptyText) == GUI::Dialog::ExecResult::OK) {
            m_allowlist_model->add_domain(move(text));
            set_modified(true);
        }
    };

    auto delete_action = GUI::CommonActions::make_delete_action([this](GUI::Action const&) {
        if (!m_allowlist_view->selection().is_empty()) {
            m_allowlist_model->delete_domain(m_allowlist_view->selection().first().row());
            set_modified(true);
        }
    });
    m_entry_context_menu = GUI::Menu::construct();
    m_entry_context_menu->add_action(move(delete_action));

    return {};
}

void AutoplaySettingsWidget::set_allowlist_model(NonnullRefPtr<AutoplayAllowlistModel> model)
{
    m_allowlist_model = model;
}

void AutoplaySettingsWidget::apply_settings()
{
    m_allowlist_model->save().release_value_but_fixme_should_propagate_errors();
    Config::write_bool("Browser"sv, "Preferences"sv, "AllowAutoplayOnAllWebsites"sv, m_allow_autoplay_on_all_websites_checkbox->is_checked());
}

void AutoplaySettingsWidget::reset_default_values()
{
    m_allowlist_model->reset_default_values();
    m_allow_autoplay_on_all_websites_checkbox->set_checked(Browser::default_allow_autoplay_on_all_websites);
}

}
