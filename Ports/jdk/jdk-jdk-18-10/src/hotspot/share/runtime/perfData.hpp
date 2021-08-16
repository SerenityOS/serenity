/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_PERFDATA_HPP
#define SHARE_RUNTIME_PERFDATA_HPP

#include "memory/allocation.hpp"
#include "runtime/perfDataTypes.hpp"
#include "runtime/perfMemory.hpp"
#include "runtime/timer.hpp"

template <typename T> class GrowableArray;

/* jvmstat global and subsystem counter name space - enumeration value
 * serve as an index into the PerfDataManager::_name_space[] array
 * containing the corresponding name space string. Only the top level
 * subsystem name spaces are represented here.
 */
enum CounterNS {
  // top level name spaces
  JAVA_NS,
  COM_NS,
  SUN_NS,
  // subsystem name spaces
  JAVA_GC,              // Garbage Collection name spaces
  COM_GC,
  SUN_GC,
  JAVA_CI,              // Compiler name spaces
  COM_CI,
  SUN_CI,
  JAVA_CLS,             // Class Loader name spaces
  COM_CLS,
  SUN_CLS,
  JAVA_RT,              // Runtime name spaces
  COM_RT,
  SUN_RT,
  JAVA_OS,              // Operating System name spaces
  COM_OS,
  SUN_OS,
  JAVA_THREADS,         // Threads System name spaces
  COM_THREADS,
  SUN_THREADS,
  JAVA_PROPERTY,        // Java Property name spaces
  COM_PROPERTY,
  SUN_PROPERTY,
  NULL_NS,
  COUNTERNS_LAST = NULL_NS
};

