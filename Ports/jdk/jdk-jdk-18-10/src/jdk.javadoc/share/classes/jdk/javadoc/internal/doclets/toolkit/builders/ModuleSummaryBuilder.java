/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.builders;

import javax.lang.model.element.ModuleElement;

import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.DocFilesHandler;
import jdk.javadoc.internal.doclets.toolkit.DocletException;
import jdk.javadoc.internal.doclets.toolkit.ModuleSummaryWriter;


/**
 * Builds the summary for a given module.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ModuleSummaryBuilder extends AbstractBuilder {

    /**
     * The module being documented.
     */
    private final ModuleElement mdle;

    /**
     * The doclet specific writer that will output the result.
     */
    private final ModuleSummaryWriter moduleWriter;

    /**
     * Construct a new ModuleSummaryBuilder.
     *
     * @param context  the build context.
     * @param mdle the module being documented.
     * @param moduleWriter the doclet specific writer that will output the
     *        result.
     */
    private ModuleSummaryBuilder(Context context,
            ModuleElement mdle, ModuleSummaryWriter moduleWriter) {
        super(context);
        this.mdle = mdle;
        this.moduleWriter = moduleWriter;
    }

    /**
     * Construct a new ModuleSummaryBuilder.
     *
     * @param context  the build context.
     * @param mdle the module being documented.
     * @param moduleWriter the doclet specific writer that will output the
     *        result.
     *
     * @return an instance of a ModuleSummaryBuilder.
     */
    public static ModuleSummaryBuilder getInstance(Context context,
            ModuleElement mdle, ModuleSummaryWriter moduleWriter) {
        return new ModuleSummaryBuilder(context, mdle, moduleWriter);
    }

    /**
     * Build the module summary.
     *
     * @throws DocletException if there is a problem while building the documentation
     */
    @Override
    public void build() throws DocletException {
        if (moduleWriter == null) {
            //Doclet does not support this output.
            return;
        }
        buildModuleDoc();
    }

    /**
     * Build the module documentation.
     *
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildModuleDoc() throws DocletException {
        Content contentTree = moduleWriter.getModuleHeader(mdle.getQualifiedName().toString());

        buildContent();

        moduleWriter.addModuleFooter();
        moduleWriter.printDocument(contentTree);
        DocFilesHandler docFilesHandler = configuration.getWriterFactory().getDocFilesHandler(mdle);
        docFilesHandler.copyDocFiles();
    }

    /**
     * Build the content for the module doc.
     *
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildContent() throws DocletException {
        Content moduleContentTree = moduleWriter.getContentHeader();

        moduleWriter.addModuleSignature(moduleContentTree);
        buildModuleDescription(moduleContentTree);
        buildSummary(moduleContentTree);

        moduleWriter.addModuleContent(moduleContentTree);
    }

    /**
     * Builds the list of summary sections for this module.
     *
     * @param moduleContentTree the module content tree to which the summaries will
     *                           be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildSummary(Content moduleContentTree) throws DocletException {
        Content summariesList = moduleWriter.getSummariesList();

        buildPackagesSummary(summariesList);
        buildModulesSummary(summariesList);
        buildServicesSummary(summariesList);

        moduleContentTree.add(moduleWriter.getSummaryTree(summariesList));
    }

    /**
     * Builds the summary of the module dependencies of this module.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildModulesSummary(Content summariesList) {
        moduleWriter.addModulesSummary(summariesList);
    }

    /**
     * Builds the summary of the packages exported or opened by this module.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildPackagesSummary(Content summariesList) {
        moduleWriter.addPackagesSummary(summariesList);
    }

    /**
     * Builds the summary of the services used or provided by this module.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildServicesSummary(Content summariesList) {
        moduleWriter.addServicesSummary(summariesList);
    }

    /**
     * Builds the description for this module.
     *
     * @param moduleContentTree the tree to which the module description will
     *                           be added
     */
    protected void buildModuleDescription(Content moduleContentTree) {
        if (!options.noComment()) {
            moduleWriter.addModuleDescription(moduleContentTree);
        }
    }
}
