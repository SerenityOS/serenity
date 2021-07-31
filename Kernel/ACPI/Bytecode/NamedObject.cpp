/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/Bytecode/NamedObject.h>

namespace Kernel::ACPI {

NamedObject::NamedObject(Span<const u8> encoded_name_string)
    : m_name_string(NameString::try_to_create(encoded_name_string))
{
}

NamedObject::NamedObject(const NameString& preloaded_name_string)
    : m_name_string(preloaded_name_string)
{
}

};
