/*	$NetBSD: build.c,v 1.17 2017/04/19 21:42:50 joerg Exp $	*/

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
__RCSID("$NetBSD: build.c,v 1.17 2017/04/19 21:42:50 joerg Exp $");

/*-
 * Copyright (c) 2007 Joerg Sonnenberger <joerg@NetBSD.org>.
 * All rights reserved.
 *
 * This code was developed as part of Google's Summer of Code 2007 program.
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

/*
 * FreeBSD install - a package for the installation and maintainance
 * of non-core utilities.
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
 * Jordan K. Hubbard
 * 18 July 1993
 *
 * This is the main body of the create module.
 *
 */

#include "lib.h"
#include "create.h"

#if HAVE_ERR_H
#include <err.h>
#endif
#if HAVE_GRP_H
#include <grp.h>
#endif
#if HAVE_PWD_H
#include <pwd.h>
#endif
#if HAVE_UNISTD_H
#include <unistd.h>
#endif
#if HAVE_FCNTL_H
#include <fcntl.h>
#endif

#include <archive.h>
#include <archive_entry.h>

static struct memory_file *contents_file;
static struct memory_file *comment_file;
static struct memory_file *desc_file;
static struct memory_file *install_file;
static struct memory_file *deinstall_file;
static struct memory_file *display_file;
static struct memory_file *build_version_file;
static struct memory_file *build_info_file;
static struct memory_file *size_pkg_file;
static struct memory_file *size_all_file;
static struct memory_file *preserve_file;

static void
write_meta_file(struct memory_file *file, struct archive *archive)
{
	struct archive_entry *entry;

	entry = archive_entry_new();
	archive_entry_set_pathname(entry, file->name);
	archive_entry_copy_stat(entry, &file->st);

	archive_entry_set_uname(entry, file->owner);
	archive_entry_set_gname(entry, file->group);

	if (archive_write_header(archive, entry))
		errx(2, "cannot write to archive: %s", archive_error_string(archive));

	archive_write_data(archive, file->data, file->len);

	archive_entry_free(entry);
}

static void
write_entry(struct archive *archive, struct archive_entry *entry)
{
	char buf[16384];
	const char *name;
	int fd;
	off_t len;
	ssize_t buf_len;

	if (archive_entry_pathname(entry) == NULL) {
		warnx("entry with NULL path");
		return;
	}

	if (archive_write_header(archive, entry)) {
		errx(2, "cannot write %s to archive: %s",
		    archive_entry_pathname(entry),
		    archive_error_string(archive));
	}

	/* Only regular files can have data. */
	if (archive_entry_filetype(entry) != AE_IFREG ||
	    archive_entry_size(entry) == 0) {
		archive_entry_free(entry);
		return;
	}

	name = archive_entry_pathname(entry);

	if ((fd = open(name, O_RDONLY)) == -1)
		err(2, "cannot open data file %s", name);

	len = archive_entry_size(entry);

	while (len > 0) {
		buf_len = (len > (off_t)sizeof(buf)) ? (ssize_t)sizeof(buf) : (ssize_t)len;

		if ((buf_len = read(fd, buf, buf_len)) == 0)
			break;
		else if (buf_len < 0)
			err(2, "cannot read from %s", name);

		archive_write_data(archive, buf, (size_t)buf_len);
		len -= buf_len;
	}

	close(fd);

	archive_entry_free(entry);
}

static void
write_normal_file(const char *name, struct archive *archive,
    struct archive_entry_linkresolver *resolver,
    const char *owner, const char *group)
{
	char buf[16384];
	ssize_t buf_len;
	struct archive_entry *entry, *sparse_entry;
	struct stat st;

	if (lstat(name, &st) == -1)
		err(2, "lstat failed for file %s", name);

	entry = archive_entry_new();
	archive_entry_set_pathname(entry, name);
	archive_entry_copy_stat(entry, &st);

	if (owner != NULL) {
		uid_t uid;

		archive_entry_set_uname(entry, owner);
		if (uid_from_user(owner, &uid) == -1)
			errx(2, "user %s unknown", owner);
		archive_entry_set_uid(entry, uid);
	} else {
		archive_entry_set_uname(entry, user_from_uid(st.st_uid, 1));
	}

	if (group != NULL) {
		gid_t gid;

		archive_entry_set_gname(entry, group);
		if (gid_from_group(group, &gid) == -1)
			errx(2, "group %s unknown", group);
		archive_entry_set_gid(entry, gid);
	} else {
		archive_entry_set_gname(entry, group_from_gid(st.st_gid, 1));
	}

	if ((st.st_mode & S_IFMT) == S_IFLNK) {
		buf_len = readlink(name, buf, sizeof buf);
		if (buf_len < 0)
			err(2, "cannot read symlink %s", name);
		buf[buf_len] = '\0';
		archive_entry_set_symlink(entry, buf);
	}

	archive_entry_linkify(resolver, &entry, &sparse_entry);

	if (entry != NULL)
		write_entry(archive, entry);
	if (sparse_entry != NULL)
		write_entry(archive, sparse_entry);
}

