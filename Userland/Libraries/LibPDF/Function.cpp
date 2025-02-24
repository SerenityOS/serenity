/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
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
    SampledFunction(NonnullRefPtr<StreamObject>);

    ReadonlySpan<u8> sample(ReadonlySpan<int> const& coordinates) const
    {
        // "For a function with multidimensional input (more than one input variable),
        //  the sample values in the first dimension vary fastest,
        //  and the values in the last dimension vary slowest.
        //  For example, for a function f(a, b, c), where a, b, and c vary from 0 to 9 in steps of 1,
        //  the sample values would appear in this order:
        //  f(0,0,0), f(1,0,0), ..., f(9,0,0),
        //  f(0,1,0), f(1,1,0), ..., f(9,1,0),
        //  f(0,2,0), f(1, 2, 0), ..., f(9, 9, 0),
        //  f(0, 0, 1), f(1, 0, 1), and so on."
        // Implied is that functions with multiple outputs store all outputs next to each other.
        size_t stride = 1;
        size_t offset = 0;
        for (size_t i = 0; i < coordinates.size(); ++i) {
            offset += coordinates[i] * stride;
            stride *= m_sizes[i];
        }
        return m_sample_data.slice(offset * m_range.size(), m_range.size());
    }

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

    NonnullRefPtr<StreamObject> m_stream;
    ReadonlyBytes m_sample_data;

    Vector<float> mutable m_inputs;
    Vector<unsigned> mutable m_left_index;
    Vector<float> mutable m_outputs;
};

SampledFunction::SampledFunction(NonnullRefPtr<StreamObject> stream)
    : m_stream(move(stream))
    , m_sample_data(m_stream->bytes())
{
}

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
    if (stream->bytes().size() < ceil_div(total_bits, static_cast<size_t>(8)))
        return Error { Error::Type::MalformedPDF, "Function type 0 stream too small" };

    auto function = adopt_ref(*new SampledFunction(stream));
    function->m_domain = move(domain);
    function->m_range = move(range.value());
    function->m_sizes = move(sizes);
    function->m_bits_per_sample = bits_per_sample;
    function->m_order = order;
    function->m_encode = move(encode);
    function->m_decode = move(decode);
    function->m_inputs.resize(function->m_domain.size());
    function->m_left_index.resize(function->m_domain.size());
    function->m_outputs.resize(function->m_range.size());
    return function;
}

