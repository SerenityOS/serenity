/*
 * Copyright (c) 2021 Tobias Christiansen <tobi@tobyase.de>
 * 
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLFieldSetElement.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/Legend.h>

namespace Web::Layout {

class FieldSet : public BlockBox {
public:
    FieldSet(DOM::Document&, HTML::HTMLFieldSetElement*, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~FieldSet() override;

    virtual void paint_border(PaintContext& context) override;

    void layout_legend();

private:
    Legend* m_legend { nullptr };

    float border_length_left_of_legend() { return max(10.0f, box_model().border.left * 5); }
};

}
