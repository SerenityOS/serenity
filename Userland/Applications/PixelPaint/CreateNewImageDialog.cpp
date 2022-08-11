/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CreateNewImageDialog.h"
#include <LibConfig/Client.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>

namespace PixelPaint {

CreateNewImageDialog::CreateNewImageDialog(GUI::Window* parent_window)
    : Dialog(parent_window)
{
    set_title("Create new image");
    set_icon(parent_window->icon());
    resize(200, 220);

    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);

    auto& layout = main_widget.set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins(4);

    auto& name_label = main_widget.add<GUI::Label>("Name:");
    name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    m_name_textbox = main_widget.add<GUI::TextBox>();
    m_name_textbox->on_change = [this] {
        m_image_name = m_name_textbox->text();
    };
    auto default_name = Config::read_string("PixelPaint"sv, "NewImage"sv, "Name"sv);
    m_name_textbox->set_text(default_name);

    auto& width_label = main_widget.add<GUI::Label>("Width:");
    width_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto& width_spinbox = main_widget.add<GUI::SpinBox>();

    auto& height_label = main_widget.add<GUI::Label>("Height:");
    height_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto& height_spinbox = main_widget.add<GUI::SpinBox>();

    auto& set_defaults_checkbox = main_widget.add<GUI::CheckBox>();
    set_defaults_checkbox.set_text("Use these settings as default");

    auto& button_container = main_widget.add<GUI::Widget>();
    button_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& ok_button = button_container.add<GUI::Button>("OK");
    ok_button.on_click = [&](auto) {
        if (set_defaults_checkbox.is_checked()) {
            Config::write_string("PixelPaint"sv, "NewImage"sv, "Name"sv, m_image_name);
            Config::write_i32("PixelPaint"sv, "NewImage"sv, "Width"sv, m_image_size.width());
            Config::write_i32("PixelPaint"sv, "NewImage"sv, "Height"sv, m_image_size.height());
        }

        done(ExecResult::OK);
    };
    ok_button.set_default(true);

    auto& cancel_button = button_container.add<GUI::Button>("Cancel");
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
