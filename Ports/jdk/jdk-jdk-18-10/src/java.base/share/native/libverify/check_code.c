/*
 * Copyright (c) 1994, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*-
 *      Verify that the code within a method block doesn't exploit any
 *      security holes.
 */
/*
   Exported function:

   jboolean
   VerifyClassForMajorVersion(JNIEnv *env, jclass cb, char *message_buffer,
                              jint buffer_length, jint major_version)

   This file now only uses the standard JNI and the following VM functions
   exported in jvm.h:

   JVM_FindClassFromClass
   JVM_IsInterface
   JVM_GetClassNameUTF
   JVM_GetClassCPEntriesCount
   JVM_GetClassCPTypes
   JVM_GetClassFieldsCount
   JVM_GetClassMethodsCount

   JVM_GetFieldIxModifiers

   JVM_GetMethodIxModifiers
   JVM_GetMethodIxExceptionTableLength
   JVM_GetMethodIxLocalsCount
   JVM_GetMethodIxArgsSize
   JVM_GetMethodIxMaxStack
   JVM_GetMethodIxNameUTF
   JVM_GetMethodIxSignatureUTF
   JVM_GetMethodIxExceptionsCount
   JVM_GetMethodIxExceptionIndexes
   JVM_GetMethodIxByteCodeLength
   JVM_GetMethodIxByteCode
   JVM_GetMethodIxExceptionTableEntry
   JVM_IsConstructorIx

   JVM_GetCPClassNameUTF
   JVM_GetCPFieldNameUTF
   JVM_GetCPMethodNameUTF
   JVM_GetCPFieldSignatureUTF
   JVM_GetCPMethodSignatureUTF
   JVM_GetCPFieldClassNameUTF
   JVM_GetCPMethodClassNameUTF
   JVM_GetCPFieldModifiers
   JVM_GetCPMethodModifiers

   JVM_ReleaseUTF
   JVM_IsSameClassPackage

 */

#include <string.h>
#include <setjmp.h>
#include <assert.h>
#include <limits.h>
#include <stdlib.h>

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "classfile_constants.h"
#include "opcodes.in_out"

/* On AIX malloc(0) and calloc(0, ...) return a NULL pointer, which is legal,
 * but the code here does not handles it. So we wrap the methods and return non-NULL
 * pointers even if we allocate 0 bytes.
 */
#ifdef _AIX
static int aix_dummy;
static void* aix_malloc(size_t len) {
  if (len == 0) {
    return &aix_dummy;
  }
  return malloc(len);
}

static void* aix_calloc(size_t n, size_t size) {
  if (n == 0) {
    return &aix_dummy;
  }
  return calloc(n, size);
}

static void aix_free(void* p) {
  if (p == &aix_dummy) {
    return;
  }
  free(p);
}

#undef malloc
#undef calloc
#undef free
#define malloc aix_malloc
#define calloc aix_calloc
#define free aix_free
#endif

#ifdef __APPLE__
/* use setjmp/longjmp versions that do not save/restore the signal mask */
#define setjmp _setjmp
#define longjmp _longjmp
#endif

#define MAX_ARRAY_DIMENSIONS 255
/* align byte code */
#ifndef ALIGN_UP
#define ALIGN_UP(n,align_grain) (((n) + ((align_grain) - 1)) & ~((align_grain)-1))
#endif /* ALIGN_UP */
#define UCALIGN(n) ((unsigned char *)ALIGN_UP((uintptr_t)(n),sizeof(int)))

#ifdef DEBUG

int verify_verbose = 0;
static struct context_type *GlobalContext;
#endif

enum {
    ITEM_Bogus,
    ITEM_Void,                  /* only as a function return value */
    ITEM_Integer,
    ITEM_Float,
    ITEM_Double,
    ITEM_Double_2,              /* 2nd word of double in register */
    ITEM_Long,
    ITEM_Long_2,                /* 2nd word of long in register */
    ITEM_Array,
    ITEM_Object,                /* Extra info field gives name. */
    ITEM_NewObject,             /* Like object, but uninitialized. */
    ITEM_InitObject,            /* "this" is init method, before call
                                    to super() */
    ITEM_ReturnAddress,         /* Extra info gives instr # of start pc */
    /* The following four are only used within array types.
     * Normally, we use ITEM_Integer, instead. */
    ITEM_Byte,
    ITEM_Short,
    ITEM_Char,
    ITEM_Boolean
};


#define UNKNOWN_STACK_SIZE -1
#define UNKNOWN_REGISTER_COUNT -1
#define UNKNOWN_RET_INSTRUCTION -1

#undef MAX
#undef MIN
#define MAX(a, b) ((a) > (b) ? (a) : (b))
#define MIN(a, b) ((a) < (b) ? (a) : (b))

#define BITS_PER_INT   (CHAR_BIT * sizeof(int)/sizeof(char))
#define SET_BIT(flags, i)  (flags[(i)/BITS_PER_INT] |= \
                                       ((unsigned)1 << ((i) % BITS_PER_INT)))
#define IS_BIT_SET(flags, i) (flags[(i)/BITS_PER_INT] & \
                                       ((unsigned)1 << ((i) % BITS_PER_INT)))

typedef unsigned int fullinfo_type;
typedef unsigned int *bitvector;

#define GET_ITEM_TYPE(thing) ((thing) & 0x1F)
#define GET_INDIRECTION(thing) (((thing) & 0xFFFF) >> 5)
#define GET_EXTRA_INFO(thing) ((thing) >> 16)
#define WITH_ZERO_INDIRECTION(thing) ((thing) & ~(0xFFE0))
#define WITH_ZERO_EXTRA_INFO(thing) ((thing) & 0xFFFF)

#define MAKE_FULLINFO(type, indirect, extra) \
     ((type) + ((indirect) << 5) + ((extra) << 16))

#define MAKE_Object_ARRAY(indirect) \
       (context->object_info + ((indirect) << 5))

#define NULL_FULLINFO MAKE_FULLINFO(ITEM_Object, 0, 0)

/* JVM_OPC_invokespecial calls to <init> need to be treated special */
#define JVM_OPC_invokeinit 0x100

/* A hash mechanism used by the verifier.
 * Maps class names to unique 16 bit integers.
 */

#define HASH_TABLE_SIZE 503

/* The buckets are managed as a 256 by 256 matrix. We allocate an entire
 * row (256 buckets) at a time to minimize fragmentation. Rows are
 * allocated on demand so that we don't waste too much space.
 */

#define MAX_HASH_ENTRIES 65536
#define HASH_ROW_SIZE 256

typedef struct hash_bucket_type {
    char *name;
    unsigned int hash;
    jclass class;
    unsigned short ID;
    unsigned short next;
    unsigned loadable:1;  /* from context->class loader */
} hash_bucket_type;

typedef struct {
    hash_bucket_type **buckets;
    unsigned short *table;
    int entries_used;
} hash_table_type;

#define GET_BUCKET(class_hash, ID)\
    (class_hash->buckets[ID / HASH_ROW_SIZE] + ID % HASH_ROW_SIZE)

/*
 * There are currently two types of resources that we need to keep
 * track of (in addition to the CCalloc pool).
 */
enum {
    VM_STRING_UTF, /* VM-allocated UTF strings */
    VM_MALLOC_BLK  /* malloc'ed blocks */
};

#define LDC_CLASS_MAJOR_VERSION 49

#define LDC_METHOD_HANDLE_MAJOR_VERSION 51

#define NONZERO_PADDING_BYTES_IN_SWITCH_MAJOR_VERSION 51

#define STATIC_METHOD_IN_INTERFACE_MAJOR_VERSION  52

#define ALLOC_STACK_SIZE 16 /* big enough */

typedef struct alloc_stack_type {
    void *ptr;
    int kind;
    struct alloc_stack_type *next;
} alloc_stack_type;

/* The context type encapsulates the current invocation of the byte
 * code verifier.
 */
struct context_type {

    JNIEnv *env;                /* current JNIEnv */

    /* buffers etc. */
    char *message;
    jint message_buf_len;
    jboolean err_code;

    alloc_stack_type *allocated_memory; /* all memory blocks that we have not
                                           had a chance to free */
    /* Store up to ALLOC_STACK_SIZE number of handles to allocated memory
       blocks here, to save mallocs. */
    alloc_stack_type alloc_stack[ALLOC_STACK_SIZE];
    int alloc_stack_top;

    /* these fields are per class */
    jclass class;               /* current class */
    jint major_version;
    jint nconstants;
    unsigned char *constant_types;
    hash_table_type class_hash;

    fullinfo_type object_info;  /* fullinfo for java/lang/Object */
    fullinfo_type string_info;  /* fullinfo for java/lang/String */
    fullinfo_type throwable_info; /* fullinfo for java/lang/Throwable */
    fullinfo_type cloneable_info; /* fullinfo for java/lang/Cloneable */
    fullinfo_type serializable_info; /* fullinfo for java/io/Serializable */

    fullinfo_type currentclass_info; /* fullinfo for context->class */
    fullinfo_type superclass_info;   /* fullinfo for superclass */

    /* these fields are per method */
    int method_index;   /* current method */
    unsigned short *exceptions; /* exceptions */
    unsigned char *code;        /* current code object */
    jint code_length;
    int *code_data;             /* offset to instruction number */
    struct instruction_data_type *instruction_data; /* info about each */
    struct handler_info_type *handler_info;
    fullinfo_type *superclasses; /* null terminated superclasses */
    int instruction_count;      /* number of instructions */
    fullinfo_type return_type;  /* function return type */
    fullinfo_type swap_table[4]; /* used for passing information */
    int bitmask_size;           /* words needed to hold bitmap of arguments */

    /* these fields are per field */
    int field_index;

    /* Used by the space allocator */
    struct CCpool *CCroot, *CCcurrent;
    char *CCfree_ptr;
    int CCfree_size;

    /* Jump here on any error. */
    jmp_buf jump_buffer;

#ifdef DEBUG
    /* keep track of how many global refs are allocated. */
    int n_globalrefs;
#endif
};

struct stack_info_type {
    struct stack_item_type *stack;
    int stack_size;
};

struct register_info_type {
    int register_count;         /* number of registers used */
    fullinfo_type *registers;
    int mask_count;             /* number of masks in the following */
    struct mask_type *masks;
};

struct mask_type {
    int entry;
    int *modifies;
};

typedef unsigned short flag_type;

struct instruction_data_type {
    int opcode;         /* may turn into "canonical" opcode */
    unsigned changed:1;         /* has it changed */
    unsigned protected:1;       /* must accessor be a subclass of "this" */
    union {
        int i;                  /* operand to the opcode */
        int *ip;
        fullinfo_type fi;
    } operand, operand2;
    fullinfo_type p;
    struct stack_info_type stack_info;
    struct register_info_type register_info;
#define FLAG_REACHED            0x01 /* instruction reached */
#define FLAG_NEED_CONSTRUCTOR   0x02 /* must call this.<init> or super.<init> */
#define FLAG_NO_RETURN          0x04 /* must throw out of method */
    flag_type or_flags;         /* true for at least one path to this inst */
#define FLAG_CONSTRUCTED        0x01 /* this.<init> or super.<init> called */
    flag_type and_flags;        /* true for all paths to this instruction */
};

struct handler_info_type {
    int start, end, handler;
    struct stack_info_type stack_info;
};

struct stack_item_type {
    fullinfo_type item;
    struct stack_item_type *next;
};

typedef struct context_type context_type;
typedef struct instruction_data_type instruction_data_type;
typedef struct stack_item_type stack_item_type;
typedef struct register_info_type register_info_type;
typedef struct stack_info_type stack_info_type;
typedef struct mask_type mask_type;

static void read_all_code(context_type *context, jclass cb, int num_methods,
                          int** code_lengths, unsigned char*** code);
static void verify_method(context_type *context, jclass cb, int index,
                          int code_length, unsigned char* code);
static void free_all_code(context_type* context, int num_methods,
                          unsigned char** code);
static void verify_field(context_type *context, jclass cb, int index);

static void verify_opcode_operands (context_type *, unsigned int inumber, int offset);
static void set_protected(context_type *, unsigned int inumber, int key, int);
static jboolean is_superclass(context_type *, fullinfo_type);

static void initialize_exception_table(context_type *);
static int instruction_length(unsigned char *iptr, unsigned char *end);
static jboolean isLegalTarget(context_type *, int offset);
static void verify_constant_pool_type(context_type *, int, unsigned);

static void initialize_dataflow(context_type *);
static void run_dataflow(context_type *context);
static void check_register_values(context_type *context, unsigned int inumber);
static void check_flags(context_type *context, unsigned int inumber);
static void pop_stack(context_type *, unsigned int inumber, stack_info_type *);
static void update_registers(context_type *, unsigned int inumber, register_info_type *);
static void update_flags(context_type *, unsigned int inumber,
                         flag_type *new_and_flags, flag_type *new_or_flags);
static void push_stack(context_type *, unsigned int inumber, stack_info_type *stack);

static void merge_into_successors(context_type *, unsigned int inumber,
                                  register_info_type *register_info,
                                  stack_info_type *stack_info,
                                  flag_type and_flags, flag_type or_flags);
static void merge_into_one_successor(context_type *context,
                                     unsigned int from_inumber,
                                     unsigned int inumber,
                                     register_info_type *register_info,
                                     stack_info_type *stack_info,
                                     flag_type and_flags, flag_type or_flags,
                                     jboolean isException);
static void merge_stack(context_type *, unsigned int inumber,
                        unsigned int to_inumber, stack_info_type *);
static void merge_registers(context_type *, unsigned int inumber,
                            unsigned int to_inumber,
                            register_info_type *);
static void merge_flags(context_type *context, unsigned int from_inumber,
                        unsigned int to_inumber,
                        flag_type new_and_flags, flag_type new_or_flags);

static stack_item_type *copy_stack(context_type *, stack_item_type *);
static mask_type *copy_masks(context_type *, mask_type *masks, int mask_count);
static mask_type *add_to_masks(context_type *, mask_type *, int , int);

static fullinfo_type decrement_indirection(fullinfo_type);

static fullinfo_type merge_fullinfo_types(context_type *context,
                                          fullinfo_type a,
                                          fullinfo_type b,
                                          jboolean assignment);
static jboolean isAssignableTo(context_type *,
                               fullinfo_type a,
                               fullinfo_type b);

static jclass object_fullinfo_to_classclass(context_type *, fullinfo_type);


#define NEW(type, count) \
        ((type *)CCalloc(context, (count)*(sizeof(type)), JNI_FALSE))
#define ZNEW(type, count) \
        ((type *)CCalloc(context, (count)*(sizeof(type)), JNI_TRUE))

static void CCinit(context_type *context);
static void CCreinit(context_type *context);
static void CCdestroy(context_type *context);
static void *CCalloc(context_type *context, int size, jboolean zero);

static fullinfo_type cp_index_to_class_fullinfo(context_type *, int, int);

static const char* get_result_signature(const char* signature);

static char signature_to_fieldtype(context_type *context,
                                   const char **signature_p, fullinfo_type *info);

static void CCerror (context_type *, char *format, ...);
static void CFerror (context_type *, char *format, ...);
static void CCout_of_memory (context_type *);

/* Because we can longjmp any time, we need to be very careful about
 * remembering what needs to be freed. */

static void check_and_push(context_type *context, const void *ptr, int kind);
static void pop_and_free(context_type *context);

static int signature_to_args_size(const char *method_signature);

#ifdef DEBUG
static void print_stack (context_type *, stack_info_type *stack_info);
static void print_registers(context_type *, register_info_type *register_info);
static void print_flags(context_type *, flag_type, flag_type);
static void print_formatted_fieldname(context_type *context, int index);
static void print_formatted_methodname(context_type *context, int index);
#endif

/*
 * Declare library specific JNI_Onload entry if static build
 */
DEF_STATIC_JNI_OnLoad

void initialize_class_hash(context_type *context)
{
    hash_table_type *class_hash = &(context->class_hash);
    class_hash->buckets = (hash_bucket_type **)
        calloc(MAX_HASH_ENTRIES / HASH_ROW_SIZE, sizeof(hash_bucket_type *));
    class_hash->table = (unsigned short *)
        calloc(HASH_TABLE_SIZE, sizeof(unsigned short));
    if (class_hash->buckets == 0 ||
        class_hash->table == 0)
        CCout_of_memory(context);
    class_hash->entries_used = 0;
}

static void finalize_class_hash(context_type *context)
{
    hash_table_type *class_hash = &(context->class_hash);
    JNIEnv *env = context->env;
    int i;
    /* 4296677: bucket index starts from 1. */
    for (i=1;i<=class_hash->entries_used;i++) {
        hash_bucket_type *bucket = GET_BUCKET(class_hash, i);
        assert(bucket != NULL);
        free(bucket->name);
        if (bucket->class) {
            (*env)->DeleteGlobalRef(env, bucket->class);
#ifdef DEBUG
            context->n_globalrefs--;
#endif
        }
    }
    if (class_hash->buckets) {
        for (i=0;i<MAX_HASH_ENTRIES / HASH_ROW_SIZE; i++) {
            if (class_hash->buckets[i] == 0)
                break;
            free(class_hash->buckets[i]);
        }
    }
    free(class_hash->buckets);
    free(class_hash->table);
}

static hash_bucket_type *
new_bucket(context_type *context, unsigned short *pID)
{
    hash_table_type *class_hash = &(context->class_hash);
    int i = *pID = class_hash->entries_used + 1;
    int row = i / HASH_ROW_SIZE;
    if (i >= MAX_HASH_ENTRIES)
        CCerror(context, "Exceeded verifier's limit of 65535 referred classes");
    if (class_hash->buckets[row] == 0) {
        class_hash->buckets[row] = (hash_bucket_type*)
            calloc(HASH_ROW_SIZE, sizeof(hash_bucket_type));
        if (class_hash->buckets[row] == 0)
            CCout_of_memory(context);
    }
    class_hash->entries_used++; /* only increment when we are sure there
                                   is no overflow. */
    return GET_BUCKET(class_hash, i);
}

static unsigned int
class_hash_fun(const char *s)
{
    int i;
    unsigned raw_hash;
    for (raw_hash = 0; (i = *s) != '\0'; ++s)
        raw_hash = raw_hash * 37 + i;
    return raw_hash;
}

/*
 * Find a class using the defining loader of the current class
 * and return a local reference to it.
 */
static jclass load_class_local(context_type *context,const char *classname)
{
    jclass cb = JVM_FindClassFromClass(context->env, classname,
                                 JNI_FALSE, context->class);
    if (cb == 0)
         CCerror(context, "Cannot find class %s", classname);
    return cb;
}

/*
 * Find a class using the defining loader of the current class
 * and return a global reference to it.
 */
static jclass load_class_global(context_type *context, const char *classname)
{
    JNIEnv *env = context->env;
    jclass local, global;

    local = load_class_local(context, classname);
    global = (*env)->NewGlobalRef(env, local);
    if (global == 0)
        CCout_of_memory(context);
#ifdef DEBUG
    context->n_globalrefs++;
#endif
    (*env)->DeleteLocalRef(env, local);
    return global;
}

/*
 * Return a unique ID given a local class reference. The loadable
 * flag is true if the defining class loader of context->class
 * is known to be capable of loading the class.
 */
