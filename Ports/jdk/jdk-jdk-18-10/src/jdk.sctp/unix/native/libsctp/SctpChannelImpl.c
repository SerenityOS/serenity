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
#include "Sctp.h"

#include "jni.h"
#include "nio_util.h"
#include "nio.h"
#include "net_util.h"
#include "net_util_md.h"
#include "sun_nio_ch_sctp_SctpNet.h"
#include "sun_nio_ch_sctp_SctpChannelImpl.h"
#include "sun_nio_ch_sctp_AssociationChange.h"
#include "sun_nio_ch_sctp_ResultContainer.h"
#include "sun_nio_ch_sctp_PeerAddrChange.h"

static int SCTP_NOTIFICATION_SIZE = sizeof(union sctp_notification);

#define MESSAGE_IMPL_CLASS              "sun/nio/ch/sctp/MessageInfoImpl"
#define RESULT_CONTAINER_CLASS          "sun/nio/ch/sctp/ResultContainer"
#define SEND_FAILED_CLASS               "sun/nio/ch/sctp/SendFailed"
#define ASSOC_CHANGE_CLASS              "sun/nio/ch/sctp/AssociationChange"
#define PEER_CHANGE_CLASS               "sun/nio/ch/sctp/PeerAddrChange"
#define SHUTDOWN_CLASS                  "sun/nio/ch/sctp/Shutdown"

struct controlData {
    int assocId;
    unsigned short streamNumber;
    jboolean unordered;
    unsigned int ppid;
};

static jclass    smi_class;    /* sun.nio.ch.sctp.MessageInfoImpl            */
static jmethodID smi_ctrID;    /* sun.nio.ch.sctp.MessageInfoImpl.<init>     */
static jfieldID  src_valueID;  /* sun.nio.ch.sctp.ResultContainer.value      */
static jfieldID  src_typeID;   /* sun.nio.ch.sctp.ResultContainer.type       */
static jclass    ssf_class;    /* sun.nio.ch.sctp.SendFailed                 */
static jmethodID ssf_ctrID;    /* sun.nio.ch.sctp.SendFailed.<init>          */
static jclass    sac_class;    /* sun.nio.ch.sctp.AssociationChange          */
static jmethodID sac_ctrID;    /* sun.nio.ch.sctp.AssociationChange.<init>   */
static jclass    spc_class;    /* sun.nio.ch.sctp.PeerAddressChanged         */
static jmethodID spc_ctrID;    /* sun.nio.ch.sctp.PeerAddressChanged.<init>  */
static jclass    ss_class;     /* sun.nio.ch.sctp.Shutdown                   */
static jmethodID ss_ctrID;     /* sun.nio.ch.sctp.Shutdown.<init>            */

/* defined in SctpNet.c */
jobject SockAddrToInetSocketAddress(JNIEnv* env, struct sockaddr* addr);

jint handleSocketError(JNIEnv *env, jint errorValue);

