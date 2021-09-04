/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <AK/StringView.h>

namespace AK {

size_t calculate_base64_decoded_length(StringView const&);

size_t calculate_base64_encoded_length(ReadonlyBytes);

ByteBuffer decode_base64(StringView const&);

String encode_base64(ReadonlyBytes);

}

using AK::decode_base64;
using AK::encode_base64;
