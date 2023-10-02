/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>

#include "Compiler/CompilerPass.h"
#include "Compiler/ControlFlowGraph.h"
#include "Compiler/EnableGraphPointers.h"

namespace JSSpecCompiler {

// TODO: Add a LOT of unit tests.

class SSABuildingPass
    : public IntraproceduralCompilerPass
    , private EnableGraphPointers<SSABuildingPass, BasicBlockRef> {
public:
    inline static constexpr StringView name = "ssa-building"sv;

    using IntraproceduralCompilerPass::IntraproceduralCompilerPass;

protected:
    void process_function() override;

private:
    friend EnableGraphPointers;

    class Vertex : public VertexBase {
    public:
        using VertexBase::VertexBase;

        BasicBlockRef block() const { return m_instance->m_order[m_index]; }
    };

    void compute_order(BasicBlockRef u, Vertex parent = invalid_node);
    void compute_dominator_tree();

    template<typename... Args>
    Vector<Vertex> unique(Args const&... args);
    void compute_dtree_tin_tout(Vertex u);
    bool is_strictly_dominating(Vertex u, Vertex v);
    void compute_dominance_frontiers();

    void add_phi_node(BasicBlockRef block, NamedVariableDeclarationRef decl);
    void place_phi_nodes();

    void make_new_ssa_variable_for(NamedVariableDeclarationRef var);
    void rename_variable(VariableRef var);
    void rename_variables(Vertex u, Vertex from = invalid_node);
    void rename_variables();

    struct NodeData {
        Vector<Vertex> incoming_edges;
        Vector<Vertex> outgoing_edges;

        Vector<Vertex> buckets;

        bool is_used = false;
        Vertex parent;
        Vertex semi_dominator;
        Vertex immediate_dominator;

        Vector<Vertex> dtree_children;
        u64 tin, tout;

        Vector<Vertex> d_frontier;

        HashMap<NamedVariableDeclarationRef, Vertex> phi_nodes;

        u64 mark = 0;
    };

    u64 m_dtree_timer;
    Vector<NodeData> m_nodes;
    Vector<NonnullRefPtr<BasicBlock>> m_order;

    u64 m_mark_version;

    HashMap<NamedVariableDeclarationRef, Vector<SSAVariableDeclarationRef>> m_def_stack;
    HashMap<NamedVariableDeclarationRef, u64> m_next_id;
    Vector<NamedVariableDeclarationRef> m_undo_vector;

    ControlFlowGraph* m_graph;
};

}