PDFErrorOr<ReadonlySpan<float>> SampledFunction::evaluate(ReadonlySpan<float> xs) const
{
    if (xs.size() != m_domain.size())
        return Error { Error::Type::MalformedPDF, "Function argument size does not match domain size" };

    if (m_order != Order::Linear)
        return Error { Error::Type::RenderingUnsupported, "Sample function with cubic order not yet implemented" };

    if (m_bits_per_sample != 8)
        return Error { Error::Type::RenderingUnsupported, "Sample function with bits per sample != 8 not yet implemented" };

    auto interpolate = [](float x, float x_min, float x_max, float y_min, float y_max) {
        return mix(y_min, y_max, (x - x_min) / (x_max - x_min));
    };

    for (size_t i = 0; i < m_domain.size(); ++i) {
        float x = clamp(xs[i], m_domain[i].lower, m_domain[i].upper);
        float e = interpolate(x, m_domain[i].lower, m_domain[i].upper, m_encode[i].lower, m_encode[i].upper);

        unsigned n = m_sizes[i] - 1;
        float ec = clamp(e, 0.0f, static_cast<float>(n));
        m_left_index[i] = min(ec, n - 1);
        m_inputs[i] = ec - m_left_index[i];
    }

    // For 1-D input data, we need to sample 2 points, one to the left and one to the right, and then interpolate between them.
    // For 2-D input data, we need to sample 4 points (top-left, top-right, bottom-left, bottom-right),
    // then reduce them to 2 points by interpolating along y, and then to 1 by interpolating along x.
    // For 3-D input data, it's 8 points in a cube around the point, then reduce to 4 points by interpolating along z,
    // then 2 by interpolating along y, then 1 by interpolating along x.
    // So for the general case, we create 2**N samples, and then for each coordinate, we cut the number of samples in half
    // by interpolating along that coordinate.
    // Instead of storing all the 2**N samples, we can calculate the product of weights for each corner,
    // and sum up the weighted samples.
    Vector<float, 4> sample_outputs;
    sample_outputs.resize(m_range.size());
    // The i'th bit of mask indicates if the i'th coordinate is rounded up or down.
    Vector<int> coordinates;
    coordinates.resize(m_domain.size());
    for (size_t mask = 0; mask < (1u << m_domain.size()); ++mask) {
        float sample_weight = 1.0f;
        for (size_t i = 0; i < m_domain.size(); ++i) {
            coordinates[i] = m_left_index[i] + ((mask >> i) & 1u);
            sample_weight *= ((mask >> i) & 1u) ? m_inputs[i] : (1.0f - m_inputs[i]);
        }
        ReadonlyBytes samples = sample(coordinates);
        for (size_t r = 0; r < m_range.size(); ++r)
            sample_outputs[r] += samples[r] * sample_weight;
    }

    for (size_t r = 0; r < m_range.size(); ++r) {
        float result = interpolate(sample_outputs[r], 0.0f, 255.0f, m_decode[r].lower, m_decode[r].upper);
        m_outputs[r] = clamp(result, m_range[r].lower, m_range[r].upper);
    }

    return m_outputs;
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
    static PDFErrorOr<NonnullRefPtr<StitchingFunction>> create(Document*, Vector<Bound> domain, Optional<Vector<Bound>> range, NonnullRefPtr<DictObject>);
    virtual PDFErrorOr<ReadonlySpan<float>> evaluate(ReadonlySpan<float>) const override;

private:
    StitchingFunction(Vector<NonnullRefPtr<Function>>);

    Bound m_domain;
    Optional<Vector<Bound>> m_range;

    Vector<NonnullRefPtr<Function>> m_functions;
    Vector<float> m_bounds;
    Vector<Bound> m_encode;
    Vector<float> mutable m_result;
};

StitchingFunction::StitchingFunction(Vector<NonnullRefPtr<Function>> functions)
    : m_functions(move(functions))
{
}

PDFErrorOr<NonnullRefPtr<StitchingFunction>>
StitchingFunction::create(Document* document, Vector<Bound> domain, Optional<Vector<Bound>> range, NonnullRefPtr<DictObject> dict)
{
    if (domain.size() != 1)
        return Error { Error::Type::MalformedPDF, "Function stitching requires domain with 1 entry" };

    // "TABLE 3.38 Additional entries specific to a type 3 function dictionary"

    if (!dict->contains(CommonNames::Functions))
        return Error { Error::Type::MalformedPDF, "Function stitching requires /Functions" };
    auto functions_array = TRY(dict->get_array(document, CommonNames::Functions));

    Vector<NonnullRefPtr<Function>> functions;
    for (size_t i = 0; i < functions_array->size(); i++) {
        auto function = TRY(Function::create(document, TRY(functions_array->get_object_at(document, i))));
        functions.append(move(function));
    }

    if (functions.is_empty())
        return Error { Error::Type::MalformedPDF, "Function stitching requires at least one function" };

    if (!dict->contains(CommonNames::Bounds))
        return Error { Error::Type::MalformedPDF, "Function stitching requires /Bounds" };
    auto bounds_array = TRY(dict->get_array(document, CommonNames::Bounds));

    if (bounds_array->size() != functions.size() - 1)
        return Error { Error::Type::MalformedPDF, "Function stitching /Bounds size does not match /Functions size" };

    Vector<float> bounds;
    for (size_t i = 0; i < bounds_array->size(); i++) {
        bounds.append(bounds_array->at(i).to_float());
        if (i > 0 && bounds[i - 1] >= bounds[i])
            return Error { Error::Type::MalformedPDF, "Function stitching /Bounds not strictly increasing" };
    }

    if (!bounds.is_empty()) {
        if (domain[0].lower == domain[0].upper)
            return Error { Error::Type::MalformedPDF, "Function stitching /Bounds requires non-zero domain" };
        if (domain[0].lower >= bounds[0] || bounds.last() >= domain[0].upper)
            return Error { Error::Type::MalformedPDF, "Function stitching /Bounds out of domain" };
    }

    if (!dict->contains(CommonNames::Encode))
        return Error { Error::Type::MalformedPDF, "Function stitching requires /Encode" };
    auto encode_array = TRY(dict->get_array(document, CommonNames::Encode));

    if (encode_array->size() != functions.size() * 2)
        return Error { Error::Type::MalformedPDF, "Function stitching /Encode size does not match /Functions size" };

    Vector<Bound> encode;
    for (size_t i = 0; i < encode_array->size(); i += 2)
        encode.append({ encode_array->at(i).to_float(), encode_array->at(i + 1).to_float() });

    auto function = adopt_ref(*new StitchingFunction(move(functions)));
    function->m_domain = domain[0];
    function->m_range = move(range);
    function->m_bounds = move(bounds);
    function->m_encode = move(encode);
    if (function->m_range.has_value())
        function->m_result.resize(function->m_range.value().size());
    return function;
}

