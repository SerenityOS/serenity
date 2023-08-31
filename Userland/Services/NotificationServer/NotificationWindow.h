/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ImageWidget.h>
#include <LibGUI/Window.h>

namespace NotificationServer {

class NotificationWindow final : public GUI::Window {
    C_OBJECT(NotificationWindow);

public:
    virtual ~NotificationWindow() override = default;
    void set_original_rect(Gfx::IntRect original_rect) { m_original_rect = original_rect; }

    void set_text(String const&);
    void set_title(String const&);
    void set_image(Gfx::ShareableBitmap const&);

    static RefPtr<NotificationWindow> get_window_by_id(i32 id);

protected:
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;

private:
    NotificationWindow(i32 client_id, String const& text, String const& title, Gfx::ShareableBitmap const&);

    virtual void screen_rects_change_event(GUI::ScreenRectsChangeEvent&) override;

    void resize_to_fit_text();
    void set_height(int);

    Gfx::IntRect m_original_rect;
    i32 m_id;

    GUI::Label* m_text_label;
    GUI::Label* m_title_label;
    GUI::ImageWidget* m_image;
    bool m_hovering { false };
};

}
