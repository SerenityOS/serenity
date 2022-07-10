/*
 * Copyright (c) 2022, Karol Kosek <krkk@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ExportPNGDialog.h"
#include <Applications/PixelPaint/ExportPNGDialogGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ValueSlider.h>

namespace PixelPaint {

ExportPNGDialog::ExportPNGDialog(Core::File& file, Image const& image, GUI::Window* parent_window)
    : GUI::Dialog(parent_window)
    , m_file(file)
    , m_image(image)
{
    set_title("Export PNG");
    set_icon(parent_window->icon());

    auto& main_widget = set_main_widget<GUI::Widget>();
    if (!main_widget.load_from_gml(export_png_dialog_gml))
        VERIFY_NOT_REACHED();

    resize(305, 115);
    set_resizable(false);

    m_transparency_checkbox = main_widget.find_descendant_of_type_named<GUI::CheckBox>("transparency_checkbox");
    m_compression_slider = main_widget.find_descendant_of_type_named<GUI::ValueSlider>("compression_slider");
    auto apply_button = main_widget.find_descendant_of_type_named<GUI::Button>("apply_button");
    auto cancel_button = main_widget.find_descendant_of_type_named<GUI::Button>("cancel_button");

    VERIFY(m_transparency_checkbox);
    VERIFY(m_compression_slider);
    VERIFY(apply_button);
    VERIFY(cancel_button);

    apply_button->on_click = [this](auto) {
        Image::ExportPNGOptions options {
            .preserve_transparency = m_transparency_checkbox->is_checked(),
            .compression_level = static_cast<Compress::ZlibCompressionLevel>(m_compression_slider->value()),
        };

        auto result = m_image.export_png_to_file(m_file, options);
        if (result.is_error()) {
            GUI::MessageBox::show_error(this, String::formatted("Export to PNG failed: {}", result.error()));
            return;
        }

        done(ExecResult::OK);
    };
    apply_button->set_default(true);

    cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };
}

}
