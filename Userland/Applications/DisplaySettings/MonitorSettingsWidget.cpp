/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MonitorSettingsWidget.h"
#include <Applications/DisplaySettings/MonitorSettingsGML.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/RadioButton.h>
#include <LibGfx/SystemTheme.h>

namespace DisplaySettings {

ErrorOr<NonnullRefPtr<MonitorSettingsWidget>> MonitorSettingsWidget::try_create()
{
    auto monitor_settings_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) MonitorSettingsWidget()));

    TRY(monitor_settings_widget->load_current_settings());
    TRY(monitor_settings_widget->create_resolution_list());
    TRY(monitor_settings_widget->create_frame());

    return monitor_settings_widget;
}

ErrorOr<void> MonitorSettingsWidget::create_resolution_list()
{
    m_resolutions.clear();
    m_resolution_strings.clear();

    auto edid = m_screen_edids[m_selected_screen_index];
    if (edid.has_value()) {
        // Try to collect all supported resolutions for the main screen
        auto resolutions = TRY(edid.value().supported_resolutions());
        for (auto& resolution : resolutions) {
            dbgln("Adding EDID supported resolution: {}x{}", resolution.width, resolution.height);
            TRY(m_resolutions.try_append({ resolution.width, resolution.height }));
        }

        // Manually create resolution list if no resolutions were collected
        if (m_resolutions.is_empty())
            edid = {};
    }

    if (!edid.has_value()) {
        // Manually create resolutions list, as the device has failed to provide valid EDID data
        TRY(m_resolutions.try_append({ 640, 480 }));
        TRY(m_resolutions.try_append({ 800, 600 }));
        TRY(m_resolutions.try_append({ 1024, 768 }));
        TRY(m_resolutions.try_append({ 1280, 720 }));
        TRY(m_resolutions.try_append({ 1280, 768 }));
        TRY(m_resolutions.try_append({ 1280, 960 }));
        TRY(m_resolutions.try_append({ 1280, 1024 }));
        TRY(m_resolutions.try_append({ 1360, 768 }));
        TRY(m_resolutions.try_append({ 1368, 768 }));
        TRY(m_resolutions.try_append({ 1440, 900 }));
        TRY(m_resolutions.try_append({ 1600, 900 }));
        TRY(m_resolutions.try_append({ 1600, 1200 }));
        TRY(m_resolutions.try_append({ 1920, 1080 }));
        TRY(m_resolutions.try_append({ 2048, 1152 }));
        TRY(m_resolutions.try_append({ 2256, 1504 }));
        TRY(m_resolutions.try_append({ 2560, 1080 }));
        TRY(m_resolutions.try_append({ 2560, 1440 }));
        TRY(m_resolutions.try_append({ 3440, 1440 }));
        dbgln("EDID unavailable; Adding resolutions manually");
    }

    TRY(generate_resolution_strings());
    return {};
}

ErrorOr<void> MonitorSettingsWidget::generate_resolution_strings()
{
    for (auto resolution : m_resolutions) {
        // Use Euclid's Algorithm to calculate greatest common factor
        i32 a = resolution.width();
        i32 b = resolution.height();
        i32 gcf = 0;
        for (;;) {
            i32 r = a % b;
            if (r == 0) {
                gcf = b;
                break;
            }
            a = b;
            b = r;
        }

        i32 aspect_width = resolution.width() / gcf;
        i32 aspect_height = resolution.height() / gcf;
        TRY(m_resolution_strings.try_append(TRY(String::formatted("{}x{} ({}:{})", resolution.width(), resolution.height(), aspect_width, aspect_height))));
    }

    return {};
}

