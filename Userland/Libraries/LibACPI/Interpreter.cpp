/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Debug.h>
#include <LibACPI/Definitions.h>
#include <LibACPI/Interpreter.h>
#include <LibACPI/Node.h>
#include <LibACPI/Object.h>
#include <LibACPI/OpcodeFilters.h>
#include <LibACPI/Table.h>

#ifdef KERNEL
#    define warnln dbgln
#    define outln dbgln
#    define DEBUG_LOG(...)
#else
#    define DEBUG_LOG(...) warnln_if(AML_DEBUG, __VA_ARGS__)
#endif

#define INTERPRETER_ERROR(simple, error, message, ...)                                     \
    do {                                                                                   \
        warnln("ACPI: Interpreter::{}: " message, __func__ __VA_OPT__(, ) __VA_ARGS__);    \
        return Error::from_string_view_or_print_error_and_return_errno(simple##sv, error); \
    } while (0);

namespace ACPI::AML {

ErrorOr<void> ScopeFrame::insert_node(NameString const& path, NonnullRefPtr<Node> node)
{
    auto target = path.type() == NameString::Type::Relative ? scope() : root();
    DEBUG_LOG("ACPI: Inserting node with name {} at path {}", node->name().to_string_view(), path.to_string());

    if (path.segment_count() < 1)
        return Error::from_string_view_or_print_error_and_return_errno("Path must have at least one segment!"sv, EINVAL);

    for (size_t i = 0; i < path.segment_count() - 1; i++) {
        auto name = TRY(path.segment(i));
        target = TRY(target->find_child(name));
    }

    auto basename = TRY(path.basename());
    TRY(target->insert_child(basename, node));
    return {};
}

ErrorOr<NonnullOwnPtr<Interpreter>> Interpreter::create()
{
    auto root = TRY(Interpreter::create_root_namespace());
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) Interpreter(root)));
}

ErrorOr<NonnullRefPtr<Node>> Interpreter::find_node(NameString const& path, NonnullRefPtr<Node> scope) const
{
    RefPtr<Node> current_scope = scope;

    bool search_rules_apply = path.type() == NameString::Type::Relative && path.segment_count() == 1 && path.parent_index() == 0;

    if (path.type() == NameString::Type::Relative && path.parent_index() > 0) {
        for (size_t i = 0; i < path.parent_index(); i++) {
            if (current_scope == m_namespace_root)
                return Error::from_string_view_or_print_error_and_return_errno("Can't go higher than root!"sv, EINVAL);

            if (auto strong = current_scope->parent().strong_ref())
                current_scope = strong;
            else
                return Error::from_string_view_or_print_error_and_return_errno("Reached a dangling node!"sv, EINVAL);
        }
    } else if (path.type() == NameString::Type::Absolute) {
        current_scope = m_namespace_root;
    }

    // https://uefi.org/specs/ACPI/6.6/05_ACPI_Software_Programming_Model.html#acpi-namespace
    if (search_rules_apply) {
        // Path is a single name segment, is relative and has no depth.
        // Crawl upwards from the current scope and try finding a node with the given name.
        auto node_name = TRY(path.segment(0));
        while (!current_scope.is_null()) {
            auto found = current_scope->find_child(node_name);
            if (!found.is_error())
                return found.release_value();

            if (current_scope == m_namespace_root) {
                warnln("ACPI: Could not find node with name {}!", node_name.to_string_view());
                return Error::from_string_view_or_print_error_and_return_errno("Node not found!"sv, EINVAL);
            }

            if (auto strong = current_scope->parent().strong_ref())
                current_scope = strong;
            else
                return Error::from_string_view_or_print_error_and_return_errno("Reached a dangling node!"sv, EINVAL);
        }

        VERIFY_NOT_REACHED();
    }

    // Search crawls down from root via name segments.
    for (size_t i = 0; i < path.segment_count(); i++) {
        auto node_name = TRY(path.segment(i));
        auto found = current_scope->find_child(node_name);
        if (found.is_error()) {
            // FIXME: Warn without allocating any string.
            warnln("ACPI: Could not find node at path {}!", path.to_string());
            return found.release_error();
        }

        current_scope = found.release_value();
    }

    return current_scope.release_nonnull();
}

ErrorOr<void> Interpreter::walk_tree(NonnullRefPtr<Node> tree_root, Function<ErrorOr<void>(NonnullRefPtr<Node>, u32)> callback) const
{
    struct Entry {
        NonnullRefPtr<Node> node;
        size_t depth;
    };

    Vector<Entry> stack;
    TRY(stack.try_append({ move(tree_root), 0 }));

    while (!stack.is_empty()) {
        auto entry = stack.take_last();

        TRY(callback(entry.node, entry.depth));

        auto const& children = entry.node->children();
        for (size_t i = children.size(); i-- > 0;)
            TRY(stack.try_append({ children[i], entry.depth + 1 }));
    }

    return {};
}

