/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
