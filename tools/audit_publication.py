from __future__ import annotations

import os
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

MAX_FILE_BYTES = 1024 * 1024
FORBIDDEN_ROOT_NAMES = frozenset({
    "task.md", "protocol.md", "changelog.md", "submission.zip", "submission.tar",
})
FORBIDDEN_SEGMENTS = frozenset({
    "exchange", "sim", "simulator", "transcript", "transcripts", "hold" + "out",
    "in" + "validated", "raw", "build", "target", "__pycache__", ".pytest_cache",
})
FORBIDDEN_SUFFIXES = frozenset({
    ".jar", ".class", ".pyc", ".jsonl", ".xz", ".zip", ".gz", ".tar",
    ".tgz", ".7z", ".exe", ".dll", ".so", ".dylib", ".pem", ".key",
    ".p12", ".pfx",
})
PROTECTED_DATASET_PATTERN = re.compile(r"\b(?:hold" + "out|in" + "validated)" + r"\b", re.IGNORECASE)
WINDOWS_ABSOLUTE_PATH_PATTERN = re.compile(r"\b[A-Za-z]:\\(?:Users|home|Documents|Desktop)\\", re.IGNORECASE)
UNIX_ABSOLUTE_PATH_PATTERN = re.compile(r"(?<![\w.-])/(?:Users|home)/[^\s'\"`]+")
EMAIL_PATTERN = re.compile(r"\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b", re.IGNORECASE)
UPLOAD_URL_PATTERN = re.compile(r"https?://[^\s'\"<>]*(?:upload|uploads|submit)[^\s'\"<>]*", re.IGNORECASE)
CREDENTIAL_PATTERN = re.compile(
    r"\b(?:secret|token|api[_-]?key|password|passwd|private[_-]?key)\w*\s*[:=]\s*[^\s'\"]+",
    re.IGNORECASE,
)
@dataclass(frozen=True, order=True)
class Finding:
    rule: str
    path: Path
    detail: str


@dataclass(frozen=True)
class AuditResult:
    ok: bool
    findings: tuple[Finding, ...]
    files_scanned: int


def audit_repository(root: Path) -> AuditResult:
    """Audit a candidate public tree without following links or silently skipping files."""
    root = Path(root)
    findings: list[Finding] = []
    files_scanned = 0

    if not root.is_dir():
        return AuditResult(False, (Finding("invalid_root", root, "root is not a directory"),), 0)

    for path in _walk_entries(root, findings):
        relative = path.relative_to(root)
        if path.is_symlink():
            findings.append(Finding("symlink", relative, "symbolic links are not publishable"))
            continue
        if not path.is_file():
            findings.append(Finding("unsupported_file_type", relative, "entry is not a regular file"))
            continue

        files_scanned += 1
        _audit_path(relative, findings)
        _audit_file(path, relative, findings)

    ordered_findings = tuple(sorted(findings))
    return AuditResult(not ordered_findings, ordered_findings, files_scanned)


def _walk_entries(root: Path, findings: list[Finding]) -> Iterable[Path]:
    def visit(directory: Path) -> Iterable[Path]:
        try:
            entries = sorted(directory.iterdir(), key=lambda entry: entry.name.casefold())
        except OSError as error:
            findings.append(Finding("unreadable_directory", directory.relative_to(root), str(error)))
            return
        for entry in entries:
            relative = entry.relative_to(root)
            if entry.name == ".git" and entry.is_dir() and not entry.is_symlink():
                continue
            if entry.is_symlink():
                yield entry
            elif entry.is_dir():
                yield from visit(entry)
            else:
                yield entry

    yield from visit(root)


def _audit_path(relative: Path, findings: list[Finding]) -> None:
    parts = tuple(part.casefold() for part in relative.parts)
    name = relative.name.casefold()
    has_forbidden_path = (
        name in FORBIDDEN_ROOT_NAMES
        or relative.stem.casefold() in FORBIDDEN_SEGMENTS
        or any(part in FORBIDDEN_SEGMENTS for part in parts)
    )
    if has_forbidden_path:
        findings.append(Finding("forbidden_path", relative, "contains a prohibited publication path"))
    elif relative.suffix.casefold() in FORBIDDEN_SUFFIXES:
        findings.append(Finding("forbidden_suffix", relative, f"prohibited suffix: {relative.suffix}"))


def _audit_file(path: Path, relative: Path, findings: list[Finding]) -> None:
    try:
        size = path.stat().st_size
    except OSError as error:
        findings.append(Finding("unreadable_file", relative, str(error)))
        return
    if size > MAX_FILE_BYTES:
        findings.append(Finding("large_file", relative, f"{size} bytes exceeds {MAX_FILE_BYTES} byte limit"))
        return
    try:
        content = path.read_bytes()
    except OSError as error:
        findings.append(Finding("unreadable_file", relative, str(error)))
        return
    if b"\x00" in content:
        findings.append(Finding("binary_file", relative, "NUL byte detected"))
        return
    try:
        text = content.decode("utf-8")
    except UnicodeDecodeError:
        findings.append(Finding("binary_file", relative, "not valid UTF-8 text"))
        return

    _append_text_findings(text, relative, findings)


def _append_text_findings(text: str, relative: Path, findings: list[Finding]) -> None:
    checks = (
        ("absolute_path", WINDOWS_ABSOLUTE_PATH_PATTERN),
        ("absolute_path", UNIX_ABSOLUTE_PATH_PATTERN),
        ("protected_dataset_term", PROTECTED_DATASET_PATTERN),
        ("upload_url", UPLOAD_URL_PATTERN),
        ("secret_assignment", CREDENTIAL_PATTERN),
    )
    if EMAIL_PATTERN.search(text):
        findings.append(Finding("email_address", relative, "email address detected"))
    for rule, pattern in checks:
        match = pattern.search(text)
        if match:
            findings.append(Finding(rule, relative, match.group(0)))


def main(argv: list[str] | None = None) -> int:
    arguments = sys.argv[1:] if argv is None else argv
    root = Path(arguments[0]) if len(arguments) == 1 else Path(".")
    if len(arguments) > 1:
        print("usage: audit_publication.py [REPOSITORY_ROOT]", file=sys.stderr)
        return 2
    result = audit_repository(root)
    print(f"scanned {result.files_scanned} files")
    for finding in result.findings:
        print(f"{finding.path}: {finding.rule}: {finding.detail}")
    return 0 if result.ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