/*
 * Classes to support access to production performance data
 *
 * The PerfData class structure is provided for creation, access, and update
 * of performance data (a.k.a. instrumentation) in a specific memory region
 * which is possibly accessible as shared memory. Although not explicitly
 * prevented from doing so, developers should not use the values returned
 * by accessor methods to make algorithmic decisions as they are potentially
 * extracted from a shared memory region. Although any shared memory region
 * created is with appropriate access restrictions, allowing read-write access
 * only to the principal that created the JVM, it is believed that a the
 * shared memory region facilitates an easier attack path than attacks
 * launched through mechanisms such as /proc. For this reason, it is
 * recommended that data returned by PerfData accessor methods be used
 * cautiously.
 *
 * There are three variability classifications of performance data
 *   Constants  -  value is written to the PerfData memory once, on creation
 *   Variables  -  value is modifiable, with no particular restrictions
 *   Counters   -  value is monotonically changing (increasing or decreasing)
 *
 * The performance data items can also have various types. The class
 * hierarchy and the structure of the memory region are designed to
 * accommodate new types as they are needed. Types are specified in
 * terms of Java basic types, which accommodates client applications
 * written in the Java programming language. The class hierarchy is:
 *
 * - PerfData (Abstract)
 *     - PerfLong (Abstract)
 *         - PerfLongConstant        (alias: PerfConstant)
 *         - PerfLongVariant (Abstract)
 *             - PerfLongVariable    (alias: PerfVariable)
 *             - PerfLongCounter     (alias: PerfCounter)
 *
 *     - PerfByteArray (Abstract)
 *         - PerfString (Abstract)
 *             - PerfStringVariable
 *             - PerfStringConstant
 *
 *
 * As seen in the class hierarchy, the initially supported types are:
 *
 *    Long      - performance data holds a Java long type
 *    ByteArray - performance data holds an array of Java bytes
 *                used for holding C++ char arrays.
 *
 * The String type is derived from the ByteArray type.
 *
 * A PerfData subtype is not required to provide an implementation for
 * each variability classification. For example, the String type provides
 * Variable and Constant variability classifications in the PerfStringVariable
 * and PerfStringConstant classes, but does not provide a counter type.
 *
 * Performance data are also described by a unit of measure. Units allow
 * client applications to make reasonable decisions on how to treat
 * performance data generically, preventing the need to hard-code the
 * specifics of a particular data item in client applications. The current
 * set of units are:
 *
 *   None        - the data has no units of measure
 *   Bytes       - data is measured in bytes
 *   Ticks       - data is measured in clock ticks
 *   Events      - data is measured in events. For example,
 *                 the number of garbage collection events or the
 *                 number of methods compiled.
 *   String      - data is not numerical. For example,
 *                 the java command line options
 *   Hertz       - data is a frequency
 *
 * The performance counters also provide a support attribute, indicating
 * the stability of the counter as a programmatic interface. The support
 * level is also implied by the name space in which the counter is created.
 * The counter name space support conventions follow the Java package, class,
 * and property support conventions:
 *
 *    java.*          - stable, supported interface
 *    com.sun.*       - unstable, supported interface
 *    sun.*           - unstable, unsupported interface
 *
 * In the above context, unstable is a measure of the interface support
 * level, not the implementation stability level.
 *
 * Currently, instances of PerfData subtypes are considered to have
 * a life time equal to that of the VM and are managed by the
 * PerfDataManager class. All constructors for the PerfData class and
 * its subtypes have protected constructors. Creation of PerfData
 * instances is performed by invoking various create methods on the
 * PerfDataManager class. Users should not attempt to delete these
 * instances as the PerfDataManager class expects to perform deletion
 * operations on exit of the VM.
 *
 * Examples:
 *
 * Creating performance counter that holds a monotonically increasing
 * long data value with units specified in U_Bytes in the "java.gc.*"
 * name space.
 *
 *   PerfLongCounter* foo_counter;
 *
 *   foo_counter = PerfDataManager::create_long_counter(JAVA_GC, "foo",
 *                                                       PerfData::U_Bytes,
 *                                                       optionalInitialValue,
 *                                                       CHECK);
 *   foo_counter->inc();
 *
 * Creating a performance counter that holds a variably change long
 * data value with units specified in U_Bytes in the "com.sun.ci
 * name space.
 *
 *   PerfLongVariable* bar_variable;
 *   bar_variable = PerfDataManager::create_long_variable(COM_CI, "bar",
.*                                                        PerfData::U_Bytes,
 *                                                        optionalInitialValue,
 *                                                        CHECK);
 *
 *   bar_variable->inc();
 *   bar_variable->set_value(0);
 *
 * Creating a performance counter that holds a constant string value in
 * the "sun.cls.*" name space.
 *
 *   PerfDataManager::create_string_constant(SUN_CLS, "foo", string, CHECK);
 *
 *   Although the create_string_constant() factory method returns a pointer
 *   to the PerfStringConstant object, it can safely be ignored. Developers
 *   are not encouraged to access the string constant's value via this
 *   pointer at this time due to security concerns.
 *
 * Creating a performance counter in an arbitrary name space that holds a
 * value that is sampled by the StatSampler periodic task.
 *
 *    PerfDataManager::create_counter("foo.sampled", PerfData::U_Events,
 *                                    &my_jlong, CHECK);
 *
 *    In this example, the PerfData pointer can be ignored as the caller
 *    is relying on the StatSampler PeriodicTask to sample the given
 *    address at a regular interval. The interval is defined by the
 *    PerfDataSamplingInterval global variable, and is applied on
 *    a system wide basis, not on an per-counter basis.
 *
 * Creating a performance counter in an arbitrary name space that utilizes
 * a helper object to return a value to the StatSampler via the take_sample()
 * method.
 *
 *     class MyTimeSampler : public PerfLongSampleHelper {
 *       public:
 *         jlong take_sample() { return os::elapsed_counter(); }
 *     };
 *
 *     PerfDataManager::create_counter(SUN_RT, "helped",
 *                                     PerfData::U_Ticks,
 *                                     new MyTimeSampler(), CHECK);
 *
 *     In this example, a subtype of PerfLongSampleHelper is instantiated
 *     and its take_sample() method is overridden to perform whatever
 *     operation is necessary to generate the data sample. This method
 *     will be called by the StatSampler at a regular interval, defined
 *     by the PerfDataSamplingInterval global variable.
 *
 *     As before, PerfSampleHelper is an alias for PerfLongSampleHelper.
 *
 * For additional uses of PerfData subtypes, see the utility classes
 * PerfTraceTime and PerfTraceTimedEvent below.
 *
 * Always-on non-sampled counters can be created independent of
 * the UsePerfData flag. Counters will be created on the c-heap
 * if UsePerfData is false.
 *
 * Until further notice, all PerfData objects should be created and
 * manipulated within a guarded block. The guard variable is
 * UsePerfData, a product flag set to true by default. This flag may
 * be removed from the product in the future.
 *
 */
class PerfData : public CHeapObj<mtInternal> {

  friend class StatSampler;      // for access to protected void sample()
  friend class PerfDataManager;  // for access to protected destructor
  friend class VMStructs;

