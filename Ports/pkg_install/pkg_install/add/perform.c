/*	$NetBSD: perform.c,v 1.120 2021/04/10 20:10:48 nia Exp $	*/
#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
__RCSID("$NetBSD: perform.c,v 1.120 2021/04/10 20:10:48 nia Exp $");

/*-
 * Copyright (c) 2003 Grant Beattie <grant@NetBSD.org>
 * Copyright (c) 2005 Dieter Baron <dillo@NetBSD.org>
 * Copyright (c) 2007 Roland Illig <rillig@NetBSD.org>
 * Copyright (c) 2008, 2009 Joerg Sonnenberger <joerg@NetBSD.org>
 * Copyright (c) 2010 Thomas Klausner <wiz@NetBSD.org>
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

#include <sys/utsname.h>
#include <sys/stat.h>
#if HAVE_ERR_H
#include <err.h>
#endif
#include <errno.h>
#if HAVE_FCNTL_H
#include <fcntl.h>
#endif
#include <limits.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <archive.h>
#include <archive_entry.h>

#include "lib.h"
#include "add.h"
#include "version.h"

struct pkg_meta {
	char *meta_contents;
	char *meta_comment;
	char *meta_desc;
	char *meta_mtree;
	char *meta_build_version;
	char *meta_build_info;
	char *meta_size_pkg;
	char *meta_size_all;
	char *meta_required_by;
	char *meta_display;
	char *meta_install;
	char *meta_deinstall;
	char *meta_preserve;
	char *meta_installed_info;
};

struct pkg_task {
	char *pkgname;

	const char *prefix;
	char *install_prefix;

	char *logdir;
	char *install_logdir;
	char *install_logdir_real;
	char *other_version;

	package_t plist;

	struct pkg_meta meta_data;

	struct archive *archive;
	struct archive_entry *entry;

	char *buildinfo[BI_ENUM_COUNT];

	size_t dep_length, dep_allocated;
	char **dependencies;
};

static const struct pkg_meta_desc {
	size_t entry_offset;
	const char *entry_filename;
	int required_file;
	mode_t perm;
} pkg_meta_descriptors[] = {
	{ offsetof(struct pkg_meta, meta_contents), CONTENTS_FNAME, 1, 0644 },
	{ offsetof(struct pkg_meta, meta_comment), COMMENT_FNAME, 1, 0444},
	{ offsetof(struct pkg_meta, meta_desc), DESC_FNAME, 1, 0444},
	{ offsetof(struct pkg_meta, meta_install), INSTALL_FNAME, 0, 0555 },
	{ offsetof(struct pkg_meta, meta_deinstall), DEINSTALL_FNAME, 0, 0555 },
	{ offsetof(struct pkg_meta, meta_display), DISPLAY_FNAME, 0, 0444 },
	{ offsetof(struct pkg_meta, meta_mtree), MTREE_FNAME, 0, 0444 },
	{ offsetof(struct pkg_meta, meta_build_version), BUILD_VERSION_FNAME, 0, 0444 },
	{ offsetof(struct pkg_meta, meta_build_info), BUILD_INFO_FNAME, 0, 0444 },
	{ offsetof(struct pkg_meta, meta_size_pkg), SIZE_PKG_FNAME, 0, 0444 },
	{ offsetof(struct pkg_meta, meta_size_all), SIZE_ALL_FNAME, 0, 0444 },
	{ offsetof(struct pkg_meta, meta_preserve), PRESERVE_FNAME, 0, 0444 },
	{ offsetof(struct pkg_meta, meta_required_by), REQUIRED_BY_FNAME, 0, 0644 },
	{ offsetof(struct pkg_meta, meta_installed_info), INSTALLED_INFO_FNAME, 0, 0644 },
	{ 0, NULL, 0, 0 },
};

static int pkg_do(const char *, int, int);

static int
compatible_platform(const char *opsys, const char *host, const char *package)
{
	const char *loc;
	size_t majorlen = 0;

	/*
	 * If the user has set the CHECK_OS_VERSION variable to "no" then skip any
	 * uname version checks and assume they know what they are doing.  This can
	 * be useful on OS where the kernel version is not a good indicator of
	 * userland compatibility, or differs but retains ABI compatibility.
	 */
	if (strcasecmp(check_os_version, "no") == 0)
	    return 1;

	/* returns 1 if host and package operating system match */
	if (strcmp(opsys, "NetBSD") == 0) {
		/*
		 * warn about -current package on a stable release and
		 * the reverse
		 */
		if ((strstr(host, ".99.") != NULL &&
		    strstr(package, ".99.") == NULL) ||
		    (strstr(package, ".99.") != NULL &&
		    strstr(host, ".99.") == NULL)) {
			return 0;
		}
		/* compare the major version only */
		loc = strchr(host, '.');
		if (loc != NULL) {
			majorlen = loc - host;
			if (majorlen != (size_t)(strchr(package, '.') - package))
				return 0;
			if (strncmp(host, package, majorlen) == 0)
				return 1;
		}
	}
	if (strcmp(host, package) == 0)
		return 1;
	return 0;
}

static int
mkdir_p(const char *path)
{
	char *p, *cur_end;
	int done, saved_errno;
	struct stat sb;

	/*
	 * Handle the easy case of direct success or
	 * pre-existing directory first.
	 */
	if (mkdir(path, 0777) == 0)
		return 0;
	if (stat(path, &sb) == 0) {
		if (S_ISDIR(sb.st_mode))
			return 0;
		errno = ENOTDIR;
		return -1;
	}

	cur_end = p = xstrdup(path);

	for (;;) {
		/*
		 * First skip leading slashes either from / or
		 * from the last iteration.
		 */
		cur_end += strspn(cur_end, "/");
		/* Find end of actual directory name. */
		cur_end += strcspn(cur_end, "/");

		/*
		 * Remember if this is the last component and
		 * overwrite / if needed.
		 */
		done = (*cur_end == '\0');
		*cur_end = '\0';

		if (mkdir(p, 0777) == -1) {
			saved_errno = errno;
			if (stat(p, &sb) == 0) {
				if (S_ISDIR(sb.st_mode))
					goto pass;
				errno = ENOTDIR;
			} else {
				errno = saved_errno;
			}
			free(p);
			return -1;
		}
pass:
		if (done)
			break;
		*cur_end = '/';
	}

	free(p);
	return 0;
}

