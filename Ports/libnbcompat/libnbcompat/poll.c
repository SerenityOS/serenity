/*	$NetBSD: poll.c,v 1.3 2008/04/29 05:46:08 martin Exp $	*/

/*-
 * Copyright (c) 2003 The NetBSD Foundation, Inc.
 * All rights reserved.
 *
 * This code is derived from software contributed to The NetBSD Foundation
 * by Charles Blundell.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#include <nbcompat.h>
#include <nbcompat/types.h>
#include <nbcompat/time.h>
#include <nbcompat/unistd.h>
#include <nbcompat/poll.h>

int
poll(struct pollfd *p, nfds_t nfds, int timout)
{
	fd_set read, write, except;
	struct timeval tv;
	nfds_t i;
	int highfd, rval;

	/*
	 * select cannot tell us much wrt POLL*BAND, POLLPRI, POLLHUP or
	 * POLLNVAL.
	 */
	FD_ZERO(&read);
	FD_ZERO(&write);
	FD_ZERO(&except);

	highfd = -1;
	for (i = 0; i < nfds; i++) {
		if (p[i].fd < 0)
			continue;
		if (p[i].fd >= FD_SETSIZE) {
			errno = EINVAL;
			return -1;
		}
		if (p[i].fd > highfd)
			highfd = p[i].fd;

		if (p[i].events & (POLLIN|POLLRDNORM|POLLRDBAND|POLLPRI))
			FD_SET(p[i].fd, &read);
		if (p[i].events & (POLLOUT|POLLWRNORM|POLLWRBAND))
			FD_SET(p[i].fd, &write);
		FD_SET(p[i].fd, &except);
	}

	tv.tv_sec = timout / 1000;
	tv.tv_usec = (timout % 1000) * 1000;

	rval = select(highfd + 1, &read, &write, &except,
		timout == -1 ? NULL : &tv);
	if (rval <= 0)
		return rval;

	rval = 0;
	for (i = 0; i < nfds; i++) {
		p[i].revents = 0;
		if (FD_ISSET(p[i].fd, &read))
			p[i].revents |= POLLIN|POLLRDNORM|POLLRDBAND|POLLPRI;
		if (FD_ISSET(p[i].fd, &write))
			p[i].revents |= POLLOUT|POLLWRNORM|POLLWRBAND;
		if (FD_ISSET(p[i].fd, &except))
			p[i].revents |= POLLERR;
		/* XXX: POLLHUP/POLLNVAL? */
		if (p[i].revents != 0)
			rval++;
	}
	return rval;
}