  public:

    // the Variability enum must be kept in synchronization with the
    // the com.sun.hotspot.perfdata.Variability class
    enum Variability {
      V_Constant = 1,
      V_Monotonic = 2,
      V_Variable = 3,
      V_last = V_Variable
    };

    // the Units enum must be kept in synchronization with the
    // the com.sun.hotspot.perfdata.Units class
    enum Units {
      U_None = 1,
      U_Bytes = 2,
      U_Ticks = 3,
      U_Events = 4,
      U_String = 5,
      U_Hertz = 6,
      U_Last = U_Hertz
    };

    // Miscellaneous flags
    enum Flags {
      F_None = 0x0,
      F_Supported = 0x1    // interface is supported - java.* and com.sun.*
    };

  private:
    char* _name;
    Variability _v;
    Units _u;
    bool _on_c_heap;
    Flags _flags;

    PerfDataEntry* _pdep;

  protected:

    void *_valuep;

    PerfData(CounterNS ns, const char* name, Units u, Variability v);
    virtual ~PerfData();

    // create the entry for the PerfData item in the PerfData memory region.
    // this region is maintained separately from the PerfData objects to
    // facilitate its use by external processes.
    void create_entry(BasicType dtype, size_t dsize, size_t dlen = 0);

    // sample the data item given at creation time and write its value
    // into the its corresponding PerfMemory location.
    virtual void sample() = 0;

  public:

    // returns a boolean indicating the validity of this object.
    // the object is valid if and only if memory in PerfMemory
    // region was successfully allocated.
    inline bool is_valid() { return _valuep != NULL; }

    // returns a boolean indicating whether the underlying object
    // was allocated in the PerfMemory region or on the C heap.
    inline bool is_on_c_heap() { return _on_c_heap; }

    // returns a pointer to a char* containing the name of the item.
    // The pointer returned is the pointer to a copy of the name
    // passed to the constructor, not the pointer to the name in the
    // PerfData memory region. This redundancy is maintained for
    // security reasons as the PerfMemory region may be in shared
    // memory.
    const char* name() { return _name; }

    // returns the variability classification associated with this item
    Variability variability() { return _v; }

    // returns the units associated with this item.
    Units units() { return _u; }

    // returns the flags associated with this item.
    Flags flags() { return _flags; }

    // returns the address of the data portion of the item in the
    // PerfData memory region.
    inline void* get_address() { return _valuep; }

    // returns the value of the data portion of the item in the
    // PerfData memory region formatted as a string.
    virtual int format(char* cp, int length) = 0;
};

/*
 * PerfLongSampleHelper, and its alias PerfSamplerHelper, is a base class
 * for helper classes that rely upon the StatSampler periodic task to
 * invoke the take_sample() method and write the value returned to its
 * appropriate location in the PerfData memory region.
 */
class PerfLongSampleHelper : public CHeapObj<mtInternal> {
  public:
    virtual jlong take_sample() = 0;
};

/*
 * PerfLong is the base class for the various Long PerfData subtypes.
 * it contains implementation details that are common among its derived
 * types.
 */
class PerfLong : public PerfData {

  protected:

    PerfLong(CounterNS ns, const char* namep, Units u, Variability v);

  public:
    int format(char* buffer, int length);

    // returns the value of the data portion of the item in the
    // PerfData memory region.
    inline jlong get_value() { return *(jlong*)_valuep; }
};

/*
 * The PerfLongConstant class, and its alias PerfConstant, implement
 * a PerfData subtype that holds a jlong data value that is set upon
 * creation of an instance of this class. This class provides no
 * methods for changing the data value stored in PerfData memory region.
 */
class PerfLongConstant : public PerfLong {

  friend class PerfDataManager; // for access to protected constructor

  private:
    // hide sample() - no need to sample constants
    void sample() { }

  protected:

    PerfLongConstant(CounterNS ns, const char* namep, Units u,
                     jlong initial_value=0)
                    : PerfLong(ns, namep, u, V_Constant) {

       if (is_valid()) *(jlong*)_valuep = initial_value;
    }
};

/*
 * The PerfLongVariant class, and its alias PerfVariant, implement
 * a PerfData subtype that holds a jlong data value that can be modified
 * in an unrestricted manner. This class provides the implementation details
 * for common functionality among its derived types.
 */
class PerfLongVariant : public PerfLong {

