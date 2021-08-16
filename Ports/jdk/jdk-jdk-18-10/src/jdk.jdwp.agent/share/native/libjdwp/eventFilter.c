/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
/*
 * eventFilter
 *
 * This module handles event filteration and the enabling/disabling
 * of the corresponding events. Used for filters on JDI EventRequests
 * and also internal requests.  Our data is in a private hidden section
 * of the HandlerNode's data.  See comment for enclosing
 * module eventHandler.
 */

#include "util.h"
#include "eventFilter.h"
#include "eventFilterRestricted.h"
#include "eventHandlerRestricted.h"
#include "stepControl.h"
#include "threadControl.h"
#include "SDE.h"
#include "jvmti.h"

typedef struct ClassFilter {
    jclass clazz;
} ClassFilter;

typedef struct LocationFilter {
    jclass clazz;
    jmethodID method;
    jlocation location;
} LocationFilter;

typedef struct ThreadFilter {
    jthread thread;
} ThreadFilter;

typedef struct CountFilter {
    jint count;
} CountFilter;

typedef struct ConditionalFilter {
    jint exprID;
} ConditionalFilter;

typedef struct FieldFilter {
    jclass clazz;
    jfieldID field;
} FieldFilter;

typedef struct ExceptionFilter {
    jclass exception;
    jboolean caught;
    jboolean uncaught;
} ExceptionFilter;

typedef struct InstanceFilter {
    jobject instance;
} InstanceFilter;

typedef struct StepFilter {
    jint size;
    jint depth;
    jthread thread;
} StepFilter;

typedef struct MatchFilter {
    char *classPattern;
} MatchFilter;

typedef struct SourceNameFilter {
    char *sourceNamePattern;
} SourceNameFilter;

typedef struct Filter_ {
    jbyte modifier;
    union {
        struct ClassFilter ClassOnly;
        struct LocationFilter LocationOnly;
        struct ThreadFilter ThreadOnly;
        struct CountFilter Count;
        struct ConditionalFilter Conditional;
        struct FieldFilter FieldOnly;
        struct ExceptionFilter ExceptionOnly;
        struct InstanceFilter InstanceOnly;
        struct StepFilter Step;
        struct MatchFilter ClassMatch;
        struct MatchFilter ClassExclude;
        struct SourceNameFilter SourceNameOnly;
    } u;
} Filter;

/* The filters array is allocated to the specified filterCount.
 * Theoretically, some compiler could do range checking on this
 * array - so, we define it to have a ludicrously large size so
 * that this range checking won't get upset.
 *
 * The actual allocated number of bytes is computed using the
 * offset of "filters" and so is not effected by this number.
 */
#define MAX_FILTERS 10000

typedef struct EventFilters_ {
    jint filterCount;
    Filter filters[MAX_FILTERS];
} EventFilters;

typedef struct EventFilterPrivate_HandlerNode_ {
    EventHandlerRestricted_HandlerNode   not_for_us;
    EventFilters                         ef;
} EventFilterPrivate_HandlerNode;

/**
 * The following macros extract filter info (EventFilters) from private
 * data at the end of a HandlerNode
 */
#define EVENT_FILTERS(node) (&(((EventFilterPrivate_HandlerNode*)(void*)node)->ef))
#define FILTER_COUNT(node)  (EVENT_FILTERS(node)->filterCount)
#define FILTERS_ARRAY(node) (EVENT_FILTERS(node)->filters)
#define FILTER(node,index)  ((FILTERS_ARRAY(node))[index])
#define NODE_EI(node)          (node->ei)

/***** filter set-up / destruction *****/

/**
 * Allocate a HandlerNode.
 * We do it because eventHandler doesn't know how big to make it.
 */
HandlerNode *
eventFilterRestricted_alloc(jint filterCount)
{
    /*LINTED*/
    size_t size = offsetof(EventFilterPrivate_HandlerNode, ef) +
                  offsetof(EventFilters, filters) +
                  (filterCount * (int)sizeof(Filter));
    HandlerNode *node = jvmtiAllocate((jint)size);

    if (node != NULL) {
        int i;
        Filter *filter;

        (void)memset(node, 0, size);

        FILTER_COUNT(node) = filterCount;

        /* Initialize all modifiers
         */
        for (i = 0, filter = FILTERS_ARRAY(node);
                                    i < filterCount;
                                    i++, filter++) {
            filter->modifier = JDWP_REQUEST_NONE;
        }
    }

    return node;
}

/**
 * Free up global refs held by the filter.
 * free things up at the JNI level if needed.
 */
