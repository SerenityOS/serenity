#pragma once

#include <string.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

/* Do what newlib does to appease GCC's --with-newlib option. */
#define _U 01
#define _L 02
#define _N 04
#define _S 010
#define _P 020
#define _C 040
#define _X 0100
#define _B 0200

extern const char _ctype_[256];

int tolower(int);
int toupper(int);
int isalnum(int);
int isalpha(int);
int iscntrl(int);
int isdigit(int);
int isxdigit(int);
int isspace(int);
int ispunct(int);
int isprint(int);
int isgraph(int);
int islower(int);
int isupper(int);

#define isalnum(c) (_ctype_[(int)(c)] & (_U | _L | _N))
#define isalpha(c) (_ctype_[(int)(c)] & (_U | _L))
#define iscntrl(c) (_ctype_[(int)(c)] & (_C))
#define isdigit(c) (_ctype_[(int)(c)] & (_N))
#define isxdigit(c) (_ctype_[(int)(c)] & (_N | _X))
#define isspace(c) (_ctype_[(int)(c)] & (_S))
#define ispunct(c) (_ctype_[(int)(c)] & (_P))
#define isprint(c) (_ctype_[(int)(c)] & (_P | _U | _L | _N | _B))
#define isgraph(c) (_ctype_[(int)(c)] & (_P | _U | _L | _N))
#define islower(c) ((_ctype_[(int)(c)] & (_U | _L)) == _L)
#define isupper(c) ((_ctype_[(int)(c)] & (_U | _L)) == _U)

#define isascii(c) ((unsigned)c <= 127)
#define toascii(c) ((c)&127)

__END_DECLS
