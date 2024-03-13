package testless;

import java.io.IOException;

public class TestlessSingle {

    public static void main(String[] args) throws IOException {
        com.sparkutils.testless.Testless.runFramelessTestName(args[0]);
    }
}
