<p align="center">
  <img src="images/houston-transparent-enhanced.png" width="400">
</p>

Houston is an AI Assistant for your Terminal. He translates your instructions, written in plain English, into Bash (or
PowerShell)
scripts.
Furthermore, Houston can use additional context information to provide more accurate and relevant commands for
your needs.

## Installation

### Requirements

- Git (git binary must be in your PATH for the ez-install script to work)
- JDK 11 or higher
- OPENAI_API_KEY environment variable set to [your OpenAI API secret key](https://beta.openai.com/account/api-keys)

### Linux and Mac

#### Install

    curl https://raw.githubusercontent.com/npgrosser/Houston/master/scripts/ez-install.sh | sh

#### Uninstall

    curl https://raw.githubusercontent.com/npgrosser/Houston/master/scripts/uninstall.sh | sh

### Windows

#### Install

    powershell -Command "& { (New-Object Net.WebClient).DownloadString('https://raw.githubusercontent.com/npgrosser/Houston/master/scripts/ez-install.ps1') | Invoke-Expression }"

Add the bin directory to your PATH environment variable. The script will tell you where it is located.

#### Uninstall

    powershell -Command "& { (New-Object Net.WebClient).DownloadString('https://raw.githubusercontent.com/npgrosser/Houston/master/scripts/uninstall.ps1') | Invoke-Expression }"

## Usage

_hu_ (/ˈhju:/) is Houston's nickname and the default command for working with him. It allows you to give Houston
an instruction in natural language.

    hu <instruction>

See `hu ---help` for all available options.

#### Examples:

    hu tell me a dad joke

    hu write a threejs example to index.html and open it in the browser
  
    hu run ubuntu container with interactive bash

### Context

Context is important.
Depending on what you want Houston to do, you should give him as much information as possible to do the task.
This could be the file tree, the type of the project in the current working directory, the contents of specific files, a
list of installed
packages, etc.
The easiest way to provide that information is to just type it as part of your instruction.
But depending on what information you want to provide, this might be cumbersome.
That's why there are some alternatives that are meant to make your life easier.

Note: Some information are so regularly relevant that they are always provided to Houston.
This includes the OS name, version, architecture etc.

#### Context Flags

The easiest way to provide context information is to make use of one of the predefined context flags:

```
Options:
  -p          Provide info about installed packages as context information
              (support limited to brew atm)
  -f PATH     Provide file name and its content as context information
  -t          Provide current file tree as context information
  -td INT     Maximum depth of the file tree (unlimited if not explicitly set)
  -r TEXT     Run the command and provide the output as context information
```

See `hu ---help` for all available options.

#### Examples using context flags:

    hu check which of the project requirements are not meat by my system -f README.md

    hu build and install the project -f README.md

    hu write a rap song about Houston -f README.md

    hu tell me what kind of project this is -t -td 1

#### Context Files

Another, more powerful and reusable way to provide context information is to use _context files_.
You can define directory specific _context files_ or global context files to describe additional requirements or
information in plain
english.
These will automatically be added as context information for every request.
Supported file names for directory specific context files are 'houston.ctxt' and '.houston'.
You can use both of them in the same directory.
_Per convention, the '.houston' file is meant for local use only and should not be committed to version control._

For security reasons, you have to explicitly trust working directories by adding them to the
~/houston/trusted_dirs file (using glob patterns).

Example content of ~/houston/trusted_dirs:

    /home/user/my-name
    /home/user/my-name/my-projects
    /home/user/my-name/my-projects/**

__

Example houston.txt file content

    - When printing to the terminal, always use capital letters and exclamation marks. 
      This is the only way to get my attention.

Think twice before adding a directory to the trusted_dirs file!
Arbitrary commands could be executed just by talking to Houston in that directory.

When a working directory is not trusted, the context files in it will just be ignored and a warning will be printed.
Besides that, you can still use Houston as usual in that directory.

#### Global context files

You can also create a _~/houston/default.ctxt_ file which will always be used, no matter what directory you
are in. It does not need to be trusted explicitly.
This is the context file that you would use to let Houston know about your individual preferences and requirements.
For example, which package manager you prefer, what your favorite language is, what you like him to call you, etc.
Use it to fine tune Houston to your needs.

Similar to the default.ctxt file, you can create arbitrary default files for specific contexts in the ~/houston/
directory.
The only difference to the default.ctxt file is that you have to explicitly enable them for each request by using the -c
flag.
You can use as many custom ctxt files as you want per request.

Example:

~/houston/pretty-output.ctxt:

    - when printing to the terminal, always use capital letters and colored output.

~/houston/maven.ctxt:

    - this working directory is a maven project

Usage:

    hu add jackson as a dependency -c maven -c pretty-output

#### Dynamic Context using Command Variables

_Context files_ also support command variables, which allow you to add dynamic data.
You can make use of them by using the following syntax: ${cmd}.
These command variables will be evaluated on each request.

Here is an example of a Context File using command variables:

    - The current working directory is ${pwd}.
    - The current user is ${whoami}.
    - The current time is ${date}.

When this template is processed, the resulting output would look similar to this:

    - The current working directory is /home/user/nico/houston.
    - The current user is nico.
    - The current time is 2020-10-10 12:00:00.

### Configuration

See _~/houston/config.yml_ file for available configuration options.
It is automatically created when you run `hu` for the first time.

---

### Tips

- Huston does not care about your system as much as you do. But he is kind enough to show you the script before
  running it. You should always check it!
- Houston is a terminal assistant and not a chatbot. When talking to him, use the imperative form (e.g. "tell me what
  time it is" instead of "what time is it?").
- Houston is not perfect. If he doesn't understand you, try to rephrase your request. Often it is enough to just append
  a few more details.
- Keep your context files small. The GPT-3 model is limited in the number of tokens per request. Try to only add the
  information
  that is really necessary. Especially the -c flag feature is meant to help you with this by providing a way to choose
  the relevant context information for each request.
- You can also use command variables to embed context files in other context files.
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
