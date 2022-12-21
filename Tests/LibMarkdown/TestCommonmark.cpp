/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <LibCore/Stream.h>
#include <LibMarkdown/Document.h>
#include <LibTest/TestCase.h>
#include <LibTest/TestSuite.h>

TEST_SETUP
{
    auto file_or_error = Core::Stream::File::open("/home/anon/Tests/commonmark.spec.json"sv, Core::Stream::OpenMode::Read);
    if (file_or_error.is_error())
        file_or_error = Core::Stream::File::open("./commonmark.spec.json"sv, Core::Stream::OpenMode::Read);
    VERIFY(!file_or_error.is_error());
    auto file = file_or_error.release_value();
    auto file_size = MUST(file->size());
    auto content = MUST(ByteBuffer::create_uninitialized(file_size));
    MUST(file->read_entire_buffer(content.bytes()));
    DeprecatedString test_data { content.bytes() };

    auto tests = JsonParser(test_data).parse().value().as_array();
    for (size_t i = 0; i < tests.size(); ++i) {
        auto testcase = tests[i].as_object();

        auto name = DeprecatedString::formatted("{}_ex{}_{}..{}",
            testcase.get_deprecated("section"sv),
            testcase.get_deprecated("example"sv),
            testcase.get_deprecated("start_line"sv),
            testcase.get_deprecated("end_line"sv));

        DeprecatedString markdown = testcase.get_deprecated("markdown"sv).as_string();
        DeprecatedString html = testcase.get_deprecated("html"sv).as_string();

        Test::TestSuite::the().add_case(adopt_ref(*new Test::TestCase(
            name, [markdown, html]() {
                auto document = Markdown::Document::parse(markdown);
                EXPECT_EQ(document->render_to_inline_html(), html);
            },
            false)));
    }
}
