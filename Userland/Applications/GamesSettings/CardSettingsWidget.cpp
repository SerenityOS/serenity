/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CardSettingsWidget.h"
#include <Applications/GamesSettings/CardSettingsWidgetGML.h>
#include <LibCards/Card.h>
#include <LibCards/CardGame.h>
#include <LibCards/CardPainter.h>
#include <LibCards/CardStack.h>
#include <LibConfig/Client.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGfx/Palette.h>

namespace GamesSettings {

static constexpr StringView default_card_back_image_path = "/res/graphics/cards/backs/buggie-deck.png"sv;

class CardGamePreview final : public Cards::CardGame {
    C_OBJECT_ABSTRACT(CardGamePreview)

public:
    static ErrorOr<NonnullRefPtr<CardGamePreview>> try_create()
    {
        auto preview = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) CardGamePreview()));

        Gfx::IntPoint point { 25, 30 };
        TRY(preview->add_stack(point, Cards::CardStack::Type::Stock));

        point.translate_by(Cards::Card::width + 30, 0);
        TRY(preview->add_stack(point, Cards::CardStack::Type::Normal));

        point.translate_by(Cards::Card::width + 30, 0);
        TRY(preview->add_stack(point, Cards::CardStack::Type::Normal));

        point.translate_by(20, 10);
        TRY(preview->add_stack(point, Cards::CardStack::Type::Normal));

        for (size_t i = 0; i < Cards::Card::card_count; ++i)
            TRY(preview->stack_at_location(0).push(TRY(Cards::Card::try_create(Cards::Suit::Diamonds, static_cast<Cards::Rank>(i)))));
        TRY(preview->stack_at_location(1).push(TRY(Cards::Card::try_create(Cards::Suit::Spades, Cards::Rank::Ace))));
        TRY(preview->stack_at_location(2).push(TRY(Cards::Card::try_create(Cards::Suit::Hearts, Cards::Rank::Queen))));
        TRY(preview->stack_at_location(3).push(TRY(Cards::Card::try_create(Cards::Suit::Clubs, Cards::Rank::Jack))));

        preview->stack_at_location(0).peek().set_upside_down(true);
        preview->stack_at_location(2).set_highlighted(true);

        return preview;
    }

private:
    CardGamePreview() = default;

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        Cards::CardGame::paint_event(event);

        GUI::Painter painter(*this);
        painter.add_clip_rect(frame_inner_rect());
        painter.add_clip_rect(event.rect());

        auto background_color = this->background_color();
        for (auto& stack : stacks())
            stack->paint(painter, background_color);
    }
};

ErrorOr<NonnullRefPtr<CardSettingsWidget>> CardSettingsWidget::try_create()
{
    auto card_settings_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) CardSettingsWidget));
    TRY(card_settings_widget->initialize());
    return card_settings_widget;
}

ErrorOr<void> CardSettingsWidget::initialize()
{
    TRY(load_from_gml(card_settings_widget_gml));

    auto background_color = Gfx::Color::from_string(Config::read_string("Games"sv, "Cards"sv, "BackgroundColor"sv)).value_or(Gfx::Color::from_rgb(0x008000));

    m_preview_frame = find_descendant_of_type_named<CardGamePreview>("cards_preview");
    m_preview_frame->set_background_color(background_color);

    m_background_color_input = find_descendant_of_type_named<GUI::ColorInput>("cards_background_color");
    m_background_color_input->set_color(background_color, GUI::AllowCallback::No);
    m_background_color_input->on_change = [&]() {
        set_modified(true);
        m_preview_frame->set_background_color(m_background_color_input->color());
    };

    m_card_back_image_view = find_descendant_of_type_named<GUI::IconView>("cards_back_image");
    m_card_back_image_view->set_model(GUI::FileSystemModel::create("/res/graphics/cards/backs"));
    m_card_back_image_view->set_model_column(GUI::FileSystemModel::Column::Name);
    if (!set_card_back_image_path(TRY(String::from_deprecated_string(Config::read_string("Games"sv, "Cards"sv, "CardBackImage"sv)))))
        set_card_back_image_path(default_card_back_image_path);
    m_card_back_image_view->on_selection_change = [&]() {
        auto& card_back_selection = m_card_back_image_view->selection();
        if (card_back_selection.is_empty())
            return;
        m_last_selected_card_back = card_back_selection.first();
        set_modified(true);
        Cards::CardPainter::the().set_background_image_path(card_back_image_path());
        m_preview_frame->update();
    };

    m_last_selected_card_back = m_card_back_image_view->selection().first();

    return {};
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

bool CardSettingsWidget::set_card_back_image_path(StringView path)
{
    auto index = static_cast<GUI::FileSystemModel*>(m_card_back_image_view->model())->index(path.to_deprecated_string(), m_card_back_image_view->model_column());
    if (index.is_valid()) {
        m_card_back_image_view->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
        Cards::CardPainter::the().set_background_image_path(path);
        m_preview_frame->update();
        return true;
    }
    return false;
}

String CardSettingsWidget::card_back_image_path() const
{
    auto& card_back_selection = m_card_back_image_view->selection();
    GUI::ModelIndex card_back_image_index = m_last_selected_card_back;
    if (!card_back_selection.is_empty())
        card_back_image_index = card_back_selection.first();
    return String::from_deprecated_string(static_cast<GUI::FileSystemModel const*>(m_card_back_image_view->model())->full_path(card_back_image_index)).release_value_but_fixme_should_propagate_errors();
}

}

REGISTER_WIDGET(GamesSettings, CardGamePreview);
