/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

ExceptionOr<NonnullRefPtr<Node>> convert_nodes_to_single_node(Vector<Variant<NonnullRefPtr<Node>, String>> const& nodes, DOM::Document& document);

}
