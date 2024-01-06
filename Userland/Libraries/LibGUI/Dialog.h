/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/EventLoop.h>
#include <LibGUI/Window.h>

namespace GUI {

class Dialog : public Window {
    C_OBJECT(Dialog)
public:
    enum class ExecResult {
        OK = 0,
        Cancel = 1,
        Aborted = 2,
        Yes = 3,
        No = 4,
        Reveal = 5,
    };

    enum class ScreenPosition {
        DoNotPosition,
        CenterWithinParent,
        Center,
    };

    virtual ~Dialog() override = default;

    ExecResult exec();

    ExecResult result() const { return m_result; }
    void done(ExecResult);

    ScreenPosition screen_position() const { return m_screen_position; }
    void set_screen_position(ScreenPosition position) { m_screen_position = position; }

    virtual void event(Core::Event&) override;

    virtual void close() override;

protected:
    explicit Dialog(Window* parent_window, ScreenPosition = ScreenPosition::CenterWithinParent);

    virtual void on_done(ExecResult) { }

private:
    OwnPtr<Core::EventLoop> m_event_loop;
    ExecResult m_result { ExecResult::Aborted };
    ScreenPosition m_screen_position { ScreenPosition::CenterWithinParent };
};

}

template<>
struct AK::Formatter<GUI::Dialog> : Formatter<Core::EventReceiver> {
};
