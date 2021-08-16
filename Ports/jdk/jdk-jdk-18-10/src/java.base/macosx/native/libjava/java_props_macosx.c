/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <objc/objc-runtime.h>

#include <CoreFoundation/CoreFoundation.h>
#include <SystemConfiguration/SystemConfiguration.h>
#include <Foundation/Foundation.h>

#include "java_props_macosx.h"

char *getPosixLocale(int cat) {
    char *lc = setlocale(cat, NULL);
    if ((lc == NULL) || (strcmp(lc, "C") == 0)) {
        lc = getenv("LANG");
    }
    if (lc == NULL) return NULL;
    return strdup(lc);
}

#define LOCALEIDLENGTH  128
#ifndef kCFCoreFoundationVersionNumber10_11_Max
#define kCFCoreFoundationVersionNumber10_11_Max 1299
#endif
char *getMacOSXLocale(int cat) {
    const char* retVal = NULL;
    char languageString[LOCALEIDLENGTH];
    char localeString[LOCALEIDLENGTH];

    // Since macOS 10.12, there is no separate language selection for
    // "format" locale, e.g., date format. Use the preferred language
    // for all LC_* categories.
    if (kCFCoreFoundationVersionNumber >
        kCFCoreFoundationVersionNumber10_11_Max) {
        cat = LC_MESSAGES;
    }

    switch (cat) {
    case LC_MESSAGES:
        {
            // get preferred language code
            CFArrayRef languages = CFLocaleCopyPreferredLanguages();
            if (languages == NULL) {
                return NULL;
            }
            if (CFArrayGetCount(languages) <= 0) {
                CFRelease(languages);
                return NULL;
            }

            CFStringRef primaryLanguage = (CFStringRef)CFArrayGetValueAtIndex(languages, 0);
            if (primaryLanguage == NULL) {
                CFRelease(languages);
                return NULL;
            }
            if (CFStringGetCString(primaryLanguage, languageString,
                                   LOCALEIDLENGTH, CFStringGetSystemEncoding()) == false) {
                CFRelease(languages);
                return NULL;
            }
            CFRelease(languages);

            // Explicitly supply region, if there is none
            char *hyphenPos = strchr(languageString, '-');
            int langStrLen = strlen(languageString);

            if (hyphenPos == NULL || // languageString contains ISO639 only, e.g., "en"
                languageString + langStrLen - hyphenPos == 5) { // ISO639-ScriptCode, e.g., "en-Latn"
                CFLocaleRef cflocale = CFLocaleCopyCurrent();
                if (cflocale != NULL) {
                    CFStringGetCString(CFLocaleGetIdentifier(cflocale),
                                   localeString, LOCALEIDLENGTH, CFStringGetSystemEncoding());
                    char *underscorePos = strrchr(localeString, '_');
                    char *region = NULL;

                    if (underscorePos != NULL) {
                        region = underscorePos + 1;
                    }

                    if (region != NULL) {
                        strcat(languageString, "-");
                        strcat(languageString, region);
                    }
                    CFRelease(cflocale);
                }
            }

            retVal = languageString;
        }
        break;

    default:
        {
            CFLocaleRef cflocale = CFLocaleCopyCurrent();
            if (cflocale != NULL) {
                if (!CFStringGetCString(CFLocaleGetIdentifier(cflocale),
                                        localeString, LOCALEIDLENGTH, CFStringGetSystemEncoding())) {
                    CFRelease(cflocale);
                    return NULL;
                }

                retVal = localeString;
                CFRelease(cflocale);
            } else {
                return NULL;
            }
        }
        break;
    }

    if (retVal != NULL) {
        // convertToPOSIXLocale() does not expect any variant codes, so ignore
        // '@' and anything following, if present.
        char* rmAt = strchr(retVal, '@');
        if (rmAt != NULL) {
            *rmAt = '\0';
        }
        return strdup(convertToPOSIXLocale(retVal));
    }

    return NULL;
}

/* Language IDs use the language designators and (optional) region
 * and script designators of BCP 47.  So possible formats are:
 *
 * "en"         (language designator only)
 * "haw"        (3-letter lanuage designator)
 * "en-GB"      (language with alpha-2 region designator)
 * "es-419"     (language with 3-digit UN M.49 area code)
 * "zh-Hans"    (language with ISO 15924 script designator)
 * "zh-Hans-US"  (language with ISO 15924 script designator and region)
 * "zh-Hans-419" (language with ISO 15924 script designator and UN M.49)
 *
 * convert these tags into POSIX conforming locale string, i.e.,
 * lang{_region}{@script}. e.g., for "zh-Hans-US" into "zh_US@Hans"
 */
