/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Window.h>

namespace GUI {

class TooltipWindow final : public Window {
    C_OBJECT(TooltipWindow);

public:
    void set_tooltip(const String& tooltip);

private:
    TooltipWindow();

    RefPtr<Label> m_label;
};

}
