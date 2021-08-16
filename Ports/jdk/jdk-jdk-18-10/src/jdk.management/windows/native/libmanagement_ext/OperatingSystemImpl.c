/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "jni_util.h"
#include "jlong.h"
#include "jvm.h"
#include "management_ext.h"
#include "com_sun_management_internal_OperatingSystemImpl.h"

#include <psapi.h>
#include <errno.h>
#include <stdlib.h>

#include <malloc.h>
#pragma warning (push,0)
#include <windows.h>
#pragma warning (pop)
#include <stdio.h>
#include <time.h>
#include <stdint.h>
#include <assert.h>

/* Disable warnings due to broken header files from Microsoft... */
#pragma warning(push, 3)
#include <pdh.h>
#include <pdhmsg.h>
#include <process.h>
#pragma warning(pop)

typedef unsigned __int32 juint;
typedef unsigned __int64 julong;

static void set_low(jlong* value, jint low) {
    *value &= (jlong)0xffffffff << 32;
    *value |= (jlong)(julong)(juint)low;
}

static void set_high(jlong* value, jint high) {
    *value &= (jlong)(julong)(juint)0xffffffff;
    *value |= (jlong)high       << 32;
}

static jlong jlong_from(jint h, jint l) {
    jlong result = 0; // initialization to avoid warning
    set_high(&result, h);
    set_low(&result,  l);
    return result;
}

static HANDLE main_process;

static void perfInit(void);

