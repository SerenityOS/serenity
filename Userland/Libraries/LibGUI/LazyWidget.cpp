/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/LazyWidget.h>

namespace GUI {

LazyWidget::LazyWidget()
{
}

LazyWidget::~LazyWidget()
{
}

void LazyWidget::show_event(ShowEvent&)
{
    if (m_has_been_shown)
        return;
    m_has_been_shown = true;

    VERIFY(on_first_show);
    on_first_show(*this);
}

}
