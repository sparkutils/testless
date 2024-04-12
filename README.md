# testless

Shaded frameless test packages for each Databricks LTS runtime, other runtimes may be added, allows the full test suite to be tested against the correct shims

This badly named (or is it?) mini project aims to use frameless test packages, bundle them for a given frameless release and run against each appropriate DBR.

NOTE!!! - frameless only publishes against particular oss versions, combinations are built in this project that are not supported by frameless (see below for why).

## Version and Naming convention

    testless_${framelessVersion}_${runtimeCompatVersion}_${sparkCompatVersion}_${scalaCompatVersion}

As such the test shade for 0.17 frameless 3.5.1 version against DBR 14.3 LTS would be:

    testless_0.17.0-3.5.1_14.3.dbr_3.5_2.12

Version combinations anticipated (depends on [#800](https://github.com/typelevel/frameless/pull/800) being accepted) also include DBRs and OSS base versions that are not explicitly or even officially supported by the Frameless project.  If there is an interesting version combination missing please raise an issue.

## How to run

Ensure the only jar added for a runtime is testless - you can use the Databricks community edition to test this as well.  If it's not the only jar, don't raise a ticket if something goes wrong (it's shaded but...).

Adding a jar via Maven coordinates on 15.0 does not seem to work on community edition (although it will show the library as green the classes aren't accessible).  

Start a correct runtime, ensure that the jar has loaded before running, open the Driver Logs tab on Databricks, then open a scala notebook attached to the correct DBR runtime and run the following command:

```scala
// closing is by default and restarts the driver on the 
frameless.setShouldCloseSession(false)
frameless.setOutputDir("testoutputPath")
// optionally pass additional scalatest params
com.sparkutils.testless.Testless.testFrameless()
```

On Databricks a usable path might be: /dbfs/databricks/testless .  If running on Databricks community edition you should keep an eye on the cluster logs and job finishing as the logs won't be kept around on community edition and often you'll need to download stdout in order to see the actual results. 

All detected frameless test suites will be run in batches of 10 suites, with each batch providing a summary (as of 14.03 against testless_0.17.0-3.5.1_14.3.dbr_3.5_2.12-0.0.1-SNAPSHOT):

```
testless - starting batch 1
Run completed in 10 minutes, 53 seconds.
Total number of tests run: 10
Suites: completed 10, aborted 0
Tests: succeeded 10, failed 0, canceled 0, ignored 0, pending 0
All tests passed.
testless - finishing batch 1
....
all testless batches completed
```

The batches are broken up as, under Databricks Community, they not only cannot run to completion (after #803/#804 and it struggled before) but close the cluster after struggling to GC.

If there are class cast or classnotfounds's etc. please raise an issue describing the target platform and full testless jar name used.  It's more than possible some test scope jar is missing for that combination.

If a batch fails with a cluster death or driver restart, you can attempt by using:

```scala
// closing is by default and restarts the driver on the 
frameless.setShouldCloseSession(false)
frameless.setOutputDir("testoutputPath")
// optionally pass additional scalatest params, via args, batchnumber (5 is a typical breaking point on Databricks with either Aggregate or NonAggregate tests)
com.sparkutils.testless.Testless.testFrameless(batchnumber)
```

### Extra Args or a single test

Use runFramelessTestName with a single fqn test name or testFrameless(String[]) with additional args and all tests.  Or replace all the running args (not advised as it will attempt gui usage) with scalaTestRunner. 

### I'd really like to run some checks to debug

You need to import from the shaded testless package and define an implicit position, the macro (although a lot is changed by the awesome [jarjar-abrams](https://github.com/eed3si9n/jarjar-abrams) returns org.scalactic.source.Position).

```scala
import frameless._

import testless.org.scalacheck.Arbitrary
import testless.org.scalacheck.Gen
import testless.org.scalatestplus.scalacheck.Checkers.check
import testless.org.scalacheck.Prop
import testless.org.scalacheck.Prop._
import testless.org.scalacheck.Prop.forAll

// a position must be defined as the macro isn't shaded fully - see https://github.com/sbt/sbt-assembly/issues/477
implicit val p: testless.org.scalactic.source.Position = testless.org.scalactic.source.Position("","",13)

implicit val sess = spark
implicit val sparkDelay: SparkDelay[Job] = Job.framelessSparkDelayForJob

import frameless.functions.aggregate.last

def prop[A: TypedEncoder: Ordering: CatalystOrdered](xs: List[A]): Prop = {
  val dataset = TypedDataset.create(xs.map(X1(_)))
  val A = dataset.col[A]('a)
  // servers do not return the same order told to
  val sxs = xs.sorted

  val datasetLast = dataset
    .coalesce(1)
    .orderBy(A: SortedTypedColumn[X1[A], A])
    .agg(last(A))
    .collect()
    .run()
    .toList

  datasetLast ?= sxs.lastOption.toList
}

check(prop[String](List("","")))
```

_NB_ As of RC7 shapeless is no longer shaded (per 477) allowing encoder derivation

## Non Shim related differences between Databricks CE and normal clusters versions

Shim takes care of the runtime class inconsistencies between spark runtimes, it cannot however abstract the potential differences in runtime behaviour. 

The Databricks Community Edition is effectively a small box running local[*] and typically crashes during the run of batch 5.  Running the tests on a proper Databricks cluster involves serialization of types, as such BigDecimal and Doubles often behave differently. Additionally, any tests that rely on upon local evaluation of in-memory collections may find errors running on a full-blown cluster when the data is serialized and partitioned.  This occurred in many tests prior to version 0.17 RC5 of sparkutils frameless.   

Additionally, the behaviour of different Databricks Runtime versions can trigger issues on previously working tests.  Version 0.0.1-RC7 (based upon sparkutils frameless 0.17 RC4) for instance failed on tests involving orderBy and first/last aggregations when running on 15.0 as it optimised with Photon rather than normal OSS.  If it looks like this is happening on a test or usage please try .coalesce(1) before sorting/aggregating as well before raising an issue.

On some runs the tolerance for covar_pop/covar_samp et al or the cube/rollup functions may need tweaking.  A number of runs on 14.3/15 have shown differences of a couple of hundred in the end result, given the test cases are generated it's entirely possible simply re-running may be enough.

## Why do this?

If you are attempting to run some Frameless based code on a DBR and it doesn't work, you can run the official test suite against that DBR version (or indeed in advance). Frameless' test coverage is very high, so it's likely one of four outcomes:

1. It's not a supported combination of base OSS and runtime
2. That DBR has an incompatible with OSS api change that Frameless uses
3. The shim has a bug
4. That version of oss/dbr behaves differently than Frameless expects.

If you see errors in com.sparkutils.shim or org.apache.spark.sql.shim packages or ShimUtils then it's 3. and please raise a ticket there.  If it's a not yet released version of shim for that DBR runtime then again, please raise a ticket on shim.  If, however, you are testing a combination with differing base versions than frameless intended like (testless_0.17-3.3.4_14.3.dbr_3.5_2.12) this is not a supported combination.  Sometimes although it's should be a supported combination the DBR may backport a fix which breaks compatibility (this has happened with the decimal precision changes in 10.4/11.3 etc) - these fall under 4 (although testless may be the place to remediate rather than the upstream libraries).

The 2nd class of issue is a really difficult one to solve and would require frameless to build directly against the compilation shims (like Quality does) - this is not ideal - please raise an issue on shim first even though it's possibly frameless related, there may be workarounds.

If it's really not 1-3 then raising a ticket on frameless is worthwhile but please attempt to run the failing tests directly looking for ordering or coalescing issues and mention the exact artifact / version of testless and the DBR runtime you tested against in your issue.

## How can I add a new runtime?

First make sure shim supports that runtime, raise an issue/PR etc. to help that, then raise one here to use that shim.

## How can I get a version re-tested?

The shaded jars are on mvn central, get a Databricks community edition account and run them (keep the driver logs tab open so you can open the stdout file).

If you get different results please raise a PR to update the Tested Combos table below.

## Tested Combos

Test runs are noted as "All tests passed" when no code changes to the tests are required to run.  Per the note above on generated test values, it's possible to have a test randomly fail, if re-running it directly (e.g. either via the test class or copy and pasting the actual test) runs successfully this is included. 

**NOTE:**  Please ensure an appropriate version combination is used in your applications.  Some of these combinations **cannot** be recommended for use even if the tests pass, for example using base oss build which is different to that of a DBR, known to be worrisome combos will have something in the Notes column.  Good test results are for information only and do not represent "support" from the frameless community.      

| Version                                                    | Minimum Shim Version | Runtime Tested Against | Test Results                                                                                                                                                                  | Run By       | Run On   | Notes                                                                                                                                                                                                                                                                                      |
|------------------------------------------------------------|----------------------|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| testless_0.17.0-3.5.1_14.3.dbr_3.5_2.12-0.0.1-SNAPSHOT     | 0.0.1-RC4            | Databricks 15.0 CE     | All tests passed                                                                                                                                                              | chris-twiner | 27.03.24 | Using #804/#803 code - requires restart from batch 5   |
| testless_0.17.0-3.5.1_14.3.dbr_3.5_2.12-0.0.1-SNAPSHOT     | 0.0.1-RC4            | Databricks 14.3 LTS CE | All tests passed                                                                                                                                                              | chris-twiner | 26.03.24 | Using #804/#803 code - requires restart from batch 5                                                                                                                                                                                                                                       |
| testless_0.17.0-3.5.1_14.0.dbr_3.5_2.12-0.0.1-SNAPSHOT     | 0.0.1-RC3            | Databricks 14.1 CE     | All tests passed                                                                                                                                                              | chris-twiner | 14.03.24 | Prefer 14.3 LTS                                                                                                                                                                                                                                                                            |
| testless_0.17.0-3.4.2_13.3.dbr_3.4_2.12-0.0.1-SNAPSHOT     | 0.0.1-RC3            | Databricks 13.3 LTS CE | All tests passed                                                                                                                                                              | chris-twiner | 14.03.24 |                                                                                                                                                                                                                                                                                            |
| testless_0.17.0-3.3.2_12.2.dbr_3.3_2.12-0.0.1-SNAPSHOT     | 0.0.1-RC4            | Databricks 12.2 LTS CE | All tests passed                                                                                                                                                              | chris-twiner | 14.03.24 | MapGroups implementation is back-ported from 3.4 - so 0.0.1-RC-3 shim doesn't work.                                                                                                                                                                                                        |
| testless_0.17.0-3.3.2_11.3.dbr_3.3_2.12-0.0.1-SNAPSHOT     | 0.0.1-RC4            | Databricks 11.3 LTS CE | All tests passed                                                                                                                                                              | chris-twiner | 26.03.24 | Same major/minor as 12.2. Prefer 12.2 LTS. Verified #803 fix (restart on batch 5)                                                                                                                                                                                                          |
| testless_0.17.0-3.3.2_10.4.dbr_3.3_2.12-0.0.1-SNAPSHOT     | 0.0.1-RC3            | Databricks 10.4 LTS CE | All but Lit predicate pushdown tests passed                                                                                                                                   | chris-twiner | 14.03.24 | Not recommended due to major/minor mismatch (3.3.0 vs 3.2.1) - caveat emptor.  Lit predicate pushdown is not possible on this Spark base version                                                                                                                                           |
| testless_0.17.0-3.3.2_9.1.dbr_3.3_2.12-0.0.1-SNAPSHOT      | 0.0.1-RC3            | Databricks 9.1 LTS CE  | All but Lit predicate pushdown tests passed                                                                                                                                   | chris-twiner | 14.03.24 | Not recommended due to EOL and major/minor mismatch (3.3.0 vs 3.1.2) - caveat emptor.  Lit predicate pushdown is not possible on this Spark base version                                                                                                                                   |
| testless_0.17.0-3.5.1_9.1.dbr_3.5_2.12-0.0.1-SNAPSHOT      | 0.0.1-RC4            | Databricks 9.1 LTS CE  | All but Lit predicate pushdown tests passed*                                                                                                                                  | chris-twiner | 26.03.24 | DO NOT USE, only here to prove approach - restart on batch 5 gets through tests. EOL, 3.5 vs 3.1, lit pushdown. * also got two spurious "Expected Vector(, ) but got Vector(, )" fails                                                                                                     |
| testless_sparkutils_0.17.0-3.5_14.3.dbr_3.5_2.12-0.0.1-RC1 | 0.0.1-RC4            | Databricks 15.0 CE     | All tests passed                                                                                                                                                              | chris-twiner | 08.04.24 | sparkutils RC1 fork - Using #804/#803 code - requires restart from batch 5                                                         |
| testless_sparkutils_0.17.0-3.5_14.3.dbr_3.5_2.12-0.0.1-RC8 | 0.0.1-RC4            | Databricks 14.3 LTS    | All tests passed                                                                                                                                                              | chris-twiner | 12.04.24 | sparkutils RC5 fork - Using #804/#803 code                                                                                             |
| testless_sparkutils_0.17.0-3.5_14.3.dbr_3.5_2.12-0.0.1-RC8 | 0.0.1-RC4            | Databricks 15.0        | All tests passed                                                                                                                                                              | chris-twiner | 12.04.24 | sparkutils RC5 fork - Using #804/#803 code                                                                                        |

**Databricks 15.x** When using dbfs to store/load the testless jar use config 'spark.databricks.driver.dbfsLibraryInstallationAllowed true' before starting to get the jar working (workspace files are not possible)                                                                                    