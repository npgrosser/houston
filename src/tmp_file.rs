use std::io;

use std::fs::File;
use std::io::Write;
use tempfile::TempPath;

/// Tmp file that allows creating the content once
pub struct SharableTmpFile {
    pub(crate) path: TempPath,
}

impl SharableTmpFile {
    pub fn new(content: &str, suffix: &str) -> io::Result<Self> {
        let tmp_path = tempfile::Builder::new()
            .suffix(suffix)
            .tempfile()?
            .into_temp_path();
        let mut tmp_file = File::create(&tmp_path)?;
        tmp_file.write_all(content.as_bytes())?;

        Ok(SharableTmpFile {
            path: tmp_path,
        })
    }
}

impl Drop for SharableTmpFile {
    fn drop(&mut self) {
        let _ = std::fs::remove_file(&self.path);
    }
}