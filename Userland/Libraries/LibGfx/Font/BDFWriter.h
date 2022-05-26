/*
 * Copyright (c) 2022, Marco Rebhan <me@dblsaiko.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BitmapFont.h"
#include <LibCore/Stream.h>

namespace Gfx {

ErrorOr<void> write_bdf(String const& path, BitmapFont const& font);

ErrorOr<void> write_bdf(Core::Stream::Stream& stream, BitmapFont const& font);

}
