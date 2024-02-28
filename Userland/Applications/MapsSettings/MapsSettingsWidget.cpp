/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MapsSettingsWidget.h"
#include "Defaults.h"
#include <LibConfig/Client.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>

namespace MapsSettings {

void MapsSettingsWidget::apply_settings()
{
    // Tile Provider
    if (m_is_custom_tile_provider) {
        Config::write_string("Maps"sv, "MapWidget"sv, "TileProviderUrlFormat"sv, m_custom_tile_provider_textbox->text());
        Config::remove_key("Maps"sv, "MapWidget"sv, "TileProviderAttributionText"sv);
        Config::remove_key("Maps"sv, "MapWidget"sv, "TileProviderAttributionUrl"sv);
    } else {
        auto tile_provider_url_format = m_tile_provider_combobox->model()->index(m_tile_provider_combobox->selected_index(), 1).data().to_byte_string();
        Config::write_string("Maps"sv, "MapWidget"sv, "TileProviderUrlFormat"sv, tile_provider_url_format);
        auto tile_provider_attribution_text = m_tile_provider_combobox->model()->index(m_tile_provider_combobox->selected_index(), 2).data().to_byte_string();
        Config::write_string("Maps"sv, "MapWidget"sv, "TileProviderAttributionText"sv, tile_provider_attribution_text);
        auto tile_provider_attribution_url = m_tile_provider_combobox->model()->index(m_tile_provider_combobox->selected_index(), 3).data().to_byte_string();
        Config::write_string("Maps"sv, "MapWidget"sv, "TileProviderAttributionUrl"sv, tile_provider_attribution_url);
    }
}

void MapsSettingsWidget::reset_default_values()
{
    set_tile_provider(Maps::default_tile_provider_url_format);
}

ErrorOr<void> MapsSettingsWidget::initialize()
{
    // Tile Provider
    Vector<GUI::JsonArrayModel::FieldSpec> tile_provider_fields;
    tile_provider_fields.empend("name", "Name"_string, Gfx::TextAlignment::CenterLeft);
    tile_provider_fields.empend("url_format", "URL format"_string, Gfx::TextAlignment::CenterLeft);
    tile_provider_fields.empend("attribution_text", "Attribution text"_string, Gfx::TextAlignment::CenterLeft);
    tile_provider_fields.empend("attribution_url", "Attribution URL"_string, Gfx::TextAlignment::CenterLeft);
    auto tile_providers = GUI::JsonArrayModel::create("/usr/share/Maps/TileProviders.json", move(tile_provider_fields));
    tile_providers->invalidate();

    Vector<JsonValue> custom_tile_provider;
    custom_tile_provider.append("Custom...");
    custom_tile_provider.append("");
    custom_tile_provider.append("");
    custom_tile_provider.append("");
    TRY(tile_providers->add(move(custom_tile_provider)));

    m_tile_provider_combobox = *find_descendant_of_type_named<GUI::ComboBox>("tile_provider_combobox");
    m_tile_provider_combobox->set_model(move(tile_providers));
    m_tile_provider_combobox->set_only_allow_values_from_model(true);

    m_custom_tile_provider_group = *find_descendant_of_type_named<GUI::Widget>("custom_tile_provider_group");

    m_custom_tile_provider_textbox = *find_descendant_of_type_named<GUI::TextBox>("custom_tile_provider_textbox");
    m_custom_tile_provider_textbox->set_placeholder(Maps::default_tile_provider_url_format);
    m_custom_tile_provider_textbox->on_change = [&]() { set_modified(true); };

    m_tile_provider_combobox->on_change = [&](ByteString const&, GUI::ModelIndex const& index) {
        auto tile_provider_url_format = m_tile_provider_combobox->model()->index(index.row(), 1).data().to_byte_string();
        m_is_custom_tile_provider = tile_provider_url_format.is_empty();
        m_custom_tile_provider_group->set_enabled(m_is_custom_tile_provider);
        set_modified(true);
    };
    set_tile_provider(Config::read_string("Maps"sv, "MapWidget"sv, "TileProviderUrlFormat"sv, Maps::default_tile_provider_url_format));

    return {};
}

void MapsSettingsWidget::set_tile_provider(StringView tile_provider_url_format)
{
    bool found = false;
    for (int index = 0; index < m_tile_provider_combobox->model()->row_count(); index++) {
        auto url_format = m_tile_provider_combobox->model()->index(index, 1).data().to_byte_string();
        if (url_format == tile_provider_url_format) {
            m_tile_provider_combobox->set_selected_index(index, GUI::AllowCallback::No);
            found = true;
            break;
        }
    }

    if (!found) {
        m_is_custom_tile_provider = true;
        m_custom_tile_provider_textbox->set_text(tile_provider_url_format, GUI::AllowCallback::No);
        m_tile_provider_combobox->set_selected_index(m_tile_provider_combobox->model()->row_count() - 1, GUI::AllowCallback::No);
        m_custom_tile_provider_group->set_enabled(true);
    } else {
        m_custom_tile_provider_group->set_enabled(false);
    }
}

}