static jvmtiError
clearFilters(HandlerNode *node)
{
    JNIEnv *env = getEnv();
    jint i;
    jvmtiError error = JVMTI_ERROR_NONE;
    Filter *filter = FILTERS_ARRAY(node);

    for (i = 0; i < FILTER_COUNT(node); ++i, ++filter) {
        switch (filter->modifier) {
            case JDWP_REQUEST_MODIFIER(ThreadOnly):
                if ( filter->u.ThreadOnly.thread != NULL ) {
                    tossGlobalRef(env, &(filter->u.ThreadOnly.thread));
                }
                break;
            case JDWP_REQUEST_MODIFIER(LocationOnly):
                tossGlobalRef(env, &(filter->u.LocationOnly.clazz));
                break;
            case JDWP_REQUEST_MODIFIER(FieldOnly):
                tossGlobalRef(env, &(filter->u.FieldOnly.clazz));
                break;
            case JDWP_REQUEST_MODIFIER(ExceptionOnly):
                if ( filter->u.ExceptionOnly.exception != NULL ) {
                    tossGlobalRef(env, &(filter->u.ExceptionOnly.exception));
                }
                break;
            case JDWP_REQUEST_MODIFIER(InstanceOnly):
                if ( filter->u.InstanceOnly.instance != NULL ) {
                    tossGlobalRef(env, &(filter->u.InstanceOnly.instance));
                }
                break;
            case JDWP_REQUEST_MODIFIER(ClassOnly):
                tossGlobalRef(env, &(filter->u.ClassOnly.clazz));
                break;
            case JDWP_REQUEST_MODIFIER(ClassMatch):
                jvmtiDeallocate(filter->u.ClassMatch.classPattern);
                break;
            case JDWP_REQUEST_MODIFIER(ClassExclude):
                jvmtiDeallocate(filter->u.ClassExclude.classPattern);
                break;
            case JDWP_REQUEST_MODIFIER(Step): {
                jthread thread = filter->u.Step.thread;
                error = stepControl_endStep(thread);
                if (error == JVMTI_ERROR_NONE) {
                    tossGlobalRef(env, &(filter->u.Step.thread));
                }
                break;
            }
        }
    }
    if (error == JVMTI_ERROR_NONE) {
        FILTER_COUNT(node) = 0; /* blast so we don't clear again */
    }

    return error;
}


/***** filtering *****/

/*
 * Match a string against a wildcard
 * string pattern.
 */
static jboolean
patternStringMatch(char *classname, const char *pattern)
{
    int pattLen;
    int compLen;
    char *start;
    int offset;

    if ( pattern==NULL || classname==NULL ) {
        return JNI_FALSE;
    }
    pattLen = (int)strlen(pattern);

    if ((pattern[0] != '*') && (pattern[pattLen-1] != '*')) {
        /* An exact match is required when there is no *: bug 4331522 */
        return strcmp(pattern, classname) == 0;
    } else {
        compLen = pattLen - 1;
        offset = (int)strlen(classname) - compLen;
        if (offset < 0) {
            return JNI_FALSE;
        } else {
            if (pattern[0] == '*') {
                pattern++;
                start = classname + offset;
            }  else {
                start = classname;
            }
            return strncmp(pattern, start, compLen) == 0;
        }
    }
}

static jboolean isVersionGte12x() {
    jint version;
    jvmtiError err =
        JVMTI_FUNC_PTR(gdata->jvmti,GetVersionNumber)(gdata->jvmti, &version);

    if (err == JVMTI_ERROR_NONE) {
        jint major, minor;

        major = (version & JVMTI_VERSION_MASK_MAJOR)
                    >> JVMTI_VERSION_SHIFT_MAJOR;
        minor = (version & JVMTI_VERSION_MASK_MINOR)
                    >> JVMTI_VERSION_SHIFT_MINOR;
        return (major > 1 || (major == 1 && minor >= 2)) ? JNI_TRUE : JNI_FALSE;
    } else {
        return JNI_FALSE;
    }
}

/* Return the object instance in which the event occurred */
/* Return NULL if static or if an error occurs */
static jobject
eventInstance(EventInfo *evinfo)
{
    jobject     object          = NULL;
    jthread     thread          ;
    jmethodID   method          ;
    jint        modifiers       = 0;
    jvmtiError  error;

    static jboolean got_version = JNI_FALSE;
    static jboolean is_version_gte_12x = JNI_FALSE;

    if (!got_version) {
        is_version_gte_12x = isVersionGte12x();
        got_version = JNI_TRUE;
    }

    switch (evinfo->ei) {
        case EI_SINGLE_STEP:
        case EI_BREAKPOINT:
        case EI_FRAME_POP:
        case EI_METHOD_ENTRY:
        case EI_METHOD_EXIT:
        case EI_EXCEPTION:
        case EI_EXCEPTION_CATCH:
        case EI_MONITOR_CONTENDED_ENTER:
        case EI_MONITOR_CONTENDED_ENTERED:
        case EI_MONITOR_WAIT:
        case EI_MONITOR_WAITED:
            thread      = evinfo->thread;
            method      = evinfo->method;
            break;
        case EI_FIELD_ACCESS:
        case EI_FIELD_MODIFICATION:
            object = evinfo->object;
            return object;
        default:
            return object; /* NULL */
    }

    error = methodModifiers(method, &modifiers);

    /* fail if error or static (0x8) */
    if (error == JVMTI_ERROR_NONE && thread!=NULL && (modifiers & 0x8) == 0) {
        FrameNumber fnum            = 0;
        if (is_version_gte_12x) {
            /* Use new 1.2.x function, GetLocalInstance */
            error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalInstance)
                        (gdata->jvmti, thread, fnum, &object);
        } else {
            /* get slot zero object "this" */
            error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalObject)
                        (gdata->jvmti, thread, fnum, 0, &object);
        }
        if (error != JVMTI_ERROR_NONE) {
            object = NULL;
        }
    }

    return object;
}

/*
 * Determine if this event is interesting to this handler.
 * Do so by checking each of the handler's filters.
 * Return false if any of the filters fail,
 * true if the handler wants this event.
 * Anyone modifying this function should check
 * eventFilterRestricted_passesUnloadFilter and
 * eventFilter_predictFiltering as well.
 *
 * If shouldDelete is returned true, a count filter has expired
 * and the corresponding node should be deleted.
 */
