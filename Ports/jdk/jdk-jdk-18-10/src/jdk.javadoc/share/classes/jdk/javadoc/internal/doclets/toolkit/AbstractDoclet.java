/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.StandardDoclet;
import jdk.javadoc.internal.doclets.formats.html.HtmlDoclet;
import jdk.javadoc.internal.doclets.toolkit.builders.AbstractBuilder;
import jdk.javadoc.internal.doclets.toolkit.builders.BuilderFactory;
import jdk.javadoc.internal.doclets.toolkit.util.ClassTree;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.UncheckedDocletException;
import jdk.javadoc.internal.doclets.toolkit.util.InternalException;
import jdk.javadoc.internal.doclets.toolkit.util.ElementListWriter;
import jdk.javadoc.internal.doclets.toolkit.util.ResourceIOException;
import jdk.javadoc.internal.doclets.toolkit.util.SimpleDocletException;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

import static javax.tools.Diagnostic.Kind.*;

/**
 * An abstract implementation of a Doclet.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public abstract class AbstractDoclet implements Doclet {

    /**
     * The global configuration information for this run.
     */
    private BaseConfiguration configuration;

    protected Messages messages;

    /*
     *  a handle to our utility methods
     */
    protected Utils utils;

    /**
     * The only doclet that may use this toolkit is {@value}
     */
    private static final String TOOLKIT_DOCLET_NAME =
        jdk.javadoc.internal.doclets.formats.html.HtmlDoclet.class.getName();

    /**
     * Verify that the only doclet that is using this toolkit is
     * #TOOLKIT_DOCLET_NAME.
     */
    private boolean isValidDoclet() {
        if (!getClass().getName().equals(TOOLKIT_DOCLET_NAME)) {
            messages.error("doclet.Toolkit_Usage_Violation",
                TOOLKIT_DOCLET_NAME);
            return false;
        }
        return true;
    }

    /**
     * The method that starts the execution of the doclet.
     *
     * @param docEnv   the {@link DocletEnvironment}.
     * @return true if the doclet executed without error.  False otherwise.
     */
    @Override
    public boolean run(DocletEnvironment docEnv) {
        configuration = getConfiguration();
        configuration.initConfiguration(docEnv, getResourceKeyMapper(docEnv));
        utils = configuration.utils;
        messages = configuration.getMessages();
        BaseOptions options = configuration.getOptions();

        if (!isValidDoclet()) {
            return false;
        }

        try {
            try {
                startGeneration();
                return true;
            } catch (UncheckedDocletException e) {
                throw (DocletException) e.getCause();
            }

        } catch (DocFileIOException e) {
            switch (e.mode) {
                case READ:
                    messages.error("doclet.exception.read.file",
                            e.fileName.getPath(), e.getCause());
                    break;
                case WRITE:
                    messages.error("doclet.exception.write.file",
                            e.fileName.getPath(), e.getCause());
            }
            dumpStack(options.dumpOnError(), e);

        } catch (ResourceIOException e) {
            messages.error("doclet.exception.read.resource",
                    e.resource.getPath(), e.getCause());
            dumpStack(options.dumpOnError(), e);

        } catch (SimpleDocletException e) {
            configuration.reporter.print(ERROR, e.getMessage());
            dumpStack(options.dumpOnError(), e);

        } catch (InternalException e) {
            configuration.reporter.print(ERROR, e.getMessage());
            reportInternalError(e.getCause());

        } catch (DocletException | RuntimeException | Error e) {
            messages.error("doclet.internal.exception", e);
            reportInternalError(e);
        }

        return false;
    }

    protected Function<String, String> getResourceKeyMapper(DocletEnvironment docEnv) {
        return null;
    }

    private void reportInternalError(Throwable t) {
        if (getClass().equals(StandardDoclet.class) || getClass().equals(HtmlDoclet.class)) {
            System.err.println(configuration.getDocResources().getText("doclet.internal.report.bug"));
        }
        dumpStack(true, t);
    }

    private void dumpStack(boolean enabled, Throwable t) {
        if (enabled && t != null) {
            t.printStackTrace(System.err);
        }
    }

    /**
     * Returns the SourceVersion indicating the features supported by this doclet.
     *
     * @return SourceVersion
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_9;
    }

    /**
     * Create the configuration instance and returns it.
     *
     * @return the configuration of the doclet.
     */
    public abstract BaseConfiguration getConfiguration();

    /**
     * Start the generation of files. Call generate methods in the individual
     * writers, which will in turn generate the documentation files. Call the
     * TreeWriter generation first to ensure the Class Hierarchy is built
     * first and then can be used in the later generation.
     *
     * @throws DocletException if there is a problem while generating the documentation
     */
    private void startGeneration() throws DocletException {

        // Modules with no documented classes may be specified on the
        // command line to specify a service provider, allow these.
        if (configuration.getSpecifiedModuleElements().isEmpty() &&
                configuration.getIncludedTypeElements().isEmpty()) {
            messages.error("doclet.No_Public_Classes_To_Document");
            return;
        }
        if (!configuration.setOptions()) {
            return;
        }
        messages.notice("doclet.build_version",
            configuration.getDocletVersion());
        ClassTree classtree = new ClassTree(configuration, configuration.getOptions().noDeprecated());

        generateClassFiles(classtree);

        ElementListWriter.generate(configuration);
        generatePackageFiles(classtree);
        generateModuleFiles();

        generateOtherFiles(classtree);
        configuration.tagletManager.printReport();
    }

    /**
     * Generate additional documentation that is added to the API documentation.
     *
     * @param classtree the data structure representing the class tree
     * @throws DocletException if there is a problem while generating the documentation
     */
    protected void generateOtherFiles(ClassTree classtree) throws DocletException {
        BuilderFactory builderFactory = configuration.getBuilderFactory();
        AbstractBuilder constantsSummaryBuilder = builderFactory.getConstantsSummaryBuilder();
        constantsSummaryBuilder.build();
        AbstractBuilder serializedFormBuilder = builderFactory.getSerializedFormBuilder();
        serializedFormBuilder.build();
    }

    /**
     * Generate the module documentation.
     *
     * @throws DocletException if there is a problem while generating the documentation
     *
     */
    protected abstract void generateModuleFiles() throws DocletException;

    /**
     * Generate the package documentation.
     *
     * @param classtree the data structure representing the class tree
     * @throws DocletException if there is a problem while generating the documentation
     */
    protected abstract void generatePackageFiles(ClassTree classtree) throws DocletException;

    /**
     * Generate the class documentation.
     *
     * @param arr the set of types to be documented
     * @param classtree the data structure representing the class tree
     * @throws DocletException if there is a problem while generating the documentation
     */
    protected abstract void generateClassFiles(SortedSet<TypeElement> arr, ClassTree classtree)
            throws DocletException;

    /**
     * Iterate through all classes and construct documentation for them.
     *
     * @param classtree the data structure representing the class tree
     * @throws DocletException if there is a problem while generating the documentation
     */
    protected void generateClassFiles(ClassTree classtree)
            throws DocletException {

        SortedSet<TypeElement> classes = new TreeSet<>(utils.comparators.makeGeneralPurposeComparator());

        // handle classes specified as files on the command line
        for (PackageElement pkg : configuration.typeElementCatalog.packages()) {
            classes.addAll(configuration.typeElementCatalog.allClasses(pkg));
        }

        // handle classes specified in modules and packages on the command line
        SortedSet<PackageElement> packages = new TreeSet<>(utils.comparators.makePackageComparator());
        packages.addAll(configuration.getSpecifiedPackageElements());
        configuration.modulePackages.values().stream().forEach(packages::addAll);
        for (PackageElement pkg : packages) {
            classes.addAll(utils.getAllClasses(pkg));
        }

        generateClassFiles(classes, classtree);
    }
}
