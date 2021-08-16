/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include <assert.h>
#include <limits.h>
#include <setjmp.h>
#include <stdlib.h>
#include <string.h>

#include "jni.h"
#include "jvm.h"
#include "check_classname.h"

typedef unsigned short unicode;

static char *
skip_over_fieldname(char *name, jboolean slash_okay,
                    unsigned int len);
static char *
skip_over_field_signature(char *name, jboolean void_okay,
                          unsigned int len);

/*
 * Return non-zero if the character is a valid in JVM class name, zero
 * otherwise.  The only characters currently disallowed from JVM class
 * names are given in the table below:
 *
 * Character    Hex     Decimal
 * '.'          0x2e    46
 * '/'          0x2f    47
 * ';'          0x3b    59
 * '['          0x5b    91
 *
 * (Method names have further restrictions dealing with the '<' and
 * '>' characters.)
 */
static int isJvmIdentifier(unicode ch) {
  if( ch > 91 || ch < 46 )
    return 1;   /* Lowercase ASCII letters are > 91 */
  else { /* 46 <= ch <= 91 */
    if (ch <= 90 && ch >= 60) {
      return 1; /* Uppercase ASCII recognized here */
    } else { /* ch == 91 || 46 <= ch <= 59 */
      if (ch == 91 || ch == 59 || ch <= 47)
        return 0;
      else
        return 1;
    }
  }
}

static unicode
next_utf2unicode(char **utfstring_ptr, int * valid)
{
    unsigned char *ptr = (unsigned char *)(*utfstring_ptr);
    unsigned char ch, ch2, ch3;
    int length = 1;             /* default length */
    unicode result = 0x80;      /* default bad result; */
    *valid = 1;
    switch ((ch = ptr[0]) >> 4) {
        default:
            result = ch;
            break;

        case 0x8: case 0x9: case 0xA: case 0xB: case 0xF:
            /* Shouldn't happen. */
            *valid = 0;
            break;

        case 0xC: case 0xD:
            /* 110xxxxx  10xxxxxx */
            if (((ch2 = ptr[1]) & 0xC0) == 0x80) {
                unsigned char high_five = ch & 0x1F;
                unsigned char low_six = ch2 & 0x3F;
                result = (high_five << 6) + low_six;
                length = 2;
            }
            break;

        case 0xE:
            /* 1110xxxx 10xxxxxx 10xxxxxx */
            if (((ch2 = ptr[1]) & 0xC0) == 0x80) {
                if (((ch3 = ptr[2]) & 0xC0) == 0x80) {
                    unsigned char high_four = ch & 0x0f;
                    unsigned char mid_six = ch2 & 0x3f;
                    unsigned char low_six = ch3 & 0x3f;
                    result = (((high_four << 6) + mid_six) << 6) + low_six;
                    length = 3;
                } else {
                    length = 2;
                }
            }
            break;
        } /* end of switch */

    *utfstring_ptr = (char *)(ptr + length);
    return result;
}

/* Take pointer to a string.  Skip over the longest part of the string that
 * could be taken as a fieldname.  Allow '/' if slash_okay is JNI_TRUE.
 *
 * Return a pointer to just past the fieldname.  Return NULL if no fieldname
 * at all was found, or in the case of slash_okay being true, we saw
 * consecutive slashes (meaning we were looking for a qualified path but
 * found something that was badly-formed).
 */
static char *
skip_over_fieldname(char *name, jboolean slash_okay,
                    unsigned int length)
{
    char *p;
    unicode ch;
    unicode last_ch = 0;
    int valid = 1;
    /* last_ch == 0 implies we are looking at the first char. */
    for (p = name; p != name + length; last_ch = ch) {
        char *old_p = p;
        ch = *p;
        if (ch < 128) {
            p++;
            if (isJvmIdentifier(ch)) {
                continue;
            }
        } else {
            char *tmp_p = p;
            ch = next_utf2unicode(&tmp_p, &valid);
            if (valid == 0)
              return 0;
            p = tmp_p;
            if (isJvmIdentifier(ch)) {
                        continue;
            }
        }

        if (slash_okay && ch == '/' && last_ch) {
            if (last_ch == '/') {
                return 0;       /* Don't permit consecutive slashes */
            }
        } else if (ch == '_' || ch == '$') {
        } else {
            return last_ch ? old_p : 0;
        }
    }
    return last_ch ? p : 0;
}

