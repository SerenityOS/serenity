/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CharacterMapWidget.h"
#include "CharacterSearchWidget.h"
#include <AK/StringUtils.h>
#include <Applications/CharacterMap/CharacterMapWindowGML.h>
#include <LibConfig/Client.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Toolbar.h>
#include <LibUnicode/CharacterTypes.h>

CharacterMapWidget::CharacterMapWidget()
{
    load_from_gml(character_map_window_gml).release_value_but_fixme_should_propagate_errors();

    m_toolbar = find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    m_font_name_label = find_descendant_of_type_named<GUI::Label>("font_name");
    m_glyph_map = find_descendant_of_type_named<GUI::GlyphMapWidget>("glyph_map");
    m_output_box = find_descendant_of_type_named<GUI::TextBox>("output_box");
    m_copy_output_button = find_descendant_of_type_named<GUI::Button>("copy_output_button");
    m_statusbar = find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    m_unicode_block_listview = find_descendant_of_type_named<GUI::ListView>("unicode_block_listview");

    m_choose_font_action = GUI::Action::create("Change &Font...", Gfx::Bitmap::load_from_file("/res/icons/16x16/app-font-editor.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        auto font_picker = GUI::FontPicker::construct(window(), &font(), false);
        if (font_picker->exec() == GUI::Dialog::ExecResult::OK) {
            auto& font = *font_picker->font();
            Config::write_string("CharacterMap"sv, "History"sv, "Font"sv, font.qualified_name());
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
        GUI::Clipboard::the().set_plain_text(builder.to_byte_string());
    });
    m_copy_selection_action->set_status_tip("Copy the highlighted characters to the clipboard"_string);

    m_previous_glyph_action = GUI::Action::create("&Previous Glyph", { Mod_Alt, Key_Left }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        m_glyph_map->select_previous_existing_glyph();
    });
    m_previous_glyph_action->set_status_tip("Seek the previous visible glyph"_string);

    m_next_glyph_action = GUI::Action::create("&Next Glyph", { Mod_Alt, Key_Right }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        m_glyph_map->select_next_existing_glyph();
    });
    m_next_glyph_action->set_status_tip("Seek the next visible glyph"_string);

    m_go_to_glyph_action = GUI::Action::create("&Go to Glyph...", { Mod_Ctrl, Key_G }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-to.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        String input;
        if (GUI::InputBox::show(window(), input, "Hexadecimal:"sv, "Go to Glyph"sv, GUI::InputType::NonemptyText) == GUI::InputBox::ExecResult::OK) {
            auto maybe_code_point = AK::StringUtils::convert_to_uint_from_hex(input);
            if (!maybe_code_point.has_value())
                return;
            auto code_point = maybe_code_point.value();
            code_point = clamp(code_point, m_range.first, m_range.last);
            m_glyph_map->set_focus(true);
            m_glyph_map->set_active_glyph(code_point);
            m_glyph_map->scroll_to_glyph(code_point);
        }
    });
    m_go_to_glyph_action->set_status_tip("Go to the specified code point"_string);

    m_find_glyphs_action = GUI::Action::create("&Find Glyphs...", { Mod_Ctrl, Key_F }, Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        if (m_find_window.is_null()) {
            m_find_window = GUI::Window::construct(window());
            auto search_widget = m_find_window->set_main_widget<CharacterSearchWidget>();
            search_widget->on_character_selected = [&](auto code_point) {
                m_glyph_map->set_active_glyph(code_point);
                m_glyph_map->scroll_to_glyph(code_point);
            };
            m_find_window->set_icon(GUI::Icon::try_create_default_icon("find"sv).value().bitmap_for_size(16));
            m_find_window->set_title("Find a Character");
            m_find_window->resize(300, 400);
            m_find_window->set_window_mode(GUI::WindowMode::Modeless);
        }
        m_find_window->show();
        m_find_window->move_to_front();
        m_find_window->find_descendant_of_type_named<GUI::TextBox>("search_input")->set_focus(true);
    });

    m_toolbar->add_action(*m_choose_font_action);
    m_toolbar->add_separator();
    m_toolbar->add_action(*m_copy_selection_action);
    m_toolbar->add_separator();
    m_toolbar->add_action(*m_previous_glyph_action);
    m_toolbar->add_action(*m_next_glyph_action);
    m_toolbar->add_action(*m_go_to_glyph_action);
    m_toolbar->add_action(*m_find_glyphs_action);

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

    auto unicode_blocks = Unicode::block_display_names();
    m_unicode_block_listview->on_selection_change = [this, unicode_blocks] {
        auto index = m_unicode_block_listview->selection().first();
        if (index.row() > 0)
            m_range = unicode_blocks[index.row() - 1].code_point_range;
        else
            m_range = { 0x0000, 0x10FFFF };
        m_glyph_map->set_active_range(m_range);
    };

    m_unicode_block_list.append("Show All");
    for (auto& block : unicode_blocks)
        m_unicode_block_list.append(block.display_name);

    m_unicode_block_model = GUI::ItemListModel<ByteString>::create(m_unicode_block_list);
    m_unicode_block_listview->set_model(*m_unicode_block_model);
    m_unicode_block_listview->set_activates_on_selection(true);
    m_unicode_block_listview->horizontal_scrollbar().set_visible(false);
    m_unicode_block_listview->set_cursor(m_unicode_block_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);

    GUI::Application::the()->on_action_enter = [this](GUI::Action& action) {
        m_statusbar->set_override_text(action.status_tip());
    };

    GUI::Application::the()->on_action_leave = [this](GUI::Action&) {
        m_statusbar->set_override_text({});
    };

    did_change_font();
    update_statusbar();
}

ErrorOr<void> CharacterMapWidget::initialize_menubar(GUI::Window& window)
{
    auto file_menu = window.add_menu("&File"_string);
    file_menu->add_action(GUI::CommonActions::make_quit_action([](GUI::Action&) {
        GUI::Application::the()->quit();
    }));

    auto view_menu = window.add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window.set_fullscreen(!window.is_fullscreen());
    }));

    auto help_menu = window.add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(&window));
    help_menu->add_action(GUI::CommonActions::make_help_action([&](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/CharacterMap.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Character Map"_string, GUI::Icon::default_icon("app-character-map"sv), &window));
    return {};
}

void CharacterMapWidget::did_change_font()
{
    m_glyph_map->set_font(font());
    m_font_name_label->set_text(font().human_readable_name());
    m_output_box->set_font(font());
}

void CharacterMapWidget::update_statusbar()
{
    auto code_point = m_glyph_map->active_glyph();
    StringBuilder builder;
    builder.appendff("U+{:04X}", code_point);
    if (auto display_name = Unicode::code_point_display_name(code_point); display_name.has_value())
        builder.appendff(" - {}", display_name.value());
    m_statusbar->set_text(builder.to_string().release_value_but_fixme_should_propagate_errors());
}
