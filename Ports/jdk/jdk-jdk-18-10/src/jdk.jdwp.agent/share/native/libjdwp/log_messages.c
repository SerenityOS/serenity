/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "util.h"

#include <time.h>
#include <errno.h>
#include <sys/types.h>

#include "proc_md.h"

#include "log_messages.h"

#ifdef JDWP_LOGGING

#define MAXLEN_INTEGER          20
#define MAXLEN_FILENAME         256
#define MAXLEN_TIMESTAMP        80
#define MAXLEN_LOCATION         (MAXLEN_FILENAME+MAXLEN_INTEGER+16)
#define MAXLEN_MESSAGE          256
#define MAXLEN_EXEC             (MAXLEN_FILENAME*2+MAXLEN_INTEGER+16)

#define TIMESTAMP_SIZE          (MAXLEN_TIMESTAMP+1)
#define MAXLEN_DT               19 // "DD.MM.YYYY HH:MM:SS"
#define MAXLEN_MS               5 // ".mmm "
#define DT_SIZE                 (MAXLEN_DT+1)
#define TZ_SIZE                 (TIMESTAMP_SIZE-MAXLEN_DT-MAXLEN_MS)

static MUTEX_T my_mutex = MUTEX_INIT;

/* Static variables (should be protected with mutex) */
static int logging;
static FILE * log_file;
static char logging_filename[MAXLEN_FILENAME+1+6];
static char location_stamp[MAXLEN_LOCATION+1];
static PID_T processPid;
static int open_count;

/*
 * "DD.MM.YYYY HH:MM:SS.mmm <TZ>"
 */
static void
get_time_stamp(char *tbuf, size_t ltbuf)
{
    char timestamp_date_time[DT_SIZE];
    char timestamp_timezone[TZ_SIZE];
    unsigned millisecs = 0;
    time_t t = 0;

    GETMILLSECS(millisecs);
    if ( time(&t) == (time_t)(-1) ) {
        t = 0;
    }

    (void)strftime(timestamp_date_time, DT_SIZE,
                "%d.%m.%Y %T", localtime(&t));
    (void)strftime(timestamp_timezone, TZ_SIZE,
                "%Z", localtime(&t));

    // Truncate milliseconds in buffer large enough to hold the
    // value which is always < 1000 (and so a maximum of 3 digits for "%.3d")
    char tmp[20];
    snprintf(tmp, sizeof(tmp), "%.3d", millisecs);
    snprintf(tbuf, ltbuf, "%s.%.3s %s", timestamp_date_time, tmp, timestamp_timezone);
}

/* Get basename of filename */
static const char *
file_basename(const char *file)
{
    char *p1;
    char *p2;

    if ( file==NULL )
        return "unknown";
    p1 = strrchr(file, '\\');
    p2 = strrchr(file, '/');
    p1 = ((p1 > p2) ? p1 : p2);
    if (p1 != NULL) {
        file = p1 + 1;
    }
    return file;
}

/* Fill in the exact source location of the LOG entry. */
static void
fill_location_stamp(const char *flavor, const char *file, int line)
{
    (void)snprintf(location_stamp, sizeof(location_stamp),
                    "%s:\"%s\":%d;",
                    flavor, file_basename(file), line);
    location_stamp[sizeof(location_stamp)-1] = 0;
}

/* Begin a log entry. */
void
log_message_begin(const char *flavor, const char *file, int line)
{
    MUTEX_LOCK(my_mutex); /* Unlocked in log_message_end() */
    if ( logging ) {
        location_stamp[0] = 0;
        fill_location_stamp(flavor, file, line);
    }
}

/* Standard Logging Format Entry */
static void
standard_logging_format(FILE *fp,
        const char *datetime,
        const char *level,
        const char *product,
        const char *module,
        const char *optional,
        const char *messageID,
        const char *message)
{
    const char *format;

    /* "[#|Date&Time&Zone|LogLevel|ProductName|ModuleID|
     *     OptionalKey1=Value1;OptionalKeyN=ValueN|MessageID:MessageText|#]\n"
     */

    format="[#|%s|%s|%s|%s|%s|%s:%s|#]\n";

    print_message(fp, "", "", format,
            datetime,
            level,
            product,
            module,
            optional,
            messageID,
            message);
}

/* End a log entry */
void
log_message_end(const char *format, ...)
{
    if ( logging ) {
        va_list ap;
        THREAD_T tid;
        char datetime[MAXLEN_TIMESTAMP+1];
        const char *level;
        const char *product;
        const char *module;
        char optional[MAXLEN_INTEGER+6+MAXLEN_INTEGER+6+MAXLEN_LOCATION+1];
        const char *messageID;
        char message[MAXLEN_MESSAGE+1];

        /* Grab the location, start file if needed, and clear the lock */
        if ( log_file == NULL && open_count == 0 && logging_filename[0] != 0 ) {
            open_count++;
            log_file = fopen(logging_filename, "w");
            if ( log_file!=NULL ) {
                (void)setvbuf(log_file, NULL, _IOLBF, BUFSIZ);
            } else {
                logging = 0;
            }
        }

        if ( log_file != NULL ) {

            /* Get the rest of the needed information */
            tid = GET_THREAD_ID();
            level = "FINEST"; /* FIXUP? */
            product = "J2SE1.5"; /* FIXUP? */
            module = "jdwp"; /* FIXUP? */
            messageID = ""; /* FIXUP: Unique message string ID? */
            (void)snprintf(optional, sizeof(optional),
                        "LOC=%s;PID=%d;THR=t@%d",
                        location_stamp,
                        (int)processPid,
                        (int)(intptr_t)tid);

            /* Construct message string. */
            va_start(ap, format);
            (void)vsnprintf(message, sizeof(message), format, ap);
            message[sizeof(message) - 1] = 0;
            va_end(ap);

            get_time_stamp(datetime, sizeof(datetime));

            /* Send out standard logging format message */
            standard_logging_format(log_file,
                datetime,
                level,
                product,
                module,
                optional,
                messageID,
                message);
        }
        location_stamp[0] = 0;
    }
    MUTEX_UNLOCK(my_mutex); /* Locked in log_message_begin() */
}

#endif

/* Set up the logging with the name of a logging file. */
void
setup_logging(const char *filename, unsigned flags)
{
#ifdef JDWP_LOGGING
    FILE *fp = NULL;

    /* Turn off logging */
    logging = 0;
    gdata->log_flags = 0;

    /* Just return if not doing logging */
    if ( filename==NULL || flags==0 )
        return;

    /* Create potential filename for logging */
    processPid = GETPID();
    (void)snprintf(logging_filename, sizeof(logging_filename),
                    "%s.%d", filename, (int)processPid);

    /* Turn on logging (do this last) */
    logging = 1;
    gdata->log_flags = flags;

#endif
}

/* Finish up logging, flush output to the logfile. */
void
finish_logging()
{
#ifdef JDWP_LOGGING
    MUTEX_LOCK(my_mutex);
    if ( logging ) {
        logging = 0;
        if ( log_file != NULL ) {
            (void)fflush(log_file);
            (void)fclose(log_file);
            log_file = NULL;
        }
    }
    MUTEX_UNLOCK(my_mutex);
#endif
}
