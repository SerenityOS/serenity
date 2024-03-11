/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Enumerate.h>
#include <AK/Queue.h>

#include "AST/AST.h"
#include "Compiler/GenericASTPass.h"
#include "Compiler/Passes/SSABuildingPass.h"
#include "Function.h"

namespace JSSpecCompiler {

void SSABuildingPass::process_function()
{
    m_dtree_timer = 0;
    m_order.clear();
    m_mark_version = 1;
    m_def_stack.clear();
    m_next_id.clear();
    m_undo_vector.clear();
    m_graph = m_function->m_cfg;

    with_graph(m_graph->blocks_count(), [&] {
        compute_dominator_tree();
        compute_dominance_frontiers();
        place_phi_nodes();
        rename_variables();
    });
}

// ===== compute_dominator_tree =====
namespace {
class DSU {
    struct NodeData {
        size_t sdom;
        size_t parent;
    };

public:
    DSU(size_t n)
        : n(n)
    {
        m_nodes.resize(n);
        for (size_t i = 0; i < n; ++i)
            m_nodes[i] = { i, i };
    }

    NodeData get(size_t u)
    {
        if (m_nodes[u].parent == u)
            return { n, u };
        auto [sdom, root] = get(m_nodes[u].parent);
        sdom = min(sdom, m_nodes[u].sdom);
        return m_nodes[u] = { sdom, root };
    }

    void merge(size_t u, size_t v, size_t v_sdom)
    {
        m_nodes[v] = { v_sdom, u };
    }

private:
    size_t n;
    Vector<NodeData> m_nodes;
};
}

void SSABuildingPass::compute_order(BasicBlockRef u, Vertex parent)
{
    if (m_nodes[u->m_index].is_used)
        return;
    m_nodes[u->m_index].is_used = true;

    Vertex reordered_u = m_order.size();
    m_order.append(RefPtr<BasicBlock>(u).release_nonnull());
    reordered_u->parent = parent;

    for (auto* v : u->m_continuation->references())
        compute_order(*v, reordered_u);
}

void SSABuildingPass::compute_dominator_tree()
{
    size_t n = m_graph->blocks_count();
    m_nodes.resize(n);

    // Algorithm is from https://tanujkhattar.wordpress.com/2016/01/11/dominator-tree-of-a-directed-graph/ ,
    // an author writes awful CP-style write-only code, but the explanation is pretty good.

    // Step 1
    compute_order(m_graph->start_block);
    VERIFY(m_order.size() == n);
    for (size_t i = 0; i < n; ++i)
        m_order[i]->m_index = i;
    m_graph->blocks = m_order;

    for (size_t i = 0; i < n; ++i) {
        Vertex u = i;

        for (auto* reference : u.block()->m_continuation->references()) {
            Vertex v { *reference };

            v->incoming_edges.append(u);
            u->outgoing_edges.append(v);
        }
    }

    // Steps 2 & 3
    DSU dsu(n);

    for (size_t i = n - 1; i > 0; --i) {
        Vertex u = i;

        Vertex& current_sdom = u->semi_dominator;
        current_sdom = n;

        for (Vertex v : u->incoming_edges) {
            if (v < u)
                current_sdom = min(current_sdom, v);
            else
                current_sdom = min(current_sdom, dsu.get(v).sdom);
        }

        current_sdom->buckets.append(u);
        for (Vertex w : u->buckets) {
            Vertex v = dsu.get(w).sdom;

            if (v->semi_dominator == w->semi_dominator)
                w->immediate_dominator = v->semi_dominator;
            else
                w->immediate_dominator = v;
        }
        dsu.merge(u->parent, u, current_sdom);
    }

    m_nodes[0].immediate_dominator = invalid_node;
    for (size_t i = 1; i < n; ++i) {
        Vertex u = i;

        if (u->immediate_dominator.is_invalid())
            u->immediate_dominator = 0;
        else if (u->immediate_dominator != u->semi_dominator)
            u->immediate_dominator = u->immediate_dominator->immediate_dominator;
    }

    // Populate dtree_children & BasicBlock::immediate_dominator
    for (size_t i = 0; i < n; ++i) {
        Vertex u = i;

        if (i != 0) {
            u.block()->m_immediate_dominator = u->immediate_dominator.block();
            u->immediate_dominator->dtree_children.append(u);
        } else {
            u.block()->m_immediate_dominator = nullptr;
        }
    }
}

// ===== compute_dominance_frontiers =====
template<typename... Args>
Vector<SSABuildingPass::Vertex> SSABuildingPass::unique(Args const&... args)
{
    ++m_mark_version;

    Vector<Vertex> result;
    (([&](auto const& list) {
        for (Vertex u : list) {
            if (u->mark != m_mark_version) {
                u->mark = m_mark_version;
                result.append(u);
            }
        }
    })(args),
        ...);
    return result;
}

void SSABuildingPass::compute_dtree_tin_tout(Vertex u)
{
    u->tin = m_dtree_timer++;
    for (Vertex v : u->dtree_children)
        compute_dtree_tin_tout(v);
    u->tout = m_dtree_timer++;
}

bool SSABuildingPass::is_strictly_dominating(Vertex u, Vertex v)
{
    return u != v && u->tin <= v->tin && v->tout <= u->tout;
}

void SSABuildingPass::compute_dominance_frontiers()
{
    compute_dtree_tin_tout(0);

    // Algorithm from https://en.wikipedia.org/wiki/Static_single-assignment_form#Converting%20to%20SSA:~:text=their%20paper%20titled-,A%20Simple%2C%20Fast%20Dominance%20Algorithm,-%3A%5B13%5D .
    // DF(u) = {w : !(u sdom w) /\ (\exists v \in incoming_edges(v) : u dom v)}
    for (size_t wi = 0; wi < m_nodes.size(); ++wi) {
        Vertex w = wi;

        for (Vertex v : w->incoming_edges) {
            Vertex u = v;
            while (u != invalid_node && !is_strictly_dominating(u, w)) {
                u->d_frontier.append(w);
                u = u->immediate_dominator;
            }
        }
    }

    for (size_t i = 0; i < m_nodes.size(); ++i) {
        Vertex u = i;

        u->d_frontier = unique(u->d_frontier);
    }
}

// ===== place_phi_nodes =====
namespace {
class VariableAssignmentCollector : private RecursiveASTVisitor {
public:
    VariableAssignmentCollector(OrderedHashMap<NamedVariableDeclarationRef, Vector<BasicBlockRef>>& declarations)
        : m_declarations(declarations)
    {
    }

