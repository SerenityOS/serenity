/*
 * Copyright (c) 2022, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ContentFilterSettingsWidget.h"

#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <Applications/BrowserSettings/Defaults.h>
#include <LibConfig/Client.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Event.h>
#include <LibGUI/Forward.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>

ErrorOr<String> DomainListModel::filter_list_file_path() const
{
    return String::formatted("{}/BrowserContentFilters.txt", Core::StandardPaths::config_directory());
}

ErrorOr<void> DomainListModel::load()
{
    // FIXME: This should be somewhat shared with Browser.
    auto file = TRY(Core::File::open(TRY(filter_list_file_path()), Core::File::OpenMode::Read));
    auto content_filter_list = TRY(Core::InputBufferedFile::create(move(file)));
    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));

    m_domain_list.clear_with_capacity();

    while (TRY(content_filter_list->can_read_line())) {
        auto line = TRY(content_filter_list->read_line(buffer));
        if (line.is_empty())
            continue;

        auto pattern = TRY(String::from_utf8(line));
        TRY(m_domain_list.try_append(move(pattern)));
    }

    return {};
}

ErrorOr<void> DomainListModel::save()
{
    if (!m_was_modified)
        return {};
    m_was_modified = false;

    StringBuilder builder;
    for (auto const& domain : m_domain_list)
        TRY(builder.try_appendff("{}\n", domain));

    auto file = TRY(Core::File::open(TRY(filter_list_file_path()), Core::File::OpenMode::Write));
    TRY(file->write_until_depleted(TRY(builder.to_byte_buffer()).bytes()));
    return {};
}

void DomainListModel::add_domain(String name)
{
    begin_insert_rows({}, m_domain_list.size(), m_domain_list.size());
    m_domain_list.append(move(name));
    end_insert_rows();
    m_was_modified = true;
    did_update(UpdateFlag::DontInvalidateIndices);
}

void DomainListModel::delete_domain(size_t index)
{
    begin_delete_rows({}, index, index);
    m_domain_list.remove(index);
    end_delete_rows();
    m_was_modified = true;
    did_update(UpdateFlag::DontInvalidateIndices);
}

void DomainListModel::reset_default_values()
{
    // FIXME: This probably should not be hardcoded.
    static constexpr Array default_domain_list {
        "207.net"sv,
        "247realmedia.com"sv,
        "2o7.net"sv,
        "adbrite.com"sv,
        "admob.com"sv,
        "adthis.com"sv,
        "advertising.com"sv,
        "aquantive.com"sv,
        "atwola.com"sv,
        "channelintelligence.com"sv,
        "doubleclick.com"sv,
        "doubleclick.net"sv,
        "esomniture.com"sv,
        "google-analytics.com"sv,
        "googleadservices.com"sv,
        "googlesyndication.com"sv,
        "gravity.com"sv,
        "hitbox.com"sv,
        "intellitxt.com"sv,
        "nielsen-online.com"sv,
        "omniture.com"sv,
        "quantcast.com"sv,
        "quantserve.com"sv,
        "scorecardresearch.com"sv,
    };

    m_domain_list.clear_with_capacity();
    for (auto domain : default_domain_list)
        m_domain_list.append(String::from_utf8(domain).release_value_but_fixme_should_propagate_errors());

    m_was_modified = true;
    did_update(UpdateFlag::InvalidateAllIndices);
}

namespace BrowserSettings {

ErrorOr<void> ContentFilterSettingsWidget::initialize()
{
    auto domain_list_model = TRY(try_make_ref_counted<DomainListModel>());
    TRY(domain_list_model->load());

    set_domain_list_model(move(domain_list_model));

    m_enable_content_filtering_checkbox = find_descendant_of_type_named<GUI::CheckBox>("enable_content_filtering_checkbox");
    m_enable_content_filtering_checkbox->set_checked(Config::read_bool("Browser"sv, "Preferences"sv, "EnableContentFilters"sv, Browser::default_enable_content_filters), GUI::AllowCallback::No);
    m_enable_content_filtering_checkbox->on_checked = [this](auto) {
        set_modified(true);
    };

    m_domain_list_view = find_descendant_of_type_named<GUI::ListView>("domain_list_view");
    m_domain_list_view->set_model(m_domain_list_model);
    m_domain_list_view->on_context_menu_request = [this](GUI::ModelIndex const& index, GUI::ContextMenuEvent const& event) {
        m_domain_list_view->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
        m_entry_context_menu->popup(event.screen_position());
    };

    m_add_new_domain_button = find_descendant_of_type_named<GUI::Button>("add_new_domain_button");
    m_add_new_domain_button->on_click = [this](unsigned) {
        String text;

        if (GUI::InputBox::show(window(), text, "Enter a domain:"sv, "Add Content Filter"sv, GUI::InputType::NonemptyText) == GUI::Dialog::ExecResult::OK) {
            m_domain_list_model->add_domain(move(text));
            set_modified(true);
        }
    };

    auto delete_action = GUI::CommonActions::make_delete_action([this](GUI::Action const&) {
        if (!m_domain_list_view->selection().is_empty()) {
            m_domain_list_model->delete_domain(m_domain_list_view->selection().first().row());
            set_modified(true);
        }
    });
    m_entry_context_menu = GUI::Menu::construct();
    m_entry_context_menu->add_action(delete_action);

    return {};
}

void ContentFilterSettingsWidget::set_domain_list_model(NonnullRefPtr<DomainListModel> domain_list_model)
{
    m_domain_list_model = move(domain_list_model);
}

void ContentFilterSettingsWidget::apply_settings()
{
    m_domain_list_model->save().release_value_but_fixme_should_propagate_errors();
    Config::write_bool("Browser"sv, "Preferences"sv, "EnableContentFilters"sv, m_enable_content_filtering_checkbox->is_checked());
}

void ContentFilterSettingsWidget::reset_default_values()
{
    m_domain_list_model->reset_default_values();
    m_enable_content_filtering_checkbox->set_checked(Browser::default_enable_content_filters);
}

}
