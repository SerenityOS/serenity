/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>

#include "Sctp.h"
#include "jni.h"
#include "jni_util.h"
#include "nio_util.h"
#include "nio.h"
#include "net_util.h"
#include "net_util_md.h"
#include "sun_nio_ch_sctp_SctpNet.h"
#include "sun_nio_ch_sctp_SctpStdSocketOption.h"

static jclass isaCls = 0;
static jmethodID isaCtrID = 0;

static const char* nativeSctpLib = "libsctp.so.1";
static jboolean funcsLoaded = JNI_FALSE;

sctp_getladdrs_func* nio_sctp_getladdrs;
sctp_freeladdrs_func* nio_sctp_freeladdrs;
sctp_getpaddrs_func* nio_sctp_getpaddrs;
sctp_freepaddrs_func* nio_sctp_freepaddrs;
sctp_bindx_func* nio_sctp_bindx;
sctp_peeloff_func* nio_sctp_peeloff;

JNIEXPORT jint JNICALL DEF_JNI_OnLoad
  (JavaVM *vm, void *reserved) {
    return JNI_VERSION_1_2;
}

static int preCloseFD = -1;     /* File descriptor to which we dup other fd's
                                   before closing them for real */

/**
 * Loads the native sctp library that contains the socket extension
 * functions, as well as locating the individual functions.
 * There will be a pending exception if this method returns false.
 */
jboolean loadSocketExtensionFuncs
  (JNIEnv* env) {
    if (dlopen(nativeSctpLib, RTLD_GLOBAL | RTLD_LAZY) == NULL) {
        JNU_ThrowByName(env, "java/lang/UnsupportedOperationException",
              dlerror());
        return JNI_FALSE;
    }

    if ((nio_sctp_getladdrs = (sctp_getladdrs_func*)
            dlsym(RTLD_DEFAULT, "sctp_getladdrs")) == NULL) {
        JNU_ThrowByName(env, "java/lang/UnsupportedOperationException",
              dlerror());
        return JNI_FALSE;
    }

    if ((nio_sctp_freeladdrs = (sctp_freeladdrs_func*)
            dlsym(RTLD_DEFAULT, "sctp_freeladdrs")) == NULL) {
        JNU_ThrowByName(env, "java/lang/UnsupportedOperationException",
              dlerror());
        return JNI_FALSE;
    }

    if ((nio_sctp_getpaddrs = (sctp_getpaddrs_func*)
            dlsym(RTLD_DEFAULT, "sctp_getpaddrs")) == NULL) {
        JNU_ThrowByName(env, "java/lang/UnsupportedOperationException",
              dlerror());
        return JNI_FALSE;
    }

    if ((nio_sctp_freepaddrs = (sctp_freepaddrs_func*)
            dlsym(RTLD_DEFAULT, "sctp_freepaddrs")) == NULL) {
        JNU_ThrowByName(env, "java/lang/UnsupportedOperationException",
              dlerror());
        return JNI_FALSE;
    }

    if ((nio_sctp_bindx = (sctp_bindx_func*)
            dlsym(RTLD_DEFAULT, "sctp_bindx")) == NULL) {
        JNU_ThrowByName(env, "java/lang/UnsupportedOperationException",
              dlerror());
        return JNI_FALSE;
    }

    if ((nio_sctp_peeloff = (sctp_peeloff_func*)
            dlsym(RTLD_DEFAULT, "sctp_peeloff")) == NULL) {
        JNU_ThrowByName(env, "java/lang/UnsupportedOperationException",
              dlerror());
        return JNI_FALSE;
    }

    funcsLoaded = JNI_TRUE;
    return JNI_TRUE;
}