static void
make_dist(const char *pkg, const char *suffix, const package_t *plist)
{
	char *archive_name;
	const char *owner, *group;
	const plist_t *p;
	struct archive *archive;
	struct archive_entry *entry, *sparse_entry;
	struct archive_entry_linkresolver *resolver;
	char *initial_cwd;
	
	archive = archive_write_new();
	archive_write_set_format_pax_restricted(archive);
	archive_write_set_options(archive, "hdrcharset=BINARY");
	if ((resolver = archive_entry_linkresolver_new()) == NULL)
		errx(2, "cannot create link resolver");
	archive_entry_linkresolver_set_strategy(resolver,
	    archive_format(archive));

	if (CompressionType == NULL) {
		if (strcmp(suffix, "tbz") == 0 ||
		    strcmp(suffix, "tar.bz2") == 0)
			CompressionType = "bzip2";
		else if (strcmp(suffix, "tgz") == 0 ||
		    strcmp(suffix, "tar.gz") == 0)
			CompressionType = "gzip";
		else
			CompressionType = "none";
	}

	if (strcmp(CompressionType, "bzip2") == 0)
		archive_write_add_filter_bzip2(archive);
	else if (strcmp(CompressionType, "gzip") == 0)
		archive_write_add_filter_gzip(archive);
	else if (strcmp(CompressionType, "xz") == 0)
		archive_write_add_filter_xz(archive);
	else if (strcmp(CompressionType, "none") != 0)
		errx(1, "Unspported compression type for -F: %s",
		    CompressionType);

	archive_name = xasprintf("%s.%s", pkg, suffix);

	if (archive_write_open_filename(archive, archive_name))
		errx(2, "cannot create archive: %s", archive_error_string(archive));

	free(archive_name);

	owner = DefaultOwner;
	group = DefaultGroup;

	write_meta_file(contents_file, archive);
	write_meta_file(comment_file, archive);
	write_meta_file(desc_file, archive);

	if (Install)
		write_meta_file(install_file, archive);
	if (DeInstall)
		write_meta_file(deinstall_file, archive);
	if (Display)
		write_meta_file(display_file, archive);
	if (BuildVersion)
		write_meta_file(build_version_file, archive);
	if (BuildInfo)
		write_meta_file(build_info_file, archive);
	if (SizePkg)
		write_meta_file(size_pkg_file, archive);
	if (SizeAll)
		write_meta_file(size_all_file, archive);
	if (Preserve)
		write_meta_file(preserve_file, archive);

	initial_cwd = getcwd(NULL, 0);

	for (p = plist->head; p; p = p->next) {
		if (p->type == PLIST_FILE) {
			write_normal_file(p->name, archive, resolver, owner, group);
		} else if (p->type == PLIST_CWD) {
			chdir(p->name);
		} else if (p->type == PLIST_IGNORE) {
			p = p->next;
		} else if (p->type == PLIST_CHOWN) {
			if (p->name != NULL)
				owner = p->name;
			else
				owner = DefaultOwner;
		} else if (p->type == PLIST_CHGRP) {
			if (p->name != NULL)
				group = p->name;
			else
				group = DefaultGroup;
		}
	}

	entry = NULL;
	archive_entry_linkify(resolver, &entry, &sparse_entry);
	while (entry != NULL) {
		write_entry(archive, entry);
		entry = NULL;
		archive_entry_linkify(resolver, &entry, &sparse_entry);
	}

	archive_entry_linkresolver_free(resolver);

	if (archive_write_free(archive))
		errx(2, "cannot finish archive: %s", archive_error_string(archive));

	free(initial_cwd);
}

static struct memory_file *
load_and_add(package_t *plist, const char *input_name,
    const char *target_name, mode_t perm)
{
	struct memory_file *file;

	file = load_memory_file(input_name, target_name, DefaultOwner,
	    DefaultGroup, perm);
	add_plist(plist, PLIST_IGNORE, NULL);
	add_plist(plist, PLIST_FILE, target_name);

	return file;
}

static struct memory_file *
make_and_add(package_t *plist, const char *target_name,
    char *content, mode_t perm)
{
	struct memory_file *file;

	file = make_memory_file(target_name, content, strlen(content),
	    DefaultOwner, DefaultGroup, perm);
	add_plist(plist, PLIST_IGNORE, NULL);
	add_plist(plist, PLIST_FILE, target_name);

	return file;
}

int
pkg_build(const char *pkg, const char *full_pkg, const char *suffix,
    package_t *plist)
{
	char *plist_buf;
	size_t plist_len;

	/* Now put the release specific items in */
	add_plist(plist, PLIST_CWD, ".");
	comment_file = make_and_add(plist, COMMENT_FNAME, Comment, 0444);
	desc_file = make_and_add(plist, DESC_FNAME, Desc, 0444);

	if (Install) {
		install_file = load_and_add(plist, Install, INSTALL_FNAME,
		    0555);
	}
	if (DeInstall) {
		deinstall_file = load_and_add(plist, DeInstall,
		    DEINSTALL_FNAME, 0555);
	}
	if (Display) {
		display_file = load_and_add(plist, Display,
		    DISPLAY_FNAME, 0444);
		add_plist(plist, PLIST_DISPLAY, DISPLAY_FNAME);
	}
	if (BuildVersion) {
		build_version_file = load_and_add(plist, BuildVersion,
		    BUILD_VERSION_FNAME, 0444);
	}
	if (BuildInfo) {
		build_info_file = load_and_add(plist, BuildInfo,
		    BUILD_INFO_FNAME, 0444);
	}
	if (SizePkg) {
		size_pkg_file = load_and_add(plist, SizePkg,
		    SIZE_PKG_FNAME, 0444);
	}
	if (SizeAll) {
		size_all_file = load_and_add(plist, SizeAll,
		    SIZE_ALL_FNAME, 0444);
	}
	if (Preserve) {
		preserve_file = load_and_add(plist, Preserve,
		    PRESERVE_FNAME, 0444);
	}

	/* Finally, write out the packing list */
	stringify_plist(plist, &plist_buf, &plist_len, realprefix);
	contents_file = make_memory_file(CONTENTS_FNAME, plist_buf, plist_len,
	    DefaultOwner, DefaultGroup, 0644);

	/* And stick it into a tar ball */
	make_dist(pkg, suffix, plist);

	return TRUE;		/* Success */
}
