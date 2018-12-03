## What is Reneri?

Reneri is a Maven plugin that checks if the undetected extreme
transformations produced by [Descartes](https://github.com/STAMP-project/pitest-descartes)
could be observed from the existing test code.

## Quick start

Reneri exposes only one Maven goal. It can be invoked from the command
line using the project's root folder as working directory.

```bash
mvn eu.stamp-project:reneri:diff
```

There is no need to modify the `pom.xml` file. It is mandatory, however,
to have executed Descartes before using the tool.

## How does Reneri work?

Reneri instruments all test classes in a Maven project to observe the
result for every non-constant expression (and sub-expression) in the
test code.

Then, the tool executes the test classes with the original application
code to obtain a ground truth. After that, the tool applies all undetected
extreme transformations reported by Descartes and check if there is a
difference with the ground truth.

The differences found can become hints to improve the assertions in the
test code.

To avoid flaky tests, the tool executes each test class several times, so
the analysis can be time-consuming.

## Funding

Descartes is partially funded by the research project STAMP (European Commission - H2020)

![STAMP - European Commission - H2020](https://github.com/STAMP-project/pitest-descartes/raw/master/docs/logo_readme_md.png)


