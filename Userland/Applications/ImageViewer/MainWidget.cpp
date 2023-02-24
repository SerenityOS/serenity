/*
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"

namespace ImageViewer {

MainWidget::MainWidget()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>(GUI::Margins {}, 2);
}

void MainWidget::keydown_event(GUI::KeyEvent& event)
{
    event.ignore();
}

}
