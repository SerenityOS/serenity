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
    auto file = MUST(Core::MappedFile::map("linearized.pdf"sv));
    auto document = MUST(PDF::Document::create(file->bytes()));
    MUST(document->initialize());
    EXPECT_EQ(document->get_page_count(), 1U);
}

TEST_CASE(non_linearized_pdf)
{
    auto file = MUST(Core::MappedFile::map("non-linearized.pdf"sv));
    auto document = MUST(PDF::Document::create(file->bytes()));
    MUST(document->initialize());
    EXPECT_EQ(document->get_page_count(), 1U);
}

TEST_CASE(complex_pdf)
{
    auto file = MUST(Core::MappedFile::map("complex.pdf"sv));
    auto document = MUST(PDF::Document::create(file->bytes()));
    MUST(document->initialize());
    EXPECT_EQ(document->get_page_count(), 3U);
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

TEST_CASE(encrypted_with_aes)
{
    auto file = MUST(Core::MappedFile::map("password-is-sup.pdf"sv));
    auto document = MUST(PDF::Document::create(file->bytes()));
    EXPECT(document->security_handler()->try_provide_user_password("sup"sv));
    MUST(document->initialize());
    EXPECT_EQ(document->get_page_count(), 1U);

    auto info_dict = MUST(document->info_dict()).value();
    EXPECT_EQ(MUST(info_dict.title()).value(), "sup");
    EXPECT_EQ(MUST(info_dict.creator()).value(), "TextEdit");
}

TEST_CASE(encrypted_object_stream)
{
    auto file = MUST(Core::MappedFile::map("encryption_nocopy.pdf"sv));
    auto document = MUST(PDF::Document::create(file->bytes()));
    MUST(document->initialize());
    EXPECT_EQ(document->get_page_count(), 1U);

    auto info_dict = MUST(document->info_dict()).value();
    EXPECT_EQ(MUST(info_dict.author()).value(), "van der Knijff");
    EXPECT_EQ(MUST(info_dict.creator()).value(), "Acrobat PDFMaker 9.1 voor Word");
}