static unsigned short
class_to_ID(context_type *context, jclass cb, jboolean loadable)
{
    JNIEnv *env = context->env;
    hash_table_type *class_hash = &(context->class_hash);
    unsigned int hash;
    hash_bucket_type *bucket;
    unsigned short *pID;
    const char *name = JVM_GetClassNameUTF(env, cb);

    check_and_push(context, name, VM_STRING_UTF);
    hash = class_hash_fun(name);
    pID = &(class_hash->table[hash % HASH_TABLE_SIZE]);
    while (*pID) {
        bucket = GET_BUCKET(class_hash, *pID);
        if (bucket->hash == hash && strcmp(name, bucket->name) == 0) {
            /*
             * There is an unresolved entry with our name
             * so we're forced to load it in case it matches us.
             */
            if (bucket->class == 0) {
                assert(bucket->loadable == JNI_TRUE);
                bucket->class = load_class_global(context, name);
            }

            /*
             * It's already in the table. Update the loadable
             * state if it's known and then we're done.
             */
            if ((*env)->IsSameObject(env, cb, bucket->class)) {
                if (loadable && !bucket->loadable)
                    bucket->loadable = JNI_TRUE;
                goto done;
            }
        }
        pID = &bucket->next;
    }
    bucket = new_bucket(context, pID);
    bucket->next = 0;
    bucket->hash = hash;
    bucket->name = malloc(strlen(name) + 1);
    if (bucket->name == 0)
        CCout_of_memory(context);
    strcpy(bucket->name, name);
    bucket->loadable = loadable;
    bucket->class = (*env)->NewGlobalRef(env, cb);
    if (bucket->class == 0)
        CCout_of_memory(context);
#ifdef DEBUG
    context->n_globalrefs++;
#endif

done:
    pop_and_free(context);
    return *pID;
}

/*
 * Return a unique ID given a class name from the constant pool.
 * All classes are lazily loaded from the defining loader of
 * context->class.
 */
static unsigned short
class_name_to_ID(context_type *context, const char *name)
{
    hash_table_type *class_hash = &(context->class_hash);
    unsigned int hash = class_hash_fun(name);
    hash_bucket_type *bucket;
    unsigned short *pID;
    jboolean force_load = JNI_FALSE;

    pID = &(class_hash->table[hash % HASH_TABLE_SIZE]);
    while (*pID) {
        bucket = GET_BUCKET(class_hash, *pID);
        if (bucket->hash == hash && strcmp(name, bucket->name) == 0) {
            if (bucket->loadable)
                goto done;
            force_load = JNI_TRUE;
        }
        pID = &bucket->next;
    }

    if (force_load) {
        /*
         * We found at least one matching named entry for a class that
         * was not known to be loadable through the defining class loader
         * of context->class. We must load our named class and update
         * the hash table in case one these entries matches our class.
         */
        JNIEnv *env = context->env;
        jclass cb = load_class_local(context, name);
        unsigned short id = class_to_ID(context, cb, JNI_TRUE);
        (*env)->DeleteLocalRef(env, cb);
        return id;
    }

    bucket = new_bucket(context, pID);
    bucket->next = 0;
    bucket->class = 0;
    bucket->loadable = JNI_TRUE; /* name-only IDs are implicitly loadable */
    bucket->hash = hash;
    bucket->name = malloc(strlen(name) + 1);
    if (bucket->name == 0)
        CCout_of_memory(context);
    strcpy(bucket->name, name);

done:
    return *pID;
}

#ifdef DEBUG
static const char *
ID_to_class_name(context_type *context, unsigned short ID)
{
    hash_table_type *class_hash = &(context->class_hash);
    hash_bucket_type *bucket = GET_BUCKET(class_hash, ID);
    return bucket->name;
}
#endif

static jclass
ID_to_class(context_type *context, unsigned short ID)
{
    hash_table_type *class_hash = &(context->class_hash);
    hash_bucket_type *bucket = GET_BUCKET(class_hash, ID);
    if (bucket->class == 0) {
        assert(bucket->loadable == JNI_TRUE);
        bucket->class = load_class_global(context, bucket->name);
    }
    return bucket->class;
}

static fullinfo_type
make_loadable_class_info(context_type *context, jclass cb)
{
    return MAKE_FULLINFO(ITEM_Object, 0,
                           class_to_ID(context, cb, JNI_TRUE));
}

static fullinfo_type
make_class_info(context_type *context, jclass cb)
{
    return MAKE_FULLINFO(ITEM_Object, 0,
                         class_to_ID(context, cb, JNI_FALSE));
}

static fullinfo_type
make_class_info_from_name(context_type *context, const char *name)
{
    return MAKE_FULLINFO(ITEM_Object, 0,
                         class_name_to_ID(context, name));
}

/* RETURNS
 * 1: on success       chosen to be consistent with previous VerifyClass
 * 0: verify error
 * 2: out of memory
 * 3: class format error
 *
 * Called by verify_class.  Verify the code of each of the methods
 * in a class.  Note that this function apparently can't be JNICALL,
 * because if it is the dynamic linker doesn't appear to be able to
 * find it on Win32.
 */

#define CC_OK 1
#define CC_VerifyError 0
#define CC_OutOfMemory 2
#define CC_ClassFormatError 3

JNIEXPORT jboolean
VerifyClassForMajorVersion(JNIEnv *env, jclass cb, char *buffer, jint len,
                           jint major_version)
{
    context_type context_structure;
    context_type *context = &context_structure;
    jboolean result = CC_OK;
    int i;
    int num_methods;
    int* code_lengths;
    unsigned char** code;

#ifdef DEBUG
    GlobalContext = context;
#endif

    memset(context, 0, sizeof(context_type));
    context->message = buffer;
    context->message_buf_len = len;

    context->env = env;
    context->class = cb;

    /* Set invalid method/field index of the context, in case anyone
       calls CCerror */
    context->method_index = -1;
    context->field_index = -1;

    /* Don't call CCerror or anything that can call it above the setjmp! */
    if (!setjmp(context->jump_buffer)) {
        jclass super;

        CCinit(context);                /* initialize heap; may throw */

        initialize_class_hash(context);

        context->major_version = major_version;
        context->nconstants = JVM_GetClassCPEntriesCount(env, cb);
        context->constant_types = (unsigned char *)
            malloc(sizeof(unsigned char) * context->nconstants + 1);

        if (context->constant_types == 0)
            CCout_of_memory(context);

        JVM_GetClassCPTypes(env, cb, context->constant_types);

        if (context->constant_types == 0)
            CCout_of_memory(context);

        context->object_info =
            make_class_info_from_name(context, "java/lang/Object");
        context->string_info =
            make_class_info_from_name(context, "java/lang/String");
        context->throwable_info =
            make_class_info_from_name(context, "java/lang/Throwable");
        context->cloneable_info =
            make_class_info_from_name(context, "java/lang/Cloneable");
        context->serializable_info =
            make_class_info_from_name(context, "java/io/Serializable");

        context->currentclass_info = make_loadable_class_info(context, cb);

        super = (*env)->GetSuperclass(env, cb);

        if (super != 0) {
            fullinfo_type *gptr;
            int i = 0;

            context->superclass_info = make_loadable_class_info(context, super);

            while(super != 0) {
                jclass tmp_cb = (*env)->GetSuperclass(env, super);
                (*env)->DeleteLocalRef(env, super);
                super = tmp_cb;
                i++;
            }
            (*env)->DeleteLocalRef(env, super);
            super = 0;

            /* Can't go on context heap since it survives more than
               one method */
            context->superclasses = gptr =
                malloc(sizeof(fullinfo_type)*(i + 1));
            if (gptr == 0) {
                CCout_of_memory(context);
            }

            super = (*env)->GetSuperclass(env, context->class);
            while(super != 0) {
                jclass tmp_cb;
                *gptr++ = make_class_info(context, super);
                tmp_cb = (*env)->GetSuperclass(env, super);
                (*env)->DeleteLocalRef(env, super);
                super = tmp_cb;
            }
            *gptr = 0;
        } else {
            context->superclass_info = 0;
        }

        (*env)->DeleteLocalRef(env, super);

        /* Look at each method */
        for (i = JVM_GetClassFieldsCount(env, cb); --i >= 0;)
            verify_field(context, cb, i);
        num_methods = JVM_GetClassMethodsCount(env, cb);
        read_all_code(context, cb, num_methods, &code_lengths, &code);
        for (i = num_methods - 1; i >= 0; --i)
            verify_method(context, cb, i, code_lengths[i], code[i]);
        free_all_code(context, num_methods, code);
        result = CC_OK;
    } else {
        result = context->err_code;
    }

    /* Cleanup */
    finalize_class_hash(context);

    while(context->allocated_memory)
        pop_and_free(context);

#ifdef DEBUG
    GlobalContext = 0;
#endif

    if (context->exceptions)
        free(context->exceptions);

    if (context->constant_types)
        free(context->constant_types);

    if (context->superclasses)
        free(context->superclasses);

#ifdef DEBUG
    /* Make sure all global refs created in the verifier are freed */
    assert(context->n_globalrefs == 0);
#endif

    CCdestroy(context);         /* destroy heap */
    return result;
}

static void
verify_field(context_type *context, jclass cb, int field_index)
{
    JNIEnv *env = context->env;
    int access_bits = JVM_GetFieldIxModifiers(env, cb, field_index);
    context->field_index = field_index;

    if (  ((access_bits & JVM_ACC_PUBLIC) != 0) &&
          ((access_bits & (JVM_ACC_PRIVATE | JVM_ACC_PROTECTED)) != 0)) {
        CCerror(context, "Inconsistent access bits.");
    }
    context->field_index = -1;
}


/**
 * We read all of the class's methods' code because it is possible that
 * the verification of one method could resulting in linking further
 * down the stack (due to class loading), which could end up rewriting
 * some of the bytecode of methods we haven't verified yet.  Since we
 * don't want to see the rewritten bytecode, cache all the code and
 * operate only on that.
 */
static void
read_all_code(context_type* context, jclass cb, int num_methods,
              int** lengths_addr, unsigned char*** code_addr)
{
    int* lengths;
    unsigned char** code;
    int i;

    lengths = malloc(sizeof(int) * num_methods);
    check_and_push(context, lengths, VM_MALLOC_BLK);

    code = malloc(sizeof(unsigned char*) * num_methods);
    check_and_push(context, code, VM_MALLOC_BLK);

    *(lengths_addr) = lengths;
    *(code_addr) = code;

    for (i = 0; i < num_methods; ++i) {
        lengths[i] = JVM_GetMethodIxByteCodeLength(context->env, cb, i);
        if (lengths[i] > 0) {
            code[i] = malloc(sizeof(unsigned char) * (lengths[i] + 1));
            check_and_push(context, code[i], VM_MALLOC_BLK);
            JVM_GetMethodIxByteCode(context->env, cb, i, code[i]);
        } else {
            code[i] = NULL;
        }
    }
}

static void
free_all_code(context_type* context, int num_methods, unsigned char** code)
{
  int i;
  for (i = 0; i < num_methods; ++i) {
      if (code[i] != NULL) {
          pop_and_free(context);
      }
  }
  pop_and_free(context); /* code */
  pop_and_free(context); /* lengths */
}

/* Verify the code of one method */
static void
verify_method(context_type *context, jclass cb, int method_index,
              int code_length, unsigned char* code)
{
    JNIEnv *env = context->env;
    int access_bits = JVM_GetMethodIxModifiers(env, cb, method_index);
    int *code_data;
    instruction_data_type *idata = 0;
    int instruction_count;
    int i, offset;
    unsigned int inumber;
    jint nexceptions;

    if ((access_bits & (JVM_ACC_NATIVE | JVM_ACC_ABSTRACT)) != 0) {
        /* not much to do for abstract and native methods */
        return;
    }

    context->code_length = code_length;
    context->code = code;

    /* CCerror can give method-specific info once this is set */
    context->method_index = method_index;

    CCreinit(context);          /* initial heap */
    code_data = NEW(int, code_length);

#ifdef DEBUG
    if (verify_verbose) {
        const char *classname = JVM_GetClassNameUTF(env, cb);
        const char *methodname =
            JVM_GetMethodIxNameUTF(env, cb, method_index);
        const char *signature =
            JVM_GetMethodIxSignatureUTF(env, cb, method_index);
        jio_fprintf(stdout, "Looking at %s.%s%s\n",
                    (classname ? classname : ""),
                    (methodname ? methodname : ""),
                    (signature ? signature : ""));
        JVM_ReleaseUTF(classname);
        JVM_ReleaseUTF(methodname);
        JVM_ReleaseUTF(signature);
    }
#endif

    if (((access_bits & JVM_ACC_PUBLIC) != 0) &&
        ((access_bits & (JVM_ACC_PRIVATE | JVM_ACC_PROTECTED)) != 0)) {
        CCerror(context, "Inconsistent access bits.");
    }

    // If this method is an overpass method, which is generated by the VM,
    // we trust the code and no check needs to be done.
    if (JVM_IsVMGeneratedMethodIx(env, cb, method_index)) {
      return;
    }

    /* Run through the code.  Mark the start of each instruction, and give
     * the instruction a number */
    for (i = 0, offset = 0; offset < code_length; i++) {
        int length = instruction_length(&code[offset], code + code_length);
        int next_offset = offset + length;
        if (length <= 0)
            CCerror(context, "Illegal instruction found at offset %d", offset);
        if (next_offset > code_length)
            CCerror(context, "Code stops in the middle of instruction "
                    " starting at offset %d", offset);
        code_data[offset] = i;
        while (++offset < next_offset)
            code_data[offset] = -1; /* illegal location */
    }
    instruction_count = i;      /* number of instructions in code */

    /* Allocate a structure to hold info about each instruction. */
    idata = NEW(instruction_data_type, instruction_count);

    /* Initialize the heap, and other info in the context structure. */
    context->code = code;
    context->instruction_data = idata;
    context->code_data = code_data;
    context->instruction_count = instruction_count;
    context->handler_info =
        NEW(struct handler_info_type,
            JVM_GetMethodIxExceptionTableLength(env, cb, method_index));
    context->bitmask_size =
        (JVM_GetMethodIxLocalsCount(env, cb, method_index)
         + (BITS_PER_INT - 1))/BITS_PER_INT;

    if (instruction_count == 0)
        CCerror(context, "Empty code");

    for (inumber = 0, offset = 0; offset < code_length; inumber++) {
        int length = instruction_length(&code[offset], code + code_length);
        instruction_data_type *this_idata = &idata[inumber];
        this_idata->opcode = code[offset];
        this_idata->stack_info.stack = NULL;
        this_idata->stack_info.stack_size  = UNKNOWN_STACK_SIZE;
        this_idata->register_info.register_count = UNKNOWN_REGISTER_COUNT;
        this_idata->changed = JNI_FALSE;  /* no need to look at it yet. */
        this_idata->protected = JNI_FALSE;  /* no need to look at it yet. */
        this_idata->and_flags = (flag_type) -1; /* "bottom" and value */
        this_idata->or_flags = 0; /* "bottom" or value*/
        /* This also sets up this_data->operand.  It also makes the
         * xload_x and xstore_x instructions look like the generic form. */
        verify_opcode_operands(context, inumber, offset);
        offset += length;
    }


    /* make sure exception table is reasonable. */
    initialize_exception_table(context);
    /* Set up first instruction, and start of exception handlers. */
    initialize_dataflow(context);
    /* Run data flow analysis on the instructions. */
    run_dataflow(context);

    /* verify checked exceptions, if any */
    nexceptions = JVM_GetMethodIxExceptionsCount(env, cb, method_index);
    context->exceptions = (unsigned short *)
        malloc(sizeof(unsigned short) * nexceptions + 1);
    if (context->exceptions == 0)
        CCout_of_memory(context);
    JVM_GetMethodIxExceptionIndexes(env, cb, method_index,
                                    context->exceptions);
    for (i = 0; i < nexceptions; i++) {
        /* Make sure the constant pool item is JVM_CONSTANT_Class */
        verify_constant_pool_type(context, (int)context->exceptions[i],
                                  1 << JVM_CONSTANT_Class);
    }
    free(context->exceptions);
    context->exceptions = 0;
    context->code = 0;
    context->method_index = -1;
}


/* Look at a single instruction, and verify its operands.  Also, for
 * simplicity, move the operand into the ->operand field.
 * Make sure that branches don't go into the middle of nowhere.
 */

static jint _ck_ntohl(jint n)
{
    unsigned char *p = (unsigned char *)&n;
    return (p[0] << 24) | (p[1] << 16) | (p[2] << 8) | p[3];
}

