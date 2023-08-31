use std::io;

use clap::Parser;
use colored::*;
use houston::{
    config,
    generator,
    config::{ApiKey, StrictUserConfig},
    context::{ContextCall, does_default_ctxt_exist, read_and_evaluate_context_file_by_name},
    generator::create_chat_prompt,
    runner::SimpleScriptRunner,
};
use houston::generator::ScriptGenerator;
use houston::runner::ScriptRunner;


#[derive(Parser, Debug)]
#[command(author, version, about, long_about = None)]
struct CliOptions {
    #[arg(short, long, default_value_t = false, help = "Run the generated program without asking for confirmation")]
    force: bool,

    #[arg(short, long, default_value_t = false, help = "Don't run the generated program, just print it to stdout")]
    dry: bool,

    #[arg(short, long, default_value_t = false, help = "Print verbose output")]
    verbose: bool,

    // config overrides
    #[arg(short, long, help = "The shell to use to run the generated script")]
    shell: Option<String>,

    #[arg(long, help = "The shell to for evaluating context files")]
    context_shell: Option<String>,

    #[arg(short, long, help = "The OpenAI model to use")]
    model: Option<String>,
    // end config overrides
    #[arg(short, long, help = "Names of the context files to use")]
    context: Vec<String>,

    #[arg(help = "The instruction for the Assistant")]
    instruction: Vec<String>,
}

#[derive(Debug)]
struct Application {
    run_mode: config::RunMode,
    verbose: bool,
    shell: String,
    context_shell: String,
    model: String,
    context: Vec<String>,
    instruction: Vec<String>,
    open_ai_api_key: ApiKey,
}

impl Application {
    fn from(cli_options: CliOptions, user_config: StrictUserConfig) -> Self {
        Application {
            run_mode: if cli_options.force {
                config::RunMode::Force
            } else if cli_options.dry {
                if cli_options.force {
                    println!("Warning: --force and --dry are both set. --force will be ignored.");
                }
                config::RunMode::Dry
            } else {
                user_config.default_run_mode
            },
            verbose: cli_options.verbose,
            shell: cli_options.shell.unwrap_or(user_config.default_shell),
            context_shell: cli_options.context_shell.unwrap_or(user_config.default_context_shell),
            model: cli_options.model.unwrap_or(user_config.open_ai.model),
            context: cli_options.context,
            instruction: cli_options.instruction,
            open_ai_api_key: user_config.open_ai.api_key,
        }
    }
}

fn os_name() -> String {
    let os = std::env::consts::OS;
    os.to_string()
}

impl Application {
    fn print_verbose(&self, message: &str) {
        if self.verbose {
            println!("{}", message.bright_black());
        }
    }

    fn print_verbose_lazy<F>(&self, message: F) where F: FnOnce() -> String {
        if self.verbose {
            let message = message();
            self.print_verbose(&message);
        }
    }

    fn run(&self) {
        self.print_verbose(&format!("{:?}", self));

        let mut context_calls = self.context.iter().map(|s| ContextCall::parse(s)).collect::<Vec<ContextCall>>();

        self.print_verbose(&format!("Context calls: {:?}", context_calls));

        if does_default_ctxt_exist() {
            context_calls.push(ContextCall {
                name: "default".to_string(),
                args: vec![],
            });
        }

        let context_calls = context_calls;


        let mut instruction = self.instruction.join(" ");
        if instruction.is_empty() {
            instruction = "print Hello World".to_string();
        }

        println!("Generating script...");

        let os = os_name();


        let mut requirements = vec!["the script is meant to be run on a ".to_string() + &os + " machine"];


        for c in context_calls {
            let result = read_and_evaluate_context_file_by_name(&c.name, &self.context_shell, &c.args_as_str_vec());

            let content = result.unwrap_or_else(|err| {
                eprintln!("Failed to read context file {}: {}", c.name, err);
                std::process::exit(1);
            });

            requirements.push(content);
        }

        let spec = generator::ScriptSpecification {
            lang: self.shell.clone(),
            instruction,
            requirements,
        };

        let generator =
            generator::ChatGptScriptGenerator::new(
                self.open_ai_api_key.clone(),
                self.model.clone());

        self.print_verbose(&format!("Using generator: {:?}", generator));
        self.print_verbose("=== Prompt ===");
        self.print_verbose_lazy(|| format!("{}", create_chat_prompt(&spec)));


        let script = generator.generate(&spec);

        let do_run_script = match self.run_mode {
            config::RunMode::Ask => {
                print_script(&script);
                println!("Do you want to run this script? (y/n)");
                let mut input = String::new();
                io::stdin().read_line(&mut input).unwrap();
                input.trim().to_lowercase() == "y"
            }
            config::RunMode::Force => true,
            config::RunMode::Dry => {
                print_script(&script);
                false
            }
        };

        if do_run_script {
            let runner = SimpleScriptRunner::new(&self.shell);
            print!("Running script...");
            // start blue ansi color
            println!("\x1b[34m");

            let _ = runner.run_script(&script, &[], None);
            // reset color
            print!("\x1b[0m");
        } else if self.run_mode == config::RunMode::Ask {
            println!("Ok, see you later!");
        }
    }
}

fn print_script(script: &str) {
    println!("{}", "=".repeat(80).white());
    println!("{}", script.green());
    println!("{}", "=".repeat(80).white());
}

fn main() {
    let options = CliOptions::parse();
    config::create_user_config_if_not_exists();
    let application = Application::from(options, config::load_user_config_strict().unwrap());
    application.run();
}
