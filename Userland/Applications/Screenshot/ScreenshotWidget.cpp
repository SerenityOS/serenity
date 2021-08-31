/*
 * Copyright (c) 2021, the SerenityOS Developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ScreenshotWidget.h"
#include "SelectableOverlay.h"
#include <Applications/Screenshot/ScreenshotWindowGML.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/Timer.h>
#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/Notification.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/PNGWriter.h>
#include <LibConfig/Client.h>
#include <serenity.h>

void ScreenshotWidget::timer_event(Core::TimerEvent&)
{
    stop_timer();

    window()->close();

    save_screenshot(false);
}

void ScreenshotWidget::set_path(String const& path)
{
    m_output_path = path;
}

ScreenshotWidget::ScreenshotWidget()
{
    load_from_gml(screenshot_window_gml);

    m_whole_button = *find_descendant_of_type_named<GUI::RadioButton>("wholescreen");
    m_custom_button = *find_descendant_of_type_named<GUI::RadioButton>("customregion");
    m_delay_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("delay");
    m_copy_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("copy_to_clipboard");
    m_cancel_button = *find_descendant_of_type_named<GUI::Button>("cancel_button");
    m_ok_button = *find_descendant_of_type_named<GUI::Button>("ok_button");

    m_window_sel = GUI::Window::construct();
    m_overlay = m_window_sel->set_main_widget<SelectableOverlay>(m_window_sel);
    m_overlay->callback = [&] {
        save_screenshot(true);

        GUI::Application::the()->quit();
    };

    m_window_sel->set_title("Screenshot");
    m_window_sel->set_has_alpha_channel(true);
    m_window_sel->set_fullscreen(true);
    m_window_sel->set_frameless(true);

    String type = Config::read_string("Screenshot", "General", "ScreenshotType", "Whole");
    if (type == "Custom") {
        m_whole_button->set_checked(false);
        m_custom_button->set_checked(true);
    } else {
        m_whole_button->set_checked(true);
        m_custom_button->set_checked(false);
    }

    m_delay_spinbox->set_value(Config::read_i32("Screenshot", "General", "Delay", 0));
    m_copy_checkbox->set_checked(Config::read_bool("Screenshot", "General", "CopyToClipboard", true));

    m_cancel_button->on_click = [&](auto) {
        GUI::Application::the()->quit();
    };

    m_ok_button->on_click = [&](auto) {
        String new_type = "Whole";
        if (m_custom_button->is_checked()) {
            new_type = "Custom";
        }

        Config::write_string("Screenshot", "General", "ScreenshotType", new_type);
        Config::write_i32("Screenshot", "General", "Delay", m_delay_spinbox->value());
        Config::write_bool("Screenshot", "General", "CopyToClipboard", m_copy_checkbox->is_checked());

        if (m_output_path.is_empty()) {
            String name = Core::DateTime::now().to_string("screenshot-%Y-%m-%d-%H-%M-%S.png");
            m_output_path = isatty(STDOUT_FILENO) ? name : String::formatted("{}/Pictures/Screenshots/{}", Core::StandardPaths::home_directory(), name);
        }

        GUI::Application::the()->set_quit_when_last_window_deleted(false);
        window()->close();

        start_timer(m_delay_spinbox->value());
    };
}

void ScreenshotWidget::save_screenshot(bool from_callback)
{
    Gfx::IntRect crop_region;
    if (m_custom_button->is_checked()) {
        if (from_callback) {
            crop_region = m_overlay->region();
            if (crop_region.is_empty()) {
                return;
            }
        } else {
            m_window_sel->show();
            return;
        }
    }

    dbgln("Taking screenshot");
    Optional<u32> screen_index;
    auto screen_bitmap = GUI::WindowServerConnection::the().get_screen_bitmap(crop_region, screen_index);
    dbgln("Taken screenshot");

    RefPtr<Gfx::Bitmap> bitmap = screen_bitmap.bitmap();
    if (!bitmap) {
        warnln("Failed to grab screenshot!");
        return;
    }

    if (m_copy_checkbox->is_checked()) {
        GUI::Clipboard::the().set_bitmap(*bitmap);

        auto notification = GUI::Notification::construct();
        notification->set_title("Screenshot taken");
        notification->set_text("Screenshot saved to clipboard");
        notification->set_icon(GUI::Icon::default_icon("app-screenshot").bitmap_for_size(32));
        notification->show();

        return;
    }

    auto encoded_bitmap = Gfx::PNGWriter::encode(*bitmap);
    if (encoded_bitmap.is_empty()) {
        warnln("Failed to encode PNG");
        return;
    }

    Core::File::ensure_parent_directories(String::formatted("{}/{}", Core::File::current_working_directory(), m_output_path));
    auto file_or_error = Core::File::open(m_output_path, Core::OpenMode::WriteOnly);
    if (file_or_error.is_error()) {
        warnln("Could not open '{}' for writing: {}", m_output_path, file_or_error.error());
        return;
    }

    auto& file = *file_or_error.value();
    if (!file.write(encoded_bitmap.data(), encoded_bitmap.size())) {
        warnln("Failed to write PNG");
        return;
    }

    auto notification = GUI::Notification::construct();
    notification->set_title("Screenshot taken");
    notification->set_text(String::formatted("Screenshot saved at {}", m_output_path));
    notification->set_icon(GUI::Icon::default_icon("app-screenshot").bitmap_for_size(32));
    notification->show();

    GUI::Application::the()->quit();
}

ScreenshotWidget::~ScreenshotWidget()
{
}
