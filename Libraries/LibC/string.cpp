#include <AK/Platform.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <assert.h>
#include <ctype.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

extern "C" {

void bzero(void* dest, size_t n)
{
    memset(dest, 0, n);
}

void bcopy(const void* src, void* dest, size_t n)
{
    memmove(dest, src, n);
}

size_t strspn(const char* s, const char* accept)
{
    const char* p = s;
cont:
    char ch = *p++;
    char ac;
    for (const char* ap = accept; (ac = *ap++) != '\0';) {
        if (ac == ch)
            goto cont;
    }
    return p - 1 - s;
}

size_t strcspn(const char* s, const char* reject)
{
    for (auto* p = s;;) {
        char c = *p++;
        auto* rp = reject;
        char rc;
        do {
            if ((rc = *rp++) == c)
                return p - 1 - s;
        } while (rc);
    }
}

size_t strlen(const char* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
}

size_t strnlen(const char* str, size_t maxlen)
{
    size_t len = 0;
    for (; len < maxlen && *str; str++)
        len++;
    return len;
}

char* strdup(const char* str)
{
    size_t len = strlen(str);
    char* new_str = (char*)malloc(len + 1);
    strcpy(new_str, str);
    return new_str;
}

char* strndup(const char* str, size_t maxlen)
{
    size_t len = min(strlen(str), maxlen);
    char* new_str = (char*)malloc(len + 1);
    memcpy(new_str, str, len);
    new_str[len] = 0;
    return new_str;
}

int strcmp(const char* s1, const char* s2)
{
    while (*s1 == *s2++)
        if (*s1++ == 0)
            return 0;
    return *(const unsigned char*)s1 - *(const unsigned char*)--s2;
}

int strncmp(const char* s1, const char* s2, size_t n)
{
    if (!n)
        return 0;
    do {
        if (*s1 != *s2++)
            return *(const unsigned char*)s1 - *(const unsigned char*)--s2;
        if (*s1++ == 0)
            break;
    } while (--n);
    return 0;
}

int memcmp(const void* v1, const void* v2, size_t n)
{
    auto* s1 = (const uint8_t*)v1;
    auto* s2 = (const uint8_t*)v2;
    while (n-- > 0) {
        if (*s1++ != *s2++)
            return s1[-1] < s2[-1] ? -1 : 1;
    }
    return 0;
}

#if ARCH(I386)
void* mmx_memcpy(void* dest, const void* src, size_t len)
{
    ASSERT(len >= 1024);

    auto* dest_ptr = (u8*)dest;
    auto* src_ptr = (const u8*)src;

    if ((u32)dest_ptr & 7) {
        u32 prologue = 8 - ((u32)dest_ptr & 7);
        len -= prologue;
        asm volatile(
            "rep movsb\n"
            : "=S"(src_ptr), "=D"(dest_ptr), "=c"(prologue)
            : "0"(src_ptr), "1"(dest_ptr), "2"(prologue)
            : "memory");
    }
    for (u32 i = len / 64; i; --i) {
        asm volatile(
            "movq (%0), %%mm0\n"
            "movq 8(%0), %%mm1\n"
            "movq 16(%0), %%mm2\n"
            "movq 24(%0), %%mm3\n"
            "movq 32(%0), %%mm4\n"
            "movq 40(%0), %%mm5\n"
            "movq 48(%0), %%mm6\n"
            "movq 56(%0), %%mm7\n"
            "movq %%mm0, (%1)\n"
            "movq %%mm1, 8(%1)\n"
            "movq %%mm2, 16(%1)\n"
            "movq %%mm3, 24(%1)\n"
            "movq %%mm4, 32(%1)\n"
            "movq %%mm5, 40(%1)\n"
            "movq %%mm6, 48(%1)\n"
            "movq %%mm7, 56(%1)\n" ::"r"(src_ptr),
            "r"(dest_ptr)
            : "memory");
        src_ptr += 64;
        dest_ptr += 64;
    }
    asm volatile("emms" ::
                     : "memory");
    // Whatever remains we'll have to memcpy.
    len %= 64;
    if (len)
        memcpy(dest_ptr, src_ptr, len);
    return dest;
}

void* memcpy(void* dest_ptr, const void* src_ptr, size_t n)
{
    if (n >= 1024)
        return mmx_memcpy(dest_ptr, src_ptr, n);

    u32 dest = (u32)dest_ptr;
    u32 src = (u32)src_ptr;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && !(src & 0x3) && n >= 12) {
        size_t u32s = n / sizeof(u32);
        asm volatile(
            "rep movsl\n"
            : "=S"(src), "=D"(dest)
            : "S"(src), "D"(dest), "c"(u32s)
            : "memory");
        n -= u32s * sizeof(u32);
        if (n == 0)
            return dest_ptr;
    }
    asm volatile(
        "rep movsb\n" ::"S"(src), "D"(dest), "c"(n)
        : "memory");
    return dest_ptr;
}

