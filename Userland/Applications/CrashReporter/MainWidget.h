/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace CrashReporter {

class MainWidget final : public GUI::Widget {
    C_OBJECT(MainWidget)

public:
    static ErrorOr<NonnullRefPtr<MainWidget>> try_create();
    virtual ~MainWidget() override = default;

private:
    MainWidget() = default;
};

}