static void
verify_opcode_operands(context_type *context, unsigned int inumber, int offset)
{
    JNIEnv *env = context->env;
    instruction_data_type *idata = context->instruction_data;
    instruction_data_type *this_idata = &idata[inumber];
    int *code_data = context->code_data;
    int mi = context->method_index;
    unsigned char *code = context->code;
    int opcode = this_idata->opcode;
    int var;

    /*
     * Set the ip fields to 0 not the i fields because the ip fields
     * are 64 bits on 64 bit architectures, the i field is only 32
     */
    this_idata->operand.ip = 0;
    this_idata->operand2.ip = 0;

    switch (opcode) {

    case JVM_OPC_jsr:
        /* instruction of ret statement */
        this_idata->operand2.i = UNKNOWN_RET_INSTRUCTION;
        /* FALLTHROUGH */
    case JVM_OPC_ifeq: case JVM_OPC_ifne: case JVM_OPC_iflt:
    case JVM_OPC_ifge: case JVM_OPC_ifgt: case JVM_OPC_ifle:
    case JVM_OPC_ifnull: case JVM_OPC_ifnonnull:
    case JVM_OPC_if_icmpeq: case JVM_OPC_if_icmpne: case JVM_OPC_if_icmplt:
    case JVM_OPC_if_icmpge: case JVM_OPC_if_icmpgt: case JVM_OPC_if_icmple:
    case JVM_OPC_if_acmpeq: case JVM_OPC_if_acmpne:
    case JVM_OPC_goto: {
        /* Set the ->operand to be the instruction number of the target. */
        int jump = (((signed char)(code[offset+1])) << 8) + code[offset+2];
        int target = offset + jump;
        if (!isLegalTarget(context, target))
            CCerror(context, "Illegal target of jump or branch");
        this_idata->operand.i = code_data[target];
        break;
    }

    case JVM_OPC_jsr_w:
        /* instruction of ret statement */
        this_idata->operand2.i = UNKNOWN_RET_INSTRUCTION;
        /* FALLTHROUGH */
    case JVM_OPC_goto_w: {
        /* Set the ->operand to be the instruction number of the target. */
        int jump = (((signed char)(code[offset+1])) << 24) +
                     (code[offset+2] << 16) + (code[offset+3] << 8) +
                     (code[offset + 4]);
        int target = offset + jump;
        if (!isLegalTarget(context, target))
            CCerror(context, "Illegal target of jump or branch");
        this_idata->operand.i = code_data[target];
        break;
    }

    case JVM_OPC_tableswitch:
    case JVM_OPC_lookupswitch: {
        /* Set the ->operand to be a table of possible instruction targets. */
        int *lpc = (int *) UCALIGN(code + offset + 1);
        int *lptr;
        int *saved_operand;
        int keys;
        int k, delta;

        if (context->major_version < NONZERO_PADDING_BYTES_IN_SWITCH_MAJOR_VERSION) {
            /* 4639449, 4647081: Padding bytes must be zero. */
            unsigned char* bptr = (unsigned char*) (code + offset + 1);
            for (; bptr < (unsigned char*)lpc; bptr++) {
                if (*bptr != 0) {
                    CCerror(context, "Non zero padding bytes in switch");
                }
            }
        }
        if (opcode == JVM_OPC_tableswitch) {
            keys = _ck_ntohl(lpc[2]) -  _ck_ntohl(lpc[1]) + 1;
            delta = 1;
        } else {
            keys = _ck_ntohl(lpc[1]); /* number of pairs */
            delta = 2;
            /* Make sure that the tableswitch items are sorted */
            for (k = keys - 1, lptr = &lpc[2]; --k >= 0; lptr += 2) {
                int this_key = _ck_ntohl(lptr[0]);  /* NB: ntohl may be unsigned */
                int next_key = _ck_ntohl(lptr[2]);
                if (this_key >= next_key) {
                    CCerror(context, "Unsorted lookup switch");
                }
            }
        }
        saved_operand = NEW(int, keys + 2);
        if (!isLegalTarget(context, offset + _ck_ntohl(lpc[0])))
            CCerror(context, "Illegal default target in switch");
        saved_operand[keys + 1] = code_data[offset + _ck_ntohl(lpc[0])];
        for (k = keys, lptr = &lpc[3]; --k >= 0; lptr += delta) {
            int target = offset + _ck_ntohl(lptr[0]);
            if (!isLegalTarget(context, target))
                CCerror(context, "Illegal branch in tableswitch");
            saved_operand[k + 1] = code_data[target];
        }
        saved_operand[0] = keys + 1; /* number of successors */
        this_idata->operand.ip = saved_operand;
        break;
    }

    case JVM_OPC_ldc: {
        /* Make sure the constant pool item is the right type. */
        int key = code[offset + 1];
        int types = (1 << JVM_CONSTANT_Integer) | (1 << JVM_CONSTANT_Float) |
                    (1 << JVM_CONSTANT_String);
        if (context->major_version >= LDC_CLASS_MAJOR_VERSION) {
            types |= 1 << JVM_CONSTANT_Class;
        }
        if (context->major_version >= LDC_METHOD_HANDLE_MAJOR_VERSION) {
            types |= (1 << JVM_CONSTANT_MethodHandle) |
                     (1 << JVM_CONSTANT_MethodType);
        }
        this_idata->operand.i = key;
        verify_constant_pool_type(context, key, types);
        break;
    }

    case JVM_OPC_ldc_w: {
        /* Make sure the constant pool item is the right type. */
        int key = (code[offset + 1] << 8) + code[offset + 2];
        int types = (1 << JVM_CONSTANT_Integer) | (1 << JVM_CONSTANT_Float) |
                    (1 << JVM_CONSTANT_String);
        if (context->major_version >= LDC_CLASS_MAJOR_VERSION) {
            types |= 1 << JVM_CONSTANT_Class;
        }
        if (context->major_version >= LDC_METHOD_HANDLE_MAJOR_VERSION) {
            types |= (1 << JVM_CONSTANT_MethodHandle) |
                     (1 << JVM_CONSTANT_MethodType);
        }
        this_idata->operand.i = key;
        verify_constant_pool_type(context, key, types);
        break;
    }

    case JVM_OPC_ldc2_w: {
        /* Make sure the constant pool item is the right type. */
        int key = (code[offset + 1] << 8) + code[offset + 2];
        int types = (1 << JVM_CONSTANT_Double) | (1 << JVM_CONSTANT_Long);
        this_idata->operand.i = key;
        verify_constant_pool_type(context, key, types);
        break;
    }

    case JVM_OPC_getfield: case JVM_OPC_putfield:
    case JVM_OPC_getstatic: case JVM_OPC_putstatic: {
        /* Make sure the constant pool item is the right type. */
        int key = (code[offset + 1] << 8) + code[offset + 2];
        this_idata->operand.i = key;
        verify_constant_pool_type(context, key, 1 << JVM_CONSTANT_Fieldref);
        if (opcode == JVM_OPC_getfield || opcode == JVM_OPC_putfield)
            set_protected(context, inumber, key, opcode);
        break;
    }

    case JVM_OPC_invokevirtual:
    case JVM_OPC_invokespecial:
    case JVM_OPC_invokestatic:
    case JVM_OPC_invokeinterface: {
        /* Make sure the constant pool item is the right type. */
        int key = (code[offset + 1] << 8) + code[offset + 2];
        const char *methodname;
        jclass cb = context->class;
        fullinfo_type clazz_info;
        int is_constructor, is_internal;
        int kind;

        switch (opcode ) {
        case JVM_OPC_invokestatic:
            kind = ((context->major_version < STATIC_METHOD_IN_INTERFACE_MAJOR_VERSION)
                       ? (1 << JVM_CONSTANT_Methodref)
                       : ((1 << JVM_CONSTANT_InterfaceMethodref) | (1 << JVM_CONSTANT_Methodref)));
            break;
        case JVM_OPC_invokeinterface:
            kind = 1 << JVM_CONSTANT_InterfaceMethodref;
            break;
        default:
            kind = 1 << JVM_CONSTANT_Methodref;
        }

        /* Make sure the constant pool item is the right type. */
        verify_constant_pool_type(context, key, kind);
        methodname = JVM_GetCPMethodNameUTF(env, cb, key);
        check_and_push(context, methodname, VM_STRING_UTF);
        is_constructor = !strcmp(methodname, "<init>");
        is_internal = methodname[0] == '<';
        pop_and_free(context);

        clazz_info = cp_index_to_class_fullinfo(context, key,
                                                JVM_CONSTANT_Methodref);
        this_idata->operand.i = key;
        this_idata->operand2.fi = clazz_info;
        if (is_constructor) {
            if (opcode != JVM_OPC_invokespecial) {
                CCerror(context,
                        "Must call initializers using invokespecial");
            }
            this_idata->opcode = JVM_OPC_invokeinit;
        } else {
            if (is_internal) {
                CCerror(context, "Illegal call to internal method");
            }
            if (opcode == JVM_OPC_invokespecial
                   && clazz_info != context->currentclass_info
                   && clazz_info != context->superclass_info) {
                int not_found = 1;

                jclass super = (*env)->GetSuperclass(env, context->class);
                while(super != 0) {
                    jclass tmp_cb;
                    fullinfo_type new_info = make_class_info(context, super);
                    if (clazz_info == new_info) {
                        not_found = 0;
                        break;
                    }
                    tmp_cb = (*env)->GetSuperclass(env, super);
                    (*env)->DeleteLocalRef(env, super);
                    super = tmp_cb;
                }
                (*env)->DeleteLocalRef(env, super);

                /* The optimizer may cause this to happen on local code */
                if (not_found) {
                    CCerror(context, "Illegal use of nonvirtual function call");
                }
            }
        }
        if (opcode == JVM_OPC_invokeinterface) {
            unsigned int args1;
            unsigned int args2;
            const char *signature =
                JVM_GetCPMethodSignatureUTF(env, context->class, key);
            check_and_push(context, signature, VM_STRING_UTF);
            args1 = signature_to_args_size(signature) + 1;
            args2 = code[offset + 3];
            if (args1 != args2) {
                CCerror(context,
                        "Inconsistent args_size for invokeinterface");
            }
            if (code[offset + 4] != 0) {
                CCerror(context,
                        "Fourth operand byte of invokeinterface must be zero");
            }
            pop_and_free(context);
        } else if (opcode == JVM_OPC_invokevirtual
                      || opcode == JVM_OPC_invokespecial)
            set_protected(context, inumber, key, opcode);
        break;
    }

    case JVM_OPC_invokedynamic:
        CCerror(context,
                "invokedynamic bytecode is not supported in this class file version");
        break;
    case JVM_OPC_instanceof:
    case JVM_OPC_checkcast:
    case JVM_OPC_new:
    case JVM_OPC_anewarray:
    case JVM_OPC_multianewarray: {
        /* Make sure the constant pool item is a class */
        int key = (code[offset + 1] << 8) + code[offset + 2];
        fullinfo_type target;
        verify_constant_pool_type(context, key, 1 << JVM_CONSTANT_Class);
        target = cp_index_to_class_fullinfo(context, key, JVM_CONSTANT_Class);
        if (GET_ITEM_TYPE(target) == ITEM_Bogus)
            CCerror(context, "Illegal type");
        switch(opcode) {
        case JVM_OPC_anewarray:
            if ((GET_INDIRECTION(target)) >= MAX_ARRAY_DIMENSIONS)
                CCerror(context, "Array with too many dimensions");
            this_idata->operand.fi = MAKE_FULLINFO(GET_ITEM_TYPE(target),
                                                   GET_INDIRECTION(target) + 1,
                                                   GET_EXTRA_INFO(target));
            break;
        case JVM_OPC_new:
            if (WITH_ZERO_EXTRA_INFO(target) !=
                             MAKE_FULLINFO(ITEM_Object, 0, 0))
                CCerror(context, "Illegal creation of multi-dimensional array");
            /* operand gets set to the "unitialized object".  operand2 gets
             * set to what the value will be after it's initialized. */
            this_idata->operand.fi = MAKE_FULLINFO(ITEM_NewObject, 0, inumber);
            this_idata->operand2.fi = target;
            break;
        case JVM_OPC_multianewarray:
            this_idata->operand.fi = target;
            this_idata->operand2.i = code[offset + 3];
            if (    (this_idata->operand2.i > (int)GET_INDIRECTION(target))
                 || (this_idata->operand2.i == 0))
                CCerror(context, "Illegal dimension argument");
            break;
        default:
            this_idata->operand.fi = target;
        }
        break;
    }

    case JVM_OPC_newarray: {
        /* Cache the result of the JVM_OPC_newarray into the operand slot */
        fullinfo_type full_info;
        switch (code[offset + 1]) {
            case JVM_T_INT:
                full_info = MAKE_FULLINFO(ITEM_Integer, 1, 0); break;
            case JVM_T_LONG:
                full_info = MAKE_FULLINFO(ITEM_Long, 1, 0); break;
            case JVM_T_FLOAT:
                full_info = MAKE_FULLINFO(ITEM_Float, 1, 0); break;
            case JVM_T_DOUBLE:
                full_info = MAKE_FULLINFO(ITEM_Double, 1, 0); break;
            case JVM_T_BOOLEAN:
                full_info = MAKE_FULLINFO(ITEM_Boolean, 1, 0); break;
            case JVM_T_BYTE:
                full_info = MAKE_FULLINFO(ITEM_Byte, 1, 0); break;
            case JVM_T_CHAR:
                full_info = MAKE_FULLINFO(ITEM_Char, 1, 0); break;
            case JVM_T_SHORT:
                full_info = MAKE_FULLINFO(ITEM_Short, 1, 0); break;
            default:
                full_info = 0;          /* Keep lint happy */
                CCerror(context, "Bad type passed to newarray");
        }
        this_idata->operand.fi = full_info;
        break;
    }

    /* Fudge iload_x, aload_x, etc to look like their generic cousin. */
    case JVM_OPC_iload_0: case JVM_OPC_iload_1: case JVM_OPC_iload_2: case JVM_OPC_iload_3:
        this_idata->opcode = JVM_OPC_iload;
        var = opcode - JVM_OPC_iload_0;
        goto check_local_variable;

    case JVM_OPC_fload_0: case JVM_OPC_fload_1: case JVM_OPC_fload_2: case JVM_OPC_fload_3:
        this_idata->opcode = JVM_OPC_fload;
        var = opcode - JVM_OPC_fload_0;
        goto check_local_variable;

    case JVM_OPC_aload_0: case JVM_OPC_aload_1: case JVM_OPC_aload_2: case JVM_OPC_aload_3:
        this_idata->opcode = JVM_OPC_aload;
        var = opcode - JVM_OPC_aload_0;
        goto check_local_variable;

    case JVM_OPC_lload_0: case JVM_OPC_lload_1: case JVM_OPC_lload_2: case JVM_OPC_lload_3:
        this_idata->opcode = JVM_OPC_lload;
        var = opcode - JVM_OPC_lload_0;
        goto check_local_variable2;

    case JVM_OPC_dload_0: case JVM_OPC_dload_1: case JVM_OPC_dload_2: case JVM_OPC_dload_3:
        this_idata->opcode = JVM_OPC_dload;
        var = opcode - JVM_OPC_dload_0;
        goto check_local_variable2;

    case JVM_OPC_istore_0: case JVM_OPC_istore_1: case JVM_OPC_istore_2: case JVM_OPC_istore_3:
        this_idata->opcode = JVM_OPC_istore;
        var = opcode - JVM_OPC_istore_0;
        goto check_local_variable;

    case JVM_OPC_fstore_0: case JVM_OPC_fstore_1: case JVM_OPC_fstore_2: case JVM_OPC_fstore_3:
        this_idata->opcode = JVM_OPC_fstore;
        var = opcode - JVM_OPC_fstore_0;
        goto check_local_variable;

    case JVM_OPC_astore_0: case JVM_OPC_astore_1: case JVM_OPC_astore_2: case JVM_OPC_astore_3:
        this_idata->opcode = JVM_OPC_astore;
        var = opcode - JVM_OPC_astore_0;
        goto check_local_variable;

    case JVM_OPC_lstore_0: case JVM_OPC_lstore_1: case JVM_OPC_lstore_2: case JVM_OPC_lstore_3:
        this_idata->opcode = JVM_OPC_lstore;
        var = opcode - JVM_OPC_lstore_0;
        goto check_local_variable2;

    case JVM_OPC_dstore_0: case JVM_OPC_dstore_1: case JVM_OPC_dstore_2: case JVM_OPC_dstore_3:
        this_idata->opcode = JVM_OPC_dstore;
        var = opcode - JVM_OPC_dstore_0;
        goto check_local_variable2;

    case JVM_OPC_wide:
        this_idata->opcode = code[offset + 1];
        var = (code[offset + 2] << 8) + code[offset + 3];
        switch(this_idata->opcode) {
            case JVM_OPC_lload:  case JVM_OPC_dload:
            case JVM_OPC_lstore: case JVM_OPC_dstore:
                goto check_local_variable2;
            default:
                goto check_local_variable;
        }

    case JVM_OPC_iinc:              /* the increment amount doesn't matter */
    case JVM_OPC_ret:
    case JVM_OPC_aload: case JVM_OPC_iload: case JVM_OPC_fload:
    case JVM_OPC_astore: case JVM_OPC_istore: case JVM_OPC_fstore:
        var = code[offset + 1];
    check_local_variable:
        /* Make sure that the variable number isn't illegal. */
        this_idata->operand.i = var;
        if (var >= JVM_GetMethodIxLocalsCount(env, context->class, mi))
            CCerror(context, "Illegal local variable number");
        break;

    case JVM_OPC_lload: case JVM_OPC_dload: case JVM_OPC_lstore: case JVM_OPC_dstore:
        var = code[offset + 1];
    check_local_variable2:
        /* Make sure that the variable number isn't illegal. */
        this_idata->operand.i = var;
        if ((var + 1) >= JVM_GetMethodIxLocalsCount(env, context->class, mi))
            CCerror(context, "Illegal local variable number");
        break;

    default:
        if (opcode > JVM_OPC_MAX)
            CCerror(context, "Quick instructions shouldn't appear yet.");
        break;
    } /* of switch */
}


static void
set_protected(context_type *context, unsigned int inumber, int key, int opcode)
{
    JNIEnv *env = context->env;
    fullinfo_type clazz_info;
    if (opcode != JVM_OPC_invokevirtual && opcode != JVM_OPC_invokespecial) {
        clazz_info = cp_index_to_class_fullinfo(context, key,
                                                JVM_CONSTANT_Fieldref);
    } else {
        clazz_info = cp_index_to_class_fullinfo(context, key,
                                                JVM_CONSTANT_Methodref);
    }
    if (is_superclass(context, clazz_info)) {
        jclass calledClass =
            object_fullinfo_to_classclass(context, clazz_info);
        int access;
        /* 4734966: JVM_GetCPFieldModifiers() or JVM_GetCPMethodModifiers() only
           searches the referenced field or method in calledClass. The following
           while loop is added to search up the superclass chain to make this
           symbolic resolution consistent with the field/method resolution
           specified in VM spec 5.4.3. */
        calledClass = (*env)->NewLocalRef(env, calledClass);
        do {
            jclass tmp_cb;
            if (opcode != JVM_OPC_invokevirtual && opcode != JVM_OPC_invokespecial) {
                access = JVM_GetCPFieldModifiers
                    (env, context->class, key, calledClass);
            } else {
                access = JVM_GetCPMethodModifiers
                    (env, context->class, key, calledClass);
            }
            if (access != -1) {
                break;
            }
            tmp_cb = (*env)->GetSuperclass(env, calledClass);
            (*env)->DeleteLocalRef(env, calledClass);
            calledClass = tmp_cb;
        } while (calledClass != 0);

        if (access == -1) {
            /* field/method not found, detected at runtime. */
        } else if (access & JVM_ACC_PROTECTED) {
            if (!JVM_IsSameClassPackage(env, calledClass, context->class))
                context->instruction_data[inumber].protected = JNI_TRUE;
        }
        (*env)->DeleteLocalRef(env, calledClass);
    }
}


static jboolean
is_superclass(context_type *context, fullinfo_type clazz_info) {
    fullinfo_type *fptr = context->superclasses;

    if (fptr == 0)
        return JNI_FALSE;
    for (; *fptr != 0; fptr++) {
        if (*fptr == clazz_info)
            return JNI_TRUE;
    }
    return JNI_FALSE;
}


/* Look through each item on the exception table.  Each of the fields must
 * refer to a legal instruction.
 */
static void
initialize_exception_table(context_type *context)
{
    JNIEnv *env = context->env;
    int mi = context->method_index;
    struct handler_info_type *handler_info = context->handler_info;
    int *code_data = context->code_data;
    int code_length = context->code_length;
    int max_stack_size = JVM_GetMethodIxMaxStack(env, context->class, mi);
    int i = JVM_GetMethodIxExceptionTableLength(env, context->class, mi);
    if (max_stack_size < 1 && i > 0) {
        // If the method contains exception handlers, it must have room
        // on the expression stack for the exception that the VM could push
        CCerror(context, "Stack size too large");
    }
    for (; --i >= 0; handler_info++) {
        JVM_ExceptionTableEntryType einfo;
        stack_item_type *stack_item = NEW(stack_item_type, 1);

        JVM_GetMethodIxExceptionTableEntry(env, context->class, mi,
                                           i, &einfo);

        if (!(einfo.start_pc < einfo.end_pc &&
              einfo.start_pc >= 0 &&
              isLegalTarget(context, einfo.start_pc) &&
              (einfo.end_pc ==  code_length ||
               isLegalTarget(context, einfo.end_pc)))) {
            CFerror(context, "Illegal exception table range");
        }
        if (!((einfo.handler_pc > 0) &&
              isLegalTarget(context, einfo.handler_pc))) {
            CFerror(context, "Illegal exception table handler");
        }

        handler_info->start = code_data[einfo.start_pc];
        /* einfo.end_pc may point to one byte beyond the end of bytecodes. */
        handler_info->end = (einfo.end_pc == context->code_length) ?
            context->instruction_count : code_data[einfo.end_pc];
        handler_info->handler = code_data[einfo.handler_pc];
        handler_info->stack_info.stack = stack_item;
        handler_info->stack_info.stack_size = 1;
        stack_item->next = NULL;
        if (einfo.catchType != 0) {
            const char *classname;
            /* Constant pool entry type has been checked in format checker */
            classname = JVM_GetCPClassNameUTF(env,
                                              context->class,
                                              einfo.catchType);
            check_and_push(context, classname, VM_STRING_UTF);
            stack_item->item = make_class_info_from_name(context, classname);
            if (!isAssignableTo(context,
                                stack_item->item,
                                context->throwable_info))
                CCerror(context, "catch_type not a subclass of Throwable");
            pop_and_free(context);
        } else {
            stack_item->item = context->throwable_info;
        }
    }
}