const char * convertToPOSIXLocale(const char* src) {
    char* scriptRegion = strchr(src, '-');
    if (scriptRegion != NULL) {
        int length = strlen(scriptRegion);
        char* region = strchr(scriptRegion + 1, '-');
        char* atMark = NULL;

        if (region == NULL) {
            // CFLocaleGetIdentifier() returns '_' before region
            region = strchr(scriptRegion + 1, '_');
        }

        *scriptRegion = '_';
        if (length > 5) {
            // Region and script both exist.
            char tmpScript[4];
            int regionLength = length - 6;
            atMark = scriptRegion + 1 + regionLength;
            memcpy(tmpScript, scriptRegion + 1, 4);
            memmove(scriptRegion + 1, region + 1, regionLength);
            memcpy(atMark + 1, tmpScript, 4);
        } else if (length == 5) {
            // script only
            atMark = scriptRegion;
        }

        if (atMark != NULL) {
            *atMark = '@';

            // assert script code
            assert(isalpha(atMark[1]) &&
                   isalpha(atMark[2]) &&
                   isalpha(atMark[3]) &&
                   isalpha(atMark[4]));
        }

        assert(((length == 3 || length == 8) &&
            // '_' followed by a 2 character region designator
                isalpha(scriptRegion[1]) &&
                isalpha(scriptRegion[2])) ||
                ((length == 4 || length == 9) &&
            // '_' followed by a 3-digit UN M.49 area code
                isdigit(scriptRegion[1]) &&
                isdigit(scriptRegion[2]) &&
                isdigit(scriptRegion[3])) ||
            // '@' followed by a 4 character script code (already validated above)
                (length == 5));
    }

    return src;
}

char *setupMacOSXLocale(int cat) {
    char * ret = getMacOSXLocale(cat);

    if (ret == NULL) {
        return getPosixLocale(cat);
    } else {
        return ret;
    }
}

// 10.9 SDK does not include the NSOperatingSystemVersion struct.
// For now, create our own
typedef struct {
        NSInteger majorVersion;
        NSInteger minorVersion;
        NSInteger patchVersion;
} OSVerStruct;

void setOSNameAndVersion(java_props_t *sprops) {
    // Hardcode os_name, and fill in os_version
    sprops->os_name = strdup("Mac OS X");

    char* osVersionCStr = NULL;
    // Mac OS 10.9 includes the [NSProcessInfo operatingSystemVersion] function,
    // but it's not in the 10.9 SDK.  So, call it via NSInvocation.
    if ([[NSProcessInfo processInfo] respondsToSelector:@selector(operatingSystemVersion)]) {
        OSVerStruct osVer;
        NSMethodSignature *sig = [[NSProcessInfo processInfo] methodSignatureForSelector:
                @selector(operatingSystemVersion)];
        NSInvocation *invoke = [NSInvocation invocationWithMethodSignature:sig];
        invoke.selector = @selector(operatingSystemVersion);
        [invoke invokeWithTarget:[NSProcessInfo processInfo]];
        [invoke getReturnValue:&osVer];

        NSString *nsVerStr;
        // Copy out the char* if running on version other than 10.16 Mac OS (10.16 == 11.x)
        // or explicitly requesting version compatibility
        if (!((long)osVer.majorVersion == 10 && (long)osVer.minorVersion >= 16) ||
                (getenv("SYSTEM_VERSION_COMPAT") != NULL)) {
            if (osVer.patchVersion == 0) { // Omit trailing ".0"
                nsVerStr = [NSString stringWithFormat:@"%ld.%ld",
                        (long)osVer.majorVersion, (long)osVer.minorVersion];
            } else {
                nsVerStr = [NSString stringWithFormat:@"%ld.%ld.%ld",
                        (long)osVer.majorVersion, (long)osVer.minorVersion, (long)osVer.patchVersion];
            }
            // Copy out the char*
            osVersionCStr = strdup([nsVerStr UTF8String]);
        } else {
            // Version 10.16, without explicit env setting of SYSTEM_VERSION_COMPAT
            // AKA 11.x; compute the version number from the letter in the ProductBuildVersion
            NSDictionary *version = [NSDictionary dictionaryWithContentsOfFile :
                             @"/System/Library/CoreServices/SystemVersion.plist"];
            if (version != NULL) {
                NSString *nsBuildVerStr = [version objectForKey : @"ProductBuildVersion"];
                if (nsBuildVerStr != NULL && nsBuildVerStr.length >= 3) {
                    int letter = [nsBuildVerStr characterAtIndex:2];
                    if (letter >= 'B' && letter <= 'Z') {
                        int vers = letter - 'A' - 1;
                        asprintf(&osVersionCStr, "11.%d", vers);
                    }
                }
            }
        }
    }
    // Fallback if running on pre-10.9 Mac OS
    if (osVersionCStr == NULL) {
        NSDictionary *version = [NSDictionary dictionaryWithContentsOfFile :
                                 @"/System/Library/CoreServices/SystemVersion.plist"];
        if (version != NULL) {
            NSString *nsVerStr = [version objectForKey : @"ProductVersion"];
            if (nsVerStr != NULL) {
                osVersionCStr = strdup([nsVerStr UTF8String]);
            }
        }
    }
    if (osVersionCStr == NULL) {
        osVersionCStr = strdup("Unknown");
    }
    sprops->os_version = osVersionCStr;
}


