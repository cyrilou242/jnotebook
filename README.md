⚠️ I do not work on this project anymore. If you're interested in interactive programming, malleable code and notebooks for Java, I suggest to look at my new project: **[Javelit](https://github.com/javelit/javelit)**

# `jnotebook`
A modern notebook system for java.

[Book of jnotebook](https://jnotebook.catheu.tech/) - [Quickstart](#quickstart) - [Roadmap](https://github.com/cyrilou242/jnotebook/discussions/1)

`jnotebook` interprets Java JShell files and render them as notebook.

![readme_screenshot.png](./assets/readme_screenshot.png)

## Rationale

Computational notebooks allow arguing from evidence by mixing prose with executable code. For a good overview of problems users encounter in traditional notebooks like Jupyter, see [I don't like notebooks](https://www.youtube.com/watch?v=7jiPeIFXb6U) and [What’s Wrong with Computational Notebooks? Pain Points, Needs, and Design Opportunities](https://austinhenley.com/pubs/Chattopadhyay2020CHI_NotebookPainpoints.pdf).

Specifically `jnotebook` wants to address the following problems:

- notebook editors are less helpful than IDE editors
- notebook code is hard to reuse
- out-of-order execution causes reproducibility issues
- notebook code is hard to version control
- the Java ecosystem does not provide a great experience for visualization and document formatting

`jnotebook` is a notebook library for Java that aims to address these problems by doing less, namely:

- no editing environment: you can keep the code editor you know and love
- (almost) no new format: `jnotebook` interprets JShell files and renders them as notebook.
    Because `jnotebook` is not required to run JShell files, you can create JShell scripts interactively with jNotebook, you won't depend on jNotebook in production later.
- no out-of-order execution: jNotebook always evaluates from top to bottom. `jnotebook` builds a dependency graph of Java statements and only recomputes the needed changes to keep the feedback loop fast.
- cells outputs are interpreted as html. This gives access to great visualization libraries and standard html for formatting.

## Quickstart
`jnotebook` requires Java 17 or higher.

`jnotebook` is distributed in a single portable binary. Download it.
``` 
curl -Ls https://get.jnotebook.catheu.tech -o jnotebook
chmod +x jnotebook
```

Then launch.
```
# linux / mac os
./jnotebook server

# windows
java -jar jnotebook server
```

Go to http://localhost:5002.
By default, the notebook folder is `notebooks`. If it does not exist, it will be created with an example notebook.
`jnotebook` automatically detects when a `.jsh` file in the `notebooks` folder is edited
and renders it in the web app.
Once your notebook is ready to be published, render it in a single html file with:

```
./jnotebook render notebooks/my_notebook.jsh my_notebook.html
```


## Install
`jnotebook` requires Java 17 or higher.

### Portable installation
The `jnotebook` binary is a portable single file. Simply download it and get running. 

#### Mac Os/Linux
```
curl -Ls https://get.jnotebook.catheu.tech -o jnotebook
chmod +x jnotebook
./jnotebook server --help
```

#### Windows
```
curl -Ls https://get.jnotebook.catheu.tech -o jnotebook.jar
java -jar jnotebook.jar server --help
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