/*
 * Read meta data from archive.
 * Bail out if a required entry is missing or entries are in the wrong order.
 */
static int
read_meta_data(struct pkg_task *pkg)
{
	const struct pkg_meta_desc *descr, *last_descr;
	const char *fname;
	char **target;
	int64_t size;
	int r, found_required;

	found_required = 0;

	r = ARCHIVE_OK;
	last_descr = 0;

	if (pkg->entry != NULL)
		goto skip_header;

	for (;;) {
		r = archive_read_next_header(pkg->archive, &pkg->entry);
		if (r != ARCHIVE_OK)
				break;
skip_header:
		fname = archive_entry_pathname(pkg->entry);

		for (descr = pkg_meta_descriptors; descr->entry_filename;
		     ++descr) {
			if (strcmp(descr->entry_filename, fname) == 0)
				break;
		}
		if (descr->entry_filename == NULL)
			break;

		if (descr->required_file)
			++found_required;

		target = (char **)((char *)&pkg->meta_data +
		    descr->entry_offset);
		if (*target) {
			warnx("duplicate entry, package corrupt");
			return -1;
		}
		if (descr < last_descr) {
			warnx("misordered package");
			return -1;
		}
		last_descr = descr;

		size = archive_entry_size(pkg->entry);
		if (size > SSIZE_MAX - 1) {
			warnx("package meta data too large to process");
			return -1;
		}
		*target = xmalloc(size + 1);
		if (archive_read_data(pkg->archive, *target, size) != size) {
			warnx("cannot read package meta data");
			return -1;
		}
		(*target)[size] = '\0';
	}

	if (r != ARCHIVE_OK)
		pkg->entry = NULL;
	if (r == ARCHIVE_EOF)
		r = ARCHIVE_OK;

	for (descr = pkg_meta_descriptors; descr->entry_filename; ++descr) {
		if (descr->required_file)
			--found_required;
	}

	return !found_required && r == ARCHIVE_OK ? 0 : -1;
}

/*
 * Free meta data.
 */
static void
free_meta_data(struct pkg_task *pkg)
{
	const struct pkg_meta_desc *descr;
	char **target;

	for (descr = pkg_meta_descriptors; descr->entry_filename; ++descr) {
		target = (char **)((char *)&pkg->meta_data +
		    descr->entry_offset);
		free(*target);
		*target = NULL;
	}
}

/*
 * Parse PLIST and populate pkg.
 */
static int
pkg_parse_plist(struct pkg_task *pkg)
{
	plist_t *p;

	parse_plist(&pkg->plist, pkg->meta_data.meta_contents);
	if ((p = find_plist(&pkg->plist, PLIST_NAME)) == NULL) {
		warnx("Invalid PLIST: missing @name");
		return -1;
	}
	if (pkg->pkgname == NULL)
		pkg->pkgname = xstrdup(p->name);
	else if (strcmp(pkg->pkgname, p->name) != 0) {
		warnx("Signature and PLIST differ on package name");
		return -1;
	}
	if ((p = find_plist(&pkg->plist, PLIST_CWD)) == NULL) {
		warnx("Invalid PLIST: missing @cwd");
		return -1;
	}

	if (Prefix != NULL &&
	    strcmp(p->name, Prefix) != 0) {
		size_t len;

		delete_plist(&pkg->plist, FALSE, PLIST_CWD, NULL);
		add_plist_top(&pkg->plist, PLIST_CWD, Prefix);
		free(pkg->meta_data.meta_contents);
		stringify_plist(&pkg->plist, &pkg->meta_data.meta_contents, &len,
		    Prefix);
		pkg->prefix = Prefix;
	} else
		pkg->prefix = p->name;

	if (Destdir != NULL)
		pkg->install_prefix = xasprintf("%s/%s", Destdir, pkg->prefix);
	else
		pkg->install_prefix = xstrdup(pkg->prefix);

	return 0;
}

/*
 * Helper function to extract value from a string of the
 * form key=value ending at eol.
 */
static char *
dup_value(const char *line, const char *eol)
{
	const char *key;
	char *val;

	key = strchr(line, '=');
	val = xmalloc(eol - key);
	memcpy(val, key + 1, eol - key - 1);
	val[eol - key - 1] = '\0';
	return val;
}

static int
check_already_installed(struct pkg_task *pkg)
{
	char *filename;
	int fd;

	filename = pkgdb_pkg_file(pkg->pkgname, CONTENTS_FNAME);
	fd = open(filename, O_RDONLY);
	free(filename);
	if (fd == -1)
		return 1;
	close(fd);

	if (ReplaceSame) {
		struct stat sb;

		pkg->install_logdir_real = pkg->install_logdir;
		pkg->install_logdir = xasprintf("%s.xxxxxx", pkg->install_logdir);
		if (stat(pkg->install_logdir, &sb) == 0) {
			warnx("package `%s' already has a temporary update "
			    "directory `%s', remove it manually",
			    pkg->pkgname, pkg->install_logdir);
			return -1;
		}
		return 1;
	}

	/* We can only arrive here for explicitly requested packages. */
	if (!Automatic && is_automatic_installed(pkg->pkgname)) {
		if (Fake ||
		    mark_as_automatic_installed(pkg->pkgname, 0) == 0)
			warnx("package `%s' was already installed as "
			      "dependency, now marked as installed "
			      "manually", pkg->pkgname);
	} else {
		warnx("package `%s' already recorded as installed",
		      pkg->pkgname);
	}
	return 0;

}