/* Given a pointer to an instruction, return its length.  Use the table
 * opcode_length[] which is automatically built.
 */
static int instruction_length(unsigned char *iptr, unsigned char *end)
{
    static unsigned char opcode_length[] = JVM_OPCODE_LENGTH_INITIALIZER;
    int instruction = *iptr;
    switch (instruction) {
        case JVM_OPC_tableswitch: {
            int *lpc = (int *)UCALIGN(iptr + 1);
            int index;
            if (lpc + 2 >= (int *)end) {
                return -1; /* do not read pass the end */
            }
            index = _ck_ntohl(lpc[2]) - _ck_ntohl(lpc[1]);
            if ((index < 0) || (index > 65535)) {
                return -1;      /* illegal */
            } else {
                unsigned char *finish = (unsigned char *)(&lpc[index + 4]);
                assert(finish >= iptr);
                return (int)(finish - iptr);
            }
        }

        case JVM_OPC_lookupswitch: {
            int *lpc = (int *) UCALIGN(iptr + 1);
            int npairs;
            if (lpc + 1 >= (int *)end)
                return -1; /* do not read pass the end */
            npairs = _ck_ntohl(lpc[1]);
            /* There can't be more than 64K labels because of the limit
             * on per-method byte code length.
             */
            if (npairs < 0 || npairs >= 65536)
                return  -1;
            else {
                unsigned char *finish = (unsigned char *)(&lpc[2 * (npairs + 1)]);
                assert(finish >= iptr);
                return (int)(finish - iptr);
            }
        }

        case JVM_OPC_wide:
            if (iptr + 1 >= end)
                return -1; /* do not read pass the end */
            switch(iptr[1]) {
                case JVM_OPC_ret:
                case JVM_OPC_iload: case JVM_OPC_istore:
                case JVM_OPC_fload: case JVM_OPC_fstore:
                case JVM_OPC_aload: case JVM_OPC_astore:
                case JVM_OPC_lload: case JVM_OPC_lstore:
                case JVM_OPC_dload: case JVM_OPC_dstore:
                    return 4;
                case JVM_OPC_iinc:
                    return 6;
                default:
                    return -1;
            }

        default: {
            if (instruction < 0 || instruction > JVM_OPC_MAX)
                return -1;

            /* A length of 0 indicates an error. */
            if (opcode_length[instruction] <= 0)
                return -1;

            return opcode_length[instruction];
        }
    }
}


/* Given the target of a branch, make sure that it's a legal target. */
static jboolean
isLegalTarget(context_type *context, int offset)
{
    int code_length = context->code_length;
    int *code_data = context->code_data;
    return (offset >= 0 && offset < code_length && code_data[offset] >= 0);
}


/* Make sure that an element of the constant pool really is of the indicated
 * type.
 */
static void
verify_constant_pool_type(context_type *context, int index, unsigned mask)
{
    int nconstants = context->nconstants;
    unsigned char *type_table = context->constant_types;
    unsigned type;

    if ((index <= 0) || (index >= nconstants))
        CCerror(context, "Illegal constant pool index");

    type = type_table[index];
    if ((mask & (1 << type)) == 0)
        CCerror(context, "Illegal type in constant pool");
}


static void
initialize_dataflow(context_type *context)
{
    JNIEnv *env = context->env;
    instruction_data_type *idata = context->instruction_data;
    int mi = context->method_index;
    jclass cb = context->class;
    int args_size = JVM_GetMethodIxArgsSize(env, cb, mi);
    fullinfo_type *reg_ptr;
    fullinfo_type full_info;
    const char *p;
    const char *signature;

    /* Initialize the function entry, since we know everything about it. */
    idata[0].stack_info.stack_size = 0;
    idata[0].stack_info.stack = NULL;
    idata[0].register_info.register_count = args_size;
    idata[0].register_info.registers = NEW(fullinfo_type, args_size);
    idata[0].register_info.mask_count = 0;
    idata[0].register_info.masks = NULL;
    idata[0].and_flags = 0;     /* nothing needed */
    idata[0].or_flags = FLAG_REACHED; /* instruction reached */
    reg_ptr = idata[0].register_info.registers;

    if ((JVM_GetMethodIxModifiers(env, cb, mi) & JVM_ACC_STATIC) == 0) {
        /* A non static method.  If this is an <init> method, the first
         * argument is an uninitialized object.  Otherwise it is an object of
         * the given class type.  java.lang.Object.<init> is special since
         * we don't call its superclass <init> method.
         */
        if (JVM_IsConstructorIx(env, cb, mi)
                && context->currentclass_info != context->object_info) {
            *reg_ptr++ = MAKE_FULLINFO(ITEM_InitObject, 0, 0);
            idata[0].or_flags |= FLAG_NEED_CONSTRUCTOR;
        } else {
            *reg_ptr++ = context->currentclass_info;
        }
    }
    signature = JVM_GetMethodIxSignatureUTF(env, cb, mi);
    check_and_push(context, signature, VM_STRING_UTF);
    /* Fill in each of the arguments into the registers. */
    for (p = signature + 1; *p != JVM_SIGNATURE_ENDFUNC; ) {
        char fieldchar = signature_to_fieldtype(context, &p, &full_info);
        switch (fieldchar) {
            case 'D': case 'L':
                *reg_ptr++ = full_info;
                *reg_ptr++ = full_info + 1;
                break;
            default:
                *reg_ptr++ = full_info;
                break;
        }
    }
    p++;                        /* skip over right parenthesis */
    if (*p == 'V') {
        context->return_type = MAKE_FULLINFO(ITEM_Void, 0, 0);
    } else {
        signature_to_fieldtype(context, &p, &full_info);
        context->return_type = full_info;
    }
    pop_and_free(context);
    /* Indicate that we need to look at the first instruction. */
    idata[0].changed = JNI_TRUE;
}


/* Run the data flow analysis, as long as there are things to change. */
static void
run_dataflow(context_type *context) {
    JNIEnv *env = context->env;
    int mi = context->method_index;
    jclass cb = context->class;
    int max_stack_size = JVM_GetMethodIxMaxStack(env, cb, mi);
    instruction_data_type *idata = context->instruction_data;
    unsigned int icount = context->instruction_count;
    jboolean work_to_do = JNI_TRUE;
    unsigned int inumber;

    /* Run through the loop, until there is nothing left to do. */
    while (work_to_do) {
        work_to_do = JNI_FALSE;
        for (inumber = 0; inumber < icount; inumber++) {
            instruction_data_type *this_idata = &idata[inumber];
            if (this_idata->changed) {
                register_info_type new_register_info;
                stack_info_type new_stack_info;
                flag_type new_and_flags, new_or_flags;

                this_idata->changed = JNI_FALSE;
                work_to_do = JNI_TRUE;
#ifdef DEBUG
                if (verify_verbose) {
                    int opcode = this_idata->opcode;
                    jio_fprintf(stdout, "Instruction %d: ", inumber);
                    print_stack(context, &this_idata->stack_info);
                    print_registers(context, &this_idata->register_info);
                    print_flags(context,
                                this_idata->and_flags, this_idata->or_flags);
                    fflush(stdout);
                }
#endif
                /* Make sure the registers and flags are appropriate */
                check_register_values(context, inumber);
                check_flags(context, inumber);

                /* Make sure the stack can deal with this instruction */
                pop_stack(context, inumber, &new_stack_info);

                /* Update the registers  and flags */
                update_registers(context, inumber, &new_register_info);
                update_flags(context, inumber, &new_and_flags, &new_or_flags);

                /* Update the stack. */
                push_stack(context, inumber, &new_stack_info);

                if (new_stack_info.stack_size > max_stack_size)
                    CCerror(context, "Stack size too large");
#ifdef DEBUG
                if (verify_verbose) {
                    jio_fprintf(stdout, "  ");
                    print_stack(context, &new_stack_info);
                    print_registers(context, &new_register_info);
                    print_flags(context, new_and_flags, new_or_flags);
                    fflush(stdout);
                }
#endif
                /* Add the new stack and register information to any
                 * instructions that can follow this instruction.     */
                merge_into_successors(context, inumber,
                                      &new_register_info, &new_stack_info,
                                      new_and_flags, new_or_flags);
            }
        }
    }
}


/* Make sure that the registers contain a legitimate value for the given
 * instruction.
*/

static void
check_register_values(context_type *context, unsigned int inumber)
{
    instruction_data_type *idata = context->instruction_data;
    instruction_data_type *this_idata = &idata[inumber];
    int opcode = this_idata->opcode;
    int operand = this_idata->operand.i;
    int register_count = this_idata->register_info.register_count;
    fullinfo_type *registers = this_idata->register_info.registers;
    jboolean double_word = JNI_FALSE;   /* default value */
    int type;

    switch (opcode) {
        default:
            return;
        case JVM_OPC_iload: case JVM_OPC_iinc:
            type = ITEM_Integer; break;
        case JVM_OPC_fload:
            type = ITEM_Float; break;
        case JVM_OPC_aload:
            type = ITEM_Object; break;
        case JVM_OPC_ret:
            type = ITEM_ReturnAddress; break;
        case JVM_OPC_lload:
            type = ITEM_Long; double_word = JNI_TRUE; break;
        case JVM_OPC_dload:
            type = ITEM_Double; double_word = JNI_TRUE; break;
    }
    if (!double_word) {
        fullinfo_type reg;
        /* Make sure we don't have an illegal register or one with wrong type */
        if (operand >= register_count) {
            CCerror(context,
                    "Accessing value from uninitialized register %d", operand);
        }
        reg = registers[operand];

        if (WITH_ZERO_EXTRA_INFO(reg) == (unsigned)MAKE_FULLINFO(type, 0, 0)) {
            /* the register is obviously of the given type */
            return;
        } else if (GET_INDIRECTION(reg) > 0 && type == ITEM_Object) {
            /* address type stuff be used on all arrays */
            return;
        } else if (GET_ITEM_TYPE(reg) == ITEM_ReturnAddress) {
            CCerror(context, "Cannot load return address from register %d",
                              operand);
            /* alternatively
                      (GET_ITEM_TYPE(reg) == ITEM_ReturnAddress)
                   && (opcode == JVM_OPC_iload)
                   && (type == ITEM_Object || type == ITEM_Integer)
               but this never occurs
            */
        } else if (reg == ITEM_InitObject && type == ITEM_Object) {
            return;
        } else if (WITH_ZERO_EXTRA_INFO(reg) ==
                        MAKE_FULLINFO(ITEM_NewObject, 0, 0) &&
                   type == ITEM_Object) {
            return;
        } else {
            CCerror(context, "Register %d contains wrong type", operand);
        }
    } else {
        /* Make sure we don't have an illegal register or one with wrong type */
        if ((operand + 1) >= register_count) {
            CCerror(context,
                    "Accessing value from uninitialized register pair %d/%d",
                    operand, operand+1);
        } else {
            if ((registers[operand] == (unsigned)MAKE_FULLINFO(type, 0, 0)) &&
                (registers[operand + 1] == (unsigned)MAKE_FULLINFO(type + 1, 0, 0))) {
                return;
            } else {
                CCerror(context, "Register pair %d/%d contains wrong type",
                        operand, operand+1);
            }
        }
    }
}


/* Make sure the flags contain legitimate values for this instruction.
*/

static void
check_flags(context_type *context, unsigned int inumber)
{
    instruction_data_type *idata = context->instruction_data;
    instruction_data_type *this_idata = &idata[inumber];
    int opcode = this_idata->opcode;
    switch (opcode) {
        case JVM_OPC_return:
            /* We need a constructor, but we aren't guaranteed it's called */
            if ((this_idata->or_flags & FLAG_NEED_CONSTRUCTOR) &&
                   !(this_idata->and_flags & FLAG_CONSTRUCTED))
                CCerror(context, "Constructor must call super() or this()");
            /* fall through */
        case JVM_OPC_ireturn: case JVM_OPC_lreturn:
        case JVM_OPC_freturn: case JVM_OPC_dreturn: case JVM_OPC_areturn:
            if (this_idata->or_flags & FLAG_NO_RETURN)
                /* This method cannot exit normally */
                CCerror(context, "Cannot return normally");
        default:
            break; /* nothing to do. */
    }
}

/* Make sure that the top of the stack contains reasonable values for the
 * given instruction.  The post-pop values of the stack and its size are
 * returned in *new_stack_info.
 */

