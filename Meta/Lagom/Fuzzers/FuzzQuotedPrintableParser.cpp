/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringView.h>
#include <LibIMAP/QuotedPrintable.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    auto quoted_printable_string = StringView(static_cast<unsigned char const*>(data), size);
    IMAP::decode_quoted_printable(quoted_printable_string);
    return 0;
}
