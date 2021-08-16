/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

#ifndef SHARE_CLASSFILE_CLASSLOADER_HPP
#define SHARE_CLASSFILE_CLASSLOADER_HPP

#include "jimage.hpp"
#include "runtime/handles.hpp"
#include "runtime/perfDataTypes.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/macros.hpp"

// The VM class loader.
#include <sys/stat.h>

// Name of boot "modules" image
#define  MODULES_IMAGE_NAME "modules"

// Class path entry (directory or zip file)

class JImageFile;
class ClassFileStream;
class PackageEntry;
template <typename T> class GrowableArray;

class ClassPathEntry : public CHeapObj<mtClass> {
private:
  ClassPathEntry* volatile _next;
protected:
  const char* copy_path(const char*path);
public:
  ClassPathEntry* next() const;
  virtual ~ClassPathEntry() {}
  void set_next(ClassPathEntry* next);

  virtual bool is_modules_image() const { return false; }
  virtual bool is_jar_file() const { return false; }
  // Is this entry created from the "Class-path" attribute from a JAR Manifest?
  virtual bool from_class_path_attr() const { return false; }
  virtual const char* name() const = 0;
  virtual JImageFile* jimage() const { return NULL; }
  virtual void close_jimage() {}
  // Constructor
  ClassPathEntry() : _next(NULL) {}
  // Attempt to locate file_name through this class path entry.
  // Returns a class file parsing stream if successfull.
  virtual ClassFileStream* open_stream(JavaThread* current, const char* name) = 0;
  // Open the stream for a specific class loader
  virtual ClassFileStream* open_stream_for_loader(JavaThread* current, const char* name, ClassLoaderData* loader_data) {
    return open_stream(current, name);
  }
};

class ClassPathDirEntry: public ClassPathEntry {
 private:
  const char* _dir;           // Name of directory
 public:
  const char* name() const { return _dir; }
  ClassPathDirEntry(const char* dir) {
    _dir = copy_path(dir);
  }
  virtual ~ClassPathDirEntry() {}
  ClassFileStream* open_stream(JavaThread* current, const char* name);
};

// Type definitions for zip file and zip file entry
typedef void* jzfile;
typedef struct {
  char *name;                   /* entry name */
  jlong time;                   /* modification time */
  jlong size;                   /* size of uncompressed data */
  jlong csize;                  /* size of compressed data (zero if uncompressed) */
  jint crc;                     /* crc of uncompressed data */
  char *comment;                /* optional zip file comment */
  jbyte *extra;                 /* optional extra data */
  jlong pos;                    /* position of LOC header (if negative) or data */
} jzentry;

class ClassPathZipEntry: public ClassPathEntry {
 private:
  jzfile* _zip;              // The zip archive
  const char*   _zip_name;   // Name of zip archive
  bool _from_class_path_attr; // From the "Class-path" attribute of a jar file
 public:
  bool is_jar_file() const { return true;  }
  bool from_class_path_attr() const { return _from_class_path_attr; }
  const char* name() const { return _zip_name; }
  ClassPathZipEntry(jzfile* zip, const char* zip_name, bool is_boot_append, bool from_class_path_attr);
  virtual ~ClassPathZipEntry();
  u1* open_entry(JavaThread* current, const char* name, jint* filesize, bool nul_terminate);
  ClassFileStream* open_stream(JavaThread* current, const char* name);
  void contents_do(void f(const char* name, void* context), void* context);
};


// For java image files
class ClassPathImageEntry: public ClassPathEntry {
private:
  const char* _name;
  DEBUG_ONLY(static ClassPathImageEntry* _singleton;)
public:
  bool is_modules_image() const;
  const char* name() const { return _name == NULL ? "" : _name; }
  JImageFile* jimage() const;
  JImageFile* jimage_non_null() const;
  void close_jimage();
  ClassPathImageEntry(JImageFile* jimage, const char* name);
  virtual ~ClassPathImageEntry() { ShouldNotReachHere(); }
  ClassFileStream* open_stream(JavaThread* current, const char* name);
  ClassFileStream* open_stream_for_loader(JavaThread* current, const char* name, ClassLoaderData* loader_data);
};

// ModuleClassPathList contains a linked list of ClassPathEntry's
// that have been specified for a specific module.  Currently,
// the only way to specify a module/path pair is via the --patch-module
// command line option.
class ModuleClassPathList : public CHeapObj<mtClass> {
private:
  Symbol* _module_name;
  // First and last entries of class path entries for a specific module
  ClassPathEntry* _module_first_entry;
  ClassPathEntry* _module_last_entry;
public:
  Symbol* module_name() const { return _module_name; }
  ClassPathEntry* module_first_entry() const { return _module_first_entry; }
  ModuleClassPathList(Symbol* module_name);
  ~ModuleClassPathList();
  void add_to_list(ClassPathEntry* new_entry);
};