static void
pop_stack(context_type *context, unsigned int inumber, stack_info_type *new_stack_info)
{
    instruction_data_type *idata = context->instruction_data;
    instruction_data_type *this_idata = &idata[inumber];
    int opcode = this_idata->opcode;
    stack_item_type *stack = this_idata->stack_info.stack;
    int stack_size = this_idata->stack_info.stack_size;
    char *stack_operands, *p;
    char buffer[257];           /* for holding manufactured argument lists */
    fullinfo_type stack_extra_info_buffer[256]; /* save info popped off stack */
    fullinfo_type *stack_extra_info = &stack_extra_info_buffer[256];
    fullinfo_type full_info;    /* only used in case of invoke instructions */
    fullinfo_type put_full_info; /* only used in case JVM_OPC_putstatic and JVM_OPC_putfield */

    switch(opcode) {
        default:
            /* For most instructions, we just use a built-in table */
            stack_operands = opcode_in_out[opcode][0];
            break;

        case JVM_OPC_putstatic: case JVM_OPC_putfield: {
            /* The top thing on the stack depends on the signature of
             * the object.                         */
            int operand = this_idata->operand.i;
            const char *signature =
                JVM_GetCPFieldSignatureUTF(context->env,
                                           context->class,
                                           operand);
            char *ip = buffer;
            check_and_push(context, signature, VM_STRING_UTF);
#ifdef DEBUG
            if (verify_verbose) {
                print_formatted_fieldname(context, operand);
            }
#endif
            if (opcode == JVM_OPC_putfield)
                *ip++ = 'A';    /* object for putfield */
            *ip++ = signature_to_fieldtype(context, &signature, &put_full_info);
            *ip = '\0';
            stack_operands = buffer;
            pop_and_free(context);
            break;
        }

        case JVM_OPC_invokevirtual: case JVM_OPC_invokespecial:
        case JVM_OPC_invokeinit:    /* invokespecial call to <init> */
        case JVM_OPC_invokestatic: case JVM_OPC_invokeinterface: {
            /* The top stuff on the stack depends on the method signature */
            int operand = this_idata->operand.i;
            const char *signature =
                JVM_GetCPMethodSignatureUTF(context->env,
                                            context->class,
                                            operand);
            char *ip = buffer;
            const char *p;
            check_and_push(context, signature, VM_STRING_UTF);
#ifdef DEBUG
            if (verify_verbose) {
                print_formatted_methodname(context, operand);
            }
#endif
            if (opcode != JVM_OPC_invokestatic)
                /* First, push the object */
                *ip++ = (opcode == JVM_OPC_invokeinit ? '@' : 'A');
            for (p = signature + 1; *p != JVM_SIGNATURE_ENDFUNC; ) {
                *ip++ = signature_to_fieldtype(context, &p, &full_info);
                if (ip >= buffer + sizeof(buffer) - 1)
                    CCerror(context, "Signature %s has too many arguments",
                            signature);
            }
            *ip = 0;
            stack_operands = buffer;
            pop_and_free(context);
            break;
        }

        case JVM_OPC_multianewarray: {
            /* Count can't be larger than 255. So can't overflow buffer */
            int count = this_idata->operand2.i; /* number of ints on stack */
            memset(buffer, 'I', count);
            buffer[count] = '\0';
            stack_operands = buffer;
            break;
        }

    } /* of switch */

    /* Run through the list of operands >>backwards<< */
    for (   p = stack_operands + strlen(stack_operands);
            p > stack_operands;
            stack = stack->next) {
        int type = *--p;
        fullinfo_type top_type = stack ? stack->item : 0;
        int size = (type == 'D' || type == 'L') ? 2 : 1;
        *--stack_extra_info = top_type;
        if (stack == NULL)
            CCerror(context, "Unable to pop operand off an empty stack");

        switch (type) {
            case 'I':
                if (top_type != MAKE_FULLINFO(ITEM_Integer, 0, 0))
                    CCerror(context, "Expecting to find integer on stack");
                break;

            case 'F':
                if (top_type != MAKE_FULLINFO(ITEM_Float, 0, 0))
                    CCerror(context, "Expecting to find float on stack");
                break;

            case 'A':           /* object or array */
                if (   (GET_ITEM_TYPE(top_type) != ITEM_Object)
                    && (GET_INDIRECTION(top_type) == 0)) {
                    /* The thing isn't an object or an array.  Let's see if it's
                     * one of the special cases  */
                    if (  (WITH_ZERO_EXTRA_INFO(top_type) ==
                                MAKE_FULLINFO(ITEM_ReturnAddress, 0, 0))
                        && (opcode == JVM_OPC_astore))
                        break;
                    if (   (GET_ITEM_TYPE(top_type) == ITEM_NewObject
                            || (GET_ITEM_TYPE(top_type) == ITEM_InitObject))
                        && ((opcode == JVM_OPC_astore) || (opcode == JVM_OPC_aload)
                            || (opcode == JVM_OPC_ifnull) || (opcode == JVM_OPC_ifnonnull)))
                        break;
                    /* The 2nd edition VM of the specification allows field
                     * initializations before the superclass initializer,
                     * if the field is defined within the current class.
                     */
                     if (   (GET_ITEM_TYPE(top_type) == ITEM_InitObject)
                         && (opcode == JVM_OPC_putfield)) {
                        int operand = this_idata->operand.i;
                        int access_bits = JVM_GetCPFieldModifiers(context->env,
                                                                  context->class,
                                                                  operand,
                                                                  context->class);
                        /* Note: This relies on the fact that
                         * JVM_GetCPFieldModifiers retrieves only local fields,
                         * and does not respect inheritance.
                         */
                        if (access_bits != -1) {
                            if ( cp_index_to_class_fullinfo(context, operand, JVM_CONSTANT_Fieldref) ==
                                 context->currentclass_info ) {
                                top_type = context->currentclass_info;
                                *stack_extra_info = top_type;
                                break;
                            }
                        }
                    }
                    CCerror(context, "Expecting to find object/array on stack");
                }
                break;

            case '@': {         /* unitialized object, for call to <init> */
                int item_type = GET_ITEM_TYPE(top_type);
                if (item_type != ITEM_NewObject && item_type != ITEM_InitObject)
                    CCerror(context,
                            "Expecting to find unitialized object on stack");
                break;
            }

            case 'O':           /* object, not array */
                if (WITH_ZERO_EXTRA_INFO(top_type) !=
                       MAKE_FULLINFO(ITEM_Object, 0, 0))
                    CCerror(context, "Expecting to find object on stack");
                break;

            case 'a':           /* integer, object, or array */
                if (      (top_type != MAKE_FULLINFO(ITEM_Integer, 0, 0))
                       && (GET_ITEM_TYPE(top_type) != ITEM_Object)
                       && (GET_INDIRECTION(top_type) == 0))
                    CCerror(context,
                            "Expecting to find object, array, or int on stack");
                break;

            case 'D':           /* double */
                if (top_type != MAKE_FULLINFO(ITEM_Double, 0, 0))
                    CCerror(context, "Expecting to find double on stack");
                break;

            case 'L':           /* long */
                if (top_type != MAKE_FULLINFO(ITEM_Long, 0, 0))
                    CCerror(context, "Expecting to find long on stack");
                break;

            case ']':           /* array of some type */
                if (top_type == NULL_FULLINFO) {
                    /* do nothing */
                } else switch(p[-1]) {
                    case 'I':   /* array of integers */
                        if (top_type != MAKE_FULLINFO(ITEM_Integer, 1, 0) &&
                            top_type != NULL_FULLINFO)
                            CCerror(context,
                                    "Expecting to find array of ints on stack");
                        break;

                    case 'L':   /* array of longs */
                        if (top_type != MAKE_FULLINFO(ITEM_Long, 1, 0))
                            CCerror(context,
                                   "Expecting to find array of longs on stack");
                        break;

                    case 'F':   /* array of floats */
                        if (top_type != MAKE_FULLINFO(ITEM_Float, 1, 0))
                            CCerror(context,
                                 "Expecting to find array of floats on stack");
                        break;

                    case 'D':   /* array of doubles */
                        if (top_type != MAKE_FULLINFO(ITEM_Double, 1, 0))
                            CCerror(context,
                                "Expecting to find array of doubles on stack");
                        break;

                    case 'A': { /* array of addresses (arrays or objects) */
                        int indirection = GET_INDIRECTION(top_type);
                        if ((indirection == 0) ||
                            ((indirection == 1) &&
                                (GET_ITEM_TYPE(top_type) != ITEM_Object)))
                            CCerror(context,
                                "Expecting to find array of objects or arrays "
                                    "on stack");
                        break;
                    }

                    case 'B':    /* array of bytes or booleans */
                        if (top_type != MAKE_FULLINFO(ITEM_Byte, 1, 0) &&
                            top_type != MAKE_FULLINFO(ITEM_Boolean, 1, 0))
                            CCerror(context,
                                  "Expecting to find array of bytes or Booleans on stack");
                        break;

                    case 'C':   /* array of characters */
                        if (top_type != MAKE_FULLINFO(ITEM_Char, 1, 0))
                            CCerror(context,
                                  "Expecting to find array of chars on stack");
                        break;

                    case 'S':   /* array of shorts */
                        if (top_type != MAKE_FULLINFO(ITEM_Short, 1, 0))
                            CCerror(context,
                                 "Expecting to find array of shorts on stack");
                        break;

                    case '?':   /* any type of array is okay */
                        if (GET_INDIRECTION(top_type) == 0)
                            CCerror(context,
                                    "Expecting to find array on stack");
                        break;

                    default:
                        CCerror(context, "Internal error #1");
                        break;
                }
                p -= 2;         /* skip over [ <char> */
                break;

            case '1': case '2': case '3': case '4': /* stack swapping */
                if (top_type == MAKE_FULLINFO(ITEM_Double, 0, 0)
                    || top_type == MAKE_FULLINFO(ITEM_Long, 0, 0)) {
                    if ((p > stack_operands) && (p[-1] == '+')) {
                        context->swap_table[type - '1'] = top_type + 1;
                        context->swap_table[p[-2] - '1'] = top_type;
                        size = 2;
                        p -= 2;
                    } else {
                        CCerror(context,
                                "Attempt to split long or double on the stack");
                    }
                } else {
                    context->swap_table[type - '1'] = stack->item;
                    if ((p > stack_operands) && (p[-1] == '+'))
                        p--;    /* ignore */
                }
                break;
            case '+':           /* these should have been caught. */
            default:
                CCerror(context, "Internal error #2");
        }
        stack_size -= size;
    }

    /* For many of the opcodes that had an "A" in their field, we really
     * need to go back and do a little bit more accurate testing.  We can, of
     * course, assume that the minimal type checking has already been done.
     */
    switch (opcode) {
        default: break;
        case JVM_OPC_aastore: {     /* array index object  */
            fullinfo_type array_type = stack_extra_info[0];
            fullinfo_type object_type = stack_extra_info[2];
            fullinfo_type target_type = decrement_indirection(array_type);
            if ((GET_ITEM_TYPE(object_type) != ITEM_Object)
                    && (GET_INDIRECTION(object_type) == 0)) {
                CCerror(context, "Expecting reference type on operand stack in aastore");
            }
            if ((GET_ITEM_TYPE(target_type) != ITEM_Object)
                    && (GET_INDIRECTION(target_type) == 0)) {
                CCerror(context, "Component type of the array must be reference type in aastore");
            }
            break;
        }

        case JVM_OPC_putfield:
        case JVM_OPC_getfield:
        case JVM_OPC_putstatic: {
            int operand = this_idata->operand.i;
            fullinfo_type stack_object = stack_extra_info[0];
            if (opcode == JVM_OPC_putfield || opcode == JVM_OPC_getfield) {
                if (!isAssignableTo
                        (context,
                         stack_object,
                         cp_index_to_class_fullinfo
                             (context, operand, JVM_CONSTANT_Fieldref))) {
                    CCerror(context,
                            "Incompatible type for getting or setting field");
                }
                if (this_idata->protected &&
                    !isAssignableTo(context, stack_object,
                                    context->currentclass_info)) {
                    CCerror(context, "Bad access to protected data");
                }
            }
            if (opcode == JVM_OPC_putfield || opcode == JVM_OPC_putstatic) {
                int item = (opcode == JVM_OPC_putfield ? 1 : 0);
                if (!isAssignableTo(context,
                                    stack_extra_info[item], put_full_info)) {
                    CCerror(context, "Bad type in putfield/putstatic");
                }
            }
            break;
        }

        case JVM_OPC_athrow:
            if (!isAssignableTo(context, stack_extra_info[0],
                                context->throwable_info)) {
                CCerror(context, "Can only throw Throwable objects");
            }
            break;

        case JVM_OPC_aaload: {      /* array index */
            /* We need to pass the information to the stack updater */
            fullinfo_type array_type = stack_extra_info[0];
            context->swap_table[0] = decrement_indirection(array_type);
            break;
        }

        case JVM_OPC_invokevirtual: case JVM_OPC_invokespecial:
        case JVM_OPC_invokeinit:
        case JVM_OPC_invokeinterface: case JVM_OPC_invokestatic: {
            int operand = this_idata->operand.i;
            const char *signature =
                JVM_GetCPMethodSignatureUTF(context->env,
                                            context->class,
                                            operand);
            int item;
            const char *p;
            check_and_push(context, signature, VM_STRING_UTF);
            if (opcode == JVM_OPC_invokestatic) {
                item = 0;
            } else if (opcode == JVM_OPC_invokeinit) {
                fullinfo_type init_type = this_idata->operand2.fi;
                fullinfo_type object_type = stack_extra_info[0];
                context->swap_table[0] = object_type; /* save value */
                if (GET_ITEM_TYPE(stack_extra_info[0]) == ITEM_NewObject) {
                    /* We better be calling the appropriate init.  Find the
                     * inumber of the "JVM_OPC_new" instruction", and figure
                     * out what the type really is.
                     */
                    unsigned int new_inumber = GET_EXTRA_INFO(stack_extra_info[0]);
                    fullinfo_type target_type = idata[new_inumber].operand2.fi;
                    context->swap_table[1] = target_type;

                    if (target_type != init_type) {
                        CCerror(context, "Call to wrong initialization method");
                    }
                    if (this_idata->protected
                        && !isAssignableTo(context, object_type,
                                           context->currentclass_info)) {
                      CCerror(context, "Bad access to protected data");
                    }
                } else {
                    /* We better be calling super() or this(). */
                    if (init_type != context->superclass_info &&
                        init_type != context->currentclass_info) {
                        CCerror(context, "Call to wrong initialization method");
                    }
                    context->swap_table[1] = context->currentclass_info;
                }
                item = 1;
            } else {
                fullinfo_type target_type = this_idata->operand2.fi;
                fullinfo_type object_type = stack_extra_info[0];
                if (!isAssignableTo(context, object_type, target_type)){
                    CCerror(context,
                            "Incompatible object argument for function call");
                }
                if (opcode == JVM_OPC_invokespecial
                    && !isAssignableTo(context, object_type,
                                       context->currentclass_info)) {
                    /* Make sure object argument is assignment compatible to current class */
                    CCerror(context,
                            "Incompatible object argument for invokespecial");
                }
                if (this_idata->protected
                    && !isAssignableTo(context, object_type,
                                       context->currentclass_info)) {
                    /* This is ugly. Special dispensation.  Arrays pretend to
                       implement public Object clone() even though they don't */
                    const char *utfName =
                        JVM_GetCPMethodNameUTF(context->env,
                                               context->class,
                                               this_idata->operand.i);
                    int is_clone = utfName && (strcmp(utfName, "clone") == 0);
                    JVM_ReleaseUTF(utfName);

                    if ((target_type == context->object_info) &&
                        (GET_INDIRECTION(object_type) > 0) &&
                        is_clone) {
                    } else {
                        CCerror(context, "Bad access to protected data");
                    }
                }
                item = 1;
            }
            for (p = signature + 1; *p != JVM_SIGNATURE_ENDFUNC; item++)
                if (signature_to_fieldtype(context, &p, &full_info) == 'A') {
                    if (!isAssignableTo(context,
                                        stack_extra_info[item], full_info)) {
                        CCerror(context, "Incompatible argument to function");
                    }
                }

            pop_and_free(context);
            break;
        }

        case JVM_OPC_return:
            if (context->return_type != MAKE_FULLINFO(ITEM_Void, 0, 0))
                CCerror(context, "Wrong return type in function");
            break;

        case JVM_OPC_ireturn: case JVM_OPC_lreturn: case JVM_OPC_freturn:
        case JVM_OPC_dreturn: case JVM_OPC_areturn: {
            fullinfo_type target_type = context->return_type;
            fullinfo_type object_type = stack_extra_info[0];
            if (!isAssignableTo(context, object_type, target_type)) {
                CCerror(context, "Wrong return type in function");
            }
            break;
        }

        case JVM_OPC_new: {
            /* Make sure that nothing on the stack already looks like what
             * we want to create.  I can't image how this could possibly happen
             * but we should test for it anyway, since if it could happen, the
             * result would be an unitialized object being able to masquerade
             * as an initialized one.
             */
            stack_item_type *item;
            for (item = stack; item != NULL; item = item->next) {
                if (item->item == this_idata->operand.fi) {
                    CCerror(context,
                            "Uninitialized object on stack at creating point");
                }
            }
            /* Info for update_registers */
            context->swap_table[0] = this_idata->operand.fi;
            context->swap_table[1] = MAKE_FULLINFO(ITEM_Bogus, 0, 0);

            break;
        }
    }
    new_stack_info->stack = stack;
    new_stack_info->stack_size = stack_size;
}


/* We've already determined that the instruction is legal.  Perform the
 * operation on the registers, and return the updated results in
 * new_register_count_p and new_registers.
 */

static void
update_registers(context_type *context, unsigned int inumber,
                 register_info_type *new_register_info)
{
    instruction_data_type *idata = context->instruction_data;
    instruction_data_type *this_idata = &idata[inumber];
    int opcode = this_idata->opcode;
    int operand = this_idata->operand.i;
    int register_count = this_idata->register_info.register_count;
    fullinfo_type *registers = this_idata->register_info.registers;
    stack_item_type *stack = this_idata->stack_info.stack;
    int mask_count = this_idata->register_info.mask_count;
    mask_type *masks = this_idata->register_info.masks;

    /* Use these as default new values. */
    int            new_register_count = register_count;
    int            new_mask_count = mask_count;
    fullinfo_type *new_registers = registers;
    mask_type     *new_masks = masks;

    enum { ACCESS_NONE, ACCESS_SINGLE, ACCESS_DOUBLE } access = ACCESS_NONE;
    int i;

    /* Remember, we've already verified the type at the top of the stack. */
    switch (opcode) {
        default: break;
        case JVM_OPC_istore: case JVM_OPC_fstore: case JVM_OPC_astore:
            access = ACCESS_SINGLE;
            goto continue_store;

        case JVM_OPC_lstore: case JVM_OPC_dstore:
            access = ACCESS_DOUBLE;
            goto continue_store;

        continue_store: {
            /* We have a modification to the registers.  Copy them if needed. */
            fullinfo_type stack_top_type = stack->item;
            int max_operand = operand + ((access == ACCESS_DOUBLE) ? 1 : 0);

            if (     max_operand < register_count
                  && registers[operand] == stack_top_type
                  && ((access == ACCESS_SINGLE) ||
                         (registers[operand + 1]== stack_top_type + 1)))
                /* No changes have been made to the registers. */
                break;
            new_register_count = MAX(max_operand + 1, register_count);
            new_registers = NEW(fullinfo_type, new_register_count);
            for (i = 0; i < register_count; i++)
                new_registers[i] = registers[i];
            for (i = register_count; i < new_register_count; i++)
                new_registers[i] = MAKE_FULLINFO(ITEM_Bogus, 0, 0);
            new_registers[operand] = stack_top_type;
            if (access == ACCESS_DOUBLE)
                new_registers[operand + 1] = stack_top_type + 1;
            break;
        }

        case JVM_OPC_iload: case JVM_OPC_fload: case JVM_OPC_aload:
        case JVM_OPC_iinc: case JVM_OPC_ret:
            access = ACCESS_SINGLE;
            break;

        case JVM_OPC_lload: case JVM_OPC_dload:
            access = ACCESS_DOUBLE;
            break;

        case JVM_OPC_jsr: case JVM_OPC_jsr_w:
            for (i = 0; i < new_mask_count; i++)
                if (new_masks[i].entry == operand)
                    CCerror(context, "Recursive call to jsr entry");
            new_masks = add_to_masks(context, masks, mask_count, operand);
            new_mask_count++;
            break;

        case JVM_OPC_invokeinit:
        case JVM_OPC_new: {
            /* For invokeinit, an uninitialized object has been initialized.
             * For new, all previous occurrences of an uninitialized object
             * from the same instruction must be made bogus.
             * We find all occurrences of swap_table[0] in the registers, and
             * replace them with swap_table[1];
             */
            fullinfo_type from = context->swap_table[0];
            fullinfo_type to = context->swap_table[1];

            int i;
            for (i = 0; i < register_count; i++) {
                if (new_registers[i] == from) {
                    /* Found a match */
                    break;
                }
            }
            if (i < register_count) { /* We broke out loop for match */
                /* We have to change registers, and possibly a mask */
                jboolean copied_mask = JNI_FALSE;
                int k;
                new_registers = NEW(fullinfo_type, register_count);
                memcpy(new_registers, registers,
                       register_count * sizeof(registers[0]));
                for ( ; i < register_count; i++) {
                    if (new_registers[i] == from) {
                        new_registers[i] = to;
                        for (k = 0; k < new_mask_count; k++) {
                            if (!IS_BIT_SET(new_masks[k].modifies, i)) {
                                if (!copied_mask) {
                                    new_masks = copy_masks(context, new_masks,
                                                           mask_count);
                                    copied_mask = JNI_TRUE;
                                }
                                SET_BIT(new_masks[k].modifies, i);
                            }
                        }
                    }
                }
            }
            break;
        }
    } /* of switch */

    if ((access != ACCESS_NONE) && (new_mask_count > 0)) {
        int i, j;
        for (i = 0; i < new_mask_count; i++) {
            int *mask = new_masks[i].modifies;
            if ((!IS_BIT_SET(mask, operand)) ||
                  ((access == ACCESS_DOUBLE) &&
                   !IS_BIT_SET(mask, operand + 1))) {
                new_masks = copy_masks(context, new_masks, mask_count);
                for (j = i; j < new_mask_count; j++) {
                    SET_BIT(new_masks[j].modifies, operand);
                    if (access == ACCESS_DOUBLE)
                        SET_BIT(new_masks[j].modifies, operand + 1);
                }
                break;
            }
        }
    }

    new_register_info->register_count = new_register_count;
    new_register_info->registers = new_registers;
    new_register_info->masks = new_masks;
    new_register_info->mask_count = new_mask_count;
}



/* We've already determined that the instruction is legal, and have updated
 * the registers.  Update the flags, too.
 */


static void
update_flags(context_type *context, unsigned int inumber,
             flag_type *new_and_flags, flag_type *new_or_flags)

{
    instruction_data_type *idata = context->instruction_data;
    instruction_data_type *this_idata = &idata[inumber];
    flag_type and_flags = this_idata->and_flags;
    flag_type or_flags = this_idata->or_flags;

    /* Set the "we've done a constructor" flag */
    if (this_idata->opcode == JVM_OPC_invokeinit) {
        fullinfo_type from = context->swap_table[0];
        if (from == MAKE_FULLINFO(ITEM_InitObject, 0, 0))
            and_flags |= FLAG_CONSTRUCTED;
    }
    *new_and_flags = and_flags;
    *new_or_flags = or_flags;
}



/* We've already determined that the instruction is legal.  Perform the
 * operation on the stack;
 *
 * new_stack_size_p and new_stack_p point to the results after the pops have
 * already been done.  Do the pushes, and then put the results back there.
 */