/* Take pointer to a string.  Skip over the longest part of the string that
 * could be taken as a field signature.  Allow "void" if void_okay.
 *
 * Return a pointer to just past the signature.  Return NULL if no legal
 * signature is found.
 */

static char *
skip_over_field_signature(char *name, jboolean void_okay,
                          unsigned int length)
{
    unsigned int array_dim = 0;
    for (;length > 0;) {
        switch (name[0]) {
            case JVM_SIGNATURE_VOID:
                if (!void_okay) return 0;
                /* FALL THROUGH */
            case JVM_SIGNATURE_BOOLEAN:
            case JVM_SIGNATURE_BYTE:
            case JVM_SIGNATURE_CHAR:
            case JVM_SIGNATURE_SHORT:
            case JVM_SIGNATURE_INT:
            case JVM_SIGNATURE_FLOAT:
            case JVM_SIGNATURE_LONG:
            case JVM_SIGNATURE_DOUBLE:
                return name + 1;

            case JVM_SIGNATURE_CLASS: {
                /* Skip over the classname, if one is there. */
                char *p =
                    skip_over_fieldname(name + 1, JNI_TRUE, --length);
                /* The next character better be a semicolon. */
                if (p && p - name - 1 > 0 && p[0] == ';')
                    return p + 1;
                return 0;
            }

            case JVM_SIGNATURE_ARRAY:
                array_dim++;
                /* JVMS 2nd ed. 4.10 */
                /*   The number of dimensions in an array is limited to 255 ... */
                if (array_dim > 255) {
                    return 0;
                }
                /* The rest of what's there better be a legal signature.  */
                name++;
                length--;
                void_okay = JNI_FALSE;
                break;

            default:
                return 0;
        }
    }
    return 0;
}

/* Determine if the specified name is legal
 * UTF name for a classname.
 *
 * Note that this routine expects the internal form of qualified classes:
 * the dots should have been replaced by slashes.
 */
jboolean verifyClassname(char *name, jboolean allowArrayClass)
{
    size_t s = strlen(name);
    assert(s <= UINT_MAX);
    unsigned int length = (unsigned int)s;
    char *p;

    if (length > 0 && name[0] == JVM_SIGNATURE_ARRAY) {
        if (!allowArrayClass) {
            return JNI_FALSE;
        } else {
            /* Everything that's left better be a field signature */
            p = skip_over_field_signature(name, JNI_FALSE, length);
        }
    } else {
        /* skip over the fieldname.  Slashes are okay */
        p = skip_over_fieldname(name, JNI_TRUE, length);
    }
    return (p != 0 && p - name == (ptrdiff_t)length);
}

/*
 * Translates '.' to '/'.  Returns JNI_TRUE if any / were present.
 */
jboolean verifyFixClassname(char *name)
{
    char *p = name;
    jboolean slashesFound = JNI_FALSE;
    int valid = 1;

    while (valid != 0 && *p != '\0') {
        if (*p == '/') {
            slashesFound = JNI_TRUE;
            p++;
        } else if (*p == '.') {
            *p++ = '/';
        } else {
            next_utf2unicode(&p, &valid);
        }
    }

    return slashesFound && valid != 0;
}

/*
 * Translates '.' to '/'.
 */
void fixClassname(char *name)
{
    char *p = name;
    int valid = 1;

    while (valid != 0 && *p != '\0') {
        if (*p == '.') {
            *p++ = '/';
        } else {
            next_utf2unicode(&p, &valid);
        }
    }
}
