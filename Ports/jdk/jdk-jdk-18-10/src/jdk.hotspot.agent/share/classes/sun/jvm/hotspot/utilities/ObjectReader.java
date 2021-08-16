/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.utilities;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;

/**
 * ObjectReader can "deserialize" objects from debuggee.
 *
 * Class Loading:
 *
 * ObjectReader loads classes using the given class loader. If no
 * class loader is supplied, it uses a ProcImageClassLoader, which
 * loads classes from debuggee core or process.

 * Object creation:
 *
 * This class uses no-arg constructor to construct objects. But if
 * there is no no-arg constructor in a given class, then it tries to
 * use other constructors with 'default' values - null for object
 * types, 0, 0.0, false etc. for primitives.  If this process fails to
 * construct an instance (because of null checking by constructor or 0
 * being invalid for an int arg etc.), then null is returned. While
 * constructing complete object graph 'null' is inserted silently on
 * failure and the deserialization continues to construct best-effort
 * object graph.
 *
 * Debug messages:
 *
 * The flag sun.jvm.hotspot.utilities.ObjectReader.DEBUG may be set to
 * non-null to get debug error messages and stack traces.
 *
 * JDK version:
 *
 * JDK classes are loaded by bootstrap class loader and not by the
 * supplied class loader or ProcImageClassLoader. This may create
 * problems if a JDK class evolves. i.e., if SA runs a JDK version
 * different from that of the debuggee, there is a possibility of
 * schema change. It is recommended that the matching JDK version be
 * used to run SA for proper object deserialization.
 *
 */

public class ObjectReader {

   private static final boolean DEBUG;
   static {
      DEBUG = System.getProperty("sun.jvm.hotspot.utilities.ObjectReader.DEBUG") != null;
   }

   public ObjectReader(ClassLoader cl) {
      this.cl = cl;
      this.oopToObjMap = new HashMap<>();
      this.fieldMap = new HashMap<>();
   }

   public ObjectReader() {
      this(new ProcImageClassLoader());
   }

   static void debugPrintln(String msg) {
      if (DEBUG) {
         System.err.println("DEBUG>" + msg);
      }
   }

   static void debugPrintStackTrace(Exception exp) {
      if (DEBUG) {
         StackTraceElement[] els = exp.getStackTrace();
         for (int i = 0; i < els.length; i++) {
            System.err.println("DEBUG>" + els[i].toString());
         }
      }
   }

   public Object readObject(Oop oop) throws ClassNotFoundException {
      if (oop instanceof Instance) {
         return readInstance((Instance) oop);
      } else if (oop instanceof TypeArray){
         return readPrimitiveArray((TypeArray)oop);
      } else if (oop instanceof ObjArray){
         return readObjectArray((ObjArray)oop);
      } else {
         return null;
      }
   }

   protected final Object getDefaultPrimitiveValue(Class clz) {
      if (clz == Boolean.TYPE) {
         return Boolean.FALSE;
      } else if (clz == Character.TYPE) {
         return ' ';
      } else if (clz == Byte.TYPE) {
         return (byte) 0;
      } else if (clz == Short.TYPE) {
         return (short) 0;
      } else if (clz == Integer.TYPE) {
         return 0;
      } else if (clz == Long.TYPE) {
         return 0L;
      } else if (clz == Float.TYPE) {
         return 0.0f;
      } else if (clz == Double.TYPE) {
         return 0.0;
      } else {
         throw new RuntimeException("should not reach here!");
      }
   }

   protected String javaLangString;
   protected String javaUtilHashtableEntry;
   protected String javaUtilHashtable;
   protected String javaUtilProperties;

   protected String javaLangString() {
      if (javaLangString == null) {
         javaLangString = "java/lang/String";
      }
      return javaLangString;
   }

   protected String javaUtilHashtableEntry() {
      if (javaUtilHashtableEntry == null) {
         javaUtilHashtableEntry = "java/util/Hashtable$Entry";
      }
      return javaUtilHashtableEntry;
   }

