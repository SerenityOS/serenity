/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef NATIVE_FUNC_H
#define NATIVE_FUNC_H

#include "gssapi.h"

#ifdef WIN32
#include <windows.h>
#define GETLIB(libName) LoadLibrary(libName)
#define GETFUNC(lib,name) GetProcAddress(lib,name)
#define CLOSELIB(lib) CloseHandle(lib)
#else
#include <dlfcn.h>
#define GETLIB(libName) dlopen(libName, RTLD_NOW)
#define GETFUNC(lib,name) dlsym(lib,name)
#define CLOSELIB(lib) dlclose(lib)
#endif

#ifndef TRUE
#define TRUE    1
#endif

#ifndef FALSE
#define FALSE   0
#endif

int loadNative(const char *libName);

/* function pointer definitions */
typedef OM_uint32 (*RELEASE_NAME_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_name_t *name);

typedef OM_uint32 (*IMPORT_NAME_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_buffer_t input_name_buffer,
                                gss_const_OID input_name_type,
                                gss_name_t *output_name);

typedef OM_uint32 (*COMPARE_NAME_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_name_t name1,
                                gss_const_name_t name2,
                                int *name_equal);

typedef OM_uint32 (*CANONICALIZE_NAME_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_name_t input_name,
                                gss_const_OID mech_type,
                                gss_name_t *output_name);

typedef OM_uint32 (*EXPORT_NAME_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_name_t input_name,
                                gss_buffer_t exported_name);

typedef OM_uint32 (*DISPLAY_NAME_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_name_t input_name,
                                gss_buffer_t output_name_buffer,
                                gss_OID *output_name_type);

typedef OM_uint32 (*ACQUIRE_CRED_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_name_t desired_name,
                                OM_uint32 time_req,
                                gss_const_OID_set desired_mech,
                                gss_cred_usage_t cred_usage,
                                gss_cred_id_t *output_cred_handle,
                                gss_OID_set *actual_mechs,
                                OM_uint32 *time_rec);

typedef OM_uint32 (*RELEASE_CRED_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_cred_id_t *cred_handle);

typedef OM_uint32 (*INQUIRE_CRED_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_cred_id_t cred_handle,
                                gss_name_t *name,
                                OM_uint32 *lifetime,
                                gss_cred_usage_t *cred_usage,
                                gss_OID_set *mechanisms);

typedef OM_uint32 (*IMPORT_SEC_CONTEXT_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_buffer_t interprocess_token,
                                gss_ctx_id_t *context_handle);

typedef OM_uint32 (*INIT_SEC_CONTEXT_FN_PTR)
                                (OM_uint32 *minor_status,
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
                                OM_uint32 *time_rec);

typedef OM_uint32 (*ACCEPT_SEC_CONTEXT_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_ctx_id_t *context_handle,
                                gss_const_cred_id_t acceptor_cred_handle,
                                gss_const_buffer_t input_token,
                                gss_const_channel_bindings_t input_chan_bindings,
                                gss_name_t *src_name,
                                gss_OID *mech_type,
                                gss_buffer_t output_token,
                                OM_uint32 *ret_flags,
                                OM_uint32 *time_rec,
                                gss_cred_id_t *delegated_cred_handle);

typedef OM_uint32 (*INQUIRE_CONTEXT_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_ctx_id_t context_handle,
                                gss_name_t *src_name,
                                gss_name_t *targ_name,
                                OM_uint32 *lifetime_rec,
                                gss_OID *mech_type,
                                OM_uint32 *ctx_flags,
                                int *locally_initiated,
                                int *open);

typedef OM_uint32 (*DELETE_SEC_CONTEXT_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_ctx_id_t *context_handle,
                                gss_buffer_t output_token);

typedef OM_uint32 (*CONTEXT_TIME_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_ctx_id_t context_handle,
                                OM_uint32 *time_rec);

typedef OM_uint32 (*WRAP_SIZE_LIMIT_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_ctx_id_t context_handle,
                                int conf_req_flag,
                                gss_qop_t qop_req,
                                OM_uint32 req_output_size,
                                OM_uint32 *max_input_size);

typedef OM_uint32 (*EXPORT_SEC_CONTEXT_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_ctx_id_t *context_handle,
                                gss_buffer_t interprocess_token);

