/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <LibCore/File.h>
#include <LibMarkdown/Document.h>
#include <LibTest/TestCase.h>
#include <LibTest/TestSuite.h>

TEST_SETUP
{
    auto file_or_error = Core::File::open("/home/anon/Tests/commonmark.spec.json"sv, Core::File::OpenMode::Read);
    if (file_or_error.is_error())
        file_or_error = Core::File::open("./commonmark.spec.json"sv, Core::File::OpenMode::Read);
    VERIFY(!file_or_error.is_error());
    auto file = file_or_error.release_value();
    auto file_size = MUST(file->size());
    auto content = MUST(ByteBuffer::create_uninitialized(file_size));
    MUST(file->read_until_filled(content.bytes()));
    ByteString test_data { content.bytes() };

    auto tests = JsonParser(test_data).parse().value().as_array();
    for (size_t i = 0; i < tests.size(); ++i) {
        auto testcase = tests[i].as_object();

        auto name = ByteString::formatted("{}_ex{}_{}..{}",
            testcase.get("section"sv).value(),
            testcase.get("example"sv).value(),
            testcase.get("start_line"sv).value(),
            testcase.get("end_line"sv).value());

        ByteString markdown = testcase.get_byte_string("markdown"sv).value();
        ByteString html = testcase.get_byte_string("html"sv).value();

        Test::TestSuite::the().add_case(adopt_ref(*new Test::TestCase(
            name, [markdown, html]() {
                auto document = Markdown::Document::parse(markdown);
                EXPECT_EQ(document->render_to_inline_html(), html);
            },
            false)));
    }
}
