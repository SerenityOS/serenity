/*
 * Copyright (c) 2022, MillerTime <miller.time.baby@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/NodeFilterWrapper.h>
#include <LibWeb/Bindings/NodeFilterWrapperFactory.h>
#include <LibWeb/DOM/NodeFilter.h>

namespace Web::Bindings {

NodeFilterWrapper* wrap(JS::GlobalObject& global_object, DOM::NodeFilter& node_filter)
{
    if (node_filter.wrapper())
        return static_cast<NodeFilterWrapper*>(node_filter.wrapper());
    return static_cast<NodeFilterWrapper*>(wrap_impl(global_object, node_filter));
}

}
