/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/BlockContainer.h>

namespace Web::Layout {

class Viewport final : public BlockContainer {
    JS_CELL(Viewport, BlockContainer);
    JS_DECLARE_ALLOCATOR(Viewport);

public:
    explicit Viewport(DOM::Document&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~Viewport() override;

    const DOM::Document& dom_node() const { return static_cast<const DOM::Document&>(*Node::dom_node()); }

private:
    virtual JS::GCPtr<Painting::Paintable> create_paintable() const override;

    virtual bool is_viewport() const override { return true; }
};

template<>
inline bool Node::fast_is<Viewport>() const { return is_viewport(); }

}
