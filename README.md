# Houston

Houston is a simple GPT-based command-line tool that allows you to generate shell commands or scripts by
giving simple, natural language instructions.   
It also supports providing context information about your system to improve the quality of the generated output.

Works on Linux, Mac and Windows.
You can use it for Bash, Powershell, Zsh, Python, or any other shell or scripting language.

## Requirements

- [Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
- [Cargo](https://doc.rust-lang.org/cargo/getting-started/installation.html)
- [OpenAI API Key](https://platform.openai.com)

## Installation

    git clone git@github.com:npgrosser/Houston.git
    cd Houston
    cargo install --path .

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
at https://platform.openai.com. If you prefer, you can also specify the key in your config file, see the Configuration
section for more information.

### Context

Houston does not know anything about your system. So, to give Houston the best chance of completing tasks,
you should provide it with the context information it needs to complete the task.
This may include details about the file tree, contents of specific files, installed packages, your bash history etc.   
For this purpose, there are some features that help you do this.

A context file is a file with a '.ctxt' extension that contains a list of information in natural language.  
You can also add dynamically generated information using
_[command variables](#user-content-dynamic-context-using-command-variables)_.

##### Default Context File

Information that you define in the  _~/houston/default.ctxt_ file is always provided to Houston.
This is the context file that you would use to let Houston know about your individual preferences and requirements.
For example, which package manager you prefer, what your favorite language is, what you like him to call you, etc.

The content of a context file should be written as a list of bullet points.

Example:

    - If I want you to install something, use apt-get if possible
    - If the script gets long, use comments to explain what you are doing
    - When printing to the console, use capital letters. I like it when you shout at me.

##### Named Context Files

Named context files are context files that you can enable on a per-instruction basis.
They are useful for providing context information that is specific to a certain task.
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

#### Dynamic Context using Command Variables

Context files can be plain text files, but they can also contain _command variables_.
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

If you want to see what is actually passed to the API, you can use the `-v` (verbose) flag.

    hu <instruction> -v

This is especially useful if you want to see how your context files are evaluated.

### Configuration

See _~/houston/config.yml_ file for available configuration options.
It is automatically created when you run `hu` for the first time.