static int
check_other_installed(struct pkg_task *pkg)
{
	FILE *f, *f_pkg;
	size_t len;
	char *pkgbase, *iter, *filename;
	package_t plist;
	plist_t *p;
	int status;

	if (pkg->install_logdir_real) {
		pkg->other_version = xstrdup(pkg->pkgname);
		return 0;
	}

	pkgbase = xstrdup(pkg->pkgname);

	if ((iter = strrchr(pkgbase, '-')) == NULL) {
		free(pkgbase);
		warnx("Invalid package name %s", pkg->pkgname);
		return -1;
	}
	*iter = '\0';
	pkg->other_version = find_best_matching_installed_pkg(pkgbase, 0);
	free(pkgbase);
	if (pkg->other_version == NULL)
		return 0;

	if (!Replace) {
		/* XXX This is redundant to the implicit conflict check. */
		warnx("A different version of %s is already installed: %s",
		    pkg->pkgname, pkg->other_version);
		return -1;
	}

	filename = pkgdb_pkg_file(pkg->other_version, REQUIRED_BY_FNAME);
	errno = 0;
	f = fopen(filename, "r");
	free(filename);
	if (f == NULL) {
		if (errno == ENOENT) {
			/* No packages depend on this, so everything is well. */
			return 0; 
		}
		warnx("Can't open +REQUIRED_BY of %s", pkg->other_version);
		return -1;
	}

	status = 0;

	while ((iter = fgetln(f, &len)) != NULL) {
		if (iter[len - 1] == '\n')
			iter[len - 1] = '\0';
		filename = pkgdb_pkg_file(iter, CONTENTS_FNAME);
		if ((f_pkg = fopen(filename, "r")) == NULL) {
			warnx("Can't open +CONTENTS of depending package %s",
			    iter);
			fclose(f);
			return -1;
		}
		read_plist(&plist, f_pkg);
		fclose(f_pkg);
		for (p = plist.head; p != NULL; p = p->next) {
			if (p->type == PLIST_IGNORE) {
				p = p->next;
				continue;
			} else if (p->type != PLIST_PKGDEP)
				continue;
			/*
			 * XXX This is stricter than necessary.
			 * XXX One pattern might be fulfilled by
			 * XXX a different package and still need this
			 * XXX one for a different pattern.
			 */
			if (pkg_match(p->name, pkg->other_version) == 0)
				continue;
			if (pkg_match(p->name, pkg->pkgname) == 1)
				continue; /* Both match, ok. */
			if (!ForceDepending) {
				warnx("Dependency of %s fulfilled by %s, "
				    "but not by %s", iter, pkg->other_version,
				    pkg->pkgname);
				status = -1;
			}
			break;
		}
		free_plist(&plist);		
	}

	fclose(f);

	return status;
}

/*
 * Read package build information from meta data.
 */
static int
read_buildinfo(struct pkg_task *pkg)
{
	const char *data, *eol, *next_line;

	data = pkg->meta_data.meta_build_info;

	for (; data != NULL && *data != '\0'; data = next_line) {
		if ((eol = strchr(data, '\n')) == NULL) {
			eol = data + strlen(data);
			next_line = eol;
		} else
			next_line = eol + 1;

		if (strncmp(data, "OPSYS=", 6) == 0)
			pkg->buildinfo[BI_OPSYS] = dup_value(data, eol);
		else if (strncmp(data, "OS_VERSION=", 11) == 0)
			pkg->buildinfo[BI_OS_VERSION] = dup_value(data, eol);
		else if (strncmp(data, "MACHINE_ARCH=", 13) == 0)
			pkg->buildinfo[BI_MACHINE_ARCH] = dup_value(data, eol);
		else if (strncmp(data, "IGNORE_RECOMMENDED=", 19) == 0)
			pkg->buildinfo[BI_IGNORE_RECOMMENDED] = dup_value(data,
			    eol);
		else if (strncmp(data, "USE_ABI_DEPENDS=", 16) == 0)
			pkg->buildinfo[BI_USE_ABI_DEPENDS] = dup_value(data,
			    eol);
		else if (strncmp(data, "LICENSE=", 8) == 0)
			pkg->buildinfo[BI_LICENSE] = dup_value(data, eol);
		else if (strncmp(data, "PKGTOOLS_VERSION=", 17) == 0)
			pkg->buildinfo[BI_PKGTOOLS_VERSION] = dup_value(data,
			    eol);
	}
	if (pkg->buildinfo[BI_OPSYS] == NULL ||
	    pkg->buildinfo[BI_OS_VERSION] == NULL ||
	    pkg->buildinfo[BI_MACHINE_ARCH] == NULL) {
		warnx("Not all required build information are present.");
		return -1;
	}

	if ((pkg->buildinfo[BI_USE_ABI_DEPENDS] != NULL &&
	    strcasecmp(pkg->buildinfo[BI_USE_ABI_DEPENDS], "YES") != 0) ||
	    (pkg->buildinfo[BI_IGNORE_RECOMMENDED] != NULL &&
	    strcasecmp(pkg->buildinfo[BI_IGNORE_RECOMMENDED], "NO") != 0)) {
		warnx("%s was built to ignore ABI dependencies", pkg->pkgname);
	}

	return 0;
}

/*
 * Free buildinfo.
 */
static void
free_buildinfo(struct pkg_task *pkg)
{
	size_t i;

	for (i = 0; i < BI_ENUM_COUNT; ++i) {
		free(pkg->buildinfo[i]);
		pkg->buildinfo[i] = NULL;
	}
}

