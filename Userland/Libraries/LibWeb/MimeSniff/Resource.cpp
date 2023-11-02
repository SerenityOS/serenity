/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Resource.h"
#include <LibWeb/Fetch/Infrastructure/URL.h>

namespace {

using namespace Web::MimeSniff;
using Byte = u8;

class BytePatternTableRow {
public:
    BytePatternTableRow(ReadonlyBytes byte_pattern, ReadonlyBytes pattern_mask, Vector<Byte> const& ignored_leading_bytes, StringView mime_type, bool is_tag_terminated = false)
        : m_byte_pattern(byte_pattern)
        , m_pattern_mask(pattern_mask)
        , m_ignored_leading_bytes(ignored_leading_bytes)
        , m_mime_type(mime_type)
        , m_is_tag_terminated(is_tag_terminated)
    {
    }

    ~BytePatternTableRow() = default;

    ReadonlyBytes byte_pattern() const { return m_byte_pattern; }
    ReadonlyBytes pattern_mask() const { return m_pattern_mask; }
    Vector<Byte> const& ignored_leading_bytes() const { return m_ignored_leading_bytes; }
    StringView mime_type() const { return m_mime_type; }
    bool is_tag_terminated() const { return m_is_tag_terminated; }

private:
    ReadonlyBytes m_byte_pattern;
    ReadonlyBytes m_pattern_mask;
    Vector<Byte> const& m_ignored_leading_bytes;
    StringView m_mime_type;

