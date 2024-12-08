/*
 * Copyright (c) 2024, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

class TaskbarFrame : public GUI::Frame {
    C_OBJECT(TaskbarFrame)
public:
    using GUI::Frame::Frame;

    virtual void paint_event(GUI::PaintEvent&) override;
};
