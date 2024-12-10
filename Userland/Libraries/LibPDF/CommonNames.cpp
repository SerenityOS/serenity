/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/CommonNames.h>

namespace PDF {

#define ENUMERATE(name) FlyByteString CommonNames::name = #name;
ENUMERATE_COMMON_NAMES(ENUMERATE)
#undef ENUMERATE

FlyByteString CommonNames::IdentityH = "Identity-H";
FlyByteString CommonNames::IdentityV = "Identity-V";

}
