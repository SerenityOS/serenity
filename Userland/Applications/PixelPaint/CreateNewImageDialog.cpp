/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CreateNewImageDialog.h"
#include <AK/Array.h>
#include <LibConfig/Client.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>

namespace PixelPaint {

CreateNewImageDialog::CreateNewImageDialog(GUI::Window* parent_window)
    : Dialog(parent_window)
{
    set_title("Create New Image");
    set_icon(parent_window->icon());
    resize(200, 220);

    auto main_widget = set_main_widget<GUI::Widget>();
    main_widget->set_fill_with_background_color(true);

    main_widget->set_layout<GUI::VerticalBoxLayout>(4);

    auto& name_label = main_widget->add<GUI::Label>("Name:"_string);
    name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    m_name_textbox = main_widget->add<GUI::TextBox>();
    m_name_textbox->on_change = [this] {
        m_image_name = m_name_textbox->text();
    };
    auto default_name = Config::read_string("PixelPaint"sv, "NewImage"sv, "Name"sv);
    m_name_textbox->set_text(default_name);

    auto& width_label = main_widget->add<GUI::Label>("Width:"_string);
    width_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto& width_spinbox = main_widget->add<GUI::SpinBox>();

    auto& height_label = main_widget->add<GUI::Label>("Height:"_string);
    height_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto& height_spinbox = main_widget->add<GUI::SpinBox>();

    enum class BackgroundIndex {
        Transparent = 0,
        White,
        Black,
        Custom
    };

    static constexpr AK::Array suggested_backgrounds = {
        "Transparent"sv,
        "White"sv,
        "Black"sv,
        "Custom"sv
    };

    m_background_color = Color::from_string(
        Config::read_string("PixelPaint"sv, "NewImage"sv, "Background"sv))
                             .value_or(Color::Transparent);

    auto selected_background_index = [&] {
        if (m_background_color == Gfx::Color::Transparent)
            return BackgroundIndex::Transparent;
        if (m_background_color == Gfx::Color::White)
            return BackgroundIndex::White;
        if (m_background_color == Gfx::Color::Black)
            return BackgroundIndex::Black;
        return BackgroundIndex::Custom;
    }();

    auto& background_label = main_widget->add<GUI::Label>("Background:"_string);
    background_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    auto& background_color_combo = main_widget->add<GUI::ComboBox>();
    auto& background_color_input = main_widget->add<GUI::ColorInput>();
    background_color_input.set_visible(false);
    background_color_combo.set_only_allow_values_from_model(true);
    background_color_combo.set_model(*GUI::ItemListModel<StringView, decltype(suggested_backgrounds)>::create(suggested_backgrounds));
    background_color_combo.on_change = [&](auto&, const GUI::ModelIndex& index) {
        auto background_index = static_cast<BackgroundIndex>(index.row());
        m_background_color = [&]() -> Gfx::Color {
            switch (background_index) {
            case BackgroundIndex::Transparent:
                return Gfx::Color::Transparent;
            case BackgroundIndex::White:
                return Gfx::Color::White;
            case BackgroundIndex::Black:
                return Gfx::Color::Black;
            default:
                return m_background_color;
            }
        }();
        background_color_input.set_color(m_background_color);
        background_color_input.set_visible(background_index == BackgroundIndex::Custom);
    };
    background_color_combo.set_selected_index(to_underlying(selected_background_index));
    background_color_input.on_change = [&] {
        m_background_color = background_color_input.color();
    };

    auto& set_defaults_checkbox = main_widget->add<GUI::CheckBox>();
    set_defaults_checkbox.set_text("Use these settings as default"_string);

    auto& button_container = main_widget->add<GUI::Widget>();
    button_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& ok_button = button_container.add<GUI::Button>("OK"_string);
    ok_button.on_click = [&](auto) {
        if (set_defaults_checkbox.is_checked()) {
            Config::write_string("PixelPaint"sv, "NewImage"sv, "Name"sv, m_image_name);
            Config::write_i32("PixelPaint"sv, "NewImage"sv, "Width"sv, m_image_size.width());
            Config::write_i32("PixelPaint"sv, "NewImage"sv, "Height"sv, m_image_size.height());
            Config::write_string("PixelPaint"sv, "NewImage"sv, "Background"sv, m_background_color.to_byte_string());
        }

        done(ExecResult::OK);
    };
    ok_button.set_default(true);

    auto& cancel_button = button_container.add<GUI::Button>("Cancel"_string);
    cancel_button.on_click = [this](auto) {
        done(ExecResult::Cancel);
    };

    width_spinbox.on_change = [this](int value) {
        m_image_size.set_width(value);
    };

    height_spinbox.on_change = [this](int value) {
        m_image_size.set_height(value);
    };

    width_spinbox.set_range(1, 16384);
    height_spinbox.set_range(1, 16384);

    auto default_width = Config::read_i32("PixelPaint"sv, "NewImage"sv, "Width"sv, 510);
    auto default_height = Config::read_i32("PixelPaint"sv, "NewImage"sv, "Height"sv, 356);
    width_spinbox.set_value(default_width);
    height_spinbox.set_value(default_height);
}

}
