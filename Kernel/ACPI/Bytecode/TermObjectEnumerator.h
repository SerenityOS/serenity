/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/NeverDestroyed.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/ACPI/Bytecode/EncodedObjectOpcode.h>
#include <Kernel/ACPI/Bytecode/NamedObject.h>
#include <Kernel/ACPI/Bytecode/Package.h>
#include <Kernel/ACPI/Bytecode/ScopeBase.h>
#include <Kernel/ACPI/Definitions.h>

namespace Kernel::ACPI {
class TermObjectEnumerator {
public:
    enum class SkipPackageSizeEncoding {
        Yes = 1,
        No,
    };

public:
    TermObjectEnumerator(ScopeBase& scope_base, Span<const u8> encoded_bytes);

    void enumerate();
    Optional<Span<u8 const>> current_data_remainder(SkipPackageSizeEncoding) const;

private:
    class Pointer {
    public:
        void load_new_pointer()
        {
            m_byte_pointer += m_add_to_get_next_object;
            m_add_to_get_next_object = 0;
        }
        size_t current_pointer() const
        {
            return m_byte_pointer;
        }
        void increment_next_object_gap(size_t calculated_gap)
        {
            m_add_to_get_next_object += calculated_gap;
        }

    private:
        size_t m_byte_pointer { 0 };
        size_t m_add_to_get_next_object { 0 };
    };

private:
    EncodedObjectOpcode current_opcode() const;

    bool enumeration_ended() const;
    void enumerate_with_object_opcode();

    void add_dynamic_length_object_to_pointer(size_t calculated_length);

    bool current_object_is_package() const;

    Package::DecodingResult calculate_package_length() const;

    void add_scope();
    void add_name();
    void add_alias();

    void add_create_bit_field();
    void add_create_byte_field();
    void add_create_word_field();
    void add_create_dword_field();
    void add_create_qword_field();
    void add_create_field();
    void add_external();

    void add_bank_field();
    void add_data_region();
    void add_op_region();
    void add_power_resource();
    void add_processor();
    void add_thermal_zone();

    // Note: According to the ACPI spec, these are named objects, but they're
    // not defined under the "NamedObj :=" notation!
    void add_device();
    void add_event();
    void add_field();
    void add_index_field();
    void add_method();
    void add_mutex();

    Optional<Span<u8 const>> possible_data_remainder_after_opcode() const;

    ScopeBase& m_scope_base;
    Span<const u8> m_encoded_bytes;
    Pointer m_decode_pointer;
};

}
