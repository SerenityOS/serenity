/*	$NetBSD: sha1.h,v 1.6 2008/10/08 14:28:14 joerg Exp $	*/

/*
 * SHA-1 in C
 * By Steve Reid <steve@edmweb.com>
 * 100% Public Domain
 */

#ifndef _NBCOMPAT_SYS_SHA1_H_
#define	_NBCOMPAT_SYS_SHA1_H_

#include <nbcompat/types.h>

#define SHA1_DIGEST_LENGTH		20
#define SHA1_DIGEST_STRING_LENGTH	41

typedef struct {
	uint32_t state[5];
	uint32_t count[2];  
	unsigned char buffer[64];
} SHA1_CTX;
  
__BEGIN_DECLS
void	SHA1Transform(uint32_t state[5], const unsigned char buffer[64]);
void	SHA1Init(SHA1_CTX *context);
void	SHA1Update(SHA1_CTX *context, const unsigned char *data, unsigned int len);
void	SHA1Final(unsigned char digest[20], SHA1_CTX *context);
char	*SHA1End(SHA1_CTX *, char *);
char	*SHA1File(char *, char *);
char	*SHA1Data(const unsigned char *, size_t, char *);
__END_DECLS

#endif /* _NBCOMPAT_SYS_SHA1_H_ */
