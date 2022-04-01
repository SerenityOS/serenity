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

bool prescan_should_abort(ByteBuffer const& input, size_t const& position);
bool prescan_is_whitespace_or_slash(u8 const& byte);
bool prescan_skip_whitespace_and_slashes(ByteBuffer const& input, size_t& position);
Optional<StringView> extract_character_encoding_from_meta_element(String const&);
RefPtr<DOM::Attribute> prescan_get_attribute(DOM::Document&, ByteBuffer const& input, size_t& position);
Optional<String> run_prescan_byte_stream_algorithm(DOM::Document&, ByteBuffer const& input);
String run_encoding_sniffing_algorithm(DOM::Document&, ByteBuffer const& input);

}