void* memset(void* dest_ptr, int c, size_t n)
{
    u32 dest = (u32)dest_ptr;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && n >= 12) {
        size_t u32s = n / sizeof(u32);
        u32 expanded_c = (u8)c;
        expanded_c |= expanded_c << 8;
        expanded_c |= expanded_c << 16;
        asm volatile(
            "rep stosl\n"
            : "=D"(dest)
            : "D"(dest), "c"(u32s), "a"(expanded_c)
            : "memory");
        n -= u32s * sizeof(u32);
        if (n == 0)
            return dest_ptr;
    }
    asm volatile(
        "rep stosb\n"
        : "=D"(dest), "=c"(n)
        : "0"(dest), "1"(n), "a"(c)
        : "memory");
    return dest_ptr;
}
#else
void* memcpy(void* dest_ptr, const void* src_ptr, size_t n)
{
    auto* dest = (u8*)dest_ptr;
    auto* src = (const u8*)src_ptr;
    for (size_t i = 0; i < n; ++i)
        dest[i] = src[i];
    return dest_ptr;
}

void* memset(void* dest_ptr, int c, size_t n)
{
    auto* dest = (u8*)dest_ptr;
    for (size_t i = 0; i < n; ++i)
        dest[i] = (u8)c;
    return dest_ptr;
}
#endif

void* memmove(void* dest, const void* src, size_t n)
{
    if (dest < src)
        return memcpy(dest, src, n);

    u8* pd = (u8*)dest;
    const u8* ps = (const u8*)src;
    for (pd += n, ps += n; n--;)
        *--pd = *--ps;
    return dest;
}

char* strcpy(char* dest, const char* src)
{
    char* originalDest = dest;
    while ((*dest++ = *src++) != '\0')
        ;
    return originalDest;
}

char* strncpy(char* dest, const char* src, size_t n)
{
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; ++i)
        dest[i] = src[i];
    for (; i < n; ++i)
        dest[i] = '\0';
    return dest;
}

char* strchr(const char* str, int c)
{
    char ch = c;
    for (;; ++str) {
        if (*str == ch)
            return const_cast<char*>(str);
        if (!*str)
            return nullptr;
    }
}

void* memchr(const void* ptr, int c, size_t size)
{
    char ch = c;
    auto* cptr = (const char*)ptr;
    for (size_t i = 0; i < size; ++i) {
        if (cptr[i] == ch)
            return const_cast<char*>(cptr + i);
    }
    return nullptr;
}

char* strrchr(const char* str, int ch)
{
    char* last = nullptr;
    char c;
    for (; (c = *str); ++str) {
        if (c == ch)
            last = const_cast<char*>(str);
    }
    return last;
}

char* strcat(char* dest, const char* src)
{
    size_t dest_length = strlen(dest);
    size_t i;
    for (i = 0; src[i] != '\0'; i++)
        dest[dest_length + i] = src[i];
    dest[dest_length + i] = '\0';
    return dest;
}

char* strncat(char* dest, const char* src, size_t n)
{
    size_t dest_length = strlen(dest);
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; i++)
        dest[dest_length + i] = src[i];
    dest[dest_length + i] = '\0';
    return dest;
}

