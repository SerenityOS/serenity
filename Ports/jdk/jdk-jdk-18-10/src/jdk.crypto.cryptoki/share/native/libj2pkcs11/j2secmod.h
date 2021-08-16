/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>

#include "sun_security_pkcs11_Secmod.h"

// #define SECMOD_DEBUG

#include "j2secmod_md.h"

#include "p11_md.h"


void *findFunction(JNIEnv *env, jlong jHandle, const char *functionName);

#ifdef SECMOD_DEBUG
#define dprintf(s) printf(s)
#define dprintf1(s, p1) printf(s, p1)
#define dprintf2(s, p1, p2) printf(s, p1, p2)
#define dprintf3(s, p1, p2, p3) printf(s, p1, p2, p3)
#else
#define dprintf(s)
#define dprintf1(s, p1)
#define dprintf2(s, p1, p2)
#define dprintf3(s, p1, p2, p3)
#endif

// NSS types

typedef int PRBool;
typedef unsigned short PRUint16;
typedef short PRInt16;
typedef unsigned int PRUint32;
typedef int PRInt32;
typedef long long PRInt64;

typedef PRUint32 PRIntervalTime;
typedef PRInt64 PRTime;

typedef struct PK11SlotInfoStr PK11SlotInfo;

typedef struct SECMODModuleStr SECMODModule;
typedef struct SECMODModuleListStr SECMODModuleList;

// Defined in NSS's secmodt.h header
/* PKCS #11 disable reasons */
typedef enum {
    PK11_DIS_NONE = 0,
    PK11_DIS_USER_SELECTED = 1,
    PK11_DIS_COULD_NOT_INIT_TOKEN = 2,
    PK11_DIS_TOKEN_VERIFY_FAILED = 3,
    PK11_DIS_TOKEN_NOT_PRESENT = 4
} PK11DisableReasons;

// Slot IDs - defined in Secmod.java on the Java side
// Values obtained from NSS's pkcs11i.h header
#define NETSCAPE_SLOT_ID 1
#define PRIVATE_KEY_SLOT_ID 2
#define FIPS_SLOT_ID 3

// Defined in NSS's secmodti.h header
/* represent a pkcs#11 slot reference counted. */
struct PK11SlotInfoStr {
    /* the PKCS11 function list for this slot */
    void *functionList;
    SECMODModule *module; /* our parent module */
    /* Boolean to indicate the current state of this slot */
    PRBool needTest;           /* Has this slot been tested for Export complience */
    PRBool isPerm;             /* is this slot a permanment device */
    PRBool isHW;               /* is this slot a hardware device */
    PRBool isInternal;         /* is this slot one of our internal PKCS #11 devices */
    PRBool disabled;           /* is this slot disabled... */
    PK11DisableReasons reason; /* Why this slot is disabled */
    PRBool readOnly;           /* is the token in this slot read-only */
    PRBool needLogin;          /* does the token of the type that needs
                                * authentication (still true even if token is logged
                                * in) */
    PRBool hasRandom;          /* can this token generated random numbers */
    PRBool defRWSession;       /* is the default session RW (we open our default
                                * session rw if the token can only handle one session
                                * at a time. */
    PRBool isThreadSafe;       /* copied from the module */
    /* The actual flags (many of which are distilled into the above PRBools) */
    CK_FLAGS flags; /* flags from PKCS #11 token Info */
    /* a default session handle to do quick and dirty functions */
    CK_SESSION_HANDLE session;
    void *sessionLock; /* lock for this session */
    /* our ID */
    CK_SLOT_ID slotID;
    /* persistant flags saved from startup to startup */
    unsigned long defaultFlags;
    /* keep track of who is using us so we don't accidently get freed while
     * still in use */
    PRInt32 refCount; /* to be in/decremented by atomic calls ONLY! */
    void *freeListLock;
    void *freeSymKeysWithSessionHead;
    void *freeSymKeysHead;
    int keyCount;
    int maxKeyCount;
    /* Password control functions for this slot. many of these are only
     * active if the appropriate flag is on in defaultFlags */
    int askpw;           /* what our password options are */
    int timeout;         /* If we're ask_timeout, what is our timeout time is
                          * seconds */
    int authTransact;    /* allow multiple authentications off one password if
                          * they are all part of the same transaction */
    PRTime authTime;     /* when were we last authenticated */
    int minPassword;     /* smallest legal password */
    int maxPassword;     /* largest legal password */
    PRUint16 series;     /* break up the slot info into various groups of
                          * inserted tokens so that keys and certs can be
                          * invalidated */
    PRUint16 flagSeries; /* record the last series for the last event
                          * returned for this slot */
    PRBool flagState;    /* record the state of the last event returned for this
                          * slot. */
    PRUint16 wrapKey;    /* current wrapping key for SSL master secrets */
    CK_MECHANISM_TYPE wrapMechanism;
    /* current wrapping mechanism for current wrapKey */
    CK_OBJECT_HANDLE refKeys[1];      /* array of existing wrapping keys for */
    CK_MECHANISM_TYPE *mechanismList; /* list of mechanism supported by this
                                       * token */
    int mechanismCount;
    /* cache the certificates stored on the token of this slot */
    void **cert_array;
    int array_size;
    int cert_count;
    char serial[16];
    /* since these are odd sizes, keep them last. They are odd sizes to
     * allow them to become null terminated strings */
    char slot_name[65];
    char token_name[33];
    PRBool hasRootCerts;
    PRBool hasRootTrust;
    PRBool hasRSAInfo;
    CK_FLAGS RSAInfoFlags;
    PRBool protectedAuthPath;
    PRBool isActiveCard;
    PRIntervalTime lastLoginCheck;
    unsigned int lastState;
    /* for Stan */
    void *nssToken;
    /* the tokeninfo struct */
    CK_TOKEN_INFO tokenInfo;
    /* fast mechanism lookup */
    char mechanismBits[256];
    CK_PROFILE_ID *profileList;
    int profileCount;
};

// Defined in NSS's secmodt.h header
struct SECMODModuleStr {
    void         *v1;
    PRBool       internal;       /* true of internally linked modules, false
                                  * for the loaded modules */
    PRBool       loaded;         /* Set to true if module has been loaded */
    PRBool       isFIPS;         /* Set to true if module is finst internal */
    char         *dllName;       /* name of the shared library which implements
                                  * this module */
    char         *commonName;    /* name of the module to display to the user */
    void         *library;       /* pointer to the library. opaque. used only by
                                  * pk11load.c */

    void         *functionList; /* The PKCS #11 function table */
    void         *refLock;       /* only used pk11db.c */
    int          refCount;       /* Module reference count */
    PK11SlotInfo **slots;        /* array of slot points attached to this mod*/
    int          slotCount;      /* count of slot in above array */
    void         *slotInfo;      /* special info about slots default settings */
    int          slotInfoCount;  /* count */
    // incomplete, sizeof() is wrong
};

// Defined in NSS's secmodt.h header
struct SECMODModuleListStr {
    SECMODModuleList    *next;
    SECMODModule        *module;
};
