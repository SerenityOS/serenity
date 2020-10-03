/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/Utf8View.h>
#include <Kernel/API/Syscall.h>
#include <alloca.h>
#include <assert.h>
#include <ctype.h>
#include <errno.h>
#include <signal.h>
#include <spawn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/internals.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

static void strtons(const char* str, char** endptr)
{
    assert(endptr);
    char* ptr = const_cast<char*>(str);
    while (isspace(*ptr)) {
        ptr += 1;
    }
    *endptr = ptr;
}

enum Sign {
    Negative,
    Positive,
};

static Sign strtosign(const char* str, char** endptr)
{
    assert(endptr);
    if (*str == '+') {
        *endptr = const_cast<char*>(str + 1);
        return Sign::Positive;
    } else if (*str == '-') {
        *endptr = const_cast<char*>(str + 1);
        return Sign::Negative;
    } else {
        *endptr = const_cast<char*>(str);
        return Sign::Positive;
    }
}

enum DigitConsumeDecision {
    Consumed,
    PosOverflow,
    NegOverflow,
    Invalid,
};

template<typename T, T min_value, T max_value>
class NumParser {
    AK_MAKE_NONMOVABLE(NumParser);

public:
    NumParser(Sign sign, int base)
        : m_base(base)
        , m_num(0)
        , m_sign(sign)
    {
        m_cutoff = positive() ? (max_value / base) : (min_value / base);
        m_max_digit_after_cutoff = positive() ? (max_value % base) : (min_value % base);
    }

    int parse_digit(char ch)
    {
        int digit;
        if (isdigit(ch))
            digit = ch - '0';
        else if (islower(ch))
            digit = ch - ('a' - 10);
        else if (isupper(ch))
            digit = ch - ('A' - 10);
        else
            return -1;

        if (static_cast<T>(digit) >= m_base)
            return -1;

        return digit;
    }

    DigitConsumeDecision consume(char ch)
    {
        int digit = parse_digit(ch);
        if (digit == -1)
            return DigitConsumeDecision::Invalid;

        if (!can_append_digit(digit)) {
            if (m_sign != Sign::Negative) {
                return DigitConsumeDecision::PosOverflow;
            } else {
                return DigitConsumeDecision::NegOverflow;
            }
        }

        m_num *= m_base;
        m_num += positive() ? digit : -digit;

        return DigitConsumeDecision::Consumed;
    }

    T number() const { return m_num; };

private:
    bool can_append_digit(int digit)
    {
        const bool is_below_cutoff = positive() ? (m_num < m_cutoff) : (m_num > m_cutoff);

        if (is_below_cutoff) {
            return true;
        } else {
            return m_num == m_cutoff && digit < m_max_digit_after_cutoff;
        }
    }

    bool positive() const
    {
        return m_sign != Sign::Negative;
    }

    const T m_base;
    T m_num;
    T m_cutoff;
    int m_max_digit_after_cutoff;
    Sign m_sign;
};

typedef NumParser<int, INT_MIN, INT_MAX> IntParser;
typedef NumParser<long long, LONG_LONG_MIN, LONG_LONG_MAX> LongLongParser;
typedef NumParser<unsigned long long, 0ULL, ULONG_LONG_MAX> ULongLongParser;

static bool is_either(char* str, int offset, char lower, char upper)
{
    char ch = *(str + offset);
    return ch == lower || ch == upper;
}

__attribute__((warn_unused_result)) int __generate_unique_filename(char* pattern)
{
    size_t length = strlen(pattern);

    if (length < 6 || memcmp(pattern + length - 6, "XXXXXX", 6)) {
        errno = EINVAL;
        return -1;
    }

    size_t start = length - 6;

    static constexpr char random_characters[] = "abcdefghijklmnopqrstuvwxyz0123456789";

    for (int attempt = 0; attempt < 100; ++attempt) {
        for (int i = 0; i < 6; ++i)
            pattern[start + i] = random_characters[(rand() % sizeof(random_characters))];
        struct stat st;
        int rc = lstat(pattern, &st);
        if (rc < 0 && errno == ENOENT)
            return 0;
    }
    errno = EEXIST;
    return -1;
}

