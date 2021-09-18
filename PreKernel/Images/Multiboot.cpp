/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <PreKernel/Images/Multiboot.h>

namespace Multiboot {

StringView parse_entry_type(MemoryEntryType type)
{
    switch (type) {
    case MemoryEntryType::Available:
        return "usable";
    case MemoryEntryType::Reserved:
        return "reserved";
    case MemoryEntryType::ACPIReclaimable:
        return "acpi-reclaimable";
    case MemoryEntryType::ACPINVS:
        return "acpi-nvs";
    case MemoryEntryType::FaultyRAM:
        return "bad memory";
    }
    VERIFY_NOT_REACHED();
}

}
