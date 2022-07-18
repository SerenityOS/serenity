/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/Forward.h>
#include <AK/HashTable.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace Web::Fetch::Infrastructure {

// https://fetch.spec.whatwg.org/#concept-header
// A header is a tuple that consists of a name (a header name) and value (a header value).
struct Header {
    ByteBuffer name;
    ByteBuffer value;
};

// https://fetch.spec.whatwg.org/#concept-header-list
// A header list is a list of zero or more headers. It is initially the empty list.
class HeaderList final : Vector<Header> {
public:
    using Vector::begin;
    using Vector::end;

    [[nodiscard]] bool contains(ReadonlyBytes) const;
    [[nodiscard]] ErrorOr<Optional<ByteBuffer>> get(ReadonlyBytes) const;
    [[nodiscard]] ErrorOr<Optional<Vector<String>>> get_decode_and_split(ReadonlyBytes) const;
    [[nodiscard]] ErrorOr<void> append(Header);
    void delete_(ReadonlyBytes name);
    [[nodiscard]] ErrorOr<void> set(Header);
    [[nodiscard]] ErrorOr<void> combine(Header);
};

[[nodiscard]] ErrorOr<OrderedHashTable<ByteBuffer>> convert_header_names_to_a_sorted_lowercase_set(Span<ReadonlyBytes>);
[[nodiscard]] bool is_header_name(ReadonlyBytes);
[[nodiscard]] bool is_header_value(ReadonlyBytes);
[[nodiscard]] ErrorOr<ByteBuffer> normalize_header_value(ReadonlyBytes);
[[nodiscard]] bool is_cors_safelisted_request_header(Header const&);
[[nodiscard]] bool is_cors_unsafe_request_header_byte(u8);
[[nodiscard]] ErrorOr<OrderedHashTable<ByteBuffer>> get_cors_unsafe_header_names(HeaderList const&);
[[nodiscard]] bool is_cors_non_wildcard_request_header_name(ReadonlyBytes);
[[nodiscard]] bool is_privileged_no_cors_request_header_name(ReadonlyBytes);
[[nodiscard]] bool is_cors_safelisted_response_header_name(ReadonlyBytes, Span<ReadonlyBytes>);
[[nodiscard]] bool is_no_cors_safelisted_request_header_name(ReadonlyBytes);
[[nodiscard]] bool is_no_cors_safelisted_request_header(Header const&);
[[nodiscard]] bool is_forbidden_header_name(ReadonlyBytes);
[[nodiscard]] bool is_forbidden_response_header_name(ReadonlyBytes);
[[nodiscard]] bool is_request_body_header_name(ReadonlyBytes);
[[nodiscard]] bool is_simple_range_header_value(ReadonlyBytes);

}
