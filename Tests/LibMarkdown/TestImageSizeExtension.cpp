/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <LibMarkdown/Document.h>
#include <LibTest/TestCase.h>

struct TestCase {
    StringView markdown;
    StringView expected_html;
};

static constexpr Array image_size_tests {
    // No image size:
    TestCase { .markdown = "![](foo.png)"sv, .expected_html = R"(<p><img src="foo.png" alt="" ></p>)"sv },
    // Only width given:
    TestCase { .markdown = "![](foo.png =100x)"sv, .expected_html = R"(<p><img src="foo.png" style="width: 100px;" alt="" ></p>)"sv },
    // Only height given:
    TestCase { .markdown = "![](foo.png =x200)"sv, .expected_html = R"(<p><img src="foo.png" style="height: 200px;" alt="" ></p>)"sv },
    // Both width and height given
    TestCase { .markdown = "![](foo.png =50x25)"sv, .expected_html = R"(<p><img src="foo.png" style="width: 50px;height: 25px;" alt="" ></p>)"sv },
    // Size contains invalid width
    TestCase { .markdown = "![](foo.png =1oox50)"sv, .expected_html = R"(<p><img src="foo.png =1oox50" alt="" ></p>)"sv },
    // Size contains invalid height
    TestCase { .markdown = "![](foo.png =900xfour)"sv, .expected_html = R"(<p><img src="foo.png =900xfour" alt="" ></p>)"sv },
};

TEST_CASE(test_image_size_markdown_extension)
{
    for (auto const& test_case : image_size_tests) {
        auto document = Markdown::Document::parse(test_case.markdown);
        auto raw_rendered_html = document->render_to_inline_html();
        auto rendered_html = StringView(raw_rendered_html).trim_whitespace();
        EXPECT_EQ(rendered_html, test_case.expected_html);
    }
}
