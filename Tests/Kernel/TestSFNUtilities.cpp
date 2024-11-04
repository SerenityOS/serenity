/*
 * Copyright (c) 2024, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <Kernel/FileSystem/FATFS/SFNUtilities.h>
#include <LibTest/TestCase.h>

#include <Kernel/FileSystem/FATFS/SFNUtilities.cpp>

TEST_CASE(test_is_valid_sfn)
{
    EXPECT_EQ(Kernel::SFNUtils::is_valid_sfn("foo.txt"sv), false);
    EXPECT_EQ(Kernel::SFNUtils::is_valid_sfn("FOO.txt"sv), false);
    EXPECT_EQ(Kernel::SFNUtils::is_valid_sfn("Foo.TXT"sv), false);
    EXPECT_EQ(Kernel::SFNUtils::is_valid_sfn("FOO.TXT"sv), true);
    EXPECT_EQ(Kernel::SFNUtils::is_valid_sfn("LONGNAME.TXT"sv), true);
    EXPECT_EQ(Kernel::SFNUtils::is_valid_sfn("VERYLONGNAME.TXT"sv), false);
    EXPECT_EQ(Kernel::SFNUtils::is_valid_sfn("LONGEXT.HTML"sv), false);
    EXPECT_EQ(Kernel::SFNUtils::is_valid_sfn("FOO."sv), false);
    EXPECT_EQ(Kernel::SFNUtils::is_valid_sfn("FOO.."sv), false);
    EXPECT_EQ(Kernel::SFNUtils::is_valid_sfn("FOO..."sv), false);
    EXPECT_EQ(Kernel::SFNUtils::is_valid_sfn("FOO.BAR.TXT"sv), false);
    EXPECT_EQ(Kernel::SFNUtils::is_valid_sfn("FOO BAR.TXT"sv), true);
}

TEST_CASE(test_create_sfn_from_lfn)
{
    auto convert_to_serialized_sfn = [](StringView name) -> ByteString {
        // Note that serialize_name and serialize_extension always produce space-padded output.
        auto sfn = MUST(Kernel::SFNUtils::create_sfn_from_lfn(name));
        auto out = MUST(sfn->serialize_name());
        out.append('.');
        out.append(MUST(sfn->serialize_extension()));
        return ByteString(out.bytes());
    };

    EXPECT_EQ(convert_to_serialized_sfn("foo.txt"sv), "FOO~1   .TXT"sv);
    EXPECT_EQ(convert_to_serialized_sfn("FOO.TXT"sv), "FOO~1   .TXT"sv);
    EXPECT_EQ(convert_to_serialized_sfn("main.c.o"sv), "MAINC~1 .O  "sv);
    EXPECT_EQ(convert_to_serialized_sfn("longname.txt"sv), "LONGNA~1.TXT"sv);
    EXPECT_EQ(convert_to_serialized_sfn("verylongname.txt"sv), "VERYLO~1.TXT"sv);
    EXPECT_EQ(convert_to_serialized_sfn("longext.html"sv), "LONGEX~1.HTM"sv);
    EXPECT_EQ(convert_to_serialized_sfn("foo."sv), "FOO~1   .   "sv);
    EXPECT_EQ(convert_to_serialized_sfn("foo.."sv), "FOO~1   .   "sv);
    EXPECT_EQ(convert_to_serialized_sfn("foo..."sv), "FOO~1   .   "sv);
    EXPECT_EQ(convert_to_serialized_sfn("foo.bar.txt"sv), "FOOBAR~1.TXT");
    EXPECT_EQ(convert_to_serialized_sfn("foo bar.txt"sv), "FOOBAR~1.TXT");
    EXPECT_EQ(convert_to_serialized_sfn("foo@bar.txt"sv), "FOO@BA~1.TXT");
}