   protected String javaUtilHashtable() {
      if (javaUtilHashtable == null) {
         javaUtilHashtable = "java/util/Hashtable";
      }
      return javaUtilHashtable;
   }

   protected String javaUtilProperties() {
      if (javaUtilProperties == null) {
         javaUtilProperties = "java/util/Properties";
      }
      return javaUtilProperties;
   }

   private void setHashtableEntry(java.util.Hashtable<Object, Object> p, Oop oop) {
      InstanceKlass ik = (InstanceKlass)oop.getKlass();
      OopField keyField = (OopField)ik.findField("key", "Ljava/lang/Object;");
      OopField valueField = (OopField)ik.findField("value", "Ljava/lang/Object;");
      OopField nextField = (OopField)ik.findField("next", "Ljava/util/Hashtable$Entry;");
      if (DEBUG) {
         if (Assert.ASSERTS_ENABLED) {
            Assert.that(ik.getName().equals(javaUtilHashtableEntry()), "Not a Hashtable$Entry?");
            Assert.that(keyField != null && valueField != null && nextField != null, "Invalid fields!");
         }
      }

      Object key = null;
      Object value = null;
      Oop next = null;
      try {
         key = readObject(keyField.getValue(oop));
         value = readObject(valueField.getValue(oop));
         next =  (Oop)nextField.getValue(oop);
         // For Properties, should use setProperty(k, v). Since it only runs in SA
         // using put(k, v) should be OK.
         p.put(key, value);
         if (next != null) {
            setHashtableEntry(p, next);
         }
      } catch (ClassNotFoundException ce) {
         if( DEBUG) {
            debugPrintln("Class not found " + ce);
            debugPrintStackTrace(ce);
         }
      }
   }

   private void setPropertiesEntry(java.util.Properties p, Oop oop) {
      InstanceKlass ik = (InstanceKlass)oop.getKlass();
      OopField keyField = (OopField)ik.findField("key", "Ljava/lang/Object;");
      OopField valueField = (OopField)ik.findField("val", "Ljava/lang/Object;");
      OopField nextField = (OopField)ik.findField("next", "Ljava/util/concurrent/ConcurrentHashMap$Node;");

      try {
         p.setProperty((String)readObject(keyField.getValue(oop)),
                       (String)readObject(valueField.getValue(oop)));
      } catch (ClassNotFoundException ce) {
         if (DEBUG) {
            debugPrintStackTrace(ce);
         }
      }
      // If this hashmap table Node is chained, then follow the chain to the next Node.
      Oop chainedOop = nextField.getValue(oop);
      if (chainedOop != null) {
          setPropertiesEntry(p, chainedOop);
      }
   }

   protected Object getHashtable(Instance oop) {
      InstanceKlass k = (InstanceKlass)oop.getKlass();
      OopField tableField = (OopField)k.findField("table", "[Ljava/util/Hashtable$Entry;");
      if (tableField == null) {
         debugPrintln("Could not find field of [Ljava/util/Hashtable$Entry;");
         return null;
      }
      java.util.Hashtable<Object, Object> table = new java.util.Hashtable<>();
      ObjArray kvs = (ObjArray)tableField.getValue(oop);
      long size = kvs.getLength();
      debugPrintln("Hashtable$Entry Size = " + size);
      for (long i=0; i<size; i++) {
         Oop entry = kvs.getObjAt(i);
         if (entry != null && entry.isInstance()) {
            setHashtableEntry(table, entry);
         }
      }
      return table;
   }

