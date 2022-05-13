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
    };
    enum class ScreenPosition {
        CenterWithinParent = 0,

        Center = 1,
        CenterLeft = 2,
        CenterRight = 3,

        TopLeft = 4,
        TopCenter = 5,
        TopRight = 6,

        BottomLeft = 7,
        BottomCenter = 8,
        BottomRight = 9,
    };

    virtual ~Dialog() override = default;

    ExecResult exec();

    ExecResult result() const { return m_result; }
    void done(ExecResult);

    virtual void event(Core::Event&) override;

    virtual void close() override;

protected:
    explicit Dialog(Window* parent_window, ScreenPosition = ScreenPosition::CenterWithinParent);

private:
    OwnPtr<Core::EventLoop> m_event_loop;
    ExecResult m_result { ExecResult::Aborted };
    ScreenPosition m_screen_position { ScreenPosition::CenterWithinParent };
};

}

template<>
struct AK::Formatter<GUI::Dialog> : Formatter<Core::Object> {
};
