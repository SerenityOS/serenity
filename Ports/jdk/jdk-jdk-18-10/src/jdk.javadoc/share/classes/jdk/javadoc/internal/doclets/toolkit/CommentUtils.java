/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 *  A utility class.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */

package jdk.javadoc.internal.doclets.toolkit;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.IdentifierTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTreeFactory;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

public class CommentUtils {

    final BaseConfiguration configuration;
    final Utils utils;
    final Resources resources;
    final DocTreeFactory treeFactory;
    final DocTrees trees;
    final Elements elementUtils;

    /**
     * A map for storing automatically generated comments for various
     * elements, such as mandated elements (Enum.values, Enum.valueOf, etc)
     * and JavaFX properties.
     *
     * @see Utils#dcTreeCache
     */
    final HashMap<Element, DocCommentInfo> dcInfoMap = new HashMap<>();

    protected CommentUtils(BaseConfiguration configuration) {
        this.configuration = configuration;
        utils = configuration.utils;
        resources = configuration.getDocResources();
        trees = configuration.docEnv.getDocTrees();
        treeFactory = trees.getDocTreeFactory();
        elementUtils = configuration.docEnv.getElementUtils();
    }

    public List<? extends DocTree> makePropertyDescriptionTree(List<? extends DocTree> content) {
        List<DocTree> out = new ArrayList<>();
        Name name = elementUtils.getName("propertyDescription");
        out.add(treeFactory.newUnknownBlockTagTree(name, content));
        return out;
    }

    public List<? extends DocTree> makePropertyDescriptionTree(String content) {
        List<DocTree> inlist = new ArrayList<>();
        inlist.add(treeFactory.newCommentTree(content));
        List<DocTree> out = new ArrayList<>();
        Name name = elementUtils.getName("propertyDescription");
        out.add(treeFactory.newUnknownBlockTagTree(name, inlist));
        return out;
    }

    public List<? extends DocTree> makeFirstSentenceTree(String content) {
        List<DocTree> out = new ArrayList<>();
        out.add(treeFactory.newTextTree(content));
        return out;
    }

    public DocTree makeSeeTree(String sig, Element e) {
        List<DocTree> list = new ArrayList<>();
        list.add(treeFactory.newReferenceTree(sig));
        return treeFactory.newSeeTree(list);
    }

    public TextTree makeTextTree(String content) {
        return treeFactory.newTextTree(content);
    }

    public TextTree makeTextTreeForResource(String key) {
        return treeFactory.newTextTree(resources.getText(key));
    }

    public void setEnumValuesTree(ExecutableElement ee) {
        List<DocTree> fullBody = new ArrayList<>();
        fullBody.add(treeFactory.newTextTree(resources.getText("doclet.enum_values_doc.fullbody")));

        List<DocTree> descriptions = new ArrayList<>();
        descriptions.add(treeFactory.newTextTree(resources.getText("doclet.enum_values_doc.return")));

        List<DocTree> tags = new ArrayList<>();
        tags.add(treeFactory.newReturnTree(descriptions));
        DocCommentTree docTree = treeFactory.newDocCommentTree(fullBody, tags);
        dcInfoMap.put(ee, new DocCommentInfo(null, docTree));
    }

    public void setEnumValueOfTree(ExecutableElement ee) {
        List<DocTree> fullBody = new ArrayList<>();
        fullBody.add(treeFactory.newTextTree(resources.getText("doclet.enum_valueof_doc.fullbody")));

        List<DocTree> tags = new ArrayList<>();

        List<DocTree> paramDescs = new ArrayList<>();
        paramDescs.add(treeFactory.newTextTree(resources.getText("doclet.enum_valueof_doc.param_name")));
        java.util.List<? extends VariableElement> parameters = ee.getParameters();
        VariableElement param = parameters.get(0);
        IdentifierTree id = treeFactory.newIdentifierTree(elementUtils.getName(param.getSimpleName().toString()));
        tags.add(treeFactory.newParamTree(false, id, paramDescs));

        List<DocTree> returnDescs = new ArrayList<>();
        returnDescs.add(treeFactory.newTextTree(resources.getText("doclet.enum_valueof_doc.return")));
        tags.add(treeFactory.newReturnTree(returnDescs));

        List<DocTree> throwsDescs = new ArrayList<>();
        throwsDescs.add(treeFactory.newTextTree(resources.getText("doclet.enum_valueof_doc.throws_ila")));

        ReferenceTree ref = treeFactory.newReferenceTree("java.lang.IllegalArgumentException");
        tags.add(treeFactory.newThrowsTree(ref, throwsDescs));

        throwsDescs = new ArrayList<>();
        throwsDescs.add(treeFactory.newTextTree(resources.getText("doclet.enum_valueof_doc.throws_npe")));

        ref = treeFactory.newReferenceTree("java.lang.NullPointerException");
        tags.add(treeFactory.newThrowsTree(ref, throwsDescs));

        DocCommentTree docTree = treeFactory.newDocCommentTree(fullBody, tags);

        dcInfoMap.put(ee, new DocCommentInfo(null, docTree));
    }

