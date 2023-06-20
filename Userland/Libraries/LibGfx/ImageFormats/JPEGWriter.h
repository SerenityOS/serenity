/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGfx/Forward.h>

namespace Gfx {

class JPEGWriter {
public:
    static ErrorOr<void> encode(Stream&, Bitmap const&);

private:
    JPEGWriter() = delete;
};

}
