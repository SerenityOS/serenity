/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include <CoreFoundation/CoreFoundation.h>
#include <stdio.h>

#define BUFLEN 256

// java.util.Calendar constants
#define CALENDAR_FIELD_ERA              0           // Calendar.ERA
#define CALENDAR_FIELD_DAY_OF_WEEK      7           // Calendar.DAY_OF_WEEK
#define CALENDAR_FIELD_AM_PM            9           // Calendar.AM_PM
#define JAPANESE_MEIJI_INDEX            232

static CFDateFormatterStyle convertDateFormatterStyle(jint javaStyle);
static CFNumberFormatterStyle convertNumberFormatterStyle(jint javaStyle);
static void copyArrayElements(JNIEnv *env, CFArrayRef cfarray, jobjectArray jarray, CFIndex sindex, int dindex, int count);
static jstring getNumberSymbolString(JNIEnv *env, jstring jlangtag, jstring jdefault, CFStringRef type);
static jchar getNumberSymbolChar(JNIEnv *env, jstring jlangtag, jchar jdefault, CFStringRef type);
jobjectArray getErasImpl(JNIEnv *env, jstring jlangtag, jint style, jobjectArray eras);
jobjectArray getWeekdaysImpl(JNIEnv *env, jclass cls, jstring jlangtag, jint style, jobjectArray wdays);
jobjectArray getAmPmImpl(JNIEnv *env, jclass cls, jstring jlangtag, jint style, jobjectArray ampms);