PDFErrorOr<ReadonlySpan<float>> StitchingFunction::evaluate(ReadonlySpan<float> xs) const
{
    if (xs.size() != 1)
        return Error { Error::Type::MalformedPDF, "Function argument size does not match domain size" };

    float x = clamp(xs[0], m_domain.lower, m_domain.upper);

    // FIXME: binary search
    size_t i = 0;
    for (; i < m_bounds.size(); ++i) {
        if (x < m_bounds[i])
            break;
    }
    float left_bound = i == 0 ? m_domain.lower : m_bounds[i - 1];
    float right_bound = i == m_bounds.size() ? m_domain.upper : m_bounds[i];

    auto interpolate = [](float x, float x_min, float x_max, float y_min, float y_max) {
        return y_min + (x - x_min) * (y_max - y_min) / (x_max - x_min);
    };
    x = interpolate(x, left_bound, right_bound, m_encode[i].lower, m_encode[i].upper);
    auto result = TRY(m_functions[i]->evaluate({ &x, 1 }));
    if (!m_range.has_value())
        return result;

    if (result.size() != m_range.value().size())
        return Error { Error::Type::MalformedPDF, "Function stitching result size does not match range size" };
    for (size_t i = 0; i < result.size(); ++i)
        m_result[i] = clamp(result[i], m_range.value()[i].lower, m_range.value()[i].upper);
    return m_result;
}

class PostScriptCalculatorFunction final : public Function {
public:
    static PDFErrorOr<NonnullRefPtr<PostScriptCalculatorFunction>> create(Vector<Bound> domain, Optional<Vector<Bound>> range, NonnullRefPtr<StreamObject>);
    virtual PDFErrorOr<ReadonlySpan<float>> evaluate(ReadonlySpan<float>) const override;

private:
    // TABLE 3.39 Operators in type 4 functions
    enum class OperatorType {
        Operand,

        // Arithmetic operators
        Abs,
        Add,
        Atan,
        Ceiling,
        Cos,
        Cvi,
        Cvr,
        Div,
        Exp,
        Floor,
        Idiv,
        Ln,
        Log,
        Mod,
        Mul,
        Neg,
        Round,
        Sin,
        Sqrt,
        Sub,
        Truncate,

        // Relational, boolean, and bitwise operators
        And,
        Bitshift,
        Eq,
        False,
        Ge,
        Gt,
        Le,
        Lt,
        Ne,
        Not,
        Or,
        True,
        Xor,

        // Conditional operators
        If,
        IfElse,

        // Stack operators
        Copy,
        Dup,
        Exch,
        Index,
        Pop,
        Roll,
    };
    static Optional<OperatorType> parse_operator(Reader&);

    struct IfElse;
    struct Token {
        // FIXME: Could nan-box this.
        OperatorType type;
        Variant<Empty, float, int> value {};
    };

    struct IfElse {
        Vector<Token> if_true;
        Vector<Token> if_false;
    };

