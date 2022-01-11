/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CharacterMapWidget.h"
#include <Applications/CharacterMap/CharacterMapWindowGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Toolbar.h>
#include <LibUnicode/CharacterTypes.h>

CharacterMapWidget::CharacterMapWidget()
{
    load_from_gml(character_map_window_gml);

    m_toolbar = find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    m_font_name_label = find_descendant_of_type_named<GUI::Label>("font_name");
    m_glyph_map = find_descendant_of_type_named<GUI::GlyphMapWidget>("glyph_map");
    m_output_box = find_descendant_of_type_named<GUI::TextBox>("output_box");
    m_copy_output_button = find_descendant_of_type_named<GUI::Button>("copy_output_button");
    m_statusbar = find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    m_choose_font_action = GUI::Action::create("Choose Font...", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-font-editor.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        auto font_picker = GUI::FontPicker::construct(window(), &font(), false);
        if (font_picker->exec() == GUI::Dialog::ExecOK) {
            auto& font = *font_picker->font();
            Config::write_string("CharacterMap", "History", "Font", font.qualified_name());
            set_font(font);
        }
    });

    m_copy_selection_action = GUI::CommonActions::make_copy_action([&](GUI::Action&) {
        auto selection = m_glyph_map->selection();
        StringBuilder builder;
        for (auto code_point = selection.start(); code_point < selection.start() + selection.size(); ++code_point) {
            if (!m_glyph_map->font().contains_glyph(code_point))
                continue;
            builder.append_code_point(code_point);
        }
        GUI::Clipboard::the().set_plain_text(builder.to_string());
    });

    m_toolbar->add_action(*m_choose_font_action);
    m_toolbar->add_separator();
    m_toolbar->add_action(*m_copy_selection_action);

    m_glyph_map->on_active_glyph_changed = [&](int) {
        update_statusbar();
    };

    m_glyph_map->on_glyph_double_clicked = [&](int code_point) {
        StringBuilder builder;
        builder.append(m_output_box->text());
        builder.append_code_point(code_point);
        m_output_box->set_text(builder.string_view());
    };

    m_copy_output_button->on_click = [&](auto) {
        GUI::Clipboard::the().set_plain_text(m_output_box->text());
    };

    did_change_font();
    update_statusbar();
}

CharacterMapWidget::~CharacterMapWidget()
{
}

void CharacterMapWidget::initialize_menubar(GUI::Window& window)
{
    auto& file_menu = window.add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_quit_action([](GUI::Action&) {
        GUI::Application::the()->quit();
    }));

    auto& help_menu = window.add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Character Map", GUI::Icon::default_icon("app-keyboard-settings"), &window));
}

void CharacterMapWidget::did_change_font()
{
    m_glyph_map->set_font(font());
    m_font_name_label->set_text(font().qualified_name());
    m_output_box->set_font(font());
}

void CharacterMapWidget::update_statusbar()
{
    auto code_point = m_glyph_map->active_glyph();
    StringBuilder builder;
    builder.appendff("U+{:04X}", code_point);
    if (auto display_name = Unicode::code_point_display_name(code_point); display_name.has_value())
        builder.appendff(" - {}", display_name.value());
    m_statusbar->set_text(builder.to_string());
}
