/*
 * Copyright (c) 2024, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <LibMaps/MapWidget.h>

namespace Maps {

struct Favorite {
    String name;
    MapWidget::LatLng latlng;
    int zoom;

    bool operator==(Favorite const& other) const = default;
};

class FavoritesModel final : public GUI::Model {
public:
    static NonnullRefPtr<FavoritesModel> create()
    {
        return adopt_ref(*new FavoritesModel());
    }

    virtual int row_count(GUI::ModelIndex const& index = GUI::ModelIndex()) const override
    {
        if (!index.is_valid())
            return m_favorites.size();
        return 0;
    }

    virtual int column_count(GUI::ModelIndex const&) const override { return 1; }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role = GUI::ModelRole::Display) const override;

    Vector<Favorite> const& favorites() const { return m_favorites; }
    Optional<Favorite&> get_favorite(GUI::ModelIndex const&);
    void add_favorite(Favorite);
    void update_favorite(GUI::ModelIndex const&, Favorite);
    void delete_favorite(Favorite const&);

    ErrorOr<void> save_to_file(Core::File&) const;
    ErrorOr<void> load_from_file(Core::File&);

private:
    Vector<Favorite> m_favorites;
};

}