    /**
     * Generates the description for the canonical constructor for a record.
     * @param ee the constructor
     */
    public void setRecordConstructorTree(ExecutableElement ee) {
        TypeElement te = utils.getEnclosingTypeElement(ee);

        List<DocTree> fullBody =
                makeDescriptionWithName("doclet.record_constructor_doc.fullbody", te.getSimpleName());

        List<DocTree> tags = new ArrayList<>();
        java.util.List<? extends VariableElement> parameters = ee.getParameters();
        for (VariableElement param : ee.getParameters()) {
            Name name = param.getSimpleName();
            IdentifierTree id = treeFactory.newIdentifierTree(name);
            tags.add(treeFactory.newParamTree(false, id,
                    makeDescriptionWithComponent("doclet.record_constructor_doc.param_name", te, name)));
        }

        DocCommentTree docTree = treeFactory.newDocCommentTree(fullBody, tags);
        dcInfoMap.put(ee, new DocCommentInfo(null, docTree));
    }

    /**
     * Generates the description for the standard {@code equals} method for a record.
     * @param ee the {@code equals} method
     */
    public void setRecordEqualsTree(ExecutableElement ee) {
        List<DocTree> fullBody = new ArrayList<>();
        add(fullBody, "doclet.record_equals_doc.fullbody.head");
        fullBody.add(treeFactory.newTextTree(" "));

        List<? extends RecordComponentElement> comps = ((TypeElement) ee.getEnclosingElement()).getRecordComponents();
        boolean hasPrimitiveComponents =
                comps.stream().anyMatch(e -> e.asType().getKind().isPrimitive());
        boolean hasReferenceComponents =
                comps.stream().anyMatch(e -> !e.asType().getKind().isPrimitive());
        if (hasPrimitiveComponents && hasReferenceComponents) {
            add(fullBody, "doclet.record_equals_doc.fullbody.tail.both");
        } else if (hasPrimitiveComponents) {
            add(fullBody, "doclet.record_equals_doc.fullbody.tail.primitive");
        } else if (hasReferenceComponents) {
            add(fullBody, "doclet.record_equals_doc.fullbody.tail.reference");
        }
        Name paramName = ee.getParameters().get(0).getSimpleName();
        IdentifierTree id = treeFactory.newIdentifierTree(paramName);
        List<DocTree> paramDesc =
                makeDescriptionWithName("doclet.record_equals_doc.param_name", paramName);
        DocTree paramTree = treeFactory.newParamTree(false, id, paramDesc);

        DocTree returnTree = treeFactory.newReturnTree(
                makeDescriptionWithName("doclet.record_equals_doc.return", paramName));

        TreePath treePath = utils.getTreePath(ee.getEnclosingElement());
        DocCommentTree docTree = treeFactory.newDocCommentTree(fullBody, List.of(paramTree, returnTree));
        dcInfoMap.put(ee, new DocCommentInfo(treePath, docTree));
    }

    private void add(List<DocTree> contents, String resourceKey) {
        // Special case to allow '{@link ...}' to appear in the string.
        // A less general case would be to detect literal use of Object.equals
        // A more general case would be to allow access to DocCommentParser somehow
        String body = resources.getText(resourceKey);
        Pattern p = Pattern.compile("\\{@link (\\S*)(.*)}");
        Matcher m = p.matcher(body);
        int start = 0;
        while (m.find(start)) {
            if (m.start() > start) {
                contents.add(treeFactory.newTextTree(body.substring(start, m.start())));
            }
            ReferenceTree refTree = treeFactory.newReferenceTree(m.group(1));
            List<DocTree> descr = List.of(treeFactory.newTextTree(m.group(2).trim())) ;
            contents.add(treeFactory.newLinkTree(refTree, descr));
            start = m.end();
        }
        if (start < body.length()) {
            contents.add(treeFactory.newTextTree(body.substring(start)));
        }
    }

