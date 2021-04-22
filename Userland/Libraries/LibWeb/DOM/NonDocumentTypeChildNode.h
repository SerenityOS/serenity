/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/Forward.h>
#include <LibWeb/TreeNode.h>

namespace Web::DOM {

template<typename NodeType>
class NonDocumentTypeChildNode {
public:
    Element* previous_element_sibling()
    {
        for (auto* sibling = static_cast<NodeType*>(this)->previous_sibling(); sibling; sibling = sibling->previous_sibling()) {
            if (is<Element>(*sibling))
                return downcast<Element>(sibling);
        }
        return nullptr;
    }

    Element* next_element_sibling()
    {
        for (auto* sibling = static_cast<NodeType*>(this)->next_sibling(); sibling; sibling = sibling->next_sibling()) {
            if (is<Element>(*sibling))
                return downcast<Element>(sibling);
        }
        return nullptr;
    }

    Element* next_element_in_pre_order()
    {
        for (auto* node = static_cast<NodeType*>(this)->next_in_pre_order(); node; node = node->next_in_pre_order()) {
            if (is<Element>(*node))
                return downcast<Element>(node);
        }
        return nullptr;
    }

    const Element* previous_element_sibling() const { return const_cast<NonDocumentTypeChildNode*>(this)->previous_element_sibling(); }
    const Element* next_element_sibling() const { return const_cast<NonDocumentTypeChildNode*>(this)->next_element_sibling(); }
    const Element* next_element_in_pre_order() const { return const_cast<NonDocumentTypeChildNode*>(this)->next_element_in_pre_order(); }

protected:
    NonDocumentTypeChildNode() { }
};

}