jboolean
eventFilterRestricted_passesFilter(JNIEnv *env,
                                   char *classname,
                                   EventInfo *evinfo,
                                   HandlerNode *node,
                                   jboolean *shouldDelete)
{
    jthread thread;
    jclass clazz;
    jmethodID method;
    Filter *filter = FILTERS_ARRAY(node);
    int i;

    *shouldDelete = JNI_FALSE;
    thread = evinfo->thread;
    clazz = evinfo->clazz;
    method = evinfo->method;

    /*
     * Suppress most events if they happen in debug threads
     */
    if ((evinfo->ei != EI_CLASS_PREPARE) &&
        (evinfo->ei != EI_GC_FINISH) &&
        (evinfo->ei != EI_CLASS_LOAD) &&
        threadControl_isDebugThread(thread)) {
        return JNI_FALSE;
    }

    for (i = 0; i < FILTER_COUNT(node); ++i, ++filter) {
        switch (filter->modifier) {
            case JDWP_REQUEST_MODIFIER(ThreadOnly):
                if (!isSameObject(env, thread, filter->u.ThreadOnly.thread)) {
                    return JNI_FALSE;
                }
                break;

            case JDWP_REQUEST_MODIFIER(ClassOnly):
                /* Class filters catch events in the specified
                 * class and any subclass/subinterface.
                 */
                if (!JNI_FUNC_PTR(env,IsAssignableFrom)(env, clazz,
                               filter->u.ClassOnly.clazz)) {
                    return JNI_FALSE;
                }
                break;

            /* This is kinda cheating assumming the event
             * fields will be in the same locations, but it is
             * true now.
             */
            case JDWP_REQUEST_MODIFIER(LocationOnly):
                if  (evinfo->method !=
                          filter->u.LocationOnly.method ||
                     evinfo->location !=
                          filter->u.LocationOnly.location ||
                     !isSameObject(env, clazz, filter->u.LocationOnly.clazz)) {
                    return JNI_FALSE;
                }
                break;

            case JDWP_REQUEST_MODIFIER(FieldOnly):
                /* Field watchpoints can be triggered from the
                 * declared class or any subclass/subinterface.
                 */
                if ((evinfo->u.field_access.field !=
                     filter->u.FieldOnly.field) ||
                    !isSameObject(env, evinfo->u.field_access.field_clazz,
                               filter->u.FieldOnly.clazz)) {
                    return JNI_FALSE;
                }
                break;

            case JDWP_REQUEST_MODIFIER(ExceptionOnly):
                /* do we want caught/uncaught exceptions */
                if (!((evinfo->u.exception.catch_clazz == NULL)?
                      filter->u.ExceptionOnly.uncaught :
                      filter->u.ExceptionOnly.caught)) {
                    return JNI_FALSE;
                }

                /* do we care about exception class */
                if (filter->u.ExceptionOnly.exception != NULL) {
                    jclass exception = evinfo->object;

                    /* do we want this exception class */
                    if (!JNI_FUNC_PTR(env,IsInstanceOf)(env, exception,
                            filter->u.ExceptionOnly.exception)) {
                        return JNI_FALSE;
                    }
                }
                break;

            case JDWP_REQUEST_MODIFIER(InstanceOnly): {
                jobject eventInst = eventInstance(evinfo);
                jobject filterInst = filter->u.InstanceOnly.instance;
                /* if no error and doesn't match, don't pass
                 * filter
                 */
                if (eventInst != NULL &&
                      !isSameObject(env, eventInst, filterInst)) {
                    return JNI_FALSE;
                }
                break;
            }
            case JDWP_REQUEST_MODIFIER(Count): {
                JDI_ASSERT(filter->u.Count.count > 0);
                if (--filter->u.Count.count > 0) {
                    return JNI_FALSE;
                }
                *shouldDelete = JNI_TRUE;
                break;
            }

            case JDWP_REQUEST_MODIFIER(Conditional):
/***
                if (...  filter->u.Conditional.exprID ...) {
                    return JNI_FALSE;
                }
***/
                break;

        case JDWP_REQUEST_MODIFIER(ClassMatch): {
            if (!patternStringMatch(classname,
                       filter->u.ClassMatch.classPattern)) {
                return JNI_FALSE;
            }
            break;
        }

        case JDWP_REQUEST_MODIFIER(ClassExclude): {
            if (patternStringMatch(classname,
                      filter->u.ClassExclude.classPattern)) {
                return JNI_FALSE;
            }
            break;
        }

        case JDWP_REQUEST_MODIFIER(Step):
                if (!isSameObject(env, thread, filter->u.Step.thread)) {
                    return JNI_FALSE;
                }
                if (!stepControl_handleStep(env, thread, clazz, method)) {
                    return JNI_FALSE;
                }
                break;

          case JDWP_REQUEST_MODIFIER(SourceNameMatch): {
              char* desiredNamePattern = filter->u.SourceNameOnly.sourceNamePattern;
              if (searchAllSourceNames(env, clazz, desiredNamePattern) != 1) {
                  /* The name isn't in the SDE; try the sourceName in the ref
                   * type
                   */
                  char *sourceName = 0;
                  jvmtiError error = JVMTI_FUNC_PTR(gdata->jvmti,GetSourceFileName)
                                            (gdata->jvmti, clazz, &sourceName);
                  if (error == JVMTI_ERROR_NONE &&
                      sourceName != 0 &&
                      patternStringMatch(sourceName, desiredNamePattern)) {
                          // got a hit - report the event
                          jvmtiDeallocate(sourceName);
                          break;
                  }
                  // We have no match, we have no source file name,
                  // or we got a JVM TI error. Don't report the event.
                  jvmtiDeallocate(sourceName);
                  return JNI_FALSE;
              }
              break;
          }

        default:
            EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"Invalid filter modifier");
            return JNI_FALSE;
        }
    }
    return JNI_TRUE;
}

