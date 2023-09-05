/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Slottable.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLSlotElement.h>

namespace Web::DOM {

SlottableMixin::~SlottableMixin() = default;

void SlottableMixin::visit_edges(JS::Cell::Visitor& visitor)
{
    visitor.visit(m_assigned_slot);
    visitor.visit(m_manual_slot_assignment);
}

// https://dom.spec.whatwg.org/#dom-slotable-assignedslot
JS::GCPtr<HTML::HTMLSlotElement> SlottableMixin::assigned_slot()
{
    // FIXME: The assignedSlot getter steps are to return the result of find a slot given this and with the open flag set.
    return nullptr;
}

}
