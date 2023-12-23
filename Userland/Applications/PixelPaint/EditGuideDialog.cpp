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

EditGuideDialog::EditGuideDialog(GUI::Window* parent_window, ByteString const& offset, Guide::Orientation orientation)
    : Dialog(parent_window)
    , m_offset(offset)
    , m_orientation(orientation)
{
    set_title("Create New Guide");
    set_icon(parent_window->icon());
    resize(200, 130);
    set_resizable(false);

    auto main_widget = set_main_widget<GUI::Widget>();
    main_widget->load_from_gml(edit_guide_dialog_gml).release_value_but_fixme_should_propagate_errors();

    auto horizontal_radio = main_widget->find_descendant_of_type_named<GUI::RadioButton>("orientation_horizontal_radio");
    auto vertical_radio = main_widget->find_descendant_of_type_named<GUI::RadioButton>("orientation_vertical_radio");
    auto ok_button = main_widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    auto cancel_button = main_widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    m_offset_text_box = main_widget->find_descendant_of_type_named<GUI::TextBox>("offset_text_box");

    VERIFY(horizontal_radio);
    VERIFY(ok_button);
    VERIFY(!m_offset_text_box.is_null());
    VERIFY(vertical_radio);
    VERIFY(cancel_button);

    if (orientation == Guide::Orientation::Vertical) {
        vertical_radio->set_checked(true);
        m_is_vertical_checked = true;
    } else if (orientation == Guide::Orientation::Horizontal) {
        horizontal_radio->set_checked(true);
        m_is_horizontal_checked = true;
    }

    if (!offset.is_empty())
        m_offset_text_box->set_text(offset);

    horizontal_radio->on_checked = [this](bool checked) { m_is_horizontal_checked = checked; };
    vertical_radio->on_checked = [this](bool checked) { m_is_vertical_checked = checked; };

    ok_button->on_click = [this](auto) {
        if (m_is_vertical_checked) {
            m_orientation = Guide::Orientation::Vertical;
        } else if (m_is_horizontal_checked) {
            m_orientation = Guide::Orientation::Horizontal;
        } else {
            done(ExecResult::Aborted);
            return;
        }

        if (m_offset_text_box->text().is_empty())
            done(ExecResult::Aborted);

        m_offset = m_offset_text_box->text();

        done(ExecResult::OK);
    };
    ok_button->set_default(true);

    cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };
}

Optional<float> EditGuideDialog::offset_as_pixel(ImageEditor const& editor)
{
    float offset = 0;
    if (m_offset.ends_with('%')) {
        auto percentage = m_offset.substring_view(0, m_offset.length() - 1).to_number<int>();
        if (!percentage.has_value())
            return {};

        if (orientation() == PixelPaint::Guide::Orientation::Horizontal)
            offset = editor.image().size().height() * ((double)percentage.value() / 100.0);
        else if (orientation() == PixelPaint::Guide::Orientation::Vertical)
            offset = editor.image().size().width() * ((double)percentage.value() / 100.0);
    } else {
        auto parsed_int = m_offset.to_number<int>();
        if (!parsed_int.has_value())
            return {};
        offset = parsed_int.value();
    }

    return offset;
}

}
