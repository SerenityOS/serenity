/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ScopeWidget.h"
#include "Layer.h"
#include <LibConfig/Client.h>

namespace PixelPaint {

ScopeWidget::~ScopeWidget()
{
    if (m_image)
        m_image->remove_client(*this);
}

void ScopeWidget::set_image(Image* image)
{
    if (m_image == image)
        return;
    if (m_image)
        m_image->remove_client(*this);
    m_image = image;
    if (m_image)
        m_image->add_client(*this);

    image_changed();
    update();
}

void ScopeWidget::set_color_at_mouseposition(Color color)
{
    if (m_color_at_mouseposition == color)
        return;

    m_color_at_mouseposition = color;
    update();
}

void ScopeWidget::set_scope_visibility(bool visible)
{
    if (visible != read_visibility_from_configuration())
        Config::write_bool("PixelPaint"sv, "Scopes"sv, widget_config_name(), visible);

    // since we are housed within a other widget we need to set the visibility on our parent widget
    if (parent_widget())
        parent_widget()->set_visible(visible);

    if (visible)
        image_changed();
}

bool ScopeWidget::read_visibility_from_configuration()
{
    return Config::read_bool("PixelPaint"sv, "Scopes"sv, widget_config_name(), false);
}

bool ScopeWidget::should_process_data()
{
    return m_image && read_visibility_from_configuration();
}
}
