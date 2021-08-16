/*
 * Copyright (c) 2004, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include <windows.h>
#include <Winhttp.h>

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"

#include "proxy_util.h"

#include "sun_net_spi_DefaultProxySelector.h"

/*
 * These functions are used by the sun.net.spi.DefaultProxySelector class
 * to access some platform specific settings.
 * On Windows use WinHTTP functions to get the system settings.
 */

/* Keep one static session for all requests. */
static HINTERNET session = NULL;

/*
 * Class:     sun_net_spi_DefaultProxySelector
 * Method:    init
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_net_spi_DefaultProxySelector_init(JNIEnv *env, jclass clazz) {

    /*
     * Get one WinHTTP session handle to initialize the WinHTTP internal data
     * structures. Keep and use only this one for the whole life time.
     */
    session = WinHttpOpen(L"Only used internal", /* we need no real agent string here */
                          WINHTTP_ACCESS_TYPE_DEFAULT_PROXY,
                          WINHTTP_NO_PROXY_NAME,
                          WINHTTP_NO_PROXY_BYPASS,
                          0);
    if (session == NULL) {
        return JNI_FALSE;
    }

    if (!initJavaClass(env)) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}


#define MAX_STR_LEN 1024

/* A linked list element for a proxy */
typedef struct list_item {
    wchar_t *host;
    int port;
    struct list_item *next;
} list_item;

/* Free the linked list */
static void freeList(list_item *head) {
    list_item *next = NULL;
    list_item *current = head;
    while (current != NULL) {
        next = current->next;
        free(current->host);
        free(current);
        current = next;
    }
}


/*
 * Creates a linked list of list_item elements that has to be freed later on.
 * Returns the size of the array as int.
 */
static int createProxyList(LPWSTR win_proxy, const WCHAR *pproto, list_item **head) {
    static const wchar_t separators[] = L"\t\r\n ;";
    list_item *current = NULL;
    int nr_elems = 0;
    wchar_t *context = NULL;
    wchar_t *current_proxy = NULL;
    BOOL error = FALSE;

    /*
     * The proxy server list contains one or more of the following strings
     * separated by semicolons or whitespace:
     *    ([<scheme>=][<scheme>"://"]<server>[":"<port>])
     */
    current_proxy = wcstok_s(win_proxy, separators, &context);
    while (current_proxy != NULL) {
        LPWSTR pport;
        LPWSTR phost;
        int portVal = 0;
        wchar_t *next_proxy = NULL;
        list_item *proxy = NULL;
        wchar_t* pos = NULL;

        /* Filter based on the scheme, if there is one */
        pos = wcschr(current_proxy, L'=');
        if (pos) {
          *pos = L'\0';
          if (wcscmp(current_proxy, pproto) != 0) {
              current_proxy = wcstok_s(NULL, separators, &context);
              continue;
          }
          current_proxy = pos + 1;
        }

        /* Let's check for a scheme and ignore it. */
        if ((phost = wcsstr(current_proxy, L"://")) != NULL) {
            phost += 3;
        } else {
            phost = current_proxy;
        }

        /* Get the port */
        pport = wcschr(phost, L':');
        if (pport != NULL) {
            *pport = 0;
            pport++;
            swscanf(pport, L"%d", &portVal);
        }

        proxy = (list_item *)malloc(sizeof(list_item));
        if (proxy != NULL) {
            proxy->next = NULL;
            proxy->port = portVal;
            proxy->host = _wcsdup(phost);

            if (proxy->host != NULL) {
                if (*head == NULL) {
                    *head = proxy; /* first elem */
                }
                if (current != NULL) {
                    current->next = proxy;
                }
                current = proxy;
                nr_elems++;
            } else {
                free(proxy); /* cleanup */
            }
        }
        /* goto next proxy if available... */
        current_proxy = wcstok_s(NULL, separators, &context);
    }
    return nr_elems;
}



/*
 * Class:     sun_net_spi_DefaultProxySelector
 * Method:    getSystemProxies
 * Signature: ([Ljava/lang/String;Ljava/lang/String;)[Ljava/net/Proxy;
 */