/*
 * Write meta data files to pkgdb after creating the directory.
 */
static int
write_meta_data(struct pkg_task *pkg)
{
	const struct pkg_meta_desc *descr;
	char *filename, **target;
	size_t len;
	ssize_t ret;
	int fd;

	if (Fake)
		return 0;

	if (mkdir_p(pkg->install_logdir)) {
		warn("Can't create pkgdb entry: %s", pkg->install_logdir);
		return -1;
	}

	for (descr = pkg_meta_descriptors; descr->entry_filename; ++descr) {
		target = (char **)((char *)&pkg->meta_data +
		    descr->entry_offset);
		if (*target == NULL)
			continue;
		filename = xasprintf("%s/%s", pkg->install_logdir,
		    descr->entry_filename);
		(void)unlink(filename);
		fd = open(filename, O_WRONLY | O_TRUNC | O_CREAT, descr->perm);
		if (fd == -1) {
			warn("Can't open meta data file: %s", filename);
			return -1;
		}
		len = strlen(*target);
		do {
			ret = write(fd, *target, len);
			if (ret == -1) {
				warn("Can't write meta data file: %s",
				    filename);
				free(filename);
				close(fd);
				return -1;
			}
			len -= ret;
		} while (ret > 0);
		if (close(fd) == -1) {
			warn("Can't close meta data file: %s", filename);
			free(filename);
			return -1;
		}
		free(filename);
	}

	return 0;
}

/*
 * Helper function for extract_files.
 */
static int
copy_data_to_disk(struct archive *reader, struct archive *writer,
    const char *filename)
{
	int r;
	const void *buff;
	size_t size;
	off_t offset;

	for (;;) {
		r = archive_read_data_block(reader, &buff, &size, &offset);
		if (r == ARCHIVE_EOF)
			return 0;
		if (r != ARCHIVE_OK) {
			warnx("Read error for %s: %s", filename,
			    archive_error_string(reader));
			return -1;
		}
		r = archive_write_data_block(writer, buff, size, offset);
		if (r != ARCHIVE_OK) {
			warnx("Write error for %s: %s", filename,
			    archive_error_string(writer));
			return -1;
		}
	}
}

/*
 * Extract package.
 * Any misordered, missing or unlisted file in the package is an error.
 */

static const int extract_flags = ARCHIVE_EXTRACT_OWNER |
    ARCHIVE_EXTRACT_PERM | ARCHIVE_EXTRACT_TIME | ARCHIVE_EXTRACT_UNLINK |
    ARCHIVE_EXTRACT_ACL | ARCHIVE_EXTRACT_FFLAGS | ARCHIVE_EXTRACT_XATTR;

static int
extract_files(struct pkg_task *pkg)
{
	char    cmd[MaxPathSize];
	const char *owner, *group, *permissions;
	struct archive *writer;
	int r;
	plist_t *p;
	const char *last_file;
	char *fullpath;

	if (Fake)
		return 0;

	if (mkdir_p(pkg->install_prefix)) {
		warn("Can't create prefix: %s", pkg->install_prefix);
		return -1;
	}

	if (!NoRecord && !pkgdb_open(ReadWrite)) {
		warn("Can't open pkgdb for writing");
		return -1;
	}

	if (chdir(pkg->install_prefix) == -1) {
		warn("Can't change into prefix: %s", pkg->install_prefix);
		return -1;
	}

	writer = archive_write_disk_new();
	archive_write_disk_set_options(writer, extract_flags);
	archive_write_disk_set_standard_lookup(writer);

	owner = NULL;
	group = NULL;
	permissions = NULL;
	last_file = NULL;

	r = -1;

	for (p = pkg->plist.head; p != NULL; p = p->next) {
		switch (p->type) {
		case PLIST_FILE:
			last_file = p->name;
			if (pkg->entry == NULL) {
				warnx("PLIST entry not in package (%s)",
				    archive_entry_pathname(pkg->entry));
				goto out;
			}
			if (strcmp(p->name, archive_entry_pathname(pkg->entry))) {
				warnx("PLIST entry and package don't match (%s vs %s)",
				    p->name, archive_entry_pathname(pkg->entry));
				goto out;
			}
			fullpath = xasprintf("%s/%s", pkg->prefix, p->name);
			pkgdb_store(fullpath, pkg->pkgname);
			free(fullpath);
			if (Verbose)
				printf("%s", p->name);
			break;

		case PLIST_PKGDIR:
			fullpath = xasprintf("%s/%s", pkg->prefix, p->name);
			mkdir_p(fullpath);
			free(fullpath);
			add_pkgdir(pkg->pkgname, pkg->prefix, p->name);
			continue;

		case PLIST_CMD:
			if (format_cmd(cmd, sizeof(cmd), p->name, pkg->prefix, last_file))
				return -1;
			printf("Executing '%s'\n", cmd);
			if (!Fake && system(cmd))
				warnx("command '%s' failed", cmd); /* XXX bail out? */
			continue;

		case PLIST_CHMOD:
			permissions = p->name;
			continue;

		case PLIST_CHOWN:
			owner = p->name;
			continue;

		case PLIST_CHGRP:
			group = p->name;
			continue;

		case PLIST_IGNORE:
			p = p->next;
			continue;

		default:
			continue;
		}

		r = archive_write_header(writer, pkg->entry);
		if (r != ARCHIVE_OK) {
			warnx("Failed to write %s for %s: %s",
			    archive_entry_pathname(pkg->entry),
			    pkg->pkgname,
			    archive_error_string(writer));
			goto out;
		}

		if (owner != NULL)
			archive_entry_set_uname(pkg->entry, owner);
		if (group != NULL)
			archive_entry_set_uname(pkg->entry, group);
		if (permissions != NULL) {
			mode_t mode;

			mode = archive_entry_mode(pkg->entry);
			mode = getmode(setmode(permissions), mode);
			archive_entry_set_mode(pkg->entry, mode);
		}

		r = copy_data_to_disk(pkg->archive, writer,
		    archive_entry_pathname(pkg->entry));
		if (r)
			goto out;
		if (Verbose)
			printf("\n");

		r = archive_read_next_header(pkg->archive, &pkg->entry);
		if (r == ARCHIVE_EOF) {
			pkg->entry = NULL;
			continue;
		}
		if (r != ARCHIVE_OK) {
			warnx("Failed to read from archive for %s: %s",
			    pkg->pkgname,
			    archive_error_string(pkg->archive));
			goto out;
		}
	}

	if (pkg->entry != NULL) {
		warnx("Package contains entries not in PLIST: %s",
		    archive_entry_pathname(pkg->entry));
		goto out;
	}

	r = 0;

out:
	if (!NoRecord)
		pkgdb_close();
	archive_write_free(writer);

	return r;
}

