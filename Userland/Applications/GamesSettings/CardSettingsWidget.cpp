/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CardSettingsWidget.h"
#include <Applications/GamesSettings/CardSettingsWidgetGML.h>
#include <LibConfig/Client.h>

CardSettingsWidget::CardSettingsWidget()
{
    load_from_gml(card_settings_widget_gml);

    m_background_color_input = find_descendant_of_type_named<GUI::ColorInput>("cards_background_color");
    auto background_color = Gfx::Color::from_string(Config::read_string("Games"sv, "Cards"sv, "BackgroundColor"sv)).value_or(Gfx::Color::from_rgb(0x008000));
    m_background_color_input->set_color(background_color, GUI::AllowCallback::No);
    m_background_color_input->on_change = [&]() { set_modified(true); };
}

void CardSettingsWidget::apply_settings()
{
    Config::write_string("Games"sv, "Cards"sv, "BackgroundColor"sv, m_background_color_input->text());
}

void CardSettingsWidget::reset_default_values()
{
    m_background_color_input->set_color(Gfx::Color::from_rgb(0x008000));
}
