/*
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
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
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/MimeSniff/MimeType.h>

namespace Web::Fetch::Infrastructure {

// https://fetch.spec.whatwg.org/#concept-header
// A header is a tuple that consists of a name (a header name) and value (a header value).
struct Header {
    ByteBuffer name;
    ByteBuffer value;

    static ErrorOr<Header> from_string_pair(StringView, StringView);
};

// https://fetch.spec.whatwg.org/#concept-header-list
// A header list is a list of zero or more headers. It is initially the empty list.
class HeaderList final
    : public JS::Cell
    , Vector<Header> {
    JS_CELL(HeaderList, JS::Cell);

public:
    using Vector::begin;
    using Vector::clear;
    using Vector::end;
    using Vector::is_empty;

    [[nodiscard]] static JS::NonnullGCPtr<HeaderList> create(JS::VM&);

    [[nodiscard]] bool contains(ReadonlyBytes) const;
    [[nodiscard]] ErrorOr<Optional<ByteBuffer>> get(ReadonlyBytes) const;
    [[nodiscard]] ErrorOr<Optional<Vector<String>>> get_decode_and_split(ReadonlyBytes) const;
    [[nodiscard]] ErrorOr<void> append(Header);
    void delete_(ReadonlyBytes name);
    [[nodiscard]] ErrorOr<void> set(Header);
    [[nodiscard]] ErrorOr<void> combine(Header);
    [[nodiscard]] ErrorOr<Vector<Header>> sort_and_combine() const;

    struct ExtractLengthFailure {
    };

    using ExtractLengthResult = Variant<u64, ExtractLengthFailure, Empty>;

    [[nodiscard]] ErrorOr<ExtractLengthResult> extract_length() const;

    [[nodiscard]] ErrorOr<Optional<MimeSniff::MimeType>> extract_mime_type() const;

    ErrorOr<Vector<ByteBuffer>> unique_names() const;
};

struct RangeHeaderValue {
    Optional<u64> start;
    Optional<u64> end;
};

struct ExtractHeaderParseFailure {
};

[[nodiscard]] StringView legacy_extract_an_encoding(Optional<MimeSniff::MimeType> const& mime_type, StringView fallback_encoding);
[[nodiscard]] ErrorOr<Optional<Vector<String>>> get_decode_and_split_header_value(ReadonlyBytes);
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
[[nodiscard]] ErrorOr<bool> is_forbidden_request_header(Header const&);
[[nodiscard]] bool is_forbidden_response_header_name(ReadonlyBytes);
[[nodiscard]] bool is_request_body_header_name(ReadonlyBytes);
[[nodiscard]] ErrorOr<Optional<Vector<ByteBuffer>>> extract_header_values(Header const&);
[[nodiscard]] ErrorOr<Variant<Vector<ByteBuffer>, ExtractHeaderParseFailure, Empty>> extract_header_list_values(ReadonlyBytes, HeaderList const&);
[[nodiscard]] Optional<RangeHeaderValue> parse_single_range_header_value(ReadonlyBytes);
[[nodiscard]] ErrorOr<ByteBuffer> default_user_agent_value();

}
