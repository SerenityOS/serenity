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
#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>

#include "sysSocket.h"
#include "socketTransport.h"

typedef jboolean bool_t;

/*
 * Table of Windows Sockets errors, the specific exception we
 * throw for the error, and the error text.
 *
 * Note that this table excludes OS dependent errors.
 */
static struct {
    int errCode;
    const char *errString;
} const winsock_errors[] = {
    { WSAEPROVIDERFAILEDINIT,   "Provider initialization failed (check %SystemRoot%)" },
    { WSAEACCES,                "Permission denied" },
    { WSAEADDRINUSE,            "Address already in use" },
    { WSAEADDRNOTAVAIL,         "Cannot assign requested address" },
    { WSAEAFNOSUPPORT,          "Address family not supported by protocol family" },
    { WSAEALREADY,              "Operation already in progress" },
    { WSAECONNABORTED,          "Software caused connection abort" },
    { WSAECONNREFUSED,          "Connection refused" },
    { WSAECONNRESET,            "Connection reset by peer" },
    { WSAEDESTADDRREQ,          "Destination address required" },
    { WSAEFAULT,                "Bad address" },
    { WSAEHOSTDOWN,             "Host is down" },
    { WSAEHOSTUNREACH,          "No route to host" },
    { WSAEINPROGRESS,           "Operation now in progress" },
    { WSAEINTR,                 "Interrupted function call" },
    { WSAEINVAL,                "Invalid argument" },
    { WSAEISCONN,               "Socket is already connected" },
    { WSAEMFILE,                "Too many open files" },
    { WSAEMSGSIZE,              "The message is larger than the maximum supported by the underlying transport" },
    { WSAENETDOWN,              "Network is down" },
    { WSAENETRESET,             "Network dropped connection on reset" },
    { WSAENETUNREACH,           "Network is unreachable" },
    { WSAENOBUFS,               "No buffer space available (maximum connections reached?)" },
    { WSAENOPROTOOPT,           "Bad protocol option" },
    { WSAENOTCONN,              "Socket is not connected" },
    { WSAENOTSOCK,              "Socket operation on nonsocket" },
    { WSAEOPNOTSUPP,            "Operation not supported" },
    { WSAEPFNOSUPPORT,          "Protocol family not supported" },
    { WSAEPROCLIM,              "Too many processes" },
    { WSAEPROTONOSUPPORT,       "Protocol not supported" },
    { WSAEPROTOTYPE,            "Protocol wrong type for socket" },
    { WSAESHUTDOWN,             "Cannot send after socket shutdown" },
    { WSAESOCKTNOSUPPORT,       "Socket type not supported" },
    { WSAETIMEDOUT,             "Connection timed out" },
    { WSATYPE_NOT_FOUND,        "Class type not found" },
    { WSAEWOULDBLOCK,           "Resource temporarily unavailable" },
    { WSAHOST_NOT_FOUND,        "Host not found" },
    { WSA_NOT_ENOUGH_MEMORY,    "Insufficient memory available" },
    { WSANOTINITIALISED,        "Successful WSAStartup not yet performed" },
    { WSANO_DATA,               "Valid name, no data record of requested type" },
    { WSANO_RECOVERY,           "This is a nonrecoverable error" },
    { WSASYSNOTREADY,           "Network subsystem is unavailable" },
    { WSATRY_AGAIN,             "Nonauthoritative host not found" },
    { WSAVERNOTSUPPORTED,       "Winsock.dll version out of range" },
    { WSAEDISCON,               "Graceful shutdown in progress" },
    { WSA_OPERATION_ABORTED,    "Overlapped operation aborted" },
};


/*
 * Initialize Windows Sockets API support
 */
BOOL WINAPI
DllMain(HINSTANCE hinst, DWORD reason, LPVOID reserved)
{
    WSADATA wsadata;

    switch (reason) {
        case DLL_PROCESS_ATTACH:
            if (WSAStartup(MAKEWORD(2,2), &wsadata) != 0) {
                return FALSE;
            }
            break;

        case DLL_PROCESS_DETACH:
            WSACleanup();
            break;

        default:
            break;
    }
    return TRUE;
}

/*
 * If we get a nonnull function pointer it might still be the case
 * that some other thread is in the process of initializing the socket
 * function pointer table, but our pointer should still be good.
 */
int
dbgsysListen(int fd, int backlog) {
    return listen(fd, backlog);
}

int
dbgsysConnect(int fd, struct sockaddr *name, socklen_t namelen) {
    int rv = connect(fd, name, namelen);
    if (rv == SOCKET_ERROR) {
        if (WSAGetLastError() == WSAEINPROGRESS || WSAGetLastError() == WSAEWOULDBLOCK) {
            return DBG_EINPROGRESS;
        }
    }
    return rv;
}

