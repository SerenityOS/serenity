/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

bool prescan_should_abort(const ByteBuffer& input, const size_t& position);
bool prescan_is_whitespace_or_slash(const u8& byte);
bool prescan_skip_whitespace_and_slashes(const ByteBuffer& input, size_t& position);
Optional<String> extract_character_encoding_from_meta_element(String const&);
RefPtr<DOM::Attribute> prescan_get_attribute(DOM::Document&, const ByteBuffer& input, size_t& position);
Optional<String> run_prescan_byte_stream_algorithm(DOM::Document&, const ByteBuffer& input);
String run_encoding_sniffing_algorithm(DOM::Document&, const ByteBuffer& input);

}
