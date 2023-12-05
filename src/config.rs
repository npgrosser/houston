use std::fmt;
use std::fs::File;
use serde::{Deserialize, Serialize};


const DEFAULT_CHAT_MODEL: &str = "gpt-4";
const CONFIG_DIR_NAME: &str = "houston";
const CONFIG_FILE_NAME: &str = "config.yml";

#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub enum RunMode {
    Force,
    Ask,
    Dry,
}

#[derive(Clone)]
pub struct ApiKey(pub String);

impl Serialize for ApiKey {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
        where S: serde::Serializer
    {
        serializer.serialize_str(&self.0)
    }
}

impl<'de> Deserialize<'de> for ApiKey {
    fn deserialize<D>(deserializer: D) -> Result<ApiKey, D::Error>
        where D: serde::Deserializer<'de>
    {
        let s = String::deserialize(deserializer)?;
        Ok(ApiKey(s))
    }
}

impl fmt::Debug for ApiKey {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        fn obfuscate_api_key(api_key: &str) -> String {
            let mut secret_api_key = api_key.to_string();
            secret_api_key.replace_range(6.., "****");
            secret_api_key
        }

        let short_api_key = obfuscate_api_key(&self.0);
        f.debug_tuple("ApiKey")
            .field(&short_api_key)
            .finish()
    }
}


#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "camelCase", default)]
pub struct OpenAiConfig {
    api_key: Option<ApiKey>,
    pub(crate) model: String,
}

impl Default for OpenAiConfig {
    fn default() -> Self {
        OpenAiConfig {
            api_key: None,
            model: DEFAULT_CHAT_MODEL.to_string(),
        }
    }
}


#[derive(Debug, Serialize, Deserialize)]
pub struct StrictOpenAiConfig {
    pub api_key: ApiKey,
    pub model: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct StrictUserConfig {
    pub default_shell: String,
    pub default_context_shell: String,
    pub default_run_mode: RunMode,
    pub open_ai: StrictOpenAiConfig,
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "camelCase", default)]
pub struct UserConfig {
    pub(crate) default_shell: Option<String>,
    pub(crate) default_context_shell: Option<String>,
    pub(crate) default_run_mode: RunMode,
    open_ai: OpenAiConfig,
}

impl Default for UserConfig {
    fn default() -> Self {
        UserConfig {
            default_shell: None,
            default_context_shell: None,
            default_run_mode: RunMode::Ask,
            open_ai: OpenAiConfig::default(),
        }
    }
}

impl UserConfig {
    /// Convert the user config into a strict version that can be used by the rest of the program.
    /// Strict means that all the required fields that are not set in the user config are resolved
    /// either from environment variables or from the system.
    ///
    /// The fields that are resolved if not present are:
    /// - default_shell
    /// - default_context_shell
    /// - open_ai.api_key
    fn to_strict(&self) -> Result<StrictUserConfig, String> {
        // load open ai key from env var if not present in config
        let api_key = match &self.open_ai.api_key {
            Some(api_key) => api_key.0.clone(),
            None => {
                let open_ai_key = std::env::var("OPENAI_API_KEY").ok();
                if open_ai_key.is_none() {
                    return Err("OPENAI_API_KEY is not set".to_string());
                }
                open_ai_key.unwrap()
            }
        };

        let open_ai = StrictOpenAiConfig {
            api_key: ApiKey(api_key),
            model: self.open_ai.model.clone(),
        };

        let default_shell = match &self.default_shell {
            Some(shell) => shell.clone(),
            None => get_default_shell_for_system(),
        };

        let default_context_shell = match &self.default_context_shell {
            Some(shell) => shell.clone(),
            None => get_default_shell_for_system(),
        };


        Ok(StrictUserConfig {
            default_shell,
            default_context_shell,
            default_run_mode: self.default_run_mode.clone(),
            open_ai,
        })
    }

    fn default_for_system() -> Self {
        let shell = get_default_shell_for_system();
        UserConfig {
            default_shell: Some(shell.clone()),
            default_context_shell: Some(shell),
            default_run_mode: RunMode::Ask,
            open_ai: OpenAiConfig::default(),
        }
    }
}

fn get_default_shell_for_system() -> String {
    let os = std::env::consts::OS;
    match os {
        "windows" => "powershell".to_string(),
        _ => "bash".to_string(),
    }
}

pub fn get_houston_dir() -> std::path::PathBuf {
    // Use the XDG_CONFIG_HOME environment variable if it's set, otherwise fallback to $HOME/.config
    let xdg_config_home = std::env::var("XDG_CONFIG_HOME")
        .ok()
        .map(std::path::PathBuf::from)
        .unwrap_or_else(|| {
            dirs::home_dir().expect("Home directory not found").join(".config")
        });

    xdg_config_home.join(CONFIG_DIR_NAME)
}

/// load user config from the default config file location
pub fn load_user_config() -> UserConfig {
    let config_path = get_houston_dir().join(CONFIG_FILE_NAME);

    if !config_path.exists() {
        return UserConfig::default();
    }

    let config_file = File::open(config_path).unwrap();
    let config: UserConfig = serde_yaml::from_reader(config_file).unwrap();

    config
}


/// load user config and convert it to a strict version
/// for that it will resolve all the required fields that are not set in the user config
/// either from environment variables or from the system.
pub fn load_user_config_strict() -> Result<StrictUserConfig, String> {
    let user_config = load_user_config();
    user_config.to_strict()
}

pub fn create_user_config_if_not_exists() {
    let config_path = get_houston_dir().join(CONFIG_FILE_NAME);

    if config_path.exists() {
        return;
    }

    let default_config = UserConfig::default_for_system();
    let default_config_str = serde_yaml::to_string(&default_config).unwrap();

    std::fs::create_dir_all(get_houston_dir()).unwrap();
    std::fs::write(config_path, default_config_str).unwrap();
}