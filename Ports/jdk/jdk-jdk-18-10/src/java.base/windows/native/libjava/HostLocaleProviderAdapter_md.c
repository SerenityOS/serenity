/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_util_locale_provider_HostLocaleProviderAdapterImpl.h"
#include "jni_util.h"
#include <windows.h>
#include <gdefs.h>
#include <stdlib.h>

#define BUFLEN 256

// java.util.Calendar constants
#define CALENDAR_FIELD_ERA              0           // Calendar.ERA
#define CALENDAR_FIELD_MONTH            2           // Calendar.MONTH
#define CALENDAR_FIELD_DAY_OF_WEEK      7           // Calendar.DAY_OF_WEEK
#define CALENDAR_FIELD_AM_PM            9           // Calendar.AM_PM
#define CALENDAR_STYLE_SHORT_MASK       0x00000001  // Calendar.SHORT
#define CALENDAR_STYLE_STANDALONE_MASK  0x00008000  // Calendar.STANDALONE

// global variables
typedef int (WINAPI *PGLIE)(const jchar *, LCTYPE, LPWSTR, int);
typedef int (WINAPI *PGCIE)(const jchar *, CALID, LPCWSTR, CALTYPE, LPWSTR, int, LPDWORD);
typedef int (WINAPI *PECIEE)(CALINFO_ENUMPROCEXEX, const jchar *, CALID, LPCWSTR, CALTYPE, LPARAM);
PGLIE pGetLocaleInfoEx;
PGCIE pGetCalendarInfoEx;
PECIEE pEnumCalendarInfoExEx;
BOOL initialized = FALSE;

// prototypes
int getLocaleInfoWrapper(const jchar *langtag, LCTYPE type, LPWSTR data, int buflen);
int getCalendarInfoWrapper(const jchar *langtag, CALID id, LPCWSTR reserved, CALTYPE type, LPWSTR data, int buflen, LPDWORD val);
jint getCalendarID(const jchar *langtag);
void replaceCalendarArrayElems(JNIEnv *env, jstring jlangtag, jint calid, jobjectArray jarray, DWORD* pTypes, int offset, int length, int style, BOOL bCal);
WCHAR * getNumberPattern(const jchar * langtag, const jint numberStyle);
void getNumberPart(const jchar * langtag, const jint numberStyle, WCHAR * number);
void getFixPart(const jchar * langtag, const jint numberStyle, BOOL positive, BOOL prefix, WCHAR * ret);
int enumCalendarInfoWrapper(const jchar * langtag, CALID calid, CALTYPE type, LPWSTR buf, int buflen);
BOOL CALLBACK EnumCalendarInfoProc(LPWSTR lpCalInfoStr, CALID calid, LPWSTR lpReserved, LPARAM lParam);
jobjectArray getErasImpl(JNIEnv *env, jstring jlangtag, jint calid, jint style, jobjectArray eras);

// from java_props_md.c
extern __declspec(dllexport) const char * getJavaIDFromLangID(LANGID langID);

CALTYPE monthsType[] = {
    CAL_SMONTHNAME1,
    CAL_SMONTHNAME2,
    CAL_SMONTHNAME3,
    CAL_SMONTHNAME4,
    CAL_SMONTHNAME5,
    CAL_SMONTHNAME6,
    CAL_SMONTHNAME7,
    CAL_SMONTHNAME8,
    CAL_SMONTHNAME9,
    CAL_SMONTHNAME10,
    CAL_SMONTHNAME11,
    CAL_SMONTHNAME12,
    CAL_SMONTHNAME13,
};

CALTYPE sMonthsType[] = {
    CAL_SABBREVMONTHNAME1,
    CAL_SABBREVMONTHNAME2,
    CAL_SABBREVMONTHNAME3,
    CAL_SABBREVMONTHNAME4,
    CAL_SABBREVMONTHNAME5,
    CAL_SABBREVMONTHNAME6,
    CAL_SABBREVMONTHNAME7,
    CAL_SABBREVMONTHNAME8,
    CAL_SABBREVMONTHNAME9,
    CAL_SABBREVMONTHNAME10,
    CAL_SABBREVMONTHNAME11,
    CAL_SABBREVMONTHNAME12,
    CAL_SABBREVMONTHNAME13,
};

