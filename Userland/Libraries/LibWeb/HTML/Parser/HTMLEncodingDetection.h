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

bool prescan_should_abort(ByteBuffer const& input, size_t const& position);
bool prescan_is_whitespace_or_slash(u8 const& byte);
bool prescan_skip_whitespace_and_slashes(ByteBuffer const& input, size_t& position);
Optional<String> extract_character_encoding_from_meta_element(String const&);
Optional<Attribute> prescan_get_attribute(ByteBuffer const& input, size_t& position);
Optional<String> run_prescan_byte_stream_algorithm(ByteBuffer const& input);
String run_encoding_sniffing_algorithm(ByteBuffer const& input);

}
