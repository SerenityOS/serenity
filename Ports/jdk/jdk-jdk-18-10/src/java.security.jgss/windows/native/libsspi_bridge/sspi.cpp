/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

// This library is client-side only, and only supports the default credentials.
// It speaks krb5 and SPNEGO. NTLM is excluded from SPNEGO negotiation.
//
// This library can be built directly with the following command:
//   cl -I %OPENJDK%\src\java.security.jgss\share\native\libj2gss\ sspi.cpp \
//      -link -dll -out:sspi_bridge.dll

#define UNICODE
#define _UNICODE

#include <windows.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <Strsafe.h>
#include <ntsecapi.h>
#include <new>

#define GSS_DLL_FILE
#include <gssapi.h>

#define SECURITY_WIN32
#include <sspi.h>

#pragma comment(lib, "secur32.lib")

// Otherwise an exception will be thrown
#define new new (std::nothrow)

// A debugging macro
#define PP(fmt, ...) \
        if (trace) { \
            fprintf(stderr, "[SSPI:%ld] "fmt"\n", __LINE__, ##__VA_ARGS__); \
            fflush(stderr); \
        }
#define SEC_SUCCESS(status) ((*minor_status = (status)), (status) >= SEC_E_OK)

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

// When SSPI_BRIDGE_TRACE is set, debug info goes to stderr. The value is ignored.
char* trace = getenv("SSPI_BRIDGE_TRACE");

void
dump(const char* title, PBYTE data, size_t len)
{
    if (trace) {
        fprintf(stderr, "==== %s ====\n", title);
        for (size_t i = 0; i < len; i++) {
            if (i != 0 && i % 16 == 0) {
                fprintf(stderr, "\n");
            }
            fprintf(stderr, "%02X ", *(data + i) & 0xff);
        }
        fprintf(stderr, "\n");
    }
}

gss_OID_desc KRB5_OID = {9, (void*)"\x2a\x86\x48\x86\xf7\x12\x01\x02\x02"};
gss_OID_desc SPNEGO_OID = {6, (void*)"\x2b\x06\x01\x05\x05\x02"};
gss_OID_desc USER_NAME_OID = {10, (void*)"\x2a\x86\x48\x86\xf7\x12\x01\x02\x01\x01"};
gss_OID_desc KRB5_NAME_OID = {10, (void*)"\x2a\x86\x48\x86\xf7\x12\x01\x02\x02\x01"};
gss_OID_desc HOST_SERVICE_NAME_OID = {10, (void*)"\x2a\x86\x48\x86\xf7\x12\x01\x02\x01\x04"};
gss_OID_desc EXPORT_NAME_OID = {6, (void*)"\x2b\x06\x01\x05\x06\x04"};

struct gss_name_struct {
    SEC_WCHAR* name;
};

struct gss_ctx_id_struct {
    CredHandle* phCred;
    CtxtHandle hCtxt;
    SecPkgContext_Sizes SecPkgContextSizes;
    SecPkgContext_NativeNames nnames;
    BOOLEAN established;
    BOOLEAN isSPNEGO;
    BOOLEAN isLocalCred;
    OM_uint32 flags;
};

struct gss_cred_id_struct {
    CredHandle* phCredK;
    CredHandle* phCredS;
    long time;
};

/* This section holds supporting functions that are not exported */

static OM_uint32
seconds_until(int inputIsUTC, TimeStamp *time)
{
    // time is local time
    LARGE_INTEGER uiLocal;
    FILETIME now;
    GetSystemTimeAsFileTime(&now);
    if (!inputIsUTC) {
        FILETIME nowLocal;
        if (FileTimeToLocalFileTime(&now, &nowLocal) == 0) {
            return -1;
        }
        now = nowLocal;
    }
    uiLocal.HighPart = now.dwHighDateTime;
    uiLocal.LowPart = now.dwLowDateTime;
    if (time->QuadPart < uiLocal.QuadPart) {
        return 0;
    }
    ULONGLONG diff = (time->QuadPart - uiLocal.QuadPart) / 10000000;
    if (diff > (ULONGLONG)~(OM_uint32)0) {
        return GSS_C_INDEFINITE;
    }
    return (OM_uint32)diff;
}

static void
show_time(char* label, TimeStamp* ts)
{
    if (trace) {
        SYSTEMTIME stLocal;
        FileTimeToSystemTime((FILETIME*)ts, &stLocal);

        // Build a string showing the date and time.
        PP("%s: %02d/%02d/%d  %02d:%02d %uld", label,
            stLocal.wMonth, stLocal.wDay, stLocal.wYear,
            stLocal.wHour, stLocal.wMinute,
            seconds_until(1, ts));
    }
}

// isSPNEGO: true, SPNEGO. false, Kerberos.
static gss_ctx_id_t
new_context(BOOLEAN isSPNEGO)
{
    gss_ctx_id_t out = new gss_ctx_id_struct;
    if (out == NULL) {
        return NULL;
    }
    out->phCred = NULL;
    out->hCtxt.dwLower = out->hCtxt.dwUpper = NULL;
    out->established = FALSE;
    out->SecPkgContextSizes.cbMaxSignature
            = out->SecPkgContextSizes.cbBlockSize
            = out->SecPkgContextSizes.cbSecurityTrailer
            = 0;
    out->nnames.sClientName = out->nnames.sServerName = NULL;
    out->isSPNEGO = isSPNEGO;
    out->isLocalCred = FALSE;
    return out;
}

static gss_cred_id_t
new_cred()
{
    gss_cred_id_t out = new gss_cred_id_struct;
    if (out) {
        out->phCredK = out->phCredS = NULL;
        out->time = 0L;
    }
    return out;
}

static int
flag_sspi_to_gss(int fin)
{
    int fout = 0;
    if (fin & ISC_REQ_MUTUAL_AUTH) fout |= GSS_C_MUTUAL_FLAG;
    if (fin & ISC_REQ_CONFIDENTIALITY) fout |= GSS_C_CONF_FLAG;
    if (fin & ISC_REQ_DELEGATE) fout |= GSS_C_DELEG_FLAG;
    if (fin & ISC_REQ_INTEGRITY) fout |= GSS_C_INTEG_FLAG;
    if (fin & ISC_REQ_REPLAY_DETECT) fout |= GSS_C_REPLAY_FLAG;
    if (fin & ISC_REQ_SEQUENCE_DETECT) fout |= GSS_C_SEQUENCE_FLAG;
    return fout;
}

static int
flag_gss_to_sspi(int fin)
{
    int fout = 0;
    if (fin & GSS_C_MUTUAL_FLAG) fout |= ISC_RET_MUTUAL_AUTH;
    if (fin & GSS_C_CONF_FLAG) fout |= ISC_RET_CONFIDENTIALITY;
    if (fin & GSS_C_DELEG_FLAG) fout |= ISC_RET_DELEGATE;
    if (fin & GSS_C_INTEG_FLAG) fout |= ISC_RET_INTEGRITY;
    if (fin & GSS_C_REPLAY_FLAG) fout |= ISC_RET_REPLAY_DETECT;
    if (fin & GSS_C_SEQUENCE_FLAG) fout |= ISC_RET_SEQUENCE_DETECT;
    return fout;
}

static BOOLEAN
is_same_oid(gss_const_OID o2, gss_const_OID o1)
{
    return o1 && o2 && o1->length == o2->length
            && !memcmp(o1->elements, o2->elements, o2->length);
}

static BOOLEAN
has_oid(gss_const_OID_set set, gss_const_OID oid)
{
    for (size_t i = 0; i < set->count; i++) {
        if (is_same_oid(&set->elements[i], oid)) {
            return TRUE;
        }
    }
    return FALSE;
}

static void
show_oid(gss_const_OID mech)
{
    if (trace) {
        if (is_same_oid(mech, &KRB5_OID)) {
            PP("Kerberos mech");
        } else if (is_same_oid(mech, &SPNEGO_OID)) {
            PP("SPNEGO mech");
        } else if (is_same_oid(mech, &USER_NAME_OID)) {
            PP("NT_USER_NAME name-type");
        } else if (is_same_oid(mech, &KRB5_NAME_OID)) {
            PP("KRB5_NAME name-type");
        } else if (is_same_oid(mech, &HOST_SERVICE_NAME_OID)) {
            PP("NT_HOSTBASED_SERVICE name-type");
        } else if (is_same_oid(mech, &EXPORT_NAME_OID)) {
            PP("NT_EXPORT_NAME name-type");
        } else {
            dump("UNKNOWN OID", (PBYTE)mech->elements, mech->length);
        }
    }
}

static void
show_oid_set(gss_const_OID_set mechs)
{
    if (trace) {
        if (mechs == NULL) {
            PP("OID set is NULL");
            return;
        }
        PP("gss_OID_set.count is %d", (int)mechs->count);
        for (size_t i = 0; i < mechs->count; i++) {
            show_oid(&mechs->elements[i]);
        }
    }
}

// Add realm to a name if there was none.
// Returns a newly allocated name.
static WCHAR*
get_full_name(WCHAR* input)
{
    // input has realm, no need to add one
    for (int i = 0;; i++) {
        if (!input[i]) { // the end
            break;
        }
        if (input[i] == L'\\') { // escaped
            i++;
            continue;
        }
        if (input[i] == L'@') {
            return _wcsdup(input);
        }
    }

    // Always use the default domain
    WCHAR* realm = _wgetenv(L"USERDNSDOMAIN");
    if (realm == NULL) {
        realm = L"";
    }

    size_t oldlen = wcslen(input);
    size_t newlen = oldlen + 1 + wcslen(realm) + 1;

    WCHAR* fullname = new WCHAR[newlen];
    if (!fullname) {
        return NULL;
    }
    wcscpy_s(fullname, newlen, input);
    wcscat_s(fullname, newlen, L"@");
    wcscat_s(fullname, newlen, realm);

    PP("get_full_name returns %ls", fullname);
    return fullname;
}

/* End support section */

/* This section holds GSS-API exported functions */

#define CHECK_OUTPUT(x)  if (!x) return GSS_S_CALL_INACCESSIBLE_WRITE;
#define CHECK_BUFFER(b)  if (!b || !b->value) return GSS_S_CALL_INACCESSIBLE_READ;
#define CHECK_OID(o)     if (!o || !o->elements) return GSS_S_CALL_INACCESSIBLE_READ;
#define CHECK_NAME(n)    if (!n || !(n->name)) return GSS_S_BAD_NAME;
#define CHECK_CONTEXT(c) if (!c) return GSS_S_NO_CONTEXT;
#define CHECK_CRED(c)    if (!c || (!(cred_handle->phCredK) && !(cred_handle->phCredS))) \
                                return GSS_S_NO_CRED;

__declspec(dllexport) OM_uint32
gss_release_name(OM_uint32 *minor_status,
                 gss_name_t *name)
{
    PP(">>>> Calling gss_release_name %p...", *name);
    if (name != NULL && *name != GSS_C_NO_NAME) {
        if ((*name)->name != NULL) {
            delete[] (*name)->name;
        }
        delete *name;
        *name = GSS_C_NO_NAME;
    }
    return GSS_S_COMPLETE;
}

__declspec(dllexport) OM_uint32
gss_import_name(OM_uint32 *minor_status,
                gss_const_buffer_t input_name_buffer,
                gss_const_OID input_name_type,
                gss_name_t *output_name)
{
    PP(">>>> Calling gss_import_name...");
    CHECK_BUFFER(input_name_buffer)
    CHECK_OUTPUT(output_name)

    int len = (int)input_name_buffer->length;
    LPSTR input = (LPSTR)input_name_buffer->value;
    if (input_name_type != NULL
            && is_same_oid(input_name_type, &EXPORT_NAME_OID)) {
        if (len < 4 || input[0] != 4 || input[1] != 1 || input[2] != 0) {
            return GSS_S_FAILURE;
        }
        int mechLen = (int)input[3]; /* including 06 len */
        len -= mechLen + 8; /* 4 header bytes, and an int32 length after OID */
        if (len <= 0) {
            return GSS_S_FAILURE;
        }
        // Reject if mech is not krb5
        if (mechLen - 2!= KRB5_OID.length ||
                memcmp(input + 6, KRB5_OID.elements, mechLen - 2)) {
            return GSS_S_FAILURE;;
        }
        input = input + mechLen + 8;
    }

    SEC_WCHAR* value = new SEC_WCHAR[len + 1];
    if (value == NULL) {
        goto err;
    }

    len = MultiByteToWideChar(CP_UTF8, 0, input, len, value, len+1);
    if (len == 0) {
        goto err;
    }
    value[len] = 0;

    PP("import_name from %ls", value);

    if (len > 33 && !wcscmp(value+len-33, L"@WELLKNOWN:ORG.H5L.REFERALS-REALM")) {
        // Remove the wellknown referrals realms
        value[len-33] = 0;
        len -= 33;
    } else if (value[len-1] == L'@') {
        // Remove the empty realm. It might come from an NT_EXPORT_NAME.
        value[len-1] = 0;
        len--;
    }
    if (len == 0) {
        goto err;
    }

    if (input_name_type != NULL
            && is_same_oid(input_name_type, &HOST_SERVICE_NAME_OID)) {
        // HOST_SERVICE_NAME_OID takes the form of service@host.
        for (int i = 0; i < len; i++) {
            if (value[i] == L'\\') {
                i++;
                continue;
            }
            if (value[i] == L'@') {
                value[i] = L'/';
                break;
            }
        }
        PP("Host-based service now %ls", value);
    }
    PP("import_name to %ls", value);
    gss_name_struct* name = new gss_name_struct;
    if (name == NULL) {
        goto err;
    }
    name->name = value;
    *output_name = (gss_name_t) name;
    return GSS_S_COMPLETE;
err:
    if (value != NULL) {
        delete[] value;
    }
    return GSS_S_FAILURE;
}

__declspec(dllexport) OM_uint32
gss_compare_name(OM_uint32 *minor_status,
                 gss_const_name_t name1,
                 gss_const_name_t name2,
                 int *name_equal)
{
    PP(">>>> Calling gss_compare_name...");
    CHECK_NAME(name1)
    CHECK_NAME(name2)
    CHECK_OUTPUT(name_equal)

    *name_equal = 0;

    SEC_WCHAR* n1 = name1->name;
    SEC_WCHAR* n2 = name2->name;
    PP("Comparing %ls and %ls", n1, n2);

    int l1 = lstrlen(n1);
    int l2 = lstrlen(n2);
    int r1 = l1; // position of @ or the end if none
    int r2 = l2;
    int i;

    for (i = 0; i < l1; i++) {
        if (n1[i] == L'\\') {
            i++;
            continue;
        }
        if (n1[i] == L'@') {
            r1 = i;
            break;
        }
    }

    for (i = 0; i < l2; i++) {
        if (n2[i] == L'\\') {
            i++;
            continue;
        }
        if (n2[i] == L'@') {
            r2 = i;
            break;
        }
    }

    if (l1 < l2 && l1 != r2
            || l2 < l1 && l2 != r1) {
        return GSS_S_COMPLETE; // different
    }

    if (l1 > l2) {
        l1 = l2; // choose the smaller one. longer=smaller @ ...
    }

    // Two names are equal if they are the same or one has no realm and
    // one has realm but they have the same name. If both have realm but
    // different, they are treated different even if the names are the same.
    // Note: the default name concept is not used here.
    // Principal names on Windows are case-insensitive, both user name
    // and service principal name.
    if (CompareStringEx(LOCALE_NAME_SYSTEM_DEFAULT, NORM_IGNORECASE,
            n1, l1, n2, l1, NULL, NULL, 0) == CSTR_EQUAL) {
        *name_equal = 1;
    }
    return GSS_S_COMPLETE;
}

__declspec(dllexport) OM_uint32
gss_canonicalize_name(OM_uint32 *minor_status,
                      gss_const_name_t input_name,
                      gss_const_OID mech_type,
                      gss_name_t *output_name)
{
    PP(">>>> Calling gss_canonicalize_name...");
    CHECK_NAME(input_name)
    CHECK_OID(mech_type)
    CHECK_OUTPUT(output_name)

    if (!is_same_oid(mech_type, &KRB5_OID)) {
        PP("Cannot canonicalize to non-krb5 OID");
        return GSS_S_BAD_MECH;
    }
    gss_name_t names2 = new gss_name_struct;
    if (names2 == NULL) {
        return GSS_S_FAILURE;
    }
    names2->name = get_full_name(input_name->name);
    if (names2->name == NULL) {
        delete names2;
        return GSS_S_FAILURE;
    }
    *output_name = names2;
    return GSS_S_COMPLETE;
}

__declspec(dllexport) OM_uint32
gss_export_name(OM_uint32 *minor_status,
                gss_const_name_t input_name,
                gss_buffer_t exported_name)
{
    PP(">>>> Calling gss_export_name...");
    CHECK_NAME(input_name)
    CHECK_OUTPUT(exported_name)

    OM_uint32 result = GSS_S_FAILURE;
    SEC_WCHAR* name = input_name->name;
    SEC_WCHAR* fullname = get_full_name(name);
    if (!fullname) {
        goto err;
    }
    PP("Make fullname: %ls -> %ls", name, fullname);
    int len;
    size_t namelen = wcslen(fullname);
    if (namelen > 255) {
        goto err;
    }
    len = (int)namelen;
    // We only deal with not-so-long names.
    // 04 01 00 ** 06 ** OID len:int32 name
    int mechLen = KRB5_OID.length;
    char* buffer = new char[10 + mechLen + len];
    if (buffer == NULL) {
        goto err;
    }
    buffer[0] = 4;
    buffer[1] = 1;
    buffer[2] = 0;
    buffer[3] = 2 + mechLen;
    buffer[4] = 6;
    buffer[5] = mechLen;
    memcpy_s(buffer + 6, mechLen, KRB5_OID.elements, mechLen);
    buffer[6 + mechLen] = buffer[7 + mechLen] = buffer[8 + mechLen] = 0;
    buffer[9 + mechLen] = (char)len;
    len = WideCharToMultiByte(CP_UTF8, 0, fullname, len,
                buffer+10+mechLen, len, NULL, NULL);
    if (len == 0) {
        delete[] buffer;
        goto err;
    }
    exported_name->length = 10 + mechLen + len;
    exported_name->value = buffer;
    result = GSS_S_COMPLETE;
err:
    if (fullname != name) {
        delete[] fullname;
    }
    return result;
}

__declspec(dllexport) OM_uint32
gss_display_name(OM_uint32 *minor_status,
                 gss_const_name_t input_name,
                 gss_buffer_t output_name_buffer,
                 gss_OID *output_name_type)
{
    PP(">>>> Calling gss_display_name...");
    CHECK_NAME(input_name)
    CHECK_OUTPUT(output_name_buffer)

    SEC_WCHAR* names = input_name->name;
    int len = (int)wcslen(names);
    char* buffer = new char[4*len+1];
    if (buffer == NULL) {
        return GSS_S_FAILURE;
    }
    len = WideCharToMultiByte(CP_UTF8, 0, names, len, buffer, 4*len, NULL, NULL);
    if (len == 0) {
        delete[] buffer;
        return GSS_S_FAILURE;
    }
    buffer[len] = 0;
    output_name_buffer->length = len;
    output_name_buffer->value = buffer;
    PP("Name found: %ls -> %d [%s]", names, len, buffer);
    if (output_name_type != NULL) {
        *output_name_type = &KRB5_NAME_OID;
    }
    return GSS_S_COMPLETE;
}

__declspec(dllexport) OM_uint32
gss_acquire_cred(OM_uint32 *minor_status,
                 gss_const_name_t desired_name,
                 OM_uint32 time_req,
                 gss_const_OID_set desired_mechs,
                 gss_cred_usage_t cred_usage,
                 gss_cred_id_t *output_cred_handle,
                 gss_OID_set *actual_mechs,
                 OM_uint32 *time_rec)
{
    PP(">>>> Calling gss_acquire_cred...");
    CHECK_OUTPUT(output_cred_handle)

    SECURITY_STATUS ss;
    TimeStamp ts;
    ts.QuadPart = 0;
    cred_usage = 0;
    PP("AcquireCredentialsHandle with %d %p", cred_usage, desired_mechs);
    show_oid_set(desired_mechs);

    BOOLEAN reqKerberos, reqSPNEGO;

    if (!desired_mechs) {
        reqKerberos = reqSPNEGO = TRUE;
    } else {
        if (has_oid(desired_mechs, &KRB5_OID)) {
            PP("reqKerberos");
            reqKerberos = TRUE;
        }
        if (has_oid(desired_mechs, &SPNEGO_OID)) {
            PP("reqSPNEGO");
            reqSPNEGO = TRUE;
        }
        if (!reqSPNEGO && !reqKerberos) {
            return GSS_S_BAD_MECH;
        }
    }

    if (actual_mechs) {
        *actual_mechs = GSS_C_NO_OID_SET;
    }

    gss_cred_id_t cred = new_cred();
    if (cred == NULL) {
        goto err;
    }

    if (reqKerberos) {
        cred->phCredK = new CredHandle;
        if (cred->phCredK == NULL) {
            goto err;
        }
        ss = AcquireCredentialsHandle(
                NULL,
                L"Kerberos",
                cred_usage == 0 ? SECPKG_CRED_BOTH :
                    (cred_usage == 1 ? SECPKG_CRED_OUTBOUND : SECPKG_CRED_INBOUND),
                NULL,
                NULL,
                NULL,
                NULL,
                cred->phCredK,
                &ts);
        if (!(SEC_SUCCESS(ss))) {
            delete cred->phCredK;
            cred->phCredK = NULL;
            goto err;
        }
    }

    if (reqSPNEGO) {
        cred->phCredS = new CredHandle;
        if (cred->phCredS == NULL) {
            goto err;
        }
        SEC_WINNT_AUTH_IDENTITY_EX auth;
        ZeroMemory(&auth, sizeof(auth));
        auth.Version = SEC_WINNT_AUTH_IDENTITY_VERSION;
        auth.Length = sizeof(auth);
        auth.Flags = SEC_WINNT_AUTH_IDENTITY_UNICODE;
        auth.PackageList = (unsigned short*)L"Kerberos";
        auth.PackageListLength = 8;
        ss = AcquireCredentialsHandle(
                NULL,
                L"Negotiate",
                cred_usage == 0 ? SECPKG_CRED_BOTH :
                    (cred_usage == 1 ? SECPKG_CRED_OUTBOUND : SECPKG_CRED_INBOUND),
                NULL,
                &auth,
                NULL,
                NULL,
                cred->phCredS,
                &ts);
        if (!(SEC_SUCCESS(ss))) {
            delete cred->phCredS;
            cred->phCredS = NULL;
            goto err;
        }
    }

    if (actual_mechs) {
        if (gss_create_empty_oid_set(minor_status, actual_mechs)) {
            goto err;
        }
        if (reqKerberos) {
            if (gss_add_oid_set_member(minor_status, &KRB5_OID, actual_mechs)) {
                goto err;
            }
        }
        if (reqSPNEGO) {
            if (gss_add_oid_set_member(minor_status, &SPNEGO_OID, actual_mechs)) {
                goto err;
            }
        }
    }

    *output_cred_handle = (gss_cred_id_t)cred;

    // Note: ts here is weirdly huge, maybe because LSA retains the
    // password and can re-acquire a TGT at anytime. It will be
    // GSSCredential.INDEFINITE_LIFETIME.
    show_time("cred expiration", &ts);
    cred->time = seconds_until(1, &ts);
    if (time_rec != NULL) {
        *time_rec = cred->time;
    }

    // Since only default cred is supported, if there is a desired_name,
    // we must make sure it is the same as the realname of the default cred.
    if (desired_name != NULL) {
        PP("Acquiring cred with a name. Check if it's me.");
        gss_name_t realname;
        if (gss_inquire_cred(minor_status, *output_cred_handle, &realname,
                NULL, NULL, NULL) != GSS_S_COMPLETE) {
            PP("Cannot get owner name of default creds");
            goto err;
        }
        SEC_WCHAR* rnames = realname->name;
        SEC_WCHAR* dnames = desired_name->name;
        int equals = 0;
        gss_compare_name(minor_status, realname, desired_name, &equals);
        gss_release_name(minor_status, &realname);
        PP("Comparing result: %d", equals);
        if (!equals) {
            goto err;
        }
    }

    return GSS_S_COMPLETE;
err:
    if (cred) {
        OM_uint32 dummy;
        gss_release_cred(&dummy, &cred);
    }
    if (actual_mechs) {
        OM_uint32 dummy;
        gss_release_oid_set(&dummy, actual_mechs);
    }
    return GSS_S_FAILURE;
}

__declspec(dllexport) OM_uint32
gss_release_cred(OM_uint32 *minor_status,
                 gss_cred_id_t *cred_handle)
{
    PP(">>>> Calling gss_release_cred...");
    if (cred_handle && *cred_handle) {
        if ((*cred_handle)->phCredK) {
            FreeCredentialsHandle((*cred_handle)->phCredK);
            delete (*cred_handle)->phCredK;
        }
        if ((*cred_handle)->phCredS) {
            FreeCredentialsHandle((*cred_handle)->phCredS);
            delete (*cred_handle)->phCredS;
        }
        delete *cred_handle;
        *cred_handle = GSS_C_NO_CREDENTIAL;
    }
    return GSS_S_COMPLETE;
}

__declspec(dllexport) OM_uint32
gss_inquire_cred(OM_uint32 *minor_status,
                 gss_const_cred_id_t cred_handle,
                 gss_name_t *name,
                 OM_uint32 *lifetime,
                 gss_cred_usage_t *cred_usage,
                 gss_OID_set *mechanisms)
{
    PP(">>>> Calling gss_inquire_cred...");
    CHECK_CRED(cred_handle)

    CredHandle* cred = cred_handle->phCredK
            ? cred_handle->phCredK
            : cred_handle->phCredS;
    SECURITY_STATUS ss;
    if (name) {
        *name = GSS_C_NO_NAME;
        SecPkgCredentials_Names snames;
        ss = QueryCredentialsAttributes(cred, SECPKG_CRED_ATTR_NAMES, &snames);
        if (!SEC_SUCCESS(ss)) {
            return GSS_S_FAILURE;
        }
        SEC_WCHAR* names = new SEC_WCHAR[lstrlen(snames.sUserName) + 1];
        if (names == NULL) {
            return GSS_S_FAILURE;
        }
        StringCchCopy(names, lstrlen(snames.sUserName) + 1, snames.sUserName);
        FreeContextBuffer(snames.sUserName);
        PP("Allocate new name at %p", names);
        gss_name_t name1 = new gss_name_struct;
        if (name1 == NULL) {
            delete[] names;
            return GSS_S_FAILURE;
        }
        name1->name = names;
        *name = (gss_name_t) name1;
    }
    if (lifetime) {
        *lifetime = cred_handle->time;
    }
    if (cred_usage) {
        *cred_usage = 1; // We only support INITIATE_ONLY now
    }
    if (mechanisms) {
        // Useless for Java
    }
    // Others inquiries not supported yet
    return GSS_S_COMPLETE;
}

__declspec(dllexport) OM_uint32
gss_import_sec_context(OM_uint32 *minor_status,
                       gss_const_buffer_t interprocess_token,
                       gss_ctx_id_t *context_handle)
{
    // Not transferable, return FAILURE
    PP(">>>> Calling UNIMPLEMENTED gss_import_sec_context...");
    *minor_status = 0;
    return GSS_S_FAILURE;
}

__declspec(dllexport) OM_uint32
gss_init_sec_context(OM_uint32 *minor_status,
                     gss_const_cred_id_t initiator_cred_handle,
                     gss_ctx_id_t *context_handle,
                     gss_const_name_t target_name,
                     gss_const_OID mech_type,
                     OM_uint32 req_flags,
                     OM_uint32 time_req,
                     gss_const_channel_bindings_t input_chan_bindings,
                     gss_const_buffer_t input_token,
                     gss_OID *actual_mech_type,
                     gss_buffer_t output_token,
                     OM_uint32 *ret_flags,
                     OM_uint32 *time_rec)
{
    PP(">>>> Calling gss_init_sec_context...");
    CHECK_NAME(target_name)
    CHECK_OUTPUT(output_token)

    SECURITY_STATUS ss;
    TimeStamp lifeTime;
    SecBufferDesc inBuffDesc;
    SecBuffer inSecBuff;
    SecBufferDesc outBuffDesc;
    SecBuffer outSecBuff;
    BOOLEAN isSPNEGO = is_same_oid(mech_type, &SPNEGO_OID);
    CredHandle* newCred = NULL;

    gss_ctx_id_t pc;

    output_token->length = 0;
    output_token->value = NULL;

    BOOLEAN firstTime = (*context_handle == GSS_C_NO_CONTEXT);
    PP("First time? %d", firstTime);
    if (firstTime) {
        pc = new_context(isSPNEGO);
        if (pc == NULL) {
            return GSS_S_FAILURE;
        }
        *context_handle = (gss_ctx_id_t) pc;
    } else {
        pc = *context_handle;
    }

    if (pc == NULL) {
        return GSS_S_NO_CONTEXT;
    }

    DWORD outFlag;
    TCHAR outName[100];

    OM_uint32 minor;
    gss_buffer_desc tn;
    gss_display_name(&minor, target_name, &tn, NULL);
    int len = MultiByteToWideChar(CP_UTF8, 0, (LPCCH)tn.value, (int)tn.length,
            outName, sizeof(outName) - 1);
    if (len == 0) {
        goto err;
    }
    outName[len] = 0;

    int flag = flag_gss_to_sspi(req_flags) | ISC_REQ_ALLOCATE_MEMORY;

    outBuffDesc.ulVersion = SECBUFFER_VERSION;
    outBuffDesc.cBuffers = 1;
    outBuffDesc.pBuffers = &outSecBuff;

    outSecBuff.BufferType = SECBUFFER_TOKEN;

    if (!firstTime) {
        inBuffDesc.ulVersion = SECBUFFER_VERSION;
        inBuffDesc.cBuffers = 1;
        inBuffDesc.pBuffers = &inSecBuff;

        inSecBuff.BufferType = SECBUFFER_TOKEN;
        inSecBuff.cbBuffer = (ULONG)input_token->length;
        inSecBuff.pvBuffer = input_token->value;
    } else if (!pc->phCred) {
        if (isSPNEGO && initiator_cred_handle
                && initiator_cred_handle->phCredS) {
            PP("Find SPNEGO credentials");
            pc->phCred = initiator_cred_handle->phCredS;
            pc->isLocalCred = FALSE;
        } else if (!isSPNEGO && initiator_cred_handle
                && initiator_cred_handle->phCredK) {
            PP("Find Kerberos credentials");
            pc->phCred = initiator_cred_handle->phCredK;
            pc->isLocalCred = FALSE;
        } else {
            PP("No credentials provided, acquire myself");
            newCred = new CredHandle;
            if (!newCred) {
                goto err;
            }
            SEC_WINNT_AUTH_IDENTITY_EX auth;
            ZeroMemory(&auth, sizeof(auth));
            auth.Version = SEC_WINNT_AUTH_IDENTITY_VERSION;
            auth.Length = sizeof(auth);
            auth.Flags = SEC_WINNT_AUTH_IDENTITY_UNICODE;
            auth.PackageList = (unsigned short*)L"Kerberos";
            auth.PackageListLength = 8;
            ss = AcquireCredentialsHandle(
                    NULL,
                    isSPNEGO ? L"Negotiate" : L"Kerberos",
                    SECPKG_CRED_OUTBOUND,
                    NULL,
                    isSPNEGO ? &auth : NULL,
                    NULL,
                    NULL,
                    newCred,
                    &lifeTime);
            if (!(SEC_SUCCESS(ss))) {
                goto err;
            }
            pc->phCred = newCred;
            pc->isLocalCred = TRUE;
        }
    }
    ss = InitializeSecurityContext(
            pc->phCred,
            firstTime ? NULL : &pc->hCtxt,
            outName,
            flag,
            0,
            SECURITY_NATIVE_DREP,
            firstTime ? NULL : &inBuffDesc,
            0,
            &pc->hCtxt,
            &outBuffDesc,
            &outFlag,
            &lifeTime);

    if (!SEC_SUCCESS(ss)) {
        PP("InitializeSecurityContext failed");
        goto err;
    }

    pc->flags = *ret_flags = flag_sspi_to_gss(outFlag);

    // Ignore the result of the next call. Might fail before context established.
    QueryContextAttributes(
            &pc->hCtxt, SECPKG_ATTR_SIZES, &pc->SecPkgContextSizes);
    PP("cbMaxSignature: %ld. cbBlockSize: %ld. cbSecurityTrailer: %ld",
            pc->SecPkgContextSizes.cbMaxSignature,
            pc->SecPkgContextSizes.cbBlockSize,
            pc->SecPkgContextSizes.cbSecurityTrailer);

    output_token->length = outSecBuff.cbBuffer;
    if (outSecBuff.cbBuffer) {
        // No idea how user would free the data. Let's duplicate one.
        output_token->value = new char[outSecBuff.cbBuffer];
        if (!output_token->value) {
            FreeContextBuffer(outSecBuff.pvBuffer);
            goto err;
        }
        memcpy(output_token->value, outSecBuff.pvBuffer, outSecBuff.cbBuffer);
        FreeContextBuffer(outSecBuff.pvBuffer);
    }

    if (ss == SEC_I_CONTINUE_NEEDED) {
        return GSS_S_CONTINUE_NEEDED;
    } else {
        pc->established = true;
        ss = QueryContextAttributes(&pc->hCtxt, SECPKG_ATTR_NATIVE_NAMES, &pc->nnames);
        if (!SEC_SUCCESS(ss)) {
            goto err;
        }
        PP("Names. %ls %ls", pc->nnames.sClientName, pc->nnames.sServerName);
        *ret_flags |= GSS_C_PROT_READY_FLAG;
        return GSS_S_COMPLETE;
    }
err:
    if (newCred) {
        delete newCred;
    }
    if (firstTime) {
        OM_uint32 dummy;
        gss_delete_sec_context(&dummy, context_handle, GSS_C_NO_BUFFER);
    }
    if (output_token->value) {
        gss_release_buffer(NULL, output_token);
    }
    output_token = GSS_C_NO_BUFFER;
    return GSS_S_FAILURE;
}

__declspec(dllexport) OM_uint32
gss_accept_sec_context(OM_uint32 *minor_status,
                       gss_ctx_id_t *context_handle,
                       gss_const_cred_id_t acceptor_cred_handle,
                       gss_const_buffer_t input_token,
                       gss_const_channel_bindings_t input_chan_bindings,
                       gss_name_t *src_name,
                       gss_OID *mech_type,
                       gss_buffer_t output_token,
                       OM_uint32 *ret_flags,
                       OM_uint32 *time_rec,
                       gss_cred_id_t *delegated_cred_handle)
{
    PP(">>>> Calling UNIMPLEMENTED gss_accept_sec_context...");
    PP("gss_accept_sec_context is not supported in this initiator-only library");
    return GSS_S_FAILURE;
}

__declspec(dllexport) OM_uint32
gss_inquire_context(OM_uint32 *minor_status,
                    gss_const_ctx_id_t context_handle,
                    gss_name_t *src_name,
                    gss_name_t *targ_name,
                    OM_uint32 *lifetime_rec,
                    gss_OID *mech_type,
                    OM_uint32 *ctx_flags,
                    int *locally_initiated,
                    int *open)
{
    PP(">>>> Calling gss_inquire_context...");
    CHECK_CONTEXT(context_handle)

    gss_name_t n1 = NULL;
    gss_name_t n2 = NULL;
    if (!context_handle->established) {
        return GSS_S_NO_CONTEXT;
    }
    if (src_name != NULL) {
        n1 = new gss_name_struct;
        if (n1 == NULL) {
            goto err;
        }
        n1->name = new SEC_WCHAR[lstrlen(context_handle->nnames.sClientName) + 1];
        if (n1->name == NULL) {
            goto err;
        }
        PP("Allocate new name at %p", n1->name);
        StringCchCopy(n1->name, lstrlen(context_handle->nnames.sClientName) + 1,
                context_handle->nnames.sClientName);
        *src_name = (gss_name_t) n1;
    }
    if (targ_name != NULL) {
        n2 = new gss_name_struct;
        if (n2 == NULL) {
            goto err;
        }
        n2->name = new SEC_WCHAR[lstrlen(context_handle->nnames.sServerName) + 1];
        if (n2->name == NULL) {
            goto err;
        }
        PP("Allocate new name at %p", n2->name);
        StringCchCopy(n2->name, lstrlen(context_handle->nnames.sServerName) + 1,
                context_handle->nnames.sServerName);
        *targ_name = (gss_name_t) n2;
    }
    if (lifetime_rec != NULL) {
        SecPkgContext_Lifespan ls;
        SECURITY_STATUS ss;
        ss = QueryContextAttributes(
                (PCtxtHandle)&context_handle->hCtxt,
                SECPKG_ATTR_LIFESPAN,
                &ls);
        if (!SEC_SUCCESS(ss)) {
            goto err;
        }
        *lifetime_rec = seconds_until(0, &ls.tsExpiry);
    }
    if (mech_type != NULL) {
        *mech_type = context_handle->isSPNEGO
                ? &SPNEGO_OID : &KRB5_OID;
    }
    if (ctx_flags != NULL) {
        *ctx_flags = context_handle->flags;
    }
    if (locally_initiated != NULL) {
        // We are always initiator
        *locally_initiated = 1;
    }
    return GSS_S_COMPLETE;
err:
    if (n1 != NULL) {
        if (n1->name != NULL) {
            delete[] n1->name;
        }
        delete n1;
        n1 = NULL;
    }
    if (n2 != NULL) {
        if (n2->name != NULL) {
            delete[] n2->name;
        }
        delete n2;
        n2 = NULL;
    }
    return GSS_S_FAILURE;
}

__declspec(dllexport) OM_uint32
gss_delete_sec_context(OM_uint32 *minor_status,
                       gss_ctx_id_t *context_handle,
                       gss_buffer_t output_token)
{
    PP(">>>> Calling gss_delete_sec_context...");
    CHECK_CONTEXT(context_handle)

    DeleteSecurityContext(&(*context_handle)->hCtxt);
    if ((*context_handle)->isLocalCred && (*context_handle)->phCred != NULL) {
        FreeCredentialsHandle((*context_handle)->phCred);
        (*context_handle)->phCred = NULL;
    }
    if ((*context_handle)->nnames.sClientName != NULL) {
        FreeContextBuffer((*context_handle)->nnames.sClientName);
        (*context_handle)->nnames.sClientName = NULL;
    }
    if ((*context_handle)->nnames.sServerName != NULL) {
        FreeContextBuffer((*context_handle)->nnames.sServerName);
        (*context_handle)->nnames.sServerName = NULL;
    }
    delete (*context_handle);
    *context_handle = GSS_C_NO_CONTEXT;
    return GSS_S_COMPLETE;
}

__declspec(dllexport) OM_uint32
gss_context_time(OM_uint32 *minor_status,
                 gss_const_ctx_id_t context_handle,
                 OM_uint32 *time_rec)
{
    PP(">>>> Calling IMPLEMENTED gss_context_time...");
    CHECK_CONTEXT(context_handle)
    CHECK_OUTPUT(time_rec)

    SECURITY_STATUS ss;
    SecPkgContext_Lifespan ls;
    ss = QueryContextAttributes(
            (PCtxtHandle)&context_handle->hCtxt,
            SECPKG_ATTR_LIFESPAN,
            &ls);
    if (ss == SEC_E_OK) {
        *time_rec = seconds_until(0, &ls.tsExpiry);
        show_time("context start", &ls.tsStart);
        show_time("context expiry", &ls.tsExpiry);
        return *time_rec == 0 ? GSS_S_CONTEXT_EXPIRED : GSS_S_COMPLETE;
    } else {
        return GSS_S_FAILURE;
    }
}

__declspec(dllexport) OM_uint32
gss_wrap_size_limit(OM_uint32 *minor_status,
                    gss_const_ctx_id_t context_handle,
                    int conf_req_flag,
                    gss_qop_t qop_req,
                    OM_uint32 req_output_size,
                    OM_uint32 *max_input_size)
{
    PP(">>>> Calling gss_wrap_size_limit...");
    CHECK_CONTEXT(context_handle)
    CHECK_OUTPUT(max_input_size)

    *max_input_size = req_output_size
            - context_handle->SecPkgContextSizes.cbSecurityTrailer
            - context_handle->SecPkgContextSizes.cbBlockSize;
    return GSS_S_COMPLETE;
}

__declspec(dllexport) OM_uint32
gss_export_sec_context(OM_uint32 *minor_status,
                       gss_ctx_id_t *context_handle,
                       gss_buffer_t interprocess_token)
{
    PP(">>>> Calling UNIMPLEMENTED gss_export_sec_context...");
    return GSS_S_FAILURE;
}

__declspec(dllexport) OM_uint32
gss_get_mic(OM_uint32 *minor_status,
            gss_const_ctx_id_t context_handle,
            gss_qop_t qop_req,
            gss_const_buffer_t message_buffer,
            gss_buffer_t msg_token)
{
    PP(">>>> Calling gss_get_mic...");
    CHECK_CONTEXT(context_handle)
    CHECK_BUFFER(message_buffer)
    CHECK_OUTPUT(msg_token)

    SECURITY_STATUS ss;
    SecBufferDesc buffDesc;
    SecBuffer secBuff[2];

    buffDesc.cBuffers = 2;
    buffDesc.pBuffers = secBuff;
    buffDesc.ulVersion = SECBUFFER_VERSION;

    secBuff[0].BufferType = SECBUFFER_DATA;
    secBuff[0].cbBuffer = (ULONG)message_buffer->length;
    secBuff[0].pvBuffer = message_buffer->value;

    secBuff[1].BufferType = SECBUFFER_TOKEN;
    secBuff[1].cbBuffer = context_handle->SecPkgContextSizes.cbMaxSignature;
    secBuff[1].pvBuffer = msg_token->value = new char[secBuff[1].cbBuffer];

    if (!secBuff[1].pvBuffer) {
        goto err;
    }

    ss = MakeSignature((PCtxtHandle)&context_handle->hCtxt, 0, &buffDesc, 0);

    if (!SEC_SUCCESS(ss)) {
        goto err;
    }

    msg_token->length = secBuff[1].cbBuffer;
    return GSS_S_COMPLETE;

err:
    msg_token->length = 0;
    msg_token->value = NULL;
    if (secBuff[1].pvBuffer) {
        delete[] secBuff[1].pvBuffer;
    }
    return GSS_S_FAILURE;
}

__declspec(dllexport) OM_uint32
gss_verify_mic(OM_uint32 *minor_status,
               gss_const_ctx_id_t context_handle,
               gss_const_buffer_t message_buffer,
               gss_const_buffer_t token_buffer,
               gss_qop_t *qop_state)
{
    PP(">>>> Calling gss_verify_mic...");
    CHECK_CONTEXT(context_handle)
    CHECK_BUFFER(message_buffer)
    CHECK_BUFFER(token_buffer)

    SECURITY_STATUS ss;
    SecBufferDesc buffDesc;
    SecBuffer secBuff[2];
    ULONG qop;

    buffDesc.ulVersion = SECBUFFER_VERSION;
    buffDesc.cBuffers = 2;
    buffDesc.pBuffers = secBuff;

    secBuff[0].BufferType = SECBUFFER_TOKEN;
    secBuff[0].cbBuffer = (ULONG)token_buffer->length;
    secBuff[0].pvBuffer = token_buffer->value;

    secBuff[1].BufferType = SECBUFFER_DATA;
    secBuff[1].cbBuffer = (ULONG)message_buffer->length;
    secBuff[1].pvBuffer = message_buffer->value;

    ss = VerifySignature((PCtxtHandle)&context_handle->hCtxt, &buffDesc, 0, &qop);
    if (qop_state) {
        *qop_state = qop;
    }

    if (ss == SEC_E_OK) {
        return GSS_S_COMPLETE;
    } else if (ss == SEC_E_OUT_OF_SEQUENCE) {
        return GSS_S_UNSEQ_TOKEN;
    } else {
        return GSS_S_BAD_SIG;
    }
}

__declspec(dllexport) OM_uint32
gss_wrap(OM_uint32 *minor_status,
         gss_const_ctx_id_t context_handle,
         int conf_req_flag,
         gss_qop_t qop_req,
         gss_const_buffer_t input_message_buffer,
         int *conf_state,
         gss_buffer_t output_message_buffer)
{
    PP(">>>> Calling gss_wrap...");
    CHECK_CONTEXT(context_handle)
    CHECK_BUFFER(input_message_buffer)
    CHECK_OUTPUT(output_message_buffer)

    SECURITY_STATUS ss;
    SecBufferDesc buffDesc;
    SecBuffer secBuff[3];

    buffDesc.ulVersion = SECBUFFER_VERSION;
    buffDesc.cBuffers = 3;
    buffDesc.pBuffers = secBuff;

    secBuff[0].BufferType = SECBUFFER_TOKEN;
    secBuff[0].cbBuffer = context_handle->SecPkgContextSizes.cbSecurityTrailer;
    output_message_buffer->value = secBuff[0].pvBuffer = malloc(
            context_handle->SecPkgContextSizes.cbSecurityTrailer
                    + input_message_buffer->length
                    + context_handle->SecPkgContextSizes.cbBlockSize);;
    if (!output_message_buffer->value) {
        goto err;
    }

    secBuff[1].BufferType = SECBUFFER_DATA;
    secBuff[1].cbBuffer = (ULONG)input_message_buffer->length;
    secBuff[1].pvBuffer = malloc(secBuff[1].cbBuffer);
    if (!secBuff[1].pvBuffer) {
        goto err;
    }
    memcpy_s(secBuff[1].pvBuffer, secBuff[1].cbBuffer,
            input_message_buffer->value, input_message_buffer->length);

    secBuff[2].BufferType = SECBUFFER_PADDING;
    secBuff[2].cbBuffer = context_handle->SecPkgContextSizes.cbBlockSize;
    secBuff[2].pvBuffer = malloc(secBuff[2].cbBuffer);
    if (!secBuff[2].pvBuffer) {
        goto err;
    }

    ss = EncryptMessage((PCtxtHandle)&context_handle->hCtxt,
            conf_req_flag ? 0 : SECQOP_WRAP_NO_ENCRYPT,
            &buffDesc, 0);
    if (conf_state) {
        *conf_state = conf_req_flag;
    }

    if (!SEC_SUCCESS(ss)) {
        goto err;
    }

    memcpy_s((PBYTE)secBuff[0].pvBuffer + secBuff[0].cbBuffer,
            input_message_buffer->length + context_handle->SecPkgContextSizes.cbBlockSize,
            secBuff[1].pvBuffer,
            secBuff[1].cbBuffer);
    memcpy_s((PBYTE)secBuff[0].pvBuffer + secBuff[0].cbBuffer + secBuff[1].cbBuffer,
            context_handle->SecPkgContextSizes.cbBlockSize,
            secBuff[2].pvBuffer,
            secBuff[2].cbBuffer);

    output_message_buffer->length = secBuff[0].cbBuffer + secBuff[1].cbBuffer
            + secBuff[2].cbBuffer;
    free(secBuff[1].pvBuffer);
    free(secBuff[2].pvBuffer);

    return GSS_S_COMPLETE;

err:
    if (secBuff[0].pvBuffer) {
        free(secBuff[0].pvBuffer);
    }
    if (secBuff[1].pvBuffer) {
        free(secBuff[1].pvBuffer);
    }
    if (secBuff[2].pvBuffer) {
        free(secBuff[2].pvBuffer);
    }
    output_message_buffer->length = 0;
    output_message_buffer->value = NULL;
    return GSS_S_FAILURE;
}

__declspec(dllexport) OM_uint32
gss_unwrap(OM_uint32 *minor_status,
           gss_const_ctx_id_t context_handle,
           gss_const_buffer_t input_message_buffer,
           gss_buffer_t output_message_buffer,
           int *conf_state,
           gss_qop_t *qop_state)
{
    PP(">>>> Calling gss_unwrap...");
    CHECK_CONTEXT(context_handle)
    CHECK_BUFFER(input_message_buffer)
    CHECK_OUTPUT(output_message_buffer)

    SECURITY_STATUS ss;
    SecBufferDesc buffDesc;
    SecBuffer secBuff[2];
    ULONG ulQop = 0;

    buffDesc.cBuffers = 2;
    buffDesc.pBuffers = secBuff;
    buffDesc.ulVersion = SECBUFFER_VERSION;

    secBuff[0].BufferType = SECBUFFER_STREAM;
    secBuff[0].cbBuffer = (ULONG)input_message_buffer->length;
    secBuff[0].pvBuffer = malloc(input_message_buffer->length);

    if (!secBuff[0].pvBuffer) {
        goto err;
    }

    memcpy_s(secBuff[0].pvBuffer, input_message_buffer->length,
            input_message_buffer->value, input_message_buffer->length);

    secBuff[1].BufferType = SECBUFFER_DATA;
    secBuff[1].cbBuffer = 0;
    secBuff[1].pvBuffer = NULL;

    ss = DecryptMessage((PCtxtHandle)&context_handle->hCtxt, &buffDesc, 0, &ulQop);
    if (qop_state) {
        *qop_state = ulQop;
    }
    if (!SEC_SUCCESS(ss)) {
        goto err;
    }

    // Must allocate a new memory block so client can release it correctly
    output_message_buffer->length = secBuff[1].cbBuffer;
    output_message_buffer->value = new char[secBuff[1].cbBuffer];

    if (!output_message_buffer->value) {
        goto err;
    }

    memcpy_s(output_message_buffer->value, secBuff[1].cbBuffer,
            secBuff[1].pvBuffer, secBuff[1].cbBuffer);
    *conf_state = ulQop == SECQOP_WRAP_NO_ENCRYPT ? 0 : 1;

    free(secBuff[0].pvBuffer);
    return GSS_S_COMPLETE;

err:
    if (secBuff[0].pvBuffer) {
        free(secBuff[0].pvBuffer);
    }
    output_message_buffer->length = 0;
    output_message_buffer->value = NULL;
    return GSS_S_FAILURE;
}

__declspec(dllexport) OM_uint32
gss_indicate_mechs(OM_uint32 *minor_status,
                   gss_OID_set *mech_set)
{
    PP(">>>> Calling gss_indicate_mechs...");
    OM_uint32 major = GSS_S_COMPLETE;

    ULONG ccPackages;
    PSecPkgInfo packages;
    EnumerateSecurityPackages(&ccPackages, &packages);
    PP("EnumerateSecurityPackages returns %ld", ccPackages);
    for (unsigned int i = 0; i < ccPackages; i++) {
        PP("#%d: %ls, %ls\n", i, packages[i].Name, packages[i].Comment);
    }
    FreeContextBuffer(packages);

    // Hardcode kerberos and SPNEGO support
    major = gss_create_empty_oid_set(minor_status, mech_set);
    if (major != GSS_S_COMPLETE) {
        goto done;
    }

    major = gss_add_oid_set_member(minor_status, &KRB5_OID, mech_set);
    if (major != GSS_S_COMPLETE) {
        goto done;
    }

    major = gss_add_oid_set_member(minor_status, &SPNEGO_OID, mech_set);
    if (major != GSS_S_COMPLETE) {
        goto done;
    }

done:

    if (major != GSS_S_COMPLETE) {
        gss_release_oid_set(minor_status, mech_set);
    }

    return major;
}

__declspec(dllexport) OM_uint32
gss_inquire_names_for_mech(OM_uint32 *minor_status,
                           gss_const_OID mechanism,
                           gss_OID_set *name_types)
{
    PP(">>>> Calling gss_inquire_names_for_mech...");
    CHECK_OID(mechanism)

    if (gss_create_empty_oid_set(minor_status, name_types)) {
        return GSS_S_FAILURE;
    }
    if (gss_add_oid_set_member(minor_status, &USER_NAME_OID, name_types)) {
        goto err;
    }
    if (gss_add_oid_set_member(minor_status, &HOST_SERVICE_NAME_OID, name_types)) {
        goto err;
    }
    if (!is_same_oid(mechanism, &SPNEGO_OID)) {
        if (gss_add_oid_set_member(minor_status, &EXPORT_NAME_OID, name_types)) {
            goto err;
        }
    }
    return GSS_S_COMPLETE;
err:
    gss_release_oid_set(minor_status, name_types);
    return GSS_S_FAILURE;
}

__declspec(dllexport) OM_uint32
gss_add_oid_set_member(OM_uint32 *minor_status,
                       gss_const_OID member_oid,
                       gss_OID_set *oid_set)
{
    PP(">>>> Calling gss_add_oid_set_member...");
    CHECK_OID(member_oid)
    CHECK_OUTPUT(oid_set)


    int count = (int)(*oid_set)->count;
    for (int i = 0; i < count; i++) {
        if (is_same_oid(&(*oid_set)->elements[i], member_oid)) {
            // already there
            return GSS_S_COMPLETE;
        }
    }
    gss_OID existing = (*oid_set)->elements;
    gss_OID newcopy = new gss_OID_desc[count + 1];
    if (newcopy == NULL) {
        return GSS_S_FAILURE;
    }
    if (existing) {
        memcpy_s(newcopy, (count + 1) * sizeof(gss_OID_desc),
                existing, count * sizeof(gss_OID_desc));
    }
    newcopy[count].length = member_oid->length;
    newcopy[count].elements = new char[member_oid->length];
    if (newcopy[count].elements == NULL) {
        delete[] newcopy;
        return GSS_S_FAILURE;
    }
    memcpy_s(newcopy[count].elements, member_oid->length,
            member_oid->elements, member_oid->length);
    (*oid_set)->elements = newcopy;
    (*oid_set)->count++;
    if (existing) {
        delete[] existing;
    }

    return GSS_S_COMPLETE;
}

__declspec(dllexport) OM_uint32
gss_display_status(OM_uint32 *minor_status,
                   OM_uint32 status_value,
                   int status_type,
                   gss_const_OID mech_type,
                   OM_uint32 *message_context,
                   gss_buffer_t status_string)
{
    PP(">>>> Calling gss_display_status...");
    TCHAR msg[256];
    int len = FormatMessage(
            FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
            0, status_value,
            MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
            msg, 256, 0);
    if (len > 0) {
        status_string->value = new char[len + 20];
        if (!status_string->value) {
            status_string = GSS_C_NO_BUFFER;
            return GSS_S_FAILURE;
        }
        status_string->length = sprintf_s(
                (LPSTR)status_string->value, len + 19,
                "(%lx) %ls", status_value, msg);
    } else {
        status_string->value = new char[33];
        if (!status_string->value) {
            status_string = GSS_C_NO_BUFFER;
            return GSS_S_FAILURE;
        }
        status_string->length = sprintf_s(
                (LPSTR)status_string->value, 32,
                "status is %lx", status_value);
    }
    if (status_string->length <= 0) {
        gss_release_buffer(NULL, status_string);
        status_string = GSS_C_NO_BUFFER;
        return GSS_S_FAILURE;
    } else {
        return GSS_S_COMPLETE;
    }
}

__declspec(dllexport) OM_uint32
gss_create_empty_oid_set(OM_uint32 *minor_status,
                         gss_OID_set *oid_set)
{
    PP(">>>> Calling gss_create_empty_oid_set...");
    CHECK_OUTPUT(oid_set)

    if (*oid_set = new gss_OID_set_desc) {
        memset(*oid_set, 0, sizeof(gss_OID_set_desc));
        return GSS_S_COMPLETE;
    }
    return GSS_S_FAILURE;
}

__declspec(dllexport) OM_uint32
gss_release_oid_set(OM_uint32 *minor_status,
                    gss_OID_set *set)
{
    PP(">>>> Calling gss_release_oid_set...");
    if (set == NULL || *set == GSS_C_NO_OID_SET) {
        return GSS_S_COMPLETE;
    }
    for (size_t i = 0; i < (*set)->count; i++) {
        delete[] (*set)->elements[i].elements;
    }
    delete[] (*set)->elements;
    delete *set;
    *set = GSS_C_NO_OID_SET;
    return GSS_S_COMPLETE;
}

__declspec(dllexport) OM_uint32
gss_release_buffer(OM_uint32 *minor_status,
                   gss_buffer_t buffer)
{
    PP(">>>> Calling gss_release_buffer...");
    if (buffer == NULL || buffer == GSS_C_NO_BUFFER) {
        return GSS_S_COMPLETE;
    }
    if (buffer->value) {
        delete[] buffer->value;
        buffer->value = NULL;
    }
    buffer->length = 0;
    return GSS_S_COMPLETE;
}

/* End implemented section */

#ifdef __cplusplus
}
#endif
