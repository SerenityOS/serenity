/*
 * Copyright (c) 2024, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace Browser {

class EditBookmarkWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(EditBookmarkWidget)
public:
    static ErrorOr<NonnullRefPtr<EditBookmarkWidget>> try_create();
    virtual ~EditBookmarkWidget() override = default;

private:
    EditBookmarkWidget() = default;
};

}
