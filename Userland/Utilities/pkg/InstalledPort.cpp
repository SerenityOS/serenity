/*
 * Copyright (c) 2023-2024, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InstalledPort.h"

Optional<InstalledPort::Type> InstalledPort::type_from_string(StringView type)
{
    if (type == "auto"sv)
        return Type::Auto;
    if (type == "manual"sv)
        return Type::Manual;
    return {};
}