    // NOTE: If the byte pattern has a tag-terminating byte, add a byte where this byte should be. The value itself is ignored in
    //       the pattern_matching_algorithm() (see the NOTE in this algorithm for more details).
    bool m_is_tag_terminated { false };
};

// https://mimesniff.spec.whatwg.org/#tag-terminating-byte
bool is_tag_terminating_byte(Byte byte)
{
    // A tag-terminating byte (abbreviated 0xTT) is any one of the following bytes: 0x20 (SP), 0x3E (">").
    return byte == 0x20 || byte == 0x3E;
}

// https://mimesniff.spec.whatwg.org/#binary-data-byte
bool is_binary_data_byte(Byte byte)
{
    //  A binary data byte is a byte in the range 0x00 to 0x08 (NUL to BS), the byte 0x0B (VT), a byte in
    //  the range 0x0E to 0x1A (SO to SUB), or a byte in the range 0x1C to 0x1F (FS to US).
    return (byte <= 0x08) || byte == 0x0B || (byte >= 0x0E && byte <= 0x1A) || (byte >= 0x1C && byte <= 0x1F);
}

// https://mimesniff.spec.whatwg.org/#pattern-matching-algorithm
bool pattern_matching_algorithm(ReadonlyBytes input, ReadonlyBytes pattern, ReadonlyBytes mask, Vector<Byte> const& ignored, bool is_tag_terminated = false)
{
    // 1. Assert: pattern’s length is equal to mask’s length.
    VERIFY(pattern.size() == mask.size());

    // 2. If input’s length is less than pattern’s length, return false.
    if (input.size() < pattern.size())
        return false;

    // 3. Let s be 0.
    size_t s = 0;

    // 4. While s < input’s length:
    while (s < input.size()) {
        // 1. If ignored does not contain input[s], break.
        if (!ignored.contains_slow(input[s]))
            break;

        // 2. Set s to s + 1.
        s++;
    }

    // 5. Let p be 0.
    size_t p = 0;

    // 6. While p < pattern’s length:
    while (p < pattern.size()) {
        // 1. Let maskedData be the result of applying the bitwise AND operator to input[s] and mask[p].
        Byte masked_data = input[s] & mask[p];

        // NOTE: This non-standard branch exists to avoid having to create 2 byte patterns just so that
        //       they can only differ by their tag-terminating byte (which could be a 0x20 or 0x3E byte).
        if (is_tag_terminated && p + 1 == pattern.size())
            return is_tag_terminating_byte(masked_data);

        // 2. If maskedData is not equal to pattern[p], return false.
        if (masked_data != pattern[p])
            return false;

        // 3. Set s to s + 1.
        s++;

        // 4. Set p to p + 1.
        p++;
    }

    // 7. Return true.
    return true;
}

Vector<Byte> const no_ignored_bytes;

// https://mimesniff.spec.whatwg.org/#matching-an-image-type-pattern
ErrorOr<Optional<MimeType>> match_an_image_type_pattern(ReadonlyBytes input)
{
    // 1. Execute the following steps for each row row in the following table:
    static Array<BytePatternTableRow, 8> pattern_table {
        // A Windows Icon signature.
        BytePatternTableRow { "\x00\x00\x01\x00"sv.bytes(), "\xFF\xFF\xFF\xFF"sv.bytes(), no_ignored_bytes, "image/x-icon"sv },

        // A Windows Cursor signature.
        BytePatternTableRow { "\x00\x00\x02\x00"sv.bytes(), "\xFF\xFF\xFF\xFF"sv.bytes(), no_ignored_bytes, "image/x-icon"sv },

        // The string "BM", a BMP signature.
        BytePatternTableRow { "\x42\x4D"sv.bytes(), "\xFF\xFF"sv.bytes(), no_ignored_bytes, "image/bmp"sv },

        // The string "GIF87a", a GIF signature.
        BytePatternTableRow { "\x47\x49\x46\x38\x37\x61"sv.bytes(), "\xFF\xFF\xFF\xFF\xFF\xFF"sv.bytes(), no_ignored_bytes, "image/gif"sv },

        // The string "GIF89a", a GIF signature.
        BytePatternTableRow { "\x47\x49\x46\x38\x39\x61"sv.bytes(), "\xFF\xFF\xFF\xFF\xFF\xFF"sv.bytes(), no_ignored_bytes, "image/gif"sv },

        // The string "RIFF" followed by four bytes followed by the string "WEBPVP".
        BytePatternTableRow { "\x52\x49\x46\x46\x00\x00\x00\x00\x57\x45\x42\x50\x56\x50"sv.bytes(),
            "\xFF\xFF\xFF\xFF\x00\x00\x00\x00\xFF\xFF\xFF\xFF\xFF\xFF"sv.bytes(), no_ignored_bytes, "image/webp"sv },

        // An error-checking byte followed by the string "PNG" followed by CR LF SUB LF, the PNG signature.
        BytePatternTableRow { "\x89\x50\x4E\x47\x0D\x0A\x1A\x0A"sv.bytes(), "\xFF\xFF\xFF\xFF\xFF\xFF\xFF\xFF"sv.bytes(), no_ignored_bytes, "image/png"sv },

        // The JPEG Start of Image marker followed by the indicator byte of another marker.
        BytePatternTableRow { "\xFF\xD8\xFF"sv.bytes(), "\xFF\xFF\xFF"sv.bytes(), no_ignored_bytes, "image/jpeg"sv },
    };

    for (auto const& row : pattern_table) {
        // 1. Let patternMatched be the result of the pattern matching algorithm given input, the value in
        //    the first column of row, the value in the second column of row, and the value in the third
        //    column of row.
        auto pattern_matched = pattern_matching_algorithm(input, row.byte_pattern(), row.pattern_mask(), row.ignored_leading_bytes());

        // 2. If patternMatched is true, return the value in the fourth column of row.
        if (pattern_matched)
            return MimeType::parse(row.mime_type());
    }

    // 2. Return undefined.
    return OptionalNone {};
}

// https://mimesniff.spec.whatwg.org/#rules-for-identifying-an-unknown-mime-type
ErrorOr<MimeType> rules_for_identifying_an_unknown_mime_type(Resource const& resource, bool sniff_scriptable = false)
{
    // 1. If the sniff-scriptable flag is set, execute the following steps for each row row in the following table:
    if (sniff_scriptable) {
        static auto text_html_mime_type = "text/html"sv;

        // https://mimesniff.spec.whatwg.org/#whitespace-byte
        // A whitespace byte (abbreviated 0xWS) is any one of the following bytes: 0x09 (HT), 0x0A (LF), 0x0C (FF), 0x0D (CR), 0x20 (SP).
        static Vector<Byte> const ignored_whitespace_bytes { 0x09, 0x0A, 0x0C, 0x0D, 0x20 };
        static Array<BytePatternTableRow, 19> pattern_table {
            // The case-insensitive string "<!DOCTYPE HTML" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x21\x44\x4F\x43\x54\x59\x50\x45\x20\x48\x54\x4D\x4C\x00"sv.bytes(),
                "\xFF\xFF\xDF\xDF\xDF\xDF\xDF\xDF\xDF\xFF\xDF\xDF\xDF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<HTML" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x48\x54\x4D\x4C\x00"sv.bytes(), "\xFF\xDF\xDF\xDF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<HEAD" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x48\x45\x41\x44\x00"sv.bytes(), "\xFF\xDF\xDF\xDF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<SCRIPT" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x53\x43\x52\x49\x50\x54\x00"sv.bytes(),
                "\xFF\xDF\xDF\xDF\xDF\xDF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<IFRAME" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x49\x46\x52\x41\x4D\x45\x00"sv.bytes(),
                "\xFF\xDF\xDF\xDF\xDF\xDF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<H1" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x48\x31\x00"sv.bytes(), "\xFF\xDF\xFF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<DIV" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x44\x49\x56\x00"sv.bytes(), "\xFF\xDF\xDF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<FONT" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x46\x4F\x4E\x54\x00"sv.bytes(), "\xFF\xDF\xDF\xDF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<TABLE" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x54\x41\x42\x4C\x45\x00"sv.bytes(), "\xFF\xDF\xDF\xDF\xDF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<A" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x41\x00"sv.bytes(), "\xFF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<STYLE" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x53\x54\x59\x4C\x45\x00"sv.bytes(),
                "\xFF\xDF\xDF\xDF\xDF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<TITLE" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x54\x49\x54\x4C\x45\x00"sv.bytes(),
                "\xFF\xDF\xDF\xDF\xDF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<B" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x42\x00"sv.bytes(), "\xFF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<BODY" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x42\x4F\x44\x59\x00"sv.bytes(), "\xFF\xDF\xDF\xDF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<BR" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x42\x52\x00"sv.bytes(), "\xFF\xDF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The case-insensitive string "<P" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x50\x00"sv.bytes(), "\xFF\xDF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The string "<!--" followed by a tag-terminating byte.
            BytePatternTableRow { "\x3C\x21\x2D\x2D\x00"sv.bytes(), "\xFF\xFF\xFF\xFF\xFF"sv.bytes(), ignored_whitespace_bytes, text_html_mime_type, true },

            // The string "<?xml".
            BytePatternTableRow { "\x3C\x3F\x78\x6D\x6C"sv.bytes(), "\xFF\xFF\xFF\xFF\xFF"sv.bytes(), ignored_whitespace_bytes, "text/xml"sv },

            // The string "%PDF-", the PDF signature.
            BytePatternTableRow { "\x25\x50\x44\x46\x2D"sv.bytes(), "\xFF\xFF\xFF\xFF\xFF"sv.bytes(), no_ignored_bytes, "application/pdf"sv },
        };

        for (auto const& row : pattern_table) {
            // 1. Let patternMatched be the result of the pattern matching algorithm given resource’s resource header,
            //    the value in the first column of row, the value in the second column of row, and the value in the
            //    third column of row.
            auto pattern_matched = pattern_matching_algorithm(resource.resource_header(), row.byte_pattern(), row.pattern_mask(), row.ignored_leading_bytes(), row.is_tag_terminated());

            // 2. If patternMatched is true, return the value in the fourth column of row.
            if (pattern_matched) {
                if (auto maybe_type = TRY(MimeType::parse(row.mime_type())); maybe_type.has_value())
                    return maybe_type.release_value();
            }
        }
    }

    // 2. Execute the following steps for each row row in the following table:
    static auto text_plain_mime_type = "text/plain"sv;
    static Array<BytePatternTableRow, 4> pattern_table {
        // The string "%!PS-Adobe-", the PostScript signature.
        BytePatternTableRow { "\x25\x21\x50\x53\x2D\x41\x64\x6F\x62\x65\x2D"sv.bytes(),
            "\xFF\xFF\xFF\xFF\xFF\xFF\xFF\xFF\xFF\xFF\xFF"sv.bytes(), no_ignored_bytes, "application/postscript"sv },

        // UTF-16BE BOM
        BytePatternTableRow { "\xFE\xFF\x00\x00"sv.bytes(), "\xFF\xFF\x00\x00"sv.bytes(), no_ignored_bytes, text_plain_mime_type },

        // UTF-16LE BOM
        BytePatternTableRow { "\xFF\xFE\x00\x00"sv.bytes(), "\xFF\xFF\x00\x00"sv.bytes(), no_ignored_bytes, text_plain_mime_type },

        // UTF-8 BOM
        BytePatternTableRow { "\xEF\xBB\xBF\x00"sv.bytes(), "\xFF\xFF\xFF\x00"sv.bytes(), no_ignored_bytes, text_plain_mime_type },
    };

    for (auto const& row : pattern_table) {
        // 1. Let patternMatched be the result of the pattern matching algorithm given resource’s resource header,
        //    the value in the first column of row, the value in the second column of row, and the value in the
        //    third column of row.
        auto pattern_matched = pattern_matching_algorithm(resource.resource_header(), row.byte_pattern(), row.pattern_mask(), row.ignored_leading_bytes());

        // 2. If patternMatched is true, return the value in the fourth column of row.
        if (pattern_matched) {
            if (auto maybe_type = TRY(MimeType::parse(row.mime_type())); maybe_type.has_value())
                return maybe_type.release_value();
        }
    }

    // 3. Let matchedType be the result of executing the image type pattern matching algorithm given resource’s resource header.
    auto matched_type = TRY(match_an_image_type_pattern(resource.resource_header()));

    // 4. If matchedType is not undefined, return matchedType.
    if (matched_type.has_value())
        return matched_type.release_value();

    // FIXME: 5. Set matchedType to the result of executing the audio or video type pattern matching algorithm given resource’s resource header.

    // 6. If matchedType is not undefined, return matchedType.
    if (matched_type.has_value())
        return matched_type.release_value();

    // FIXME: 7. Set matchedType to the result of executing the archive type pattern matching algorithm given resource’s resource header.

    // 8. If matchedType is not undefined, return matchedType.
    if (matched_type.has_value())
        return matched_type.release_value();

    // 9. If resource’s resource header contains no binary data bytes, return "text/plain".
    if (!any_of(resource.resource_header(), is_binary_data_byte))
        return MimeType::create("text"_string, "plain"_string);

    // 10. Return "application/octet-stream".
    return MimeType::create("application"_string, "octet-stream"_string);
}

}