class ClassLoader: AllStatic {
 public:
  enum ClassLoaderType {
    BOOT_LOADER = 1,      /* boot loader */
    PLATFORM_LOADER  = 2, /* PlatformClassLoader */
    APP_LOADER  = 3       /* AppClassLoader */
  };
 protected:

  // Performance counters
  static PerfCounter* _perf_accumulated_time;
  static PerfCounter* _perf_classes_inited;
  static PerfCounter* _perf_class_init_time;
  static PerfCounter* _perf_class_init_selftime;
  static PerfCounter* _perf_classes_verified;
  static PerfCounter* _perf_class_verify_time;
  static PerfCounter* _perf_class_verify_selftime;
  static PerfCounter* _perf_classes_linked;
  static PerfCounter* _perf_class_link_time;
  static PerfCounter* _perf_class_link_selftime;
  static PerfCounter* _perf_sys_class_lookup_time;
  static PerfCounter* _perf_shared_classload_time;
  static PerfCounter* _perf_sys_classload_time;
  static PerfCounter* _perf_app_classload_time;
  static PerfCounter* _perf_app_classload_selftime;
  static PerfCounter* _perf_app_classload_count;
  static PerfCounter* _perf_define_appclasses;
  static PerfCounter* _perf_define_appclass_time;
  static PerfCounter* _perf_define_appclass_selftime;
  static PerfCounter* _perf_app_classfile_bytes_read;
  static PerfCounter* _perf_sys_classfile_bytes_read;

  static PerfCounter* _unsafe_defineClassCallCounter;

  // The boot class path consists of 3 ordered pieces:
  //  1. the module/path pairs specified to --patch-module
  //    --patch-module=<module>=<file>(<pathsep><file>)*
  //  2. the base piece
  //    [jimage | build with exploded modules]
  //  3. boot loader append path
  //    [-Xbootclasspath/a]; [jvmti appended entries]
  //
  // The boot loader must obey this order when attempting
  // to load a class.

  // 1. Contains the module/path pairs specified to --patch-module
  static GrowableArray<ModuleClassPathList*>* _patch_mod_entries;

  // 2. the base piece
  //    Contains the ClassPathEntry of the modular java runtime image.
  //    If no java runtime image is present, this indicates a
  //    build with exploded modules is being used instead.
  static ClassPathEntry* _jrt_entry;
  static GrowableArray<ModuleClassPathList*>* _exploded_entries;
  enum { EXPLODED_ENTRY_SIZE = 80 }; // Initial number of exploded modules

  // 3. the boot loader's append path
  //    [-Xbootclasspath/a]; [jvmti appended entries]
  //    Note: boot loader append path does not support named modules.
  static ClassPathEntry* volatile _first_append_entry_list;
  static ClassPathEntry* first_append_entry() {
    return Atomic::load_acquire(&_first_append_entry_list);
  }

  // Last entry in linked list of appended ClassPathEntry instances
  static ClassPathEntry* volatile _last_append_entry;

  // Info used by CDS
  CDS_ONLY(static ClassPathEntry* _app_classpath_entries;)
  CDS_ONLY(static ClassPathEntry* _last_app_classpath_entry;)
  CDS_ONLY(static ClassPathEntry* _module_path_entries;)
  CDS_ONLY(static ClassPathEntry* _last_module_path_entry;)
  CDS_ONLY(static void setup_app_search_path(JavaThread* current, const char* class_path);)
  CDS_ONLY(static void setup_module_search_path(JavaThread* current, const char* path);)
  static void add_to_app_classpath_entries(JavaThread* current,
                                           const char* path,
                                           ClassPathEntry* entry,
                                           bool check_for_duplicates);
  CDS_ONLY(static void add_to_module_path_entries(const char* path,
                                           ClassPathEntry* entry);)
 public:
  CDS_ONLY(static ClassPathEntry* app_classpath_entries() {return _app_classpath_entries;})
  CDS_ONLY(static ClassPathEntry* module_path_entries() {return _module_path_entries;})

  static bool has_bootclasspath_append() { return first_append_entry() != NULL; }

 protected:
  // Initialization:
  //   - setup the boot loader's system class path
  //   - setup the boot loader's patch mod entries, if present
  //   - create the ModuleEntry for java.base
  static void setup_bootstrap_search_path(JavaThread* current);
  static void setup_bootstrap_search_path_impl(JavaThread* current, const char *class_path);
  static void setup_patch_mod_entries();
  static void create_javabase();

  static void* dll_lookup(void* lib, const char* name, const char* path);
  static void load_java_library();
  static void load_zip_library();
  static void load_jimage_library();

