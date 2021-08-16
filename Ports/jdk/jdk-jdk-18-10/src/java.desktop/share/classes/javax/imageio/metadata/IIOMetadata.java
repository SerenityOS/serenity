/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio.metadata;

import org.w3c.dom.Node;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * An abstract class to be extended by objects that represent metadata
 * (non-image data) associated with images and streams.  Plug-ins
 * represent metadata using opaque, plug-in specific objects.  These
 * objects, however, provide the ability to access their internal
 * information as a tree of {@code IIOMetadataNode} objects that
 * support the XML DOM interfaces as well as additional interfaces for
 * storing non-textual data and retrieving information about legal
 * data values.  The format of such trees is plug-in dependent, but
 * plug-ins may choose to support a plug-in neutral format described
 * below.  A single plug-in may support multiple metadata formats,
 * whose names maybe determined by calling
 * {@code getMetadataFormatNames}.  The plug-in may also support
 * a single special format, referred to as the "native" format, which
 * is designed to encode its metadata losslessly.  This format will
 * typically be designed specifically to work with a specific file
 * format, so that images may be loaded and saved in the same format
 * with no loss of metadata, but may be less useful for transferring
 * metadata between an {@code ImageReader} and an
 * {@code ImageWriter} for different image formats.  To convert
 * between two native formats as losslessly as the image file formats
 * will allow, an {@code ImageTranscoder} object must be used.
 *
 * @see javax.imageio.ImageReader#getImageMetadata
 * @see javax.imageio.ImageReader#getStreamMetadata
 * @see javax.imageio.ImageReader#readAll
 * @see javax.imageio.ImageWriter#getDefaultStreamMetadata
 * @see javax.imageio.ImageWriter#getDefaultImageMetadata
 * @see javax.imageio.ImageWriter#write
 * @see javax.imageio.ImageWriter#convertImageMetadata
 * @see javax.imageio.ImageWriter#convertStreamMetadata
 * @see javax.imageio.IIOImage
 * @see javax.imageio.ImageTranscoder
 *
 */
public abstract class IIOMetadata {

    /**
     * A boolean indicating whether the concrete subclass supports the
     * standard metadata format, set via the constructor.
     */
    protected boolean standardFormatSupported;

    /**
     * The name of the native metadata format for this object,
     * initialized to {@code null} and set via the constructor.
     */
    protected String nativeMetadataFormatName = null;

    /**
     * The name of the class implementing {@code IIOMetadataFormat}
     * and representing the native metadata format, initialized to
     * {@code null} and set via the constructor.
     */
    protected String nativeMetadataFormatClassName = null;

    /**
     * An array of names of formats, other than the standard and
     * native formats, that are supported by this plug-in,
     * initialized to {@code null} and set via the constructor.
     */
    protected String[] extraMetadataFormatNames = null;

    /**
     * An array of names of classes implementing {@code IIOMetadataFormat}
     * and representing the metadata formats, other than the standard and
     * native formats, that are supported by this plug-in,
     * initialized to {@code null} and set via the constructor.
     */
    protected String[] extraMetadataFormatClassNames = null;

    /**
     * An {@code IIOMetadataController} that is suggested for use
     * as the controller for this {@code IIOMetadata} object.  It
     * may be retrieved via {@code getDefaultController}.  To
     * install the default controller, call
     * {@code setController(getDefaultController())}.  This
     * instance variable should be set by subclasses that choose to
     * provide their own default controller, usually a GUI, for
     * setting parameters.
     *
     * @see IIOMetadataController
     * @see #getDefaultController
     */
    protected IIOMetadataController defaultController = null;

    /**
     * The {@code IIOMetadataController} that will be
     * used to provide settings for this {@code IIOMetadata}
     * object when the {@code activateController} method
     * is called.  This value overrides any default controller,
     * even when {@code null}.
     *
     * @see IIOMetadataController
     * @see #setController(IIOMetadataController)
     * @see #hasController()
     * @see #activateController()
     */
    protected IIOMetadataController controller = null;

    /**
     * Constructs an empty {@code IIOMetadata} object.  The
     * subclass is responsible for supplying values for all protected
     * instance variables that will allow any non-overridden default
     * implementations of methods to satisfy their contracts.  For example,
     * {@code extraMetadataFormatNames} should not have length 0.
     */
    protected IIOMetadata() {}

