/*
 * Copyright (c) 2024, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FavoritesModel.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>

namespace Maps {

GUI::Variant FavoritesModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (index.row() < 0 || index.row() >= row_count())
        return {};

    if (role == GUI::ModelRole::TextAlignment)
        return Gfx::TextAlignment::CenterLeft;

    auto const& favorite = m_favorites.at(index.row());
    if (role == GUI::ModelRole::Display)
        return ByteString::formatted("{}\n{:.5}, {:.5}", favorite.name, favorite.latlng.latitude, favorite.latlng.longitude);

    return {};
}

Optional<Favorite&> FavoritesModel::get_favorite(GUI::ModelIndex const& index)
{
    if (index.row() < 0 || index.row() >= row_count())
        return {};
    return m_favorites.at(index.row());
}

void FavoritesModel::add_favorite(Favorite favorite)
{
    m_favorites.append(move(favorite));
    invalidate();
}

void FavoritesModel::update_favorite(GUI::ModelIndex const& index, Favorite favorite)
{
    if (index.row() < 0 || index.row() >= row_count())
        return;
    m_favorites[index.row()] = move(favorite);
    invalidate();
}

void FavoritesModel::delete_favorite(Favorite const& favorite)
{
    m_favorites.remove_first_matching([&](auto& other) {
        return other == favorite;
    });
    invalidate();
}

ErrorOr<void> FavoritesModel::save_to_file(Core::File& file) const
{
    JsonArray array {};
    array.ensure_capacity(m_favorites.size());

    for (auto const& favorite : m_favorites) {
        JsonObject object;
        object.set("name", favorite.name.to_byte_string());
        object.set("latitude", favorite.latlng.latitude);
        object.set("longitude", favorite.latlng.longitude);
        object.set("zoom", favorite.zoom);
        TRY(array.append(object));
    }

    auto json_string = array.to_byte_string();
    TRY(file.write_until_depleted(json_string.bytes()));
    return {};
}

ErrorOr<void> FavoritesModel::load_from_file(Core::File& file)
{
    auto json_bytes = TRY(file.read_until_eof());
    StringView json_string { json_bytes };
    auto json = TRY(JsonValue::from_string(json_string));
    if (!json.is_array())
        return Error::from_string_literal("Failed to read favorites from file: Not a JSON array.");
    auto& json_array = json.as_array();

    Vector<Favorite> new_favorites;
    TRY(new_favorites.try_ensure_capacity(json_array.size()));
    TRY(json_array.try_for_each([&](JsonValue const& json_value) -> ErrorOr<void> {
        if (!json_value.is_object())
            return {};
        auto& json_object = json_value.as_object();

        Favorite favorite;

        auto name = json_object.get_byte_string("name"sv);
        if (!name.has_value())
            return {};
        favorite.name = MUST(String::from_byte_string(*name));

        auto latitude = json_object.get_double_with_precision_loss("latitude"sv);
        if (!latitude.has_value())
            return {};
        auto longitude = json_object.get_double_with_precision_loss("longitude"sv);
        if (!longitude.has_value())
            return {};
        favorite.latlng = { *latitude, *longitude };

        auto zoom = json_object.get_i32("zoom"sv);
        if (!zoom.has_value())
            return {};
        favorite.zoom = *zoom;

        new_favorites.append(favorite);
        return {};
    }));

    m_favorites = move(new_favorites);
    invalidate();
    return {};
}

}