/*
 * Class:     sun_nio_ch_sctp_SctpChannelImpl
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_nio_ch_sctp_SctpChannelImpl_initIDs
  (JNIEnv *env, jclass klass) {
    jclass cls;

    /* MessageInfoImpl */
    cls = (*env)->FindClass(env, MESSAGE_IMPL_CLASS);
    CHECK_NULL(cls);
    smi_class = (*env)->NewGlobalRef(env, cls);
    CHECK_NULL(smi_class);
    smi_ctrID = (*env)->GetMethodID(env, cls, "<init>",
            "(ILjava/net/SocketAddress;IIZZI)V");
    CHECK_NULL(smi_ctrID);

    /* ResultContainer */
    cls = (*env)->FindClass(env, RESULT_CONTAINER_CLASS);
    CHECK_NULL(cls);
    src_valueID = (*env)->GetFieldID(env, cls, "value", "Ljava/lang/Object;");
    CHECK_NULL(src_valueID);
    src_typeID = (*env)->GetFieldID(env, cls, "type", "I");
    CHECK_NULL(src_typeID);

    /* SendFailed */
    cls = (*env)->FindClass(env, SEND_FAILED_CLASS);
    CHECK_NULL(cls);
    ssf_class = (*env)->NewGlobalRef(env, cls);
    CHECK_NULL(ssf_class);
    ssf_ctrID = (*env)->GetMethodID(env, cls, "<init>",
        "(ILjava/net/SocketAddress;Ljava/nio/ByteBuffer;II)V");
    CHECK_NULL(ssf_ctrID);

    /* AssociationChange */
    cls = (*env)->FindClass(env, ASSOC_CHANGE_CLASS);
    CHECK_NULL(cls);
    sac_class = (*env)->NewGlobalRef(env, cls);
    CHECK_NULL(sac_class);
    sac_ctrID = (*env)->GetMethodID(env, cls, "<init>", "(IIII)V");
    CHECK_NULL(sac_ctrID);

    /* PeerAddrChange */
    cls = (*env)->FindClass(env, PEER_CHANGE_CLASS);
    CHECK_NULL(cls);
    spc_class = (*env)->NewGlobalRef(env, cls);
    CHECK_NULL(spc_class);
    spc_ctrID = (*env)->GetMethodID(env, cls, "<init>",
            "(ILjava/net/SocketAddress;I)V");
    CHECK_NULL(spc_ctrID);

    /* Shutdown */
    cls = (*env)->FindClass(env, SHUTDOWN_CLASS);
    CHECK_NULL(cls);
    ss_class = (*env)->NewGlobalRef(env, cls);
    CHECK_NULL(ss_class);
    ss_ctrID = (*env)->GetMethodID(env, cls, "<init>", "(I)V");
    CHECK_NULL(ss_ctrID);
}

void getControlData
  (struct msghdr* msg, struct controlData* cdata) {
    struct cmsghdr* cmsg;

    for (cmsg = CMSG_FIRSTHDR(msg); cmsg != NULL; cmsg = CMSG_NXTHDR(msg, cmsg)) {
        if (cmsg->cmsg_level == IPPROTO_SCTP && cmsg->cmsg_type == SCTP_SNDRCV) {
            struct sctp_sndrcvinfo *sri;

            sri = (struct sctp_sndrcvinfo *) CMSG_DATA(cmsg);
            cdata->assocId = sri->sinfo_assoc_id;
            cdata->streamNumber = sri->sinfo_stream;
            cdata->unordered = (sri->sinfo_flags & SCTP_UNORDERED) ? JNI_TRUE :
                JNI_FALSE;
            cdata->ppid = ntohl(sri->sinfo_ppid);

            return;
        }
    }
    return;
}

void setControlData
  (struct msghdr* msg, struct controlData* cdata) {
    struct cmsghdr* cmsg;
    struct sctp_sndrcvinfo *sri;

    cmsg = CMSG_FIRSTHDR(msg);
    cmsg->cmsg_level = IPPROTO_SCTP;
    cmsg->cmsg_type = SCTP_SNDRCV;
    cmsg->cmsg_len = CMSG_LEN(sizeof(struct sctp_sndrcvinfo));

    /* Initialize the payload */
    sri = (struct sctp_sndrcvinfo*) CMSG_DATA(cmsg);
    memset(sri, 0, sizeof (*sri));

    if (cdata->streamNumber > 0) {
        sri->sinfo_stream = cdata->streamNumber;
    }
    if (cdata->assocId > 0) {
        sri->sinfo_assoc_id = cdata->assocId;
    }
    if (cdata->unordered == JNI_TRUE) {
        sri->sinfo_flags = sri->sinfo_flags | SCTP_UNORDERED;
    }

    if (cdata->ppid > 0) {
        sri->sinfo_ppid = htonl(cdata->ppid);
    }

    /* Sum of the length of all control messages in the buffer. */
    msg->msg_controllen = cmsg->cmsg_len;
}

