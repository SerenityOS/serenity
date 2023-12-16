/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// Has to be defined before including due to legacy Unices
#define SYSLOG_NAMES 1

#include <AK/ByteString.h>
#include <AK/StringBuilder.h>
#include <stdio.h>
#include <string.h>
#include <syslog.h>
#include <unistd.h>

// This implementation doesn't talk to a syslog server. Any options related to
// that are no-ops.

extern "C" {

// For implementation simplicity, we actually only use the re-entrant version
// of each function, and the version that isn't just redirects with a static
// struct to share.
static struct syslog_data global_log_data = {
    .ident = nullptr,
    .logopt = 0,
    .facility = LOG_USER,
    .maskpri = LOG_UPTO(LOG_DEBUG)
};

// Used when ident is null, since syslog traditionally prints the program's
// own name; the process name will always be the same unless we exec.
static char program_name_buffer[256];
static bool program_name_set = false;

// Convenience function for initialization and checking what string to use
// for the program name.
static char const* get_syslog_ident(struct syslog_data* data)
{
    if (!program_name_set && data->ident == nullptr)
        program_name_set = get_process_name(program_name_buffer, sizeof(program_name_buffer)) >= 0;

    if (data->ident != nullptr)
        return data->ident;
    else if (program_name_set)
        return program_name_buffer;

    VERIFY_NOT_REACHED();
}

void openlog_r(char const* ident, int logopt, int facility, struct syslog_data* data)
{
    data->ident = ident;
    data->logopt = logopt;
    data->facility = facility;
    // default value
    data->maskpri = LOG_UPTO(LOG_DEBUG);
    // would be where we connect to a daemon
}

void openlog(char const* ident, int logopt, int facility)
{
    openlog_r(ident, logopt, facility, &global_log_data);
}

void closelog_r(struct syslog_data* data)
{
    // would be where we disconnect from a daemon
    // restore defaults
    data->ident = nullptr;
    data->logopt = 0;
    data->facility = LOG_USER;
    data->maskpri = LOG_UPTO(LOG_DEBUG);
}

void closelog(void)
{
    closelog_r(&global_log_data);
}

int setlogmask_r(int maskpri, struct syslog_data* data)
{
    // Remember, this takes the input of LOG_MASK/LOG_UPTO
    int old_maskpri = data->maskpri;
    data->maskpri = maskpri;
    return old_maskpri;
}

int setlogmask(int maskpri)
{
    return setlogmask_r(maskpri, &global_log_data);
}

void syslog_r(int priority, struct syslog_data* data, char const* message, ...)
{
    va_list ap;
    va_start(ap, message);
    vsyslog_r(priority, data, message, ap);
    va_end(ap);
}

void syslog(int priority, char const* message, ...)
{
    va_list ap;
    va_start(ap, message);
    vsyslog_r(priority, &global_log_data, message, ap);
    va_end(ap);
}

void vsyslog_r(int priority, struct syslog_data* data, char const* message, va_list args)
{
    StringBuilder combined;

    int real_priority = LOG_PRI(priority);
    // Lots of parens, but it just extracts the priority from combo and masks.
    if (!(data->maskpri & LOG_MASK(real_priority)))
        return;

    // Some metadata would be consumed by a syslog daemon, if we had one.
    if (data->logopt & LOG_PID)
        combined.appendff("{}[{}]: ", get_syslog_ident(data), getpid());
    else
        combined.appendff("{}: ", get_syslog_ident(data));

    combined.appendvf(message, args);
    auto combined_string = combined.to_byte_string();

    if (data->logopt & LOG_CONS)
        dbgputstr(combined_string.characters(), combined_string.length());
    if (data->logopt & LOG_PERROR)
        fputs(combined_string.characters(), stderr);
}

void vsyslog(int priority, char const* message, va_list args)
{
    vsyslog_r(priority, &global_log_data, message, args);
}
}
