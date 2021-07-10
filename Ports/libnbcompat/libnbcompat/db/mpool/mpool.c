/*	$NetBSD: mpool.c,v 1.6 2013/09/08 16:24:43 ryoon Exp $	*/
/*	NetBSD: mpool.c,v 1.18 2008/09/11 12:58:00 joerg Exp 	*/

/*-
 * Copyright (c) 1990, 1993, 1994
 *	The Regents of the University of California.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#include <nbcompat.h>
#include <nbcompat/cdefs.h>

__RCSID("$NetBSD: mpool.c,v 1.6 2013/09/08 16:24:43 ryoon Exp $");

#include <nbcompat/queue.h>
#include <sys/stat.h>

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <nbcompat/db.h>

#define	__MPOOLINTERFACE_PRIVATE
#include <nbcompat/mpool.h>

#ifndef	EFTYPE
#define	EFTYPE		EINVAL
#endif

#if 0
#ifdef __weak_alias
__weak_alias(mpool_close,_mpool_close)
__weak_alias(mpool_filter,_mpool_filter)
__weak_alias(mpool_get,_mpool_get)
__weak_alias(mpool_new,_mpool_new)
__weak_alias(mpool_open,_mpool_open)
__weak_alias(mpool_put,_mpool_put)
__weak_alias(mpool_sync,_mpool_sync)
#endif
#endif

static BKT *mpool_bkt(MPOOL *);
static BKT *mpool_look(MPOOL *, pgno_t);
static int  mpool_write(MPOOL *, BKT *);

#ifdef BROKEN_PREAD
#include "../pread.c"
#endif
#ifdef BROKEN_PWRITE
#include "../pwrite.c"
#endif

/*
 * mpool_open --
 *	Initialize a memory pool.
 */
/*ARGSUSED*/
MPOOL *
mpool_open(void *key, int fd, pgno_t pagesize, pgno_t maxcache)
{
	struct stat sb;
	MPOOL *mp;
	int entry;

	/*
	 * Get information about the file.
	 *
	 * XXX
	 * We don't currently handle pipes, although we should.
	 */
	if (fstat(fd, &sb))
		return (NULL);
	if (!S_ISREG(sb.st_mode)) {
#if defined(__MINT__)
		errno = EACCES;
#else
		errno = ESPIPE;
#endif
		return (NULL);
	}

	/* Allocate and initialize the MPOOL cookie. */
	if ((mp = (MPOOL *)calloc(1, sizeof(MPOOL))) == NULL)
		return (NULL);
	CIRCLEQ_INIT(&mp->lqh);
	for (entry = 0; entry < HASHSIZE; ++entry)
		CIRCLEQ_INIT(&mp->hqh[entry]);
	mp->maxcache = maxcache;
	mp->npages = (pgno_t)(sb.st_size / pagesize);
	mp->pagesize = pagesize;
	mp->fd = fd;
	return (mp);
}

/*
 * mpool_filter --
 *	Initialize input/output filters.
 */
void
mpool_filter(MPOOL *mp, void (*pgin)(void *, pgno_t, void *),
    void (*pgout)(void *, pgno_t, void *), void *pgcookie)
{
	mp->pgin = pgin;
	mp->pgout = pgout;
	mp->pgcookie = pgcookie;
}
	
/*
 * mpool_new --
 *	Get a new page of memory.
 */
void *
mpool_new( MPOOL *mp, pgno_t *pgnoaddr)
{
	struct _hqh *head;
	BKT *bp;

	if (mp->npages == MAX_PAGE_NUMBER) {
		(void)fprintf(stderr, "mpool_new: page allocation overflow.\n");
		abort();
	}
#ifdef STATISTICS
	++mp->pagenew;
#endif
	/*
	 * Get a BKT from the cache.  Assign a new page number, attach
	 * it to the head of the hash chain, the tail of the lru chain,
	 * and return.
	 */
	if ((bp = mpool_bkt(mp)) == NULL)
		return (NULL);
	*pgnoaddr = bp->pgno = mp->npages++;
	bp->flags = MPOOL_PINNED;

	head = &mp->hqh[HASHKEY(bp->pgno)];
	CIRCLEQ_INSERT_HEAD(head, bp, hq);
	CIRCLEQ_INSERT_TAIL(&mp->lqh, bp, q);
	return (bp->page);
}

/*
 * mpool_get
 *	Get a page.
 */
