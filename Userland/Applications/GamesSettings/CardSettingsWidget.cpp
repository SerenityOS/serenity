/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CardSettingsWidget.h"
#include <Applications/GamesSettings/CardSettingsWidgetGML.h>
#include <LibCards/CardPainter.h>
#include <LibConfig/Client.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGfx/Palette.h>

static constexpr StringView default_card_back_image_path = "/res/icons/cards/buggie-deck.png"sv;

CardSettingsWidget::CardSettingsWidget()
{
    load_from_gml(card_settings_widget_gml);

    auto background_color = Gfx::Color::from_string(Config::read_string("Games"sv, "Cards"sv, "BackgroundColor"sv)).value_or(Gfx::Color::from_rgb(0x008000));

    m_preview_frame = find_descendant_of_type_named<GUI::Frame>("cards_preview");
    set_cards_background_color(background_color);

    m_preview_card_back = find_descendant_of_type_named<GUI::ImageWidget>("cards_preview_card_back");
    m_preview_card_back->set_bitmap(Cards::CardPainter::the().card_back());

    m_preview_card_front_ace = find_descendant_of_type_named<GUI::ImageWidget>("cards_preview_card_front_ace");
    m_preview_card_front_ace->set_bitmap(Cards::CardPainter::the().card_front(Cards::Suit::Spades, Cards::Rank::Ace));
    m_preview_card_front_queen = find_descendant_of_type_named<GUI::ImageWidget>("cards_preview_card_front_queen");
    m_preview_card_front_queen->set_bitmap(Cards::CardPainter::the().card_front(Cards::Suit::Hearts, Cards::Rank::Queen));

    m_background_color_input = find_descendant_of_type_named<GUI::ColorInput>("cards_background_color");
    m_background_color_input->set_color(background_color, GUI::AllowCallback::No);
    m_background_color_input->on_change = [&]() {
        set_modified(true);
        set_cards_background_color(m_background_color_input->color());
    };

    m_card_back_image_view = find_descendant_of_type_named<GUI::IconView>("cards_back_image");
    m_card_back_image_view->set_model(GUI::FileSystemModel::create("/res/icons/cards"));
    m_card_back_image_view->set_model_column(GUI::FileSystemModel::Column::Name);
    if (!set_card_back_image_path(Config::read_string("Games"sv, "Cards"sv, "CardBackImage"sv)))
        set_card_back_image_path(default_card_back_image_path);
    m_card_back_image_view->on_selection_change = [&]() {
        if (m_card_back_image_view->selection().is_empty())
            return;
        set_modified(true);
        Cards::CardPainter::the().set_background_image_path(card_back_image_path());
        m_preview_card_back->update();
    };
}

void CardSettingsWidget::apply_settings()
{
    Config::write_string("Games"sv, "Cards"sv, "BackgroundColor"sv, m_background_color_input->text());
    Config::write_string("Games"sv, "Cards"sv, "CardBackImage"sv, card_back_image_path());
}

void CardSettingsWidget::reset_default_values()
{
    m_background_color_input->set_color(Gfx::Color::from_rgb(0x008000));
    set_card_back_image_path(default_card_back_image_path);
}

void CardSettingsWidget::set_cards_background_color(Gfx::Color color)
{
    auto new_palette = m_preview_frame->palette();
    new_palette.set_color(Gfx::ColorRole::Background, color);
    m_preview_frame->set_palette(new_palette);
}

bool CardSettingsWidget::set_card_back_image_path(String const& path)
{
    auto index = static_cast<GUI::FileSystemModel*>(m_card_back_image_view->model())->index(path, m_card_back_image_view->model_column());
    if (index.is_valid()) {
        m_card_back_image_view->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
        Cards::CardPainter::the().set_background_image_path(path);
        m_preview_card_back->update();
        return true;
    }
    return false;
}

String CardSettingsWidget::card_back_image_path() const
{
    auto card_back_image_index = m_card_back_image_view->selection().first();
    return static_cast<GUI::FileSystemModel const*>(m_card_back_image_view->model())->full_path(card_back_image_index);
}
