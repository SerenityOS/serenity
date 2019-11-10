#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Syscall.h>
#include <alloca.h>
#include <assert.h>
#include <ctype.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

extern "C" {

typedef void (*__atexit_handler)();
static int __atexit_handler_count = 0;
static __atexit_handler __atexit_handlers[32];

void exit(int status)
{
    for (int i = 0; i < __atexit_handler_count; ++i)
        __atexit_handlers[i]();
    extern void _fini();
    _fini();
    fflush(stdout);
    fflush(stderr);
    _exit(status);
    ASSERT_NOT_REACHED();
}

int atexit(void (*handler)())
{
    ASSERT(__atexit_handler_count < 32);
    __atexit_handlers[__atexit_handler_count++] = handler;
    return 0;
}

void abort()
{
    raise(SIGABRT);
    ASSERT_NOT_REACHED();
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
    if (!overwrite && !getenv(name))
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
}

double strtod(const char* str, char** endptr)
{
    (void)str;
    (void)endptr;
    dbgprintf("LibC: strtod: '%s'\n", str);
    ASSERT_NOT_REACHED();
}

long double strtold(const char* str, char** endptr)
{
    (void)str;
    (void)endptr;
    dbgprintf("LibC: strtold: '%s'\n", str);
    ASSERT_NOT_REACHED();
}

float strtof(const char* str, char** endptr)
{
    (void)str;
    (void)endptr;
    dbgprintf("LibC: strtof: '%s'\n", str);
    ASSERT_NOT_REACHED();
}

double atof(const char* str)
{
    size_t len = strlen(str);
    size_t weight = 1;
    int exp_val = 0;
    double value = 0.0f;
    double fraction = 0.0f;
    bool has_sign = false;
    bool is_negative = false;
    bool is_fractional = false;
    bool is_scientific = false;

    if (str[0] == '-') {
        is_negative = true;
        has_sign = true;
    }
    if (str[0] == '+') {
        has_sign = true;
    }

    for (size_t i = has_sign; i < len; i++) {

        // Looks like we're about to start working on the fractional part
        if (str[i] == '.') {
            is_fractional = true;
            continue;
        }

        if (str[i] == 'e' || str[i] == 'E') {
            if (str[i + 1] == '-' || str[i + 1] == '+')
                exp_val = atoi(str + i + 2);
            else
                exp_val = atoi(str + i + 1);

            is_scientific = true;
            continue;
        }

        if (str[i] < '0' || str[i] > '9' || exp_val != 0)
            continue;

        if (is_fractional) {
            fraction *= 10;
            fraction += str[i] - '0';
            weight *= 10;
        } else {
            value = value * 10;
            value += str[i] - '0';
        }
    }

    fraction /= weight;
    value += fraction;

    if (is_scientific) {
        bool divide = exp_val < 0;
        if (divide)
            exp_val *= -1;

        for (int i = 0; i < exp_val; i++) {
            if (divide)
                value /= 10;
            else
                value *= 10;
        }
    }

    return is_negative ? -value : value;
}

int atoi(const char* str)
{
    size_t len = strlen(str);
    int value = 0;
    bool isNegative = false;
    for (size_t i = 0; i < len; ++i) {
        if (i == 0 && str[0] == '-') {
            isNegative = true;
            continue;
        }
        if (str[i] < '0' || str[i] > '9')
            return value;
        value = value * 10;
        value += str[i] - '0';
    }
    return isNegative ? -value : value;
}

long atol(const char* str)
{
    static_assert(sizeof(int) == sizeof(long));
    return atoi(str);
}

long long atoll(const char* str)
{
    dbgprintf("FIXME(Libc): atoll('%s') passing through to atol()\n", str);
    return atol(str);
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
    int rc = syscall(SC_ptsname_r, fd, buffer, size);
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

    auto child = fork();
    if (child < 0)
        return -1;

    if (!child) {
        int rc = execl("/bin/sh", "sh", "-c", command, nullptr);
        ASSERT(rc < 0);
        perror("execl");
        exit(127);
    }
    int wstatus;
    waitpid(child, &wstatus, 0);
    return WEXITSTATUS(wstatus);
}

char* mktemp(char* pattern)
{
    int length = strlen(pattern);

    if (length < 6 || !String(pattern).ends_with("XXXXXX")) {
        pattern[0] = '\0';
        errno = EINVAL;
        return pattern;
    }

    int start = length - 6;

    static constexpr char random_characters[] = "abcdefghijklmnopqrstuvwxyz0123456789";

    for (int attempt = 0; attempt < 100; ++attempt) {
        for (int i = 0; i < 6; ++i)
            pattern[start + i] = random_characters[(rand() % sizeof(random_characters))];
        struct stat st;
        int rc = lstat(pattern, &st);
        if (rc < 0 && errno == ENOENT)
            return pattern;
    }
    pattern[0] = '\0';
    errno = EEXIST;
    return pattern;
}

char* mkdtemp(char* pattern)
{
    int length = strlen(pattern);

    if (length < 6 || !String(pattern).ends_with("XXXXXX")) {
        errno = EINVAL;
        return nullptr;
    }

    int start = length - 6;

    static constexpr char random_characters[] = "abcdefghijklmnopqrstuvwxyz0123456789";

    for (int attempt = 0; attempt < 100; ++attempt) {
        for (int i = 0; i < 6; ++i)
            pattern[start + i] = random_characters[(rand() % sizeof(random_characters))];
        struct stat st;
        int rc = lstat(pattern, &st);
        if (rc < 0 && errno == ENOENT) {
            if (mkdir(pattern, 0700) < 0)
                return nullptr;
            return pattern;
        }
    }

    errno = EEXIST;
    return nullptr;
}

void* bsearch(const void* key, const void* base, size_t nmemb, size_t size, int (*compar)(const void*, const void*))
{
    dbgprintf("FIXME(LibC): bsearch(%p, %p, %u, %u, %p)\n", key, base, nmemb, size, compar);
    ASSERT_NOT_REACHED();
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

size_t mbtowc(wchar_t* wch, const char* data, size_t data_size)
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

template<typename T, T min_value, T max_value>
static T strtol_impl(const char* nptr, char** endptr, int base)
{
    errno = 0;

    if (base < 0 || base == 1 || base > 36) {
        errno = EINVAL;
        if (endptr)
            *endptr = const_cast<char*>(nptr);
        return 0;
    }

    const char* p = nptr;
    while (isspace(*p))
        ++p;

    bool is_negative = false;
    if (*p == '-') {
        is_negative = true;
        ++p;
    } else {
        if (*p == '+')
            ++p;
    }

    if (base == 0 || base == 16) {
        if (base == 0)
            base = 10;
        if (*p == '0') {
            if (*(p + 1) == 'X' || *(p + 1) == 'x') {
                p += 2;
                base = 16;
            } else if (base != 16) {
                base = 8;
            }
        }
    }

    long cutoff_point = is_negative ? (min_value / base) : (max_value / base);
    int max_valid_digit_at_cutoff_point = is_negative ? (min_value % base) : (max_value % base);

    long num = 0;

    bool has_overflowed = false;
    unsigned digits_consumed = 0;

    for (;;) {
        char ch = *(p++);
        int digit;
        if (isdigit(ch))
            digit = ch - '0';
        else if (islower(ch))
            digit = ch - ('a' - 10);
        else if (isupper(ch))
            digit = ch - ('A' - 10);
        else
            break;

        if (digit >= base)
            break;

        if (has_overflowed)
            continue;

        bool is_past_cutoff = is_negative ? num < cutoff_point : num > cutoff_point;

        if (is_past_cutoff || (num == cutoff_point && digit > max_valid_digit_at_cutoff_point)) {
            has_overflowed = true;
            num = is_negative ? min_value : max_value;
            errno = ERANGE;
        } else {
            num *= base;
            num += is_negative ? -digit : digit;
            ++digits_consumed;
        }
    }

    if (endptr) {
        if (has_overflowed || digits_consumed > 0)
            *endptr = const_cast<char*>(p - 1);
        else
            *endptr = const_cast<char*>(nptr);
    }
    return num;
}

long strtol(const char* str, char** endptr, int base)
{
    return strtol_impl<long, LONG_MIN, LONG_MAX>(str, endptr, base);
}

unsigned long strtoul(const char* str, char** endptr, int base)
{
    auto value = strtol(str, endptr, base);
    ASSERT(value >= 0);
    return value;
}

long long strtoll(const char* str, char** endptr, int base)
{
    return strtol_impl<long long, LONG_LONG_MIN, LONG_LONG_MAX>(str, endptr, base);
}

unsigned long long strtoull(const char* str, char** endptr, int base)
{
    auto value = strtoll(str, endptr, base);
    ASSERT(value >= 0);
    return value;
}

// Serenity's PRNG is not cryptographically secure. Do not rely on this for
// any real crypto! These functions (for now) are for compatibility.
// TODO: In the future, rand can be made determinstic and this not.
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