/* Determine if this event is interesting to this handler.  Do so
 * by checking each of the handler's filters.  Return false if any
 * of the filters fail, true if the handler wants this event.
 * Special version of filter for unloads since they don't have an
 * event structure or a jclass.
 *
 * If shouldDelete is returned true, a count filter has expired
 * and the corresponding node should be deleted.
 */
jboolean
eventFilterRestricted_passesUnloadFilter(JNIEnv *env,
                                         char *classname,
                                         HandlerNode *node,
                                         jboolean *shouldDelete)
{
    Filter *filter = FILTERS_ARRAY(node);
    int i;

    *shouldDelete = JNI_FALSE;
    for (i = 0; i < FILTER_COUNT(node); ++i, ++filter) {
        switch (filter->modifier) {

            case JDWP_REQUEST_MODIFIER(Count): {
                JDI_ASSERT(filter->u.Count.count > 0);
                if (--filter->u.Count.count > 0) {
                    return JNI_FALSE;
                }
                *shouldDelete = JNI_TRUE;
                break;
            }

            case JDWP_REQUEST_MODIFIER(ClassMatch): {
                if (!patternStringMatch(classname,
                        filter->u.ClassMatch.classPattern)) {
                    return JNI_FALSE;
                }
                break;
            }

            case JDWP_REQUEST_MODIFIER(ClassExclude): {
                if (patternStringMatch(classname,
                       filter->u.ClassExclude.classPattern)) {
                    return JNI_FALSE;
                }
                break;
            }

            default:
                EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"Invalid filter modifier");
                return JNI_FALSE;
        }
    }
    return JNI_TRUE;
}

/**
 * This function returns true only if it is certain that
 * all events for the given node in the given stack frame will
 * be filtered. It is used to optimize stepping. (If this
 * function returns true the stepping algorithm does not
 * have to step through every instruction in this stack frame;
 * instead, it can use more efficient method entry/exit
 * events.
 */
jboolean
eventFilter_predictFiltering(HandlerNode *node, jclass clazz, char *classname)
{
    JNIEnv     *env;
    jboolean    willBeFiltered;
    Filter     *filter;
    jboolean    done;
    int         count;
    int         i;

    willBeFiltered = JNI_FALSE;
    env            = NULL;
    filter         = FILTERS_ARRAY(node);
    count          = FILTER_COUNT(node);
    done           = JNI_FALSE;

    for (i = 0; (i < count) && (!done); ++i, ++filter) {
        switch (filter->modifier) {
            case JDWP_REQUEST_MODIFIER(ClassOnly):
                if ( env==NULL ) {
                    env = getEnv();
                }
                if (!JNI_FUNC_PTR(env,IsAssignableFrom)(env, clazz,
                                 filter->u.ClassOnly.clazz)) {
                    willBeFiltered = JNI_TRUE;
                    done = JNI_TRUE;
                }
                break;

            case JDWP_REQUEST_MODIFIER(Count): {
                /*
                 * If preceding filters have determined that events will
                 * be filtered out, that is fine and we won't get here.
                 * However, the count must be decremented - even if
                 * subsequent filters will filter these events.  We
                 * thus must end now unable to predict
                 */
                done = JNI_TRUE;
                break;
            }

            case JDWP_REQUEST_MODIFIER(ClassMatch): {
                if (!patternStringMatch(classname,
                        filter->u.ClassMatch.classPattern)) {
                    willBeFiltered = JNI_TRUE;
                    done = JNI_TRUE;
                }
                break;
            }

            case JDWP_REQUEST_MODIFIER(ClassExclude): {
                if (patternStringMatch(classname,
                       filter->u.ClassExclude.classPattern)) {
                    willBeFiltered = JNI_TRUE;
                    done = JNI_TRUE;
                }
                break;
            }
        }
    }

    return willBeFiltered;
}

/**
 * Determine if the given breakpoint node is in the specified class.
 */
jboolean
eventFilterRestricted_isBreakpointInClass(JNIEnv *env, jclass clazz,
                                          HandlerNode *node)
{
    Filter *filter = FILTERS_ARRAY(node);
    int i;

    for (i = 0; i < FILTER_COUNT(node); ++i, ++filter) {
        switch (filter->modifier) {
            case JDWP_REQUEST_MODIFIER(LocationOnly):
                return isSameObject(env, clazz, filter->u.LocationOnly.clazz);
        }
    }
    return JNI_TRUE; /* should never come here */
}

/***** filter set-up *****/

jvmtiError
eventFilter_setConditionalFilter(HandlerNode *node, jint index,
                                 jint exprID)
{
    ConditionalFilter *filter = &FILTER(node, index).u.Conditional;
    if (index >= FILTER_COUNT(node)) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }
    FILTER(node, index).modifier = JDWP_REQUEST_MODIFIER(Conditional);
    filter->exprID = exprID;
    return JVMTI_ERROR_NONE;
}

jvmtiError
eventFilter_setCountFilter(HandlerNode *node, jint index,
                           jint count)
{
    CountFilter *filter = &FILTER(node, index).u.Count;
    if (index >= FILTER_COUNT(node)) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }
    if (count <= 0) {
        return JDWP_ERROR(INVALID_COUNT);
    } else {
        FILTER(node, index).modifier = JDWP_REQUEST_MODIFIER(Count);
        filter->count = count;
        return JVMTI_ERROR_NONE;
    }
}

