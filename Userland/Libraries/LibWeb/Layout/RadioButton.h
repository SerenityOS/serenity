/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/Layout/FormAssociatedLabelableNode.h>

namespace Web::Layout {

class RadioButton final : public FormAssociatedLabelableNode {
public:
    RadioButton(DOM::Document&, HTML::HTMLInputElement&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~RadioButton() override;

private:
    virtual RefPtr<Painting::Paintable> create_paintable() const override;
};

}