static void
push_stack(context_type *context, unsigned int inumber, stack_info_type *new_stack_info)
{
    instruction_data_type *idata = context->instruction_data;
    instruction_data_type *this_idata = &idata[inumber];
    int opcode = this_idata->opcode;
    int operand = this_idata->operand.i;

    int stack_size = new_stack_info->stack_size;
    stack_item_type *stack = new_stack_info->stack;
    char *stack_results;

    fullinfo_type full_info = 0;
    char buffer[5], *p;         /* actually [2] is big enough */

    /* We need to look at all those opcodes in which either we can't tell the
     * value pushed onto the stack from the opcode, or in which the value
     * pushed onto the stack is an object or array.  For the latter, we need
     * to make sure that full_info is set to the right value.
     */
    switch(opcode) {
        default:
            stack_results = opcode_in_out[opcode][1];
            break;

        case JVM_OPC_ldc: case JVM_OPC_ldc_w: case JVM_OPC_ldc2_w: {
            /* Look to constant pool to determine correct result. */
            unsigned char *type_table = context->constant_types;
            switch (type_table[operand]) {
                case JVM_CONSTANT_Integer:
                    stack_results = "I"; break;
                case JVM_CONSTANT_Float:
                    stack_results = "F"; break;
                case JVM_CONSTANT_Double:
                    stack_results = "D"; break;
                case JVM_CONSTANT_Long:
                    stack_results = "L"; break;
                case JVM_CONSTANT_String:
                    stack_results = "A";
                    full_info = context->string_info;
                    break;
                case JVM_CONSTANT_Class:
                    if (context->major_version < LDC_CLASS_MAJOR_VERSION)
                        CCerror(context, "Internal error #3");
                    stack_results = "A";
                    full_info = make_class_info_from_name(context,
                                                          "java/lang/Class");
                    break;
                case JVM_CONSTANT_MethodHandle:
                case JVM_CONSTANT_MethodType:
                    if (context->major_version < LDC_METHOD_HANDLE_MAJOR_VERSION)
                        CCerror(context, "Internal error #3");
                    stack_results = "A";
                    switch (type_table[operand]) {
                    case JVM_CONSTANT_MethodType:
                      full_info = make_class_info_from_name(context,
                                                            "java/lang/invoke/MethodType");
                      break;
                    default: //JVM_CONSTANT_MethodHandle
                      full_info = make_class_info_from_name(context,
                                                            "java/lang/invoke/MethodHandle");
                      break;
                    }
                    break;
                default:
                    CCerror(context, "Internal error #3");
                    stack_results = ""; /* Never reached: keep lint happy */
            }
            break;
        }

        case JVM_OPC_getstatic: case JVM_OPC_getfield: {
            /* Look to signature to determine correct result. */
            int operand = this_idata->operand.i;
            const char *signature = JVM_GetCPFieldSignatureUTF(context->env,
                                                               context->class,
                                                               operand);
            check_and_push(context, signature, VM_STRING_UTF);
#ifdef DEBUG
            if (verify_verbose) {
                print_formatted_fieldname(context, operand);
            }
#endif
            buffer[0] = signature_to_fieldtype(context, &signature, &full_info);
            buffer[1] = '\0';
            stack_results = buffer;
            pop_and_free(context);
            break;
        }

        case JVM_OPC_invokevirtual: case JVM_OPC_invokespecial:
        case JVM_OPC_invokeinit:
        case JVM_OPC_invokestatic: case JVM_OPC_invokeinterface: {
            /* Look to signature to determine correct result. */
            int operand = this_idata->operand.i;
            const char *signature = JVM_GetCPMethodSignatureUTF(context->env,
                                                                context->class,
                                                                operand);
            const char *result_signature;
            check_and_push(context, signature, VM_STRING_UTF);
            result_signature = get_result_signature(signature);
            if (result_signature++ == NULL) {
                CCerror(context, "Illegal signature %s", signature);
            }
            if (result_signature[0] == JVM_SIGNATURE_VOID) {
                stack_results = "";
            } else {
                buffer[0] = signature_to_fieldtype(context, &result_signature,
                                                   &full_info);
                buffer[1] = '\0';
                stack_results = buffer;
            }
            pop_and_free(context);
            break;
        }

        case JVM_OPC_aconst_null:
            stack_results = opcode_in_out[opcode][1];
            full_info = NULL_FULLINFO; /* special NULL */
            break;

        case JVM_OPC_new:
        case JVM_OPC_checkcast:
        case JVM_OPC_newarray:
        case JVM_OPC_anewarray:
        case JVM_OPC_multianewarray:
            stack_results = opcode_in_out[opcode][1];
            /* Conveniently, this result type is stored here */
            full_info = this_idata->operand.fi;
            break;

        case JVM_OPC_aaload:
            stack_results = opcode_in_out[opcode][1];
            /* pop_stack() saved value for us. */
            full_info = context->swap_table[0];
            break;

        case JVM_OPC_aload:
            stack_results = opcode_in_out[opcode][1];
            /* The register hasn't been modified, so we can use its value. */
            full_info = this_idata->register_info.registers[operand];
            break;
    } /* of switch */

    for (p = stack_results; *p != 0; p++) {
        int type = *p;
        stack_item_type *new_item = NEW(stack_item_type, 1);
        new_item->next = stack;
        stack = new_item;
        switch (type) {
            case 'I':
                stack->item = MAKE_FULLINFO(ITEM_Integer, 0, 0); break;
            case 'F':
                stack->item = MAKE_FULLINFO(ITEM_Float, 0, 0); break;
            case 'D':
                stack->item = MAKE_FULLINFO(ITEM_Double, 0, 0);
                stack_size++; break;
            case 'L':
                stack->item = MAKE_FULLINFO(ITEM_Long, 0, 0);
                stack_size++; break;
            case 'R':
                stack->item = MAKE_FULLINFO(ITEM_ReturnAddress, 0, operand);
                break;
            case '1': case '2': case '3': case '4': {
                /* Get the info saved in the swap_table */
                fullinfo_type stype = context->swap_table[type - '1'];
                stack->item = stype;
                if (stype == MAKE_FULLINFO(ITEM_Long, 0, 0) ||
                    stype == MAKE_FULLINFO(ITEM_Double, 0, 0)) {
                    stack_size++; p++;
                }
                break;
            }
            case 'A':
                /* full_info should have the appropriate value. */
                assert(full_info != 0);
                stack->item = full_info;
                break;
            default:
                CCerror(context, "Internal error #4");

            } /* switch type */
        stack_size++;
    } /* outer for loop */

    if (opcode == JVM_OPC_invokeinit) {
        /* If there are any instances of "from" on the stack, we need to
         * replace it with "to", since calling <init> initializes all versions
         * of the object, obviously.     */
        fullinfo_type from = context->swap_table[0];
        stack_item_type *ptr;
        for (ptr = stack; ptr != NULL; ptr = ptr->next) {
            if (ptr->item == from) {
                fullinfo_type to = context->swap_table[1];
                stack = copy_stack(context, stack);
                for (ptr = stack; ptr != NULL; ptr = ptr->next)
                    if (ptr->item == from) ptr->item = to;
                break;
            }
        }
    }

    new_stack_info->stack_size = stack_size;
    new_stack_info->stack = stack;
}


/* We've performed an instruction, and determined the new registers and stack
 * value.  Look at all of the possibly subsequent instructions, and merge
 * this stack value into theirs.
 */

static void
merge_into_successors(context_type *context, unsigned int inumber,
                      register_info_type *register_info,
                      stack_info_type *stack_info,
                      flag_type and_flags, flag_type or_flags)
{
    instruction_data_type *idata = context->instruction_data;
    instruction_data_type *this_idata = &idata[inumber];
    int opcode = this_idata->opcode;
    int operand = this_idata->operand.i;
    struct handler_info_type *handler_info = context->handler_info;
    int handler_info_length =
        JVM_GetMethodIxExceptionTableLength(context->env,
                                            context->class,
                                            context->method_index);


    int buffer[2];              /* default value for successors */
    int *successors = buffer;   /* table of successors */
    int successors_count;
    int i;

    switch (opcode) {
    default:
        successors_count = 1;
        buffer[0] = inumber + 1;
        break;

    case JVM_OPC_ifeq: case JVM_OPC_ifne: case JVM_OPC_ifgt:
    case JVM_OPC_ifge: case JVM_OPC_iflt: case JVM_OPC_ifle:
    case JVM_OPC_ifnull: case JVM_OPC_ifnonnull:
    case JVM_OPC_if_icmpeq: case JVM_OPC_if_icmpne: case JVM_OPC_if_icmpgt:
    case JVM_OPC_if_icmpge: case JVM_OPC_if_icmplt: case JVM_OPC_if_icmple:
    case JVM_OPC_if_acmpeq: case JVM_OPC_if_acmpne:
        successors_count = 2;
        buffer[0] = inumber + 1;
        buffer[1] = operand;
        break;

    case JVM_OPC_jsr: case JVM_OPC_jsr_w:
        if (this_idata->operand2.i != UNKNOWN_RET_INSTRUCTION)
            idata[this_idata->operand2.i].changed = JNI_TRUE;
        /* FALLTHROUGH */
    case JVM_OPC_goto: case JVM_OPC_goto_w:
        successors_count = 1;
        buffer[0] = operand;
        break;


    case JVM_OPC_ireturn: case JVM_OPC_lreturn: case JVM_OPC_return:
    case JVM_OPC_freturn: case JVM_OPC_dreturn: case JVM_OPC_areturn:
    case JVM_OPC_athrow:
        /* The testing for the returns is handled in pop_stack() */
        successors_count = 0;
        break;

    case JVM_OPC_ret: {
        /* This is slightly slow, but good enough for a seldom used instruction.
         * The EXTRA_ITEM_INFO of the ITEM_ReturnAddress indicates the
         * address of the first instruction of the subroutine.  We can return
         * to 1 after any instruction that jsr's to that instruction.
         */
        if (this_idata->operand2.ip == NULL) {
            fullinfo_type *registers = this_idata->register_info.registers;
            int called_instruction = GET_EXTRA_INFO(registers[operand]);
            int i, count, *ptr;;
            for (i = context->instruction_count, count = 0; --i >= 0; ) {
                if (((idata[i].opcode == JVM_OPC_jsr) ||
                     (idata[i].opcode == JVM_OPC_jsr_w)) &&
                    (idata[i].operand.i == called_instruction))
                    count++;
            }
            this_idata->operand2.ip = ptr = NEW(int, count + 1);
            *ptr++ = count;
            for (i = context->instruction_count, count = 0; --i >= 0; ) {
                if (((idata[i].opcode == JVM_OPC_jsr) ||
                     (idata[i].opcode == JVM_OPC_jsr_w)) &&
                    (idata[i].operand.i == called_instruction))
                    *ptr++ = i + 1;
            }
        }
        successors = this_idata->operand2.ip; /* use this instead */
        successors_count = *successors++;
        break;

    }

    case JVM_OPC_tableswitch:
    case JVM_OPC_lookupswitch:
        successors = this_idata->operand.ip; /* use this instead */
        successors_count = *successors++;
        break;
    }

#ifdef DEBUG
    if (verify_verbose) {
        jio_fprintf(stdout, " [");
        for (i = handler_info_length; --i >= 0; handler_info++)
            if (handler_info->start <= (int)inumber && handler_info->end > (int)inumber)
                jio_fprintf(stdout, "%d* ", handler_info->handler);
        for (i = 0; i < successors_count; i++)
            jio_fprintf(stdout, "%d ", successors[i]);
        jio_fprintf(stdout,   "]\n");
    }
#endif

    handler_info = context->handler_info;
    for (i = handler_info_length; --i >= 0; handler_info++) {
        if (handler_info->start <= (int)inumber && handler_info->end > (int)inumber) {
            int handler = handler_info->handler;
            if (opcode != JVM_OPC_invokeinit) {
                merge_into_one_successor(context, inumber, handler,
                                         &this_idata->register_info, /* old */
                                         &handler_info->stack_info,
                                         (flag_type) (and_flags
                                                      & this_idata->and_flags),
                                         (flag_type) (or_flags
                                                      | this_idata->or_flags),
                                         JNI_TRUE);
            } else {
                /* We need to be a little bit more careful with this
                 * instruction.  Things could either be in the state before
                 * the instruction or in the state afterwards */
                fullinfo_type from = context->swap_table[0];
                flag_type temp_or_flags = or_flags;
                if (from == MAKE_FULLINFO(ITEM_InitObject, 0, 0))
                    temp_or_flags |= FLAG_NO_RETURN;
                merge_into_one_successor(context, inumber, handler,
                                         &this_idata->register_info, /* old */
                                         &handler_info->stack_info,
                                         this_idata->and_flags,
                                         this_idata->or_flags,
                                         JNI_TRUE);
                merge_into_one_successor(context, inumber, handler,
                                         register_info,
                                         &handler_info->stack_info,
                                         and_flags, temp_or_flags, JNI_TRUE);
            }
        }
    }
    for (i = 0; i < successors_count; i++) {
        int target = successors[i];
        if (target >= context->instruction_count)
            CCerror(context, "Falling off the end of the code");
        merge_into_one_successor(context, inumber, target,
                                 register_info, stack_info, and_flags, or_flags,
                                 JNI_FALSE);
    }
}

/* We have a new set of registers and stack values for a given instruction.
 * Merge this new set into the values that are already there.
 */

static void
merge_into_one_successor(context_type *context,
                         unsigned int from_inumber, unsigned int to_inumber,
                         register_info_type *new_register_info,
                         stack_info_type *new_stack_info,
                         flag_type new_and_flags, flag_type new_or_flags,
                         jboolean isException)
{
    instruction_data_type *idata = context->instruction_data;
    register_info_type register_info_buf;
    stack_info_type stack_info_buf;
#ifdef DEBUG
    instruction_data_type *this_idata = &idata[to_inumber];
    register_info_type old_reg_info;
    stack_info_type old_stack_info;
    flag_type old_and_flags = 0;
    flag_type old_or_flags = 0;
#endif

#ifdef DEBUG
    if (verify_verbose) {
        old_reg_info = this_idata->register_info;
        old_stack_info = this_idata->stack_info;
        old_and_flags = this_idata->and_flags;
        old_or_flags = this_idata->or_flags;
    }
#endif

    /* All uninitialized objects are set to "bogus" when jsr and
     * ret are executed. Thus uninitialized objects can't propagate
     * into or out of a subroutine.
     */
    if (idata[from_inumber].opcode == JVM_OPC_ret ||
        idata[from_inumber].opcode == JVM_OPC_jsr ||
        idata[from_inumber].opcode == JVM_OPC_jsr_w) {
        int new_register_count = new_register_info->register_count;
        fullinfo_type *new_registers = new_register_info->registers;
        int i;
        stack_item_type *item;

        for (item = new_stack_info->stack; item != NULL; item = item->next) {
            if (GET_ITEM_TYPE(item->item) == ITEM_NewObject) {
                /* This check only succeeds for hand-contrived code.
                 * Efficiency is not an issue.
                 */
                stack_info_buf.stack = copy_stack(context,
                                                  new_stack_info->stack);
                stack_info_buf.stack_size = new_stack_info->stack_size;
                new_stack_info = &stack_info_buf;
                for (item = new_stack_info->stack; item != NULL;
                     item = item->next) {
                    if (GET_ITEM_TYPE(item->item) == ITEM_NewObject) {
                        item->item = MAKE_FULLINFO(ITEM_Bogus, 0, 0);
                    }
                }
                break;
            }
        }
        for (i = 0; i < new_register_count; i++) {
            if (GET_ITEM_TYPE(new_registers[i]) == ITEM_NewObject) {
                /* This check only succeeds for hand-contrived code.
                 * Efficiency is not an issue.
                 */
                fullinfo_type *new_set = NEW(fullinfo_type,
                                             new_register_count);
                for (i = 0; i < new_register_count; i++) {
                    fullinfo_type t = new_registers[i];
                    new_set[i] = GET_ITEM_TYPE(t) != ITEM_NewObject ?
                        t : MAKE_FULLINFO(ITEM_Bogus, 0, 0);
                }
                register_info_buf.register_count = new_register_count;
                register_info_buf.registers = new_set;
                register_info_buf.mask_count = new_register_info->mask_count;
                register_info_buf.masks = new_register_info->masks;
                new_register_info = &register_info_buf;
                break;
            }
        }
    }

    /* Returning from a subroutine is somewhat ugly.  The actual thing
     * that needs to get merged into the new instruction is a joining
     * of info from the ret instruction with stuff in the jsr instruction
     */
    if (idata[from_inumber].opcode == JVM_OPC_ret && !isException) {
        int new_register_count = new_register_info->register_count;
        fullinfo_type *new_registers = new_register_info->registers;
        int new_mask_count = new_register_info->mask_count;
        mask_type *new_masks = new_register_info->masks;
        int operand = idata[from_inumber].operand.i;
        int called_instruction = GET_EXTRA_INFO(new_registers[operand]);
        instruction_data_type *jsr_idata = &idata[to_inumber - 1];
        register_info_type *jsr_reginfo = &jsr_idata->register_info;
        if (jsr_idata->operand2.i != (int)from_inumber) {
            if (jsr_idata->operand2.i != UNKNOWN_RET_INSTRUCTION)
                CCerror(context, "Multiple returns to single jsr");
            jsr_idata->operand2.i = from_inumber;
        }
        if (jsr_reginfo->register_count == UNKNOWN_REGISTER_COUNT) {
            /* We don't want to handle the returned-to instruction until
             * we've dealt with the jsr instruction.   When we get to the
             * jsr instruction (if ever), we'll re-mark the ret instruction
             */
            ;
        } else {
            int register_count = jsr_reginfo->register_count;
            fullinfo_type *registers = jsr_reginfo->registers;
            int max_registers = MAX(register_count, new_register_count);
            fullinfo_type *new_set = NEW(fullinfo_type, max_registers);
            int *return_mask;
            struct register_info_type new_new_register_info;
            int i;
            /* Make sure the place we're returning from is legal! */
            for (i = new_mask_count; --i >= 0; )
                if (new_masks[i].entry == called_instruction)
                    break;
            if (i < 0)
                CCerror(context, "Illegal return from subroutine");
            /* pop the masks down to the indicated one.  Remember the mask
             * we're popping off. */
            return_mask = new_masks[i].modifies;
            new_mask_count = i;
            for (i = 0; i < max_registers; i++) {
                if (IS_BIT_SET(return_mask, i))
                    new_set[i] = i < new_register_count ?
                          new_registers[i] : MAKE_FULLINFO(ITEM_Bogus, 0, 0);
                else
                    new_set[i] = i < register_count ?
                        registers[i] : MAKE_FULLINFO(ITEM_Bogus, 0, 0);
            }
            new_new_register_info.register_count = max_registers;
            new_new_register_info.registers      = new_set;
            new_new_register_info.mask_count     = new_mask_count;
            new_new_register_info.masks          = new_masks;


            merge_stack(context, from_inumber, to_inumber, new_stack_info);
            merge_registers(context, to_inumber - 1, to_inumber,
                            &new_new_register_info);
            merge_flags(context, from_inumber, to_inumber, new_and_flags, new_or_flags);
        }
    } else {
        merge_stack(context, from_inumber, to_inumber, new_stack_info);
        merge_registers(context, from_inumber, to_inumber, new_register_info);
        merge_flags(context, from_inumber, to_inumber,
                    new_and_flags, new_or_flags);
    }

#ifdef DEBUG
    if (verify_verbose && idata[to_inumber].changed) {
        register_info_type *register_info = &this_idata->register_info;
        stack_info_type *stack_info = &this_idata->stack_info;
        if (memcmp(&old_reg_info, register_info, sizeof(old_reg_info)) ||
            memcmp(&old_stack_info, stack_info, sizeof(old_stack_info)) ||
            (old_and_flags != this_idata->and_flags) ||
            (old_or_flags != this_idata->or_flags)) {
            jio_fprintf(stdout, "   %2d:", to_inumber);
            print_stack(context, &old_stack_info);
            print_registers(context, &old_reg_info);
            print_flags(context, old_and_flags, old_or_flags);
            jio_fprintf(stdout, " => ");
            print_stack(context, &this_idata->stack_info);
            print_registers(context, &this_idata->register_info);
            print_flags(context, this_idata->and_flags, this_idata->or_flags);
            jio_fprintf(stdout, "\n");
        }
    }
#endif

}

