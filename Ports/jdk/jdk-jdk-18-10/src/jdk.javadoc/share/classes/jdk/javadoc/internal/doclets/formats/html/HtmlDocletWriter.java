/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.formats.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor9;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.SimpleTypeVisitor9;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.AttributeTree.ValueKind;
import com.sun.source.doctree.CommentTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocRootTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.IndexTree;
import com.sun.source.doctree.InheritDocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.SummaryTree;
import com.sun.source.doctree.SystemPropertyTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.SimpleDocTreeVisitor;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.Entity;
import jdk.javadoc.internal.doclets.formats.html.markup.Head;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlDocument;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlId;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.Links;
import jdk.javadoc.internal.doclets.formats.html.markup.RawHtml;
import jdk.javadoc.internal.doclets.formats.html.markup.Script;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.ClassWriter;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.Messages;
import jdk.javadoc.internal.doclets.toolkit.PackageSummaryWriter;
import jdk.javadoc.internal.doclets.toolkit.Resources;
import jdk.javadoc.internal.doclets.toolkit.taglets.DocRootTaglet;
import jdk.javadoc.internal.doclets.toolkit.taglets.Taglet;
import jdk.javadoc.internal.doclets.toolkit.taglets.TagletWriter;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;
import jdk.javadoc.internal.doclets.toolkit.util.Comparators;
import jdk.javadoc.internal.doclets.toolkit.util.DocFile;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocLink;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;
import jdk.javadoc.internal.doclets.toolkit.util.DocletConstants;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;
import jdk.javadoc.internal.doclets.toolkit.util.Utils.DeclarationPreviewLanguageFeatures;
import jdk.javadoc.internal.doclets.toolkit.util.Utils.ElementFlag;
import jdk.javadoc.internal.doclets.toolkit.util.Utils.PreviewSummary;
import jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable;
import jdk.javadoc.internal.doclint.HtmlTag;

import static com.sun.source.doctree.DocTree.Kind.CODE;
import static com.sun.source.doctree.DocTree.Kind.COMMENT;
import static com.sun.source.doctree.DocTree.Kind.LINK;
import static com.sun.source.doctree.DocTree.Kind.LINK_PLAIN;
import static com.sun.source.doctree.DocTree.Kind.SEE;
import static com.sun.source.doctree.DocTree.Kind.TEXT;
import static jdk.javadoc.internal.doclets.toolkit.util.CommentHelper.SPACER;