JNIEXPORT jobjectArray JNICALL
Java_sun_net_spi_DefaultProxySelector_getSystemProxies(JNIEnv *env,
                                                       jobject this,
                                                       jstring proto,
                                                       jstring host)
{
    jobjectArray proxy_array = NULL;
    jobject type_proxy = NULL;
    LPCWSTR lpProto;
    LPCWSTR lpHost;
    list_item *head = NULL;

    BOOL                                   use_auto_proxy = FALSE;
    WINHTTP_CURRENT_USER_IE_PROXY_CONFIG   ie_proxy_config;
    WINHTTP_AUTOPROXY_OPTIONS              auto_proxy_options;
    WINHTTP_PROXY_INFO                     proxy_info;
    LPWSTR win_proxy = NULL;
    LPWSTR win_bypass_proxy = NULL;

    memset(&ie_proxy_config, 0, sizeof(WINHTTP_CURRENT_USER_IE_PROXY_CONFIG));
    memset(&auto_proxy_options, 0, sizeof(WINHTTP_AUTOPROXY_OPTIONS));
    memset(&proxy_info, 0, sizeof(WINHTTP_PROXY_INFO));

    lpHost = (*env)->GetStringChars(env, host, NULL);
    if (lpHost == NULL) {
        if (!(*env)->ExceptionCheck(env))
            JNU_ThrowOutOfMemoryError(env, NULL);
        return NULL;
    }

    lpProto = (*env)->GetStringChars(env, proto, NULL);
    if (lpProto == NULL) {
        (*env)->ReleaseStringChars(env, host, lpHost);
        if (!(*env)->ExceptionCheck(env))
            JNU_ThrowOutOfMemoryError(env, NULL);
        return NULL;
    }

    if (WinHttpGetIEProxyConfigForCurrentUser(&ie_proxy_config) == FALSE) {
        /* cleanup and exit */
        (*env)->ReleaseStringChars(env, host, lpHost);
        (*env)->ReleaseStringChars(env, proto, lpProto);
        return NULL;
    }

    if (ie_proxy_config.fAutoDetect) {
        /* Windows uses WPAD */
        auto_proxy_options.dwAutoDetectFlags = WINHTTP_AUTO_DETECT_TYPE_DHCP |
                                               WINHTTP_AUTO_DETECT_TYPE_DNS_A;
        auto_proxy_options.dwFlags = WINHTTP_AUTOPROXY_AUTO_DETECT;
        auto_proxy_options.fAutoLogonIfChallenged = TRUE;
        use_auto_proxy = TRUE;
    } else if (ie_proxy_config.lpszAutoConfigUrl != NULL) {
        /* Windows uses PAC file */
        auto_proxy_options.lpszAutoConfigUrl = ie_proxy_config.lpszAutoConfigUrl;
        auto_proxy_options.dwFlags = WINHTTP_AUTOPROXY_CONFIG_URL;
        use_auto_proxy = TRUE;
    } else if (ie_proxy_config.lpszProxy != NULL) {
        /* Windows uses manually entered proxy. */
        use_auto_proxy = FALSE;
        win_bypass_proxy = ie_proxy_config.lpszProxyBypass;
        win_proxy = ie_proxy_config.lpszProxy;
    }

    if (use_auto_proxy) {
        WCHAR url[MAX_STR_LEN];
        /* Create url for WinHttpGetProxyForUrl */
        _snwprintf(url, sizeof(url) - 1, L"%s://%s", lpProto, lpHost);
        /* Get proxy for URL from Windows */
        use_auto_proxy = WinHttpGetProxyForUrl(session, &url[0], &auto_proxy_options, &proxy_info);
        if (use_auto_proxy) {
            win_proxy = proxy_info.lpszProxy;
            win_bypass_proxy = proxy_info.lpszProxyBypass;
        }
    }

    /* Check the bypass entry. */
    if (NULL != win_bypass_proxy) {
        /*
         * From MSDN:
         * The proxy bypass list contains one or more server names separated by
         * semicolons or whitespace. The proxy bypass list can also contain the
         * string "<local>" to indicate that all local intranet sites are
         * bypassed. Local intranet sites are considered to be all servers that
         * do not contain a period in their name.
         */
        wchar_t *context = NULL;
        LPWSTR s = wcstok_s(win_bypass_proxy, L"; ", &context);

        while (s != NULL) {
            size_t maxlen = wcslen(s);
            if (wcsncmp(s, lpHost, maxlen) == 0) {
                /*
                 * The URL host name matches with one of the prefixes, use a
                 * direct connection.
                 */
                goto noproxy;
            }
            if (wcsncmp(s, L"<local>", maxlen) == 0) {
                /*
                 * All local intranet sites are bypassed - Microsoft consider all
                 * servers that do not contain a period in their name to be local.
                 */
                if (wcschr(lpHost, '.') == NULL) {
                    goto noproxy;
                }
            }
            s = wcstok_s(NULL, L"; ", &context);
        }
    }

    if (win_proxy != NULL) {
        wchar_t *context = NULL;
        int defport = 0;
        int nr_elems = 0;

        /* Set the default port value & proxy type from protocol. */
        if ((wcscmp(lpProto, L"http") == 0) ||
            (wcscmp(lpProto, L"ftp") == 0))
            defport = 80;
        if (wcscmp(lpProto, L"https") == 0)
            defport = 443;
        if (wcscmp(lpProto, L"socks") == 0) {
            defport = 1080;
            type_proxy = (*env)->GetStaticObjectField(env, ptype_class, ptype_socksID);
        } else {
            type_proxy = (*env)->GetStaticObjectField(env, ptype_class, ptype_httpID);
        }
        if (type_proxy == NULL || (*env)->ExceptionCheck(env)) {
            goto noproxy;
        }

        nr_elems = createProxyList(win_proxy, lpProto, &head);
        if (nr_elems != 0 && head != NULL) {
            int index = 0;
            proxy_array = (*env)->NewObjectArray(env, nr_elems, proxy_class, NULL);
            if (proxy_array == NULL || (*env)->ExceptionCheck(env)) {
                goto noproxy;
            }
            while (head != NULL && index < nr_elems) {
                jstring jhost;
                jobject isa;
                jobject proxy;

                if (head->host != NULL && proxy_array != NULL) {
                    /* Let's create the appropriate Proxy object then. */
                    if (head->port == 0) {
                        head->port = defport;
                    }
                    jhost = (*env)->NewString(env, head->host, (jsize)wcslen(head->host));
                    if (jhost == NULL || (*env)->ExceptionCheck(env)) {
                        proxy_array = NULL;
                    }
                    isa = (*env)->CallStaticObjectMethod(env, isaddr_class,
                                                         isaddr_createUnresolvedID, jhost,
                                                         head->port);
                    if (isa == NULL || (*env)->ExceptionCheck(env)) {
                        proxy_array = NULL;
                    }
                    proxy = (*env)->NewObject(env, proxy_class, proxy_ctrID, type_proxy, isa);
                    if (proxy == NULL || (*env)->ExceptionCheck(env)) {
                        proxy_array = NULL;
                    }
                    (*env)->SetObjectArrayElement(env, proxy_array, index, proxy);
                    if ((*env)->ExceptionCheck(env)) {
                        proxy_array = NULL;
                    }
                    index++;
                }
                head = head->next;
            }
        }
    }

noproxy:
    if (head != NULL) {
        freeList(head);
    }
    if (proxy_info.lpszProxy != NULL)
      GlobalFree(proxy_info.lpszProxy);
    if (proxy_info.lpszProxyBypass != NULL)
      GlobalFree(proxy_info.lpszProxyBypass);
    if (ie_proxy_config.lpszAutoConfigUrl != NULL)
      GlobalFree(ie_proxy_config.lpszAutoConfigUrl);
    if (ie_proxy_config.lpszProxy != NULL)
      GlobalFree(ie_proxy_config.lpszProxy);
    if (ie_proxy_config.lpszProxyBypass != NULL)
      GlobalFree(ie_proxy_config.lpszProxyBypass);
    if (lpHost != NULL)
      (*env)->ReleaseStringChars(env, host, lpHost);
    if (lpProto != NULL)
      (*env)->ReleaseStringChars(env, proto, lpProto);

    return proxy_array;
}
