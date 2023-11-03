/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/StringView.h>
#include <LibIMAP/QuotedPrintable.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);
    auto quoted_printable_string = StringView(static_cast<unsigned char const*>(data), size);
    (void)IMAP::decode_quoted_printable(quoted_printable_string);
    return 0;
}
