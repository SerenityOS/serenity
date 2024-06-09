/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SearchPanel.h"
#include <AK/JsonParser.h>

namespace Maps {

ErrorOr<void> SearchPanel::initialize()
{
    m_request_client = TRY(Protocol::RequestClient::try_create());

    m_search_textbox = *find_descendant_of_type_named<GUI::TextBox>("search_textbox");
    m_search_button = *find_descendant_of_type_named<GUI::Button>("search_button");
    m_start_container = *find_descendant_of_type_named<GUI::Frame>("start_container");
    m_empty_container = *find_descendant_of_type_named<GUI::Frame>("empty_container");
    m_places_list = *find_descendant_of_type_named<GUI::ListView>("places_list");

    m_empty_container->set_visible(false);
    m_places_list->set_visible(false);

    m_search_textbox->on_return_pressed = [this]() {
        search(MUST(String::from_byte_string(m_search_textbox->text())));
    };
    m_search_button->on_click = [this](unsigned) {
        search(MUST(String::from_byte_string(m_search_textbox->text())));
    };

    m_places_list->set_item_height(m_places_list->font().preferred_line_height() * 2 + m_places_list->vertical_padding());
    m_places_list->on_selection_change = [this]() {
        auto const& index = m_places_list->selection().first();
        if (!index.is_valid())
            return;
        on_selected_place_change(m_places.at(index.row()));
    };

    return {};
}

void SearchPanel::search(StringView query)
{
    // Show start container when empty query
    if (query.is_empty()) {
        m_start_container->set_visible(true);
        m_empty_container->set_visible(false);
        m_places_list->set_visible(false);
        return;
    }
    m_start_container->set_visible(false);

    // Start HTTP GET request to load people.json
    HTTP::HeaderMap headers;
    headers.set("User-Agent", "SerenityOS Maps");
    headers.set("Accept", "application/json");
    URL::URL url(MUST(String::formatted("https://nominatim.openstreetmap.org/search?q={}&format=json", URL::percent_encode(query, URL::PercentEncodeSet::Query))));
    auto request = m_request_client->start_request("GET", url, headers, {});
    VERIFY(!request.is_null());
    m_request = request;

    request->set_buffered_request_finished_callback([this, request, url](bool success, auto, auto&, auto, ReadonlyBytes payload) {
        m_request.clear();
        if (!success) {
            dbgln("Maps: Can't load: {}", url);
            return;
        }

        // Parse JSON data
        JsonParser parser(payload);
        auto result = parser.parse();
        if (result.is_error()) {
            dbgln("Maps: Can't parse JSON: {}", url);
            return;
        }

        // Show empty label when no places are found
        auto json_places = result.release_value().as_array();
        if (json_places.size() == 0) {
            m_empty_container->set_visible(true);
            m_places_list->set_visible(false);
            return;
        }

        // Parse places from JSON data
        m_places.clear();
        m_places_names.clear();
        for (size_t i = 0; i < json_places.size(); i++) {
            // FIXME: Handle JSON parsing errors
            auto const& json_place = json_places.at(i).as_object();

            MapWidget::LatLng latlng = { json_place.get_byte_string("lat"sv).release_value().to_number<double>().release_value(),
                json_place.get_byte_string("lon"sv).release_value().to_number<double>().release_value() };
            String name = MUST(String::formatted("{}\n{:.5}, {:.5}", json_place.get_byte_string("display_name"sv).release_value(), latlng.latitude, latlng.longitude));

            // Calculate the right zoom level for bounding box
            auto const& json_boundingbox = json_place.get_array("boundingbox"sv);
            MapWidget::LatLngBounds bounds = {
                { json_boundingbox->at(0).as_string().to_number<double>().release_value(),
                    json_boundingbox->at(2).as_string().to_number<double>().release_value() },
                { json_boundingbox->at(1).as_string().to_number<double>().release_value(),
                    json_boundingbox->at(3).as_string().to_number<double>().release_value() }
            };

            m_places.append({ name, latlng, bounds.get_zoom() });
            m_places_names.append(name);
        }
        on_places_change(m_places);

        // Update and show places list
        m_empty_container->set_visible(false);
        m_places_list->set_model(*GUI::ItemListModel<String>::create(m_places_names));
        m_places_list->set_visible(true);
    });

    request->on_certificate_requested = []() -> Protocol::Request::CertificateAndKey { return {}; };
}

void SearchPanel::reset()
{
    m_search_textbox->set_text(""sv);
    search(""sv);
}

}