    /**
     * Generates the description for the standard {@code hashCode} method for a record.
     * @param ee the {@code hashCode} method
     */
    public void setRecordHashCodeTree(ExecutableElement ee) {
        List<DocTree> fullBody = List.of(makeTextTreeForResource("doclet.record_hashCode_doc.fullbody"));

        DocTree returnTree = treeFactory.newReturnTree(
                List.of(makeTextTreeForResource("doclet.record_hashCode_doc.return")));

        DocCommentTree docTree = treeFactory.newDocCommentTree(fullBody, List.of(returnTree));
        dcInfoMap.put(ee, new DocCommentInfo(null, docTree));
    }

    /**
     * Generates the description for the standard {@code toString} method for a record.
     * @param ee the {@code toString} method
     */
    public void setRecordToStringTree(ExecutableElement ee) {
        List<DocTree> fullBody = List.of(
                treeFactory.newTextTree(resources.getText("doclet.record_toString_doc.fullbody")));

        DocTree returnTree = treeFactory.newReturnTree(List.of(
                treeFactory.newTextTree(resources.getText("doclet.record_toString_doc.return"))));

        DocCommentTree docTree = treeFactory.newDocCommentTree(fullBody, List.of(returnTree));
        dcInfoMap.put(ee, new DocCommentInfo(null, docTree));
    }

    /**
     * Generates the description for the accessor method for a state component of a record.
     * @param ee the accessor method
     */
    public void setRecordAccessorTree(ExecutableElement ee) {
        TypeElement te = utils.getEnclosingTypeElement(ee);

        List<DocTree> fullBody =
                makeDescriptionWithComponent("doclet.record_accessor_doc.fullbody", te, ee.getSimpleName());

        DocTree returnTree = treeFactory.newReturnTree(
                    makeDescriptionWithComponent("doclet.record_accessor_doc.return", te, ee.getSimpleName()));

        DocCommentTree docTree = treeFactory.newDocCommentTree(fullBody, List.of(returnTree));
        dcInfoMap.put(ee, new DocCommentInfo(null, docTree));
    }

    /**
     * Generates the description for the field for a state component of a record.
     * @param ve the field
     */
    public void setRecordFieldTree(VariableElement ve) {
        TypeElement te = utils.getEnclosingTypeElement(ve);

        List<DocTree> fullBody =
            makeDescriptionWithComponent("doclet.record_field_doc.fullbody", te, ve.getSimpleName());

        DocCommentTree docTree = treeFactory.newDocCommentTree(fullBody, List.of());
        dcInfoMap.put(ve, new DocCommentInfo(null, docTree));
    }

    /**
     * Creates a description that contains a reference to a state component of a record.
     * The description is looked up as a resource, and should contain {@code {0}} where the
     * reference to the component is to be inserted. The reference will be a link if the
     * doc comment for the record has a {@code @param} tag for the component.
     * @param key the resource key for the description
     * @param elem the record element
     * @param component the name of the component
     * @return the description
     */
    private List<DocTree> makeDescriptionWithComponent(String key, TypeElement elem, Name component) {
        List<DocTree> result = new ArrayList<>();
        String text = resources.getText(key);
        int index = text.indexOf("{0}");
        result.add(treeFactory.newTextTree(text.substring(0, index)));
        Name A = elementUtils.getName("a");
        Name CODE = elementUtils.getName("code");
        Name HREF = elementUtils.getName("href");
        List<DocTree> code = List.of(
                treeFactory.newStartElementTree(CODE, List.of(), false),
                treeFactory.newTextTree(component.toString()),
                treeFactory.newEndElementTree(CODE));
        if (hasParamForComponent(elem, component)) {
            DocTree href = treeFactory.newAttributeTree(HREF,
                    AttributeTree.ValueKind.DOUBLE,
                    List.of(treeFactory.newTextTree("#param-" + component)));
            result.add(treeFactory.newStartElementTree(A, List.of(href), false));
            result.addAll(code);
            result.add(treeFactory.newEndElementTree(A));
        } else {
            result.addAll(code);
        }
        result.add(treeFactory.newTextTree(text.substring(index + 3)));
        return result;
    }

