# Getting started and building

## Requirements

Building requires a JDK 21 or newer and Maven 3.9+. A Java runtime 21+ is sufficient to run the packaged CLI, its compiled examples, and generated applications. Compiling a newly generated `.java` file requires a JDK.

Check the tools selected by your shell:

```sh
java -version
javac -version
mvn -version
```

The Java version reported by Maven is the one used for the project build. When your shell has multiple Java installations, set `JAVA_HOME` and put its tools first on `PATH` so standalone `javac` uses the same JDK:

```sh
export JAVA_HOME="/path/to/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
```

On macOS, `export JAVA_HOME=$(/usr/libexec/java_home -v '21+')` selects an installed compatible JDK. Add its `bin` directory to `PATH` as above. On Windows Command Prompt:

```bat
set "JAVA_HOME=C:\path\to\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
```

Replace the example installation paths with your actual JDK directory.

## Build everything

From the repository root:

```sh
mvn clean verify
```

This builds the commons, lexer, parser, examples and CLI modules. Maven generates six example parsers and one DFA scanner, compiles their Java consumers, runs the tests, packages the executable CLI, and runs distribution integration tests.

The CLI appears in the repository's `bin/` directory:

```text
bin/
  lexengine          # executable POSIX launcher
  lexengine.cmd      # Windows launcher
  lex-engine.jar     # executable JAR containing generators and compiled examples
```

Run it with either launcher or Java directly:

```sh
./bin/lexengine --version
./bin/lexengine --help
java -jar bin/lex-engine.jar examples --list
```

Windows Command Prompt:

```bat
bin\lexengine.cmd --version
bin\lexengine.cmd examples --all
```

## Which Maven phases create which outputs?

| Command | Result |
| --- | --- |
| `mvn compile` | Generates and compiles the examples and project classes. |
| `mvn test` | Also runs unit tests; CLI packaging has not run yet. |
| `mvn package` | Also creates `bin/lexengine`, `bin/lexengine.cmd`, and `bin/lex-engine.jar`. |
| `mvn verify` | Also tests the packaged JAR and launcher after packaging. |
| `mvn install` | Performs verification and installs the module artifacts in the local Maven repository. |
| `mvn clean` | Removes module build outputs and the three generated CLI distribution files. |

`clean` preserves unrelated files placed in `bin/`. Generated Java sources live under `lexengine-examples/target/generated-sources/lexengine/`; edit the specifications under `src/main/resources/` and rebuild to change them.

## Use the dedicated CLI builder

The builder selects `lexengine-cli` and builds all of its reactor dependencies:

```sh
./scripts/build-cli.sh
./scripts/build-cli.sh clean
./scripts/build-cli.sh -DskipTests
```

Windows:

```bat
scripts\build-cli.cmd
scripts\build-cli.cmd clean
```

The scripts locate the repository relative to their own location, so you can invoke them from another directory using an absolute path. They require Maven on `PATH` and forward additional Maven arguments before the final `package` goal.

The equivalent direct command is:

```sh
mvn -pl lexengine-cli -am package
```

Use `verify` for distribution integration tests. `-DskipTests` skips execution of tests; use it after a successful verified build when you only need to rebuild artifacts.

## First example

```sh
./bin/lexengine examples arithmetic "12 + 3 * (7 - 4)"
```

The first output line is:

```text
Accepted arithmetic using LR1
```

The remaining lines display the labeled parse tree. Try all examples or load a multiline input file:

```sh
./bin/lexengine examples --all
./bin/lexengine examples json --input-file lexengine-examples/src/main/resources/samples/document.json
```

See [example walkthroughs](examples.md) for the grammars, Java consumers and invalid inputs to try.

## Generate and compile a parser

```sh
./bin/lexengine parser \
  --grammar lexengine-examples/src/main/resources/list.lg \
  --parser-type LL1 \
  --output-directory target/generated-demo

javac -encoding UTF-8 -d target/generated-demo/classes \
  target/generated-demo/ListParser.java
```

The package declared in `list.lg` is `org.lexengine.examples.generated`. Use the generated class from an application:

```java
import org.lexengine.examples.generated.ListParser;

var tree = new ListParser("[12, 34, 56]").parse();
System.out.println(tree.root().label()); // NumberList
```

The generated source depends only on the JDK. Your application does not need the generator JAR at runtime.

## Move the CLI distribution

Copy the three files in `bin/` together to another folder. The launchers find `lex-engine.jar` relative to themselves, and try `JAVA_HOME` before `java` on `PATH`. The POSIX launcher checks for Java 21+; on macOS, if the selected runtime is missing or too old, it discovers a compatible installed JDK with `java_home`. Other systems need a compatible runtime configured explicitly. They preserve the caller's current working directory, so relative grammar and input paths are resolved from that directory.

To place build outputs in another location:

```sh
mvn package -Dlexengine.distributionDirectory="/tmp/lex engine"
```

Use an absolute output path. The configured directory is also used by the CLI clean execution and integration tests. Generated distribution files are ignored by Git; launcher sources are tracked under `lexengine-cli/src/main/scripts/`.

## Build only the examples

```sh
mvn -pl lexengine-examples -am clean test
```

This generates and tests examples without packaging the CLI module. After compilation, run the example entry point with module class directories on the classpath:

```sh
LEX_CP="lexengine-examples/target/classes:lexengine-parser/target/classes:lexengine-lexer/target/classes:lexengine-commons/target/classes"
java -cp "$LEX_CP" org.lexengine.examples.Main list "[1, 2, 3]"
```

Windows classpaths use `;` as the separator. The packaged CLI avoids manual classpath configuration.