namespace Web::MimeSniff {

ErrorOr<Resource> Resource::create(ReadonlyBytes data, StringView scheme, Optional<MimeType> supplied_type, bool no_sniff)
{
    // NOTE: Non-standard but for cases where pattern matching fails, let's fall back to the safest MIME type.
    auto default_computed_mime_type = TRY(MimeType::create("application"_string, "octet-stream"_string));
    auto resource = Resource { data, no_sniff, move(default_computed_mime_type) };
    TRY(resource.supplied_mime_type_detection_algorithm(scheme, move(supplied_type)));
    TRY(resource.mime_type_sniffing_algorithm());

    return resource;
}

Resource::Resource(ReadonlyBytes data, bool no_sniff, MimeType&& default_computed_mime_type)
    : m_no_sniff(no_sniff)
    , m_computed_mime_type(move(default_computed_mime_type))
{
    read_the_resource_header(data);
}

Resource::Resource(Resource&&) = default;
Resource& Resource::operator=(Resource&&) = default;

Resource::~Resource() = default;

// https://mimesniff.spec.whatwg.org/#supplied-mime-type-detection-algorithm
// NOTE: Parameters are non-standard.
ErrorOr<void> Resource::supplied_mime_type_detection_algorithm(StringView scheme, Optional<MimeType> supplied_type)
{
    // 1. Let supplied-type be null.
    // 2. If the resource is retrieved via HTTP, execute the following steps:
    //        1. If one or more Content-Type headers are associated with the resource, execute the following steps:
    //               1. Set supplied-type to the value of the last Content-Type header associated with the resource.
    //               2. Set the check-for-apache-bug flag if supplied-type is exactly equal to one of the values in the following table:
    // NOTE: Non-standard but this algorithm expects the caller to handle step 2.1.1.
    if (supplied_type.has_value()) {
        if (Fetch::Infrastructure::is_http_or_https_scheme(scheme)) {
            static Array<StringView, 4> apache_bug_mime_types = {
                "text/plain"sv,
                "text/plain; charset=ISO-8859-1"sv,
                "text/plain; charset=iso-8859-1"sv,
                "text/plain; charset=UTF-8"sv
            };

            for (auto apache_bug_mime_type : apache_bug_mime_types) {
                if (TRY(supplied_type->serialized()) == apache_bug_mime_type)
                    m_check_for_apache_bug_flag = true;
            }
        }
    }

    // 3. If the resource is retrieved directly from the file system, set supplied-type
    //    to the MIME type provided by the file system.
    // 4. If the resource is retrieved via another protocol (such as FTP), set
    //    supplied-type to the MIME type as determined by that protocol, if any.
    // 5. If supplied-type is not a MIME type, the supplied MIME type is undefined.
    //    Abort these steps.
    // 6. The supplied MIME type is supplied-type.
    // NOTE: The expectation is for the caller to handle these spec steps.
    m_supplied_mime_type = supplied_type;

    return {};
}

// https://mimesniff.spec.whatwg.org/#read-the-resource-header
void Resource::read_the_resource_header(ReadonlyBytes data)
{
    // 1. Let buffer be a byte sequence.
    ByteBuffer buffer;

    // 2. Read bytes of the resource into buffer until one of the following conditions is met:
    //      - the end of the resource is reached.
    //      - the number of bytes in buffer is greater than or equal to 1445.
    //      FIXME: - a reasonable amount of time has elapsed, as determined by the user agent.
    for (auto byte : data) {
        buffer.append(byte);

        if (buffer.size() >= 1445)
            break;
    }

    // 3. The resource header is buffer.
    m_resource_header = move(buffer);
}

// https://mimesniff.spec.whatwg.org/#mime-type-sniffing-algorithm
ErrorOr<void> Resource::mime_type_sniffing_algorithm()
{
    // 1. If the supplied MIME type is undefined or if the supplied MIME type’s essence
    //    is "unknown/unknown", "application/unknown", or "*/*", execute the rules for
    //    identifying an unknown MIME type with the sniff-scriptable flag equal to the
    //    inverse of the no-sniff flag and abort these steps.
    if (!m_supplied_mime_type.has_value() || m_supplied_mime_type->essence().is_one_of("unknown/unknown", "application/unknown", "*/*")) {
        m_computed_mime_type = TRY(rules_for_identifying_an_unknown_mime_type(*this, !m_no_sniff));
        return {};
    }

    // 2. If the no-sniff flag is set, the computed MIME type is the supplied MIME type.
    //    Abort these steps.
    if (m_no_sniff) {
        m_computed_mime_type = m_supplied_mime_type.value();
        return {};
    }

    // 3. If the check-for-apache-bug flag is set, execute the rules for distinguishing
    //    if a resource is text or binary and abort these steps.
    if (m_check_for_apache_bug_flag) {
        // FIXME: Execute the rules for distinguishing if a resource is text or binary and abort these steps.
        return {};
    }

    // 4. If the supplied MIME type is an XML MIME type, the computed MIME type is the supplied MIME type.
    //    Abort these steps.
    if (m_supplied_mime_type->is_xml()) {
        m_computed_mime_type = m_supplied_mime_type.value();
        return {};
    }

    // 5. If the supplied MIME type’s essence is "text/html", execute the rules for distinguishing if a
    //    resource is a feed or HTML and abort these steps.
    if (m_supplied_mime_type->essence() == "text/html") {
        // FIXME: Execute the rules for distinguishing if a resource is a feed or HTML and abort these steps.
        return {};
    }

    // FIXME: 6. If the supplied MIME type is an image MIME type supported by the user agent, let matched-type be
    //    the result of executing the image type pattern matching algorithm with the resource header as
    //    the byte sequence to be matched.
    Optional<MimeType> matched_type;

    // 7. If matched-type is not undefined, the computed MIME type is matched-type.
    //    Abort these steps.
    if (matched_type.has_value()) {
        m_computed_mime_type = matched_type.release_value();
        return {};
    }

    // FIXME: 8. If the supplied MIME type is an audio or video MIME type supported by the user agent, let matched-type be
    //    the result of executing the audio or video type pattern matching algorithm with the resource header as
    //    the byte sequence to be matched.

    // 9. If matched-type is not undefined, the computed MIME type is matched-type.
    //    Abort these steps.
    if (matched_type.has_value()) {
        m_computed_mime_type = matched_type.release_value();
        return {};
    }

    // 10. The computed MIME type is the supplied MIME type.
    m_computed_mime_type = m_supplied_mime_type.value();

    return {};
}

}