/*
 * Register dependencies after sucessfully installing the package.
 */
static void
pkg_register_depends(struct pkg_task *pkg)
{
	int fd;
	size_t text_len, i;
	char *required_by, *text;

	if (Fake)
		return;

	text = xasprintf("%s\n", pkg->pkgname);
	text_len = strlen(text);

	for (i = 0; i < pkg->dep_length; ++i) {
		required_by = pkgdb_pkg_file(pkg->dependencies[i], REQUIRED_BY_FNAME);

		fd = open(required_by, O_WRONLY | O_APPEND | O_CREAT, 0644);
		if (fd == -1) {
			warn("can't open dependency file '%s',"
			    "registration is incomplete!", required_by);
		} else if (write(fd, text, text_len) != (ssize_t)text_len) {
			warn("can't write to dependency file `%s'", required_by);
			close(fd);
		} else if (close(fd) == -1)
			warn("cannot close file %s", required_by);

		free(required_by);
	}

	free(text);
}

/*
 * Reduce the result from uname(3) to a canonical form.
 */
static void
normalise_platform(struct utsname *host_name)
{
#ifdef NUMERIC_VERSION_ONLY
	size_t span;

	span = strspn(host_name->release, "0123456789.");
	host_name->release[span] = '\0';
#endif
}

/*
 * Check build platform of the package against local host.
 */
static int
check_platform(struct pkg_task *pkg)
{
	struct utsname host_uname;
	const char *effective_arch;
	int fatal;

	if (uname(&host_uname) < 0) {
		if (Force) {
			warnx("uname() failed, continuing.");
			return 0;
		} else {
			warnx("uname() failed, aborting.");
			return -1;
		}
	}

	normalise_platform(&host_uname);

	if (OverrideMachine != NULL)
		effective_arch = OverrideMachine;
	else
		effective_arch = PKGSRC_MACHINE_ARCH;

	/* If either the OS or arch are different, bomb */
	if (strcmp(OPSYS_NAME, pkg->buildinfo[BI_OPSYS]) ||
	    strcmp(effective_arch, pkg->buildinfo[BI_MACHINE_ARCH]) != 0)
		fatal = 1;
	else
		fatal = 0;

	if (fatal ||
	    compatible_platform(OPSYS_NAME, host_uname.release,
				pkg->buildinfo[BI_OS_VERSION]) != 1) {
		warnx("Warning: package `%s' was built for a platform:",
		    pkg->pkgname);
		warnx("%s/%s %s (pkg) vs. %s/%s %s (this host)",
		    pkg->buildinfo[BI_OPSYS],
		    pkg->buildinfo[BI_MACHINE_ARCH],
		    pkg->buildinfo[BI_OS_VERSION],
		    OPSYS_NAME,
		    effective_arch,
		    host_uname.release);
		if (!Force && fatal)
			return -1;
	}
	return 0;
}

static int
check_pkgtools_version(struct pkg_task *pkg)
{
	const char *val = pkg->buildinfo[BI_PKGTOOLS_VERSION];
	int version;

	if (val == NULL) {
		warnx("Warning: package `%s' lacks pkg_install version data",
		    pkg->pkgname);
		return 0;
	}

	if (strlen(val) != 8 || strspn(val, "0123456789") != 8) {
		warnx("Warning: package `%s' contains an invalid pkg_install version",
		    pkg->pkgname);
		return Force ? 0 : -1;
	}
	version = atoi(val);
	if (version > PKGTOOLS_VERSION) {
		warnx("%s: package `%s' was built with a newer pkg_install version",
		    Force ? "Warning" : "Error", pkg->pkgname);
		return Force ? 0 : -1;
	}
	return 0;
}

/*
 * Run the install script.
 */
static int
run_install_script(struct pkg_task *pkg, const char *argument)
{
	int ret;
	char *filename;

	if (pkg->meta_data.meta_install == NULL || NoInstall)
		return 0;

	if (Destdir != NULL)
		setenv(PKG_DESTDIR_VNAME, Destdir, 1);
	setenv(PKG_PREFIX_VNAME, pkg->prefix, 1);
	setenv(PKG_METADATA_DIR_VNAME, pkg->logdir, 1);
	setenv(PKG_REFCOUNT_DBDIR_VNAME, config_pkg_refcount_dbdir, 1);

	if (Verbose)
		printf("Running install with %s for %s.\n", argument,
		    pkg->pkgname);
	if (Fake)
		return 0;

	filename = pkgdb_pkg_file(pkg->pkgname, INSTALL_FNAME);

	ret = 0;
	errno = 0;
	if (fcexec(pkg->install_logdir, filename, pkg->pkgname, argument,
	    (void *)NULL)) {
		if (errno != 0)
			warn("exec of install script failed");
		else
			warnx("install script returned error status");
		ret = -1;
	}
	free(filename);

	return ret;
}

