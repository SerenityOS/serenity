/*
 * Copyright (c) 2021, Conor Byrne <cbyrneee@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GMLPreviewDialog.h"
#include <LibGUI/BoxLayout.h>

namespace HackStudio {

GMLPreviewDialog::GMLPreviewDialog(String const& content, String const& filename)
    : GUI::Dialog(nullptr)
{
    set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-hack-studio.png"));
    center_on_screen();
    resize(300, 300);
    set_resizable(true);

    m_gml_preview = set_main_widget<GMLPreviewWidget>(content);
    m_gml_preview->set_fill_with_background_color(true);

    load_gml(content, filename);
}

void GMLPreviewDialog::load_gml(String const& content, String const& filename)
{
    m_gml_preview->load_gml(content);
    set_title(String::formatted("GML Preview: {}", filename));
}

}
