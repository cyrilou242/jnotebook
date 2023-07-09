# jNotebook

## Live notebooks for Java.   

[Book of jNotebook](https://jnotebook.catheu.tech/) - [Quickstart](#quickstart) - [Roadmap](https://github.com/cyrilou242/jnotebook/discussions/1)

jNotebook interprets Java JShell files and render them as notebook.

![readme_screenshot.png](./assets/readme_screenshot.png)

## Rationale

Computational notebooks allow arguing from evidence by mixing prose with executable code. For a good overview of problems users encounter in traditional notebooks like Jupyter, see [I don't like notebooks](https://www.youtube.com/watch?v=7jiPeIFXb6U) and [What’s Wrong with Computational Notebooks? Pain Points, Needs, and Design Opportunities](https://austinhenley.com/pubs/Chattopadhyay2020CHI_NotebookPainpoints.pdf).

Specifically jNotebook wants to address the following problems:

- notebook editors are less helpful than IDE editors
- notebook code is hard to reuse
- out-of-order execution causes reproducibility issues
- notebook code is hard to version control
- the Java ecosystem does not provide a great experience for visualization and document formatting

jNotebook is a notebook library for Java that aims to address these problems by doing less, namely:

- no editing environment: you can keep the code editor you know and love
- (almost) no new format: jNotebook interprets JShell files and renders them as notebook.
    Because jNotebook is not required to run JShell files, you can create JShell scripts interactively with jNotebook, you won't depend on jNotebook in production later.
- no out-of-order execution: jNotebook always evaluates from top to bottom. jNotebook builds a dependency graph of Java statements and only recomputes the needed changes to keep the feedback loop fast.
- cells outputs are interpreted as html. This gives access to great visualization libraries and standard html for formatting.

## Quickstart
`jnotebook` requires Java 17 or higher.

Download a portable single file binary.
``` 
curl -Ls https://repo1.maven.org/maven2/tech/catheu/jnotebook-distribution/0.6.0/jnotebook-distribution-0.6.0.jar -o jnotebook
chmod +x jnotebook
```

Then launch.
```
./jnotebook server
```

Go to http://localhost:5002.
By default, the notebook folder is `notebooks`. If it does not exist, it will be created with an example notebook.


## Installation
`jnotebook` requires Java 17 or higher.

### Portable installation
The `jnotebook` binary is a portable single file. Simply download it and get running. 

#### Mac Os/Linux
```
curl -Ls https://repo1.maven.org/maven2/tech/catheu/jnotebook-distribution/0.6.0/jnotebook-distribution-0.6.0.jar -o jnotebook
chmod +x jnotebook
./jnotebook server --help
```

#### Windows
```
curl -L https://repo1.maven.org/maven2/tech/catheu/jnotebook-distribution/0.6.0/jnotebook-distribution-0.6.0.jar -o jnotebook.bat
icacls jnotebook.bat /grant Everyone:F
jnotebook server --help
```

#### Optional - Put the binary in your path
To have jnotebook always available in your terminal, make it available in your path.
You can move it in a standard folder (requires root privilege)
```
# Mac OS
mv jnotebook /usr/local/bin/git

# Linux 
mv jnotebook /usr/local/bin/jnotebook
```

Or put the binary in a folder and add the folder to your PATH.
```
mkdir ~/jnotebook/bin && mv jnotebook ~jnotebook/bin/jnotebook
# Linux
echo 'export PATH="/path/to/folder:$PATH"' >> ~/.bashrc && source ~/.bashrc
# Mac Os
echo 'export PATH="/path/to/folder:$PATH"' >> ~/.bash_profile && source ~/.bash_profile

Windows
@echo off
set "folder=C:\path\to\folder"
setx PATH "%PATH%;%folder%"
```
