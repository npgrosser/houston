use std::fmt::Display;
use std::{fmt, io};
use crate::runner::{ScriptRunner};

pub trait TemplateEvaluator {
    fn evaluate(&self, template: &str, args: &[&str],
    ) -> Result<String, TemplateEvaluationError>;
}

#[derive(Debug)]
pub enum TemplateEvaluationError {
    SyntaxError(String),
    IoError(io::Error),
}

pub struct DefaultTemplateEvaluator {
    script_runner: Box<dyn ScriptRunner>,
}

impl DefaultTemplateEvaluator {
    pub fn new(script_runner: Box<dyn ScriptRunner>) -> Self {
        DefaultTemplateEvaluator { script_runner }
    }
}

impl Display for TemplateEvaluationError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            TemplateEvaluationError::SyntaxError(msg) => {
                write!(f, "Syntax error: {}", msg)
            }
            TemplateEvaluationError::IoError(e) => {
                write!(f, "IO error: {}", e)
            }
        }
    }
}

impl DefaultTemplateEvaluator {
    fn evaluate_line(&self,
                     template_line: &str,
                     args: &[&str],
    ) -> Result<String, TemplateEvaluationError> {
        let mut result = template_line.to_string();
        let mut start_index = result.find("${").map_or(-1, |i| i as isize);

        while start_index != -1 {
            let end_index = find_closing_curly_bracket(&result, start_index as usize);
            if end_index == -1 {
                return Err(TemplateEvaluationError::SyntaxError(
                    "Missing closing curly bracket".to_string(),
                ));
            }

            let cmd = &result[start_index as usize + 2..end_index as usize];
            let command_result = self.script_runner.run_script_and_get_stdout(cmd, args).map_err(|e| {
                TemplateEvaluationError::IoError(e)
            })?.trim().to_string();


            // todo: better infinite loop protection
            //  command result should be allowed to contain "${"
            //  just need to improve how the next start_index is found
            if command_result.contains("${") {
                return Err(TemplateEvaluationError::IoError(
                    io::Error::new(io::ErrorKind::Other,
                                   "Command result cannot contain ${".to_string())
                ));
            }

            result = result.replace(&result[start_index as usize..end_index as usize + 1], &command_result);
            start_index = result.find("${").map_or(-1, |i| i as isize);
        }

        Ok(result)
    }
}

impl TemplateEvaluator for DefaultTemplateEvaluator {
    /// finds all commands in the template and runs them
    /// commands are enclosed in as ${command}
    fn evaluate(&self, template: &str,
                args: &[&str],
    ) -> Result<String, TemplateEvaluationError> {
        let lines = template.lines();

        let mut result = String::new();

        for line in lines {
            let evaluated_line = self.evaluate_line(line, args)?;
            result.push_str(&evaluated_line);
            result.push('\n');
        };

        Ok(result)
    }
}

fn find_closing_curly_bracket(text: &str, opening_bracket_index: usize) -> isize {
    find_closing_bracket(text, opening_bracket_index, '{', '}')
}

fn find_closing_bracket(
    text: &str,
    opening_bracket_index: usize,
    opening: char,
    closing: char,
) -> isize {
    let mut bracket_count = 0;

    for (i, c) in text[opening_bracket_index..].char_indices() {
        if c == opening {
            bracket_count += 1;
        } else if c == closing {
            bracket_count -= 1;
            if bracket_count == 0 {
                return (i + opening_bracket_index) as isize;
            }
        }
    }

    -1
}

#[cfg(test)]
mod tests {
    use std::collections::HashMap;
    use super::*;


    #[test]
    fn test_find_closing_bracket() {
        let text = "hello {world}";
        let result = find_closing_bracket(text, 6, '{', '}');
        assert_eq!(result, 12);
    }

    #[test]
    fn test_find_closing_bracket_nested() {
        let text = "hello {world {nested}}";
        let result = find_closing_bracket(text, 6, '{', '}');
        assert_eq!(result, 21);
    }

