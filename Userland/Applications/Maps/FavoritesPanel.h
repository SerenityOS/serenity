/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "MapWidget.h"
#include <LibGUI/Dialog.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/ListView.h>

namespace Maps {

class FavoritesPanel final : public GUI::Widget {
    C_OBJECT(FavoritesPanel)

public:
    struct Favorite {
        String name;
        MapWidget::LatLng latlng;
        int zoom;
    };
    static ErrorOr<NonnullRefPtr<FavoritesPanel>> try_create();
    ErrorOr<void> initialize();

    void load_favorites();
    void reset();
    ErrorOr<void> add_favorite(Favorite const& favorite);

    Function<void(Vector<Favorite> const&)> on_favorites_change;
    Function<void(Favorite const&)> on_selected_favorite_change;

protected:
    FavoritesPanel() = default;

private:
    ErrorOr<void> edit_favorite(int row);
    void favorites_changed();

    RefPtr<GUI::Frame> m_empty_container;
    RefPtr<GUI::ListView> m_favorites_list;
    RefPtr<GUI::Menu> m_context_menu;
};

}