    static bool skip_whitespace_and_comments(Reader&);
    static PDFErrorOr<Vector<Token>> parse_postscript_calculator_function(Reader&, Vector<NonnullOwnPtr<IfElse>>&);

    struct Stack {
        Array<float, 100> stack;
        size_t top { 0 };

        PDFErrorOr<void> push(float value)
        {
            if (top == stack.size())
                return Error { Error::Type::RenderingUnsupported, "PostScript stack overflow"_string };
            stack[top++] = value;
            return {};
        }

        PDFErrorOr<float> pop()
        {
            if (top == 0)
                return Error { Error::Type::RenderingUnsupported, "PostScript stack underflow"_string };
            return stack[--top];
        }
    };
    PDFErrorOr<void> execute(Vector<Token> const&, Stack&) const;

    Vector<Bound> m_domain;
    Vector<Bound> m_range;
    Vector<Token> m_tokens;
    Vector<NonnullOwnPtr<IfElse>> m_if_elses;

    Vector<float> mutable m_result;
};

Optional<PostScriptCalculatorFunction::OperatorType> PostScriptCalculatorFunction::parse_operator(Reader& reader)
{
    auto match_keyword = [&](char const* keyword) {
        if (reader.matches(keyword)) {
            // FIXME: Check if followed by whitespace or any of (, ), <, >, [, ], {, }, /, %.
            //        Currently, this incorrectly accepts `add4` as `add 4`.
            reader.consume((int)strlen(keyword));
            return true;
        }
        return false;
    };

    if (match_keyword("abs"))
        return OperatorType::Abs;
    if (match_keyword("add"))
        return OperatorType::Add;
    if (match_keyword("atan"))
        return OperatorType::Atan;
    if (match_keyword("ceiling"))
        return OperatorType::Ceiling;
    if (match_keyword("cos"))
        return OperatorType::Cos;
    if (match_keyword("cvi"))
        return OperatorType::Cvi;
    if (match_keyword("cvr"))
        return OperatorType::Cvr;
    if (match_keyword("div"))
        return OperatorType::Div;
    if (match_keyword("exp"))
        return OperatorType::Exp;
    if (match_keyword("floor"))
        return OperatorType::Floor;
    if (match_keyword("idiv"))
        return OperatorType::Idiv;
    if (match_keyword("ln"))
        return OperatorType::Ln;
    if (match_keyword("log"))
        return OperatorType::Log;
    if (match_keyword("mod"))
        return OperatorType::Mod;
    if (match_keyword("mul"))
        return OperatorType::Mul;
    if (match_keyword("neg"))
        return OperatorType::Neg;
    if (match_keyword("round"))
        return OperatorType::Round;
    if (match_keyword("sin"))
        return OperatorType::Sin;
    if (match_keyword("sqrt"))
        return OperatorType::Sqrt;
    if (match_keyword("sub"))
        return OperatorType::Sub;
    if (match_keyword("truncate"))
        return OperatorType::Truncate;
    if (match_keyword("and"))
        return OperatorType::And;
    if (match_keyword("bitshift"))
        return OperatorType::Bitshift;
    if (match_keyword("eq"))
        return OperatorType::Eq;
    if (match_keyword("false"))
        return OperatorType::False;
    if (match_keyword("ge"))
        return OperatorType::Ge;
    if (match_keyword("gt"))
        return OperatorType::Gt;
    if (match_keyword("le"))
        return OperatorType::Le;
    if (match_keyword("lt"))
        return OperatorType::Lt;
    if (match_keyword("ne"))
        return OperatorType::Ne;
    if (match_keyword("not"))
        return OperatorType::Not;
    if (match_keyword("or"))
        return OperatorType::Or;
    if (match_keyword("true"))
        return OperatorType::True;
    if (match_keyword("xor"))
        return OperatorType::Xor;
    // If and Ifelse handled elsewhere.
    if (match_keyword("copy"))
        return OperatorType::Copy;
    if (match_keyword("dup"))
        return OperatorType::Dup;
    if (match_keyword("exch"))
        return OperatorType::Exch;
    if (match_keyword("index"))
        return OperatorType::Index;
    if (match_keyword("pop"))
        return OperatorType::Pop;
    if (match_keyword("roll"))
        return OperatorType::Roll;
    return {};
}

