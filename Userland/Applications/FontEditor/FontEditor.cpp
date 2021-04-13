/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "FontEditor.h"
#include "GlyphEditorWidget.h"
#include "GlyphMapWidget.h"
#include "NewFontDialog.h"
#include <AK/StringBuilder.h>
#include <Applications/FontEditor/FontEditorWindowGML.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/FontPickerWeightModel.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/Palette.h>
#include <stdlib.h>

static RefPtr<GUI::Window> create_font_preview_window(FontEditorWidget& editor)
{
    auto window = GUI::Window::construct();
    window->set_window_type(GUI::WindowType::ToolWindow);
    window->set_title("Font preview");
    window->resize(400, 150);
    window->set_minimum_size(200, 100);
    window->center_within(*editor.window());

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.layout()->set_margins({ 2, 2, 2, 2 });
    main_widget.layout()->set_spacing(4);

    auto& preview_box = main_widget.add<GUI::GroupBox>();
    preview_box.set_layout<GUI::VerticalBoxLayout>();
    preview_box.layout()->set_margins({ 8, 8, 8, 8 });

    auto& preview_label = preview_box.add<GUI::Label>();
    preview_label.set_font(editor.edited_font());

    editor.on_initialize = [&] {
        preview_label.set_font(editor.edited_font());
    };

    auto& preview_textbox = main_widget.add<GUI::TextBox>();
    preview_textbox.set_text("waxy and quivering jocks fumble the pizza");
    preview_textbox.set_placeholder("Preview text");

    preview_textbox.on_change = [&] {
        auto preview = String::formatted("{}\n{}",
            preview_textbox.text(),
            preview_textbox.text().to_uppercase());
        preview_label.set_text(preview);
    };

    return window;
}

