package com.sparkutils.testless;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class Testless {
    public static void scalaTestRunner(String[] args){
        org.scalatest.tools.Runner.run(args);
    }

    public static void testFrameless(int batchStartingNumber) throws IOException {
        testFrameless(new String[]{}, batchStartingNumber);
    }
    public static void testFrameless(String[] args) throws IOException {
        testFrameless(args, 0);
    }

    public static void testFrameless(String[] args, int batchStartingNumber) throws IOException {
        ArrayList<String> oargs = new ArrayList<String>();
        oargs.add("-oWDFT");
        for (int i = 0; i < args.length; i++) {
            oargs.add(args[i]);
        }

        ArrayList<String> classargs = new ArrayList<>();
        ClassPath classPath = ClassPath.from(Testless.class.getClassLoader());
        ImmutableSet<ClassPath.ClassInfo> framelessInfo = classPath.getTopLevelClassesRecursive("frameless");
        for (ClassPath.ClassInfo i: framelessInfo) {
            try {
                Class<?> clazz = i.load();
                org.scalatest.Suite suite = (org.scalatest.Suite) clazz.newInstance();
                // it's a suite
                classargs.add("-s");
                classargs.add(clazz.getName());
            } catch (Throwable t) {
                // ignore
            }
        }

        int numberOfBatches = classargs.size() / 2 / 10;
        int argsPerBatch = 20;

        Iterator<String> classargItr = classargs.iterator();

        for (int j = 0; j < (batchStartingNumber * argsPerBatch) && classargItr.hasNext(); j++) {
            classargItr.next();
        }

        for (int i = batchStartingNumber; i < numberOfBatches; i++) {
            System.out.println("testless - starting batch "+i);
            String[] joined = new String[oargs.size() + argsPerBatch];
            oargs.<String>toArray(joined);
            for (int j = 0; j < argsPerBatch && classargItr.hasNext(); j++) {
                joined[oargs.size() + j] = classargItr.next();
            }
            org.scalatest.tools.Runner.run(joined);
            System.out.println("testless - gc'ing after finishing batch "+i);
            System.gc();
            System.gc();
        }
        System.out.println("all testless batches completed");
    }

    public static void runFramelessTestName(String testName) throws IOException {
        ArrayList<String> oargs = new ArrayList<String>();
        oargs.add("-oWDFT");

        oargs.add("-s");
        oargs.add(testName);

        String[] joined = new String[oargs.size()];
        oargs.<String>toArray(joined);
        org.scalatest.tools.Runner.run(joined);
        System.out.println("testless finished properly");
    }

    public static void testFrameless() throws IOException {
        testFrameless(new String[]{});
    }

    public static void main(String[] args) throws IOException {
        testFrameless(args);
    }
}
