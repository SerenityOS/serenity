/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/Layout/FormAssociatedLabelableNode.h>

namespace Web::Layout {

class ButtonBox : public FormAssociatedLabelableNode {
public:
    ButtonBox(DOM::Document&, HTML::HTMLInputElement&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~ButtonBox() override;

    virtual void prepare_for_replaced_layout() override;

private:
    virtual RefPtr<Painting::Paintable> create_paintable() const override;
};

}
