/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.ServiceUIFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PrinterInfo;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterMakeAndModel;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterState;
import javax.print.event.PrintServiceAttributeListener;

/**
 * Stub implementation of a custom {@link PrintService}.
 *
 * @author reinhapa
 */
public class PrintServiceStub implements PrintService {
    private final String _name;
    private final Set<DocFlavor> _flavors;
    private final Map<Class<?>, Object> _attributes;

    public PrintServiceStub(String name) {
        _name = name;
        _flavors = new HashSet<DocFlavor>();
        _flavors.add(DocFlavor.SERVICE_FORMATTED.PAGEABLE);
        _flavors.add(DocFlavor.SERVICE_FORMATTED.PRINTABLE);
        _attributes = new HashMap<>();
        _attributes.put(PrinterName.class, new PrinterName(name, null));
        _attributes.put(PrinterState.class, PrinterState.IDLE);
        _attributes.put(PrinterInfo.class, new PrinterInfo("Custom location",
                null));
        _attributes.put(PrinterIsAcceptingJobs.class,
                PrinterIsAcceptingJobs.ACCEPTING_JOBS);
        _attributes.put(PrinterMakeAndModel.class, new PrinterMakeAndModel(
                "Custom printer", null));
        _attributes.put(Media.class, new Media[] { MediaSizeName.ISO_A4 });
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean isDocFlavorSupported(DocFlavor flavor) {
        return _flavors.contains(flavor);
    }

    @Override
    public Object getSupportedAttributeValues(
            Class<? extends Attribute> category, DocFlavor flavor,
            AttributeSet attributes) {
        return _attributes.get(category);
    }

    @Override
    public boolean isAttributeCategorySupported(
            Class<? extends Attribute> category) {
        return _attributes.containsKey(category);
    }

    @Override
    public <T extends PrintServiceAttribute> T getAttribute(Class<T> category) {
        return category.cast(_attributes.get(category));
    }

    @Override
    public PrintServiceAttributeSet getAttributes() {
        return new HashPrintServiceAttributeSet(_attributes.values().toArray(
                new PrintServiceAttribute[_attributes.size()]));
    }

    @Override
    public DocFlavor[] getSupportedDocFlavors() {
        return _flavors.toArray(new DocFlavor[_flavors.size()]);
    }

    // not implemented methods

    @Override
    public DocPrintJob createPrintJob() {
        return null;
    }

    @Override
    public void addPrintServiceAttributeListener(
            PrintServiceAttributeListener listener) {

    }

    @Override
    public void removePrintServiceAttributeListener(
            PrintServiceAttributeListener listener) {

    }

    @Override
    public Class<?>[] getSupportedAttributeCategories() {
        return null;
    }

    @Override
    public Object getDefaultAttributeValue(Class<? extends Attribute> category) {
        return null;
    }

    @Override
    public boolean isAttributeValueSupported(Attribute attrval,
            DocFlavor flavor, AttributeSet attributes) {
        return false;
    }

    @Override
    public AttributeSet getUnsupportedAttributes(DocFlavor flavor,
            AttributeSet attributes) {
        return null;
    }

    @Override
    public ServiceUIFactory getServiceUIFactory() {
        return null;
    }
}
