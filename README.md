# testless

Shaded frameless test packages for each Databricks LTS runtime, other runtimes may be added, allows the full test suite to be tested against the correct shims

This badly named (or is it?) mini project aims to use frameless test packages, bundle them for a given frameless release and run against each appropriate DBR.

NOTE!!! - frameless only publishes against particular oss versions, combinations are built in this project that are not supported by frameless (see below for why) and are named as unsupported for this reason.

## Version and Naming convention

    testless_${framelessVersion}_[supported|unsupported]_${runtimeCompatVersion}_${sparkCompatVersion}_${scalaCompatVersion}

As such the test shade for 0.17 frameless 3.5.1 version against DBR 14.3 LTS would be:

    testless_0.17.0-3.5.1_supported_14.3.dbr_3.5_2.12

However running the 0.17 frameless 3.3.4 version against 14.3 would be:

    testless_0.17.0-3.3.4_unsupported_14.3.dbr_3.5_2.12

Version combinations anticipated (depends on [#300](https://github.com/typelevel/frameless/pull/800) being accepted) also include DBRs and OSS base versions that are not explicitly or even officially supported by the Frameless project.  If there is an interesting version combination missing please raise an issue.

## How to run

Ensure the only jar added for a runtime is testless - you can use the databricks community edition to test this as well.  If it's not the only jar, don't raise a ticket if something goes wrong (it's shaded but...).

Open a scala notebook and run the following command:

```scala
frameless.setOutputDir("testoutputPath")
// optionally pass additional scalatest params
com.sparkutils.testless.Testless.testFrameless()
```

On Databricks a usable path might be: /dbfs/databricks/testless 

All detected frameless test suites will be run with the summary (as of 11.03 against testless_0.17.0-3.5.1_14.3.dbr_3.5_2.12-0.0.1-SNAPSHOT):

```
Run completed in 47 minutes, 0 seconds.
Total number of tests run: 450
Suites: completed 85, aborted 0
Tests: succeeded 448, failed 2, canceled 0, ignored 0, pending 0
*** 2 TESTS FAILED ***
```

If there are class cast or classnotfounds's etc. please raise an issue describing the target platform and full testless jar name used.  It's more than possible some test scope jar is missing for that combination.

### Extra Args or a single test

Use runFramelessTestName with a single fqn test name or testFrameless(String[]) with additional args and all tests.  Or replace all the running args (not advised as it will attempt gui usage) with scalaTestRunner. 

## Why do this?

If you are attempting to run some Frameless based code on a DBR and it doesn't work, you can run the official test suite against that DBR version (or indeed in advance). Frameless' test coverage is very high, so it's likely one of four outcomes:

1. It's not a supported combination of base OSS and runtime (typically has unsupported in the name)
2. That DBR has an incompatible with OSS api change that Frameless uses
3. The shim has a bug
4. That version of oss/dbr behaves differently than Frameless expects.

If you see errors in com.sparkutils.shim or org.apache.spark.sql.shim packages or ShimUtils then it's 3. and please raise a ticket there.  If it's a not yet released version of shim for that DBR runtime then again, please raise a ticket on shim.  If, however, you are testing a combination with differing base versions than frameless intended like (testless_0.17-3.3.4_unsupported_14.3.dbr_3.5_2.12) this is not a supported combination.  Sometimes although it's should be a supported combination the DBR may backport a fix which breaks compatibility (this has happened with the decimal precision changes in 10.4/11.3 etc) - these fall under 4 (although testless may be the fix for it).

The 2nd class of issue is a really difficult one to solve and would require frameless to build directly against the compilation shims (like Quality does) - this is not ideal - please raise an issue on shim first even though it's possibly frameless related, there may be workarounds.

If it's really not 1-3 then raising a ticket on frameless is worthwhile but please mention the exact version of testless and the DBR runtime you tested against in your issue.

## How can I add a new runtime?

First make sure shim supports that runtime, raise an issue/PR etc. to help that, then raise one here to use that shim.

## Why does the driver restart on Databricka?

No idea yet, if you have the driver logs tab open you can still see the runs results even though the old driver is toast. 

## How can I get a version re-tested?

The shaded jars are on mvn central, get a Databricks community edition account and run them (keep the driver logs tab open so you can open the stdout file after the driver is restarted).

If you get different results please raise a PR to update the Tested Combos table below.

## Tested Combos

Each test run takes about 45 minutes on Databricks community edition, as such a differentiation is made between tested by testless commiters and community provided. 

**NOTE:**  Please ensure an appropriate version combination is used in your applications.  Some of these combinations **cannot** be recommended for use.  Good test results are for information only and do not represent "support" from the frameless community.      

| Version                                                | Shim Version | Runtime Tested Against | Test Results      | Run By       | Run On   | Notes           |
|--------------------------------------------------------|--------------|------------------------|-------------------|--------------|----------|-----------------|
| testless_0.17.0-3.5.1_14.3.dbr_3.5_2.12-0.0.1-SNAPSHOT | 0.0.1-RC3    | Databricks 14.3 LTS    | All tests passed                  | chris-twiner | 14.03.24 |                 |
| testless_0.17.0-3.5.1_14.0.dbr_3.5_2.12-0.0.1-SNAPSHOT | 0.0.1-RC3    | Databricks 14.1        | All tests passed       | chris-twiner | 14.03.24 | Prefer 14.3 LTS |
| testless_0.17.0-3.4.2_13.3.dbr_3.4_2.12-0.0.1-SNAPSHOT | 0.0.1-RC3    | Databricks 13.3 LTS    | All tests passed       | chris-twiner | 14.03.24 |  |

