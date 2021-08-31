/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EditGuideDialog.h"
#include <Applications/PixelPaint/EditGuideDialogGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

EditGuideDialog::EditGuideDialog(GUI::Window* parent_window)
    : Dialog(parent_window)
{
    set_title("Create new Guide");
    set_icon(parent_window->icon());
    resize(200, 120);
    set_resizable(false);

    auto& main_widget = set_main_widget<GUI::Widget>();
    if (!main_widget.load_from_gml(edit_guide_dialog_gml))
        VERIFY_NOT_REACHED();

    auto horizontal_radio = main_widget.find_descendant_of_type_named<GUI::RadioButton>("orientation_horizontal_radio");
    auto vertical_radio = main_widget.find_descendant_of_type_named<GUI::RadioButton>("orientation_vertical_radio");
    auto offset_text_box = main_widget.find_descendant_of_type_named<GUI::TextBox>("offset_text_box");
    auto ok_button = main_widget.find_descendant_of_type_named<GUI::Button>("ok_button");
    auto cancel_button = main_widget.find_descendant_of_type_named<GUI::Button>("cancel_button");
    VERIFY(horizontal_radio);
    VERIFY(ok_button);
    VERIFY(offset_text_box);
    VERIFY(vertical_radio);
    VERIFY(cancel_button);

    horizontal_radio->on_checked = [this](bool checked) { m_is_horizontal_checked = checked; };
    vertical_radio->on_checked = [this](bool checked) { m_is_vertical_checked = checked; };

    ok_button->on_click = [this, &offset_text_box](auto) {
        if (m_is_vertical_checked) {
            m_orientation = Guide::Orientation::Vertical;
        } else if (m_is_horizontal_checked) {
            m_orientation = Guide::Orientation::Horizontal;
        } else {
            done(ExecResult::ExecAborted);
            return;
        }

        if (offset_text_box->text().is_empty())
            done(ExecResult::ExecAborted);

        m_offset = offset_text_box->text();

        done(ExecResult::ExecOK);
    };

    cancel_button->on_click = [this](auto) {
        done(ExecResult::ExecCancel);
    };
}

}