// TODO: test: can create send failed without any data? if so need to
// update API so that buffer can be null if no data.
void handleSendFailed
  (JNIEnv* env, int fd, jobject resultContainerObj, struct sctp_send_failed *ssf,
   int read, jboolean isEOR, struct sockaddr* sap) {
    jobject bufferObj = NULL, resultObj, isaObj;
    char *addressP;
    struct sctp_sndrcvinfo *sri;
    int remaining, dataLength;

    /* the actual undelivered message data is directly after the ssf */
    int dataOffset = sizeof(struct sctp_send_failed);

    sri = (struct sctp_sndrcvinfo*) &ssf->ssf_info;

    /* the number of bytes remaining to be read in the sctp_send_failed notif*/
    remaining = ssf->ssf_length - read;

    /* the size of the actual undelivered message */
    dataLength = ssf->ssf_length - dataOffset;

    /* retrieved address from sockaddr */
    isaObj = SockAddrToInetSocketAddress(env, sap);
    CHECK_NULL(isaObj);

    /* data retrieved from sff_data */
    if (dataLength > 0) {
        struct iovec iov[1];
        struct msghdr msg[1];
        int rv, alreadyRead;
        char *dataP = (char*) ssf;
        dataP += dataOffset;

        if ((addressP = malloc(dataLength)) == NULL) {
            JNU_ThrowOutOfMemoryError(env, "handleSendFailed");
            return;
        }

        memset(msg, 0, sizeof (*msg));
        msg->msg_iov = iov;
        msg->msg_iovlen = 1;

        bufferObj = (*env)->NewDirectByteBuffer(env, addressP, dataLength);
        if (bufferObj == NULL) {
            free(addressP);
            return;
        }

        alreadyRead = read - dataOffset;
        if (alreadyRead > 0) {
            memcpy(addressP, /*ssf->ssf_data*/ dataP, alreadyRead);
            iov->iov_base = addressP + alreadyRead;
            iov->iov_len = dataLength - alreadyRead;
        } else {
            iov->iov_base = addressP;
            iov->iov_len = dataLength;
        }

        if (remaining > 0) {
            if ((rv = recvmsg(fd, msg, 0)) < 0) {
                free(addressP);
                handleSocketError(env, errno);
                return;
            }

            if (rv != (dataLength - alreadyRead) || !(msg->msg_flags & MSG_EOR)) {
                //TODO: assert false: "should not reach here";
                free(addressP);
                return;
            }
            // TODO: Set and document (in API) buffers position.
        }
    }

    /* create SendFailed */
    resultObj = (*env)->NewObject(env, ssf_class, ssf_ctrID, ssf->ssf_assoc_id,
            isaObj, bufferObj, ssf->ssf_error, sri->sinfo_stream);
    if (resultObj == NULL) {
        if (bufferObj != NULL) free(addressP);
        return;
    }
    (*env)->SetObjectField(env, resultContainerObj, src_valueID, resultObj);
    (*env)->SetIntField(env, resultContainerObj, src_typeID,
            sun_nio_ch_sctp_ResultContainer_SEND_FAILED);
}

void handleAssocChange
  (JNIEnv* env, jobject resultContainerObj, struct sctp_assoc_change *sac) {
    jobject resultObj;
    int state = 0;

    switch (sac->sac_state) {
        case SCTP_COMM_UP :
            state = sun_nio_ch_sctp_AssociationChange_SCTP_COMM_UP;
            break;
        case SCTP_COMM_LOST :
            state = sun_nio_ch_sctp_AssociationChange_SCTP_COMM_LOST;
            break;
        case SCTP_RESTART :
            state = sun_nio_ch_sctp_AssociationChange_SCTP_RESTART;
            break;
        case SCTP_SHUTDOWN_COMP :
            state = sun_nio_ch_sctp_AssociationChange_SCTP_SHUTDOWN;
            break;
        case SCTP_CANT_STR_ASSOC :
            state = sun_nio_ch_sctp_AssociationChange_SCTP_CANT_START;
    }

    /* create AssociationChange */
    resultObj = (*env)->NewObject(env, sac_class, sac_ctrID, sac->sac_assoc_id,
        state, sac->sac_outbound_streams, sac->sac_inbound_streams);
    CHECK_NULL(resultObj);
    (*env)->SetObjectField(env, resultContainerObj, src_valueID, resultObj);
    (*env)->SetIntField(env, resultContainerObj, src_typeID,
            sun_nio_ch_sctp_ResultContainer_ASSOCIATION_CHANGED);
}

