/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/CommonNames.h>

namespace PDF {

#define ENUMERATE(name) ByteString CommonNames::name = #name;
ENUMERATE_COMMON_NAMES(ENUMERATE)
#undef ENUMERATE

ByteString CommonNames::IdentityH = "Identity-H";

}