 private:
  static int  _libzip_loaded; // used to sync loading zip.
  static void release_load_zip_library();
  static inline void load_zip_library_if_needed();

 public:
  static jzfile* open_zip_file(const char* canonical_path, char** error_msg, JavaThread* thread);
  static ClassPathEntry* create_class_path_entry(JavaThread* current,
                                                 const char *path, const struct stat* st,
                                                 bool is_boot_append,
                                                 bool from_class_path_attr);

  // Canonicalizes path names, so strcmp will work properly. This is mainly
  // to avoid confusing the zip library
  static char* get_canonical_path(const char* orig, Thread* thread);
  static const char* file_name_for_class_name(const char* class_name,
                                              int class_name_len);
  static PackageEntry* get_package_entry(Symbol* pkg_name, ClassLoaderData* loader_data);
  static int crc32(int crc, const char* buf, int len);
  static bool update_class_path_entry_list(JavaThread* current,
                                           const char *path,
                                           bool check_for_duplicates,
                                           bool is_boot_append,
                                           bool from_class_path_attr);
  static void print_bootclasspath();

  // Timing
  static PerfCounter* perf_accumulated_time()         { return _perf_accumulated_time; }
  static PerfCounter* perf_classes_inited()           { return _perf_classes_inited; }
  static PerfCounter* perf_class_init_time()          { return _perf_class_init_time; }
  static PerfCounter* perf_class_init_selftime()      { return _perf_class_init_selftime; }
  static PerfCounter* perf_classes_verified()         { return _perf_classes_verified; }
  static PerfCounter* perf_class_verify_time()        { return _perf_class_verify_time; }
  static PerfCounter* perf_class_verify_selftime()    { return _perf_class_verify_selftime; }
  static PerfCounter* perf_classes_linked()           { return _perf_classes_linked; }
  static PerfCounter* perf_class_link_time()          { return _perf_class_link_time; }
  static PerfCounter* perf_class_link_selftime()      { return _perf_class_link_selftime; }
  static PerfCounter* perf_sys_class_lookup_time()    { return _perf_sys_class_lookup_time; }
  static PerfCounter* perf_shared_classload_time()    { return _perf_shared_classload_time; }
  static PerfCounter* perf_sys_classload_time()       { return _perf_sys_classload_time; }
  static PerfCounter* perf_app_classload_time()       { return _perf_app_classload_time; }
  static PerfCounter* perf_app_classload_selftime()   { return _perf_app_classload_selftime; }
  static PerfCounter* perf_app_classload_count()      { return _perf_app_classload_count; }
  static PerfCounter* perf_define_appclasses()        { return _perf_define_appclasses; }
  static PerfCounter* perf_define_appclass_time()     { return _perf_define_appclass_time; }
  static PerfCounter* perf_define_appclass_selftime() { return _perf_define_appclass_selftime; }
  static PerfCounter* perf_app_classfile_bytes_read() { return _perf_app_classfile_bytes_read; }
  static PerfCounter* perf_sys_classfile_bytes_read() { return _perf_sys_classfile_bytes_read; }

  // Record how many calls to Unsafe_DefineClass
  static PerfCounter* unsafe_defineClassCallCounter() {
    return _unsafe_defineClassCallCounter;
  }

  // Modular java runtime image is present vs. a build with exploded modules
  static bool has_jrt_entry() { return (_jrt_entry != NULL); }
  static ClassPathEntry* get_jrt_entry() { return _jrt_entry; }
  static void close_jrt_image();

  // Add a module's exploded directory to the boot loader's exploded module build list
  static void add_to_exploded_build_list(JavaThread* current, Symbol* module_name);

  // Attempt load of individual class from either the patched or exploded modules build lists
  static ClassFileStream* search_module_entries(JavaThread* current,
                                                const GrowableArray<ModuleClassPathList*>* const module_list,
                                                const char* const class_name,
                                                const char* const file_name);

  // Load individual .class file
  static InstanceKlass* load_class(Symbol* class_name, bool search_append_only, TRAPS);

  // If the specified package has been loaded by the system, then returns
  // the name of the directory or ZIP file that the package was loaded from.
  // Returns null if the package was not loaded.
  // Note: The specified name can either be the name of a class or package.
  // If a package name is specified, then it must be "/"-separator and also
  // end with a trailing "/".
  static oop get_system_package(const char* name, TRAPS);

  // Returns an array of Java strings representing all of the currently
  // loaded system packages.
  // Note: The package names returned are "/"-separated and end with a
  // trailing "/".
  static objArrayOop get_system_packages(TRAPS);

  // Initialization
  static void initialize(TRAPS);
  static void classLoader_init2(JavaThread* current);
  CDS_ONLY(static void initialize_shared_path(JavaThread* current);)
  CDS_ONLY(static void initialize_module_path(TRAPS);)