    void run(BasicBlockRef block)
    {
        m_current_block = block;

        for (auto& expression : block->m_expressions)
            run_in_subtree(expression);
        run_in_const_subtree(block->m_continuation);
    }

protected:
    RecursionDecision on_entry(Tree tree) override
    {
        if (tree->is_statement())
            TODO();
        return RecursionDecision::Recurse;
    }

    void on_leave(Tree tree) override
    {
        if (auto binary_operation = as<BinaryOperation>(tree); binary_operation) {
            if (binary_operation->m_operation != BinaryOperator::Assignment)
                return;

            if (auto variable = as<Variable>(binary_operation->m_left); variable) {
                auto& vector = m_declarations.get(variable->m_name).value();
                if (vector.is_empty() || vector.last() != m_current_block)
                    vector.append(m_current_block);
            }
        }
    }

private:
    BasicBlockRef m_current_block;
    OrderedHashMap<NamedVariableDeclarationRef, Vector<BasicBlockRef>>& m_declarations;
};
}

void SSABuildingPass::add_phi_node(BasicBlockRef block, NamedVariableDeclarationRef decl)
{
    BasicBlock::PhiNode node { .var = make_ref_counted<Variable>(decl) };
    for (Vertex incoming : Vertex(block)->incoming_edges) {
        BasicBlockRef incoming_block = incoming.block();
        auto value = make_ref_counted<Variable>(decl);
        node.branches.append({ .block = incoming_block, .value = value });
    }
    block->m_phi_nodes.append(move(node));
}

void SSABuildingPass::place_phi_nodes()
{
    // Entry block has implicit declarations of all variables.
    OrderedHashMap<NamedVariableDeclarationRef, Vector<BasicBlockRef>> m_declarations;
    for (auto const& [name, var_decl] : m_function->m_local_variables)
        m_declarations.set(var_decl, { m_order[0] });
    m_declarations.set(m_function->m_named_return_value, { m_order[0] });

    VariableAssignmentCollector collector(m_declarations);
    for (auto const& block : m_order)
        collector.run(block);

    for (auto const& [decl, blocks] : m_declarations) {
        ++m_mark_version;

        Queue<BasicBlockRef> queue;
        for (auto const& block : blocks)
            queue.enqueue(block);

        while (!queue.is_empty()) {
            Vertex u(queue.dequeue());

            for (Vertex frontier : u->d_frontier) {
                if (frontier->mark == m_mark_version)
                    continue;
                frontier->mark = m_mark_version;
                add_phi_node(frontier.block(), decl);
            }
        }
    }
}

// ===== rename_variables =====
namespace {
template<typename CreateSSAVariableFunc, typename RenameVariableFunc>
class VariableRenamer : private RecursiveASTVisitor {
public:
    VariableRenamer(CreateSSAVariableFunc create, RenameVariableFunc rename)
        : m_create(create)
        , m_rename(rename)
    {
    }

