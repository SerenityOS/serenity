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
#include <Kernel/ACPI/Bytecode/NamedObject.h>
#include <Kernel/ACPI/Bytecode/Scope.h>
#include <Kernel/ACPI/Definitions.h>

namespace Kernel::ACPI {

class TermObjectEnumerator;
class Processor : public Scope {
public:
    virtual NamedObject::Type type() const override { return NamedObject::Type::Processor; }
    static NonnullRefPtr<Processor> must_create(u8 processor_id, u32 processor_block_address, const NameString& preloaded_name_string, Span<u8 const> encoded_term_list);
    void enumerate(Span<u8 const> encoded_term_list);

    u8 processor_id() const { return m_processor_id; }
    u8 processor_block_address() const { return m_processor_block_address; }

private:
    Processor(u8 processor_id, u32 processor_block_address, const NameString& preloaded_name_string);
    const u8 m_processor_id;
    const u32 m_processor_block_address;
};

}