JNIEXPORT void JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_initialize0
  (JNIEnv *env, jclass cls)
{
    main_process = GetCurrentProcess();
    perfInit();
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getCommittedVirtualMemorySize0
  (JNIEnv *env, jobject mbean)
{
    PROCESS_MEMORY_COUNTERS pmc;
    if (GetProcessMemoryInfo(main_process, &pmc, sizeof(PROCESS_MEMORY_COUNTERS)) == 0) {
        return (jlong)-1L;
    } else {
        return (jlong) pmc.PagefileUsage;
    }
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getTotalSwapSpaceSize0
  (JNIEnv *env, jobject mbean)
{
    MEMORYSTATUSEX ms;
    ms.dwLength = sizeof(ms);
    GlobalMemoryStatusEx(&ms);
    return (jlong) ms.ullTotalPageFile;
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getFreeSwapSpaceSize0
  (JNIEnv *env, jobject mbean)
{
    MEMORYSTATUSEX ms;
    ms.dwLength = sizeof(ms);
    GlobalMemoryStatusEx(&ms);
    return (jlong) ms.ullAvailPageFile;
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getProcessCpuTime0
  (JNIEnv *env, jobject mbean)
{

    FILETIME process_creation_time, process_exit_time,
             process_user_time, process_kernel_time;

    // Using static variables declared above
    // Units are 100-ns intervals.  Convert to ns.
    GetProcessTimes(main_process, &process_creation_time,
                    &process_exit_time,
                    &process_kernel_time, &process_user_time);
    return (jlong_from(process_user_time.dwHighDateTime,
                        process_user_time.dwLowDateTime) +
            jlong_from(process_kernel_time.dwHighDateTime,
                        process_kernel_time.dwLowDateTime)) * 100;
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getFreeMemorySize0
  (JNIEnv *env, jobject mbean)
{
    MEMORYSTATUSEX ms;
    ms.dwLength = sizeof(ms);
    GlobalMemoryStatusEx(&ms);
    return (jlong) ms.ullAvailPhys;
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getTotalMemorySize0
  (JNIEnv *env, jobject mbean)
{
    MEMORYSTATUSEX ms;
    ms.dwLength = sizeof(ms);
    GlobalMemoryStatusEx(&ms);
    return (jlong) ms.ullTotalPhys;
}

/* Performance Data Helper API (PDH) support */

typedef PDH_STATUS (WINAPI *PdhAddCounterFunc)(
                           HQUERY      hQuery,
                           LPCSTR      szFullCounterPath,
                           DWORD       dwUserData,
                           HCOUNTER    *phCounter
                           );
typedef PDH_STATUS (WINAPI *PdhOpenQueryFunc)(
                           LPCWSTR     szDataSource,
                           DWORD       dwUserData,
                           HQUERY      *phQuery
                           );
typedef PDH_STATUS (WINAPI *PdhCollectQueryDataFunc)(
                           HQUERY      hQuery
                           );

typedef PDH_STATUS (WINAPI *PdhEnumObjectItemsFunc)(
                           LPCTSTR     szDataSource,
                           LPCTSTR     szMachineName,
                           LPCTSTR     szObjectName,
                           LPTSTR      mszCounterList,
                           LPDWORD     pcchCounterListLength,
                           LPTSTR      mszInstanceList,
                           LPDWORD     pcchInstanceListLength,
                           DWORD       dwDetailLevel,
                           DWORD       dwFlags
                           );
typedef PDH_STATUS (WINAPI *PdhRemoveCounterFunc)(
                           HCOUNTER   hCounter
                           );
typedef PDH_STATUS (WINAPI *PdhLookupPerfNameByIndexFunc)(
                           LPCSTR     szMachineName,
                           DWORD      dwNameIndex,
                           LPSTR      szNameBuffer,
                           LPDWORD    pcchNameBufferSize
                           );
typedef DWORD (WINAPI *PdhCloseQueryFunc)(
                      HQUERY      hQuery
                      );

typedef DWORD (WINAPI *PdhGetFormattedCounterValueFunc)(
                      HCOUNTER                hCounter,
                      DWORD                   dwFormat,
                      LPDWORD                 lpdwType,
                      PPDH_FMT_COUNTERVALUE   pValue
                      );

static PdhAddCounterFunc PdhAddCounter_i;
static PdhOpenQueryFunc PdhOpenQuery_i;
static PdhCloseQueryFunc PdhCloseQuery_i;
static PdhCollectQueryDataFunc PdhCollectQueryData_i;
static PdhGetFormattedCounterValueFunc PdhGetFormattedCounterValue_i;
static PdhEnumObjectItemsFunc PdhEnumObjectItems_i;
static PdhRemoveCounterFunc PdhRemoveCounter_i;
static PdhLookupPerfNameByIndexFunc PdhLookupPerfNameByIndex_i;

/*
 * Struct for PDH queries.
 */
typedef struct {
    HQUERY      query;
    uint64_t    lastUpdate; // Last time query was updated (ticks)
} UpdateQueryS, *UpdateQueryP;

// Min time between query updates (ticks)
static const int MIN_UPDATE_INTERVAL = 500;

/*
 * Struct for a PDH query with multiple counters.
 */
typedef struct {
    UpdateQueryS  query;
    HCOUNTER*     counters;
    int           noOfCounters;
} MultipleCounterQueryS, *MultipleCounterQueryP;

/*
 * Struct for a PDH query with a single counter.
 */
typedef struct {
    UpdateQueryS  query;
    HCOUNTER      counter;
} SingleCounterQueryS, *SingleCounterQueryP;


typedef struct {
    CRITICAL_SECTION cs;
    DWORD owningThread;
    DWORD recursionCount;
} PdhCriticalSectionS, *PdhCriticalSectionP;

static PdhCriticalSectionS initializationLock;

static void InitializePdhCriticalSection(PdhCriticalSectionP criticalSection) {
    assert(criticalSection);

    InitializeCriticalSection(&criticalSection->cs);
    criticalSection->owningThread = 0;
    criticalSection->recursionCount = 0;
}

static void EnterPdhCriticalSection(PdhCriticalSectionP criticalSection) {
    assert(criticalSection);

    EnterCriticalSection(&criticalSection->cs);
    criticalSection->recursionCount++;
    if (!criticalSection->owningThread) {
        criticalSection->owningThread = GetCurrentThreadId();
    }
}

static void LeavePdhCriticalSection(PdhCriticalSectionP criticalSection) {
    assert(criticalSection);
    assert(GetCurrentThreadId() == criticalSection->owningThread);
    assert(criticalSection->recursionCount >= 1);

    criticalSection->recursionCount--;
    if (!criticalSection->recursionCount) {
        criticalSection->owningThread = 0;
    }
    LeaveCriticalSection(&criticalSection->cs);
}

/*
 * INFO: Using PDH APIs Correctly in a Localized Language (Q287159)
 *   http://support.microsoft.com/default.aspx?scid=kb;EN-US;q287159
 * The index value for the base system counters and objects like processor,
 * process, thread, memory, and so forth are always the same irrespective
 * of the localized version of the operating system or service pack installed.
 * To find the correct index for an object or counter, inspect the registry key/value:
 * [HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Perflib\009\Counter]
 */
static const DWORD PDH_PROCESSOR_IDX = 238;
static const DWORD PDH_PROCESSOR_TIME_IDX = 6;
static const DWORD PDH_PROCESS_IDX = 230;
static const DWORD PDH_ID_PROCESS_IDX = 784;

/* useful pdh fmt's */
static const char* const OBJECT_COUNTER_FMT = "\\%s\\%s";
static const size_t OBJECT_COUNTER_FMT_LEN = 2;
static const char* const OBJECT_WITH_INSTANCES_COUNTER_FMT = "\\%s(%s)\\%s";
static const size_t OBJECT_WITH_INSTANCES_COUNTER_FMT_LEN = 4;
static const char* const PROCESS_OBJECT_INSTANCE_COUNTER_FMT = "\\%s(%s#%s)\\%s";
static const size_t PROCESS_OBJECT_INSTANCE_COUNTER_FMT_LEN = 5;

static const char* pdhProcessImageName = NULL; /* "java" */
static char* pdhIDProcessCounterFmt = NULL;    /* "\Process(java#%d)\ID Process" */

static int numberOfJavaProcessesAtInitialization = 0;

/*
 * Currently used CPU queries/counters and variables
 */
static SingleCounterQueryP processTotalCPULoad = NULL;
static MultipleCounterQueryP multiCounterCPULoad = NULL;
static double cpuFactor = .0;
static DWORD  numCpus = 0;

/*
 * Seems WinXP PDH returns PDH_MORE_DATA whenever we send in a NULL buffer.
 * Let's just ignore it, since we make sure we have enough buffer anyway.
 */
static int
pdhFail(PDH_STATUS pdhStat) {
    return pdhStat != ERROR_SUCCESS && pdhStat != PDH_MORE_DATA;
}

static const char*
allocateAndCopy(const char* const originalString) {
    size_t len;
    char* allocatedString;

    assert(originalString);

    len = strlen(originalString);

    allocatedString = malloc(len + 1);

    if (!allocatedString) {
        return NULL;
    }

    strncpy(allocatedString, originalString, len);
    allocatedString[len] = '\0';

    return allocatedString;
}

/*
 * Allocates memory into the supplied pointer and
 * fills it with the localized PDH artifact description, if indexed correctly.
 * Caller owns the memory from the point of returning from this function.
 *
 * @param index    the PDH counter index as specified in the registry
 * @param ppBuffer pointer to a char*.
 * @return         0 if successful, negative on failure.
 */
static int
lookupNameByIndex(DWORD index, char** ppBuffer) {
    DWORD size;

    assert(ppBuffer);

    /* determine size needed */
    if (PdhLookupPerfNameByIndex_i(NULL, index, NULL, &size) != PDH_MORE_DATA) {
      /* invalid index? */
      return -1;
    }

    *ppBuffer = malloc((size_t)size);

    if (!*ppBuffer) {
        return -1;
    }

    if (PdhLookupPerfNameByIndex_i(NULL, index, *ppBuffer, &size) != ERROR_SUCCESS) {
        free(*ppBuffer);
        *ppBuffer = NULL;
        return -1;
    }

    /* windows vista does not null-terminate the string
     * (although the docs says it will) */
    (*ppBuffer)[size - 1] = '\0';

    return 0;
}

/*
* Construct a fully qualified PDH path
*
* @param objectName   a PDH Object string representation (required)
* @param counterName  a PDH Counter string representation (required)
* @param imageName    a process image name string, ex. "java" (opt)
* @param instance     an instance string, ex. "0", "1", ... (opt)
* @return             the fully qualified PDH path.
*
* Caller will own the returned malloc:ed string
*/
static const char*
makeFullCounterPath(const char* const objectName,
                    const char* const counterName,
                    const char* const imageName,
                    const char* const instance) {

    size_t fullCounterPathLen;
    char* fullCounterPath;

    assert(objectName);
    assert(counterName);

    fullCounterPathLen = strlen(objectName);
    fullCounterPathLen += strlen(counterName);

    if (imageName) {
        /*
         * For paths using the "Process" Object.
         *
         * Examples:
         * abstract: "\Process(imageName#instance)\Counter"
         * actual:   "\Process(java#2)\ID Process"
         */
        fullCounterPathLen += PROCESS_OBJECT_INSTANCE_COUNTER_FMT_LEN;
        fullCounterPathLen += strlen(imageName);

        /*
         * imageName must be passed together with an associated
         * instance "number" ("0", "1", "2", ...).
         * This is required in order to create valid "Process" Object paths.
         *
         * Examples: "\Process(java#0)", \Process(java#1"), ...
         */
        assert(instance);

        fullCounterPathLen += strlen(instance);

        fullCounterPath = malloc(fullCounterPathLen + 1);

        if (!fullCounterPath) {
            return NULL;
        }

        _snprintf(fullCounterPath,
                  fullCounterPathLen,
                  PROCESS_OBJECT_INSTANCE_COUNTER_FMT,
                  objectName,
                  imageName,
                  instance,
                  counterName);
    } else {
        if (instance) {
            /*
             * For paths where the Object has multiple instances.
             *
             * Examples:
             * abstract: "\Object(instance)\Counter"
             * actual:   "\Processor(0)\% Privileged Time"
             */
            fullCounterPathLen += strlen(instance);
            fullCounterPathLen += OBJECT_WITH_INSTANCES_COUNTER_FMT_LEN;
        } else {
            /*
             * For "normal" paths.
             *
             * Examples:
             * abstract: "\Object\Counter"
             * actual:   "\Memory\Available Mbytes"
             */
            fullCounterPathLen += OBJECT_COUNTER_FMT_LEN;
        }

        fullCounterPath = malloc(fullCounterPathLen + 1);

        if (!fullCounterPath) {
            return NULL;
        }

        if (instance) {
            _snprintf(fullCounterPath,
                      fullCounterPathLen,
                      OBJECT_WITH_INSTANCES_COUNTER_FMT,
                      objectName,
                      instance,
                      counterName);
        } else {
            _snprintf(fullCounterPath,
                      fullCounterPathLen,
                      OBJECT_COUNTER_FMT,
                      objectName,
                      counterName);
        }
    }

    fullCounterPath[fullCounterPathLen] = '\0';

    return fullCounterPath;
}

/*
 * Resolves an index for a PDH artifact to
 * a localized, malloc:ed string representation.
 * Caller will own the returned malloc:ed string.
 *
 * @param pdhArtifactIndex  PDH index
 * @return                  malloc:ed string representation
 *                          of the requested pdh artifact (localized).
 *                          NULL on failure.
 */
static const char*
getPdhLocalizedArtifact(DWORD pdhArtifactIndex) {
    char* pdhLocalizedArtifactString;

    if (lookupNameByIndex(pdhArtifactIndex,
                          &pdhLocalizedArtifactString) != 0) {
        return NULL;
    }

    return pdhLocalizedArtifactString;
}

static void
pdhCleanup(HQUERY* const query, HCOUNTER* const counter) {
    if (counter && *counter) {
        PdhRemoveCounter_i(*counter);
        *counter = NULL;
    }
    if (query && *query) {
        PdhCloseQuery_i(*query);
        *query = NULL;
    }
}

static void
destroySingleCounter(SingleCounterQueryP counterQuery) {
    if (counterQuery) {
        pdhCleanup(&counterQuery->query.query, &counterQuery->counter);
    }
}

static void
destroyMultiCounter(MultipleCounterQueryP multiCounterQuery) {
    int i;
    if (multiCounterQuery) {
        if (multiCounterQuery->counters) {
            for (i = 0; i < multiCounterQuery->noOfCounters; i++) {
                pdhCleanup(NULL, &multiCounterQuery->counters[i]);
            }
            free(multiCounterQuery->counters);
            multiCounterQuery->counters = NULL;
        }
        pdhCleanup(&multiCounterQuery->query.query, NULL);
    }
}

static int
openQuery(HQUERY* const query) {
    assert(query);

    if (PdhOpenQuery_i(NULL, 0, query) != ERROR_SUCCESS) {
        return -1;
    }

    return 0;
}

static int
addCounter(HQUERY query,
           const char* const fullCounterPath,
           HCOUNTER* const counter) {

    assert(fullCounterPath);
    assert(counter);

    if (PdhAddCounter_i(query,
                        fullCounterPath,
                        0,
                        counter) != ERROR_SUCCESS) {
        return -1;
    }

    return 0;
}

/*
 * Sets up the supplied SingleCounterQuery to listen for the specified counter.
 *
 * @param counterQuery       the counter query to set up.
 * @param fullCounterPath    the string specifying the full path to the counter.
 * @returns                  0 if successful, negative on failure.
 */
static int
initializeSingleCounterQuery(SingleCounterQueryP counterQuery,
                             const char* const fullCounterPath) {
    assert(counterQuery);
    assert(fullCounterPath);

    if (openQuery(&counterQuery->query.query) == 0) {
        if (addCounter(counterQuery->query.query,
                       fullCounterPath,
                       &counterQuery->counter) == 0) {
            return 0;
        }
    }

    return -1;
}

/*
 * Sets up a SingleCounterQuery
 *
 * param counter             the counter query to set up.
 * param localizedObject     string representing the PDH object to query
 * param localizedCounter    string representing the PDH counter to query
 * param processImageName    if the counter query needs the process image name ("java")
 * param instance            if the counter has instances, this is the instance ("\Processor(0)\")
                                 where 0 is the instance
 * param firstSampleOnInit   for counters that need two queries to yield their values,
                                 the first query can be issued just after initialization
 *
 * @returns                   0 if successful, negative on failure.
 */
static int
initializeSingleCounter(SingleCounterQueryP const counter,
                        const char* const localizedObject,
                        const char* const localizedCounter,
                        const char* const processImageName,
                        const char* const instance,
                        BOOL firstSampleOnInit) {
    int retValue = -1;

    const char* fullCounterPath = makeFullCounterPath(localizedObject,
                                                      localizedCounter,
                                                      processImageName,
                                                      instance);

    if (fullCounterPath) {

        assert(counter);

        if (initializeSingleCounterQuery(counter, fullCounterPath) == 0) {
            /*
             * According to the MSDN documentation, rate counters must be read twice:
             *
             * "Obtaining the value of rate counters such as Page faults/sec requires that
             *  PdhCollectQueryData be called twice, with a specific time interval between
             *  the two calls, before calling PdhGetFormattedCounterValue. Call Sleep to
             *  implement the waiting period between the two calls to PdhCollectQueryData."
             *
             *  Take the first sample here already to allow for the next (first) "real" sample
             *  to succeed.
             */
            if (firstSampleOnInit) {
                PdhCollectQueryData_i(counter->query.query);
            }

            retValue = 0;
        }
        free((char*)fullCounterPath);
    }

    return retValue;
}

static void
perfInit(void) {
    InitializePdhCriticalSection(&initializationLock);
}

static int
getProcessID() {
    static int myPid = 0;
    if (0 == myPid) {
        myPid = _getpid();
    }
    return myPid;
}

/*
 * Working against the Process object and it's related counters is inherently problematic
 * when using the PDH API:
 *
 * For PDH, a process is not primarily identified by it's process id,
 * but with a sequential number, for example \Process(java#0), \Process(java#1), ....
 * The really bad part is that this list is reset as soon as one process exits:
 * If \Process(java#1) exits, \Process(java#3) now becomes \Process(java#2) etc.
 *
 * The PDH query api requires a process identifier to be submitted when registering
 * a query, but as soon as the list resets, the query is invalidated (since the name
 * changed).
 *
 * Solution:
 * The #number identifier for a Process query can only decrease after process creation.
 *
 * Therefore we create an array of counter queries for all process object instances
 * up to and including ourselves:
 *
 * Ex. we come in as third process instance (java#2), we then create and register
 * queries for the following Process object instances:
 * java#0, java#1, java#2
 *
 * currentQueryIndexForProcess() keeps track of the current "correct" query
 * (in order to keep this index valid when the list resets from underneath,
 * ensure to call getCurrentQueryIndexForProcess() before every query involving
 * Process object instance data).
 */
static int
currentQueryIndexForProcess(void) {
    HQUERY tmpQuery = NULL;
    HCOUNTER handleCounter = NULL;
    int retValue = -1;

    assert(pdhProcessImageName);
    assert(pdhIDProcessCounterFmt);

    if (openQuery(&tmpQuery) == 0) {
        int index;

        /* iterate over all instance indexes and try to find our own pid */
        for (index = 0; index < INT_MAX; ++index) {
            char fullIDProcessCounterPath[MAX_PATH];
            PDH_FMT_COUNTERVALUE counterValue;
            PDH_STATUS res;

            _snprintf(fullIDProcessCounterPath,
                      MAX_PATH,
                      pdhIDProcessCounterFmt,
                      index);

            if (addCounter(tmpQuery, fullIDProcessCounterPath, &handleCounter) != 0) {
                break;
            }

            res = PdhCollectQueryData_i(tmpQuery);

            if (PDH_INVALID_HANDLE == res || PDH_NO_DATA == res) {
                break;
            }

            PdhGetFormattedCounterValue_i(handleCounter,
                                          PDH_FMT_LONG,
                                          NULL,
                                          &counterValue);
            /*
             * This check seems to be needed for Win2k SMP boxes, since
             * they for some reason don't return PDH_NO_DATA for non existing
             * counters.
             */
            if (counterValue.CStatus != PDH_CSTATUS_VALID_DATA) {
                break;
            }

            if ((LONG)getProcessID() == counterValue.longValue) {
                retValue = index;
                break;
            }
        }
    }

    pdhCleanup(&tmpQuery, &handleCounter);

    return retValue;
}

/*
 * If successful, returns the #index corresponding to our PID
 * as resolved by the pdh query:
 * "\Process(java#index)\ID Process" (or localized equivalent)
 *
 * This function should be called before attempting to read
 * from any Process related counter(s), and the return value
 * is the index to be used for indexing an array of Process object query's:
 *
 * Example:
 * processTotalCPULoad[currentQueryIndex].query
 *
 * Returns -1 on failure.
 */
static int
getCurrentQueryIndexForProcess() {
    int currentQueryIndex = currentQueryIndexForProcess();

    assert(currentQueryIndex >= 0 &&
           currentQueryIndex < numberOfJavaProcessesAtInitialization);

    return currentQueryIndex;
}

/*
 * Returns the PDH string identifying the current process image name.
 * Use this name as a qualifier when getting counters from the PDH Process Object
 * representing your process.

 * Example:
 * "\Process(java#0)\Virtual Bytes" - where "java" is the PDH process
 * image name.
 *
 * Please note that the process image name is not necessarily "java",
 * hence the use of GetModuleFileName() to detect the process image name.
 *
 * @return   the process image name to be used when retrieving
 *           PDH counters from the current process. The caller will
             own the returned malloc:ed string. NULL if failure.
 */
static const char*
getPdhProcessImageName() {
    char moduleName[MAX_PATH];
    char* processImageName;
    char* dotPos;

    // Find our module name and use it to extract the image name used by PDH
    DWORD getmfnReturn = GetModuleFileName(NULL, moduleName, sizeof(moduleName));

    if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
        return NULL;
    }

    if (getmfnReturn >= MAX_PATH || 0 == getmfnReturn) {
        return NULL;
    }

    processImageName = strrchr(moduleName, '\\'); //drop path
    processImageName++;                           //skip slash
    dotPos = strrchr(processImageName, '.');      //drop .exe
    dotPos[0] = '\0';

    return allocateAndCopy(processImageName);
}

/*
 * Sets up the supplied MultipleCounterQuery to check on the processors via PDH CPU counters.
 * TODO: Refactor and prettify as with the the SingleCounter queries
 * if more MultipleCounterQueries are discovered/needed.
 *
 * @param multiCounterCPULoad  a pointer to a MultipleCounterQueryS, will be filled in with
 *                             the necessary info to check the PDH processor counters.
 * @return                     0 if successful, negative on failure.
 */
static int
initializeMultipleCounterForCPUs(MultipleCounterQueryP multiCounterCPULoad) {
    DWORD cSize = 0;
    DWORD iSize = 0;
    DWORD pCount;
    DWORD index;
    char* processor = NULL; //'Processor' == PDH_PROCESSOR_IDX
    char* time = NULL;      //'Time' == PDH_PROCESSOR_TIME_IDX
    char* instances = NULL;
    char* tmp;
    int   retValue = -1;
    PDH_STATUS pdhStat;

    if (lookupNameByIndex(PDH_PROCESSOR_IDX, &processor) != 0) {
        goto end;
    }

    if (lookupNameByIndex(PDH_PROCESSOR_TIME_IDX, &time) != 0) {
        goto end;
    }

    //ok, now we have enough to enumerate all processors.
    pdhStat = PdhEnumObjectItems_i(
                                   NULL, // reserved
                                   NULL, // local machine
                                   processor, // object to enumerate
                                   NULL, // pass in NULL buffers
                                   &cSize, // and 0 length to get
                                   NULL, // required size
                                   &iSize, // of the buffers in chars
                                   PERF_DETAIL_WIZARD, // counter detail level
                                   0);

    if (pdhFail(pdhStat)) {
        goto end;
    }

    instances = calloc(iSize, 1);

    if (!instances) {
        goto end;
    }

    cSize = 0;

    pdhStat = PdhEnumObjectItems_i(
                                   NULL, // reserved
                                   NULL, // local machine
                                   processor, // object to enumerate
                                   NULL, // pass in NULL buffers
                                   &cSize,
                                   instances, // now allocated to be filled in
                                   &iSize, // and size is known
                                   PERF_DETAIL_WIZARD, // counter detail level
                                   0);

    if (pdhFail(pdhStat)) {
        goto end;
    }

    // enumerate the Processor instances ("\Processor(0)", "\Processor(1)", ..., "\Processor(_Total)")
    for (pCount = 0, tmp = instances; *tmp != '\0'; tmp = &tmp[strlen(tmp)+1], pCount++);

    assert(pCount == numCpus+1);

    //ok, we now have the number of Processor instances - allocate an HCOUNTER for each
    multiCounterCPULoad->counters = (HCOUNTER*)malloc(pCount * sizeof(HCOUNTER));

    if (!multiCounterCPULoad->counters) {
        goto end;
    }

    multiCounterCPULoad->noOfCounters = pCount;

    if (openQuery(&multiCounterCPULoad->query.query) != 0) {
        goto end;
    }

    // fetch instance and register its corresponding HCOUNTER with the query
    for (index = 0, tmp = instances; *tmp != '\0'; tmp = &tmp[strlen(tmp)+1], ++index) {
        const char* const fullCounterPath = makeFullCounterPath(processor, time, NULL, tmp);

        if (!fullCounterPath) {
            goto end;
        }

        retValue = addCounter(multiCounterCPULoad->query.query,
                              fullCounterPath,
                              &multiCounterCPULoad->counters[index]);

        free((char*)fullCounterPath);

        if (retValue != 0) {
            goto end;
        }
    }

    // Query once to initialize the counters which require at least two samples
    // (like the % CPU usage) to calculate correctly.
    PdhCollectQueryData_i(multiCounterCPULoad->query.query);

  end:
    if (processor) {
        free(processor);
    }

    if (time) {
        free(time);
    }

    if (instances) {
        free(instances);
    }

    return retValue;
}

/*
 * Dynamically sets up function pointers to the PDH library.
 *
 * @param h  HMODULE for the PDH library
 * @return   0 on success, negative on failure.
 */
static int
bindPdhFunctionPointers(HMODULE h) {
    assert(h);
    assert(GetCurrentThreadId() == initializationLock.owningThread);

    /* The 'A' at the end means the ANSI (not the UNICODE) vesions of the methods */
    PdhAddCounter_i         = (PdhAddCounterFunc)GetProcAddress(h, "PdhAddCounterA");
    PdhOpenQuery_i         = (PdhOpenQueryFunc)GetProcAddress(h, "PdhOpenQueryA");
    PdhCloseQuery_i         = (PdhCloseQueryFunc)GetProcAddress(h, "PdhCloseQuery");
    PdhCollectQueryData_i     = (PdhCollectQueryDataFunc)GetProcAddress(h, "PdhCollectQueryData");
    PdhGetFormattedCounterValue_i = (PdhGetFormattedCounterValueFunc)GetProcAddress(h, "PdhGetFormattedCounterValue");
    PdhEnumObjectItems_i         = (PdhEnumObjectItemsFunc)GetProcAddress(h, "PdhEnumObjectItemsA");
    PdhRemoveCounter_i         = (PdhRemoveCounterFunc)GetProcAddress(h, "PdhRemoveCounter");
    PdhLookupPerfNameByIndex_i     = (PdhLookupPerfNameByIndexFunc)GetProcAddress(h, "PdhLookupPerfNameByIndexA");

    if (!PdhAddCounter_i || !PdhOpenQuery_i ||
        !PdhCloseQuery_i || !PdhCollectQueryData_i ||
        !PdhGetFormattedCounterValue_i || !PdhEnumObjectItems_i ||
        !PdhRemoveCounter_i || !PdhLookupPerfNameByIndex_i)
    {
        return -1;
    }
    return 0;
}

/*
 * Returns the counter value as a double for the specified query.
 * Will collect the query data and update the counter values as necessary.
 *
 * @param query       the query to update (if needed).
 * @param c           the counter to read.
 * @param value       where to store the formatted value.
 * @param format      the format to use (i.e. PDH_FMT_DOUBLE, PDH_FMT_LONG etc)
 * @return            0 if no error
 *                    -1 if PdhCollectQueryData fails
 *                    -2 if PdhGetFormattedCounterValue fails
 */
static int
getPerformanceData(UpdateQueryP query, HCOUNTER c, PDH_FMT_COUNTERVALUE* value, DWORD format) {
    clock_t now = clock();

    /*
     * Need to limit how often we update the query
     * to minimize the Heisenberg effect.
     * (PDH behaves erratically if the counters are
     * queried too often, especially counters that
     * store and use values from two consecutive updates,
     * like cpu load.)
     */
    if (now - query->lastUpdate > MIN_UPDATE_INTERVAL) {
        if (PdhCollectQueryData_i(query->query) != ERROR_SUCCESS) {
            return -1;
        }
        query->lastUpdate = now;
    }

    if (PdhGetFormattedCounterValue_i(c, format, NULL, value) != ERROR_SUCCESS) {
        return -2;
    }

    return 0;
}

static int
allocateAndInitializePdhConstants() {
    const char* pdhLocalizedProcessObject = NULL;
    const char* pdhLocalizedIDProcessCounter = NULL;
    size_t pdhIDProcessCounterFmtLen;
    int currentQueryIndex;
    int retValue = -1;

    assert(GetCurrentThreadId() == initializationLock.owningThread);

    assert(!pdhProcessImageName);
    pdhProcessImageName = getPdhProcessImageName();
    if (!pdhProcessImageName) {
        goto end;
    }

    pdhLocalizedProcessObject = getPdhLocalizedArtifact(PDH_PROCESS_IDX);
    if (!pdhLocalizedProcessObject) {
        goto end;
    }

    pdhLocalizedIDProcessCounter = getPdhLocalizedArtifact(PDH_ID_PROCESS_IDX);
    if (!pdhLocalizedIDProcessCounter) {
        goto end;
    }

    assert(!pdhIDProcessCounterFmt);

    pdhIDProcessCounterFmtLen = strlen(pdhProcessImageName);
    pdhIDProcessCounterFmtLen += strlen(pdhLocalizedProcessObject);
    pdhIDProcessCounterFmtLen += strlen(pdhLocalizedIDProcessCounter);
    pdhIDProcessCounterFmtLen += PROCESS_OBJECT_INSTANCE_COUNTER_FMT_LEN;
    pdhIDProcessCounterFmtLen += 2; // "%d"

    assert(pdhIDProcessCounterFmtLen < MAX_PATH);
    pdhIDProcessCounterFmt = malloc(pdhIDProcessCounterFmtLen + 1);
    if (!pdhIDProcessCounterFmt) {
        goto end;
    }

    /* "\Process(java#%d)\ID Process" */
    _snprintf(pdhIDProcessCounterFmt,
              pdhIDProcessCounterFmtLen,
              PROCESS_OBJECT_INSTANCE_COUNTER_FMT,
              pdhLocalizedProcessObject,
              pdhProcessImageName,
              "%d",
              pdhLocalizedIDProcessCounter);

    pdhIDProcessCounterFmt[pdhIDProcessCounterFmtLen] = '\0';

    assert(0 == numberOfJavaProcessesAtInitialization);
    currentQueryIndex = currentQueryIndexForProcess();
    if (-1 == currentQueryIndex) {
        goto end;
    }

    numberOfJavaProcessesAtInitialization = currentQueryIndex + 1;
    assert(numberOfJavaProcessesAtInitialization >= 1);

    retValue = 0;

  end:

    if (pdhLocalizedProcessObject) {
        free((char*)pdhLocalizedProcessObject);
    }

    if (pdhLocalizedIDProcessCounter) {
        free((char*)pdhLocalizedIDProcessCounter);
    }

    return retValue;
}

static void
deallocatePdhConstants() {
    assert(GetCurrentThreadId() == initializationLock.owningThread);

    if (pdhProcessImageName) {
        free((char*)pdhProcessImageName);
        pdhProcessImageName = NULL;
    }

    if (pdhIDProcessCounterFmt) {
      free(pdhIDProcessCounterFmt);
      pdhIDProcessCounterFmt = NULL;
    }

    numberOfJavaProcessesAtInitialization = 0;
}

static int
initializeCPUCounters() {
    SYSTEM_INFO si;
    char* localizedProcessObject;
    char* localizedProcessorTimeCounter;
    int i;
    int retValue = -1;

    assert(GetCurrentThreadId() == initializationLock.owningThread);

    assert(0 == numCpus);
    GetSystemInfo(&si);
    numCpus = si.dwNumberOfProcessors;
    assert(numCpus >= 1);

    /* Initialize the denominator for the jvm load calculations */
    assert(.0 == cpuFactor);
    cpuFactor = numCpus * 100;

    if (lookupNameByIndex(PDH_PROCESS_IDX,
                          &localizedProcessObject) == 0) {

        if (lookupNameByIndex(PDH_PROCESSOR_TIME_IDX,
                              &localizedProcessorTimeCounter) == 0) {

            assert(processTotalCPULoad);
            assert(pdhProcessImageName);

            for (i = 0; i < numberOfJavaProcessesAtInitialization; ++i) {
                char instanceIndexBuffer[32];
                retValue = initializeSingleCounter(&processTotalCPULoad[i],
                                                   localizedProcessObject,
                                                   localizedProcessorTimeCounter,
                                                   pdhProcessImageName,
                                                   itoa(i, instanceIndexBuffer, 10),
                                                   TRUE);
                if (retValue != 0) {
                    break;
                }
            }
            free(localizedProcessorTimeCounter);
        }
        free(localizedProcessObject);
    }

    if (retValue != 0) {
        return -1;
    }

    assert(multiCounterCPULoad);
    return initializeMultipleCounterForCPUs(multiCounterCPULoad);
}

static void
deallocateCPUCounters() {
    int i;

    assert(GetCurrentThreadId() == initializationLock.owningThread);

    if (processTotalCPULoad) {
        for (i = 0; i < numberOfJavaProcessesAtInitialization; ++i) {
            destroySingleCounter(&processTotalCPULoad[i]);
        }
        free(processTotalCPULoad);
        processTotalCPULoad = NULL;
    }

    if (multiCounterCPULoad) {
        destroyMultiCounter(multiCounterCPULoad);
        free(multiCounterCPULoad);
        multiCounterCPULoad = NULL;
    }

    cpuFactor = .0;
    numCpus = 0;
}

static void
pdhInitErrorHandler(HMODULE h) {
    assert(GetCurrentThreadId() == initializationLock.owningThread);

    deallocatePdhConstants();

    if (h) {
        FreeLibrary(h);
    }
}

/*
 * Helper to initialize the PDH library, function pointers and constants.
 *
 * @return  0 if successful, negative on failure.
 */
static int
pdhInit() {
    static BOOL initialized = FALSE;
    int retValue;

    if (initialized) {
        return 0;
    }

    retValue = 0;

    EnterPdhCriticalSection(&initializationLock); {
        if (!initialized) {
            HMODULE h = NULL;
            if ((h = LoadLibrary("pdh.dll")) == NULL) {
                retValue = -1;
            } else if (bindPdhFunctionPointers(h) < 0) {
                retValue = -1;
            } else if (allocateAndInitializePdhConstants() < 0) {
                retValue = -1;
            }

            if (0 == retValue) {
                initialized = TRUE;
            } else {
                pdhInitErrorHandler(h);
            }
        }
    } LeavePdhCriticalSection(&initializationLock);

    return retValue;
}

static int
allocateCPUCounters() {
    assert(GetCurrentThreadId() == initializationLock.owningThread);
    assert(numberOfJavaProcessesAtInitialization >= 1);
    assert(!processTotalCPULoad);
    assert(!multiCounterCPULoad);

    /*
     * Create an array of Process object queries, for each instance
     * up to and including our own (java#0, java#1, java#2, ...).
     */
    processTotalCPULoad = calloc(numberOfJavaProcessesAtInitialization,
                                 sizeof(SingleCounterQueryS));

    if (!processTotalCPULoad) {
        return -1;
    }

    multiCounterCPULoad = calloc(1, sizeof(MultipleCounterQueryS));

    if (!multiCounterCPULoad) {
        return -1;
    }

    return 0;
}

static int
initializePdhCPUCounters() {
    static BOOL initialized = FALSE;
    int retValue;

    if (initialized) {
        return 0;
    }

    retValue = 0;

    EnterPdhCriticalSection(&initializationLock); {
        if (!initialized) {
            if (pdhInit() < 0) {
                retValue = -1;
            }  else if (allocateCPUCounters() < 0) {
                retValue = -1;
            } else if (initializeCPUCounters() < 0) {
                retValue = -1;
            }

            if (0 == retValue) {
                initialized = TRUE;
            } else {
              deallocateCPUCounters();
            }
        }
    } LeavePdhCriticalSection(&initializationLock);

    return retValue;
}

static int
perfCPUInit() {
    return initializePdhCPUCounters();
}

static double
perfGetProcessCPULoad() {
    PDH_FMT_COUNTERVALUE cv;
    int currentQueryIndex;

    if (perfCPUInit() < 0) {
        // warn?
        return -1.0;
    }

    currentQueryIndex = getCurrentQueryIndexForProcess();

    if (getPerformanceData(&processTotalCPULoad[currentQueryIndex].query,
                           processTotalCPULoad[currentQueryIndex].counter,
                           &cv,
                           PDH_FMT_DOUBLE | PDH_FMT_NOCAP100) == 0) {
        double d = cv.doubleValue / cpuFactor;
        d = min(1, d);
        d = max(0, d);
        return d;
    }
    return -1.0;
}

static double
perfGetCPULoad(int which) {
    PDH_FMT_COUNTERVALUE cv;
    HCOUNTER c;

    if (perfCPUInit() < 0) {
        // warn?
        return -1.0;
    }

    if (-1 == which) {
        c = multiCounterCPULoad->counters[multiCounterCPULoad->noOfCounters - 1];
    } else {
        if (which < multiCounterCPULoad->noOfCounters) {
            c = multiCounterCPULoad->counters[which];
        } else {
            return -1.0;
        }
    }
    if (getPerformanceData(&multiCounterCPULoad->query, c, &cv, PDH_FMT_DOUBLE ) == 0) {
        return cv.doubleValue / 100;
    }
    return -1.0;
}

JNIEXPORT jdouble JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getCpuLoad0
(JNIEnv *env, jobject dummy)
{
    return perfGetCPULoad(-1);
}

JNIEXPORT jdouble JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getProcessCpuLoad0
(JNIEnv *env, jobject dummy)
{
    return perfGetProcessCPULoad();
}
