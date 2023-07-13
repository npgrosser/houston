use std::fmt::{Display, Formatter};
use crate::config::ApiKey;
use openai_api_rust::*;
use openai_api_rust::chat::*;

pub struct ScriptSpecification {
    pub lang: String,
    pub instruction: String,
    pub requirements: Vec<String>,
}

pub trait ScriptGenerator {
    fn generate(&self, spec: &ScriptSpecification) -> String;
}


#[derive(Debug)]
pub struct ChatGptScriptGenerator {
    api_key: ApiKey,
    model: String,
}


impl ChatGptScriptGenerator {
    pub fn new(api_key: ApiKey, model: String) -> Self {
        ChatGptScriptGenerator {
            api_key,
            model,
        }
    }
}

#[derive(Debug)]
pub struct ChatPrompt {
    system_message: String,
    user_message: String,
}

impl Display for ChatPrompt {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "[System]\n{}\n[User]\n{}", self.system_message, self.user_message)
    }
}

impl ChatPrompt {
    fn to_messages(&self) -> Vec<Message> {
        vec![
            Message {
                role: Role::System,
                content: self.system_message.clone(),
            },
            Message {
                role: Role::User,
                content: self.user_message.clone(),
            },
        ]
    }
}

pub fn create_chat_prompt(spec: &ScriptSpecification) -> ChatPrompt {
    let lang = &spec.lang;
    let system_message = format!("You are a {0} script generator.\n\
    The user gives you a description/goal for a {0} script and sometimes a list of extra requirements or context.\n\
    You respond with the {0} script.\nNote that you only respond with the script, not additional explanation, no code block etc.\n\
    Your response must be a valid {0} script. So never use ''' to indicate the script start and end", lang);

    let mut user_message = spec.instruction.clone();

    if !spec.requirements.is_empty() {
        user_message.push_str("\n\nAdditional Requirements:\n");
        for requirement in &spec.requirements {
            if !requirement.is_empty() {
                user_message.push('\n');
                if !requirement.starts_with('-') {
                    user_message.push_str("- ")
                }
                user_message.push_str(requirement);
            }
        }
    }

    ChatPrompt {
        system_message,
        user_message,
    }
}


impl ScriptGenerator for ChatGptScriptGenerator {
    fn generate(&self, spec: &ScriptSpecification) -> String {
        let api_key = &self.api_key.0;

        let auth = Auth::new(api_key);
        let openai = OpenAI::new(auth, "https://api.openai.com/v1/");

        let prompt = create_chat_prompt(spec);

        let body = ChatBody {
            model: self.model.clone(),
            messages: prompt.to_messages(),
            temperature: Some(0.5),
            top_p: Some(1.0),
            n: Some(1),
            stream: None,
            stop: None,
            max_tokens: None,
            presence_penalty: None,
            frequency_penalty: None,
            logit_bias: None,
            user: None,
        };

        let response = openai.chat_completion_create(&body);
        let completion = response.unwrap();

        let choice = completion.choices.first().unwrap();
        choice.message.as_ref().unwrap().content.clone().trim().to_string()
    }
}