void handleShutdown
  (JNIEnv* env, jobject resultContainerObj, struct sctp_shutdown_event* sse) {
    /* create Shutdown */
    jobject resultObj = (*env)->NewObject(env, ss_class, ss_ctrID, sse->sse_assoc_id);
    CHECK_NULL(resultObj);
    (*env)->SetObjectField(env, resultContainerObj, src_valueID, resultObj);
    (*env)->SetIntField(env, resultContainerObj, src_typeID,
            sun_nio_ch_sctp_ResultContainer_SHUTDOWN);
}

void handlePeerAddrChange
  (JNIEnv* env, jobject resultContainerObj, struct sctp_paddr_change* spc) {
    int event = 0;
    jobject addressObj, resultObj;
    unsigned int state = spc->spc_state;

    switch (state) {
        case SCTP_ADDR_AVAILABLE :
            event = sun_nio_ch_sctp_PeerAddrChange_SCTP_ADDR_AVAILABLE;
            break;
        case SCTP_ADDR_UNREACHABLE :
            event = sun_nio_ch_sctp_PeerAddrChange_SCTP_ADDR_UNREACHABLE;
            break;
        case SCTP_ADDR_REMOVED :
            event = sun_nio_ch_sctp_PeerAddrChange_SCTP_ADDR_REMOVED;
            break;
        case SCTP_ADDR_ADDED :
            event = sun_nio_ch_sctp_PeerAddrChange_SCTP_ADDR_ADDED;
            break;
        case SCTP_ADDR_MADE_PRIM :
            event = sun_nio_ch_sctp_PeerAddrChange_SCTP_ADDR_MADE_PRIM;
            break;
#ifdef __linux__
        case SCTP_ADDR_CONFIRMED :
            event = sun_nio_ch_sctp_PeerAddrChange_SCTP_ADDR_CONFIRMED;
            break;
#endif  /* __linux__ */
    }

    addressObj = SockAddrToInetSocketAddress(env, (struct sockaddr*)&spc->spc_aaddr);
    CHECK_NULL(addressObj);

    /* create PeerAddressChanged */
    resultObj = (*env)->NewObject(env, spc_class, spc_ctrID, spc->spc_assoc_id,
            addressObj, event);
    CHECK_NULL(resultObj);
    (*env)->SetObjectField(env, resultContainerObj, src_valueID, resultObj);
    (*env)->SetIntField(env, resultContainerObj, src_typeID,
            sun_nio_ch_sctp_ResultContainer_PEER_ADDRESS_CHANGED);
}

void handleUninteresting
  (union sctp_notification *snp) {
    //fprintf(stdout,"\nNative: handleUninterestingNotification: Receive notification type [%u]", snp->sn_header.sn_type);
}

/**
 * Handle notifications from the SCTP stack.
 * Returns JNI_TRUE if the notification is one that is of interest to the
 * Java API, otherwise JNI_FALSE.
 */
