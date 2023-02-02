/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/AboutDialogGML.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>

namespace GUI {

AboutDialog::AboutDialog(StringView name, StringView version, Gfx::Bitmap const* icon, Window* parent_window)
    : Dialog(parent_window)
    , m_name(name)
    , m_icon(icon)
    , m_version_string(version)
{
    resize(413, 204);
    set_title(DeprecatedString::formatted("About {}", m_name));
    set_resizable(false);

    if (parent_window)
        set_icon(parent_window->icon());

    auto widget = set_main_widget<Widget>().release_value_but_fixme_should_propagate_errors();
    widget->load_from_gml(about_dialog_gml).release_value_but_fixme_should_propagate_errors();

    auto icon_wrapper = find_descendant_of_type_named<Widget>("icon_wrapper");
    if (icon) {
        icon_wrapper->set_visible(true);
        auto icon_image = find_descendant_of_type_named<ImageWidget>("icon");
        icon_image->set_bitmap(m_icon);
    } else {
        icon_wrapper->set_visible(false);
    }

    find_descendant_of_type_named<GUI::Label>("name")->set_text(m_name);
    // If we are displaying a dialog for an application, insert 'SerenityOS' below the application name
    find_descendant_of_type_named<GUI::Label>("serenity_os")->set_visible(m_name != "SerenityOS");
    find_descendant_of_type_named<GUI::Label>("version")->set_text(m_version_string);

    auto ok_button = find_descendant_of_type_named<DialogButton>("ok_button");
    ok_button->on_click = [this](auto) {
        done(ExecResult::OK);
    };
}

}
