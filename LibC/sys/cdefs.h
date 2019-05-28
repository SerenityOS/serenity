#pragma once

#define _POSIX_VERSION 200809L

#define ALWAYS_INLINE inline __attribute__((always_inline))

#ifdef __cplusplus
#    define __BEGIN_DECLS extern "C" {
#    define __END_DECLS }
#else
#    define __BEGIN_DECLS
#    define __END_DECLS
#endif

#undef __P
#define __P(a) a

#define offsetof(type, member) __builtin_offsetof(type, member)

#ifdef __cplusplus
//extern "C" int main(int, char**);
#endif
