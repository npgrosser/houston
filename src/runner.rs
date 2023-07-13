use std::io;
use std::process::Command;
use std::io::{BufRead, BufReader};

use crate::tmp_file::SharableTmpFile;


pub trait ScriptRunner {
    fn run_script(&self, script: &str,
                  args: &[&str],
                  handle_stdout: Option<&mut dyn FnMut(&str)>) -> io::Result<()>;

    fn run_script_and_get_stdout(&self, script: &str, args: &[&str]) -> io::Result<String> {
        let mut stdout = String::new();
        let mut handle_stdout = |line: &str| {
            stdout.push_str(line);
            stdout.push('\n');
        };
        self.run_script(script, args, Some(&mut handle_stdout))?;
        Ok(stdout)
    }
}

pub struct SimpleScriptRunner {
    shell: String,
}


impl SimpleScriptRunner {
    pub fn new(program: &str) -> Self {
        SimpleScriptRunner {
            shell: program.to_string()
        }
    }
}

impl ScriptRunner for SimpleScriptRunner {
    fn run_script(&self, script: &str, args: &[&str], handle_stdout: Option<&mut dyn FnMut(&str)>) -> io::Result<()> {
        let mut command = Command::new(&self.shell);


        let suffix = if self.shell.to_lowercase().contains("powershell") {
            ".ps1"
        } else {
            ""
        };

        let tmp_file = SharableTmpFile::new(script, suffix)?;

        command.arg(tmp_file.path.to_str().unwrap());
        command.args(args);

        let process = if let Some(handle_stdout) = handle_stdout {
            command.stdout(std::process::Stdio::piped());
            let mut child = command.spawn()?;
            let stdout = child.stdout.take().unwrap();
            let reader = BufReader::new(stdout);

            for line in reader.lines() {
                match line {
                    Ok(line) => handle_stdout(&line),
                    e => {
                        return Err(io::Error::new(
                            io::ErrorKind::Other,
                            format!("Failed to read stdout: {:?}", e),
                        ));
                    }
                }
            }
            child
        } else {
            command.spawn()?
        };

        process.wait_with_output()?;
        Ok(())
    }
}


#[cfg(test)]
mod tests {
    use crate::runner::{ScriptRunner, SimpleScriptRunner};

    #[test]
    fn test_shell_script_runner_with_args() {
        let args = vec!["hello", "world"];

        let result = if cfg!(windows) {
            let runner = SimpleScriptRunner::new("powershell");
            runner.run_script_and_get_stdout("Write-Host $args", &args)
        } else {
            let runner = SimpleScriptRunner::new("bash");
            runner.run_script_and_get_stdout("echo $1 $2", &args)
        };

        assert_eq!(result.unwrap().trim(), "hello world");
    }
}
