/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/TestSuite.h>

#include "../CSV.h"
#include "../XSV.h"
#include <LibCore/File.h>

TEST_CASE(should_parse_valid_data)
{
    {
        auto data = R"~~~(Foo, Bar, Baz
                      1, 2, 3
                      4, 5, 6
                      """x", y"z, 9)~~~";
        auto csv = Reader::CSV { data, Reader::default_behaviours() | Reader::ParserBehaviour::ReadHeaders | Reader::ParserBehaviour::TrimLeadingFieldSpaces };
        EXPECT(!csv.has_error());

        EXPECT_EQ(csv[0]["Foo"], "1");
        EXPECT_EQ(csv[2]["Foo"], "\"x");
        EXPECT_EQ(csv[2]["Bar"], "y\"z");
    }

    {
        auto data = R"~~~(Foo, Bar, Baz
                      1     	 , 2, 3
                      4, "5 "       , 6
                      """x", y"z, 9                       )~~~";
        auto csv = Reader::CSV { data, Reader::default_behaviours() | Reader::ParserBehaviour::ReadHeaders | Reader::ParserBehaviour::TrimLeadingFieldSpaces | Reader::ParserBehaviour::TrimTrailingFieldSpaces };
        EXPECT(!csv.has_error());

        EXPECT_EQ(csv[0]["Foo"], "1");
        EXPECT_EQ(csv[1]["Bar"], "5 ");
        EXPECT_EQ(csv[2]["Foo"], "\"x");
        EXPECT_EQ(csv[2]["Baz"], "9");
    }
}

TEST_CASE(should_fail_nicely)
{
    {
        auto data = R"~~~(Foo, Bar, Baz
                      x, y)~~~";
        auto csv = Reader::CSV { data, Reader::default_behaviours() | Reader::ParserBehaviour::ReadHeaders | Reader::ParserBehaviour::TrimLeadingFieldSpaces };
        EXPECT(csv.has_error());
        EXPECT_EQ(csv.error(), Reader::ReadError::NonConformingColumnCount);
    }

    {
        auto data = R"~~~(Foo, Bar, Baz
                      x, y, "z)~~~";
        auto csv = Reader::CSV { data, Reader::default_behaviours() | Reader::ParserBehaviour::ReadHeaders | Reader::ParserBehaviour::TrimLeadingFieldSpaces };
        EXPECT(csv.has_error());
        EXPECT_EQ(csv.error(), Reader::ReadError::QuoteFailure);
    }
}

TEST_CASE(should_iterate_rows)
{
    auto data = R"~~~(Foo, Bar, Baz
                      1, 2, 3
                      4, 5, 6
                      """x", y"z, 9)~~~";
    auto csv = Reader::CSV { data, Reader::default_behaviours() | Reader::ParserBehaviour::ReadHeaders | Reader::ParserBehaviour::TrimLeadingFieldSpaces };
    EXPECT(!csv.has_error());

    bool ran = false;
    for (auto row : csv)
        ran = !row[0].is_empty();

    EXPECT(ran);
}

BENCHMARK_CASE(fairly_big_data)
{
    auto file_or_error = Core::File::open(__FILE__ ".data", Core::IODevice::OpenMode::ReadOnly);
    EXPECT_EQ_FORCE(file_or_error.is_error(), false);

    auto data = file_or_error.value()->read_all();
    auto csv = Reader::CSV { data, Reader::default_behaviours() | Reader::ParserBehaviour::ReadHeaders };

    EXPECT(!csv.has_error());
    EXPECT_EQ(csv.size(), 100000u);
}

TEST_MAIN(XSV)
