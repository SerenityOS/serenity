/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define AT_NULL 0           /* No length, last entry's a_type has this value */
#define AT_IGNORE 1         /* Entry has no meaning, a_un undefined */
#define AT_EXECFD 2         /* a_val contains a file descriptor of the main program image */
#define AT_PHDR 3           /* a_ptr contains pointer to program header table of main program image */
#define AT_PHENT 4          /* a_val holds size of program header table entries */
#define AT_PHNUM 5          /* a_val holds number of program header table entries */
#define AT_PAGESZ 6         /* a_val gives system page size in bytes */
#define AT_BASE 7           /* a_ptr holds base address that Loader was loaded into memory */
#define AT_FLAGS 8          /* a_val holds 1 bit flags. Undefined flags are 0 */
#define AT_ENTRY 9          /* a_ptr holds entry point of the main program */
#define AT_NOTELF 10        /* a_val non-zero if the program is not ELF */
#define AT_UID 11           /* a_val holds real user id of process */
#define AT_EUID 12          /* a_val holds effective user id of process */
#define AT_GID 13           /* a_val holds real group id of process */
#define AT_EGID 14          /* a_val holds effective group id of process */
#define AT_PLATFORM 15      /* a_val points to a string containing platform name */
#define AT_HWCAP 16         /* a_val contains bitmask of CPU features. Equivalent to CPUID 1.EDX*/
#define AT_CLKTCK 17        /* a_val contains frequency at which times() increments. (Re: Spec. What is times()?) */
#define AT_SECURE 23        /* a_val holds 1 if program in secure mode (e.g. suid). Otherwise 0 */
#define AT_BASE_PLATFORM 24 /* a_ptr points to a string identifying base platform name, which might be different from platform (e.g x86_64 when in i386 compat)  */
#define AT_RANDOM 25        /* a_ptr points to 16 securely generated random bytes */
#define AT_HWCAP2 26        /* a_val holds extended hw feature mask. Currently 0 */
#define AT_EXECFN 31        /* a_ptr points to filename of executed program */
#define AT_EXE_BASE 32      /* a_ptr holds base address where main program was loaded into memory */
#define AT_EXE_SIZE 33      /* a_val holds the size of the main program in memory */

/* Auxiliary Vector types, from Intel386 ABI ver 1.0 section 2.3.3 */
typedef struct
{
    long a_type; /* Note: Extended to long from int, for ease of compatibility w/64 bit */
    union {
        long a_val;
        void* a_ptr;
        void (*a_fnc)(void); /* In spec, not used */
    } a_un;
} auxv_t;
