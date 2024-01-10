/*
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace HexEditor {

class EditAnnotationWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(GoToOffsetWidget)
public:
    static ErrorOr<NonnullRefPtr<EditAnnotationWidget>> try_create();
    virtual ~EditAnnotationWidget() override = default;

private:
    EditAnnotationWidget() = default;
};

}
