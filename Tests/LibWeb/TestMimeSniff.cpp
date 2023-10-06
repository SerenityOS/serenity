/*
 * Copyright (c) 2023, Kemal Zebari <kemalzebra@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibWeb/MimeSniff/Resource.h>

TEST_CASE(determine_computed_mime_type_given_no_sniff_is_set)
{
    auto mime_type = MUST(Web::MimeSniff::MimeType::create("text"_string, "html"_string));
    auto computed_mime_type = MUST(Web::MimeSniff::Resource::sniff("\x00"sv.bytes(), Web::MimeSniff::SniffingConfiguration { .supplied_type = mime_type, .no_sniff = true }));

    EXPECT_EQ("text/html"sv, MUST(computed_mime_type.serialized()));
}

TEST_CASE(determine_computed_mime_type_given_no_sniff_is_unset)
{
    auto supplied_type = MUST(Web::MimeSniff::MimeType::create("text"_string, "html"_string));
    auto computed_mime_type = MUST(Web::MimeSniff::Resource::sniff("\x00"sv.bytes(), Web::MimeSniff::SniffingConfiguration { .supplied_type = supplied_type }));

    EXPECT_EQ("application/octet-stream"sv, MUST(computed_mime_type.serialized()));

    // Make sure we cover the XML code path in the mime type sniffing algorithm.
    auto xml_mime_type = "application/rss+xml"sv;
    supplied_type = MUST(Web::MimeSniff::MimeType::parse(xml_mime_type)).release_value();
    computed_mime_type = MUST(Web::MimeSniff::Resource::sniff("\x00"sv.bytes(), Web::MimeSniff::SniffingConfiguration { .supplied_type = supplied_type }));

    EXPECT_EQ(xml_mime_type, MUST(computed_mime_type.serialized()));
}
