## What is Reneri?

Reneri is a Maven plugin that generates hints to improve a test suite, by 
observing the execution of undetected extreme transformations discovered by 
[Descartes](https://github.com/STAMP-project/pitest-descartes) in a Java project.

## Quick start

Reneri offers three Maven goals:

```bash
mvn eu.stamp-project:reneri:observeMethods
```
This goal instruments each method signaled by Descartes and observes the 
execution of the original method and each transformed variant of the method. 
The differences between the executions are reported.

```bash
mvn eu.stamp-project:reneri:observeTests
```
This goal finds and instrument each test case that executes a method signaled by
Descartes. The execution of each test case is observed for the original methods 
and the transformed variants. Differences between the executions are reported.

```bash
mvn eu.stamp-project:reneri:hints
```
Generates improvement hints according to the results obtained with the execution
of the two previous goals. These hints can be leveraged by other tools.

There is no need to modify the `pom.xml` file. It is mandatory, however,
to have executed Descartes before using the tool.
Reneri requires as input both the `METHODS` and `JSON` reports produced by 
Descartes.

A typical usage scenario would involve the execution of the three goals to 
obtain the improvement hints.

## How does Reneri work?

Reneri executes the test cases that execute methods for which at least one
extreme transformation was not detected by the test suite. Such methods are 
discovered using Descartes.

The plugin first instruments all methods to observe the program state after each
invocation. Both, the original and transformed variants of all methods are 
executed. The difference between the executions are recorded and reported.

In a second stage, the tool instruments the test classes to observe the value
of all expressions inside test methods. Then the original methods and their 
transformed variants are executed. The differences between the executions are 
recorded and reported.

The observed differences at different levels allow Reneri to point at the places
in code where developers can modify the code to improve the existing test case
by adding assertions or new test inputs.

To avoid flaky tests, the tool executes each test class several times, so
the analysis can be time-consuming.

## Funding

Reneri is partially funded by the research project STAMP (European Commission - H2020)

![STAMP - European Commission - H2020](https://github.com/STAMP-project/pitest-descartes/raw/master/docs/logo_readme_md.png)


