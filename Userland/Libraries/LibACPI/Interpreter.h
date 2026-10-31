/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/Vector.h>
#include <LibACPI/Node.h>
#include <LibACPI/OpcodeFrame.h>
#include <LibACPI/TableStream.h>

namespace ACPI::AML {

class ScopeFrame {
public:
    ScopeFrame(NonnullRefPtr<Node> root, NonnullRefPtr<Node> scope, size_t start, size_t end, size_t table_index)
        : m_root(move(root))
        , m_scope(move(scope))
        , m_start(start)
        , m_end(end)
        , m_table_index(table_index)
    {
    }

    ErrorOr<void> insert_node(NameString const& path, NonnullRefPtr<Node> node);

    NonnullRefPtr<Node> root() { return m_root; }
    NonnullRefPtr<Node> scope() { return m_scope; }
    size_t start() const { return m_start; }
    size_t end() const { return m_end; }
    size_t size() const { return m_end - m_start; }

private:
    NonnullRefPtr<Node> m_root;
    NonnullRefPtr<Node> m_scope;

    size_t m_start { 0 };
    size_t m_end { 0 };
    size_t m_table_index { 0 };
};

class Interpreter {
public:
    static ErrorOr<NonnullOwnPtr<Interpreter>> create();

    ErrorOr<void> load(ReadonlyBytes bytes);
    ErrorOr<void> print_namespace() const;

    ErrorOr<Object> evaluate(StringView path);
    ErrorOr<Object> evaluate(NameString const& path);

private:
    enum class State : u8 {
        ReadingOpcode,
        ReadingOperand,
        FinalizingOpcode,
    };

    Interpreter(NonnullRefPtr<Node> namespace_root)
        : m_namespace_root(move(namespace_root))
    {
    }

    static ErrorOr<NonnullRefPtr<Node>> create_root_namespace();

    ErrorOr<NonnullRefPtr<Node>> find_node(NameString const& path, NonnullRefPtr<Node> scope) const;
    ErrorOr<void> walk_tree(NonnullRefPtr<Node> tree_root, Function<ErrorOr<void>(NonnullRefPtr<Node>, u32)> callback) const;

    ErrorOr<Object> evaluate(NonnullRefPtr<Node> node);

    ErrorOr<IterationDecision> cycle(TableStream& stream);
    ErrorOr<IterationDecision> read_operand(TableStream& stream, OperandType operand);

    ErrorOr<Object> finalize_value_opcode(OpcodeFrame& frame);
    ErrorOr<void> finalize_term_opcode(OpcodeFrame& frame, TableStream& stream);

    void debug_print_header(TableStream& stream, size_t operand_index = 0);

    State m_state { State::ReadingOpcode };

    u64 m_current_field_offset { 0 };

    AK::Vector<TableStream> m_loaded_tables;
    Vector<ScopeFrame> m_scope_frames;
    Vector<OpcodeFrame> m_opcode_frames;

    NonnullRefPtr<Node> m_namespace_root;
};

}