static void
merge_stack(context_type *context, unsigned int from_inumber,
            unsigned int to_inumber, stack_info_type *new_stack_info)
{
    instruction_data_type *idata = context->instruction_data;
    instruction_data_type *this_idata = &idata[to_inumber];

    int new_stack_size =  new_stack_info->stack_size;
    stack_item_type *new_stack = new_stack_info->stack;

    int stack_size = this_idata->stack_info.stack_size;

    if (stack_size == UNKNOWN_STACK_SIZE) {
        /* First time at this instruction.  Just copy. */
        this_idata->stack_info.stack_size = new_stack_size;
        this_idata->stack_info.stack = new_stack;
        this_idata->changed = JNI_TRUE;
    } else if (new_stack_size != stack_size) {
        CCerror(context, "Inconsistent stack height %d != %d",
                new_stack_size, stack_size);
    } else {
        stack_item_type *stack = this_idata->stack_info.stack;
        stack_item_type *old, *new;
        jboolean change = JNI_FALSE;
        for (old = stack, new = new_stack; old != NULL;
                   old = old->next, new = new->next) {
            assert(new != NULL);
            if (!isAssignableTo(context, new->item, old->item)) {
                change = JNI_TRUE;
                break;
            }
        }
        if (change) {
            stack = copy_stack(context, stack);
            for (old = stack, new = new_stack; old != NULL;
                          old = old->next, new = new->next) {
                if (new == NULL) {
                    break;
                }
                old->item = merge_fullinfo_types(context, old->item, new->item,
                                                 JNI_FALSE);
                if (GET_ITEM_TYPE(old->item) == ITEM_Bogus) {
                        CCerror(context, "Mismatched stack types");
                }
            }
            if (old != NULL || new != NULL) {
                CCerror(context, "Mismatched stack types");
            }
            this_idata->stack_info.stack = stack;
            this_idata->changed = JNI_TRUE;
        }
    }
}

static void
merge_registers(context_type *context, unsigned int from_inumber,
                unsigned int to_inumber, register_info_type *new_register_info)
{
    instruction_data_type *idata = context->instruction_data;
    instruction_data_type *this_idata = &idata[to_inumber];
    register_info_type    *this_reginfo = &this_idata->register_info;

    int            new_register_count = new_register_info->register_count;
    fullinfo_type *new_registers = new_register_info->registers;
    int            new_mask_count = new_register_info->mask_count;
    mask_type     *new_masks = new_register_info->masks;


    if (this_reginfo->register_count == UNKNOWN_REGISTER_COUNT) {
        this_reginfo->register_count = new_register_count;
        this_reginfo->registers = new_registers;
        this_reginfo->mask_count = new_mask_count;
        this_reginfo->masks = new_masks;
        this_idata->changed = JNI_TRUE;
    } else {
        /* See if we've got new information on the register set. */
        int register_count = this_reginfo->register_count;
        fullinfo_type *registers = this_reginfo->registers;
        int mask_count = this_reginfo->mask_count;
        mask_type *masks = this_reginfo->masks;

        jboolean copy = JNI_FALSE;
        int i, j;
        if (register_count > new_register_count) {
            /* Any register larger than new_register_count is now bogus */
            this_reginfo->register_count = new_register_count;
            register_count = new_register_count;
            this_idata->changed = JNI_TRUE;
        }
        for (i = 0; i < register_count; i++) {
            fullinfo_type prev_value = registers[i];
            if ((i < new_register_count)
                  ? (!isAssignableTo(context, new_registers[i], prev_value))
                  : (prev_value != MAKE_FULLINFO(ITEM_Bogus, 0, 0))) {
                copy = JNI_TRUE;
                break;
            }
        }

        if (copy) {
            /* We need a copy.  So do it. */
            fullinfo_type *new_set = NEW(fullinfo_type, register_count);
            for (j = 0; j < i; j++)
                new_set[j] =  registers[j];
            for (j = i; j < register_count; j++) {
                if (i >= new_register_count)
                    new_set[j] = MAKE_FULLINFO(ITEM_Bogus, 0, 0);
                else
                    new_set[j] = merge_fullinfo_types(context,
                                                      new_registers[j],
                                                      registers[j], JNI_FALSE);
            }
            /* Some of the end items might now be bogus. This step isn't
             * necessary, but it may save work later. */
            while (   register_count > 0
                   && GET_ITEM_TYPE(new_set[register_count-1]) == ITEM_Bogus)
                register_count--;
            this_reginfo->register_count = register_count;
            this_reginfo->registers = new_set;
            this_idata->changed = JNI_TRUE;
        }
        if (mask_count > 0) {
            /* If the target instruction already has a sequence of masks, then
             * we need to merge new_masks into it.  We want the entries on
             * the mask to be the longest common substring of the two.
             *   (e.g.   a->b->d merged with a->c->d should give a->d)
             * The bits set in the mask should be the or of the corresponding
             * entries in each of the original masks.
             */
            int i, j, k;
            int matches = 0;
            int last_match = -1;
            jboolean copy_needed = JNI_FALSE;
            for (i = 0; i < mask_count; i++) {
                int entry = masks[i].entry;
                for (j = last_match + 1; j < new_mask_count; j++) {
                    if (new_masks[j].entry == entry) {
                        /* We have a match */
                        int *prev = masks[i].modifies;
                        int *new = new_masks[j].modifies;
                        matches++;
                        /* See if new_mask has bits set for "entry" that
                         * weren't set for mask.  If so, need to copy. */
                        for (k = context->bitmask_size - 1;
                               !copy_needed && k >= 0;
                               k--)
                            if (~prev[k] & new[k])
                                copy_needed = JNI_TRUE;
                        last_match = j;
                        break;
                    }
                }
            }
            if ((matches < mask_count) || copy_needed) {
                /* We need to make a copy for the new item, since either the
                 * size has decreased, or new bits are set. */
                mask_type *copy = NEW(mask_type, matches);
                for (i = 0; i < matches; i++) {
                    copy[i].modifies = NEW(int, context->bitmask_size);
                }
                this_reginfo->masks = copy;
                this_reginfo->mask_count = matches;
                this_idata->changed = JNI_TRUE;
                matches = 0;
                last_match = -1;
                for (i = 0; i < mask_count; i++) {
                    int entry = masks[i].entry;
                    for (j = last_match + 1; j < new_mask_count; j++) {
                        if (new_masks[j].entry == entry) {
                            int *prev1 = masks[i].modifies;
                            int *prev2 = new_masks[j].modifies;
                            int *new = copy[matches].modifies;
                            copy[matches].entry = entry;
                            for (k = context->bitmask_size - 1; k >= 0; k--)
                                new[k] = prev1[k] | prev2[k];
                            matches++;
                            last_match = j;
                            break;
                        }
                    }
                }
            }
        }
    }
}


static void
merge_flags(context_type *context, unsigned int from_inumber,
            unsigned int to_inumber,
            flag_type new_and_flags, flag_type new_or_flags)
{
    /* Set this_idata->and_flags &= new_and_flags
           this_idata->or_flags |= new_or_flags
     */
    instruction_data_type *idata = context->instruction_data;
    instruction_data_type *this_idata = &idata[to_inumber];
    flag_type this_and_flags = this_idata->and_flags;
    flag_type this_or_flags = this_idata->or_flags;
    flag_type merged_and = this_and_flags & new_and_flags;
    flag_type merged_or = this_or_flags | new_or_flags;

    if ((merged_and != this_and_flags) || (merged_or != this_or_flags)) {
        this_idata->and_flags = merged_and;
        this_idata->or_flags = merged_or;
        this_idata->changed = JNI_TRUE;
    }
}


/* Make a copy of a stack */

static stack_item_type *
copy_stack(context_type *context, stack_item_type *stack)
{
    int length;
    stack_item_type *ptr;

    /* Find the length */
    for (ptr = stack, length = 0; ptr != NULL; ptr = ptr->next, length++);

    if (length > 0) {
        stack_item_type *new_stack = NEW(stack_item_type, length);
        stack_item_type *new_ptr;
        for (    ptr = stack, new_ptr = new_stack;
                 ptr != NULL;
                 ptr = ptr->next, new_ptr++) {
            new_ptr->item = ptr->item;
            new_ptr->next = new_ptr + 1;
        }
        new_stack[length - 1].next = NULL;
        return new_stack;
    } else {
        return NULL;
    }
}


static mask_type *
copy_masks(context_type *context, mask_type *masks, int mask_count)
{
    mask_type *result = NEW(mask_type, mask_count);
    int bitmask_size = context->bitmask_size;
    int *bitmaps = NEW(int, mask_count * bitmask_size);
    int i;
    for (i = 0; i < mask_count; i++) {
        result[i].entry = masks[i].entry;
        result[i].modifies = &bitmaps[i * bitmask_size];
        memcpy(result[i].modifies, masks[i].modifies, bitmask_size * sizeof(int));
    }
    return result;
}


static mask_type *
add_to_masks(context_type *context, mask_type *masks, int mask_count, int d)
{
    mask_type *result = NEW(mask_type, mask_count + 1);
    int bitmask_size = context->bitmask_size;
    int *bitmaps = NEW(int, (mask_count + 1) * bitmask_size);
    int i;
    for (i = 0; i < mask_count; i++) {
        result[i].entry = masks[i].entry;
        result[i].modifies = &bitmaps[i * bitmask_size];
        memcpy(result[i].modifies, masks[i].modifies, bitmask_size * sizeof(int));
    }
    result[mask_count].entry = d;
    result[mask_count].modifies = &bitmaps[mask_count * bitmask_size];
    memset(result[mask_count].modifies, 0, bitmask_size * sizeof(int));
    return result;
}



/* We create our own storage manager, since we malloc lots of little items,
 * and I don't want to keep trace of when they become free.  I sure wish that
 * we had heaps, and I could just free the heap when done.
 */

#define CCSegSize 2000

struct CCpool {                 /* a segment of allocated memory in the pool */
    struct CCpool *next;
    int segSize;                /* almost always CCSegSize */
    int poolPad;
    char space[CCSegSize];
};

/* Initialize the context's heap. */
static void CCinit(context_type *context)
{
    struct CCpool *new = (struct CCpool *) malloc(sizeof(struct CCpool));
    /* Set context->CCroot to 0 if new == 0 to tell CCdestroy to lay off */
    context->CCroot = context->CCcurrent = new;
    if (new == 0) {
        CCout_of_memory(context);
    }
    new->next = NULL;
    new->segSize = CCSegSize;
    context->CCfree_size = CCSegSize;
    context->CCfree_ptr = &new->space[0];
}


/* Reuse all the space that we have in the context's heap. */
static void CCreinit(context_type *context)
{
    struct CCpool *first = context->CCroot;
    context->CCcurrent = first;
    context->CCfree_size = CCSegSize;
    context->CCfree_ptr = &first->space[0];
}

/* Destroy the context's heap. */
static void CCdestroy(context_type *context)
{
    struct CCpool *this = context->CCroot;
    while (this) {
        struct CCpool *next = this->next;
        free(this);
        this = next;
    }
    /* These two aren't necessary.  But can't hurt either */
    context->CCroot = context->CCcurrent = NULL;
    context->CCfree_ptr = 0;
}

/* Allocate an object of the given size from the context's heap. */
static void *
CCalloc(context_type *context, int size, jboolean zero)
{

    register char *p;
    /* Round CC to the size of a pointer */
    size = (size + (sizeof(void *) - 1)) & ~(sizeof(void *) - 1);

    if (context->CCfree_size <  size) {
        struct CCpool *current = context->CCcurrent;
        struct CCpool *new;
        if (size > CCSegSize) { /* we need to allocate a special block */
            new = (struct CCpool *)malloc(sizeof(struct CCpool) +
                                          (size - CCSegSize));
            if (new == 0) {
                CCout_of_memory(context);
            }
            new->next = current->next;
            new->segSize = size;
            current->next = new;
        } else {
            new = current->next;
            if (new == NULL) {
                new = (struct CCpool *) malloc(sizeof(struct CCpool));
                if (new == 0) {
                    CCout_of_memory(context);
                }
                current->next = new;
                new->next = NULL;
                new->segSize = CCSegSize;
            }
        }
        context->CCcurrent = new;
        context->CCfree_ptr = &new->space[0];
        context->CCfree_size = new->segSize;
    }
    p = context->CCfree_ptr;
    context->CCfree_ptr += size;
    context->CCfree_size -= size;
    if (zero)
        memset(p, 0, size);
    return p;
}

/* Get the class associated with a particular field or method or class in the
 * constant pool.  If is_field is true, we've got a field or method.  If
 * false, we've got a class.
 */
static fullinfo_type
cp_index_to_class_fullinfo(context_type *context, int cp_index, int kind)
{
    JNIEnv *env = context->env;
    fullinfo_type result;
    const char *classname;
    switch (kind) {
    case JVM_CONSTANT_Class:
        classname = JVM_GetCPClassNameUTF(env,
                                          context->class,
                                          cp_index);
        break;
    case JVM_CONSTANT_Methodref:
        classname = JVM_GetCPMethodClassNameUTF(env,
                                                context->class,
                                                cp_index);
        break;
    case JVM_CONSTANT_Fieldref:
        classname = JVM_GetCPFieldClassNameUTF(env,
                                               context->class,
                                               cp_index);
        break;
    default:
        classname = NULL;
        CCerror(context, "Internal error #5");
    }

    check_and_push(context, classname, VM_STRING_UTF);
    if (classname[0] == JVM_SIGNATURE_ARRAY) {
        /* This make recursively call us, in case of a class array */
        signature_to_fieldtype(context, &classname, &result);
    } else {
        result = make_class_info_from_name(context, classname);
    }
    pop_and_free(context);
    return result;
}


static int
print_CCerror_info(context_type *context)
{
    JNIEnv *env = context->env;
    jclass cb = context->class;
    const char *classname = JVM_GetClassNameUTF(env, cb);
    const char *name = 0;
    const char *signature = 0;
    int n = 0;
    if (context->method_index != -1) {
        name = JVM_GetMethodIxNameUTF(env, cb, context->method_index);
        signature =
            JVM_GetMethodIxSignatureUTF(env, cb, context->method_index);
        n += jio_snprintf(context->message, context->message_buf_len,
                          "(class: %s, method: %s signature: %s) ",
                          (classname ? classname : ""),
                          (name ? name : ""),
                          (signature ? signature : ""));
    } else if (context->field_index != -1 ) {
        name = JVM_GetMethodIxNameUTF(env, cb, context->field_index);
        n += jio_snprintf(context->message, context->message_buf_len,
                          "(class: %s, field: %s) ",
                          (classname ? classname : 0),
                          (name ? name : 0));
    } else {
        n += jio_snprintf(context->message, context->message_buf_len,
                          "(class: %s) ", classname ? classname : "");
    }
    JVM_ReleaseUTF(classname);
    JVM_ReleaseUTF(name);
    JVM_ReleaseUTF(signature);
    return n;
}

static void
CCerror (context_type *context, char *format, ...)
{
    int n = print_CCerror_info(context);
    va_list args;
    if (n >= 0 && n < context->message_buf_len) {
        va_start(args, format);
        jio_vsnprintf(context->message + n, context->message_buf_len - n,
                      format, args);
        va_end(args);
    }
    context->err_code = CC_VerifyError;
    longjmp(context->jump_buffer, 1);
}

static void
CCout_of_memory(context_type *context)
{
    int n = print_CCerror_info(context);
    context->err_code = CC_OutOfMemory;
    longjmp(context->jump_buffer, 1);
}

static void
CFerror(context_type *context, char *format, ...)
{
    int n = print_CCerror_info(context);
    va_list args;
    if (n >= 0 && n < context->message_buf_len) {
        va_start(args, format);
        jio_vsnprintf(context->message + n, context->message_buf_len - n,
                      format, args);
        va_end(args);
    }
    context->err_code = CC_ClassFormatError;
    longjmp(context->jump_buffer, 1);
}

/*
 * Need to scan the entire signature to find the result type because
 * types in the arg list and the result type could contain embedded ')'s.
 */
static const char* get_result_signature(const char* signature) {
    const char *p;
    for (p = signature; *p != JVM_SIGNATURE_ENDFUNC; p++) {
        switch (*p) {
          case JVM_SIGNATURE_BOOLEAN:
          case JVM_SIGNATURE_BYTE:
          case JVM_SIGNATURE_CHAR:
          case JVM_SIGNATURE_SHORT:
          case JVM_SIGNATURE_INT:
          case JVM_SIGNATURE_FLOAT:
          case JVM_SIGNATURE_DOUBLE:
          case JVM_SIGNATURE_LONG:
          case JVM_SIGNATURE_FUNC:  /* ignore initial (, if given */
            break;
          case JVM_SIGNATURE_CLASS:
            while (*p != JVM_SIGNATURE_ENDCLASS) p++;
            break;
          case JVM_SIGNATURE_ARRAY:
            while (*p == JVM_SIGNATURE_ARRAY) p++;
            /* If an array of classes, skip over class name, too. */
            if (*p == JVM_SIGNATURE_CLASS) {
                while (*p != JVM_SIGNATURE_ENDCLASS) p++;
            }
            break;
          default:
            /* Indicate an error. */
            return NULL;
        }
    }
    return p++; /* skip over ')'. */
}

static char
signature_to_fieldtype(context_type *context,
                       const char **signature_p, fullinfo_type *full_info_p)
{
    const char *p = *signature_p;
    fullinfo_type full_info = MAKE_FULLINFO(ITEM_Bogus, 0, 0);
    char result;
    int array_depth = 0;

    for (;;) {
        switch(*p++) {
            default:
                result = 0;
                break;

            case JVM_SIGNATURE_BOOLEAN:
                full_info = (array_depth > 0)
                              ? MAKE_FULLINFO(ITEM_Boolean, 0, 0)
                              : MAKE_FULLINFO(ITEM_Integer, 0, 0);
                result = 'I';
                break;

            case JVM_SIGNATURE_BYTE:
                full_info = (array_depth > 0)
                              ? MAKE_FULLINFO(ITEM_Byte, 0, 0)
                              : MAKE_FULLINFO(ITEM_Integer, 0, 0);
                result = 'I';
                break;

            case JVM_SIGNATURE_CHAR:
                full_info = (array_depth > 0)
                              ? MAKE_FULLINFO(ITEM_Char, 0, 0)
                              : MAKE_FULLINFO(ITEM_Integer, 0, 0);
                result = 'I';
                break;

            case JVM_SIGNATURE_SHORT:
                full_info = (array_depth > 0)
                              ? MAKE_FULLINFO(ITEM_Short, 0, 0)
                              : MAKE_FULLINFO(ITEM_Integer, 0, 0);
                result = 'I';
                break;

            case JVM_SIGNATURE_INT:
                full_info = MAKE_FULLINFO(ITEM_Integer, 0, 0);
                result = 'I';
                break;

            case JVM_SIGNATURE_FLOAT:
                full_info = MAKE_FULLINFO(ITEM_Float, 0, 0);
                result = 'F';
                break;

            case JVM_SIGNATURE_DOUBLE:
                full_info = MAKE_FULLINFO(ITEM_Double, 0, 0);
                result = 'D';
                break;

            case JVM_SIGNATURE_LONG:
                full_info = MAKE_FULLINFO(ITEM_Long, 0, 0);
                result = 'L';
                break;

            case JVM_SIGNATURE_ARRAY:
                array_depth++;
                continue;       /* only time we ever do the loop > 1 */

            case JVM_SIGNATURE_CLASS: {
                char buffer_space[256];
                char *buffer = buffer_space;
                char *finish = strchr(p, JVM_SIGNATURE_ENDCLASS);
                int length;
                if (finish == NULL) {
                    /* Signature must have ';' after the class name.
                     * If it does not, return 0 and ITEM_Bogus in full_info. */
                    result = 0;
                    break;
                }
                assert(finish >= p);
                length = (int)(finish - p);
                if (length + 1 > (int)sizeof(buffer_space)) {
                    buffer = calloc(length + 1, sizeof(char));
                    check_and_push(context, buffer, VM_MALLOC_BLK);
                }
                memcpy(buffer, p, length);
                buffer[length] = '\0';
                full_info = make_class_info_from_name(context, buffer);
                result = 'A';
                p = finish + 1;
                if (buffer != buffer_space)
                    pop_and_free(context);
                break;
            }
        } /* end of switch */
        break;
    }
    *signature_p = p;
    if (array_depth == 0 || result == 0) {
        /* either not an array, or result is bogus */
        *full_info_p = full_info;
        return result;
    } else {
        if (array_depth > MAX_ARRAY_DIMENSIONS)
            CCerror(context, "Array with too many dimensions");
        *full_info_p = MAKE_FULLINFO(GET_ITEM_TYPE(full_info),
                                     array_depth,
                                     GET_EXTRA_INFO(full_info));
        return 'A';
    }
}


