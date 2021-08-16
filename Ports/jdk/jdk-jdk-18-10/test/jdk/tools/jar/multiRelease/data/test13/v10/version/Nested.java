package version;

public class Nested {
    public int getVersion() {
        return 10;
    }

    protected void doNothing() {
    }

    class nested {
        int save = getVersion();

        class nestnested {
            int save = getVersion();;
        }
    }
}