   private Properties getProperties(Instance oop) {
      InstanceKlass k = (InstanceKlass)oop.getKlass();
      OopField mapField = (OopField)k.findField("map", "Ljava/util/concurrent/ConcurrentHashMap;");
      if (mapField == null) {
         debugPrintln("Could not find field of Ljava/util/concurrent/ConcurrentHashMap");
         return null;
      }

      Instance mapObj = (Instance)mapField.getValue(oop);
      if (mapObj == null) {
         debugPrintln("Could not get map field from java.util.Properties");
         return null;
      }

      InstanceKlass mk = (InstanceKlass)mapObj.getKlass();
      OopField tableField = (OopField)mk.findField("table", "[Ljava/util/concurrent/ConcurrentHashMap$Node;");
      if (tableField == null) {
         debugPrintln("Could not find field of [Ljava/util/concurrent/ConcurrentHashMap$Node");
         return null;
      }

      java.util.Properties props = new java.util.Properties();
      ObjArray kvs = (ObjArray)tableField.getValue(mapObj);
      long size = kvs.getLength();
      debugPrintln("ConcurrentHashMap$Node Size = " + size);
      LongStream.range(0, size)
                .mapToObj(kvs::getObjAt)
                .filter(o -> o != null)
                .forEach(o -> setPropertiesEntry(props, o));

      return props;
   }

   public Object readInstance(Instance oop) throws ClassNotFoundException {
      Object result = getFromObjTable(oop);
      if (result == null) {
         InstanceKlass kls = (InstanceKlass) oop.getKlass();
         // Handle java.lang.String instances differently. As part of JSR-133, fields of immutable
         // classes have been made final. The algorithm below will not be able to read Strings from
         // debuggee (can't use reflection to set final fields). But, need to read Strings is very
         // important.
         // Same for Hashtable, key and hash are final, could not be set in the algorithm too.
         // FIXME: need a framework to handle many other special cases.
         if (kls.getName().equals(javaLangString())) {
            return OopUtilities.stringOopToString(oop);
         }

         if (kls.getName().equals(javaUtilHashtable())) {
            return getHashtable(oop);
         }

         if (kls.getName().equals(javaUtilProperties())) {
            return getProperties(oop);
         }

         Class<?> clz = readClass(kls);
         try {
            result = clz.getDeclaredConstructor().newInstance();
         } catch (Exception ex) {
            // no-arg constructor failed to create object. Let us try
            // to call constructors one-by-one with default arguments
            // (null for objects, 0/0.0 etc. for primitives) till we
            // succeed or fail on all constructors.

            java.lang.reflect.Constructor[] ctrs = clz.getDeclaredConstructors();
            for (int n = 0; n < ctrs.length; n++) {
               java.lang.reflect.Constructor c = ctrs[n];
               Class[] paramTypes = c.getParameterTypes();
               Object[] params = new Object[paramTypes.length];
               for (int i = 0; i < params.length; i++) {
                  if (paramTypes[i].isPrimitive()) {
                     params[i] = getDefaultPrimitiveValue(paramTypes[i]);
                  }
               }
               try {
                  c.setAccessible(true);
                  result = c.newInstance(params);
                  break;
               } catch (Exception exp) {
                  if (DEBUG) {
                     debugPrintln("Can't create object using " + c);
                     debugPrintStackTrace(exp);
                  }
               }
            }
         }

         if (result != null) {
            putIntoObjTable(oop, result);
            oop.iterate(new FieldSetter(result), false);
         }
      }
      return result;
   }

