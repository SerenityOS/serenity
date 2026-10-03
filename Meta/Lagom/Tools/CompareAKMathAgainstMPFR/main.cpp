/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FloatingPoint.h>
#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <AK/Math.h>
#include <AK/NumberFormat.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/Time.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <float.h>
#include <math.h>

#include "reference_functions.h"

namespace {

struct Range {
    double min {};
    double max {};

    bool contains(double x) const
    {
        return x >= min && x <= max;
    }

    bool overlaps_with(Range other) const
    {
        return contains(other.min) || contains(other.max);
    }
};

struct Options {
    bool test_system_libm = false;
    bool wide = false;
    bool verbose = false;
    Optional<Range> accepted_range {};
    Optional<StringView> core_math_path {};
    Optional<u32> sample_core_math {};
    Optional<StringView> filter {};
};

Array g_edge_cases = to_array<double>({ // Denormals
    5e-324, -5e-324,
    2.2250738585072014e-308, -2.2250738585072014e-308,
    // Very small values (near zero but not zero)
    1e-300, -1e-300,
    1e-15, -1e-15,
    // Values near interesting points
    1.0, -1.0,
    0.5, -0.5,
    2.0, -2.0,
    10.0, -10.0,
    AK::Pi<double>, -AK::Pi<double>, AK::Pi<double> / 2., -AK::Pi<double> / 2,
    AK::E<double>,
    // Near 1
    1.0 - 2e-52, 1.0 + 2e-52,
    // Large values
    1e100, -1e100,
    1.7976931348623157e+308, -1.7976931348623157e+308,
    AK::Infinity<double>, -AK::Infinity<double>,
    AK::NaN<double>, -AK::NaN<double> });

Array g_exp_perf_ranges = to_array<Range>({ { 0, 1 }, { -10, 10 }, { -745, 709 } });
Array g_log_perf_ranges = to_array<Range>({ { 0.01, 1 }, { 1, 100 }, { 1, 1e5 } });

Array g_lgamma_perf_ranges = to_array<Range>({ { -100, 0 }, { 0.001, 1 }, { 1, 100 }, { 1, 1e5 } });
Array g_tgamma_perf_ranges = g_lgamma_perf_ranges;

Array g_hyperbolic_perf_ranges = to_array<Range>({ { -1, 1 }, { -10, 10 }, { 100, 1e5 } });
Array g_sinh_perf_ranges = g_hyperbolic_perf_ranges;
Array g_cosh_perf_ranges = g_hyperbolic_perf_ranges;
Array g_tanh_perf_ranges = g_hyperbolic_perf_ranges;

struct RangeWithCount : Range {
    u32 count {};
};

Array g_exp_test_ranges = to_array<RangeWithCount>({ { { -10, 10 }, 150 }, { { -745, 709 }, 50 }, { { -1e-10, 1e-10 }, 50 } });
Array g_log_test_ranges = to_array<RangeWithCount>({ { { 1e-300, 1 }, 100 }, { { 1, 10 }, 100 }, { { 10, 1e300 }, 50 } });

Array g_lgamma_test_ranges = to_array<RangeWithCount>({ { { -100, 0 }, 150 }, { { 1e-300, 1 }, 100 }, { { 1, 10 }, 100 }, { { 10, 1e300 }, 50 } });
Array g_tgamma_test_ranges = g_lgamma_test_ranges;

Array g_hyperbolic_test_ranges = to_array<RangeWithCount>({ { { -10, 10 }, 150 }, { { -700, 700 }, 50 }, { { -1e-10, 1e-10 }, 50 } });
Array g_cosh_test_ranges = g_hyperbolic_test_ranges;
Array g_sinh_test_ranges = g_hyperbolic_test_ranges;
Array g_tanh_test_ranges = g_hyperbolic_test_ranges;

struct MathFunction {
    StringView name;
    double (*ak_function)(double);
    double (*libc_function)(double);
    double (*reference_function)(double);
    ReadonlySpan<RangeWithCount> test_ranges {};
    ReadonlySpan<Range> perf_ranges {};