  protected:
    jlong* _sampled;
    PerfLongSampleHelper* _sample_helper;

    PerfLongVariant(CounterNS ns, const char* namep, Units u, Variability v,
                    jlong initial_value=0)
                   : PerfLong(ns, namep, u, v) {
      if (is_valid()) *(jlong*)_valuep = initial_value;
    }

    PerfLongVariant(CounterNS ns, const char* namep, Units u, Variability v,
                    jlong* sampled);

    PerfLongVariant(CounterNS ns, const char* namep, Units u, Variability v,
                    PerfLongSampleHelper* sample_helper);

    void sample();

  public:
    inline void inc() { (*(jlong*)_valuep)++; }
    inline void inc(jlong val) { (*(jlong*)_valuep) += val; }
    inline void dec(jlong val) { inc(-val); }
    inline void add(jlong val) { (*(jlong*)_valuep) += val; }
};

/*
 * The PerfLongCounter class, and its alias PerfCounter, implement
 * a PerfData subtype that holds a jlong data value that can (should)
 * be modified in a monotonic manner. The inc(jlong) and add(jlong)
 * methods can be passed negative values to implement a monotonically
 * decreasing value. However, we rely upon the programmer to honor
 * the notion that this counter always moves in the same direction -
 * either increasing or decreasing.
 */
class PerfLongCounter : public PerfLongVariant {

  friend class PerfDataManager; // for access to protected constructor

  protected:

    PerfLongCounter(CounterNS ns, const char* namep, Units u,
                    jlong initial_value=0)
                   : PerfLongVariant(ns, namep, u, V_Monotonic,
                                     initial_value) { }

    PerfLongCounter(CounterNS ns, const char* namep, Units u, jlong* sampled)
                  : PerfLongVariant(ns, namep, u, V_Monotonic, sampled) { }

    PerfLongCounter(CounterNS ns, const char* namep, Units u,
                    PerfLongSampleHelper* sample_helper)
                   : PerfLongVariant(ns, namep, u, V_Monotonic,
                                     sample_helper) { }
};

/*
 * The PerfLongVariable class, and its alias PerfVariable, implement
 * a PerfData subtype that holds a jlong data value that can
 * be modified in an unrestricted manner.
 */
class PerfLongVariable : public PerfLongVariant {

  friend class PerfDataManager; // for access to protected constructor

  protected:

    PerfLongVariable(CounterNS ns, const char* namep, Units u,
                     jlong initial_value=0)
                    : PerfLongVariant(ns, namep, u, V_Variable,
                                      initial_value) { }

    PerfLongVariable(CounterNS ns, const char* namep, Units u, jlong* sampled)
                    : PerfLongVariant(ns, namep, u, V_Variable, sampled) { }

    PerfLongVariable(CounterNS ns, const char* namep, Units u,
                     PerfLongSampleHelper* sample_helper)
                    : PerfLongVariant(ns, namep, u, V_Variable,
                                      sample_helper) { }

  public:
    inline void set_value(jlong val) { (*(jlong*)_valuep) = val; }
};

/*
 * The PerfByteArray provides a PerfData subtype that allows the creation
 * of a contiguous region of the PerfData memory region for storing a vector
 * of bytes. This class is currently intended to be a base class for
 * the PerfString class, and cannot be instantiated directly.
 */
class PerfByteArray : public PerfData {

  protected:
    jint _length;

    PerfByteArray(CounterNS ns, const char* namep, Units u, Variability v,
                  jint length);
};

class PerfString : public PerfByteArray {

  protected:

    void set_string(const char* s2);

    PerfString(CounterNS ns, const char* namep, Variability v, jint length,
               const char* initial_value)
              : PerfByteArray(ns, namep, U_String, v, length) {
       if (is_valid()) set_string(initial_value);
    }

  public:

    int format(char* buffer, int length);
};

/*
 * The PerfStringConstant class provides a PerfData sub class that
 * allows a null terminated string of single byte characters to be
 * stored in the PerfData memory region.
 */
class PerfStringConstant : public PerfString {

  friend class PerfDataManager; // for access to protected constructor

  private:

    // hide sample() - no need to sample constants
    void sample() { }

  protected:

    // Restrict string constant lengths to be <= PerfMaxStringConstLength.
    // This prevents long string constants, as can occur with very
    // long classpaths or java command lines, from consuming too much
    // PerfData memory.
    PerfStringConstant(CounterNS ns, const char* namep,
                       const char* initial_value);
};