jboolean handleNotification
  (JNIEnv* env, int fd, jobject resultContainerObj, union sctp_notification* snp,
   int read, jboolean isEOR, struct sockaddr* sap) {
    switch (snp->sn_header.sn_type) {
        case SCTP_SEND_FAILED:
            handleSendFailed(env, fd, resultContainerObj, &snp->sn_send_failed,
                    read, isEOR, sap);
            return JNI_TRUE;
        case SCTP_ASSOC_CHANGE:
            handleAssocChange(env, resultContainerObj, &snp->sn_assoc_change);
            return JNI_TRUE;
        case SCTP_SHUTDOWN_EVENT:
            handleShutdown(env, resultContainerObj, &snp->sn_shutdown_event);
            return JNI_TRUE;
        case SCTP_PEER_ADDR_CHANGE:
            handlePeerAddrChange(env, resultContainerObj, &snp->sn_paddr_change);
            return JNI_TRUE;
        default :
            /* the Java API is not interested in this event, maybe we are? */
            handleUninteresting(snp);
    }
    return JNI_FALSE;
}

void handleMessage
  (JNIEnv* env, jobject resultContainerObj, struct msghdr* msg,int read,
   jboolean isEOR, struct sockaddr* sap) {
    jobject isa, resultObj;
    struct controlData cdata[1];

    if (read == 0) {
        /* we reached EOF */
        read = -1;
    }

    isa = SockAddrToInetSocketAddress(env, sap);
    CHECK_NULL(isa);
    getControlData(msg, cdata);

    /* create MessageInfoImpl */
    resultObj = (*env)->NewObject(env, smi_class, smi_ctrID, cdata->assocId,
                                  isa, read, cdata->streamNumber,
                                  isEOR ? JNI_TRUE : JNI_FALSE,
                                  cdata->unordered, cdata->ppid);
    CHECK_NULL(resultObj);
    (*env)->SetObjectField(env, resultContainerObj, src_valueID, resultObj);
    (*env)->SetIntField(env, resultContainerObj, src_typeID,
                        sun_nio_ch_sctp_ResultContainer_MESSAGE);
}

/*
 * Class:     sun_nio_ch_sctp_SctpChannelImpl
 * Method:    receive0
 * Signature: (ILsun/nio/ch/sctp/ResultContainer;JIZ)I
 */
JNIEXPORT jint JNICALL Java_sun_nio_ch_sctp_SctpChannelImpl_receive0
  (JNIEnv *env, jclass klass, jint fd, jobject resultContainerObj,
   jlong address, jint length, jboolean peek) {
    SOCKETADDRESS sa;
    ssize_t rv = 0;
    jlong *addr = jlong_to_ptr(address);
    struct iovec iov[1];
    struct msghdr msg[1];
    char cbuf[CMSG_SPACE(sizeof (struct sctp_sndrcvinfo))];
    int flags = peek == JNI_TRUE ? MSG_PEEK : 0;

    /* Set up the msghdr structure for receiving */
    memset(msg, 0, sizeof (*msg));
    msg->msg_name = &sa;
    msg->msg_namelen = sizeof(sa);
    iov->iov_base = addr;
    iov->iov_len = length;
    msg->msg_iov = iov;
    msg->msg_iovlen = 1;
    msg->msg_control = cbuf;
    msg->msg_controllen = sizeof(cbuf);
    msg->msg_flags = 0;

    do {
        if ((rv = recvmsg(fd, msg, flags)) < 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                return IOS_UNAVAILABLE;
            } else if (errno == EINTR) {
                return IOS_INTERRUPTED;

#ifdef __linux__
            } else if (errno == ENOTCONN) {
                /* ENOTCONN when EOF reached */
                rv = 0;
                /* there will be no control data */
                msg->msg_controllen = 0;
#endif /* __linux__ */

            } else {
                handleSocketError(env, errno);
                return 0;
            }
        }

        if (msg->msg_flags & MSG_NOTIFICATION) {
            char *bufp = (char*)addr;
            union sctp_notification *snp;
            jboolean allocated = JNI_FALSE;

            if (!(msg->msg_flags & MSG_EOR) && length < SCTP_NOTIFICATION_SIZE) {
                char* newBuf;
                int rvSAVE = rv;

                if ((newBuf = malloc(SCTP_NOTIFICATION_SIZE)) == NULL) {
                    JNU_ThrowOutOfMemoryError(env, "Out of native heap space.");
                    return -1;
                }
                allocated = JNI_TRUE;

                memcpy(newBuf, addr, rv);
                iov->iov_base = newBuf + rv;
                iov->iov_len = SCTP_NOTIFICATION_SIZE - rv;
                if ((rv = recvmsg(fd, msg, flags)) < 0) {
                    handleSocketError(env, errno);
                    free(newBuf);
                    return 0;
                }
                bufp = newBuf;
                rv += rvSAVE;
            }
            snp = (union sctp_notification *) bufp;
            if (handleNotification(env, fd, resultContainerObj, snp, rv,
                                   (msg->msg_flags & MSG_EOR),
                                   &sa.sa) == JNI_TRUE) {
                /* We have received a notification that is of interest
                   to the Java API. The appropriate notification will be
                   set in the result container. */
                if (allocated == JNI_TRUE) {
                    free(bufp);
                }
                return 0;
            }

            if (allocated == JNI_TRUE) {
                free(bufp);
            }

            // set iov back to addr, and reset msg_controllen
            iov->iov_base = addr;
            iov->iov_len = length;
            msg->msg_control = cbuf;
            msg->msg_controllen = sizeof(cbuf);
        }
    } while (msg->msg_flags & MSG_NOTIFICATION);

    handleMessage(env, resultContainerObj, msg, rv,
            (msg->msg_flags & MSG_EOR), &sa.sa);
    return rv;
}

