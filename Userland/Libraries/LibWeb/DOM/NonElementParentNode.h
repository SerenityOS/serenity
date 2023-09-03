/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/TreeNode.h>

namespace Web::DOM {

template<typename NodeType>
class NonElementParentNode {
public:
    JS::GCPtr<Element const> get_element_by_id(DeprecatedFlyString const& id) const
    {
        JS::GCPtr<Element const> found_element;
        static_cast<NodeType const*>(this)->template for_each_in_inclusive_subtree_of_type<Element>([&](auto& element) {
            if (element.deprecated_attribute(HTML::AttributeNames::id) == id) {
                found_element = &element;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        return found_element;
    }

    JS::GCPtr<Element> get_element_by_id(DeprecatedFlyString const& id)
    {
        JS::GCPtr<Element> found_element;
        static_cast<NodeType*>(this)->template for_each_in_inclusive_subtree_of_type<Element>([&](auto& element) {
            if (element.deprecated_attribute(HTML::AttributeNames::id) == id) {
                found_element = &element;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        return found_element;
    }

protected:
    NonElementParentNode() = default;
};

}
