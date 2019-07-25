#pragma once

#ifdef __i386__
#define AK_ARCH_I386 1
#endif

#ifdef __x86_64__
#define AK_ARCH_X86_64 1
#endif

#define ARCH(arch) (defined(AK_ARCH_##arch) && AK_ARCH_##arch)

#ifdef __clang__
#    define CONSUMABLE(initial_state) __attribute__((consumable(initial_state)))
#    define CALLABLE_WHEN(...) __attribute__((callable_when(__VA_ARGS__)))
#    define SET_TYPESTATE(state) __attribute__((set_typestate(state)))
#    define RETURN_TYPESTATE(state) __attribute__((return_typestate(state)))
#else
#    define CONSUMABLE(initial_state)
#    define CALLABLE_WHEN(...)
#    define SET_TYPESTATE(state)
#    define RETURN_TYPESTATE(state)
#endif

#ifndef __serenity__
#define PAGE_SIZE sysconf(_SC_PAGESIZE)
#endif