   public Object readPrimitiveArray(final TypeArray array) {

      Object result = getFromObjTable(array);
      if (result == null) {
         int length = (int) array.getLength();
         TypeArrayKlass klass = (TypeArrayKlass) array.getKlass();
         int type = (int) klass.getElementType();
         switch (type) {
            case TypeArrayKlass.T_BOOLEAN: {
               final boolean[] arrayObj = new boolean[length];
               array.iterate(new DefaultOopVisitor() {
                                public void doBoolean(BooleanField field, boolean isVMField) {
                                   IndexableFieldIdentifier ifd = (IndexableFieldIdentifier) field.getID();
                                   arrayObj[ifd.getIndex()] = field.getValue(array);
                                }
                            }, false);
               result = arrayObj;
               }
               break;

            case TypeArrayKlass.T_CHAR: {
               final char[] arrayObj = new char[length];
               array.iterate(new DefaultOopVisitor() {
                                public void doChar(CharField field, boolean isVMField) {
                                   IndexableFieldIdentifier ifd = (IndexableFieldIdentifier) field.getID();
                                   arrayObj[ifd.getIndex()] = field.getValue(array);
                                }
                            }, false);
               result = arrayObj;
               }
               break;

            case TypeArrayKlass.T_FLOAT: {
               final float[] arrayObj = new float[length];
               array.iterate(new DefaultOopVisitor() {
                                public void doFloat(FloatField field, boolean isVMField) {
                                   IndexableFieldIdentifier ifd = (IndexableFieldIdentifier) field.getID();
                                   arrayObj[ifd.getIndex()] = field.getValue(array);
                                }
                            }, false);
               result = arrayObj;
               }
               break;

            case TypeArrayKlass.T_DOUBLE: {
               final double[] arrayObj = new double[length];
               array.iterate(new DefaultOopVisitor() {
                                public void doDouble(DoubleField field, boolean isVMField) {
                                   IndexableFieldIdentifier ifd = (IndexableFieldIdentifier) field.getID();
                                   arrayObj[ifd.getIndex()] = field.getValue(array);
                                }
                            }, false);
               result = arrayObj;
               }
               break;

            case TypeArrayKlass.T_BYTE: {
               final byte[] arrayObj = new byte[length];
               array.iterate(new DefaultOopVisitor() {
                                public void doByte(ByteField field, boolean isVMField) {
                                   IndexableFieldIdentifier ifd = (IndexableFieldIdentifier) field.getID();
                                   arrayObj[ifd.getIndex()] = field.getValue(array);
                                }
                            }, false);
               result = arrayObj;
               }
               break;

            case TypeArrayKlass.T_SHORT: {
               final short[] arrayObj = new short[length];
               array.iterate(new DefaultOopVisitor() {
                                public void doShort(ShortField field, boolean isVMField) {
                                   IndexableFieldIdentifier ifd = (IndexableFieldIdentifier) field.getID();
                                   arrayObj[ifd.getIndex()] = field.getValue(array);
                                }
                            }, false);
               result = arrayObj;
               }
               break;

            case TypeArrayKlass.T_INT: {
               final int[] arrayObj = new int[length];
               array.iterate(new DefaultOopVisitor() {
                                public void doInt(IntField field, boolean isVMField) {
                                   IndexableFieldIdentifier ifd = (IndexableFieldIdentifier) field.getID();
                                   arrayObj[ifd.getIndex()] = field.getValue(array);
                                }
                            }, false);
               result = arrayObj;
               }
               break;

            case TypeArrayKlass.T_LONG: {
               final long[] arrayObj = new long[length];
               array.iterate(new DefaultOopVisitor() {
                                public void doLong(LongField field, boolean isVMField) {
                                   IndexableFieldIdentifier ifd = (IndexableFieldIdentifier) field.getID();
                                   arrayObj[ifd.getIndex()] = field.getValue(array);
                                }
                            }, false);
               result = arrayObj;
               }
               break;

            default:
               throw new RuntimeException("should not reach here!");
         }

         putIntoObjTable(array, result);
      }
      return result;
   }

   protected final boolean isRobust(OopHandle handle) {
      return RobustOopDeterminator.oopLooksValid(handle);
   }

