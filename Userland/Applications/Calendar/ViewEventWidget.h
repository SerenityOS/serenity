/*
 * Copyright (c) 2024, Sanil Gupta <sanilg566@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ViewEventDialog.h"
#include <LibGUI/Calendar.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Model.h>
#include <LibGUI/Window.h>

namespace Calendar {

class ViewEventWidget final : public GUI::Widget {
    C_OBJECT(ViewEventWidget);

public:
    static ErrorOr<NonnullRefPtr<ViewEventWidget>> create(ViewEventDialog*, Vector<Event>& events);
    virtual ~ViewEventWidget() override = default;

private:
    ViewEventWidget() = default;
    static ErrorOr<NonnullRefPtr<ViewEventWidget>> try_create();
};

}