/**
 * Class for the Html Format Code Generation specific to JavaDoc.
 * This Class contains methods related to the Html Code Generation which
 * are used extensively while generating the entire documentation.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class HtmlDocletWriter {

    /**
     * Relative path from the file getting generated to the destination
     * directory. For example, if the file getting generated is
     * "java/lang/Object.html", then the path to the root is "../..".
     * This string can be empty if the file getting generated is in
     * the destination directory.
     */
    public final DocPath pathToRoot;

    /**
     * Platform-independent path from the current or the
     * destination directory to the file getting generated.
     * Used when creating the file.
     */
    public final DocPath path;

    /**
     * Name of the file getting generated. If the file getting generated is
     * "java/lang/Object.html", then the filename is "Object.html".
     */
    public final DocPath filename;

    /**
     * The global configuration information for this run.
     */
    public final HtmlConfiguration configuration;

    protected final HtmlOptions options;

    protected final Utils utils;

    protected final Contents contents;

    protected final Messages messages;

    protected final Resources resources;

    protected final Links links;

    protected final DocPaths docPaths;

    protected final Comparators comparators;

    protected final HtmlIds htmlIds;

    /**
     * To check whether annotation heading is printed or not.
     */
    protected boolean printedAnnotationHeading = false;

    /**
     * To check whether the repeated annotations is documented or not.
     */
    private boolean isAnnotationDocumented = false;

    /**
     * To check whether the container annotations is documented or not.
     */
    private boolean isContainerDocumented = false;

    /**
     * The window title of this file.
     */
    protected String winTitle;

    protected Script mainBodyScript;

    /**
     * A table of the anchors used for at-index and related tags,
     * so that they can be made unique by appending a suitable suffix.
     * (Ideally, javadoc should be tracking all id's generated in a file
     * to avoid generating duplicates.)
     */
    Map<String, Integer> indexAnchorTable = new HashMap<>();

    /**
     * Creates an {@code HtmlDocletWriter}.
     *
     * @param configuration the configuration for this doclet
     * @param path the file to be generated.
     */
    public HtmlDocletWriter(HtmlConfiguration configuration, DocPath path) {
        this.configuration = configuration;
        this.options = configuration.getOptions();
        this.contents = configuration.getContents();
        this.messages = configuration.messages;
        this.resources = configuration.docResources;
        this.links = new Links(path);
        this.utils = configuration.utils;
        this.comparators = utils.comparators;
        this.htmlIds = configuration.htmlIds;
        this.path = path;
        this.pathToRoot = path.parent().invert();
        this.filename = path.basename();
        this.docPaths = configuration.docPaths;
        this.mainBodyScript = new Script();

        messages.notice("doclet.Generating_0",
            DocFile.createFileForOutput(configuration, path).getPath());
    }

    /**
     * Replace {&#064;docRoot} tag used in options that accept HTML text, such
     * as -header, -footer, -top and -bottom, and when converting a relative
     * HREF where commentTagsToString inserts a {&#064;docRoot} where one was
     * missing.  (Also see DocRootTaglet for {&#064;docRoot} tags in doc
     * comments.)
     * <p>
     * Replace {&#064;docRoot} tag in htmlstr with the relative path to the
     * destination directory from the directory where the file is being
     * written, looping to handle all such tags in htmlstr.
     * <p>
     * For example, for "-d docs" and -header containing {&#064;docRoot}, when
     * the HTML page for source file p/C1.java is being generated, the
     * {&#064;docRoot} tag would be inserted into the header as "../",
     * the relative path from docs/p/ to docs/ (the document root).
     * <p>
     * Note: This doc comment was written with '&amp;#064;' representing '@'
     * to prevent the inline tag from being interpreted.
     */
    public String replaceDocRootDir(String htmlstr) {
        // Return if no inline tags exist
        int index = htmlstr.indexOf("{@");
        if (index < 0) {
            return htmlstr;
        }
        Matcher docrootMatcher = docrootPattern.matcher(htmlstr);
        if (!docrootMatcher.find()) {
            return htmlstr;
        }
        StringBuilder buf = new StringBuilder();
        int prevEnd = 0;
        do {
            int match = docrootMatcher.start();
            // append htmlstr up to start of next {@docroot}
            buf.append(htmlstr.substring(prevEnd, match));
            prevEnd = docrootMatcher.end();
            if (options.docrootParent().length() > 0 && htmlstr.startsWith("/..", prevEnd)) {
                // Insert the absolute link if {@docRoot} is followed by "/..".
                buf.append(options.docrootParent());
                prevEnd += 3;
            } else {
                // Insert relative path where {@docRoot} was located
                buf.append(pathToRoot.isEmpty() ? "." : pathToRoot.getPath());
            }
            // Append slash if next character is not a slash
            if (prevEnd < htmlstr.length() && htmlstr.charAt(prevEnd) != '/') {
                buf.append('/');
            }
        } while (docrootMatcher.find());
        buf.append(htmlstr.substring(prevEnd));
        return buf.toString();
    }
    //where:
        // Note: {@docRoot} is not case sensitive when passed in with a command-line option:
        private static final Pattern docrootPattern =
                Pattern.compile(Pattern.quote("{@docroot}"), Pattern.CASE_INSENSITIVE);


    /**
     * Add method information.
     *
     * @param method the method to be documented
     * @param dl the content tree to which the method information will be added
     */
    private void addMethodInfo(ExecutableElement method, Content dl) {
        TypeElement enclosing = utils.getEnclosingTypeElement(method);
        List<? extends TypeMirror> intfacs = enclosing.getInterfaces();
        ExecutableElement overriddenMethod = utils.overriddenMethod(method);
        VisibleMemberTable vmt = configuration.getVisibleMemberTable(enclosing);
        // Check whether there is any implementation or overridden info to be
        // printed. If no overridden or implementation info needs to be
        // printed, do not print this section.
        if ((!intfacs.isEmpty()
                && !vmt.getImplementedMethods(method).isEmpty())
                || overriddenMethod != null) {
            MethodWriterImpl.addImplementsInfo(this, method, dl);
            if (overriddenMethod != null) {
                MethodWriterImpl.addOverridden(this,
                        utils.overriddenType(method),
                        overriddenMethod,
                        dl);
            }
        }
    }

    /**
     * Adds the tags information.
     *
     * @param e the Element for which the tags will be generated
     * @param htmlTree the documentation tree to which the tags will be added
     */
    protected void addTagsInfo(Element e, Content htmlTree) {
        if (options.noComment()) {
            return;
        }
        HtmlTree dl = HtmlTree.DL(HtmlStyle.notes);
        if (utils.isExecutableElement(e) && !utils.isConstructor(e)) {
            addMethodInfo((ExecutableElement)e, dl);
        }
        Content output = getBlockTagOutput(e);
        dl.add(output);
        htmlTree.add(dl);
    }

    /**
     * Returns the content generated from the default supported set of block tags
     * for this element.
     *
     * @param element the element
     *
     * @return the content
     */
    protected Content getBlockTagOutput(Element element) {
        return getBlockTagOutput(element, configuration.tagletManager.getBlockTaglets(element));
    }

    /**
     * Returns the content generated from a specified set of block tags
     * for this element.
     *
     * @param element the element
     * @param taglets the taglets to handle the required set of tags
     *
     * @return the content
     */
    protected Content getBlockTagOutput(Element element, List<Taglet> taglets) {
        return getTagletWriterInstance(false)
                .getBlockTagOutput(configuration.tagletManager, element, taglets);
    }

    /**
     * Returns whether there are any tags in a field for the Serialization Overview
     * section to be generated.
     *
     * @param field the field to check
     * @return {@code true} if and only if there are tags to be included
     */
    protected boolean hasSerializationOverviewTags(VariableElement field) {
        Content output = getBlockTagOutput(field);
        return !output.isEmpty();
    }

    private Content getInlineTagOutput(Element element, DocTree holder, DocTree tree, TagletWriterImpl.Context context) {
        return getTagletWriterInstance(context)
                .getInlineTagOutput(element, configuration.tagletManager, holder, tree);
    }

    /**
     * Returns a TagletWriter that knows how to write HTML.
     *
     * @param isFirstSentence  true if we want to write the first sentence
     * @return a TagletWriter that knows how to write HTML.
     */
    public TagletWriter getTagletWriterInstance(boolean isFirstSentence) {
        return new TagletWriterImpl(this, isFirstSentence);
    }

    /**
     * Returns a TagletWriter that knows how to write HTML.
     *
     * @param context  the enclosing context for the trees
     * @return a TagletWriter
     */
    public TagletWriter getTagletWriterInstance(TagletWriterImpl.Context context) {
        return new TagletWriterImpl(this, context);
    }

    /**
     * Generates the HTML document tree and prints it out.
     *
     * @param metakeywords Array of String keywords for META tag. Each element
     *                     of the array is assigned to a separate META tag.
     *                     Pass in null for no array
     * @param description the content for the description META tag.
     * @param body the body htmltree to be included in the document
     * @throws DocFileIOException if there is a problem writing the file
     */
    public void printHtmlDocument(List<String> metakeywords,
                                  String description,
                                  Content body)
            throws DocFileIOException {
        printHtmlDocument(metakeywords, description, new ContentBuilder(), Collections.emptyList(), body);
    }

    /**
     * Generates the HTML document tree and prints it out.
     *
     * @param metakeywords Array of String keywords for META tag. Each element
     *                     of the array is assigned to a separate META tag.
     *                     Pass in null for no array
     * @param description the content for the description META tag.
     * @param localStylesheets local stylesheets to be included in the HEAD element
     * @param body the body htmltree to be included in the document
     * @throws DocFileIOException if there is a problem writing the file
     */
    public void printHtmlDocument(List<String> metakeywords,
                                  String description,
                                  List<DocPath> localStylesheets,
                                  Content body)
            throws DocFileIOException {
        printHtmlDocument(metakeywords, description, new ContentBuilder(), localStylesheets, body);
    }

    /**
     * Generates the HTML document tree and prints it out.
     *
     * @param metakeywords Array of String keywords for META tag. Each element
     *                     of the array is assigned to a separate META tag.
     *                     Pass in null for no array
     * @param description the content for the description META tag.
     * @param extraHeadContent any additional content to be included in the HEAD element
     * @param localStylesheets local stylesheets to be included in the HEAD element
     * @param body the body htmltree to be included in the document
     * @throws DocFileIOException if there is a problem writing the file
     */
    public void printHtmlDocument(List<String> metakeywords,
                                  String description,
                                  Content extraHeadContent,
                                  List<DocPath> localStylesheets,
                                  Content body)
            throws DocFileIOException {
        List<DocPath> additionalStylesheets = configuration.getAdditionalStylesheets();
        additionalStylesheets.addAll(localStylesheets);
        Head head = new Head(path, configuration.getDocletVersion(), configuration.startTime)
                .setTimestamp(!options.noTimestamp())
                .setDescription(description)
                .setGenerator(getGenerator(getClass()))
                .setTitle(winTitle)
                .setCharset(options.charset())
                .addKeywords(metakeywords)
                .setStylesheets(configuration.getMainStylesheet(), additionalStylesheets)
                .setIndex(options.createIndex(), mainBodyScript)
                .addContent(extraHeadContent);

        HtmlDocument htmlDocument = new HtmlDocument(
                HtmlTree.HTML(configuration.getLocale().getLanguage(), head, body));
        htmlDocument.write(DocFile.createFileForOutput(configuration, path));
    }

    /**
     * Get the window title.
     *
     * @param title the title string to construct the complete window title
     * @return the window title string
     */
    public String getWindowTitle(String title) {
        if (options.windowTitle().length() > 0) {
            title += " (" + options.windowTitle() + ")";
        }
        return title;
    }

    /**
     * Returns a {@code <header>} element, containing the user "top" text, if any,
     * amd the main navigation bar.
     *
     * @param pageMode the pageMode used to configure the navigation bar
     *
     * @return the {@code <header>} element
     */
    protected HtmlTree getHeader(Navigation.PageMode pageMode) {
        return getHeader(pageMode, null);
    }

    /**
     * Returns a {@code <header>} element, containing the user "top" text, if any,
     * amd the main navigation bar.
     *
     * @param pageMode the page mode used to configure the navigation bar
     * @param element  the element used to configure the navigation bar
     *
     * @return the {@code <header>} element
     */
    protected HtmlTree getHeader(Navigation.PageMode pageMode, Element element) {
        return HtmlTree.HEADER()
                .add(new RawHtml(replaceDocRootDir(options.top())))
                .add(getNavBar(pageMode, element).getContent());
    }

    /**
     * Returns a basic navigation bar for a kind of page and element.
     *
     * @apiNote the result may be further configured by overriding this method
     *
     * @param pageMode the page mode
     * @param element  the defining element for the navigation bar, or {@code null} if none
     * @return the basic navigation bar
     */
    protected Navigation getNavBar(Navigation.PageMode pageMode, Element element) {
        return new Navigation(element, configuration, pageMode, path)
                .setUserHeader(new RawHtml(replaceDocRootDir(options.header())));
    }

    /**
     * Returns a {@code <footer>} element containing the user's "bottom" text,
     * or {@code null} if there is no such text.
     *
     * @return the {@code <footer>} element or {@code null}.
     */
    public HtmlTree getFooter() {
        String bottom = options.bottom();
        return (bottom == null || bottom.isEmpty())
                ? null
                : HtmlTree.FOOTER()
                    .add(new HtmlTree(TagName.HR))
                    .add(HtmlTree.P(HtmlStyle.legalCopy,
                            HtmlTree.SMALL(
                                    new RawHtml(replaceDocRootDir(bottom)))));
    }

    /**
     * Get the overview tree link for the main tree.
     *
     * @param label the label for the link
     * @return a content tree for the link
     */
    protected Content getNavLinkMainTree(String label) {
        Content mainTreeContent = links.createLink(pathToRoot.resolve(DocPaths.OVERVIEW_TREE),
                Text.of(label));
        Content li = HtmlTree.LI(mainTreeContent);
        return li;
    }

    /**
     * Returns a content object containing the package name. A localized content object is
     * returned for an unnamed package. Use {@link Utils#getPackageName(PackageElement)} to
     * get a static string for the unnamed package instead.
     *
     * @param packageElement the package to check
     * @return package name content
     */
    public Content getLocalizedPackageName(PackageElement packageElement) {
        return packageElement == null || packageElement.isUnnamed()
                ? contents.defaultPackageLabel
                : getPackageLabel(packageElement.getQualifiedName());
    }

    /**
     * Returns a package name label.
     *
     * @param packageName the package name
     * @return the package name content
     */
    public Content getPackageLabel(CharSequence packageName) {
        return Text.of(packageName);
    }

    /**
     * Return the path to the class page for a typeElement.
     *
     * @param te   TypeElement for which the path is requested.
     * @param name Name of the file(doesn't include path).
     */
    protected DocPath pathString(TypeElement te, DocPath name) {
        return pathString(utils.containingPackage(te), name);
    }

    /**
     * Return path to the given file name in the given package. So if the name
     * passed is "Object.html" and the name of the package is "java.lang", and
     * if the relative path is "../.." then returned string will be
     * "../../java/lang/Object.html"
     *
     * @param packageElement Package in which the file name is assumed to be.
     * @param name File name, to which path string is.
     */
    protected DocPath pathString(PackageElement packageElement, DocPath name) {
        return pathToRoot.resolve(docPaths.forPackage(packageElement).resolve(name));
    }

    /**
     * Return the link to the given package.
     *
     * @param packageElement the package to link to.
     * @param label the label for the link.
     * @return a content tree for the package link.
     */
    public Content getPackageLink(PackageElement packageElement, Content label) {
        boolean included = packageElement != null && utils.isIncluded(packageElement);
        if (!included) {
            for (PackageElement p : configuration.packages) {
                if (p.equals(packageElement)) {
                    included = true;
                    break;
                }
            }
        }
        Set<ElementFlag> flags;
        if (packageElement != null) {
            flags = utils.elementFlags(packageElement);
        } else {
            flags = EnumSet.noneOf(ElementFlag.class);
        }
        DocLink targetLink = null;
        if (included || packageElement == null) {
            targetLink = new DocLink(pathString(packageElement, DocPaths.PACKAGE_SUMMARY));
        } else {
            targetLink = getCrossPackageLink(packageElement);
        }
        if (targetLink != null) {
            if (flags.contains(ElementFlag.PREVIEW)) {
                return new ContentBuilder(
                    links.createLink(targetLink, label),
                    HtmlTree.SUP(links.createLink(targetLink.withFragment(htmlIds.forPreviewSection(packageElement).name()),
                                                  contents.previewMark))
                );
            }
            return links.createLink(targetLink, label);
        } else {
            if (flags.contains(ElementFlag.PREVIEW)) {
                return new ContentBuilder(
                    label,
                    HtmlTree.SUP(contents.previewMark)
                );
            }
            return label;
        }
    }

    /**
     * Get Module link.
     *
     * @param mdle the module being documented
     * @param label tag for the link
     * @return a content for the module link
     */
    public Content getModuleLink(ModuleElement mdle, Content label) {
        Set<ElementFlag> flags = mdle != null ? utils.elementFlags(mdle)
                                              : EnumSet.noneOf(ElementFlag.class);
        boolean included = utils.isIncluded(mdle);
        if (included) {
            DocLink targetLink = new DocLink(pathToRoot.resolve(docPaths.moduleSummary(mdle)));
            Content link = links.createLink(targetLink, label, "");
            if (flags.contains(ElementFlag.PREVIEW) && label != contents.moduleLabel) {
                link = new ContentBuilder(
                        link,
                        HtmlTree.SUP(links.createLink(targetLink.withFragment(htmlIds.forPreviewSection(mdle).name()),
                                                      contents.previewMark))
                );
            }
            return link;
        }
        if (flags.contains(ElementFlag.PREVIEW)) {
            return new ContentBuilder(
                label,
                HtmlTree.SUP(contents.previewMark)
            );
        }
        return label;
    }

    /**
     * Add the link to the content tree.
     *
     * @param element program element for which the link will be added
     * @param label label for the link
     * @param htmltree the content tree to which the link will be added
     */
    public void addSrcLink(Element element, Content label, Content htmltree) {
        if (element == null) {
            return;
        }
        TypeElement te = utils.getEnclosingTypeElement(element);
        if (te == null) {
            // must be a typeElement since in has no containing class.
            te = (TypeElement) element;
        }
        if (utils.isIncluded(te)) {
            DocPath href = pathToRoot
                    .resolve(DocPaths.SOURCE_OUTPUT)
                    .resolve(docPaths.forClass(te));
            Content content = links.createLink(href
                    .fragment(SourceToHTMLConverter.getAnchorName(utils, element).name()), label, "");
            htmltree.add(content);
        } else {
            htmltree.add(label);
        }
    }

    /**
     * Return the link to the given class.
     *
     * @param linkInfo the information about the link.
     *
     * @return the link for the given class.
     */
    public Content getLink(HtmlLinkInfo linkInfo) {
        HtmlLinkFactory factory = new HtmlLinkFactory(this);
        return factory.getLink(linkInfo);
    }

    /**
     * Return the type parameters for the given class.
     *
     * @param linkInfo the information about the link.
     * @return the type for the given class.
     */
    public Content getTypeParameterLinks(HtmlLinkInfo linkInfo) {
        HtmlLinkFactory factory = new HtmlLinkFactory(this);
        return factory.getTypeParameterLinks(linkInfo);
    }

    /*************************************************************
     * Return a class cross link to external class documentation.
     * The -link option does not allow users to
     * link to external classes in the "default" package.
     *
     * @param classElement the class element
     * @param refMemName the name of the member being referenced.  This should
     * be null or empty string if no member is being referenced.
     * @param label the label for the external link.
     * @param style optional style for the link.
     * @param code true if the label should be code font.
     * @return the link
     */
    public Content getCrossClassLink(TypeElement classElement, String refMemName,
                                    Content label, HtmlStyle style, boolean code) {
        if (classElement != null) {
            String className = utils.getSimpleName(classElement);
            PackageElement packageElement = utils.containingPackage(classElement);
            Content defaultLabel = Text.of(className);
            if (code)
                defaultLabel = HtmlTree.CODE(defaultLabel);
            if (getCrossPackageLink(packageElement) != null) {
                /*
                The package exists in external documentation, so link to the external
                class (assuming that it exists).  This is definitely a limitation of
                the -link option.  There are ways to determine if an external package
                exists, but no way to determine if the external class exists.  We just
                have to assume that it does.
                */
                DocLink link = configuration.extern.getExternalLink(packageElement, pathToRoot,
                                className + ".html", refMemName);
                return links.createLink(link,
                    (label == null) || label.isEmpty() ? defaultLabel : label, style,
                    resources.getText("doclet.Href_Class_Or_Interface_Title",
                        getLocalizedPackageName(packageElement)), true);
            }
        }
        return null;
    }

    public DocLink getCrossPackageLink(PackageElement element) {
        return configuration.extern.getExternalLink(element, pathToRoot,
            DocPaths.PACKAGE_SUMMARY.getPath());
    }

    public DocLink getCrossModuleLink(ModuleElement element) {
        return configuration.extern.getExternalLink(element, pathToRoot,
            docPaths.moduleSummary(utils.getModuleName(element)).getPath());
    }

    /**
     * Get the class link.
     *
     * @param context the id of the context where the link will be added
     * @param element to link to
     * @return a content tree for the link
     */
    public Content getQualifiedClassLink(HtmlLinkInfo.Kind context, Element element) {
        HtmlLinkInfo htmlLinkInfo = new HtmlLinkInfo(configuration, context, (TypeElement)element);
        return getLink(htmlLinkInfo.label(utils.getFullyQualifiedName(element)));
    }

    /**
     * Add the class link.
     *
     * @param context the id of the context where the link will be added
     * @param typeElement to link to
     * @param contentTree the content tree to which the link will be added
     */
    public void addPreQualifiedClassLink(HtmlLinkInfo.Kind context, TypeElement typeElement, Content contentTree) {
        addPreQualifiedClassLink(context, typeElement, null, contentTree);
    }

    /**
     * Retrieve the class link with the package portion of the label in
     * plain text.  If the qualifier is excluded, it will not be included in the
     * link label.
     *
     * @param typeElement the class to link to.
     * @return the link with the package portion of the label in plain text.
     */
    public Content getPreQualifiedClassLink(HtmlLinkInfo.Kind context, TypeElement typeElement) {
        ContentBuilder classlink = new ContentBuilder();
        PackageElement pkg = utils.containingPackage(typeElement);
        if (pkg != null && ! configuration.shouldExcludeQualifier(pkg.getSimpleName().toString())) {
            classlink.add(getEnclosingPackageName(typeElement));
        }
        classlink.add(getLink(new HtmlLinkInfo(configuration,
                context, typeElement).label(utils.getSimpleName(typeElement))));
        return classlink;
    }

    /**
     * Add the class link with the package portion of the label in
     * plain text. If the qualifier is excluded, it will not be included in the
     * link label.
     *
     * @param context the id of the context where the link will be added
     * @param typeElement the class to link to
     * @param style optional style for the link
     * @param contentTree the content tree to which the link with be added
     */
    public void addPreQualifiedClassLink(HtmlLinkInfo.Kind context,
                                         TypeElement typeElement, HtmlStyle style, Content contentTree) {
        PackageElement pkg = utils.containingPackage(typeElement);
        if(pkg != null && ! configuration.shouldExcludeQualifier(pkg.getSimpleName().toString())) {
            contentTree.add(getEnclosingPackageName(typeElement));
        }
        HtmlLinkInfo linkinfo = new HtmlLinkInfo(configuration, context, typeElement)
                .label(utils.getSimpleName(typeElement))
                .style(style);
        Content link = getLink(linkinfo);
        contentTree.add(link);
    }

    /**
     * Get the enclosed name of the package
     *
     * @param te  TypeElement
     * @return the name
     */
    public String getEnclosingPackageName(TypeElement te) {

        PackageElement encl = configuration.utils.containingPackage(te);
        return (encl.isUnnamed()) ? "" : (encl.getQualifiedName() + ".");
    }

    /**
     * Return the main type element of the current page or null for pages that don't have one.
     *
     * @return the type element of the current page.
     */
    protected TypeElement getCurrentPageElement() {
        return null;
    }

    /**
     * Add the class link, with only class name as the strong link and prefixing
     * plain package name.
     *
     * @param context the id of the context where the link will be added
     * @param typeElement the class to link to
     * @param contentTree the content tree to which the link with be added
     */
    public void addPreQualifiedStrongClassLink(HtmlLinkInfo.Kind context, TypeElement typeElement, Content contentTree) {
        addPreQualifiedClassLink(context, typeElement, HtmlStyle.typeNameLink, contentTree);
    }

    /**
     * Get the link for the given member.
     *
     * @param context the id of the context where the link will be added
     * @param element the member being linked to
     * @param label the label for the link
     * @return a content tree for the element link
     */
    public Content getDocLink(HtmlLinkInfo.Kind context, Element element, CharSequence label) {
        return getDocLink(context, utils.getEnclosingTypeElement(element), element,
                Text.of(label), null, false);
    }

    /**
     * Return the link for the given member.
     *
     * @param context the id of the context where the link will be printed.
     * @param typeElement the typeElement that we should link to. This is not
     *            not necessarily the type containing element since we may be
     *            inheriting comments.
     * @param element the member being linked to.
     * @param label the label for the link.
     * @return the link for the given member.
     */
    public Content getDocLink(HtmlLinkInfo.Kind context, TypeElement typeElement, Element element,
                              CharSequence label) {
        return getDocLink(context, typeElement, element, Text.of(label), null, false);
    }

    /**
     * Return the link for the given member.
     *
     * @param context the id of the context where the link will be printed.
     * @param typeElement the typeElement that we should link to. This is not
     *            not necessarily the type containing element since we may be
     *            inheriting comments.
     * @param element the member being linked to.
     * @param label the label for the link.
     * @param style optional style for the link.
     * @return the link for the given member.
     */
    public Content getDocLink(HtmlLinkInfo.Kind context, TypeElement typeElement, Element element,
                              CharSequence label, HtmlStyle style) {
        return getDocLink(context, typeElement, element, Text.of(label), style, false);
    }

    /**
     * Return the link for the given member.
     *
     * @param context the id of the context where the link will be printed.
     * @param typeElement the typeElement that we should link to. This is not
     *            not necessarily the type containing element since we may be
     *            inheriting comments.
     * @param element the member being linked to.
     * @param label the label for the link.
     * @return the link for the given member.
     */
    public Content getDocLink(HtmlLinkInfo.Kind context, TypeElement typeElement, Element element,
                              CharSequence label, boolean isProperty) {
        return getDocLink(context, typeElement, element, Text.of(label), null, isProperty);
    }

    /**
     * Return the link for the given member.
     *
     * @param context the id of the context where the link will be printed.
     * @param typeElement the typeElement that we should link to. This is not
     *            not necessarily the type containing element since we may be
     *            inheriting comments.
     * @param element the member being linked to.
     * @param label the label for the link.
     * @param style optional style to use for the link.
     * @param isProperty true if the element parameter is a JavaFX property.
     * @return the link for the given member.
     */
    public Content getDocLink(HtmlLinkInfo.Kind context, TypeElement typeElement, Element element,
                              Content label, HtmlStyle style, boolean isProperty) {
        if (!utils.isLinkable(typeElement, element)) {
            return label;
        }

        if (utils.isExecutableElement(element)) {
            ExecutableElement ee = (ExecutableElement)element;
            HtmlId id = isProperty ? htmlIds.forProperty(ee) : htmlIds.forMember(ee);
            return getLink(new HtmlLinkInfo(configuration, context, typeElement)
                .label(label)
                .where(id.name())
                .style(style)
                .targetMember(element));
        }

        if (utils.isVariableElement(element) || utils.isTypeElement(element)) {
            return getLink(new HtmlLinkInfo(configuration, context, typeElement)
                .label(label)
                .where(element.getSimpleName().toString())
                .style(style)
                .targetMember(element));
        }

        return label;
    }

    public Content seeTagToContent(Element element, DocTree see, TagletWriterImpl.Context context) {
        Kind kind = see.getKind();
        CommentHelper ch = utils.getCommentHelper(element);
        String tagName = ch.getTagName(see);

        String seeText = utils.normalizeNewlines(ch.getText(see)).toString();
        List<? extends DocTree> label;
        switch (kind) {
            case LINK, LINK_PLAIN ->
                // {@link[plain] reference label...}
                label = ((LinkTree) see).getLabel();

            case SEE -> {
                List<? extends DocTree> ref = ((SeeTree) see).getReference();
                assert !ref.isEmpty();
                switch (ref.get(0).getKind()) {
                    case TEXT -> {
                        // @see "Reference"
                        return Text.of(seeText);
                    }
                    case START_ELEMENT -> {
                        // @see <a href="...">...</a>
                        return new RawHtml(replaceDocRootDir(removeTrailingSlash(seeText)));
                    }
                    case REFERENCE -> {
                        // @see reference label...
                        label = ref.subList(1, ref.size());
                    }
                    default ->
                        throw new IllegalStateException(ref.get(0).getKind().toString());
                }
            }

            default ->
                throw new IllegalStateException(kind.toString());
        }

        boolean isLinkPlain = kind == LINK_PLAIN;
        Content labelContent = plainOrCode(isLinkPlain,
                commentTagsToContent(see, element, label, context));

        // The signature from the @see tag. We will output this text when a label is not specified.
        Content text = plainOrCode(isLinkPlain,
                Text.of(Objects.requireNonNullElse(ch.getReferencedSignature(see), "")));

        TypeElement refClass = ch.getReferencedClass(see);
        Element refMem =       ch.getReferencedMember(see);
        String refMemName =    ch.getReferencedMemberName(see);

        if (refMemName == null && refMem != null) {
            refMemName = refMem.toString();
        }
        if (refClass == null) {
            ModuleElement refModule = ch.getReferencedModule(see);
            if (refModule != null && utils.isIncluded(refModule)) {
                return getModuleLink(refModule, labelContent.isEmpty() ? text : labelContent);
            }
            //@see is not referencing an included class
            PackageElement refPackage = ch.getReferencedPackage(see);
            if (refPackage != null && utils.isIncluded(refPackage)) {
                //@see is referencing an included package
                if (labelContent.isEmpty())
                    labelContent = plainOrCode(isLinkPlain,
                            Text.of(refPackage.getQualifiedName()));
                return getPackageLink(refPackage, labelContent);
            } else {
                // @see is not referencing an included class, module or package. Check for cross links.
                String refModuleName =  ch.getReferencedModuleName(see);
                DocLink elementCrossLink = (refPackage != null) ? getCrossPackageLink(refPackage) :
                        (configuration.extern.isModule(refModuleName))
                                ? getCrossModuleLink(utils.elementUtils.getModuleElement(refModuleName))
                                : null;
                if (elementCrossLink != null) {
                    // Element cross link found
                    return links.createExternalLink(elementCrossLink,
                            (labelContent.isEmpty() ? text : labelContent));
                } else {
                    // No cross link found so print warning
                    messages.warning(ch.getDocTreePath(see),
                            "doclet.see.class_or_package_not_found",
                            "@" + tagName,
                            seeText);
                    return (labelContent.isEmpty() ? text: labelContent);
                }
            }
        } else if (refMemName == null) {
            // Must be a class reference since refClass is not null and refMemName is null.
            if (labelContent.isEmpty()) {
                TypeMirror referencedType = ch.getReferencedType(see);
                if (utils.isGenericType(referencedType)) {
                    // This is a generic type link, use the TypeMirror representation.
                    return plainOrCode(isLinkPlain, getLink(
                            new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.DEFAULT, referencedType)));
                }
                labelContent = plainOrCode(isLinkPlain, Text.of(utils.getSimpleName(refClass)));
            }
            return getLink(new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.DEFAULT, refClass)
                    .label(labelContent));
        } else if (refMem == null) {
            // Must be a member reference since refClass is not null and refMemName is not null.
            // However, refMem is null, so this referenced member does not exist.
            return (labelContent.isEmpty() ? text: labelContent);
        } else {
            // Must be a member reference since refClass is not null and refMemName is not null.
            // refMem is not null, so this @see tag must be referencing a valid member.
            TypeElement containing = utils.getEnclosingTypeElement(refMem);

            // Find the enclosing type where the method is actually visible
            // in the inheritance hierarchy.
            ExecutableElement overriddenMethod = null;
            if (refMem.getKind() == ElementKind.METHOD) {
                VisibleMemberTable vmt = configuration.getVisibleMemberTable(containing);
                overriddenMethod = vmt.getOverriddenMethod((ExecutableElement)refMem);

                if (overriddenMethod != null)
                    containing = utils.getEnclosingTypeElement(overriddenMethod);
            }
            if (ch.getText(see).trim().startsWith("#") &&
                ! (utils.isPublic(containing) || utils.isLinkable(containing))) {
                // Since the link is relative and the holder is not even being
                // documented, this must be an inherited link.  Redirect it.
                // The current class either overrides the referenced member or
                // inherits it automatically.
                if (this instanceof ClassWriterImpl writer) {
                    containing = writer.getTypeElement();
                } else if (!utils.isPublic(containing)) {
                    messages.warning(
                        ch.getDocTreePath(see), "doclet.see.class_or_package_not_accessible",
                        tagName, utils.getFullyQualifiedName(containing));
                } else {
                    messages.warning(
                        ch.getDocTreePath(see), "doclet.see.class_or_package_not_found",
                        tagName, seeText);
                }
            }
            if (configuration.currentTypeElement != containing) {
                refMemName = (utils.isConstructor(refMem))
                        ? refMemName
                        : utils.getSimpleName(containing) + "." + refMemName;
            }
            if (utils.isExecutableElement(refMem)) {
                if (refMemName.indexOf('(') < 0) {
                    refMemName += utils.makeSignature((ExecutableElement) refMem, null, true);
                }
                if (overriddenMethod != null) {
                    // The method to actually link.
                    refMem = overriddenMethod;
                }
            }

            return getDocLink(HtmlLinkInfo.Kind.SEE_TAG, containing,
                    refMem, (labelContent.isEmpty()
                            ? plainOrCode(isLinkPlain, Text.of(refMemName))
                            : labelContent), null, false);
        }
    }

    private String removeTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() -1) : s;
    }

    private Content plainOrCode(boolean plain, Content body) {
        return (plain || body.isEmpty()) ? body : HtmlTree.CODE(body);
    }

    /**
     * Add the inline comment.
     *
     * @param element the Element for which the inline comment will be added
     * @param tag the inline tag to be added
     * @param htmltree the content tree to which the comment will be added
     */
    public void addInlineComment(Element element, DocTree tag, Content htmltree) {
        CommentHelper ch = utils.getCommentHelper(element);
        List<? extends DocTree> description = ch.getDescription(tag);
        addCommentTags(element, description, false, false, false, htmltree);
    }

    /**
     * Get the deprecated phrase as content.
     *
     * @param e the Element for which the inline deprecated comment will be added
     * @return a content tree for the deprecated phrase.
     */
    public Content getDeprecatedPhrase(Element e) {
        return (utils.isDeprecatedForRemoval(e))
                ? contents.deprecatedForRemovalPhrase
                : contents.deprecatedPhrase;
    }

    /**
     * Add the inline deprecated comment.
     *
     * @param e the Element for which the inline deprecated comment will be added
     * @param tag the inline tag to be added
     * @param htmltree the content tree to which the comment will be added
     */
    public void addInlineDeprecatedComment(Element e, DeprecatedTree tag, Content htmltree) {
        CommentHelper ch = utils.getCommentHelper(e);
        addCommentTags(e, ch.getBody(tag), true, false, false, htmltree);
    }

    /**
     * Adds the summary content.
     *
     * @param element the Element for which the summary will be generated
     * @param htmltree the documentation tree to which the summary will be added
     */
    public void addSummaryComment(Element element, Content htmltree) {
        addSummaryComment(element, utils.getFirstSentenceTrees(element), htmltree);
    }

    /**
     * Adds the preview content.
     *
     * @param element the Element for which the summary will be generated
     * @param firstSentenceTags the first sentence tags for the doc
     * @param htmltree the documentation tree to which the summary will be added
     */
    public void addPreviewComment(Element element, List<? extends DocTree> firstSentenceTags, Content htmltree) {
        addCommentTags(element, firstSentenceTags, false, true, true, htmltree);
    }

    /**
     * Adds the summary content.
     *
     * @param element the Element for which the summary will be generated
     * @param firstSentenceTags the first sentence tags for the doc
     * @param htmltree the documentation tree to which the summary will be added
     */
    public void addSummaryComment(Element element, List<? extends DocTree> firstSentenceTags, Content htmltree) {
        addCommentTags(element, firstSentenceTags, false, true, true, htmltree);
    }

    public void addSummaryDeprecatedComment(Element element, DeprecatedTree tag, Content htmltree) {
        CommentHelper ch = utils.getCommentHelper(element);
        List<? extends DocTree> body = ch.getBody(tag);
        addCommentTags(element, ch.getFirstSentenceTrees(body), true, true, true, htmltree);
    }

    /**
     * Adds the inline comment.
     *
     * @param element the Element for which the inline comments will be generated
     * @param htmltree the documentation tree to which the inline comments will be added
     */
    public void addInlineComment(Element element, Content htmltree) {
        addCommentTags(element, utils.getFullBody(element), false, false, false, htmltree);
    }

    /**
     * Adds the comment tags.
     *
     * @param element the Element for which the comment tags will be generated
     * @param tags the first sentence tags for the doc
     * @param depr true if it is deprecated
     * @param first true if the first sentence tags should be added
     * @param inSummary true if the comment tags are added into the summary section
     * @param htmltree the documentation tree to which the comment tags will be added
     */
    private void addCommentTags(Element element, List<? extends DocTree> tags, boolean depr,
            boolean first, boolean inSummary, Content htmltree) {
        if (options.noComment()) {
            return;
        }
        Content div;
        Content result = commentTagsToContent(null, element, tags, first, inSummary);
        if (depr) {
            div = HtmlTree.DIV(HtmlStyle.deprecationComment, result);
            htmltree.add(div);
        } else {
            div = HtmlTree.DIV(HtmlStyle.block, result);
            htmltree.add(div);
        }
        if (tags.isEmpty()) {
            htmltree.add(Entity.NO_BREAK_SPACE);
        }
    }

    boolean ignoreNonInlineTag(DocTree dtree) {
        Name name = null;
        if (dtree.getKind() == Kind.START_ELEMENT) {
            StartElementTree setree = (StartElementTree)dtree;
            name = setree.getName();
        } else if (dtree.getKind() == Kind.END_ELEMENT) {
            EndElementTree eetree = (EndElementTree)dtree;
            name = eetree.getName();
        }

        if (name != null) {
            HtmlTag htmlTag = HtmlTag.get(name);
            if (htmlTag != null &&
                    htmlTag.blockType != jdk.javadoc.internal.doclint.HtmlTag.BlockType.INLINE) {
                return true;
            }
        }
        return false;
    }

    boolean isAllWhiteSpace(String body) {
        for (int i = 0 ; i < body.length(); i++) {
            if (!Character.isWhitespace(body.charAt(i)))
                return false;
        }
        return true;
    }

    // Notify the next DocTree handler to take necessary action
    private boolean commentRemoved = false;

    /**
     * Converts inline tags and text to Content, expanding the
     * inline tags along the way.  Called wherever text can contain
     * an inline tag, such as in comments or in free-form text arguments
     * to block tags.
     *
     * @param holderTag    specific tag where comment resides
     * @param element    specific element where comment resides
     * @param tags   array of text tags and inline tags (often alternating)
               present in the text of interest for this element
     * @param isFirstSentence  true if text is first sentence
     * @return a Content object
     */
    public Content commentTagsToContent(DocTree holderTag,
                                        Element element,
                                        List<? extends DocTree> tags,
                                        boolean isFirstSentence)
    {
        return commentTagsToContent(holderTag, element, tags, isFirstSentence, false);
    }

    /**
     * Converts inline tags and text to text strings, expanding the
     * inline tags along the way.  Called wherever text can contain
     * an inline tag, such as in comments or in free-form text arguments
     * to block tags.
     *
     * @param holderTag       specific tag where comment resides
     * @param element         specific element where comment resides
     * @param trees           array of text tags and inline tags (often alternating)
     *                        present in the text of interest for this element
     * @param isFirstSentence true if text is first sentence
     * @param inSummary       if the comment tags are added into the summary section
     * @return a Content object
     */
    public Content commentTagsToContent(DocTree holderTag,
                                        Element element,
                                        List<? extends DocTree> trees,
                                        boolean isFirstSentence,
                                        boolean inSummary) {
        return commentTagsToContent(holderTag, element, trees,
                new TagletWriterImpl.Context(isFirstSentence, inSummary));
    }

    /**
     * Converts inline tags and text to text strings, expanding the
     * inline tags along the way.  Called wherever text can contain
     * an inline tag, such as in comments or in free-form text arguments
     * to block tags.
     *
     * @param holderTag specific tag where comment resides
     * @param element   specific element where comment resides
     * @param trees     list of text trees and inline tag trees (often alternating)
     *                  present in the text of interest for this element
     * @param context   the enclosing context for the trees
     *
     * @return a Content object
     */
    public Content commentTagsToContent(DocTree holderTag,
                                        Element element,
                                        List<? extends DocTree> trees,
                                        TagletWriterImpl.Context context)
    {
        final Content result = new ContentBuilder() {
            @Override
            public ContentBuilder add(CharSequence text) {
                return super.add(utils.normalizeNewlines(text));
            }
        };
        CommentHelper ch = utils.getCommentHelper(element);
        // Array of all possible inline tags for this javadoc run
        configuration.tagletManager.checkTags(element, trees, true);
        commentRemoved = false;

        for (ListIterator<? extends DocTree> iterator = trees.listIterator(); iterator.hasNext();) {
            boolean isFirstNode = !iterator.hasPrevious();
            DocTree tag = iterator.next();
            boolean isLastNode  = !iterator.hasNext();

            if (context.isFirstSentence) {
                // Ignore block tags
                if (ignoreNonInlineTag(tag))
                    continue;

                // Ignore any trailing whitespace OR whitespace after removed html comment
                if ((isLastNode || commentRemoved)
                        && tag.getKind() == TEXT
                        && isAllWhiteSpace(ch.getText(tag)))
                    continue;

                // Ignore any leading html comments
                if ((isFirstNode || commentRemoved) && tag.getKind() == COMMENT) {
                    commentRemoved = true;
                    continue;
                }
            }

            boolean allDone = new SimpleDocTreeVisitor<Boolean, Content>() {

                private boolean inAnAtag() {
                    if (utils.isStartElement(tag)) {
                        StartElementTree st = (StartElementTree)tag;
                        Name name = st.getName();
                        if (name != null) {
                            HtmlTag htag = HtmlTag.get(name);
                            return htag != null && htag.equals(HtmlTag.A);
                        }
                    }
                    return false;
                }

                @Override
                public Boolean visitAttribute(AttributeTree node, Content c) {
                    StringBuilder sb = new StringBuilder(SPACER).append(node.getName().toString());
                    if (node.getValueKind() == ValueKind.EMPTY) {
                        result.add(sb);
                        return false;
                    }
                    sb.append("=");
                    String quote;
                    switch (node.getValueKind()) {
                        case DOUBLE:
                            quote = "\"";
                            break;
                        case SINGLE:
                            quote = "'";
                            break;
                        default:
                            quote = "";
                            break;
                    }
                    sb.append(quote);
                    result.add(sb);
                    Content docRootContent = new ContentBuilder();

                    boolean isHRef = inAnAtag() && node.getName().toString().equalsIgnoreCase("href");
                    for (DocTree dt : node.getValue()) {
                        if (utils.isText(dt) && isHRef) {
                            String text = ((TextTree) dt).getBody();
                            if (text.startsWith("/..") && !options.docrootParent().isEmpty()) {
                                result.add(options.docrootParent());
                                docRootContent = new ContentBuilder();
                                result.add(textCleanup(text.substring(3), isLastNode));
                            } else {
                                if (!docRootContent.isEmpty()) {
                                    docRootContent = copyDocRootContent(docRootContent);
                                } else {
                                    text = redirectRelativeLinks(element, (TextTree) dt);
                                }
                                result.add(textCleanup(text, isLastNode));
                            }
                        } else {
                            docRootContent = copyDocRootContent(docRootContent);
                            dt.accept(this, docRootContent);
                        }
                    }
                    copyDocRootContent(docRootContent);
                    result.add(quote);
                    return false;
                }

                @Override
                public Boolean visitComment(CommentTree node, Content c) {
                    result.add(new RawHtml(node.getBody()));
                    return false;
                }

                private Content copyDocRootContent(Content content) {
                    if (!content.isEmpty()) {
                        result.add(content);
                        return new ContentBuilder();
                    }
                    return content;
                }

                @Override
                public Boolean visitDocRoot(DocRootTree node, Content c) {
                    Content docRootContent = getInlineTagOutput(element, holderTag, node, context);
                    if (c != null) {
                        c.add(docRootContent);
                    } else {
                        result.add(docRootContent);
                    }
                    return false;
                }

                @Override
                public Boolean visitEndElement(EndElementTree node, Content c) {
                    RawHtml rawHtml = new RawHtml("</" + node.getName() + ">");
                    result.add(rawHtml);
                    return false;
                }

                @Override
                public Boolean visitEntity(EntityTree node, Content c) {
                    result.add(new RawHtml(node.toString()));
                    return false;
                }

                @Override
                public Boolean visitErroneous(ErroneousTree node, Content c) {
                    DocTreePath dtp = ch.getDocTreePath(node);
                    if (dtp != null) {
                        String body = node.getBody();
                        if (body.matches("(?i)\\{@[a-z]+.*")) {
                            messages.warning(dtp,"doclet.tag.invalid_usage", body);
                        } else {
                            messages.warning(dtp, "doclet.tag.invalid_input", body);
                        }
                    }
                    result.add(Text.of(node.toString()));
                    return false;
                }

                @Override
                public Boolean visitInheritDoc(InheritDocTree node, Content c) {
                    Content output = getInlineTagOutput(element, holderTag, node, context);
                    result.add(output);
                    // if we obtained the first sentence successfully, nothing more to do
                    return (context.isFirstSentence && !output.isEmpty());
                }

                @Override
                public Boolean visitIndex(IndexTree node, Content p) {
                    Content output = getInlineTagOutput(element, holderTag, node, context);
                    if (output != null) {
                        result.add(output);
                    }
                    return false;
                }

                @Override
                public Boolean visitLink(LinkTree node, Content c) {
                    var inTags = context.inTags;
                    if (inTags.contains(LINK) || inTags.contains(LINK_PLAIN) || inTags.contains(SEE)) {
                        DocTreePath dtp = ch.getDocTreePath(node);
                        if (dtp != null) {
                            messages.warning(dtp, "doclet.see.nested_link", "{@" + node.getTagName() + "}");
                        }
                        Content label = commentTagsToContent(node, element, node.getLabel(), context);
                        if (label.isEmpty()) {
                            label = Text.of(node.getReference().getSignature());
                        }
                        result.add(label);
                    } else {
                        Content content = seeTagToContent(element, node, context.within(node));
                        result.add(content);
                    }
                    return false;
                }

                @Override
                public Boolean visitLiteral(LiteralTree node, Content c) {
                    String s = node.getBody().getBody();
                    Content content = Text.of(utils.normalizeNewlines(s));
                    if (node.getKind() == CODE)
                        content = HtmlTree.CODE(content);
                    result.add(content);
                    return false;
                }

                @Override
                public Boolean visitSee(SeeTree node, Content c) {
                    result.add(seeTagToContent(element, node, context));
                    return false;
                }

                @Override
                public Boolean visitStartElement(StartElementTree node, Content c) {
                    String text = "<" + node.getName();
                    RawHtml rawHtml = new RawHtml(utils.normalizeNewlines(text));
                    result.add(rawHtml);

                    for (DocTree dt : node.getAttributes()) {
                        dt.accept(this, null);
                    }
                    result.add(new RawHtml(node.isSelfClosing() ? "/>" : ">"));
                    return false;
                }

                @Override
                public Boolean visitSummary(SummaryTree node, Content c) {
                    Content output = getInlineTagOutput(element, holderTag, node, context);
                    result.add(output);
                    return false;
                }

                @Override
                public Boolean visitSystemProperty(SystemPropertyTree node, Content p) {
                    Content output = getInlineTagOutput(element, holderTag, node, context);
                    if (output != null) {
                        result.add(output);
                    }
                    return false;
                }

                private CharSequence textCleanup(String text, boolean isLast) {
                    return textCleanup(text, isLast, false);
                }

                private CharSequence textCleanup(String text, boolean isLast, boolean stripLeading) {
                    boolean stripTrailing = context.isFirstSentence && isLast;
                    if (stripLeading && stripTrailing) {
                        text = text.strip();
                    } else if (stripLeading) {
                        text = text.stripLeading();
                    } else if (stripTrailing) {
                        text = text.stripTrailing();
                    }
                    text = utils.replaceTabs(text);
                    return utils.normalizeNewlines(text);
                }

                @Override
                public Boolean visitText(TextTree node, Content c) {
                    String text = node.getBody();
                    result.add(new RawHtml(textCleanup(text, isLastNode, commentRemoved)));
                    return false;
                }

                @Override
                protected Boolean defaultAction(DocTree node, Content c) {
                    Content output = getInlineTagOutput(element, holderTag, node, context);
                    if (output != null) {
                        result.add(output);
                    }
                    return false;
                }

            }.visit(tag, null);
            commentRemoved = false;
            if (allDone)
                break;
        }
        return result;
    }

    /**
     * Returns true if relative links should be redirected.
     *
     * @return true if a relative link should be redirected.
     */
    private boolean shouldRedirectRelativeLinks(Element element) {
        if (element == null || utils.isOverviewElement(element)) {
            // Can't redirect unless there is a valid source element.
            return false;
        }
        // Retrieve the element of this writer if it is a "primary" writer for an element.
        // Note: It would be nice to have getCurrentPageElement() return package and module elements
        // in their respective writers, but other uses of the method are only interested in TypeElements.
        Element currentPageElement = getCurrentPageElement();
        if (currentPageElement == null) {
            if (this instanceof PackageWriterImpl packageWriter) {
                currentPageElement = packageWriter.packageElement;
            } else if (this instanceof ModuleWriterImpl moduleWriter) {
                currentPageElement = moduleWriter.mdle;
            }
        }
        // Redirect link if the current writer is not the primary writer for the source element.
        return currentPageElement == null
                || (currentPageElement != element
                    &&  currentPageElement != utils.getEnclosingTypeElement(element));
    }

    /**
     * Returns true if element lives in the same package as the type or package
     * element of this writer.
     */
    private boolean inSamePackage(Element element) {
        Element currentPageElement = (this instanceof PackageWriterImpl packageWriter)
                ? packageWriter.packageElement : getCurrentPageElement();
        return currentPageElement != null && !utils.isModule(element)
                && utils.containingPackage(currentPageElement) == utils.containingPackage(element);
    }

    /**
     * Suppose a piece of documentation has a relative link.  When you copy
     * that documentation to another place such as the index or class-use page,
     * that relative link will no longer work.  We should redirect those links
     * so that they will work again.
     * <p>
     * Here is the algorithm used to fix the link:
     * <p>
     * {@literal <relative link> => docRoot + <relative path to file> + <relative link> }
     * <p>
     * For example, suppose DocletEnvironment has this link:
     * {@literal <a href="package-summary.html">The package Page</a> }
     * <p>
     * If this link appeared in the index, we would redirect
     * the link like this:
     *
     * {@literal <a href="./jdk/javadoc/doclet/package-summary.html">The package Page</a>}
     *
     * @param element the Element object whose documentation is being written.
     * @param tt the text being written.
     *
     * @return the text, with all the relative links redirected to work.
     */
    private String redirectRelativeLinks(Element element, TextTree tt) {
        String text = tt.getBody();
        if (!shouldRedirectRelativeLinks(element)) {
            return text;
        }
        String lower = Utils.toLowerCase(text);
        if (lower.startsWith("mailto:")
                || lower.startsWith("http:")
                || lower.startsWith("https:")
                || lower.startsWith("file:")) {
            return text;
        }
        if (text.startsWith("#")) {
            // Redirected fragment link: prepend HTML file name to make it work
            if (utils.isModule(element)) {
                text = "module-summary.html" + text;
            } else if (utils.isPackage(element)) {
                text = DocPaths.PACKAGE_SUMMARY.getPath() + text;
            } else {
                TypeElement typeElement = element instanceof TypeElement
                        ? (TypeElement) element : utils.getEnclosingTypeElement(element);
                text = docPaths.forName(typeElement).getPath() + text;
            }
        }

        if (!inSamePackage(element)) {
            DocPath redirectPathFromRoot = new SimpleElementVisitor14<DocPath, Void>() {
                @Override
                public DocPath visitType(TypeElement e, Void p) {
                    return docPaths.forPackage(utils.containingPackage(e));
                }

                @Override
                public DocPath visitPackage(PackageElement e, Void p) {
                    return docPaths.forPackage(e);
                }

                @Override
                public DocPath visitVariable(VariableElement e, Void p) {
                    return docPaths.forPackage(utils.containingPackage(e));
                }

                @Override
                public DocPath visitExecutable(ExecutableElement e, Void p) {
                    return docPaths.forPackage(utils.containingPackage(e));
                }

                @Override
                public DocPath visitModule(ModuleElement e, Void p) {
                    return DocPaths.forModule(e);
                }

                @Override
                protected DocPath defaultAction(Element e, Void p) {
                    return null;
                }
            }.visit(element);
            if (redirectPathFromRoot != null) {
                text = "{@" + (new DocRootTaglet()).getName() + "}/"
                        + redirectPathFromRoot.resolve(text).getPath();
                return replaceDocRootDir(text);
            }
        }
        return text;
    }

    /**
     * According to
     * <cite>The Java Language Specification</cite>,
     * all the outer classes and static nested classes are core classes.
     */
    public boolean isCoreClass(TypeElement typeElement) {
        return utils.getEnclosingTypeElement(typeElement) == null || utils.isStatic(typeElement);
    }

    /**
     * Return a content tree containing the  annotation types for the given element.
     *
     * @param element an Element
     * @param lineBreak if true add new line between each member value
     * @return the documentation tree containing the annotation info
     */
    Content getAnnotationInfo(Element element, boolean lineBreak) {
        return getAnnotationInfo(element.getAnnotationMirrors(), lineBreak);
    }

    /**
     * Return a content tree containing the annotation types for the given element.
     *
     * @param descList a list of annotation mirrors
     * @param lineBreak if true add new line between each member value
     * @return the documentation tree containing the annotation info
     */
    Content getAnnotationInfo(List<? extends AnnotationMirror> descList, boolean lineBreak) {
        List<Content> annotations = getAnnotations(descList, lineBreak);
        String sep = "";
        ContentBuilder builder = new ContentBuilder();
        for (Content annotation: annotations) {
            builder.add(sep);
            builder.add(annotation);
            if (!lineBreak) {
                sep = " ";
            }
        }
        return builder;
    }

    /**
     * Return the string representations of the annotation types for
     * the given doc.
     *
     * @param descList a list of annotation mirrors.
     * @param lineBreak if true, add new line between each member value.
     * @return a list of strings representing the annotations being
     *         documented.
     */
    public List<Content> getAnnotations(List<? extends AnnotationMirror> descList, boolean lineBreak) {
        List<Content> results = new ArrayList<>();
        ContentBuilder annotation;
        for (AnnotationMirror aDesc : descList) {
            TypeElement annotationElement = (TypeElement)aDesc.getAnnotationType().asElement();
            // If an annotation is not documented, do not add it to the list. If
            // the annotation is of a repeatable type, and if it is not documented
            // and also if its container annotation is not documented, do not add it
            // to the list. If an annotation of a repeatable type is not documented
            // but its container is documented, it will be added to the list.
            if (!utils.isDocumentedAnnotation(annotationElement) &&
                (!isAnnotationDocumented && !isContainerDocumented)) {
                continue;
            }
            annotation = new ContentBuilder();
            isAnnotationDocumented = false;
            HtmlLinkInfo linkInfo = new HtmlLinkInfo(configuration,
                                                     HtmlLinkInfo.Kind.ANNOTATION, annotationElement);
            Map<? extends ExecutableElement, ? extends AnnotationValue> pairs = aDesc.getElementValues();
            // If the annotation is mandated, do not print the container.
            if (utils.configuration.workArounds.isMandated(aDesc)) {
                for (ExecutableElement ee : pairs.keySet()) {
                    AnnotationValue annotationValue = pairs.get(ee);
                    List<AnnotationValue> annotationTypeValues = new ArrayList<>();

                    new SimpleAnnotationValueVisitor9<Void, List<AnnotationValue>>() {
                        @Override
                        public Void visitArray(List<? extends AnnotationValue> vals, List<AnnotationValue> p) {
                            p.addAll(vals);
                            return null;
                        }

                        @Override
                        protected Void defaultAction(Object o, List<AnnotationValue> p) {
                            p.add(annotationValue);
                            return null;
                        }
                    }.visit(annotationValue, annotationTypeValues);

                    String sep = "";
                    for (AnnotationValue av : annotationTypeValues) {
                        annotation.add(sep);
                        annotation.add(annotationValueToContent(av));
                        sep = " ";
                    }
                }
            } else if (isAnnotationArray(pairs)) {
                // If the container has 1 or more value defined and if the
                // repeatable type annotation is not documented, do not print
                // the container.
                if (pairs.size() == 1 && isAnnotationDocumented) {
                    List<AnnotationValue> annotationTypeValues = new ArrayList<>();
                    for (AnnotationValue a :  pairs.values()) {
                        new SimpleAnnotationValueVisitor9<Void, List<AnnotationValue>>() {
                            @Override
                            public Void visitArray(List<? extends AnnotationValue> vals, List<AnnotationValue> annotationTypeValues) {
                               annotationTypeValues.addAll(vals);
                               return null;
                            }
                        }.visit(a, annotationTypeValues);
                    }
                    String sep = "";
                    for (AnnotationValue av : annotationTypeValues) {
                        annotation.add(sep);
                        annotation.add(annotationValueToContent(av));
                        sep = " ";
                    }
                }
                // If the container has 1 or more value defined and if the
                // repeatable type annotation is not documented, print the container.
                else {
                    addAnnotations(annotationElement, linkInfo, annotation, pairs, false);
                }
            }
            else {
                addAnnotations(annotationElement, linkInfo, annotation, pairs, lineBreak);
            }
            annotation.add(lineBreak ? DocletConstants.NL : "");
            results.add(annotation);
        }
        return results;
    }

    /**
     * Add annotation to the annotation string.
     *
     * @param annotationDoc the annotation being documented
     * @param linkInfo the information about the link
     * @param annotation the annotation string to which the annotation will be added
     * @param map annotation type element to annotation value pairs
     * @param linkBreak if true, add new line between each member value
     */
    private void addAnnotations(TypeElement annotationDoc, HtmlLinkInfo linkInfo,
                                ContentBuilder annotation,
                                Map<? extends ExecutableElement, ? extends AnnotationValue> map,
                                boolean linkBreak) {
        linkInfo.label = Text.of("@" + annotationDoc.getSimpleName());
        annotation.add(getLink(linkInfo));
        if (!map.isEmpty()) {
            annotation.add("(");
            boolean isFirst = true;
            Set<? extends ExecutableElement> keys = map.keySet();
            boolean multipleValues = keys.size() > 1;
            for (ExecutableElement element : keys) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    annotation.add(",");
                    if (linkBreak) {
                        annotation.add(DocletConstants.NL);
                        int spaces = annotationDoc.getSimpleName().length() + 2;
                        for (int k = 0; k < (spaces); k++) {
                            annotation.add(" ");
                        }
                    }
                }
                String simpleName = element.getSimpleName().toString();
                if (multipleValues || !"value".equals(simpleName)) { // Omit "value=" where unnecessary
                    annotation.add(getDocLink(HtmlLinkInfo.Kind.ANNOTATION, element, simpleName));
                    annotation.add("=");
                }
                AnnotationValue annotationValue = map.get(element);
                List<AnnotationValue> annotationTypeValues = new ArrayList<>();
                new SimpleAnnotationValueVisitor9<Void, AnnotationValue>() {
                    @Override
                    public Void visitArray(List<? extends AnnotationValue> vals, AnnotationValue p) {
                        annotationTypeValues.addAll(vals);
                        return null;
                    }
                    @Override
                    protected Void defaultAction(Object o, AnnotationValue p) {
                        annotationTypeValues.add(p);
                        return null;
                    }
                }.visit(annotationValue, annotationValue);
                annotation.add(annotationTypeValues.size() == 1 ? "" : "{");
                String sep = "";
                for (AnnotationValue av : annotationTypeValues) {
                    annotation.add(sep);
                    annotation.add(annotationValueToContent(av));
                    sep = ",";
                }
                annotation.add(annotationTypeValues.size() == 1 ? "" : "}");
                isContainerDocumented = false;
            }
            annotation.add(")");
        }
    }

    /**
     * Check if the annotation contains an array of annotation as a value. This
     * check is to verify if a repeatable type annotation is present or not.
     *
     * @param pairs annotation type element and value pairs
     *
     * @return true if the annotation contains an array of annotation as a value.
     */
    private boolean isAnnotationArray(Map<? extends ExecutableElement, ? extends AnnotationValue> pairs) {
        AnnotationValue annotationValue;
        for (ExecutableElement ee : pairs.keySet()) {
            annotationValue = pairs.get(ee);
            boolean rvalue = new SimpleAnnotationValueVisitor9<Boolean, Void>() {
                @Override
                public Boolean visitArray(List<? extends AnnotationValue> vals, Void p) {
                    if (vals.size() > 1) {
                        if (vals.get(0) instanceof AnnotationMirror) {
                            isContainerDocumented = true;
                            return new SimpleAnnotationValueVisitor9<Boolean, Void>() {
                                @Override
                                public Boolean visitAnnotation(AnnotationMirror a, Void p) {
                                    isContainerDocumented = true;
                                    Element asElement = a.getAnnotationType().asElement();
                                    if (utils.isDocumentedAnnotation((TypeElement)asElement)) {
                                        isAnnotationDocumented = true;
                                    }
                                    return true;
                                }
                                @Override
                                protected Boolean defaultAction(Object o, Void p) {
                                    return false;
                                }
                            }.visit(vals.get(0));
                        }
                    }
                    return false;
                }

                @Override
                protected Boolean defaultAction(Object o, Void p) {
                    return false;
                }
            }.visit(annotationValue);
            if (rvalue) {
                return true;
            }
        }
        return false;
    }

    private Content annotationValueToContent(AnnotationValue annotationValue) {
        return new SimpleAnnotationValueVisitor9<Content, Void>() {

            @Override
            public Content visitType(TypeMirror t, Void p) {
                return new SimpleTypeVisitor9<Content, Void>() {
                    @Override
                    public Content visitDeclared(DeclaredType t, Void p) {
                        HtmlLinkInfo linkInfo = new HtmlLinkInfo(configuration,
                                HtmlLinkInfo.Kind.ANNOTATION, t);
                        String name = utils.isIncluded(t.asElement())
                                ? t.asElement().getSimpleName().toString()
                                : utils.getFullyQualifiedName(t.asElement());
                        linkInfo.label = Text.of(name + utils.getDimension(t) + ".class");
                        return getLink(linkInfo);
                    }
                    @Override
                    protected Content defaultAction(TypeMirror e, Void p) {
                        return Text.of(t + utils.getDimension(t) + ".class");
                    }
                }.visit(t);
            }
            @Override
            public Content visitAnnotation(AnnotationMirror a, Void p) {
                List<Content> list = getAnnotations(List.of(a), false);
                ContentBuilder buf = new ContentBuilder();
                for (Content c : list) {
                    buf.add(c);
                }
                return buf;
            }
            @Override
            public Content visitEnumConstant(VariableElement c, Void p) {
                return getDocLink(HtmlLinkInfo.Kind.ANNOTATION, c, c.getSimpleName());
            }
            @Override
            public Content visitArray(List<? extends AnnotationValue> vals, Void p) {
                ContentBuilder buf = new ContentBuilder();
                String sep = "";
                for (AnnotationValue av : vals) {
                    buf.add(sep);
                    buf.add(visit(av));
                    sep = " ";
                }
                return buf;
            }
            @Override
            protected Content defaultAction(Object o, Void p) {
                return Text.of(annotationValue.toString());
            }
        }.visit(annotationValue);
    }

    protected TableHeader getPackageTableHeader() {
        return new TableHeader(contents.packageLabel, contents.descriptionLabel);
    }

    /**
     * Generates a string for use in a description meta element,
     * based on an element and its enclosing elements
     * @param prefix a prefix for the string
     * @param elem the element
     * @return the description
     */
    static String getDescription(String prefix, Element elem) {
        LinkedList<Element> chain = new LinkedList<>();
        for (Element e = elem; e != null; e = e.getEnclosingElement()) {
            // ignore unnamed enclosing elements
            if (e.getSimpleName().length() == 0 && e != elem) {
                break;
            }
            chain.addFirst(e);
        }
        StringBuilder sb = new StringBuilder();
        for (Element e: chain) {
            String name;
            switch (e.getKind()) {
                case MODULE:
                case PACKAGE:
                    name = ((QualifiedNameable) e).getQualifiedName().toString();
                    if (name.length() == 0) {
                        name = "<unnamed>";
                    }
                    break;

                default:
                    name = e.getSimpleName().toString();
                    break;
            }

            if (sb.length() == 0) {
                sb.append(prefix).append(": ");
            } else {
                sb.append(", ");
            }
            sb.append(e.getKind().toString().toLowerCase(Locale.US).replace("_", " "))
                    .append(": ")
                    .append(name);
        }
        return sb.toString();
    }

    static String getGenerator(Class<?> clazz) {
        return "javadoc/" + clazz.getSimpleName();
    }

    /**
     * Returns an HtmlTree for the BODY tag.
     *
     * @param title title for the window
     * @return an HtmlTree for the BODY tag
     */
    public HtmlTree getBody(String title) {
        HtmlTree body = new HtmlTree(TagName.BODY).setStyle(getBodyStyle());

        this.winTitle = title;
        // Don't print windowtitle script for overview-frame, allclasses-frame
        // and package-frame
        body.add(mainBodyScript.asContent());
        Content noScript = HtmlTree.NOSCRIPT(HtmlTree.DIV(contents.noScriptMessage));
        body.add(noScript);
        return body;
    }

    public HtmlStyle getBodyStyle() {
        String kind = getClass().getSimpleName()
                .replaceAll("(Writer)?(Impl)?$", "")
                .replaceAll("AnnotationType", "Class")
                .replaceAll("^(Module|Package|Class)$", "$1Declaration")
                .replace("API", "Api");
        String page = kind.substring(0, 1).toLowerCase(Locale.US) + kind.substring(1) + "Page";
        return HtmlStyle.valueOf(page);
    }

    Script getMainBodyScript() {
        return mainBodyScript;
    }

    /**
     * Returns the path of module/package specific stylesheets for the element.
     * @param element module/Package element
     * @return list of path of module/package specific stylesheets
     * @throws DocFileIOException
     */
    List<DocPath> getLocalStylesheets(Element element) throws DocFileIOException {
        List<DocPath> stylesheets = new ArrayList<>();
        DocPath basePath = null;
        if (element instanceof PackageElement pkg) {
            stylesheets.addAll(getModuleStylesheets(pkg));
            basePath = docPaths.forPackage(pkg);
        } else if (element instanceof ModuleElement mdle) {
            basePath = DocPaths.forModule(mdle);
        }
        for (DocPath stylesheet : getStylesheets(element)) {
            stylesheets.add(basePath.resolve(stylesheet.getPath()));
        }
        return stylesheets;
    }

    private List<DocPath> getModuleStylesheets(PackageElement pkgElement) throws
            DocFileIOException {
        List<DocPath> moduleStylesheets = new ArrayList<>();
        ModuleElement moduleElement = utils.containingModule(pkgElement);
        if (moduleElement != null && !moduleElement.isUnnamed()) {
            List<DocPath> localStylesheets = getStylesheets(moduleElement);
            DocPath basePath = DocPaths.forModule(moduleElement);
            for (DocPath stylesheet : localStylesheets) {
                moduleStylesheets.add(basePath.resolve(stylesheet));
            }
        }
        return moduleStylesheets;
    }

    private List<DocPath> getStylesheets(Element element) throws DocFileIOException {
        List<DocPath> localStylesheets = configuration.localStylesheetMap.get(element);
        if (localStylesheets == null) {
            DocFilesHandlerImpl docFilesHandler = (DocFilesHandlerImpl)configuration
                    .getWriterFactory().getDocFilesHandler(element);
            localStylesheets = docFilesHandler.getStylesheets();
            configuration.localStylesheetMap.put(element, localStylesheets);
        }
        return localStylesheets;
    }

    public void addPreviewSummary(Element forWhat, Content target) {
        if (utils.isPreviewAPI(forWhat)) {
            Content div = HtmlTree.DIV(HtmlStyle.block);
            div.add(HtmlTree.SPAN(HtmlStyle.previewLabel, contents.previewPhrase));
            target.add(div);
        }
    }

    public void addPreviewInfo(Element forWhat, Content target) {
        if (utils.isPreviewAPI(forWhat)) {
            //in Java platform:
            HtmlTree previewDiv = HtmlTree.DIV(HtmlStyle.previewBlock);
            previewDiv.setId(htmlIds.forPreviewSection(forWhat));
            String name = (switch (forWhat.getKind()) {
                case PACKAGE, MODULE ->
                        ((QualifiedNameable) forWhat).getQualifiedName();
                case CONSTRUCTOR ->
                        ((TypeElement) forWhat.getEnclosingElement()).getSimpleName();
                default -> forWhat.getSimpleName();
            }).toString();
            Content nameCode = HtmlTree.CODE(Text.of(name));
            boolean isReflectivePreview = utils.isReflectivePreviewAPI(forWhat);
            String leadingNoteKey =
                    !isReflectivePreview ? "doclet.PreviewPlatformLeadingNote"
                                         : "doclet.ReflectivePreviewPlatformLeadingNote";
            Content leadingNote =
                    contents.getContent(leadingNoteKey, nameCode);
            previewDiv.add(HtmlTree.SPAN(HtmlStyle.previewLabel,
                                         leadingNote));
            if (!isReflectivePreview) {
                Content note1 = contents.getContent("doclet.PreviewTrailingNote1", nameCode);
                previewDiv.add(HtmlTree.DIV(HtmlStyle.previewComment, note1));
            }
            Content note2 = contents.getContent("doclet.PreviewTrailingNote2", nameCode);
            previewDiv.add(HtmlTree.DIV(HtmlStyle.previewComment, note2));
            target.add(previewDiv);
        } else if (forWhat.getKind().isClass() || forWhat.getKind().isInterface()) {
            //in custom code:
            List<Content> previewNotes = getPreviewNotes((TypeElement) forWhat);
            if (!previewNotes.isEmpty()) {
                Name name = forWhat.getSimpleName();
                Content nameCode = HtmlTree.CODE(Text.of(name));
                HtmlTree previewDiv = HtmlTree.DIV(HtmlStyle.previewBlock);
                previewDiv.setId(htmlIds.forPreviewSection(forWhat));
                Content leadingNote = contents.getContent("doclet.PreviewLeadingNote", nameCode);
                previewDiv.add(HtmlTree.SPAN(HtmlStyle.previewLabel,
                                             leadingNote));
                HtmlTree ul = new HtmlTree(TagName.UL);
                ul.setStyle(HtmlStyle.previewComment);
                for (Content note : previewNotes) {
                    ul.add(HtmlTree.LI(note));
                }
                previewDiv.add(ul);
                Content note1 =
                        contents.getContent("doclet.PreviewTrailingNote1",
                                            nameCode);
                previewDiv.add(HtmlTree.DIV(HtmlStyle.previewComment, note1));
                Content note2 =
                        contents.getContent("doclet.PreviewTrailingNote2",
                                            name);
                previewDiv.add(HtmlTree.DIV(HtmlStyle.previewComment, note2));
                target.add(previewDiv);
            }
        }
    }

    private List<Content> getPreviewNotes(TypeElement el) {
        String className = el.getSimpleName().toString();
        List<Content> result = new ArrayList<>();
        PreviewSummary previewAPITypes = utils.declaredUsingPreviewAPIs(el);
        Set<TypeElement> previewAPI = new HashSet<>(previewAPITypes.previewAPI);
        Set<TypeElement> reflectivePreviewAPI = new HashSet<>(previewAPITypes.reflectivePreviewAPI);
        Set<TypeElement> declaredUsingPreviewFeature = new HashSet<>(previewAPITypes.declaredUsingPreviewFeature);
        Set<DeclarationPreviewLanguageFeatures> previewLanguageFeatures = new HashSet<>();
        for (Element enclosed : el.getEnclosedElements()) {
            if (!utils.isIncluded(enclosed)) {
                continue;
            }
            if (!enclosed.getKind().isClass() && !enclosed.getKind().isInterface()) {
                PreviewSummary memberAPITypes = utils.declaredUsingPreviewAPIs(enclosed);
                declaredUsingPreviewFeature.addAll(memberAPITypes.declaredUsingPreviewFeature);
                previewAPI.addAll(memberAPITypes.previewAPI);
                reflectivePreviewAPI.addAll(memberAPITypes.reflectivePreviewAPI);
                previewLanguageFeatures.addAll(utils.previewLanguageFeaturesUsed(enclosed));
            } else if (!utils.previewLanguageFeaturesUsed(enclosed).isEmpty()) {
                declaredUsingPreviewFeature.add((TypeElement) enclosed);
            }
        }
        previewLanguageFeatures.addAll(utils.previewLanguageFeaturesUsed(el));
        if (!previewLanguageFeatures.isEmpty()) {
            for (DeclarationPreviewLanguageFeatures feature : previewLanguageFeatures) {
                String featureDisplayName =
                        resources.getText("doclet.Declared_Using_Preview." + feature.name());
                result.add(withPreviewFeatures("doclet.Declared_Using_Preview", className,
                                               featureDisplayName, feature.features));
            }
        }
        if (!declaredUsingPreviewFeature.isEmpty()) {
            result.add(withLinks("doclet.UsesDeclaredUsingPreview", className, declaredUsingPreviewFeature));
        }
        if (!previewAPI.isEmpty()) {
            result.add(withLinks("doclet.PreviewAPI", className, previewAPI));
        }
        if (!reflectivePreviewAPI.isEmpty()) {
            result.add(withLinks("doclet.ReflectivePreviewAPI", className, reflectivePreviewAPI));
        }
        return result;
    }

    private Content withPreviewFeatures(String key, String className, String featureName, List<String> features) {
        String[] sep = new String[] {""};
        ContentBuilder featureCodes = new ContentBuilder();
        features.stream()
                .forEach(c -> {
                    featureCodes.add(sep[0]);
                    featureCodes.add(HtmlTree.CODE(new ContentBuilder().add(c)));
                    sep[0] = ", ";
                });
        return contents.getContent(key,
                                   HtmlTree.CODE(Text.of(className)),
                                   new HtmlTree(TagName.EM).add(featureName),
                                   featureCodes);
    }

    private Content withLinks(String key, String className, Set<TypeElement> elements) {
        String[] sep = new String[] {""};
        ContentBuilder links = new ContentBuilder();
        elements.stream()
                .sorted((te1, te2) -> te1.getSimpleName().toString().compareTo(te2.getSimpleName().toString()))
                .distinct()
                .map(te -> getLink(new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.CLASS, te)
                        .label(HtmlTree.CODE(Text.of(te.getSimpleName()))).skipPreview(true)))
                .forEach(c -> {
                    links.add(sep[0]);
                    links.add(c);
                    sep[0] = ", ";
                });
        return contents.getContent(key,
                                   HtmlTree.CODE(Text.of(className)),
                                   links);
    }

}