   public Object readObjectArray(final ObjArray array) throws ClassNotFoundException {
       Object result = getFromObjTable(array);
       if (result == null) {
          int length = (int) array.getLength();
          ObjArrayKlass klass = (ObjArrayKlass) array.getKlass();
          Klass bottomKls = klass.getBottomKlass();
          Class bottomCls = null;
          final int dimension = (int) klass.getDimension();
          int[] dimArray = null;
          if (bottomKls instanceof InstanceKlass) {
             bottomCls = readClass((InstanceKlass) bottomKls);
             dimArray = new int[dimension];
          } else { // instanceof TypeArrayKlass
             TypeArrayKlass botKls = (TypeArrayKlass) bottomKls;
             dimArray = new int[dimension -1];
          }
          // initialize the length
          dimArray[0] = length;
          final Object[] arrayObj = (Object[]) java.lang.reflect.Array.newInstance(bottomCls, dimArray);
          putIntoObjTable(array, arrayObj);
          result = arrayObj;
          array.iterate(new DefaultOopVisitor() {
                               public void doOop(OopField field, boolean isVMField) {
                                  OopHandle handle = field.getValueAsOopHandle(getObj());
                                  if (! isRobust(handle)) {
                                     return;
                                  }

                                  IndexableFieldIdentifier ifd = (IndexableFieldIdentifier) field.getID();
                                  try {
                                     arrayObj[ifd.getIndex()] = readObject(field.getValue(getObj()));
                                  } catch (Exception e) {
                                     if (DEBUG) {
                                        debugPrintln("Array element set failed for " + ifd);
                                        debugPrintStackTrace(e);
                                     }
                                  }
                               }
                        }, false);
       }
       return result;
   }

   protected class FieldSetter extends DefaultOopVisitor {
      protected Object obj;

      public FieldSetter(Object obj) {
         this.obj = obj;
      }

      private void printFieldSetError(java.lang.reflect.Field f, Exception ex) {
         if (DEBUG) {
            if (f != null) debugPrintln("Field set failed for " + f);
            debugPrintStackTrace(ex);
         }
      }

      // Callback methods for each field type in an object
      public void doOop(OopField field, boolean isVMField) {
         OopHandle handle = field.getValueAsOopHandle(getObj());
         if (! isRobust(handle) ) {
            return;
         }

         java.lang.reflect.Field f = null;
         try {
            f = readField(field);
            if (Modifier.isFinal(f.getModifiers())) return;
            f.setAccessible(true);
            f.set(obj, readObject(field.getValue(getObj())));
         } catch (Exception ex) {
            printFieldSetError(f, ex);
         }
      }

      public void doByte(ByteField field, boolean isVMField) {
         java.lang.reflect.Field f = null;
         try {
            f = readField(field);
            if (Modifier.isFinal(f.getModifiers())) return;
            f.setAccessible(true);
            f.setByte(obj, field.getValue(getObj()));
         } catch (Exception ex) {
            printFieldSetError(f, ex);
         }
      }

      public void doChar(CharField field, boolean isVMField) {
         java.lang.reflect.Field f = null;
         try {
            f = readField(field);
            if (Modifier.isFinal(f.getModifiers())) return;
            f.setAccessible(true);
            f.setChar(obj, field.getValue(getObj()));
         } catch (Exception ex) {
            printFieldSetError(f, ex);
         }
      }

      public void doBoolean(BooleanField field, boolean isVMField) {
         java.lang.reflect.Field f = null;
         try {
            f = readField(field);
            if (Modifier.isFinal(f.getModifiers())) return;
            f.setAccessible(true);
            f.setBoolean(obj, field.getValue(getObj()));
         } catch (Exception ex) {
            printFieldSetError(f, ex);
         }
      }

      public void doShort(ShortField field, boolean isVMField) {
         java.lang.reflect.Field f = null;
         try {
            f = readField(field);
            if (Modifier.isFinal(f.getModifiers())) return;
            f.setAccessible(true);
            f.setShort(obj, field.getValue(getObj()));
         } catch (Exception ex) {
            printFieldSetError(f, ex);
         }
      }

