/*
 * Copyright (c) 2024, Aryan Baburajan <aryanbaburajan2007@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace GUI {

class EmojiInputDialogWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(EmojiInputDialogWidget)

public:
    static ErrorOr<NonnullRefPtr<EmojiInputDialogWidget>> try_create();
    virtual ~EmojiInputDialogWidget() override = default;

private:
    EmojiInputDialogWidget() = default;
};

}
