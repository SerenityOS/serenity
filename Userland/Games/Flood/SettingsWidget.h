/*
 * Copyright (c) 2023, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace Flood {

class SettingsWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(SettingsWidget)
public:
    static ErrorOr<NonnullRefPtr<SettingsWidget>> try_create();
    virtual ~SettingsWidget() override = default;

private:
    SettingsWidget() = default;
};

}