int dbgsysFinishConnect(int fd, int timeout) {
    int rv;
    struct timeval t;
    fd_set wr, ex;

    t.tv_sec = timeout / 1000;
    t.tv_usec = (timeout % 1000) * 1000;

    FD_ZERO(&wr);
    FD_ZERO(&ex);
    FD_SET((unsigned int)fd, &wr);
    FD_SET((unsigned int)fd, &ex);

    rv = select(fd+1, 0, &wr, &ex, &t);
    if (rv == 0) {
        return SYS_ERR;     /* timeout */
    }

    /*
     * Check if there was an error - this is preferable to check if
     * the socket is writable because some versions of Windows don't
     * report a connected socket as being writable.
     */
    if (!FD_ISSET(fd, &ex)) {
        return SYS_OK;
    }

    /*
     * Unable to establish connection - to get the reason we must
     * call getsockopt.
     */
    return SYS_ERR;
}


int
dbgsysAccept(int fd, struct sockaddr *name, socklen_t *namelen) {
    return (int)accept(fd, name, namelen);
}

int
dbgsysRecvFrom(int fd, char *buf, size_t nBytes,
                  int flags, struct sockaddr *from, socklen_t *fromlen) {
    return recvfrom(fd, buf, (int)nBytes, flags, from, fromlen);
}

int
dbgsysSendTo(int fd, char *buf, size_t len,
                int flags, struct sockaddr *to, socklen_t tolen) {
    return sendto(fd, buf, (int)len, flags, to, tolen);
}

int
dbgsysRecv(int fd, char *buf, size_t nBytes, int flags) {
    return recv(fd, buf, (int) nBytes, flags);
}

int
dbgsysSend(int fd, char *buf, size_t nBytes, int flags) {
    return send(fd, buf, (int)nBytes, flags);
}

int
dbgsysGetAddrInfo(const char *hostname, const char *service,
                  const struct addrinfo *hints,
                  struct addrinfo **result) {
    return getaddrinfo(hostname, service, hints, result);
}

void
dbgsysFreeAddrInfo(struct addrinfo *info) {
    freeaddrinfo(info);
}

unsigned short
dbgsysHostToNetworkShort(unsigned short hostshort) {
    return htons(hostshort);
}

int
dbgsysSocket(int domain, int type, int protocol) {
  int fd = (int)socket(domain, type, protocol);
  if (fd != SOCKET_ERROR) {
    SetHandleInformation((HANDLE)(UINT_PTR)fd, HANDLE_FLAG_INHERIT, FALSE);
  }
  return fd;
}

int
dbgsysSocketClose(int fd) {
    struct linger l;
    int len = sizeof(l);

    if (getsockopt(fd, SOL_SOCKET, SO_LINGER, (char *)&l, &len) == 0) {
        if (l.l_onoff == 0) {
            shutdown(fd, SD_SEND);
        }
    }
    return closesocket(fd);
}

/* Additions to original follow */

int
dbgsysBind(int fd, struct sockaddr *name, socklen_t namelen) {
    return bind(fd, name, namelen);
}


uint32_t
dbgsysHostToNetworkLong(uint32_t hostlong) {
    return (uint32_t)htonl((u_long)hostlong);
}

unsigned short
dbgsysNetworkToHostShort(unsigned short netshort) {
    return ntohs(netshort);
}

int
dbgsysGetSocketName(int fd, struct sockaddr *name, socklen_t *namelen) {
    return getsockname(fd, name, namelen);
}

uint32_t
dbgsysNetworkToHostLong(uint32_t netlong) {
    return (uint32_t)ntohl((u_long)netlong);
}