    /**
     * Constructs an {@code IIOMetadata} object with the given
     * format names and format class names, as well as a boolean
     * indicating whether the standard format is supported.
     *
     * <p> This constructor does not attempt to check the class names
     * for validity.  Invalid class names may cause exceptions in
     * subsequent calls to {@code getMetadataFormat}.
     *
     * @param standardMetadataFormatSupported {@code true} if
     * this object can return or accept a DOM tree using the standard
     * metadata format.
     * @param nativeMetadataFormatName the name of the native metadata
     * format, as a {@code String}, or {@code null} if there
     * is no native format.
     * @param nativeMetadataFormatClassName the name of the class of
     * the native metadata format, or {@code null} if there is
     * no native format.
     * @param extraMetadataFormatNames an array of {@code String}s
     * indicating additional formats supported by this object, or
     * {@code null} if there are none.
     * @param extraMetadataFormatClassNames an array of {@code String}s
     * indicating the class names of any additional formats supported by
     * this object, or {@code null} if there are none.
     *
     * @exception IllegalArgumentException if
     * {@code extraMetadataFormatNames} has length 0.
     * @exception IllegalArgumentException if
     * {@code extraMetadataFormatNames} and
     * {@code extraMetadataFormatClassNames} are neither both
     * {@code null}, nor of the same length.
     */
    protected IIOMetadata(boolean standardMetadataFormatSupported,
                          String nativeMetadataFormatName,
                          String nativeMetadataFormatClassName,
                          String[] extraMetadataFormatNames,
                          String[] extraMetadataFormatClassNames) {
        this.standardFormatSupported = standardMetadataFormatSupported;
        this.nativeMetadataFormatName = nativeMetadataFormatName;
        this.nativeMetadataFormatClassName = nativeMetadataFormatClassName;
        if (extraMetadataFormatNames != null) {
            if (extraMetadataFormatNames.length == 0) {
                throw new IllegalArgumentException
                    ("extraMetadataFormatNames.length == 0!");
            }
            if (extraMetadataFormatClassNames == null) {
                throw new IllegalArgumentException
                    ("extraMetadataFormatNames != null && extraMetadataFormatClassNames == null!");
            }
            if (extraMetadataFormatClassNames.length !=
                extraMetadataFormatNames.length) {
                throw new IllegalArgumentException
                    ("extraMetadataFormatClassNames.length != extraMetadataFormatNames.length!");
            }
            this.extraMetadataFormatNames = extraMetadataFormatNames.clone();
            this.extraMetadataFormatClassNames = extraMetadataFormatClassNames.clone();
        } else {
            if (extraMetadataFormatClassNames != null) {
                throw new IllegalArgumentException
                    ("extraMetadataFormatNames == null && extraMetadataFormatClassNames != null!");
            }
        }
    }

    /**
     * Returns {@code true} if the standard metadata format is
     * supported by {@code getMetadataFormat},
     * {@code getAsTree}, {@code setFromTree}, and
     * {@code mergeTree}.
     *
     * <p> The default implementation returns the value of the
     * {@code standardFormatSupported} instance variable.
     *
     * @return {@code true} if the standard metadata format
     * is supported.
     *
     * @see #getAsTree
     * @see #setFromTree
     * @see #mergeTree
     * @see #getMetadataFormat
     */
    public boolean isStandardMetadataFormatSupported() {
        return standardFormatSupported;
    }

    /**
     * Returns {@code true} if this object does not support the
     * {@code mergeTree}, {@code setFromTree}, and
     * {@code reset} methods.
     *
     * @return true if this {@code IIOMetadata} object cannot be
     * modified.
     */
    public abstract boolean isReadOnly();

