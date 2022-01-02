/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Filter.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>

namespace PixelPaint {

RefPtr<GUI::Widget> Filter::get_settings_widget()
{
    if (!m_settings_widget) {
        m_settings_widget = GUI::Widget::construct();
        m_settings_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& name_label = m_settings_widget->add<GUI::Label>(filter_name());
        name_label.set_text_alignment(Gfx::TextAlignment::TopLeft);

        m_settings_widget->add<GUI::Widget>();
    }

    return m_settings_widget.ptr();
}

}