/*
 * The PerfStringVariable class provides a PerfData sub class that
 * allows a null terminated string of single byte character data
 * to be stored in PerfData memory region. The string value can be reset
 * after initialization. If the string value is >= max_length, then
 * it will be truncated to max_length characters. The copied string
 * is always null terminated.
 */
class PerfStringVariable : public PerfString {

  friend class PerfDataManager; // for access to protected constructor

  protected:

    // sampling of string variables are not yet supported
    void sample() { }

    PerfStringVariable(CounterNS ns, const char* namep, jint max_length,
                       const char* initial_value)
                      : PerfString(ns, namep, V_Variable, max_length+1,
                                   initial_value) { }

  public:
    inline void set_value(const char* val) { set_string(val); }
};


/*
 * The PerfDataList class is a container class for managing lists
 * of PerfData items. The intention of this class is to allow for
 * alternative implementations for management of list of PerfData
 * items without impacting the code that uses the lists.
 *
 * The initial implementation is based upon GrowableArray. Searches
 * on GrowableArray types is linear in nature and this may become
 * a performance issue for creation of PerfData items, particularly
 * from Java code where a test for existence is implemented as a
 * search over all existing PerfData items.
 *
 * The abstraction is not complete. A more general container class
 * would provide an Iterator abstraction that could be used to
 * traverse the lists. This implementation still relies upon integer
 * iterators and the at(int index) method. However, the GrowableArray
 * is not directly visible outside this class and can be replaced by
 * some other implementation, as long as that implementation provides
 * a mechanism to iterate over the container by index.
 */
class PerfDataList : public CHeapObj<mtInternal> {

  private:

    // GrowableArray implementation
    typedef GrowableArray<PerfData*> PerfDataArray;

    PerfDataArray* _set;

    // method to search for a instrumentation object by name
    static bool by_name(void* name, PerfData* pd);

  protected:
    // we expose the implementation here to facilitate the clone
    // method.
    PerfDataArray* get_impl() { return _set; }

  public:

    // create a PerfDataList with the given initial length
    PerfDataList(int length);

    // create a PerfDataList as a shallow copy of the given PerfDataList
    PerfDataList(PerfDataList* p);

    ~PerfDataList();

    // return the PerfData item indicated by name,
    // or NULL if it doesn't exist.
    PerfData* find_by_name(const char* name);

    // return true if a PerfData item with the name specified in the
    // argument exists, otherwise return false.
    bool contains(const char* name) { return find_by_name(name) != NULL; }

    // return the number of PerfData items in this list
    inline int length();

    // add a PerfData item to this list
    inline void append(PerfData *p);

    // remove the given PerfData item from this list. When called
    // while iterating over the list, this method will result in a
    // change in the length of the container. The at(int index)
    // method is also impacted by this method as elements with an
    // index greater than the index of the element removed by this
    // method will be shifted down by one.
    inline void remove(PerfData *p);

    // create a new PerfDataList from this list. The new list is
    // a shallow copy of the original list and care should be taken
    // with respect to delete operations on the elements of the list
    // as the are likely in use by another copy of the list.
    PerfDataList* clone();

    // for backward compatibility with GrowableArray - need to implement
    // some form of iterator to provide a cleaner abstraction for
    // iteration over the container.
    inline PerfData* at(int index);
};


/*
 * The PerfDataManager class is responsible for creating PerfData
 * subtypes via a set a factory methods and for managing lists
 * of the various PerfData types.
 */
class PerfDataManager : AllStatic {

  friend class StatSampler;   // for access to protected PerfDataList methods

  private:
    static PerfDataList* _all;
    static PerfDataList* _sampled;
    static PerfDataList* _constants;
    static const char* _name_spaces[];
    static volatile bool _has_PerfData;

    // add a PerfData item to the list(s) of know PerfData objects
    static void add_item(PerfData* p, bool sampled);

  protected:
    // return the list of all known PerfData items
    static PerfDataList* all();
    static inline int count();

    // return the list of all known PerfData items that are to be
    // sampled by the StatSampler.
    static PerfDataList* sampled();

    // return the list of all known PerfData items that have a
    // variability classification of type Constant
    static PerfDataList* constants();

  public:

    // method to check for the existence of a PerfData item with
    // the given name.
    static inline bool exists(const char* name);

    // method to map a CounterNS enumeration to a namespace string
    static const char* ns_to_string(CounterNS ns) {
      return _name_spaces[ns];
    }