      public void doInt(IntField field, boolean isVMField) {
         java.lang.reflect.Field f = null;
         try {
            f = readField(field);
            if (Modifier.isFinal(f.getModifiers())) return;
            f.setAccessible(true);
            f.setInt(obj, field.getValue(getObj()));
         } catch (Exception ex) {
            printFieldSetError(f, ex);
         }
      }

      public void doLong(LongField field, boolean isVMField) {
         java.lang.reflect.Field f = null;
         try {
            f = readField(field);
            if (Modifier.isFinal(f.getModifiers())) return;
            f.setAccessible(true);
            f.setLong(obj, field.getValue(getObj()));
         } catch (Exception ex) {
            printFieldSetError(f, ex);
         }
      }

      public void doFloat(FloatField field, boolean isVMField) {
         java.lang.reflect.Field f = null;
         try {
            f = readField(field);
            if (Modifier.isFinal(f.getModifiers())) return;
            f.setAccessible(true);
            f.setFloat(obj, field.getValue(getObj()));
         } catch (Exception ex) {
            printFieldSetError(f, ex);
         }
      }

      public void doDouble(DoubleField field, boolean isVMField) {
         java.lang.reflect.Field f = null;
         try {
            f = readField(field);
            if (Modifier.isFinal(f.getModifiers())) return;
            f.setAccessible(true);
            f.setDouble(obj, field.getValue(getObj()));
         } catch (Exception ex) {
            printFieldSetError(f, ex);
         }
      }

      public void doCInt(CIntField field, boolean isVMField) {
         throw new RuntimeException("should not reach here!");
      }
   }

   public Class readClass(InstanceKlass kls) throws ClassNotFoundException {
      Class cls = (Class) getFromObjTable(kls);
      if (cls == null) {
         cls = Class.forName(kls.getName().asString().replace('/', '.'), true, cl);
         putIntoObjTable(kls, cls);
      }
      return cls;
   }

   public Object readMethodOrConstructor(sun.jvm.hotspot.oops.Method m)
                     throws NoSuchMethodException, ClassNotFoundException {
      String name = m.getName().asString();
      if (name.equals("<init>")) {
         return readConstructor(m);
      } else {
         return readMethod(m);
      }
   }

   public java.lang.reflect.Method readMethod(sun.jvm.hotspot.oops.Method m)
            throws NoSuchMethodException, ClassNotFoundException {
      java.lang.reflect.Method result = (java.lang.reflect.Method) getFromObjTable(m);
      if (result == null) {
         Class<?> clz = readClass(m.getMethodHolder());
         String name = m.getName().asString();
         Class[] paramTypes = getParamTypes(m.getSignature());
         result = clz.getMethod(name, paramTypes);
         putIntoObjTable(m, result);
      }
      return result;
   }

   public java.lang.reflect.Constructor readConstructor(sun.jvm.hotspot.oops.Method m)
            throws NoSuchMethodException, ClassNotFoundException {
      java.lang.reflect.Constructor result = (java.lang.reflect.Constructor) getFromObjTable(m);
      if (result == null) {
         Class<?> clz = readClass(m.getMethodHolder());
         String name = m.getName().asString();
         Class[] paramTypes = getParamTypes(m.getSignature());
         result = clz.getDeclaredConstructor(paramTypes);
         putIntoObjTable(m, result);
      }
      return result;
   }

   public java.lang.reflect.Field readField(sun.jvm.hotspot.oops.Field f)
            throws NoSuchFieldException, ClassNotFoundException {
      java.lang.reflect.Field result = (java.lang.reflect.Field) fieldMap.get(f);
      if (result == null) {
         FieldIdentifier fieldId = f.getID();
         Class clz = readClass((InstanceKlass) f.getFieldHolder());
         String name = fieldId.getName();
         try {
            result = clz.getField(name);
         } catch (NoSuchFieldException nsfe) {
            result = clz.getDeclaredField(name);
         }
         fieldMap.put(f, result);
      }
      return result;
   }

