/*
 * Copyright (c) 2024, circl <circl.lastname@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWindow.h"
#include "MainWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/Process.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>

namespace Screenshot {

MainWindow::MainWindow()
{
    auto app_icon = GUI::Icon::default_icon("app-screenshot"sv);

    set_title("Screenshot");
    set_icon(app_icon.bitmap_for_size(16));
    resize(300, 220);
    set_resizable(false);
    set_minimizable(false);

    auto main_widget = MUST(MainWidget::try_create());
    set_main_widget(main_widget);

    m_ok_button = *main_widget->find_descendant_of_type_named<GUI::DialogButton>("ok_button");
    m_ok_button->set_default(true);
    m_ok_button->on_click = [this](auto) {
        take_screenshot();
    };

    m_cancel_button = *main_widget->find_descendant_of_type_named<GUI::DialogButton>("cancel_button");
    m_cancel_button->on_click = [this](auto) {
        close();
    };

    m_browse = *main_widget->find_descendant_of_type_named<GUI::Button>("browse");
    m_browse->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"sv).release_value_but_fixme_should_propagate_errors());
    m_browse->on_click = [this](auto) {
        auto filepath = GUI::FilePicker::get_open_filepath(this, "Save screenshot to...", m_destination->text(), true);

        if (filepath.has_value()) {
            Config::write_string("Screenshot"sv, "General"sv, "SavePath"sv, filepath.value());
            m_destination->set_text(filepath.value());
            m_destination->repaint();
        }
    };

    m_selected_area = *main_widget->find_descendant_of_type_named<GUI::RadioButton>("selected_area");

    m_output_radio_clipboard = *main_widget->find_descendant_of_type_named<GUI::RadioButton>("output_radio_clipboard");

    m_output_radio_pixel_paint = *main_widget->find_descendant_of_type_named<GUI::RadioButton>("output_radio_pixel_paint");

    m_output_radio_file = *main_widget->find_descendant_of_type_named<GUI::RadioButton>("output_radio_file");

    m_output_radio_file->on_checked = [this](bool is_checked) {
        m_browse->set_enabled(is_checked);
        m_destination->set_enabled(is_checked);
    };

    m_destination = *main_widget->find_descendant_of_type_named<GUI::TextBox>("destination");
    m_destination->set_text(Config::read_string("Screenshot"sv, "General"sv, "SavePath"sv, Core::StandardPaths::pictures_directory()));
}

void MainWindow::take_screenshot()
{
    close();

    Vector<StringView> arguments;

    if (m_selected_area->is_checked())
        arguments.append("-r"sv);

    if (m_output_radio_pixel_paint->is_checked())
        arguments.append("-e"sv);
    else if (m_output_radio_clipboard->is_checked())
        arguments.append("-c"sv);

    // FIXME: Place common screenshot code into library and use that
    MUST(Core::Process::spawn("/bin/shot"sv, arguments, m_destination->text(), Core::Process::KeepAsChild::No));
}

}
