use std::io;
use crate::config::get_houston_dir;
use crate::runner::SimpleScriptRunner;
use crate::template::{DefaultTemplateEvaluator, TemplateEvaluator};


pub fn does_default_ctxt_exist() -> bool {
    get_context_path_by_name("default").exists()
}

pub fn read_and_evaluate_context_file_by_name(name: &str, shell: &str,
                                              args: &[&str],
) -> io::Result<String> {
    let template = read_context_file_by_name(name)?;
    let runner = SimpleScriptRunner::new(shell);
    let template_evaluator = DefaultTemplateEvaluator::new(Box::new(runner));
    let evaluated = template_evaluator.evaluate(&template, args,
    ).map_err(|e| {
        io::Error::new(
            io::ErrorKind::Other,
            format!("Failed to evaluate context template: {}", e),
        )
    })?;

    Ok(evaluated)
}

fn get_context_path_by_name(name: &str) -> std::path::PathBuf {
    let houston_dir = get_houston_dir();
    let file_name = name.to_string() + ".ctxt";
    houston_dir.join(file_name)
}

fn read_context_file_by_name(name: &str) -> io::Result<String> {
    let file_path = get_context_path_by_name(name);
    std::fs::read_to_string(file_path)
}


#[derive(Debug)]
pub struct ContextCall {
    pub name: String,
    pub args: Vec<String>,
}

impl ContextCall {
    pub fn args_as_str_vec(&self) -> Vec<&str> {
        self.args.iter().map(|s| s.as_str()).collect::<Vec<&str>>()
    }

    pub fn parse(input: &str) -> Self {
        let mut parts = input.split(':');
        let name = parts.next().unwrap().to_string();


        let rest = parts.collect::<Vec<&str>>();

        let args = if rest.is_empty() {
            vec![]
        } else {
            let joined = rest.join(":");
            joined.split(' ').map(|s| s.to_string()).collect()
        };

        ContextCall {
            name,
            args,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_context_call_parse_multiple_args() {
        let call = ContextCall::parse("foo:bar baz");
        assert_eq!(call.name, "foo");
        assert_eq!(call.args, vec!["bar", "baz"]);
    }

    #[test]
    fn test_context_call_parse_one_arg() {
        let call = ContextCall::parse("foo:bar");
        assert_eq!(call.name, "foo");
        assert_eq!(call.args, vec!["bar"]);
    }

    #[test]
    fn test_context_call_parse_no_args() {
        let call = ContextCall::parse("foo");
        assert_eq!(call.name, "foo");
        assert!(call.args.is_empty());
    }
}