#define MONTHTYPES (sizeof(monthsType) / sizeof(CALTYPE))

CALTYPE wDaysType[] = {
    CAL_SDAYNAME7,
    CAL_SDAYNAME1,
    CAL_SDAYNAME2,
    CAL_SDAYNAME3,
    CAL_SDAYNAME4,
    CAL_SDAYNAME5,
    CAL_SDAYNAME6,
};

CALTYPE sWDaysType[] = {
    CAL_SABBREVDAYNAME7,
    CAL_SABBREVDAYNAME1,
    CAL_SABBREVDAYNAME2,
    CAL_SABBREVDAYNAME3,
    CAL_SABBREVDAYNAME4,
    CAL_SABBREVDAYNAME5,
    CAL_SABBREVDAYNAME6,
};

#define DOWTYPES (sizeof(wDaysType) / sizeof(CALTYPE))

LCTYPE ampmType [] = {
    LOCALE_SAM,
    LOCALE_SPM,
};

#define AMPMTYPES (sizeof(ampmType) / sizeof(LCTYPE))

WCHAR * fixes[2][2][3][16] =
{
    { //prefix
        { //positive
            { // number
                L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"",
            },
            { // currency
                L"\xA4", L"", L"\xA4 ", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"",
            },
            { // percent
                L"", L"", L"%", L"% ", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"",
            }
        },
        { // negative
            { // number
                L"(", L"-", L"- ", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"",
            },
            { //currency
                L"(\xA4", L"-\xA4", L"\xA4-", L"\xA4", L"(", L"-", L"", L"", L"-", L"-\xA4 ", L"", L"\xA4 ", L"\xA4 -", L"", L"(\xA4 ", L"("
            },
            { // percent
                L"-", L"-", L"-%", L"%-", L"%", L"", L"", L"-% ", L"", L"% ", L"% -", L"", L"", L"", L"", L"",
            }
        }
    },
    { // suffix
        { //positive
            { // number
                L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L""
            },
            { // currency
                L"", L"\xA4 ", L"", L" \xA4", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"",
            },
            { // percent
                L" %", L"%", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"",
            }
        },
        { // negative
            { // number
                L")", L"", L" ", L"-", L" -", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"", L"",
            },
            { //currency
                L")", L"", L"", L"-", L"\xA4)", L"\xA4", L"-\xA4", L"\xA4-", L" \xA4", L"", L" \xA4-", L"-", L"", L"- \xA4", L")", L" \xA4)"
            },
            { // percent
                L" %", L"%", L"", L"", L"-", L"-%", L"%-", L"", L" %-", L"-", L"", L"- %", L"", L"", L"", L"",
            }
        }
    }
};

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    initialize
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_initialize
  (JNIEnv *env, jclass cls) {
    if (!initialized) {
        pGetLocaleInfoEx = (PGLIE)GetProcAddress(
            GetModuleHandle("kernel32.dll"),
            "GetLocaleInfoEx");
        pGetCalendarInfoEx = (PGCIE)GetProcAddress(
            GetModuleHandle("kernel32.dll"),
            "GetCalendarInfoEx");
        pEnumCalendarInfoExEx = (PECIEE)GetProcAddress(
            GetModuleHandle("kernel32.dll"),
            "EnumCalendarInfoExEx");
        initialized =TRUE;
    }

    return pGetLocaleInfoEx != NULL &&
           pGetCalendarInfoEx != NULL &&
           pEnumCalendarInfoExEx != NULL;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getDefaultLocale
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getDefaultLocale
  (JNIEnv *env, jclass cls, jint cat) {
    char * localeString = NULL;
    LANGID langid;
    jstring ret;

    switch (cat) {
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_CAT_DISPLAY:
            langid = LANGIDFROMLCID(GetUserDefaultUILanguage());
            break;
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_CAT_FORMAT:
        default:
            langid = LANGIDFROMLCID(GetUserDefaultLCID());
            break;
    }

    localeString = (char *)getJavaIDFromLangID(langid);
    if (localeString != NULL) {
        ret = (*env)->NewStringUTF(env, localeString);
        free(localeString);
    } else {
        JNU_ThrowOutOfMemoryError(env, "memory allocation error");
        ret = NULL;
    }
    return ret;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getDateTimePattern
 * Signature: (IILjava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getDateTimePattern
  (JNIEnv *env, jclass cls, jint dateStyle, jint timeStyle, jstring jlangtag) {
    WCHAR pattern[BUFLEN];
    const jchar *langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, NULL);

    pattern[0] = L'\0';

    if (dateStyle == 0 || dateStyle == 1) {
        getLocaleInfoWrapper(langtag, LOCALE_SLONGDATE, pattern, BUFLEN);
    } else if (dateStyle == 2 || dateStyle == 3) {
        getLocaleInfoWrapper(langtag, LOCALE_SSHORTDATE, pattern, BUFLEN);
    }

    if (timeStyle == 0 || timeStyle == 1) {
        getLocaleInfoWrapper(langtag, LOCALE_STIMEFORMAT, pattern, BUFLEN);
    } else if (timeStyle == 2 || timeStyle == 3) {
        getLocaleInfoWrapper(langtag, LOCALE_SSHORTTIME, pattern, BUFLEN);
    }

    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    return (*env)->NewString(env, pattern, (jsize)wcslen(pattern));
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getCalendarID
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getCalendarID
  (JNIEnv *env, jclass cls, jstring jlangtag) {
    const jchar *langtag;
    jint ret;
    langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, 0);
    ret = getCalendarID(langtag);
    (*env)->ReleaseStringChars(env, jlangtag, langtag);
    return ret;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getAmPmStrings
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getAmPmStrings
  (JNIEnv *env, jclass cls, jstring jlangtag, jobjectArray ampms) {
    replaceCalendarArrayElems(env, jlangtag, -1, ampms, ampmType,
                          0, AMPMTYPES, 0, FALSE);
    return ampms;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getEras
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getEras
  (JNIEnv *env, jclass cls, jstring jlangtag, jobjectArray eras) {
    return getErasImpl(env, jlangtag, -1, 0, eras);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getMonths
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getMonths
  (JNIEnv *env, jclass cls, jstring jlangtag, jobjectArray months) {
    replaceCalendarArrayElems(env, jlangtag, -1, months, monthsType,
                      0, MONTHTYPES, 0, TRUE);
    return months;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getShortMonths
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getShortMonths
  (JNIEnv *env, jclass cls, jstring jlangtag, jobjectArray smonths) {
    replaceCalendarArrayElems(env, jlangtag, -1, smonths, sMonthsType,
                      0, MONTHTYPES, 0, TRUE);
    return smonths;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getWeekdays
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getWeekdays
  (JNIEnv *env, jclass cls, jstring jlangtag, jobjectArray wdays) {
    replaceCalendarArrayElems(env, jlangtag, -1, wdays, wDaysType,
                      1, DOWTYPES, 0, TRUE);
    return wdays;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getShortWeekdays
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getShortWeekdays
  (JNIEnv *env, jclass cls, jstring jlangtag, jobjectArray swdays) {
    replaceCalendarArrayElems(env, jlangtag, -1, swdays, sWDaysType,
                      1, DOWTYPES, 0, TRUE);
    return swdays;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getNumberPattern
 * Signature: (ILjava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getNumberPattern
  (JNIEnv *env, jclass cls, jint numberStyle, jstring jlangtag) {
    const jchar *langtag;
    jstring ret;
    WCHAR * pattern;

    langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, NULL);
    pattern = getNumberPattern(langtag, numberStyle);
    CHECK_NULL_RETURN(pattern, NULL);

    (*env)->ReleaseStringChars(env, jlangtag, langtag);
    ret = (*env)->NewString(env, pattern, (jsize)wcslen(pattern));
    free(pattern);

    return ret;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    isNativeDigit
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_isNativeDigit
  (JNIEnv *env, jclass cls, jstring jlangtag) {
    DWORD num;
    int got;
    const jchar *langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, JNI_FALSE);
    got = getLocaleInfoWrapper(langtag,
        LOCALE_IDIGITSUBSTITUTION | LOCALE_RETURN_NUMBER,
        (LPWSTR)&num, sizeof(num));
    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    return got && num == 2; // 2: native digit substitution
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getCurrencySymbol
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getCurrencySymbol
  (JNIEnv *env, jclass cls, jstring jlangtag, jstring currencySymbol) {
    WCHAR buf[BUFLEN];
    int got;
    const jchar *langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, currencySymbol);
    got = getLocaleInfoWrapper(langtag, LOCALE_SCURRENCY, buf, BUFLEN);
    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    if (got) {
        return (*env)->NewString(env, buf, (jsize)wcslen(buf));
    } else {
        return currencySymbol;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getDecimalSeparator
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getDecimalSeparator
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar decimalSeparator) {
    WCHAR buf[BUFLEN];
    int got;
    const jchar *langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, decimalSeparator);
    got = getLocaleInfoWrapper(langtag, LOCALE_SDECIMAL, buf, BUFLEN);
    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    if (got) {
        return buf[0];
    } else {
        return decimalSeparator;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getGroupingSeparator
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getGroupingSeparator
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar groupingSeparator) {
    WCHAR buf[BUFLEN];
    int got;
    const jchar *langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, groupingSeparator);
    got = getLocaleInfoWrapper(langtag, LOCALE_STHOUSAND, buf, BUFLEN);
    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    if (got) {
        return buf[0];
    } else {
        return groupingSeparator;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getInfinity
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getInfinity
  (JNIEnv *env, jclass cls, jstring jlangtag, jstring infinity) {
    WCHAR buf[BUFLEN];
    int got;
    const jchar *langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, infinity);
    got = getLocaleInfoWrapper(langtag, LOCALE_SPOSINFINITY, buf, BUFLEN);
    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    if (got) {
        return (*env)->NewString(env, buf, (jsize)wcslen(buf));
    } else {
        return infinity;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getInternationalCurrencySymbol
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getInternationalCurrencySymbol
  (JNIEnv *env, jclass cls, jstring jlangtag, jstring internationalCurrencySymbol) {
    WCHAR buf[BUFLEN];
    int got;
    const jchar *langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, internationalCurrencySymbol);
    got = getLocaleInfoWrapper(langtag, LOCALE_SINTLSYMBOL, buf, BUFLEN);
    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    if (got) {
        return (*env)->NewString(env, buf, (jsize)wcslen(buf));
    } else {
        return internationalCurrencySymbol;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getMinusSign
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getMinusSign
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar minusSign) {
    WCHAR buf[BUFLEN];
    int got;
    const jchar *langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, minusSign);
    got = getLocaleInfoWrapper(langtag, LOCALE_SNEGATIVESIGN, buf, BUFLEN);
    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    if (got) {
        return buf[0];
    } else {
        return minusSign;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getMonetaryDecimalSeparator
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getMonetaryDecimalSeparator
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar monetaryDecimalSeparator) {
    WCHAR buf[BUFLEN];
    int got;
    const jchar *langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, monetaryDecimalSeparator);
    got = getLocaleInfoWrapper(langtag, LOCALE_SMONDECIMALSEP, buf, BUFLEN);
    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    if (got) {
        return buf[0];
    } else {
        return monetaryDecimalSeparator;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getNaN
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getNaN
  (JNIEnv *env, jclass cls, jstring jlangtag, jstring nan) {
    WCHAR buf[BUFLEN];
    int got;
    const jchar *langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, nan);
    got = getLocaleInfoWrapper(langtag, LOCALE_SNAN, buf, BUFLEN);
    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    if (got) {
        return (*env)->NewString(env, buf, (jsize)wcslen(buf));
    } else {
        return nan;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getPercent
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getPercent
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar percent) {
    WCHAR buf[BUFLEN];
    int got;
    const jchar *langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, percent);
    got = getLocaleInfoWrapper(langtag, LOCALE_SPERCENT, buf, BUFLEN);
    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    if (got) {
        return buf[0];
    } else {
        return percent;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getPerMill
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getPerMill
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar perMill) {
    WCHAR buf[BUFLEN];
    const jchar *langtag;
    int got;
    langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, perMill);
    got = getLocaleInfoWrapper(langtag, LOCALE_SPERMILLE, buf, BUFLEN);

    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    if (got) {
        return buf[0];
    } else {
        return perMill;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getZeroDigit
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getZeroDigit
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar zeroDigit) {
    WCHAR buf[BUFLEN];
    const jchar *langtag;
    int got;
    langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, zeroDigit);
    got = getLocaleInfoWrapper(langtag, LOCALE_SNATIVEDIGITS, buf, BUFLEN);

    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    if (got) {
        return buf[0];
    } else {
        return zeroDigit;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getCalendarDataValue
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getCalendarDataValue
  (JNIEnv *env, jclass cls, jstring jlangtag, jint type) {
    DWORD num;
    const jchar *langtag;
    int got = 0;

    langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    CHECK_NULL_RETURN(langtag, -1);
    switch (type) {
    case sun_util_locale_provider_HostLocaleProviderAdapterImpl_CD_FIRSTDAYOFWEEK:
        got = getLocaleInfoWrapper(langtag,
            LOCALE_IFIRSTDAYOFWEEK | LOCALE_RETURN_NUMBER,
            (LPWSTR)&num, sizeof(num));
        break;
    case sun_util_locale_provider_HostLocaleProviderAdapterImpl_CD_FIRSTWEEKOFYEAR:
        got = getLocaleInfoWrapper(langtag,
            LOCALE_IFIRSTWEEKOFYEAR | LOCALE_RETURN_NUMBER,
            (LPWSTR)&num, sizeof(num));
        break;
    }

    (*env)->ReleaseStringChars(env, jlangtag, langtag);

    if (got) {
        return num;
    } else {
        return -1;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getCalendarDisplayStrings
 * Signature: (Ljava/lang/String;III)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getCalendarDisplayStrings
  (JNIEnv *env, jclass cls, jstring jlangtag, jint calid, jint field, jint style) {
    jobjectArray ret = NULL;
    CALTYPE * pCalType = NULL;

    switch (field) {
    case CALENDAR_FIELD_ERA:
        return getErasImpl(env, jlangtag, calid, style, NULL);

    case CALENDAR_FIELD_AM_PM:
        ret = (*env)->NewObjectArray(env, AMPMTYPES,
                (*env)->FindClass(env, "java/lang/String"), NULL);
        if (ret != NULL) {
            replaceCalendarArrayElems(env, jlangtag, calid, ret, ampmType,
                          0, AMPMTYPES, style, FALSE);
        }
        return ret;

    case CALENDAR_FIELD_DAY_OF_WEEK:
        ret = (*env)->NewObjectArray(env, DOWTYPES,
                (*env)->FindClass(env, "java/lang/String"), NULL);
        if (ret != NULL) {
            if (style & CALENDAR_STYLE_SHORT_MASK) {
                pCalType = sWDaysType;
            } else {
                pCalType = wDaysType;
            }

            replaceCalendarArrayElems(env, jlangtag, calid, ret, pCalType,
                          0, DOWTYPES, style, TRUE);
        }
        return ret;

    case CALENDAR_FIELD_MONTH:
        ret = (*env)->NewObjectArray(env, MONTHTYPES,
                (*env)->FindClass(env, "java/lang/String"), NULL);
        if (ret != NULL) {
            if (style & CALENDAR_STYLE_SHORT_MASK) {
                pCalType = sMonthsType;
            } else {
                pCalType = monthsType;
            }

            replaceCalendarArrayElems(env, jlangtag, calid, ret, pCalType,
                          0, MONTHTYPES, style, TRUE);
        }
        return ret;

    default:
        // not supported
        return NULL;
    }
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getDisplayString
 * Signature: (Ljava/lang/String;ILjava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getDisplayString
  (JNIEnv *env, jclass cls, jstring jlangtag, jint type, jstring jvalue) {
    LCTYPE lcType;
    jstring jStr;
    const jchar * pjChar;
    WCHAR buf[BUFLEN];
    int got = 0;

    switch (type) {
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_CURRENCY_NAME:
            lcType = LOCALE_SNATIVECURRNAME;
            jStr = jlangtag;
            break;
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_CURRENCY_SYMBOL:
            lcType = LOCALE_SCURRENCY;
            jStr = jlangtag;
            break;
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_LOCALE_LANGUAGE:
            lcType = LOCALE_SLOCALIZEDLANGUAGENAME;
            jStr = jvalue;
            break;
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_LOCALE_REGION:
            lcType = LOCALE_SLOCALIZEDCOUNTRYNAME;
            jStr = jvalue;
            break;
        default:
            return NULL;
    }

    pjChar = (*env)->GetStringChars(env, jStr, JNI_FALSE);
    CHECK_NULL_RETURN(pjChar, NULL);
    got = getLocaleInfoWrapper(pjChar, lcType, buf, BUFLEN);
    (*env)->ReleaseStringChars(env, jStr, pjChar);

    if (got) {
        return (*env)->NewString(env, buf, (jsize)wcslen(buf));
    } else {
        return NULL;
    }
}

int getLocaleInfoWrapper(const jchar *langtag, LCTYPE type, LPWSTR data, int buflen) {
    if (pGetLocaleInfoEx) {
        if (wcscmp(L"und", (LPWSTR)langtag) == 0) {
            // defaults to "en"
            return pGetLocaleInfoEx(L"en", type, data, buflen);
        } else {
            return pGetLocaleInfoEx((LPWSTR)langtag, type, data, buflen);
        }
    } else {
        // If we ever wanted to support WinXP, we will need extra module from
        // MS...
        // return GetLocaleInfo(DownlevelLocaleNameToLCID(langtag, 0), type, data, buflen);
        return 0;
    }
}

int getCalendarInfoWrapper(const jchar *langtag, CALID id, LPCWSTR reserved, CALTYPE type, LPWSTR data, int buflen, LPDWORD val) {
    if (pGetCalendarInfoEx) {
        if (wcscmp(L"und", (LPWSTR)langtag) == 0) {
            // defaults to "en"
            return pGetCalendarInfoEx(L"en", id, reserved, type, data, buflen, val);
        } else {
            return pGetCalendarInfoEx((LPWSTR)langtag, id, reserved, type, data, buflen, val);
        }
    } else {
        // If we ever wanted to support WinXP, we will need extra module from
        // MS...
        // return GetCalendarInfo(DownlevelLocaleNameToLCID(langtag, 0), ...);
        return 0;
    }
}

jint getCalendarID(const jchar *langtag) {
    DWORD type = -1;
    int got = getLocaleInfoWrapper(langtag,
        LOCALE_ICALENDARTYPE | LOCALE_RETURN_NUMBER,
        (LPWSTR)&type, sizeof(type));

    if (got) {
        switch (type) {
            case CAL_GREGORIAN:
            case CAL_GREGORIAN_US:
            case CAL_JAPAN:
            case CAL_TAIWAN:
            case CAL_HIJRI:
            case CAL_THAI:
            case CAL_GREGORIAN_ME_FRENCH:
            case CAL_GREGORIAN_ARABIC:
            case CAL_GREGORIAN_XLIT_ENGLISH:
            case CAL_GREGORIAN_XLIT_FRENCH:
            case CAL_UMALQURA:
                break;

            default:
                // non-supported calendars return -1
                type = -1;
                break;
        }
    }

    return type;
}

void replaceCalendarArrayElems(JNIEnv *env, jstring jlangtag, jint calid, jobjectArray jarray, DWORD* pTypes, int offset, int length, int style, BOOL bCal) {
    WCHAR name[BUFLEN];
    const jchar *langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    jstring tmp_string;
    CALTYPE isGenitive;

    CHECK_NULL(langtag);

    if (calid < 0) {
        calid = getCalendarID(langtag);
    }

    if (calid != -1) {
        int i;

        if (!(style & CALENDAR_STYLE_STANDALONE_MASK)) {
            isGenitive = CAL_RETURN_GENITIVE_NAMES;
        }

        for (i = 0; i < length; i++) {
            if (bCal && getCalendarInfoWrapper(langtag, calid, NULL,
                            pTypes[i] | isGenitive, name, BUFLEN, NULL) != 0 ||
                getLocaleInfoWrapper(langtag, pTypes[i] | isGenitive, name, BUFLEN) != 0) {
                tmp_string = (*env)->NewString(env, name, (jsize)wcslen(name));
                if (tmp_string != NULL) {
                    (*env)->SetObjectArrayElement(env, jarray, i + offset, tmp_string);
                }
            }
        }
    }

    (*env)->ReleaseStringChars(env, jlangtag, langtag);
}

WCHAR * getNumberPattern(const jchar * langtag, const jint numberStyle) {
    WCHAR ret[BUFLEN];
    WCHAR number[BUFLEN];
    WCHAR fix[BUFLEN];

    getFixPart(langtag, numberStyle, TRUE, TRUE, ret); // "+"
    getNumberPart(langtag, numberStyle, number);
    wcscat_s(ret, BUFLEN-wcslen(ret), number);      // "+12.34"
    getFixPart(langtag, numberStyle, TRUE, FALSE, fix);
    wcscat_s(ret, BUFLEN-wcslen(ret), fix);         // "+12.34$"
    wcscat_s(ret, BUFLEN-wcslen(ret), L";");        // "+12.34$;"
    getFixPart(langtag, numberStyle, FALSE, TRUE, fix);
    wcscat_s(ret, BUFLEN-wcslen(ret), fix);         // "+12.34$;("
    wcscat_s(ret, BUFLEN-wcslen(ret), number);      // "+12.34$;(12.34"
    getFixPart(langtag, numberStyle, FALSE, FALSE, fix);
    wcscat_s(ret, BUFLEN-wcslen(ret), fix);         // "+12.34$;(12.34$)"

    return _wcsdup(ret);
}

void getNumberPart(const jchar * langtag, const jint numberStyle, WCHAR * number) {
    DWORD digits = 0;
    DWORD leadingZero = 0;
    WCHAR grouping[BUFLEN];
    int groupingLen;
    WCHAR fractionPattern[BUFLEN];
    WCHAR * integerPattern = number;
    WCHAR * pDest;

    // Get info from Windows
    switch (numberStyle) {
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_CURRENCY:
            getLocaleInfoWrapper(langtag,
                LOCALE_ICURRDIGITS | LOCALE_RETURN_NUMBER,
                (LPWSTR)&digits, sizeof(digits));
            break;

        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_INTEGER:
            break;

        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_NUMBER:
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_PERCENT:
        default:
            getLocaleInfoWrapper(langtag,
                LOCALE_IDIGITS | LOCALE_RETURN_NUMBER,
                (LPWSTR)&digits, sizeof(digits));
            break;
    }

    getLocaleInfoWrapper(langtag,
        LOCALE_ILZERO | LOCALE_RETURN_NUMBER,
        (LPWSTR)&leadingZero, sizeof(leadingZero));
    groupingLen = getLocaleInfoWrapper(langtag, LOCALE_SGROUPING, grouping, BUFLEN);

    // fraction pattern
    if (digits > 0) {
        int i;
        for(i = digits;  i > 0; i--) {
            fractionPattern[i] = L'#';
        }
        fractionPattern[0] = L'.';
        fractionPattern[digits+1] = L'\0';
    } else {
        fractionPattern[0] = L'\0';
    }

    // integer pattern
    pDest = integerPattern;
    if (groupingLen > 0) {
        int cur = groupingLen - 1;// subtracting null terminator
        while (--cur >= 0) {
            int repnum;

            if (grouping[cur] == L';') {
                continue;
            }

            repnum = grouping[cur] - 0x30;
            if (repnum > 0) {
                *pDest++ = L'#';
                *pDest++ = L',';
                while(--repnum > 0) {
                    *pDest++ = L'#';
                }
            }
        }
    }

    if (leadingZero != 0) {
        *pDest++ = L'0';
    } else {
        *pDest++ = L'#';
    }
    *pDest = L'\0';

    wcscat_s(integerPattern, BUFLEN, fractionPattern);
}

void getFixPart(const jchar * langtag, const jint numberStyle, BOOL positive, BOOL prefix, WCHAR * ret) {
    DWORD pattern = 0;
    int style = numberStyle;
    int got = 0;

    if (positive) {
        if (style == sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_CURRENCY) {
            got = getLocaleInfoWrapper(langtag,
                LOCALE_ICURRENCY | LOCALE_RETURN_NUMBER,
                (LPWSTR)&pattern, sizeof(pattern));
        } else if (style == sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_PERCENT) {
            got = getLocaleInfoWrapper(langtag,
                LOCALE_IPOSITIVEPERCENT | LOCALE_RETURN_NUMBER,
                (LPWSTR)&pattern, sizeof(pattern));
        }
    } else {
        if (style == sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_CURRENCY) {
            got = getLocaleInfoWrapper(langtag,
                LOCALE_INEGCURR | LOCALE_RETURN_NUMBER,
                (LPWSTR)&pattern, sizeof(pattern));
        } else if (style == sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_PERCENT) {
            got = getLocaleInfoWrapper(langtag,
                LOCALE_INEGATIVEPERCENT | LOCALE_RETURN_NUMBER,
                (LPWSTR)&pattern, sizeof(pattern));
        } else {
            got = getLocaleInfoWrapper(langtag,
                LOCALE_INEGNUMBER | LOCALE_RETURN_NUMBER,
                (LPWSTR)&pattern, sizeof(pattern));
        }
    }

    if (numberStyle == sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_INTEGER) {
        style = sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_NUMBER;
    }

    wcscpy(ret, fixes[!prefix][!positive][style][pattern]);
}

int enumCalendarInfoWrapper(const jchar *langtag, CALID calid, CALTYPE type, LPWSTR buf, int buflen) {
    if (pEnumCalendarInfoExEx) {
        if (wcscmp(L"und", (LPWSTR)langtag) == 0) {
            // defaults to "en"
            return pEnumCalendarInfoExEx(&EnumCalendarInfoProc, L"en",
                calid, NULL, type, (LPARAM)buf);
        } else {
            return pEnumCalendarInfoExEx(&EnumCalendarInfoProc, langtag,
                calid, NULL, type, (LPARAM)buf);
        }
    } else {
        return 0;
    }
}

BOOL CALLBACK EnumCalendarInfoProc(LPWSTR lpCalInfoStr, CALID calid, LPWSTR lpReserved, LPARAM lParam) {
    wcscat_s((LPWSTR)lParam, BUFLEN, lpCalInfoStr);
    wcscat_s((LPWSTR)lParam, BUFLEN, L",");
    return TRUE;
}

jobjectArray getErasImpl(JNIEnv *env, jstring jlangtag, jint calid, jint style, jobjectArray eras) {
    const jchar * langtag = (*env)->GetStringChars(env, jlangtag, JNI_FALSE);
    WCHAR buf[BUFLEN];
    jobjectArray ret = eras;
    CALTYPE type;

    CHECK_NULL_RETURN(langtag, ret);

    buf[0] = '\0';
    if (style & CALENDAR_STYLE_SHORT_MASK) {
        type = CAL_SABBREVERASTRING;
    } else {
        type = CAL_SERASTRING;
    }

    if (calid < 0) {
        calid = getCalendarID(langtag);
    }

    if (calid != -1 && enumCalendarInfoWrapper(langtag, calid, type, buf, BUFLEN)) {
        // format in buf: "era0,era1,era2," where era0 is the current one
        int eraCount;
        LPWSTR current;
        jsize array_length;

        for(eraCount = 0, current = buf; *current != '\0'; current++) {
            if (*current == L',') {
                eraCount ++;
            }
        }

        if (eras != NULL) {
            array_length = (*env)->GetArrayLength(env, eras);
        } else {
            // +1 for the "before" era, e.g., BC, which Windows does not return.
            array_length = (jsize)eraCount + 1;
            ret = (*env)->NewObjectArray(env, array_length,
                (*env)->FindClass(env, "java/lang/String"), NULL);
        }

        if (ret != NULL) {
            int eraIndex;
            LPWSTR era;

            for(eraIndex = 0, era = current = buf; eraIndex < eraCount; era = current, eraIndex++) {
                while (*current != L',') {
                    current++;
                }
                *current++ = '\0';

                if (eraCount - eraIndex < array_length &&
                    *era != '\0') {
                    (*env)->SetObjectArrayElement(env, ret,
                        (jsize)(eraCount - eraIndex),
                        (*env)->NewString(env, era, (jsize)wcslen(era)));
                }
            }

            // Hack for the Japanese Imperial Calendar to insert Gregorian era for
            // "Before Meiji"
            if (calid == CAL_JAPAN) {
                buf[0] = '\0';
                if (enumCalendarInfoWrapper(langtag, CAL_GREGORIAN, type, buf, BUFLEN)) {
                    jsize len = (jsize)wcslen(buf);
                    buf[--len] = '\0'; // remove the last ','
                    (*env)->SetObjectArrayElement(env, ret, 0, (*env)->NewString(env, buf, len));
                }
            }
        }
    }

    (*env)->ReleaseStringChars(env, jlangtag, langtag);
    return ret;
}