  static int compute_Object_vtable();

  static ClassPathEntry* classpath_entry(int n);

  static bool is_in_patch_mod_entries(Symbol* module_name);

#if INCLUDE_CDS
  // Sharing dump and restore

  // Helper function used by CDS code to get the number of boot classpath
  // entries during shared classpath setup time.
  static int num_boot_classpath_entries();

  static ClassPathEntry* get_next_boot_classpath_entry(ClassPathEntry* e);

  // Helper function used by CDS code to get the number of app classpath
  // entries during shared classpath setup time.
  static int num_app_classpath_entries();

  // Helper function used by CDS code to get the number of module path
  // entries during shared classpath setup time.
  static int num_module_path_entries();
  static void  exit_with_path_failure(const char* error, const char* message);
  static char* skip_uri_protocol(char* source);
  static void  record_result(JavaThread* current, InstanceKlass* ik, const ClassFileStream* stream);
#endif

  static char* lookup_vm_options();

  static JImageLocationRef jimage_find_resource(JImageFile* jf, const char* module_name,
                                                const char* file_name, jlong &size);

  static void  trace_class_path(const char* msg, const char* name = NULL);

  // VM monitoring and management support
  static jlong classloader_time_ms();
  static jlong class_method_total_size();
  static jlong class_init_count();
  static jlong class_init_time_ms();
  static jlong class_verify_time_ms();
  static jlong class_link_count();
  static jlong class_link_time_ms();

  // adds a class path to the boot append entries
  static void add_to_boot_append_entries(ClassPathEntry* new_entry);

  // creates a class path zip entry (returns NULL if JAR file cannot be opened)
  static ClassPathZipEntry* create_class_path_zip_entry(const char *apath, bool is_boot_append);

  static bool string_ends_with(const char* str, const char* str_to_find);

  // Extract package name from a fully qualified class name
  // *bad_class_name is set to true if there's a problem with parsing class_name, to
  // distinguish from a class_name with no package name, as both cases have a NULL return value
  static Symbol* package_from_class_name(const Symbol* class_name, bool* bad_class_name = NULL);

  // Debugging
  static void verify()              PRODUCT_RETURN;
};

// PerfClassTraceTime is used to measure time for class loading related events.
// This class tracks cumulative time and exclusive time for specific event types.
// During the execution of one event, other event types (e.g. class loading and
// resolution) as well as recursive calls of the same event type could happen.
// Only one elapsed timer (cumulative) and one thread-local self timer (exclusive)
// (i.e. only one event type) are active at a time even multiple PerfClassTraceTime
// instances have been created as multiple events are happening.
class PerfClassTraceTime {
 public:
  enum {
    CLASS_LOAD   = 0,
    CLASS_LINK   = 1,
    CLASS_VERIFY = 2,
    CLASS_CLINIT = 3,
    DEFINE_CLASS = 4,
    EVENT_TYPE_COUNT = 5
  };
 protected:
  // _t tracks time from initialization to destruction of this timer instance
  // including time for all other event types, and recursive calls of this type.
  // When a timer is called recursively, the elapsedTimer _t would not be used.
  elapsedTimer     _t;
  PerfLongCounter* _timep;
  PerfLongCounter* _selftimep;
  PerfLongCounter* _eventp;
  // pointer to thread-local recursion counter and timer array
  // The thread_local timers track cumulative time for specific event types
  // exclusive of time for other event types, but including recursive calls
  // of the same type.
  int*             _recursion_counters;
  elapsedTimer*    _timers;
  int              _event_type;
  int              _prev_active_event;

 public:

  inline PerfClassTraceTime(PerfLongCounter* timep,     /* counter incremented with inclusive time */
                            PerfLongCounter* selftimep, /* counter incremented with exclusive time */
                            PerfLongCounter* eventp,    /* event counter */
                            int* recursion_counters,    /* thread-local recursion counter array */
                            elapsedTimer* timers,       /* thread-local timer array */
                            int type                    /* event type */ ) :
      _timep(timep), _selftimep(selftimep), _eventp(eventp), _recursion_counters(recursion_counters), _timers(timers), _event_type(type) {
    initialize();
  }

  inline PerfClassTraceTime(PerfLongCounter* timep,     /* counter incremented with inclusive time */
                            elapsedTimer* timers,       /* thread-local timer array */
                            int type                    /* event type */ ) :
      _timep(timep), _selftimep(NULL), _eventp(NULL), _recursion_counters(NULL), _timers(timers), _event_type(type) {
    initialize();
  }

  ~PerfClassTraceTime();
  void initialize();
};

#endif // SHARE_CLASSFILE_CLASSLOADER_HPP
