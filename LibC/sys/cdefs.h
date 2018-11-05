#pragma once

#define ALWAYS_INLINE inline __attribute__ ((always_inline))
#define __NORETURN __attribute__ ((noreturn))

#ifdef __cplusplus
#define __BEGIN_DECLS extern "C" {
#define __END_DECLS }
#else
#define __BEGIN_DECLS
#define __END_DECLS
#endif

#undef __P
#define __P(a) a
