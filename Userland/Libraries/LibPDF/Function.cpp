/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Function.h>
#include <LibPDF/ObjectDerivatives.h>

// PDF 1.7 spec, 3.9 Functions

namespace PDF {

struct Bound {
    float lower;
    float upper;
};

// 3.9.1 Type 0 (Sampled) Functions
class SampledFunction final : public Function {
public:
    static PDFErrorOr<NonnullRefPtr<SampledFunction>> create(Document*, Vector<Bound> domain, Optional<Vector<Bound>> range, NonnullRefPtr<StreamObject>);
    virtual PDFErrorOr<ReadonlySpan<float>> evaluate(ReadonlySpan<float>) const override;

private:
    Vector<Bound> m_domain;
    Vector<Bound> m_range;

    Vector<unsigned> m_sizes;
    int m_bits_per_sample { 0 };

    enum class Order {
        Linear = 1,
        Cubic = 3,
    };
    Order m_order { Order::Linear };

    Vector<Bound> m_encode;
    Vector<Bound> m_decode;
};

PDFErrorOr<NonnullRefPtr<SampledFunction>>
SampledFunction::create(Document* document, Vector<Bound> domain, Optional<Vector<Bound>> range, NonnullRefPtr<StreamObject> stream)
{
    if (!range.has_value())
        return Error { Error::Type::MalformedPDF, "Function type 0 requires range" };

    // "TABLE 3.36 Additional entries specific to a type 0 function dictionary"
    auto const& dict = stream->dict();

    if (!dict->contains(CommonNames::Size))
        return Error { Error::Type::MalformedPDF, "Function type 0 requires /Size" };
    auto size_array = TRY(dict->get_array(document, CommonNames::Size));
    Vector<unsigned> sizes;
    for (auto const& size_value : *size_array) {
        if (size_value.to_int() <= 0)
            return Error { Error::Type::MalformedPDF, "Function type 0 /Size entry not positive" };
        sizes.append(static_cast<unsigned>(size_value.to_int()));
    }
    if (sizes.size() != domain.size())
        return Error { Error::Type::MalformedPDF, "Function type 0 /Size array has invalid size" };

    if (!dict->contains(CommonNames::BitsPerSample))
        return Error { Error::Type::MalformedPDF, "Function type 0 requires /BitsPerSample" };
    auto bits_per_sample = TRY(document->resolve_to<int>(dict->get_value(CommonNames::BitsPerSample)));
    switch (bits_per_sample) {
    case 1:
    case 2:
    case 4:
    case 8:
    case 12:
    case 16:
    case 24:
    case 32:
        // Ok!
        break;
    default:
        dbgln("invalid /BitsPerSample {}", bits_per_sample);
        return Error { Error::Type::MalformedPDF, "Function type 0 has invalid /BitsPerSample" };
    }

    Order order = Order::Linear;
    if (dict->contains(CommonNames::Order))
        order = static_cast<Order>(TRY(document->resolve_to<int>(dict->get_value(CommonNames::Order))));
    if (order != Order::Linear && order != Order::Cubic)
        return Error { Error::Type::MalformedPDF, "Function type 0 has invalid /Order" };

    Vector<Bound> encode;
    if (dict->contains(CommonNames::Encode)) {
        auto encode_array = TRY(dict->get_array(document, CommonNames::Encode));
        if (encode_array->size() % 2 != 0)
            return Error { Error::Type::MalformedPDF, "Function type 0 /Encode size not multiple of 2" };
        for (size_t i = 0; i < encode_array->size(); i += 2)
            encode.append({ encode_array->at(i).to_float(), encode_array->at(i + 1).to_float() });
    } else {
        for (unsigned const size : sizes)
            encode.append({ 0, static_cast<float>(size - 1) });
    }
    if (encode.size() != sizes.size())
        return Error { Error::Type::MalformedPDF, "Function type 0 /Encode array has invalid size" };

    Vector<Bound> decode;
    if (dict->contains(CommonNames::Decode)) {
        auto decode_array = TRY(dict->get_array(document, CommonNames::Decode));
        if (decode_array->size() % 2 != 0)
            return Error { Error::Type::MalformedPDF, "Function type 0 /Decode size not multiple of 2" };
        for (size_t i = 0; i < decode_array->size(); i += 2)
            decode.append({ decode_array->at(i).to_float(), decode_array->at(i + 1).to_float() });
    } else {
        decode = range.value();
    }
    if (decode.size() != range.value().size())
        return Error { Error::Type::MalformedPDF, "Function type 0 /Decode array has invalid size" };

    size_t size_product = 1;
    for (unsigned const size : sizes)
        size_product *= size;
    size_t bits_per_plane = size_product * bits_per_sample;
    size_t total_bits = bits_per_plane * decode.size();
    if (stream->bytes().size() < ceil_div(total_bits, 8ull))
        return Error { Error::Type::MalformedPDF, "Function type 0 stream too small" };

    auto function = adopt_ref(*new SampledFunction());
    function->m_domain = move(domain);
    function->m_range = move(range.value());
    function->m_sizes = move(sizes);
    function->m_bits_per_sample = bits_per_sample;
    function->m_order = order;
    function->m_encode = move(encode);
    function->m_decode = move(decode);
    return function;
}

PDFErrorOr<ReadonlySpan<float>> SampledFunction::evaluate(ReadonlySpan<float>) const
{
    return Error(Error::Type::RenderingUnsupported, "SampledFunction not yet implemented"_string);
}

// 3.9.2 Type 2 (Exponential Interpolation) Functions
class ExponentialInterpolationFunction final : public Function {
public:
    static PDFErrorOr<NonnullRefPtr<ExponentialInterpolationFunction>> create(Document*, Vector<Bound> domain, Optional<Vector<Bound>> range, NonnullRefPtr<DictObject>);
    virtual PDFErrorOr<ReadonlySpan<float>> evaluate(ReadonlySpan<float>) const override;

private:
    Bound m_domain;
    Optional<Vector<Bound>> m_range;