/*ARGSUSED*/
void *
mpool_get(MPOOL *mp, pgno_t pgno, u_int flags)
{
	struct _hqh *head;
	BKT *bp;
	off_t off;
	ssize_t nr;

	/* Check for attempt to retrieve a non-existent page. */
	if (pgno >= mp->npages) {
		errno = EINVAL;
		return (NULL);
	}

#ifdef STATISTICS
	++mp->pageget;
#endif

	/* Check for a page that is cached. */
	if ((bp = mpool_look(mp, pgno)) != NULL) {
#ifdef DEBUG
		if (bp->flags & MPOOL_PINNED) {
			(void)fprintf(stderr,
			    "mpool_get: page %d already pinned\n", bp->pgno);
			abort();
		}
#endif
		/*
		 * Move the page to the head of the hash chain and the tail
		 * of the lru chain.
		 */
		head = &mp->hqh[HASHKEY(bp->pgno)];
		CIRCLEQ_REMOVE(head, bp, hq);
		CIRCLEQ_INSERT_HEAD(head, bp, hq);
		CIRCLEQ_REMOVE(&mp->lqh, bp, q);
		CIRCLEQ_INSERT_TAIL(&mp->lqh, bp, q);

		/* Return a pinned page. */
		bp->flags |= MPOOL_PINNED;
		return (bp->page);
	}

	/* Get a page from the cache. */
	if ((bp = mpool_bkt(mp)) == NULL)
		return (NULL);

	/* Read in the contents. */
#ifdef STATISTICS
	++mp->pageread;
#endif
	off = mp->pagesize * pgno;
	if ((nr = pread(mp->fd, bp->page, (size_t)mp->pagesize, off)) != (int)mp->pagesize) {
		if (nr >= 0)
			errno = EFTYPE;
		return (NULL);
	}

	/* Set the page number, pin the page. */
	bp->pgno = pgno;
	bp->flags = MPOOL_PINNED;

	/*
	 * Add the page to the head of the hash chain and the tail
	 * of the lru chain.
	 */
	head = &mp->hqh[HASHKEY(bp->pgno)];
	CIRCLEQ_INSERT_HEAD(head, bp, hq);
	CIRCLEQ_INSERT_TAIL(&mp->lqh, bp, q);

	/* Run through the user's filter. */
	if (mp->pgin != NULL)
		(mp->pgin)(mp->pgcookie, bp->pgno, bp->page);

	return (bp->page);
}

/*
 * mpool_put
 *	Return a page.
 */
/*ARGSUSED*/
int
mpool_put(MPOOL *mp, void *page, u_int flags)
{
	BKT *bp;

#ifdef STATISTICS
	++mp->pageput;
#endif
	bp = (BKT *)(void *)((char *)page - sizeof(BKT));
#ifdef DEBUG
	if (!(bp->flags & MPOOL_PINNED)) {
		(void)fprintf(stderr,
		    "mpool_put: page %d not pinned\n", bp->pgno);
		abort();
	}
#endif
	bp->flags &= ~MPOOL_PINNED;
	bp->flags |= flags & MPOOL_DIRTY;
	return (RET_SUCCESS);
}

/*
 * mpool_close
 *	Close the buffer pool.
 */
int
mpool_close(MPOOL *mp)
{
	BKT *bp;

	/* Free up any space allocated to the lru pages. */
	while ((bp = mp->lqh.cqh_first) != (void *)&mp->lqh) {
		CIRCLEQ_REMOVE(&mp->lqh, mp->lqh.cqh_first, q);
		free(bp);
	}

	/* Free the MPOOL cookie. */
	free(mp);
	return (RET_SUCCESS);
}

/*
 * mpool_sync
 *	Sync the pool to disk.
 */
int
mpool_sync(MPOOL *mp)
{
	BKT *bp;

	/* Walk the lru chain, flushing any dirty pages to disk. */
	for (bp = mp->lqh.cqh_first;
	    bp != (void *)&mp->lqh; bp = bp->q.cqe_next)
		if (bp->flags & MPOOL_DIRTY &&
		    mpool_write(mp, bp) == RET_ERROR)
			return (RET_ERROR);

	/* Sync the file descriptor. */
	return (fsync(mp->fd) ? RET_ERROR : RET_SUCCESS);
}

/*
 * mpool_bkt
 *	Get a page from the cache (or create one).
 */
