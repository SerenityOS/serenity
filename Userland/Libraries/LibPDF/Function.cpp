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

class SampledFunction final : public Function {
public:
    virtual PDFErrorOr<ReadonlySpan<float>> evaluate(ReadonlySpan<float>) const override;
};

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

PDFErrorOr<Vector<PostScriptCalculatorFunction::Token>>
PostScriptCalculatorFunction::parse_postscript_calculator_function(Reader& reader, Vector<NonnullOwnPtr<IfElse>>& if_elses)
{
    // Assumes valid syntax.
    reader.consume_whitespace();
    if (!reader.consume('{'))
        return Error { Error::Type::MalformedPDF, "PostScript expected '{'" };

    Vector<PostScriptCalculatorFunction::Token> tokens;
    while (!reader.matches('}')) {
        if (reader.consume_whitespace())
            continue;

        if (reader.matches('{')) {
            auto if_true = TRY(parse_postscript_calculator_function(reader, if_elses));
            reader.consume_whitespace();
            if (reader.matches("if")) {
                reader.consume(2);
                tokens.append({ OperatorType::If, (int)if_elses.size() });
                if_elses.append(adopt_own(*new IfElse { move(if_true), {} }));
                continue;
            }

            VERIFY(reader.matches('{'));
            auto if_false = TRY(parse_postscript_calculator_function(reader, if_elses));
            reader.consume_whitespace();

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
            int j = -(int)TRY(stack.pop());
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
            elements.slice(0, j).reverse();
            elements.slice(j).reverse();
            elements.reverse();
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

    // FIXME: Does this need reversing?
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
        return adopt_ref(*new SampledFunction());
    // The spec has no entry for `1`.
    case 2:
        // FIXME: spec is not clear on if this should work with a StreamObject.
        return ExponentialInterpolationFunction::create(document, move(domain), move(optional_range), function_dict);
    case 3:
        return adopt_ref(*new StitchingFunction());
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