FontEditorWidget::FontEditorWidget(const String& path, RefPtr<Gfx::BitmapFont>&& edited_font)
{
    load_from_gml(font_editor_window_gml);

    auto& toolbar = *find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    auto& statusbar = *find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    auto& glyph_map_container = *find_descendant_of_type_named<GUI::Widget>("glyph_map_container");
    m_glyph_editor_container = *find_descendant_of_type_named<GUI::Widget>("glyph_editor_container");
    m_left_column_container = *find_descendant_of_type_named<GUI::Widget>("left_column_container");
    m_glyph_editor_width_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("glyph_editor_width_spinbox");
    m_name_textbox = *find_descendant_of_type_named<GUI::TextBox>("name_textbox");
    m_family_textbox = *find_descendant_of_type_named<GUI::TextBox>("family_textbox");
    m_presentation_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("presentation_spinbox");
    m_weight_combobox = *find_descendant_of_type_named<GUI::ComboBox>("weight_combobox");
    m_type_combobox = *find_descendant_of_type_named<GUI::ComboBox>("type_combobox");
    m_spacing_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("spacing_spinbox");
    m_mean_line_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("mean_line_spinbox");
    m_baseline_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("baseline_spinbox");
    m_fixed_width_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("fixed_width_checkbox");
    m_font_metadata_groupbox = *find_descendant_of_type_named<GUI::GroupBox>("font_metadata_groupbox");

    m_glyph_editor_widget = m_glyph_editor_container->add<GlyphEditorWidget>();
    m_glyph_map_widget = glyph_map_container.add<GlyphMapWidget>();

    auto update_demo = [&] {
        if (m_font_preview_window)
            m_font_preview_window->update();
    };

    m_new_action = GUI::Action::create("New Font...", { Mod_Ctrl, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-font.png"), [&](auto&) {
        auto new_font_wizard = NewFontDialog::construct(window());
        if (new_font_wizard->exec() == GUI::Dialog::ExecOK) {
            auto metadata = new_font_wizard->new_font_metadata();

            String name = metadata.name;
            auto parts = name.split(' ');
            if (parts.size() > 1)
                name = parts[0];
            parts.clear();

            String weight = GUI::weight_to_name(metadata.weight).to_string();
            parts = weight.split(' ');
            if (parts.size() > 1)
                weight = String::formatted("{}{}", parts[0], parts[1]);

            RefPtr<Gfx::BitmapFont> new_font = Gfx::BitmapFont::create(metadata.glyph_height, metadata.glyph_width, metadata.is_fixed_width, metadata.type);
            String path = String::formatted("/tmp/{}{}{}.font", name, weight, metadata.presentation_size);
            if (!new_font) {
                String message = String::formatted("Failed to create new font: {}\n", path);
                GUI::MessageBox::show(window(), message, "Font Editor", GUI::MessageBox::Type::Error);
                return;
            }

            new_font->set_name(metadata.name);
            new_font->set_family(metadata.family);
            new_font->set_presentation_size(metadata.presentation_size);
            new_font->set_weight(metadata.weight);
            new_font->set_baseline(metadata.baseline);
            new_font->set_mean_line(metadata.mean_line);

            window()->set_title(String::formatted("{} - Font Editor", path));
            initialize(path, move(new_font));
        }
    });
    m_open_action = GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window(), {}, "/res/fonts/");
        if (!open_path.has_value())
            return;

        auto bitmap_font = Gfx::BitmapFont::load_from_file(open_path.value());
        if (!bitmap_font) {
            String message = String::formatted("Couldn't load font: {}\n", open_path.value());
            GUI::MessageBox::show(window(), message, "Font Editor", GUI::MessageBox::Type::Error);
            return;
        }
        RefPtr<Gfx::BitmapFont> new_font = static_ptr_cast<Gfx::BitmapFont>(bitmap_font->clone());
        if (!new_font) {
            String message = String::formatted("Couldn't load font: {}\n", open_path.value());
            GUI::MessageBox::show(window(), message, "Font Editor", GUI::MessageBox::Type::Error);
            return;
        }
        window()->set_title(String::formatted("{} - Font Editor", open_path.value()));
        initialize(open_path.value(), move(new_font));
    });
    m_save_action = GUI::CommonActions::make_save_action([&](auto&) {
        save_as(m_path);
    });
    m_save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        LexicalPath lexical_path(m_path);
        Optional<String> save_path = GUI::FilePicker::get_save_filepath(window(), lexical_path.title(), lexical_path.extension());
        if (!save_path.has_value())
            return;

        if (save_as(save_path.value()))
            window()->set_title(String::formatted("{} - Font Editor", save_path.value()));
    });
    m_cut_action = GUI::CommonActions::make_cut_action([&](auto&) {
        m_glyph_editor_widget->cut_glyph();
    });
    m_copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        m_glyph_editor_widget->copy_glyph();
    });
    m_paste_action = GUI::CommonActions::make_paste_action([&](auto&) {
        m_glyph_editor_widget->paste_glyph();
        m_glyph_map_widget->update_glyph(m_glyph_map_widget->selected_glyph());
    });
    m_paste_action->set_enabled(GUI::Clipboard::the().mime_type() == "glyph/x-fonteditor");
    m_delete_action = GUI::CommonActions::make_delete_action([&](auto&) {
        m_edited_font->set_glyph_width(m_glyph_map_widget->selected_glyph(), m_edited_font->max_glyph_width());
        m_glyph_editor_widget->delete_glyph();
        m_glyph_map_widget->update_glyph(m_glyph_map_widget->selected_glyph());
        m_glyph_editor_width_spinbox->set_value(m_edited_font->glyph_width(m_glyph_map_widget->selected_glyph()));
    });
    m_open_preview_action = GUI::Action::create("Preview Font", Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"), [&](auto&) {
        if (!m_font_preview_window)
            m_font_preview_window = create_font_preview_window(*this);
        m_font_preview_window->show();
        m_font_preview_window->move_to_front();
    });
    m_open_preview_action->set_checked(false);
    m_show_metadata_action = GUI::Action::create_checkable("Font Metadata", { Mod_Ctrl, Key_M }, [&](auto& action) {
        set_show_font_metadata(action.is_checked());
    });
    m_show_metadata_action->set_checked(true);

    toolbar.add_action(*m_new_action);
    toolbar.add_action(*m_open_action);
    toolbar.add_action(*m_save_action);
    toolbar.add_separator();
    toolbar.add_action(*m_cut_action);
    toolbar.add_action(*m_copy_action);
    toolbar.add_action(*m_paste_action);
    toolbar.add_action(*m_delete_action);
    toolbar.add_separator();
    toolbar.add_action(*m_open_preview_action);

    GUI::Clipboard::the().on_change = [&](const String& data_type) {
        m_paste_action->set_enabled(data_type == "glyph/x-fonteditor");
    };

    m_glyph_editor_widget->on_glyph_altered = [this, update_demo](int glyph) {
        m_glyph_map_widget->update_glyph(glyph);
        update_demo();
    };

    m_glyph_map_widget->on_glyph_selected = [&](int glyph) {
        m_glyph_editor_widget->set_glyph(glyph);
        m_glyph_editor_width_spinbox->set_value(m_edited_font->glyph_width(m_glyph_map_widget->selected_glyph()));
        StringBuilder builder;
        builder.appendff("{:#02x} (", glyph);
        if (glyph < 128) {
            if (glyph == 10)
                builder.append("LF");
            else
                builder.append(glyph);
        } else {
            builder.append(128 | 64 | (glyph / 64));
            builder.append(128 | (glyph % 64));
        }
        builder.append(") ");
        builder.appendff("[{}x{}]", m_edited_font->glyph_width(glyph), m_edited_font->glyph_height());
        statusbar.set_text(builder.to_string());
    };

    m_name_textbox->on_change = [&] {
        m_edited_font->set_name(m_name_textbox->text());
    };

    m_family_textbox->on_change = [&] {
        m_edited_font->set_family(m_family_textbox->text());
    };

    m_fixed_width_checkbox->on_checked = [&, update_demo](bool checked) {
        m_edited_font->set_fixed_width(checked);
        m_glyph_editor_width_spinbox->set_enabled(!m_edited_font->is_fixed_width());
        m_glyph_editor_width_spinbox->set_value(m_edited_font->glyph_width(m_glyph_map_widget->selected_glyph()));
        m_glyph_editor_widget->update();
        update_demo();
    };

    m_glyph_editor_width_spinbox->on_change = [this, update_demo](int value) {
        m_edited_font->set_glyph_width(m_glyph_map_widget->selected_glyph(), value);
        m_glyph_editor_widget->update();
        m_glyph_map_widget->update_glyph(m_glyph_map_widget->selected_glyph());
        update_demo();
    };

    m_weight_combobox->on_change = [this]() {
        m_edited_font->set_weight(GUI::name_to_weight(m_weight_combobox->text()));
    };

    m_type_combobox->on_change = [this](auto&, const auto& index) {
        m_edited_font->set_type(static_cast<Gfx::FontTypes>(index.row()));
    };

    m_presentation_spinbox->on_change = [this, update_demo](int value) {
        m_edited_font->set_presentation_size(value);
        update_demo();
    };

    m_spacing_spinbox->on_change = [this, update_demo](int value) {
        m_edited_font->set_glyph_spacing(value);
        update_demo();
    };

    m_baseline_spinbox->on_change = [this, update_demo](int value) {
        m_edited_font->set_baseline(value);
        m_glyph_editor_widget->update();
        update_demo();
    };

    m_mean_line_spinbox->on_change = [this, update_demo](int value) {
        m_edited_font->set_mean_line(value);
        m_glyph_editor_widget->update();
        update_demo();
    };

    initialize(path, move(edited_font));
}