jvmtiError
eventFilter_setThreadOnlyFilter(HandlerNode *node, jint index,
                                jthread thread)
{
    JNIEnv *env = getEnv();
    ThreadFilter *filter = &FILTER(node, index).u.ThreadOnly;
    if (index >= FILTER_COUNT(node)) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }
    if (NODE_EI(node) == EI_GC_FINISH) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    /* Create a thread ref that will live beyond */
    /* the end of this call */
    saveGlobalRef(env, thread, &(filter->thread));
    FILTER(node, index).modifier = JDWP_REQUEST_MODIFIER(ThreadOnly);
    return JVMTI_ERROR_NONE;
}

jvmtiError
eventFilter_setLocationOnlyFilter(HandlerNode *node, jint index,
                                  jclass clazz, jmethodID method,
                                  jlocation location)
{
    JNIEnv *env = getEnv();
    LocationFilter *filter = &FILTER(node, index).u.LocationOnly;
    if (index >= FILTER_COUNT(node)) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }
    if ((NODE_EI(node) != EI_BREAKPOINT) &&
        (NODE_EI(node) != EI_FIELD_ACCESS) &&
        (NODE_EI(node) != EI_FIELD_MODIFICATION) &&
        (NODE_EI(node) != EI_SINGLE_STEP) &&
        (NODE_EI(node) != EI_EXCEPTION)) {

        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    /* Create a class ref that will live beyond */
    /* the end of this call */
    saveGlobalRef(env, clazz, &(filter->clazz));
    FILTER(node, index).modifier = JDWP_REQUEST_MODIFIER(LocationOnly);
    filter->method = method;
    filter->location = location;
    return JVMTI_ERROR_NONE;
}

jvmtiError
eventFilter_setFieldOnlyFilter(HandlerNode *node, jint index,
                               jclass clazz, jfieldID field)
{
    JNIEnv *env = getEnv();
    FieldFilter *filter = &FILTER(node, index).u.FieldOnly;
    if (index >= FILTER_COUNT(node)) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }
    if ((NODE_EI(node) != EI_FIELD_ACCESS) &&
        (NODE_EI(node) != EI_FIELD_MODIFICATION)) {

        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    /* Create a class ref that will live beyond */
    /* the end of this call */
    saveGlobalRef(env, clazz, &(filter->clazz));
    FILTER(node, index).modifier = JDWP_REQUEST_MODIFIER(FieldOnly);
    filter->field = field;
    return JVMTI_ERROR_NONE;
}

jvmtiError
eventFilter_setClassOnlyFilter(HandlerNode *node, jint index,
                               jclass clazz)
{
    JNIEnv *env = getEnv();
    ClassFilter *filter = &FILTER(node, index).u.ClassOnly;
    if (index >= FILTER_COUNT(node)) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }
    if (
        (NODE_EI(node) == EI_GC_FINISH) ||
        (NODE_EI(node) == EI_THREAD_START) ||
        (NODE_EI(node) == EI_THREAD_END)) {

        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    /* Create a class ref that will live beyond */
    /* the end of this call */
    saveGlobalRef(env, clazz, &(filter->clazz));
    FILTER(node, index).modifier = JDWP_REQUEST_MODIFIER(ClassOnly);
    return JVMTI_ERROR_NONE;
}

jvmtiError
eventFilter_setExceptionOnlyFilter(HandlerNode *node, jint index,
                                   jclass exceptionClass,
                                   jboolean caught,
                                   jboolean uncaught)
{
    JNIEnv *env = getEnv();
    ExceptionFilter *filter = &FILTER(node, index).u.ExceptionOnly;
    if (index >= FILTER_COUNT(node)) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }
    if (NODE_EI(node) != EI_EXCEPTION) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    filter->exception = NULL;
    if (exceptionClass != NULL) {
        /* Create a class ref that will live beyond */
        /* the end of this call */
        saveGlobalRef(env, exceptionClass, &(filter->exception));
    }
    FILTER(node, index).modifier =
                       JDWP_REQUEST_MODIFIER(ExceptionOnly);
    filter->caught = caught;
    filter->uncaught = uncaught;
    return JVMTI_ERROR_NONE;
}

jvmtiError
eventFilter_setInstanceOnlyFilter(HandlerNode *node, jint index,
                                  jobject instance)
{
    JNIEnv *env = getEnv();
    InstanceFilter *filter = &FILTER(node, index).u.InstanceOnly;
    if (index >= FILTER_COUNT(node)) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    filter->instance = NULL;
    if (instance != NULL) {
        /* Create an object ref that will live beyond
         * the end of this call
         */
        saveGlobalRef(env, instance, &(filter->instance));
    }
    FILTER(node, index).modifier =
                       JDWP_REQUEST_MODIFIER(InstanceOnly);
    return JVMTI_ERROR_NONE;
}

jvmtiError
eventFilter_setClassMatchFilter(HandlerNode *node, jint index,
                                char *classPattern)
{
    MatchFilter *filter = &FILTER(node, index).u.ClassMatch;
    if (index >= FILTER_COUNT(node)) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }
    if (
        (NODE_EI(node) == EI_THREAD_START) ||
        (NODE_EI(node) == EI_THREAD_END)) {

        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    FILTER(node, index).modifier =
                       JDWP_REQUEST_MODIFIER(ClassMatch);
    filter->classPattern = classPattern;
    return JVMTI_ERROR_NONE;
}

jvmtiError
eventFilter_setClassExcludeFilter(HandlerNode *node, jint index,
                                  char *classPattern)
{
    MatchFilter *filter = &FILTER(node, index).u.ClassExclude;
    if (index >= FILTER_COUNT(node)) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }
    if (
        (NODE_EI(node) == EI_THREAD_START) ||
        (NODE_EI(node) == EI_THREAD_END)) {

        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    FILTER(node, index).modifier =
                       JDWP_REQUEST_MODIFIER(ClassExclude);
    filter->classPattern = classPattern;
    return JVMTI_ERROR_NONE;
}

