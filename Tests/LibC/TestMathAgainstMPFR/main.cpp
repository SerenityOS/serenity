/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FloatingPoint.h>
#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <AK/Math/Constants.h>
#include <AK/NumberFormat.h>
#include <AK/String.h>
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

struct Options {
    bool use_serenity_libm = false;
    bool wide = false;
    bool verbose = false;
    Optional<StringView> core_math_path {};
};

ErrorOr<void> restart_with_preloaded_libm(Main::Arguments const& arguments)
{
    auto* self_path = realpath(arguments.argv[0], nullptr);
    if (!self_path)
        return Error::from_errno(errno);

    LexicalPath lexical_path { self_path };
    auto tmp = ByteString::formatted("{}/../lib/libm-for-math-test.so"sv, lexical_path.dirname());

    auto* libm_path = realpath(tmp.characters(), nullptr);
    if (!libm_path)
        return Error::from_errno(errno);

    if (!FileSystem::exists(StringView { libm_path, strlen(libm_path) }))
        return Error::from_string_literal("Unable to find custom libm");

    setenv("LD_PRELOAD", libm_path, 1);

    free(self_path);
    free(libm_path);

    Vector<StringView> args;
    args.extend(arguments.strings);
    args.remove_all_matching([](StringView name) { return name == "--use-serenity-libm"; });
    VERIFY(args.size() != arguments.strings.size());
    TRY(Core::System::exec(
        args[0],
        args,
        Core::System::SearchInPath::Yes));

    VERIFY_NOT_REACHED();
}

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

struct Range {
    double min {};
    double max {};
};

Array g_exp_perf_ranges = to_array<Range>({ { 0, 1 }, { -10, 10 }, { -745, 709 } });
Array g_log_perf_ranges = to_array<Range>({ { 0.01, 1 }, { 1, 100 }, { 1, 1e5 } });
Array g_lgamma_perf_ranges = to_array<Range>({ { 0, 1 }, { -10, 10 }, { 0, 1e5 } });

struct RangeWithCount {
    double min {};
    double max {};
    u32 count {};
};

Array g_exp_test_ranges = to_array<RangeWithCount>({ { -10, 10, 150 }, { -745, 709, 50 }, { -1e-10, 1e-10, 50 } });
Array g_lgamma_test_ranges = to_array<RangeWithCount>({ { 1e-300, 10, 1000 }, { -1, -5, 100 }, { 0, 1000, 50 } });
Array g_log_test_ranges = to_array<RangeWithCount>({ { 1e-300, 1, 100 }, { 1, 10, 100 }, { 10, 1e300, 50 } });

struct MathFunction {
    StringView name;
    double (*libc_function)(double);
    double (*reference_function)(double);

    ReadonlySpan<RangeWithCount> test_ranges {};
    ReadonlySpan<Range> perf_ranges {};
};

#define DEFINE_MATH_FUNC(fname)                        \
    MathFunction                                       \
    {                                                  \
        .name = #fname##sv,                            \
        .libc_function = &fname,                       \
        .reference_function = &CORE_MATH::ref_##fname, \
        .test_ranges = g_##fname##_test_ranges,        \
        .perf_ranges = g_##fname##_perf_ranges,        \
    }

Array g_functions = to_array({
    DEFINE_MATH_FUNC(lgamma),
    DEFINE_MATH_FUNC(exp),
    DEFINE_MATH_FUNC(log),
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
            worst_cases.append(value);
        }

        return worst_cases;
    }();

    if (result.is_error()) {
        warnln("Error while opening worst-cases file for {}:{}", function.name, result.error());
        return {};
    }

    return result.release_value();
}

Vector<double> sample_worst_cases(Vector<double> worst_cases)
{
    // FIXME: This should be a runtime option.
    static constexpr u32 TEST_SAMPLES = 1000;

    Vector<double> out;
    out.ensure_capacity(TEST_SAMPLES);

    auto step = static_cast<double>(worst_cases.size()) / TEST_SAMPLES;
    for (u32 i = 0; i < TEST_SAMPLES; ++i)
        out.unchecked_append(worst_cases[static_cast<u64>(i * step)]);

    return out;
}

Vector<double> generate_test_cases(Options const& options, MathFunction const& function)
{
    u64 count {};
    for (auto range : function.test_ranges)
        count += range.count;

    auto all_worst_cases = load_worst_cases(options, function);
    auto worst_cases = sample_worst_cases(move(all_worst_cases));

    u64 total_count = g_edge_cases.size() + count + worst_cases.size();

    Vector<double> test_cases;
    test_cases.ensure_capacity(total_count);

    test_cases.extend(worst_cases);
    test_cases.extend(g_edge_cases);

    for (auto range : function.test_ranges) {
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
        accuracy_add(r, value, function.libc_function(value), function.reference_function(value));

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

Vector<double> generate_range(MathFunction const& function)
{
    srand(0x12345678u);

    auto ranges = function.perf_ranges;

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
            dummy += function.libc_function(range[i]);
    }

    Array<double, PERF_ROUNDS> samples;
    for (u32 r = 0; r < PERF_ROUNDS; r++) {
        auto start = MonotonicTime::now();
        auto deadline = start + Duration::from_milliseconds(MIN_TIME_MS);
        u64 total_ops {};
        while (MonotonicTime::now() < deadline) {
            for (u32 i = 0; i < range.size(); i++)
                dummy += function.libc_function(range[i]);
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

PerfResult run_perf(MathFunction const& function)
{
    auto test_range = generate_range(function);

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
    parser.add_option(options.use_serenity_libm, "Use serenity's math functions instead of the system's libm.", "use-serenity-libm");
    parser.add_option(options.core_math_path, "Path to CORE-MATH root folder, used to extract hard-to-round cases", "core-math", 0, "PATH");
    parser.add_option(options.wide, "Use wide formatting", "format-wide");
    parser.add_option(options.verbose, "Verbose output", "verbose", 'v');
    parser.parse(arguments);

    if (options.use_serenity_libm) {
        TRY(restart_with_preloaded_libm(arguments));
        VERIFY_NOT_REACHED();
    }

    if (!options.core_math_path.has_value()) {
        warnln("Warning: CORE-MATH is not provided.");
        warnln("Please use --core-math path/to/core-math, otherwise the displayed accuracy may be overestimated.");
        warnln("CORE-MATH can be downloaded from https://gitlab.inria.fr/core-math/core-math/");
    }

    outln("Running libm benchmark...\n");
    u32 w = options.wide ? 22 : 10;  // width for Max ULP column
    u32 w2 = options.wide ? 24 : 12; // width for Mean ULP column
    outln("{:-14}{:>{}}{:>{}}{:>8}{:>8}{:>10}{:>14}{:>8}{:>8}",
        "Function", "Max ULP", w, "Mean ULP", w2, "% CR", "% FR",
        "Accuracy", "Ops/sec", "Perf+", "Total");

    u32 ruler_width = 72 + w + w2;
    outln("{}", MUST(String::repeated('-', ruler_width)));

    double log_sum = 0.0;
    int score_count = 0;

    for (auto const& function : g_functions) {
        AccuracyResult accuracy = run_accuracy(options, function);
        PerfResult perf = run_perf(function);

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
            outln("{:-14}  ^ input: {}  expected: {}  got: {}",
                ""sv, accuracy.worse_input, accuracy.worst_expected, accuracy.worst_computed);
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
