/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibXML/Parser/Parser.h>

extern "C" int LLVMFuzzerTestOneInput(u8 const* data, size_t size)
{
    XML::Parser parser({ data, size });
    (void)parser.parse();
    return 0;
}
