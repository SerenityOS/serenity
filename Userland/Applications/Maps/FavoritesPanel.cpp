/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FavoritesPanel.h"
#include "FavoritesEditDialog.h"
#include <LibCore/StandardPaths.h>
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>

namespace Maps {

ErrorOr<void> FavoritesPanel::initialize()
{
    m_empty_container = *find_descendant_of_type_named<GUI::Frame>("empty_container");
    m_favorites_list = *find_descendant_of_type_named<GUI::ListView>("favorites_list");

    m_model = FavoritesModel::create();
    m_favorites_list->set_model(m_model);
    m_favorites_list->set_item_height(m_favorites_list->font().preferred_line_height() * 2 + m_favorites_list->vertical_padding());
    m_favorites_list->on_selection_change = [this]() {
        if (auto favorite = m_model->get_favorite(m_favorites_list->selection().first()); favorite.has_value())
            on_selected_favorite_change(*favorite);
    };

    m_favorites_list->on_context_menu_request = [this](auto const& index, auto const& event) {
        m_context_menu = GUI::Menu::construct();
        m_context_menu->add_action(GUI::Action::create(
            "&Edit...", MUST(Gfx::Bitmap::load_from_file("/res/icons/16x16/rename.png"sv)), [this, index](auto&) {
                MUST(edit_favorite(index));
            },
            this));
        m_context_menu->add_action(GUI::CommonActions::make_delete_action(
            [this, index](auto&) {
                if (auto favorite = m_model->get_favorite(index); favorite.has_value()) {
                    m_model->delete_favorite(*favorite);
                    favorites_changed();
                }
            },
            this));
        m_context_menu->popup(event.screen_position());
    };
    return {};
}

void FavoritesPanel::load_favorites()
{
    if (auto maybe_file = Core::File::open(MUST(String::formatted("{}/MapsFavorites.json", Core::StandardPaths::config_directory())), Core::File::OpenMode::Read); !maybe_file.is_error()) {
        auto file = maybe_file.release_value();
        (void)m_model->load_from_file(*file);
    }
    favorites_changed();
}

void FavoritesPanel::reset()
{
    m_favorites_list->selection().clear();
    m_favorites_list->scroll_to_top();
}

void FavoritesPanel::add_favorite(Favorite favorite)
{
    m_model->add_favorite(move(favorite));
    favorites_changed();
}

void FavoritesPanel::delete_favorite(Favorite const& favorite)
{
    m_model->delete_favorite(favorite);
    favorites_changed();
}

ErrorOr<void> FavoritesPanel::edit_favorite(GUI::ModelIndex const& index)
{
    auto favorite = m_model->get_favorite(index);
    if (!favorite.has_value())
        return {};

    auto edit_dialog = TRY(GUI::Dialog::try_create(window()));
    edit_dialog->set_title("Edit Favorite");
    edit_dialog->resize(260, 61);
    edit_dialog->set_resizable(false);

    auto widget = TRY(Maps::FavoritesEditDialog::try_create());
    edit_dialog->set_main_widget(widget);

    auto& name_textbox = *widget->find_descendant_of_type_named<GUI::TextBox>("name_textbox");
    name_textbox.set_text(favorite->name);
    name_textbox.set_focus(true);
    name_textbox.select_all();

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [&](auto) {
        favorite->name = MUST(String::from_byte_string(name_textbox.text()));
        m_model->update_favorite(index, *favorite);
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
    // Update UI
    m_empty_container->set_visible(m_model->row_count() == 0);
    m_favorites_list->set_visible(m_model->row_count() > 0);
    on_favorites_change(m_model->favorites());

    // Save favorites
    if (auto maybe_file = Core::File::open(MUST(String::formatted("{}/MapsFavorites.json", Core::StandardPaths::config_directory())), Core::File::OpenMode::Write); !maybe_file.is_error()) {
        auto file = maybe_file.release_value();
        MUST(m_model->save_to_file(*file));
    }
}

}
