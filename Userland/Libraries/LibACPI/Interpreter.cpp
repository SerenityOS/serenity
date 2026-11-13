/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Debug.h>
#include <AK/NumericLimits.h>
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

static constexpr size_t PREDEFINED_METHOD_TABLE_INDEX = NumericLimits<size_t>::max();

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
                return resolve_alias(found.release_value());

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

    return resolve_alias(current_scope.release_nonnull());
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
    auto& top_frame = m_frame_stack.last();

    if constexpr (AML_DEBUG) {
        warn("ACPI: {:08X} ", stream.offset());
        for (size_t i = 0; i < (top_frame.opcode_frames.size() - 1) - (top_frame.state == State::FinalizingOpcode ? 1 : 0); i++) {
            warn("|   ");
        }

        switch (top_frame.state) {
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
    auto& top_scope_frame = m_frame_stack.last().scope_frames.last();

    switch (frame.opcode()) {
    case Opcode::ZeroOp:
        return Object(u64(0));
    case Opcode::OneOp:
        return Object(u64(1));
    case Opcode::OnesOp:
        return Object(top_scope_frame.number_mask());

    case Opcode::BytePrefix:
    case Opcode::WordPrefix:
    case Opcode::DWordPrefix:
    case Opcode::QWordPrefix:
    case Opcode::StringPrefix:
    case Opcode::EvaluatedValue:
        return move(frame.operand(0));

    case Opcode::BufferOp:
        return move(frame.operand(2));

    case Opcode::PackageOp: {
        auto operands = frame.release_operands();
        operands.remove(0, 2);
        return Package(move(operands));
    }

    case Opcode::AddOp:
    case Opcode::SubtractOp:
    case Opcode::MultiplyOp:
    case Opcode::ShiftLeftOp:
    case Opcode::ShiftRightOp:
    case Opcode::AndOp:
    case Opcode::NandOp:
    case Opcode::OrOp:
    case Opcode::NorOp:
    case Opcode::XorOp: {
        auto const calculate = [](Opcode opcode, u64 left, u64 right) -> u64 {
            switch (opcode) {
            case Opcode::AddOp:
                return left + right;
            case Opcode::SubtractOp:
                return left - right;
            case Opcode::MultiplyOp:
                return left * right;
            case Opcode::ShiftLeftOp:
                return left << right;
            case Opcode::ShiftRightOp:
                return left >> right;
            case Opcode::AndOp:
                return left & right;
            case Opcode::NandOp:
                return ~(left & right);
            case Opcode::OrOp:
                return left | right;
            case Opcode::NorOp:
                return ~(left | right);
            case Opcode::XorOp:
                return left ^ right;
            default:
                VERIFY_NOT_REACHED();
            }
        };

        auto left = TRY(TRY(resolve_references(frame.operand(0)))->as<u64>());
        auto right = TRY(TRY(resolve_references(frame.operand(1)))->as<u64>());
        auto& target = frame.operand(2);

        auto result = Object(calculate(frame.opcode(), left, right) & top_scope_frame.number_mask());

        TRY(store(target, TRY(result.clone())));
        return result;
    }

    case Opcode::LAndOp:
    case Opcode::LOrOp:
    case Opcode::LEqualOp:
    case Opcode::LGreaterOp:
    case Opcode::LLessOp: {
        auto const calculate = [](Opcode opcode, u64 left, u64 right) -> u64 {
            switch (opcode) {
            case Opcode::LAndOp:
                return left && right;
            case Opcode::LOrOp:
                return left || right;
            case Opcode::LEqualOp:
                return left == right;
            case Opcode::LGreaterOp:
                return left > right;
            case Opcode::LLessOp:
                return left < right;
            default:
                VERIFY_NOT_REACHED();
            }
        };

        auto left = TRY(TRY(resolve_references(frame.operand(0)))->as<u64>());
        auto right = TRY(TRY(resolve_references(frame.operand(1)))->as<u64>());

        return Object(calculate(frame.opcode(), left, right));
    }

    case Opcode::LNotOp: {
        auto operand = TRY(TRY(resolve_references(frame.operand(0)))->as<u64>());
        return Object(static_cast<u64>(!operand));
    }

    case Opcode::Local0Op... Opcode::Local7Op:
        return LocalReference(to_underlying(frame.opcode()) - to_underlying(Opcode::Local0Op));
    case Opcode::Arg0Op... Opcode::Arg6Op:
        return ArgumentReference(to_underlying(frame.opcode()) - to_underlying(Opcode::Arg0Op));

    case Opcode::StoreOp: {
        auto value = move(frame.operand(0));
        TRY(store(frame.operand(1), TRY(value.clone())));
        return value;
    }

    case Opcode::NodeEvaluation: {
        auto name = TRY(frame.operand(0).release_as<NameString>());
        auto node = TRY(find_node(name, top_scope_frame.scope()));

        auto arguments = frame.release_operands();
        arguments.remove(0, 1);

        return evaluate(node, move(arguments));
    }

    default:
        INTERPRETER_ERROR("Unimplemented!", EINVAL, "Unimplemented opcode {}!", frame.opcode());
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<void> Interpreter::finalize_term_opcode(OpcodeFrame& frame, TableStream& stream)
{
    auto& top_frame = m_frame_stack.last();
    auto& top_scope_frame = top_frame.scope_frames.last();

    switch (frame.opcode()) {
    case Opcode::AliasOp: {
        auto target = TRY(frame.operand(0).release_as<NameString>());
        auto path = TRY(frame.operand(1).release_as<NameString>());

        auto target_node = TRY(find_node(target, top_scope_frame.scope()));
        auto node = TRY(try_make_ref_counted<AliasNode>(target_node));
        TRY(top_scope_frame.insert_node(path, node));
        return {};
    }

    case Opcode::ScopeOp: {
        auto length = TRY(frame.operand(0).release_as<u64>());
        auto path = TRY(frame.operand(1).release_as<NameString>());
        auto scope = TRY(find_node(path, top_scope_frame.scope()));

        auto scope_frame = ScopeFrame(m_namespace_root, scope, frame.start(), frame.start() + length, m_loaded_tables.size() - 1, top_scope_frame.number_mask());
        TRY(top_frame.scope_frames.try_append(move(scope_frame)));
        return {};
    }

    case Opcode::NameOp: {
        auto path = TRY(frame.operand(0).release_as<NameString>());
        auto data = move(frame.operand(1));

        auto node = TRY(try_make_ref_counted<NameNode>(move(data)));
        TRY(top_scope_frame.insert_node(path, node));
        return {};
    }

    case Opcode::MethodOp: {
        auto length = TRY(frame.operand(0).release_as<u64>());
        auto path = TRY(frame.operand(1).release_as<NameString>());
        auto flags = TRY(frame.operand(2).release_as<u64>());

        auto node = TRY(try_make_ref_counted<MethodNode>(stream.offset(), frame.start() + length, m_loaded_tables.size() - 1, bit_cast<MethodFlags>(static_cast<u8>(flags)), top_scope_frame.number_mask()));
        TRY(top_scope_frame.insert_node(path, node));

        TRY(stream.seek(frame.start() + length));
        return {};
    }

    case Opcode::MutexOp: {
        auto path = TRY(frame.operand(0).release_as<NameString>());
        auto sync_flags = TRY(frame.operand(1).release_as<u64>());

        auto node = TRY(try_make_ref_counted<MutexNode>(sync_flags));
        TRY(top_scope_frame.insert_node(path, node));
        return {};
    }

    case Opcode::OpRegionOp: {
        auto path = TRY(frame.operand(0).release_as<NameString>());
        auto region_space = TRY(frame.operand(1).release_as<u64>());
        auto region_offset = TRY(frame.operand(2).release_as<u64>());
        auto region_length = TRY(frame.operand(3).release_as<u64>());

        auto node = TRY(try_make_ref_counted<OperationRegionNode>(region_space, region_offset, region_length));
        TRY(top_scope_frame.insert_node(path, node));
        return {};
    }

    case Opcode::FieldOp:
    case Opcode::IndexFieldOp:
        // NOTE: All the field elements are read as operands, so all we need to do is reset the field offset.
        top_frame.current_field_offset = 0;
        return {};

    case Opcode::DeviceOp: {
        auto length = TRY(frame.operand(0).release_as<u64>());
        auto path = TRY(frame.operand(1).release_as<NameString>());

        auto node = TRY(try_make_ref_counted<DeviceNode>());
        TRY(top_scope_frame.insert_node(path, node));

        auto scope_frame = ScopeFrame(m_namespace_root, node, frame.start(), frame.start() + length, m_loaded_tables.size() - 1, top_scope_frame.number_mask());
        TRY(top_frame.scope_frames.try_append(move(scope_frame)));
        return {};
    }

    case Opcode::ProcessorOp: {
        auto length = TRY(frame.operand(0).release_as<u64>());
        auto path = TRY(frame.operand(1).release_as<NameString>());
        auto id = TRY(frame.operand(2).release_as<u64>());
        auto processor_register_block_address = TRY(frame.operand(3).release_as<u64>());
        auto processor_register_block_length = TRY(frame.operand(4).release_as<u64>());

        auto node = TRY(try_make_ref_counted<ProcessorNode>(id, processor_register_block_address, processor_register_block_length));
        TRY(top_scope_frame.insert_node(path, node));

        auto scope_frame = ScopeFrame(m_namespace_root, node, frame.start(), frame.start() + length, m_loaded_tables.size() - 1, top_scope_frame.number_mask());
        TRY(top_frame.scope_frames.try_append(move(scope_frame)));
        return {};
    }

    case Opcode::PowerResOp: {
        auto length = TRY(frame.operand(0).release_as<u64>());
        auto path = TRY(frame.operand(1).release_as<NameString>());
        auto system_level = TRY(frame.operand(2).release_as<u64>());
        auto resource_order = TRY(frame.operand(3).release_as<u64>());

        auto node = TRY(try_make_ref_counted<PowerResourceNode>(system_level, resource_order));
        TRY(top_scope_frame.insert_node(path, node));

        auto scope_frame = ScopeFrame(m_namespace_root, node, frame.start(), frame.start() + length, m_loaded_tables.size() - 1, top_scope_frame.number_mask());
        TRY(top_frame.scope_frames.try_append(move(scope_frame)));
        return {};
    }

    case Opcode::IfOp: {
        auto length = TRY(frame.operand(0).release_as<u64>()) - (stream.offset() - frame.start());
        auto condition = TRY(frame.operand(1).release_as<u64>());

        top_frame.last_if_condition = condition;
        if (!condition)
            TRY(stream.seek(length, SeekMode::FromCurrentPosition));

        return {};
    }
    case Opcode::ElseOp: {
        auto length = TRY(frame.operand(0).release_as<u64>()) - (stream.offset() - frame.start());

        if (top_frame.last_if_condition)
            TRY(stream.seek(length, SeekMode::FromCurrentPosition));

        return {};
    }

    case Opcode::ReturnOp:
        // FIXME: We shouldn't resolve the references here
        top_scope_frame.return_value() = move(*TRY(resolve_references(frame.operand(0))));
        return {};

    default:
        TRY(finalize_value_opcode(frame));
        return {};
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<void> Interpreter::read_operand(TableStream& stream, OperandType operand)
{
    auto& top_frame = m_frame_stack.last();
    auto& top_scope_frame = top_frame.scope_frames.last();
    auto& top_opcode_frame = top_frame.opcode_frames.last();

    switch (operand) {
    case OperandType::NameString: {
        auto value = TRY(stream.read_name_string());
        DEBUG_LOG("Reading namestring... ({})", value.to_string());
        TRY(top_opcode_frame.finish_operand(move(value), stream.offset()));
        return {};
    }
    case OperandType::PackageLength: {
        auto length = TRY(stream.read_package_length());
        DEBUG_LOG("Reading package length... ({:#X})", length);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(length), stream.offset()));
        return {};
    }
    case OperandType::ByteData: {
        auto value = TRY(stream.read_value<u8>());
        DEBUG_LOG("Reading byte... ({})", value);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(value), stream.offset()));
        return {};
    }
    case OperandType::WordData: {
        auto value = TRY(stream.read_value<u16>());
        DEBUG_LOG("Reading word... ({})", value);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(value), stream.offset()));
        return {};
    }
    case OperandType::DWordData: {
        auto value = TRY(stream.read_value<u32>());
        DEBUG_LOG("Reading dword... ({})", value);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(value), stream.offset()));
        return {};
    }
    case OperandType::QWordData: {
        auto value = TRY(stream.read_value<u64>());
        DEBUG_LOG("Reading qword... ({})", value);
        TRY(top_opcode_frame.finish_operand(static_cast<u64>(value) & top_scope_frame.number_mask(), stream.offset()));
        return {};
    }
    case OperandType::String: {
        DEBUG_LOG("Reading string...");
        auto value = TRY(stream.read_string());
        TRY(top_opcode_frame.finish_operand(String(move(value)), stream.offset()));
        return {};
    }

    // https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#fieldelement
    case OperandType::FieldElement:
    case OperandType::IndexFieldElement: {
#ifndef KERNEL
        if constexpr (AML_DEBUG)
            warn("Reading field element...");
#endif
        if (is_lead_name_char(TRY(stream.peek_byte()))) {
            // NamedField := NameSeg PkgLength
            auto name_segment_bytes = TRY(stream.read_in_place<u8 const>(4));
            auto name_segment = TRY(NameSegment::from_bytes(name_segment_bytes));

            auto package_length = TRY(stream.read_package_length());

            if (operand == OperandType::FieldElement) {
                auto operation_region_node = TRY(find_node(*TRY(top_opcode_frame.operand(1).as_pointer<NameString>()), top_scope_frame.scope()));
                if (operation_region_node->type() != NodeType::OperationRegion)
                    INTERPRETER_ERROR("Expected operation region node!", EINVAL, "Expected operation region node, got {}", operation_region_node->kind());

                auto flags = TRY(top_opcode_frame.operand(2).release_as<u64>());

                DEBUG_LOG(" ({}, {})", name_segment.to_string_view(), package_length);

                auto node = TRY(try_make_ref_counted<FieldNode>(static_ptr_cast<OperationRegionNode>(operation_region_node), bit_cast<FieldFlags>(static_cast<u8>(flags)), top_frame.current_field_offset, package_length));
                TRY(top_scope_frame.scope()->insert_child(name_segment, node));
            } else if (operand == OperandType::IndexFieldElement) {
                auto index_node = TRY(find_node(*TRY(top_opcode_frame.operand(1).as_pointer<NameString>()), top_scope_frame.scope()));
                if (index_node->type() != NodeType::Field)
                    INTERPRETER_ERROR("Expected field node!", EINVAL, "Expected field node, got {}", index_node->kind());

                auto data_node = TRY(find_node(*TRY(top_opcode_frame.operand(2).as_pointer<NameString>()), top_scope_frame.scope()));
                if (data_node->type() != NodeType::Field)
                    INTERPRETER_ERROR("Expected field node!", EINVAL, "Expected field node, got {}", data_node->kind());

                auto flags = TRY(top_opcode_frame.operand(3).release_as<u64>());

                DEBUG_LOG(" ({}, {})", name_segment.to_string_view(), package_length);

                auto node = TRY(try_make_ref_counted<IndexFieldNode>(static_ptr_cast<FieldNode>(index_node), static_ptr_cast<FieldNode>(data_node), bit_cast<FieldFlags>(static_cast<u8>(flags)), top_frame.current_field_offset, package_length));
                TRY(top_scope_frame.scope()->insert_child(name_segment, node));
            }

            top_frame.current_field_offset += package_length;

            TRY(top_opcode_frame.finish_operand(stream.offset()));
            return {};
        }

        auto type = TRY(stream.read_value<u8>());
        DEBUG_LOG(" Type: {:02X}", type);
        switch (type) {
        case 0x00: {
            // ReservedField := 0x00 PkgLength
            top_frame.current_field_offset += TRY(stream.read_package_length());

            TRY(top_opcode_frame.finish_operand(stream.offset()));
            return {};
        }

        default:
            INTERPRETER_ERROR("Unimplemented field type!", EINVAL, "Unimplemented field type {:02X}!", type);
        }

        VERIFY_NOT_REACHED();
    }

    case OperandType::ByteBuffer: {
        auto package_length = TRY(top_opcode_frame.operand(0).as<u64>());
        auto buffer_data_length = package_length - (stream.offset() - top_opcode_frame.start());

        auto size = TRY(top_opcode_frame.operand(1).as<u64>());
        DEBUG_LOG("Reading byte buffer of size {}...", size);
        auto buffer = TRY(ByteBuffer::create_zeroed(size));

        for (size_t i = 0; i < buffer_data_length; i++)
            buffer[i] = TRY(stream.read_value<u8>());

        TRY(top_opcode_frame.finish_operand(Object(Buffer(move(buffer))), stream.offset()));
        return {};
    }

    case OperandType::PackageElement: {
        DEBUG_LOG("Reading package element...");

        auto initial = TRY(stream.peek_byte());
        if (is_namestring_start(initial)) {
            auto name_string = TRY(stream.read_name_string());
            auto buffer = TRY(name_string.to_bytes());
            TRY(top_opcode_frame.finish_operand(Object(Buffer(move(buffer))), stream.offset()));
            return {};
        }

        top_frame.state = State::ReadingOpcode;
        return {};
    }

    case OperandType::TermArg:
    case OperandType::DataRefObject:
        DEBUG_LOG("Reading complex object {}...", to_underlying(operand));

        top_frame.state = State::ReadingOpcode;
        return {};

    case OperandType::SuperName:
    case OperandType::Target:
        DEBUG_LOG("Reading target...");

        if (is_namestring_start(TRY(stream.peek_byte())))
            INTERPRETER_ERROR("Unsupported target!", ENOTSUP, "Reading name strings as targets is not yet supported!");

        if (operand == OperandType::Target && TRY(stream.peek_byte()) == 0x00) {
            TRY(stream.read_value<u8>());
            TRY(top_opcode_frame.finish_operand(Object(), stream.offset()));
            return {};
        }

        top_frame.state = State::ReadingOpcode;
        return {};

    default:
        DEBUG_LOG("Failure!");
        INTERPRETER_ERROR("Unimplemented operand type!", EINVAL, "Operand type {} unimplemented!", to_underlying(operand));
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<void> Interpreter::cycle(TableStream& stream)
{
    auto& top_frame = m_frame_stack.last();

    switch (top_frame.state) {
    case Interpreter::State::ReadingOpcode: {
        // Pop all scope frames that we've exited.
        // Do it here in case any pending opcodes need to be finalized at EOF.
        while (top_frame.scope_frames.size() != 0) {
            auto& scope_frame = top_frame.scope_frames.last();
            if (stream.offset() < scope_frame.end()) {
                break;
            }

            top_frame.scope_frames.take_last();
        }

        if (top_frame.scope_frames.is_empty()) {
            if (top_frame.opcode_frames.size() > 1)
                INTERPRETER_ERROR("Unexpected EOF!", EINVAL, "Reached EOF with unclosed opcode frames!");
            m_frame_stack.take_last();
            return {};
        }

        debug_print_header(stream);

        if (is_namestring_start(TRY(stream.peek_byte()))) {
            auto name = TRY(stream.read_name_string());
            DEBUG_LOG("{}", name.to_string());

            auto node = TRY(find_node(name, top_frame.scope_frames.last().scope()));
            size_t argument_count = node->type() == NodeType::Method ? static_ptr_cast<MethodNode>(node)->argument_count() : 0;

            Vector<OperandType> operand_template;
            TRY(operand_template.try_ensure_capacity(argument_count + 2));
            TRY(operand_template.try_append(OperandType::NameString));
            for (size_t i = 0; i < argument_count; ++i)
                TRY(operand_template.try_append(OperandType::TermArg));
            TRY(operand_template.try_append(OperandType::End));

            auto opcode_frame = TRY(OpcodeFrame::from_opcode(Opcode::NodeEvaluation, stream.offset(), operand_template));
            TRY(opcode_frame.finish_operand(Object(move(name)), 0));

            TRY(top_frame.opcode_frames.try_append(move(opcode_frame)));
            top_frame.state = State::ReadingOperand;
            return {};
        }

        auto opcode = TRY(stream.read_opcode());
        if (!top_frame.opcode_frames.last().filter_opcode_for_next_operand(opcode)) {
            DEBUG_LOG("Failure!");
            if (top_frame.opcode_frames.size() == 1)
                INTERPRETER_ERROR("Filtered opcode!", EINVAL, "Argument opcode {} filtered at root!", opcode);

            INTERPRETER_ERROR("Filtered opcode!", EINVAL, "Argument opcode {} filtered by opcode {}!", opcode, top_frame.opcode_frames.last().opcode());
        }

        DEBUG_LOG("Opcode: {}", opcode);

        auto opcode_frame = TRY(OpcodeFrame::from_opcode(static_cast<Opcode>(opcode), stream.offset()));
        TRY(top_frame.opcode_frames.try_append(move(opcode_frame)));
        top_frame.state = State::ReadingOperand;
        return {};
    }

    case Interpreter::State::ReadingOperand: {
        auto operand = TRY(top_frame.opcode_frames.last().begin_operand(stream.offset()));
        if (operand == OperandType::End) {
            top_frame.state = State::FinalizingOpcode;
            return {};
        }

        debug_print_header(stream, top_frame.opcode_frames.last().operand_count());
        TRY(read_operand(stream, operand));
        return {};
    }

    case Interpreter::State::FinalizingOpcode: {
        debug_print_header(stream);

        auto top_frame_value = top_frame.opcode_frames.take_last();
        // If we receive an EAGAIN we should continue the execution loop because another method has started execution.
        if (top_frame.opcode_frames.size() > 1) {
            DEBUG_LOG("Finalizing argument {}", top_frame_value.opcode());

            auto maybe_value = finalize_value_opcode(top_frame_value);
            if (maybe_value.is_error()) {
                if (maybe_value.error().code() == EAGAIN)
                    return {};
                return maybe_value.release_error();
            }
            TRY(top_frame.opcode_frames.last().finish_operand(maybe_value.release_value(), stream.offset()));

            top_frame.state = State::ReadingOperand;
        } else {
            DEBUG_LOG("Finalizing term {}", top_frame_value.opcode());

            auto maybe_error = finalize_term_opcode(top_frame_value, stream);
            if (maybe_error.is_error()) {
                if (maybe_error.error().code() == EAGAIN)
                    return {};
                return maybe_error.release_error();
            }

            top_frame.state = State::ReadingOpcode;
        }

        if (!m_frame_stack.last().scope_frames.last().return_value().is_empty() && m_frame_stack.size() > 1) {
            auto return_value = move(m_frame_stack.last().scope_frames.last().return_value());
            m_frame_stack.take_last();

            Vector<OperandType> operand_template;
            TRY(operand_template.try_append(OperandType::TermArg));
            TRY(operand_template.try_append(OperandType::End));

            auto opcode_frame = TRY(OpcodeFrame::from_opcode(Opcode::EvaluatedValue, stream.offset(), operand_template));
            TRY(opcode_frame.finish_operand(move(return_value), 0));
            TRY(m_frame_stack.last().opcode_frames.try_append(move(opcode_frame)));
        }

        return {};
    }
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<Object> Interpreter::evaluate(StringView path, ReadonlySpan<Object> arguments)
{
    return evaluate(TRY(NameString::from_string_view(path)), arguments);
}

ErrorOr<Object> Interpreter::evaluate(NameString const& path, ReadonlySpan<Object> arguments)
{
    auto node = TRY(find_node(path, m_namespace_root));
    return evaluate(node, arguments);
}

ErrorOr<Object> Interpreter::evaluate(Node& node, ReadonlySpan<Object> arguments)
{
    if (node.type() != NodeType::Method && !arguments.is_empty())
        INTERPRETER_ERROR("Cannot pass arguments to non-method node!", EINVAL, "Node type {} does not accept arguments!", node.kind());

    switch (node.type()) {
    case NodeType::Name: {
        auto& name_node = static_cast<NameNode&>(node);
        return name_node.data().clone();
    }

    case NodeType::Field:
        INTERPRETER_ERROR("Field evaluation not supported!", EINVAL, "Evaluating fields is not supported yet!");
    case NodeType::IndexField:
        INTERPRETER_ERROR("IndexField evaluation not supported!", EINVAL, "Evaluating index fields is not supported yet!");

    case NodeType::Method:
        return call_method(static_cast<MethodNode&>(node), arguments);

    default:
        INTERPRETER_ERROR("Cannot evaluate node type!", EINVAL, "Node type {} not supported for evaluation!", node.kind());
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<Object> Interpreter::call_method(MethodNode& node, ReadonlySpan<Object> arguments)
{
    dbgln_if(AML_DEBUG, "ACPI: Calling method {}", node.name().to_string_view());

    if (node.table_index() == PREDEFINED_METHOD_TABLE_INDEX)
        INTERPRETER_ERROR("Running predefined methods is not implemented", ENOTSUP, "Running predefined methods is not implemented");

    if (arguments.size() != node.argument_count())
        INTERPRETER_ERROR("Argument count mismatch!", EINVAL, "Expected {} arguments, got {}", node.argument_count(), arguments.size());

    auto parent_scope = node.parent().strong_ref();
    if (!parent_scope)
        INTERPRETER_ERROR("Method has no parent!", EINVAL, "Method node at {} has no parent!", node.start());

    auto root_scope_frame = ScopeFrame(m_namespace_root, parent_scope.release_nonnull(), node.start(), node.end(), node.table_index(), node.number_mask());
    for (size_t i = 0; i < arguments.size(); ++i) {
        auto argument = TRY(arguments[i].clone());
        root_scope_frame.argument(i) = move(*TRY(resolve_references(argument)));
    }

    TableStream stream(m_loaded_tables[node.table_index()]);
    TRY(stream.seek(node.start()));

    TRY(allocate_frame(move(stream), move(root_scope_frame)));

    // If there were already frames on the stack, something is already running the cycle loop.
    // We need to inform the caller that it should continue executing the cycle loop to get the value.
    if (m_frame_stack.size() > 1)
        return Error::from_errno(EAGAIN);

    while (!m_frame_stack.is_empty()) {
        TRY(cycle(m_frame_stack.last().stream));

        if (!m_frame_stack.is_empty() && !m_frame_stack.last().scope_frames.last().return_value().is_empty())
            break;
    }

    if (!m_frame_stack.is_empty() && !m_frame_stack.last().scope_frames.is_empty() && !m_frame_stack.last().scope_frames.last().return_value().is_empty()) {
        VERIFY(m_frame_stack.size() == 1);
        auto return_value = move(m_frame_stack.last().scope_frames.last().return_value());
        m_frame_stack.take_last();
        return return_value;
    }

    return Object();
}

ErrorOr<Object*> Interpreter::resolve_reference(Object& reference)
{
    auto& top_scope_frame = m_frame_stack.last().scope_frames.last();

    if (reference.is<ArgumentReference>()) {
        auto argument_index = MUST(reference.as<ArgumentReference>()).index();
        return &top_scope_frame.argument(argument_index);
    }

    if (reference.is<LocalReference>()) {
        auto local_index = MUST(reference.as<LocalReference>()).index();
        return &top_scope_frame.local(local_index);
    }

    if (reference.is<NodeReference>())
        INTERPRETER_ERROR("Resolving node references is not supported!", EINVAL, "Resolving node references is not supported yet!");

    return &reference;
}

ErrorOr<Object*> Interpreter::resolve_references(Object& reference)
{
    auto* object = &reference;
    while (object->is_reference())
        object = TRY(resolve_reference(*object));
    return object;
}

NonnullRefPtr<Node> Interpreter::resolve_alias(NonnullRefPtr<Node> alias_node) const
{
    auto node = alias_node;
    while (node->type() == NodeType::Alias) {
        auto& alias_node = static_cast<AliasNode&>(*node);
        node = alias_node.target();
    }
    return node;
}

ErrorOr<void> Interpreter::store(Object const& target, Object value)
{
    // FIXME: Handle multilevel references for the target object.
    // FIXME: Handle buffer size immutability.

    if (target.is_empty())
        return {};

    if (target.is<ArgumentReference>()) {
        auto argument_index = MUST(target.as<ArgumentReference>()).index();
        m_frame_stack.last().scope_frames.last().argument(argument_index) = move(value);
        return {};
    }

    if (target.is<LocalReference>()) {
        auto local_index = MUST(target.as<LocalReference>()).index();
        m_frame_stack.last().scope_frames.last().local(local_index) = move(value);
        return {};
    }

    if (target.is<NodeReference>())
        INTERPRETER_ERROR("Setting node values is not supported!", EINVAL, "Setting node values is not supported yet!");

    INTERPRETER_ERROR("Unsupported target type!", EINVAL, "Unsupported target type!");
}

ErrorOr<void> Interpreter::allocate_frame(TableStream stream, ScopeFrame root_scope_frame)
{
    Frame frame(move(stream));

    auto root_opcode_frame = TRY(OpcodeFrame::root_frame());
    TRY(frame.opcode_frames.try_append(move(root_opcode_frame)));
    TRY(frame.scope_frames.try_append(move(root_scope_frame)));

    TRY(m_frame_stack.try_append(move(frame)));
    return {};
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
    TRY(namespace_root->insert_child("_GL_"sv, TRY(try_make_ref_counted<MutexNode>(0))));
    TRY(namespace_root->insert_child("_REV"sv, TRY(try_make_ref_counted<NameNode>(Object(u64(2))))));

    MethodFlags flags {};
    flags.argument_count = 1;
    TRY(namespace_root->insert_child("_OSI"sv, TRY(try_make_ref_counted<MethodNode>(0, 0, PREDEFINED_METHOD_TABLE_INDEX, flags, NumericLimits<u64>::max()))));

    // NOTE: We need to pretend to be Windows to ensure compatibility with a wide range of BIOSes.
    //       See https://docs.kernel.org/firmware-guide/acpi/osi.html.
    constexpr StringView os_name = "Microsoft Windows NT"sv;

    ByteBuffer os_buffer;
    TRY(os_buffer.try_append(os_name.bytes()));
    TRY(namespace_root->insert_child("_OS_"sv, TRY(try_make_ref_counted<NameNode>(String(move(os_buffer))))));

    return namespace_root;
}

ErrorOr<void> Interpreter::load(ReadonlyBytes bytes)
{
    TRY(m_loaded_tables.try_append(TRY(ByteBuffer::copy(bytes))));

    auto& buffer = m_loaded_tables.last();
    TableStream stream(buffer);

    if (!validate_table(*reinterpret_cast<Structures::SDTHeader const*>(bytes.data())))
        INTERPRETER_ERROR("Incorrect checksum!", EINVAL, "Table checksum incorrect!");

    auto block_header = TRY(stream.read_sdt_header());
    outln("ACPI: Interpreter::load[{}]: Loading table '{}' by OEM '{}'", m_loaded_tables.size() - 1, StringView(block_header.oem_table_id, 8), StringView(block_header.oem_id, 6));

    u64 number_mask = block_header.revision < 2 ? NumericLimits<u32>::max() : NumericLimits<u64>::max();
    dbgln_if(AML_DEBUG, "ACPI: Interpreter::load: Number mask: {:x}", number_mask);

    auto root_scope_frame = ScopeFrame(m_namespace_root, m_namespace_root, stream.offset(), TRY(stream.size()), m_loaded_tables.size() - 1, number_mask);
    TRY(allocate_frame(move(stream), move(root_scope_frame)));

    while (!m_frame_stack.is_empty())
        TRY(cycle(m_frame_stack.last().stream));

    return {};
}

}
