/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef SIZECALC_H
#define SIZECALC_H

/*
 * A machinery for safe calculation of sizes used when allocating memory.
 *
 * All size checks are performed against the SIZE_MAX (the maximum value for
 * size_t). All numerical arguments as well as the result of calculation must
 * be non-negative integers less than or equal to SIZE_MAX, otherwise the
 * calculated size is considered unsafe.
 *
 * If the SIZECALC_ALLOC_THROWING_BAD_ALLOC macro is defined, then _ALLOC_
 * helper macros throw the std::bad_alloc instead of returning NULL.
 */

#include <stdint.h> /* SIZE_MAX for C99+ */
/* http://stackoverflow.com/questions/3472311/what-is-a-portable-method-to-find-the-maximum-value-of-size-t */
#ifndef SIZE_MAX
#define SIZE_MAX ((size_t)-1)
#endif

#define IS_SAFE_SIZE_T(x) ((x) >= 0 && (unsigned long long)(x) <= SIZE_MAX)

#define IS_SAFE_SIZE_MUL(m, n) \
    (IS_SAFE_SIZE_T(m) && IS_SAFE_SIZE_T(n) && ((m) == 0 || (n) == 0 || (size_t)(n) <= (SIZE_MAX / (size_t)(m))))

#define IS_SAFE_SIZE_ADD(a, b) \
    (IS_SAFE_SIZE_T(a) && IS_SAFE_SIZE_T(b) && (size_t)(b) <= (SIZE_MAX - (size_t)(a)))



/* Helper macros */

#ifdef SIZECALC_ALLOC_THROWING_BAD_ALLOC
#define FAILURE_RESULT throw std::bad_alloc()
#else
#define FAILURE_RESULT NULL
#endif

/*
 * A helper macro to safely allocate an array of size m*n.
 * Example usage:
 *    int* p = (int*)SAFE_SIZE_ARRAY_ALLOC(malloc, sizeof(int), n);
 *    if (!p) throw OutOfMemory;
 *    // Use the allocated array...
 */
#define SAFE_SIZE_ARRAY_ALLOC(func, m, n) \
    (IS_SAFE_SIZE_MUL((m), (n)) ? ((func)((m) * (n))) : FAILURE_RESULT)

#define SAFE_SIZE_ARRAY_REALLOC(func, p, m, n) \
    (IS_SAFE_SIZE_MUL((m), (n)) ? ((func)((p), (m) * (n))) : FAILURE_RESULT)

/*
 * A helper macro to safely allocate an array of type 'type' with 'n' items
 * using the C++ new[] operator.
 * Example usage:
 *    MyClass* p = SAFE_SIZE_NEW_ARRAY(MyClass, n);
 *    // Use the pointer.
 * This macro throws the std::bad_alloc C++ exception to indicate
 * a failure.
 * NOTE: if 'n' is calculated, the calling code is responsible for using the
 * IS_SAFE_... macros to check if the calculations are safe.
 */
#define SAFE_SIZE_NEW_ARRAY(type, n) \
    (IS_SAFE_SIZE_MUL(sizeof(type), (n)) ? (new type[(n)]) : throw std::bad_alloc())

#define SAFE_SIZE_NEW_ARRAY2(type, n, m) \
    (IS_SAFE_SIZE_MUL((m), (n)) && IS_SAFE_SIZE_MUL(sizeof(type), (n) * (m)) ? \
     (new type[(n) * (m)]) : throw std::bad_alloc())

/*
 * Checks if a data structure of size (a + m*n) can be safely allocated
 * w/o producing an integer overflow when calculating its size.
 */
#define IS_SAFE_STRUCT_SIZE(a, m, n) \
    ( \
      IS_SAFE_SIZE_MUL((m), (n)) && IS_SAFE_SIZE_ADD((m) * (n), (a)) \
    )

/*
 * A helper macro for implementing safe memory allocation for a data structure
 * of size (a + m * n).
 * Example usage:
 *    void * p = SAFE_SIZE_ALLOC(malloc, header, num, itemSize);
 *    if (!p) throw OutOfMemory;
 *    // Use the allocated memory...
 */
#define SAFE_SIZE_STRUCT_ALLOC(func, a, m, n) \
    (IS_SAFE_STRUCT_SIZE((a), (m), (n)) ? ((func)((a) + (m) * (n))) : FAILURE_RESULT)


#endif /* SIZECALC_H */

