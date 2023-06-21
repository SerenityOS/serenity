/*
 * Copyright (c) 2022-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace Web::Infra {

[[nodiscard]] ErrorOr<ByteBuffer> decode_forgiving_base64(StringView);

}
