/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.xml.sax.ext;

import java.io.IOException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;


/**
 * Extended interface for mapping external entity references to input
 * sources, or providing a missing external subset.  The
 * {@link XMLReader#setEntityResolver XMLReader.setEntityResolver()} method
 * is used to provide implementations of this interface to parsers.
 * When a parser uses the methods in this interface, the
 * {@link EntityResolver2#resolveEntity EntityResolver2.resolveEntity()}
 * method (in this interface) is used <em>instead of</em> the older (SAX 1.0)
 * {@link EntityResolver#resolveEntity EntityResolver.resolveEntity()} method.
 *
 * <p>If a SAX application requires the customized handling which this
 * interface defines for external entities, it must ensure that it uses
 * an XMLReader with the
 * <em>http://xml.org/sax/features/use-entity-resolver2</em> feature flag
 * set to <em>true</em> (which is its default value when the feature is
 * recognized).  If that flag is unrecognized, or its value is false,
 * or the resolver does not implement this interface, then only the
 * {@link EntityResolver} method will be used.
 *
 * <p>That supports three categories of application that modify entity
 * resolution.  <em>Old Style</em> applications won't know about this interface;
 * they will provide an EntityResolver.
 * <em>Transitional Mode</em> provide an EntityResolver2 and automatically
 * get the benefit of its methods in any systems (parsers or other tools)
 * supporting it, due to polymorphism.
 * Both <em>Old Style</em> and <em>Transitional Mode</em> applications will
 * work with any SAX2 parser.
 * <em>New style</em> applications will fail to run except on SAX2 parsers
 * that support this particular feature.
 * They will insist that feature flag have a value of "true", and the
 * EntityResolver2 implementation they provide  might throw an exception
 * if the original SAX 1.0 style entity resolution method is invoked.
 *
 * @see org.xml.sax.XMLReader#setEntityResolver
 *
 * @since 1.5, SAX 2.0 (extensions 1.1 alpha)
 * @author David Brownell
 */
public interface EntityResolver2 extends EntityResolver
{
    /**
     * Allows applications to provide an external subset for documents
     * that don't explicitly define one.  Documents with DOCTYPE declarations
     * that omit an external subset can thus augment the declarations
     * available for validation, entity processing, and attribute processing
     * (normalization, defaulting, and reporting types including ID).
     * This augmentation is reported
     * through the {@link LexicalHandler#startDTD startDTD()} method as if
     * the document text had originally included the external subset;
     * this callback is made before any internal subset data or errors
     * are reported.
     *
     * <p>This method can also be used with documents that have no DOCTYPE
     * declaration.  When the root element is encountered,
     * but no DOCTYPE declaration has been seen, this method is
     * invoked.  If it returns a value for the external subset, that root
     * element is declared to be the root element, giving the effect of
     * splicing a DOCTYPE declaration at the end the prolog of a document
     * that could not otherwise be valid.  The sequence of parser callbacks
     * in that case logically resembles this:
     *
     * <pre>
     * ... comments and PIs from the prolog (as usual)
     * startDTD ("rootName", source.getPublicId (), source.getSystemId ());
     * startEntity ("[dtd]");
     * ... declarations, comments, and PIs from the external subset
     * endEntity ("[dtd]");
     * endDTD ();
     * ... then the rest of the document (as usual)
     * startElement (..., "rootName", ...);
     * </pre>
     *
     * <p>Note that the InputSource gets no further resolution.
     * Implementations of this method may wish to invoke
     * {@link #resolveEntity resolveEntity()} to gain benefits such as use
     * of local caches of DTD entities.  Also, this method will never be
     * used by a (non-validating) processor that is not including external
     * parameter entities.
     *
     * <p>Uses for this method include facilitating data validation when
     * interoperating with XML processors that would always require
     * undesirable network accesses for external entities, or which for
     * other reasons adopt a "no DTDs" policy.
     * Non-validation motives include forcing documents to include DTDs so
     * that attributes are handled consistently.
     * For example, an XPath processor needs to know which attibutes have
     * type "ID" before it can process a widely used type of reference.
     *
     * <p><strong>Warning:</strong> Returning an external subset modifies
     * the input document.  By providing definitions for general entities,
     * it can make a malformed document appear to be well formed.
     *
     * @param name Identifies the document root element.  This name comes
     *  from a DOCTYPE declaration (where available) or from the actual
     *  root element.
     * @param baseURI The document's base URI, serving as an additional
     *  hint for selecting the external subset.  This is always an absolute
     *  URI, unless it is null because the XMLReader was given an InputSource
     *  without one.
     *
     * @return An InputSource object describing the new external subset
     *  to be used by the parser, or null to indicate that no external
     *  subset is provided.
     *
     * @throws SAXException Any SAX exception, possibly wrapping
     *  another exception.
     * @throws IOException Probably indicating a failure to create
     *  a new InputStream or Reader, or an illegal URL.
     */
    public InputSource getExternalSubset (String name, String baseURI)
    throws SAXException, IOException;

    /**
     * Allows applications to map references to external entities into input
     * sources, or tell the parser it should use conventional URI resolution.
     * This method is only called for external entities which have been
     * properly declared.
     * This method provides more flexibility than the {@link EntityResolver}
     * interface, supporting implementations of more complex catalogue
     * schemes such as the one defined by the <a href=
     *   "http://www.oasis-open.org/committees/entity/spec-2001-08-06.html"
     *   >OASIS XML Catalogs</a> specification.
     *
     * <p>Parsers configured to use this resolver method will call it
     * to determine the input source to use for any external entity
     * being included because of a reference in the XML text.
     * That excludes the document entity, and any external entity returned
     * by {@link #getExternalSubset getExternalSubset()}.
     * When a (non-validating) processor is configured not to include
     * a class of entities (parameter or general) through use of feature
     * flags, this method is not invoked for such entities.
     *
     * <p>Note that the entity naming scheme used here is the same one
     * used in the {@link LexicalHandler}, or in the {@link
     *   org.xml.sax.ContentHandler#skippedEntity
     *   ContentHandler.skippedEntity()}
     * method.
     *
     * @param name Identifies the external entity being resolved.
     *  Either "[dtd]" for the external subset, or a name starting
     *  with "%" to indicate a parameter entity, or else the name of
     *  a general entity.  This is never null when invoked by a SAX2
     *  parser.
     * @param publicId The public identifier of the external entity being
     *  referenced (normalized as required by the XML specification), or
     *  null if none was supplied.
     * @param baseURI The URI with respect to which relative systemIDs
     *  are interpreted.  This is always an absolute URI, unless it is
     *  null (likely because the XMLReader was given an InputSource without
     *  one).  This URI is defined by the XML specification to be the one
     *  associated with the "{@literal <}" starting the relevant declaration.
     * @param systemId The system identifier of the external entity
     *  being referenced; either a relative or absolute URI.
     *  This is never null when invoked by a SAX2 parser; only declared
     *  entities, and any external subset, are resolved by such parsers.
     *
     * @return An InputSource object describing the new input source to
     *  be used by the parser.  Returning null directs the parser to
     *  resolve the system ID against the base URI and open a connection
     *  to resulting URI.
     *
     * @throws SAXException Any SAX exception, possibly wrapping
     *  another exception.
     * @throws IOException Probably indicating a failure to create
     *  a new InputStream or Reader, or an illegal URL.
     */
    public InputSource resolveEntity (
            String name,
            String publicId,
            String baseURI,
            String systemId
    ) throws SAXException, IOException;
}