ErrorOr<void> Interpreter::print_namespace() const
{
    TRY(walk_tree(m_namespace_root, [&](NonnullRefPtr<Node> node, u32 depth) -> ErrorOr<void> {
        StringBuilder sb;
        for (u32 i = 0; i < depth; i++)
            TRY(sb.try_appendff("| "));

        auto name = node->name().to_string_view();
        TRY(sb.try_appendff("{}: ", name));
        TRY(node->write_description(sb));
        outln("{}", sb.string_view());

        return {};
    }));

    return {};
}

void Interpreter::debug_print_header(TableStream& stream, size_t operand_index)
{
#ifndef KERNEL
    if constexpr (AML_DEBUG) {
        warn("ACPI: {:08X} ", stream.offset());
        for (size_t i = 0; i < (m_opcode_frames.size() - 1) - (m_state == State::FinalizingOpcode ? 1 : 0); i++) {
            warn("|   ");
        }

        switch (m_state) {
        case State::ReadingOpcode:
            warn("ini");
            break;
        case State::ReadingOperand:
            warn("opr {}", operand_index);
            break;
        case State::FinalizingOpcode:
            warn("fin");
            break;
        }
        warn("        ");
    }
#else
    (void)stream;
    (void)operand_index;
#endif
}

ErrorOr<Object> Interpreter::finalize_value_opcode(OpcodeFrame& frame)
{
    switch (frame.opcode()) {
    case Opcode::ZeroOp:
        return Object(u64(0));
    case Opcode::OneOp:
        return Object(u64(1));

    case Opcode::BytePrefix:
    case Opcode::WordPrefix:
    case Opcode::DWordPrefix:
    case Opcode::QWordPrefix:
        VERIFY(frame.operand(0).is<u64>());
        return frame.operand(0);

    default:
        INTERPRETER_ERROR("Unimplemented!", EINVAL, "Unimplemented value opcode {}!", frame.opcode());
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<void> Interpreter::finalize_term_opcode(OpcodeFrame& frame)
{
    switch (frame.opcode()) {
    case Opcode::ScopeOp: {
        auto length = TRY(frame.operand(0).as<u64>());
        auto path = TRY(frame.operand(1).as<NameString>());
        auto scope = TRY(find_node(path, m_scope_frames.last().scope()));

        auto scope_frame = ScopeFrame(m_namespace_root, scope, frame.start(), frame.start() + length + 1, m_loaded_tables.size() - 1);
        TRY(m_scope_frames.try_append(scope_frame));
        return {};
    }

    case Opcode::NameOp: {
        auto path = TRY(frame.operand(0).as<NameString>());
        auto data = frame.operand(1);

        auto node = TRY(try_make_ref_counted<NameNode>(data));
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
        DEBUG_LOG("Reading namestring... ({})", value.to_string());
        TRY(top_opcode_frame.finish_operand(value));
        return IterationDecision::Continue;
    }
    case OperandType::PackageLength: {
        auto length = TRY(stream.read_package_length());
        DEBUG_LOG("Reading package length... ({:#X})", length);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(length)));
        return IterationDecision::Continue;
    }
    case OperandType::ByteData: {
        auto value = TRY(stream.read_value<u8>());
        DEBUG_LOG("Reading byte... ({})", value);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(value)));
        return IterationDecision::Continue;
    }
    case OperandType::WordData: {
        auto value = TRY(stream.read_value<u16>());
        DEBUG_LOG("Reading word... ({})", value);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(value)));
        return IterationDecision::Continue;
    }
    case OperandType::DWordData: {
        auto value = TRY(stream.read_value<u32>());
        DEBUG_LOG("Reading dword... ({})", value);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(value)));
        return IterationDecision::Continue;
    }
    case OperandType::QWordData: {
        auto value = TRY(stream.read_value<u64>());
        DEBUG_LOG("Reading qword... ({})", value);
        TRY(top_opcode_frame.finish_operand(value));
        return IterationDecision::Continue;
    }

    case OperandType::TermArg:
    case OperandType::DataRefObject:
        DEBUG_LOG("Reading complex object {}...", to_underlying(operand));

        m_state = State::ReadingOpcode;
        return IterationDecision::Continue;

    default:
        DEBUG_LOG("Failure!");
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
            DEBUG_LOG("{}", name.to_string());

            warnln("ACPI: Reading name string as value is not yet supported");
            return Error::from_errno(ENOTSUP);
        }

        auto opcode = TRY(stream.read_opcode());
        if (!top_opcode_frame.filter_opcode_for_next_operand(opcode)) {
            DEBUG_LOG("Failure!");
            INTERPRETER_ERROR("Filtered opcode!", EINVAL, "Argument opcode {} filtered by opcode {}!", opcode, top_opcode_frame.opcode());
        }

        DEBUG_LOG("Opcode: {}", opcode);

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

        debug_print_header(stream, top_opcode_frame.operand_count());
        return read_operand(stream, operand);
    }

    case Interpreter::State::FinalizingOpcode: {
        debug_print_header(stream);

        auto top_frame_value = m_opcode_frames.take_last();
        if (m_opcode_frames.size() > 1) {
            DEBUG_LOG("Finalizing argument {}", top_frame_value.opcode());

            auto value = TRY(finalize_value_opcode(top_frame_value));
            TRY(m_opcode_frames.last().finish_operand(value));

            m_state = State::ReadingOperand;
        } else {
            DEBUG_LOG("Finalizing term {}", top_frame_value.opcode());

            TRY(finalize_term_opcode(top_frame_value));

            m_state = State::ReadingOpcode;
        }

        return IterationDecision::Continue;
    }
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<Object> Interpreter::evaluate(NameString const& path)
{
    auto node = TRY(find_node(path, m_namespace_root));
    return evaluate(node);
}

