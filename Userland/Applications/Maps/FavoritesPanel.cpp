/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FavoritesPanel.h"
#include "FavoritesEditDialog.h"
#include <LibCore/StandardPaths.h>
#include <LibGUI/Button.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>

namespace Maps {

ErrorOr<void> FavoritesPanel::initialize()
{
    m_empty_container = *find_descendant_of_type_named<GUI::Frame>("empty_container");
    m_favorites_list = *find_descendant_of_type_named<GUI::ListView>("favorites_list");

    m_favorites_list->set_item_height(m_favorites_list->font().preferred_line_height() * 2 + m_favorites_list->vertical_padding());
    m_favorites_list->on_selection_change = [this]() {
        auto const& index = m_favorites_list->selection().first();
        if (!index.is_valid())
            return;
        auto& model = *m_favorites_list->model();
        on_selected_favorite_change({ MUST(String::from_byte_string(model.index(index.row(), 0).data().to_byte_string())),
            { model.index(index.row(), 1).data().as_double(),
                model.index(index.row(), 2).data().as_double() },
            model.index(index.row(), 3).data().to_i32() });
    };

    m_favorites_list->on_context_menu_request = [this](auto const& index, auto const& event) {
        m_context_menu = GUI::Menu::construct();
        m_context_menu->add_action(GUI::Action::create(
            "&Edit...", MUST(Gfx::Bitmap::load_from_file("/res/icons/16x16/rename.png"sv)), [this, index](auto&) {
                MUST(edit_favorite(index.row()));
            },
            this));
        m_context_menu->add_action(GUI::CommonActions::make_delete_action(
            [this, index](auto&) {
                auto& model = *static_cast<GUI::JsonArrayModel*>(m_favorites_list->model());
                MUST(model.remove(index.row()));
                MUST(model.store());
                favorites_changed();
            },
            this));
        m_context_menu->popup(event.screen_position());
    };
    return {};
}

void FavoritesPanel::load_favorites()
{
    Vector<GUI::JsonArrayModel::FieldSpec> favorites_fields;
    favorites_fields.empend("name", "Name"_string, Gfx::TextAlignment::CenterLeft, [](JsonObject const& object) -> GUI::Variant {
        ByteString name = object.get_byte_string("name"sv).release_value();
        double latitude = object.get_double_with_precision_loss("latitude"sv).release_value();
        double longitude = object.get_double_with_precision_loss("longitude"sv).release_value();
        return ByteString::formatted("{}\n{:.5}, {:.5}", name, latitude, longitude);
    });
    favorites_fields.empend("latitude", "Latitude"_string, Gfx::TextAlignment::CenterLeft, [](JsonObject const& object) -> GUI::Variant {
        return object.get_double_with_precision_loss("latitude"sv).release_value();
    });
    favorites_fields.empend("longitude", "Longitude"_string, Gfx::TextAlignment::CenterLeft, [](JsonObject const& object) -> GUI::Variant {
        return object.get_double_with_precision_loss("longitude"sv).release_value();
    });
    favorites_fields.empend("zoom", "Zoom"_string, Gfx::TextAlignment::CenterLeft);
    m_favorites_list->set_model(*GUI::JsonArrayModel::create(ByteString::formatted("{}/MapsFavorites.json", Core::StandardPaths::config_directory()), move(favorites_fields)));
    m_favorites_list->model()->invalidate();
    favorites_changed();
}

ErrorOr<void> FavoritesPanel::add_favorite(Favorite const& favorite)
{
    auto& model = *static_cast<GUI::JsonArrayModel*>(m_favorites_list->model());
    Vector<JsonValue> favorite_json;
    favorite_json.append(favorite.name.to_byte_string());
    favorite_json.append(favorite.latlng.latitude);
    favorite_json.append(favorite.latlng.longitude);
    favorite_json.append(favorite.zoom);
    TRY(model.add(move(favorite_json)));
    TRY(model.store());
    favorites_changed();
    return {};
}

void FavoritesPanel::reset()
{
    m_favorites_list->selection().clear();
    m_favorites_list->scroll_to_top();
}

ErrorOr<void> FavoritesPanel::edit_favorite(int row)
{
    auto& model = *static_cast<GUI::JsonArrayModel*>(m_favorites_list->model());

    auto edit_dialog = TRY(GUI::Dialog::try_create(window()));
    edit_dialog->set_title("Edit Favorite");
    edit_dialog->resize(260, 61);
    edit_dialog->set_resizable(false);

    auto widget = TRY(Maps::FavoritesEditDialog::try_create());
    edit_dialog->set_main_widget(widget);

    auto& name_textbox = *widget->find_descendant_of_type_named<GUI::TextBox>("name_textbox");
    name_textbox.set_text(model.index(row, 0).data().to_byte_string().split('\n').at(0));
    name_textbox.set_focus(true);
    name_textbox.select_all();

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [&](auto) {
        Vector<JsonValue> favorite_json;
        favorite_json.append(name_textbox.text());
        favorite_json.append(model.index(row, 1).data().as_double());
        favorite_json.append(model.index(row, 2).data().as_double());
        favorite_json.append(model.index(row, 3).data().to_i32());
        MUST(model.set(row, move(favorite_json)));
        MUST(model.store());
        favorites_changed();
        edit_dialog->done(GUI::Dialog::ExecResult::OK);
    };
    ok_button.set_default(true);

    auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.on_click = [edit_dialog](auto) {
        edit_dialog->done(GUI::Dialog::ExecResult::Cancel);
    };

    edit_dialog->exec();
    return {};
}

void FavoritesPanel::favorites_changed()
{
    auto& model = *static_cast<GUI::JsonArrayModel*>(m_favorites_list->model());
    m_empty_container->set_visible(model.row_count() == 0);
    m_favorites_list->set_visible(model.row_count() > 0);

    Vector<Favorite> favorites;
    for (int index = 0; index < model.row_count(); index++) {
        favorites.append({ MUST(String::from_byte_string(model.index(index, 0).data().to_byte_string())),
            { model.index(index, 1).data().as_double(),
                model.index(index, 2).data().as_double() },
            model.index(index, 3).data().to_i32() });
    }
    on_favorites_change(favorites);
}

}
