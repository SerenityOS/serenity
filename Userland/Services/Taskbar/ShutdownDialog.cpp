/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ShutdownDialog.h"
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>

struct Option {
    String title;
    Vector<char const*> cmd;
    bool enabled;
    bool default_action;
};

static const Vector<Option> options = {
    { "Power off computer", { "/bin/shutdown", "--now", "--force", nullptr }, true, true },
    { "Reboot", { "/bin/reboot", "--force", nullptr }, true, false },
    { "Log out", { "/bin/logout", "--force", nullptr }, true, false },
};

Vector<char const*> ShutdownDialog::show()
{
    auto dialog = ShutdownDialog::construct();
    auto rc = dialog->exec();
    if (rc == ExecResult::ExecOK && dialog->m_selected_option != -1)
        return options[dialog->m_selected_option].cmd;

    return {};
}

ShutdownDialog::ShutdownDialog()
    : Dialog(nullptr)
{
    auto& widget = set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.layout()->set_spacing(0);

    auto& banner_image = widget.add<GUI::ImageWidget>();
    banner_image.load_from_file("/res/graphics/brand-banner.png");

    auto& content_container = widget.add<GUI::Widget>();
    content_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& left_container = content_container.add<GUI::Widget>();
    left_container.set_fixed_width(60);
    left_container.set_layout<GUI::VerticalBoxLayout>();
    left_container.layout()->set_margins({ 12, 0, 0 });

    auto& icon_wrapper = left_container.add<GUI::Widget>();
    icon_wrapper.set_fixed_size(32, 48);
    icon_wrapper.set_layout<GUI::VerticalBoxLayout>();

    auto& icon_image = icon_wrapper.add<GUI::ImageWidget>();
    icon_image.set_bitmap(Gfx::Bitmap::try_load_from_file("/res/icons/32x32/shutdown.png").release_value_but_fixme_should_propagate_errors());

    auto& right_container = content_container.add<GUI::Widget>();
    right_container.set_layout<GUI::VerticalBoxLayout>();
    right_container.layout()->set_margins({ 12, 12, 8, 0 });

    auto& label = right_container.add<GUI::Label>("What would you like to do?");
    label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    label.set_fixed_height(22);
    label.set_font(Gfx::FontDatabase::default_font().bold_variant());

    for (size_t i = 0; i < options.size(); i++) {
        auto action = options[i];
        auto& radio = right_container.add<GUI::RadioButton>();
        radio.set_enabled(action.enabled);
        radio.set_text(action.title);

        radio.on_checked = [this, i](auto) {
            m_selected_option = i;
        };

        if (action.default_action) {
            radio.set_checked(true);
            m_selected_option = i;
        }
    }

    right_container.layout()->add_spacer();

    auto& button_container = right_container.add<GUI::Widget>();
    button_container.set_fixed_height(23);
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container.layout()->set_spacing(5);
    button_container.layout()->add_spacer();
    auto& ok_button = button_container.add<GUI::Button>("OK");
    ok_button.set_fixed_size(80, 23);
    ok_button.on_click = [this](auto) {
        done(Dialog::ExecOK);
    };
    auto& cancel_button = button_container.add<GUI::Button>("Cancel");
    cancel_button.set_fixed_size(80, 23);
    cancel_button.on_click = [this](auto) {
        done(ExecResult::ExecCancel);
    };

    resize(413, 235);
    center_on_screen();
    set_resizable(false);
    set_title("Exit SerenityOS");
    set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/power.png").release_value_but_fixme_should_propagate_errors());

    // Request WindowServer to re-update us on the current theme as we might've not been alive for the last notification.
    refresh_system_theme();
}

ShutdownDialog::~ShutdownDialog()
{
}
