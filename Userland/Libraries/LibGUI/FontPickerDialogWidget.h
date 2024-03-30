/*
 * Copyright (c) 2024, Aryan Baburajan <aryanbaburajan@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace GUI {

class FontPickerDialogWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(FontPickerDialogWidget)
public:
    static ErrorOr<NonnullRefPtr<FontPickerDialogWidget>> try_create();
    virtual ~FontPickerDialogWidget() override = default;

private:
    FontPickerDialogWidget() = default;
};

}