ErrorOr<void> MonitorSettingsWidget::create_frame()
{
    TRY(load_from_gml(monitor_settings_window_gml));

    m_monitor_widget = *find_descendant_of_type_named<DisplaySettings::MonitorWidget>("monitor_widget");

    m_screen_combo = *find_descendant_of_type_named<GUI::ComboBox>("screen_combo");
    m_screen_combo->set_only_allow_values_from_model(true);
    m_screen_combo->set_model(*GUI::ItemListModel<String>::create(m_screens));
    m_screen_combo->on_change = [this](auto&, const GUI::ModelIndex& index) {
        m_selected_screen_index = index.row();
        auto result = selected_screen_index_or_resolution_changed(DidScreenIndexChange::Yes);
        if (result.is_error())
            GUI::MessageBox::show_error(window(), "Screen info could not be updated"sv);
    };

    m_resolution_combo = *find_descendant_of_type_named<GUI::ComboBox>("resolution_combo");
    m_resolution_combo->set_only_allow_values_from_model(true);
    m_resolution_combo->set_model(*GUI::ItemListModel<String>::create(m_resolution_strings));
    m_resolution_combo->on_change = [this](auto&, const GUI::ModelIndex& index) {
        auto& selected_screen = m_screen_layout.screens[m_selected_screen_index];
        selected_screen.resolution = m_resolutions.at(index.row());
        // Try to auto re-arrange things if there are overlaps or disconnected screens
        m_screen_layout.normalize();
        auto result = selected_screen_index_or_resolution_changed(DidScreenIndexChange::No);
        if (result.is_error()) {
            GUI::MessageBox::show_error(window(), "Screen info could not be updated"sv);
            return;
        }
        set_modified(true);
    };

    m_display_scale_radio_1x = *find_descendant_of_type_named<GUI::RadioButton>("scale_1x");
    m_display_scale_radio_1x->on_checked = [this](bool checked) {
        if (checked) {
            auto& selected_screen = m_screen_layout.screens[m_selected_screen_index];
            selected_screen.scale_factor = 1;
            // Try to auto re-arrange things if there are overlaps or disconnected screens
            m_screen_layout.normalize();
            m_monitor_widget->set_desktop_scale_factor(1);
            m_monitor_widget->update();
            set_modified(true);
        }
    };
    m_display_scale_radio_2x = *find_descendant_of_type_named<GUI::RadioButton>("scale_2x");
    m_display_scale_radio_2x->on_checked = [this](bool checked) {
        if (checked) {
            auto& selected_screen = m_screen_layout.screens[m_selected_screen_index];
            selected_screen.scale_factor = 2;
            // Try to auto re-arrange things if there are overlaps or disconnected screens
            m_screen_layout.normalize();
            m_monitor_widget->set_desktop_scale_factor(2);
            m_monitor_widget->update();
            set_modified(true);
        }
    };

    m_dpi_label = *find_descendant_of_type_named<GUI::Label>("display_dpi");

    m_screen_combo->set_selected_index(m_selected_screen_index);
    TRY(selected_screen_index_or_resolution_changed(DidScreenIndexChange::Yes));
    return {};
}

static ErrorOr<String> display_name_from_edid(EDID::Parser const& edid)
{
    auto manufacturer_name = edid.manufacturer_name();
    auto product_name = edid.display_product_name();

    auto build_manufacturer_product_name = [&]() -> ErrorOr<String> {
        if (product_name.is_empty())
            return TRY(String::from_byte_string(manufacturer_name));
        return String::formatted("{} {}", manufacturer_name, product_name);
    };

    if (auto screen_size = edid.screen_size(); screen_size.has_value()) {
        auto diagonal_inch = hypot(screen_size.value().horizontal_cm(), screen_size.value().vertical_cm()) / 2.54;
        return String::formatted("{} {}\"", TRY(build_manufacturer_product_name()), roundf(diagonal_inch));
    }

    return build_manufacturer_product_name();
}