    void run(BasicBlockRef block)
    {
        for (auto& expression : block->m_expressions)
            run_in_subtree(expression);
        run_in_const_subtree(block->m_continuation);
    }

protected:
    RecursionDecision on_entry(Tree tree) override
    {
        if (tree->is_statement())
            TODO();

        auto binary_operation = as<BinaryOperation>(tree);
        if (binary_operation && binary_operation->m_operation == BinaryOperator::Assignment) {
            run_in_subtree(binary_operation->m_right);
            if (auto variable = as<Variable>(binary_operation->m_left); variable) {
                m_create(variable->m_name);
                m_rename(variable.release_nonnull());
            } else {
                run_in_subtree(binary_operation->m_left);
            }
            return RecursionDecision::Continue;
        }

        if (auto variable = as<Variable>(tree); variable) {
            m_rename(variable.release_nonnull());
            return RecursionDecision::Continue;
        }

        return RecursionDecision::Recurse;
    }

private:
    CreateSSAVariableFunc m_create;
    RenameVariableFunc m_rename;
};
}

void SSABuildingPass::make_new_ssa_variable_for(NamedVariableDeclarationRef var)
{
    m_undo_vector.append(var);

    u64 id = 0;
    if (auto it = m_next_id.find(var); it == m_next_id.end())
        m_next_id.set(var, 1);
    else
        id = it->value++;
    auto ssa_decl = make_ref_counted<SSAVariableDeclaration>(id);

    m_function->m_local_ssa_variables.append(ssa_decl);

    if (auto it = m_def_stack.find(var); it == m_def_stack.end())
        m_def_stack.set(var, { ssa_decl });
    else
        it->value.append(ssa_decl);
}

void SSABuildingPass::rename_variable(VariableRef var)
{
    var->m_ssa = m_def_stack.get(var->m_name).value().last();
}

void SSABuildingPass::rename_variables(Vertex u, Vertex from)
{
    size_t rollback_point = m_undo_vector.size();

    for (auto& phi_node : u.block()->m_phi_nodes) {
        // TODO: Find the right branch index without iterating through all of the branches.
        bool found = false;
        for (auto& branch : phi_node.branches) {
            if (branch.block->m_index == from) {
                rename_variable(branch.value);
                found = true;
                break;
            }
        }
        VERIFY(found);
    }

    if (u->mark == m_mark_version)
        return;
    u->mark = m_mark_version;

    for (auto& phi_node : u.block()->m_phi_nodes) {
        make_new_ssa_variable_for(phi_node.var->m_name);
        rename_variable(phi_node.var);
    }

    VariableRenamer renamer(
        [&](NamedVariableDeclarationRef decl) {
            make_new_ssa_variable_for(move(decl));
        },
        [&](VariableRef var) {
            rename_variable(move(var));
        });
    renamer.run(u.block());

    if (auto function_return = as<ControlFlowFunctionReturn>(u.block()->m_continuation); function_return) {
        // CFG should have exactly one ControlFlowFunctionReturn.
        VERIFY(m_function->m_return_value == nullptr);
        m_function->m_return_value = function_return->m_return_value->m_ssa;
    }

    for (size_t j : u->outgoing_edges)
        rename_variables(j, u);

    while (m_undo_vector.size() > rollback_point)
        (void)m_def_stack.get(m_undo_vector.take_last()).value().take_last();
}

void SSABuildingPass::rename_variables()
{
    HashMap<StringView, size_t> argument_index_by_name;
    for (auto [i, argument] : enumerate(m_function->arguments()))
        argument_index_by_name.set(argument.name, i);
    m_function->m_ssa_arguments.resize(m_function->arguments().size());

    for (auto const& [name, var_decl] : m_function->m_local_variables) {
        make_new_ssa_variable_for(var_decl);

        if (auto maybe_index = argument_index_by_name.get(name); maybe_index.has_value()) {
            size_t index = maybe_index.value();
            m_function->m_ssa_arguments[index] = m_def_stack.get(var_decl).value()[0];
        }
    }
    make_new_ssa_variable_for(m_function->m_named_return_value);

    ++m_mark_version;
    rename_variables(0);
    VERIFY(m_function->m_return_value);
    m_function->reindex_ssa_variables();
}

}
