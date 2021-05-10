/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

class SpeechBubble final : public GUI::Widget {
    C_OBJECT(SpeechBubble);

public:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;

    Function<void()> on_dismiss;

private:
    SpeechBubble() = default;
};
