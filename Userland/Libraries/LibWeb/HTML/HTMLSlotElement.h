/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/DOM/Slot.h>
#include <LibWeb/DOM/Slottable.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

struct AssignedNodesOptions {
    bool flatten { false };
};

class HTMLSlotElement final
    : public HTMLElement
    , public DOM::Slot {
    WEB_PLATFORM_OBJECT(HTMLSlotElement, HTMLElement);

public:
    virtual ~HTMLSlotElement() override;

    Vector<JS::Handle<DOM::Node>> assigned_nodes(AssignedNodesOptions options = {});
    Vector<JS::Handle<DOM::Element>> assigned_elements(AssignedNodesOptions options = {});

    using SlottableHandle = Variant<JS::Handle<DOM::Element>, JS::Handle<DOM::Text>>;
    void assign(Vector<SlottableHandle> nodes);

    ReadonlySpan<DOM::Slottable> manually_assigned_nodes() const { return m_manually_assigned_nodes; }

private:
    HTMLSlotElement(DOM::Document&, DOM::QualifiedName);

    virtual bool is_html_slot_element() const override { return true; }

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(JS::Cell::Visitor&) override;

    // https://html.spec.whatwg.org/multipage/scripting.html#manually-assigned-nodes
    Vector<DOM::Slottable> m_manually_assigned_nodes;
};

}

namespace Web::DOM {

template<>
inline bool Node::fast_is<HTML::HTMLSlotElement>() const { return is_html_slot_element(); }

}
