#pragma once

#ifdef __i386__
#define AK_ARCH_I386 1
#endif

#ifdef __x86_64__
#define AK_ARCH_X86_64 1
#endif

#define ARCH(arch) (defined(AK_ARCH_##arch) && AK_ARCH_##arch)