static BKT *
mpool_bkt(MPOOL *mp)
{
	struct _hqh *head;
	BKT *bp;

	/* If under the max cached, always create a new page. */
	if (mp->curcache < mp->maxcache)
		goto new;

	/*
	 * If the cache is max'd out, walk the lru list for a buffer we
	 * can flush.  If we find one, write it (if necessary) and take it
	 * off any lists.  If we don't find anything we grow the cache anyway.
	 * The cache never shrinks.
	 */
	for (bp = mp->lqh.cqh_first;
	    bp != (void *)&mp->lqh; bp = bp->q.cqe_next)
		if (!(bp->flags & MPOOL_PINNED)) {
			/* Flush if dirty. */
			if (bp->flags & MPOOL_DIRTY &&
			    mpool_write(mp, bp) == RET_ERROR)
				return (NULL);
#ifdef STATISTICS
			++mp->pageflush;
#endif
			/* Remove from the hash and lru queues. */
			head = &mp->hqh[HASHKEY(bp->pgno)];
			CIRCLEQ_REMOVE(head, bp, hq);
			CIRCLEQ_REMOVE(&mp->lqh, bp, q);
#ifdef DEBUG
			{
				void *spage = bp->page;
				(void)memset(bp, 0xff,
				    (size_t)(sizeof(BKT) + mp->pagesize));
				bp->page = spage;
			}
#endif
			return (bp);
		}

new:	if ((bp = (BKT *)malloc((size_t)(sizeof(BKT) + mp->pagesize))) == NULL)
		return (NULL);
#ifdef STATISTICS
	++mp->pagealloc;
#endif
#if defined(DEBUG) || defined(PURIFY)
	(void)memset(bp, 0xff, (size_t)(sizeof(BKT) + mp->pagesize));
#endif
	bp->page = (char *)(void *)bp + sizeof(BKT);
	++mp->curcache;
	return (bp);
}

/*
 * mpool_write
 *	Write a page to disk.
 */
static int
mpool_write(MPOOL *mp, BKT *bp)
{
	off_t off;

#ifdef STATISTICS
	++mp->pagewrite;
#endif

	/* Run through the user's filter. */
	if (mp->pgout)
		(mp->pgout)(mp->pgcookie, bp->pgno, bp->page);

	off = mp->pagesize * bp->pgno;
	if (pwrite(mp->fd, bp->page, (size_t)mp->pagesize, off) != (int)mp->pagesize)
		return (RET_ERROR);

	/*
	 * Re-run through the input filter since this page may soon be
	 * accessed via the cache, and whatever the user's output filter
	 * did may screw things up if we don't let the input filter
	 * restore the in-core copy.
	 */
	if (mp->pgin)
		(mp->pgin)(mp->pgcookie, bp->pgno, bp->page);

	bp->flags &= ~MPOOL_DIRTY;
	return (RET_SUCCESS);
}

/*
 * mpool_look
 *	Lookup a page in the cache.
 */
static BKT *
mpool_look(MPOOL *mp, pgno_t pgno)
{
	struct _hqh *head;
	BKT *bp;

	head = &mp->hqh[HASHKEY(pgno)];
	for (bp = head->cqh_first; bp != (void *)head; bp = bp->hq.cqe_next)
		if (bp->pgno == pgno) {
#ifdef STATISTICS
			++mp->cachehit;
#endif
			return (bp);
		}
#ifdef STATISTICS
	++mp->cachemiss;
#endif
	return (NULL);
}

#ifdef STATISTICS
/*
 * mpool_stat
 *	Print out cache statistics.
 */
void
mpool_stat(mp)
	MPOOL *mp;
{
	BKT *bp;
	int cnt;
	const char *sep;

	(void)fprintf(stderr, "%lu pages in the file\n", (u_long)mp->npages);
	(void)fprintf(stderr,
	    "page size %lu, cacheing %lu pages of %lu page max cache\n",
	    (u_long)mp->pagesize, (u_long)mp->curcache, (u_long)mp->maxcache);
	(void)fprintf(stderr, "%lu page puts, %lu page gets, %lu page new\n",
	    mp->pageput, mp->pageget, mp->pagenew);
	(void)fprintf(stderr, "%lu page allocs, %lu page flushes\n",
	    mp->pagealloc, mp->pageflush);
	if (mp->cachehit + mp->cachemiss)
		(void)fprintf(stderr,
		    "%.0f%% cache hit rate (%lu hits, %lu misses)\n", 
		    ((double)mp->cachehit / (mp->cachehit + mp->cachemiss))
		    * 100, mp->cachehit, mp->cachemiss);
	(void)fprintf(stderr, "%lu page reads, %lu page writes\n",
	    mp->pageread, mp->pagewrite);

	sep = "";
	cnt = 0;
	for (bp = mp->lqh.cqh_first;
	    bp != (void *)&mp->lqh; bp = bp->q.cqe_next) {
		(void)fprintf(stderr, "%s%d", sep, bp->pgno);
		if (bp->flags & MPOOL_DIRTY)
			(void)fprintf(stderr, "d");
		if (bp->flags & MPOOL_PINNED)
			(void)fprintf(stderr, "P");
		if (++cnt == 10) {
			sep = "\n";
			cnt = 0;
		} else
			sep = ", ";
			
	}
	(void)fprintf(stderr, "\n");
}
#endif
