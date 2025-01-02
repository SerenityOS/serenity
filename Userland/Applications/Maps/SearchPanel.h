/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/Frame.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/ListView.h>
#include <LibGUI/TextBox.h>
#include <LibMaps/MapWidget.h>
#include <LibProtocol/Request.h>
#include <LibProtocol/RequestClient.h>

namespace Maps {

class SearchPanel final : public GUI::Widget {
    C_OBJECT(SearchPanel)

public:
    static ErrorOr<NonnullRefPtr<SearchPanel>> try_create();
    ErrorOr<void> initialize();

    void search(StringView query);
    void reset();

    struct Place {
        String name;
        MapWidget::LatLng latlng;
        int zoom;
    };
    Function<void(Vector<Place> const&)> on_places_change;
    Function<void(Place const&)> on_selected_place_change;

private:
    SearchPanel() = default;

    RefPtr<Protocol::RequestClient> m_request_client;
    RefPtr<Protocol::Request> m_request;
    RefPtr<GUI::TextBox> m_search_textbox;
    RefPtr<GUI::Button> m_search_button;
    RefPtr<GUI::Frame> m_start_container;
    RefPtr<GUI::Frame> m_empty_container;
    RefPtr<GUI::ListView> m_places_list;
    Vector<Place> m_places;
    Vector<String> m_places_names;
};

}