struct find_conflict_data {
	const char *pkg;
	const char *old_pkg;
	const char *pattern;
};

static int
check_explicit_conflict_iter(const char *cur_pkg, void *cookie)
{
	struct find_conflict_data *data = cookie;

	if (data->old_pkg && strcmp(data->old_pkg, cur_pkg) == 0)
		return 0;

	warnx("Package `%s' conflicts with `%s', and `%s' is installed.",
	    data->pkg, data->pattern, cur_pkg);

	return 1;
}

static int
check_explicit_conflict(struct pkg_task *pkg)
{
	struct find_conflict_data data;
	char *installed, *installed_pattern;
	plist_t *p;
	int status;

	status = 0;

	for (p = pkg->plist.head; p != NULL; p = p->next) {
		if (p->type == PLIST_IGNORE) {
			p = p->next;
			continue;
		}
		if (p->type != PLIST_PKGCFL)
			continue;
		data.pkg = pkg->pkgname;
		data.old_pkg = pkg->other_version;
		data.pattern = p->name;
		status |= match_installed_pkgs(p->name,
		    check_explicit_conflict_iter, &data);
	}

	if (some_installed_package_conflicts_with(pkg->pkgname,
	    pkg->other_version, &installed, &installed_pattern)) {
		warnx("Installed package `%s' conflicts with `%s' when trying to install `%s'.",
			installed, installed_pattern, pkg->pkgname);
		free(installed);
		free(installed_pattern);
		status |= -1;
	}

	return status;
}

static int
check_implicit_conflict(struct pkg_task *pkg)
{
	plist_t *p;
	char *fullpath, *existing;
	int status;

	if (!pkgdb_open(ReadOnly)) {
#if notyet /* XXX empty pkgdb without database? */
		warn("Can't open pkgdb for reading");
		return -1;
#else
		return 0;
#endif
	}

	status = 0;

	for (p = pkg->plist.head; p != NULL; p = p->next) {
		if (p->type == PLIST_IGNORE) {
			p = p->next;
			continue;
		} else if (p->type != PLIST_FILE)
			continue;

		fullpath = xasprintf("%s/%s", pkg->prefix, p->name);
		existing = pkgdb_retrieve(fullpath);
		free(fullpath);
		if (existing == NULL)
			continue;
		if (pkg->other_version != NULL &&
		    strcmp(pkg->other_version, existing) == 0)
			continue;

		warnx("Conflicting PLIST with %s: %s", existing, p->name);
		if (!Force) {
			status = -1;
			if (!Verbose)
				break;
		}
	}

	pkgdb_close();
	return status;
}

/*
 * Install a required dependency and verify its installation.
 */
static int
install_depend_pkg(const char *dep)
{
	/* XXX check cyclic dependencies? */
	if (Fake || NoRecord) {
		if (!Force) {
			warnx("Missing dependency %s\n", dep);
			return 1;
		}
		warnx("Missing dependency %s, continuing", dep);
	}

	if (pkg_do(dep, 1, 0)) {
		if (!ForceDepends) {
			warnx("Can't install dependency %s", dep);
			return 1;
		}
		warnx("Can't install dependency %s, continuing", dep);
	}

	if (find_best_matching_installed_pkg(dep, 0) == NULL) {
		if (!ForceDepends) {
			warnx("Just installed dependency %s disappeared", dep);
			return 1;
		}
		warnx("Missing dependency %s ignored", dep);
	}

	return 0;
}

static int
check_dependencies(struct pkg_task *pkg)
{
	plist_t *p;
	char *best_installed;
	int status;
	size_t i;

	status = 0;

	/*
	 * Recursively handle dependencies, installing as required.
	 */
	for (p = pkg->plist.head; p != NULL; p = p->next) {
		if (p->type == PLIST_IGNORE) {
			p = p->next;
			continue;
		} else if (p->type != PLIST_PKGDEP)
			continue;

		if (find_best_matching_installed_pkg(p->name, 0) == NULL) {
			if (install_depend_pkg(p->name) != 0) {
				status = -1;
				break;
			}
		}
	}

	/*
	 * Now that all dependencies have been processed we can find the best
	 * matches for pkg_register_depends() to store in our +REQUIRED_BY.
	 */
	for (p = pkg->plist.head; p != NULL; p = p->next) {
		if (p->type == PLIST_IGNORE) {
			p = p->next;
			continue;
		} else if (p->type != PLIST_PKGDEP)
			continue;

		best_installed = find_best_matching_installed_pkg(p->name, 0);
		if (best_installed == NULL) {
			warnx("Expected dependency %s still missing", p->name);
			return -1;
		}

		for (i = 0; i < pkg->dep_length; ++i) {
			if (strcmp(best_installed, pkg->dependencies[i]) == 0)
				break;
		}
		if (i < pkg->dep_length) {
			/* Already used as dependency, so skip it. */
			free(best_installed);
			continue;
		}
		if (pkg->dep_length + 1 >= pkg->dep_allocated) {
			char **tmp;
			pkg->dep_allocated = 2 * pkg->dep_allocated + 1;
			pkg->dependencies = xrealloc(pkg->dependencies,
			    pkg->dep_allocated * sizeof(*tmp));
		}
		pkg->dependencies[pkg->dep_length++] = best_installed;
	}

	return status;
}

static int
preserve_meta_data_file(struct pkg_task *pkg, const char *name)
{
	char *old_file, *new_file;
	int rv;

	if (Fake)
		return 0;

	old_file = pkgdb_pkg_file(pkg->other_version, name);
	new_file = xasprintf("%s/%s", pkg->install_logdir, name);
	rv = 0;
	if (rename(old_file, new_file) == -1 && errno != ENOENT) {
		warn("Can't move %s from %s to %s", name, old_file, new_file);
		rv = -1;			
	}
	free(old_file);
	free(new_file);
	return rv;
}

