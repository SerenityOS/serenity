/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Node.h"
#include "OpcodeFrame.h"
#include "TableStream.h"
#include <AK/Debug.h>
#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/Vector.h>

namespace ACPI::AML {

class ScopeFrame {
public:
    ScopeFrame(RefPtr<Node> root, RefPtr<Node> scope, size_t start, size_t end, size_t table_index)
        : m_root(move(root))
        , m_scope(move(scope))
        , m_start(start)
        , m_end(end)
        , m_table_index(table_index)
    {
    }

    ErrorOr<RefPtr<Node>> find_node(NameString const& path);
    ErrorOr<void> insert_node(NameString const& path, RefPtr<Node> const& node);

    RefPtr<Node> root() { return m_root; }
    RefPtr<Node> scope() { return m_scope; }
    size_t start() const { return m_start; }
    size_t end() const { return m_end; }
    size_t size() const { return m_end - m_start; }

private:
    RefPtr<Node> m_root, m_scope;
    size_t m_start, m_end, m_table_index;
};

class Interpreter {
public:
    ErrorOr<void> load(ReadonlyBytes bytes, bool ignore_block_header = false);
    ErrorOr<void> print_namespace() const;

    ErrorOr<NodeData> evaluate(NameString const& path);

private:
    enum class State : u8 {
        ReadingOpcode,
        ReadingOperand,
        FinalizingOpcode
    };

    ErrorOr<NodeData> evaluate(RefPtr<Node> node);

    ErrorOr<void> ensure_root_namespace();
    ErrorOr<IterationDecision> cycle(TableStream& stream);
    ErrorOr<IterationDecision> read_operand(TableStream& stream, OperandType operand);

    ErrorOr<NodeData> finalize_value_opcode(OpcodeFrame& frame);
    ErrorOr<void> finalize_term_opcode(OpcodeFrame& frame);

    void debug_print_header(TableStream& stream, size_t operand_index = 0);
    ErrorOr<void> print_node(RefPtr<Node> const& node, int depth) const;

    State m_state { State::ReadingOpcode };

    AK::Vector<TableStream> m_loaded_tables;
    Vector<ScopeFrame> m_scope_frames;
    Vector<OpcodeFrame> m_opcode_frames;

    RefPtr<Node> m_namespace_root;
};

}