const char* const sys_errlist[] = {
    "Success (not an error)",
    "Operation not permitted",
    "No such file or directory",
    "No such process",
    "Interrupted syscall",
    "I/O error",
    "No such device or address",
    "Argument list too long",
    "Exec format error",
    "Bad fd number",
    "No child processes",
    "Try again",
    "Out of memory",
    "Permission denied",
    "Bad address",
    "Block device required",
    "Device or resource busy",
    "File already exists",
    "Cross-device link",
    "No such device",
    "Not a directory",
    "Is a directory",
    "Invalid argument",
    "File table overflow",
    "Too many open files",
    "Not a TTY",
    "Text file busy",
    "File too large",
    "No space left on device",
    "Illegal seek",
    "Read-only filesystem",
    "Too many links",
    "Broken pipe",
    "Range error",
    "Name too long",
    "Too many symlinks",
    "Overflow",
    "Operation not supported",
    "No such syscall",
    "Not implemented",
    "Address family not supported",
    "Not a socket",
    "Address in use",
    "Failed without setting an error code (bug!)",
    "Directory not empty",
    "Math argument out of domain",
    "Connection refused",
    "Address not available",
    "Already connected",
    "Connection aborted",
    "Connection already in progress",
    "Connection reset",
    "Desination address required",
    "Host unreachable",
    "Illegal byte sequence",
    "Message size",
    "Network down",
    "Network unreachable",
    "Network reset",
    "No buffer space",
    "No lock available",
    "No message",
    "No protocol option",
    "Not connected",
    "Operation would block",
    "Protocol not supported",
    "Resource deadlock would occur",
    "Timed out",
    "Wrong protocol type",
    "Operation in progress",
    "No such thread",
    "Protocol error",
    "Not supported",
    "The highest errno +1 :^)",
};

int sys_nerr = EMAXERRNO;

char* strerror(int errnum)
{
    if (errnum >= EMAXERRNO) {
        printf("strerror() missing string for errnum=%d\n", errnum);
        return const_cast<char*>("Unknown error");
    }
    return const_cast<char*>(sys_errlist[errnum]);
}

char* strsignal(int signum)
{
    if (signum >= NSIG) {
        printf("strsignal() missing string for signum=%d\n", signum);
        return const_cast<char*>("Unknown signal");
    }
    return const_cast<char*>(sys_siglist[signum]);
}

char* strstr(const char* haystack, const char* needle)
{
    char nch;
    char hch;

    if ((nch = *needle++) != 0) {
        size_t len = strlen(needle);
        do {
            do {
                if ((hch = *haystack++) == 0)
                    return nullptr;
            } while (hch != nch);
        } while (strncmp(haystack, needle, len) != 0);
        --haystack;
    }
    return const_cast<char*>(haystack);
}

char* strpbrk(const char* s, const char* accept)
{
    while (*s)
        if (strchr(accept, *s++))
            return const_cast<char*>(--s);
    return nullptr;
}

char* strtok_r(char* str, const char* delim, char** saved_str)
{
    if (!str) {
        if (!saved_str)
            return nullptr;
        str = *saved_str;
    }

    size_t token_start = 0;
    size_t token_end = 0;
    size_t str_len = strlen(str);
    size_t delim_len = strlen(delim);

    for (size_t i = 0; i < str_len; ++i) {
        bool is_proper_delim = false;

        for (size_t j = 0; j < delim_len; ++j) {
            if (str[i] == delim[j]) {
                // Skip beginning delimiters
                if (token_end - token_start == 0) {
                    ++token_start;
                    break;
                }

                is_proper_delim = true;
            }
        }

        ++token_end;
        if (is_proper_delim && token_end > 0) {
            --token_end;
            break;
        }
    }

    if (str[token_start] == '\0')
        return nullptr;

    if (token_end == 0) {
        *saved_str = nullptr;
        return &str[token_start];
    }

    *saved_str = &str[token_end + 1];
    str[token_end] = '\0';
    return &str[token_start];
}

char* strtok(char* str, const char* delim)
{
    static char* saved_str;
    return strtok_r(str, delim, &saved_str);
}

int strcoll(const char* s1, const char* s2)
{
    return strcmp(s1, s2);
}

size_t strxfrm(char* dest, const char* src, size_t n)
{
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; ++i)
        dest[i] = src[i];
    for (; i < n; ++i)
        dest[i] = '\0';
    return i;
}
}
