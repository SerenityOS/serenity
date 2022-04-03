/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "WindowIdentifier.h"
#include <LibGUI/Button.h>

class TaskbarButton final : public GUI::Button {
    C_OBJECT(TaskbarButton)
public:
    virtual ~TaskbarButton() override = default;

    void update_taskbar_rect();
    void clear_taskbar_rect();

private:
    explicit TaskbarButton(WindowIdentifier const&);

    virtual void context_menu_event(GUI::ContextMenuEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;

    WindowIdentifier m_identifier;
};
