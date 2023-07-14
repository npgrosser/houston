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

impl TemplateEvaluator for DefaultTemplateEvaluator {
    /// finds all commands in the template and runs them
    /// commands are of the form ${command}
    fn evaluate(&self, template: &str, args: &[&str]) -> Result<String, TemplateEvaluationError> {
        let mut result = String::new();
        let mut remaining_text = template;
        loop {
            let open_index = remaining_text.find("${");
            match open_index {
                Some(open_index) => {
                    let close_index = find_closing_curly_bracket(remaining_text, open_index);
                    if close_index == -1 {
                        return Err(TemplateEvaluationError::SyntaxError(
                            "Missing closing curly bracket".to_string(),
                        ));
                    }

                    let cmd = &remaining_text[open_index + 2..close_index as usize];
                    let cmd_out = self.script_runner.run_script_and_get_stdout(
                        cmd, args).map_err(TemplateEvaluationError::IoError)?;

                    result.push_str(&remaining_text[..open_index]);
                    result.push_str(cmd_out.trim_end());
                    remaining_text = &remaining_text[close_index as usize + 1..];
                }
                None => {
                    result.push_str(remaining_text);
                    return Ok(result);
                }
            }
        }
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
    use crate::template::TemplateEvaluationError::{IoError, SyntaxError};
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
        pub fn new_from_vec(input_output_vec: Vec<(&str, &str)>) -> Self {
            let mut map = HashMap::new();
            for (input, output) in input_output_vec {
                map.insert(input.to_string(), output.to_string());
            }
            MockScriptRunner { input_output_map: map }
        }

        pub fn new_single_command(input: &str, output: &str) -> Self {
            let mut map = HashMap::new();
            map.insert(input.to_string(), output.to_string());
            MockScriptRunner { input_output_map: map }
        }

        pub fn new_no_commands() -> Self {
            MockScriptRunner { input_output_map: HashMap::new() }
        }
    }

    impl ScriptRunner for MockScriptRunner {
        fn run_script(&self, script: &str,
                      _args: &[&str],
                      _handle_stdout: Option<&mut dyn FnMut(&str)>) -> io::Result<()> {
            self.run_script_and_get_stdout(script, _args).map(|_| ())
        }

        fn run_script_and_get_stdout(&self, script: &str, _args: &[&str]) -> io::Result<String> {
            return match self.input_output_map.get(script) {
                Some(value) => Ok(value.to_string()),
                None => Err(io::Error::new(io::ErrorKind::NotFound, "not found")),
            };
        }
    }

    #[test]
    fn test_evaluate() {
        let script_runner = Box::new(MockScriptRunner::new_single_command(
            "echo world", "world"));
        let evaluator = DefaultTemplateEvaluator::new(script_runner);

        let result = evaluator.evaluate("hello ${echo world}", &[]);

        assert_eq!(result.unwrap(), "hello world");
    }

    #[test]
    fn test_evaluate_with_multiple_commands() {
        let script_runner = Box::new(MockScriptRunner::new_single_command(
            "echo world", "world"));

        let evaluator = DefaultTemplateEvaluator::new(script_runner);

        let result = evaluator.evaluate("hello ${echo world} ${echo world}", &[]);

        assert_eq!(result.unwrap(), "hello world world");
    }


    #[test]
    fn test_evaluate_line_with_multiple_commands_and_text() {
        let script_runner = Box::new(MockScriptRunner::new_single_command(
            "echo world", "world"));
        let evaluator = DefaultTemplateEvaluator::new(script_runner);

        let result = evaluator.evaluate("hello ${echo world} ${echo world} world", &[]);
        assert_eq!(result.unwrap(), "hello world world world");
    }

    #[test]
    fn test_evaluate_line_with_nested_commands() {
        let script_runner = Box::new(MockScriptRunner::new_single_command(
            "echo '${echo world}'", "${echo world}")
        );

        let evaluator = DefaultTemplateEvaluator::new(script_runner);

        let result = evaluator.evaluate("hello ${echo '${echo world}'}", &[]);
        assert_eq!(result.unwrap(), "hello ${echo world}");
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

        assert_eq!(result.unwrap(), "hello world\nmy name is john");
    }

    #[test]
    fn test_syntax_error() {
        let script_runner = Box::new(MockScriptRunner::new_single_command(
            "echo world", "world"));
        let evaluator = DefaultTemplateEvaluator::new(script_runner);

        let result = evaluator.evaluate("hello ${echo world", &[]);

        assert!(matches!(result, Err(SyntaxError(_))), "Expected SyntaxError, got {:?}", result);
    }

    #[test]
    fn test_commando_error() {
        let script_runner = Box::new(MockScriptRunner::new_no_commands());
        let evaluator = DefaultTemplateEvaluator::new(script_runner);

        let result = evaluator.evaluate("hello ${echo world} ${echo world} ${echo world}", &[]);

        assert!(matches!(result, Err(IoError(_))), "Expected IoError, got {:?}", result);
    }
}