    #[test]
    fn test_find_closing_bracket_with_multiple_brackets() {
        let text = "hello {world {nested}} {world {nested}}";
        let result = find_closing_bracket(text, 6, '{', '}');
        assert_eq!(result, 21);

        let result = find_closing_bracket(text, 23, '{', '}');
        assert_eq!(result, 38);

        let result = find_closing_bracket(text, 13, '{', '}');
        assert_eq!(result, 20);
    }

    struct MockScriptRunner {
        pub input_output_map: HashMap<String, String>,
    }

    impl MockScriptRunner {
        pub fn new(input_output_map: HashMap<String, String>) -> Self {
            MockScriptRunner { input_output_map }
        }

        pub fn new_from_vec(input_output_vec: Vec<(&str, &str)>) -> Self {
            let mut map = HashMap::new();
            for (input, output) in input_output_vec {
                map.insert(input.to_string(), output.to_string());
            }
            MockScriptRunner::new(map)
        }

        pub fn new_single_command(input: &str, output: &str) -> Self {
            let mut map = HashMap::new();
            map.insert(input.to_string(), output.to_string());
            MockScriptRunner { input_output_map: map }
        }
    }

    impl ScriptRunner for MockScriptRunner {
        fn run_script(&self, script: &str,
                      _args: &[&str],
                      _handle_stdout: Option<&mut dyn FnMut(&str)>) -> io::Result<()> {
            assert!(self.input_output_map.get(script).is_some());
            Ok(())
        }

        fn run_script_and_get_stdout(&self, script: &str, _args: &[&str]) -> io::Result<String> {
            let result = self.input_output_map.get(script).unwrap();
            Ok(result.to_string())
        }
    }

    #[test]
    fn test_evaluate_line() {
        let script_runner = Box::new(MockScriptRunner::new_single_command(
            "echo world", "world"));
        let evaluator = DefaultTemplateEvaluator::new(script_runner);

        let result = evaluator.evaluate_line("hello ${echo world}", &[]);

        assert_eq!(result.unwrap(), "hello world");
    }

    #[test]
    fn test_evaluate_line_with_multiple_commands() {
        let script_runner = Box::new(MockScriptRunner::new_single_command(
            "echo world", "world"));

        let evaluator = DefaultTemplateEvaluator::new(script_runner);

        let result = evaluator.evaluate_line("hello ${echo world} ${echo world}", &[]);

        assert_eq!(result.unwrap(), "hello world world");
    }


    #[test]
    fn test_evaluate_line_with_multiple_commands_and_text() {
        let script_runner = Box::new(MockScriptRunner::new_single_command(
            "echo world", "world"));
        let evaluator = DefaultTemplateEvaluator::new(script_runner);

        let result = evaluator.evaluate_line("hello ${echo world} ${echo world} world", &[]);
        assert_eq!(result.unwrap(), "hello world world world");
    }

    #[test]
    fn test_evaluate_line_with_inner_brackets() {
        let script_runner = Box::new(MockScriptRunner::new_single_command(
            "echo ${echo world}", "echo world") // todo: update when inner ${} is supported
        );

        let evaluator = DefaultTemplateEvaluator::new(script_runner);

        let result = evaluator.evaluate_line("hello ${echo ${echo world}}", &[]);
        println!("{:?}", result);
        assert_eq!(result.unwrap(), "hello echo world");
    }


    #[test]
    fn test_multi_line_template() {
        let script_runner = Box::new(MockScriptRunner::new_from_vec(
            vec![
                ("echo world", "world"),
                ("get name", "john"),
            ]
        ));
        let evaluator = DefaultTemplateEvaluator::new(script_runner);

        let result = evaluator.evaluate(
            "hello ${echo world}\nmy name is ${get name}", &[]);

        assert_eq!(result.unwrap(), "hello world\nmy name is john\n");
    }
}
