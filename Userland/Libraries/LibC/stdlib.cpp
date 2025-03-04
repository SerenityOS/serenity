/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/CharacterTypes.h>
#include <AK/FloatingPointStringConversions.h>
#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <AK/Random.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/Utf8View.h>
#include <alloca.h>
#include <assert.h>
#include <bits/pthread_cancel.h>
#include <ctype.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <signal.h>
#include <spawn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/auxv.h>
#include <sys/internals.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/sysmacros.h>
#include <sys/wait.h>
#include <syscall.h>
#include <unistd.h>
#include <wchar.h>

static void strtons(char const* str, char** endptr)
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

static Sign strtosign(char const* str, char** endptr)
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

    T number() const { return m_num; }

private:
    bool can_append_digit(int digit)
    {
        bool const is_below_cutoff = positive() ? (m_num < m_cutoff) : (m_num > m_cutoff);

        if (is_below_cutoff) {
            return true;
        } else {
            return m_num == m_cutoff && digit <= m_max_digit_after_cutoff;
        }
    }

    bool positive() const
    {
        return m_sign != Sign::Negative;
    }

    T const m_base;
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

template<typename Callback>
inline int generate_unique_filename(char* pattern, size_t suffix_length, Callback callback)
{
    size_t length = strlen(pattern);

    if (length < 6 + suffix_length || memcmp(pattern + length - 6 - suffix_length, "XXXXXX", 6))
        return EINVAL;

    size_t start = length - 6 - suffix_length;

    constexpr char random_characters[] = "abcdefghijklmnopqrstuvwxyz0123456789";

    for (int attempt = 0; attempt < 100; ++attempt) {
        for (int i = 0; i < 6; ++i)
            pattern[start + i] = random_characters[(arc4random() % (sizeof(random_characters) - 1))];
        if (callback() == IterationDecision::Break)
            return 0;
    }

    return EEXIST;
}

static bool is_infinity_string(char* parse_ptr, char** endptr)
{
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

                return true;
            }
        }
    }

    return false;
}

static bool is_nan_string(char* parse_ptr, char** endptr)
{
    // FIXME: Actually parse (or at least skip) the (n-char-sequenceopt) part
    if (is_either(parse_ptr, 0, 'n', 'N')) {
        if (is_either(parse_ptr, 1, 'a', 'A')) {
            if (is_either(parse_ptr, 2, 'n', 'N')) {
                if (endptr)
                    *endptr = parse_ptr + 3;
                return true;
            }
        }
    }

    return false;
}

