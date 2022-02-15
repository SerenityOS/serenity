/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>

GalleryWidget::GalleryWidget()
{
    load_from_gml(window_gml);

    auto& tab_widget = *find_descendant_of_type_named<GUI::TabWidget>("tab_widget");
    tab_widget.set_reorder_allowed(true);

    auto basics_tab = tab_widget.try_add_tab<GUI::Widget>("Basics").release_value_but_fixme_should_propagate_errors();
    basics_tab->load_from_gml(basics_tab_gml);

    m_enabled_label = basics_tab->find_descendant_of_type_named<GUI::Label>("enabled_label");
    m_label_frame = basics_tab->find_descendant_of_type_named<GUI::Frame>("label_frame");

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

    m_frame_shape_combobox = basics_tab->find_descendant_of_type_named<GUI::ComboBox>("frame_shape_combobox");
    m_frame_shape_combobox->set_model(*GUI::ItemListModel<String>::create(m_frame_shapes));

    m_frame_shape_combobox->on_change = [&](auto&, const auto& index) {
        m_label_frame->set_frame_shape(static_cast<Gfx::FrameShape>((index.row() - 1) % 3 + 1));
        m_label_frame->set_frame_shadow(static_cast<Gfx::FrameShadow>((index.row() - 1) / 3));
        m_label_frame->update();
    };

    m_frame_shape_combobox->on_return_pressed = [&]() {
        m_enabled_label->set_text(m_frame_shape_combobox->text());
    };

    m_thickness_spinbox = basics_tab->find_descendant_of_type_named<GUI::SpinBox>("thickness_spinbox");
    m_thickness_spinbox->set_value(1);

    m_thickness_spinbox->on_change = [&](auto value) {
        m_label_frame->set_frame_thickness(value);
    };

    m_button_icons.append(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/book-open.png").release_value_but_fixme_should_propagate_errors());
    m_button_icons.append(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/inspector-object.png").release_value_but_fixme_should_propagate_errors());
    m_button_icons.append(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/ladybug.png").release_value_but_fixme_should_propagate_errors());

    m_icon_button = basics_tab->find_descendant_of_type_named<GUI::Button>("icon_button");
    m_icon_button->set_icon(*m_button_icons[2]);

    m_disabled_icon_button = basics_tab->find_descendant_of_type_named<GUI::Button>("disabled_icon_button");
    m_disabled_icon_button->set_icon(*m_button_icons[2]);

    m_icon_button->on_click = [&](auto) {
        static size_t i;
        if (i >= m_button_icons.size())
            i = 0;
        m_icon_button->set_icon(*m_button_icons[i]);
        m_disabled_icon_button->set_icon(*m_button_icons[i]);
        i++;
    };

    m_text_editor = basics_tab->find_descendant_of_type_named<GUI::TextEditor>("text_editor");

    m_font_button = basics_tab->find_descendant_of_type_named<GUI::Button>("font_button");
    m_font_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-font-editor.png").release_value_but_fixme_should_propagate_errors());

    m_font_button->on_click = [&](auto) {
        auto picker = GUI::FontPicker::try_create(window(), &m_text_editor->font(), false).release_value_but_fixme_should_propagate_errors();
        if (picker->exec() == GUI::Dialog::ExecOK) {
            m_text_editor->set_font(picker->font());
        }
    };

    m_file_button = basics_tab->find_descendant_of_type_named<GUI::Button>("file_button");
    m_file_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/open.png").release_value_but_fixme_should_propagate_errors());

    m_file_button->on_click = [&](auto) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window());
        if (!open_path.has_value())
            return;
        m_text_editor->set_text(open_path.value());
    };

    m_input_button = basics_tab->find_descendant_of_type_named<GUI::Button>("input_button");
    m_input_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/properties.png").release_value_but_fixme_should_propagate_errors());

    m_input_button->on_click = [&](auto) {
        String value;
        if (GUI::InputBox::show(window(), value, "Enter input:", "Input") == GUI::InputBox::ExecOK && !value.is_empty())
            m_text_editor->set_text(value);
    };

    m_font_colorinput = basics_tab->find_descendant_of_type_named<GUI::ColorInput>("font_colorinput");

    m_font_colorinput->on_change = [&]() {
        auto palette = m_text_editor->palette();
        palette.set_color(Gfx::ColorRole::BaseText, m_font_colorinput->color());
        m_text_editor->set_palette(palette);
        m_text_editor->update();
    };

    m_msgbox_button = basics_tab->find_descendant_of_type_named<GUI::Button>("msgbox_button");
    m_msgbox_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-browser.png").release_value_but_fixme_should_propagate_errors());

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

    m_msgbox_icon_combobox = basics_tab->find_descendant_of_type_named<GUI::ComboBox>("msgbox_icon_combobox");
    m_msgbox_icon_combobox->set_model(*GUI::ItemListModel<String>::create(m_msgbox_icons));
    m_msgbox_icon_combobox->set_selected_index(0);

    m_msgbox_icon_combobox->on_change = [&](auto&, const auto& index) {
        m_msgbox_type = static_cast<GUI::MessageBox::Type>(index.row());
    };

    m_msgbox_buttons_combobox = basics_tab->find_descendant_of_type_named<GUI::ComboBox>("msgbox_buttons_combobox");
    m_msgbox_buttons_combobox->set_model(*GUI::ItemListModel<String>::create(m_msgbox_buttons));
    m_msgbox_buttons_combobox->set_selected_index(0);

    m_msgbox_buttons_combobox->on_change = [&](auto&, const auto& index) {
        m_msgbox_input_type = static_cast<GUI::MessageBox::InputType>(index.row());
    };

    m_msgbox_button->on_click = [&](auto) {
        GUI::MessageBox::show(window(), m_text_editor->text(), "Message", m_msgbox_type, m_msgbox_input_type);
    };

    auto sliders_tab = tab_widget.try_add_tab<GUI::Widget>("Sliders").release_value_but_fixme_should_propagate_errors();
    sliders_tab->load_from_gml(sliders_tab_gml);

    m_vertical_progressbar_left = sliders_tab->find_descendant_of_type_named<GUI::VerticalProgressbar>("vertical_progressbar_left");
    m_vertical_progressbar_left->set_value(0);

    m_vertical_progressbar_right = sliders_tab->find_descendant_of_type_named<GUI::VerticalProgressbar>("vertical_progressbar_right");
    m_vertical_progressbar_right->set_value(100);

    m_vertical_slider_left = sliders_tab->find_descendant_of_type_named<GUI::VerticalSlider>("vertical_slider_left");
    m_vertical_slider_right = sliders_tab->find_descendant_of_type_named<GUI::VerticalSlider>("vertical_slider_right");

    m_vertical_slider_left->on_change = [&](auto value) {
        m_vertical_progressbar_left->set_value(m_vertical_slider_left->max() - value);
    };

    m_vertical_slider_right->on_change = [&](auto value) {
        m_vertical_progressbar_right->set_value((100 / m_vertical_slider_right->max()) * (m_vertical_slider_right->max() - value));
    };

    m_horizontal_progressbar = sliders_tab->find_descendant_of_type_named<GUI::HorizontalProgressbar>("horizontal_progressbar");
    m_horizontal_progressbar->set_value(0);

    m_horizontal_slider_left = sliders_tab->find_descendant_of_type_named<GUI::HorizontalSlider>("horizontal_slider_left");
    m_horizontal_slider_right = sliders_tab->find_descendant_of_type_named<GUI::HorizontalSlider>("horizontal_slider_right");

    m_horizontal_slider_left->on_change = [&](auto value) {
        m_horizontal_progressbar->set_value(value);
        if (!(value % (100 / m_horizontal_slider_right->max())))
            m_horizontal_slider_right->set_value(value / (100 / m_horizontal_slider_right->max()));
    };

    m_horizontal_slider_right->on_change = [&](auto value) {
        m_horizontal_progressbar->set_value((value * 100) / m_horizontal_slider_right->max());
        m_horizontal_slider_left->set_value((value * 100) / m_horizontal_slider_right->max());
    };

    m_enabled_scrollbar = sliders_tab->find_descendant_of_type_named<GUI::Scrollbar>("enabled_scrollbar");
    m_enabled_scrollbar->set_orientation(Orientation::Horizontal);

    m_disabled_scrollbar = sliders_tab->find_descendant_of_type_named<GUI::Scrollbar>("disabled_scrollbar");
    m_disabled_scrollbar->set_orientation(Orientation::Horizontal);

    m_opacity_imagewidget = sliders_tab->find_descendant_of_type_named<GUI::ImageWidget>("opacity_imagewidget");
    m_opacity_imagewidget->load_from_file("/res/graphics/brand-banner.png");

    m_opacity_slider = sliders_tab->find_descendant_of_type_named<GUI::OpacitySlider>("opacity_slider");

    m_opacity_slider->on_change = [&](auto percent) {
        m_opacity_imagewidget->set_opacity_percent(percent);
        m_opacity_value_slider->set_value(percent);
    };

    m_opacity_value_slider = sliders_tab->find_descendant_of_type_named<GUI::ValueSlider>("opacity_value_slider");
    m_opacity_value_slider->set_range(0, 100);

    m_opacity_value_slider->on_change = [&](auto percent) {
        m_opacity_imagewidget->set_opacity_percent(percent);
        m_opacity_slider->set_value(percent);
    };

    auto wizards_tab = tab_widget.try_add_tab<GUI::Widget>("Wizards").release_value_but_fixme_should_propagate_errors();
    wizards_tab->load_from_gml(wizards_tab_gml);

    m_wizard_button = wizards_tab->find_descendant_of_type_named<GUI::Button>("wizard_button");
    m_wizard_output = wizards_tab->find_descendant_of_type_named<GUI::TextEditor>("wizard_output");
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

    m_wizard_output->set_text(String::formatted("{}{}", serenityos_ascii, wizard_ascii));

    m_wizard_button->on_click = [&](auto) {
        StringBuilder sb;
        sb.append(m_wizard_output->get_text());
        sb.append("\nWizard started.");
        m_wizard_output->set_text(sb.to_string());

        auto wizard = DemoWizardDialog::try_create(window()).release_value_but_fixme_should_propagate_errors();
        int result = wizard->exec();

        sb.append(String::formatted("\nWizard execution complete.\nDialog ExecResult code: {}", result));
        if (result == GUI::Dialog::ExecResult::ExecOK)
            sb.append(String::formatted(" (ExecOK)\n'Installation' location: \"{}\"", wizard->page_1_location()));
        m_wizard_output->set_text(sb.string_view());
    };

    auto cursors_tab = tab_widget.try_add_tab<GUI::Widget>("Cursors").release_value_but_fixme_should_propagate_errors();
    cursors_tab->load_from_gml(cursors_tab_gml);

    m_cursors_tableview = cursors_tab->find_descendant_of_type_named<GUI::TableView>("cursors_tableview");
    m_cursors_tableview->set_highlight_selected_rows(true);
    m_cursors_tableview->set_alternating_row_colors(false);
    m_cursors_tableview->set_vertical_padding(16);
    m_cursors_tableview->set_column_headers_visible(false);
    m_cursors_tableview->set_highlight_key_column(false);

    auto sorting_proxy_model = MUST(GUI::SortingProxyModel::create(MouseCursorModel::create()));
    sorting_proxy_model->set_sort_role(GUI::ModelRole::Display);

    m_cursors_tableview->set_model(sorting_proxy_model);
    m_cursors_tableview->set_key_column_and_sort_order(MouseCursorModel::Column::Name, GUI::SortOrder::Ascending);
    m_cursors_tableview->model()->invalidate();
    m_cursors_tableview->set_column_width(0, 25);

    m_cursors_tableview->on_activation = [&](const GUI::ModelIndex& index) {
        auto icon_index = index.model()->index(index.row(), MouseCursorModel::Column::Bitmap);
        m_cursors_tableview->set_override_cursor(NonnullRefPtr<Gfx::Bitmap>(icon_index.data().as_bitmap()));
    };

    auto icons_tab = tab_widget.try_add_tab<GUI::Widget>("Icons").release_value_but_fixme_should_propagate_errors();
    icons_tab->load_from_gml(icons_tab_gml);

    m_icons_tableview = icons_tab->find_descendant_of_type_named<GUI::TableView>("icons_tableview");
    m_icons_tableview->set_highlight_selected_rows(true);
    m_icons_tableview->set_alternating_row_colors(false);
    m_icons_tableview->set_vertical_padding(24);
    m_icons_tableview->set_column_headers_visible(false);
    m_icons_tableview->set_highlight_key_column(false);

    auto sorting_proxy_icons_model = MUST(GUI::SortingProxyModel::create(FileIconsModel::create()));
    sorting_proxy_icons_model->set_sort_role(GUI::ModelRole::Display);

    m_icons_tableview->set_model(sorting_proxy_icons_model);
    m_icons_tableview->set_key_column_and_sort_order(FileIconsModel::Column::Name, GUI::SortOrder::Ascending);
    m_icons_tableview->model()->invalidate();
    m_icons_tableview->set_column_width(0, 36);
    m_icons_tableview->set_column_width(1, 20);
}
