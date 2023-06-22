/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
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
    IntegersAre64Bit,
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

    ErrorOr<RefPtr<ASTNode>> parse_target();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_expression_opcode();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_object();
    ErrorOr<NonnullRefPtr<ASTNode>> parse_term_argument();

    ErrorOr<NonnullRefPtr<DefineName>> parse_define_name();
    ErrorOr<NonnullRefPtr<DefineScope>> parse_define_scope();
    ErrorOr<NonnullRefPtr<DefineBuffer>> parse_define_buffer();
    ErrorOr<NonnullRefPtr<DefinePackage>> parse_define_package();
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
