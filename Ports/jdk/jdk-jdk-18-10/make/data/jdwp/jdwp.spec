/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

JDWP "Java(tm) Debug Wire Protocol"
(CommandSet VirtualMachine=1
    (Command Version=1
        "Returns the JDWP version implemented by the target VM. "
        "The version string format is implementation dependent. "
        (Out
        )
        (Reply
            (string description "Text information on the VM version")
            (int    jdwpMajor   "Major JDWP Version number")
            (int    jdwpMinor   "Minor JDWP Version number")
            (string vmVersion   "Target VM JRE version, as in the java.version property")
            (string vmName      "Target VM name, as in the java.vm.name property")
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
    (Command ClassesBySignature=2
        "Returns reference types for all the classes loaded by the target VM "
        "which match the given signature. "
        "Multple reference types will be returned if two or more class "
        "loaders have loaded a class of the same name. "
        "The search is confined to loaded classes only; no attempt is made "
        "to load a class of the given signature. "
        (Out
            (string signature "JNI signature of the class to find "
                              "(for example, \"Ljava/lang/String;\"). "
            )
        )
        (Reply
            (Repeat classes "Number of reference types that follow."
                (Group ClassInfo
                    (byte refTypeTag  "<a href=\"#JDWP_TypeTag\">Kind</a> "
                                      "of following reference type. ")
                    (referenceTypeID typeID "Matching loaded reference type")
                    (int status "The current class "
                                "<a href=\"#JDWP_ClassStatus\">status.</a> ")
                )
            )
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
    (Command AllClasses=3
        "Returns reference types for all classes currently loaded by the "
        "target VM. "
        "See <a href=\"../jvmti.html#GetLoadedClasses\">JVM TI GetLoadedClasses</a>."
        (Out
        )
        (Reply
            (Repeat classes "Number of reference types that follow."
                (Group ClassInfo
                    (byte refTypeTag  "<a href=\"#JDWP_TypeTag\">Kind</a> "
                                      "of following reference type. ")
                    (referenceTypeID typeID "Loaded reference type")
                    (string signature
                                "The JNI signature of the loaded reference type")
                    (int status "The current class "
                                "<a href=\"#JDWP_ClassStatus\">status.</a> ")
                )
            )
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
    (Command AllThreads=4
        "Returns all threads currently running in the target VM . "
        "The returned list contains threads created through "
        "java.lang.Thread, all native threads attached to "
        "the target VM through JNI, and system threads created "
        "by the target VM. Threads that have not yet been started "
        "and threads that have completed their execution are not "
        "included in the returned list. "
        (Out
        )
        (Reply
            (Repeat threads "Number of threads that follow."
                (threadObject thread "A running thread")
            )
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
    (Command TopLevelThreadGroups=5
        "Returns all thread groups that do not have a parent. This command "
        "may be used as the first step in building a tree (or trees) of the "
        "existing thread groups."
        (Out
        )
        (Reply
            (Repeat groups "Number of thread groups that follow."
                (threadGroupObject group "A top level thread group")
            )
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
    (Command Dispose=6
        "Invalidates this virtual machine mirror. "
        "The communication channel to the target VM is closed, and "
        "the target VM prepares to accept another subsequent connection "
        "from this debugger or another debugger, including the "
        "following tasks: "
        "<ul>"
        "<li>All event requests are cancelled. "
        "<li>All threads suspended by the thread-level "
        "<a href=\"#JDWP_ThreadReference_Resume\">resume</a> command "
        "or the VM-level "
        "<a href=\"#JDWP_VirtualMachine_Resume\">resume</a> command "
        "are resumed as many times as necessary for them to run. "
        "<li>Garbage collection is re-enabled in all cases where it was "
        "<a href=\"#JDWP_ObjectReference_DisableCollection\">disabled</a> "
        "</ul>"
        "Any current method invocations executing in the target VM "
        "are continued after the disconnection. Upon completion of any such "
        "method invocation, the invoking thread continues from the "
        "location where it was originally stopped. "
        "<p>"
        "Resources originating in  "
        "this VirtualMachine (ObjectReferences, ReferenceTypes, etc.) "
        "will become invalid. "
        (Out
        )
        (Reply
        )
        (ErrorSet
        )
    )
    (Command IDSizes=7
        "Returns the sizes of variably-sized data types in the target VM."
        "The returned values indicate the number of bytes used by the "
        "identifiers in command and reply packets."
        (Out
        )
        (Reply
            (int fieldIDSize "fieldID size in bytes ")
            (int methodIDSize "methodID size in bytes ")
            (int objectIDSize "objectID size in bytes ")
            (int referenceTypeIDSize "referenceTypeID size in bytes ")
            (int frameIDSize "frameID size in bytes ")
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
    (Command Suspend=8
        "Suspends the execution of the application running in the target "
        "VM. All Java threads currently running will be suspended. "
        "<p>"
        "Unlike java.lang.Thread.suspend, "
        "suspends of both the virtual machine and individual threads are "
        "counted. Before a thread will run again, it must be resumed through "
        "the <a href=\"#JDWP_VirtualMachine_Resume\">VM-level resume</a> command "
        "or the <a href=\"#JDWP_ThreadReference_Resume\">thread-level resume</a> command "
        "the same number of times it has been suspended. "
        (Out
        )
        (Reply
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
    (Command Resume=9
        "Resumes execution of the application after the suspend "
        "command or an event has stopped it. "
        "Suspensions of the Virtual Machine and individual threads are "
        "counted. If a particular thread is suspended n times, it must "
        "resumed n times before it will continue. "
        (Out
        )
        (Reply
        )
        (ErrorSet
        )
    )
    (Command Exit=10
        "Terminates the target VM with the given exit code. "
        "On some platforms, the exit code might be truncated, for "
        "example, to the low order 8 bits. "
        "All ids previously returned from the target VM become invalid. "
        "Threads running in the VM are abruptly terminated. "
        "A thread death exception is not thrown and "
        "finally blocks are not run."
        (Out
            (int exitCode "the exit code")
        )
        (Reply
        )
        (ErrorSet
        )
    )
    (Command CreateString=11
        "Creates a new string object in the target VM and returns "
        "its id. "
        (Out
            (string utf "UTF-8 characters to use in the created string. ")
        )
        (Reply
            (stringObject stringObject
                "Created string (instance of java.lang.String) ")
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
    (Command Capabilities=12
        "Retrieve this VM's capabilities. The capabilities are returned "
        "as booleans, each indicating the presence or absence of a "
        "capability. The commands associated with each capability will "
        "return the NOT_IMPLEMENTED error if the cabability is not "
        "available."
        (Out
        )
        (Reply
            (boolean canWatchFieldModification
                     "Can the VM watch field modification, and therefore "
                     "can it send the Modification Watchpoint Event?")
            (boolean canWatchFieldAccess
                     "Can the VM watch field access, and therefore "
                     "can it send the Access Watchpoint Event?")
            (boolean canGetBytecodes
                     "Can the VM get the bytecodes of a given method? ")
            (boolean canGetSyntheticAttribute
                     "Can the VM determine whether a field or method is "
                     "synthetic? (that is, can the VM determine if the "
                     "method or the field was invented by the compiler?) ")
            (boolean canGetOwnedMonitorInfo
                     "Can the VM get the owned monitors infornation for "
                     "a thread?")
            (boolean canGetCurrentContendedMonitor
                     "Can the VM get the current contended monitor of a thread?")
            (boolean canGetMonitorInfo
                     "Can the VM get the monitor information for a given object? ")
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
    (Command ClassPaths=13
        "Retrieve the classpath and bootclasspath of the target VM. "
        "If the classpath is not defined, returns an empty list. If the "
        "bootclasspath is not defined returns an empty list."
        (Out
        )
        (Reply
            (string baseDir "Base directory used to resolve relative "
                            "paths in either of the following lists.")
            (Repeat classpaths "Number of paths in classpath."
                (string path "One component of classpath") )
            (Repeat bootclasspaths "Number of paths in bootclasspath."
                (string path "One component of bootclasspath") )
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
    (Command DisposeObjects=14
        "Releases a list of object IDs. For each object in the list, the "
        "following applies. "
        "The count of references held by the back-end (the reference "
        "count) will be decremented by refCnt. "
        "If thereafter the reference count is less than "
        "or equal to zero, the ID is freed. "
        "Any back-end resources associated with the freed ID may "
        "be freed, and if garbage collection was "
        "disabled for the object, it will be re-enabled. "
        "The sender of this command "
        "promises that no further commands will be sent "
        "referencing a freed ID.
        "<p>"
        "Use of this command is not required. If it is not sent, "
        "resources associated with each ID will be freed by the back-end "
        "at some time after the corresponding object is garbage collected. "
        "It is most useful to use this command to reduce the load on the "
        "back-end if a very large number of "
        "objects has been retrieved from the back-end (a large array, "
        "for example) but may not be garbage collected any time soon. "
        "<p>"
        "IDs may be re-used by the back-end after they "
        "have been freed with this command."
        "This description assumes reference counting, "
        "a back-end may use any implementation which operates "
        "equivalently. "
        (Out
            (Repeat requests "Number of object dispose requests that follow"
                (Group Request
                    (object object "The object ID")
                    (int refCnt "The number of times this object ID has been "
                                "part of a packet received from the back-end. "
                                "An accurate count prevents the object ID "
                                "from being freed on the back-end if "
                                "it is part of an incoming packet, not yet "
                                "handled by the front-end.")
                )
            )
        )
        (Reply
        )
        (ErrorSet
        )
    )
    (Command HoldEvents=15
        "Tells the target VM to stop sending events. Events are not discarded; "
        "they are held until a subsequent ReleaseEvents command is sent. "
        "This command is useful to control the number of events sent "
        "to the debugger VM in situations where very large numbers of events "
        "are generated. "
        "While events are held by the debugger back-end, application "
        "execution may be frozen by the debugger back-end to prevent "
        "buffer overflows on the back end.
        "Responses to commands are never held and are not affected by this
        "command. If events are already being held, this command is "
        "ignored."
        (Out
        )
        (Reply
        )
        (ErrorSet
        )
    )
    (Command ReleaseEvents=16
        "Tells the target VM to continue sending events. This command is "
        "used to restore normal activity after a HoldEvents command. If "
        "there is no current HoldEvents command in effect, this command is "
        "ignored."
        (Out
        )
        (Reply
        )
        (ErrorSet
        )
    )
    (Command CapabilitiesNew=17
        "Retrieve all of this VM's capabilities. The capabilities are returned "
        "as booleans, each indicating the presence or absence of a "
        "capability. The commands associated with each capability will "
        "return the NOT_IMPLEMENTED error if the cabability is not "
        "available."
        "Since JDWP version 1.4."
        (Out
        )
        (Reply
            (boolean canWatchFieldModification
                     "Can the VM watch field modification, and therefore "
                     "can it send the Modification Watchpoint Event?")
            (boolean canWatchFieldAccess
                     "Can the VM watch field access, and therefore "
                     "can it send the Access Watchpoint Event?")
            (boolean canGetBytecodes
                     "Can the VM get the bytecodes of a given method? ")
            (boolean canGetSyntheticAttribute
                     "Can the VM determine whether a field or method is "
                     "synthetic? (that is, can the VM determine if the "
                     "method or the field was invented by the compiler?) ")
            (boolean canGetOwnedMonitorInfo
                     "Can the VM get the owned monitors infornation for "
                     "a thread?")
            (boolean canGetCurrentContendedMonitor
                     "Can the VM get the current contended monitor of a thread?")
            (boolean canGetMonitorInfo
                     "Can the VM get the monitor information for a given object? ")
            (boolean canRedefineClasses
                     "Can the VM redefine classes?")
            (boolean canAddMethod
                     "Can the VM add methods when redefining classes? "
                     "<p>@Deprecated(since=\"15\") A JVM TI based JDWP back-end "
                     "will never set this capability to true.")
            (boolean canUnrestrictedlyRedefineClasses
                     "Can the VM redefine classes "
                     "in ways that are normally restricted?"
                     "<p>@Deprecated(since=\"15\") A JVM TI based JDWP back-end "
                     "will never set this capability to true.")
            (boolean canPopFrames
                     "Can the VM pop stack frames?")
            (boolean canUseInstanceFilters
                     "Can the VM filter events by specific object?")
            (boolean canGetSourceDebugExtension
                     "Can the VM get the source debug extension?")
            (boolean canRequestVMDeathEvent
                     "Can the VM request VM death events?")
            (boolean canSetDefaultStratum
                     "Can the VM set a default stratum?")
            (boolean canGetInstanceInfo
                     "Can the VM return instances, counts of instances of classes "
                     "and referring objects?")
            (boolean canRequestMonitorEvents
                     "Can the VM request monitor events?")
            (boolean canGetMonitorFrameInfo
                     "Can the VM get monitors with frame depth info?")
            (boolean canUseSourceNameFilters
                     "Can the VM filter class prepare events by source name?")
            (boolean canGetConstantPool
                     "Can the VM return the constant pool information?")
            (boolean canForceEarlyReturn
                     "Can the VM force early return from a method?")
            (boolean reserved22
                     "Reserved for future capability")
            (boolean reserved23
                     "Reserved for future capability")
            (boolean reserved24
                     "Reserved for future capability")
            (boolean reserved25
                     "Reserved for future capability")
            (boolean reserved26
                     "Reserved for future capability")
            (boolean reserved27
                     "Reserved for future capability")
            (boolean reserved28
                     "Reserved for future capability")
            (boolean reserved29
                     "Reserved for future capability")
            (boolean reserved30
                     "Reserved for future capability")
            (boolean reserved31
                     "Reserved for future capability")
            (boolean reserved32
                     "Reserved for future capability")
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
    (Command RedefineClasses=18
        "Installs new class definitions. "
        "If there are active stack frames in methods of the redefined classes in the "
        "target VM then those active frames continue to run the bytecodes of the "
        "original method. These methods are considered obsolete - see "
        "<a href=\"#JDWP_Method_IsObsolete\">IsObsolete</a>. The methods in the "
        "redefined classes will be used for new invokes in the target VM. "
        "The original method ID refers to the redefined method. "
        "All breakpoints in the redefined classes are cleared."
        "If resetting of stack frames is desired, the "
        "<a href=\"#JDWP_StackFrame_PopFrames\">PopFrames</a> command can be used "
        "to pop frames with obsolete methods."
        "<p>"
        "Unless the canUnrestrictedlyRedefineClasses capability is present "
        "the redefinition must follow the restrictions described in "
        "<a href=\"../jvmti.html#RedefineClasses\">JVM TI RedefineClasses</a>."
        "<p>"
        "Requires canRedefineClasses capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>. "
        "<p>@Deprecated(since=\"15\")  "
        "In addition to the canRedefineClasses capability, the target VM must "
        "have the canAddMethod capability to add methods when redefining classes, "
        "or the canUnrestrictedlyRedefineClasses capability to redefine classes in ways "
        "that are normally restricted."
        (Out
            (Repeat classes "Number of reference types that follow."
                (Group ClassDef
                    (referenceType refType "The reference type.")
                    (Repeat classfile "Number of bytes defining class (below)"
                         (byte classbyte "byte in JVM class file "
                                         "format.")
                    )
                )
            )
        )
        (Reply
        )
        (ErrorSet
            (Error INVALID_CLASS    "One of the refTypes is not the ID of a reference "
                                    "type.")
            (Error INVALID_OBJECT   "One of the refTypes is not a known ID.")
            (Error UNSUPPORTED_VERSION)
            (Error INVALID_CLASS_FORMAT)
            (Error CIRCULAR_CLASS_DEFINITION)
            (Error FAILS_VERIFICATION)
            (Error NAMES_DONT_MATCH)
            (Error NOT_IMPLEMENTED  "No aspect of this functionality is implemented "
                                    "(CapabilitiesNew.canRedefineClasses is false)")
            (Error ADD_METHOD_NOT_IMPLEMENTED)
            (Error SCHEMA_CHANGE_NOT_IMPLEMENTED)
            (Error HIERARCHY_CHANGE_NOT_IMPLEMENTED)
            (Error DELETE_METHOD_NOT_IMPLEMENTED)
            (Error CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED)
            (Error METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED)
            (Error CLASS_ATTRIBUTE_CHANGE_NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
    (Command SetDefaultStratum=19
        "Set the default stratum. Requires canSetDefaultStratum capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."
        (Out
            (string stratumID "default stratum, or empty string to use "
                              "reference type default.")
        )
        (Reply
        )
        (ErrorSet
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
    (Command AllClassesWithGeneric=20
        "Returns reference types for all classes currently loaded by the "
        "target VM.  "
        "Both the JNI signature and the generic signature are "
        "returned for each class.  "
        "Generic signatures are described in the signature attribute "
        "section in "
        "<cite>The Java Virtual Machine Specification</cite>. "
        "Since JDWP version 1.5."
        (Out
        )
        (Reply
            (Repeat classes "Number of reference types that follow."
                (Group ClassInfo
                    (byte refTypeTag  "<a href=\"#JDWP_TypeTag\">Kind</a> "
                                      "of following reference type. ")
                    (referenceTypeID typeID "Loaded reference type")
                    (string signature
                                "The JNI signature of the loaded reference type.")
                    (string genericSignature
                                "The generic signature of the loaded reference type "
                                "or an empty string if there is none.")
                    (int status "The current class "
                                "<a href=\"#JDWP_ClassStatus\">status.</a> ")
                )
            )
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )

    (Command InstanceCounts=21
        "Returns the number of instances of each reference type in the input list. "
        "Only instances that are reachable for the purposes of "
        "garbage collection are counted.  If a reference type is invalid, "
        "eg. it has been unloaded, zero is returned for its instance count."
        "<p>Since JDWP version 1.6. Requires canGetInstanceInfo capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."
        (Out
            (Repeat refTypesCount "Number of reference types that follow.    Must be non-negative."
                (referenceType refType "A reference type ID.")
            )
          )
        (Reply
            (Repeat counts "The number of counts that follow."
              (long instanceCount "The number of instances for the corresponding reference type "
                                  "in 'Out Data'.")
            )
        )
        (ErrorSet
            (Error ILLEGAL_ARGUMENT   "refTypesCount is less than zero.")
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
    (Command AllModules=22
        "Returns all modules in the target VM."
        "<p>Since JDWP version 9."
        (Out
        )
        (Reply
            (Repeat modules "The number of the modules that follow."
                (moduleID module "One of the modules.")
            )
        )
        (ErrorSet
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
)

(CommandSet ReferenceType=2
    (Command Signature=1
        "Returns the type signature of a reference type. "
        "Type signature formats are the same as specified in "
        "<a href=\"../jvmti.html#GetClassSignature\">JVM TI GetClassSignature</a>."
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (string signature
                "The JNI signature for the reference type.")
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command ClassLoader=2
        "Returns the instance of java.lang.ClassLoader which loaded "
        "a given reference type. If the reference type was loaded by the "
        "system class loader, the returned object ID is null."
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (classLoaderObject classLoader "The class loader for the reference type. ")
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command Modifiers=3
        "Returns the modifiers (also known as access flags) for a reference type. "
        "The returned bit mask contains information on the declaration "
        "of the reference type. If the reference type is an array or "
        "a primitive class (for example, java.lang.Integer.TYPE), the "
        "value of the returned bit mask is undefined."
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (int modBits "Modifier bits as defined in Chapter 4 of "
                         "<cite>The Java Virtual Machine Specification</cite>")
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command Fields=4
        "Returns information for each field in a reference type. "
        "Inherited fields are not included. "
        "The field list will include any synthetic fields created "
        "by the compiler. "
        "Fields are returned in the order they occur in the class file."
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (Repeat declared "Number of declared fields."
                (Group FieldInfo
                    (field fieldID "Field ID.")
                    (string name "Name of field.")
                    (string signature "JNI Signature of field.")
                    (int modBits "The modifier bit flags (also known as access flags) "
                                 "which provide additional information on the  "
                                 "field declaration. Individual flag values are "
                                 "defined in Chapter 4 of "
                                 "<cite>The Java Virtual Machine Specification</cite>. "
                                 "In addition, The <code>0xf0000000</code> bit identifies "
                                 "the field as synthetic, if the synthetic attribute "
                                 "<a href=\"#JDWP_VirtualMachine_Capabilities\">capability</a> is available.")
                )
            )
        )
        (ErrorSet
            (Error CLASS_NOT_PREPARED)
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command Methods=5
        "Returns information for each method in a reference type. "
        "Inherited methods are not included. The list of methods will "
        "include constructors (identified with the name \"&lt;init&gt;\"), "
        "the initialization method (identified with the name \"&lt;clinit&gt;\") "
        "if present, and any synthetic methods created by the compiler. "
        "Methods are returned in the order they occur in the class file."
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (Repeat declared "Number of declared methods."
                (Group MethodInfo
                    (method methodID "Method ID.")
                    (string name "Name of method.")
                    (string signature "JNI signature of method.")
                    (int modBits "The modifier bit flags (also known as access flags) "
                                 "which provide additional information on the  "
                                 "method declaration. Individual flag values are "
                                 "defined in Chapter 4 of "
                                 "<cite>The Java Virtual Machine Specification</cite>. "
                                 "In addition, The <code>0xf0000000</code> bit identifies "
                                 "the method as synthetic, if the synthetic attribute "
                                 "<a href=\"#JDWP_VirtualMachine_Capabilities\">capability</a> is available.")
                )
            )
        )
        (ErrorSet
            (Error CLASS_NOT_PREPARED)
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command GetValues=6
        "Returns the value of one or more static fields of the "
        "reference type. Each field must be member of the reference type "
        "or one of its superclasses, superinterfaces, or implemented interfaces. "
        "Access control is not enforced; for example, the values of private "
        "fields can be obtained."
        (Out
            (referenceType refType "The reference type ID.")
            (Repeat fields "The number of values to get"
                (Group Field
                    (field fieldID "A field to get")
                )
            )
        )
        (Reply
            (Repeat values "The number of values returned, always equal to fields, "
                           "the number of values to get."
                (value value "The field value")
            )
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error INVALID_FIELDID)
            (Error VM_DEAD)
        )
    )
    (Command SourceFile=7
        "Returns the name of source file in which a reference type was "
        "declared. "
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (string sourceFile "The source file name. No path information "
                               "for the file is included")
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error ABSENT_INFORMATION "The source file attribute is absent.")
            (Error VM_DEAD)
        )
    )
    (Command NestedTypes=8
        "Returns the classes and interfaces directly nested within this type."
        "Types further nested within those types are not included. "
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (Repeat classes "The number of nested classes and interfaces"
                (Group TypeInfo
                    (byte refTypeTag  "<a href=\"#JDWP_TypeTag\">Kind</a> "
                                      "of following reference type. ")
                    (referenceTypeID typeID "The nested class or interface ID.")
                )
            )
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command Status=9
        "Returns the current status of the reference type. The status "
        "indicates the extent to which the reference type has been "
        "initialized, as described in section 2.1.6 of "
        "<cite>The Java Virtual Machine Specification</cite>. "
        "If the class is linked the PREPARED and VERIFIED bits in the returned status bits "
        "will be set. If the class is initialized the INITIALIZED bit in the returned "
        "status bits will be set. If an error occured during initialization then the "
        "ERROR bit in the returned status bits will be set. "
        "The returned status bits are undefined for array types and for "
        "primitive classes (such as java.lang.Integer.TYPE). "
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (int status "<a href=\"#JDWP_ClassStatus\">Status</a> bits:"
                        "See <a href=\"#JDWP_ClassStatus\">JDWP.ClassStatus</a>")
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command Interfaces=10
        "Returns the interfaces declared as implemented by this class. "
        "Interfaces indirectly implemented (extended by the implemented "
        "interface or implemented by a superclass) are not included."
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (Repeat interfaces "The number of implemented interfaces"
                (interfaceType interfaceType "implemented interface.")
            )
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command ClassObject=11
        "Returns the class object corresponding to this type. "
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (classObject classObject "class object.")
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command SourceDebugExtension=12
        "Returns the value of the SourceDebugExtension attribute. "
        "Since JDWP version 1.4. Requires canGetSourceDebugExtension capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (string extension "extension attribute")
        )
        (ErrorSet
            (Error INVALID_CLASS      "refType is not the ID of a reference "
                                      "type.")
            (Error INVALID_OBJECT     "refType is not a known ID.")
            (Error ABSENT_INFORMATION "If the extension is not specified.")
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
    (Command SignatureWithGeneric=13
        "Returns the JNI signature of a reference type along with the "
        "generic signature if there is one.  "
        "Generic signatures are described in the signature attribute "
        "section in "
        "<cite>The Java Virtual Machine Specification</cite>. "
        "Since JDWP version 1.5."
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (string signature
                "The JNI signature for the reference type.")
            (string genericSignature
                "The generic signature for the reference type or an empty "
                "string if there is none.")
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command FieldsWithGeneric=14
        "Returns information, including the generic signature if any, "
        "for each field in a reference type. "
        "Inherited fields are not included. "
        "The field list will include any synthetic fields created "
        "by the compiler. "
        "Fields are returned in the order they occur in the class file.  "
        "Generic signatures are described in the signature attribute "
        "section in "
        "<cite>The Java Virtual Machine Specification</cite>. "
        "Since JDWP version 1.5."
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (Repeat declared "Number of declared fields."
                (Group FieldInfo
                    (field fieldID "Field ID.")
                    (string name "The name of the field.")
                    (string signature "The JNI signature of the field.")
                    (string genericSignature "The generic signature of the "
                                             "field, or an empty string if there is none.")
                    (int modBits "The modifier bit flags (also known as access flags) "
                                 "which provide additional information on the  "
                                 "field declaration. Individual flag values are "
                                 "defined in Chapter 4 of "
                                 "<cite>The Java Virtual Machine Specification</cite>. "
                                 "In addition, The <code>0xf0000000</code> bit identifies "
                                 "the field as synthetic, if the synthetic attribute "
                                 "<a href=\"#JDWP_VirtualMachine_Capabilities\">capability</a> is available.")
                )
            )
        )
        (ErrorSet
            (Error CLASS_NOT_PREPARED)
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command MethodsWithGeneric=15
        "Returns information, including the generic signature if any, "
        "for each method in a reference type. "
        "Inherited methodss are not included. The list of methods will "
        "include constructors (identified with the name \"&lt;init&gt;\"), "
        "the initialization method (identified with the name \"&lt;clinit&gt;\") "
        "if present, and any synthetic methods created by the compiler. "
        "Methods are returned in the order they occur in the class file.  "
        "Generic signatures are described in the signature attribute "
        "section in "
        "<cite>The Java Virtual Machine Specification</cite>. "
        "Since JDWP version 1.5."
        (Out
            (referenceType refType "The reference type ID.")
        )
        (Reply
            (Repeat declared "Number of declared methods."
                (Group MethodInfo
                    (method methodID "Method ID.")
                    (string name "The name of the method.")
                    (string signature "The JNI signature of the method.")
                    (string genericSignature "The generic signature of the method, or "
                                             "an empty string if there is none.")
                    (int modBits "The modifier bit flags (also known as access flags) "
                                 "which provide additional information on the  "
                                 "method declaration. Individual flag values are "
                                 "defined in Chapter 4 of "
                                 "<cite>The Java Virtual Machine Specification</cite>. "
                                 "In addition, The <code>0xf0000000</code> bit identifies "
                                 "the method as synthetic, if the synthetic attribute "
                                 "<a href=\"#JDWP_VirtualMachine_Capabilities\">capability</a> is available.")
                )
            )
        )
        (ErrorSet
            (Error CLASS_NOT_PREPARED)
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command Instances=16
        "Returns instances of this reference type. "
        "Only instances that are reachable for the purposes of "
        "garbage collection are returned. "
        "<p>Since JDWP version 1.6. Requires canGetInstanceInfo capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."
        (Out
            (referenceType refType "The reference type ID.")
            (int maxInstances "Maximum number of instances to return.  Must be non-negative. "
                              "If zero, all instances are returned.")
        )
        (Reply
            (Repeat instances "The number of instances that follow."
                 (tagged-object instance "An instance of this reference type.")
             )
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error ILLEGAL_ARGUMENT  "maxInstances is less than zero.")
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
    (Command ClassFileVersion=17
        "Returns the class file major and minor version numbers, as defined in the class "
        "file format of the Java Virtual Machine specification. "
         "<p>Since JDWP version 1.6. "
        (Out
            (referenceType refType "The class.")
        )
        (Reply
            (int majorVersion "Major version number")
            (int minorVersion "Minor version number")
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error ABSENT_INFORMATION "The class file version information is "
                                      "absent for primitive and array types.")
            (Error VM_DEAD)
        )
    )
    (Command ConstantPool=18
        "Return the raw bytes of the constant pool in the format of the "
        "constant_pool item of the Class File Format in "
        "<cite>The Java Virtual Machine Specification</cite>. "
        "<p>Since JDWP version 1.6. Requires canGetConstantPool capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>.""
        (Out
            (referenceType refType "The class.")
        )
        (Reply
            (int count "Total number of constant pool entries plus one. This "
                       "corresponds to the constant_pool_count item of the "
                       "Class File Format in "
                       "<cite>The Java Virtual Machine Specification</cite>. ")
            (Repeat bytes
                (byte cpbytes "Raw bytes of constant pool")
            )
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error NOT_IMPLEMENTED   "If the target virtual machine does not "
                                     "support the retrieval of constant pool information.")
            (Error ABSENT_INFORMATION "The Constant Pool information is "
                                      "absent for primitive and array types.")
            (Error VM_DEAD)
        )
    )
    (Command Module=19
        "Returns the module that this reference type belongs to."
        "<p>Since JDWP version 9."
        (Out
            (referenceType refType "The reference type.")
        )
        (Reply
            (moduleID module "The module this reference type belongs to.")
        )
        (ErrorSet
            (Error INVALID_CLASS   "refType is not the ID of a reference type.")
            (Error INVALID_OBJECT  "refType is not a known ID.")
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
)
(CommandSet ClassType=3
    (Command Superclass=1
        "Returns the immediate superclass of a class."
        (Out
            (classType clazz "The class type ID.")
        )
        (Reply
            (classType superclass
                "The superclass (null if the class ID for java.lang.Object is specified).")
        )
        (ErrorSet
            (Error INVALID_CLASS     "clazz is not the ID of a class.")
            (Error INVALID_OBJECT    "clazz is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command SetValues=2
        "Sets the value of one or more static fields. "
        "Each field must be member of the class type "
        "or one of its superclasses, superinterfaces, or implemented interfaces. "
        "Access control is not enforced; for example, the values of private "
        "fields can be set. Final fields cannot be set."
        "For primitive values, the value's type must match the "
        "field's type exactly. For object values, there must exist a "
        "widening reference conversion from the value's type to the
        "field's type and the field's type must be loaded. "
        (Out
            (classType clazz "The class type ID.")
            (Repeat values "The number of fields to set."
                (Group FieldValue "A Field/Value pair."
                    (field fieldID "Field to set.")
                    (untagged-value value "Value to put in the field.")
                )
            )
        )
        (Reply "none"
        )
        (ErrorSet
            (Error INVALID_CLASS     "clazz is not the ID of a class.")
            (Error CLASS_NOT_PREPARED)
            (Error INVALID_OBJECT    "clazz is not a known ID or a value of an "
                                     "object field is not a known ID.")
            (Error INVALID_FIELDID)
            (Error VM_DEAD)
        )
    )
    (Command InvokeMethod=3
        "Invokes a static method. "
        "The method must be member of the class type "
        "or one of its superclasses. "
        "Access control is not enforced; for example, private "
        "methods can be invoked."
        "<p>"
        "The method invocation will occur in the specified thread. "
        "Method invocation can occur only if the specified thread "
        "has been suspended by an event. "
        "Method invocation is not supported "
        "when the target VM has been suspended by the front-end. "
        "<p>"
        "The specified method is invoked with the arguments in the specified "
        "argument list. "
        "The method invocation is synchronous; the reply packet is not "
        "sent until the invoked method returns in the target VM. "
        "The return value (possibly the void value) is "
        "included in the reply packet. "
        "If the invoked method throws an exception, the "
        "exception object ID is set in the reply packet; otherwise, the "
        "exception object ID is null. "
        "<p>"
        "For primitive arguments, the argument value's type must match the "
        "argument's type exactly. For object arguments, there must exist a "
        "widening reference conversion from the argument value's type to the "
        "argument's type and the argument's type must be loaded. "
        "<p>"
        "By default, all threads in the target VM are resumed while "
        "the method is being invoked if they were previously "
        "suspended by an event or by command. "
        "This is done to prevent the deadlocks "
        "that will occur if any of the threads own monitors "
        "that will be needed by the invoked method. It is possible that "
        "breakpoints or other events might occur during the invocation. "
        "Note, however, that this implicit resume acts exactly like "
        "the ThreadReference resume command, so if the thread's suspend "
        "count is greater than 1, it will remain in a suspended state "
        "during the invocation. By default, when the invocation completes, "
        "all threads in the target VM are suspended, regardless their state "
        "before the invocation. "
        "<p>"
        "The resumption of other threads during the invoke can be prevented "
        "by specifying the INVOKE_SINGLE_THREADED "
        "bit flag in the <code>options</code> field; however, "
        "there is no protection against or recovery from the deadlocks "
        "described above, so this option should be used with great caution. "
        "Only the specified thread will be resumed (as described for all "
        "threads above). Upon completion of a single threaded invoke, the invoking thread "
        "will be suspended once again. Note that any threads started during "
        "the single threaded invocation will not be suspended when the "
        "invocation completes. "
        "<p>"
        "If the target VM is disconnected during the invoke (for example, through "
        "the VirtualMachine dispose command) the method invocation continues. "
        (Out
            (classType clazz "The class type ID.")
            (threadObject thread "The thread in which to invoke.")
            (method methodID "The method to invoke.")
            (Repeat arguments
                (value arg "The argument value.")
            )
            (int options "Invocation <a href=\"#JDWP_InvokeOptions\">options</a>")
        )
        (Reply
            (value returnValue "The returned value.")
            (tagged-object exception "The thrown exception.")
        )
        (ErrorSet
            (Error INVALID_CLASS     "clazz is not the ID of a class.")
            (Error INVALID_OBJECT    "clazz is not a known ID.")
            (Error INVALID_METHODID  "methodID is not the ID of a static method in "
                                     "this class type or one of its superclasses.")
            (Error INVALID_THREAD)
            (Error THREAD_NOT_SUSPENDED)
            (Error VM_DEAD)
        )
    )
    (Command NewInstance=4
        "Creates a new object of this type, invoking the specified "
        "constructor. The constructor method ID must be a member of "
        "the class type."
        "<p>"
        "Instance creation will occur in the specified thread. "
        "Instance creation can occur only if the specified thread "
        "has been suspended by an event. "
        "Method invocation is not supported "
        "when the target VM has been suspended by the front-end. "
        "<p>"
        "The specified constructor is invoked with the arguments in the specified "
        "argument list. "
        "The constructor invocation is synchronous; the reply packet is not "
        "sent until the invoked method returns in the target VM. "
        "The return value (possibly the void value) is "
        "included in the reply packet. "
        "If the constructor throws an exception, the "
        "exception object ID is set in the reply packet; otherwise, the "
        "exception object ID is null. "
        "<p>"
        "For primitive arguments, the argument value's type must match the "
        "argument's type exactly. For object arguments, there must exist a "
        "widening reference conversion from the argument value's type to the "
        "argument's type and the argument's type must be loaded. "
        "<p>"
        "By default, all threads in the target VM are resumed while "
        "the method is being invoked if they were previously "
        "suspended by an event or by command. "
        "This is done to prevent the deadlocks "
        "that will occur if any of the threads own monitors "
        "that will be needed by the invoked method. It is possible that "
        "breakpoints or other events might occur during the invocation. "
        "Note, however, that this implicit resume acts exactly like "
        "the ThreadReference resume command, so if the thread's suspend "
        "count is greater than 1, it will remain in a suspended state "
        "during the invocation. By default, when the invocation completes, "
        "all threads in the target VM are suspended, regardless their state "
        "before the invocation. "
        "<p>"
        "The resumption of other threads during the invoke can be prevented "
        "by specifying the INVOKE_SINGLE_THREADED "
        "bit flag in the <code>options</code> field; however, "
        "there is no protection against or recovery from the deadlocks "
        "described above, so this option should be used with great caution. "
        "Only the specified thread will be resumed (as described for all "
        "threads above). Upon completion of a single threaded invoke, the invoking thread "
        "will be suspended once again. Note that any threads started during "
        "the single threaded invocation will not be suspended when the "
        "invocation completes. "
        "<p>"
        "If the target VM is disconnected during the invoke (for example, through "
        "the VirtualMachine dispose command) the method invocation continues. "
        (Out
            (classType clazz "The class type ID.")
            (threadObject thread "The thread in which to invoke the constructor.")
            (method methodID "The constructor to invoke.")
            (Repeat arguments
                (value arg "The argument value.")
            )
            (int options "Constructor invocation <a href=\"#JDWP_InvokeOptions\">options</a>")
        )
        (Reply
            (tagged-object newObject "The newly created object, or null "
                                     "if the constructor threw an exception.")
            (tagged-object exception "The thrown exception, if any; otherwise, null.")
        )
        (ErrorSet
            (Error INVALID_CLASS     "clazz is not the ID of a class.")
            (Error INVALID_OBJECT    "clazz is not a known ID or a value of an "
                                     "object parameter is not a known ID..")
            (Error INVALID_METHODID  "methodID is not the ID of a method.")
            (Error INVALID_OBJECT)
            (Error INVALID_THREAD)
            (Error THREAD_NOT_SUSPENDED)
            (Error VM_DEAD)
        )
    )
)
(CommandSet ArrayType=4
    (Command NewInstance=1
        "Creates a new array object of this type with a given length."
        (Out
            (arrayType arrType "The array type of the new instance.")
            (int length "The length of the array.")
        )
        (Reply
            (tagged-object newArray "The newly created array object. ")
        )
        (ErrorSet
            (Error INVALID_ARRAY)
            (Error INVALID_OBJECT)
            (Error VM_DEAD)
        )
    )
)
(CommandSet InterfaceType=5
    (Command InvokeMethod=1
        "Invokes a static method. "
        "The method must not be a static initializer. "
        "The method must be a member of the interface type. "
        "<p>Since JDWP version 1.8 "
        "<p>"
        "The method invocation will occur in the specified thread. "
        "Method invocation can occur only if the specified thread "
        "has been suspended by an event. "
        "Method invocation is not supported "
        "when the target VM has been suspended by the front-end. "
        "<p>"
        "The specified method is invoked with the arguments in the specified "
        "argument list. "
        "The method invocation is synchronous; the reply packet is not "
        "sent until the invoked method returns in the target VM. "
        "The return value (possibly the void value) is "
        "included in the reply packet. "
        "If the invoked method throws an exception, the "
        "exception object ID is set in the reply packet; otherwise, the "
        "exception object ID is null. "
        "<p>"
        "For primitive arguments, the argument value's type must match the "
        "argument's type exactly. For object arguments, there must exist a "
        "widening reference conversion from the argument value's type to the "
        "argument's type and the argument's type must be loaded. "
        "<p>"
        "By default, all threads in the target VM are resumed while "
        "the method is being invoked if they were previously "
        "suspended by an event or by a command. "
        "This is done to prevent the deadlocks "
        "that will occur if any of the threads own monitors "
        "that will be needed by the invoked method. It is possible that "
        "breakpoints or other events might occur during the invocation. "
        "Note, however, that this implicit resume acts exactly like "
        "the ThreadReference resume command, so if the thread's suspend "
        "count is greater than 1, it will remain in a suspended state "
        "during the invocation. By default, when the invocation completes, "
        "all threads in the target VM are suspended, regardless their state "
        "before the invocation. "
        "<p>"
        "The resumption of other threads during the invoke can be prevented "
        "by specifying the INVOKE_SINGLE_THREADED "
        "bit flag in the <code>options</code> field; however, "
        "there is no protection against or recovery from the deadlocks "
        "described above, so this option should be used with great caution. "
        "Only the specified thread will be resumed (as described for all "
        "threads above). Upon completion of a single threaded invoke, the invoking thread "
        "will be suspended once again. Note that any threads started during "
        "the single threaded invocation will not be suspended when the "
        "invocation completes. "
        "<p>"
        "If the target VM is disconnected during the invoke (for example, through "
        "the VirtualMachine dispose command) the method invocation continues. "
        (Out
            (interfaceType clazz "The interface type ID.")
            (threadObject thread "The thread in which to invoke.")
            (method methodID "The method to invoke.")
            (Repeat arguments
                (value arg "The argument value.")
            )
            (int options "Invocation <a href=\"#JDWP_InvokeOptions\">options</a>")
        )
        (Reply
            (value returnValue "The returned value.")
            (tagged-object exception "The thrown exception.")
        )
        (ErrorSet
            (Error INVALID_CLASS     "clazz is not the ID of an interface.")
            (Error INVALID_OBJECT    "clazz is not a known ID.")
            (Error INVALID_METHODID  "methodID is not the ID of a static method in this "
                                     "interface type or is the ID of a static initializer.")
            (Error INVALID_THREAD)
            (Error THREAD_NOT_SUSPENDED)
            (Error VM_DEAD)
        )
    )
)
(CommandSet Method=6
    (Command LineTable=1
        "Returns line number information for the method, if present. "
        "The line table maps source line numbers to the initial code index "
        "of the line. The line table "
        "is ordered by code index (from lowest to highest). The line number "
        "information is constant unless a new class definition is installed "
        "using <a href=\"#JDWP_VirtualMachine_RedefineClasses\">RedefineClasses</a>."
        (Out
            (referenceType refType "The class.")
            (method methodID "The method.")
        )
        (Reply
            (long start "Lowest valid code index for the method, >=0, or -1 if the method is native ")
            (long end "Highest valid code index for the method, >=0, or -1 if the method is native")
            (Repeat lines "The number of entries in the line table for this method."
                (Group LineInfo
                    (long lineCodeIndex "Initial code index of the line, "
                                        "start &lt;= lineCodeIndex &lt; end")
                    (int lineNumber "Line number.")
                )
            )
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error INVALID_METHODID  "methodID is not the ID of a method.")
            (Error VM_DEAD)
        )
    )
    (Command VariableTable=2
        "Returns variable information for the method. The variable table "
        "includes arguments and locals declared within the method. For "
        "instance methods, the \"this\" reference is included in the "
        "table. Also, synthetic variables may be present. "
        (Out
            (referenceType refType "The class.")
            (method methodID "The method.")
        )
        (Reply
            (int argCnt "The number of words in the frame used by arguments. "
                        "Eight-byte arguments use two words; all others use one. ")
            (Repeat slots "The number of variables."
                (Group SlotInfo "Information about the variable."
                    (long codeIndex
                        "First code index at which the variable is visible (unsigned). "
                        "Used in conjunction with <code>length</code>. "
                        "The variable can be get or set only when the current "
                        "<code>codeIndex</code> &lt;= current frame code index &lt; <code>codeIndex + length</code> ")
                    (string name "The variable's name.")
                    (string signature "The variable type's JNI signature.")
                    (int length
                        "Unsigned value used in conjunction with <code>codeIndex</code>. "
                        "The variable can be get or set only when the current "
                        "<code>codeIndex</code> &lt;= current frame code index &lt; <code>code index + length</code> ")
                    (int slot "The local variable's index in its frame")
                )
            )
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error INVALID_METHODID  "methodID is not the ID of a method.")
            (Error ABSENT_INFORMATION "there is no variable information for the method.")
            (Error VM_DEAD)
        )
    )
    (Command Bytecodes=3
        "Retrieve the method's bytecodes as defined in "
        "<cite>The Java Virtual Machine Specification</cite>. "
        "Requires canGetBytecodes capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."
        (Out
            (referenceType refType "The class.")
            (method methodID "The method.")
        )
        (Reply
            (Repeat bytes
                (byte bytecode "A Java bytecode.")
            )
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error INVALID_METHODID  "methodID is not the ID of a method.")
            (Error NOT_IMPLEMENTED   "If the target virtual machine does not "
                                     "support the retrieval of bytecodes.")
            (Error VM_DEAD)
        )
    )
    (Command IsObsolete=4
        "Determine if this method is obsolete. A method is obsolete if it has been replaced "
        "by a non-equivalent method using the "
        "<a href=\"#JDWP_VirtualMachine_RedefineClasses\">RedefineClasses</a> command. "
        "The original and redefined methods are considered equivalent if their bytecodes are "
        "the same except for indices into the constant pool and the referenced constants are "
        "equal."
        (Out
            (referenceType refType "The class.")
            (method methodID "The method.")
        )
        (Reply
            (boolean isObsolete    "true if this method has been replaced"
                                   "by a non-equivalent method using"
                                   "the RedefineClasses command.")
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error INVALID_METHODID  "methodID is not the ID of a method.")
            (Error NOT_IMPLEMENTED   "If the target virtual machine does "
                                     "not support this query.")
            (Error VM_DEAD)
        )
    )
    (Command VariableTableWithGeneric=5
        "Returns variable information for the method, including "
        "generic signatures for the variables. The variable table "
        "includes arguments and locals declared within the method. For "
        "instance methods, the \"this\" reference is included in the "
        "table. Also, synthetic variables may be present. "
        "Generic signatures are described in the signature attribute "
        "section in "
        "<cite>The Java Virtual Machine Specification</cite>. "
        "Since JDWP version 1.5."
        (Out
            (referenceType refType "The class.")
            (method methodID "The method.")
        )
        (Reply
            (int argCnt "The number of words in the frame used by arguments. "
                        "Eight-byte arguments use two words; all others use one. ")
            (Repeat slots "The number of variables."
                (Group SlotInfo "Information about the variable."
                    (long codeIndex
                        "First code index at which the variable is visible (unsigned). "
                        "Used in conjunction with <code>length</code>. "
                        "The variable can be get or set only when the current "
                        "<code>codeIndex</code> &lt;= current frame code index &lt; <code>codeIndex + length</code> ")
                    (string name "The variable's name.")
                    (string signature "The variable type's JNI signature.")
                    (string genericSignature "The variable type's generic "
                         "signature or an empty string if there is none.")
                    (int length
                        "Unsigned value used in conjunction with <code>codeIndex</code>. "
                        "The variable can be get or set only when the current "
                        "<code>codeIndex</code> &lt;= current frame code index &lt; <code>code index + length</code> ")
                    (int slot "The local variable's index in its frame")
                )
            )
        )
        (ErrorSet
            (Error INVALID_CLASS     "refType is not the ID of a reference "
                                     "type.")
            (Error INVALID_OBJECT    "refType is not a known ID.")
            (Error INVALID_METHODID  "methodID is not the ID of a method.")
            (Error ABSENT_INFORMATION "there is no variable information for the method.")
            (Error VM_DEAD)
        )
    )

)
(CommandSet Field=8
)
(CommandSet ObjectReference=9
    (Command ReferenceType=1
        "Returns the runtime type of the object. "
        "The runtime type will be a class or an array. "
        (Out
            (object object "The object ID")
        )
        (Reply
            (byte refTypeTag  "<a href=\"#JDWP_TypeTag\">Kind</a> "
                              "of following reference type. ")
            (referenceTypeID typeID "The runtime reference type.")
        )
        (ErrorSet
            (Error INVALID_OBJECT)
            (Error VM_DEAD)
        )
    )
    (Command GetValues=2
        "Returns the value of one or more instance fields. "
        "Each field must be member of the object's type "
        "or one of its superclasses, superinterfaces, or implemented interfaces. "
        "Access control is not enforced; for example, the values of private "
        "fields can be obtained."
        (Out
            (object object "The object ID")
            (Repeat fields "The number of values to get"
                (Group Field
                    (field fieldID "Field to get.")
                )
            )
        )
        (Reply
            (Repeat values "The number of values returned, always equal to 'fields', "
                           "the number of values to get. Field values are ordered "
                           "in the reply in the same order as corresponding fieldIDs "
                           "in the command."
                (value value "The field value")
            )
        )
        (ErrorSet
            (Error INVALID_OBJECT)
            (Error INVALID_FIELDID)
            (Error VM_DEAD)
        )
    )
    (Command SetValues=3
        "Sets the value of one or more instance fields. "
        "Each field must be member of the object's type "
        "or one of its superclasses, superinterfaces, or implemented interfaces. "
        "Access control is not enforced; for example, the values of private "
        "fields can be set. "
        "For primitive values, the value's type must match the "
        "field's type exactly. For object values, there must be a "
        "widening reference conversion from the value's type to the
        "field's type and the field's type must be loaded. "
        (Out
            (object object "The object ID")
            (Repeat values "The number of fields to set."
                (Group FieldValue "A Field/Value pair."
                    (field fieldID "Field to set.")
                    (untagged-value value "Value to put in the field.")
                )
            )
        )
        (Reply "none"
        )
        (ErrorSet
            (Error INVALID_OBJECT)
            (Error INVALID_FIELDID)
            (Error VM_DEAD)
        )
    )
    (Command MonitorInfo=5
        "Returns monitor information for an object. All threads int the VM must "
        "be suspended."
        "Requires canGetMonitorInfo capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."
        (Out
            (object object "The object ID")
        )
        (Reply
            (threadObject owner "The monitor owner, or null if it is not currently owned.")
            (int entryCount "The number of times the monitor has been entered.")
            (Repeat waiters "The number of threads that are waiting for the monitor "
                            "0 if there is no current owner"
                (threadObject thread "A thread waiting for this monitor.")
            )
        )
        (ErrorSet
            (Error INVALID_OBJECT)
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
    (Command InvokeMethod=6
        "Invokes a instance method. "
        "The method must be member of the object's type "
        "or one of its superclasses, superinterfaces, or implemented interfaces. "
        "Access control is not enforced; for example, private "
        "methods can be invoked."
        "<p>"
        "The method invocation will occur in the specified thread. "
        "Method invocation can occur only if the specified thread "
        "has been suspended by an event. "
        "Method invocation is not supported "
        "when the target VM has been suspended by the front-end. "
        "<p>"
        "The specified method is invoked with the arguments in the specified "
        "argument list. "
        "The method invocation is synchronous; the reply packet is not "
        "sent until the invoked method returns in the target VM. "
        "The return value (possibly the void value) is "
        "included in the reply packet. "
        "If the invoked method throws an exception, the "
        "exception object ID is set in the reply packet; otherwise, the "
        "exception object ID is null. "
        "<p>"
        "For primitive arguments, the argument value's type must match the "
        "argument's type exactly. For object arguments, there must be a "
        "widening reference conversion from the argument value's type to the "
        "argument's type and the argument's type must be loaded. "
        "<p>"
        "By default, all threads in the target VM are resumed while "
        "the method is being invoked if they were previously "
        "suspended by an event or by a command. "
        "This is done to prevent the deadlocks "
        "that will occur if any of the threads own monitors "
        "that will be needed by the invoked method. It is possible that "
        "breakpoints or other events might occur during the invocation. "
        "Note, however, that this implicit resume acts exactly like "
        "the ThreadReference resume command, so if the thread's suspend "
        "count is greater than 1, it will remain in a suspended state "
        "during the invocation. By default, when the invocation completes, "
        "all threads in the target VM are suspended, regardless their state "
        "before the invocation. "
        "<p>"
        "The resumption of other threads during the invoke can be prevented "
        "by specifying the INVOKE_SINGLE_THREADED "
        "bit flag in the <code>options</code> field; however, "
        "there is no protection against or recovery from the deadlocks "
        "described above, so this option should be used with great caution. "
        "Only the specified thread will be resumed (as described for all "
        "threads above). Upon completion of a single threaded invoke, the invoking thread "
        "will be suspended once again. Note that any threads started during "
        "the single threaded invocation will not be suspended when the "
        "invocation completes. "
        "<p>"
        "If the target VM is disconnected during the invoke (for example, through "
        "the VirtualMachine dispose command) the method invocation continues. "
        (Out
            (object object "The object ID")
            (threadObject thread "The thread in which to invoke.")
            (classType clazz "The class type.")
            (method methodID "The method to invoke.")
            (Repeat arguments "The number of arguments."
                (value arg "The argument value.")
            )
            (int options "Invocation <a href=\"#JDWP_InvokeOptions\">options</a>")
        )
        (Reply
            (value returnValue "The returned value, or null if an exception is thrown.")
            (tagged-object exception "The thrown exception, if any.")
        )
        (ErrorSet
            (Error INVALID_OBJECT)
            (Error INVALID_CLASS     "clazz is not the ID of a reference "
                                     "type.")
            (Error INVALID_METHODID  "methodID is not the ID of an instance method "
                                     "in this object's type or one of its superclasses, "
                                     "superinterfaces, or implemented interfaces.")
            (Error INVALID_THREAD)
            (Error THREAD_NOT_SUSPENDED)
            (Error VM_DEAD)
        )
    )
    (Command DisableCollection=7
        "Prevents garbage collection for the given object. By "
        "default all objects in back-end replies may be "
        "collected at any time the target VM is running. A call to "
        "this command guarantees that the object will not be "
        "collected. The "
        "<a href=\"#JDWP_ObjectReference_EnableCollection\">EnableCollection</a> "
        "command can be used to "
        "allow collection once again. "
        "<p>"
        "Note that while the target VM is suspended, no garbage "
        "collection will occur because all threads are suspended. "
        "The typical examination of variables, fields, and arrays "
        "during the suspension is safe without explicitly disabling "
        "garbage collection. "
        "<p>"
        "This method should be used sparingly, as it alters the "
        "pattern of garbage collection in the target VM and, "
        "consequently, may result in application behavior under the "
        "debugger that differs from its non-debugged behavior. "
        (Out
            (object object "The object ID")
        )
        (Reply "none"
        )
        (ErrorSet
            (Error INVALID_OBJECT)
            (Error VM_DEAD)
        )
    )
    (Command EnableCollection=8
        "Permits garbage collection for this object. By default all "
        "objects returned by JDWP may become unreachable in the target VM, "
        "and hence may be garbage collected. A call to this command is "
        "necessary only if garbage collection was previously disabled with "
        "the <a href=\"#JDWP_ObjectReference_DisableCollection\">DisableCollection</a> "
        "command."
        (Out
            (object object "The object ID")
        )
        (Reply "none"
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
    (Command IsCollected=9
        "Determines whether an object has been garbage collected in the "
        "target VM. "
        (Out
            (object object "The object ID")
        )
        (Reply
            (boolean isCollected "true if the object has been collected; false otherwise")
        )
        (ErrorSet
            (Error INVALID_OBJECT)
            (Error VM_DEAD)
        )
    )
    (Command ReferringObjects=10
        "Returns objects that directly reference this object.  "
        "Only objects that are reachable for the purposes "
        "of garbage collection are returned. "
        "Note that an object can also be referenced in other ways, "
        "such as from a local variable in a stack frame, or from a JNI global "
        "reference.  Such non-object referrers are not returned by this command. "
        "<p>Since JDWP version 1.6. Requires canGetInstanceInfo capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."
        (Out
            (object object "The object ID")
            (int maxReferrers "Maximum number of referring objects to return. "
                              "Must be non-negative. If zero, all referring "
                              "objects are returned.")
        )
        (Reply
            (Repeat referringObjects "The number of objects that follow."
                (tagged-object instance "An object that references this object.")
             )
        )
        (ErrorSet
            (Error INVALID_OBJECT    "object is not a known ID.")
            (Error ILLEGAL_ARGUMENT  "maxReferrers is less than zero.")
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
)

(CommandSet StringReference=10
    (Command Value=1
        "Returns the characters contained in the string. "
        (Out
            (object stringObject "The String object ID. ")
        )
        (Reply
            (string stringValue "UTF-8 representation of the string value.")
       )
        (ErrorSet
            (Error INVALID_STRING)
            (Error INVALID_OBJECT)
            (Error VM_DEAD)
        )
    )
)
(CommandSet ThreadReference=11
    (Command Name=1
        "Returns the thread name. "
        (Out
            (threadObject thread "The thread object ID. ")
        )
        (Reply
            (string threadName "The thread name.")
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command Suspend=2
        "Suspends the thread. "
        "<p>"
        "Unlike java.lang.Thread.suspend(), suspends of both "
        "the virtual machine and individual threads are counted. Before "
        "a thread will run again, it must be resumed the same number "
        "of times it has been suspended. "
        "<p>"
        "Suspending single threads with command has the same "
        "dangers java.lang.Thread.suspend(). If the suspended "
        "thread holds a monitor needed by another running thread, "
        "deadlock is possible in the target VM (at least until the "
        "suspended thread is resumed again). "
        "<p>"
        "The suspended thread is guaranteed to remain suspended until "
        "resumed through one of the JDI resume methods mentioned above; "
        "the application in the target VM cannot resume the suspended thread "
        "through {@link java.lang.Thread#resume}. "
        "<p>"
        "Note that this doesn't change the status of the thread (see the "
        "<a href=\"#JDWP_ThreadReference_Status\">ThreadStatus</a> command.) "
        "For example, if it was "
        "Running, it will still appear running to other threads. "
        (Out
            (threadObject thread "The thread object ID. ")
        )
        (Reply "none"
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command Resume=3
        "Resumes the execution of a given thread. If this thread was "
        "not previously suspended by the front-end, "
        "calling this command has no effect. "
        "Otherwise, the count of pending suspends on this thread is "
        "decremented. If it is decremented to 0, the thread will "
        "continue to execute. "
        (Out
            (threadObject thread "The thread object ID. ")
        )
        (Reply "none"
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command Status=4
        "Returns the current status of a thread. The thread status "
        "reply indicates the thread status the last time it was running. "
        "the suspend status provides information on the thread's "
        "suspension, if any."
        (Out
            (threadObject thread "The thread object ID. ")
        )
        (Reply
            (int threadStatus "One of the thread status codes "
                    "See <a href=\"#JDWP_ThreadStatus\">JDWP.ThreadStatus</a>")
            (int suspendStatus "One of the suspend status codes "
                    "See <a href=\"#JDWP_SuspendStatus\">JDWP.SuspendStatus</a>")
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command ThreadGroup=5
        "Returns the thread group that contains a given thread. "
        (Out
            (threadObject thread "The thread object ID. ")
        )
        (Reply
            (threadGroupObject group "The thread group of this thread. ")
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command Frames=6
        "Returns the current call stack of a suspended thread. "
        "The sequence of frames starts with "
        "the currently executing frame, followed by its caller, "
        "and so on. The thread must be suspended, and the returned "
        "frameID is valid only while the thread is suspended. "
        (Out
            (threadObject thread "The thread object ID. ")
            (int startFrame "The index of the first frame to retrieve.")
            (int length
                        "The count of frames to retrieve "
                        "(-1 means all remaining). ")
        )
        (Reply
            (Repeat frames "The number of frames retreived"
                (Group Frame
                    (frame frameID "The ID of this frame. ")
                    (location location "The current location of this frame")
                )
            )
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command FrameCount=7
        "Returns the count of frames on this thread's stack. "
        "The thread must be suspended, and the returned "
        "count is valid only while the thread is suspended. "
        "Returns JDWP.Error.errorThreadNotSuspended if not suspended. "
        (Out
            (threadObject thread "The thread object ID. ")
        )
        (Reply
            (int frameCount "The count of frames on this thread's stack. ")
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command OwnedMonitors=8
        "Returns the objects whose monitors have been entered by this thread. "
        "The thread must be suspended, and the returned information is "
        "relevant only while the thread is suspended. "
        "Requires canGetOwnedMonitorInfo capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."
        (Out
            (threadObject thread "The thread object ID. ")
        )
        (Reply
            (Repeat owned "The number of owned monitors"
                (tagged-object monitor "An owned monitor")
            )
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
    (Command CurrentContendedMonitor=9
        "Returns the object, if any, for which this thread is waiting. The "
        "thread may be waiting to enter a monitor, or it may be waiting, via "
        "the java.lang.Object.wait method, for another thread to invoke the "
        "notify method. "
        "The thread must be suspended, and the returned information is "
        "relevant only while the thread is suspended. "
        "Requires canGetCurrentContendedMonitor capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."
        (Out
            (threadObject thread "The thread object ID. ")
        )
        (Reply
            (tagged-object monitor "The contended monitor, or null if "
                                   "there is no current contended monitor. ")
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
    (Command Stop=10
        "Stops the thread with an asynchronous exception. "
        (Out
            (threadObject thread "The thread object ID. ")
            (object throwable "Asynchronous exception. This object must "
                              "be an instance of java.lang.Throwable or a subclass")
        )
        (Reply "none"
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT "If thread is not a known ID or the asynchronous "
                                  "exception has been garbage collected.")
            (Error VM_DEAD)
        )
    )
    (Command Interrupt=11
        "Interrupt the thread, as if done by java.lang.Thread.interrupt "
        (Out
            (threadObject thread "The thread object ID. ")
        )
        (Reply "none"
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command SuspendCount=12
        "Get the suspend count for this thread. The suspend count is the  "
        "number of times the thread has been suspended through the "
        "thread-level or VM-level suspend commands without a corresponding resume "
        (Out
            (threadObject thread "The thread object ID. ")
        )
        (Reply
            (int suspendCount "The number of outstanding suspends of this thread. ")
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command OwnedMonitorsStackDepthInfo=13
        "Returns monitor objects owned by the thread, along with stack depth at which "
        "the monitor was acquired. Returns stack depth of -1  if "
        "the implementation cannot determine the stack depth "
        "(e.g., for monitors acquired by JNI MonitorEnter)."
        "The thread must be suspended, and the returned information is "
        "relevant only while the thread is suspended. "
        "Requires canGetMonitorFrameInfo capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>. "
        "<p>Since JDWP version 1.6. "

        (Out
            (threadObject thread "The thread object ID. ")
        )
        (Reply
            (Repeat owned "The number of owned monitors"
               (Group monitor
                  (tagged-object monitor "An owned monitor")
                  (int stack_depth "Stack depth location where monitor was acquired")
               )
            )
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
    (Command ForceEarlyReturn=14
        "Force a method to return before it reaches a return "
        "statement.  "
        "<p>"
        "The method which will return early is referred to as the "
        "called method. The called method is the current method (as "
        "defined by the Frames section in "
        "<cite>The Java Virtual Machine Specification</cite>) "
        "for the specified thread at the time this command "
        "is received. "
        "<p>"
        "The specified thread must be suspended. "
        "The return occurs when execution of Java programming "
        "language code is resumed on this thread. Between sending this "
        "command and resumption of thread execution, the "
        "state of the stack is undefined. "
        "<p>"
        "No further instructions are executed in the called "
        "method. Specifically, finally blocks are not executed. Note: "
        "this can cause inconsistent states in the application. "
        "<p>"
        "A lock acquired by calling the called method (if it is a "
        "synchronized method) and locks acquired by entering "
        "synchronized blocks within the called method are "
        "released. Note: this does not apply to JNI locks or "
        "java.util.concurrent.locks locks. "
        "<p>"
        "Events, such as MethodExit, are generated as they would be in "
        "a normal return. "
        "<p>"
        "The called method must be a non-native Java programming "
        "language method. Forcing return on a thread with only one "
        "frame on the stack causes the thread to exit when resumed. "
        "<p>"
        "For void methods, the value must be a void value. "
        "For methods that return primitive values, the value's type must "
        "match the return type exactly.  For object values, there must be a "
        "widening reference conversion from the value's type to the "
        "return type type and the return type must be loaded. "
        "<p>"
        "Since JDWP version 1.6. Requires canForceEarlyReturn capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."
        (Out
            (threadObject thread "The thread object ID. ")
            (value value "The value to return. ")
        )
        (Reply "none"
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "Thread or value is not a known ID.")
            (Error THREAD_NOT_SUSPENDED)
            (Error THREAD_NOT_ALIVE)
            (Error OPAQUE_FRAME      "Attempted to return early from "
                                     "a frame corresponding to a native "
                                     "method. Or the implementation is "
                                     "unable to provide this functionality "
                                     "on this frame.")
            (Error NO_MORE_FRAMES)
            (Error NOT_IMPLEMENTED)
            (Error TYPE_MISMATCH   "Value is not an appropriate type for the "
                                   "return value of the method.")
            (Error VM_DEAD)
        )
    )

)
(CommandSet ThreadGroupReference=12
    (Command Name=1
        "Returns the thread group name. "
        (Out
            (threadGroupObject group "The thread group object ID. ")
        )
        (Reply
            (string groupName "The thread group's name.")
        )
        (ErrorSet
            (Error INVALID_THREAD_GROUP)
            (Error INVALID_OBJECT    "group is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command Parent=2
        "Returns the thread group, if any, which contains a given thread group. "
        (Out
            (threadGroupObject group "The thread group object ID. ")
        )
        (Reply
            (threadGroupObject parentGroup "The parent thread group object, or "
                                           "null if the given thread group "
                                           "is a top-level thread group")
        )
        (ErrorSet
            (Error INVALID_THREAD_GROUP)
            (Error INVALID_OBJECT    "group is not a known ID.")
            (Error VM_DEAD)
        )
    )
    (Command Children=3
        "Returns the live threads and active thread groups directly contained "
        "in this thread group. Threads and thread groups in child "
        "thread groups are not included. "
        "A thread is alive if it has been started and has not yet been stopped. "
        "See <a href=../../api/java.base/java/lang/ThreadGroup.html>java.lang.ThreadGroup </a>
        "for information about active ThreadGroups.
        (Out
            (threadGroupObject group "The thread group object ID. ")
        )
        (Reply
            (Repeat childThreads "The number of live child threads. "
                (threadObject childThread "A direct child thread ID. ")
            )
            (Repeat childGroups "The number of active child thread groups. "
                (threadGroupObject childGroup "A direct child thread group ID. ")
            )
        )
        (ErrorSet
            (Error INVALID_THREAD_GROUP)
            (Error INVALID_OBJECT    "group is not a known ID.")
            (Error VM_DEAD)
        )
    )
)
(CommandSet ArrayReference=13
    (Command Length=1
        "Returns the number of components in a given array. "
        (Out
            (arrayObject arrayObject "The array object ID. ")
        )
        (Reply
            (int arrayLength "The length of the array.")
        )
        (ErrorSet
            (Error INVALID_OBJECT    "arrayObject is not a known ID.")
            (Error INVALID_ARRAY)
            (Error VM_DEAD)
        )
    )
    (Command GetValues=2
        "Returns a range of array components. The specified range must "
        "be within the bounds of the array. "
        (Out
            (arrayObject arrayObject "The array object ID. ")
            (int firstIndex "The first index to retrieve.")
            (int length "The number of components to retrieve.")
        )
        (Reply
            (typed-sequence values "The retrieved values. If the values "
                                   "are objects, they are tagged-values; "
                                   "otherwise, they are untagged-values")
        )
        (ErrorSet
            (Error INVALID_LENGTH "If index is beyond the end of this array.")
            (Error INVALID_OBJECT    "arrayObject is not a known ID.")
            (Error INVALID_ARRAY)
            (Error VM_DEAD)
        )
    )
    (Command SetValues=3
        "Sets a range of array components. The specified range must "
        "be within the bounds of the array. "
        "For primitive values, each value's type must match the "
        "array component type exactly. For object values, there must be a "
        "widening reference conversion from the value's type to the
        "array component type and the array component type must be loaded. "
        (Out
            (arrayObject arrayObject "The array object ID. ")
            (int firstIndex "The first index to set.")
            (Repeat values "The number of values to set. "
                (untagged-value value "A value to set. ")
            )
        )
        (Reply "none"
        )
        (ErrorSet
            (Error INVALID_LENGTH "If index is beyond the end of this array.")
            (Error INVALID_OBJECT    "arrayObject is not a known ID.")
            (Error INVALID_ARRAY)
            (Error VM_DEAD)
        )
    )
)
(CommandSet ClassLoaderReference=14
    (Command VisibleClasses=1
        "Returns a list of all classes which this class loader can find "
        "by name via <code>ClassLoader::loadClass</code>, "
        "<code>Class::forName</code> and bytecode linkage. That is, "
        "all classes for which this class loader has been recorded as an "
        "<i>initiating</i> loader. The list contains each "
        "reference type created by this loader and any types for which "
        "loading was delegated by this class loader to another class loader. "
        "<p>"
        "The visible class list has useful properties with respect to "
        "the type namespace. A particular type name will occur at most "
        "once in the list. Each field or variable declared with that "
        "type name in a class defined by "
        "this class loader must be resolved to that single type. "
        "<p>"
        "No ordering of the returned list is guaranteed. "
        "<p>"
        "See <a href=\"../jvmti.html#GetClassLoaderClasses\">JVM TI GetClassLoaderClasses</a>. "
        (Out
            (classLoaderObject classLoaderObject "The class loader object ID. ")
        )
        (Reply
            (Repeat classes "The number of visible classes. "
                (Group ClassInfo
                    (byte refTypeTag  "<a href=\"#JDWP_TypeTag\">Kind</a> "
                                      "of following reference type. ")
                    (referenceTypeID typeID
                        "A class visible to this class loader.")
                )
            )
        )
        (ErrorSet
            (Error INVALID_OBJECT)
            (Error INVALID_CLASS_LOADER)
            (Error VM_DEAD)
        )
    )
)
(CommandSet EventRequest=15
    (Command Set=1
        "Set an event request. When the event described by this request "
        "occurs, an <a href=\"#JDWP_Event\">event</a> is sent from the "
        "target VM. If an event occurs that has not been requested then it is not sent "
        "from the target VM. The two exceptions to this are the VM Start Event and "
        "the VM Death Event which are automatically generated events - see "
        "<a href=\"#JDWP_Event_Composite\">Composite Command</a> for further details."
        (Out
            (byte eventKind "Event kind to request. "
                      "See <a href=\"#JDWP_EventKind\">JDWP.EventKind</a> "
                      "for a complete list of events that can be requested; "
                      "some events may require a capability in order to be requested. "
                      )
            (byte suspendPolicy
                      "What threads are suspended when this event occurs? "
                      "Note that the order of events and command replies "
                      "accurately reflects the order in which threads are "
                      "suspended and resumed. For example, if a "
                      "<a href=\"#JDWP_VirtualMachine_Resume\">VM-wide resume</a> "
                      "is processed before an event occurs which suspends the "
                      "VM, the reply to the resume command will be written to "
                      "the transport before the suspending event.")
            (Repeat modifiers "Constraints used to control the number "
                              "of generated events."
                              "Modifiers specify additional tests that "
                              "an event must satisfy before it is placed "
                              "in the event queue. Events are filtered by "
                              "applying each modifier to an event in the "
                              "order they are specified in this collection "
                              "Only events that satisfy all modifiers "
                              "are reported. A value of 0 means there are no "
                              "modifiers in the request."
                              "<p>"
                              "Filtering can improve "
                              "debugger performance dramatically by
                              "reducing the "
                              "amount of event traffic sent from the "
                              "target VM to the debugger VM. "
                (Select Modifier
                    (byte modKind "Modifier kind")
                    (Alt Count=1
                        "Limit the requested event to be reported at most once after a "
                        "given number of occurrences.  The event is not reported "
                        "the first <code>count - 1</code> times this filter is reached. "
                        "To request a one-off event, call this method with a count of 1. "
                        "<p>"
                        "Once the count reaches 0, any subsequent filters in this request "
                        "are applied. If none of those filters cause the event to be "
                        "suppressed, the event is reported. Otherwise, the event is not "
                        "reported. In either case subsequent events are never reported for "
                        "this request. "
                        "This modifier can be used with any event kind."

                        (int count "Count before event. One for one-off.")
                    )
                    (Alt Conditional=2 "Conditional on expression"
                        (int exprID "For the future")
                    )
                    (Alt ThreadOnly=3
                        "Restricts reported events to "
                        "those in the given thread. "
                        "This modifier can be used with any event kind "
                        "except for class unload. "

                        (threadObject thread "Required thread")
                    )
                    (Alt ClassOnly=4
                        "For class prepare events, restricts the events "
                        "generated by this request to be the "
                        "preparation of the given reference type and any subtypes. "
                        "For monitor wait and waited events, restricts the events "
                        "generated by this request to those whose monitor object "
                        "is of the given reference type or any of its subtypes. "
                        "For other events, restricts the events generated "
                        "by this request to those "
                        "whose location is in the given reference type or any of its subtypes. "
                        "An event will be generated for any location in a reference type that can "
                        "be safely cast to the given reference type. "
                        "This modifier can be used with any event kind except "
                        "class unload, thread start, and thread end. "

                        (referenceType clazz "Required class")
                    )
                    (Alt ClassMatch=5
                        "Restricts reported events to those for classes whose name "
                        "matches the given restricted regular expression. "
                        "For class prepare events, the prepared class name "
                        "is matched. For class unload events, the "
                        "unloaded class name is matched. For monitor wait "
                        "and waited events, the name of the class of the "
                        "monitor object is matched. For other events, "
                        "the class name of the event's location is matched. "
                        "This modifier can be used with any event kind except "
                        "thread start and thread end. "

                        (string classPattern "Required class pattern. "
                                "Matches are limited to exact matches of the "
                                "given class pattern and matches of patterns that "
                                "begin or end with '*'; for example, "
                                "\"*.Foo\" or \"java.*\". "
                        )

                    )
                    (Alt ClassExclude=6
                        "Restricts reported events to those for classes whose name "
                        "does not match the given restricted regular expression. "
                        "For class prepare events, the prepared class name "
                        "is matched. For class unload events, the "
                        "unloaded class name is matched. For monitor wait and "
                        "waited events, the name of the class of the monitor "
                        "object is matched. For other events, "
                        "the class name of the event's location is matched. "
                        "This modifier can be used with any event kind except "
                        "thread start and thread end. "

                        (string classPattern "Disallowed class pattern. "
                                "Matches are limited to exact matches of the "
                                "given class pattern and matches of patterns that "
                                "begin or end with '*'; for example, "
                                "\"*.Foo\" or \"java.*\". "
                        )
                    )
                    (Alt LocationOnly=7
                        "Restricts reported events to those that occur at "
                        "the given location. "
                        "This modifier can be used with "
                        "breakpoint, field access, field modification, "
                        "step, and exception event kinds. "

                        (location loc "Required location")
                    )
                    (Alt ExceptionOnly=8
                        "Restricts reported exceptions by their class and "
                        "whether they are caught or uncaught. "
                        "This modifier can be used with "
                        "exception event kinds only. "

                        (referenceType exceptionOrNull
                                "Exception to report. Null (0) means report "
                                "exceptions of all types. "
                                "A non-null type restricts the reported exception "
                                "events to exceptions of the given type or "
                                "any of its subtypes. "
                        )
                        (boolean caught "Report caught exceptions")
                        (boolean uncaught "Report uncaught exceptions. "
                                "Note that it "
                                "is not always possible to determine whether an "
                                "exception is caught or uncaught at the time it is "
                                "thrown. See the exception event catch location under "
                                "<a href=\"#JDWP_Event_Composite\">composite events</a> "
                                "for more information. "
                        )

                    )
                    (Alt FieldOnly=9
                        "Restricts reported events to those that occur for "
                        "a given field. "
                        "This modifier can be used with "
                        "field access and field modification event kinds only. "

                        (referenceType declaring "Type in which field is declared.")
                        (field fieldID "Required field")
                    )
                    (Alt Step=10
                        "Restricts reported step events "
                        "to those which satisfy "
                        "depth and size constraints. "
                        "This modifier can be used with "
                        "step event kinds only. "

                        (threadObject thread "Thread in which to step")
                        (int size "size of each step. "
                           "See <a href=\"#JDWP_StepSize\">JDWP.StepSize</a>")
                        (int depth "relative call stack limit. "
                           "See <a href=\"#JDWP_StepDepth\">JDWP.StepDepth</a>")
                    )
                    (Alt InstanceOnly=11
                        "Restricts reported events to those whose "
                        "active 'this' object is the given object. "
                        "Match value is the null object for static methods. "
                        "This modifier can be used with any event kind "
                        "except class prepare, class unload, thread start, "
                        "and thread end. Introduced in JDWP version 1.4."

                        (object instance "Required 'this' object")
                    )
                    (Alt SourceNameMatch=12
                        "Restricts reported class prepare events to those "
                        "for reference types which have a source name "
                        "which matches the given restricted regular expression. "
                        "The source names are determined by the reference type's "
                        "<a href=\"#JDWP_ReferenceType_SourceDebugExtension\"> "
                        "SourceDebugExtension</a>. "
                        "This modifier can only be used with class prepare "
                        "events. "
                        "Since JDWP version 1.6. Requires the canUseSourceNameFilters "
                        "capability - see "
                        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."

                        (string sourceNamePattern "Required source name pattern. "
                                "Matches are limited to exact matches of the "
                                "given pattern and matches of patterns that "
                                "begin or end with '*'; for example, "
                                "\"*.Foo\" or \"java.*\". "
                        )
                    )

                )
            )
        )
        (Reply
            (int requestID "ID of created request")
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_CLASS)
            (Error INVALID_STRING)
            (Error INVALID_OBJECT)
            (Error INVALID_COUNT)
            (Error INVALID_FIELDID)
            (Error INVALID_METHODID)
            (Error INVALID_LOCATION)
            (Error INVALID_EVENT_TYPE)
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
    (Command Clear=2
        "Clear an event request. See <a href=\"#JDWP_EventKind\">JDWP.EventKind</a> "
        "for a complete list of events that can be cleared. Only the event request matching "
        "the specified event kind and requestID is cleared. If there isn't a matching event "
        "request the command is a no-op and does not result in an error. Automatically "
        "generated events do not have a corresponding event request and may not be cleared "
        "using this command."
        (Out
            (byte eventKind "Event kind to clear")
            (int requestID "ID of request to clear")
        )
        (Reply "none"
        )
        (ErrorSet
            (Error VM_DEAD)
            (Error INVALID_EVENT_TYPE)
        )
    )
    (Command ClearAllBreakpoints=3
        "Removes all set breakpoints, a no-op if there are no breakpoints set."
        (Out "none"
        )
        (Reply "none"
        )
        (ErrorSet
            (Error VM_DEAD)
        )
    )
)
(CommandSet StackFrame=16
    (Command GetValues=1
        "Returns the value of one or more local variables in a "
        "given frame. Each variable must be visible at the frame's code index. "
        "Even if local variable information is not available, values can "
        "be retrieved if the front-end is able to "
        "determine the correct local variable index. (Typically, this "
        "index can be determined for method arguments from the method "
        "signature without access to the local variable table information.) "
        (Out
            (threadObject thread "The frame's thread. ")
            (frame frame "The frame ID. ")
            (Repeat slots "The number of values to get. "
                (Group SlotInfo
                    (int slot "The local variable's index in the frame. ")
                    (byte sigbyte "A <a href=\"#JDWP_Tag\">tag</a> "
                                  "identifying the type of the variable ")
                )
            )
        )
        (Reply
            (Repeat values "The number of values retrieved, always equal to slots, "
                           "the number of values to get."
                (value slotValue "The value of the local variable. ")
            )
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT)
            (Error INVALID_FRAMEID)
            (Error INVALID_SLOT)
            (Error VM_DEAD)
        )
    )
    (Command SetValues=2
        "Sets the value of one or more local variables. "
        "Each variable must be visible at the current frame code index. "
        "For primitive values, the value's type must match the "
        "variable's type exactly. For object values, there must be a "
        "widening reference conversion from the value's type to the
        "variable's type and the variable's type must be loaded. "
        "<p>"
        "Even if local variable information is not available, values can "
        "be set, if the front-end is able to "
        "determine the correct local variable index. (Typically, this
        "index can be determined for method arguments from the method "
        "signature without access to the local variable table information.) "
        (Out
            (threadObject thread "The frame's thread. ")
            (frame frame "The frame ID. ")
            (Repeat slotValues "The number of values to set. "
                (Group SlotInfo
                    (int slot "The slot ID. ")
                    (value slotValue "The value to set. ")
                )
            )
        )
        (Reply "none"
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT)
            (Error INVALID_FRAMEID)
            (Error VM_DEAD)
        )
    )
    (Command ThisObject=3
        "Returns the value of the 'this' reference for this frame. "
        "If the frame's method is static or native, the reply "
        "will contain the null object reference. "
        (Out
            (threadObject thread "The frame's thread. ")
            (frame frame "The frame ID. ")
        )
        (Reply
            (tagged-object objectThis "The 'this' object for this frame. ")
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT)
            (Error INVALID_FRAMEID)
            (Error VM_DEAD)
        )
    )
    (Command PopFrames=4
        "Pop the top-most stack frames of the thread stack, up to, and including 'frame'. "
        "The thread must be suspended to perform this command. "
        "The top-most stack frames are discarded and the stack frame previous to 'frame' "
        "becomes the current frame. The operand stack is restored -- the argument values "
        "are added back and if the invoke was not <code>invokestatic</code>, "
        "<code>objectref</code> is added back as well. The Java virtual machine "
        "program counter is restored to the opcode of the invoke instruction."
        "<p>"
        "Since JDWP version 1.4. Requires canPopFrames capability - see "
        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."
        (Out
            (threadObject thread "The thread object ID. ")
            (frame frame "The frame ID. ")
        )
        (Reply "none"
        )
        (ErrorSet
            (Error INVALID_THREAD)
            (Error INVALID_OBJECT    "thread is not a known ID.")
            (Error INVALID_FRAMEID)
            (Error THREAD_NOT_SUSPENDED)
            (Error NO_MORE_FRAMES)
            (Error INVALID_FRAMEID)
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
)
(CommandSet ClassObjectReference=17
    (Command ReflectedType = 1
        "Returns the reference type reflected by this class object."
        (Out
            (classObject classObject "The class object. ")
        )
        (Reply
            (byte refTypeTag  "<a href=\"#JDWP_TypeTag\">Kind</a> "
                              "of following reference type. ")
            (referenceTypeID typeID "reflected reference type")
        )
        (ErrorSet
            (Error INVALID_OBJECT)
            (Error VM_DEAD)
        )
    )
)
(CommandSet ModuleReference=18
    (Command Name=1
        "Returns the name of this module."
        "<p>Since JDWP version 9."
        (Out
            (moduleID module "This module.")
        )
        (Reply
            (string name  "The module's name.")
        )
        (ErrorSet
            (Error INVALID_MODULE)
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
    (Command ClassLoader=2
        "Returns the class loader of this module."
        "<p>Since JDWP version 9."
        (Out
            (moduleID module "This module.")
        )
        (Reply
            (classLoaderObject classLoader  "The module's class loader.")
        )
        (ErrorSet
            (Error INVALID_MODULE)
            (Error NOT_IMPLEMENTED)
            (Error VM_DEAD)
        )
    )
)
(CommandSet Event=64
    (Command Composite=100
        "Several events may occur at a given time in the target VM. "
        "For example, there may be more than one breakpoint request "
        "for a given location "
        "or you might single step to the same location as a "
        "breakpoint request.  These events are delivered "
        "together as a composite event.  For uniformity, a "
        "composite event is always used "
        "to deliver events, even if there is only one event to report. "
        "<P>"
        "The events that are grouped in a composite event are restricted in the "
        "following ways: "
        "<UL>"
        "<LI>Only with other thread start events for the same thread:"
        "    <UL>"
        "    <LI>Thread Start Event"
        "    </UL>"
        "<LI>Only with other thread death events for the same thread:"
        "    <UL>"
        "    <LI>Thread Death Event"
        "    </UL>"
        "<LI>Only with other class prepare events for the same class:"
        "    <UL>"
        "    <LI>Class Prepare Event"
        "    </UL>"
        "<LI>Only with other class unload events for the same class:"
        "    <UL>"
        "    <LI>Class Unload Event"
        "    </UL>"
        "<LI>Only with other access watchpoint events for the same field access:"
        "    <UL>"
        "    <LI>Access Watchpoint Event"
        "    </UL>"
        "<LI>Only with other modification watchpoint events for the same field "
        "modification:"
        "    <UL>"
        "    <LI>Modification Watchpoint Event"
        "    </UL>"
        "<LI>Only with other Monitor contended enter events for the same monitor object: "
        "    <UL>"
        "    <LI>Monitor Contended Enter Event"
        "    </UL>"
        "<LI>Only with other Monitor contended entered events for the same monitor object: "
        "    <UL>"
        "    <LI>Monitor Contended Entered Event"
        "    </UL>"
        "<LI>Only with other Monitor wait events for the same monitor object: "
        "    <UL>"
        "    <LI>Monitor Wait Event"
        "    </UL>"
        "<LI>Only with other Monitor waited events for the same monitor object: "
        "    <UL>"
        "    <LI>Monitor Waited Event"
        "    </UL>"
        "<LI>Only with other ExceptionEvents for the same exception occurrance:"
        "    <UL>"
        "    <LI>ExceptionEvent"
        "    </UL>"
        "<LI>Only with other members of this group, at the same location "
        "and in the same thread: "
        "    <UL>"
        "    <LI>Breakpoint Event"
        "    <LI>Step Event"
        "    <LI>Method Entry Event"
        "    <LI>Method Exit Event"
        "    </UL>"
        "</UL>"
        "<P>"
        "The VM Start Event and VM Death Event are automatically generated events. "
        "This means they do not need to be requested using the "
        "<a href=\"#JDWP_EventRequest_Set\">EventRequest.Set</a> command. "
        "The VM Start event signals the completion of VM initialization. The VM Death "
        "event signals the termination of the VM."
        "If there is a debugger connected at the time when an automatically generated "
        "event occurs it is sent from the target VM. Automatically generated events may "
        "also be requested using the EventRequest.Set command and thus multiple events "
        "of the same event kind will be sent from the target VM when an event occurs."
        "Automatically generated events are sent with the requestID field "
        "in the Event Data set to 0. The value of the suspendPolicy field in the "
        "Event Data depends on the event. For the automatically generated VM Start "
        "Event the value of suspendPolicy is not defined and is therefore implementation "
        "or configuration specific. In the Sun implementation, for example, the "
        "suspendPolicy is specified as an option to the JDWP agent at launch-time."
        "The automatically generated VM Death Event will have the suspendPolicy set to "
        "NONE."

       (Event "Generated event"
            (byte suspendPolicy
                "Which threads where suspended by this composite event?")
            (Repeat events "Events in set."
                (Select Events
                    (byte eventKind "Event kind selector")
                    (Alt VMStart=JDWP.EventKind.VM_START
                        "Notification of initialization of a target VM.  This event is "
                        "received before the main thread is started and before any "
                        "application code has been executed. Before this event occurs "
                        "a significant amount of system code has executed and a number "
                        "of system classes have been loaded. "
                        "This event is always generated by the target VM, even "
                        "if not explicitly requested."

                     (int requestID
                             "Request that generated event (or 0 if this "
                             "event is automatically generated.")
                        (threadObject thread "Initial thread")
                    )
                    (Alt SingleStep=JDWP.EventKind.SINGLE_STEP
                        "Notification of step completion in the target VM. The step event "
                        "is generated before the code at its location is executed. "

                        (int requestID "Request that generated event")
                        (threadObject thread "Stepped thread")
                        (location location "Location stepped to")
                    )
                    (Alt Breakpoint=JDWP.EventKind.BREAKPOINT
                        "Notification of a breakpoint in the target VM. The breakpoint event "
                        "is generated before the code at its location is executed. "

                        (int requestID "Request that generated event")
                        (threadObject thread "Thread which hit breakpoint")
                        (location location "Location hit")
                    )
                    (Alt MethodEntry=JDWP.EventKind.METHOD_ENTRY
                         "Notification of a method invocation in the target VM. This event "
                         "is generated before any code in the invoked method has executed. "
                         "Method entry events are generated for both native and non-native "
                         "methods. "
                         "<P>"
                         "In some VMs method entry events can occur for a particular thread "
                         "before its thread start event occurs if methods are called "
                         "as part of the thread's initialization. "

                        (int requestID "Request that generated event")
                        (threadObject thread "Thread which entered method")
                        (location location "The initial executable location in the method.")
                    )
                    (Alt MethodExit=JDWP.EventKind.METHOD_EXIT
                         "Notification of a method return in the target VM. This event "
                         "is generated after all code in the method has executed, but the "
                         "location of this event is the last executed location in the method. "
                         "Method exit events are generated for both native and non-native "
                         "methods. Method exit events are not generated if the method terminates "
                         "with a thrown exception. "

                        (int requestID "Request that generated event")
                        (threadObject thread "Thread which exited method")
                        (location location "Location of exit")
                    )
                    (Alt MethodExitWithReturnValue=JDWP.EventKind.METHOD_EXIT_WITH_RETURN_VALUE
                         "Notification of a method return in the target VM. This event "
                         "is generated after all code in the method has executed, but the "
                         "location of this event is the last executed location in the method. "
                         "Method exit events are generated for both native and non-native "
                         "methods. Method exit events are not generated if the method terminates "
                         "with a thrown exception. <p>Since JDWP version 1.6. "

                        (int requestID "Request that generated event")
                        (threadObject thread "Thread which exited method")
                        (location location "Location of exit")
                        (value value "Value that will be returned by the method")
                    )
                    (Alt MonitorContendedEnter=JDWP.EventKind.MONITOR_CONTENDED_ENTER
                         "Notification that a thread in the target VM is attempting "
                         "to enter a monitor that is already acquired by another thread. "
                         "Requires canRequestMonitorEvents capability - see "
                         "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>. "
                         "<p>Since JDWP version 1.6. "

                        (int requestID
                                "Request that generated event")
                        (threadObject thread "Thread which is trying to enter the monitor")
                        (tagged-object object "Monitor object reference")
                        (location location "Location of contended monitor enter")
                    )
                    (Alt MonitorContendedEntered=JDWP.EventKind.MONITOR_CONTENDED_ENTERED
                         "Notification of a thread in the target VM is entering a monitor "
                         "after waiting for it to be released by another thread. "
                         "Requires canRequestMonitorEvents capability - see "
                         "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>. "
                         "<p>Since JDWP version 1.6. "

                        (int requestID
                                "Request that generated event")
                        (threadObject thread "Thread which entered monitor")
                        (tagged-object object "Monitor object reference")
                        (location location "Location of contended monitor enter")
                    )
                    (Alt MonitorWait=JDWP.EventKind.MONITOR_WAIT
                         "Notification of a thread about to wait on a monitor object. "
                         "Requires canRequestMonitorEvents capability - see "
                         "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>. "
                         "<p>Since JDWP version 1.6. "

                        (int requestID
                                "Request that generated event")
                        (threadObject thread "Thread which is about to wait")
                        (tagged-object object "Monitor object reference")
                        (location location "Location at which the wait will occur")
                        (long     timeout  "Thread wait time in milliseconds")
                    )
                    (Alt MonitorWaited=JDWP.EventKind.MONITOR_WAITED
                         "Notification that a thread in the target VM has finished waiting on "
                         "Requires canRequestMonitorEvents capability - see "
                         "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>. "
                         "a monitor object. "
                         "<p>Since JDWP version 1.6. "

                        (int requestID
                                "Request that generated event")
                        (threadObject thread "Thread which waited")
                        (tagged-object object "Monitor object reference")
                        (location location "Location at which the wait occured")
                        (boolean  timed_out "True if timed out")
                    )
                    (Alt Exception=JDWP.EventKind.EXCEPTION
                         "Notification of an exception in the target VM. "
                         "If the exception is thrown from a non-native method, "
                         "the exception event is generated at the location where the "
                         "exception is thrown. "
                         "If the exception is thrown from a native method, the exception event "
                         "is generated at the first non-native location reached after the exception "
                         "is thrown. "

                        (int requestID "Request that generated event")
                        (threadObject thread "Thread with exception")
                        (location location "Location of exception throw "
                        "(or first non-native location after throw if thrown from a native method)")
                        (tagged-object exception "Thrown exception")
                        (location catchLocation
                            "Location of catch, or 0 if not caught. An exception "
                            "is considered to be caught if, at the point of the throw, the "
                            "current location is dynamically enclosed in a try statement that "
                            "handles the exception. (See the JVM specification for details). "
                            "If there is such a try statement, the catch location is the "
                            "first location in the appropriate catch clause. "
                            "<p>"
                            "If there are native methods in the call stack at the time of the "
                            "exception, there are important restrictions to note about the "
                            "returned catch location. In such cases, "
                            "it is not possible to predict whether an exception will be handled "
                            "by some native method on the call stack. "
                            "Thus, it is possible that exceptions considered uncaught "
                            "here will, in fact, be handled by a native method and not cause "
                            "termination of the target VM. Furthermore, it cannot be assumed that the "
                            "catch location returned here will ever be reached by the throwing "
                            "thread. If there is "
                            "a native frame between the current location and the catch location, "
                            "the exception might be handled and cleared in that native method "
                            "instead. "
                            "<p>"
                            "Note that compilers can generate try-catch blocks in some cases "
                            "where they are not explicit in the source code; for example, "
                            "the code generated for <code>synchronized</code> and "
                            "<code>finally</code> blocks can contain implicit try-catch blocks. "
                            "If such an implicitly generated try-catch is "
                            "present on the call stack at the time of the throw, the exception "
                            "will be considered caught even though it appears to be uncaught from "
                            "examination of the source code. "
                        )
                    )
                    (Alt ThreadStart=JDWP.EventKind.THREAD_START
                        "Notification of a new running thread in the target VM. "
                        "The new thread can be the result of a call to "
                        "<code>java.lang.Thread.start</code> or the result of "
                        "attaching a new thread to the VM though JNI. The "
                        "notification is generated by the new thread some time before "
                        "its execution starts. "
                        "Because of this timing, it is possible to receive other events "
                        "for the thread before this event is received. (Notably, "
                        "Method Entry Events and Method Exit Events might occur "
                        "during thread initialization. "
                        "It is also possible for the "
                        "<a href=\"#JDWP_VirtualMachine_AllThreads\">VirtualMachine AllThreads</a> "
                        "command to return "
                        "a thread before its thread start event is received. "
                        "<p>"
                        "Note that this event gives no information "
                        "about the creation of the thread object which may have happened "
                        "much earlier, depending on the VM being debugged. "

                        (int requestID "Request that generated event")
                        (threadObject thread "Started thread")
                    )
                    (Alt ThreadDeath=JDWP.EventKind.THREAD_DEATH
                        "Notification of a completed thread in the target VM. The "
                        "notification is generated by the dying thread before it terminates. "
                        "Because of this timing, it is possible "
                        "for {@link VirtualMachine#allThreads} to return this thread "
                        "after this event is received. "
                        "<p>"
                        "Note that this event gives no information "
                        "about the lifetime of the thread object. It may or may not be collected "
                        "soon depending on what references exist in the target VM. "

                        (int requestID "Request that generated event")
                        (threadObject thread "Ending thread")
                    )
                    (Alt ClassPrepare=JDWP.EventKind.CLASS_PREPARE
                        "Notification of a class prepare in the target VM. See the JVM "
                        "specification for a definition of class preparation. Class prepare "
                        "events are not generated for primtiive classes (for example, "
                        "java.lang.Integer.TYPE). "

                        (int requestID "Request that generated event")
                        (threadObject thread "Preparing thread. "
                             "In rare cases, this event may occur in a debugger system "
                             "thread within the target VM. Debugger threads take precautions "
                             "to prevent these events, but they cannot be avoided under some "
                             "conditions, especially for some subclasses of "
                             "java.lang.Error. "
                             "If the event was generated by a debugger system thread, the "
                             "value returned by this method is null, and if the requested  "
                             "<a href=\"#JDWP_SuspendPolicy\">suspend policy</a> "
                             "for the event was EVENT_THREAD "
                             "all threads will be suspended instead, and the "
                             "composite event's suspend policy will reflect this change. "
                             "<p>"
                             "Note that the discussion above does not apply to system threads "
                             "created by the target VM during its normal (non-debug) operation. "
                        )
                        (byte refTypeTag  "Kind of reference type. "
                           "See <a href=\"#JDWP_TypeTag\">JDWP.TypeTag</a>")
                        (referenceTypeID typeID "Type being prepared")
                        (string signature "Type signature")
                        (int status "Status of type. "
                         "See <a href=\"#JDWP_ClassStatus\">JDWP.ClassStatus</a>")
                    )
                    (Alt ClassUnload=JDWP.EventKind.CLASS_UNLOAD
                         "Notification of a class unload in the target VM. "
                         "<p>"
                         "There are severe constraints on the debugger back-end during "
                         "garbage collection, so unload information is greatly limited. "

                        (int requestID "Request that generated event")
                        (string signature "Type signature")
                    )
                    (Alt FieldAccess=JDWP.EventKind.FIELD_ACCESS
                        "Notification of a field access in the target VM. "
                        "Field modifications "
                        "are not considered field accesses. "
                        "Requires canWatchFieldAccess capability - see "
                        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."

                      (int requestID "Request that generated event")
                        (threadObject thread "Accessing thread")
                        (location location "Location of access")
                        (byte refTypeTag  "Kind of reference type. "
                           "See <a href=\"#JDWP_TypeTag\">JDWP.TypeTag</a>")
                        (referenceTypeID typeID "Type of field")
                        (field fieldID "Field being accessed")
                        (tagged-object object
                                "Object being accessed (null=0 for statics")
                    )
                    (Alt FieldModification=JDWP.EventKind.FIELD_MODIFICATION
                        "Notification of a field modification in the target VM. "
                        "Requires canWatchFieldModification capability - see "
                        "<a href=\"#JDWP_VirtualMachine_CapabilitiesNew\">CapabilitiesNew</a>."

                        (int requestID "Request that generated event")
                        (threadObject thread "Modifying thread")
                        (location location "Location of modify")
                        (byte refTypeTag  "Kind of reference type. "
                           "See <a href=\"#JDWP_TypeTag\">JDWP.TypeTag</a>")
                        (referenceTypeID typeID "Type of field")
                        (field fieldID "Field being modified")
                        (tagged-object object
                                "Object being modified (null=0 for statics")
                        (value valueToBe "Value to be assigned")
                    )
                    (Alt VMDeath=JDWP.EventKind.VM_DEATH
                        (int requestID
                                "Request that generated event")
                    )
                )
            )
        )
    )
)
(ConstantSet Error
    (Constant NONE                   =0   "No error has occurred.")
    (Constant INVALID_THREAD         =10  "Passed thread is null, is not a valid thread or has exited.")
    (Constant INVALID_THREAD_GROUP   =11  "Thread group invalid.")
    (Constant INVALID_PRIORITY       =12  "Invalid priority.")
    (Constant THREAD_NOT_SUSPENDED   =13  "If the specified thread has not been "
                                          "suspended by an event.")
    (Constant THREAD_SUSPENDED       =14  "Thread already suspended.")
    (Constant THREAD_NOT_ALIVE       =15  "Thread has not been started or is now dead.")

    (Constant INVALID_OBJECT         =20  "If this reference type has been unloaded "
                                          "and garbage collected.")
    (Constant INVALID_CLASS          =21  "Invalid class.")
    (Constant CLASS_NOT_PREPARED     =22  "Class has been loaded but not yet prepared.")
    (Constant INVALID_METHODID       =23  "Invalid method.")
    (Constant INVALID_LOCATION       =24  "Invalid location.")
    (Constant INVALID_FIELDID        =25  "Invalid field.")
    (Constant INVALID_FRAMEID        =30  "Invalid jframeID.")
    (Constant NO_MORE_FRAMES         =31  "There are no more Java or JNI frames on the "
                                          "call stack.")
    (Constant OPAQUE_FRAME           =32  "Information about the frame is not available.")
    (Constant NOT_CURRENT_FRAME      =33  "Operation can only be performed on current frame.")
    (Constant TYPE_MISMATCH          =34  "The variable is not an appropriate type for "
                                          "the function used.")
    (Constant INVALID_SLOT           =35  "Invalid slot.")
    (Constant DUPLICATE              =40  "Item already set.")
    (Constant NOT_FOUND              =41  "Desired element not found.")
    (Constant INVALID_MODULE         =42  "Invalid module.")
    (Constant INVALID_MONITOR        =50  "Invalid monitor.")
    (Constant NOT_MONITOR_OWNER      =51  "This thread doesn't own the monitor.")
    (Constant INTERRUPT              =52  "The call has been interrupted before completion.")
    (Constant INVALID_CLASS_FORMAT   =60  "The virtual machine attempted to read a class "
                                          "file and determined that the file is malformed "
                                          "or otherwise cannot be interpreted as a class file.")
    (Constant CIRCULAR_CLASS_DEFINITION
                                     =61  "A circularity has been detected while "
                                          "initializing a class.")
    (Constant FAILS_VERIFICATION     =62  "The verifier detected that a class file, "
                                          "though well formed, contained some sort of "
                                          "internal inconsistency or security problem.")
    (Constant ADD_METHOD_NOT_IMPLEMENTED
                                     =63  "Adding methods has not been implemented.")
    (Constant SCHEMA_CHANGE_NOT_IMPLEMENTED
                                     =64  "Schema change has not been implemented.")
    (Constant INVALID_TYPESTATE      =65  "The state of the thread has been modified, "
                                          "and is now inconsistent.")
    (Constant HIERARCHY_CHANGE_NOT_IMPLEMENTED
                                     =66  "A direct superclass is different for the new class "
                                          "version, or the set of directly implemented "
                                          "interfaces is different "
                                          "and canUnrestrictedlyRedefineClasses is false.")
    (Constant DELETE_METHOD_NOT_IMPLEMENTED
                                     =67  "The new class version does not declare a method "
                                          "declared in the old class version "
                                          "and canUnrestrictedlyRedefineClasses is false.")
    (Constant UNSUPPORTED_VERSION    =68  "A class file has a version number not supported "
                                          "by this VM.")
    (Constant NAMES_DONT_MATCH       =69  "The class name defined in the new class file is "
                                          "different from the name in the old class object.")
    (Constant CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED
                                     =70  "The new class version has different modifiers and "
                                          "canUnrestrictedlyRedefineClasses is false.")
    (Constant METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED
                                     =71  "A method in the new class version has "
                                          "different modifiers "
                                          "than its counterpart in the old class version and "
                                          "canUnrestrictedlyRedefineClasses is false.")
    (Constant CLASS_ATTRIBUTE_CHANGE_NOT_IMPLEMENTED
                                     =72  "The new class version has a different NestHost, "
                                          "NestMembers, PermittedSubclasses, or Record class attribute "
                                          "and canUnrestrictedlyRedefineClasses is false.")
    (Constant NOT_IMPLEMENTED        =99  "The functionality is not implemented in "
                                          "this virtual machine.")
    (Constant NULL_POINTER           =100 "Invalid pointer.")
    (Constant ABSENT_INFORMATION     =101 "Desired information is not available.")
    (Constant INVALID_EVENT_TYPE     =102 "The specified event type id is not recognized.")
    (Constant ILLEGAL_ARGUMENT       =103 "Illegal argument.")
    (Constant OUT_OF_MEMORY          =110 "The function needed to allocate memory and "
                                          "no more memory was available for allocation.")
    (Constant ACCESS_DENIED          =111 "Debugging has not been enabled in this "
                                          "virtual machine. JVMTI cannot be used.")
    (Constant VM_DEAD                =112 "The virtual machine is not running.")
    (Constant INTERNAL               =113 "An unexpected internal error has occurred.")
    (Constant UNATTACHED_THREAD      =115 "The thread being used to call this function "
                                          "is not attached to the virtual machine. "
                                          "Calls must be made from attached threads.")
    (Constant INVALID_TAG            =500 "object type id or class tag.")
    (Constant ALREADY_INVOKING       =502 "Previous invoke not complete.")
    (Constant INVALID_INDEX          =503 "Index is invalid.")
    (Constant INVALID_LENGTH         =504 "The length is invalid.")
    (Constant INVALID_STRING         =506 "The string is invalid.")
    (Constant INVALID_CLASS_LOADER   =507 "The class loader is invalid.")
    (Constant INVALID_ARRAY          =508 "The array is invalid.")
    (Constant TRANSPORT_LOAD         =509 "Unable to load the transport.")
    (Constant TRANSPORT_INIT         =510 "Unable to initialize the transport.")
    (Constant NATIVE_METHOD          =511  )
    (Constant INVALID_COUNT          =512 "The count is invalid.")
)
(ConstantSet EventKind
    (Constant SINGLE_STEP            =1   )
    (Constant BREAKPOINT             =2   )
    (Constant FRAME_POP              =3   )
    (Constant EXCEPTION              =4   )
    (Constant USER_DEFINED           =5   )
    (Constant THREAD_START           =6   )
    (Constant THREAD_DEATH           =7   )
    (Constant THREAD_END             =7   "obsolete - was used in jvmdi")
    (Constant CLASS_PREPARE          =8   )
    (Constant CLASS_UNLOAD           =9   )
    (Constant CLASS_LOAD             =10  )
    (Constant FIELD_ACCESS           =20  )
    (Constant FIELD_MODIFICATION     =21  )
    (Constant EXCEPTION_CATCH        =30  )
    (Constant METHOD_ENTRY           =40  )
    (Constant METHOD_EXIT            =41  )
    (Constant METHOD_EXIT_WITH_RETURN_VALUE =42  )
    (Constant MONITOR_CONTENDED_ENTER          =43  )
    (Constant MONITOR_CONTENDED_ENTERED        =44 )
    (Constant MONITOR_WAIT           =45 )
    (Constant MONITOR_WAITED         =46 )
    (Constant VM_START               =90  )
    (Constant VM_INIT                =90  "obsolete - was used in jvmdi")
    (Constant VM_DEATH               =99  )
    (Constant VM_DISCONNECTED        =100 "Never sent across JDWP")
)

(ConstantSet ThreadStatus
    (Constant ZOMBIE                 =0  )
    (Constant RUNNING                =1  )
    (Constant SLEEPING               =2  )
    (Constant MONITOR                =3  )
    (Constant WAIT                   =4  )
)

(ConstantSet SuspendStatus
    (Constant SUSPEND_STATUS_SUSPENDED = 0x1 )
)
(ConstantSet ClassStatus
    (Constant VERIFIED               =1  )
    (Constant PREPARED               =2  )
    (Constant INITIALIZED            =4  )
    (Constant ERROR                  =8  )
)
(ConstantSet TypeTag
    (Constant CLASS=1 "ReferenceType is a class. ")
    (Constant INTERFACE=2 "ReferenceType is an interface. ")
    (Constant ARRAY=3 "ReferenceType is an array. ")
)
(ConstantSet Tag
    (Constant ARRAY = '[' "'[' - an array object (objectID size). ")
    (Constant BYTE = 'B' "'B' - a byte value (1 byte).")
    (Constant CHAR = 'C' "'C' - a character value (2 bytes).")
    (Constant OBJECT = 'L' "'L' - an object (objectID size).")
    (Constant FLOAT = 'F' "'F' - a float value (4 bytes).")
    (Constant DOUBLE = 'D' "'D' - a double value (8 bytes).")
    (Constant INT = 'I' "'I' - an int value (4 bytes).")
    (Constant LONG = 'J' "'J' - a long value (8 bytes).")
    (Constant SHORT = 'S' "'S' - a short value (2 bytes).")
    (Constant VOID = 'V' "'V' - a void value (no bytes).")
    (Constant BOOLEAN = 'Z' "'Z' - a boolean value (1 byte).")
    (Constant STRING = 's' "'s' - a String object (objectID size). ")
    (Constant THREAD = 't' "'t' - a Thread object (objectID size). ")
    (Constant THREAD_GROUP = 'g'
        "'g' - a ThreadGroup object (objectID size). ")
    (Constant CLASS_LOADER = 'l'
        "'l' - a ClassLoader object (objectID size). ")
    (Constant CLASS_OBJECT = 'c'
        "'c' - a class object object (objectID size). ")
)

(ConstantSet StepDepth
    (Constant INTO = 0
        "Step into any method calls that occur before the end of the step. ")
    (Constant OVER = 1
        "Step over any method calls that occur before the end of the step. ")
    (Constant OUT = 2
        "Step out of the current method. ")
)

(ConstantSet StepSize
    (Constant MIN = 0
        "Step by the minimum possible amount (often a bytecode instruction). ")
    (Constant LINE = 1
        "Step to the next source line unless there is no line number information in which case a MIN step is done instead.")
)

(ConstantSet SuspendPolicy
    (Constant NONE = 0
        "Suspend no threads when this event is encountered. ")
    (Constant EVENT_THREAD = 1
        "Suspend the event thread when this event is encountered. ")
    (Constant ALL = 2
        "Suspend all threads when this event is encountered. ")
)

(ConstantSet InvokeOptions
    "The invoke options are a combination of zero or more of the following bit flags:"
    (Constant INVOKE_SINGLE_THREADED = 0x01
        "otherwise, all threads started. ")
    (Constant INVOKE_NONVIRTUAL = 0x02
        "otherwise, normal virtual invoke (instance methods only)")
)
