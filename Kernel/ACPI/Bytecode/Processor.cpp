/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntrusiveList.h>
#include <AK/NeverDestroyed.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/ACPI/Bytecode/Processor.h>
#include <Kernel/ACPI/Bytecode/TermObjectEnumerator.h>
#include <Kernel/Sections.h>

namespace Kernel::ACPI {

UNMAP_AFTER_INIT NonnullRefPtr<Processor> Processor::must_create(u8 processor_id, u32 processor_block_address, const NameString& preloaded_name_string, Span<u8 const> encoded_term_list)
{
    auto new_processor = adopt_ref_if_nonnull(new (nothrow) Processor(processor_id, processor_block_address, preloaded_name_string)).release_nonnull();
    new_processor->enumerate(encoded_term_list);
    return new_processor;
}

UNMAP_AFTER_INIT void Processor::enumerate(Span<u8 const> encoded_term_list)
{
    // Note: We assert here because a scope with no data in it makes no sense.
    VERIFY(encoded_term_list.size());
    TermObjectEnumerator enumerator(*this, encoded_term_list);
    enumerator.enumerate();
}

UNMAP_AFTER_INIT Processor::Processor(u8 processor_id, u32 processor_block_address, const NameString& preloaded_name_string)
    : Scope(preloaded_name_string)
    , m_processor_id(processor_id)
    , m_processor_block_address(processor_block_address)
{
}

}
