/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NumericLimits.h>
#include <AK/Vector.h>

namespace JSSpecCompiler {

struct VoidRef { };

template<typename T, typename NativeNodeRef = VoidRef>
class EnableGraphPointers {
public:
    class VertexBase {
    public:
        VertexBase() = default;
        VertexBase(size_t index)
            : m_index(index)
        {
        }

        bool is_invalid() const { return m_index == invalid_node; }
        operator size_t() const { return m_index; }

        explicit VertexBase(NativeNodeRef const& node)
        requires(!IsSame<NativeNodeRef, VoidRef>)
            : VertexBase(node->m_index)
        {
        }

        auto& operator*() const { return m_instance->m_nodes[m_index]; }
        auto* operator->() const { return &m_instance->m_nodes[m_index]; }

    protected:
        size_t m_index = invalid_node;
    };

    using Vertex = VertexBase;

    inline static constexpr size_t invalid_node = NumericLimits<size_t>::max();

    template<typename Func>
    void with_graph(Func func)
    {
        m_instance = static_cast<T*>(this);
        func();
        m_instance = nullptr;
    }

    template<typename Func>
    void with_graph(size_t n, Func func)
    {
        m_instance = static_cast<T*>(this);
        m_instance->m_nodes.resize(n);
        func();
        m_instance->m_nodes.clear();
        m_instance = nullptr;
    }

protected:
    inline static thread_local T* m_instance = nullptr;
};

}
