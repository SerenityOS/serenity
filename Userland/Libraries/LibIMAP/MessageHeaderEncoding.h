/*
 * Copyright (c) 2023, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>

namespace IMAP {

ErrorOr<ByteBuffer> decode_rfc2047_encoded_words(StringView input);

}
