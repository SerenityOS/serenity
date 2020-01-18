/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*-
 * Copyright (c) 1980, 1983, 1990 The Regents of the University of California.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *	This product includes software developed by the University of
 *	California, Berkeley and its contributors.
 * 4. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#if defined(LIBC_SCCS) && !defined(lint)
static char sccsid[] = "@(#)qsort.c	5.9 (Berkeley) 2/23/91";
#endif /* LIBC_SCCS and not lint */

#include <stdlib.h>
#include <sys/types.h>

static void insertion_sort(void* bot, size_t nmemb, size_t size, int (*compar)(const void*, const void*));
static void insertion_sort_r(void* bot, size_t nmemb, size_t size, int (*compar)(const void*, const void*, void*), void* arg);

void qsort(void* bot, size_t nmemb, size_t size, int (*compar)(const void*, const void*))
{
    if (nmemb <= 1)
        return;

    insertion_sort(bot, nmemb, size, compar);
}

void qsort_r(void* bot, size_t nmemb, size_t size, int (*compar)(const void*, const void*, void*), void* arg)
{
    if (nmemb <= 1)
        return;

    insertion_sort_r(bot, nmemb, size, compar, arg);
}

void insertion_sort(void* bot, size_t nmemb, size_t size, int (*compar)(const void*, const void*))
{
    int cnt;
    unsigned char ch;
    char *s1, *s2, *t1, *t2, *top;
    top = (char*)bot + nmemb * size;
    for (t1 = (char*)bot + size; t1 < top;) {
        for (t2 = t1; (t2 -= size) >= bot && compar(t1, t2) < 0;)
            ;
        if (t1 != (t2 += size)) {
            for (cnt = size; cnt--; ++t1) {
                ch = *t1;
                for (s1 = s2 = t1; (s2 -= size) >= t2; s1 = s2)
                    *s1 = *s2;
                *s1 = ch;
            }
        } else
            t1 += size;
    }
}

void insertion_sort_r(void* bot, size_t nmemb, size_t size, int (*compar)(const void*, const void*, void*), void* arg)
{
    int cnt;
    unsigned char ch;
    char *s1, *s2, *t1, *t2, *top;
    top = (char*)bot + nmemb * size;
    for (t1 = (char*)bot + size; t1 < top;) {
        for (t2 = t1; (t2 -= size) >= bot && compar(t1, t2, arg) < 0;)
            ;
        if (t1 != (t2 += size)) {
            for (cnt = size; cnt--; ++t1) {
                ch = *t1;
                for (s1 = s2 = t1; (s2 -= size) >= t2; s1 = s2)
                    *s1 = *s2;
                *s1 = ch;
            }
        } else
            t1 += size;
    }
}
