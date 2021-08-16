/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.shellsupport.doc;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.InheritDocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.Pair;

/**Helper to find javadoc and resolve @inheritDoc.
 */
public abstract class JavadocHelper implements AutoCloseable {
    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    /**Create the helper.
     *
     * @param mainTask JavacTask from which the further Elements originate
     * @param sourceLocations paths where source files should be searched
     * @return a JavadocHelper
     */
    public static JavadocHelper create(JavacTask mainTask, Collection<? extends Path> sourceLocations) {
        StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null);
        try {
            fm.setLocationFromPaths(StandardLocation.SOURCE_PATH, sourceLocations);
            return new OnDemandJavadocHelper(mainTask, fm);
        } catch (IOException ex) {
            try {
                fm.close();
            } catch (IOException closeEx) {
            }
            return new JavadocHelper() {
                @Override
                public String getResolvedDocComment(Element forElement) throws IOException {
                    return null;
                }
                @Override
                public Element getSourceElement(Element forElement) throws IOException {
                    return forElement;
                }
                @Override
                public void close() throws IOException {}
            };
        }
    }

    /**Returns javadoc for the given element, if it can be found, or null otherwise. The javadoc
     * will have @inheritDoc resolved.
     *
     * @param forElement element for which the javadoc should be searched
     * @return javadoc if found, null otherwise
     * @throws IOException if something goes wrong in the search
     */
    public abstract String getResolvedDocComment(Element forElement) throws IOException;

    /**Returns an element representing the same given program element, but the returned element will
     * be resolved from source, if it can be found. Returns the original element if the source for
     * the given element cannot be found.
     *
     * @param forElement element for which the source element should be searched
     * @return source element if found, the original element otherwise
     * @throws IOException if something goes wrong in the search
     */
    public abstract Element getSourceElement(Element forElement) throws IOException;

    /**Closes the helper.
     *
     * @throws IOException if something foes wrong during the close
     */
    @Override
    public abstract void close() throws IOException;

    private static final class OnDemandJavadocHelper extends JavadocHelper {
        private final JavacTask mainTask;
        private final JavaFileManager baseFileManager;
        private final StandardJavaFileManager fm;
        private final Map<String, Pair<JavacTask, TreePath>> signature2Source = new HashMap<>();

        private OnDemandJavadocHelper(JavacTask mainTask, StandardJavaFileManager fm) {
            this.mainTask = mainTask;
            this.baseFileManager = ((JavacTaskImpl) mainTask).getContext().get(JavaFileManager.class);
            this.fm = fm;
        }

        @Override
        public String getResolvedDocComment(Element forElement) throws IOException {
            Pair<JavacTask, TreePath> sourceElement = getSourceElement(mainTask, forElement);

            if (sourceElement == null)
                return null;

            return getResolvedDocComment(sourceElement.fst, sourceElement.snd);
        }

        @Override
        public Element getSourceElement(Element forElement) throws IOException {
            Pair<JavacTask, TreePath> sourceElement = getSourceElement(mainTask, forElement);

            if (sourceElement == null)
                return forElement;

            Element result = Trees.instance(sourceElement.fst).getElement(sourceElement.snd);

            if (result == null)
                return forElement;

            return result;
        }

        private String getResolvedDocComment(JavacTask task, TreePath el) throws IOException {
            DocTrees trees = DocTrees.instance(task);
            Element element = trees.getElement(el);
            String docComment = trees.getDocComment(el);

            if (docComment == null && element.getKind() == ElementKind.METHOD) {
                //if a method does not have a javadoc,
                //try to use javadoc from the methods overridden by this method:
                ExecutableElement executableElement = (ExecutableElement) element;
                Iterable<Element> superTypes =
                        () -> superTypeForInheritDoc(task, element.getEnclosingElement()).iterator();
                for (Element sup : superTypes) {
                   for (ExecutableElement supMethod : ElementFilter.methodsIn(sup.getEnclosedElements())) {
                       TypeElement clazz = (TypeElement) executableElement.getEnclosingElement();
                       if (task.getElements().overrides(executableElement, supMethod, clazz)) {
                           Pair<JavacTask, TreePath> source = getSourceElement(task, supMethod);

                           if (source != null) {
                               String overriddenComment = getResolvedDocComment(source.fst, source.snd);

                               if (overriddenComment != null) {
                                   return overriddenComment;
                               }
                           }
                       }
                   }
                }
            }

            if (docComment == null)
                return null;

            Pair<DocCommentTree, Integer> parsed = parseDocComment(task, docComment);
            DocCommentTree docCommentTree = parsed.fst;
            int offset = parsed.snd;
            IOException[] exception = new IOException[1];
            Comparator<int[]> spanComp =
                    (span1, span2) -> span1[0] != span2[0] ? span2[0] - span1[0]
                                                           : span2[1] - span1[0];
            //spans in the docComment that should be replaced with the given Strings:
            Map<int[], List<String>> replace = new TreeMap<>(spanComp);
            DocSourcePositions sp = trees.getSourcePositions();

            //fill in missing elements and resolve {@inheritDoc}
            //if an element is (silently) missing in the javadoc, a synthetic {@inheritDoc}
            //is created for it.
            new DocTreeScanner<Void, Void>() {
                /* enclosing doctree that may contain {@inheritDoc} (explicit or synthetic)*/
                private Stack<DocTree> interestingParent = new Stack<>();
                /* current top-level DocCommentTree*/
                private DocCommentTree dcTree;
                /* javadoc from a super method from which we may copy elements.*/
                private String inherited;
                /* JavacTask from which inherited originates.*/
                private JavacTask inheritedJavacTask;
                /* TreePath to the super method from which inherited originates.*/
                private TreePath inheritedTreePath;
                /* Synthetic trees that contain {@inheritDoc} and
                 * texts which which they should be replaced.*/
                private Map<DocTree, String> syntheticTrees = new IdentityHashMap<>();
                /* Position on which the synthetic trees should be inserted.*/
                private long insertPos = offset;
                @Override @DefinedBy(Api.COMPILER_TREE)
                public Void visitDocComment(DocCommentTree node, Void p) {
                    dcTree = node;
                    interestingParent.push(node);
                    try {
                        if (node.getFullBody().isEmpty()) {
                            //there is no body in the javadoc, add synthetic {@inheritDoc}, which
                            //will be automatically filled in visitInheritDoc:
                            DocCommentTree dc = parseDocComment(task, "{@inheritDoc}").fst;
                            syntheticTrees.put(dc, "*\n");
                            interestingParent.push(dc);
                            boolean prevInSynthetic = inSynthetic;
                            try {
                                inSynthetic = true;
                                scan(dc.getFirstSentence(), p);
                                scan(dc.getBody(), p);
                            } finally {
                                inSynthetic = prevInSynthetic;
                                interestingParent.pop();
                            }
                        } else {
                            scan(node.getFirstSentence(), p);
                            scan(node.getBody(), p);
                        }
                        //add missing @param, @throws and @return, augmented with {@inheritDoc}
                        //which will be resolved in visitInheritDoc:
                        List<DocTree> augmentedBlockTags = new ArrayList<>(node.getBlockTags());
                        if (element.getKind() == ElementKind.METHOD) {
                            ExecutableElement executableElement = (ExecutableElement) element;
                            List<String> parameters =
                                    executableElement.getParameters()
                                                     .stream()
                                                     .map(param -> param.getSimpleName().toString())
                                                     .toList();
                            List<String> throwsList =
                                    executableElement.getThrownTypes()
                                                     .stream()
                                                     .map(TypeMirror::toString)
                                                     .toList();
                            Set<String> missingParams = new HashSet<>(parameters);
                            Set<String> missingThrows = new HashSet<>(throwsList);
                            boolean hasReturn = false;

                            for (DocTree dt : augmentedBlockTags) {
                                switch (dt.getKind()) {
                                    case PARAM:
                                        missingParams.remove(((ParamTree) dt).getName().getName().toString());
                                        break;
                                    case THROWS:
                                        missingThrows.remove(getThrownException(task, el, docCommentTree, (ThrowsTree) dt));
                                        break;
                                    case RETURN:
                                        hasReturn = true;
                                        break;
                                }
                            }

                            for (String missingParam : missingParams) {
                                DocTree syntheticTag = parseBlockTag(task, "@param " + missingParam + " {@inheritDoc}");
                                syntheticTrees.put(syntheticTag, "@param " + missingParam + " *\n");
                                insertTag(augmentedBlockTags, syntheticTag, parameters, throwsList);
                            }

                            for (String missingThrow : missingThrows) {
                                DocTree syntheticTag = parseBlockTag(task, "@throws " + missingThrow + " {@inheritDoc}");
                                syntheticTrees.put(syntheticTag, "@throws " + missingThrow + " *\n");
                                insertTag(augmentedBlockTags, syntheticTag, parameters, throwsList);
                            }

                            if (!hasReturn) {
                                DocTree syntheticTag = parseBlockTag(task, "@return {@inheritDoc}");
                                syntheticTrees.put(syntheticTag, "@return *\n");
                                insertTag(augmentedBlockTags, syntheticTag, parameters, throwsList);
                            }
                        }
                        scan(augmentedBlockTags, p);
                        return null;
                    } finally {
                        interestingParent.pop();
                    }
                }
                @Override @DefinedBy(Api.COMPILER_TREE)
                public Void visitParam(ParamTree node, Void p) {
                    interestingParent.push(node);
                    try {
                        return super.visitParam(node, p);
                    } finally {
                        interestingParent.pop();
                    }
                }
                @Override @DefinedBy(Api.COMPILER_TREE)
                public Void visitThrows(ThrowsTree node, Void p) {
                    interestingParent.push(node);
                    try {
                        return super.visitThrows(node, p);
                    } finally {
                        interestingParent.pop();
                    }
                }
                @Override @DefinedBy(Api.COMPILER_TREE)
                public Void visitReturn(ReturnTree node, Void p) {
                    interestingParent.push(node);
                    try {
                        return super.visitReturn(node, p);
                    } finally {
                        interestingParent.pop();
                    }
                }
                @Override @DefinedBy(Api.COMPILER_TREE)
                public Void visitInheritDoc(InheritDocTree node, Void p) {
                    //replace (schedule replacement into the replace map)
                    //{@inheritDoc} with the corresponding text from an overridden method

                    //first, fill in inherited, inheritedJavacTask and inheritedTreePath if not
                    //done yet:
                    if (inherited == null) {
                        try {
                            if (element.getKind() == ElementKind.METHOD) {
                                ExecutableElement executableElement = (ExecutableElement) element;
                                Iterable<ExecutableElement> superMethods =
                                        () -> superMethodsForInheritDoc(task, executableElement).
                                              iterator();
                                for (Element supMethod : superMethods) {
                                   Pair<JavacTask, TreePath> source =
                                           getSourceElement(task, supMethod);

                                   if (source != null) {
                                       String overriddenComment =
                                               getResolvedDocComment(source.fst,
                                                                     source.snd);

                                       if (overriddenComment != null) {
                                           inheritedJavacTask = source.fst;
                                           inheritedTreePath = source.snd;
                                           inherited = overriddenComment;
                                           break;
                                       }
                                   }
                                }
                            }
                        } catch (IOException ex) {
                            exception[0] = ex;
                            return null;
                        }
                    }
                    if (inherited == null) {
                        return null;
                    }
                    Pair<DocCommentTree, Integer> parsed =
                            parseDocComment(inheritedJavacTask, inherited);
                    DocCommentTree inheritedDocTree = parsed.fst;
                    int offset = parsed.snd;
                    List<List<? extends DocTree>> inheritedText = new ArrayList<>();
                    //find the corresponding piece in the inherited javadoc
                    //(interesting parent keeps the enclosing tree):
                    DocTree parent = interestingParent.peek();
                    switch (parent.getKind()) {
                        case DOC_COMMENT:
                            inheritedText.add(inheritedDocTree.getFullBody());
                            break;
                        case PARAM:
                            String paramName = ((ParamTree) parent).getName().getName().toString();
                            new DocTreeScanner<Void, Void>() {
                                @Override @DefinedBy(Api.COMPILER_TREE)
                                public Void visitParam(ParamTree node, Void p) {
                                    if (node.getName().getName().contentEquals(paramName)) {
                                        inheritedText.add(node.getDescription());
                                    }
                                    return super.visitParam(node, p);
                                }
                            }.scan(inheritedDocTree, null);
                            break;
                        case THROWS:
                            String thrownName = getThrownException(task, el, docCommentTree, (ThrowsTree) parent);
                            new DocTreeScanner<Void, Void>() {
                                @Override @DefinedBy(Api.COMPILER_TREE)
                                public Void visitThrows(ThrowsTree node, Void p) {
                                    if (Objects.equals(getThrownException(inheritedJavacTask, inheritedTreePath, inheritedDocTree, node), thrownName)) {
                                        inheritedText.add(node.getDescription());
                                    }
                                    return super.visitThrows(node, p);
                                }
                            }.scan(inheritedDocTree, null);
                            break;
                        case RETURN:
                            new DocTreeScanner<Void, Void>() {
                                @Override @DefinedBy(Api.COMPILER_TREE)
                                public Void visitReturn(ReturnTree node, Void p) {
                                    inheritedText.add(node.getDescription());
                                    return super.visitReturn(node, p);
                                }
                            }.scan(inheritedDocTree, null);
                            break;
                    }
                    if (!inheritedText.isEmpty()) {
                        long start = Long.MAX_VALUE;
                        long end = Long.MIN_VALUE;

                        for (DocTree t : inheritedText.get(0)) {
                            start = Math.min(start,
                                             sp.getStartPosition(null, inheritedDocTree, t) - offset);
                            end   = Math.max(end,
                                             sp.getEndPosition(null, inheritedDocTree, t) - offset);
                        }
                        String text = end >= 0 ? inherited.substring((int) start, (int) end) : "";

                        if (syntheticTrees.containsKey(parent)) {
                            //if the {@inheritDoc} is inside a synthetic tree, don't delete anything,
                            //but insert the required text
                            //(insertPos is the position at which new stuff should be added):
                            int[] span = new int[] {(int) insertPos, (int) insertPos};
                            replace.computeIfAbsent(span, s -> new ArrayList<>())
                                    .add(syntheticTrees.get(parent).replace("*", text));
                        } else {
                            //replace the {@inheritDoc} with the full text from
                            //the overridden method:
                            long inheritedStart = sp.getStartPosition(null, dcTree, node);
                            long inheritedEnd   = sp.getEndPosition(null, dcTree, node);
                            int[] span = new int[] {(int) inheritedStart, (int) inheritedEnd};

                            replace.computeIfAbsent(span, s -> new ArrayList<>())
                                    .add(text);
                        }
                    }
                    return super.visitInheritDoc(node, p);
                }
                private boolean inSynthetic;
                @Override @DefinedBy(Api.COMPILER_TREE)
                public Void scan(DocTree tree, Void p) {
                    if (exception[0] != null) {
                        return null;
                    }
                    boolean prevInSynthetic = inSynthetic;
                    try {
                        inSynthetic |= syntheticTrees.containsKey(tree);
                        return super.scan(tree, p);
                    } finally {
                        if (!inSynthetic && tree != null) {
                            //for nonsynthetic trees, preserve the ending position as the future
                            //insertPos (as future missing elements should be inserted behind
                            //this tree)
                            //if there is a newline immediately behind this tree, insert behind
                            //the newline:
                            long endPos = sp.getEndPosition(null, dcTree, tree);
                            if (endPos >= 0) {
                                if (endPos - offset + 1 < docComment.length() &&
                                    docComment.charAt((int) (endPos - offset + 1)) == '\n') {
                                    endPos++;
                                }
                                if (endPos - offset < docComment.length()) {
                                    insertPos = endPos + 1;
                                } else {
                                    insertPos = endPos;
                                }
                            }
                        }
                        inSynthetic = prevInSynthetic;
                    }
                }

                /* Insert a synthetic tag (toInsert) into the list of tags at
                 * an appropriate position.*/
                private void insertTag(List<DocTree> tags, DocTree toInsert, List<String> parameters, List<String> throwsTypes) {
                    Comparator<DocTree> comp = (tag1, tag2) -> {
                        if (tag1.getKind() == tag2.getKind()) {
                            switch (toInsert.getKind()) {
                                case PARAM: {
                                    ParamTree p1 = (ParamTree) tag1;
                                    ParamTree p2 = (ParamTree) tag2;
                                    int i1 = parameters.indexOf(p1.getName().getName().toString());
                                    int i2 = parameters.indexOf(p2.getName().getName().toString());

                                    return i1 - i2;
                                }
                                case THROWS: {
                                    ThrowsTree t1 = (ThrowsTree) tag1;
                                    ThrowsTree t2 = (ThrowsTree) tag2;
                                    int i1 = throwsTypes.indexOf(getThrownException(task, el, docCommentTree, t1));
                                    int i2 = throwsTypes.indexOf(getThrownException(task, el, docCommentTree, t2));

                                    return i1 - i2;
                                }
                            }
                        }

                        int i1 = tagOrder.indexOf(tag1.getKind());
                        int i2 = tagOrder.indexOf(tag2.getKind());

                        return i1 - i2;
                    };

                    for (int i = 0; i < tags.size(); i++) {
                        if (comp.compare(tags.get(i), toInsert) >= 0) {
                            tags.add(i, toInsert);
                            return ;
                        }
                    }
                    tags.add(toInsert);
                }

                private final List<DocTree.Kind> tagOrder = Arrays.asList(DocTree.Kind.PARAM, DocTree.Kind.THROWS, DocTree.Kind.RETURN);
            }.scan(docCommentTree, null);

            if (replace.isEmpty())
                return docComment;

            //do actually replace {@inheritDoc} with the new text (as scheduled by the visitor
            //above):
            StringBuilder replacedInheritDoc = new StringBuilder(docComment);

            for (Entry<int[], List<String>> e : replace.entrySet()) {
                replacedInheritDoc.delete(e.getKey()[0] - offset, e.getKey()[1] - offset);
                replacedInheritDoc.insert(e.getKey()[0] - offset,
                                          e.getValue().stream().collect(Collectors.joining("")));
            }

            return replacedInheritDoc.toString();
        }

        /* Find methods from which the given method may inherit javadoc, in the proper order.*/
        private Stream<ExecutableElement> superMethodsForInheritDoc(JavacTask task,
                                                                     ExecutableElement method) {
            TypeElement type = (TypeElement) method.getEnclosingElement();

            return this.superTypeForInheritDoc(task, type)
                       .flatMap(sup -> ElementFilter.methodsIn(sup.getEnclosedElements()).stream())
                       .filter(supMethod -> task.getElements().overrides(method, supMethod, type));
        }

        /* Find types from which methods in type may inherit javadoc, in the proper order.*/
        private Stream<Element> superTypeForInheritDoc(JavacTask task, Element type) {
            TypeElement clazz = (TypeElement) type;
            Stream<Element> result = interfaces(clazz);
            result = Stream.concat(result, interfaces(clazz).flatMap(el -> superTypeForInheritDoc(task, el)));

            if (clazz.getSuperclass().getKind() == TypeKind.DECLARED) {
                Element superClass = ((DeclaredType) clazz.getSuperclass()).asElement();
                result = Stream.concat(result, Stream.of(superClass));
                result = Stream.concat(result, superTypeForInheritDoc(task, superClass));
            }

            return result;
        }
        //where:
            private Stream<Element> interfaces(TypeElement clazz) {
                return clazz.getInterfaces()
                            .stream()
                            .filter(tm -> tm.getKind() == TypeKind.DECLARED)
                            .map(tm -> ((DeclaredType) tm).asElement());
            }

         private DocTree parseBlockTag(JavacTask task, String blockTag) {
            DocCommentTree dc = parseDocComment(task, blockTag).fst;

            return dc.getBlockTags().get(0);
        }

        private Pair<DocCommentTree, Integer> parseDocComment(JavacTask task, String javadoc) {
            DocTrees trees = DocTrees.instance(task);
            try {
                SimpleJavaFileObject fo =
                        new SimpleJavaFileObject(new URI("mem://doc.html"), Kind.HTML) {
                    @Override @DefinedBy(Api.COMPILER)
                    public CharSequence getCharContent(boolean ignoreEncodingErrors)
                            throws IOException {
                        return "<body>" + javadoc + "</body>";
                    }
                };
                DocCommentTree tree = trees.getDocCommentTree(fo);
                int offset = (int) trees.getSourcePositions().getStartPosition(null, tree, tree);
                offset += "<body>".length();
                return Pair.of(tree, offset);
            } catch (URISyntaxException ex) {
                throw new IllegalStateException(ex);
            }
        }

        private String getThrownException(JavacTask task, TreePath rootOn, DocCommentTree comment, ThrowsTree tt) {
            DocTrees trees = DocTrees.instance(task);
            Element exc = trees.getElement(new DocTreePath(new DocTreePath(rootOn, comment), tt.getExceptionName()));
            return exc != null ? exc.toString() : null;
        }

        private Pair<JavacTask, TreePath> getSourceElement(JavacTask origin, Element el) throws IOException {
            String handle = elementSignature(el);
            Pair<JavacTask, TreePath> cached = signature2Source.get(handle);

            if (cached != null) {
                return cached.fst != null ? cached : null;
            }

            TypeElement type = topLevelType(el);

            if (type == null)
                return null;

            Elements elements = origin.getElements();
            String binaryName = elements.getBinaryName(type).toString();
            ModuleElement module = elements.getModuleOf(type);
            String moduleName = module == null || module.isUnnamed()
                    ? null
                    : module.getQualifiedName().toString();
            Pair<JavacTask, CompilationUnitTree> source = findSource(moduleName, binaryName);

            if (source == null)
                return null;

            fillElementCache(source.fst, source.snd);

            cached = signature2Source.get(handle);

            if (cached != null) {
                return cached;
            } else {
                signature2Source.put(handle, Pair.of(null, null));
                return null;
            }
        }
        //where:
            private String elementSignature(Element el) {
                switch (el.getKind()) {
                    case ANNOTATION_TYPE: case CLASS: case ENUM: case INTERFACE:
                        return ((TypeElement) el).getQualifiedName().toString();
                    case FIELD:
                        return elementSignature(el.getEnclosingElement()) + "." + el.getSimpleName() + ":" + el.asType();
                    case ENUM_CONSTANT:
                        return elementSignature(el.getEnclosingElement()) + "." + el.getSimpleName();
                    case EXCEPTION_PARAMETER: case LOCAL_VARIABLE: case PARAMETER: case RESOURCE_VARIABLE:
                        return el.getSimpleName() + ":" + el.asType();
                    case CONSTRUCTOR: case METHOD:
                        StringBuilder header = new StringBuilder();
                        header.append(elementSignature(el.getEnclosingElement()));
                        if (el.getKind() == ElementKind.METHOD) {
                            header.append(".");
                            header.append(el.getSimpleName());
                        }
                        header.append("(");
                        String sep = "";
                        ExecutableElement method = (ExecutableElement) el;
                        for (Iterator<? extends VariableElement> i = method.getParameters().iterator(); i.hasNext();) {
                            VariableElement p = i.next();
                            header.append(sep);
                            header.append(p.asType());
                            sep = ", ";
                        }
                        header.append(")");
                        return header.toString();
                   default:
                        return el.toString();
                }
            }

            private TypeElement topLevelType(Element el) {
                if (el.getKind() == ElementKind.PACKAGE)
                    return null;

                while (el != null && el.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
                    el = el.getEnclosingElement();
                }

                return el != null && (el.getKind().isClass() || el.getKind().isInterface()) ? (TypeElement) el : null;
            }

            private void fillElementCache(JavacTask task, CompilationUnitTree cut) throws IOException {
                Trees trees = Trees.instance(task);

                new TreePathScanner<Void, Void>() {
                    @Override @DefinedBy(Api.COMPILER_TREE)
                    public Void visitMethod(MethodTree node, Void p) {
                        handleDeclaration();
                        return null;
                    }

                    @Override @DefinedBy(Api.COMPILER_TREE)
                    public Void visitClass(ClassTree node, Void p) {
                        handleDeclaration();
                        return super.visitClass(node, p);
                    }

                    @Override @DefinedBy(Api.COMPILER_TREE)
                    public Void visitVariable(VariableTree node, Void p) {
                        handleDeclaration();
                        return super.visitVariable(node, p);
                    }

                    private void handleDeclaration() {
                        Element currentElement = trees.getElement(getCurrentPath());

                        if (currentElement != null) {
                            signature2Source.put(elementSignature(currentElement), Pair.of(task, getCurrentPath()));
                        }
                    }
                }.scan(cut, null);
            }

        private Pair<JavacTask, CompilationUnitTree> findSource(String moduleName,
                                                                String binaryName) throws IOException {
            JavaFileObject jfo = fm.getJavaFileForInput(StandardLocation.SOURCE_PATH,
                                                        binaryName,
                                                        JavaFileObject.Kind.SOURCE);

            if (jfo == null)
                return null;

            List<JavaFileObject> jfos = Arrays.asList(jfo);
            JavaFileManager patchFM = moduleName != null
                    ? new PatchModuleFileManager(baseFileManager, jfo, moduleName)
                    : baseFileManager;
            JavacTaskImpl task = (JavacTaskImpl) compiler.getTask(null, patchFM, d -> {}, null, null, jfos);
            Iterable<? extends CompilationUnitTree> cuts = task.parse();

            task.enter();

            return Pair.of(task, cuts.iterator().next());
        }

        @Override
        public void close() throws IOException {
            fm.close();
        }

        private static final class PatchModuleFileManager
                extends ForwardingJavaFileManager<JavaFileManager> {

            private final JavaFileObject file;
            private final String moduleName;

            public PatchModuleFileManager(JavaFileManager fileManager,
                                          JavaFileObject file,
                                          String moduleName) {
                super(fileManager);
                this.file = file;
                this.moduleName = moduleName;
            }

            @Override @DefinedBy(Api.COMPILER)
            public Location getLocationForModule(Location location,
                                                 JavaFileObject fo) throws IOException {
                return fo == file
                        ? PATCH_LOCATION
                        : super.getLocationForModule(location, fo);
            }

            @Override @DefinedBy(Api.COMPILER)
            public String inferModuleName(Location location) throws IOException {
                return location == PATCH_LOCATION
                        ? moduleName
                        : super.inferModuleName(location);
            }

            @Override @DefinedBy(Api.COMPILER)
            public boolean hasLocation(Location location) {
                return location == StandardLocation.PATCH_MODULE_PATH ||
                       super.hasLocation(location);
            }

            private static final Location PATCH_LOCATION = new Location() {
                @Override @DefinedBy(Api.COMPILER)
                public String getName() {
                    return "PATCH_LOCATION";
                }

                @Override @DefinedBy(Api.COMPILER)
                public boolean isOutputLocation() {
                    return false;
                }

                @Override @DefinedBy(Api.COMPILER)
                public boolean isModuleOrientedLocation() {
                    return false;
                }

            };
        }
    }

}
