/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibWeb/DOM/Attribute.h>

namespace Web::HTML {

bool prescan_should_abort(const ByteBuffer& input, const size_t& position);
bool prescan_is_whitespace_or_slash(const u8& byte);
bool prescan_skip_whitespace_and_slashes(const ByteBuffer& input, size_t& position);
Optional<Attribute> prescan_get_attribute(const ByteBuffer& input, size_t& position);
Optional<String> run_prescan_byte_stream_algorithm(const ByteBuffer& input);
String run_encoding_sniffing_algorithm(const ByteBuffer& input);

}