    /**
     * Returns whether or not the doc comment for a record contains an {@code @param}}
     * for a state component of the record.
     * @param elem the record element
     * @param component the name of the component
     * @return whether or not there is a {@code @param}} for the component
     */
    private boolean hasParamForComponent(TypeElement elem, Name component) {
        DocCommentTree elemComment = utils.getDocCommentTree(elem);
        if (elemComment == null) {
            return false;
        }

        for (DocTree t : elemComment.getBlockTags()) {
            if (t instanceof ParamTree pt && pt.getName().getName() == component) {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates a description that contains the simple name of a program element
     * The description is looked up as a resource, and should contain {@code {0}} where the
     * name is to be inserted.
     * @param key the resource key for the description
     * @param name the name
     * @return the description
     */
    private List<DocTree> makeDescriptionWithName(String key, Name name) {
        String text = resources.getText(key);
        int index = text.indexOf("{0}");
        if (index == -1) {
            return List.of(treeFactory.newTextTree(text));
        } else {
            Name CODE = elementUtils.getName("code");
            return List.of(
                    treeFactory.newTextTree(text.substring(0, index)),
                    treeFactory.newStartElementTree(CODE, List.of(), false),
                    treeFactory.newTextTree(name.toString()),
                    treeFactory.newEndElementTree(CODE),
                    treeFactory.newTextTree(text.substring(index + 3))
            );
        }
    }

    /*
     * Returns the TreePath/DocCommentTree info that has been generated for an element.
     * @param e the element
     * @return the info object containing the tree path and doc comment
     */
    // "synthetic" is not the best word here, and should not be confused with synthetic elements
    public DocCommentInfo getSyntheticCommentInfo(Element e) {
        return dcInfoMap.get(e);
    }

    /*
     * Returns the TreePath/DocCommentTree info for HTML sources.
     */
    public DocCommentInfo getHtmlCommentInfo(Element e) {
        FileObject fo = null;
        PackageElement pe = null;
        switch (e.getKind()) {
            case OTHER:
                if (e instanceof DocletElement de) {
                    fo = de.getFileObject();
                    pe = de.getPackageElement();
                }
                break;
            case PACKAGE:
                pe = (PackageElement) e;
                fo = configuration.workArounds.getJavaFileObject(pe);
                break;
            default:
                return null;
        }
        if (fo == null) {
            return null;
        }

        DocCommentTree dcTree = trees.getDocCommentTree(fo);
        if (dcTree == null) {
            return null;
        }
        DocTreePath treePath = trees.getDocTreePath(fo, pe);
        return new DocCommentInfo(treePath.getTreePath(), dcTree);
    }

    public DocCommentTree parse(URI uri, String text) {
        return trees.getDocCommentTree(new SimpleJavaFileObject(
                uri, JavaFileObject.Kind.SOURCE) {
            @Override @DefinedBy(Api.COMPILER)
            public CharSequence getCharContent(boolean ignoreEncoding) {
                return text;
            }
        });
    }

    public void setDocCommentTree(Element element, List<? extends DocTree> fullBody,
                                  List<? extends DocTree> blockTags) {
        DocCommentTree docTree = treeFactory.newDocCommentTree(fullBody, blockTags);
        dcInfoMap.put(element, new DocCommentInfo(null, docTree));
        // A method having null comment (no comment) that might need to be replaced
        // with a generated comment, remove such a comment from the cache.
        utils.removeCommentHelper(element);
    }

    /**
     * Info about a doc comment:
     *   the position in the enclosing AST, and
     *   the parsed doc comment itself.
     *
     * The position in the AST is {@code null} for automatically generated comments,
     * such as for {@code Enum.values}, {@code Enum.valuesOf}, and JavaFX properties.
     */
    public static class DocCommentInfo {
        /**
         * The position of the comment in the enclosing AST, or {@code null}
         * for automatically generated comments.
         */
        public final TreePath treePath;

        /**
         * The doc comment tree that is the root node of a parsed doc comment,
         * or {@code null} if there is no comment.
         */
        public final DocCommentTree dcTree;

        public DocCommentInfo(TreePath treePath, DocCommentTree dcTree) {
            this.treePath = treePath;
            this.dcTree = dcTree;
        }
    }
}
