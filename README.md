# Notebook

Notebook for Java.   
Intepret jshell files.

## How to run

./mvnw compile exec:java -Dexec.mainClass="ai.catheu.notebook.Main"


## run with dependencies in the class-path

Use `--class-path` or `-cp` like with `java`.

### Use maven dependencies
Make sure your project is built with `mvn install`.
If there is a `pom.xml` file where Notebook is launched, Notebook tries to add the maven runtime dependencies to the classpath.

For more fined grained control, use the following: 
```
java -jar notebook.jar --class-path $(./mvnw -q exec:exec -pl 'notebook-core' -Dexec.executable=echo -Dexec.args="%classpath")
```
Use `-pl` to control which module classpath you want. Use ... to include test dependencies.


### Use gradle dependencies
This is undocumented. Please contribute!

## Developer
Set the log level to `DEBUG`: 
```
-Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```
