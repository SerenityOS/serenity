/** /nodynamiccopyright/
 * Class to hold annotations for TestElementsAnnotatedWith.
 */

import annot.AnnotatedElementInfo;

@AnnotatedElementInfo(annotationName="java.lang.SuppressWarnings",
                      expectedSize=0,
                      names={})
@Undefined
public class ErroneousAnnotations {
    @Undefined
    private void foo() {return;}
}