    Vector<float> m_c0;
    Vector<float> m_c1;
    float m_n;

    Vector<float> mutable m_values;
};

PDFErrorOr<NonnullRefPtr<ExponentialInterpolationFunction>>
ExponentialInterpolationFunction::create(Document* document, Vector<Bound> domain, Optional<Vector<Bound>> range, NonnullRefPtr<DictObject> function_dict)
{
    if (domain.size() != 1)
        return Error { Error::Type::MalformedPDF, "Function exponential requires domain with 1 entry" };

    // "TABLE 3.37 Additional entries specific to a type 2 function dictionary"

    if (!function_dict->contains(CommonNames::N))
        return Error { Error::Type::MalformedPDF, "Function exponential requires /N" };

    auto n = TRY(document->resolve(function_dict->get_value(CommonNames::N))).to_float();

    Vector<float> c0;
    if (function_dict->contains(CommonNames::C0)) {
        auto c0_array = TRY(function_dict->get_array(document, CommonNames::C0));
        for (size_t i = 0; i < c0_array->size(); i++)
            c0.append(c0_array->at(i).to_float());
    } else {
        c0.append(0.0f);
    }

    Vector<float> c1;
    if (function_dict->contains(CommonNames::C1)) {
        auto c1_array = TRY(function_dict->get_array(document, CommonNames::C1));
        for (size_t i = 0; i < c1_array->size(); i++)
            c1.append(c1_array->at(i).to_float());
    } else {
        c1.append(1.0f);
    }

    if (c0.size() != c1.size())
        return Error { Error::Type::MalformedPDF, "Function exponential mismatching C0 and C1 arrays" };

    if (range.has_value()) {
        if (range->size() != c0.size())
            return Error { Error::Type::MalformedPDF, "Function exponential mismatching Range and C arrays" };
    }

    // "Values of Domain must constrain x in such a way that if N is not an integer,
    //  all values of x must be non-negative, and if N is negative, no value of x may be zero."
    if (n != (int)n && domain[0].lower < 0)
        return Error { Error::Type::MalformedPDF, "Function exponential requires non-negative bound for non-integer N" };
    if (n < 0 && (domain[0].lower <= 0 && domain[0].upper >= 0))
        return Error { Error::Type::MalformedPDF, "Function exponential with negative N requires non-zero domain" };

    auto function = adopt_ref(*new ExponentialInterpolationFunction());
    function->m_domain = domain[0];
    function->m_range = move(range);
    function->m_c0 = move(c0);
    function->m_c1 = move(c1);
    function->m_n = n;
    function->m_values.resize(function->m_c0.size());
    return function;
}

PDFErrorOr<ReadonlySpan<float>> ExponentialInterpolationFunction::evaluate(ReadonlySpan<float> xs) const
{
    if (xs.size() != 1)
        return Error { Error::Type::MalformedPDF, "Function argument size does not match domain size" };

    float const x = clamp(xs[0], m_domain.lower, m_domain.upper);

    for (size_t i = 0; i < m_c0.size(); ++i)
        m_values[i] = m_c0[i] + pow(x, m_n) * (m_c1[i] - m_c0[i]);

    if (m_range.has_value()) {
        for (size_t i = 0; i < m_c0.size(); ++i)
            m_values[i] = clamp(m_values[i], m_range.value()[i].lower, m_range.value()[i].upper);
    }

    return m_values;
}

class StitchingFunction final : public Function {
public:
    virtual PDFErrorOr<ReadonlySpan<float>> evaluate(ReadonlySpan<float>) const override;
};

PDFErrorOr<ReadonlySpan<float>> StitchingFunction::evaluate(ReadonlySpan<float>) const
{
    return Error(Error::Type::RenderingUnsupported, "StitchingFunction not yet implemented"_string);
}

class PostScriptCalculatorFunction final : public Function {
public:
    virtual PDFErrorOr<ReadonlySpan<float>> evaluate(ReadonlySpan<float>) const override;
};

PDFErrorOr<ReadonlySpan<float>> PostScriptCalculatorFunction::evaluate(ReadonlySpan<float>) const
{
    return Error(Error::Type::RenderingUnsupported, "PostScriptCalculatorFunction not yet implemented"_string);
}

PDFErrorOr<NonnullRefPtr<Function>> Function::create(Document* document, NonnullRefPtr<Object> object)
{
    if (!object->is<DictObject>() && !object->is<StreamObject>())
        return Error { Error::Type::MalformedPDF, "Function object must be dict or stream" };

    auto function_dict = object->is<DictObject>() ? object->cast<DictObject>() : object->cast<StreamObject>()->dict();

    // "TABLE 3.35 Entries common to all function dictionaries"

    if (!function_dict->contains(CommonNames::FunctionType))
        return Error { Error::Type::MalformedPDF, "Function requires /FunctionType" };
    auto function_type = TRY(document->resolve_to<int>(function_dict->get_value(CommonNames::FunctionType)));

    if (!function_dict->contains(CommonNames::Domain))
        return Error { Error::Type::MalformedPDF, "Function requires /Domain" };
    auto domain_array = TRY(function_dict->get_array(document, CommonNames::Domain));
    if (domain_array->size() % 2 != 0)
        return Error { Error::Type::MalformedPDF, "Function /Domain size not multiple of 2" };

    Vector<Bound> domain;
    for (size_t i = 0; i < domain_array->size(); i += 2) {
        domain.append({ domain_array->at(i).to_float(), domain_array->at(i + 1).to_float() });
        if (domain.last().lower > domain.last().upper)
            return Error { Error::Type::MalformedPDF, "Function /Domain lower bound > upper bound" };
    }

    // Can't use PDFErrorOr with Optional::map()
    Optional<Vector<Bound>> optional_range;
    if (function_dict->contains(CommonNames::Range)) {
        auto range_array = TRY(function_dict->get_array(document, CommonNames::Range));
        if (range_array->size() % 2 != 0)
            return Error { Error::Type::MalformedPDF, "Function /Range size not multiple of 2" };

        Vector<Bound> range;
        for (size_t i = 0; i < range_array->size(); i += 2) {
            range.append({ range_array->at(i).to_float(), range_array->at(i + 1).to_float() });
            if (range.last().lower > range.last().upper)
                return Error { Error::Type::MalformedPDF, "Function /Range lower bound > upper bound" };
        }
        optional_range = move(range);
    }

    switch (function_type) {
    case 0:
        if (!object->is<StreamObject>())
            return Error { Error::Type::MalformedPDF, "Function type 0 requires stream object" };
        return SampledFunction::create(document, move(domain), move(optional_range), object->cast<StreamObject>());
    // The spec has no entry for `1`.
    case 2:
        // FIXME: spec is not clear on if this should work with a StreamObject.
        return ExponentialInterpolationFunction::create(document, move(domain), move(optional_range), function_dict);
    case 3:
        return adopt_ref(*new StitchingFunction());
    case 4:
        return adopt_ref(*new PostScriptCalculatorFunction());
    default:
        dbgln("invalid function type {}", function_type);
        return Error(Error::Type::MalformedPDF, "Function has unkonwn type"_string);
    }
}

}
