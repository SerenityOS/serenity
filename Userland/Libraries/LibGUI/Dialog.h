/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Window.h>

namespace GUI {

class Dialog : public Window {
    C_OBJECT(Dialog)
public:
    enum ExecResult {
        ExecOK = 0,
        ExecCancel = 1,
        ExecAborted = 2,
        ExecYes = 3,
        ExecNo = 4,
    };

    virtual ~Dialog() override;

    int exec();

    int result() const { return m_result; }
    void done(int result);

    virtual void event(Core::Event&) override;

    virtual void close() override;

protected:
    explicit Dialog(Window* parent_window);

private:
    OwnPtr<Core::EventLoop> m_event_loop;
    int m_result { ExecAborted };
};

}

template<>
struct AK::Formatter<GUI::Dialog> : Formatter<Core::Object> {
};
