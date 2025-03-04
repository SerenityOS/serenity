/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Firmware/ACPI/Definitions.h>
#include <Kernel/Firmware/ACPI/StaticParsing.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Memory/TypedMapping.h>

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Firmware/ACPI.h>
#endif

namespace Kernel::ACPI::StaticParsing {

Optional<PhysicalAddress> find_rsdp()
{
    Optional<PhysicalAddress> rsdp;
    if (!g_boot_info.acpi_rsdp_paddr.is_null())
        rsdp = g_boot_info.acpi_rsdp_paddr;

#if ARCH(X86_64)
    if (!rsdp.has_value() && g_boot_info.boot_method == BootMethod::Multiboot1)
        rsdp = StaticParsing::find_rsdp_in_ia_pc_specific_memory_locations();
#endif

    return rsdp;
}

static bool match_table_signature(PhysicalAddress table_header, StringView signature)
{
    // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
    VERIFY(signature.length() == 4);

    auto table = Memory::map_typed<Structures::RSDT>(table_header).release_value_but_fixme_should_propagate_errors();
    return !strncmp(table->h.sig, signature.characters_without_null_termination(), 4);
}

ErrorOr<Optional<PhysicalAddress>> search_table_in_xsdt(PhysicalAddress xsdt_address, StringView signature)
{
    // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
    VERIFY(signature.length() == 4);

    auto xsdt = TRY(Memory::map_typed<Structures::XSDT>(xsdt_address));

    for (size_t i = 0; i < ((xsdt->h.length - sizeof(Structures::SDTHeader)) / sizeof(u64)); ++i) {
        if (match_table_signature(PhysicalAddress((PhysicalPtr)xsdt->table_ptrs[i]), signature))
            return PhysicalAddress((PhysicalPtr)xsdt->table_ptrs[i]);
    }
    return Optional<PhysicalAddress> {};
}

ErrorOr<Optional<PhysicalAddress>> find_table(PhysicalAddress rsdp_address, StringView signature)
{
    // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
    VERIFY(signature.length() == 4);

    auto rsdp = TRY(Memory::map_typed<Structures::RSDPDescriptor20>(rsdp_address));

    if (rsdp->base.revision == 0)
        return search_table_in_rsdt(PhysicalAddress(rsdp->base.rsdt_ptr), signature);

    if (rsdp->base.revision >= 2) {
        if (rsdp->xsdt_ptr)
            return search_table_in_xsdt(PhysicalAddress(rsdp->xsdt_ptr), signature);
        return search_table_in_rsdt(PhysicalAddress(rsdp->base.rsdt_ptr), signature);
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<Optional<PhysicalAddress>> search_table_in_rsdt(PhysicalAddress rsdt_address, StringView signature)
{
    // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
    VERIFY(signature.length() == 4);

    auto rsdt = TRY(Memory::map_typed<Structures::RSDT>(rsdt_address));

    for (u32 i = 0; i < ((rsdt->h.length - sizeof(Structures::SDTHeader)) / sizeof(u32)); i++) {
        if (match_table_signature(PhysicalAddress((PhysicalPtr)rsdt->table_ptrs[i]), signature))
            return PhysicalAddress((PhysicalPtr)rsdt->table_ptrs[i]);
    }
    return Optional<PhysicalAddress> {};
}

}
