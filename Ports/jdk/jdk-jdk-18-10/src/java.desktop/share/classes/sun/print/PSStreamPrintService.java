/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.print;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.Locale;

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.ServiceUIFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.event.PrintServiceAttributeListener;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.ColorSupported;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.print.attribute.standard.Fidelity;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;

public class PSStreamPrintService extends StreamPrintService
    implements SunPrinterJobService {

    private static final Class<?>[] suppAttrCats = {
        Chromaticity.class,
        Copies.class,
        Fidelity.class,
        JobName.class,
        Media.class,
        MediaPrintableArea.class,
        OrientationRequested.class,
        PageRanges.class,
        RequestingUserName.class,
        SheetCollate.class,
        Sides.class,
    };

    private static int MAXCOPIES = 1000;

    private static final MediaSizeName[] mediaSizes = {
        MediaSizeName.NA_LETTER,
        MediaSizeName.TABLOID,
        MediaSizeName.LEDGER,
        MediaSizeName.NA_LEGAL,
        MediaSizeName.EXECUTIVE,
        MediaSizeName.ISO_A3,
        MediaSizeName.ISO_A4,
        MediaSizeName.ISO_A5,
        MediaSizeName.ISO_B4,
        MediaSizeName.ISO_B5,
    };

    public PSStreamPrintService(OutputStream out) {
        super(out);
    }

    public String getOutputFormat() {
        return PSStreamPrinterFactory.psMimeType;
    }


    public DocFlavor[] getSupportedDocFlavors() {
        return PSStreamPrinterFactory.getFlavors();
    }

    public DocPrintJob createPrintJob() {
        return new PSStreamPrintJob(this);
    }

    public boolean usesClass(Class<?> c) {
        return (c == sun.print.PSPrinterJob.class);
    }

    public String getName() {
        return "Postscript output";
    }

    public void addPrintServiceAttributeListener(
                         PrintServiceAttributeListener listener) {
        return;
    }

    public void removePrintServiceAttributeListener(
                            PrintServiceAttributeListener listener) {
        return;
    }


    public <T extends PrintServiceAttribute>
        T getAttribute(Class<T> category)
    {
        if (category == null) {
            throw new NullPointerException("category");
        }
        if (!(PrintServiceAttribute.class.isAssignableFrom(category))) {
            throw new IllegalArgumentException("Not a PrintServiceAttribute");
        }
        if (category == ColorSupported.class) {
            @SuppressWarnings("unchecked")
            T tmp = (T)ColorSupported.SUPPORTED;
            return tmp;
        } else {
            return null;
        }
    }
    public PrintServiceAttributeSet getAttributes() {
        PrintServiceAttributeSet attrs = new HashPrintServiceAttributeSet();
        attrs.add(ColorSupported.SUPPORTED);

        return AttributeSetUtilities.unmodifiableView(attrs);
    }

    public boolean isDocFlavorSupported(DocFlavor flavor) {
        DocFlavor [] flavors = getSupportedDocFlavors();
        for (int f=0; f<flavors.length; f++) {
            if (flavor.equals(flavors[f])) {
                return true;
            }
        }
        return false;
    }


    public Class<?>[] getSupportedAttributeCategories() {
        Class<?>[] cats = new Class<?>[suppAttrCats.length];
        System.arraycopy(suppAttrCats, 0, cats, 0, cats.length);
        return cats;
    }

    public boolean
        isAttributeCategorySupported(Class<? extends Attribute> category)
    {
        if (category == null) {
            throw new NullPointerException("null category");
        }
        if (!(Attribute.class.isAssignableFrom(category))) {
            throw new IllegalArgumentException(category +
                                             " is not an Attribute");
        }

        for (int i=0;i<suppAttrCats.length;i++) {
            if (category == suppAttrCats[i]) {
                return true;
            }
        }
        return false;
    }


    public Object
        getDefaultAttributeValue(Class<? extends Attribute> category)
    {
        if (category == null) {
            throw new NullPointerException("null category");
        }
        if (!Attribute.class.isAssignableFrom(category)) {
            throw new IllegalArgumentException(category +
                                             " is not an Attribute");
        }

        if (!isAttributeCategorySupported(category)) {
            return null;
        }

        if (category == Copies.class) {
            return new Copies(1);
        } else if (category == Chromaticity.class) {
            return Chromaticity.COLOR;
        } else if (category == Fidelity.class) {
            return Fidelity.FIDELITY_FALSE;
        } else if (category == Media.class) {
            String defaultCountry = Locale.getDefault().getCountry();
            if (defaultCountry != null &&
                (defaultCountry.isEmpty() ||
                 defaultCountry.equals(Locale.US.getCountry()) ||
                 defaultCountry.equals(Locale.CANADA.getCountry()))) {
                return MediaSizeName.NA_LETTER;
            } else {
                 return MediaSizeName.ISO_A4;
            }
        } else if (category == MediaPrintableArea.class) {
            String defaultCountry = Locale.getDefault().getCountry();
            float iw, ih;
            float margin = 0.5f; // both these papers > 5" in all dimensions
            if (defaultCountry != null &&
                (defaultCountry.isEmpty() ||
                 defaultCountry.equals(Locale.US.getCountry()) ||
                 defaultCountry.equals(Locale.CANADA.getCountry()))) {
                iw = MediaSize.NA.LETTER.getX(Size2DSyntax.INCH) - 2*margin;
                ih = MediaSize.NA.LETTER.getY(Size2DSyntax.INCH) - 2*margin;
            } else {
                iw = MediaSize.ISO.A4.getX(Size2DSyntax.INCH) - 2*margin;
                ih = MediaSize.ISO.A4.getY(Size2DSyntax.INCH) - 2*margin;
            }
            return new MediaPrintableArea(margin, margin, iw, ih,
                                          MediaPrintableArea.INCH);
        } else if (category == OrientationRequested.class) {
            return OrientationRequested.PORTRAIT;
        } else if (category == PageRanges.class) {
            return new PageRanges(1, Integer.MAX_VALUE);
        } else if (category == SheetCollate.class) {
            return SheetCollate.UNCOLLATED;
        } else if (category == Sides.class) {
            return Sides.ONE_SIDED;

        } else
            return null;
    }


    public Object
        getSupportedAttributeValues(Class<? extends Attribute> category,
                                    DocFlavor flavor,
                                    AttributeSet attributes)
    {

        if (category == null) {
            throw new NullPointerException("null category");
        }
        if (!Attribute.class.isAssignableFrom(category)) {
            throw new IllegalArgumentException(category +
                                             " does not implement Attribute");
        }
        if (flavor != null && !isDocFlavorSupported(flavor)) {
            throw new IllegalArgumentException(flavor +
                                               " is an unsupported flavor");
        }

        if (!isAttributeCategorySupported(category)) {
            return null;
        }

        if (category == Chromaticity.class) {
            Chromaticity[]arr = new Chromaticity[1];
            arr[0] = Chromaticity.COLOR;
            //arr[1] = Chromaticity.MONOCHROME;
            return (arr);
        } else if (category == JobName.class) {
            return new JobName("", null);
        } else if (category == RequestingUserName.class) {
            return new RequestingUserName("", null);
        } else if (category == OrientationRequested.class) {
            if (flavor == null ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE) ||
                flavor.equals(DocFlavor.INPUT_STREAM.GIF) ||
                flavor.equals(DocFlavor.INPUT_STREAM.JPEG) ||
                flavor.equals(DocFlavor.INPUT_STREAM.PNG) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.GIF) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.JPEG) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.PNG) ||
                flavor.equals(DocFlavor.URL.GIF) ||
                flavor.equals(DocFlavor.URL.JPEG) ||
                flavor.equals(DocFlavor.URL.PNG)) {
                OrientationRequested []arr = new OrientationRequested[3];
                arr[0] = OrientationRequested.PORTRAIT;
                arr[1] = OrientationRequested.LANDSCAPE;
                arr[2] = OrientationRequested.REVERSE_LANDSCAPE;
                return arr;
            } else {
                return null;
            }
        } else if ((category == Copies.class) ||
                   (category == CopiesSupported.class)) {
            return new CopiesSupported(1, MAXCOPIES);
        } else if (category == Media.class) {
            Media []arr = new Media[mediaSizes.length];
            System.arraycopy(mediaSizes, 0, arr, 0, mediaSizes.length);
            return arr;
        } else if (category == Fidelity.class) {
            Fidelity []arr = new Fidelity[2];
            arr[0] = Fidelity.FIDELITY_FALSE;
            arr[1] = Fidelity.FIDELITY_TRUE;
            return arr;
        } else if (category == MediaPrintableArea.class) {
            if (attributes == null) {
                return null;
            }
            MediaSize mediaSize = (MediaSize)attributes.get(MediaSize.class);
            if (mediaSize == null) {
                Media media = (Media)attributes.get(Media.class);
                if (media != null && media instanceof MediaSizeName) {
                    MediaSizeName msn = (MediaSizeName)media;
                    mediaSize = MediaSize.getMediaSizeForName(msn);
                }
            }
            if (mediaSize == null) {
                return null;
            } else {
                MediaPrintableArea []arr = new MediaPrintableArea[1];
                float w = mediaSize.getX(MediaSize.INCH);
                float h = mediaSize.getY(MediaSize.INCH);
                /* For dimensions >= 5 inches use 0.5 inch margins.
                 * For smaller dimensions, use 10% margins.
                 */
                float xmargin = 0.5f;
                float ymargin = 0.5f;
                if (w < 5f) {
                    xmargin = w/10;
                }
                if (h < 5f) {
                    ymargin = h/10;
                }
                arr[0] = new MediaPrintableArea(xmargin, ymargin,
                                                w - 2*xmargin,
                                                h - 2*ymargin,
                                                MediaSize.INCH);
                return arr;
            }
        } else if (category == PageRanges.class) {
            if (flavor == null ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {
                PageRanges []arr = new PageRanges[1];
                arr[0] = new PageRanges(1, Integer.MAX_VALUE);
                return arr;
            } else {
                return null;
            }
        } else if (category == SheetCollate.class) {
            if (flavor == null ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {
                SheetCollate []arr = new SheetCollate[2];
                arr[0] = SheetCollate.UNCOLLATED;
                arr[1] = SheetCollate.COLLATED;
                return arr;
            } else {
                SheetCollate []arr = new SheetCollate[1];
                arr[0] = SheetCollate.UNCOLLATED;
                return arr;
            }
        } else if (category == Sides.class) {
            if (flavor == null ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {
                Sides []arr = new Sides[3];
                arr[0] = Sides.ONE_SIDED;
                arr[1] = Sides.TWO_SIDED_LONG_EDGE;
                arr[2] = Sides.TWO_SIDED_SHORT_EDGE;
                return arr;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean isSupportedCopies(Copies copies) {
        int numCopies = copies.getValue();
        return (numCopies > 0 && numCopies < MAXCOPIES);
    }

    private boolean isSupportedMedia(MediaSizeName msn) {
        for (int i=0; i<mediaSizes.length; i++) {
            if (msn.equals(mediaSizes[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean isAttributeValueSupported(Attribute attr,
                                             DocFlavor flavor,
                                             AttributeSet attributes) {
        if (attr == null) {
            throw new NullPointerException("null attribute");
        }
        if (flavor != null && !isDocFlavorSupported(flavor)) {
            throw new IllegalArgumentException(flavor +
                                               " is an unsupported flavor");
        }
        Class<? extends Attribute> category = attr.getCategory();
        if (!isAttributeCategorySupported(category)) {
            return false;
        }
        else if (attr.getCategory() == Chromaticity.class) {
            return attr == Chromaticity.COLOR;
        }
        else if (attr.getCategory() == Copies.class) {
            return isSupportedCopies((Copies)attr);
        } else if (attr.getCategory() == Media.class &&
                   attr instanceof MediaSizeName) {
            return isSupportedMedia((MediaSizeName)attr);
        } else if (attr.getCategory() == OrientationRequested.class) {
            if (attr == OrientationRequested.REVERSE_PORTRAIT ||
                (flavor != null) &&
                !(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE))) {
                return false;
            }
        } else if (attr.getCategory() == PageRanges.class) {
            if (flavor != null &&
                !(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE))) {
                return false;
            }
        } else if (attr.getCategory() == SheetCollate.class) {
            if (flavor != null &&
                !(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE))) {
                return false;
            }
        } else if (attr.getCategory() == Sides.class) {
            if (flavor != null &&
                !(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
                flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE))) {
                return false;
            }
        }
        return true;
    }

    public AttributeSet getUnsupportedAttributes(DocFlavor flavor,
                                                 AttributeSet attributes) {

        if (flavor != null && !isDocFlavorSupported(flavor)) {
            throw new IllegalArgumentException("flavor " + flavor +
                                               "is not supported");
        }

        if (attributes == null) {
            return null;
        }

        Attribute attr;
        AttributeSet unsupp = new HashAttributeSet();
        Attribute[] attrs = attributes.toArray();
        for (int i=0; i<attrs.length; i++) {
            try {
                attr = attrs[i];
                if (!isAttributeCategorySupported(attr.getCategory())) {
                    unsupp.add(attr);
                } else if (!isAttributeValueSupported(attr, flavor,
                                                      attributes)) {
                    unsupp.add(attr);
                }
            } catch (ClassCastException e) {
            }
        }
        if (unsupp.isEmpty()) {
            return null;
        } else {
            return unsupp;
        }
    }

    public ServiceUIFactory getServiceUIFactory() {
        return null;
    }

    public String toString() {
        return "PSStreamPrintService: " + getName();
    }

    /* Stream services have an output stream which cannot be shared,
     * so two services are equal only if they are the same object.
     */
    public boolean equals(Object obj) {
        return (obj == this ||
                 (obj instanceof PSStreamPrintService &&
                 ((PSStreamPrintService)obj).getName().equals(getName())));
    }

   public int hashCode() {
        return this.getClass().hashCode()+getName().hashCode();
    }

}
