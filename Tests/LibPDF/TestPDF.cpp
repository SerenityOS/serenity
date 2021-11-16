/*
 * Copyright (c) 2021, Simon Woertz <simon@woertz.at>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/MappedFile.h>
#include <LibPDF/Document.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>

TEST_CASE(linearized_pdf)
{
    auto file = Core::MappedFile::map("linearized.pdf").release_value();
    auto document = PDF::Document::create(file->bytes());
    EXPECT_EQ(document->get_page_count(), 1U);
}

TEST_CASE(non_linearized_pdf)
{
    auto file = Core::MappedFile::map("non-linearized.pdf").release_value();
    auto document = PDF::Document::create(file->bytes());
    EXPECT_EQ(document->get_page_count(), 1U);
}

TEST_CASE(complex_pdf)
{
    auto file = Core::MappedFile::map("complex.pdf").release_value();
    auto document = PDF::Document::create(file->bytes());
    EXPECT_EQ(document->get_page_count(), 3U);
}
