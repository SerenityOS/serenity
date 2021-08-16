import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

public class TransferableList extends ArrayList {
    private static class NullInvocationHandler implements InvocationHandler, Serializable {
        public Object invoke(Object proxy, Method method, Object[] args)
          throws Throwable {
            throw new Error("UNIMPLEMENTED");
        }
    }

    public TransferableList() {
        try {
            InvocationHandler handler = new NullInvocationHandler();
            Class<?> proxyClass = Proxy.getProxyClass(
                ListInterface.class.getClassLoader(),
                new Class[] { ListInterface.class, AnotherInterface.class });
            AnotherInterface obj = (AnotherInterface) proxyClass.
                    getConstructor(new Class[]{InvocationHandler.class}).
                    newInstance(handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

interface ListInterface extends Serializable {}
