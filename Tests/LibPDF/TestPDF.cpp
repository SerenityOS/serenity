/*
 * Copyright (c) 2021, Simon Woertz <simon@woertz.at>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/Forward.h>
#include <LibCore/MappedFile.h>
#include <LibPDF/Document.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>

TEST_CASE(linearized_pdf)
{
    auto file = Core::MappedFile::map("linearized.pdf"sv).release_value();
    auto document = PDF::Document::create(file->bytes());
    EXPECT(!document.is_error());
    EXPECT(!document.value()->initialize().is_error());
    EXPECT_EQ(document.value()->get_page_count(), 1U);
}

TEST_CASE(non_linearized_pdf)
{
    auto file = Core::MappedFile::map("non-linearized.pdf"sv).release_value();
    auto document = PDF::Document::create(file->bytes());
    EXPECT(!document.is_error());
    EXPECT(!document.value()->initialize().is_error());
    EXPECT_EQ(document.value()->get_page_count(), 1U);
}

TEST_CASE(complex_pdf)
{
    auto file = Core::MappedFile::map("complex.pdf"sv).release_value();
    auto document = PDF::Document::create(file->bytes());
    EXPECT(!document.is_error());
    EXPECT(!document.value()->initialize().is_error());
    EXPECT_EQ(document.value()->get_page_count(), 3U);
}

TEST_CASE(empty_file_issue_10702)
{
    AK::ReadonlyBytes empty;
    auto document = PDF::Document::create(empty);
    EXPECT(document.is_error());
}

TEST_CASE(truncated_pdf_header_issue_10717)
{
    AK::DeprecatedString string { "%PDF-2.11%" };
    auto document = PDF::Document::create(string.bytes());
    EXPECT(document.is_error());
}