FontEditorWidget::~FontEditorWidget()
{
}

void FontEditorWidget::initialize(const String& path, RefPtr<Gfx::BitmapFont>&& edited_font)
{
    if (m_edited_font == edited_font)
        return;
    m_path = path;
    m_edited_font = edited_font;

    m_glyph_map_widget->initialize(*m_edited_font);
    m_glyph_editor_widget->initialize(*m_edited_font);

    m_glyph_editor_container->set_fixed_size(m_glyph_editor_widget->preferred_width(), m_glyph_editor_widget->preferred_height());
    m_left_column_container->set_fixed_width(m_glyph_editor_widget->preferred_width());
    m_glyph_editor_width_spinbox->set_enabled(!m_edited_font->is_fixed_width());
    m_glyph_editor_width_spinbox->set_max(m_edited_font->max_glyph_width());

    m_name_textbox->set_text(m_edited_font->name());
    m_family_textbox->set_text(m_edited_font->family());

    m_presentation_spinbox->set_value(m_edited_font->presentation_size());
    m_spacing_spinbox->set_value(m_edited_font->glyph_spacing());
    m_mean_line_spinbox->set_value(m_edited_font->mean_line());
    m_baseline_spinbox->set_value(m_edited_font->baseline());

    m_font_weight_list.clear();
    for (auto& it : GUI::font_weight_names)
        m_font_weight_list.append(it.name);
    m_weight_combobox->set_model(*GUI::ItemListModel<String>::create(m_font_weight_list));

    int i = 0;
    for (auto it : GUI::font_weight_names) {
        if (it.weight == m_edited_font->weight()) {
            m_weight_combobox->set_selected_index(i);
            break;
        }
        i++;
    }

    m_font_type_list.clear();
    StringBuilder type_count;
    for (int i = 0; i < Gfx::FontTypes::__Count; i++) {
        type_count.appendff("{}", Gfx::BitmapFont::type_name_by_type(static_cast<Gfx::FontTypes>(i)));
        m_font_type_list.append(type_count.to_string());
        type_count.clear();
    }
    m_type_combobox->set_model(*GUI::ItemListModel<String>::create(m_font_type_list));
    m_type_combobox->set_selected_index(m_edited_font->type());

    m_fixed_width_checkbox->set_checked(m_edited_font->is_fixed_width());

    m_glyph_map_widget->set_selected_glyph('A');
    deferred_invoke([this](auto&) {
        m_glyph_map_widget->set_focus(true);
        m_glyph_map_widget->scroll_to_glyph(m_glyph_map_widget->selected_glyph());
    });

    if (on_initialize)
        on_initialize();
}

