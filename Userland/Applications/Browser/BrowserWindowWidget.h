/*
 * Copyright (c) 2024, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Menu.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Widget.h>

namespace Browser {

class BrowserWindowWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(BrowserWindowWidget)
public:
    static ErrorOr<NonnullRefPtr<BrowserWindowWidget>> try_create();
    virtual ~BrowserWindowWidget() override = default;

private:
    BrowserWindowWidget() = default;
};

}
