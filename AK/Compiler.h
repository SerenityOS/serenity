#pragma once

#define PACKED __attribute__ ((packed))
#define NORETURN __attribute__ ((noreturn))
#undef ALWAYS_INLINE
#define ALWAYS_INLINE inline __attribute__ ((always_inline))
#define NEVER_INLINE __attribute__ ((noinline))
#define MALLOC_ATTR __attribute__ ((malloc))
#define PURE __attribute__ ((pure))
#define WARN_UNUSED_RESULT __attribute__ ((warn_unused_result))
