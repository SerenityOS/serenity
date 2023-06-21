/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Presentation.h"
#include <LibGUI/Action.h>
#include <LibGUI/Event.h>
#include <LibGUI/UIDimensions.h>
#include <LibGUI/Widget.h>
#include <LibWebView/OutOfProcessWebView.h>

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
    virtual void second_paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void drag_enter_event(GUI::DragEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;

private:
    void update_web_view();
    void update_slides_actions();

    RefPtr<WebView::OutOfProcessWebView> m_web_view;

    OwnPtr<Presentation> m_current_presentation;
    RefPtr<GUI::Action> m_next_slide_action;
    RefPtr<GUI::Action> m_previous_slide_action;
    RefPtr<GUI::Action> m_present_from_first_slide_action;

    RefPtr<GUI::Action> m_full_screen_action;
    RefPtr<GUI::Action> m_resize_to_fit_content_action;
};