ErrorOr<void> MonitorSettingsWidget::load_current_settings()
{
    m_screen_layout = GUI::ConnectionToWindowServer::the().get_screen_layout();

    m_screens.clear();
    m_screen_edids.clear();

    size_t virtual_screen_count = 0;
    for (size_t i = 0; i < m_screen_layout.screens.size(); i++) {
        String screen_display_name;
        if (m_screen_layout.screens[i].mode == WindowServer::ScreenLayout::Screen::Mode::Device) {
            if (auto edid = EDID::Parser::from_display_connector_device(m_screen_layout.screens[i].device.value()); !edid.is_error()) { // TODO: multihead
                screen_display_name = TRY(display_name_from_edid(edid.value()));
                TRY(m_screen_edids.try_append(edid.release_value()));
            } else {
                dbgln("Error getting EDID from device {}: {}", m_screen_layout.screens[i].device.value(), edid.error());
                screen_display_name = TRY(String::from_byte_string(m_screen_layout.screens[i].device.value()));
                TRY(m_screen_edids.try_append({}));
            }
        } else {
            dbgln("Frame buffer {} is virtual.", i);
            screen_display_name = TRY(String::formatted("Virtual screen {}", virtual_screen_count++));
            TRY(m_screen_edids.try_append({}));
        }
        if (i == m_screen_layout.main_screen_index)
            TRY(m_screens.try_append(TRY(String::formatted("{}: {} (main screen)", i + 1, screen_display_name))));
        else
            TRY(m_screens.try_append(TRY(String::formatted("{}: {}", i + 1, screen_display_name))));
    }
    m_selected_screen_index = m_screen_layout.main_screen_index;

    if (!m_screen_combo.is_null()) {
        m_screen_combo->set_selected_index(m_selected_screen_index);
        TRY(selected_screen_index_or_resolution_changed(DidScreenIndexChange::Yes));
    }
    return {};
}

ErrorOr<void> MonitorSettingsWidget::selected_screen_index_or_resolution_changed(DidScreenIndexChange screen_index_changed)
{
    // Generate new resolution list only when changing monitors
    if (screen_index_changed == DidScreenIndexChange::Yes)
        TRY(create_resolution_list());

    auto& screen = m_screen_layout.screens[m_selected_screen_index];

    // Let's attempt to find the current resolution based on the screen layout settings
    auto index = m_resolutions.find_first_index(screen.resolution).value_or(0);
    Gfx::IntSize current_resolution = m_resolutions.at(index);

    Optional<unsigned> screen_dpi;
    String screen_dpi_tooltip;
    if (m_screen_edids[m_selected_screen_index].has_value()) {
        auto& edid = m_screen_edids[m_selected_screen_index];
        if (auto screen_size = edid.value().screen_size(); screen_size.has_value()) {
            auto x_cm = screen_size.value().horizontal_cm();
            auto y_cm = screen_size.value().vertical_cm();
            auto diagonal_inch = hypot(x_cm, y_cm) / 2.54;
            auto diagonal_pixels = hypot(current_resolution.width(), current_resolution.height());
            if (diagonal_pixels != 0.0) {
                screen_dpi = diagonal_pixels / diagonal_inch;
                screen_dpi_tooltip = TRY(String::formatted("{} inch display ({}cm x {}cm)", roundf(diagonal_inch), x_cm, y_cm));
            }
        }
    }

    if (screen_dpi.has_value()) {
        auto dpi_label_value = TRY(String::formatted("{} dpi", screen_dpi.value()));
        m_dpi_label->set_tooltip(screen_dpi_tooltip);
        m_dpi_label->set_text(move(dpi_label_value));
        m_dpi_label->set_visible(true);
    } else {
        m_dpi_label->set_visible(false);
    }

    if (screen.scale_factor != 1 && screen.scale_factor != 2) {
        dbgln("unexpected ScaleFactor {}, setting to 1", screen.scale_factor);
        screen.scale_factor = 1;
    }
    (screen.scale_factor == 1 ? m_display_scale_radio_1x : m_display_scale_radio_2x)->set_checked(true, GUI::AllowCallback::No);
    m_monitor_widget->set_desktop_scale_factor(screen.scale_factor);

    // Select the current selected resolution as it may differ
    m_monitor_widget->set_desktop_resolution(current_resolution);
    m_resolution_combo->set_selected_index(index, GUI::AllowCallback::No);

    m_monitor_widget->update();

    return {};
}

