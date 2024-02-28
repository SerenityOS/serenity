/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "FavoritesModel.h"
#include <LibGUI/ListView.h>

namespace Maps {

class FavoritesPanel final : public GUI::Widget {
    C_OBJECT(FavoritesPanel)

public:
    static ErrorOr<NonnullRefPtr<FavoritesPanel>> try_create();
    ErrorOr<void> initialize();

    void load_favorites();
    void reset();
    void add_favorite(Favorite favorite);
    void delete_favorite(Favorite const& favorite);

    Function<void(Vector<Favorite> const&)> on_favorites_change;
    Function<void(Favorite const&)> on_selected_favorite_change;

protected:
    FavoritesPanel() = default;

private:
    ErrorOr<void> edit_favorite(GUI::ModelIndex const& index);
    void favorites_changed();

    RefPtr<GUI::Frame> m_empty_container;
    RefPtr<GUI::ListView> m_favorites_list;
    RefPtr<FavoritesModel> m_model;
    RefPtr<GUI::Menu> m_context_menu;
};

}
