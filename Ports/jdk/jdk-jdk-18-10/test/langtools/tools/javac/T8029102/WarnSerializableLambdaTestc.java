/*
 * @test /nodynamiccopyright/
 * @bug 8026721
 * @summary Enhance Lambda serialization
 *     Checks that the warning for accessing non public members of a class is fired correctly.
 * @compile -Xlint:serial -Werror WarnSerializableLambdaTestc.java
 */

import javax.tools.SimpleJavaFileObject;
import java.io.Serializable;

public class WarnSerializableLambdaTestc {
    public interface SerializableIntf<T> extends Serializable {
        String get(T o);
    }

    private void dontWarn() {
        SerializableIntf<SimpleJavaFileObject> s = SimpleJavaFileObject::getName;
    }
}