void MonitorSettingsWidget::apply_settings()
{
    // Fetch the latest configuration again, in case it has been changed by someone else.
    // This isn't technically race free, but if the user automates changing settings we can't help...
    auto current_layout = GUI::ConnectionToWindowServer::the().get_screen_layout();
    if (m_screen_layout != current_layout) {
        auto result = GUI::ConnectionToWindowServer::the().set_screen_layout(m_screen_layout, false);
        // Run load_current_settings to refresh screen info.
        if (result.success() && !load_current_settings().is_error()) {
            auto seconds_until_revert = 10;

            auto box_text = [this, &seconds_until_revert]() -> ErrorOr<String> {
                auto output = String::formatted("Do you want to keep the new screen layout?\nReverting in {} {}.",
                    seconds_until_revert, seconds_until_revert == 1 ? "second" : "seconds");
                if (output.is_error()) {
                    GUI::MessageBox::show_error(window(), "Unable to apply changes"sv);
                    return Error::from_string_literal("Unable to create a formatted string");
                }
                return output.release_value();
            };

            auto current_box_text_or_error = box_text();
            if (current_box_text_or_error.is_error())
                return;
            auto current_box_text = current_box_text_or_error.release_value();

            auto box = GUI::MessageBox::create(window(), current_box_text, "Confirm Settings"sv,
                GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo)
                           .release_value_but_fixme_should_propagate_errors();
            box->set_icon(window()->icon());

            // If after 10 seconds the user doesn't close the message box, just close it.
            auto revert_timer = Core::Timer::create_repeating(1000, [&] {
                seconds_until_revert -= 1;
                current_box_text_or_error = box_text();
                if (current_box_text_or_error.is_error()) {
                    seconds_until_revert = 0;
                    box->close();
                    return;
                }
                auto current_box_text = current_box_text_or_error.release_value();
                box->set_text(current_box_text);
                if (seconds_until_revert <= 0) {
                    box->close();
                }
            });
            revert_timer->start();

            // If the user selects "No", closes the window or the window gets closed by the 10 seconds timer, revert the changes.
            if (box->exec() == GUI::MessageBox::ExecResult::Yes) {
                auto save_result = GUI::ConnectionToWindowServer::the().save_screen_layout();
                if (!save_result.success()) {
                    auto detailed_error_message = String::formatted("Error saving settings: {}", save_result.error_msg());
                    if (!detailed_error_message.is_error())
                        GUI::MessageBox::show_error(window(), detailed_error_message.release_value());
                    else
                        GUI::MessageBox::show_error(window(), "Unable to save settings"sv);
                }
            } else {
                auto restore_result = GUI::ConnectionToWindowServer::the().set_screen_layout(current_layout, false);
                if (!restore_result.success() || load_current_settings().is_error()) {
                    auto detailed_error_message = String::formatted("Error restoring settings: {}", restore_result.error_msg());
                    if (!detailed_error_message.is_error())
                        GUI::MessageBox::show_error(window(), detailed_error_message.release_value());
                    else
                        GUI::MessageBox::show_error(window(), "Unable to restore settings"sv);
                }
            }
        } else {
            auto detailed_error_message = String::formatted("Error setting screen layout: {}", result.error_msg());
            if (!detailed_error_message.is_error())
                GUI::MessageBox::show_error(window(), detailed_error_message.release_value());
            else
                GUI::MessageBox::show_error(window(), "Unable to set screen layout"sv);
        }
    }
}

void MonitorSettingsWidget::show_screen_numbers(bool show)
{
    if (m_showing_screen_numbers == show)
        return;
    m_showing_screen_numbers = show;
    GUI::ConnectionToWindowServer::the().async_show_screen_numbers(show);
}

void MonitorSettingsWidget::show_event(GUI::ShowEvent&)
{
    show_screen_numbers(true);
}

void MonitorSettingsWidget::hide_event(GUI::HideEvent&)
{
    show_screen_numbers(false);
}

}
