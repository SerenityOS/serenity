/*
 * Copyright (c) 2021, Simon Woertz <simon@woertz.at>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/Forward.h>
#include <LibCore/MappedFile.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Function.h>
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

TEST_CASE(encodig)
{
    auto file = MUST(Core::MappedFile::map("encoding.pdf"sv));
    auto document = MUST(PDF::Document::create(file->bytes()));
    MUST(document->initialize());
    EXPECT_EQ(document->get_page_count(), 1U);

    auto info_dict = MUST(document->info_dict()).value();
    EXPECT_EQ(MUST(info_dict.author()).value(), "Nico Weber");
    EXPECT_EQ(MUST(info_dict.producer()).value(), (char const*)u8"Manüally Created");

    // FIXME: Make this pass.
    // EXPECT_EQ(MUST(info_dict.title()).value(), (char const*)u8"Êñ©•ding test");
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

TEST_CASE(malformed_pdf_document)
{
    Array test_inputs = {
        "oss-fuzz-testcase-62065.pdf"sv
    };

    for (auto test_input : test_inputs) {
        auto file = MUST(Core::MappedFile::map(test_input));
        auto document_or_error = PDF::Document::create(file->bytes());
        EXPECT(document_or_error.is_error());
    }
}

static PDF::Value make_array(Vector<float> floats)
{
    Vector<PDF::Value> values;
    for (auto f : floats)
        values.append(PDF::Value { f });
    return PDF::Value { adopt_ref(*new PDF::ArrayObject(move(values))) };
}

static PDF::PDFErrorOr<NonnullRefPtr<PDF::Function>> make_function(int type, ReadonlyBytes data, Vector<float> domain, Vector<float> range, Function<void(HashMap<DeprecatedFlyString, PDF::Value>&)> extra_keys = nullptr)
{
    HashMap<DeprecatedFlyString, PDF::Value> map;
    map.set(PDF::CommonNames::FunctionType, PDF::Value { type });
    map.set(PDF::CommonNames::Domain, make_array(move(domain)));
    map.set(PDF::CommonNames::Range, make_array(move(range)));
    if (extra_keys)
        extra_keys(map);
    auto dict = adopt_ref(*new PDF::DictObject(move(map)));
    auto stream = adopt_ref(*new PDF::StreamObject(dict, MUST(ByteBuffer::copy(data))));

    // document isn't used for anything, but UBSan complains about a (harmless) method call on a null object without it.
    auto file = MUST(Core::MappedFile::map("linearized.pdf"sv));
    auto document = MUST(PDF::Document::create(file->bytes()));
    return PDF::Function::create(document, stream);
}

static PDF::PDFErrorOr<NonnullRefPtr<PDF::Function>> make_sampled_function(ReadonlyBytes data, Vector<float> domain, Vector<float> range, Vector<float> sizes)
{
    return make_function(0, data, move(domain), move(range), [&sizes](auto& map) {
        map.set(PDF::CommonNames::Size, make_array(sizes));
        map.set(PDF::CommonNames::BitsPerSample, PDF::Value { 8 });
    });
}

TEST_CASE(sampled)
{
    auto f1 = MUST(make_sampled_function(Vector<u8> { { 0, 255, 0 } }, { 0.0f, 1.0f }, { 0.0f, 10.0f }, { 3 }));
    EXPECT_EQ(MUST(f1->evaluate(Vector<float> { 0.0f })), Vector<float> { 0.0f });
    EXPECT_EQ(MUST(f1->evaluate(Vector<float> { 0.25f })), Vector<float> { 5.0f });
    EXPECT_EQ(MUST(f1->evaluate(Vector<float> { 0.5f })), Vector<float> { 10.0f });
    EXPECT_EQ(MUST(f1->evaluate(Vector<float> { 0.75f })), Vector<float> { 5.0f });
    EXPECT_EQ(MUST(f1->evaluate(Vector<float> { 1.0f })), Vector<float> { 0.0f });

    auto f2 = MUST(make_sampled_function(Vector<u8> { { 0, 255, 255, 0, 0, 255 } }, { 0.0f, 1.0f }, { 0.0f, 10.0f, 0.0f, 8.0f }, { 3 }));
    EXPECT_EQ(MUST(f2->evaluate(Vector<float> { 0.0f })), (Vector<float> { 0.0f, 8.0f }));
    EXPECT_EQ(MUST(f2->evaluate(Vector<float> { 0.25f })), (Vector<float> { 5.0f, 4.0f }));
    EXPECT_EQ(MUST(f2->evaluate(Vector<float> { 0.5f })), (Vector<float> { 10.0f, 0.0f }));
    EXPECT_EQ(MUST(f2->evaluate(Vector<float> { 0.75f })), (Vector<float> { 5.0f, 4.0f }));
    EXPECT_EQ(MUST(f2->evaluate(Vector<float> { 1.0f })), (Vector<float> { 0.0f, 8.0f }));

    auto f3 = MUST(make_sampled_function(Vector<u8> { { 0, 255, 0, 255, 0, 255 } }, { 0.0f, 1.0f, 0.0f, 1.0f }, { 0.0f, 10.0f }, { 3, 2 }));
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 0.0f, 0.0f })), Vector<float> { 0.0f });
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 0.25f, 0.0f })), Vector<float> { 5.0f });
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 0.5f, 0.0f })), Vector<float> { 10.0f });
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 0.75f, 0.0f })), Vector<float> { 5.0f });
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 1.0f, 0.0f })), Vector<float> { 0.0f });

    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 0.0f, 0.5f })), Vector<float> { 5.0f });
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 0.25f, 0.5f })), Vector<float> { 5.0f });
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 0.5f, 0.5f })), Vector<float> { 5.0f });
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 0.75f, 0.5f })), Vector<float> { 5.0f });
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 1.0f, 0.5f })), Vector<float> { 5.0f });

    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 0.0f, 1.0f })), Vector<float> { 10.0f });
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 0.25f, 1.0f })), Vector<float> { 5.0f });
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 0.5f, 1.0f })), Vector<float> { 0.0f });
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 0.75f, 1.0f })), Vector<float> { 5.0f });
    EXPECT_EQ(MUST(f3->evaluate(Vector<float> { 1.0f, 1.0f })), Vector<float> { 10.0f });

    auto f4 = MUST(make_sampled_function(Vector<u8> { { 0, 255, 255, 0, 0, 255, 255, 0 } }, { 0.0f, 1.0f, 0.0f, 1.0f }, { 0.0f, 10.0f, 0.0f, 8.0f }, { 2, 2 }));
    EXPECT_EQ(MUST(f4->evaluate(Vector<float> { 0.0f, 0.0f })), (Vector<float> { 0.0f, 8.0f }));
    EXPECT_EQ(MUST(f4->evaluate(Vector<float> { 0.5f, 0.5f })), (Vector<float> { 5.0f, 4.0f }));
}

static PDF::PDFErrorOr<NonnullRefPtr<PDF::Function>> make_postscript_function(StringView program, Vector<float> domain, Vector<float> range)
{
    return make_function(4, program.bytes(), move(domain), move(range));
}

static NonnullRefPtr<PDF::Function> check_postscript_function(StringView program, Vector<float> domain, Vector<float> range)
{
    auto function = make_postscript_function(program, move(domain), move(range));
    if (function.is_error())
        FAIL(function.error().message());
    return function.value();
}

static void check_evaluate(StringView program, Vector<float> inputs, Vector<float> outputs)
{
    Vector<float> domain;
    for (size_t i = 0; i < inputs.size(); ++i) {
        domain.append(-100.0f);
        domain.append(100.0f);
    }
    Vector<float> range;
    for (size_t i = 0; i < outputs.size(); ++i) {
        range.append(-100.0f);
        range.append(100.0f);
    }
    auto function = check_postscript_function(program, domain, range);
    auto result = function->evaluate(inputs);
    if (result.is_error())
        FAIL(result.error().message());
    EXPECT_EQ(result.value(), outputs);
}

TEST_CASE(postscript)
{
    // Arithmetic operators
    check_evaluate("{ abs }"sv, { 0.5f }, { 0.5f });
    check_evaluate("{ add }"sv, { 0.25f, 0.5f }, { 0.75f });
    check_evaluate("{ atan }"sv, { 1.0f, 0.01f }, { AK::to_degrees(atan2f(0.01f, 1.0f)) });
    check_evaluate("{ ceiling }"sv, { 0.5f }, { 1.0f });
    check_evaluate("{ cos }"sv, { 1.0f }, { cosf(AK::to_radians(1.0f)) });
    check_evaluate("{ cvi }"sv, { 0.5f }, { 0.0f });
    check_evaluate("{ cvr }"sv, { 0.5f }, { 0.5f });
    check_evaluate("{ div }"sv, { 0.5f, 1.0f }, { 0.5f });
    check_evaluate("{ exp }"sv, { 0.0f }, { 1.0f });
    check_evaluate("{ floor }"sv, { 0.5f }, { 0.0f });
    check_evaluate("{ idiv }"sv, { 0.5f, 1.0f }, { 0.0f });
    check_evaluate("{ ln }"sv, { 10.0f }, { logf(10.0f) });
    check_evaluate("{ log }"sv, { 10.0f }, { log10f(10.0f) });
    check_evaluate("{ mod }"sv, { 0.5f, 0.25f }, { 0.0f });
    check_evaluate("{ mul }"sv, { 0.5f, 0.25f }, { 0.125f });
    check_evaluate("{ neg }"sv, { 0.5f }, { -0.5f });
    check_evaluate("{ round }"sv, { 0.5f }, { 1.0f });
    check_evaluate("{ sin }"sv, { 1.0f }, { sinf(AK::to_radians(1.0f)) });
    check_evaluate("{ sqrt }"sv, { 0.5f }, { sqrtf(0.5f) });
    check_evaluate("{ sub }"sv, { 0.5f, 0.25f }, { 0.25f });
    check_evaluate("{ truncate }"sv, { 0.5f }, { 0.0f });

    // Relational, boolean, and bitwise operators
    check_evaluate("{ and }"sv, { 0.0f, 1.0f }, { 0.0f });
    check_evaluate("{ bitshift }"sv, { 1.0f, 3.0f }, { 8.0f });
    check_evaluate("{ bitshift }"sv, { 8.0f, -2.0f }, { 2.0f });
    check_evaluate("{ eq }"sv, { 0.5f, 0.5f }, { 1.0f });
    check_evaluate("{ ge }"sv, { 0.5f, 0.5f }, { 1.0f });
    check_evaluate("{ gt }"sv, { 0.5f, 0.5f }, { 0.0f });
    check_evaluate("{ le }"sv, { 0.5f, 0.5f }, { 1.0f });
    check_evaluate("{ lt }"sv, { 0.5f, 0.5f }, { 0.0f });
    check_evaluate("{ ne }"sv, { 0.5f, 0.5f }, { 0.0f });
    check_evaluate("{ not }"sv, { 0.5f }, { 0.0f });
    check_evaluate("{ or }"sv, { 0.0f, 1.0f }, { 1.0f });
    check_evaluate("{ xor }"sv, { 0.0f, 1.0f }, { 1.0f });

    // Conditional operators
    check_evaluate("{ { 4 } if }"sv, { 1.0f }, { 4.0f });
    check_evaluate("{ { 4 } if }"sv, { 0.0f }, {});
    check_evaluate("{ { 4 } { 5 } ifelse }"sv, { 1.0f }, { 4.0f });
    check_evaluate("{ { 4 } { 5 } ifelse }"sv, { 0.0f }, { 5.0f });

    // Stack operators
    check_evaluate("{ 2 copy }"sv, { 8.0f, 0.5f, 1.0f }, { 8.0f, 0.5f, 1.0f, 0.5f, 1.0f });
    check_evaluate("{ dup }"sv, { 1.0f, 0.5f }, { 1.0f, 0.5f, 0.5f });
    check_evaluate("{ exch }"sv, { 8.0f, 1.0f, 0.5f }, { 8.0f, 0.5f, 1.0f });
    check_evaluate("{ 1 index }"sv, { 8.0f, 1.0f, 0.5f }, { 8.0f, 1.0f, 0.5f, 1.0f });
    check_evaluate("{ pop }"sv, { 8.0f, 1.0f, 0.5f }, { 8.0f, 1.0f });
    check_evaluate("{ 3 1 roll }"sv, { 0.5f, 1.0f, 2.0f }, { 2.0f, 0.5f, 1.0f });
    check_evaluate("{ 3 -1 roll }"sv, { 0.5f, 1.0f, 2.0f }, { 1.0f, 2.0f, 0.5f });
}
