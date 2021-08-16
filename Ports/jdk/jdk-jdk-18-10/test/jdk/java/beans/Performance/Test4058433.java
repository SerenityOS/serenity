/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 */

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.FeatureDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.ParameterDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/*
 * @test
 * @bug 4058433
 * @summary Generates BeanInfo for public classes in AWT, Accessibility, and Swing
 * @author Sergey Malenkov
 */
public class Test4058433 implements Comparator<Object> {
    @Override
    public int compare(Object one, Object two) {
        if (one instanceof Method && two instanceof Method) {
            Method oneMethod = (Method) one;
            Method twoMethod = (Method) two;
            int result = oneMethod.getName().compareTo(twoMethod.getName());
            if (result != 0) {
                return result;
            }
        }
        if (one instanceof FeatureDescriptor && two instanceof FeatureDescriptor) {
            FeatureDescriptor oneFD = (FeatureDescriptor) one;
            FeatureDescriptor twoFD = (FeatureDescriptor) two;
            int result = oneFD.getName().compareTo(twoFD.getName());
            if (result != 0) {
                return result;
            }
        }
        return one.toString().compareTo(two.toString());
    }

    public static void main(String[] args) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        fs.getFileStores();

        TreeSet<Class<?>> types = new TreeSet<>(new Test4058433());
        Files.walkFileTree(fs.getPath("/modules/java.desktop"), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) {
                file = file.subpath(2, file.getNameCount());
                if (file.startsWith("java/awt/")
                        || file.startsWith("javax/accessibility/")
                        || file.startsWith("javax/swing/")) {
                    String name =file.toString();
                    if (name.endsWith(".class")) {
                        name = name.substring(0, name.indexOf(".")).replace('/', '.');

                        final Class<?> type;
                        try {
                            type = Class.forName(name);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                        if (!BeanInfo.class.isAssignableFrom(type) && !type.isInterface()
                            && !type.isEnum() && !type.isAnnotation()
                            && !type.isAnonymousClass()) {
                            if (null == type.getDeclaringClass()) {
                                types.add(type);
                            }
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("found " + types.size() + " classes");
        long time = -System.currentTimeMillis();
        for (Class<?> type : types) {
            System.out.println("========================================");
            BeanInfo info = Introspector.getBeanInfo(type);

            BeanDescriptor bd = info.getBeanDescriptor();
            System.out.println(bd.getBeanClass());
            print("customizer", bd.getCustomizerClass());
            print(bd);
            print("mono 16x16", info.getIcon(BeanInfo.ICON_MONO_16x16));
            print("mono 32x32", info.getIcon(BeanInfo.ICON_MONO_32x32));
            print("color 16x16", info.getIcon(BeanInfo.ICON_COLOR_16x16));
            print("color 32x32", info.getIcon(BeanInfo.ICON_COLOR_32x32));

            PropertyDescriptor[] pds = info.getPropertyDescriptors();
            PropertyDescriptor dpd = getDefault(pds, info.getDefaultPropertyIndex());
            System.out.println(pds.length + " property descriptors");
            Arrays.sort(pds, new Test4058433());
            for (PropertyDescriptor pd : pds) {
                print(pd);
                if (dpd == pd) {
                    System.out.println("default property");
                }
                print("bound", pd.isBound());
                print("constrained", pd.isConstrained());
                print("property editor", pd.getPropertyEditorClass());
                print("property type", pd.getPropertyType());
                print("read method", pd.getReadMethod());
                print("write method", pd.getWriteMethod());
                if (pd instanceof IndexedPropertyDescriptor) {
                    IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
                    print("indexed property type", ipd.getIndexedPropertyType());
                    print("indexed read method", ipd.getIndexedReadMethod());
                    print("indexed write method", ipd.getIndexedWriteMethod());
                }
            }
            EventSetDescriptor[] esds = info.getEventSetDescriptors();
            EventSetDescriptor desd = getDefault(esds, info.getDefaultEventIndex());
            System.out.println(esds.length + " event set descriptors");
            Arrays.sort(esds, new Test4058433());
            for (EventSetDescriptor esd : esds) {
                print(esd);
                if (desd == esd) {
                    System.out.println("default event set");
                }
                print("in default", esd.isInDefaultEventSet());
                print("unicast", esd.isUnicast());
                print("listener type", esd.getListenerType());
                print("get listener method", esd.getGetListenerMethod());
                print("add listener method", esd.getAddListenerMethod());
                print("remove listener method", esd.getRemoveListenerMethod());
                Method[] methods = esd.getListenerMethods();
                Arrays.sort(methods, new Test4058433());
                for (Method method : methods) {
                    print("listener method", method);
                }
                print(esd.getListenerMethodDescriptors());
            }
            print(info.getMethodDescriptors());
        }
        time += System.currentTimeMillis();
        System.out.println("DONE IN " + time + " MS");
    }

    private static <T> T getDefault(T[] array, int index) {
        return (index == -1) ? null : array[index];
    }

    private static void print(MethodDescriptor[] mds) {
        System.out.println(mds.length + " method descriptors");
        Arrays.sort(mds, new Test4058433());
        for (MethodDescriptor md : mds) {
            print(md);
            print("method", md.getMethod());
            ParameterDescriptor[] pds = md.getParameterDescriptors();
            if (pds != null) {
                System.out.println(pds.length + " parameter descriptors");
                for (ParameterDescriptor pd : pds) {
                    print(pd);
                }
            }
        }
    }

    private static void print(FeatureDescriptor descriptor) {
        String name = descriptor.getName();
        String display = descriptor.getDisplayName();
        String description = descriptor.getShortDescription();
        System.out.println("name: " + name);
        if (!Objects.equals(name, display)) {
            System.out.println("display name: " + display);
        }
        if (!Objects.equals(display, description)) {
            System.out.println("description: " + description.trim());
        }
        print("expert", descriptor.isExpert());
        print("hidden", descriptor.isHidden());
        print("preferred", descriptor.isPreferred());
        TreeMap<String,Object> map = new TreeMap<>();
        Enumeration<String> enumeration = descriptor.attributeNames();
        while (enumeration.hasMoreElements()) {
            String id = enumeration.nextElement();
            Object value = descriptor.getValue(id);
            if (value.getClass().isArray()) {
                TreeSet<String> set = new TreeSet<>();
                int index = 0;
                int length = Array.getLength(value);
                while (index < length) {
                    set.add(Array.get(value, index++) + ", " +
                            Array.get(value, index++) + ", " +
                            Array.get(value, index++));
                }
                value = set.toString();
            }
            map.put(id, value);
        }
        for (Entry<String,Object> entry : map.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private static void print(String id, boolean flag) {
        if (flag) {
            System.out.println(id + " is set");
        }
    }

    private static void print(String id, Class<?> type) {
        if (type != null) {
            System.out.println(id + ": " + type.getName());
        }
    }

    private static void print(String id, Method method) {
        if (method != null) {
            System.out.println(id + ": " + method);
        }
    }

    private static void print(String name, Image image) {
        if (image != null) {
            System.out.println(name + " icon is exist");
        }
    }
}
