/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RecursionDecision.h>

#include "Compiler/CompilerPass.h"

namespace JSSpecCompiler {

class RecursiveASTVisitor {
public:
    virtual ~RecursiveASTVisitor() = default;

    void run_in_const_subtree(NullableTree tree);
    void run_in_subtree(Tree& tree);

protected:
    virtual RecursionDecision on_entry(Tree) { return RecursionDecision::Recurse; }
    virtual void on_leave(Tree) { }

    void replace_current_node_with(NullableTree tree);

private:
    RecursionDecision recurse(Tree root, NodeSubtreePointer& pointer);

    NodeSubtreePointer* m_current_subtree_pointer = nullptr;
};

class GenericASTPass
    : public CompilerPass
    , protected RecursiveASTVisitor {
public:
    GenericASTPass(FunctionRef function)
        : CompilerPass(function)
    {
    }

    void run() override;
};

}