    // methods to test the interface stability of a given counter namespace
    //
    static bool is_stable_supported(CounterNS ns) {
      return (ns != NULL_NS) && ((ns % 3) == JAVA_NS);
    }
    static bool is_unstable_supported(CounterNS ns) {
      return (ns != NULL_NS) && ((ns % 3) == COM_NS);
    }

    // methods to test the interface stability of a given counter name
    //
    static bool is_stable_supported(const char* name) {
      const char* javadot = "java.";
      return strncmp(name, javadot, strlen(javadot)) == 0;
    }
    static bool is_unstable_supported(const char* name) {
      const char* comdot = "com.sun.";
      return strncmp(name, comdot, strlen(comdot)) == 0;
    }

    // method to construct counter name strings in a given name space.
    // The string object is allocated from the Resource Area and calls
    // to this method must be made within a ResourceMark.
    //
    static char* counter_name(const char* name_space, const char* name);

    // method to construct name space strings in a given name space.
    // The string object is allocated from the Resource Area and calls
    // to this method must be made within a ResourceMark.
    //
    static char* name_space(const char* name_space, const char* sub_space) {
      return counter_name(name_space, sub_space);
    }

    // same as above, but appends the instance number to the name space
    //
    static char* name_space(const char* name_space, const char* sub_space,
                            int instance);
    static char* name_space(const char* name_space, int instance);


    // these methods provide the general interface for creating
    // performance data resources. The types of performance data
    // resources can be extended by adding additional create<type>
    // methods.

    // Constant Types
    static PerfStringConstant* create_string_constant(CounterNS ns,
                                                      const char* name,
                                                      const char *s, TRAPS);

    static PerfLongConstant* create_long_constant(CounterNS ns,
                                                  const char* name,
                                                  PerfData::Units u,
                                                  jlong val, TRAPS);


    // Variable Types
    static PerfStringVariable* create_string_variable(CounterNS ns,
                                                      const char* name,
                                                      int max_length,
                                                      const char *s, TRAPS);

    static PerfStringVariable* create_string_variable(CounterNS ns,
                                                      const char* name,
                                                      const char *s, TRAPS) {
      return create_string_variable(ns, name, 0, s, THREAD);
    };

    static PerfLongVariable* create_long_variable(CounterNS ns,
                                                  const char* name,
                                                  PerfData::Units u,
                                                  jlong ival, TRAPS);

    static PerfLongVariable* create_long_variable(CounterNS ns,
                                                  const char* name,
                                                  PerfData::Units u, TRAPS) {
      return create_long_variable(ns, name, u, (jlong)0, THREAD);
    };

    static PerfLongVariable* create_long_variable(CounterNS, const char* name,
                                                  PerfData::Units u,
                                                  jlong* sp, TRAPS);

    static PerfLongVariable* create_long_variable(CounterNS ns,
                                                  const char* name,
                                                  PerfData::Units u,
                                                  PerfLongSampleHelper* sh,
                                                  TRAPS);


    // Counter Types
    static PerfLongCounter* create_long_counter(CounterNS ns, const char* name,
                                                PerfData::Units u,
                                                jlong ival, TRAPS);

    static PerfLongCounter* create_long_counter(CounterNS ns, const char* name,
                                                PerfData::Units u, TRAPS) {
      return create_long_counter(ns, name, u, (jlong)0, THREAD);
    };

    static PerfLongCounter* create_long_counter(CounterNS ns, const char* name,
                                                PerfData::Units u, jlong* sp,
                                                TRAPS);

    static PerfLongCounter* create_long_counter(CounterNS ns, const char* name,
                                                PerfData::Units u,
                                                PerfLongSampleHelper* sh,
                                                TRAPS);


    // these creation methods are provided for ease of use. These allow
    // Long performance data types to be created with a shorthand syntax.

    static PerfConstant* create_constant(CounterNS ns, const char* name,
                                         PerfData::Units u, jlong val, TRAPS) {
      return create_long_constant(ns, name, u, val, THREAD);
    }

    static PerfVariable* create_variable(CounterNS ns, const char* name,
                                         PerfData::Units u, jlong ival, TRAPS) {
      return create_long_variable(ns, name, u, ival, THREAD);
    }

    static PerfVariable* create_variable(CounterNS ns, const char* name,
                                         PerfData::Units u, TRAPS) {
      return create_long_variable(ns, name, u, (jlong)0, THREAD);
    }