/* Given an array type, create the type that has one less level of
 * indirection.
 */

static fullinfo_type
decrement_indirection(fullinfo_type array_info)
{
    if (array_info == NULL_FULLINFO) {
        return NULL_FULLINFO;
    } else {
        int type = GET_ITEM_TYPE(array_info);
        int indirection = GET_INDIRECTION(array_info) - 1;
        int extra_info = GET_EXTRA_INFO(array_info);
        if (   (indirection == 0)
               && ((type == ITEM_Short || type == ITEM_Byte || type == ITEM_Boolean || type == ITEM_Char)))
            type = ITEM_Integer;
        return MAKE_FULLINFO(type, indirection, extra_info);
    }
}


/* See if we can assign an object of the "from" type to an object
 * of the "to" type.
 */

static jboolean isAssignableTo(context_type *context,
                             fullinfo_type from, fullinfo_type to)
{
    return (merge_fullinfo_types(context, from, to, JNI_TRUE) == to);
}

/* Given two fullinfo_type's, find their lowest common denominator.  If
 * the assignable_p argument is non-null, we're really just calling to find
 * out if "<target> := <value>" is a legitimate assignment.
 *
 * We treat all interfaces as if they were of type java/lang/Object, since the
 * runtime will do the full checking.
 */
static fullinfo_type
merge_fullinfo_types(context_type *context,
                     fullinfo_type value, fullinfo_type target,
                     jboolean for_assignment)
{
    JNIEnv *env = context->env;
    if (value == target) {
        /* If they're identical, clearly just return what we've got */
        return value;
    }

    /* Both must be either arrays or objects to go further */
    if (GET_INDIRECTION(value) == 0 && GET_ITEM_TYPE(value) != ITEM_Object)
        return MAKE_FULLINFO(ITEM_Bogus, 0, 0);
    if (GET_INDIRECTION(target) == 0 && GET_ITEM_TYPE(target) != ITEM_Object)
        return MAKE_FULLINFO(ITEM_Bogus, 0, 0);

    /* If either is NULL, return the other. */
    if (value == NULL_FULLINFO)
        return target;
    else if (target == NULL_FULLINFO)
        return value;

    /* If either is java/lang/Object, that's the result. */
    if (target == context->object_info)
        return target;
    else if (value == context->object_info) {
        /* Minor hack.  For assignments, Interface := Object, return Interface
         * rather than Object, so that isAssignableTo() will get the right
         * result.      */
        if (for_assignment && (WITH_ZERO_EXTRA_INFO(target) ==
                                  MAKE_FULLINFO(ITEM_Object, 0, 0))) {
            jclass cb = object_fullinfo_to_classclass(context,
                                                      target);
            int is_interface = cb && JVM_IsInterface(env, cb);
            if (is_interface)
                return target;
        }
        return value;
    }
    if (GET_INDIRECTION(value) > 0 || GET_INDIRECTION(target) > 0) {
        /* At least one is an array.  Neither is java/lang/Object or NULL.
         * Moreover, the types are not identical.
         * The result must either be Object, or an array of some object type.
         */
        fullinfo_type value_base, target_base;
        int dimen_value = GET_INDIRECTION(value);
        int dimen_target = GET_INDIRECTION(target);

        if (target == context->cloneable_info ||
            target == context->serializable_info) {
            return target;
        }

        if (value == context->cloneable_info ||
            value == context->serializable_info) {
            return value;
        }

        /* First, if either item's base type isn't ITEM_Object, promote it up
         * to an object or array of object.  If either is elemental, we can
         * punt.
         */
        if (GET_ITEM_TYPE(value) != ITEM_Object) {
            if (dimen_value == 0)
                return MAKE_FULLINFO(ITEM_Bogus, 0, 0);
            dimen_value--;
            value = MAKE_Object_ARRAY(dimen_value);

        }
        if (GET_ITEM_TYPE(target) != ITEM_Object) {
            if (dimen_target == 0)
                return MAKE_FULLINFO(ITEM_Bogus, 0, 0);
            dimen_target--;
            target = MAKE_Object_ARRAY(dimen_target);
        }
        /* Both are now objects or arrays of some sort of object type */
        value_base = WITH_ZERO_INDIRECTION(value);
        target_base = WITH_ZERO_INDIRECTION(target);
        if (dimen_value == dimen_target) {
            /* Arrays of the same dimension.  Merge their base types. */
            fullinfo_type  result_base =
                merge_fullinfo_types(context, value_base, target_base,
                                            for_assignment);
            if (result_base == MAKE_FULLINFO(ITEM_Bogus, 0, 0))
                /* bogus in, bogus out */
                return result_base;
            return MAKE_FULLINFO(ITEM_Object, dimen_value,
                                 GET_EXTRA_INFO(result_base));
        } else {
            /* Arrays of different sizes. If the smaller dimension array's base
             * type is java/lang/Cloneable or java/io/Serializable, return it.
             * Otherwise return java/lang/Object with a dimension of the smaller
             * of the two */
            if (dimen_value < dimen_target) {
                if (value_base == context->cloneable_info ||
                    value_base == context ->serializable_info) {
                    return value;
                }
                return MAKE_Object_ARRAY(dimen_value);
            } else {
                if (target_base == context->cloneable_info ||
                    target_base == context->serializable_info) {
                    return target;
                }
                return MAKE_Object_ARRAY(dimen_target);
            }
        }
    } else {
        /* Both are non-array objects. Neither is java/lang/Object or NULL */
        jclass cb_value, cb_target, cb_super_value, cb_super_target;
        fullinfo_type result_info;

        /* Let's get the classes corresponding to each of these.  Treat
         * interfaces as if they were java/lang/Object.  See hack note above. */
        cb_target = object_fullinfo_to_classclass(context, target);
        if (cb_target == 0)
            return MAKE_FULLINFO(ITEM_Bogus, 0, 0);
        if (JVM_IsInterface(env, cb_target))
            return for_assignment ? target : context->object_info;
        cb_value = object_fullinfo_to_classclass(context, value);
        if (cb_value == 0)
            return MAKE_FULLINFO(ITEM_Bogus, 0, 0);
        if (JVM_IsInterface(env, cb_value))
            return context->object_info;

        /* If this is for assignment of target := value, we just need to see if
         * cb_target is a superclass of cb_value.  Save ourselves a lot of
         * work.
         */
        if (for_assignment) {
            cb_super_value = (*env)->GetSuperclass(env, cb_value);
            while (cb_super_value != 0) {
                jclass tmp_cb;
                if ((*env)->IsSameObject(env, cb_super_value, cb_target)) {
                    (*env)->DeleteLocalRef(env, cb_super_value);
                    return target;
                }
                tmp_cb =  (*env)->GetSuperclass(env, cb_super_value);
                (*env)->DeleteLocalRef(env, cb_super_value);
                cb_super_value = tmp_cb;
            }
            (*env)->DeleteLocalRef(env, cb_super_value);
            return context->object_info;
        }

        /* Find out whether cb_value or cb_target is deeper in the class
         * tree by moving both toward the root, and seeing who gets there
         * first.                                                          */
        cb_super_value = (*env)->GetSuperclass(env, cb_value);
        cb_super_target = (*env)->GetSuperclass(env, cb_target);
        while((cb_super_value != 0) &&
              (cb_super_target != 0)) {
            jclass tmp_cb;
            /* Optimization.  If either hits the other when going up looking
             * for a parent, then might as well return the parent immediately */
            if ((*env)->IsSameObject(env, cb_super_value, cb_target)) {
                (*env)->DeleteLocalRef(env, cb_super_value);
                (*env)->DeleteLocalRef(env, cb_super_target);
                return target;
            }
            if ((*env)->IsSameObject(env, cb_super_target, cb_value)) {
                (*env)->DeleteLocalRef(env, cb_super_value);
                (*env)->DeleteLocalRef(env, cb_super_target);
                return value;
            }
            tmp_cb = (*env)->GetSuperclass(env, cb_super_value);
            (*env)->DeleteLocalRef(env, cb_super_value);
            cb_super_value = tmp_cb;

            tmp_cb = (*env)->GetSuperclass(env, cb_super_target);
            (*env)->DeleteLocalRef(env, cb_super_target);
            cb_super_target = tmp_cb;
        }
        cb_value = (*env)->NewLocalRef(env, cb_value);
        cb_target = (*env)->NewLocalRef(env, cb_target);
        /* At most one of the following two while clauses will be executed.
         * Bring the deeper of cb_target and cb_value to the depth of the
         * shallower one.
         */
        while (cb_super_value != 0) {
          /* cb_value is deeper */
            jclass cb_tmp;

            cb_tmp = (*env)->GetSuperclass(env, cb_super_value);
            (*env)->DeleteLocalRef(env, cb_super_value);
            cb_super_value = cb_tmp;

            cb_tmp = (*env)->GetSuperclass(env, cb_value);
            (*env)->DeleteLocalRef(env, cb_value);
            cb_value = cb_tmp;
        }
        while (cb_super_target != 0) {
          /* cb_target is deeper */
            jclass cb_tmp;

            cb_tmp = (*env)->GetSuperclass(env, cb_super_target);
            (*env)->DeleteLocalRef(env, cb_super_target);
            cb_super_target = cb_tmp;

            cb_tmp = (*env)->GetSuperclass(env, cb_target);
            (*env)->DeleteLocalRef(env, cb_target);
            cb_target = cb_tmp;
        }

        /* Walk both up, maintaining equal depth, until a join is found.  We
         * know that we will find one.  */
        while (!(*env)->IsSameObject(env, cb_value, cb_target)) {
            jclass cb_tmp;
            cb_tmp = (*env)->GetSuperclass(env, cb_value);
            (*env)->DeleteLocalRef(env, cb_value);
            cb_value = cb_tmp;
            cb_tmp = (*env)->GetSuperclass(env, cb_target);
            (*env)->DeleteLocalRef(env, cb_target);
            cb_target = cb_tmp;
        }
        result_info = make_class_info(context, cb_value);
        (*env)->DeleteLocalRef(env, cb_value);
        (*env)->DeleteLocalRef(env, cb_super_value);
        (*env)->DeleteLocalRef(env, cb_target);
        (*env)->DeleteLocalRef(env, cb_super_target);
        return result_info;
    } /* both items are classes */
}


/* Given a fullinfo_type corresponding to an Object, return the jclass
 * of that type.
 *
 * This function always returns a global reference!
 */

static jclass
object_fullinfo_to_classclass(context_type *context, fullinfo_type classinfo)
{
    unsigned short info = GET_EXTRA_INFO(classinfo);
    return ID_to_class(context, info);
}

static void free_block(void *ptr, int kind)
{
    switch (kind) {
    case VM_STRING_UTF:
        JVM_ReleaseUTF(ptr);
        break;
    case VM_MALLOC_BLK:
        free(ptr);
        break;
    }
}

static void check_and_push(context_type *context, const void *ptr, int kind)
{
    alloc_stack_type *p;
    if (ptr == 0)
        CCout_of_memory(context);
    if (context->alloc_stack_top < ALLOC_STACK_SIZE)
        p = &(context->alloc_stack[context->alloc_stack_top++]);
    else {
        /* Otherwise we have to malloc */
        p = malloc(sizeof(alloc_stack_type));
        if (p == 0) {
            /* Make sure we clean up. */
            free_block((void *)ptr, kind);
            CCout_of_memory(context);
        }
    }
    p->kind = kind;
    p->ptr = (void *)ptr;
    p->next = context->allocated_memory;
    context->allocated_memory = p;
}

static void pop_and_free(context_type *context)
{
    alloc_stack_type *p = context->allocated_memory;
    context->allocated_memory = p->next;
    free_block(p->ptr, p->kind);
    if (p < context->alloc_stack + ALLOC_STACK_SIZE &&
        p >= context->alloc_stack)
        context->alloc_stack_top--;
    else
        free(p);
}

static int signature_to_args_size(const char *method_signature)
{
    const char *p;
    int args_size = 0;
    for (p = method_signature; *p != JVM_SIGNATURE_ENDFUNC; p++) {
        switch (*p) {
          case JVM_SIGNATURE_BOOLEAN:
          case JVM_SIGNATURE_BYTE:
          case JVM_SIGNATURE_CHAR:
          case JVM_SIGNATURE_SHORT:
          case JVM_SIGNATURE_INT:
          case JVM_SIGNATURE_FLOAT:
            args_size += 1;
            break;
          case JVM_SIGNATURE_CLASS:
            args_size += 1;
            while (*p != JVM_SIGNATURE_ENDCLASS) p++;
            break;
          case JVM_SIGNATURE_ARRAY:
            args_size += 1;
            while ((*p == JVM_SIGNATURE_ARRAY)) p++;
            /* If an array of classes, skip over class name, too. */
            if (*p == JVM_SIGNATURE_CLASS) {
                while (*p != JVM_SIGNATURE_ENDCLASS)
                  p++;
            }
            break;
          case JVM_SIGNATURE_DOUBLE:
          case JVM_SIGNATURE_LONG:
            args_size += 2;
            break;
          case JVM_SIGNATURE_FUNC:  /* ignore initial (, if given */
            break;
          default:
            /* Indicate an error. */
            return 0;
        }
    }
    return args_size;
}

#ifdef DEBUG

/* Below are for debugging. */

static void print_fullinfo_type(context_type *, fullinfo_type, jboolean);

static void
print_stack(context_type *context, stack_info_type *stack_info)
{
    stack_item_type *stack = stack_info->stack;
    if (stack_info->stack_size == UNKNOWN_STACK_SIZE) {
        jio_fprintf(stdout, "x");
    } else {
        jio_fprintf(stdout, "(");
        for ( ; stack != 0; stack = stack->next)
            print_fullinfo_type(context, stack->item,
                (jboolean)(verify_verbose > 1 ? JNI_TRUE : JNI_FALSE));
        jio_fprintf(stdout, ")");
    }
}

static void
print_registers(context_type *context, register_info_type *register_info)
{
    int register_count = register_info->register_count;
    if (register_count == UNKNOWN_REGISTER_COUNT) {
        jio_fprintf(stdout, "x");
    } else {
        fullinfo_type *registers = register_info->registers;
        int mask_count = register_info->mask_count;
        mask_type *masks = register_info->masks;
        int i, j;

        jio_fprintf(stdout, "{");
        for (i = 0; i < register_count; i++)
            print_fullinfo_type(context, registers[i],
                (jboolean)(verify_verbose > 1 ? JNI_TRUE : JNI_FALSE));
        jio_fprintf(stdout, "}");
        for (i = 0; i < mask_count; i++) {
            char *separator = "";
            int *modifies = masks[i].modifies;
            jio_fprintf(stdout, "<%d: ", masks[i].entry);
            for (j = 0;
                 j < JVM_GetMethodIxLocalsCount(context->env,
                                                context->class,
                                                context->method_index);
                 j++)
                if (IS_BIT_SET(modifies, j)) {
                    jio_fprintf(stdout, "%s%d", separator, j);
                    separator = ",";
                }
            jio_fprintf(stdout, ">");
        }
    }
}


static void
print_flags(context_type *context, flag_type and_flags, flag_type or_flags)
{
    if (and_flags != ((flag_type)-1) || or_flags != 0) {
        jio_fprintf(stdout, "<%x %x>", and_flags, or_flags);
    }
}

static void
print_fullinfo_type(context_type *context, fullinfo_type type, jboolean verbose)
{
    int i;
    int indirection = GET_INDIRECTION(type);
    for (i = indirection; i-- > 0; )
        jio_fprintf(stdout, "[");
    switch (GET_ITEM_TYPE(type)) {
        case ITEM_Integer:
            jio_fprintf(stdout, "I"); break;
        case ITEM_Float:
            jio_fprintf(stdout, "F"); break;
        case ITEM_Double:
            jio_fprintf(stdout, "D"); break;
        case ITEM_Double_2:
            jio_fprintf(stdout, "d"); break;
        case ITEM_Long:
            jio_fprintf(stdout, "L"); break;
        case ITEM_Long_2:
            jio_fprintf(stdout, "l"); break;
        case ITEM_ReturnAddress:
            jio_fprintf(stdout, "a"); break;
        case ITEM_Object:
            if (!verbose) {
                jio_fprintf(stdout, "A");
            } else {
                unsigned short extra = GET_EXTRA_INFO(type);
                if (extra == 0) {
                    jio_fprintf(stdout, "/Null/");
                } else {
                    const char *name = ID_to_class_name(context, extra);
                    const char *name2 = strrchr(name, '/');
                    jio_fprintf(stdout, "/%s/", name2 ? name2 + 1 : name);
                }
            }
            break;
        case ITEM_Char:
            jio_fprintf(stdout, "C"); break;
        case ITEM_Short:
            jio_fprintf(stdout, "S"); break;
        case ITEM_Boolean:
            jio_fprintf(stdout, "Z"); break;
        case ITEM_Byte:
            jio_fprintf(stdout, "B"); break;
        case ITEM_NewObject:
            if (!verbose) {
                jio_fprintf(stdout, "@");
            } else {
                int inum = GET_EXTRA_INFO(type);
                fullinfo_type real_type =
                    context->instruction_data[inum].operand2.fi;
                jio_fprintf(stdout, ">");
                print_fullinfo_type(context, real_type, JNI_TRUE);
                jio_fprintf(stdout, "<");
            }
            break;
        case ITEM_InitObject:
            jio_fprintf(stdout, verbose ? ">/this/<" : "@");
            break;

        default:
            jio_fprintf(stdout, "?"); break;
    }
    for (i = indirection; i-- > 0; )
        jio_fprintf(stdout, "]");
}


static void
print_formatted_fieldname(context_type *context, int index)
{
    JNIEnv *env = context->env;
    jclass cb = context->class;
    const char *classname = JVM_GetCPFieldClassNameUTF(env, cb, index);
    const char *fieldname = JVM_GetCPFieldNameUTF(env, cb, index);
    jio_fprintf(stdout, "  <%s.%s>",
                classname ? classname : "", fieldname ? fieldname : "");
    JVM_ReleaseUTF(classname);
    JVM_ReleaseUTF(fieldname);
}

static void
print_formatted_methodname(context_type *context, int index)
{
    JNIEnv *env = context->env;
    jclass cb = context->class;
    const char *classname = JVM_GetCPMethodClassNameUTF(env, cb, index);
    const char *methodname = JVM_GetCPMethodNameUTF(env, cb, index);
    jio_fprintf(stdout, "  <%s.%s>",
                classname ? classname : "", methodname ? methodname : "");
    JVM_ReleaseUTF(classname);
    JVM_ReleaseUTF(methodname);
}

#endif /*DEBUG*/
