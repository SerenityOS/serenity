/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Stream.h>
#include <LibGfx/Bitmap.h>

namespace Gfx::CCITT {

struct Group4EncodingOptions {
    enum class AppendEOFB : u8 {
        No,
        Yes,
    };

    AppendEOFB append_eofb { AppendEOFB::Yes };
};

class Group4Encoder {
public:
    static ErrorOr<void> encode(Stream&, Bitmap const&, Group4EncodingOptions const& = {});

private:
    ~Group4Encoder() = delete;
};

}
