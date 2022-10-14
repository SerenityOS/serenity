/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Presentation.h"
#include <LibGUI/Action.h>
#include <LibGUI/Event.h>
#include <LibGUI/UIDimensions.h>
#include <LibGUI/Widget.h>

// Title, Author
constexpr StringView const title_template = "{} ({}) — Presenter"sv;

class PresenterWidget : public GUI::Widget {
    C_OBJECT(PresenterWidget);

public:
    PresenterWidget();
    ErrorOr<void> initialize_menubar();

    virtual ~PresenterWidget() override = default;

    // Errors that happen here are directly displayed to the user.
    void set_file(StringView file_name);

protected:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void drag_enter_event(GUI::DragEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

private:
    OwnPtr<Presentation> m_current_presentation;
    RefPtr<GUI::Action> m_next_slide_action;
    RefPtr<GUI::Action> m_previous_slide_action;
};