bool PostScriptCalculatorFunction::skip_whitespace_and_comments(Reader& reader)
{
    bool did_skip = false;
    while (!reader.done()) {
        if (reader.consume_whitespace()) {
            did_skip = true;
            continue;
        }
        if (reader.matches('%')) {
            did_skip = true;
            reader.consume();
            while (!reader.consume_eol())
                reader.consume();
            continue;
        }
        break;
    }
    return did_skip;
}

PDFErrorOr<Vector<PostScriptCalculatorFunction::Token>>
PostScriptCalculatorFunction::parse_postscript_calculator_function(Reader& reader, Vector<NonnullOwnPtr<IfElse>>& if_elses)
{
    // Assumes valid syntax.
    skip_whitespace_and_comments(reader);
    if (!reader.consume('{'))
        return Error { Error::Type::MalformedPDF, "PostScript expected '{'" };

    Vector<PostScriptCalculatorFunction::Token> tokens;
    while (!reader.matches('}')) {
        if (skip_whitespace_and_comments(reader))
            continue;

        if (reader.matches('{')) {
            auto if_true = TRY(parse_postscript_calculator_function(reader, if_elses));
            skip_whitespace_and_comments(reader);
            if (reader.matches("if")) {
                reader.consume(2);
                tokens.append({ OperatorType::If, (int)if_elses.size() });
                if_elses.append(adopt_own(*new IfElse { move(if_true), {} }));
                continue;
            }

            VERIFY(reader.matches('{'));
            auto if_false = TRY(parse_postscript_calculator_function(reader, if_elses));
            skip_whitespace_and_comments(reader);

            if (reader.matches("ifelse")) {
                reader.consume(6);
                tokens.append({ OperatorType::IfElse, (int)if_elses.size() });
                if_elses.append(adopt_own(*new IfElse { move(if_true), move(if_false) }));
                continue;
            }

            return Error { Error::Type::MalformedPDF, "PostScript confused parsing {}-delimited expressions"_string };
        }

        if (reader.matches_number()) {
            // FIXME: Nicer float conversion.
            // FIXME: Check if followed by whitespace or any of (, ), <, >, [, ], {, }, /, %.
            //        Currently, this incorrectly accepts `4add` as `4 add`.
            //        (I think technically `4add` should be an identifier? But since this subset supports no
            //        identifiers, that won't happen in practice. We should reject it though, instead of accepting it
            //        as `4 add`.)
            char const* start = reinterpret_cast<char const*>(reader.bytes().slice(reader.offset()).data());
            char* endptr;
            float value = strtof(start, &endptr);
            reader.move_by(endptr - start);
            tokens.append({ OperatorType::Operand, value });
            continue;
        }

        if (Optional<OperatorType> op = parse_operator(reader); op.has_value()) {
            tokens.append({ op.value() });
            continue;
        }

        return Error { Error::Type::MalformedPDF, "PostScript unknown operator"_string };
    }
    VERIFY(reader.consume('}'));

    return tokens;
}

PDFErrorOr<NonnullRefPtr<PostScriptCalculatorFunction>>
PostScriptCalculatorFunction::create(Vector<Bound> domain, Optional<Vector<Bound>> range, NonnullRefPtr<StreamObject> stream)
{
    if (!range.has_value())
        return Error { Error::Type::MalformedPDF, "Function type 4 requires /Range" };

    Vector<NonnullOwnPtr<IfElse>> if_elses;
    Reader reader { stream->bytes() };
    auto tokens = TRY(parse_postscript_calculator_function(reader, if_elses));

    auto function = adopt_ref(*new PostScriptCalculatorFunction());
    function->m_domain = move(domain);
    function->m_range = move(range.value());
    function->m_tokens = move(tokens);
    function->m_if_elses = move(if_elses);
    return function;
}