int
dbgsysSetSocketOption(int fd, jint cmd, jboolean on, jvalue value)
{
    if (cmd == TCP_NODELAY) {
        struct protoent *proto = getprotobyname("TCP");
        int tcp_level = (proto == 0 ? IPPROTO_TCP: proto->p_proto);
        long onl = (long)on;

        if (setsockopt(fd, tcp_level, TCP_NODELAY,
                       (char *)&onl, sizeof(long)) < 0) {
                return SYS_ERR;
        }
    } else if (cmd == SO_LINGER) {
        struct linger arg;
        arg.l_onoff = on;

        if(on) {
            arg.l_linger = (unsigned short)value.i;
            if(setsockopt(fd, SOL_SOCKET, SO_LINGER,
                          (char*)&arg, sizeof(arg)) < 0) {
                return SYS_ERR;
            }
        } else {
            if (setsockopt(fd, SOL_SOCKET, SO_LINGER,
                           (char*)&arg, sizeof(arg)) < 0) {
                return SYS_ERR;
            }
        }
    } else if (cmd == SO_SNDBUF) {
        jint buflen = value.i;
        if (setsockopt(fd, SOL_SOCKET, SO_SNDBUF,
                       (char *)&buflen, sizeof(buflen)) < 0) {
            return SYS_ERR;
        }
    } else if (cmd == SO_REUSEADDR) {
        /*
         * On Windows the SO_REUSEADDR socket option doesn't implement
         * BSD semantics. Specifically, the socket option allows multiple
         * processes to bind to the same address/port rather than allowing
         * a process to bind with a previous connection in the TIME_WAIT
         * state. Hence on Windows we never enable this option for TCP
         * option.
         */
        int sotype, arglen=sizeof(sotype);
        if (getsockopt(fd, SOL_SOCKET, SO_TYPE, (void *)&sotype, &arglen) == SOCKET_ERROR) {
            return SYS_ERR;
        }
        if (sotype != SOCK_STREAM) {
            int oni = (int)on;
            if (setsockopt(fd, SOL_SOCKET, SO_REUSEADDR,
                       (char *)&oni, sizeof(oni)) == SOCKET_ERROR) {
                return SYS_ERR;
            }
        }
    } else {
        return SYS_ERR;
    }
    return SYS_OK;
}

int dbgsysConfigureBlocking(int fd, jboolean blocking) {
    u_long argp;
    int result = 0;

    if (blocking == JNI_FALSE) {
        argp = 1;
    } else {
        argp = 0;
    }
    result = ioctlsocket(fd, FIONBIO, &argp);
    if (result == SOCKET_ERROR) {
        return SYS_ERR;
    } else {
        return SYS_OK;
    }
}

int
dbgsysPoll(int fd, jboolean rd, jboolean wr, long timeout) {
    int rv;
    struct timeval t;
    fd_set rd_tbl, wr_tbl;

    t.tv_sec = timeout / 1000;
    t.tv_usec = (timeout % 1000) * 1000;

    FD_ZERO(&rd_tbl);
    if (rd) {
        FD_SET((unsigned int)fd, &rd_tbl);
    }

    FD_ZERO(&wr_tbl);
    if (wr) {
        FD_SET((unsigned int)fd, &wr_tbl);
    }

    rv = select(fd+1, &rd_tbl, &wr_tbl, 0, &t);
    if (rv >= 0) {
        rv = 0;
        if (FD_ISSET(fd, &rd_tbl)) {
            rv |= DBG_POLLIN;
        }
        if (FD_ISSET(fd, &wr_tbl)) {
            rv |= DBG_POLLOUT;
        }
    }
    return rv;
}

int
dbgsysGetLastIOError(char *buf, jint size) {
    int table_size = sizeof(winsock_errors) /
                     sizeof(winsock_errors[0]);
    int i;
    int error = WSAGetLastError();

    /*
     * Check table for known winsock errors
     */
    i=0;
    while (i < table_size) {
        if (error == winsock_errors[i].errCode) {
            break;
        }
        i++;
    }

    if (i < table_size) {
        strcpy(buf, winsock_errors[i].errString);
    } else {
        sprintf(buf, "winsock error %d", error);
    }
    return 0;
}


int
dbgsysTlsAlloc() {
    return TlsAlloc();
}

void
dbgsysTlsFree(int index) {
    TlsFree(index);
}

void
dbgsysTlsPut(int index, void *value) {
    TlsSetValue(index, value);
}

void *
dbgsysTlsGet(int index) {
    return TlsGetValue(index);
}

#define FT2INT64(ft) \
        ((INT64)(ft).dwHighDateTime << 32 | (INT64)(ft).dwLowDateTime)

long
dbgsysCurrentTimeMillis() {
    static long fileTime_1_1_70 = 0;    /* midnight 1/1/70 */
    SYSTEMTIME st0;
    FILETIME   ft0;

    /* initialize on first usage */
    if (fileTime_1_1_70 == 0) {
        memset(&st0, 0, sizeof(st0));
        st0.wYear  = 1970;
        st0.wMonth = 1;
        st0.wDay   = 1;
        SystemTimeToFileTime(&st0, &ft0);
        fileTime_1_1_70 = FT2INT64(ft0);
    }

    GetSystemTime(&st0);
    SystemTimeToFileTime(&st0, &ft0);

    return (FT2INT64(ft0) - fileTime_1_1_70) / 10000;
}
