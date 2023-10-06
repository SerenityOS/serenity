/*
 * Copyright (c) 2023, Kemal Zebari <kemalzebra@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Fetch/Infrastructure/URL.h>
#include <LibWeb/MimeSniff/Resource.h>

namespace Web::MimeSniff {

ErrorOr<Resource> Resource::create(ReadonlyBytes data, SniffingConfiguration configuration)
{
    // NOTE: Non-standard but for cases where pattern matching fails, let's fall back to the safest MIME type.
    auto default_computed_mime_type = TRY(MimeType::create("application"_string, "octet-stream"_string));
    auto resource = Resource { data, configuration.no_sniff, move(default_computed_mime_type) };

    TRY(resource.supplied_mime_type_detection_algorithm(configuration.scheme, move(configuration.supplied_type)));
    TRY(resource.mime_type_sniffing_algorithm());

    return resource;
}

ErrorOr<MimeType> Resource::sniff(ReadonlyBytes data, SniffingConfiguration configuration)
{
    auto resource = TRY(create(data, move(configuration)));
    return move(resource.m_computed_mime_type);
}

Resource::Resource(ReadonlyBytes data, bool no_sniff, MimeType&& default_computed_mime_type)
    : m_no_sniff(no_sniff)
    , m_computed_mime_type(move(default_computed_mime_type))
{
    read_the_resource_header(data);
}

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
            static Array<StringView, 4> constexpr apache_bug_mime_types = {
                "text/plain"sv,
                "text/plain; charset=ISO-8859-1"sv,
                "text/plain; charset=iso-8859-1"sv,
                "text/plain; charset=UTF-8"sv
            };

            auto serialized_supplied_type = TRY(supplied_type->serialized());
            for (auto apache_bug_mime_type : apache_bug_mime_types) {
                if (serialized_supplied_type == apache_bug_mime_type) {
                    m_check_for_apache_bug_flag = true;
                    break;
                }
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
    //      - a reasonable amount of time has elapsed, as determined by the user agent.
    // FIXME: The spec expects us to be reading from a stream. Reimplement this spec step once
    //        we have greater support for streaming in areas that calls on this API.
    static size_t constexpr MAX_SNIFF_SIZE = 1445;
    buffer.append(data.slice(0, min(data.size(), MAX_SNIFF_SIZE)));

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

        // FIXME: Execute the rules for identifying an unknown MIME type with the
        // sniff-scriptable flag equal to the inverse of the no-sniff flag and abort
        // these steps.
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
