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
#include <Kernel/ACPI/Bytecode/Scope.h>
#include <Kernel/ACPI/Bytecode/TermObjectEnumerator.h>
#include <Kernel/Sections.h>

namespace Kernel::ACPI {

UNMAP_AFTER_INIT NonnullRefPtr<Scope> Scope::must_create(const TermObjectEnumerator& parent_enumerator, Span<u8 const> encoded_name_string)
{
    auto new_scope = adopt_ref_if_nonnull(new (nothrow) Scope(encoded_name_string)).release_nonnull();
    new_scope->enumerate(parent_enumerator);
    return new_scope;
}

UNMAP_AFTER_INIT void Scope::enumerate(const TermObjectEnumerator& parent_enumerator)
{
    auto scope_data_bytes = parent_enumerator.current_data_remainder(TermObjectEnumerator::SkipPackageSizeEncoding::Yes);
    // Note: We assert here because a scope with no data in it makes no sense.
    VERIFY(scope_data_bytes.has_value());
    VERIFY(scope_data_bytes.value().size());
    // Note: Start the enumeration from the end of the Scope NameString.
    dbgln_if(ACPI_AML_DEBUG, "Scope name {}, length {}", m_name_string->full_name(), m_name_string->encoded_length());
    TermObjectEnumerator enumerator(*this, scope_data_bytes.value().slice(m_name_string->encoded_length()));
    enumerator.enumerate();
}

UNMAP_AFTER_INIT Scope::Scope(Span<u8 const> encoded_name_string)
    : NamedObject(encoded_name_string)
{
}

UNMAP_AFTER_INIT Scope::Scope(const NameString& preloaded_name_string)
    : NamedObject(preloaded_name_string)
{
}

}
