/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/Firmware/ACPI/AML/AST.h>
#include <Kernel/Firmware/ACPI/AML/Namespace.h>
#include <Kernel/Library/KString.h>

namespace Kernel::ACPI::AML {

enum class IntegerBitness {
    IntegersAre32Bit,
    IntegersAre64Bit
};

class Parser {
public:
    explicit Parser(Namespace&, ReadonlyBytes, IntegerBitness);

    ErrorOr<void> populate_namespace();

private:
    u8 current() const { return m_bytecode[m_offset]; }
    [[nodiscard]] u8 consume()
    {
        auto byte = current();
        m_offset++;
        return byte;
    }
    void skip() { m_offset++; }

    ErrorOr<Vector<StringView>> resolve_path(StringView);
    ErrorOr<NonnullRefPtr<ASTNode>> get_object_at_path(StringView);

    u32 parse_package_length();
    ErrorOr<StringView> parse_name_segment();
    ErrorOr<NonnullOwnPtr<KString>> parse_name_string();

    u64 parse_byte_data();
    u64 parse_word_data();
    u64 parse_dword_data();
    u64 parse_qword_data();
    ErrorOr<u64> parse_integer();

    ErrorOr<ByteBuffer> parse_byte_list(size_t end_offset);
    ErrorOr<Vector<NonnullRefPtr<ASTNode>>> parse_term_list(size_t end_offset);
    ErrorOr<Vector<NonnullRefPtr<ASTNode>>> parse_field_list(size_t end_offset);
    ErrorOr<Vector<NonnullRefPtr<ASTNode>>> parse_package_element_list(size_t end_offset);

    ErrorOr<NonnullRefPtr<StringData>> parse_string();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_computational_data();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_data_object();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_data_reference_object();

    ErrorOr<NonnullRefPtr<LocalObject>> parse_local_object();
    ErrorOr<NonnullRefPtr<ArgumentObject>> parse_argument_object();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_simple_name();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_super_name();
    ErrorOr<RefPtr<ASTNode>> parse_target();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_method_invocation();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_reference_type_opcode();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_expression_opcode();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_statement_opcode();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_object();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_term_argument();

    ErrorOr<NonnullRefPtr<CreateDWordField>> parse_create_dword_field();
    ErrorOr<NonnullRefPtr<Acquire>> parse_acquire();
    ErrorOr<NonnullRefPtr<Release>> parse_release();
    ErrorOr<NonnullRefPtr<Store>> parse_store();
    ErrorOr<NonnullRefPtr<BinaryExpression>> parse_add();
    ErrorOr<NonnullRefPtr<BinaryExpression>> parse_subtract();
    ErrorOr<NonnullRefPtr<UpdateExpression>> parse_increment();
    ErrorOr<NonnullRefPtr<UpdateExpression>> parse_decrement();
    ErrorOr<NonnullRefPtr<BinaryExpression>> parse_shift_left();
    ErrorOr<NonnullRefPtr<BinaryExpression>> parse_shift_right();
    ErrorOr<NonnullRefPtr<BinaryExpression>> parse_bitwise_and();
    ErrorOr<NonnullRefPtr<BinaryExpression>> parse_bitwise_or();
    ErrorOr<NonnullRefPtr<UnaryExpression>> parse_deref_of();
    ErrorOr<NonnullRefPtr<UnaryExpression>> parse_size_of();
    ErrorOr<NonnullRefPtr<Index>> parse_index();
    ErrorOr<NonnullRefPtr<LogicalExpression>> parse_logical_and();
    ErrorOr<NonnullRefPtr<LogicalExpression>> parse_logical_or();
    ErrorOr<NonnullRefPtr<UnaryExpression>> parse_logical_not();
    ErrorOr<NonnullRefPtr<LogicalExpression>> parse_logical_equal();
    ErrorOr<NonnullRefPtr<LogicalExpression>> parse_logical_greater();
    ErrorOr<NonnullRefPtr<LogicalExpression>> parse_logical_less();
    ErrorOr<NonnullRefPtr<ToBuffer>> parse_to_buffer();
    ErrorOr<NonnullRefPtr<ToHexString>> parse_to_hex_string();

    ErrorOr<NonnullRefPtr<Notify>> parse_notify();
    ErrorOr<NonnullRefPtr<If>> parse_if();
    ErrorOr<NonnullRefPtr<While>> parse_while();
    ErrorOr<NonnullRefPtr<Return>> parse_return();
    ErrorOr<NonnullRefPtr<Break>> parse_break();

    ErrorOr<NonnullRefPtr<DefineName>> parse_define_name();
    ErrorOr<NonnullRefPtr<DefineScope>> parse_define_scope();
    ErrorOr<NonnullRefPtr<DefineBuffer>> parse_define_buffer();
    ErrorOr<NonnullRefPtr<DefinePackage>> parse_define_package();
    ErrorOr<NonnullRefPtr<DefineVariablePackage>> parse_define_variable_package();
    ErrorOr<NonnullRefPtr<DefineMethod>> parse_define_method();
    ErrorOr<NonnullRefPtr<DefineMutex>> parse_define_mutex();
    ErrorOr<NonnullRefPtr<DefineOperationRegion>> parse_define_operation_region();
    ErrorOr<NonnullRefPtr<DefineField>> parse_define_field();
    ErrorOr<NonnullRefPtr<DefineDevice>> parse_define_device();
    ErrorOr<NonnullRefPtr<DefineProcessor>> parse_define_processor();

    ErrorOr<void> process_deferred_methods();

    Namespace& m_root_namespace;
    Vector<StringView> m_current_scope;

    ReadonlyBytes m_bytecode;
    size_t m_offset { 0 };
    IntegerBitness m_integer_bitness;

    struct DeferredMethod {
        NonnullRefPtr<DefineMethod> method;
        Vector<StringView> scope;
        size_t start;
        size_t end;
    };
    Vector<DeferredMethod> m_deferred_methods;
};

}