    double (*tested_function)(double) = ak_function;
};

#define DEFINE_MATH_FUNC(fname)                  \
    MathFunction                                 \
    {                                            \
        .name = #fname##sv,                      \
        .ak_function = &AK::fname,               \
        .libc_function = &fname,                 \
        .reference_function = &CORE_MATH::fname, \
        .test_ranges = g_##fname##_test_ranges,  \
        .perf_ranges = g_##fname##_perf_ranges,  \
    }

Array g_functions = to_array({
    DEFINE_MATH_FUNC(exp),
    DEFINE_MATH_FUNC(log),

    DEFINE_MATH_FUNC(lgamma),
    DEFINE_MATH_FUNC(tgamma),

    DEFINE_MATH_FUNC(cosh),
    DEFINE_MATH_FUNC(sinh),
    DEFINE_MATH_FUNC(tanh),
});

double ulp_of(double x)
{
    if (!isfinite(x) || x == 0.0)
        return 0.0;
    auto extractor = FloatExtractor<double>::from_float(x);
    if (extractor.exponent == 0)
        return 5e-324;
    return ldexp(1.0, extractor.exponent - 1075);
}

double ulp_error(double computed, double expected)
{
    if (isnan(expected) && isnan(computed))
        return 0.0;
    if (isnan(expected) || isnan(computed))
        return AK::Infinity<double>;
    if (expected == computed)
        return 0.0;
    if (!isfinite(expected) || !isfinite(computed)) {
        // Different sign infinities: infinite error.
        if (!isfinite(expected) && !isfinite(computed))
            return AK::Infinity<double>;
        // Treat +/-Infinity as +/-DBL_MAX + 1 ULP.
        double inf = isfinite(expected) ? computed : expected;
        double finite_val = isfinite(expected) ? expected : computed;
        double edge = inf > 0 ? DBL_MAX : -DBL_MAX;
        return fabs(finite_val - edge) / ulp_of(edge) + 1;
    }
    if (expected == 0.0) {
        if (computed == 0.0)
            return 0.0;
        return fabs(computed) / 5e-324;
    }
    return fabs(computed - expected) / ulp_of(expected);
}

struct AccuracyResult {
    int count {};
    double max_ulp {};
    double mean_ulp {};
    double correctly_rounded {};
    double faithfully_rounded {};
    double worse_input {};
    double worst_expected {};
    double worst_computed {};
};

Vector<double> load_worst_cases(Options const& options, MathFunction const& function)
{
    if (!options.core_math_path.has_value())
        return {};

    auto result = [&] -> ErrorOr<Vector<double>> {
        auto worst_case_path = ByteString::formatted("{}/src/binary64/{}/{}.wc", *options.core_math_path, function.name, function.name);
        auto file = TRY(Core::File::open(worst_case_path, Core::File::OpenMode::Read));
        auto buffered_file = TRY(Core::InputBufferedFile::create(move(file)));

        Vector<double> worst_cases;

        Array<u8, 1024> line_buffer {};
        while (TRY(buffered_file->can_read_line())) {
            auto line = TRY(buffered_file->read_line(line_buffer));
            if (line.starts_with('#'))
                continue;
            VERIFY(line.length() < line_buffer.size());
            line_buffer[line.length()] = '\0';
            auto value = strtod(line.characters_without_null_termination(), nullptr);

            if (!options.accepted_range.has_value() || options.accepted_range->contains(value))
                worst_cases.append(value);
        }

        return worst_cases;
    }();

    if (result.is_error()) {
        warnln("Error while opening worst-cases file for {}: {}", function.name, result.error());
        return {};
    }

    return result.release_value();
}

Vector<double> sample_worst_cases(Options const& options, Vector<double> worst_cases)
{
    if (!options.sample_core_math.has_value())
        return worst_cases;

    auto number_of_sampled_cases = min(worst_cases.size(), *options.sample_core_math);

    Vector<double> out;
    out.ensure_capacity(number_of_sampled_cases);

    auto step = max(1, static_cast<double>(worst_cases.size()) / *options.sample_core_math);
    for (u32 i = 0; i < number_of_sampled_cases; ++i)
        out.unchecked_append(worst_cases[static_cast<u64>(i * step)]);

    return out;
}

Vector<double> filter_edge_cases(Options const& options, Span<double> edge_cases)
{
    Vector<double> out;
    for (auto value : edge_cases) {
        if (!options.accepted_range.has_value() || options.accepted_range->contains(value))
            out.append(value);
    }
    return out;
}

template<OneOf<Range, RangeWithCount> R>
Vector<R> adjust_ranges(Options const& options, ReadonlySpan<R> ranges)
{
    Vector<R> out {};
    for (auto range : ranges) {
        if (!options.accepted_range.has_value()) {
            out.append(range);
        } else if (options.accepted_range->overlaps_with(range)) {
            range.min = max(range.min, options.accepted_range->min);
            range.max = min(range.max, options.accepted_range->max);
            out.append(range);
        }
    }
    return out;
}

Vector<double> generate_test_cases(Options const& options, MathFunction const& function)
{
    auto all_worst_cases = load_worst_cases(options, function);
    auto worst_cases = sample_worst_cases(options, move(all_worst_cases));

    auto filtered_edge_cases = filter_edge_cases(options, g_edge_cases);
    auto adjusted_test_ranges = adjust_ranges(options, function.test_ranges);

    u64 count {};
    for (auto range : adjusted_test_ranges)
        count += range.count;

    u64 total_count = filtered_edge_cases.size() + count + worst_cases.size();

    Vector<double> test_cases;
    test_cases.ensure_capacity(total_count);

    test_cases.extend(worst_cases);
    test_cases.extend(filtered_edge_cases);

    for (auto range : adjusted_test_ranges) {
        for (u32 i = 0; i < range.count; i++) {
            double t = range.min + (range.max - range.min) * i / max(count - 1, 1);
            test_cases.append(t);
        }
    }

    VERIFY(test_cases.size() == total_count);

    return test_cases;
}

void accuracy_add(AccuracyResult& r, double input, double computed, double expected)
{
    double ulp = ulp_error(computed, expected);
    double capped = ulp < 1e15 ? ulp : 1e15;
    r.mean_ulp += capped; // sum; divided by count later
    if (ulp <= 0.5)
        r.correctly_rounded += 1; // count; converted later
    if (ulp <= 1.0)
        r.faithfully_rounded += 1;
    if (ulp > r.max_ulp || (isinf(ulp) && !isinf(r.max_ulp))) {
        r.max_ulp = ulp;
        r.worst_expected = expected;
        r.worst_computed = computed;
        r.worse_input = input;
    }
    r.count++;
}

AccuracyResult run_accuracy(Options const& options, MathFunction const& function)
{
    AccuracyResult r {};

    auto test_cases = generate_test_cases(options, function);

    for (auto value : test_cases)
        accuracy_add(r, value, function.tested_function(value), function.reference_function(value));

    if (r.count > 0) {
        r.correctly_rounded = r.correctly_rounded / r.count * 100.0;
        r.faithfully_rounded = r.faithfully_rounded / r.count * 100.0;
        r.mean_ulp = r.mean_ulp / r.count;
    }
    return r;
}

struct PerfResult {
    double average_ops_per_second {};
    double bonus_points {};
};

constexpr u32 SAMPLE_PER_RANGE = 10000;
constexpr u32 MIN_TIME_MS = 10;
constexpr u32 PERF_ROUNDS = 3;
constexpr u32 REFERENCE_RATE = 1000e6;

Vector<double> generate_range(Options const& options, MathFunction const& function)
{
    srand(0x12345678u);

    auto ranges = adjust_ranges(options, function.perf_ranges);

    u64 total_size = ranges.size() * SAMPLE_PER_RANGE;
    Vector<double> test_range {};
    test_range.resize(total_size);

    for (u32 i = 0; i < ranges.size(); i++) {
        for (u32 j = 0; j < SAMPLE_PER_RANGE; j++) {
            test_range[i * SAMPLE_PER_RANGE + j] = ranges[i].min + (rand() / static_cast<double>(RAND_MAX)) * (ranges[i].max - ranges[i].min);
        }
    }

    return test_range;
}

double measure(MathFunction const& function, Span<double> range)
{
    // Warm-up
    double dummy = 0;
    for (int w = 0; w < 5; w++) {
        for (u32 i = 0; i < range.size(); i++)
            dummy += function.tested_function(range[i]);
    }

    Array<double, PERF_ROUNDS> samples;
    for (u32 r = 0; r < PERF_ROUNDS; r++) {
        auto start = MonotonicTime::now();
        auto deadline = start + Duration::from_milliseconds(MIN_TIME_MS);
        u64 total_ops {};
        while (MonotonicTime::now() < deadline) {
            for (u32 i = 0; i < range.size(); i++)
                dummy += function.tested_function(range[i]);
            total_ops += range.size();
        }
        samples[r] = 1e6 * total_ops / (MonotonicTime::now() - start).to_microseconds();
    }

    // FIXME: Do we have a helper to compute the median?
    if (samples[0] > samples[1]) {
        swap(samples[0], samples[1]);
    }
    if (samples[1] > samples[2]) {
        swap(samples[1], samples[2]);
    }
    if (samples[0] > samples[1]) {
        swap(samples[0], samples[1]);
    }
    AK::taint_for_optimizer(dummy);
    return samples[1];
}

PerfResult run_perf(Options const& options, MathFunction const& function)
{
    auto test_range = generate_range(options, function);

    PerfResult r {};
    r.average_ops_per_second = measure(function, test_range);
    r.bonus_points = r.average_ops_per_second / REFERENCE_RATE * 20.0;
    clamp(r.bonus_points, 0.0, 20.0);
    return r;
}

}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser parser;
    Options options;
    Optional<StringView> range {};
    parser.add_option(options.test_system_libm, "Test the system's libm instead of AK's math functions.", "test-system-libm");
    parser.add_option(options.core_math_path, "Path to CORE-MATH root folder, used to extract hard-to-round cases", "core-math", 0, "PATH");
    parser.add_option(options.sample_core_math, "Only use N samples from CORE-MATH", "sample-core-math", 0, "N");
    parser.add_option(options.filter, "Only test math functions whose names include FILTER", "filter", 'f', "FILTER");
    parser.add_option(range, "Only test math function inside the given range. RANGE should have the following shape \"min:max\"", "range", 'r', "RANGE");
    parser.add_option(options.wide, "Use wide formatting", "format-wide");
    parser.add_option(options.verbose, "Verbose output", "verbose", 'v');
    parser.parse(arguments);

    if (!options.core_math_path.has_value()) {
        warnln("Warning: CORE-MATH is not provided.");
        warnln("Please use the --core-math option to provide more test cases.");
        warnln("CORE-MATH can be downloaded from https://gitlab.inria.fr/core-math/core-math/");

        if (options.sample_core_math.has_value())
            return Error::from_string_literal("Invalid argument: --sample-core-math used without --core-math");
    }

    if (range.has_value()) {
        auto splits = range->split_view(':');
        if (splits.size() != 2)
            return Error::from_string_literal("Invalid argument for RANGE");
        auto maybe_value = AK::StringUtils::convert_to_floating_point<double>(splits[0]);
        if (!maybe_value.has_value())
            return Error::from_string_literal("Invalid argument for min in RANGE");
        auto range_min = maybe_value.value();
        maybe_value = AK::StringUtils::convert_to_floating_point<double>(splits[1]);
        if (!maybe_value.has_value())
            return Error::from_string_literal("Invalid argument for max in RANGE");
        options.accepted_range = { range_min, maybe_value.value() };

        if (options.accepted_range->min > options.accepted_range->max)
            return Error::from_string_literal("Invalid argument for RANGE, min > max");
    }

    out("Running math benchmark for {}", options.test_system_libm ? "the system libm" : "AK");
    if (options.accepted_range.has_value()) {
        // FIXME: Print values using the scientific notation.
        out(", with values limited to [{}, {}]", options.accepted_range->min, options.accepted_range->max);
    }
    outln("...");

    if (options.sample_core_math.has_value())
        outln("Only using {} cases from CORE-MATH worst cases.", *options.sample_core_math);

    outln();

    u32 w = options.wide ? 22 : 10;  // width for Max ULP column
    u32 w2 = options.wide ? 24 : 12; // width for Mean ULP column
    outln("{:-14}{:>{}}{:>{}}{:>8}{:>8}{:>10}{:>14}{:>8}{:>8}",
        "Function", "Max ULP", w, "Mean ULP", w2, "% CR", "% FR",
        "Accuracy", "Ops/sec", "Perf+", "Total");

    u32 ruler_width = 72 + w + w2;
    outln("{}", MUST(String::repeated('-', ruler_width)));

    // FIXME: Don't forget to update this when adding support for testing float functions.
    CORE_MATH::set_exponent_limits();

    double log_sum = 0.0;
    int score_count = 0;

    for (auto& function : g_functions) {
        if (options.filter.has_value() && !function.name.contains(*options.filter))
            continue;

        if (options.test_system_libm)
            function.tested_function = function.libc_function;

        AccuracyResult accuracy = run_accuracy(options, function);
        PerfResult perf = run_perf(options, function);

        double accuracy_score = 100.0 / (1.0 + accuracy.mean_ulp);
        double total = accuracy_score + perf.bonus_points;

        out("{:-14}"sv, function.name);
        // FIXME: Add support for exponent notation and use it here.
        //        This should also allow us to get rid of the wide mode.
        out("{:>{}.2f}{:>{}.4f}"sv, accuracy.max_ulp, w, accuracy.mean_ulp, w2);
        out("{:>7.1f}%{:>7.1f}%"sv, accuracy.correctly_rounded, accuracy.faithfully_rounded);
        out("{:>10.1f}"sv, accuracy_score);
        out("{:>14}"sv, human_readable_quantity(perf.average_ops_per_second, AK::HumanReadableBasedOn::Base10, "/s"sv));
        out("{:>8.1f}{:>8.1f}"sv, perf.bonus_points, total);
        outln("");

        if (options.verbose && accuracy.max_ulp > 0) {
            outln("{:-14}  ^ input: {}  expected: {}  got: {} - ULP at expected {}",
                ""sv, accuracy.worse_input, accuracy.worst_expected, accuracy.worst_computed, ulp_of(accuracy.worst_expected));
        }

        if (total > 0) {
            log_sum += log(total);
            score_count++;
        }
    }

    outln("{}", MUST(String::repeated('=', ruler_width)));
    outln("\nOverall Score: {:.1f}\n", score_count > 0 ? exp(log_sum / score_count) : 0.0);

    return 0;
}