void FontEditorWidget::initialize_menubar(GUI::Menubar& menubar)
{
    auto& app_menu = menubar.add_menu("&File");
    app_menu.add_action(*m_new_action);
    app_menu.add_action(*m_open_action);
    app_menu.add_action(*m_save_action);
    app_menu.add_action(*m_save_as_action);
    app_menu.add_separator();
    app_menu.add_action(GUI::CommonActions::make_quit_action([this](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& edit_menu = menubar.add_menu("&Edit");
    edit_menu.add_action(*m_cut_action);
    edit_menu.add_action(*m_copy_action);
    edit_menu.add_action(*m_paste_action);
    edit_menu.add_action(*m_delete_action);

    auto& view_menu = menubar.add_menu("&View");
    view_menu.add_action(*m_open_preview_action);
    view_menu.add_separator();
    view_menu.add_action(*m_show_metadata_action);

    auto& help_menu = menubar.add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/FontEditor.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("Font Editor", GUI::Icon::default_icon("app-font-editor"), window()));
}

bool FontEditorWidget::save_as(const String& path)
{
    auto ret_val = m_edited_font->write_to_file(path);
    if (!ret_val) {
        GUI::MessageBox::show(window(), "The font file could not be saved.", "Save failed", GUI::MessageBox::Type::Error);
        return false;
    }
    m_path = path;
    return true;
}

void FontEditorWidget::set_show_font_metadata(bool show)
{
    if (m_font_metadata == show)
        return;
    m_font_metadata = show;
    m_font_metadata_groupbox->set_visible(m_font_metadata);
}