template<FloatingPoint T>
static T c_str_to_floating_point(char const* str, char** endptr)
{
    // First, they decompose the input string into three parts:
    char* parse_ptr = const_cast<char*>(str);

    // An initial, possibly empty, sequence of white-space characters (as specified by isspace())
    strtons(parse_ptr, &parse_ptr);

    // A subject sequence interpreted as a floating-point constant or representing infinity or NaN

    if (*parse_ptr == '\0') {
        if (endptr)
            *endptr = const_cast<char*>(str);
        return 0.;
    }

    bool is_hex = [&] {
        // A hexfloat must start with either 0x, 0X, -0x or -0X and have something after it
        char const* parse_head = parse_ptr;
        if (*parse_head == '-')
            ++parse_head;

        if (*parse_head != '0')
            return false;

        ++parse_head;

        if (*parse_head != 'x')
            return false;

        ++parse_head;

        // We must have at least one digit but it can come after the "decimal" point.

        if (is_ascii_hex_digit(*parse_head))
            return true;

        if (*parse_head != '.')
            return false;

        ++parse_head;

        return is_ascii_hex_digit(*parse_head);
    }();

    AK::FloatingPointParseResults<T> double_parse_result;
    if (is_hex) {
        // A 0x or 0X, then a non-empty sequence of hexadecimal digits optionally containing a radix character;
        // then an optional binary exponent part consisting of the character 'p' or the character 'P',
        // optionally followed by a '+' or '-' character, and then followed by one or more decimal digits

        double_parse_result = AK::parse_first_hexfloat_until_zero_character<T>(parse_ptr);
    } else {
        // A non-empty sequence of decimal digits optionally containing a radix character;
        // then an optional exponent part consisting of the character 'e' or the character 'E',
        // optionally followed by a '+' or '-' character, and then followed by one or more decimal digits
        double_parse_result = AK::parse_first_floating_point_until_zero_character<T>(parse_ptr);
    }

    if (double_parse_result.error == AK::FloatingPointError::None) {
        // The only way to get NaN (which we shouldn't) or infinities is rounding up to them so we
        // have to set ERANGE in that case.
        if (!__builtin_isfinite(double_parse_result.value))
            errno = ERANGE;

        if (endptr)
            *endptr = const_cast<char*>(double_parse_result.end_ptr);
        return double_parse_result.value;
    }

    if (double_parse_result.error == AK::FloatingPointError::RoundedDownToZero || double_parse_result.error == AK::FloatingPointError::OutOfRange) {
        // This is a special case for strtod, where we have a double so close to zero we had to round
        // it to zero, in which case we have to set ERANGE
        errno = ERANGE;

        if (endptr)
            *endptr = const_cast<char*>(double_parse_result.end_ptr);
        return double_parse_result.value;
    }

    // The only way we are here is if the input was not valid for parse_first_floating_point or not a valid hex float
    // So the only cases left are:
    // - One of INF or INFINITY, ignoring case
    // - One of NAN or NAN(n-char-sequenceopt), ignoring case in the NAN part

    Sign const sign = strtosign(parse_ptr, &parse_ptr);

    if (is_infinity_string(parse_ptr, endptr)) {
        // Don't set errno to ERANGE here:
        // The caller may want to distinguish between "input is
        // literal infinity" and "input is not literal infinity
        // but did not fit into double".
        if (sign != Sign::Negative)
            return static_cast<T>(__builtin_huge_val());
        else
            return static_cast<T>(-__builtin_huge_val());
    }

    if (is_nan_string(parse_ptr, endptr)) {
        errno = ERANGE;
        // FIXME: Do we actually want to return "different" NaN bit values?
        if (sign != Sign::Negative)
            return static_cast<T>(__builtin_nan(""));
        else
            return static_cast<T>(-__builtin_nan(""));
    }

    // If no conversion could be performed, 0 shall be returned, and errno may be set to [EINVAL].
    // FIXME: This is in the posix standard linked from strtod but not in implementations of strtod
    //        and not in the man pages for linux strtod.
    if (endptr)
        *endptr = const_cast<char*>(str);
    return 0;
}

[[gnu::weak]] extern void __call_fini_functions() asm("__call_fini_functions");