static Boolean getProxyInfoForProtocol(CFDictionaryRef inDict, CFStringRef inEnabledKey,
                                       CFStringRef inHostKey, CFStringRef inPortKey,
                                       CFStringRef *outProxyHost, int *ioProxyPort) {
    /* See if the proxy is enabled. */
    CFNumberRef cf_enabled = CFDictionaryGetValue(inDict, inEnabledKey);
    if (cf_enabled == NULL) {
        return false;
    }

    int isEnabled = false;
    if (!CFNumberGetValue(cf_enabled, kCFNumberIntType, &isEnabled)) {
        return isEnabled;
    }

    if (!isEnabled) return false;
    *outProxyHost = CFDictionaryGetValue(inDict, inHostKey);

    // If cf_host is null, that means the checkbox is set,
    //   but no host was entered. We'll treat that as NOT ENABLED.
    // If cf_port is null or cf_port isn't a number, that means
    //   no port number was entered. Treat this as ENABLED with the
    //   protocol's default port.
    if (*outProxyHost == NULL) {
        return false;
    }

    if (CFStringGetLength(*outProxyHost) == 0) {
        return false;
    }

    int newPort = 0;
    CFNumberRef cf_port = NULL;
    if ((cf_port = CFDictionaryGetValue(inDict, inPortKey)) != NULL &&
        CFNumberGetValue(cf_port, kCFNumberIntType, &newPort) &&
        newPort > 0) {
        *ioProxyPort = newPort;
    } else {
        // bad port or no port - leave *ioProxyPort unchanged
    }

    return true;
}

static char *createUTF8CString(const CFStringRef theString) {
    if (theString == NULL) return NULL;

    const CFIndex stringLength = CFStringGetLength(theString);
    const CFIndex bufSize = CFStringGetMaximumSizeForEncoding(stringLength, kCFStringEncodingUTF8) + 1;
    char *returnVal = (char *)malloc(bufSize);

    if (CFStringGetCString(theString, returnVal, bufSize, kCFStringEncodingUTF8)) {
        return returnVal;
    }

    free(returnVal);
    return NULL;
}

// Return TRUE if str is a syntactically valid IP address.
// Using inet_pton() instead of inet_aton() for IPv6 support.
// len is only a hint; cstr must still be nul-terminated
static int looksLikeIPAddress(char *cstr, size_t len) {
    if (len == 0  ||  (len == 1 && cstr[0] == '.')) return FALSE;

    char dst[16]; // big enough for INET6
    return (1 == inet_pton(AF_INET, cstr, dst)  ||
            1 == inet_pton(AF_INET6, cstr, dst));
}