PDFErrorOr<void> PostScriptCalculatorFunction::execute(Vector<Token> const& tokens, Stack& stack) const
{

    for (auto const& token : tokens) {
        switch (token.type) {
        case OperatorType::Operand:
            TRY(stack.push(token.value.get<float>()));
            break;
        case OperatorType::Abs:
            TRY(stack.push(fabsf(TRY(stack.pop()))));
            break;
        case OperatorType::Add: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(a + b));
            break;
        }
        case OperatorType::Atan: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(AK::to_degrees(atan2f(b, a))));
            break;
        }
        case OperatorType::Ceiling:
            TRY(stack.push(ceilf(TRY(stack.pop()))));
            break;
        case OperatorType::Cos:
            TRY(stack.push(cosf(AK::to_radians(TRY(stack.pop())))));
            break;
        case OperatorType::Cvi:
            TRY(stack.push((int)TRY(stack.pop())));
            break;
        case OperatorType::Cvr:
            TRY(stack.push(TRY(stack.pop())));
            break;
        case OperatorType::Div: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(a / b));
            break;
        }
        case OperatorType::Exp:
            TRY(stack.push(expf(TRY(stack.pop()))));
            break;
        case OperatorType::Floor:
            TRY(stack.push(floorf(TRY(stack.pop()))));
            break;
        case OperatorType::Idiv: {
            int b = (int)TRY(stack.pop());
            int a = (int)TRY(stack.pop());
            TRY(stack.push(a / b));
            break;
        }
        case OperatorType::Ln:
            TRY(stack.push(logf(TRY(stack.pop()))));
            break;
        case OperatorType::Log:
            TRY(stack.push(log10f(TRY(stack.pop()))));
            break;
        case OperatorType::Mod: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(fmodf(a, b)));
            break;
        }
        case OperatorType::Mul: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(a * b));
            break;
        }
        case OperatorType::Neg:
            TRY(stack.push(-TRY(stack.pop())));
            break;
        case OperatorType::Round:
            TRY(stack.push(roundf(TRY(stack.pop()))));
            break;
        case OperatorType::Sin:
            TRY(stack.push(sinf(AK::to_radians(TRY(stack.pop())))));
            break;
        case OperatorType::Sqrt:
            TRY(stack.push(sqrtf(TRY(stack.pop()))));
            break;
        case OperatorType::Sub: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(a - b));
            break;
        }
        case OperatorType::Truncate:
            TRY(stack.push(truncf(TRY(stack.pop()))));
            break;
        case OperatorType::And: {
            int b = (int)TRY(stack.pop());
            int a = (int)TRY(stack.pop());
            TRY(stack.push(a & b));
            break;
        }
        case OperatorType::Bitshift: {
            int b = (int)TRY(stack.pop());
            int a = (int)TRY(stack.pop());
            if (b >= 0)
                TRY(stack.push(a << b));
            else
                TRY(stack.push(a >> -b));
            break;
        }
        case OperatorType::Eq: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(a == b ? 1.0f : 0.0f));
            break;
        }
        case OperatorType::False:
            TRY(stack.push(0.0f));
            break;
        case OperatorType::Ge: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(a >= b ? 1.0f : 0.0f));
            break;
        }
        case OperatorType::Gt: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(a > b ? 1.0f : 0.0f));
            break;
        }
        case OperatorType::Le: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(a <= b ? 1.0f : 0.0f));
            break;
        }
        case OperatorType::Lt: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(a < b ? 1.0f : 0.0f));
            break;
        }
        case OperatorType::Ne: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(a != b ? 1.0f : 0.0f));
            break;
        }
        case OperatorType::Not: {
            TRY(stack.push(TRY(stack.pop()) == 0.0f ? 1.0f : 0.0f));
            break;
        }
        case OperatorType::Or: {
            int b = (int)TRY(stack.pop());
            int a = (int)TRY(stack.pop());
            TRY(stack.push(a | b));
            break;
        }
        case OperatorType::True:
            TRY(stack.push(1.0f));
            break;
        case OperatorType::Xor: {
            int b = (int)TRY(stack.pop());
            int a = (int)TRY(stack.pop());
            TRY(stack.push(a ^ b));
            break;
        }
        case OperatorType::If: {
            auto const& if_else = m_if_elses[token.value.get<int>()];
            VERIFY(if_else->if_false.is_empty());
            if (TRY(stack.pop()) != 0.0f)
                TRY(execute(if_else->if_true, stack));
            break;
        }
        case OperatorType::IfElse: {
            auto const& if_else = m_if_elses[token.value.get<int>()];
            if (TRY(stack.pop()) != 0.0f)
                TRY(execute(if_else->if_true, stack));
            else
                TRY(execute(if_else->if_false, stack));
            break;
        }
        case OperatorType::Copy: {
            int n = (int)TRY(stack.pop());
            if (n < 0)
                return Error { Error::Type::RenderingUnsupported, "PostScript copy with negative argument"_string };
            if ((size_t)n > stack.top)
                return Error { Error::Type::RenderingUnsupported, "PostScript copy with argument larger than stack"_string };
            for (int i = 0; i < n; ++i)
                TRY(stack.push(stack.stack[stack.top - n]));
            break;
        }
        case OperatorType::Dup:
            TRY(stack.push(stack.stack[stack.top - 1]));
            break;
        case OperatorType::Exch: {
            float b = TRY(stack.pop());
            float a = TRY(stack.pop());
            TRY(stack.push(b));
            TRY(stack.push(a));
            break;
        }
        case OperatorType::Index: {
            int i = (int)TRY(stack.pop());
            if (i < 0)
                return Error { Error::Type::RenderingUnsupported, "PostScript index with negative argument"_string };
            if ((size_t)i >= stack.top)
                return Error { Error::Type::RenderingUnsupported, "PostScript index with argument larger than stack"_string };
            TRY(stack.push(stack.stack[stack.top - 1 - i]));
            break;
        }
        case OperatorType::Pop:
            TRY(stack.pop());
            break;
        case OperatorType::Roll: {
            int j = (int)TRY(stack.pop());
            int n = (int)TRY(stack.pop());
            if (n < 0)
                return Error { Error::Type::RenderingUnsupported, "PostScript roll with negative argument"_string };
            if ((size_t)n > stack.top)
                return Error { Error::Type::RenderingUnsupported, "PostScript roll with argument larger than stack"_string };
            if (j < 0)
                j += n;
            if (j < 0)
                return Error { Error::Type::RenderingUnsupported, "PostScript roll with negative argument"_string };
            if (j > n)
                return Error { Error::Type::RenderingUnsupported, "PostScript roll with argument larger than stack"_string };
            // http://pointer-overloading.blogspot.com/2013/09/algorithms-rotating-one-dimensional.html
            auto elements = stack.stack.span().slice(stack.top - n, n);
            elements.reverse();
            elements.slice(0, j).reverse();
            elements.slice(j).reverse();
            break;
        }
        }
    }

    return {};
}

