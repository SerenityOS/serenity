/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"

namespace PixelPaint {

class BucketTool final : public Tool {
public:
    BucketTool();
    virtual ~BucketTool() override;

    virtual void on_mousedown(Layer&, MouseEvent&) override;
    virtual GUI::Widget* get_properties_widget() override;

private:
    RefPtr<GUI::Widget> m_properties_widget;
    int m_threshold { 0 };
};

}