// Convert Mac OS X proxy exception entry to Java syntax.
// See Radar #3441134 for details.
// Returns NULL if this exception should be ignored by Java.
// May generate a string with multiple exceptions separated by '|'.
static char * createConvertedException(CFStringRef cf_original) {
    // This is done with char* instead of CFString because inet_pton()
    // needs a C string.
    char *c_exception = createUTF8CString(cf_original);
    if (!c_exception) return NULL;

    int c_len = strlen(c_exception);

    // 1. sanitize exception prefix
    if (c_len >= 1  &&  0 == strncmp(c_exception, ".", 1)) {
        memmove(c_exception, c_exception+1, c_len);
        c_len -= 1;
    } else if (c_len >= 2  &&  0 == strncmp(c_exception, "*.", 2)) {
        memmove(c_exception, c_exception+2, c_len-1);
        c_len -= 2;
    }

    // 2. pre-reject other exception wildcards
    if (strchr(c_exception, '*')) {
        free(c_exception);
        return NULL;
    }

    // 3. no IP wildcarding
    if (looksLikeIPAddress(c_exception, c_len)) {
        return c_exception;
    }

    // 4. allow domain suffixes
    // c_exception is now "str\0" - change to "str|*.str\0"
    c_exception = reallocf(c_exception, c_len+3+c_len+1);
    if (!c_exception) return NULL;

    strncpy(c_exception+c_len, "|*.", 3);
    strncpy(c_exception+c_len+3, c_exception, c_len);
    c_exception[c_len+3+c_len] = '\0';
    return c_exception;
}

/*
 * Method for fetching the user.home path and storing it in the property list.
 * For signed .apps running in the Mac App Sandbox, user.home is set to the
 * app's sandbox container.
 */
void setUserHome(java_props_t *sprops) {
    if (sprops == NULL) { return; }
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    sprops->user_home = createUTF8CString((CFStringRef)NSHomeDirectory());
    [pool drain];
}

/*
 * Method for fetching proxy info and storing it in the property list.
 */
void setProxyProperties(java_props_t *sProps) {
    if (sProps == NULL) return;

    char buf[16];    /* Used for %d of an int - 16 is plenty */
    CFStringRef
    cf_httpHost = NULL,
    cf_httpsHost = NULL,
    cf_ftpHost = NULL,
    cf_socksHost = NULL;
    int
    httpPort = 80, // Default proxy port values
    httpsPort = 443,
    ftpPort = 21,
    socksPort = 1080;

    CFDictionaryRef dict = SCDynamicStoreCopyProxies(NULL);
    if (dict == NULL) return;

    /* Read the proxy exceptions list */
    CFArrayRef cf_list = CFDictionaryGetValue(dict, kSCPropNetProxiesExceptionsList);

    CFMutableStringRef cf_exceptionList = NULL;
    if (cf_list != NULL) {
        CFIndex len = CFArrayGetCount(cf_list), idx;

        cf_exceptionList = CFStringCreateMutable(NULL, 0);
        for (idx = (CFIndex)0; idx < len; idx++) {
            CFStringRef cf_ehost;
            if ((cf_ehost = CFArrayGetValueAtIndex(cf_list, idx))) {
                /* Convert this exception from Mac OS X syntax to Java syntax.
                 See Radar #3441134 for details. This may generate a string
                 with multiple Java exceptions separated by '|'. */
                char *c_exception = createConvertedException(cf_ehost);
                if (c_exception) {
                    /* Append the host to the list of exclusions. */
                    if (CFStringGetLength(cf_exceptionList) > 0) {
                        CFStringAppendCString(cf_exceptionList, "|", kCFStringEncodingMacRoman);
                    }
                    CFStringAppendCString(cf_exceptionList, c_exception, kCFStringEncodingMacRoman);
                    free(c_exception);
                }
            }
        }
    }

    if (cf_exceptionList != NULL) {
        if (CFStringGetLength(cf_exceptionList) > 0) {
            sProps->exceptionList = createUTF8CString(cf_exceptionList);
        }
        CFRelease(cf_exceptionList);
    }

#define CHECK_PROXY(protocol, PROTOCOL)                                     \
    sProps->protocol##ProxyEnabled =                                        \
    getProxyInfoForProtocol(dict, kSCPropNetProxies##PROTOCOL##Enable,      \
    kSCPropNetProxies##PROTOCOL##Proxy,         \
    kSCPropNetProxies##PROTOCOL##Port,          \
    &cf_##protocol##Host, &protocol##Port);     \
    if (sProps->protocol##ProxyEnabled) {                                   \
        sProps->protocol##Host = createUTF8CString(cf_##protocol##Host);    \
        snprintf(buf, sizeof(buf), "%d", protocol##Port);                   \
        sProps->protocol##Port = malloc(strlen(buf) + 1);                   \
        strcpy(sProps->protocol##Port, buf);                                \
    }

    CHECK_PROXY(http, HTTP);
    CHECK_PROXY(https, HTTPS);
    CHECK_PROXY(ftp, FTP);
    CHECK_PROXY(socks, SOCKS);

#undef CHECK_PROXY

    CFRelease(dict);
}
