/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/AboutDialogWidget.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>

namespace GUI {

NonnullRefPtr<AboutDialog> AboutDialog::create(String const& name, String version, RefPtr<Gfx::Bitmap const> icon, Window* parent_window)
{
    auto dialog = adopt_ref(*new AboutDialog(name, version, icon, parent_window));
    dialog->set_title(ByteString::formatted("About {}", name));

    auto widget = AboutDialogWidget::try_create().release_value_but_fixme_should_propagate_errors();
    dialog->set_main_widget(widget);

    auto icon_wrapper = widget->find_descendant_of_type_named<Widget>("icon_wrapper");
    if (icon) {
        icon_wrapper->set_visible(true);
        auto icon_image = widget->find_descendant_of_type_named<ImageWidget>("icon");
        icon_image->set_bitmap(icon);
    } else {
        icon_wrapper->set_visible(false);
    }

    widget->find_descendant_of_type_named<GUI::Label>("name")->set_text(name);
    // If we are displaying a dialog for an application, insert 'SerenityOS' below the application name
    widget->find_descendant_of_type_named<GUI::Label>("serenity_os")->set_visible(name != "SerenityOS");
    widget->find_descendant_of_type_named<GUI::Label>("version")->set_text(version);

    auto ok_button = widget->find_descendant_of_type_named<DialogButton>("ok_button");
    ok_button->on_click = [dialog](auto) {
        dialog->done(ExecResult::OK);
    };

    return dialog;
}

AboutDialog::AboutDialog(String const& name, String version, RefPtr<Gfx::Bitmap const> icon, Window* parent_window)
    : Dialog(parent_window)
    , m_name(name)
    , m_version_string(move(version))
    , m_icon(move(icon))
{
    resize(413, 204);
    set_resizable(false);

    if (parent_window)
        set_icon(parent_window->icon());
}

void AboutDialog::show(String name, String version, RefPtr<Gfx::Bitmap const> icon, Window* parent_window, RefPtr<Gfx::Bitmap const> window_icon)
{
    auto dialog = AboutDialog::create(move(name), move(version), move(icon), parent_window);
    if (window_icon)
        dialog->set_icon(window_icon);
    dialog->exec();
}

}