PDFErrorOr<ReadonlySpan<float>> PostScriptCalculatorFunction::evaluate(ReadonlySpan<float> xs) const
{
    if (xs.size() != m_domain.size())
        return Error { Error::Type::MalformedPDF, "Function argument size does not match domain size" };

    Stack stack;
    for (size_t i = 0; i < xs.size(); ++i)
        TRY(stack.push(clamp(xs[i], m_domain[i].lower, m_domain[i].upper)));

    TRY(execute(m_tokens, stack));

    if (stack.top != m_range.size())
        return Error { Error::Type::MalformedPDF, "Postscript result size does not match range size"_string };

    m_result.resize(stack.top);
    for (size_t i = 0; i < stack.top; ++i)
        m_result[i] = clamp(stack.stack[i], m_range[i].lower, m_range[i].upper);
    return m_result;
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
        // FIXME: spec is not clear on if this should work with a StreamObject.
        return StitchingFunction::create(document, move(domain), move(optional_range), function_dict);
    case 4:
        if (!object->is<StreamObject>())
            return Error { Error::Type::MalformedPDF, "Function type 4 requires stream object" };
        return PostScriptCalculatorFunction::create(move(domain), move(optional_range), object->cast<StreamObject>());
    default:
        dbgln("invalid function type {}", function_type);
        return Error(Error::Type::MalformedPDF, "Function has unkonwn type"_string);
    }
}

}
