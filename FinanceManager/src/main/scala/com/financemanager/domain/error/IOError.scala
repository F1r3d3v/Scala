package com.financemanager.domain.error

enum IOError(val message: String):
  case FileNotFound(path: String) extends IOError("File not found: " + path)
  case ReadError(path: String, cause: String)
      extends IOError("Error reading file " + path + ": " + cause)
  case WriteError(path: String, cause: String)
      extends IOError("Error writing file " + path + ": " + cause)
  case EndOfFile(path: String)
      extends IOError("Unexpected end of file: " + path)
