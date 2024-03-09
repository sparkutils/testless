# testless
Shaded frameless test packages for each Databricks LTS runtime, other runtimes may be added, allows the full test suite to be tested against the correct shims

This badly named (or is it?) mini project aims to use frameless test packages, bundle them for a given frameless release and run against each appropriate dbr.

## Version and Naming convention

    testless_${framelessVersion}_${runtimeCompatVersion}_${sparkCompatVersion}_${scalaCompatVersion}

As such the test shade for 0.17 frameless against DBR 14.3 LTS would be:

    testless_0.17_14.3.dbr_3.5_2.12

Version combinations anticipated (depends on [#300](https://github.com/typelevel/frameless/pull/800) being accepted) also include DBRs and OSS base versions that are not explicitly or even officially supported by the Frameless project.

## How to run

Ensure the only jar added for a runtime is testless - you can use the databricks community edition to test this as well.  If it's not the only jar, don't raise a ticket if something goes wrong (it's shaded but...).

Open a scala notebook and run the following command:

```scala
//pending....
```

## Why do this?

If you are attempting to run some Frameless based code on a DBR and it doesn't work, you can run the official test suite against that DBR version. Frameless' test coverage is very high, so it's likely one of four outcomes:

1. It's not a supported combination of base OSS and runtime
2. That DBR has an incompatible with OSS api change that Frameless uses
3. The shim has a bug
4. That version of oss/dbr behaves differently than Frameless expects.

If you see errors in com.sparkutils.shim or org.apache.spark.sql.shim pacakges or ShimUtils then it's 3. and please raise a ticket there.  If it's a not yet released version of shim for that DBR runtime then again, please raise a ticket on shim.

The 2nd class of issue is a really difficult one to solve and would require frameless to build directly against the compilation shims (like Quality does) - this is not ideal - please raise an issue on shim first even though it's possibly frameless related, there may be workarounds.

If it's really not 1-3 then raising a ticket on frameless is worthwhile but please mention the exact version of testless and the DBR runtime you tested against in your issue.

