/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/Bytecode/Alias.h>
#include <Kernel/ACPI/Bytecode/EncodedObjectOpcode.h>
#include <Kernel/ACPI/Bytecode/GlobalScope.h>
#include <Kernel/ACPI/Bytecode/Package.h>
#include <Kernel/ACPI/Bytecode/TermObjectEnumerator.h>
#include <Kernel/Sections.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::ACPI {

UNMAP_AFTER_INIT NonnullOwnPtr<GlobalScope> GlobalScope::must_create(const Vector<PhysicalAddress>& aml_table_addresses)
{
    return adopt_own_if_nonnull(new (nothrow) GlobalScope(aml_table_addresses)).release_nonnull();
}

UNMAP_AFTER_INIT void GlobalScope::parse_encoded_bytes(Span<const u8> encoded_bytes)
{
    TermObjectEnumerator enumerator(*this, encoded_bytes);
    enumerator.enumerate();
}

UNMAP_AFTER_INIT GlobalScope::GlobalScope(const Vector<PhysicalAddress>& aml_tables_addresses)
{
    VERIFY(aml_tables_addresses.size() >= 1);
    for (auto& aml_table_address : aml_tables_addresses) {
        VERIFY(!aml_table_address.is_null());
        size_t aml_blob_length = 0;
        {
            auto aml_sdt = Memory::map_typed<Structures::SDTHeader>(aml_table_address);
            VERIFY(aml_sdt->length > sizeof(Structures::SDTHeader));
            aml_blob_length = aml_sdt->length - sizeof(Structures::SDTHeader);
        }
        auto aml_blob = Memory::map_typed<Structures::AMLTable>(aml_table_address, aml_blob_length);
        dbgln_if(ACPI_AML_DEBUG, "Parsing AML @ {}, Length {}", aml_tables_addresses[0], aml_blob_length);
        parse_encoded_bytes({ aml_blob->aml_code, aml_blob_length });
    }
}

}