static int
start_replacing(struct pkg_task *pkg)
{
	int result = -1;

	if (preserve_meta_data_file(pkg, REQUIRED_BY_FNAME))
		return -1;

	if (preserve_meta_data_file(pkg, PRESERVE_FNAME))
		return -1;

	if (pkg->meta_data.meta_installed_info == NULL &&
	    preserve_meta_data_file(pkg, INSTALLED_INFO_FNAME))
		return -1;

	if (Verbose || Fake) {
		printf("%s/pkg_delete -K %s -p %s%s%s '%s'\n",
			BINDIR, pkgdb_get_dir(), pkg->prefix,
			Destdir ? " -P ": "", Destdir ? Destdir : "",
			pkg->other_version);
	}
	if (!Fake) {
		result = fexec_skipempty(BINDIR "/pkg_delete", "-K", pkgdb_get_dir(),
		    "-p", pkg->prefix,
		    Destdir ? "-P": "", Destdir ? Destdir : "",
		    pkg->other_version, NULL);
		if (result != 0) {
			warnx("command failed: %s/pkg_delete -K %s -p %s %s%s%s",
			      BINDIR, pkgdb_get_dir(), pkg->prefix, Destdir ? "-P" : " ",
			      Destdir ? Destdir : "", pkg->other_version);
		}
	}

	return result;
}

static int check_input(const char *line, size_t len)
{
	if (line == NULL || len == 0)
		return 1;
	switch (*line) {
	case 'Y':
	case 'y':
	case 'T':
	case 't':
	case '1':
		return 0;
	default:
		return 1;
	}
}

static int
check_signature(struct pkg_task *pkg, int invalid_sig)
{
#ifdef BOOTSTRAP
	return 0;
#else
	char *line;
	size_t len;

	if (strcasecmp(verified_installation, "never") == 0)
		return 0;
	if (strcasecmp(verified_installation, "always") == 0) {
		if (invalid_sig)
			warnx("No valid signature found, rejected");
		return invalid_sig;
	}
	if (strcasecmp(verified_installation, "trusted") == 0) {
		if (!invalid_sig)
			return 0;
		fprintf(stderr, "No valid signature found for %s.\n",
		    pkg->pkgname);
		fprintf(stderr,
		    "Do you want to proceed with the installation [y/n]?\n");
		line = fgetln(stdin, &len);
		if (check_input(line, len)) {
			fprintf(stderr, "Cancelling installation\n");
			return 1;
		}
		return 0;
	}
	if (strcasecmp(verified_installation, "interactive") == 0) {
		fprintf(stderr, "Do you want to proceed with "
		    "the installation of %s [y/n]?\n", pkg->pkgname);
		line = fgetln(stdin, &len);
		if (check_input(line, len)) {
			fprintf(stderr, "Cancelling installation\n");
			return 1;
		}
		return 0;
	}
	warnx("Unknown value of configuration variable VERIFIED_INSTALLATION");
	return 1;
#endif
}

static int
check_vulnerable(struct pkg_task *pkg)
{
#ifdef BOOTSTRAP
	return 0;
#else
	static struct pkg_vulnerabilities *pv;
	int require_check;
	char *line;
	size_t len;

	if (strcasecmp(check_vulnerabilities, "never") == 0)
		return 0;
	else if (strcasecmp(check_vulnerabilities, "always") == 0)
		require_check = 1;
	else if (strcasecmp(check_vulnerabilities, "interactive") == 0)
		require_check = 0;
	else {
		warnx("Unknown value of the configuration variable"
		    "CHECK_VULNERABILITIES");
		return 1;
	}

	if (pv == NULL) {
		pv = read_pkg_vulnerabilities_file(pkg_vulnerabilities_file,
		    require_check, 0);
		if (pv == NULL)
			return require_check;
	}

	if (!audit_package(pv, pkg->pkgname, NULL, 0, 2))
		return 0;

	if (require_check)
		return 1;

	fprintf(stderr, "Do you want to proceed with the installation of %s"
	    " [y/n]?\n", pkg->pkgname);
	line = fgetln(stdin, &len);
	if (check_input(line, len)) {
		fprintf(stderr, "Cancelling installation\n");
		return 1;
	}
	return 0;
#endif
}

static int
check_license(struct pkg_task *pkg)
{
#ifdef BOOTSTRAP
	return 0;
#else
	if (LicenseCheck == 0)
		return 0;

	if ((pkg->buildinfo[BI_LICENSE] == NULL ||
	     *pkg->buildinfo[BI_LICENSE] == '\0')) {
	
		if (LicenseCheck == 1)
			return 0;
		warnx("No LICENSE set for package `%s'", pkg->pkgname);
		return 1;
	}

	switch (acceptable_license(pkg->buildinfo[BI_LICENSE])) {
	case 0:
		warnx("License `%s' of package `%s' is not acceptable",
		    pkg->buildinfo[BI_LICENSE], pkg->pkgname);
		return 1;
	case 1:
		return 0;
	default:
		warnx("Invalid LICENSE for package `%s'", pkg->pkgname);
		return 1;
	}
#endif
}

/*
 * Install a single package.
 */
