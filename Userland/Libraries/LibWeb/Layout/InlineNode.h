/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/Box.h>

namespace Web::Layout {

class InlineNode : public NodeWithStyleAndBoxModelMetrics {
public:
    InlineNode(DOM::Document&, DOM::Element*, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~InlineNode() override;

    virtual RefPtr<Painting::Paintable> create_paintable() const override;
};

}
