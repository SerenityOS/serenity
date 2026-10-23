/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Interpreter.h"
#include "Definitions.h"
#include "OpcodeFilters.h"
#include <AK/Assertions.h>
#include <AK/Debug.h>
#include <AK/Forward.h>
#include <AK/RefPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Stream.h>
#include <LibACPI/NameString.h>
#include <LibACPI/Node.h>
#include <LibACPI/NodeData.h>
#include <LibACPI/OpcodeFrame.h>
#include <LibACPI/Table.h>

#define INTERPRETER_ERROR(simple, error, message, ...)                                     \
    do {                                                                                   \
        dbg("[ACPI] Interpreter::{}: ", __func__);                                         \
        dbgln(message __VA_OPT__(, ) __VA_ARGS__);                                         \
        return Error::from_string_view_or_print_error_and_return_errno(simple##sv, error); \
    } while (0);

namespace ACPI::AML {

ErrorOr<RefPtr<Node>> ScopeFrame::find_node(NameString const& path)
{
    return Node::find_node(path, scope());
}

ErrorOr<void> ScopeFrame::insert_node(NameString const& path, RefPtr<Node> const& node)
{
    auto target = path.type() == NameString::Type::Relative ? scope() : root();
    dbgln_if(AML_DEBUG, "[ACPI] Inserting node with name {} at path {}", node->name().to_string_view(), path.to_string());

    for (size_t i = 0; i < path.count() - 1; i++) {
        auto name = TRY(path.segment(i));
        target = TRY(target->find_child(name));
    }

    // Fine to throw error if path has no segments, as the resulting node needs a name.
    auto basename = TRY(path.basename());
    TRY(target->insert_child(basename, node));
    return {};
}

ErrorOr<void> Interpreter::print_namespace() const
{
    TRY(print_node(m_namespace_root, 0));
    return {};
}

ErrorOr<void> Interpreter::print_node(RefPtr<Node> const& node, int depth) const
{
    if (node.is_null())
        return {};

    StringBuilder sb;
    for (auto i = 0; i < depth; i++)
        TRY(sb.try_appendff("| "));

    auto name = node->name().to_string_view();
    TRY(sb.try_appendff("{}{}{}{}: ", name[0], name[1], name[2], name[3]));
    TRY(node->write_description(sb));
    dbgln("{}", sb.string_view());

    for (auto const& child : node->children())
        TRY(print_node(child, depth + 1));

    return {};
}

void Interpreter::debug_print_header(TableStream& stream, size_t operand_index)
{
    if constexpr (AML_DEBUG) {
        dbg("[ACPI] {:08X} ", stream.offset());
        for (size_t i = 0; i < (m_opcode_frames.size() - 1) - (m_state == State::FinalizingOpcode ? 1 : 0); i++) {
            dbg("|   ");
        }

        switch (m_state) {
        case State::ReadingOpcode:
            dbg("ini");
            break;
        case State::ReadingOperand:
            dbg("opr {}", operand_index);
            break;
        case State::FinalizingOpcode:
            dbg("fin");
            break;
        }
        dbg("        ");
    }
}

ErrorOr<NodeData> Interpreter::finalize_value_opcode(OpcodeFrame& frame)
{
    switch (frame.opcode()) {
    case Opcode::ZeroOp:
        return NodeData(u64(0));
    case Opcode::OneOp:
        return NodeData(u64(1));

    case Opcode::BytePrefix:
    case Opcode::WordPrefix:
    case Opcode::DWordPrefix:
    case Opcode::QWordPrefix:
        VERIFY(frame[0].is<u64>());
        return frame[0];

    default:
        INTERPRETER_ERROR("Unimplemented!", EINVAL, "Unimplemented value opcode {}!", frame.opcode());
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<void> Interpreter::finalize_term_opcode(OpcodeFrame& frame)
{
    switch (frame.opcode()) {
    case Opcode::ScopeOp: {
        auto length = TRY(frame[0].as<u64>());
        auto path = TRY(frame[1].as<NameString>());
        auto scope = TRY(m_scope_frames.last().find_node(path));

        auto scope_frame = ScopeFrame(m_namespace_root, scope, frame.start(), frame.start() + length + 1, m_loaded_tables.size() - 1);
        TRY(m_scope_frames.try_append(scope_frame));
        return {};
    }

    case Opcode::NameOp: {
        auto path = TRY(frame[0].as<NameString>());
        auto data = frame[1];

        auto node = make_ref_counted<NameNode>(data);
        TRY(m_scope_frames.last().insert_node(path, node));
        return {};
    }

    default:
        INTERPRETER_ERROR("Unimplemented!", EINVAL, "Unimplemented term opcode {}!", frame.opcode());
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<IterationDecision> Interpreter::read_operand(TableStream& stream, OperandType operand)
{
    auto& top_opcode_frame = m_opcode_frames.last();

    switch (operand) {
    case OperandType::NameString: {
        auto value = TRY(stream.read_name_string());
        dbgln_if(AML_DEBUG, "Reading namestring... ({})", value.to_string());
        TRY(top_opcode_frame.finish_operand(value));
        return IterationDecision::Continue;
    }
    case OperandType::PackageLength: {
        auto length = TRY(stream.read_package_length());
        dbgln_if(AML_DEBUG, "Reading package length... 0x({:X})", length);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(length)));
        return IterationDecision::Continue;
    }
    case OperandType::ByteData: {
        auto value = TRY(stream.read_value<u8>());
        dbgln_if(AML_DEBUG, "Reading byte... ({})", value);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(value)));
        return IterationDecision::Continue;
    }
    case OperandType::WordData: {
        auto value = TRY(stream.read_value<u16>());
        dbgln_if(AML_DEBUG, "Reading word... ({})", value);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(value)));
        return IterationDecision::Continue;
    }
    case OperandType::DWordData: {
        auto value = TRY(stream.read_value<u32>());
        dbgln_if(AML_DEBUG, "Reading dword... ({})", value);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(value)));
        return IterationDecision::Continue;
    }
    case OperandType::QWordData: {
        auto value = TRY(stream.read_value<u64>());
        dbgln_if(AML_DEBUG, "Reading qword... ({})", value);
        TRY(top_opcode_frame.finish_operand(value));
        return IterationDecision::Continue;
    }

    case OperandType::TermArg:
    case OperandType::DataRefObject:
        dbgln_if(AML_DEBUG, "Reading complex object {}...", to_underlying(operand));

        m_state = State::ReadingOpcode;
        return IterationDecision::Continue;

    default:
        dbgln("Failure!");
        INTERPRETER_ERROR("Unimplemented operand type!", EINVAL, "Operand type {} unimplemented!", to_underlying(operand));
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<IterationDecision> Interpreter::cycle(TableStream& stream)
{
    // Stacks have already been initialized.
    auto& top_opcode_frame = m_opcode_frames.last();

    switch (m_state) {
    case Interpreter::State::ReadingOpcode: {
        // Pop all scope frames that we've exited.
        // Do it here in case any pending opcodes need to be finalized at EOF.
        while (m_scope_frames.size() != 0) {
            auto& scope_frame = m_scope_frames.last();
            if (stream.offset() < scope_frame.end()) {
                break;
            }

            m_scope_frames.take_last();
        }

        if (stream.is_eof())
            return IterationDecision::Break;

        debug_print_header(stream);

        auto start = stream.offset();
        if (is_namestring_start(TRY(stream.peek_byte()))) {
            auto name = TRY(stream.read_name_string());
            dbgln_if(AML_DEBUG, "{}", name.to_string());

            dbgln("[ACPI]: Reading name string as value is not yet supported");
            return Error::from_errno(ENOTSUP);
        }

        auto opcode = TRY(stream.read_opcode());
        if (!top_opcode_frame.filter_opcode_for_next_operand(opcode)) {
            dbgln("Failure!");
            INTERPRETER_ERROR("Filtered opcode!", EINVAL, "Argument opcode {} filtered by opcode {}!", opcode, top_opcode_frame.opcode());
        }

        dbgln_if(AML_DEBUG, "Opcode: {}", opcode);

        auto opcode_frame = TRY(OpcodeFrame::from_opcode(static_cast<Opcode>(opcode), start));
        TRY(m_opcode_frames.try_append(move(opcode_frame)));
        m_state = State::ReadingOperand;
        return IterationDecision::Continue;
    }

    case Interpreter::State::ReadingOperand: {
        auto operand = TRY(top_opcode_frame.begin_operand());
        if (operand == OperandType::End) {
            m_state = State::FinalizingOpcode;
            return IterationDecision::Continue;
        }

        debug_print_header(stream, top_opcode_frame.size());
        return read_operand(stream, operand);
    }

    case Interpreter::State::FinalizingOpcode: {
        debug_print_header(stream);

        auto top_frame_value = m_opcode_frames.take_last();
        if (m_opcode_frames.size() > 1) {
            dbgln_if(AML_DEBUG, "Finalizing argument {}", top_frame_value.opcode());

            auto value = TRY(finalize_value_opcode(top_frame_value));
            TRY(m_opcode_frames.last().finish_operand(value));

            m_state = State::ReadingOperand;
        } else {
            dbgln_if(AML_DEBUG, "Finalizing term {}", top_frame_value.opcode());

            TRY(finalize_term_opcode(top_frame_value));

            m_state = State::ReadingOpcode;
        }

        return IterationDecision::Continue;
    }
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<NodeData> Interpreter::evaluate(NameString const& path)
{
    auto node = TRY(Node::find_node(path, m_namespace_root));
    return evaluate(node);
}

ErrorOr<NodeData> Interpreter::evaluate(RefPtr<Node> node)
{
    if (node->type() == NodeType::Name) {
        auto name_node = static_ptr_cast<NameNode>(node);
        return name_node->data();
    }

    INTERPRETER_ERROR("Cannot evaluate node type!", EINVAL, "Node type {} not supported for evaluation!", node->kind());
}

ErrorOr<void> Interpreter::ensure_root_namespace()
{
    if (m_namespace_root) {
        return {};
    }

    m_namespace_root = make_ref_counted<ScopeNode>();
    TRY(m_namespace_root->insert_child("_GPE"sv, make_ref_counted<ScopeNode>()));
    TRY(m_namespace_root->insert_child("_PR_"sv, make_ref_counted<ScopeNode>()));
    TRY(m_namespace_root->insert_child("_SB_"sv, make_ref_counted<ScopeNode>()));
    TRY(m_namespace_root->insert_child("_SI_"sv, make_ref_counted<ScopeNode>()));
    TRY(m_namespace_root->insert_child("_TZ_"sv, make_ref_counted<ScopeNode>()));

    TRY(m_namespace_root->insert_child("_REV"sv, make_ref_counted<NameNode>(NodeData(u64(2)))));

    // FIXME: ACPI spec also mendates we insert _GL, _OSI, _OS, _DLM

    return {};
}

ErrorOr<void> Interpreter::load(ReadonlyBytes bytes, bool ignore_block_header)
{
    TRY(m_loaded_tables.try_append(TableStream(bytes)));
    auto& stream_ref = m_loaded_tables.last();

    if (!ignore_block_header) {
        if (!validate_table(*bit_cast<Structures::SDTHeader*>(bytes.data()))) {
            INTERPRETER_ERROR("Incorrect checksum!", EINVAL, "Table checksum incorrect!");
        }

        auto block_header = TRY(stream_ref.read_sdt_header());
        dbgln("[ACPI] Interpreter::load[{}]: Loading table '{}' by OEM '{}'", m_loaded_tables.size() - 1, StringView(block_header.oem_table_id, 8), StringView(block_header.oem_id, 6));
    }

    TRY(ensure_root_namespace());

    auto root_scope_frame = ScopeFrame(m_namespace_root, m_namespace_root, stream_ref.offset(), TRY(stream_ref.size()), m_loaded_tables.size() - 1);
    TRY(m_scope_frames.try_append(move(root_scope_frame)));

    auto root_opcode_frame = TRY(OpcodeFrame::root_frame());
    TRY(m_opcode_frames.try_append(move(root_opcode_frame)));

    while (m_opcode_frames.size() > 0 && m_scope_frames.size() > 0) {
        if (TRY(cycle(stream_ref)) == IterationDecision::Break) {
            break;
        }
    }

    if (m_scope_frames.size() != 0 || m_opcode_frames.size() != 1) {
        return Error::from_string_view_or_print_error_and_return_errno("[ACPI] Interpreter::load: Unbalanced scope or opcode frames!"sv, EINVAL);
    }

    m_opcode_frames.take_last();
    return {};
}

}