static int
pkg_do(const char *pkgpath, int mark_automatic, int top_level)
{
	char *archive_name;
	int status, invalid_sig;
	struct pkg_task *pkg;

	pkg = xcalloc(1, sizeof(*pkg));

	status = -1;

	pkg->archive = find_archive(pkgpath, top_level, &archive_name);
	if (pkg->archive == NULL) {
		warnx("no pkg found for '%s', sorry.", pkgpath);
		goto clean_find_archive;
	}

#ifndef BOOTSTRAP
	invalid_sig = pkg_verify_signature(archive_name, &pkg->archive, &pkg->entry,
	    &pkg->pkgname);
#else
	invalid_sig = 0;
#endif
	free(archive_name);

	if (pkg->archive == NULL)
		goto clean_memory;

	if (read_meta_data(pkg))
		goto clean_memory;

	/* Parse PLIST early, so that messages can use real package name. */
	if (pkg_parse_plist(pkg))
		goto clean_memory;

	if (check_signature(pkg, invalid_sig))
		goto clean_memory;

	if (read_buildinfo(pkg))
		goto clean_memory;

	if (check_pkgtools_version(pkg))
		goto clean_memory;

	if (check_vulnerable(pkg))
		goto clean_memory;

	if (check_license(pkg))
		goto clean_memory;

	if (pkg->meta_data.meta_mtree != NULL)
		warnx("mtree specification in pkg `%s' ignored", pkg->pkgname);

	pkg->logdir = xasprintf("%s/%s", config_pkg_dbdir, pkg->pkgname);

	if (Destdir != NULL)
		pkg->install_logdir = xasprintf("%s/%s", Destdir, pkg->logdir);
	else
		pkg->install_logdir = xstrdup(pkg->logdir);

	if (NoRecord && !Fake) {
		const char *tmpdir;

		tmpdir = getenv("TMPDIR");
		if (tmpdir == NULL)
			tmpdir = "/tmp";

		free(pkg->install_logdir);
		pkg->install_logdir = xasprintf("%s/pkg_install.XXXXXX", tmpdir);
		/* XXX pkg_add -u... */
		if (mkdtemp(pkg->install_logdir) == NULL) {
			warn("mkdtemp failed");
			goto clean_memory;
		}
	}

	switch (check_already_installed(pkg)) {
	case 0:
		status = 0;
		goto clean_memory;
	case 1:
		break;
	case -1:
		goto clean_memory;
	}

	if (check_platform(pkg))
		goto clean_memory;

	if (check_other_installed(pkg))
		goto clean_memory; 

	if (check_explicit_conflict(pkg))
		goto clean_memory;

	if (check_implicit_conflict(pkg))
		goto clean_memory;

	if (pkg->other_version != NULL) {
		/*
		 * Replacing an existing package.
		 * Write meta-data, get rid of the old version,
		 * install/update dependencies and finally extract.
		 */
		if (write_meta_data(pkg))
			goto nuke_pkgdb;

		if (start_replacing(pkg))
			goto nuke_pkgdb;

		if (pkg->install_logdir_real) {
			rename(pkg->install_logdir, pkg->install_logdir_real);
			free(pkg->install_logdir);
			pkg->install_logdir = pkg->install_logdir_real;
			pkg->install_logdir_real = NULL;
		}

		if (check_dependencies(pkg))
			goto nuke_pkgdb;
	} else {
		/*
		 * Normal installation.
		 * Install/update dependencies first and
		 * write the current package to disk afterwards.
		 */ 
		if (check_dependencies(pkg))
			goto clean_memory;

		if (write_meta_data(pkg))
			goto nuke_pkgdb;
	}

	if (run_install_script(pkg, "PRE-INSTALL"))
		goto nuke_pkgdb;

	if (extract_files(pkg))
		goto nuke_pkg;

	if (run_install_script(pkg, "POST-INSTALL"))
		goto nuke_pkgdb;

	/* XXX keep +INSTALL_INFO for updates? */
	/* XXX keep +PRESERVE for updates? */
	if (mark_automatic)
		mark_as_automatic_installed(pkg->pkgname, 1);

	pkg_register_depends(pkg);

	if (Verbose)
		printf("Package %s registered in %s\n", pkg->pkgname, pkg->install_logdir);

	if (pkg->meta_data.meta_display != NULL)
		fputs(pkg->meta_data.meta_display, stdout);

	status = 0;
	goto clean_memory;

nuke_pkg:
	if (!Fake) {
		if (pkg->other_version) {
			warnx("Updating of %s to %s failed.",
			    pkg->other_version, pkg->pkgname);
			warnx("Remember to run pkg_admin rebuild-tree after fixing this.");
		}
		delete_package(FALSE, &pkg->plist, FALSE, Destdir);
	}

nuke_pkgdb:
	if (!Fake) {
		(void) remove_files(pkg->install_logdir, "+*");
		if (recursive_remove(pkg->install_logdir, 1))
			warn("Couldn't remove %s", pkg->install_logdir);
		free(pkg->install_logdir_real);
		free(pkg->install_logdir);
		free(pkg->logdir);
		pkg->install_logdir_real = NULL;
		pkg->install_logdir = NULL;
		pkg->logdir = NULL;
	}

clean_memory:
	if (pkg->logdir != NULL && NoRecord && !Fake) {
		if (recursive_remove(pkg->install_logdir, 1))
			warn("Couldn't remove %s", pkg->install_logdir);
	}
	free(pkg->install_prefix);
	free(pkg->install_logdir_real);
	free(pkg->install_logdir);
	free(pkg->logdir);
	free_buildinfo(pkg);
	free_plist(&pkg->plist);
	free_meta_data(pkg);
	if (pkg->archive)
		archive_read_free(pkg->archive);
	free(pkg->other_version);
	free(pkg->pkgname);
clean_find_archive:
	free(pkg);
	return status;
}

int
pkg_perform(lpkg_head_t *pkgs)
{
	int     errors = 0;
	lpkg_t *lpp;

	while ((lpp = TAILQ_FIRST(pkgs)) != NULL) {
		if (pkg_do(lpp->lp_name, Automatic, 1))
			++errors;
		TAILQ_REMOVE(pkgs, lpp, lp_link);
		free_lpkg(lpp);
	}

	return errors;
}