jvmtiError
eventFilter_setStepFilter(HandlerNode *node, jint index,
                          jthread thread, jint size, jint depth)
{
    jvmtiError error;
    JNIEnv *env = getEnv();
    StepFilter *filter = &FILTER(node, index).u.Step;
    if (index >= FILTER_COUNT(node)) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }
    if (NODE_EI(node) != EI_SINGLE_STEP) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    /* Create a thread ref that will live beyond */
    /* the end of this call */
    saveGlobalRef(env, thread, &(filter->thread));
    error = stepControl_beginStep(env, filter->thread, size, depth, node);
    if (error != JVMTI_ERROR_NONE) {
        tossGlobalRef(env, &(filter->thread));
        return error;
    }
    FILTER(node, index).modifier = JDWP_REQUEST_MODIFIER(Step);
    filter->depth = depth;
    filter->size = size;
    return JVMTI_ERROR_NONE;
}

jvmtiError
eventFilter_setSourceNameMatchFilter(HandlerNode *node,
                                    jint index,
                                    char *sourceNamePattern) {
    SourceNameFilter *filter = &FILTER(node, index).u.SourceNameOnly;
    if (index >= FILTER_COUNT(node)) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }
    if (NODE_EI(node) != EI_CLASS_PREPARE) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    FILTER(node, index).modifier =
                       JDWP_REQUEST_MODIFIER(SourceNameMatch);
    filter->sourceNamePattern = sourceNamePattern;
    return JVMTI_ERROR_NONE;

}

/***** JVMTI event enabling / disabling *****/

/**
 * Return the Filter that is of the specified type (modifier).
 * Return NULL if not found.
 */
static Filter *
findFilter(HandlerNode *node, jint modifier)
{
    int i;
    Filter *filter;
    for (i = 0, filter = FILTERS_ARRAY(node);
                      i <FILTER_COUNT(node);
                      i++, filter++) {
        if (filter->modifier == modifier) {
            return filter;
        }
    }
    return NULL;
}

/**
 * Determine if the specified breakpoint node is in the
 * same location as the LocationFilter passed in arg.
 *
 * This is a match function called by a
 * eventHandlerRestricted_iterator invokation.
 */
static jboolean
matchBreakpoint(JNIEnv *env, HandlerNode *node, void *arg)
{
    LocationFilter *goal = (LocationFilter *)arg;
    Filter *filter = FILTERS_ARRAY(node);
    int i;

    for (i = 0; i < FILTER_COUNT(node); ++i, ++filter) {
        switch (filter->modifier) {
        case JDWP_REQUEST_MODIFIER(LocationOnly): {
            LocationFilter *trial = &(filter->u.LocationOnly);
            if (trial->method == goal->method &&
                trial->location == goal->location &&
                isSameObject(env, trial->clazz, goal->clazz)) {
                return JNI_TRUE;
            }
        }
        }
    }
    return JNI_FALSE;
}

/**
 * Set a breakpoint if this is the first one at this location.
 */
static jvmtiError
setBreakpoint(HandlerNode *node)
{
    jvmtiError error = JVMTI_ERROR_NONE;
    Filter *filter;

    filter = findFilter(node, JDWP_REQUEST_MODIFIER(LocationOnly));
    if (filter == NULL) {
        /* bp event with no location filter */
        error = AGENT_ERROR_INTERNAL;
    } else {
        LocationFilter *lf = &(filter->u.LocationOnly);

        /* if this is the first handler for this
         * location, set bp at JVMTI level
         */
        if (!eventHandlerRestricted_iterator(
                EI_BREAKPOINT, matchBreakpoint, lf)) {
            LOG_LOC(("SetBreakpoint at location: method=%p,location=%d",
                        lf->method, (int)lf->location));
            error = JVMTI_FUNC_PTR(gdata->jvmti,SetBreakpoint)
                        (gdata->jvmti, lf->method, lf->location);
        }
    }
    return error;
}

/**
 * Clear a breakpoint if this is the last one at this location.
 */
static jvmtiError
clearBreakpoint(HandlerNode *node)
{
    jvmtiError error = JVMTI_ERROR_NONE;
    Filter *filter;

    filter = findFilter(node, JDWP_REQUEST_MODIFIER(LocationOnly));
    if (filter == NULL) {
        /* bp event with no location filter */
        error = AGENT_ERROR_INTERNAL;
    } else {
        LocationFilter *lf = &(filter->u.LocationOnly);

        /* if this is the last handler for this
         * location, clear bp at JVMTI level
         */
        if (!eventHandlerRestricted_iterator(
                EI_BREAKPOINT, matchBreakpoint, lf)) {
            LOG_LOC(("ClearBreakpoint at location: method=%p,location=%d",
                        lf->method, (int)lf->location));
            error = JVMTI_FUNC_PTR(gdata->jvmti,ClearBreakpoint)
                        (gdata->jvmti, lf->method, lf->location);
        }
    }
    return error;
}

/**
 * Return true if a breakpoint is set at the specified location.
 */
jboolean
isBreakpointSet(jclass clazz, jmethodID method, jlocation location)
{
    LocationFilter lf;

    lf.clazz    = clazz;
    lf.method   = method;
    lf.location = location;

    return eventHandlerRestricted_iterator(EI_BREAKPOINT,
                                           matchBreakpoint, &lf);
}

