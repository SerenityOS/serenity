/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/ByteBuffer.h>
#include <YAK/String.h>
#include <YAK/StringView.h>

namespace YAK {

size_t calculate_base64_decoded_length(const StringView&);

size_t calculate_base64_encoded_length(ReadonlyBytes);

ByteBuffer decode_base64(const StringView&);

String encode_base64(ReadonlyBytes);

}

using YAK::decode_base64;
using YAK::encode_base64;
