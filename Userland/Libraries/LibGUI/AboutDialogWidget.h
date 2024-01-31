/*
 * Copyright (c) 2024, Aarushi Chauhan <aarushi595.chauhan@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace GUI {

class AboutDialogWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(AboutDialogWidget)
public:
    static ErrorOr<NonnullRefPtr<AboutDialogWidget>> try_create();
    virtual ~AboutDialogWidget() override = default;

private:
    AboutDialogWidget() = default;
};

}
