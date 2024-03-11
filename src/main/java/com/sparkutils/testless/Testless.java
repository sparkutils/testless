package com.sparkutils.testless;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.util.ArrayList;

public class Testless {
    public static void scalaTestRunner(String[] args){
        org.scalatest.tools.Runner.run(args);
    }

    public static void testFrameless(String[] args) throws IOException {
        ArrayList<String> oargs = new ArrayList<String>();
        oargs.add("-oWDT");
        for (int i = 0; i < args.length; i++) {
            oargs.add(args[i]);
        }

        ClassPath classPath = ClassPath.from(Testless.class.getClassLoader());
        ImmutableSet<ClassPath.ClassInfo> framelessInfo = classPath.getTopLevelClassesRecursive("frameless");
        for (ClassPath.ClassInfo i: framelessInfo) {
            try {
                Class<?> clazz = i.load();
                org.scalatest.Suite suite = (org.scalatest.Suite) clazz.newInstance();
                // it's a suite
                oargs.add("-s");
                oargs.add(clazz.getName());
            } catch (Throwable t) {
                // ignore
            }
        }

        String[] joined = new String[oargs.size()];
        oargs.<String>toArray(joined);
        org.scalatest.tools.Runner.run(joined);
    }

    public static void runFramelessTestName(String testName) throws IOException {
        ArrayList<String> oargs = new ArrayList<String>();
        oargs.add("-oWDT");

        oargs.add("-s");
        oargs.add(testName);

        String[] joined = new String[oargs.size()];
        oargs.<String>toArray(joined);
        org.scalatest.tools.Runner.run(joined);
    }

    public static void testFrameless() throws IOException {
        testFrameless(new String[]{});
    }

    public static void main(String[] args) throws IOException {
        testFrameless(args);
    }
}
