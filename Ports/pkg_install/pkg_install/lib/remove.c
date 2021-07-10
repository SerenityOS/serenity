/*	$NetBSD: remove.c,v 1.3 2009/08/02 17:56:45 joerg Exp $	*/

/*-
 * Copyright (c) 2008 Joerg Sonnenberger <joerg@NetBSD.org>.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#if HAVE_CONFIG_H
#include "config.h"
#endif

#include <nbcompat.h>

#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif

__RCSID("$NetBSD: remove.c,v 1.3 2009/08/02 17:56:45 joerg Exp $");

#if HAVE_DIRENT_H
#include <dirent.h>
#endif
#if HAVE_ERR_H
#include <err.h>
#endif
#include <errno.h>
#if HAVE_FCNTL_H
#include <fcntl.h>
#endif
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "lib.h"

static int
safe_fchdir(int cwd)
{
	int tmp_errno, rv;

	tmp_errno = errno;
	rv = fchdir(cwd);
	errno = tmp_errno;

	return rv;
}

static int
long_remove(const char **path_ptr, int missing_ok, int *did_chdir)
{
	char tmp_path[PATH_MAX + 1];
	const char *path;
	size_t i, len;
	int rv;

	path = *path_ptr;
	len = strlen(path);
	*did_chdir = 0;

	while (len >= PATH_MAX) {
		for (i = PATH_MAX - 1; i > 0; --i) {
			if (path[i] == '/')
				break;
		}
		if (i == 0) {
			errno = ENAMETOOLONG;
			return -1; /* Assumes PATH_MAX > NAME_MAX */
		}
		memcpy(tmp_path, path, i);
		tmp_path[i] = '\0';
		if (chdir(tmp_path))
			return -1;
		*did_chdir = 1;
		path += i + 1;
		len -= i + 1;
	}

	if (remove(path) == 0 || (errno == ENOENT && missing_ok))
		rv = 0;
	else
		rv = -1;

	*path_ptr = path;

	return rv;
}

static int
recursive_remove_internal(const char *path, int missing_ok, int cwd)
{
	DIR *dir;
	struct dirent *de;
	const char *sub_path;
	char *subdir;
	int did_chdir, rv;

	/*
	 * If the argument is longer than PATH_MAX, long_remove
	 * will try to shorten it using chdir.  So before returning,
	 * make sure to fchdir back to the original cwd.
	 */
	sub_path = path;
	if (long_remove(&sub_path, missing_ok, &did_chdir) == 0)
		rv = 0;
	else if (errno != ENOTEMPTY) /* Other errors are terminal. */
		rv = -1;
	else
		rv = 1;

	if (rv != 1) {
		if (did_chdir && safe_fchdir(cwd) == -1 && rv == 0)
			rv = -1;
		return rv;
	}

	if ((dir = opendir(sub_path)) == NULL) {
		if (errno == EMFILE)
			warn("opendir failed");
		return -1;
	}

	if (did_chdir && fchdir(cwd) == -1)
		return -1;

	rv = 0;

	while ((de = readdir(dir)) != NULL) {
		if (strcmp(de->d_name, ".") == 0)
			continue;
		if (strcmp(de->d_name, "..") == 0)
			continue;
		subdir = xasprintf("%s/%s", path, de->d_name);
		rv = recursive_remove_internal(subdir, 1, cwd);
		free(subdir);
	}

	closedir(dir);

	safe_fchdir(cwd);

	rv |= long_remove(&path, missing_ok, &did_chdir);

	if (did_chdir && safe_fchdir(cwd) == -1 && rv == 0)
		rv = -1;

	return rv;
}

int
recursive_remove(const char *path, int missing_ok)
{
	int orig_cwd, rv;

	/* First try the easy case of regular file or empty directory. */
	if (remove(path) == 0 || (errno == ENOENT && missing_ok))
		return 0;

	/*
	 * If the path is too long, long_remove will use chdir to shorten it,
	 * so remember the current directory first.
	 */
	if ((orig_cwd = open(".", O_RDONLY)) == -1)
		return -1;

	rv = recursive_remove_internal(path, missing_ok, orig_cwd);

	close(orig_cwd);
	return rv;
}