/*
 * Class:     sun_nio_ch_sctp_SctpChannelImpl
 * Method:    send0
 * Signature: (IJILjava/net/InetAddress;IIIZI)I
 */
JNIEXPORT jint JNICALL Java_sun_nio_ch_sctp_SctpChannelImpl_send0
  (JNIEnv *env, jclass klass, jint fd, jlong address, jint length,
   jobject targetAddress, jint targetPort, jint assocId, jint streamNumber,
   jboolean unordered, jint ppid) {
    SOCKETADDRESS sa;
    int sa_len = 0;
    ssize_t rv = 0;
    jlong *addr = jlong_to_ptr(address);
    struct iovec iov[1];
    struct msghdr msg[1];
    int cbuf_size = CMSG_SPACE(sizeof (struct sctp_sndrcvinfo));
    char cbuf[CMSG_SPACE(sizeof (struct sctp_sndrcvinfo))];
    struct controlData cdata[1];

    /* SctpChannel:
     *    targetAddress may contain the preferred address or NULL to use primary,
     *    assocId will always be -1
     * SctpMultiChannell:
     *    Setup new association, targetAddress will contain address, assocId = -1
     *    Association already existing, assocId != -1, targetAddress = preferred addr
     */
    if (targetAddress != NULL /*&& assocId <= 0*/) {
        if (NET_InetAddressToSockaddr(env, targetAddress, targetPort, &sa,
                                      &sa_len, JNI_TRUE) != 0) {
            return IOS_THROWN;
        }
    } else {
        memset(&sa, '\x0', sizeof(sa));
    }

    /* Set up the msghdr structure for sending */
    memset(msg, 0, sizeof (*msg));
    memset(cbuf, 0, cbuf_size);
    msg->msg_name = &sa;
    msg->msg_namelen = sa_len;
    iov->iov_base = addr;
    iov->iov_len = length;
    msg->msg_iov = iov;
    msg->msg_iovlen = 1;
    msg->msg_control = cbuf;
    msg->msg_controllen = cbuf_size;
    msg->msg_flags = 0;

    cdata->streamNumber = streamNumber;
    cdata->assocId = assocId;
    cdata->unordered = unordered;
    cdata->ppid = ppid;
    setControlData(msg, cdata);

    if ((rv = sendmsg(fd, msg, 0)) < 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            return IOS_UNAVAILABLE;
        } else if (errno == EINTR) {
            return IOS_INTERRUPTED;
        } else if (errno == EPIPE) {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                            "Socket is shutdown for writing");
        } else {
            handleSocketError(env, errno);
            return 0;
        }
    }

    return rv;
}

