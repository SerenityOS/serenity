/*
 * Copyright (c) 2024, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace Chess {

class PromotionWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(PromotionWidget)
public:
    static ErrorOr<NonnullRefPtr<PromotionWidget>> try_create();
    virtual ~PromotionWidget() override = default;

private:
    PromotionWidget() = default;
};

}