/**
 * Determine if the specified watchpoint node has the
 * same field as the FieldFilter passed in arg.
 *
 * This is a match function called by a
 * eventHandlerRestricted_iterator invokation.
 */
static jboolean
matchWatchpoint(JNIEnv *env, HandlerNode *node, void *arg)
{
    FieldFilter *goal = (FieldFilter *)arg;
    Filter *filter = FILTERS_ARRAY(node);
    int i;

    for (i = 0; i < FILTER_COUNT(node); ++i, ++filter) {
        switch (filter->modifier) {
        case JDWP_REQUEST_MODIFIER(FieldOnly): {
            FieldFilter *trial = &(filter->u.FieldOnly);
            if (trial->field == goal->field &&
                isSameObject(env, trial->clazz, goal->clazz)) {
                return JNI_TRUE;
            }
        }
        }
    }
    return JNI_FALSE;
}

/**
 * Set a watchpoint if this is the first one on this field.
 */
static jvmtiError
setWatchpoint(HandlerNode *node)
{
    jvmtiError error = JVMTI_ERROR_NONE;
    Filter *filter;

    filter = findFilter(node, JDWP_REQUEST_MODIFIER(FieldOnly));
    if (filter == NULL) {
        /* event with no field filter */
        error = AGENT_ERROR_INTERNAL;
    } else {
        FieldFilter *ff = &(filter->u.FieldOnly);

        /* if this is the first handler for this
         * field, set wp at JVMTI level
         */
        if (!eventHandlerRestricted_iterator(
                NODE_EI(node), matchWatchpoint, ff)) {
            error = (NODE_EI(node) == EI_FIELD_ACCESS) ?
                JVMTI_FUNC_PTR(gdata->jvmti,SetFieldAccessWatch)
                        (gdata->jvmti, ff->clazz, ff->field) :
                JVMTI_FUNC_PTR(gdata->jvmti,SetFieldModificationWatch)
                        (gdata->jvmti, ff->clazz, ff->field);
        }
    }
    return error;
}

/**
 * Clear a watchpoint if this is the last one on this field.
 */
static jvmtiError
clearWatchpoint(HandlerNode *node)
{
    jvmtiError error = JVMTI_ERROR_NONE;
    Filter *filter;

    filter = findFilter(node, JDWP_REQUEST_MODIFIER(FieldOnly));
    if (filter == NULL) {
        /* event with no field filter */
        error = AGENT_ERROR_INTERNAL;
    } else {
        FieldFilter *ff = &(filter->u.FieldOnly);

        /* if this is the last handler for this
         * field, clear wp at JVMTI level
         */
        if (!eventHandlerRestricted_iterator(
                NODE_EI(node), matchWatchpoint, ff)) {
            error = (NODE_EI(node) == EI_FIELD_ACCESS) ?
                JVMTI_FUNC_PTR(gdata->jvmti,ClearFieldAccessWatch)
                        (gdata->jvmti, ff->clazz, ff->field) :
                JVMTI_FUNC_PTR(gdata->jvmti,ClearFieldModificationWatch)
                                (gdata->jvmti, ff->clazz, ff->field);
        }
    }
    return error;
}

/**
 * Determine the thread this node is filtered on.
 * NULL if not thread filtered.
 */
static jthread
requestThread(HandlerNode *node)
{
    int i;
    Filter *filter = FILTERS_ARRAY(node);

    for (i = 0; i < FILTER_COUNT(node); ++i, ++filter) {
        switch (filter->modifier) {
            case JDWP_REQUEST_MODIFIER(ThreadOnly):
                return filter->u.ThreadOnly.thread;
        }
    }

    return NULL;
}

/**
 * Determine if the specified node has a
 * thread filter with the thread passed in arg.
 *
 * This is a match function called by a
 * eventHandlerRestricted_iterator invokation.
 */
static jboolean
matchThread(JNIEnv *env, HandlerNode *node, void *arg)
{
    jthread goalThread = (jthread)arg;
    jthread reqThread = requestThread(node);

    /* If the event's thread and the passed thread are the same
     * (or both are NULL), we have a match.
     */
    return isSameObject(env, reqThread, goalThread);
}

/**
 * Do any enabling of events (including setting breakpoints etc)
 * needed to get the events requested by this handler node.
 */
static jvmtiError
enableEvents(HandlerNode *node)
{
    jvmtiError error = JVMTI_ERROR_NONE;

    switch (NODE_EI(node)) {
        /* The stepping code directly enables/disables stepping as
         * necessary
         */
        case EI_SINGLE_STEP:
        /* Internal thread event handlers are always present
         * (hardwired in the event hook), so we don't change the
         * notification mode here.
         */
        case EI_THREAD_START:
        case EI_THREAD_END:
        case EI_VM_INIT:
        case EI_VM_DEATH:
        case EI_CLASS_PREPARE:
        case EI_GC_FINISH:
            return error;

        case EI_FIELD_ACCESS:
        case EI_FIELD_MODIFICATION:
            error = setWatchpoint(node);
            break;

        case EI_BREAKPOINT:
            error = setBreakpoint(node);
            break;

        default:
            break;
    }

    /* Don't globally enable if the above failed */
    if (error == JVMTI_ERROR_NONE) {
        jthread thread = requestThread(node);

        /* If this is the first request of it's kind on this
         * thread (or all threads (thread == NULL)) then enable
         * these events on this thread.
         */
        if (!eventHandlerRestricted_iterator(
                NODE_EI(node), matchThread, thread)) {
            error = threadControl_setEventMode(JVMTI_ENABLE,
                                               NODE_EI(node), thread);
        }
    }
    return error;
}