    /**
     * Returns the name of the "native" metadata format for this
     * plug-in, which typically allows for lossless encoding and
     * transmission of the metadata stored in the format handled by
     * this plug-in.  If no such format is supported,
     * {@code null} will be returned.
     *
     * <p> The structure and contents of the "native" metadata format
     * are defined by the plug-in that created this
     * {@code IIOMetadata} object.  Plug-ins for simple formats
     * will usually create a dummy node for the root, and then a
     * series of child nodes representing individual tags, chunks, or
     * keyword/value pairs.  A plug-in may choose whether or not to
     * document its native format.
     *
     * <p> The default implementation returns the value of the
     * {@code nativeMetadataFormatName} instance variable.
     *
     * @return the name of the native format, or {@code null}.
     *
     * @see #getExtraMetadataFormatNames
     * @see #getMetadataFormatNames
     */
    public String getNativeMetadataFormatName() {
        return nativeMetadataFormatName;
    }

    /**
     * Returns an array of {@code String}s containing the names
     * of additional metadata formats, other than the native and standard
     * formats, recognized by this plug-in's
     * {@code getAsTree}, {@code setFromTree}, and
     * {@code mergeTree} methods.  If there are no such additional
     * formats, {@code null} is returned.
     *
     * <p> The default implementation returns a clone of the
     * {@code extraMetadataFormatNames} instance variable.
     *
     * @return an array of {@code String}s with length at least
     * 1, or {@code null}.
     *
     * @see #getAsTree
     * @see #setFromTree
     * @see #mergeTree
     * @see #getNativeMetadataFormatName
     * @see #getMetadataFormatNames
     */
    public String[] getExtraMetadataFormatNames() {
        if (extraMetadataFormatNames == null) {
            return null;
        }
        return extraMetadataFormatNames.clone();
    }

    /**
     * Returns an array of {@code String}s containing the names
     * of all metadata formats, including the native and standard
     * formats, recognized by this plug-in's {@code getAsTree},
     * {@code setFromTree}, and {@code mergeTree} methods.
     * If there are no such formats, {@code null} is returned.
     *
     * <p> The default implementation calls
     * {@code getNativeMetadataFormatName},
     * {@code isStandardMetadataFormatSupported}, and
     * {@code getExtraMetadataFormatNames} and returns the
     * combined results.
     *
     * @return an array of {@code String}s.
     *
     * @see #getNativeMetadataFormatName
     * @see #isStandardMetadataFormatSupported
     * @see #getExtraMetadataFormatNames
     */
    public String[] getMetadataFormatNames() {
        String nativeName = getNativeMetadataFormatName();
        String standardName = isStandardMetadataFormatSupported() ?
            IIOMetadataFormatImpl.standardMetadataFormatName : null;
        String[] extraNames = getExtraMetadataFormatNames();

        int numFormats = 0;
        if (nativeName != null) {
            ++numFormats;
        }
        if (standardName != null) {
            ++numFormats;
        }
        if (extraNames != null) {
            numFormats += extraNames.length;
        }
        if (numFormats == 0) {
            return null;
        }

        String[] formats = new String[numFormats];
        int index = 0;
        if (nativeName != null) {
            formats[index++] = nativeName;
        }
        if (standardName != null) {
            formats[index++] = standardName;
        }
        if (extraNames != null) {
            for (int i = 0; i < extraNames.length; i++) {
                formats[index++] = extraNames[i];
            }
        }

        return formats;
    }