// from java_props_macosx.c
extern char * getMacOSXLocale(int cat);
extern char * getPosixLocale(int cat);

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getDefaultLocale
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getDefaultLocale
  (JNIEnv *env, jclass cls, jint cat) {
    char * localeString = NULL;
    int posixCat;
    jstring ret = NULL;

    switch (cat) {
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_CAT_DISPLAY:
            posixCat = LC_MESSAGES;
            break;
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_CAT_FORMAT:
        default:
            posixCat = LC_CTYPE;
            break;
    }

    localeString = getMacOSXLocale(posixCat);
    if (localeString == NULL) {
        localeString = getPosixLocale(posixCat);
        if (localeString == NULL) {
            JNU_ThrowOutOfMemoryError(env, NULL);
            return NULL;
        }
    }
    ret = (*env)->NewStringUTF(env, localeString);
    free(localeString);

    return ret;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getDateTimePatternNative
 * Signature: (IILjava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getDateTimePatternNative
  (JNIEnv *env, jclass cls, jint dateStyle, jint timeStyle, jstring jlangtag) {
    jstring ret = NULL;
    CFLocaleRef cflocale = CFLocaleCopyCurrent();

    if (cflocale != NULL) {
        CFDateFormatterRef df = CFDateFormatterCreate(kCFAllocatorDefault,
                                                  cflocale,
                                                  convertDateFormatterStyle(dateStyle),
                                                  convertDateFormatterStyle(timeStyle));
        if (df != NULL) {
            char buf[BUFLEN];
            CFStringRef formatStr = CFDateFormatterGetFormat(df);
            CFStringGetCString(formatStr, buf, BUFLEN, kCFStringEncodingUTF8);
            ret = (*env)->NewStringUTF(env, buf);
            CFRelease(df);
        }
        CFRelease(cflocale);
    }

    return ret;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getCalendarID
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getCalendarID
  (JNIEnv *env, jclass cls, jstring jlangtag) {
    jstring ret = NULL;
    CFLocaleRef cflocale = CFLocaleCopyCurrent();

    if (cflocale != NULL) {
        char buf[BUFLEN];
        CFTypeRef calid = CFLocaleGetValue(cflocale, kCFLocaleCalendarIdentifier);
        CFStringGetCString((CFStringRef)calid, buf, BUFLEN, kCFStringEncodingUTF8);
        ret = (*env)->NewStringUTF(env, buf);
        CFRelease(cflocale);
    }

    return ret;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getAmPmStrings
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getAmPmStrings
  (JNIEnv *env, jclass cls, jstring jlangtag, jobjectArray ampms) {
      return getAmPmImpl(env, cls, jlangtag, 0, ampms);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getEras
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getEras
  (JNIEnv *env, jclass cls, jstring jlangtag, jobjectArray eras) {
    return getErasImpl(env, jlangtag, 0, eras);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getMonths
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getMonths
  (JNIEnv *env, jclass cls, jstring jlangtag, jobjectArray months) {
    CFLocaleRef cflocale = CFLocaleCopyCurrent();
    if (cflocale != NULL) {
        CFDateFormatterRef df = CFDateFormatterCreate(kCFAllocatorDefault,
                                                  cflocale,
                                                  kCFDateFormatterFullStyle,
                                                  kCFDateFormatterFullStyle);
        if (df != NULL) {
            CFArrayRef cfmonths = CFDateFormatterCopyProperty(df, kCFDateFormatterMonthSymbols);
            if (cfmonths != NULL) {
                copyArrayElements(env, cfmonths, months, 0, 0, CFArrayGetCount(cfmonths));
                CFRelease(cfmonths);
            }
            CFRelease(df);
        }
        CFRelease(cflocale);
    }

    return months;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getShortMonths
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getShortMonths
  (JNIEnv *env, jclass cls, jstring jlangtag, jobjectArray smonths) {
    CFLocaleRef cflocale = CFLocaleCopyCurrent();
    if (cflocale != NULL) {
        CFDateFormatterRef df = CFDateFormatterCreate(kCFAllocatorDefault,
                                                  cflocale,
                                                  kCFDateFormatterFullStyle,
                                                  kCFDateFormatterFullStyle);
        if (df != NULL) {
            CFArrayRef cfsmonths = CFDateFormatterCopyProperty(df, kCFDateFormatterShortMonthSymbols);
            if (cfsmonths != NULL) {
                copyArrayElements(env, cfsmonths, smonths, 0, 0, CFArrayGetCount(cfsmonths));
                CFRelease(cfsmonths);
            }
            CFRelease(df);
        }
        CFRelease(cflocale);
    }

    return smonths;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getWeekdays
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getWeekdays
    (JNIEnv *env, jclass cls, jstring jlangtag, jobjectArray wdays) {
    return getWeekdaysImpl(env, cls, jlangtag, 0, wdays);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getShortWeekdays
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getShortWeekdays
  (JNIEnv *env, jclass cls, jstring jlangtag, jobjectArray swdays) {
    CFLocaleRef cflocale = CFLocaleCopyCurrent();
    if (cflocale != NULL) {
        CFDateFormatterRef df = CFDateFormatterCreate(kCFAllocatorDefault,
                                                  cflocale,
                                                  kCFDateFormatterFullStyle,
                                                  kCFDateFormatterFullStyle);
        if (df != NULL) {
            CFArrayRef cfswdays = CFDateFormatterCopyProperty(df, kCFDateFormatterShortWeekdaySymbols);
            if (cfswdays != NULL) {
                copyArrayElements(env, cfswdays, swdays, 0, 1, CFArrayGetCount(cfswdays));
                CFRelease(cfswdays);
            }
            CFRelease(df);
        }
        CFRelease(cflocale);
    }

    return swdays;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getNumberPatternNative
 * Signature: (ILjava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getNumberPatternNative
  (JNIEnv *env, jclass cls, jint numberStyle, jstring jlangtag) {
    jstring ret = NULL;
    CFLocaleRef cflocale = CFLocaleCopyCurrent();
    if (cflocale != NULL) {
        CFNumberFormatterRef nf = CFNumberFormatterCreate(kCFAllocatorDefault,
                                                  cflocale,
                                                  convertNumberFormatterStyle(numberStyle));
        if (nf != NULL) {
            char buf[BUFLEN];
            CFStringRef formatStr = CFNumberFormatterGetFormat(nf);
            CFStringGetCString(formatStr, buf, BUFLEN, kCFStringEncodingUTF8);
            ret = (*env)->NewStringUTF(env, buf);
            CFRelease(nf);
        }
        CFRelease(cflocale);
    }

    return ret;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getCurrencySymbol
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getCurrencySymbol
  (JNIEnv *env, jclass cls, jstring jlangtag, jstring currencySymbol) {
    return getNumberSymbolString(env, jlangtag, currencySymbol, kCFNumberFormatterCurrencySymbol);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getDecimalSeparator
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getDecimalSeparator
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar decimalSeparator) {
    return getNumberSymbolChar(env, jlangtag, decimalSeparator, kCFNumberFormatterDecimalSeparator);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getGroupingSeparator
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getGroupingSeparator
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar groupingSeparator) {
    return getNumberSymbolChar(env, jlangtag, groupingSeparator, kCFNumberFormatterGroupingSeparator);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getInfinity
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getInfinity
  (JNIEnv *env, jclass cls, jstring jlangtag, jstring infinity) {
    return getNumberSymbolString(env, jlangtag, infinity, kCFNumberFormatterInfinitySymbol);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getInternationalCurrencySymbol
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getInternationalCurrencySymbol
  (JNIEnv *env, jclass cls, jstring jlangtag, jstring internationalCurrencySymbol) {
    return getNumberSymbolString(env, jlangtag, internationalCurrencySymbol, kCFNumberFormatterInternationalCurrencySymbol);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getMinusSign
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getMinusSign
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar minusSign) {
    return getNumberSymbolChar(env, jlangtag, minusSign, kCFNumberFormatterMinusSign);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getMonetaryDecimalSeparator
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getMonetaryDecimalSeparator
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar monetaryDecimalSeparator) {
    return getNumberSymbolChar(env, jlangtag, monetaryDecimalSeparator, kCFNumberFormatterCurrencyDecimalSeparator);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getNaN
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getNaN
  (JNIEnv *env, jclass cls, jstring jlangtag, jstring nan) {
    return getNumberSymbolString(env, jlangtag, nan, kCFNumberFormatterNaNSymbol);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getPercent
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getPercent
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar percent) {
    return getNumberSymbolChar(env, jlangtag, percent, kCFNumberFormatterPercentSymbol);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getPerMill
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getPerMill
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar perMill) {
    return getNumberSymbolChar(env, jlangtag, perMill, kCFNumberFormatterPerMillSymbol);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getZeroDigit
 * Signature: (Ljava/lang/String;C)C
 */
JNIEXPORT jchar JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getZeroDigit
  (JNIEnv *env, jclass cls, jstring jlangtag, jchar zeroDigit) {
    // The following code *should* work, but not for some reason :o
    //
    //return getNumberSymbolChar(env, jlangtag, zeroDigit, kCFNumberFormatterZeroSymbol);
    //
    // so here is a workaround.
    jchar ret = zeroDigit;
    CFLocaleRef cflocale = CFLocaleCopyCurrent();

    if (cflocale != NULL) {
        CFNumberFormatterRef nf = CFNumberFormatterCreate(kCFAllocatorDefault,
                                                  cflocale,
                                                  kCFNumberFormatterNoStyle);
        if (nf != NULL) {
            int zero = 0;
            CFStringRef str = CFNumberFormatterCreateStringWithValue(kCFAllocatorDefault,
                              nf, kCFNumberIntType, &zero);
            if (str != NULL) {
                if (CFStringGetLength(str) > 0) {
                    ret = CFStringGetCharacterAtIndex(str, 0);
                }
                CFRelease(str);
            }

            CFRelease(nf);
        }

        CFRelease(cflocale);
    }

    return ret;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getExponentSeparator
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getExponentSeparator
  (JNIEnv *env, jclass cls, jstring jlangtag, jstring exponent) {
    return getNumberSymbolString(env, jlangtag, exponent, kCFNumberFormatterExponentSymbol);
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getCalendarInt
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getCalendarInt
  (JNIEnv *env, jclass cls, jstring jlangtag, jint type) {
    jint ret = 0;
    CFCalendarRef cfcal = CFCalendarCopyCurrent();

    if (cfcal != NULL) {
        switch (type) {
            case sun_util_locale_provider_HostLocaleProviderAdapterImpl_CD_FIRSTDAYOFWEEK:
                ret = CFCalendarGetFirstWeekday(cfcal);
                break;
            case sun_util_locale_provider_HostLocaleProviderAdapterImpl_CD_MINIMALDAYSINFIRSTWEEK:
                ret = CFCalendarGetMinimumDaysInFirstWeek(cfcal);
                break;
            default:
                ret = 0;
        }

        CFRelease(cfcal);
    }

    return ret;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getCalendarDisplayStrings
 * Signature: (Ljava/lang/String;III)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getCalendarDisplayStrings
  (JNIEnv *env, jclass cls, jstring jlangtag, jint field, jint style) {
    switch (field) {
    case CALENDAR_FIELD_ERA:
        return getErasImpl(env, jlangtag, style, NULL);

    case CALENDAR_FIELD_DAY_OF_WEEK:
        return getWeekdaysImpl(env, cls, jlangtag, style, NULL);

    case CALENDAR_FIELD_AM_PM:
        return getAmPmImpl(env, cls, jlangtag, style, NULL);

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
  (JNIEnv *env, jclass cls, jstring jlangtag, jint type, jstring value) {
    jstring ret = NULL;

    const char *clangtag = (*env)->GetStringUTFChars(env, jlangtag, 0);
    if (clangtag != NULL) {
        const char *cvalue = (*env)->GetStringUTFChars(env, value, 0);
        if (cvalue != NULL) {
            CFStringRef cflangtag =
                CFStringCreateWithCString(kCFAllocatorDefault, clangtag, kCFStringEncodingUTF8);
            if (cflangtag != NULL) {
                CFLocaleRef cflocale = CFLocaleCreate(kCFAllocatorDefault, cflangtag);
                if (cflocale != NULL) {
                    CFStringRef cfvalue =
                        CFStringCreateWithCString(kCFAllocatorDefault, cvalue, kCFStringEncodingUTF8);
                    if (cfvalue != NULL) {
                        CFStringRef str = NULL;
                        switch (type) {
                            case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_LOCALE_LANGUAGE:
                                str = CFLocaleCopyDisplayNameForPropertyValue(cflocale, kCFLocaleLanguageCode, cfvalue);
                                break;
                            case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_LOCALE_SCRIPT:
                                str = CFLocaleCopyDisplayNameForPropertyValue(cflocale, kCFLocaleScriptCode, cfvalue);
                                break;
                            case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_LOCALE_REGION:
                                str = CFLocaleCopyDisplayNameForPropertyValue(cflocale, kCFLocaleCountryCode, cfvalue);
                                break;
                            case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_LOCALE_VARIANT:
                                str = CFLocaleCopyDisplayNameForPropertyValue(cflocale, kCFLocaleVariantCode, cfvalue);
                                break;
                            case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_CURRENCY_CODE:
                                str = CFLocaleCopyDisplayNameForPropertyValue(cflocale, kCFLocaleCurrencyCode, cfvalue);
                                break;
                            case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_CURRENCY_SYMBOL:
                                str = CFLocaleCopyDisplayNameForPropertyValue(cflocale, kCFLocaleCurrencySymbol, cfvalue);
                                break;
                        }
                        if (str != NULL) {
                            char buf[BUFLEN];
                            CFStringGetCString(str, buf, BUFLEN, kCFStringEncodingUTF8);
                            CFRelease(str);
                            ret = (*env)->NewStringUTF(env, buf);
                        }
                        CFRelease(cfvalue);
                    }
                    CFRelease(cflocale);
                }
                CFRelease(cflangtag);
            }
            (*env)->ReleaseStringUTFChars(env, value, cvalue);
        }
        (*env)->ReleaseStringUTFChars(env, jlangtag, clangtag);
    }

    return ret;
}

/*
 * Class:     sun_util_locale_provider_HostLocaleProviderAdapterImpl
 * Method:    getTimeZoneDisplayString
 * Signature: (Ljava/lang/String;ILjava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_util_locale_provider_HostLocaleProviderAdapterImpl_getTimeZoneDisplayString
  (JNIEnv *env, jclass cls, jstring jlangtag, jint type, jstring tzid) {
    jstring ret = NULL;

    const char *clangtag = (*env)->GetStringUTFChars(env, jlangtag, 0);
    if (clangtag != NULL) {
        const char *ctzid = (*env)->GetStringUTFChars(env, tzid, 0);
        if (ctzid != NULL) {
            CFStringRef cflangtag =
                CFStringCreateWithCString(kCFAllocatorDefault, clangtag, kCFStringEncodingUTF8);
            if (cflangtag != NULL) {
                CFLocaleRef cflocale = CFLocaleCreate(kCFAllocatorDefault, cflangtag);
                if (cflocale != NULL) {
                    CFStringRef cftzid =
                        CFStringCreateWithCString(kCFAllocatorDefault, ctzid, kCFStringEncodingUTF8);
                    if (cftzid != NULL) {
                        CFTimeZoneRef cftz = CFTimeZoneCreateWithName(kCFAllocatorDefault, cftzid, false);
                        if (cftz != NULL) {
                            CFStringRef str = NULL;
                            switch (type) {
                                case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_TZ_SHORT_STANDARD:
                                    str = CFTimeZoneCopyLocalizedName(cftz, kCFTimeZoneNameStyleShortStandard, cflocale);
                                    break;
                                case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_TZ_SHORT_DST:
                                    str = CFTimeZoneCopyLocalizedName(cftz, kCFTimeZoneNameStyleShortDaylightSaving, cflocale);
                                    break;
                                case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_TZ_LONG_STANDARD:
                                    str = CFTimeZoneCopyLocalizedName(cftz, kCFTimeZoneNameStyleStandard, cflocale);
                                    break;
                                case sun_util_locale_provider_HostLocaleProviderAdapterImpl_DN_TZ_LONG_DST:
                                    str = CFTimeZoneCopyLocalizedName(cftz, kCFTimeZoneNameStyleDaylightSaving, cflocale);
                                    break;
                            }
                            if (str != NULL) {
                                char buf[BUFLEN];
                                CFStringGetCString(str, buf, BUFLEN, kCFStringEncodingUTF8);
                                CFRelease(str);
                                ret = (*env)->NewStringUTF(env, buf);
                            }
                            CFRelease(cftz);
                        }
                        CFRelease(cftzid);
                    }
                    CFRelease(cflocale);
                }
                CFRelease(cflangtag);
            }
            (*env)->ReleaseStringUTFChars(env, tzid, ctzid);
        }
        (*env)->ReleaseStringUTFChars(env, jlangtag, clangtag);
    }

    return ret;
}

static CFDateFormatterStyle convertDateFormatterStyle(jint javaStyle) {
    switch (javaStyle) {
        case 0: // FULL
            return kCFDateFormatterFullStyle;
        case 1: // LONG
            return kCFDateFormatterLongStyle;
        case 2: // MEDIUM
            return kCFDateFormatterMediumStyle;
        case 3: // LONG
            return kCFDateFormatterShortStyle;
        case -1: // No style
        default:
            return kCFDateFormatterNoStyle;
    }
}

static CFNumberFormatterStyle convertNumberFormatterStyle(jint javaStyle) {
    switch (javaStyle) {
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_CURRENCY:
            return kCFNumberFormatterCurrencyStyle;
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_INTEGER:
            return kCFNumberFormatterDecimalStyle;
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_NUMBER:
            return kCFNumberFormatterDecimalStyle;
        case sun_util_locale_provider_HostLocaleProviderAdapterImpl_NF_PERCENT:
            return kCFNumberFormatterPercentStyle;
        default:
            return kCFNumberFormatterNoStyle;
    }
}

static void copyArrayElements(JNIEnv *env, CFArrayRef cfarray, jobjectArray jarray, CFIndex sindex, int dindex, int count) {
    char buf[BUFLEN];
    jstring tmp_string;

    for (; count > 0; sindex++, dindex++, count--) {
        CFStringGetCString(CFArrayGetValueAtIndex(cfarray, sindex), buf, BUFLEN, kCFStringEncodingUTF8);
        tmp_string = (*env)->NewStringUTF(env, buf);
        if (tmp_string != NULL) {
            (*env)->SetObjectArrayElement(env, jarray, dindex, tmp_string);
        } else {
            break;
        }
    }
}

static jstring getNumberSymbolString(JNIEnv *env, jstring jlangtag, jstring jdefault, CFStringRef type) {
    char buf[BUFLEN];
    jstring ret = jdefault;
    CFLocaleRef cflocale = CFLocaleCopyCurrent();

    if (cflocale != NULL) {
        CFNumberFormatterRef nf = CFNumberFormatterCreate(kCFAllocatorDefault,
                                                  cflocale,
                                                  kCFNumberFormatterNoStyle);
        if (nf != NULL) {
            CFStringRef str = CFNumberFormatterCopyProperty(nf, type);
            if (str != NULL) {
                CFStringGetCString(str, buf, BUFLEN, kCFStringEncodingUTF8);
                CFRelease(str);
                ret = (*env)->NewStringUTF(env, buf);
            }

            CFRelease(nf);
        }

        CFRelease(cflocale);
    }

    return ret;
}

static jchar getNumberSymbolChar(JNIEnv *env, jstring jlangtag, jchar jdefault, CFStringRef type) {
    jchar ret = jdefault;
    CFLocaleRef cflocale = CFLocaleCopyCurrent();

    if (cflocale != NULL) {
        CFNumberFormatterRef nf = CFNumberFormatterCreate(kCFAllocatorDefault,
                                                  cflocale,
                                                  kCFNumberFormatterNoStyle);
        if (nf != NULL) {
            CFStringRef str = CFNumberFormatterCopyProperty(nf, type);
            if (str != NULL) {
                if (CFStringGetLength(str) > 0) {
                    ret = CFStringGetCharacterAtIndex(str, 0);
                }
                CFRelease(str);
            }

            CFRelease(nf);
        }

        CFRelease(cflocale);
    }

    return ret;
}

jobjectArray getErasImpl(JNIEnv *env, jstring jlangtag, jint style, jobjectArray eras) {
    jobjectArray ret = eras;
    CFLocaleRef cflocale = CFLocaleCopyCurrent();
    if (cflocale != NULL) {
        CFDateFormatterRef df = CFDateFormatterCreate(kCFAllocatorDefault,
                                                  cflocale,
                                                  convertDateFormatterStyle(style),
                                                  convertDateFormatterStyle(style));
        if (df != NULL) {
            CFArrayRef cferas = CFDateFormatterCopyProperty(df, kCFDateFormatterEraSymbols);
            if (cferas != NULL) {
                int eraCount = CFArrayGetCount(cferas);

                if (eras == NULL) {
                    ret = (*env)->NewObjectArray(env, (jsize)eraCount,
                        (*env)->FindClass(env, "java/lang/String"), NULL);
                }
                CFTypeRef cal = CFLocaleGetValue(cflocale, kCFLocaleCalendarIdentifier);
                int sindex = cal == kCFJapaneseCalendar ? JAPANESE_MEIJI_INDEX : 0;
                int dindex = cal == kCFJapaneseCalendar ? 1 : 0; // 0 is "BeforeMeiji" in JCal
                copyArrayElements(env, cferas, ret, sindex, dindex, eraCount - sindex);
                CFRelease(cferas);
            }
            CFRelease(df);
        }
        CFRelease(cflocale);
    }

    return ret;
}

jobjectArray getWeekdaysImpl(JNIEnv *env, jclass cls, jstring jlangtag, jint style, jobjectArray wdays) {
    jobjectArray ret = wdays;
    CFLocaleRef cflocale = CFLocaleCopyCurrent();
    if (cflocale != NULL) {
        CFDateFormatterRef df = CFDateFormatterCreate(kCFAllocatorDefault,
                                                  cflocale,
                                                  convertDateFormatterStyle(style),
                                                  convertDateFormatterStyle(style));
        if (df != NULL) {
            CFArrayRef cfwdays = CFDateFormatterCopyProperty(df, kCFDateFormatterWeekdaySymbols);
            if (cfwdays != NULL) {
                int dayCount = CFArrayGetCount(cfwdays);

                if (wdays == NULL) {
                    ret = (*env)->NewObjectArray(env, dayCount + 1,
                        (*env)->FindClass(env, "java/lang/String"), NULL);
                }
                copyArrayElements(env, cfwdays, ret, 0, 1, dayCount);
                CFRelease(cfwdays);
            }
            CFRelease(df);
        }
        CFRelease(cflocale);
    }

    return ret;
}

jobjectArray getAmPmImpl(JNIEnv *env, jclass cls, jstring jlangtag, jint style, jobjectArray ampms) {
    CFLocaleRef cflocale = CFLocaleCopyCurrent();
    jstring tmp_string;
    if (cflocale != NULL) {
        CFDateFormatterRef df = CFDateFormatterCreate(kCFAllocatorDefault,
                                                  cflocale,
                                                  convertDateFormatterStyle(style),
                                                  convertDateFormatterStyle(style));
        if (df != NULL) {
            char buf[BUFLEN];
            if (ampms == NULL) {
                ampms = (*env)->NewObjectArray(env, 2,
                    (*env)->FindClass(env, "java/lang/String"), NULL);
            }
            CFStringRef amStr = CFDateFormatterCopyProperty(df, kCFDateFormatterAMSymbol);
            if (amStr != NULL) {
                CFStringGetCString(amStr, buf, BUFLEN, kCFStringEncodingUTF8);
                CFRelease(amStr);
                tmp_string = (*env)->NewStringUTF(env, buf);
                if (tmp_string != NULL) {
                    (*env)->SetObjectArrayElement(env, ampms, 0, tmp_string);
                }
            }
            if (!(*env)->ExceptionCheck(env)){
                CFStringRef pmStr = CFDateFormatterCopyProperty(df, kCFDateFormatterPMSymbol);
                if (pmStr != NULL) {
                    CFStringGetCString(pmStr, buf, BUFLEN, kCFStringEncodingUTF8);
                    CFRelease(pmStr);
                    tmp_string = (*env)->NewStringUTF(env, buf);
                    if (tmp_string != NULL) {
                        (*env)->SetObjectArrayElement(env, ampms, 1, tmp_string);
                    }
                }
            }
            CFRelease(df);
        }
        CFRelease(cflocale);
    }

    return ampms;
}
