/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include "../CSV.h"
#include "../XSV.h"
#include <AK/ByteBuffer.h>
#include <string.h>

TEST_CASE(should_parse_valid_data)
{
    {
        auto data = R"~~~(Foo, Bar, Baz
                      1, 2, 3
                      4, 5, 6
                      """x", y"z, 9)~~~"sv;
        auto csv = Reader::CSV { data, Reader::default_behaviors() | Reader::ParserBehavior::ReadHeaders | Reader::ParserBehavior::TrimLeadingFieldSpaces };
        csv.parse();
        EXPECT(!csv.has_error());

        EXPECT_EQ(csv[0]["Foo"sv], "1"sv);
        EXPECT_EQ(csv[2]["Foo"sv], "\"x"sv);
        EXPECT_EQ(csv[2]["Bar"sv], "y\"z"sv);
    }

    {
        auto data = R"~~~(Foo, Bar, Baz
                      1     	 , 2, 3
                      4, "5 "       , 6
                      """x", y"z, 9                       )~~~"sv;
        auto csv = Reader::CSV { data, Reader::default_behaviors() | Reader::ParserBehavior::ReadHeaders | Reader::ParserBehavior::TrimLeadingFieldSpaces | Reader::ParserBehavior::TrimTrailingFieldSpaces };
        csv.parse();
        EXPECT(!csv.has_error());

        EXPECT_EQ(csv[0]["Foo"sv], "1"sv);
        EXPECT_EQ(csv[1]["Bar"sv], "5 "sv);
        EXPECT_EQ(csv[2]["Foo"sv], "\"x"sv);
        EXPECT_EQ(csv[2]["Baz"sv], "9"sv);
    }
}

TEST_CASE(should_fail_nicely)
{
    {
        auto data = R"~~~(Foo, Bar, Baz
                      x, y)~~~"sv;
        auto csv = Reader::CSV { data, Reader::default_behaviors() | Reader::ParserBehavior::ReadHeaders | Reader::ParserBehavior::TrimLeadingFieldSpaces };
        csv.parse();
        EXPECT(csv.has_error());
        EXPECT_EQ(csv.error(), Reader::ReadError::NonConformingColumnCount);
    }

    {
        auto data = R"~~~(Foo, Bar, Baz
                      x, y, "z)~~~"sv;
        auto csv = Reader::CSV { data, Reader::default_behaviors() | Reader::ParserBehavior::ReadHeaders | Reader::ParserBehavior::TrimLeadingFieldSpaces };
        csv.parse();
        EXPECT(csv.has_error());
        EXPECT_EQ(csv.error(), Reader::ReadError::QuoteFailure);
    }
}

TEST_CASE(should_iterate_rows)
{
    auto data = R"~~~(Foo, Bar, Baz
                      1, 2, 3
                      4, 5, 6
                      """x", y"z, 9)~~~"sv;
    auto csv = Reader::CSV { data, Reader::default_behaviors() | Reader::ParserBehavior::ReadHeaders | Reader::ParserBehavior::TrimLeadingFieldSpaces };
    csv.parse();
    EXPECT(!csv.has_error());

    bool ran = false;
    for (auto row : csv)
        ran = !row[0].is_empty();

    EXPECT(ran);
}

BENCHMARK_CASE(fairly_big_data)
{
    constexpr auto num_rows = 100000u;
    constexpr auto line = "well,hello,friends,1,2,3,4,5,6,7,8,pizza,guacamole\n"sv;
    auto buf = ByteBuffer::create_uninitialized((line.length() * num_rows) + 1).release_value();
    buf[buf.size() - 1] = '\0';

    for (size_t row = 0; row <= num_rows; ++row) {
        memcpy(buf.offset_pointer(row * line.length()), line.characters_without_null_termination(), line.length());
    }

    auto csv = Reader::CSV { StringView { buf.bytes() }, Reader::default_behaviors() | Reader::ParserBehavior::ReadHeaders };
    csv.parse();

    EXPECT(!csv.has_error());
    EXPECT_EQ(csv.size(), num_rows);
}
