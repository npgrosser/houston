<p align="center">
  <img src="images/houston-transparent-enhanced.png" width="400">
</p>

Houston is an AI Assistant and Script Generator for your Terminal that allows you to accomplish a wide range of tasks by
giving simple, natural language instructions. It is [context aware](#user-content-context) and can translate your plain
English instructions into
simple scripts or shell commands, while giving you the option to review it before it is executed.
On Linux and Mac, the default scripting language is Bash, and on Windows, it is PowerShell.

## Installation

### Requirements

- Git (git binary must be in your PATH for the ez-install script to work)
- JDK 11 or higher

### Linux and Mac

    curl https://raw.githubusercontent.com/npgrosser/Houston/master/scripts/ez-install.sh | sh

### Windows

    powershell -Command "& { (New-Object Net.WebClient).DownloadString('https://raw.githubusercontent.com/npgrosser/Houston/master/scripts/ez-install.ps1') | Invoke-Expression }"

## Usage

The default command for interacting with Houston is `hu` (short for Houston).
You can give Houston an instruction in natural language.

For example:

- `hu run ubuntu container with interactive bash`
- `hu find all pdf files in my home directory`
- `hu delete unused docker images and networks`
- `hu tell me a dad joke`

You can also pass command line arguments to provide context or configure the behavior of Houston. See the rest of the
readme for more information.

To use Houston, the OPENAI_API_KEY environment variable must be set to your OpenAI API key. You can get one
at https://beta.openai.com. If you prefer, you can also specify the key in your config file, see the Configuration
section for more information.

### Context

To give Houston the best chance of completing tasks, it's important to provide relevant context.  
This may include details about the file tree, contents of specific files, installed packages, your bash history etc.   
For this purpose, there are some features that help you do this.

#### Context Flags

The easiest way to provide context is to make use of one of the predefined context flags:

```
Options:
  -f PATH     Provide file name and its content as context information
  -t          Provide current file tree as context information
  -td INT     Maximum depth of the file tree (unlimited if not explicitly set)
  -r TEXT     Run the command and provide the output as context information
```

See `hu ---help` for all available options.

##### Examples

- `hu check which of the project requirements are not met by my system -f README.md`
- `hu build and install the project -f README.md`
- `hu write a rap song about Houston -f README.md`
- `hu tell me what kind of project this is -t -td 1`

#### Context Files

Another, more powerful and generic way to provide context information is to use _context files_.
Theoretically, you could also use them to mimic the functionality of the context flags.
A context file is a file with a '.ctxt' extension that contains a list of information in natural language.  
You can also add dynamically generated information using
_[command variables](#user-content-dynamic-context-using-command-variables)_.

##### Default Context File

Information that you define in the  _~/houston/default.ctxt_ file is always provided to Houston.
This is the context file that you would use to let Houston know about your individual preferences and requirements.
For example, which package manager you prefer, what your favorite language is, what you like him to call you, etc.
Use it to fine tune Houston to your needs.

Example:

    - If I want you to install something, use apt-get if the package is available there
    - If the script gets long, use comments to explain what you are doing
    - When printing to the console, use capital letters. I like it when you shout at me

##### Named Context Files

Named context files are context files that you can enable on a per-instruction basis.
They are useful for providing context information that is specific to a certain task.
You can see them as a way to easily define your own context flags.
Like the default context file, they are located in the _~/houston_ directory.
You enable them by using the `-c` flag.

    hu <instruction> -c <context-file-name>

Example:

_~/houston/pretty-output.ctxt_

    - when printing to the terminal, use colored output and fancy ASCII art

Usage:

    hu print hello world -c pretty-output

Per instruction you can enable multiple context files.

    hu <instruction> -c <context-file-name-1> -c <context-file-name-2>

##### Directory Context Files

Directory context files are context files that are located in a directory and are automatically enabled for all
instructions given in that directory or any of its subdirectories.
Both 'houston.ctxt' and '.houston' are valid names for directory specific context files.

For security reasons, you have to explicitly trust directories to allow Houston to use their context files.
You do this by adding the directory to the _~/houston/trusted_dirs_ file (using glob patterns).

Example content of _~/houston/trusted_dirs_:

    /home/user/my-name
    /home/user/my-name/my-projects
    /home/user/my-name/my-projects/**

Example content of _/home/user/my-name/my-projects/houston.ctxt_:

    - I prefer to use Maven as my build tool for Java and Kotlin projects
    - If it's between 10pm and 6am, I want you to tell me to stop working

#### Dynamic Context using Command Variables

Context files are not just simple text files.
They are templates that can be filled with dynamic information using _command variables_.
You can make use of them by using the `${cmd}` syntax.
These command variables are evaluated on each instruction and replaced with the output of the command.

Here is an example of a Context File using command variables:

    - The current working directory is ${pwd}.
    - The current user is ${whoami}.
    - The current time is ${date}.

When this template is processed, the resulting output would look similar to this:

    - The current working directory is /home/user/my-name/houston.
    - The current user is my-name.
    - The current time is 2020-10-10 12:00:00.

When writing your own context files, keep in mind that all the data will be sent to OpenAI.
Therefore, you should only use commands that do not reveal sensitive information.

##### Using arguments in context files

When using a named context via the -c flag, you can also pass arguments.
For example:

    hu tell me a joke -c lang:german

Arguments are passed to the template, where you can access it as you would do with any normal shell script.
Example:

~/houston/lang.ctxt:

    - When printing to the terminal, always use the ${echo $1} language.

_Keep in mind that we need to use the echo command here because command variables are replaced with the output of the
command_

If you want to pass multiple arguments, you just need to make sure to add quotes around the _context spec_.

    hu tell me a joke -c "langs:german english french"

---

_Note that the above example assumes that Bash is used as the shell to evaluate the command variables._     
_When using a different shell, you might need to use a different syntax to access the arguments._   
_For example, in Powershell, you would use the $args variable. Also, you would not need to use the echo command._

    - When printing to the terminal, always use the ${$args[0]} language.

##### Examples

Here are a few more examples of named context files to give you an idea of how they can be used.

###### git.ctx

A context file that adds the current git status as context information.

    - The current git status is:
    ```
    ${git status}
    ```

###### docker.ctx

A context file that adds the current docker status as context information.

    - The current docker status is:
    ```
    ${docker ps -a}
    ```
    - The current docker images are:
    ```
    ${docker images}
    ```

###### history.ctx

A context file that adds the last n (default 10) commands from your bash history as context information.

    - The last ${1:-10} commands I ran were:
    ```
    ${tail -n ${1:-10} ~/.bash_history | cut -d ';' -f 2-}
    ```

###### env.ctx

A context file that adds the current environment variables as context information.

    - The current environment variables are:
    ```
    ${env}
    ```

###### file-tree.ctx

A context file that adds the current file tree as context information.

    - The current file tree is:
    ```
    ${tree -L ${1:-3}}
    ```

### Debugging

If you want to see what is actually passed to the API, you can use the `--debug` flag.    
This is especially useful if you want to see how your context files are evaluated.

### Configuration

See _~/houston/config.yml_ file for available configuration options.
It is automatically created when you run `hu` for the first time.

---

### Tips

- Keep your context files small. The GPT-3 models are limited in the number of tokens per request. Try to only add the
  information
  that is really necessary. Especially the -c flag feature is meant to help you with this by providing a way to choose
  the relevant context information for each request.
- You can use command variables to embed context files in other context files.
  E.g. (`${cat ~/houston/maven.ctxt && cat ~/houston/git.ctxt}`)
- If you use Houston a lot, you might want to check https://beta.openai.com/account/usage from time to time.
- Tools like [zsh-autosuggestions](https://github.com/zsh-users/zsh-autosuggestions)
  or [PSReadLine](https://devblogs.microsoft.com/powershell/psreadline-2-2-6-enables-predictive-intellisense-by-default/)
  will make it easier for you to work with Houston.

---

## Related Projects

Here are some other projects that offer similar functionality or might be a good addition to Houston:

- [plz-cli](https://github.com/m1guelpf/plz-cli/): Another GPT-3 based "Copilot for your terminal" written in Rust.
- [howdoi](https://github.com/gleitz/howdoi): A command line tool for instant coding answers.
- [socli](https://github.com/gautamkrishnar/socli): A Stack Overflow command line client.
- [explainshell](https://explainshell.com/): A web based tool to explain shell commands.
- [cheat.sh](https://github.com/chubin/cheat.sh): A command line tool for viewing technical cheat sheets.