jint
handleSocketError(JNIEnv *env, jint errorValue)
{
    char *xn;
    switch (errorValue) {
        case EINPROGRESS:     /* Non-blocking connect */
            return 0;
        case EPROTO:
            xn= JNU_JAVANETPKG "ProtocolException";
            break;
        case ECONNREFUSED:
            xn = JNU_JAVANETPKG "ConnectException";
            break;
        case ETIMEDOUT:
            xn = JNU_JAVANETPKG "ConnectException";
            break;
        case EHOSTUNREACH:
            xn = JNU_JAVANETPKG "NoRouteToHostException";
            break;
        case EADDRINUSE:  /* Fall through */
        case EADDRNOTAVAIL:
            xn = JNU_JAVANETPKG "BindException";
            break;
        default:
            xn = JNU_JAVANETPKG "SocketException";
            break;
    }
    errno = errorValue;
    JNU_ThrowByNameWithLastError(env, xn, "NioSocketError");
    return IOS_THROWN;
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_nio_ch_sctp_SctpNet_init
  (JNIEnv *env, jclass cl) {
    int sp[2];
    if (socketpair(PF_UNIX, SOCK_STREAM, 0, sp) < 0) {
        JNU_ThrowIOExceptionWithLastError(env, "socketpair failed");
        return;
    }
    preCloseFD = sp[0];
    close(sp[1]);
    initInetAddressIDs(env);
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    socket0
 * Signature: (Z)I
 */
JNIEXPORT jint JNICALL Java_sun_nio_ch_sctp_SctpNet_socket0
  (JNIEnv *env, jclass klass, jboolean oneToOne) {
    int fd;
    struct sctp_event_subscribe event;
#ifdef AF_INET6
    int domain = ipv6_available() ? AF_INET6 : AF_INET;
#else
    int domain = AF_INET;
#endif

    /* Try to load the socket API extension functions */
    if (!funcsLoaded && !loadSocketExtensionFuncs(env)) {
        return 0;
    }

    fd = socket(domain, (oneToOne ? SOCK_STREAM : SOCK_SEQPACKET), IPPROTO_SCTP);

    if (fd < 0) {
        if (errno == EPROTONOSUPPORT || errno == ESOCKTNOSUPPORT) {
            JNU_ThrowByNameWithLastError(env, "java/lang/UnsupportedOperationException",
                                         "Protocol not supported");
            return IOS_THROWN;
        } else {
            return handleSocketError(env, errno);
        }
    }

    /* Enable events */
    memset(&event, 0, sizeof(event));
    event.sctp_data_io_event = 1;
    event.sctp_association_event = 1;
    event.sctp_address_event = 1;
    event.sctp_send_failure_event = 1;
    //event.sctp_peer_error_event = 1;
    event.sctp_shutdown_event = 1;
    //event.sctp_partial_delivery_event = 1;
    //event.sctp_adaptation_layer_event = 1;
    if (setsockopt(fd, IPPROTO_SCTP, SCTP_EVENTS, &event, sizeof(event)) != 0) {
       handleSocketError(env, errno);
    }
    return fd;
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    bindx
 * Signature: (I[Ljava/net/InetAddress;IIZ)V
 */
JNIEXPORT void JNICALL Java_sun_nio_ch_sctp_SctpNet_bindx
  (JNIEnv *env, jclass klass, jint fd, jobjectArray addrs, jint port,
   jint addrsLength, jboolean add, jboolean preferIPv6) {
    SOCKETADDRESS *sap, *tmpSap;
    int i;
    jobject ia;

    if (addrsLength < 1)
        return;

    if ((sap = calloc(addrsLength, sizeof(SOCKETADDRESS))) == NULL) {
        JNU_ThrowOutOfMemoryError(env, "heap allocation failure");
        return;
    }

    tmpSap = sap;
    for (i = 0; i < addrsLength; i++) {
        ia = (*env)->GetObjectArrayElement(env, addrs, i);
        if (NET_InetAddressToSockaddr(env, ia, port, tmpSap, NULL,
                                      preferIPv6) != 0) {
            free(sap);
            return;
        }
        tmpSap++;
    }

    if (nio_sctp_bindx(fd, (void *)sap, addrsLength, add ? SCTP_BINDX_ADD_ADDR :
                       SCTP_BINDX_REM_ADDR) != 0) {
        handleSocketError(env, errno);
    }

    free(sap);
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    listen0
 * Signature: (II)V
 */
JNIEXPORT void JNICALL
Java_sun_nio_ch_sctp_SctpNet_listen0
  (JNIEnv *env, jclass cl, jint fd, jint backlog) {
    if (listen(fd, backlog) < 0)
        handleSocketError(env, errno);
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    connect0
 * Signature: (ILjava/net/InetAddress;I)I
 */
JNIEXPORT jint JNICALL
Java_sun_nio_ch_sctp_SctpNet_connect0
  (JNIEnv *env, jclass clazz, int fd, jobject iao, jint port) {
    SOCKETADDRESS sa;
    int sa_len = 0;
    int rv;

    if (NET_InetAddressToSockaddr(env, iao, port, &sa, &sa_len,
                                  JNI_TRUE) != 0) {
        return IOS_THROWN;
    }

    rv = connect(fd, &sa.sa, sa_len);
    if (rv != 0) {
        if (errno == EINPROGRESS) {
            return IOS_UNAVAILABLE;
        } else if (errno == EINTR) {
            return IOS_INTERRUPTED;
        }
        return handleSocketError(env, errno);
    }
    return 1;
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    close0
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_nio_ch_sctp_SctpNet_close0
  (JNIEnv *env, jclass clazz, jint fd) {
    if (fd != -1) {
        int rv = close(fd);
        if (rv < 0)
            JNU_ThrowIOExceptionWithLastError(env, "Close failed");
    }
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    preClose0
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_nio_ch_sctp_SctpNet_preClose0
  (JNIEnv *env, jclass clazz, jint fd) {
    if (preCloseFD >= 0) {
        if (dup2(preCloseFD, fd) < 0)
            JNU_ThrowIOExceptionWithLastError(env, "dup2 failed");
    }
}

void initializeISA(JNIEnv* env) {
    if (isaCls == 0) {
        jclass c = (*env)->FindClass(env, "java/net/InetSocketAddress");
        CHECK_NULL(c);
        isaCtrID = (*env)->GetMethodID(env, c, "<init>",
                                     "(Ljava/net/InetAddress;I)V");
        CHECK_NULL(isaCtrID);
        isaCls = (*env)->NewGlobalRef(env, c);
        CHECK_NULL(isaCls);
        (*env)->DeleteLocalRef(env, c);
    }
}

jobject SockAddrToInetSocketAddress(JNIEnv *env, SOCKETADDRESS *sap) {
    int port = 0;

    jobject ia = NET_SockaddrToInetAddress(env, sap, &port);
    if (ia == NULL)
        return NULL;

    if (isaCls == 0) {
        initializeISA(env);
        CHECK_NULL_RETURN(isaCls, NULL);
    }

    return (*env)->NewObject(env, isaCls, isaCtrID, ia, port);
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    getLocalAddresses0
 * Signature: (I)[Ljava/net/SocketAddress;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_nio_ch_sctp_SctpNet_getLocalAddresses0
  (JNIEnv *env, jclass klass, jint fd)
{
    void *addr_buf, *laddr;
    int i, addrCount;
    jobjectArray isaa;

    if ((addrCount = nio_sctp_getladdrs(fd, 0, (struct sockaddr **)&addr_buf)) == -1) {
        handleSocketError(env, errno);
        return NULL;
    }

    if (addrCount < 1)
        return NULL;

    if (isaCls == 0) {
        initializeISA(env);
        CHECK_NULL_RETURN(isaCls, NULL);
    }

    isaa = (*env)->NewObjectArray(env, addrCount, isaCls, NULL);
    if (isaa == NULL) {
        nio_sctp_freeladdrs(addr_buf);
        return NULL;
    }

    laddr = addr_buf;
    for (i = 0; i < addrCount; i++) {
        int port = 0;
        jobject ia, isa = NULL;
        ia = NET_SockaddrToInetAddress(env, (SOCKETADDRESS *)addr_buf, &port);
        if (ia != NULL)
            isa = (*env)->NewObject(env, isaCls, isaCtrID, ia, port);
        if (isa == NULL)
            break;
        (*env)->SetObjectArrayElement(env, isaa, i, isa);

        if (((struct sockaddr *)addr_buf)->sa_family == AF_INET)
            addr_buf = ((struct sockaddr_in *)addr_buf) + 1;
        else
            addr_buf = ((struct sockaddr_in6 *)addr_buf) + 1;
    }

    nio_sctp_freeladdrs(laddr);
    return isaa;
}

jobjectArray getRemoteAddresses(JNIEnv *env, jint fd, sctp_assoc_t id) {
    void *addr_buf, *paddr;
    int i, addrCount;
    jobjectArray isaa;

    if ((addrCount = nio_sctp_getpaddrs(fd, id, (struct sockaddr **)&addr_buf)) == -1) {
        handleSocketError(env, errno);
        return NULL;
    }

    if (addrCount < 1)
        return NULL;

    if (isaCls == 0) {
        initializeISA(env);
        CHECK_NULL_RETURN(isaCls, NULL);
    }

    isaa = (*env)->NewObjectArray(env, addrCount, isaCls, NULL);
    if (isaa == NULL) {
        nio_sctp_freepaddrs(addr_buf);
        return NULL;
    }

    paddr = addr_buf;
    for (i = 0; i < addrCount; i++) {
        int port = 0;
        jobject ia, isa = NULL;
        ia = NET_SockaddrToInetAddress(env, (SOCKETADDRESS *)addr_buf, &port);
        if (ia != NULL)
            isa = (*env)->NewObject(env, isaCls, isaCtrID, ia, port);
        if (isa == NULL)
            break;
        (*env)->SetObjectArrayElement(env, isaa, i, isa);

        if (((struct sockaddr *)addr_buf)->sa_family == AF_INET)
            addr_buf = ((struct sockaddr_in *)addr_buf) + 1;
        else
            addr_buf = ((struct sockaddr_in6 *)addr_buf) + 1;
    }

    nio_sctp_freepaddrs(paddr);
    return isaa;
}

 /*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    getRemoteAddresses0
 * Signature: (II)[Ljava/net/SocketAddress;
 */
JNIEXPORT jobjectArray JNICALL Java_sun_nio_ch_sctp_SctpNet_getRemoteAddresses0
  (JNIEnv *env, jclass klass, jint fd, jint assocId) {
    return getRemoteAddresses(env, fd, assocId);
}

/* Map the Java level option to the native level */
int mapSocketOption
  (jint cmd, int *level, int *optname) {
    static struct {
        jint cmd;
        int level;
        int optname;
    } const opts[] = {
        { sun_nio_ch_sctp_SctpStdSocketOption_SCTP_DISABLE_FRAGMENTS,   IPPROTO_SCTP, SCTP_DISABLE_FRAGMENTS },
        { sun_nio_ch_sctp_SctpStdSocketOption_SCTP_EXPLICIT_COMPLETE,   IPPROTO_SCTP, SCTP_EXPLICIT_EOR },
        { sun_nio_ch_sctp_SctpStdSocketOption_SCTP_FRAGMENT_INTERLEAVE, IPPROTO_SCTP, SCTP_FRAGMENT_INTERLEAVE },
        { sun_nio_ch_sctp_SctpStdSocketOption_SCTP_NODELAY,             IPPROTO_SCTP, SCTP_NODELAY },
        { sun_nio_ch_sctp_SctpStdSocketOption_SO_SNDBUF,                SOL_SOCKET,   SO_SNDBUF },
        { sun_nio_ch_sctp_SctpStdSocketOption_SO_RCVBUF,                SOL_SOCKET,   SO_RCVBUF },
        { sun_nio_ch_sctp_SctpStdSocketOption_SO_LINGER,                SOL_SOCKET,   SO_LINGER } };

    int i;
    for (i=0; i<(int)(sizeof(opts) / sizeof(opts[0])); i++) {
        if (cmd == opts[i].cmd) {
            *level = opts[i].level;
            *optname = opts[i].optname;
            return 0;
        }
    }

    /* not found */
    return -1;
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    setIntOption0
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_sun_nio_ch_sctp_SctpNet_setIntOption0
  (JNIEnv *env, jclass klass, jint fd, jint opt, int arg) {
    int klevel, kopt;
    int result;
    struct linger linger;
    void *parg;
    int arglen;

    if (mapSocketOption(opt, &klevel, &kopt) < 0) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                     "Unsupported socket option");
        return;
    }

    if (opt == sun_nio_ch_sctp_SctpStdSocketOption_SO_LINGER) {
        parg = (void *)&linger;
        arglen = sizeof(linger);
        if (arg >= 0) {
            linger.l_onoff = 1;
            linger.l_linger = arg;
        } else {
            linger.l_onoff = 0;
            linger.l_linger = 0;
        }
    } else {
        parg = (void *)&arg;
        arglen = sizeof(arg);
    }

    if (NET_SetSockOpt(fd, klevel, kopt, parg, arglen) < 0) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                     "sun_nio_ch_sctp_SctpNet.setIntOption0");
    }
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    getIntOption0
 * Signature: (II)I
 */
JNIEXPORT int JNICALL Java_sun_nio_ch_sctp_SctpNet_getIntOption0
  (JNIEnv *env, jclass klass, jint fd, jint opt) {
    int klevel, kopt;
    int result;
    struct linger linger;
    void *arg;
    int arglen;

    memset((char *) &linger, 0, sizeof(linger));
    if (mapSocketOption(opt, &klevel, &kopt) < 0) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                     "Unsupported socket option");
        return -1;
    }

    if (opt == sun_nio_ch_sctp_SctpStdSocketOption_SO_LINGER) {
        arg = (void *)&linger;
        arglen = sizeof(linger);
    } else {
        arg = (void *)&result;
        arglen = sizeof(result);
    }

    if (NET_GetSockOpt(fd, klevel, kopt, arg, &arglen) < 0) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                     "sun.nio.ch.Net.getIntOption");
        return -1;
    }

    if (opt == sun_nio_ch_sctp_SctpStdSocketOption_SO_LINGER)
        return linger.l_onoff ? linger.l_linger : -1;
    else
        return result;
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    getPrimAddrOption0
 * Signature: (II)Ljava/net/SocketAddress;
 */
JNIEXPORT jobject JNICALL Java_sun_nio_ch_sctp_SctpNet_getPrimAddrOption0
  (JNIEnv *env, jclass klass, jint fd, jint assocId) {
    struct sctp_setprim prim;
    unsigned int prim_len = sizeof(prim);

    prim.ssp_assoc_id = assocId;

    if (getsockopt(fd, IPPROTO_SCTP, SCTP_PRIMARY_ADDR, &prim, &prim_len) < 0) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                     "sun.nio.ch.SctpNet.getPrimAddrOption0");
        return NULL;
    }

    return SockAddrToInetSocketAddress(env, (SOCKETADDRESS *)&prim.ssp_addr);
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    setPrimAddrOption0
 * Signature: (IILjava/net/InetAddress;I)V
 */
JNIEXPORT void JNICALL Java_sun_nio_ch_sctp_SctpNet_setPrimAddrOption0
  (JNIEnv *env, jclass klass, jint fd, jint assocId, jobject iaObj, jint port) {
    struct sctp_setprim prim;

    if (NET_InetAddressToSockaddr(env, iaObj, port,
                                  (SOCKETADDRESS *)&prim.ssp_addr,
                                  NULL, JNI_TRUE) != 0) {
        return;
    }

    prim.ssp_assoc_id = assocId;

    if (setsockopt(fd, IPPROTO_SCTP, SCTP_PRIMARY_ADDR, &prim, sizeof(prim)) < 0) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                     "sun.nio.ch.SctpNet.setPrimAddrOption0");
    }
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    setPeerPrimAddrOption0
 * Signature: (IILjava/net/InetAddress;I)V
 */
JNIEXPORT void JNICALL Java_sun_nio_ch_sctp_SctpNet_setPeerPrimAddrOption0
  (JNIEnv *env, jclass klass, jint fd, jint assocId,
   jobject iaObj, jint port, jboolean preferIPv6) {
    struct sctp_setpeerprim prim;

    if (NET_InetAddressToSockaddr(env, iaObj, port,
                                  (SOCKETADDRESS *)&prim.sspp_addr,
                                  NULL, preferIPv6) != 0) {
        return;
    }

    prim.sspp_assoc_id = assocId;

    if (setsockopt(fd, IPPROTO_SCTP, SCTP_SET_PEER_PRIMARY_ADDR, &prim,
                   sizeof(prim)) < 0) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                     "sun.nio.ch.SctpNet.setPeerPrimAddrOption0");
    }
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    getInitMsgOption0
 * Signature: (I[I)V
 */
JNIEXPORT void JNICALL Java_sun_nio_ch_sctp_SctpNet_getInitMsgOption0
  (JNIEnv *env, jclass klass, jint fd, jintArray retVal) {
    struct sctp_initmsg sctp_initmsg;
    unsigned int sim_len = sizeof(sctp_initmsg);
    int vals[2];

    if (getsockopt(fd, IPPROTO_SCTP, SCTP_INITMSG, &sctp_initmsg,
            &sim_len) < 0) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                     "sun.nio.ch.SctpNet.getInitMsgOption0");
        return;
    }

    vals[0] = sctp_initmsg.sinit_max_instreams;
    vals[1] = sctp_initmsg.sinit_num_ostreams;
    (*env)->SetIntArrayRegion(env, retVal, 0, 2, vals);
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    setInitMsgOption0
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_sun_nio_ch_sctp_SctpNet_setInitMsgOption0
  (JNIEnv *env, jclass klass, jint fd, jint inArg, jint outArg) {
    struct sctp_initmsg sctp_initmsg;

    sctp_initmsg.sinit_max_instreams = (unsigned int)inArg;
    sctp_initmsg.sinit_num_ostreams = (unsigned int)outArg;
    sctp_initmsg.sinit_max_attempts = 0;  // default
    sctp_initmsg.sinit_max_init_timeo = 0;  // default

    if (setsockopt(fd, IPPROTO_SCTP, SCTP_INITMSG, &sctp_initmsg,
          sizeof(sctp_initmsg)) < 0) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                     "sun.nio.ch.SctpNet.setInitMsgOption0");
    }
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    shutdown0
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_sun_nio_ch_sctp_SctpNet_shutdown0
  (JNIEnv *env, jclass klass, jint fd, jint assocId) {
    int rv;
    struct msghdr msg[1];
    struct iovec iov[1];
    int cbuf_size = CMSG_SPACE(sizeof (struct sctp_sndrcvinfo));
    char cbuf[CMSG_SPACE(sizeof (struct sctp_sndrcvinfo))];
    struct cmsghdr* cmsg;
    struct sctp_sndrcvinfo *sri;

    /* SctpSocketChannel */
    if (assocId < 0) {
        shutdown(fd, SHUT_WR);
        return;
    }

    memset(msg, 0, sizeof (*msg));
    memset(cbuf, 0, cbuf_size);
    msg->msg_name = NULL;
    msg->msg_namelen = 0;
    iov->iov_base = NULL;
    iov->iov_len = 0;
    msg->msg_iov = iov;
    msg->msg_iovlen = 1;
    msg->msg_control = cbuf;
    msg->msg_controllen = cbuf_size;
    msg->msg_flags = 0;

    cmsg = CMSG_FIRSTHDR(msg);
    cmsg->cmsg_level = IPPROTO_SCTP;
    cmsg->cmsg_type = SCTP_SNDRCV;
    cmsg->cmsg_len = CMSG_LEN(sizeof(struct sctp_sndrcvinfo));

    /* Initialize the payload: */
    sri = (struct sctp_sndrcvinfo*) CMSG_DATA(cmsg);
    memset(sri, 0, sizeof (*sri));

    if (assocId > 0) {
        sri->sinfo_assoc_id = assocId;
    }

    sri->sinfo_flags = sri->sinfo_flags | SCTP_EOF;

    /* Sum of the length of all control messages in the buffer. */
    msg->msg_controllen = cmsg->cmsg_len;

    if ((rv = sendmsg(fd, msg, 0)) < 0) {
        handleSocketError(env, errno);
    }
}

/*
 * Class:     sun_nio_ch_sctp_SctpNet
 * Method:    branch
 * Signature: (II)I
 */
JNIEXPORT int JNICALL Java_sun_nio_ch_sctp_SctpNet_branch0
  (JNIEnv *env, jclass klass, jint fd, jint assocId) {
    int newfd = 0;
    if ((newfd = nio_sctp_peeloff(fd, assocId)) < 0) {
        handleSocketError(env, errno);
    }

    return newfd;
}
