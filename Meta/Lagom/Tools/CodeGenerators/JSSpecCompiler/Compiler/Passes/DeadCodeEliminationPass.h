/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Compiler/EnableGraphPointers.h"
#include "Compiler/GenericASTPass.h"
#include "Compiler/StronglyConnectedComponents.h"

namespace JSSpecCompiler {

class DeadCodeEliminationPass
    : public IntraproceduralCompilerPass
    , private RecursiveASTVisitor
    , private EnableGraphPointers<DeadCodeEliminationPass, SSAVariableDeclarationRef> {
public:
    inline static constexpr StringView name = "dce"sv;

    using IntraproceduralCompilerPass::IntraproceduralCompilerPass;

protected:
    void process_function() override;

private:
    friend EnableGraphPointers;

    static Vertex as_vertex(Variable* variable);
    RecursionDecision on_entry(Tree tree) override;
    void on_leave(Tree tree) override;
    void remove_unused_phi_nodes();

    struct NodeData {
        Vector<Vertex> outgoing_edges;
        Vector<Vertex> incoming_edges;

        bool is_referenced = false;
    };

    Vector<NodeData> m_nodes;
};

}