ErrorOr<Object> Interpreter::evaluate(NonnullRefPtr<Node> node)
{
    if (node->type() == NodeType::Name) {
        auto name_node = static_ptr_cast<NameNode>(node);
        return name_node->data();
    }

    INTERPRETER_ERROR("Cannot evaluate node type!", EINVAL, "Node type {} not supported for evaluation!", node->kind());
}

ErrorOr<NonnullRefPtr<Node>> Interpreter::create_root_namespace()
{
    auto namespace_root = TRY(try_make_ref_counted<ScopeNode>());

    // https://uefi.org/specs/ACPI/6.6/05_ACPI_Software_Programming_Model.html#predefined-root-namespaces
    TRY(namespace_root->insert_child("_GPE"sv, TRY(try_make_ref_counted<ScopeNode>())));
    TRY(namespace_root->insert_child("_PR_"sv, TRY(try_make_ref_counted<ScopeNode>())));
    TRY(namespace_root->insert_child("_SB_"sv, TRY(try_make_ref_counted<ScopeNode>())));
    TRY(namespace_root->insert_child("_SI_"sv, TRY(try_make_ref_counted<ScopeNode>())));
    TRY(namespace_root->insert_child("_TZ_"sv, TRY(try_make_ref_counted<ScopeNode>())));

    // https://uefi.org/specs/ACPI/6.6/05_ACPI_Software_Programming_Model.html#predefined-objects
    TRY(namespace_root->insert_child("_REV"sv, TRY(try_make_ref_counted<NameNode>(Object(u64(2))))));
    // FIXME: ACPI spec also mendates we insert _GL, _OSI, _OS, _DLM

    return namespace_root;
}

ErrorOr<void> Interpreter::load(ReadonlyBytes bytes)
{
    TRY(m_loaded_tables.try_append(TableStream(bytes)));
    auto& stream = m_loaded_tables.last();

    if (!validate_table(*reinterpret_cast<Structures::SDTHeader const*>(bytes.data())))
        INTERPRETER_ERROR("Incorrect checksum!", EINVAL, "Table checksum incorrect!");

    auto block_header = TRY(stream.read_sdt_header());
    outln("ACPI: Interpreter::load[{}]: Loading table '{}' by OEM '{}'", m_loaded_tables.size() - 1, StringView(block_header.oem_table_id, 8), StringView(block_header.oem_id, 6));

    auto root_scope_frame = ScopeFrame(m_namespace_root, m_namespace_root, stream.offset(), TRY(stream.size()), m_loaded_tables.size() - 1);
    TRY(m_scope_frames.try_append(move(root_scope_frame)));

    auto root_opcode_frame = TRY(OpcodeFrame::root_frame());
    TRY(m_opcode_frames.try_append(move(root_opcode_frame)));

    while (m_opcode_frames.size() > 0 && m_scope_frames.size() > 0) {
        if (TRY(cycle(stream)) == IterationDecision::Break) {
            break;
        }
    }

    if (m_scope_frames.size() != 0 || m_opcode_frames.size() != 1)
        return Error::from_string_view_or_print_error_and_return_errno("ACPI: Interpreter::load: Unbalanced scope or opcode frames!"sv, EINVAL);

    m_opcode_frames.take_last();
    return {};
}

}
