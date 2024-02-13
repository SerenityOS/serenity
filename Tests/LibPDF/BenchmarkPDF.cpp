/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/MappedFile.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Function.h>
#include <LibTest/TestCase.h>

static PDF::Value make_array(Vector<float> floats)
{
    Vector<PDF::Value> values;
    for (auto f : floats)
        values.append(PDF::Value { f });
    return PDF::Value { make_object<PDF::ArrayObject>(move(values)) };
}

static PDF::PDFErrorOr<NonnullRefPtr<PDF::Function>> make_function(int type, ReadonlyBytes data, Vector<float> domain, Vector<float> range, Function<void(HashMap<DeprecatedFlyString, PDF::Value>&)> extra_keys = nullptr)
{
    HashMap<DeprecatedFlyString, PDF::Value> map;
    map.set(PDF::CommonNames::FunctionType, PDF::Value { type });
    map.set(PDF::CommonNames::Domain, make_array(move(domain)));
    map.set(PDF::CommonNames::Range, make_array(move(range)));
    if (extra_keys)
        extra_keys(map);
    auto dict = make_object<PDF::DictObject>(move(map));
    auto stream = make_object<PDF::StreamObject>(dict, MUST(ByteBuffer::copy(data)));

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

static PDF::PDFErrorOr<NonnullRefPtr<PDF::Function>> make_bench_sampled_function()
{
    Vector<float> domain = { 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f };
    Vector<float> range = { 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f };
    Vector<float> sizes = { 9, 9, 9, 9 };
    Vector<u8> data;
    size_t total_size = range.size() / 2;
    for (auto size : sizes)
        total_size *= static_cast<size_t>(size);
    data.resize(total_size);
    return make_sampled_function(data.span(), move(domain), move(range), move(sizes));
}

BENCHMARK_CASE(function)
{
    auto bench_function = MUST(make_bench_sampled_function());

    Vector<float, 4> inputs;
    inputs.resize(4);
    for (int i = 0; i < 500'000; ++i) {
        inputs[0] = i * 31;
        inputs[1] = i * 19;
        inputs[2] = i * 103;
        inputs[3] = i * 7;
        auto result = MUST(bench_function->evaluate(inputs));
        VERIFY(result[0] == 0);
        VERIFY(result[1] == 0);
        VERIFY(result[2] == 0);
    }
}