extern "C" {

void exit(int status)
{
#ifndef _DYNAMIC_LOADER
    __pthread_key_destroy_for_current_thread();
#endif

    __cxa_finalize(nullptr);

    if (secure_getenv("LIBC_DUMP_MALLOC_STATS"))
        serenity_dump_malloc_stats();

    __call_fini_functions();
    fflush(nullptr);

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

void _abort()
{
    // According to the GCC manual __builtin_trap() can call abort() so using it here might not seem safe at first. However,
    // on all the platforms we support GCC emits a trapping instruction instead of a call.
    __builtin_trap();
}

void abort()
{
    // For starters, send ourselves a SIGABRT.
    raise(SIGABRT);
    // If that didn't kill us, try harder.
    sigset_t set;
    sigemptyset(&set);
    sigaddset(&set, SIGABRT);
    sigprocmask(SIG_UNBLOCK, &set, nullptr);
    raise(SIGABRT);
    _abort();
}

static HashTable<FlatPtr> s_malloced_environment_variables;

static void free_environment_variable_if_needed(char const* var)
{
    if (!s_malloced_environment_variables.contains((FlatPtr)var))
        return;
    s_malloced_environment_variables.remove((FlatPtr)var);
    free(const_cast<char*>(var));
}

char* getenv(char const* name)
{
    size_t vl = strlen(name);
    for (size_t i = 0; environ[i]; ++i) {
        char const* decl = environ[i];
        char* eq = strchr(decl, '=');
        if (!eq)
            continue;
        size_t varLength = eq - decl;
        if (vl != varLength)
            continue;
        if (strncmp(decl, name, varLength) == 0)
            return eq + 1;
    }
    return nullptr;
}

char* secure_getenv(char const* name)
{
    if (getauxval(AT_SECURE))
        return nullptr;
    return getenv(name);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/unsetenv.html
int unsetenv(char const* name)
{
    auto new_var_len = strlen(name);
    if (new_var_len == 0 || strchr(name, '=')) {
        errno = EINVAL;
        return -1;
    }

    size_t environ_size = 0;
    int skip = -1;

    for (; environ[environ_size]; ++environ_size) {
        char* old_var = environ[environ_size];
        char* old_eq = strchr(old_var, '=');
        VERIFY(old_eq);
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

int clearenv()
{
    for (char** env = environ; *env; ++env) {
        free_environment_variable_if_needed(*env);
        *env = nullptr;
    }
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/setenv.html
int setenv(char const* name, char const* value, int overwrite)
{
    auto new_var_len = strlen(name);
    if (new_var_len == 0 || strchr(name, '=')) {
        errno = EINVAL;
        return -1;
    }

    if (!overwrite && getenv(name))
        return 0;

    auto const total_length = new_var_len + strlen(value) + 2;
    auto* var = (char*)malloc(total_length);
    snprintf(var, total_length, "%s=%s", name, value);
    s_malloced_environment_variables.set((FlatPtr)var);
    return putenv(var);
}

// A non-evil version of putenv that will strdup the env (and free it later)
int serenity_putenv(char const* new_var, size_t length)
{
    auto* var = strndup(new_var, length);
    s_malloced_environment_variables.set((FlatPtr)var);
    return putenv(var);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/putenv.html
int putenv(char* new_var)
{
    char* new_eq = strchr(new_var, '=');
    if (!new_eq)
        return unsetenv(new_var);

    auto new_var_name_len = new_eq - new_var;
    int environ_size = 0;
    for (; environ[environ_size]; ++environ_size) {
        char* old_var = environ[environ_size];
        auto old_var_name_max_length = strnlen(old_var, new_var_name_len);
        char* old_eq = static_cast<char*>(memchr(old_var, '=', old_var_name_max_length + 1));
        if (!old_eq)
            continue; // name is longer, or possibly freed or overwritten value

        auto old_var_name_len = old_eq - old_var;
        if (new_var_name_len != old_var_name_len)
            continue; // can't match

        if (strncmp(new_var, old_var, new_var_name_len) == 0) {
            free_environment_variable_if_needed(old_var);
            environ[environ_size] = new_var;
            return 0;
        }
    }

    // At this point, we need to append the new var.
    // 2 here: one for the new var, one for the sentinel value.
    auto** new_environ = static_cast<char**>(kmalloc_array(environ_size + 2, sizeof(char*)));
    if (new_environ == nullptr) {
        errno = ENOMEM;
        return -1;
    }

    for (int i = 0; environ[i]; ++i)
        new_environ[i] = environ[i];

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

static char const* __progname = NULL;

char const* getprogname()
{
    return __progname;
}

void setprogname(char const* progname)
{
    for (int i = strlen(progname) - 1; i >= 0; i--) {
        if (progname[i] == '/') {
            __progname = progname + i + 1;
            return;
        }
    }

    __progname = progname;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strtod.html
double strtod(char const* str, char** endptr)
{
    return c_str_to_floating_point<double>(str, endptr);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strtold.html
long double strtold(char const* str, char** endptr)
{
    assert(sizeof(double) == sizeof(long double));
    return strtod(str, endptr);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strtof.html
float strtof(char const* str, char** endptr)
{
    return c_str_to_floating_point<float>(str, endptr);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/atof.html
double atof(char const* str)
{
    return strtod(str, nullptr);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/atoi.html
int atoi(char const* str)
{
    long value = strtol(str, nullptr, 10);
    if (value > INT_MAX) {
        return INT_MAX;
    }
    return value;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/atol.html
long atol(char const* str)
{
    return strtol(str, nullptr, 10);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/atoll.html
long long atoll(char const* str)
{
    return strtoll(str, nullptr, 10);
}

static char ptsname_buf[32];
// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ptsname.html
char* ptsname(int fd)
{
    if (ptsname_r(fd, ptsname_buf, sizeof(ptsname_buf)) < 0)
        return nullptr;
    return ptsname_buf;
}

int ptsname_r(int fd, char* buffer, size_t size)
{
    struct stat stat;
    if (fstat(fd, &stat) < 0)
        return -1;

    StringBuilder devpts_path_builder;
    devpts_path_builder.append("/dev/pts/"sv);

    int master_pty_index = 0;
    // Note: When the user opens a PTY from /dev/ptmx with posix_openpt(), the open file descriptor
    // points to /dev/ptmx, (major number is 5 and minor number is 2), but internally
    // in the kernel, it points to a new MasterPTY device. When we do ioctl with TIOCGPTN option
    // on the open file descriptor, it actually asks the MasterPTY what is the assigned index
    // of it when the PTYMultiplexer created it.
    if (ioctl(fd, TIOCGPTN, &master_pty_index) < 0)
        return -1;

    if (master_pty_index < 0) {
        errno = EINVAL;
        return -1;
    }

    devpts_path_builder.appendff("{:d}", master_pty_index);
    if (devpts_path_builder.length() > size) {
        errno = ERANGE;
        return -1;
    }
    memset(buffer, 0, devpts_path_builder.length() + 1);
    auto full_devpts_path_string = devpts_path_builder.to_byte_string();
    if (!full_devpts_path_string.copy_characters_to_buffer(buffer, size)) {
        errno = ERANGE;
        return -1;
    }
    return 0;
}

static unsigned long s_next_rand = 1;
static long s_next_rand48 = 0;

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/rand.html
int rand()
{
    s_next_rand = s_next_rand * 1103515245 + 12345;
    return ((unsigned)(s_next_rand / ((RAND_MAX + 1) * 2)) % (RAND_MAX + 1));
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/srand.html
void srand(unsigned seed)
{
    s_next_rand = seed;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/drand48.html
double drand48()
{
    constexpr u64 a = 0x5DEECE66DULL;
    constexpr u64 c = 0xBULL;
    constexpr u64 m = 1ULL << 48;

    s_next_rand48 = (a * s_next_rand48 + c) & (m - 1);
    return static_cast<double>(s_next_rand48) / m;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/srand48.html
void srand48(long seed)
{
    s_next_rand48 = (seed & 0xFFFFFFFF) << 16 | 0x330E;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/abs.html
int abs(int i)
{
    return i < 0 ? -i : i;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/labs.html
long int labs(long int i)
{
    return i < 0 ? -i : i;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/llabs.html
long long int llabs(long long int i)
{
    return i < 0 ? -i : i;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/random.html
long int random()
{
    return rand();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/srandom.html
void srandom(unsigned seed)
{
    srand(seed);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/system.html
int system(char const* command)
{
    __pthread_maybe_cancel();

    if (!command)
        return 1;

    pid_t child;
    char const* argv[] = { "sh", "-c", command, nullptr };
    if ((errno = posix_spawn(&child, "/bin/sh", nullptr, nullptr, const_cast<char**>(argv), environ)))
        return -1;
    int wstatus;
    waitpid(child, &wstatus, 0);
    return WEXITSTATUS(wstatus);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mktemp.html
char* mktemp(char* pattern)
{
    auto error = generate_unique_filename(pattern, 0, [&] {
        struct stat st;
        int rc = lstat(pattern, &st);
        if (rc < 0 && errno == ENOENT)
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });
    if (error) {
        pattern[0] = '\0';
        errno = error;
    }
    return pattern;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mkstemp.html
int mkstemp(char* pattern)
{
    return mkstemps(pattern, 0);
}

// https://man7.org/linux/man-pages/man3/mkstemps.3.html
int mkstemps(char* pattern, int suffix_length)
{
    VERIFY(suffix_length >= 0);
    int fd = -1;
    auto error = generate_unique_filename(pattern, static_cast<size_t>(suffix_length), [&] {
        fd = open(pattern, O_RDWR | O_CREAT | O_EXCL, S_IRUSR | S_IWUSR); // I'm using the flags I saw glibc using.
        if (fd >= 0)
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });
    if (error) {
        errno = error;
        return -1;
    }
    return fd;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mkdtemp.html
char* mkdtemp(char* pattern)
{
    auto error = generate_unique_filename(pattern, 0, [&] {
        if (mkdir(pattern, 0700) == 0)
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });
    if (error) {
        errno = error;
        return nullptr;
    }
    return pattern;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/bsearch.html
void* bsearch(void const* key, void const* base, size_t nmemb, size_t size, int (*compar)(void const*, void const*))
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

    return nullptr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/div.html
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ldiv.html
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/lldiv.html
lldiv_t lldiv(long long numerator, long long denominator)
{
    lldiv_t result;
    result.quot = numerator / denominator;
    result.rem = numerator % denominator;

    if (numerator >= 0 && result.rem < 0) {
        result.quot++;
        result.rem -= denominator;
    }
    return result;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mblen.html
int mblen(char const* s, size_t n)
{
    // POSIX: Equivalent to mbtowc(NULL, s, n), but we mustn't change the state of mbtowc.
    static mbstate_t internal_state = {};

    // Reset the internal state and ask whether we have shift states.
    if (s == nullptr) {
        internal_state = {};
        return 0;
    }

    size_t ret = mbrtowc(nullptr, s, n, &internal_state);

    // Incomplete characters get returned as illegal sequence.
    if (ret == -2ul) {
        errno = EILSEQ;
        return -1;
    }

    return ret;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mbstowcs.html
size_t mbstowcs(wchar_t* pwcs, char const* s, size_t n)
{
    static mbstate_t state = {};
    return mbsrtowcs(pwcs, &s, n, &state);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mbtowc.html
int mbtowc(wchar_t* pwc, char const* s, size_t n)
{
    static mbstate_t internal_state = {};

    // Reset the internal state and ask whether we have shift states.
    if (s == nullptr) {
        internal_state = {};
        return 0;
    }

    size_t ret = mbrtowc(pwc, s, n, &internal_state);

    // Incomplete characters get returned as illegal sequence.
    // Internal state is undefined, so don't bother with resetting.
    if (ret == -2ul) {
        errno = EILSEQ;
        return -1;
    }

    return ret;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wctomb.html
int wctomb(char* s, wchar_t wc)
{
    static mbstate_t _internal_state = {};

    // nullptr asks whether we have state-dependent encodings, but we don't have any.
    if (s == nullptr)
        return 0;

    return static_cast<int>(wcrtomb(s, wc, &_internal_state));
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcstombs.html
size_t wcstombs(char* dest, wchar_t const* src, size_t max)
{
    char* original_dest = dest;
    while ((size_t)(dest - original_dest) < max) {
        StringView v { (char const*)src, sizeof(wchar_t) };

        // FIXME: dependent on locale, for now utf-8 is supported.
        Utf8View utf8 { v };
        if (*utf8.begin() == '\0') {
            *dest = '\0';
            return (size_t)(dest - original_dest); // Exclude null character in returned size
        }

        for (auto byte : utf8) {
            if (byte != '\0')
                *dest++ = byte;
        }
        ++src;
    }
    return max;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strtol.html
long strtol(char const* str, char** endptr, int base)
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strtoul.html
unsigned long strtoul(char const* str, char** endptr, int base)
{
    unsigned long long value = strtoull(str, endptr, base);
    if (value > ULONG_MAX) {
        errno = ERANGE;
        return ULONG_MAX;
    }
    return value;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strtoll.html
long long strtoll(char const* str, char** endptr, int base)
{
    // Parse spaces and sign
    char* parse_ptr = const_cast<char*>(str);
    strtons(parse_ptr, &parse_ptr);
    Sign const sign = strtosign(parse_ptr, &parse_ptr);

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
                VERIFY_NOT_REACHED();
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strtoull.html
unsigned long long strtoull(char const* str, char** endptr, int base)
{
    // Parse spaces and sign
    char* parse_ptr = const_cast<char*>(str);
    strtons(parse_ptr, &parse_ptr);

    if (base == 16) {
        // Dr. POSIX: "If the value of base is 16, the characters 0x or 0X may optionally precede
        //             the sequence of letters and digits, following the sign if present."
        if (*parse_ptr == '0') {
            if (tolower(*(parse_ptr + 1)) == 'x')
                parse_ptr += 2;
        }
    }
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
                VERIFY_NOT_REACHED();
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

uint32_t arc4random(void)
{
    uint32_t buf;
    arc4random_buf(&buf, sizeof(buf));
    return buf;
}

static pthread_mutex_t s_randomness_mutex = PTHREAD_MUTEX_INITIALIZER;
static u8* s_randomness_buffer;
static size_t s_randomness_index;

void arc4random_buf(void* buffer, size_t buffer_size)
{
    pthread_mutex_lock(&s_randomness_mutex);

    size_t bytes_needed = buffer_size;
    auto* ptr = static_cast<u8*>(buffer);

    while (bytes_needed > 0) {
        if (!s_randomness_buffer || s_randomness_index >= PAGE_SIZE) {
            if (!s_randomness_buffer) {
                s_randomness_buffer = static_cast<u8*>(mmap(nullptr, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_RANDOMIZED, 0, 0));
                VERIFY(s_randomness_buffer != MAP_FAILED);
                __pthread_fork_atfork_register_child(
                    [] {
                        munmap(s_randomness_buffer, PAGE_SIZE);
                        s_randomness_buffer = nullptr;
                        s_randomness_index = 0;
                    });
            }
            syscall(SC_getrandom, s_randomness_buffer, PAGE_SIZE);
            s_randomness_index = 0;
        }

        size_t available_bytes = PAGE_SIZE - s_randomness_index;
        size_t bytes_to_copy = min(bytes_needed, available_bytes);

        memcpy(ptr, s_randomness_buffer + s_randomness_index, bytes_to_copy);

        s_randomness_index += bytes_to_copy;
        bytes_needed -= bytes_to_copy;
        ptr += bytes_to_copy;
    }

    pthread_mutex_unlock(&s_randomness_mutex);
}

uint32_t arc4random_uniform(uint32_t max_bounds)
{
    return AK::get_random_uniform(max_bounds);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/realpath.html
char* realpath(char const* pathname, char* buffer)
{
    if (!pathname) {
        errno = EFAULT;
        return nullptr;
    }
    size_t size = PATH_MAX;
    bool self_allocated = false;
    if (buffer == nullptr) {
        // Since we self-allocate, try to sneakily use a smaller buffer instead, in an attempt to use less memory.
        size = 64;
        buffer = (char*)malloc(size);
        self_allocated = true;
    }
    Syscall::SC_realpath_params params { { pathname, strlen(pathname) }, { buffer, size } };
    int rc = syscall(SC_realpath, &params);
    if (rc < 0) {
        if (self_allocated)
            free(buffer);
        errno = -rc;
        return nullptr;
    }
    if (self_allocated && static_cast<size_t>(rc) > size) {
        // There was silent truncation, *and* we can simply retry without the caller noticing.
        free(buffer);
        size = static_cast<size_t>(rc);
        buffer = (char*)malloc(size);
        params.buffer = { buffer, size };
        rc = syscall(SC_realpath, &params);
        if (rc < 0) {
            // Can only happen if we lose a race. Let's pretend we lost the race in the first place.
            free(buffer);
            errno = -rc;
            return nullptr;
        }
        size_t new_size = static_cast<size_t>(rc);
        if (new_size < size) {
            // If we're here, the symlink has become longer while we were looking at it.
            // There's not much we can do, unless we want to loop endlessly
            // in this case. Let's leave it up to the caller whether to loop.
            free(buffer);
            errno = EAGAIN;
            return nullptr;
        }
    }
    errno = 0;
    return buffer;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_openpt.html
int posix_openpt(int flags)
{
    if (flags & ~(O_RDWR | O_NOCTTY | O_CLOEXEC)) {
        errno = EINVAL;
        return -1;
    }

    return open("/dev/ptmx", flags);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/grantpt.html
int grantpt([[maybe_unused]] int fd)
{
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/unlockpt.html
int unlockpt([[maybe_unused]] int fd)
{
    return 0;
}
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/_Exit.html
void _Exit(int status)
{
    _exit(status);
}