    /**
     * Returns an {@code IIOMetadataFormat} object describing the
     * given metadata format, or {@code null} if no description
     * is available.  The supplied name must be one of those returned
     * by {@code getMetadataFormatNames} (<i>i.e.</i>, either the
     * native format name, the standard format name, or one of those
     * returned by {@code getExtraMetadataFormatNames}).
     *
     * <p> The default implementation checks the name against the
     * global standard metadata format name, and returns that format
     * if it is supported.  Otherwise, it checks against the native
     * format names followed by any additional format names.  If a
     * match is found, it retrieves the name of the
     * {@code IIOMetadataFormat} class from
     * {@code nativeMetadataFormatClassName} or
     * {@code extraMetadataFormatClassNames} as appropriate, and
     * constructs an instance of that class using its
     * {@code getInstance} method.
     *
     * @param formatName the desired metadata format.
     *
     * @return an {@code IIOMetadataFormat} object.
     *
     * @exception IllegalArgumentException if {@code formatName}
     * is {@code null} or is not one of the names recognized by
     * the plug-in.
     * @exception IllegalStateException if the class corresponding to
     * the format name cannot be loaded.
     */
    public IIOMetadataFormat getMetadataFormat(String formatName) {
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        if (standardFormatSupported
            && formatName.equals
                (IIOMetadataFormatImpl.standardMetadataFormatName)) {
            return IIOMetadataFormatImpl.getStandardFormatInstance();
        }
        String formatClassName = null;
        if (formatName.equals(nativeMetadataFormatName)) {
            formatClassName = nativeMetadataFormatClassName;
        } else if (extraMetadataFormatNames != null) {
            for (int i = 0; i < extraMetadataFormatNames.length; i++) {
                if (formatName.equals(extraMetadataFormatNames[i])) {
                    formatClassName = extraMetadataFormatClassNames[i];
                    break;  // out of for
                }
            }
        }
        if (formatClassName == null) {
            throw new IllegalArgumentException("Unsupported format name");
        }
        try {
            final String className = formatClassName;
            // Try to load from the module of the IIOMetadata implementation
            // for this plugin since the IIOMetadataImpl is part of the plugin
            PrivilegedAction<Class<?>> pa = () -> { return getMetadataFormatClass(className); };
            @SuppressWarnings("removal")
            Class<?> cls = AccessController.doPrivileged(pa);
            Method meth = cls.getMethod("getInstance");
            return (IIOMetadataFormat) meth.invoke(null);
        } catch (Exception e) {
            RuntimeException ex =
                new IllegalStateException ("Can't obtain format");
            ex.initCause(e);
            throw ex;
        }
    }

