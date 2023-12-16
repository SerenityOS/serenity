/*
 * Copyright (c) 2021, Conor Byrne <cbyrneee@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GMLPreviewWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>

namespace HackStudio {

GMLPreviewWidget::GMLPreviewWidget(ByteString const& gml_content)
{
    set_layout<GUI::VerticalBoxLayout>();
    load_gml(gml_content);
}

void GMLPreviewWidget::load_gml(ByteString const& gml)
{
    remove_all_children();

    if (gml.is_empty()) {
        auto& label = add<GUI::Label>();
        label.set_text("Open a .gml file to show the preview"_string);

        return;
    }

    // FIXME: Parsing errors happen while the user is typing. What should we do about them?
    (void)load_from_gml(gml, [](StringView name) -> ErrorOr<NonnullRefPtr<Core::EventReceiver>> {
        return GUI::Label::construct(TRY(String::formatted("{} is not registered as a GML element!", name)));
    });

    if (children().is_empty()) {
        auto& label = add<GUI::Label>();
        label.set_text("Failed to load GML!"_string);
    }
}

}
