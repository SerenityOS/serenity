/*
 * Copyright (c) 2022, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ContentFilterSettingsWidget.h"

#include <AK/NonnullRefPtr.h>
#include <Applications/BrowserSettings/ContentFilterSettingsWidgetGML.h>
#include <LibConfig/Client.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/Stream.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Event.h>
#include <LibGUI/Forward.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>

static String filter_list_file_path()
{
    return String::formatted("{}/BrowserContentFilters.txt", Core::StandardPaths::config_directory());
}

ErrorOr<void> DomainListModel::load()
{
    // FIXME: This should be somewhat shared with Browser.
    auto file = TRY(Core::Stream::File::open(filter_list_file_path(), Core::Stream::OpenMode::Read));
    auto content_filter_list = TRY(Core::Stream::BufferedFile::create(move(file)));
    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));
    while (TRY(content_filter_list->can_read_line())) {
        auto length = TRY(content_filter_list->read_line(buffer));
        StringView line { buffer.data(), length };
        dbgln("Content filter for {}", line);
        if (!line.is_empty())
            m_domain_list.append(line);
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

    auto file = TRY(Core::Stream::File::open(filter_list_file_path(), Core::Stream::OpenMode::Write));
    TRY(file->write(builder.to_byte_buffer().bytes()));
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
    m_domain_list = {
        "207.net",
        "247realmedia.com",
        "2o7.net",
        "adbrite.com",
        "admob.com",
        "adthis.com",
        "advertising.com",
        "aquantive.com",
        "atwola.com",
        "channelintelligence.com",
        "doubleclick.com",
        "doubleclick.net",
        "esomniture.com",
        "google-analytics.com",
        "googleadservices.com",
        "googlesyndication.com",
        "gravity.com",
        "hitbox.com",
        "intellitxt.com",
        "nielsen-online.com",
        "omniture.com",
        "quantcast.com",
        "quantserve.com",
        "scorecardresearch.com",
    };
    m_was_modified = true;
    did_update(UpdateFlag::InvalidateAllIndices);
}

ContentFilterSettingsWidget::ContentFilterSettingsWidget()
{
    load_from_gml(content_filter_settings_widget_gml);
    m_enable_content_filtering_checkbox = find_descendant_of_type_named<GUI::CheckBox>("enable_content_filtering_checkbox");
    m_domain_list_view = find_descendant_of_type_named<GUI::ListView>("domain_list_view");
    m_add_new_domain_button = find_descendant_of_type_named<GUI::Button>("add_new_domain_button");

    m_enable_content_filtering_checkbox->set_checked(Config::read_bool("Browser", "Preferences", "EnableContentFilters"));

    m_add_new_domain_button->on_click = [&](unsigned) {
        String text;
        if (GUI::InputBox::show(window(), text, "Enter domain name", "Add domain to Content Filter") == GUI::Dialog::ExecOK)
            m_domain_list_model->add_domain(std::move(text));
    };

    m_domain_list_model = make_ref_counted<DomainListModel>();
    // FIXME: Propagate errors
    MUST(m_domain_list_model->load());
    m_domain_list_view->set_model(m_domain_list_model);

    auto delete_action = GUI::CommonActions::make_delete_action([&](GUI::Action const&) {
        if (!m_domain_list_view->selection().is_empty())
            m_domain_list_model->delete_domain(m_domain_list_view->selection().first().row());
    });
    m_entry_context_menu = GUI::Menu::construct();
    m_entry_context_menu->add_action(delete_action);

    m_domain_list_view->on_context_menu_request = [&](GUI::ModelIndex const& index, GUI::ContextMenuEvent const& event) {
        m_domain_list_view->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
        m_entry_context_menu->popup(event.screen_position());
    };
}

void ContentFilterSettingsWidget::apply_settings()
{
    // FIXME: Propagate errors
    MUST(m_domain_list_model->save());
    Config::write_bool("Browser", "Preferences", "EnableContentFilters", m_enable_content_filtering_checkbox->is_checked());
}

void ContentFilterSettingsWidget::reset_default_values()
{
    m_domain_list_model->reset_default_values();
}
