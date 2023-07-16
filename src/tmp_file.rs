use std::io;

use std::fs::File;
use std::io::Write;
use tempfile::TempPath;

/// Tmp file that closes the file directly after creation to make it sharable for reading.
pub struct SharableTmpFile {
    pub(crate) path: TempPath,
}

impl SharableTmpFile {
    /// Creates a new sharable tmp file with the given text content and name suffix.
    /// The file is located in the system's temp directory.
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