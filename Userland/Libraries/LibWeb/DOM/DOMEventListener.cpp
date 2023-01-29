/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/AbortSignal.h>
#include <LibWeb/DOM/DOMEventListener.h>
#include <LibWeb/DOM/IDLEventListener.h>

namespace Web::DOM {

DOMEventListener::DOMEventListener() = default;
DOMEventListener::~DOMEventListener() = default;

void DOMEventListener::visit_edges(Cell::Visitor& visitor)
{
    Cell::visit_edges(visitor);
    visitor.visit(callback.ptr());
    visitor.visit(signal.ptr());
}

}