/**
 * Do any disabling of events (including clearing breakpoints etc)
 * needed to no longer get the events requested by this handler node.
 */
static jvmtiError
disableEvents(HandlerNode *node)
{
    jvmtiError error = JVMTI_ERROR_NONE;
    jvmtiError error2 = JVMTI_ERROR_NONE;
    jthread thread;


    switch (NODE_EI(node)) {
        /* The stepping code directly enables/disables stepping as
         * necessary
         */
        case EI_SINGLE_STEP:
        /* Internal thread event handlers are always present
         * (hardwired in the event hook), so we don't change the
         * notification mode here.
         */
        case EI_THREAD_START:
        case EI_THREAD_END:
        case EI_VM_INIT:
        case EI_VM_DEATH:
        case EI_CLASS_PREPARE:
        case EI_GC_FINISH:
            return error;

        case EI_FIELD_ACCESS:
        case EI_FIELD_MODIFICATION:
            error = clearWatchpoint(node);
            break;

        case EI_BREAKPOINT:
            error = clearBreakpoint(node);
            break;

        default:
            break;
    }

    thread = requestThread(node);

    /* If this is the last request of it's kind on this thread
     * (or all threads (thread == NULL)) then disable these
     * events on this thread.
     *
     * Disable even if the above caused an error
     */
    if (!eventHandlerRestricted_iterator(NODE_EI(node), matchThread, thread)) {
        error2 = threadControl_setEventMode(JVMTI_DISABLE,
                                            NODE_EI(node), thread);
    }
    return error != JVMTI_ERROR_NONE? error : error2;
}


/***** filter (and event) installation and deinstallation *****/

/**
 * Make the set of event filters that correspond with this
 * node active (including enabling the corresponding events).
 */
jvmtiError
eventFilterRestricted_install(HandlerNode *node)
{
    return enableEvents(node);
}

/**
 * Make the set of event filters that correspond with this
 * node inactive (including disabling the corresponding events
 * and freeing resources).
 */
jvmtiError
eventFilterRestricted_deinstall(HandlerNode *node)
{
    jvmtiError error1, error2;

    error1 = disableEvents(node);
    error2 = clearFilters(node);

    return error1 != JVMTI_ERROR_NONE? error1 : error2;
}

/***** debugging *****/

#ifdef DEBUG

void
eventFilter_dumpHandlerFilters(HandlerNode *node)
{
    int i;
    Filter *filter = FILTERS_ARRAY(node);

    for (i = 0; i < FILTER_COUNT(node); ++i, ++filter) {
        switch (filter->modifier) {
            case JDWP_REQUEST_MODIFIER(ThreadOnly):
                tty_message("ThreadOnly: thread(%p)",
                            filter->u.ThreadOnly.thread);
                break;
            case JDWP_REQUEST_MODIFIER(ClassOnly): {
                char *class_name;
                classSignature(filter->u.ClassOnly.clazz, &class_name, NULL);
                tty_message("ClassOnly: clazz(%s)",
                            class_name);
                break;
            }
            case JDWP_REQUEST_MODIFIER(LocationOnly): {
                char *method_name;
                char *class_name;
                methodSignature(filter->u.LocationOnly.method, &method_name, NULL, NULL);
                classSignature(filter->u.LocationOnly.clazz, &class_name, NULL);
                tty_message("LocationOnly: clazz(%s), method(%s) location(%d)",
                            class_name,
                            method_name,
                            filter->u.LocationOnly.location);
                break;
            }
            case JDWP_REQUEST_MODIFIER(FieldOnly): {
                char *class_name;
                classSignature(filter->u.FieldOnly.clazz, &class_name, NULL);
                tty_message("FieldOnly: clazz(%p), field(%d)",
                            class_name,
                            filter->u.FieldOnly.field);
                break;
            }
            case JDWP_REQUEST_MODIFIER(ExceptionOnly):
                tty_message("ExceptionOnly: clazz(%p), caught(%d) uncaught(%d)",
                            filter->u.ExceptionOnly.exception,
                            filter->u.ExceptionOnly.caught,
                            filter->u.ExceptionOnly.uncaught);
                break;
            case JDWP_REQUEST_MODIFIER(InstanceOnly):
                tty_message("InstanceOnly: instance(%p)",
                            filter->u.InstanceOnly.instance);
                break;
            case JDWP_REQUEST_MODIFIER(Count):
                tty_message("Count: count(%d)",
                            filter->u.Count.count);
                break;
            case JDWP_REQUEST_MODIFIER(Conditional):
                tty_message("Conditional: exprID(%d)",
                            filter->u.Conditional.exprID);
                break;
            case JDWP_REQUEST_MODIFIER(ClassMatch):
                tty_message("ClassMatch: classPattern(%s)",
                            filter->u.ClassMatch.classPattern);
                break;
            case JDWP_REQUEST_MODIFIER(ClassExclude):
                tty_message("ClassExclude: classPattern(%s)",
                            filter->u.ClassExclude.classPattern);
                break;
            case JDWP_REQUEST_MODIFIER(Step):
                tty_message("Step: size(%d) depth(%d) thread(%p)",
                            filter->u.Step.size,
                            filter->u.Step.depth,
                            filter->u.Step.thread);
                break;
            case JDWP_REQUEST_MODIFIER(SourceNameMatch):
                tty_message("SourceNameMatch: sourceNamePattern(%s)",
                            filter->u.SourceNameOnly.sourceNamePattern);
                break;
            default:
                EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT, "Invalid filter modifier");
                return;
        }
    }
}

#endif /* DEBUG */
