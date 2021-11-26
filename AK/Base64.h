/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>

namespace AK {

size_t calculate_base64_decoded_length(StringView);

size_t calculate_base64_encoded_length(ReadonlyBytes);

Optional<ByteBuffer> decode_base64(StringView);

String encode_base64(ReadonlyBytes);

}

using AK::decode_base64;
using AK::encode_base64;
