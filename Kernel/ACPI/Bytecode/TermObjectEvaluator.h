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
#include <AK/Result.h>
#include <Kernel/ACPI/Bytecode/EncodedTermOpcode.h>
#include <Kernel/ACPI/Bytecode/EvaluatedValue.h>
#include <Kernel/ACPI/Bytecode/NamedObject.h>
#include <Kernel/ACPI/Bytecode/Package.h>
#include <Kernel/ACPI/Bytecode/ScopeBase.h>
#include <Kernel/ACPI/Definitions.h>

namespace Kernel::ACPI {

// class TermObjectEnumerator;
class TermObjectEvaluator {
public:
    enum class DecodeError {
        UnknownOpcode,
    };

public:
    explicit TermObjectEvaluator(Span<const u8> encoded_bytes);
    EvaluatedValue try_to_evaluate_value() const;

    size_t overall_terms_span_length() const;

    EncodedTermOpcode current_opcode() const;

private:
    EvaluatedValue load_byte() const;
    EvaluatedValue load_word() const;
    EvaluatedValue load_dword() const;
    EvaluatedValue load_qword() const;

    EvaluatedValue load_ones() const;
    EvaluatedValue load_one() const;
    EvaluatedValue load_zero() const;

    EvaluatedValue load_null_terminated_string() const;
    EvaluatedValue load_buffer() const;
    EvaluatedValue load_package() const;

    Span<const u8> m_encoded_bytes;
    // const TermObjectEnumerator& m_enumerator;
    size_t m_byte_pointer { 0 };
};

}
