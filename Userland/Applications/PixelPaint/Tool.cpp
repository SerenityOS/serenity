/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tool.h"
#include "ImageEditor.h"
#include <LibGUI/Action.h>

namespace PixelPaint {

Tool::Tool()
{
}

Tool::~Tool()
{
}

void Tool::setup(ImageEditor& editor)
{
    m_editor = editor;
}

void Tool::set_action(GUI::Action* action)
{
    m_action = action;
}

}
