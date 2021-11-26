/*
 * Copyright (c) 2021, Conor Byrne <cbyrneee@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GMLPreviewWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>

namespace HackStudio {

GMLPreviewWidget::GMLPreviewWidget(String const& gml_content)
{
    set_layout<GUI::VerticalBoxLayout>();
    load_gml(gml_content);
}

void GMLPreviewWidget::load_gml(String const& gml)
{
    remove_all_children();

    if (gml.is_empty()) {
        auto& label = add<GUI::Label>();
        label.set_text("Open a .gml file to show the preview");

        return;
    }

    load_from_gml(gml, [](const String& name) -> RefPtr<Core::Object> {
        return GUI::Label::construct(String::formatted("{} is not registered as a GML element!", name));
    });

    if (children().is_empty()) {
        auto& label = add<GUI::Label>();
        label.set_text("Failed to load GML!");
    }
}

}