typedef OM_uint32 (*GET_MIC_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_ctx_id_t context_handle,
                                gss_qop_t qop_req,
                                gss_const_buffer_t message_buffer,
                                gss_buffer_t msg_token);

typedef OM_uint32 (*VERIFY_MIC_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_ctx_id_t context_handle,
                                gss_const_buffer_t message_buffer,
                                gss_const_buffer_t token_buffer,
                                gss_qop_t *qop_state);

typedef OM_uint32 (*WRAP_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_ctx_id_t context_handle,
                                int conf_req_flag,
                                gss_qop_t qop_req,
                                gss_const_buffer_t input_message_buffer,
                                int *conf_state,
                                gss_buffer_t output_message_buffer);

typedef OM_uint32 (*UNWRAP_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_ctx_id_t context_handle,
                                gss_const_buffer_t input_message_buffer,
                                gss_buffer_t output_message_buffer,
                                int *conf_state,
                                gss_qop_t *qop_state);

typedef OM_uint32 (*INDICATE_MECHS_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_OID_set *mech_set);

typedef OM_uint32 (*INQUIRE_NAMES_FOR_MECH_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_OID mechanism,
                                gss_OID_set *name_types);

typedef OM_uint32 (*ADD_OID_SET_MEMBER_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_const_OID member_oid,
                                gss_OID_set *oid_set);

typedef OM_uint32 (*DISPLAY_STATUS_FN_PTR)
                                (OM_uint32 *minor_status,
                                OM_uint32 status_value,
                                int status_type,
                                gss_const_OID mech_type,
                                OM_uint32 *message_context,
                                gss_buffer_t status_string);

typedef OM_uint32 (*CREATE_EMPTY_OID_SET_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_OID_set *oid_set);

typedef OM_uint32 (*RELEASE_OID_SET_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_OID_set *set);

typedef OM_uint32 (*RELEASE_BUFFER_FN_PTR)
                                (OM_uint32 *minor_status,
                                gss_buffer_t buffer);


/* dynamically resolved functions from gss library */

typedef struct GSS_FUNCTION_TABLE {
    gss_OID_set                         mechs;
    RELEASE_NAME_FN_PTR                 releaseName;
    IMPORT_NAME_FN_PTR                  importName;
    COMPARE_NAME_FN_PTR                 compareName;
    CANONICALIZE_NAME_FN_PTR            canonicalizeName;
    EXPORT_NAME_FN_PTR                  exportName;
    DISPLAY_NAME_FN_PTR                 displayName;
    ACQUIRE_CRED_FN_PTR                 acquireCred;
    RELEASE_CRED_FN_PTR                 releaseCred;
    INQUIRE_CRED_FN_PTR                 inquireCred;
    IMPORT_SEC_CONTEXT_FN_PTR           importSecContext;
    INIT_SEC_CONTEXT_FN_PTR             initSecContext;
    ACCEPT_SEC_CONTEXT_FN_PTR           acceptSecContext;
    INQUIRE_CONTEXT_FN_PTR              inquireContext;
    DELETE_SEC_CONTEXT_FN_PTR           deleteSecContext;
    CONTEXT_TIME_FN_PTR                 contextTime;
    WRAP_SIZE_LIMIT_FN_PTR              wrapSizeLimit;
    EXPORT_SEC_CONTEXT_FN_PTR           exportSecContext;
    GET_MIC_FN_PTR                      getMic;
    VERIFY_MIC_FN_PTR                   verifyMic;
    WRAP_FN_PTR                         wrap;
    UNWRAP_FN_PTR                       unwrap;
    INDICATE_MECHS_FN_PTR               indicateMechs;
    INQUIRE_NAMES_FOR_MECH_FN_PTR       inquireNamesForMech;
    ADD_OID_SET_MEMBER_FN_PTR           addOidSetMember;
    DISPLAY_STATUS_FN_PTR               displayStatus;
    CREATE_EMPTY_OID_SET_FN_PTR         createEmptyOidSet;
    RELEASE_OID_SET_FN_PTR              releaseOidSet;
    RELEASE_BUFFER_FN_PTR               releaseBuffer;

} GSS_FUNCTION_TABLE;

typedef GSS_FUNCTION_TABLE *GSS_FUNCTION_TABLE_PTR;

/* global GSS function table */
extern GSS_FUNCTION_TABLE_PTR ftab;

#endif
