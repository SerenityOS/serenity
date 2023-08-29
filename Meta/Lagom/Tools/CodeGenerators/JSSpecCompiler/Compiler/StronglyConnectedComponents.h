/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>

#include "Compiler/EnableGraphPointers.h"

namespace JSSpecCompiler {

namespace Detail {
template<typename GraphVertex, typename GraphNode>
class StronglyConnectedComponents
    : private EnableGraphPointers<StronglyConnectedComponents<GraphVertex, GraphNode>> {
    using Self = StronglyConnectedComponents<GraphVertex, GraphNode>;
    using Vertex = typename EnableGraphPointers<Self>::Vertex;

public:
    StronglyConnectedComponents(Vector<GraphNode> const& graph)
        : m_graph(graph)
    {
    }

    Vector<Vector<GraphVertex>> find()
    {
        Vector<Vector<GraphVertex>> result;
        size_t n = m_graph.size();
        Self::with_graph(n, [&] {
            for (size_t i = 0; i < m_graph.size(); ++i)
                find_order(i);
            for (size_t i = n; i--;) {
                if (!m_order[i]->is_processed) {
                    result.empend();
                    find_component(GraphVertex(m_order[i]), result.last());
                }
            }
        });
        return result;
    }

private:
    friend EnableGraphPointers<Self>;

    void find_order(Vertex u)
    {
        if (u->is_visited)
            return;
        u->is_visited = true;

        for (auto v : GraphVertex(u)->incoming_edges)
            find_order(Vertex(v));
        m_order.append(u);
    }

    void find_component(GraphVertex u, Vector<GraphVertex>& current_scc)
    {
        current_scc.empend(u);
        Vertex(u)->is_processed = true;
        for (auto v : u->outgoing_edges)
            if (!Vertex(v)->is_processed)
                find_component(v, current_scc);
    }

    struct NodeData {
        bool is_visited = false;
        bool is_processed = false;
    };

    Vector<GraphNode> const& m_graph;
    Vector<NodeData> m_nodes;
    Vector<Vertex> m_order;
};
}

template<typename NodeData>
auto find_strongly_connected_components(Vector<NodeData> const& graph)
{
    using Vertex = RemoveCVReference<decltype(graph[0].outgoing_edges[0])>;
    return Detail::StronglyConnectedComponents<Vertex, NodeData>(graph).find();
}

}