    // If updating this method also see the same in ImageReaderWriterSpi.java
    private Class<?> getMetadataFormatClass(String formatClassName) {
        Module thisModule = IIOMetadata.class.getModule();
        Module targetModule = this.getClass().getModule();
        Class<?> c = null;
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            c = Class.forName(formatClassName, false, cl);
            if (!IIOMetadataFormat.class.isAssignableFrom(c)) {
                return null;
            }
        } catch (ClassNotFoundException e) {
        }
        if (thisModule.equals(targetModule) || c == null) {
            return c;
        }
        if (targetModule.isNamed()) {
            int i = formatClassName.lastIndexOf(".");
            String pn = i > 0 ? formatClassName.substring(0, i) : "";
            if (!targetModule.isExported(pn, thisModule)) {
                throw new IllegalStateException("Class " + formatClassName +
                   " in named module must be exported to java.desktop module.");
            }
        }
        return c;
    }

    /**
     * Returns an XML DOM {@code Node} object that represents the
     * root of a tree of metadata contained within this object
     * according to the conventions defined by a given metadata
     * format.
     *
     * <p> The names of the available metadata formats may be queried
     * using the {@code getMetadataFormatNames} method.
     *
     * @param formatName the desired metadata format.
     *
     * @return an XML DOM {@code Node} object forming the
     * root of a tree.
     *
     * @exception IllegalArgumentException if {@code formatName}
     * is {@code null} or is not one of the names returned by
     * {@code getMetadataFormatNames}.
     *
     * @see #getMetadataFormatNames
     * @see #setFromTree
     * @see #mergeTree
     */
    public abstract Node getAsTree(String formatName);

    /**
     * Alters the internal state of this {@code IIOMetadata}
     * object from a tree of XML DOM {@code Node}s whose syntax
     * is defined by the given metadata format.  The previous state is
     * altered only as necessary to accommodate the nodes that are
     * present in the given tree.  If the tree structure or contents
     * are invalid, an {@code IIOInvalidTreeException} will be
     * thrown.
     *
     * <p> As the semantics of how a tree or subtree may be merged with
     * another tree are completely format-specific, plug-in authors may
     * implement this method in whatever manner is most appropriate for
     * the format, including simply replacing all existing state with the
     * contents of the given tree.
     *
     * @param formatName the desired metadata format.
     * @param root an XML DOM {@code Node} object forming the
     * root of a tree.
     *
     * @exception IllegalStateException if this object is read-only.
     * @exception IllegalArgumentException if {@code formatName}
     * is {@code null} or is not one of the names returned by
     * {@code getMetadataFormatNames}.
     * @exception IllegalArgumentException if {@code root} is
     * {@code null}.
     * @exception IIOInvalidTreeException if the tree cannot be parsed
     * successfully using the rules of the given format.
     *
     * @see #getMetadataFormatNames
     * @see #getAsTree
     * @see #setFromTree
     */
    public abstract void mergeTree(String formatName, Node root)
        throws IIOInvalidTreeException;

    /**
     * Returns an {@code IIOMetadataNode} representing the chroma
     * information of the standard {@code javax_imageio_1.0}
     * metadata format, or {@code null} if no such information is
     * available.  This method is intended to be called by the utility
     * routine {@code getStandardTree}.
     *
     * <p> The default implementation returns {@code null}.
     *
     * <p> Subclasses should override this method to produce an
     * appropriate subtree if they wish to support the standard
     * metadata format.
     *
     * @return an {@code IIOMetadataNode}, or {@code null}.
     *
     * @see #getStandardTree
     */
    protected IIOMetadataNode getStandardChromaNode() {
        return null;
    }

    /**
     * Returns an {@code IIOMetadataNode} representing the
     * compression information of the standard
     * {@code javax_imageio_1.0} metadata format, or
     * {@code null} if no such information is available.  This
     * method is intended to be called by the utility routine
     * {@code getStandardTree}.
     *
     * <p> The default implementation returns {@code null}.
     *
     * <p> Subclasses should override this method to produce an
     * appropriate subtree if they wish to support the standard
     * metadata format.
     *
     * @return an {@code IIOMetadataNode}, or {@code null}.
     *
     * @see #getStandardTree
     */
    protected IIOMetadataNode getStandardCompressionNode() {
        return null;
    }

    /**
     * Returns an {@code IIOMetadataNode} representing the data
     * format information of the standard
     * {@code javax_imageio_1.0} metadata format, or
     * {@code null} if no such information is available.  This
     * method is intended to be called by the utility routine
     * {@code getStandardTree}.
     *
     * <p> The default implementation returns {@code null}.
     *
     * <p> Subclasses should override this method to produce an
     * appropriate subtree if they wish to support the standard
     * metadata format.
     *
     * @return an {@code IIOMetadataNode}, or {@code null}.
     *
     * @see #getStandardTree
     */
    protected IIOMetadataNode getStandardDataNode() {
        return null;
    }

    /**
     * Returns an {@code IIOMetadataNode} representing the
     * dimension information of the standard
     * {@code javax_imageio_1.0} metadata format, or
     * {@code null} if no such information is available.  This
     * method is intended to be called by the utility routine
     * {@code getStandardTree}.
     *
     * <p> The default implementation returns {@code null}.
     *
     * <p> Subclasses should override this method to produce an
     * appropriate subtree if they wish to support the standard
     * metadata format.
     *
     * @return an {@code IIOMetadataNode}, or {@code null}.
     *
     * @see #getStandardTree
     */
    protected IIOMetadataNode getStandardDimensionNode() {
        return null;
    }

    /**
     * Returns an {@code IIOMetadataNode} representing the document
     * information of the standard {@code javax_imageio_1.0}
     * metadata format, or {@code null} if no such information is
     * available.  This method is intended to be called by the utility
     * routine {@code getStandardTree}.
     *
     * <p> The default implementation returns {@code null}.
     *
     * <p> Subclasses should override this method to produce an
     * appropriate subtree if they wish to support the standard
     * metadata format.
     *
     * @return an {@code IIOMetadataNode}, or {@code null}.
     *
     * @see #getStandardTree
     */
    protected IIOMetadataNode getStandardDocumentNode() {
        return null;
    }

    /**
     * Returns an {@code IIOMetadataNode} representing the textual
     * information of the standard {@code javax_imageio_1.0}
     * metadata format, or {@code null} if no such information is
     * available.  This method is intended to be called by the utility
     * routine {@code getStandardTree}.
     *
     * <p> The default implementation returns {@code null}.
     *
     * <p> Subclasses should override this method to produce an
     * appropriate subtree if they wish to support the standard
     * metadata format.
     *
     * @return an {@code IIOMetadataNode}, or {@code null}.
     *
     * @see #getStandardTree
     */
    protected IIOMetadataNode getStandardTextNode() {
        return null;
    }

    /**
     * Returns an {@code IIOMetadataNode} representing the tiling
     * information of the standard {@code javax_imageio_1.0}
     * metadata format, or {@code null} if no such information is
     * available.  This method is intended to be called by the utility
     * routine {@code getStandardTree}.
     *
     * <p> The default implementation returns {@code null}.
     *
     * <p> Subclasses should override this method to produce an
     * appropriate subtree if they wish to support the standard
     * metadata format.
     *
     * @return an {@code IIOMetadataNode}, or {@code null}.
     *
     * @see #getStandardTree
     */
    protected IIOMetadataNode getStandardTileNode() {
        return null;
    }

    /**
     * Returns an {@code IIOMetadataNode} representing the
     * transparency information of the standard
     * {@code javax_imageio_1.0} metadata format, or
     * {@code null} if no such information is available.  This
     * method is intended to be called by the utility routine
     * {@code getStandardTree}.
     *
     * <p> The default implementation returns {@code null}.
     *
     * <p> Subclasses should override this method to produce an
     * appropriate subtree if they wish to support the standard
     * metadata format.
     *
     * @return an {@code IIOMetadataNode}, or {@code null}.
     */
    protected IIOMetadataNode getStandardTransparencyNode() {
        return null;
    }

    /**
     * Appends a new node to an existing node, if the new node is
     * non-{@code null}.
     */
    private void append(IIOMetadataNode root, IIOMetadataNode node) {
        if (node != null) {
            root.appendChild(node);
        }
    }

    /**
     * A utility method to return a tree of
     * {@code IIOMetadataNode}s representing the metadata
     * contained within this object according to the conventions of
     * the standard {@code javax_imageio_1.0} metadata format.
     *
     * <p> This method calls the various {@code getStandard*Node}
     * methods to supply each of the subtrees rooted at the children
     * of the root node.  If any of those methods returns
     * {@code null}, the corresponding subtree will be omitted.
     * If all of them return {@code null}, a tree consisting of a
     * single root node will be returned.
     *
     * @return an {@code IIOMetadataNode} representing the root
     * of a metadata tree in the {@code javax_imageio_1.0}
     * format.
     *
     * @see #getStandardChromaNode
     * @see #getStandardCompressionNode
     * @see #getStandardDataNode
     * @see #getStandardDimensionNode
     * @see #getStandardDocumentNode
     * @see #getStandardTextNode
     * @see #getStandardTileNode
     * @see #getStandardTransparencyNode
     */
    protected final IIOMetadataNode getStandardTree() {
        IIOMetadataNode root = new IIOMetadataNode
                (IIOMetadataFormatImpl.standardMetadataFormatName);
        append(root, getStandardChromaNode());
        append(root, getStandardCompressionNode());
        append(root, getStandardDataNode());
        append(root, getStandardDimensionNode());
        append(root, getStandardDocumentNode());
        append(root, getStandardTextNode());
        append(root, getStandardTileNode());
        append(root, getStandardTransparencyNode());
        return root;
    }

    /**
     * Sets the internal state of this {@code IIOMetadata} object
     * from a tree of XML DOM {@code Node}s whose syntax is
     * defined by the given metadata format.  The previous state is
     * discarded.  If the tree's structure or contents are invalid, an
     * {@code IIOInvalidTreeException} will be thrown.
     *
     * <p> The default implementation calls {@code reset}
     * followed by {@code mergeTree(formatName, root)}.
     *
     * @param formatName the desired metadata format.
     * @param root an XML DOM {@code Node} object forming the
     * root of a tree.
     *
     * @exception IllegalStateException if this object is read-only.
     * @exception IllegalArgumentException if {@code formatName}
     * is {@code null} or is not one of the names returned by
     * {@code getMetadataFormatNames}.
     * @exception IllegalArgumentException if {@code root} is
     * {@code null}.
     * @exception IIOInvalidTreeException if the tree cannot be parsed
     * successfully using the rules of the given format.
     *
     * @see #getMetadataFormatNames
     * @see #getAsTree
     * @see #mergeTree
     */
    public void setFromTree(String formatName, Node root)
        throws IIOInvalidTreeException {
        reset();
        mergeTree(formatName, root);
    }

    /**
     * Resets all the data stored in this object to default values,
     * usually to the state this object was in immediately after
     * construction, though the precise semantics are plug-in specific.
     * Note that there are many possible default values, depending on
     * how the object was created.
     *
     * @exception IllegalStateException if this object is read-only.
     *
     * @see javax.imageio.ImageReader#getStreamMetadata
     * @see javax.imageio.ImageReader#getImageMetadata
     * @see javax.imageio.ImageWriter#getDefaultStreamMetadata
     * @see javax.imageio.ImageWriter#getDefaultImageMetadata
     */
    public abstract void reset();

    /**
     * Sets the {@code IIOMetadataController} to be used
     * to provide settings for this {@code IIOMetadata}
     * object when the {@code activateController} method
     * is called, overriding any default controller.  If the
     * argument is {@code null}, no controller will be
     * used, including any default.  To restore the default, use
     * {@code setController(getDefaultController())}.
     *
     * <p> The default implementation sets the {@code controller}
     * instance variable to the supplied value.
     *
     * @param controller An appropriate
     * {@code IIOMetadataController}, or {@code null}.
     *
     * @see IIOMetadataController
     * @see #getController
     * @see #getDefaultController
     * @see #hasController
     * @see #activateController()
     */
    public void setController(IIOMetadataController controller) {
        this.controller = controller;
    }

    /**
     * Returns whatever {@code IIOMetadataController} is currently
     * installed.  This could be the default if there is one,
     * {@code null}, or the argument of the most recent call
     * to {@code setController}.
     *
     * <p> The default implementation returns the value of the
     * {@code controller} instance variable.
     *
     * @return the currently installed
     * {@code IIOMetadataController}, or {@code null}.
     *
     * @see IIOMetadataController
     * @see #setController
     * @see #getDefaultController
     * @see #hasController
     * @see #activateController()
     */
    public IIOMetadataController getController() {
        return controller;
    }

    /**
     * Returns the default {@code IIOMetadataController}, if there
     * is one, regardless of the currently installed controller.  If
     * there is no default controller, returns {@code null}.
     *
     * <p> The default implementation returns the value of the
     * {@code defaultController} instance variable.
     *
     * @return the default {@code IIOMetadataController}, or
     * {@code null}.
     *
     * @see IIOMetadataController
     * @see #setController(IIOMetadataController)
     * @see #getController
     * @see #hasController
     * @see #activateController()
     */
    public IIOMetadataController getDefaultController() {
        return defaultController;
    }

    /**
     * Returns {@code true} if there is a controller installed
     * for this {@code IIOMetadata} object.
     *
     * <p> The default implementation returns {@code true} if the
     * {@code getController} method returns a
     * non-{@code null} value.
     *
     * @return {@code true} if a controller is installed.
     *
     * @see IIOMetadataController
     * @see #setController(IIOMetadataController)
     * @see #getController
     * @see #getDefaultController
     * @see #activateController()
     */
    public boolean hasController() {
        return (getController() != null);
    }

    /**
     * Activates the installed {@code IIOMetadataController} for
     * this {@code IIOMetadata} object and returns the resulting
     * value.  When this method returns {@code true}, all values for this
     * {@code IIOMetadata} object will be ready for the next write
     * operation.  If {@code false} is
     * returned, no settings in this object will have been disturbed
     * (<i>i.e.</i>, the user canceled the operation).
     *
     * <p> Ordinarily, the controller will be a GUI providing a user
     * interface for a subclass of {@code IIOMetadata} for a
     * particular plug-in.  Controllers need not be GUIs, however.
     *
     * <p> The default implementation calls {@code getController}
     * and the calls {@code activate} on the returned object if
     * {@code hasController} returns {@code true}.
     *
     * @return {@code true} if the controller completed normally.
     *
     * @exception IllegalStateException if there is no controller
     * currently installed.
     *
     * @see IIOMetadataController
     * @see #setController(IIOMetadataController)
     * @see #getController
     * @see #getDefaultController
     * @see #hasController
     */
    public boolean activateController() {
        if (!hasController()) {
            throw new IllegalStateException("hasController() == false!");
        }
        return getController().activate(this);
    }
}
