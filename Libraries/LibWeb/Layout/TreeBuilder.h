/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/NonnullRefPtrVector.h>
#include <AK/RefPtr.h>
#include <LibWeb/Forward.h>

namespace Web::Layout {

class TreeBuilder {
public:
    TreeBuilder();

    RefPtr<Layout::Node> build(DOM::Node&);

private:
    void create_layout_tree(DOM::Node&);

    void push_parent(Layout::NodeWithStyle& node) { m_parent_stack.append(&node); }
    void pop_parent() { m_parent_stack.take_last(); }

    template<CSS::Display, typename Callback>
    void for_each_in_tree_with_display(NodeWithStyle& root, Callback);

    void fixup_tables(NodeWithStyle& root);
    void remove_irrelevant_boxes(NodeWithStyle& root);
    void generate_missing_child_wrappers(NodeWithStyle& root);
    void generate_missing_parents(NodeWithStyle& root);

    RefPtr<Layout::Node> m_layout_root;
    Vector<Layout::NodeWithStyle*> m_parent_stack;
};

}