   protected final ClassLoader cl;
   protected Map<Object, Object> oopToObjMap;
   protected Map<sun.jvm.hotspot.oops.Field, java.lang.reflect.Field> fieldMap;

   protected void putIntoObjTable(Oop oop, Object obj) {
      oopToObjMap.put(oop, obj);
   }

   protected Object getFromObjTable(Oop oop) {
      return oopToObjMap.get(oop);
   }

   protected void putIntoObjTable(Metadata oop, Object obj) {
      oopToObjMap.put(oop, obj);
   }

   protected Object getFromObjTable(Metadata oop) {
      return oopToObjMap.get(oop);
   }

   protected class SignatureParser extends SignatureIterator {
      protected Vector<Class<?>> tmp = new Vector<>();

      public SignatureParser(Symbol s) {
         super(s);
      }

      public void doBool  () { tmp.add(Boolean.TYPE);    }
      public void doChar  () { tmp.add(Character.TYPE);  }
      public void doFloat () { tmp.add(Float.TYPE);      }
      public void doDouble() { tmp.add(Double.TYPE);     }
      public void doByte  () { tmp.add(Byte.TYPE);       }
      public void doShort () { tmp.add(Short.TYPE);      }
      public void doInt   () { tmp.add(Integer.TYPE);    }
      public void doLong  () { tmp.add(Long.TYPE);       }
      public void doVoid  () {
         if(isReturnType()) {
            tmp.add(Void.TYPE);
         } else {
            throw new RuntimeException("should not reach here");
         }
      }

      public void doObject(int begin, int end) {
         tmp.add(getClass(begin, end));
      }

      public void doArray (int begin, int end) {
        int inner = arrayInnerBegin(begin);
        Class elemCls = null;
        switch (_signature.getByteAt(inner)) {
        case 'B': elemCls = Boolean.TYPE; break;
        case 'C': elemCls = Character.TYPE; break;
        case 'D': elemCls = Double.TYPE; break;
        case 'F': elemCls = Float.TYPE; break;
        case 'I': elemCls = Integer.TYPE; break;
        case 'J': elemCls = Long.TYPE; break;
        case 'S': elemCls = Short.TYPE; break;
        case 'Z': elemCls = Boolean.TYPE; break;
        case 'L': elemCls = getClass(inner + 1, end); break;
        default: break;
        }

        int dimension = inner - begin;
        // create 0 x 0 ... array and get class from that
        int[] dimArray = new int[dimension];
        tmp.add(java.lang.reflect.Array.newInstance(elemCls, dimArray).getClass());
      }

      protected Class getClass(int begin, int end) {
         String className = getClassName(begin, end);
         try {
            return Class.forName(className, true, cl);
         } catch (Exception e) {
            if (DEBUG) {
               debugPrintln("Can't load class " + className);
            }
            throw new RuntimeException(e);
         }
      }

      protected String getClassName(int begin, int end) {
         StringBuilder buf = new StringBuilder();
         for (int i = begin; i < end; i++) {
            char c = (char) (_signature.getByteAt(i) & 0xFF);
            if (c == '/') {
               buf.append('.');
            } else {
               buf.append(c);
            }
         }
         return buf.toString();
      }

      protected int arrayInnerBegin(int begin) {
         while (_signature.getByteAt(begin) == '[') {
           ++begin;
         }
         return begin;
      }

      public int getNumParams() {
         return tmp.size();
      }

      public Enumeration getParamTypes() {
         return tmp.elements();
      }
   }

   protected Class[] getParamTypes(Symbol signature) {
      SignatureParser sp = new SignatureParser(signature);
      sp.iterateParameters();
      Class result[] = new Class[sp.getNumParams()];
      Enumeration e = sp.getParamTypes();
      int i = 0;
      while (e.hasMoreElements()) {
         result[i] = (Class) e.nextElement();
         i++;
      }
      return result;
   }
}
