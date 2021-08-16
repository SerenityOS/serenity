/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.text.MessageFormat;
import java.util.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import jdk.javadoc.internal.doclets.toolkit.ClassWriter;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.MemberSummaryWriter;
import jdk.javadoc.internal.doclets.toolkit.WriterFactory;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;
import jdk.javadoc.internal.doclets.toolkit.util.DocFinder;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;
import jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable;
import jdk.javadoc.internal.doclets.toolkit.CommentUtils;

import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.*;

/**
 * Builds the member summary.
 * There are two anonymous subtype variants of this builder, created
 * in the {@link #getInstance} methods. One is for general types;
 * the other is for annotation types.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public abstract class MemberSummaryBuilder extends AbstractMemberBuilder {

    /*
     * Comparator used to sort the members in the summary.
     */
    private final Comparator<Element> comparator;

    /**
     * The member summary writers for the given class.
     */
    private final EnumMap<VisibleMemberTable.Kind, MemberSummaryWriter> memberSummaryWriters;

    final PropertyHelper pHelper;

    /**
     * Construct a new MemberSummaryBuilder.
     *
     * @param context       the build context.
     * @param typeElement   the type element.
     */
    private MemberSummaryBuilder(Context context, TypeElement typeElement) {
        super(context, typeElement);
        memberSummaryWriters = new EnumMap<>(VisibleMemberTable.Kind.class);
        comparator = utils.comparators.makeIndexElementComparator();
        pHelper = new PropertyHelper(this);
    }

    /**
     * Construct a new MemberSummaryBuilder for a general type.
     *
     * @param classWriter   the writer for the class whose members are being
     *                      summarized.
     * @param context       the build context.
     * @return              the instance
     */
    public static MemberSummaryBuilder getInstance(
            ClassWriter classWriter, Context context) {
        MemberSummaryBuilder builder = new MemberSummaryBuilder(context, classWriter.getTypeElement()) {
            @Override
            public void build(Content summariesList) {
                buildPropertiesSummary(summariesList);
                buildNestedClassesSummary(summariesList);
                buildEnumConstantsSummary(summariesList);
                buildAnnotationTypeRequiredMemberSummary(summariesList);
                buildAnnotationTypeOptionalMemberSummary(summariesList);
                buildFieldsSummary(summariesList);
                buildConstructorsSummary(summariesList);
                buildMethodsSummary(summariesList);
            }

            @Override
            public boolean hasMembersToDocument() {
                return visibleMemberTable.hasVisibleMembers();
            }
        };
        WriterFactory wf = context.configuration.getWriterFactory();
        for (VisibleMemberTable.Kind kind : VisibleMemberTable.Kind.values()) {
            MemberSummaryWriter msw = builder.getVisibleMemberTable().hasVisibleMembers(kind)
                    ? wf.getMemberSummaryWriter(classWriter, kind)
                    : null;
            builder.memberSummaryWriters.put(kind, msw);
        }
        return builder;
    }

    /**
     * Return the specified visible member map.
     *
     * @return the specified visible member map.
     * @throws ArrayIndexOutOfBoundsException when the type is invalid.
     * @see VisibleMemberTable
     */
    public VisibleMemberTable getVisibleMemberTable() {
        return visibleMemberTable;
    }

    /**.
     * Return the specified member summary writer.
     *
     * @param kind the kind of member summary writer to return.
     * @return the specified member summary writer.
     * @throws ArrayIndexOutOfBoundsException when the type is invalid.
     * @see VisibleMemberTable
     */
    public MemberSummaryWriter getMemberSummaryWriter(VisibleMemberTable.Kind kind) {
        return memberSummaryWriters.get(kind);
    }

    /**
     * Returns a list of methods that will be documented for the given class.
     * This information can be used for doclet specific documentation
     * generation.
     *
     * @param kind the kind of elements to return.
     * @return a list of methods that will be documented.
     * @see VisibleMemberTable
     */
    public SortedSet<Element> members(VisibleMemberTable.Kind kind) {
        TreeSet<Element> out = new TreeSet<>(comparator);
        out.addAll(getVisibleMembers(kind));
        return out;
    }

    /**
     * Builds the summary for any optional members of an annotation type.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildAnnotationTypeOptionalMemberSummary(Content summariesList) {
        MemberSummaryWriter writer = memberSummaryWriters.get(ANNOTATION_TYPE_MEMBER_OPTIONAL);
        addSummary(writer, ANNOTATION_TYPE_MEMBER_OPTIONAL, false, summariesList);
    }

    /**
     * Builds the summary for any required members of an annotation type.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildAnnotationTypeRequiredMemberSummary(Content summariesList) {
        MemberSummaryWriter writer = memberSummaryWriters.get(ANNOTATION_TYPE_MEMBER_REQUIRED);
        addSummary(writer, ANNOTATION_TYPE_MEMBER_REQUIRED, false, summariesList);
    }

    /**
     * Builds the summary for any enum constants of an enum type.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildEnumConstantsSummary(Content summariesList) {
        MemberSummaryWriter writer = memberSummaryWriters.get(ENUM_CONSTANTS);
        addSummary(writer, ENUM_CONSTANTS, false, summariesList);
    }

    /**
     * Builds the summary for any fields.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildFieldsSummary(Content summariesList) {
        MemberSummaryWriter writer = memberSummaryWriters.get(FIELDS);
        addSummary(writer, FIELDS, true, summariesList);
    }

    /**
     * Builds the summary for any properties.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildPropertiesSummary(Content summariesList) {
        MemberSummaryWriter writer = memberSummaryWriters.get(PROPERTIES);
        addSummary(writer, PROPERTIES, true, summariesList);
    }

    /**
     * Builds the summary for any nested classes.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildNestedClassesSummary(Content summariesList) {
        MemberSummaryWriter writer = memberSummaryWriters.get(NESTED_CLASSES);
        addSummary(writer, NESTED_CLASSES, true, summariesList);
    }

    /**
     * Builds the summary for any methods.
     *
     * @param summariesList the content tree to which the documentation will be added
     */
    protected void buildMethodsSummary(Content summariesList) {
        MemberSummaryWriter writer = memberSummaryWriters.get(METHODS);
        addSummary(writer, METHODS, true, summariesList);
    }

    /**
     * Builds the summary for any constructors.
     *
     * @param summariesList the content tree to which the documentation will be added
     */
    protected void buildConstructorsSummary(Content summariesList) {
        MemberSummaryWriter writer = memberSummaryWriters.get(CONSTRUCTORS);
        addSummary(writer, CONSTRUCTORS, false, summariesList);
    }

    /**
     * Build the member summary for the given members.
     *
     * @param writer the summary writer to write the output.
     * @param kind the kind of  members to summarize.
     * @param summaryTreeList list of content trees to which the documentation will be added
     */
    private void buildSummary(MemberSummaryWriter writer,
            VisibleMemberTable.Kind kind, LinkedList<Content> summaryTreeList) {
        SortedSet<? extends Element> members = asSortedSet(getVisibleMembers(kind));
        if (!members.isEmpty()) {
            for (Element member : members) {
                final Element property = pHelper.getPropertyElement(member);
                if (property != null) {
                    processProperty(member, property);
                }
                List<? extends DocTree> firstSentenceTags = utils.getFirstSentenceTrees(member);
                if (utils.isExecutableElement(member) && firstSentenceTags.isEmpty()) {
                    //Inherit comments from overridden or implemented method if
                    //necessary.
                    DocFinder.Output inheritedDoc =
                            DocFinder.search(configuration,
                                    new DocFinder.Input(utils, member));
                    if (inheritedDoc.holder != null
                            && !utils.getFirstSentenceTrees(inheritedDoc.holder).isEmpty()) {
                        // let the comment helper know of the overridden element
                        CommentHelper ch = utils.getCommentHelper(member);
                        ch.setOverrideElement(inheritedDoc.holder);
                        firstSentenceTags = utils.getFirstSentenceTrees(inheritedDoc.holder);
                    }
                }
                writer.addMemberSummary(typeElement, member, firstSentenceTags);
            }
            summaryTreeList.add(writer.getSummaryTableTree(typeElement));
        }
    }

    /**
     * Process the property method, property setter and/or property getter
     * comment text so that it contains the documentation from
     * the property field. The method adds the leading sentence,
     * copied documentation including the defaultValue tag and
     * the see tags if the appropriate property getter and setter are
     * available.
     *
     * @param member the member which is to be augmented.
     * @param property the original property documentation.
     */
    private void processProperty(Element member,
                                 Element property) {
        CommentUtils cmtutils = configuration.cmtUtils;
        final boolean isSetter = isSetter(member);
        final boolean isGetter = isGetter(member);

        List<DocTree> fullBody = new ArrayList<>();
        List<DocTree> blockTags = new ArrayList<>();
        if (isGetter || isSetter) {
            //add "[GS]ets the value of the property PROPERTY_NAME."
            if (isSetter) {
                String text = MessageFormat.format(
                        resources.getText("doclet.PropertySetterWithName"),
                        utils.propertyName((ExecutableElement)member));
                fullBody.addAll(cmtutils.makeFirstSentenceTree(text));
            }
            if (isGetter) {
                String text = MessageFormat.format(
                        resources.getText("doclet.PropertyGetterWithName"),
                        utils.propertyName((ExecutableElement) member));
                fullBody.addAll(cmtutils.makeFirstSentenceTree(text));
            }
            List<? extends DocTree> propertyTags = utils.getBlockTags(property,
                    t -> (t instanceof UnknownBlockTagTree tree)
                            && (tree.getTagName().equals("propertyDescription")));
            if (propertyTags.isEmpty()) {
                List<? extends DocTree> comment = utils.getFullBody(property);
                blockTags.addAll(cmtutils.makePropertyDescriptionTree(comment));
            }
        } else {
            fullBody.addAll(utils.getFullBody(property));
        }

        // copy certain tags
        List<? extends SinceTree> tags = utils.getBlockTags(property, Kind.SINCE, SinceTree.class);
        blockTags.addAll(tags);

        List<? extends DocTree> bTags = utils.getBlockTags(property,
                t -> (t instanceof UnknownBlockTagTree tree)
                        && (tree.getTagName().equals("defaultValue")));
        blockTags.addAll(bTags);

        //add @see tags
        if (!isGetter && !isSetter) {
            ExecutableElement getter = pHelper.getGetterForProperty((ExecutableElement)member);
            ExecutableElement setter = pHelper.getSetterForProperty((ExecutableElement)member);

            if (null != getter) {
                StringBuilder sb = new StringBuilder("#");
                sb.append(utils.getSimpleName(getter)).append("()");
                blockTags.add(cmtutils.makeSeeTree(sb.toString(), getter));
            }

            if (null != setter) {
                VariableElement param = setter.getParameters().get(0);
                StringBuilder sb = new StringBuilder("#");
                sb.append(utils.getSimpleName(setter));
                if (!utils.isTypeVariable(param.asType())) {
                    sb.append("(").append(utils.getTypeSignature(param.asType(), false, true)).append(")");
                }
                blockTags.add(cmtutils.makeSeeTree(sb.toString(), setter));
            }
        }
        cmtutils.setDocCommentTree(member, fullBody, blockTags);
    }

    /**
     * Test whether the method is a getter.
     * @param element property method documentation. Needs to be either property
     * method, property getter, or property setter.
     * @return true if the given documentation belongs to a getter.
     */
    private boolean isGetter(Element element) {
        final String pedName = element.getSimpleName().toString();
        return pedName.startsWith("get") || pedName.startsWith("is");
    }

    /**
     * Test whether the method is a setter.
     * @param element property method documentation. Needs to be either property
     * method, property getter, or property setter.
     * @return true if the given documentation belongs to a setter.
     */
    private boolean isSetter(Element element) {
        return element.getSimpleName().toString().startsWith("set");
    }

    /**
     * Build the inherited member summary for the given methods.
     *
     * @param writer the writer for this member summary.
     * @param kind the kind of members to document.
     * @param summaryTreeList list of content trees to which the documentation will be added
     */
    private void buildInheritedSummary(MemberSummaryWriter writer,
            VisibleMemberTable.Kind kind, LinkedList<Content> summaryTreeList) {
        VisibleMemberTable visibleMemberTable = getVisibleMemberTable();
        SortedSet<? extends Element> inheritedMembersFromMap = asSortedSet(visibleMemberTable.getAllVisibleMembers(kind));

        for (TypeElement inheritedClass : visibleMemberTable.getVisibleTypeElements()) {
            if (!(utils.isPublic(inheritedClass) || utils.isLinkable(inheritedClass))) {
                continue;
            }
            if (inheritedClass == typeElement) {
                continue;
            }
            if (utils.hasHiddenTag(inheritedClass)) {
                continue;
            }

            List<? extends Element> members = inheritedMembersFromMap.stream()
                    .filter(e -> utils.getEnclosingTypeElement(e) == inheritedClass)
                    .toList();

            if (!members.isEmpty()) {
                SortedSet<Element> inheritedMembers = new TreeSet<>(comparator);
                inheritedMembers.addAll(members);
                Content inheritedTree = writer.getInheritedSummaryHeader(inheritedClass);
                Content linksTree = writer.getInheritedSummaryLinksTree();
                addSummaryFootNote(inheritedClass, inheritedMembers, linksTree, writer);
                inheritedTree.add(linksTree);
                summaryTreeList.add(inheritedTree);
            }
        }
    }

    private void addSummaryFootNote(TypeElement inheritedClass, SortedSet<Element> inheritedMembers,
                                    Content linksTree, MemberSummaryWriter writer) {
        for (Element member : inheritedMembers) {
            TypeElement t = utils.isUndocumentedEnclosure(inheritedClass)
                    ? typeElement : inheritedClass;
            writer.addInheritedMemberSummary(t, member, inheritedMembers.first() == member,
                    inheritedMembers.last() == member, linksTree);
        }
    }

    /**
     * Adds the summary for the documentation.
     *
     * @param writer               the writer for this member summary
     * @param kind                 the kind of members to document
     * @param showInheritedSummary true if a summary of any inherited elements should be documented
     * @param summariesList        the list of summaries to which the summary will be added
     */
    private void addSummary(MemberSummaryWriter writer,
                            VisibleMemberTable.Kind kind,
                            boolean showInheritedSummary,
                            Content summariesList)
    {
        LinkedList<Content> summaryTreeList = new LinkedList<>();
        buildSummary(writer, kind, summaryTreeList);
        if (showInheritedSummary)
            buildInheritedSummary(writer, kind, summaryTreeList);
        if (!summaryTreeList.isEmpty()) {
            Content memberTree = writer.getMemberSummaryHeader(typeElement, summariesList);
            summaryTreeList.forEach(memberTree::add);
            writer.addSummary(summariesList, memberTree);
        }
    }

    private SortedSet<? extends Element> asSortedSet(Collection<? extends Element> members) {
        SortedSet<Element> out = new TreeSet<>(comparator);
        out.addAll(members);
        return out;
    }

    static class PropertyHelper {

        private final Map<Element, Element> classPropertiesMap = new HashMap<>();

        private final MemberSummaryBuilder  builder;

        PropertyHelper(MemberSummaryBuilder builder) {
            this.builder = builder;
            computeProperties();
        }

        private void computeProperties() {
            VisibleMemberTable vmt = builder.getVisibleMemberTable();
            List<ExecutableElement> props = ElementFilter.methodsIn(vmt.getVisibleMembers(PROPERTIES));
            for (ExecutableElement propertyMethod : props) {
                ExecutableElement getter = vmt.getPropertyGetter(propertyMethod);
                ExecutableElement setter = vmt.getPropertySetter(propertyMethod);
                VariableElement field = vmt.getPropertyField(propertyMethod);

                addToPropertiesMap(propertyMethod, field, getter, setter);
            }
        }

        private void addToPropertiesMap(ExecutableElement propertyMethod,
                                        VariableElement field,
                                        ExecutableElement getter,
                                        ExecutableElement setter) {
            if (field == null || builder.utils.getDocCommentTree(field) == null) {
                addToPropertiesMap(propertyMethod, propertyMethod);
                addToPropertiesMap(getter, propertyMethod);
                addToPropertiesMap(setter, propertyMethod);
            } else {
                addToPropertiesMap(propertyMethod, field);
                addToPropertiesMap(getter, field);
                addToPropertiesMap(setter, field);
            }
        }

        private void addToPropertiesMap(Element propertyMethod,
                                        Element commentSource) {
            if (null == propertyMethod || null == commentSource) {
                return;
            }
            Utils utils = builder.utils;
            DocCommentTree docTree = utils.hasDocCommentTree(propertyMethod)
                    ? utils.getDocCommentTree(propertyMethod)
                    : null;

            /* The second condition is required for the property buckets. In
             * this case the comment is at the property method (not at the field)
             * and it needs to be listed in the map.
             */
            if ((docTree == null) || propertyMethod.equals(commentSource)) {
                classPropertiesMap.put(propertyMethod, commentSource);
            }
        }

        /**
         * Returns the property field documentation belonging to the given member.
         * @param element the member for which the property documentation is needed.
         * @return the property field documentation, null if there is none.
         */
        public Element getPropertyElement(Element element) {
            return classPropertiesMap.get(element);
        }

        /**
         * Returns the getter documentation belonging to the given property method.
         * @param propertyMethod the method for which the getter is needed.
         * @return the getter documentation, null if there is none.
         */
        public ExecutableElement getGetterForProperty(ExecutableElement propertyMethod) {
            return builder.getVisibleMemberTable().getPropertyGetter(propertyMethod);
        }

        /**
         * Returns the setter documentation belonging to the given property method.
         * @param propertyMethod the method for which the setter is needed.
         * @return the setter documentation, null if there is none.
         */
        public ExecutableElement getSetterForProperty(ExecutableElement propertyMethod) {
            return builder.getVisibleMemberTable().getPropertySetter(propertyMethod);
        }
    }
}
