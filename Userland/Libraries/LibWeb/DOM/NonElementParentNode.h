/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Forward.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/TreeNode.h>

namespace Web::DOM {

template<typename NodeType>
class NonElementParentNode {
public:
    JS::GCPtr<Element const> get_element_by_id(FlyString const& id) const
    {
        JS::GCPtr<Element const> found_element;
        static_cast<NodeType const*>(this)->template for_each_in_inclusive_subtree_of_type<Element>([&](auto& element) {
            if (element.id() == id) {
                found_element = &element;
                return TraversalDecision::Break;
            }
            return TraversalDecision::Continue;
        });
        return found_element;
    }

    JS::GCPtr<Element> get_element_by_id(FlyString const& id)
    {
        JS::GCPtr<Element> found_element;
        static_cast<NodeType*>(this)->template for_each_in_inclusive_subtree_of_type<Element>([&](auto& element) {
            if (element.id() == id) {
                found_element = &element;
                return TraversalDecision::Continue;
            }
            return TraversalDecision::Continue;
        });
        return found_element;
    }

protected:
    NonElementParentNode() = default;
};

}
