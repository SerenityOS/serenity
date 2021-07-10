/* $NetBSD: pread.c,v 1.1 2010/04/20 00:32:22 joerg Exp $ */

static ssize_t
working_pread(int fd, void *buf, size_t nbytes, off_t off)
{
	if (lseek(fd, off, SEEK_SET) == -1)
		return -1;
	return read(fd, buf, nbytes);
}

#undef pread
#define	pread(fd, buf, nbytes, off) working_pread(fd, buf, nbytes,off)
