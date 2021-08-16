/*
 * MUSCLE SmartCard Development ( https://pcsclite.apdu.fr/ )
 *
 * Copyright (C) 1999-2003
 *  David Corcoran <corcoran@musclecard.com>
 * Copyright (C) 2002-2009
 *  Ludovic Rousseau <ludovic.rousseau@free.fr>
 *
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @file
 * @brief This handles smart card reader communications.
 */

#ifndef __winscard_h__
#define __winscard_h__

#include <pcsclite.h>

#ifdef __cplusplus
extern "C"
{
#endif

#ifndef PCSC_API
#define PCSC_API
#endif

      PCSC_API LONG SCardEstablishContext(DWORD dwScope,
            /*@null@*/ LPCVOID pvReserved1, /*@null@*/ LPCVOID pvReserved2,
            /*@out@*/ LPSCARDCONTEXT phContext);

      PCSC_API LONG SCardReleaseContext(SCARDCONTEXT hContext);

      PCSC_API LONG SCardIsValidContext(SCARDCONTEXT hContext);

      PCSC_API LONG SCardConnect(SCARDCONTEXT hContext,
            LPCSTR szReader,
            DWORD dwShareMode,
            DWORD dwPreferredProtocols,
            /*@out@*/ LPSCARDHANDLE phCard, /*@out@*/ LPDWORD pdwActiveProtocol);

      PCSC_API LONG SCardReconnect(SCARDHANDLE hCard,
            DWORD dwShareMode,
            DWORD dwPreferredProtocols,
            DWORD dwInitialization, /*@out@*/ LPDWORD pdwActiveProtocol);

      PCSC_API LONG SCardDisconnect(SCARDHANDLE hCard, DWORD dwDisposition);

      PCSC_API LONG SCardBeginTransaction(SCARDHANDLE hCard);

      PCSC_API LONG SCardEndTransaction(SCARDHANDLE hCard, DWORD dwDisposition);

      PCSC_API LONG SCardStatus(SCARDHANDLE hCard,
            /*@null@*/ /*@out@*/ LPSTR mszReaderName,
            /*@null@*/ /*@out@*/ LPDWORD pcchReaderLen,
            /*@null@*/ /*@out@*/ LPDWORD pdwState,
            /*@null@*/ /*@out@*/ LPDWORD pdwProtocol,
            /*@null@*/ /*@out@*/ LPBYTE pbAtr,
            /*@null@*/ /*@out@*/ LPDWORD pcbAtrLen);

      PCSC_API LONG SCardGetStatusChange(SCARDCONTEXT hContext,
            DWORD dwTimeout,
            SCARD_READERSTATE *rgReaderStates, DWORD cReaders);

      PCSC_API LONG SCardControl(SCARDHANDLE hCard, DWORD dwControlCode,
            LPCVOID pbSendBuffer, DWORD cbSendLength,
            /*@out@*/ LPVOID pbRecvBuffer, DWORD cbRecvLength,
            LPDWORD lpBytesReturned);

      PCSC_API LONG SCardTransmit(SCARDHANDLE hCard,
            const SCARD_IO_REQUEST *pioSendPci,
            LPCBYTE pbSendBuffer, DWORD cbSendLength,
            /*@out@*/ SCARD_IO_REQUEST *pioRecvPci,
            /*@out@*/ LPBYTE pbRecvBuffer, LPDWORD pcbRecvLength);

      PCSC_API LONG SCardListReaderGroups(SCARDCONTEXT hContext,
            /*@out@*/ LPSTR mszGroups, LPDWORD pcchGroups);

      PCSC_API LONG SCardListReaders(SCARDCONTEXT hContext,
            /*@null@*/ /*@out@*/ LPCSTR mszGroups,
            /*@null@*/ /*@out@*/ LPSTR mszReaders,
            /*@out@*/ LPDWORD pcchReaders);

      PCSC_API LONG SCardFreeMemory(SCARDCONTEXT hContext, LPCVOID pvMem);

      PCSC_API LONG SCardCancel(SCARDCONTEXT hContext);

      PCSC_API LONG SCardGetAttrib(SCARDHANDLE hCard, DWORD dwAttrId,
            /*@out@*/ LPBYTE pbAttr, LPDWORD pcbAttrLen);

      PCSC_API LONG SCardSetAttrib(SCARDHANDLE hCard, DWORD dwAttrId,
            LPCBYTE pbAttr, DWORD cbAttrLen);

#ifdef __cplusplus
}
#endif

#endif

