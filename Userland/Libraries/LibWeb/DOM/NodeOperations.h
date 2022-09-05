/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/GCPtr.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

ExceptionOr<JS::NonnullGCPtr<Node>> convert_nodes_to_single_node(Vector<Variant<JS::Handle<Node>, String>> const& nodes, DOM::Document& document);

}
