/*
 * Copyright (c) 2024, Sanil Gupta <sanilg566@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace KeyboardSettings {

class KeymapDialog : public GUI::Widget {
    C_OBJECT_ABSTRACT(KeymapDialog)
public:
    static ErrorOr<NonnullRefPtr<KeymapDialog>> try_create();
    virtual ~KeymapDialog() override = default;

private:
    KeymapDialog() = default;
};

}
