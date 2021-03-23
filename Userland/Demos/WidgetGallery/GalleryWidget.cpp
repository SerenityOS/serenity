/*
 * Copyright (c) 2021, the SerenityOS developers
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

#include "GalleryWidget.h"
#include "DemoWizardDialog.h"
#include "GalleryModels.h"
#include <AK/StringBuilder.h>
#include <Demos/WidgetGallery/BasicsTabGML.h>
#include <Demos/WidgetGallery/CursorsTabGML.h>
#include <Demos/WidgetGallery/IconsTabGML.h>
#include <Demos/WidgetGallery/SlidersTabGML.h>
#include <Demos/WidgetGallery/WindowGML.h>
#include <Demos/WidgetGallery/WizardsTabGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>

GalleryWidget::GalleryWidget()
{
    load_from_gml(window_gml);

    auto& tab_widget = *find_descendant_of_type_named<GUI::TabWidget>("tab_widget");

    auto& basics_tab = tab_widget.add_tab<GUI::Widget>("Basics");
    basics_tab.load_from_gml(basics_tab_gml);

    m_enabled_label = basics_tab.find_descendant_of_type_named<GUI::Label>("enabled_label");
    m_label_frame = basics_tab.find_descendant_of_type_named<GUI::Frame>("label_frame");

    m_frame_shapes.append("No Frame");
    m_frame_shapes.append("Plain Box");
    m_frame_shapes.append("Plain Container");
    m_frame_shapes.append("Plain Panel");
    m_frame_shapes.append("Raised Box");
    m_frame_shapes.append("Raised Container");
    m_frame_shapes.append("Raised Panel");
    m_frame_shapes.append("Sunken Box");
    m_frame_shapes.append("Sunken Container");
    m_frame_shapes.append("Sunken Panel");

    m_frame_shape_combobox = basics_tab.find_descendant_of_type_named<GUI::ComboBox>("frame_shape_combobox");
    m_frame_shape_combobox->set_model(*GUI::ItemListModel<String>::create(m_frame_shapes));

    m_frame_shape_combobox->on_change = [&](auto&, const auto& index) {
        m_label_frame->set_frame_shape(static_cast<Gfx::FrameShape>((index.row() - 1) % 3 + 1));
        m_label_frame->set_frame_shadow(static_cast<Gfx::FrameShadow>((index.row() - 1) / 3));
        m_label_frame->update();
    };

    m_frame_shape_combobox->on_return_pressed = [&]() {
        m_enabled_label->set_text(m_frame_shape_combobox->text());
    };

    m_thickness_spinbox = basics_tab.find_descendant_of_type_named<GUI::SpinBox>("thickness_spinbox");
    m_thickness_spinbox->set_value(1);

    m_thickness_spinbox->on_change = [&](auto value) {
        m_label_frame->set_frame_thickness(value);
    };

    m_button_icons.append(Gfx::Bitmap::load_from_file("/res/icons/16x16/book-open.png"));
    m_button_icons.append(Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"));
    m_button_icons.append(Gfx::Bitmap::load_from_file("/res/icons/16x16/ladybug.png"));

    m_icon_button = basics_tab.find_descendant_of_type_named<GUI::Button>("icon_button");
    m_icon_button->set_icon(*m_button_icons[2]);

    m_disabled_icon_button = basics_tab.find_descendant_of_type_named<GUI::Button>("disabled_icon_button");
    m_disabled_icon_button->set_icon(*m_button_icons[2]);

    m_icon_button->on_click = [&]() {
        static size_t i;
        if (i >= m_button_icons.size())
            i = 0;
        m_icon_button->set_icon(*m_button_icons[i]);
        m_disabled_icon_button->set_icon(*m_button_icons[i]);
        i++;
    };

    m_text_editor = basics_tab.find_descendant_of_type_named<GUI::TextEditor>("text_editor");

    m_font_button = basics_tab.find_descendant_of_type_named<GUI::Button>("font_button");
    m_font_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-font-editor.png"));

    m_font_button->on_click = [&]() {
        auto picker = GUI::FontPicker::construct(window(), &m_text_editor->font(), false);
        if (picker->exec() == GUI::Dialog::ExecOK) {
            m_text_editor->set_font(picker->font());
        }
    };

    m_file_button = basics_tab.find_descendant_of_type_named<GUI::Button>("file_button");
    m_file_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"));

    m_file_button->on_click = [&]() {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window());
        if (!open_path.has_value())
            return;
        m_text_editor->set_text(open_path.value());
    };

    m_input_button = basics_tab.find_descendant_of_type_named<GUI::Button>("input_button");
    m_input_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/properties.png"));

    m_input_button->on_click = [&]() {
        String value;
        if (GUI::InputBox::show(window(), value, "Enter input:", "Input") == GUI::InputBox::ExecOK && !value.is_empty())
            m_text_editor->set_text(value);
    };

    m_font_colorinput = basics_tab.find_descendant_of_type_named<GUI::ColorInput>("font_colorinput");

    m_font_colorinput->on_change = [&]() {
        auto palette = m_text_editor->palette();
        palette.set_color(Gfx::ColorRole::BaseText, m_font_colorinput->color());
        m_text_editor->set_palette(palette);
        m_text_editor->update();
    };

    m_msgbox_button = basics_tab.find_descendant_of_type_named<GUI::Button>("msgbox_button");
    m_msgbox_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-irc-client.png"));

    m_msgbox_type = GUI::MessageBox::Type::None;
    m_msgbox_input_type = GUI::MessageBox::InputType::OK;

    m_msgbox_icons.append("None");
    m_msgbox_icons.append("Information");
    m_msgbox_icons.append("Warning");
    m_msgbox_icons.append("Error");
    m_msgbox_icons.append("Question");

    m_msgbox_buttons.append("OK");
    m_msgbox_buttons.append("OK Cancel");
    m_msgbox_buttons.append("Yes No");
    m_msgbox_buttons.append("Yes No Cancel");

    m_msgbox_icon_combobox = basics_tab.find_descendant_of_type_named<GUI::ComboBox>("msgbox_icon_combobox");
    m_msgbox_icon_combobox->set_model(*GUI::ItemListModel<String>::create(m_msgbox_icons));
    m_msgbox_icon_combobox->set_selected_index(0);

    m_msgbox_icon_combobox->on_change = [&](auto&, const auto& index) {
        m_msgbox_type = static_cast<GUI::MessageBox::Type>(index.row());
    };

    m_msgbox_buttons_combobox = basics_tab.find_descendant_of_type_named<GUI::ComboBox>("msgbox_buttons_combobox");
    m_msgbox_buttons_combobox->set_model(*GUI::ItemListModel<String>::create(m_msgbox_buttons));
    m_msgbox_buttons_combobox->set_selected_index(0);

    m_msgbox_buttons_combobox->on_change = [&](auto&, const auto& index) {
        m_msgbox_input_type = static_cast<GUI::MessageBox::InputType>(index.row());
    };

    m_msgbox_button->on_click = [&]() {
        GUI::MessageBox::show(window(), m_text_editor->text(), "Message", m_msgbox_type, m_msgbox_input_type);
    };

    auto& sliders_tab = tab_widget.add_tab<GUI::Widget>("Sliders");
    sliders_tab.load_from_gml(sliders_tab_gml);

    m_vertical_progressbar_left = sliders_tab.find_descendant_of_type_named<GUI::VerticalProgressBar>("vertical_progressbar_left");
    m_vertical_progressbar_left->set_value(0);

    m_vertical_progressbar_right = sliders_tab.find_descendant_of_type_named<GUI::VerticalProgressBar>("vertical_progressbar_right");
    m_vertical_progressbar_right->set_value(100);

    m_vertical_slider_left = sliders_tab.find_descendant_of_type_named<GUI::VerticalSlider>("vertical_slider_left");
    m_vertical_slider_right = sliders_tab.find_descendant_of_type_named<GUI::VerticalSlider>("vertical_slider_right");

    m_vertical_slider_left->on_change = [&](auto value) {
        m_vertical_progressbar_left->set_value(m_vertical_slider_left->max() - value);
    };

    m_vertical_slider_right->on_change = [&](auto value) {
        m_vertical_progressbar_right->set_value((100 / m_vertical_slider_right->max()) * (m_vertical_slider_right->max() - value));
    };

    m_horizontal_progressbar = sliders_tab.find_descendant_of_type_named<GUI::HorizontalProgressBar>("horizontal_progressbar");
    m_horizontal_progressbar->set_value(0);

    m_horizontal_slider_left = sliders_tab.find_descendant_of_type_named<GUI::HorizontalSlider>("horizontal_slider_left");
    m_horizontal_slider_right = sliders_tab.find_descendant_of_type_named<GUI::HorizontalSlider>("horizontal_slider_right");

    m_horizontal_slider_left->on_change = [&](auto value) {
        m_horizontal_progressbar->set_value(value);
        if (!(value % (100 / m_horizontal_slider_right->max())))
            m_horizontal_slider_right->set_value(value / (100 / m_horizontal_slider_right->max()));
    };

    m_horizontal_slider_right->on_change = [&](auto value) {
        m_horizontal_progressbar->set_value((value * 100) / m_horizontal_slider_right->max());
        m_horizontal_slider_left->set_value((value * 100) / m_horizontal_slider_right->max());
    };

    m_enabled_scrollbar = sliders_tab.find_descendant_of_type_named<GUI::ScrollBar>("enabled_scrollbar");
    m_enabled_scrollbar->set_orientation(Orientation::Horizontal);

    m_disabled_scrollbar = sliders_tab.find_descendant_of_type_named<GUI::ScrollBar>("disabled_scrollbar");
    m_disabled_scrollbar->set_orientation(Orientation::Horizontal);

    m_opacity_imagewidget = sliders_tab.find_descendant_of_type_named<GUI::ImageWidget>("opacity_imagewidget");
    m_opacity_imagewidget->load_from_file("/res/graphics/brand-banner.png");

    m_opacity_slider = sliders_tab.find_descendant_of_type_named<GUI::OpacitySlider>("opacity_slider");

    m_opacity_slider->on_change = [&](auto percent) {
        m_opacity_imagewidget->set_opacity_percent(percent);
    };

    auto& wizards_tab = tab_widget.add_tab<GUI::Widget>("Wizards");
    wizards_tab.load_from_gml(wizards_tab_gml);

    m_wizard_button = wizards_tab.find_descendant_of_type_named<GUI::Button>("wizard_button");
    m_wizard_output = wizards_tab.find_descendant_of_type_named<GUI::TextEditor>("wizard_output");
    m_wizard_output->set_should_hide_unnecessary_scrollbars(true);

    const char* serenityos_ascii = {
        "   ____                 _ __       ____  ____\n"
        "  / __/__ _______ ___  (_) /___ __/ __ \\/ __/\n"
        " _\\ \\/ -_) __/ -_) _ \\/ / __/ // / /_/ /\\ \\\n"
        "/___/\\__/_/  \\__/_//_/_/\\__/\\_, /\\____/___/\n"
        "                           /___/\n"
    };

    const char* wizard_ascii = {
        "              _,-'|\n"
        "           ,-'._  |\n"
        " .||,      |####\\ |\n"
        "\\`' ,/     \\'L' | |\n"
        "= ,. =      |-,#| |\n"
        "/ || \\    ,-'\\#/,'`.\n"
        "  ||     ,'   `,,. `.\n"
        "  ,|____,' , ,;' \\| |\n"
        " (3|\\    _/|/'   _| |\n"
        "  ||/,-''  | >-'' _,\\\\\n"
        "  ||'      ==\\ ,-'  ,'\n"
        "  ||       |  V \\ ,|\n"
        "  ||       |    |` |\n"
        "  ||       |    |   \\\n"
        "  ||       |    \\    \\\n"
        "  ||       |     |    \\\n"
        "  ||       |      \\_,-'\n"
        "  ||       |___,,--')_\\\n"
        "  ||         |_|  _ccc/-\n"
        "  ||        ccc/__\n"
        " _||_-\n"
    };

    StringBuilder sb;
    sb.appendf("%s%s", serenityos_ascii, wizard_ascii);
    m_wizard_output->set_text(sb.to_string());

    m_wizard_button->on_click = [&]() {
        StringBuilder sb;
        sb.append(m_wizard_output->get_text());
        sb.append("\nWizard started.");
        m_wizard_output->set_text(sb.to_string());

        auto wizard = DemoWizardDialog::construct(window());
        int result = wizard->exec();

        sb.append(String::formatted("\nWizard execution complete.\nDialog ExecResult code: {}", result));
        if (result == GUI::Dialog::ExecResult::ExecOK)
            sb.append(String::formatted(" (ExecOK)\n'Installation' location: \"{}\"", wizard->page_1_location()));
        m_wizard_output->set_text(sb.string_view());
    };

    auto& cursors_tab = tab_widget.add_tab<GUI::Widget>("Cursors");
    cursors_tab.load_from_gml(cursors_tab_gml);

    m_cursors_tableview = cursors_tab.find_descendant_of_type_named<GUI::TableView>("cursors_tableview");
    m_cursors_tableview->set_highlight_selected_rows(true);
    m_cursors_tableview->set_alternating_row_colors(false);
    m_cursors_tableview->set_vertical_padding(16);
    m_cursors_tableview->set_column_headers_visible(false);
    m_cursors_tableview->set_highlight_key_column(false);

    auto sorting_proxy_model = GUI::SortingProxyModel::create(MouseCursorModel::create());
    sorting_proxy_model->set_sort_role(GUI::ModelRole::Display);

    m_cursors_tableview->set_model(sorting_proxy_model);
    m_cursors_tableview->set_key_column_and_sort_order(MouseCursorModel::Column::Name, GUI::SortOrder::Ascending);
    m_cursors_tableview->model()->update();
    m_cursors_tableview->set_column_width(0, 25);

    m_cursors_tableview->on_activation = [&](const GUI::ModelIndex& index) {
        switch (index.row()) {
        case 0:
            window()->set_cursor(Gfx::StandardCursor::Arrow);
            break;
        case 1:
            window()->set_cursor(Gfx::StandardCursor::Crosshair);
            break;
        case 2:
            window()->set_cursor(Gfx::StandardCursor::Disallowed);
            break;
        case 3:
            window()->set_cursor(Gfx::StandardCursor::Drag);
            break;
        case 4:
            window()->set_cursor(Gfx::StandardCursor::Hand);
            break;
        case 5:
            window()->set_cursor(Gfx::StandardCursor::Help);
            break;
        case 6:
            window()->set_cursor(Gfx::StandardCursor::Hidden);
            break;
        case 7:
            window()->set_cursor(Gfx::StandardCursor::IBeam);
            break;
        case 8:
            window()->set_cursor(Gfx::StandardCursor::Move);
            break;
        case 9:
            window()->set_cursor(Gfx::StandardCursor::ResizeColumn);
            break;
        case 10:
            window()->set_cursor(Gfx::StandardCursor::ResizeDiagonalBLTR);
            break;
        case 11:
            window()->set_cursor(Gfx::StandardCursor::ResizeDiagonalTLBR);
            break;
        case 12:
            window()->set_cursor(Gfx::StandardCursor::ResizeHorizontal);
            break;
        case 13:
            window()->set_cursor(Gfx::StandardCursor::ResizeRow);
            break;
        case 14:
            window()->set_cursor(Gfx::StandardCursor::ResizeVertical);
            break;
        case 15:
            window()->set_cursor(Gfx::StandardCursor::Wait);
            break;
        default:
            window()->set_cursor(Gfx::StandardCursor::Arrow);
        }
    };

    auto& icons_tab = tab_widget.add_tab<GUI::Widget>("Icons");
    icons_tab.load_from_gml(icons_tab_gml);

    m_icons_tableview = icons_tab.find_descendant_of_type_named<GUI::TableView>("icons_tableview");
    m_icons_tableview->set_highlight_selected_rows(true);
    m_icons_tableview->set_alternating_row_colors(false);
    m_icons_tableview->set_vertical_padding(24);
    m_icons_tableview->set_column_headers_visible(false);
    m_icons_tableview->set_highlight_key_column(false);

    auto sorting_proxy_icons_model = GUI::SortingProxyModel::create(FileIconsModel::create());
    sorting_proxy_icons_model->set_sort_role(GUI::ModelRole::Display);

    m_icons_tableview->set_model(sorting_proxy_icons_model);
    m_icons_tableview->set_key_column_and_sort_order(FileIconsModel::Column::Name, GUI::SortOrder::Ascending);
    m_icons_tableview->model()->update();
    m_icons_tableview->set_column_width(0, 36);
    m_icons_tableview->set_column_width(1, 20);
}

GalleryWidget::~GalleryWidget()
{
}