extern "C" {

void exit(int status)
{
    __cxa_finalize(nullptr);

    if (getenv("LIBC_DUMP_MALLOC_STATS"))
        serenity_dump_malloc_stats();

    extern void _fini();
    _fini();
    fflush(stdout);
    fflush(stderr);
    _exit(status);
}

static void __atexit_to_cxa_atexit(void* handler)
{
    reinterpret_cast<void (*)()>(handler)();
}

int atexit(void (*handler)())
{
    return __cxa_atexit(__atexit_to_cxa_atexit, (void*)handler, nullptr);
}

void abort()
{
    // For starters, send ourselves a SIGABRT.
    raise(SIGABRT);
    // If that didn't kill us, try harder.
    raise(SIGKILL);
    _exit(127);
}

static HashTable<const char*> s_malloced_environment_variables;

static void free_environment_variable_if_needed(const char* var)
{
    if (!s_malloced_environment_variables.contains(var))
        return;
    free(const_cast<char*>(var));
    s_malloced_environment_variables.remove(var);
}

char* getenv(const char* name)
{
    size_t vl = strlen(name);
    for (size_t i = 0; environ[i]; ++i) {
        const char* decl = environ[i];
        char* eq = strchr(decl, '=');
        if (!eq)
            continue;
        size_t varLength = eq - decl;
        if (vl != varLength)
            continue;
        if (strncmp(decl, name, varLength) == 0) {
            return eq + 1;
        }
    }
    return nullptr;
}

int unsetenv(const char* name)
{
    auto new_var_len = strlen(name);
    size_t environ_size = 0;
    int skip = -1;

    for (; environ[environ_size]; ++environ_size) {
        char* old_var = environ[environ_size];
        char* old_eq = strchr(old_var, '=');
        ASSERT(old_eq);
        size_t old_var_len = old_eq - old_var;

        if (new_var_len != old_var_len)
            continue; // can't match

        if (strncmp(name, old_var, new_var_len) == 0)
            skip = environ_size;
    }

    if (skip == -1)
        return 0; // not found: no failure.

    // Shuffle the existing array down by one.
    memmove(&environ[skip], &environ[skip + 1], ((environ_size - 1) - skip) * sizeof(environ[0]));
    environ[environ_size - 1] = nullptr;

    free_environment_variable_if_needed(name);
    return 0;
}

int setenv(const char* name, const char* value, int overwrite)
{
    if (!overwrite && getenv(name))
        return 0;
    auto length = strlen(name) + strlen(value) + 2;
    auto* var = (char*)malloc(length);
    snprintf(var, length, "%s=%s", name, value);
    s_malloced_environment_variables.set(var);
    return putenv(var);
}

int putenv(char* new_var)
{
    char* new_eq = strchr(new_var, '=');

    if (!new_eq)
        return unsetenv(new_var);

    auto new_var_len = new_eq - new_var;
    int environ_size = 0;
    for (; environ[environ_size]; ++environ_size) {
        char* old_var = environ[environ_size];
        char* old_eq = strchr(old_var, '=');
        ASSERT(old_eq);
        auto old_var_len = old_eq - old_var;

        if (new_var_len != old_var_len)
            continue; // can't match

        if (strncmp(new_var, old_var, new_var_len) == 0) {
            free_environment_variable_if_needed(old_var);
            environ[environ_size] = new_var;
            return 0;
        }
    }

    // At this point, we need to append the new var.
    // 2 here: one for the new var, one for the sentinel value.
    char** new_environ = (char**)malloc((environ_size + 2) * sizeof(char*));
    if (new_environ == nullptr) {
        errno = ENOMEM;
        return -1;
    }

    for (int i = 0; environ[i]; ++i) {
        new_environ[i] = environ[i];
    }

    new_environ[environ_size] = new_var;
    new_environ[environ_size + 1] = nullptr;

    // swap new and old
    // note that the initial environ is not heap allocated!
    extern bool __environ_is_malloced;
    if (__environ_is_malloced)
        free(environ);
    __environ_is_malloced = true;
    environ = new_environ;
    return 0;
}

double strtod(const char* str, char** endptr)
{
    // Parse spaces, sign, and base
    char* parse_ptr = const_cast<char*>(str);
    strtons(parse_ptr, &parse_ptr);
    const Sign sign = strtosign(parse_ptr, &parse_ptr);

    // Parse inf/nan, if applicable.
    if (is_either(parse_ptr, 0, 'i', 'I')) {
        if (is_either(parse_ptr, 1, 'n', 'N')) {
            if (is_either(parse_ptr, 2, 'f', 'F')) {
                parse_ptr += 3;
                if (is_either(parse_ptr, 0, 'i', 'I')) {
                    if (is_either(parse_ptr, 1, 'n', 'N')) {
                        if (is_either(parse_ptr, 2, 'i', 'I')) {
                            if (is_either(parse_ptr, 3, 't', 'T')) {
                                if (is_either(parse_ptr, 4, 'y', 'Y')) {
                                    parse_ptr += 5;
                                }
                            }
                        }
                    }
                }
                if (endptr)
                    *endptr = parse_ptr;
                // Don't set errno to ERANGE here:
                // The caller may want to distinguish between "input is
                // literal infinity" and "input is not literal infinity
                // but did not fit into double".
                if (sign != Sign::Negative) {
                    return __builtin_huge_val();
                } else {
                    return -__builtin_huge_val();
                }
            }
        }
    }
    if (is_either(parse_ptr, 0, 'n', 'N')) {
        if (is_either(parse_ptr, 1, 'a', 'A')) {
            if (is_either(parse_ptr, 2, 'n', 'N')) {
                if (endptr)
                    *endptr = parse_ptr + 3;
                errno = ERANGE;
                if (sign != Sign::Negative) {
                    return __builtin_nan("");
                } else {
                    return -__builtin_nan("");
                }
            }
        }
    }

    // Parse base
    char exponent_lower;
    char exponent_upper;
    int base = 10;
    if (*parse_ptr == '0') {
        const char base_ch = *(parse_ptr + 1);
        if (base_ch == 'x' || base_ch == 'X') {
            base = 16;
            parse_ptr += 2;
        }
    }

    if (base == 10) {
        exponent_lower = 'e';
        exponent_upper = 'E';
    } else {
        exponent_lower = 'p';
        exponent_upper = 'P';
    }

    // Parse "digits", possibly keeping track of the exponent offset.
    // We parse the most significant digits and the position in the
    // base-`base` representation separately. This allows us to handle
    // numbers like `0.0000000000000000000000000000000000001234` or
    // `1234567890123456789012345678901234567890` with ease.
    LongLongParser digits { sign, base };
    bool digits_usable = false;
    bool should_continue = true;
    bool digits_overflow = false;
    bool after_decimal = false;
    int exponent = 0;
    do {
        if (!after_decimal && *parse_ptr == '.') {
            after_decimal = true;
            parse_ptr += 1;
            continue;
        }

        bool is_a_digit;
        if (digits_overflow) {
            is_a_digit = digits.parse_digit(*parse_ptr) != -1;
        } else {
            DigitConsumeDecision decision = digits.consume(*parse_ptr);
            switch (decision) {
            case DigitConsumeDecision::Consumed:
                is_a_digit = true;
                // The very first actual digit must pass here:
                digits_usable = true;
                break;
            case DigitConsumeDecision::PosOverflow:
            case DigitConsumeDecision::NegOverflow:
                is_a_digit = true;
                digits_overflow = true;
                break;
            case DigitConsumeDecision::Invalid:
                is_a_digit = false;
                break;
            default:
                ASSERT_NOT_REACHED();
            }
        }

        if (is_a_digit) {
            exponent -= after_decimal ? 1 : 0;
            exponent += digits_overflow ? 1 : 0;
        }

        should_continue = is_a_digit;
        parse_ptr += should_continue;
    } while (should_continue);

    if (!digits_usable) {
        // No actual number value available.
        if (endptr)
            *endptr = const_cast<char*>(str);
        return 0.0;
    }

    // Parse exponent.
    // We already know the next character is not a digit in the current base,
    // nor a valid decimal point. Check whether it's an exponent sign.
    if (*parse_ptr == exponent_lower || *parse_ptr == exponent_upper) {
        // Need to keep the old parse_ptr around, in case of rollback.
        char* old_parse_ptr = parse_ptr;
        parse_ptr += 1;

        // Can't use atol or strtol here: Must accept excessive exponents,
        // even exponents >64 bits.
        Sign exponent_sign = strtosign(parse_ptr, &parse_ptr);
        IntParser exponent_parser { exponent_sign, base };
        bool exponent_usable = false;
        bool exponent_overflow = false;
        should_continue = true;
        do {
            bool is_a_digit;
            if (exponent_overflow) {
                is_a_digit = exponent_parser.parse_digit(*parse_ptr) != -1;
            } else {
                DigitConsumeDecision decision = exponent_parser.consume(*parse_ptr);
                switch (decision) {
                case DigitConsumeDecision::Consumed:
                    is_a_digit = true;
                    // The very first actual digit must pass here:
                    exponent_usable = true;
                    break;
                case DigitConsumeDecision::PosOverflow:
                case DigitConsumeDecision::NegOverflow:
                    is_a_digit = true;
                    exponent_overflow = true;
                    break;
                case DigitConsumeDecision::Invalid:
                    is_a_digit = false;
                    break;
                default:
                    ASSERT_NOT_REACHED();
                }
            }

            should_continue = is_a_digit;
            parse_ptr += should_continue;
        } while (should_continue);

        if (!exponent_usable) {
            parse_ptr = old_parse_ptr;
        } else if (exponent_overflow) {
            // Technically this is wrong. If someone gives us 5GB of digits,
            // and then an exponent of -5_000_000_000, the resulting exponent
            // should be around 0.
            // However, I think it's safe to assume that we never have to deal
            // with that many digits anyway.
            if (sign != Sign::Negative) {
                exponent = INT_MIN;
            } else {
                exponent = INT_MAX;
            }
        } else {
            // Literal exponent is usable and fits in an int.
            // However, `exponent + exponent_parser.number()` might overflow an int.
            // This would result in the wrong sign of the exponent!
            long long new_exponent = static_cast<long long>(exponent) + static_cast<long long>(exponent_parser.number());
            if (new_exponent < INT_MIN) {
                exponent = INT_MIN;
            } else if (new_exponent > INT_MAX) {
                exponent = INT_MAX;
            } else {
                exponent = static_cast<int>(new_exponent);
            }
        }
    }

    // Parsing finished. now we only have to compute the result.
    if (endptr)
        *endptr = const_cast<char*>(parse_ptr);

    // If `digits` is zero, we don't even have to look at `exponent`.
    if (digits.number() == 0) {
        if (sign != Sign::Negative) {
            return 0.0;
        } else {
            return -0.0;
        }
    }

    // Deal with extreme exponents.
    // The smallest normal is 2^-1022.
    // The smallest denormal is 2^-1074.
    // The largest number in `digits` is 2^63 - 1.
    // Therefore, if "base^exponent" is smaller than 2^-(1074+63), the result is 0.0 anyway.
    // This threshold is roughly 5.3566 * 10^-343.
    // So if the resulting exponent is -344 or lower (closer to -inf),
    // the result is 0.0 anyway.
    // We only need to avoid false positives, so we can ignore base 16.
    if (exponent <= -344) {
        errno = ERANGE;
        // Definitely can't be represented more precisely.
        // I lied, sometimes the result is +0.0, and sometimes -0.0.
        if (sign != Sign::Negative) {
            return 0.0;
        } else {
            return -0.0;
        }
    }
    // The largest normal is 2^+1024-eps.
    // The smallest number in `digits` is 1.
    // Therefore, if "base^exponent" is 2^+1024, the result is INF anyway.
    // This threshold is roughly 1.7977 * 10^-308.
    // So if the resulting exponent is +309 or higher,
    // the result is INF anyway.
    // We only need to avoid false positives, so we can ignore base 16.
    if (exponent >= 309) {
        errno = ERANGE;
        // Definitely can't be represented more precisely.
        // I lied, sometimes the result is +INF, and sometimes -INF.
        if (sign != Sign::Negative) {
            return __builtin_huge_val();
        } else {
            return -__builtin_huge_val();
        }
    }

    // TODO: If `exponent` is large, this could be made faster.
    double value = digits.number();
    if (exponent < 0) {
        exponent = -exponent;
        for (int i = 0; i < exponent; ++i) {
            value /= base;
        }
        if (value == -0.0 || value == +0.0) {
            errno = ERANGE;
        }
    } else if (exponent > 0) {
        for (int i = 0; i < exponent; ++i) {
            value *= base;
        }
        if (value == -__builtin_huge_val() || value == +__builtin_huge_val()) {
            errno = ERANGE;
        }
    }

    return value;
}

long double strtold(const char* str, char** endptr)
{
    assert(sizeof(double) == sizeof(long double));
    return strtod(str, endptr);
}

float strtof(const char* str, char** endptr)
{
    return strtod(str, endptr);
}

double atof(const char* str)
{
    return strtod(str, nullptr);
}

int atoi(const char* str)
{
    long value = strtol(str, nullptr, 10);
    if (value > INT_MAX) {
        return INT_MAX;
    }
    return value;
}

long atol(const char* str)
{
    return strtol(str, nullptr, 10);
}

long long atoll(const char* str)
{
    return strtoll(str, nullptr, 10);
}

static char ptsname_buf[32];
char* ptsname(int fd)
{
    if (ptsname_r(fd, ptsname_buf, sizeof(ptsname_buf)) < 0)
        return nullptr;
    return ptsname_buf;
}

int ptsname_r(int fd, char* buffer, size_t size)
{
    int rc = syscall(SC_ptsname, fd, buffer, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

static unsigned long s_next_rand = 1;

int rand()
{
    s_next_rand = s_next_rand * 1103515245 + 12345;
    return ((unsigned)(s_next_rand / ((RAND_MAX + 1) * 2)) % (RAND_MAX + 1));
}

void srand(unsigned seed)
{
    s_next_rand = seed;
}

int abs(int i)
{
    return i < 0 ? -i : i;
}

long int random()
{
    return rand();
}

void srandom(unsigned seed)
{
    srand(seed);
}

int system(const char* command)
{
    if (!command)
        return 1;

    pid_t child;
    const char* argv[] = { "sh", "-c", command, nullptr };
    if ((errno = posix_spawn(&child, "/bin/sh", nullptr, nullptr, const_cast<char**>(argv), environ)))
        return -1;
    int wstatus;
    waitpid(child, &wstatus, 0);
    return WEXITSTATUS(wstatus);
}

char* mktemp(char* pattern)
{
    if (__generate_unique_filename(pattern) < 0)
        pattern[0] = '\0';

    return pattern;
}

int mkstemp(char* pattern)
{
    char* path = mktemp(pattern);

    int fd = open(path, O_RDWR | O_CREAT | O_EXCL, S_IRUSR | S_IWUSR); // I'm using the flags I saw glibc using.
    if (fd >= 0)
        return fd;

    return -1;
}

char* mkdtemp(char* pattern)
{
    if (__generate_unique_filename(pattern) < 0)
        return nullptr;

    if (mkdir(pattern, 0700) < 0)
        return nullptr;

    return pattern;
}

void* bsearch(const void* key, const void* base, size_t nmemb, size_t size, int (*compar)(const void*, const void*))
{
    char* start = static_cast<char*>(const_cast<void*>(base));
    while (nmemb > 0) {
        char* middle_memb = start + (nmemb / 2) * size;
        int comparison = compar(key, middle_memb);
        if (comparison == 0)
            return middle_memb;
        else if (comparison > 0) {
            start = middle_memb + size;
            --nmemb;
        }
        nmemb /= 2;
    }

    return NULL;
}

div_t div(int numerator, int denominator)
{
    div_t result;
    result.quot = numerator / denominator;
    result.rem = numerator % denominator;

    if (numerator >= 0 && result.rem < 0) {
        result.quot++;
        result.rem -= denominator;
    }
    return result;
}

ldiv_t ldiv(long numerator, long denominator)
{
    ldiv_t result;
    result.quot = numerator / denominator;
    result.rem = numerator % denominator;

    if (numerator >= 0 && result.rem < 0) {
        result.quot++;
        result.rem -= denominator;
    }
    return result;
}

size_t mbstowcs(wchar_t*, const char*, size_t)
{
    ASSERT_NOT_REACHED();
}

int mbtowc(wchar_t* wch, const char* data, size_t data_size)
{
    // FIXME: This needs a real implementation.
    UNUSED_PARAM(data_size);

    if (wch && data) {
        *wch = *data;
        return 1;
    }

    if (!wch && data) {
        return 1;
    }

    return 0;
}

int wctomb(char*, wchar_t)
{
    ASSERT_NOT_REACHED();
}

size_t wcstombs(char* dest, const wchar_t* src, size_t max)
{
    char* originalDest = dest;
    while ((size_t)(dest - originalDest) < max) {
        StringView v { (const char*)src, sizeof(wchar_t) };

        // FIXME: dependent on locale, for now utf-8 is supported.
        Utf8View utf8 { v };
        if (*utf8.begin() == '\0') {
            *dest = '\0';
            return (size_t)(dest - originalDest); // Exclude null character in returned size
        }

        for (auto byte : utf8) {
            if (byte != '\0')
                *dest++ = byte;
        }
        ++src;
    }
    return max;
}

long strtol(const char* str, char** endptr, int base)
{
    long long value = strtoll(str, endptr, base);
    if (value < LONG_MIN) {
        errno = ERANGE;
        return LONG_MIN;
    } else if (value > LONG_MAX) {
        errno = ERANGE;
        return LONG_MAX;
    }
    return value;
}

unsigned long strtoul(const char* str, char** endptr, int base)
{
    unsigned long long value = strtoull(str, endptr, base);
    if (value > ULONG_MAX) {
        errno = ERANGE;
        return ULONG_MAX;
    }
    return value;
}

long long strtoll(const char* str, char** endptr, int base)
{
    // Parse spaces and sign
    char* parse_ptr = const_cast<char*>(str);
    strtons(parse_ptr, &parse_ptr);
    const Sign sign = strtosign(parse_ptr, &parse_ptr);

    // Parse base
    if (base == 0) {
        if (*parse_ptr == '0') {
            if (tolower(*(parse_ptr + 1)) == 'x') {
                base = 16;
                parse_ptr += 2;
            } else {
                base = 8;
            }
        } else {
            base = 10;
        }
    }

    // Parse actual digits.
    LongLongParser digits { sign, base };
    bool digits_usable = false;
    bool should_continue = true;
    bool overflow = false;
    do {
        bool is_a_digit;
        if (overflow) {
            is_a_digit = digits.parse_digit(*parse_ptr) >= 0;
        } else {
            DigitConsumeDecision decision = digits.consume(*parse_ptr);
            switch (decision) {
            case DigitConsumeDecision::Consumed:
                is_a_digit = true;
                // The very first actual digit must pass here:
                digits_usable = true;
                break;
            case DigitConsumeDecision::PosOverflow:
            case DigitConsumeDecision::NegOverflow:
                is_a_digit = true;
                overflow = true;
                break;
            case DigitConsumeDecision::Invalid:
                is_a_digit = false;
                break;
            default:
                ASSERT_NOT_REACHED();
            }
        }

        should_continue = is_a_digit;
        parse_ptr += should_continue;
    } while (should_continue);

    if (!digits_usable) {
        // No actual number value available.
        if (endptr)
            *endptr = const_cast<char*>(str);
        return 0;
    }

    if (endptr)
        *endptr = parse_ptr;

    if (overflow) {
        errno = ERANGE;
        if (sign != Sign::Negative) {
            return LONG_LONG_MAX;
        } else {
            return LONG_LONG_MIN;
        }
    }

    return digits.number();
}

unsigned long long strtoull(const char* str, char** endptr, int base)
{
    // Parse spaces and sign
    char* parse_ptr = const_cast<char*>(str);
    strtons(parse_ptr, &parse_ptr);

    // Parse base
    if (base == 0) {
        if (*parse_ptr == '0') {
            if (tolower(*(parse_ptr + 1)) == 'x') {
                base = 16;
                parse_ptr += 2;
            } else {
                base = 8;
            }
        } else {
            base = 10;
        }
    }

    // Parse actual digits.
    ULongLongParser digits { Sign::Positive, base };
    bool digits_usable = false;
    bool should_continue = true;
    bool overflow = false;
    do {
        bool is_a_digit;
        if (overflow) {
            is_a_digit = digits.parse_digit(*parse_ptr) >= 0;
        } else {
            DigitConsumeDecision decision = digits.consume(*parse_ptr);
            switch (decision) {
            case DigitConsumeDecision::Consumed:
                is_a_digit = true;
                // The very first actual digit must pass here:
                digits_usable = true;
                break;
            case DigitConsumeDecision::PosOverflow:
            case DigitConsumeDecision::NegOverflow:
                is_a_digit = true;
                overflow = true;
                break;
            case DigitConsumeDecision::Invalid:
                is_a_digit = false;
                break;
            default:
                ASSERT_NOT_REACHED();
            }
        }

        should_continue = is_a_digit;
        parse_ptr += should_continue;
    } while (should_continue);

    if (!digits_usable) {
        // No actual number value available.
        if (endptr)
            *endptr = const_cast<char*>(str);
        return 0;
    }

    if (endptr)
        *endptr = parse_ptr;

    if (overflow) {
        errno = ERANGE;
        return LONG_LONG_MAX;
    }

    return digits.number();
}

// Serenity's PRNG is not cryptographically secure. Do not rely on this for
// any real crypto! These functions (for now) are for compatibility.
// TODO: In the future, rand can be made deterministic and this not.
uint32_t arc4random(void)
{
    char buf[4];
    syscall(SC_getrandom, buf, 4, 0);
    return *(uint32_t*)buf;
}

void arc4random_buf(void* buffer, size_t buffer_size)
{
    // arc4random_buf should never fail, but user supplied buffers could fail.
    // However, if the user passes a garbage buffer, that's on them.
    syscall(SC_getrandom, buffer, buffer_size, 0);
}

uint32_t arc4random_uniform(uint32_t max_bounds)
{
    // XXX: Should actually apply special rules for uniformity; avoid what is
    // called "modulo bias".
    return arc4random() % max_bounds;
}

char* realpath(const char* pathname, char* buffer)
{
    if (!pathname) {
        errno = EFAULT;
        return nullptr;
    }
    size_t size = PATH_MAX;
    if (buffer == nullptr)
        buffer = (char*)malloc(size);
    Syscall::SC_realpath_params params { { pathname, strlen(pathname) }, { buffer, size } };
    int rc = syscall(SC_realpath, &params);
    if (rc < 0) {
        errno = -rc;
        return nullptr;
    }
    errno = 0;
    return buffer;
}

int posix_openpt(int flags)
{
    if (flags & ~(O_RDWR | O_NOCTTY | O_CLOEXEC)) {
        errno = EINVAL;
        return -1;
    }

    return open("/dev/ptmx", flags);
}

int grantpt(int fd)
{
    (void)fd;
    return 0;
}

int unlockpt(int fd)
{
    (void)fd;
    return 0;
}
}
