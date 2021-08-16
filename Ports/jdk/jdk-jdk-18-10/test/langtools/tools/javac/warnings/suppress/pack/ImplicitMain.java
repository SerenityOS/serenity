/* /nodynamiccopyright/ */
package pack;

@SuppressWarnings("deprecation")
public class ImplicitMain {
    private Object test() {
        return new ImplicitUse();
    }
}

@Deprecated
class Dep {

}
