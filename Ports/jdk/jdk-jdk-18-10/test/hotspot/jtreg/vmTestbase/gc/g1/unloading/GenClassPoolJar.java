/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.unloading;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;

/**
 * Class that imitates shell script to produce jar file with many similar
 * classes inside.
 *
 * The class generates sources, compiles the first one, applies magic of ASM
 * to multiply classes and packs into classPool.jar
 *
 * Generation template is supposed to be ClassNNN.java.template
 */
public class GenClassPoolJar {

    private final String templateFile;
    private final String destDir;
    private final int count;

    private final File tmpArea;
    private final File pkgDir;

    private static final String JAR_NAME = "classPool.jar";
    private static final String PKG_DIR_NAME = "gc/g1/unloading/rootSetHelper/classesPool";

    public static void main(String args[]) {
       new GenClassPoolJar(args).script();
    }

    /**
     * Creates generator and parses command line args.
     * @param args command line args
     */
    public GenClassPoolJar(String args[]) {
        if (args.length != 3) {
            System.err.println("Usage:");
            System.err.println("java " + GenClassPoolJar.class.getCanonicalName() +
                    " <template-file> <ouput-dir> <count>" );
            throw new Error("Illegal number of parameters");
        }
        templateFile = args[0];
        destDir = args[1];
        count = Integer.parseInt(args[2]);

        tmpArea = new File(destDir, "tmp-area");
        pkgDir = new File(tmpArea, PKG_DIR_NAME);

    }
    /**
     * Does everything.
     */
    public void script() {
        long startTime = System.currentTimeMillis();
        System.out.println("Trying to produce: " + destDir + "/" + JAR_NAME);
        try {

            if (!pkgDir.exists() && !pkgDir.mkdirs()) {
                throw new Error("Failed to create " + pkgDir);
            }


            String javaTemplate = readTemplate(templateFile);
            File java0 = new File(pkgDir, "Class0.java");
            File class0 = new File(pkgDir, "Class0.class");
            writeSource(java0, generateSource(javaTemplate, 0));

            /*
             * Generating and compiling all the sources is not our way -
             * too easy and too slow.
             * We compile just first class and use ASM to obtain others
             * via instrumenting.
             */
            File[] toCompile = {java0};
            compile(toCompile, tmpArea.getAbsolutePath());
            byte[] classTemplate = readFile(class0); // the first compiled class
            createJar(new File(destDir, JAR_NAME), javaTemplate, classTemplate, count);


            deleteFolder(tmpArea);
            long endTime = System.currentTimeMillis();
            System.out.println("Success in " + ((endTime - startTime)/1000) + " seconds");
        } catch (Throwable whatever) {
            throw new Error(whatever);
        }
    }

    /**
     * Generates source number num.
     * @param template template to generate from
     * @param num number
     * @return content of java file
     */
    String generateSource(String template, int num) {
        return template.replaceAll("_NNN_", "" + num);
    }

    /**
     * Reads content of the given file.
     * @param file name of file to read
     * @return file content
     * @throws IOException if something bad has happened
     */
    String readTemplate(String file) throws IOException {
        if (!new File(file).exists()) {
            throw new Error("Template " + file + " doesn't exist");
        }
        List<String> lines = Files.readAllLines(Paths.get(file));
        StringBuilder sb = new StringBuilder();
        for (String line: lines) {
            if (line.trim().startsWith("#")) {
                continue;
            }
            sb.append(line).append(System.lineSeparator());
        }
        return sb.toString();
    }

    /**
     * Writes given content to the given file.
     *
     * @param file to create
     * @param content java source
     * @throws IOException if something bad has happened
     */
    void writeSource(File file, String content) throws IOException {
        List<String> list = Arrays.asList(content.split(System.lineSeparator()));
        Files.write(Paths.get(file.getAbsolutePath()), list);
    }


    /**
     * Compiles given files into given folder.
     *
     * @param files to compile
     * @param destDir where to compile
     * @throws IOException
     */
    void compile(File[] files, String destDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> optionList = new ArrayList<>();
        optionList.addAll(Arrays.asList("-d", destDir));
        StandardJavaFileManager sjfm = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> fileObjects = sjfm.getJavaFileObjects(files);
        JavaCompiler.CompilationTask task = compiler.getTask(null, null, null, optionList, null, fileObjects);
        task.call();
        sjfm.close();
    }

    /**
     * Puts a number of classes and java sources in the given jar.
     *
     * @param jarFile        name of jar file
     * @param javaTemplate   content of java source template
     * @param classTemplate  content of compiled java class
     * @param count          number of classes to generate
     * @throws IOException
     */
    void createJar(File jarFile, String javaTemplate, byte[] classTemplate, int count) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile), new Manifest())) {
            for (int i = 1; i <= count; i++) {
                String name = PKG_DIR_NAME + "/Class" + i;
                jar.putNextEntry(new JarEntry(name + ".java"));
                byte[] content = generateSource(javaTemplate, 0).getBytes();
                jar.write(content, 0, content.length);

                jar.putNextEntry(new JarEntry(name + ".class"));
                content = morphClass(classTemplate, name);
                jar.write(content, 0, content.length);
            }
        }
    }

   byte[] readFile(File f) throws IOException {
       return Files.readAllBytes(Paths.get(f.getAbsolutePath()));
   }

   void writeFile(File f, byte[] content) throws IOException {
        Files.write(Paths.get(f.getAbsolutePath()), content);
   }

   void deleteFolder(File dir) throws IOException {
       Files.walkFileTree(Paths.get(dir.getAbsolutePath()), new FileVisitor<Path>() {

           @Override
           public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
               return FileVisitResult.CONTINUE;
           }

           @Override
           public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
               Files.delete(file);
               return FileVisitResult.CONTINUE;
           }

           @Override
           public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
               return FileVisitResult.CONTINUE;
           }

           @Override
           public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
               Files.delete(dir);
               return FileVisitResult.CONTINUE;
           }

       });
   }

   /**
    * Puts new name on the given class.
    *
    * @param classToMorph  class file content
    * @param newName       new name
    * @return              new class file to write into class
    */
   byte[] morphClass(byte[] classToMorph, String newName) {
       ClassReader cr = new ClassReader(classToMorph);
       ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
       ClassVisitor cv = new ClassRenamer(cw, newName);
       cr.accept(cv, 0);
       return cw.toByteArray();
   }

    /**
     * Visitor to rename class.
     */
    static class ClassRenamer extends ClassVisitor implements Opcodes {
        private final String newName;

        public ClassRenamer(ClassVisitor cv, String newName) {
            super(ASM4, cv);
            this.newName = newName;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            cv.visit(version, access, newName, signature, superName, interfaces);
        }

    }
}
