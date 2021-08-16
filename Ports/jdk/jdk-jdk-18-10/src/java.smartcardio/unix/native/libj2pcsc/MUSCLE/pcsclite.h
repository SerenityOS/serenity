/*
 * MUSCLE SmartCard Development ( https://pcsclite.apdu.fr/ )
 *
 * Copyright (C) 1999-2004
 *  David Corcoran <corcoran@musclecard.com>
 * Copyright (C) 2002-2011
 *  Ludovic Rousseau <ludovic.rousseau@free.fr>
 * Copyright (C) 2005
 *  Martin Paljak <martin@paljak.pri.ee>
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
 * @brief This keeps a list of defines for pcsc-lite.
 *
 * Error codes from http://msdn.microsoft.com/en-us/library/aa924526.aspx
 */

#ifndef __pcsclite_h__
#define __pcsclite_h__

#include <wintypes.h>

#ifdef __cplusplus
extern "C"
{
#endif

typedef LONG SCARDCONTEXT; /**< \p hContext returned by SCardEstablishContext() */
typedef SCARDCONTEXT *PSCARDCONTEXT;
typedef SCARDCONTEXT *LPSCARDCONTEXT;
typedef LONG SCARDHANDLE; /**< \p hCard returned by SCardConnect() */
typedef SCARDHANDLE *PSCARDHANDLE;
typedef SCARDHANDLE *LPSCARDHANDLE;

#define MAX_ATR_SIZE                  33      /**< Maximum ATR size */

/* Set structure elements aligment on bytes
 * http://gcc.gnu.org/onlinedocs/gcc/Structure_002dPacking-Pragmas.html */
#ifdef __APPLE__
#pragma pack(1)
#endif

typedef struct
{
      const char *szReader;
      void *pvUserData;
      DWORD dwCurrentState;
      DWORD dwEventState;
      DWORD cbAtr;
      unsigned char rgbAtr[MAX_ATR_SIZE];
}
SCARD_READERSTATE, *LPSCARD_READERSTATE;

/** Protocol Control Information (PCI) */
typedef struct
{
      unsigned long dwProtocol;      /**< Protocol identifier */
      unsigned long cbPciLength;      /**< Protocol Control Inf Length */
}
SCARD_IO_REQUEST, *PSCARD_IO_REQUEST, *LPSCARD_IO_REQUEST;

typedef const SCARD_IO_REQUEST *LPCSCARD_IO_REQUEST;

extern const SCARD_IO_REQUEST g_rgSCardT0Pci, g_rgSCardT1Pci, g_rgSCardRawPci;

/* restore default structure elements alignment */
#ifdef __APPLE__
#pragma pack()
#endif

#define SCARD_PCI_T0      (&g_rgSCardT0Pci) /**< protocol control information (PCI) for T=0 */
#define SCARD_PCI_T1      (&g_rgSCardT1Pci) /**< protocol control information (PCI) for T=1 */
#define SCARD_PCI_RAW      (&g_rgSCardRawPci) /**< protocol control information (PCI) for RAW protocol */

/**
 * @defgroup ErrorCodes ErrorCodes
 * @brief Error code documentation
 *
 * The error codes descriptions are from
 * http://msdn.microsoft.com/en-us/library/aa924526.aspx
 */
/** @ingroup ErrorCodes */
#define SCARD_S_SUCCESS                  ((LONG)0x00000000) /**< No error was encountered. */
/** @ingroup ErrorCodes */
#define SCARD_F_INTERNAL_ERROR            ((LONG)0x80100001) /**< An internal consistency check failed. */
/** @ingroup ErrorCodes */
#define SCARD_E_CANCELLED            ((LONG)0x80100002) /**< The action was cancelled by an SCardCancel request. */
/** @ingroup ErrorCodes */
#define SCARD_E_INVALID_HANDLE            ((LONG)0x80100003) /**< The supplied handle was invalid. */
/** @ingroup ErrorCodes */
#define SCARD_E_INVALID_PARAMETER      ((LONG)0x80100004) /**< One or more of the supplied parameters could not be properly interpreted. */
/** @ingroup ErrorCodes */
#define SCARD_E_INVALID_TARGET            ((LONG)0x80100005) /**< Registry startup information is missing or invalid. */
/** @ingroup ErrorCodes */
#define SCARD_E_NO_MEMORY            ((LONG)0x80100006) /**< Not enough memory available to complete this command. */
/** @ingroup ErrorCodes */
#define SCARD_F_WAITED_TOO_LONG            ((LONG)0x80100007) /**< An internal consistency timer has expired. */
/** @ingroup ErrorCodes */
#define SCARD_E_INSUFFICIENT_BUFFER      ((LONG)0x80100008) /**< The data buffer to receive returned data is too small for the returned data. */
/** @ingroup ErrorCodes */
#define SCARD_E_UNKNOWN_READER            ((LONG)0x80100009) /**< The specified reader name is not recognized. */
/** @ingroup ErrorCodes */
#define SCARD_E_TIMEOUT                  ((LONG)0x8010000A) /**< The user-specified timeout value has expired. */
/** @ingroup ErrorCodes */
#define SCARD_E_SHARING_VIOLATION      ((LONG)0x8010000B) /**< The smart card cannot be accessed because of other connections outstanding. */
/** @ingroup ErrorCodes */
#define SCARD_E_NO_SMARTCARD            ((LONG)0x8010000C) /**< The operation requires a Smart Card, but no Smart Card is currently in the device. */
/** @ingroup ErrorCodes */
#define SCARD_E_UNKNOWN_CARD            ((LONG)0x8010000D) /**< The specified smart card name is not recognized. */
/** @ingroup ErrorCodes */
#define SCARD_E_CANT_DISPOSE            ((LONG)0x8010000E) /**< The system could not dispose of the media in the requested manner. */
/** @ingroup ErrorCodes */
#define SCARD_E_PROTO_MISMATCH            ((LONG)0x8010000F) /**< The requested protocols are incompatible with the protocol currently in use with the smart card. */
/** @ingroup ErrorCodes */
#define SCARD_E_NOT_READY            ((LONG)0x80100010) /**< The reader or smart card is not ready to accept commands. */
/** @ingroup ErrorCodes */
#define SCARD_E_INVALID_VALUE            ((LONG)0x80100011) /**< One or more of the supplied parameters values could not be properly interpreted. */
/** @ingroup ErrorCodes */
#define SCARD_E_SYSTEM_CANCELLED      ((LONG)0x80100012) /**< The action was cancelled by the system, presumably to log off or shut down. */
/** @ingroup ErrorCodes */
#define SCARD_F_COMM_ERROR            ((LONG)0x80100013) /**< An internal communications error has been detected. */
/** @ingroup ErrorCodes */
#define SCARD_F_UNKNOWN_ERROR            ((LONG)0x80100014) /**< An internal error has been detected, but the source is unknown. */
/** @ingroup ErrorCodes */
#define SCARD_E_INVALID_ATR            ((LONG)0x80100015) /**< An ATR obtained from the registry is not a valid ATR string. */
/** @ingroup ErrorCodes */
#define SCARD_E_NOT_TRANSACTED            ((LONG)0x80100016) /**< An attempt was made to end a non-existent transaction. */
/** @ingroup ErrorCodes */
#define SCARD_E_READER_UNAVAILABLE      ((LONG)0x80100017) /**< The specified reader is not currently available for use. */
/** @ingroup ErrorCodes */
#define SCARD_P_SHUTDOWN            ((LONG)0x80100018) /**< The operation has been aborted to allow the server application to exit. */
/** @ingroup ErrorCodes */
#define SCARD_E_PCI_TOO_SMALL            ((LONG)0x80100019) /**< The PCI Receive buffer was too small. */
/** @ingroup ErrorCodes */
#define SCARD_E_READER_UNSUPPORTED      ((LONG)0x8010001A) /**< The reader driver does not meet minimal requirements for support. */
/** @ingroup ErrorCodes */
#define SCARD_E_DUPLICATE_READER      ((LONG)0x8010001B) /**< The reader driver did not produce a unique reader name. */
/** @ingroup ErrorCodes */
#define SCARD_E_CARD_UNSUPPORTED      ((LONG)0x8010001C) /**< The smart card does not meet minimal requirements for support. */
/** @ingroup ErrorCodes */
#define SCARD_E_NO_SERVICE            ((LONG)0x8010001D) /**< The Smart card resource manager is not running. */
/** @ingroup ErrorCodes */
#define SCARD_E_SERVICE_STOPPED            ((LONG)0x8010001E) /**< The Smart card resource manager has shut down. */
/** @ingroup ErrorCodes */
#define SCARD_E_UNEXPECTED            ((LONG)0x8010001F) /**< An unexpected card error has occurred. */
/** @ingroup ErrorCodes */
#define SCARD_E_UNSUPPORTED_FEATURE      ((LONG)0x8010001F) /**< This smart card does not support the requested feature. */
/** @ingroup ErrorCodes */
#define SCARD_E_ICC_INSTALLATION      ((LONG)0x80100020) /**< No primary provider can be found for the smart card. */
/** @ingroup ErrorCodes */
#define SCARD_E_ICC_CREATEORDER            ((LONG)0x80100021) /**< The requested order of object creation is not supported. */
/** @ingroup ErrorCodes */
/* #define SCARD_E_UNSUPPORTED_FEATURE      ((LONG)0x80100022) / **< This smart card does not support the requested feature. */
/** @ingroup ErrorCodes */
#define SCARD_E_DIR_NOT_FOUND            ((LONG)0x80100023) /**< The identified directory does not exist in the smart card. */
/** @ingroup ErrorCodes */
#define SCARD_E_FILE_NOT_FOUND            ((LONG)0x80100024) /**< The identified file does not exist in the smart card. */
/** @ingroup ErrorCodes */
#define SCARD_E_NO_DIR                  ((LONG)0x80100025) /**< The supplied path does not represent a smart card directory. */
/** @ingroup ErrorCodes */
#define SCARD_E_NO_FILE                  ((LONG)0x80100026) /**< The supplied path does not represent a smart card file. */
/** @ingroup ErrorCodes */
#define SCARD_E_NO_ACCESS            ((LONG)0x80100027) /**< Access is denied to this file. */
/** @ingroup ErrorCodes */
#define SCARD_E_WRITE_TOO_MANY            ((LONG)0x80100028) /**< The smart card does not have enough memory to store the information. */
/** @ingroup ErrorCodes */
#define SCARD_E_BAD_SEEK            ((LONG)0x80100029) /**< There was an error trying to set the smart card file object pointer. */
/** @ingroup ErrorCodes */
#define SCARD_E_INVALID_CHV            ((LONG)0x8010002A) /**< The supplied PIN is incorrect. */
/** @ingroup ErrorCodes */
#define SCARD_E_UNKNOWN_RES_MNG            ((LONG)0x8010002B) /**< An unrecognized error code was returned from a layered component. */
/** @ingroup ErrorCodes */
#define SCARD_E_NO_SUCH_CERTIFICATE      ((LONG)0x8010002C) /**< The requested certificate does not exist. */
/** @ingroup ErrorCodes */
#define SCARD_E_CERTIFICATE_UNAVAILABLE      ((LONG)0x8010002D) /**< The requested certificate could not be obtained. */
/** @ingroup ErrorCodes */
#define SCARD_E_NO_READERS_AVAILABLE    ((LONG)0x8010002E) /**< Cannot find a smart card reader. */
/** @ingroup ErrorCodes */
#define SCARD_E_COMM_DATA_LOST            ((LONG)0x8010002F) /**< A communications error with the smart card has been detected. Retry the operation. */
/** @ingroup ErrorCodes */
#define SCARD_E_NO_KEY_CONTAINER      ((LONG)0x80100030) /**< The requested key container does not exist on the smart card. */
/** @ingroup ErrorCodes */
#define SCARD_E_SERVER_TOO_BUSY            ((LONG)0x80100031) /**< The Smart Card Resource Manager is too busy to complete this operation. */

/** @ingroup ErrorCodes */
#define SCARD_W_UNSUPPORTED_CARD      ((LONG)0x80100065) /**< The reader cannot communicate with the card, due to ATR string configuration conflicts. */
/** @ingroup ErrorCodes */
#define SCARD_W_UNRESPONSIVE_CARD      ((LONG)0x80100066) /**< The smart card is not responding to a reset. */
/** @ingroup ErrorCodes */
#define SCARD_W_UNPOWERED_CARD            ((LONG)0x80100067) /**< Power has been removed from the smart card, so that further communication is not possible. */
/** @ingroup ErrorCodes */
#define SCARD_W_RESET_CARD            ((LONG)0x80100068) /**< The smart card has been reset, so any shared state information is invalid. */
/** @ingroup ErrorCodes */
#define SCARD_W_REMOVED_CARD            ((LONG)0x80100069) /**< The smart card has been removed, so further communication is not possible. */

/** @ingroup ErrorCodes */
#define SCARD_W_SECURITY_VIOLATION      ((LONG)0x8010006A) /**< Access was denied because of a security violation. */
/** @ingroup ErrorCodes */
#define SCARD_W_WRONG_CHV            ((LONG)0x8010006B) /**< The card cannot be accessed because the wrong PIN was presented. */
/** @ingroup ErrorCodes */
#define SCARD_W_CHV_BLOCKED            ((LONG)0x8010006C) /**< The card cannot be accessed because the maximum number of PIN entry attempts has been reached. */
/** @ingroup ErrorCodes */
#define SCARD_W_EOF                  ((LONG)0x8010006D) /**< The end of the smart card file has been reached. */
/** @ingroup ErrorCodes */
#define SCARD_W_CANCELLED_BY_USER      ((LONG)0x8010006E) /**< The user pressed "Cancel" on a Smart Card Selection Dialog. */
/** @ingroup ErrorCodes */
#define SCARD_W_CARD_NOT_AUTHENTICATED      ((LONG)0x8010006F) /**< No PIN was presented to the smart card. */

#define SCARD_AUTOALLOCATE (DWORD)(-1)      /**< see SCardFreeMemory() */
#define SCARD_SCOPE_USER            0x0000      /**< Scope in user space */
#define SCARD_SCOPE_TERMINAL            0x0001      /**< Scope in terminal */
#define SCARD_SCOPE_SYSTEM            0x0002      /**< Scope in system */
#define SCARD_SCOPE_GLOBAL            0x0003      /**< Scope is global */

#define SCARD_PROTOCOL_UNDEFINED      0x0000      /**< protocol not set */
#define SCARD_PROTOCOL_UNSET SCARD_PROTOCOL_UNDEFINED      /* backward compat */
#define SCARD_PROTOCOL_T0            0x0001      /**< T=0 active protocol. */
#define SCARD_PROTOCOL_T1            0x0002      /**< T=1 active protocol. */
#define SCARD_PROTOCOL_RAW            0x0004      /**< Raw active protocol. */
#define SCARD_PROTOCOL_T15            0x0008      /**< T=15 protocol. */

#define SCARD_PROTOCOL_ANY            (SCARD_PROTOCOL_T0|SCARD_PROTOCOL_T1)      /**< IFD determines prot. */

#define SCARD_SHARE_EXCLUSIVE            0x0001      /**< Exclusive mode only */
#define SCARD_SHARE_SHARED            0x0002      /**< Shared mode only */
#define SCARD_SHARE_DIRECT            0x0003      /**< Raw mode only */

#define SCARD_LEAVE_CARD            0x0000      /**< Do nothing on close */
#define SCARD_RESET_CARD            0x0001      /**< Reset on close */
#define SCARD_UNPOWER_CARD            0x0002      /**< Power down on close */
#define SCARD_EJECT_CARD            0x0003      /**< Eject on close */

#define SCARD_UNKNOWN                  0x0001      /**< Unknown state */
#define SCARD_ABSENT                  0x0002      /**< Card is absent */
#define SCARD_PRESENT                  0x0004      /**< Card is present */
#define SCARD_SWALLOWED                  0x0008      /**< Card not powered */
#define SCARD_POWERED                  0x0010      /**< Card is powered */
#define SCARD_NEGOTIABLE            0x0020      /**< Ready for PTS */
#define SCARD_SPECIFIC                  0x0040      /**< PTS has been set */

#define SCARD_STATE_UNAWARE            0x0000      /**< App wants status */
#define SCARD_STATE_IGNORE            0x0001      /**< Ignore this reader */
#define SCARD_STATE_CHANGED            0x0002      /**< State has changed */
#define SCARD_STATE_UNKNOWN            0x0004      /**< Reader unknown */
#define SCARD_STATE_UNAVAILABLE            0x0008      /**< Status unavailable */
#define SCARD_STATE_EMPTY            0x0010      /**< Card removed */
#define SCARD_STATE_PRESENT            0x0020      /**< Card inserted */
#define SCARD_STATE_ATRMATCH            0x0040      /**< ATR matches card */
#define SCARD_STATE_EXCLUSIVE            0x0080      /**< Exclusive Mode */
#define SCARD_STATE_INUSE            0x0100      /**< Shared Mode */
#define SCARD_STATE_MUTE            0x0200      /**< Unresponsive card */
#define SCARD_STATE_UNPOWERED            0x0400      /**< Unpowered card */

#ifndef INFINITE
#define INFINITE                  0xFFFFFFFF      /**< Infinite timeout */
#endif

#define PCSCLITE_VERSION_NUMBER            "1.8.26"      /**< Current version */
/** Maximum readers context (a slot is count as a reader) */
#define PCSCLITE_MAX_READERS_CONTEXTS                  16

#define MAX_READERNAME                  128

#ifndef SCARD_ATR_LENGTH
#define SCARD_ATR_LENGTH            MAX_ATR_SIZE      /**< Maximum ATR size */
#endif

/*
 * The message and buffer sizes must be multiples of 16.
 * The max message size must be at least large enough
 * to accomodate the transmit_struct
 */
#define MAX_BUFFER_SIZE                  264      /**< Maximum Tx/Rx Buffer for short APDU */
#define MAX_BUFFER_SIZE_EXTENDED      (4 + 3 + (1<<16) + 3 + 2)      /**< enhanced (64K + APDU + Lc + Le + SW) Tx/Rx Buffer */

/*
 * Gets a stringified error response
 */
const char *pcsc_stringify_error(const LONG);

#ifdef __cplusplus
}
#endif

#endif
