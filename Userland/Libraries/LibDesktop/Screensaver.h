/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/Time.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Point.h>

namespace Desktop {

class Screensaver : public GUI::Widget {
    C_OBJECT_ABSTRACT(Screensaver)
public:
    Function<void()> on_screensaver_exit;

    static ErrorOr<NonnullRefPtr<GUI::Window>> create_window(StringView title, StringView icon);

    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;

protected:
    Screensaver()
        : m_start_time(MonotonicTime::now())
    {
    }

private:
    void trigger_exit();

    Optional<Gfx::IntPoint> m_mouse_origin;
    MonotonicTime m_start_time;
};

}
