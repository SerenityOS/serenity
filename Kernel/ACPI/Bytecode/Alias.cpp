/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/Bytecode/Alias.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/Sections.h>

namespace Kernel::ACPI {

UNMAP_AFTER_INIT Alias::Alias(Span<const u8> encoded_name_string, Span<const u8> encoded_aliased_name_string)
    : NamedObject(encoded_name_string)
    , m_aliased_name_string(NameString::try_to_create(encoded_aliased_name_string))
{
}

}
