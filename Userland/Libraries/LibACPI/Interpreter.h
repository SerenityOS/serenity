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
#include <LibACPI/Nodes.h>
#include <LibACPI/OpcodeFrame.h>
#include <LibACPI/TableStream.h>

namespace ACPI::AML {

class ScopeFrame {
    AK_MAKE_NONCOPYABLE(ScopeFrame);
    AK_MAKE_DEFAULT_MOVABLE(ScopeFrame);

public:
    ScopeFrame(NonnullRefPtr<Node> root, NonnullRefPtr<Node> scope, size_t start, size_t end, size_t table_index, u64 number_mask)
        : m_root(move(root))
        , m_scope(move(scope))
        , m_start(start)
        , m_end(end)
        , m_table_index(table_index)
        , m_number_mask(number_mask)
    {
    }

    ErrorOr<void> insert_node(NameString const& path, NonnullRefPtr<Node> node);

    NonnullRefPtr<Node> root() { return m_root; }
    NonnullRefPtr<Node> scope() { return m_scope; }
    size_t start() const { return m_start; }
    size_t end() const { return m_end; }
    size_t size() const { return m_end - m_start; }
    u64 number_mask() const { return m_number_mask; }

    Object& local(size_t i) { return m_locals[i]; }
    Object& argument(size_t i) { return m_arguments[i]; }

    Object& return_value() { return m_return_value; }

private:
    NonnullRefPtr<Node> m_root;
    NonnullRefPtr<Node> m_scope;

    size_t m_start { 0 };
    size_t m_end { 0 };
    size_t m_table_index { 0 };
    u64 m_number_mask { 0 };

    // FIXME: Ensure locals and arguments live as long as any reference to them exists.
    Array<Object, 8> m_locals;
    Array<Object, 7> m_arguments;

    Object m_return_value;
};

class Interpreter {
public:
    static ErrorOr<NonnullOwnPtr<Interpreter>> create();

    ErrorOr<void> load(ReadonlyBytes bytes);
    ErrorOr<void> print_namespace() const;

    ErrorOr<Object> evaluate(StringView path, ReadonlySpan<Object> arguments = {});
    ErrorOr<Object> evaluate(NameString const& path, ReadonlySpan<Object> arguments = {});

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

    ErrorOr<Object> evaluate(Node& node, ReadonlySpan<Object> arguments = {});
    ErrorOr<Object> call_method(MethodNode& node, ReadonlySpan<Object> arguments);

    ErrorOr<Object*> resolve_reference(Object& reference);
    ErrorOr<Object*> resolve_references(Object& reference);

    ErrorOr<void> store(Object const& target, Object value);

    ErrorOr<void> cycle(TableStream& stream);
    ErrorOr<void> read_operand(TableStream& stream, OperandType operand);

    ErrorOr<Object> finalize_value_opcode(OpcodeFrame& frame);
    ErrorOr<void> finalize_term_opcode(OpcodeFrame& frame, TableStream& stream);

    void debug_print_header(TableStream& stream, size_t operand_index = 0);

    ErrorOr<void> allocate_frame(TableStream stream, ScopeFrame root_scope_frame);

    struct Frame {
        TableStream stream;
        State state { State::ReadingOpcode };

        Vector<ScopeFrame> scope_frames;
        Vector<OpcodeFrame> opcode_frames;

        u64 current_field_offset { 0 };

        Frame(TableStream stream)
            : stream(move(stream))
        {
        }
    };

    AK::Vector<ByteBuffer> m_loaded_tables;
    NonnullRefPtr<Node> m_namespace_root;

    Vector<Frame> m_frame_stack;
};

}