    static PerfVariable* create_variable(CounterNS ns, const char* name,
                                         PerfData::Units u, jlong* sp, TRAPS) {
      return create_long_variable(ns, name, u, sp, THREAD);
    }

    static PerfVariable* create_variable(CounterNS ns, const char* name,
                                         PerfData::Units u,
                                         PerfSampleHelper* sh, TRAPS) {
      return create_long_variable(ns, name, u, sh, THREAD);
    }

    static PerfCounter* create_counter(CounterNS ns, const char* name,
                                       PerfData::Units u, jlong ival, TRAPS) {
      return create_long_counter(ns, name, u, ival, THREAD);
    }

    static PerfCounter* create_counter(CounterNS ns, const char* name,
                                       PerfData::Units u, TRAPS) {
      return create_long_counter(ns, name, u, (jlong)0, THREAD);
    }

    static PerfCounter* create_counter(CounterNS ns, const char* name,
                                       PerfData::Units u, jlong* sp, TRAPS) {
      return create_long_counter(ns, name, u, sp, THREAD);
    }

    static PerfCounter* create_counter(CounterNS ns, const char* name,
                                       PerfData::Units u,
                                       PerfSampleHelper* sh, TRAPS) {
      return create_long_counter(ns, name, u, sh, THREAD);
    }

    static void destroy();
    static bool has_PerfData() { return _has_PerfData; }
};

// Useful macros to create the performance counters
#define NEWPERFTICKCOUNTER(counter, counter_ns, counter_name)  \
  {counter = PerfDataManager::create_counter(counter_ns, counter_name, \
                                             PerfData::U_Ticks,CHECK);}

#define NEWPERFEVENTCOUNTER(counter, counter_ns, counter_name)  \
  {counter = PerfDataManager::create_counter(counter_ns, counter_name, \
                                             PerfData::U_Events,CHECK);}

#define NEWPERFBYTECOUNTER(counter, counter_ns, counter_name)  \
  {counter = PerfDataManager::create_counter(counter_ns, counter_name, \
                                             PerfData::U_Bytes,CHECK);}

// Utility Classes

/*
 * this class will administer a PerfCounter used as a time accumulator
 * for a basic block much like the TraceTime class.
 *
 * Example:
 *
 *    static PerfCounter* my_time_counter = PerfDataManager::create_counter("my.time.counter", PerfData::U_Ticks, 0LL, CHECK);
 *
 *    {
 *      PerfTraceTime ptt(my_time_counter);
 *      // perform the operation you want to measure
 *    }
 *
 * Note: use of this class does not need to occur within a guarded
 * block. The UsePerfData guard is used with the implementation
 * of this class.
 */
class PerfTraceTime : public StackObj {

  protected:
    elapsedTimer _t;
    PerfLongCounter* _timerp;

  public:
    inline PerfTraceTime(PerfLongCounter* timerp) : _timerp(timerp) {
      if (!UsePerfData) return;
      _t.start();
    }

    inline void suspend() { if (!UsePerfData) return; _t.stop(); }
    inline void resume() { if (!UsePerfData) return; _t.start(); }

    ~PerfTraceTime();
};

/* The PerfTraceTimedEvent class is responsible for counting the
 * occurrence of some event and measuring the the elapsed time of
 * the event in two separate PerfCounter instances.
 *
 * Example:
 *
 *    static PerfCounter* my_time_counter = PerfDataManager::create_counter("my.time.counter", PerfData::U_Ticks, CHECK);
 *    static PerfCounter* my_event_counter = PerfDataManager::create_counter("my.event.counter", PerfData::U_Events, CHECK);
 *
 *    {
 *      PerfTraceTimedEvent ptte(my_time_counter, my_event_counter);
 *      // perform the operation you want to count and measure
 *    }
 *
 * Note: use of this class does not need to occur within a guarded
 * block. The UsePerfData guard is used with the implementation
 * of this class.
 *
 */
class PerfTraceTimedEvent : public PerfTraceTime {

  protected:
    PerfLongCounter* _eventp;

  public:
    inline PerfTraceTimedEvent(PerfLongCounter* timerp, PerfLongCounter* eventp): PerfTraceTime(timerp), _eventp(eventp) {
      if (!UsePerfData) return;
      _eventp->inc();
    }

};

#endif // SHARE_RUNTIME_PERFDATA_HPP
