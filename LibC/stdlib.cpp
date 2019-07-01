#include <AK/AKString.h>
#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <AK/StdLibExtras.h>
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

double strtod(const char* str, char** endptr)
{
    (void)str;
    (void)endptr;
    dbgprintf("LibC: strtod: '%s'\n", str);
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
    dbgprintf("LibC: atof: '%s'\n", str);
    ASSERT_NOT_REACHED();
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
    auto child = fork();
    if (!child) {
        int rc = execl("/bin/sh", "sh", "-c", command, nullptr);
        if (rc < 0)
            perror("execl");
        exit(0);
    }
    int wstatus;
    waitpid(child, &wstatus, 0);
    return WEXITSTATUS(wstatus);
}

char* mktemp(char* pattern)
{
    int length = strlen(pattern);

    // FIXME: Check for an invalid template pattern and return EINVAL.
    if (length < 6) {
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
    return result;
}

ldiv_t ldiv(long numerator, long denominator)
{
    ldiv_t result;
    result.quot = numerator / denominator;
    result.rem = numerator % denominator;
    return result;
}

size_t mbstowcs(wchar_t*, const char*, size_t)
{
    ASSERT_NOT_REACHED();
}

long strtol(const char* str, char** endptr, int base)
{
    int sign = 1;
    while (isspace(*str))
        str++;
    if (*str == '-' || *str == '+') {
        if (*str == '-')
            sign = -1;
        str++;
    }
    if (base == 0 || base == 16) {
        if (base == 0)
            base = 10;
        if (*str == '0') {
            str++;
            if (*str == 'X' || *str == 'x') {
                str++;
                base = 16;
            } else if (base != 16) {
                base = 8;
            }
        }
    }
    const char* estr = str + strlen(str) - 1;
    long track = 1;
    long num = 0;
    while (estr >= str) {
        if ((*estr >= '0' && *estr <= '9') || (base > 10 && (*estr >= 'A' && *estr <= 'Z'))) {
            int digit_value = *estr - '0';
            if (*estr >= 'A' && *estr <= 'Z')
                digit_value = 10 + (*estr - 'A');
            num += (track *= base) / base * digit_value;
        } else {
            if (endptr != NULL)
                *endptr = estr;
            return 0;
        };
        estr--;
    }
    return num * sign;
}

unsigned long strtoul(const char* str, char** endptr, int base)
{
    auto value = strtol(str, endptr, base);
    ASSERT(value >= 0);
    return value;
}
}
