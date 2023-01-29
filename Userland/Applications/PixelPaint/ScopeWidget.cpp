/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ScopeWidget.h"
#include "Layer.h"

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

}
